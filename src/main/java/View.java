import com.google.common.base.Objects;

/**
 * To change this template use File | Settings | File Templates.
 */
public class View {
    private final String view;
    private final String entityId;
    private final String viewName;


    public View(String view, String entityId, String viewName) {
        this.view = view;
        this.entityId = entityId;
        this.viewName = viewName;
    }

    public String getView() {
        return view;
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
                .add("view", view)
                .add("entityId", entityId)
                .add("viewName", viewName)
                .toString();
    }
}
