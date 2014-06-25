import com.fasterxml.jackson.databind.ObjectMapper;
import com.google.common.base.Optional;
import com.google.common.collect.ArrayListMultimap;
import com.google.common.collect.Lists;
import com.google.common.collect.Multimap;
import com.yammer.metrics.annotation.Timed;
import org.elasticsearch.action.get.GetResponse;
import org.elasticsearch.action.get.MultiGetItemResponse;
import org.elasticsearch.action.get.MultiGetResponse;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.xerial.snappy.Snappy;
import redis.clients.jedis.Jedis;
import redis.clients.jedis.ShardedJedis;
import redis.clients.jedis.exceptions.JedisConnectionException;

import javax.ws.rs.*;
import javax.ws.rs.core.MediaType;
import java.io.IOException;
import java.io.UnsupportedEncodingException;
import java.util.Collection;
import java.util.Date;
import java.util.List;
import java.util.Map;
import java.util.concurrent.Callable;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;

/**
 * To change this template use File | Settings | File Templates.
 */
@Path("/views")
@Produces(MediaType.APPLICATION_JSON)
public class ViewResource {

    private static final Logger logger = LoggerFactory.getLogger(ViewResource.class);

    private final JedisManaged jedis;
    private final ESManaged es;
    private final String defaultViewJson;
    private final ObjectMapper objectMapper;
    private final ExecutorService executorService;


    public ViewResource(JedisManaged jedis, ESManaged es, String defaultViewJson, ObjectMapper objectMapper, Integer executorPoolSize) {
        this.jedis = jedis;
        this.es = es;
        this.defaultViewJson = defaultViewJson;
        this.objectMapper = objectMapper;
        this.executorService = Executors.newFixedThreadPool(executorPoolSize);

    }

    @POST
    @Timed
    @Consumes(MediaType.APPLICATION_JSON)
    public void createViews(CreateViewRequest request) {
        try {
            String viewName = request.getViewName();
            String entityIdPrefix = request.getEntityIdPrefix();
            int counter = request.getCounter();
            int viewSizeInBytes = request.getViewSizeInBytes();

            String viewData = generateJsonDataSize(viewSizeInBytes);
            byte[] finalViewData;
            String compressionType = null;
            if (request.isCompress()) {
                finalViewData = compress(viewData);
                compressionType = "SNAPPY";
            } else {
                finalViewData = viewData.getBytes("UTF-8");
            }

            for (int i = 0; i < counter; i++) {
                String entityId = entityIdPrefix + "-" + i;
                String id = generateId(entityId, viewName);
                ComputedView cv = new ComputedView(entityId, viewName, new Date(), 1L, compressionType, finalViewData);
                byte[] cvJson = objectMapper.writeValueAsBytes(cv);

                //index in ES
                indexInES(id, cvJson);

                //set in redis
                setInRedis(id, cvJson);

                logger.info("createdView with id: {}", id);
            }

        } catch (Exception e){
            logger.error("Exception in createViews for request: {}", request, e);
            throw new RuntimeException(e);
        }
    }

    private void setInRedis(String id, byte[] cvJson) throws UnsupportedEncodingException {
        boolean broken = false;
        ShardedJedis resource = jedis.getPool().getResource();
        try {
            resource.set(id.getBytes("UTF-8"), cvJson);
        } catch (JedisConnectionException e) {
            broken = true;
            throw new RuntimeException(e);
        } finally {
            if (broken) {
                jedis.getPool().returnBrokenResource(resource);
            } else {
                jedis.getPool().returnResource(resource);
            }
        }
    }

    private void indexInES(String id, byte[] cvJson) {
        es.getClient().prepareIndex("zulu", "views", id)
                .setSource(cvJson)
                .execute()
                .actionGet();
    }

    private String generateJsonDataSize(int viewSizeInBytes) {
        StringBuilder toRet = new StringBuilder();
        if (defaultViewJson.length() > viewSizeInBytes) {
            toRet.append(defaultViewJson.substring(0, viewSizeInBytes));
        } else {
            for (int i = 0; i < viewSizeInBytes/defaultViewJson.length(); i++) {
                toRet.append(defaultViewJson);
            }
            toRet.append(defaultViewJson.substring(0, viewSizeInBytes - toRet.length()));
        }
        logger.info("Returning string of size: {} KB", toRet.length()/1024);
        return toRet.toString();
    }

    private byte[] compress(String str) throws IOException {
        byte[] bytes = str.getBytes("UTF-8");
        int inpSize = bytes.length;
        byte[] compress = Snappy.compress(bytes);
        int outputSize = compress.length;
        logger.info("Compressed from {} to {}, compression ratio {}", inpSize, outputSize, inpSize/outputSize);
        return compress;
    }


    private String decompress(byte[] compressedBytes) throws IOException {
        byte[] uncompress = Snappy.uncompress(compressedBytes);
        return new String(uncompress, "UTF-8");
    }


    @GET
    @Timed
    public ViewResponse getViews(@QueryParam("entityIds") String commaSeparatedEntityIds,
                                 @QueryParam("viewNames") String commaSeparatedViewNames,
                                 @QueryParam("addToGC") Optional<Boolean> addToGC,
                                 @QueryParam("decompress") Optional<Boolean> decompress,
                                 @QueryParam("fetchFromCache") Optional<Boolean> fetchFromCache) {

        try {

            String[] entityIds = commaSeparatedEntityIds.split(",");
            String[] viewNames = commaSeparatedViewNames.split(",");

            if (fetchFromCache.or(true)) {
                return fetchFromRedis(addToGC, decompress, entityIds, viewNames);
            } else {
                return fetchFromES(addToGC, decompress, entityIds, viewNames);
            }
        } catch (Exception e) {
            logger.error("Exception in getViews for request entityIds: {}, viewNames: {}",
                    commaSeparatedEntityIds, commaSeparatedViewNames, e);
            throw new RuntimeException(e);
        }
    }

