package com.something15525.trimetgo.trimet_go.task;

import android.content.Context;
import android.os.AsyncTask;
import android.util.Log;
import com.something15525.trimetgo.trimet_go.data.model.Arrival;
import com.something15525.trimetgo.trimet_go.data.model.ArrivalsResult;
import com.something15525.trimetgo.trimet_go.data.model.Detour;
import com.something15525.trimetgo.trimet_go.network.JSONParser;
import com.something15525.trimetgo.trimet_go.util.ConnectionUtils;
import java.util.ArrayList;
import org.joda.time.DateTime;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

public abstract class GetArrivalDataAsync extends AsyncTask<String, Void, ArrivalsResult> {

    private Context f4721a;

    protected GetArrivalDataAsync(Context context) {
        this.f4721a = context;
    }

    @Override // android.os.AsyncTask
    public ArrivalsResult doInBackground(String... strArr) {
        ArrivalsResult arrivalsResult = new ArrivalsResult();
        if (ConnectionUtils.isOnline(this.f4721a)) {
            try {
                boolean z = false;
                JSONObject fetchedJson = new JSONParser().fetch(strArr[0]);
                if (fetchedJson == null || !fetchedJson.has("resultSet")) {
                    arrivalsResult.setQueryError(true);
                    return arrivalsResult;
                }
                JSONObject jSONObject = fetchedJson.getJSONObject("resultSet");
                ArrayList<Detour> arrayList = new ArrayList<>();
                try {
                    JSONArray jSONArray = jSONObject.getJSONArray("detour");
                    for (int i = 0; i < jSONArray.length(); i++) {
                        JSONObject jSONObject2 = jSONArray.getJSONObject(i);
                        Detour detour = new Detour();
                        JSONArray jSONArray2 = jSONObject2.getJSONArray("route");
                        int[] iArr = new int[jSONArray2.length()];
                        for (int i2 = 0; i2 < jSONArray2.length(); i2++) {
                            iArr[i2] = jSONArray2.getJSONObject(i2).getInt("route");
                        }
                        detour.setId(Integer.parseInt(jSONObject2.getString("id")));
                        detour.setDesc(jSONObject2.getString("desc"));
                        detour.setRoutes(iArr);
                        arrayList.add(detour);
                    }
                } catch (Exception e) {
                    Log.e("GetArrivalDataAsync", "Failed to parse detour JSON", e);
                }
                ArrayList arrayList2 = new ArrayList();
                try {
                    JSONArray jSONArray3 = jSONObject.getJSONArray("arrival");
                    for (int i3 = 0; i3 < jSONArray3.length(); i3++) {
                        JSONObject jSONObject3 = jSONArray3.getJSONObject(i3);
                        Arrival arrival = new Arrival();
                        arrival.setStatus(jSONObject3.getString("status"));
                        arrival.setFullSign(jSONObject3.getString("fullSign"));
                        arrival.setShortSign(jSONObject3.getString("shortSign"));
                        arrival.setRouteId(jSONObject3.getInt("route"));
                        try {
                            if (arrival.getStatus().equals("estimated")) {
                                arrival.setEstimated(new DateTime(jSONObject3.getLong("estimated")));
                            }
                            arrival.setScheduled(new DateTime(jSONObject3.getLong("scheduled")));
                        } catch (IllegalArgumentException | UnsupportedOperationException e2) {
                            Log.e("GetArrivalDataAsync", "Failed to parse arrival time", e2);
                        }
                        arrival.setDetours(arrayList);
                        arrayList2.add(arrival);
                    }
                } catch (JSONException e3) {
                    Log.e("GetArrivalDataAsync", "Failed to parse arrivals JSON", e3);
                    z = true;
                }
                arrivalsResult.setQueryError(z);
                arrivalsResult.setArrivals(arrayList2);
            } catch (Exception e) {
                Log.e("GetArrivalDataAsync", "Failed to parse result set", e);
            }
        }
        return arrivalsResult;
    }

    @Override // android.os.AsyncTask
    public abstract void onPostExecute(ArrivalsResult arrivalsResult);
}
