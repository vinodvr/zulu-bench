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
    private int viewSizeInBytes;
    @NotNull
    private boolean compress = true;

    public boolean isCompress() {
        return compress;
    }

    public void setCompress(boolean compress) {
        this.compress = compress;
    }

    public void setViewName(String viewName) {
        this.viewName = viewName;
    }

    public void setEntityIdPrefix(String entityIdPrefix) {
        this.entityIdPrefix = entityIdPrefix;
    }

    public void setCounter(int counter) {
        this.counter = counter;
    }

    public void setViewSizeInBytes(int viewSizeInBytes) {
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

    public int getViewSizeInBytes() {
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
