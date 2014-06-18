import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;

import java.util.Date;

/**
 * To change this template use File | Settings | File Templates.
 */
public class ComputedView {

    private final String entityId;
    private final String viewName;
    private final Date createdAt;
    private final long version;
    private final String compressionType;
    private final byte[] view;

    @JsonCreator
    public ComputedView(@JsonProperty("entityId") String entityId,
                        @JsonProperty("viewName") String viewName,
                        @JsonProperty("createdAt") Date createdAt,
                        @JsonProperty("version") long version,
                        @JsonProperty("compressionType") String compressionType,
                        @JsonProperty("view") byte[] view) {
        this.entityId = entityId;
        this.viewName = viewName;
        this.createdAt = createdAt;
        this.version = version;
        this.compressionType = compressionType;
        this.view = view;
    }

    public String getEntityId() {
        return entityId;
    }

    public String getViewName() {
        return viewName;
    }

    public Date getCreatedAt() {
        return createdAt;
    }

    public long getVersion() {
        return version;
    }

    public String getCompressionType() {
        return compressionType;
    }

    public byte[] getView() {
        return view;
    }
}
