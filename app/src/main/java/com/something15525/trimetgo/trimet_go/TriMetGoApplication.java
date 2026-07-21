package com.something15525.trimetgo.trimet_go;

import android.app.Application;
import android.util.Log;
import com.parse.Parse;
import com.something15525.trimetgo.trimet_go.util.Constants2;

public class TriMetGoApplication extends Application {
    private static final String TAG = TriMetGoApplication.class.getSimpleName();

    @Override // android.app.Application
    public void onCreate() {
        super.onCreate();
        com.google.android.material.color.DynamicColors.applyToActivitiesIfAvailable(this);
        String parseAppId = Constants2.getParseAppId();
        String parseServerUrl = Constants2.getParseServerUrl();
        if (!parseAppId.isEmpty() && !parseServerUrl.isEmpty()) {
            Parse.enableLocalDatastore(this);
            Parse.initialize(new Parse.Configuration.Builder(this)
                    .applicationId(parseAppId)
                    .clientKey(null)
                    .server(parseServerUrl)
                    .build());
        } else {
            Log.i(TAG, "Parse initialization skipped because credentials are not configured.");
        }
    }
}
