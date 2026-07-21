package com.something15525.trimetgo.trimet_go.util;

import android.net.ConnectivityManager;
import android.content.Context;
import android.net.NetworkInfo;

public class ConnectionUtils {
    public static boolean isOnline(Context context) {
        NetworkInfo activeNetworkInfo;
        return (context == null || (activeNetworkInfo = ((ConnectivityManager) context.getSystemService(Context.CONNECTIVITY_SERVICE)).getActiveNetworkInfo()) == null || !activeNetworkInfo.isConnected()) ? false : true;
    }

}
