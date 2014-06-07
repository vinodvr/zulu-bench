import com.google.common.base.Objects;

/**
 * To change this template use File | Settings | File Templates.
 */
public class EntityViewNameTuple {
    private final String entityId;
    private final String viewName;

    public EntityViewNameTuple(String entityId, String viewName) {
        this.entityId = entityId;
        this.viewName = viewName;
    }

    public String getEntityId() {
        return entityId;
    }

    public String getViewName() {
        return viewName;
    }

    @Override
    public String toString() {
        return Objects.toStringHelper(this)
                .add("entityId", entityId)
                .add("viewName", viewName)
                .toString();
    }
}