    private ViewResponse fetchFromES(Optional<Boolean> addToGC, Optional<Boolean> decompress, String[] entityIds, String[] viewNames)
            throws Exception {
        List<View> views = Lists.newArrayList();
        List<EntityViewNameTuple> missingViews = Lists.newArrayList();
        List<String> ids = Lists.newArrayList();
        for (String entityId : entityIds) {
            for (String viewName : viewNames) {
                ids.add(generateId(entityId, viewName));
            }
        }
        MultiGetResponse multiResponse = es.getClient().prepareMultiGet().add("zulu", "views", ids).execute().get();
        MultiGetItemResponse[] responses = multiResponse.getResponses();
        for (MultiGetItemResponse response : responses) {
            String id = response.getId();
            if (response.isFailed()) {
                throw new RuntimeException("ES failed for id:" + id + ", with message: " + response.getFailure().getMessage());
            } else {
                GetResponse getResponse = response.getResponse();
                if (getResponse.isExists()) {
                    convertToViewObj(addToGC, decompress, views, getResponse);
                } else {
                    missingViews.add(convertToTuple(id));
                }
            }
        }
        return new ViewResponse(views, missingViews);
    }


    class CallableResult {
        List<View> views = Lists.newArrayList();
        List<EntityViewNameTuple> missingViews = Lists.newArrayList();
    }

    private ViewResponse fetchFromRedis(final Optional<Boolean> addToGC, final Optional<Boolean> decompress, String[] entityIds, String[] viewNames)
            throws IOException {
        final List<View> views = Lists.newArrayList();
        final List<EntityViewNameTuple> missingViews = Lists.newArrayList();
        boolean broken = false;
        ShardedJedis resource = jedis.getPool().getResource();
        try {
            Multimap<Jedis, String> shardToKeysMap = ArrayListMultimap.create();
            for (String entityId : entityIds) {
                for (String viewName : viewNames) {
                    String key = generateId(entityId, viewName);
                    Jedis shard = resource.getShard(key);
                    shardToKeysMap.put(shard, key);
                }
            }

            List<Callable<CallableResult>> tasks = Lists.newArrayList();
            for (final Map.Entry<Jedis, Collection<String>> entry : shardToKeysMap.asMap().entrySet()) {
                tasks.add(new Callable<CallableResult>() {
                    @Override
                    public CallableResult call() throws Exception {
                        CallableResult result = new CallableResult();
                        Jedis jedis = entry.getKey();
                        Collection<String> keys = entry.getValue();
                        List<String> responses = jedis.mget(keys.toArray(new String[keys.size()]));

                        int i = 0;
                        for (String key : keys) {
                            String entityId = extractId(key);
                            String viewName = extractViewName(key);
                            String keyResponse = responses.get(i++);
                            if (keyResponse == null) {
                                result.missingViews.add(new EntityViewNameTuple(entityId, viewName));
                            } else {
                                ComputedView cv = objectMapper.readValue(keyResponse, ComputedView.class);
                                byte[] viewAsBytes = cv.getView();
                                String view = handleCompression(decompress, viewAsBytes);
                                result.views.add(new View(view, entityId, viewName));
                                handleGC(addToGC, view);
                            }
                        }
                        return result;
                    }
                });
            }

            try {
                List<Future<CallableResult>> futures = executorService.invokeAll(tasks);
                for (Future<CallableResult> future: futures) {
                    CallableResult callableResult = future.get();
                    views.addAll(callableResult.views);
                    missingViews.addAll(callableResult.missingViews);
                }
            } catch (InterruptedException e) {
                throw new RuntimeException(e);
            }
        } catch (JedisConnectionException e) {
            logger.error("Exception in getViews: ", e);
            broken = true;
            throw new RuntimeException(e);
        } catch (Exception e) {
            logger.error("Exception in getViews: ", e);
            throw new RuntimeException(e);
        } finally {
            if (broken) {
                jedis.getPool().returnBrokenResource(resource);
            } else {
                jedis.getPool().returnResource(resource);
            }
        }
        return new ViewResponse(views, missingViews);
    }


    private void convertToViewObj(Optional<Boolean> addToGC, Optional<Boolean> decompress, List<View> views, GetResponse getResponse) throws IOException {
        ComputedView cv = objectMapper.readValue(getResponse.getSourceAsBytes(), ComputedView.class);
        byte[] viewAsBytes = cv.getView();
        String view = handleCompression(decompress, viewAsBytes);
        views.add(new View(view, cv.getEntityId(), cv.getViewName()));
        handleGC(addToGC, view);
    }

    private String handleCompression(Optional<Boolean> decompress, byte[] viewAsBytes) throws IOException {
        String view;
        if (decompress.or(true)) {
            view = decompress(viewAsBytes);
        } else {
            view = new String(viewAsBytes, "UTF-8");
        }
        return view;
    }


    private void handleGC(Optional<Boolean> addToGC, String view) {
        if (addToGC.or(false)) {
            String gcStr = view + System.currentTimeMillis();
            //noinspection ResultOfMethodCallIgnored
            gcStr.length();
        }
    }

    private EntityViewNameTuple convertToTuple(String str) {
        String entityId = str.substring(2);
        return new EntityViewNameTuple(entityId, entityId);     //TODO: fix this line
    }

    public static String generateId(String entityId, String viewName) {
        return "_ZV_" + entityId + "_" + viewName;
    }

    private String extractId(String key) {
        return key.substring(4, key.lastIndexOf("_"));
    }

    private String extractViewName(String key) {
        return key.substring(key.lastIndexOf("_") + 1, key.length());
    }

}
