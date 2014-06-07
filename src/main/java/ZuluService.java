import com.yammer.dropwizard.Service;
import com.yammer.dropwizard.config.Bootstrap;
import com.yammer.dropwizard.config.Environment;

/**
 * To change this template use File | Settings | File Templates.
 */
public class ZuluService extends Service<ZuluServiceConfig> {

    public static void main(String[] args) throws Exception {
        new ZuluService().run(args);
    }

    @Override
    public void initialize(Bootstrap<ZuluServiceConfig> bootstrap) {
        bootstrap.setName("zulu-service");
    }

    @Override
    public void run(ZuluServiceConfig conf, Environment environment) throws Exception {
        JedisManaged jedisManaged = new JedisManaged(
                conf.getRedisHostName(),
                conf.getRedisPort(),
                conf.getMaxRedisConnections());
        environment.manage(jedisManaged);
        environment.addResource(new ViewResource(jedisManaged));
        environment.addHealthCheck(new ZuluServiceHealthCheck());
    }
}
