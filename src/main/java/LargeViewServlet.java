import redis.clients.jedis.Jedis;
import redis.clients.jedis.exceptions.JedisConnectionException;

import javax.servlet.ServletException;
import javax.servlet.ServletOutputStream;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;

/**
 * To change this template use File | Settings | File Templates.
 */
public class LargeViewServlet extends HttpServlet {

    private final JedisManaged jedis;

    public LargeViewServlet(JedisManaged jedis) {
        this.jedis = jedis;
    }

    protected void doGet(HttpServletRequest request,
                         HttpServletResponse response) throws ServletException, IOException {

        String[] entityIds = request.getParameter("entityIds").split(",");
        String[] viewNames = request.getParameter("viewNames").split(",");

        ServletOutputStream outputStream = response.getOutputStream();

        for (String entityId : entityIds) {
            for (String viewName: viewNames) {
                String redisKey = ViewResource.generateRedisKey(entityId, viewName);
                boolean found = writeResponse(redisKey, outputStream);
            }
        }

        outputStream.flush();

    }

    private boolean writeResponse(String redisKey, ServletOutputStream outputStream) throws IOException {
        boolean broken = false;
        Jedis resource = jedis.getPool().getResource();
        try {
            outputStream.print(redisKey + ":");
            boolean found = resource.streamGet(redisKey, outputStream);
            if (!found)
                outputStream.print("NOT-FOUND");
            outputStream.println();
            return found;
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
}
