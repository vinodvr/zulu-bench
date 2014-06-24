import com.fasterxml.jackson.annotation.JsonProperty;
import com.yammer.dropwizard.config.Configuration;
import org.hibernate.validator.constraints.NotEmpty;

import javax.validation.constraints.NotNull;

/**
 * To change this template use File | Settings | File Templates.
 */
public class ZuluServiceConfig extends Configuration {

    @NotEmpty
    @JsonProperty
    private String redisHostName;

    @NotNull
    @JsonProperty
    private Integer maxRedisConnections;

    @NotNull
    @JsonProperty
    private String esClusterName;

    @NotNull
    @JsonProperty
    private String esClusterUnicastHosts;


    public String getRedisHostName() {
        return redisHostName;
    }

    public int getMaxRedisConnections() {
        return maxRedisConnections;
    }

    public String getEsClusterName() {
        return esClusterName;
    }

    public String getEsClusterUnicastHosts() {
        return esClusterUnicastHosts;
    }
}
