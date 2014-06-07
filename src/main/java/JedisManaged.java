import com.yammer.dropwizard.lifecycle.Managed;
import redis.clients.jedis.Jedis;
import redis.clients.jedis.JedisPool;
import redis.clients.jedis.JedisPoolConfig;
import redis.clients.util.Pool;

/**
 * To change this template use File | Settings | File Templates.
 */
public class JedisManaged implements Managed {

    private final String redisHost;
    private final int redisPort;
    private final int maxConnections;
    private Pool<Jedis> pool;

    public JedisManaged(String redisHost, int redisPort, int maxConnections) {

        this.redisHost = redisHost;
        this.redisPort = redisPort;
        this.maxConnections = maxConnections;
    }

    @Override
    public void start() throws Exception {
        JedisPoolConfig poolConfig = new JedisPoolConfig();
        poolConfig.setMaxTotal(maxConnections);
        poolConfig.setMaxIdle(maxConnections);
        poolConfig.setTestOnBorrow(true);
        pool = new JedisPool(poolConfig, redisHost, redisPort);
    }

    @Override
    public void stop() throws Exception {
        pool.destroy();
    }

    public Pool<Jedis> getPool() {
        return pool;
    }
}
