package butterknife.internal;

import android.view.View;
import androidx.annotation.NonNull;
import androidx.annotation.UiThread;

public class Utils {
    @NonNull
    @UiThread
    public static View findRequiredView(View source, int id, String description) {
        View view = source.findViewById(id);
        if (view != null) {
            return view;
        }
        throw new IllegalStateException(
                "Required view '" + description + "' with id " + id + " not found");
    }

    @SuppressWarnings("unchecked")
    @NonNull
    @UiThread
    public static <T extends View> T findRequiredViewAsType(
            View source, int id, String description, Class<T> cls) {
        View view = findRequiredView(source, id, description);
        return (T) view;
    }

    @SuppressWarnings("unchecked")
    public static <T extends View> T findOptionalViewAsType(
            View source, int id, String description, Class<T> cls) {
        View view = source.findViewById(id);
        return (T) view;
    }

    public static View findOptionalView(View source, int id) {
        return source.findViewById(id);
    }
}
