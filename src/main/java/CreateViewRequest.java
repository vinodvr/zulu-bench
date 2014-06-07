import com.google.common.base.Objects;

import javax.validation.constraints.NotNull;
import javax.xml.bind.annotation.XmlRootElement;

/**
 * To change this template use File | Settings | File Templates.
 */
@XmlRootElement
public class CreateViewRequest {
    @NotNull
    private String viewName;
    @NotNull
    private String entityIdPrefix;
    @NotNull
    private int counter;
    @NotNull
    private long viewSizeInBytes;

    public void setViewName(String viewName) {
        this.viewName = viewName;
    }

    public void setEntityIdPrefix(String entityIdPrefix) {
        this.entityIdPrefix = entityIdPrefix;
    }

    public void setCounter(int counter) {
        this.counter = counter;
    }

    public void setViewSizeInBytes(long viewSizeInBytes) {
        this.viewSizeInBytes = viewSizeInBytes;
    }

    public String getViewName() {
        return viewName;
    }

    public String getEntityIdPrefix() {
        return entityIdPrefix;
    }

    public int getCounter() {
        return counter;
    }

    public long getViewSizeInBytes() {
        return viewSizeInBytes;
    }

    @Override
    public String toString() {
        return Objects.toStringHelper(this)
                .add("viewName", viewName)
                .add("entityIdPrefix", entityIdPrefix)
                .add("counter", counter)
                .add("viewSizeInBytes", viewSizeInBytes)
                .toString();
    }
}
