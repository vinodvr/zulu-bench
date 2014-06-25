import com.fasterxml.jackson.databind.ObjectMapper;
import com.yammer.dropwizard.Service;
import com.yammer.dropwizard.config.Bootstrap;
import com.yammer.dropwizard.config.Environment;
import org.apache.commons.io.IOUtils;

import java.io.InputStream;

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
                conf.getMaxRedisConnections());
        ESManaged esManaged = new ESManaged(conf.getEsClusterName(), conf.getEsClusterUnicastHosts());
        ObjectMapper objectMapper = new ObjectMapper();

        InputStream resourceAsStream = getClass().getClassLoader().getResourceAsStream("default-view.json");
        String defaultViewJson = IOUtils.toString(resourceAsStream, "UTF-8");
        environment.manage(jedisManaged);
        environment.manage(esManaged);
        environment.addResource(new ViewResource(jedisManaged, esManaged, defaultViewJson, objectMapper,conf.getExecutorPoolSize()));
        environment.addServlet(new LargeViewServlet(jedisManaged), "/largeViews");
        environment.addHealthCheck(new ZuluServiceHealthCheck());
    }
}
