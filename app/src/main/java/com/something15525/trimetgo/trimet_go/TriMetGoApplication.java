package com.something15525.trimetgo.trimet_go;

import android.app.Application;
import com.parse.Parse;
import com.something15525.trimetgo.trimet_go.util.Constants2;

public class TriMetGoApplication extends Application {

    @Override // android.app.Application
    public void onCreate() {
        super.onCreate();
        com.google.android.material.color.DynamicColors.applyToActivitiesIfAvailable(this);
        Parse.enableLocalDatastore(this);
        Parse.initialize(new Parse.Configuration.Builder(this)
                .applicationId(Constants2.f4891c)
                .clientKey(null)
                .server(Constants2.f4892d)
                .build());
    }
}
