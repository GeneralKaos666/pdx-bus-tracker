package com.trimettransit.tracker.util;

import com.trimettransit.tracker.BuildConfig;

public class Constants2 {

    public static String getTrimetApiKey() {
        return BuildConfig.TRIMET_API_KEY == null ? "" : BuildConfig.TRIMET_API_KEY.trim();
    }

    public static boolean hasTrimetApiKey() {
        return !getTrimetApiKey().isEmpty();
    }
}
