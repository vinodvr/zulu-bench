import com.google.common.base.Optional;
import com.google.common.collect.Lists;
import com.google.common.collect.Maps;
import com.yammer.metrics.annotation.Timed;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.xerial.snappy.Snappy;
import redis.clients.jedis.Jedis;
import redis.clients.jedis.exceptions.JedisConnectionException;

import javax.ws.rs.*;
import javax.ws.rs.core.MediaType;
import java.io.IOException;
import java.util.List;
import java.util.concurrent.ConcurrentMap;

/**
 * To change this template use File | Settings | File Templates.
 */
@Path("/views")
@Produces(MediaType.APPLICATION_JSON)
public class ViewResource {

    private static final Logger logger = LoggerFactory.getLogger(ViewResource.class);

    private final ConcurrentMap<String, String> viewsCache = Maps.newConcurrentMap();


    private final JedisManaged jedis;
    private final String defaultViewJson;

    public ViewResource(JedisManaged jedis, String defaultViewJson) {
        this.jedis = jedis;
        this.defaultViewJson = defaultViewJson;
    }

    @POST
    @Timed
    @Consumes(MediaType.APPLICATION_JSON)
    public void createViews(CreateViewRequest request) {
        try {
            boolean broken = false;
            Jedis resource = jedis.getPool().getResource();
            try {
                String viewName = request.getViewName();
                String entityIdPrefix = request.getEntityIdPrefix();
                int counter = request.getCounter();
                int viewSizeInBytes = request.getViewSizeInBytes();

                String viewData = generateJsonDataSize(viewSizeInBytes);
                byte[] compressedViewData;
                if (request.isCompress()) {
                    compressedViewData = compress(viewData);
                } else {
                    compressedViewData = viewData.getBytes("UTF-8");
                }

                for (int i = 0; i < counter; i++) {
                    String key = generateRedisKey(entityIdPrefix + "-" + i, viewName);
                    String resp = resource.set(key.getBytes("UTF-8"), compressedViewData);
                    logger.info("createdView with key: {}, resp: {}", key, resp);
                }

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
        } catch (Exception e){
            logger.error("Exception in createViews for request: {}", request, e);
            throw new RuntimeException(e);
        }
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
         /*InputStream sis = new ByteArrayInputStream(str.getBytes("UTF-8"));
        ByteArrayOutputStream os = new ByteArrayOutputStream();
        GZIPOutputStream gos = new GZIPOutputStream(os);
        try {
            IOUtils.copy(sis, gos);
        } finally {
            IOUtils.closeQuietly(gos);
            IOUtils.closeQuietly(os);
            IOUtils.closeQuietly(sis);
        }
        byte[] bytes = os.toByteArray();
        int outputSize = bytes.length;*/
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
        /*InputStream sis = new ByteArrayInputStream(compressedBytes);
        GZIPInputStream gis = new GZIPInputStream(sis);
        StringWriter sw = new StringWriter();
        try {
            IOUtils.copy(gis, sw);
            return sw.toString();
        } finally {
            IOUtils.closeQuietly(sw);
            IOUtils.closeQuietly(gis);
            IOUtils.closeQuietly(sis);
        }*/
    }


    @GET
    @Timed
    public ViewResponse getViews(@QueryParam("entityIds") String commaSeparatedEntityIds,
                                 @QueryParam("viewNames") String commaSeparatedViewNames,
                                 @QueryParam("addToGC") Optional<Boolean> addToGC,
                                 @QueryParam("decompress") Optional<Boolean> decompress) {
        try {
            String[] entityIds = commaSeparatedEntityIds.split(",");
            String[] viewNames = commaSeparatedViewNames.split(",");
            boolean broken = false;
            Jedis resource = jedis.getPool().getResource();
            try {
                byte[][] keys = new byte[entityIds.length * viewNames.length][];
                int i = 0;
                for (String entityId : entityIds) {
                    for (String viewName : viewNames) {
                        String key = generateRedisKey(entityId, viewName);
                        keys[i++] = key.getBytes("UTF-8");
                    }
                }

                List<byte[]> multiGetResponse = resource.mget(keys);

                List<View> views = Lists.newArrayList();
                List<EntityViewNameTuple> missingViews = Lists.newArrayList();
                int j = 0;
                for (String entityId : entityIds) {
                    for (String viewName : viewNames) {
                        byte[] responseCompressed = multiGetResponse.get(j++);

                        if (responseCompressed == null) {
                            missingViews.add(new EntityViewNameTuple(entityId, viewName));
                        } else {
                            String response;
                            if (decompress.or(true)) {
                                response = decompress(responseCompressed);
                            } else {
                                response = new String(responseCompressed, "UTF-8");
                            }
                            views.add(new View(response, entityId, viewName));
                            if (addToGC.or(false)) {
                                String gcStr = response + System.currentTimeMillis();
                                //noinspection ResultOfMethodCallIgnored
                                gcStr.length();
                            }
                        }
                    }
                }
                return new ViewResponse(views, missingViews);
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
        } catch (Exception e){
            logger.error("Exception in getViews for request entityIds: {}, viewNames: {}",
                    commaSeparatedEntityIds, commaSeparatedViewNames, e);
            throw new RuntimeException(e);
        }
    }

    public static String generateRedisKey(String entityId, String viewName) {
        return "_ZV_" + entityId + "_" + viewName;
    }
}
