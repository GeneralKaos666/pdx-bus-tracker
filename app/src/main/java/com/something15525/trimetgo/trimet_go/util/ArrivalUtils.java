package com.something15525.trimetgo.trimet_go.util;

import android.app.Activity;
import android.content.Intent;
import com.something15525.trimetgo.trimet_go.activities.DisplayArrivals;
import com.something15525.trimetgo.trimet_go.data.local.DatabaseHelper;
import com.something15525.trimetgo.trimet_go.data.model.Stop;

public class ArrivalUtils {
    public static Intent a(Activity activity, Stop stop, boolean z, int i) {
        if (z) {
            new DatabaseHelper(activity.getApplicationContext()).addRecentStop(stop);
        }
        Intent intent = new Intent(activity, (Class<?>) DisplayArrivals.class);
        intent.putExtra("EXTRA_STOP_DESCRIPTION", stop.getDesc());
        intent.putExtra("EXTRA_STOP_ROUTE_DIRECTION_DESCRIPTION", stop.getDirDesc());
        intent.putExtra("EXTRA_STOP_LONGITUDE", stop.getLongitude());
        intent.putExtra("EXTRA_STOP_LATITUDE", stop.getLatitude());
        intent.putExtra("EXTRA_STOP_TRANSIT_TYPE", stop.getTransitType());
        intent.putExtra("EXTRA_STOP_LOC_ID", stop.getLocId());
        if (i != -1) {
            intent.putExtra("EXTRA_STOP_ROUTE_TO_SHOW", i);
        }
        return intent;
    }
}
