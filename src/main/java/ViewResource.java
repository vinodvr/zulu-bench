import com.google.common.collect.Lists;
import com.yammer.metrics.annotation.Timed;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import redis.clients.jedis.Jedis;
import redis.clients.jedis.exceptions.JedisConnectionException;

import javax.ws.rs.*;
import javax.ws.rs.core.MediaType;
import java.util.List;

/**
 * To change this template use File | Settings | File Templates.
 */
@Path("/views")
@Produces(MediaType.APPLICATION_JSON)
public class ViewResource {

    private static final Logger logger = LoggerFactory.getLogger(ViewResource.class);


    private final JedisManaged jedis;

    public ViewResource(JedisManaged jedis) {
        this.jedis = jedis;
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
                long viewSizeInBytes = request.getViewSizeInBytes();

                StringBuilder viewDataBuilder = new StringBuilder();
                for (int i = 0; i < viewSizeInBytes; i++) {
                    viewDataBuilder.append("V");
                }
                String viewData = viewDataBuilder.toString();
                for (int i = 0; i < counter; i++) {
                    String key = generateRedisKey(entityIdPrefix + "-" + i, viewName);
                    String resp = resource.set(key, viewData);
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


    @GET
    @Timed
    public ViewResponse getViews(@QueryParam("entityIds") String commaSeparatedEntityIds,
                                 @QueryParam("viewNames") String commaSeparatedViewNames) {
        try {
            String[] entityIds = commaSeparatedEntityIds.split(",");
            String[] viewNames = commaSeparatedViewNames.split(",");
            boolean broken = false;
            Jedis resource = jedis.getPool().getResource();
            try {
                List<View> views = Lists.newArrayList();
                List<EntityViewNameTuple> missingViews = Lists.newArrayList();
                for (String entityId : entityIds) {
                    for (String viewName : viewNames) {
                        String key = generateRedisKey(entityId, viewName);
                        String response = resource.get(key);
                        if (response == null) {
                            missingViews.add(new EntityViewNameTuple(entityId, viewName));
                        } else {
                            //TODO: remove System.millis below..
                            views.add(new View(response + System.currentTimeMillis(), entityId, viewName));
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

    private String generateRedisKey(String entityId, String viewName) {
        return "_ZV_" + entityId + "_" + viewName;
    }
}
