package com.trimettransit.tracker.task;

import android.content.Context;
import android.util.Log;
import android.os.AsyncTask;
import com.trimettransit.tracker.data.model.Stop;
import com.trimettransit.tracker.network.JSONParser;
import com.trimettransit.tracker.util.ConnectionUtils;
import org.json.JSONObject;

public abstract class GetQRArrivalDataAsync extends AsyncTask<String, Void, Stop> {

        private final Context context;

    public GetQRArrivalDataAsync(Context context) {
        this.context = context;
    }

        @Override // android.os.AsyncTask
        public Stop doInBackground(String... strArr) {
        Stop stop = new Stop();
        if (ConnectionUtils.isOnline(this.context)) {
            try {
                if (strArr == null || strArr.length == 0) return stop;
                JSONObject fetchedJson = new JSONParser().fetch(strArr[0]);
                if (fetchedJson == null || !fetchedJson.has("resultSet")) {
                    return stop;
                }
                JSONObject jSONObject = fetchedJson.getJSONObject("resultSet");
                if (jSONObject.has("location")) {
                    JSONObject jSONObject2 = jSONObject.getJSONArray("location").getJSONObject(0);
                    stop.setLatitude(jSONObject2.getDouble("lat"));
                    stop.setLongitude(jSONObject2.getDouble("lng"));
                    stop.setLocId(jSONObject2.getInt("id"));
                    stop.setDesc(jSONObject2.getString("desc"));
                    stop.setDirDesc(jSONObject2.getString("dir"));
                    stop.setTransitType("B");
                }
            } catch (Exception e) {
                Log.e("GetQRArrivalDataAsync", "Failed to fetch stop data from QR", e);
            }
        }
        return stop;
    }
}
