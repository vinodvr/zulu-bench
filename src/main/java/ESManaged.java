import com.yammer.dropwizard.lifecycle.Managed;
import org.elasticsearch.client.Client;
import org.elasticsearch.common.settings.ImmutableSettings;
import org.elasticsearch.common.settings.Settings;
import org.elasticsearch.node.Node;
import org.elasticsearch.node.NodeBuilder;

/**
 * To change this template use File | Settings | File Templates.
 */
public class ESManaged implements Managed {
    private final String esClusterName;
    private final String esClusterUnicastHosts;
    private Node node;

    public ESManaged(String esClusterName, String esClusterUnicastHosts) {

        this.esClusterName = esClusterName;
        this.esClusterUnicastHosts = esClusterUnicastHosts;
    }

    @Override
    public void start() throws Exception {
        //node = new NodeBuilder().client(true).clusterName(esClusterName).build().start();

        Settings settings = ImmutableSettings.settingsBuilder()
                .put("discovery.zen.ping.multicast.enabled", "false")
                .put("discovery.zen.ping.unicast.hosts", esClusterUnicastHosts)
                .build();
        node = new NodeBuilder().clusterName(esClusterName).client(true).settings(settings).build().start();
    }

    @Override
    public void stop() throws Exception {
        node.stop();
    }

    public Client getClient() {
        return node.client();
    }
}
