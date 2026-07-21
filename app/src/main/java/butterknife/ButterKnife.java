package butterknife;

import android.app.Activity;
import android.view.View;

public class ButterKnife {
    public static Unbinder bind(Activity activity) {
        return bind(activity, activity.getWindow().getDecorView());
    }

    public static Unbinder bind(Object target, Activity activity) {
        return bind(target, activity.getWindow().getDecorView());
    }

    public static Unbinder bind(Object target, View source) {
        try {
            Class<?> bindingClass = Class.forName(
                    target.getClass().getName() + "_ViewBinding");
            return (Unbinder) bindingClass
                    .getConstructor(target.getClass(), View.class)
                    .newInstance(target, source);
        } catch (Exception e) {
            // ViewBinding class not found — views won't be wired
            return () -> {};
        }
    }
}
