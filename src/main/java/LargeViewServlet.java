import org.apache.commons.io.IOUtils;
import redis.clients.jedis.Jedis;
import redis.clients.jedis.exceptions.JedisConnectionException;

import javax.servlet.ServletException;
import javax.servlet.ServletOutputStream;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.zip.GZIPInputStream;

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
        boolean decompress = request.getParameter("decompress") == null ?
                true :
                Boolean.valueOf(request.getParameter("decompress"));
        ServletOutputStream outputStream = response.getOutputStream();

        for (String entityId : entityIds) {
            for (String viewName: viewNames) {
                String redisKey = ViewResource.generateRedisKey(entityId, viewName);
                writeResponse(redisKey, outputStream, decompress);
            }
        }

        outputStream.flush();

    }

    private void writeResponse(String redisKey, ServletOutputStream outputStream, boolean decompress) throws IOException {
        boolean broken = false;
        Jedis resource = jedis.getPool().getResource();
        try {
            outputStream.print(redisKey + ":");
            byte[] result = resource.get(redisKey.getBytes("UTF-8"));
            if (result == null) {
                outputStream.print("NOT-FOUND");
            } else if (decompress) {
                decompressAndStream(result, outputStream);
            } else {
                outputStream.print(new String(result, "UTF-8"));
            }
            outputStream.println();
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

    private void decompressAndStream(byte[] compressedBytes, ServletOutputStream os) throws IOException {
        InputStream sis = new ByteArrayInputStream(compressedBytes);
        GZIPInputStream gis = new GZIPInputStream(sis);
        try {
            IOUtils.copy(gis, os);
        } finally {
            IOUtils.closeQuietly(gis);
            IOUtils.closeQuietly(sis);
        }
    }

}
