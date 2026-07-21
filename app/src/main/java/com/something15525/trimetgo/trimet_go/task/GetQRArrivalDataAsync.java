package com.something15525.trimetgo.trimet_go.task;

import android.content.Context;
import android.util.Log;
import android.os.AsyncTask;
import com.something15525.trimetgo.trimet_go.data.model.Stop;
import com.something15525.trimetgo.trimet_go.network.JSONParser;
import com.something15525.trimetgo.trimet_go.util.ConnectionUtils;
import org.json.JSONObject;

public abstract class GetQRArrivalDataAsync extends AsyncTask<String, Void, Stop> {

        private Context f4725a;

    public GetQRArrivalDataAsync(Context context) {
        this.f4725a = context;
    }

        @Override // android.os.AsyncTask
        public Stop doInBackground(String... strArr) {
        Stop stop = new Stop();
        if (ConnectionUtils.isOnline(this.f4725a)) {
            try {
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
