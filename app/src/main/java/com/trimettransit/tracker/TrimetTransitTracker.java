package com.trimettransit.tracker;

import android.app.Application;

public class TrimetTransitTracker extends Application {
    @Override // android.app.Application
    public void onCreate() {
        super.onCreate();
        com.google.android.material.color.DynamicColors.applyToActivitiesIfAvailable(this);
    }
}
