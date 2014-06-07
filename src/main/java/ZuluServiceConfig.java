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
    private Integer redisPort;

    @NotNull
    @JsonProperty
    private Integer maxRedisConnections;


    public String getRedisHostName() {
        return redisHostName;
    }

    public int getRedisPort() {
        return redisPort;
    }

    public int getMaxRedisConnections() {
        return maxRedisConnections;
    }
}
