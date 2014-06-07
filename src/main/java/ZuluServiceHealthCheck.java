import com.yammer.metrics.core.HealthCheck;

/**
 * To change this template use File | Settings | File Templates.
 */
public class ZuluServiceHealthCheck extends HealthCheck {
    public ZuluServiceHealthCheck() {
        super(ZuluServiceHealthCheck.class.getName());
    }

    @Override
    protected Result check() throws Exception {
        return Result.healthy();
    }
}
