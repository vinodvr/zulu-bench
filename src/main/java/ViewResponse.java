import com.google.common.base.Objects;

import java.util.List;

/**
 * To change this template use File | Settings | File Templates.
 */
public class ViewResponse {
    private final List<View> views;
    private final List<EntityViewNameTuple> missingViews;

    public ViewResponse(List<View> views, List<EntityViewNameTuple> missingViews) {
        this.views = views;
        this.missingViews = missingViews;
    }

    public List<View> getViews() {
        return views;
    }

    public List<EntityViewNameTuple> getMissingViews() {
        return missingViews;
    }

    @Override
    public String toString() {
        return Objects.toStringHelper(this)
                .add("views", views)
                .add("missingViews", missingViews)
                .toString();
    }
}
