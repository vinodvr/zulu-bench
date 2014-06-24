import com.yammer.dropwizard.lifecycle.Managed;
import org.apache.commons.pool2.impl.GenericObjectPoolConfig;
import redis.clients.jedis.JedisShardInfo;
import redis.clients.jedis.ShardedJedisPool;

import java.util.ArrayList;
import java.util.List;

/**
 * To change this template use File | Settings | File Templates.
 */
public class JedisManaged implements Managed {

    private final String[] redisHostList;
    private final int maxConnections;
    private ShardedJedisPool pool;

    public JedisManaged(String redisHostList, int maxConnections) {
        this.redisHostList = redisHostList.split(",");
        this.maxConnections = maxConnections;
    }

    @Override
    public void start() throws Exception {
        GenericObjectPoolConfig poolConfig = new GenericObjectPoolConfig();
        poolConfig.setMaxTotal(maxConnections);
        poolConfig.setMaxIdle(maxConnections);
        poolConfig.setMinIdle(-1);
        poolConfig.setTestOnBorrow(true);

        List<JedisShardInfo> shards = new ArrayList<JedisShardInfo>();
        for (String redisHost: redisHostList) {
            String[] split = redisHost.split(":");
            JedisShardInfo si = new JedisShardInfo(split[0], Integer.valueOf(split[1]));
            shards.add(si);
        }
        pool = new ShardedJedisPool(poolConfig, shards);
    }


    @Override
    public void stop() throws Exception {
        pool.destroy();
    }

    public ShardedJedisPool getPool() {
        return pool;
    }
}
