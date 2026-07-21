package com.something15525.trimetgo.trimet_go.activities;

import android.app.Activity;
import android.content.Intent;
import android.content.res.Configuration;
import android.graphics.Color;
import android.os.Build;
import android.os.Bundle;
import android.text.TextUtils;
import android.view.LayoutInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.ListView;
import android.widget.RelativeLayout;
import android.widget.TextView;

import androidx.appcompat.app.ActionBar;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowCompat;
import androidx.core.view.WindowInsetsCompat;

import com.something15525.trimetgo.trimet_go.R;
import com.something15525.trimetgo.trimet_go.data.local.DatabaseHelper;
import com.something15525.trimetgo.trimet_go.data.model.Route;
import com.something15525.trimetgo.trimet_go.data.model.Stop;
import com.something15525.trimetgo.trimet_go.network.JSONParser;
import com.something15525.trimetgo.trimet_go.util.ArrivalUtils;
import com.something15525.trimetgo.trimet_go.util.ConnectionUtils;
import com.something15525.trimetgo.trimet_go.util.Constants2;

import org.json.JSONException;
import org.json.JSONArray;
import org.json.JSONObject;

import java.util.ArrayList;
import java.util.Locale;

public class SearchStops extends AppCompatActivity {

    private Toolbar actionBarToolbar;
    private Activity f4759f;
    private TextView mNoConnectionTextView;
    private TextView mNoResultsTextView;
    private LinearLayout mProgressBar;
    private ListView mStopResultsListView;
    private RelativeLayout rootView;

    // Search is now powered by TriMet API route config instead of dead Parse backend
    private ArrayList<Stop> allStopsCache = null;
    private JSONParser jsonParser = new JSONParser();

    @Override
    protected void onCreate(Bundle bundle) {
        WindowCompat.setDecorFitsSystemWindows(getWindow(), false);
        super.onCreate(bundle);
        setContentView(R.layout.activity_searchstops);
        if (Build.VERSION.SDK_INT < 35) {
            getWindow().setStatusBarColor(Color.TRANSPARENT);
            getWindow().setNavigationBarColor(Color.TRANSPARENT);
        }
        int nightMode = getResources().getConfiguration().uiMode & Configuration.UI_MODE_NIGHT_MASK;
        boolean isDark = nightMode == Configuration.UI_MODE_NIGHT_YES;
        ViewCompat.getWindowInsetsController(getWindow().getDecorView())
                .setAppearanceLightStatusBars(!isDark);
        ViewCompat.getWindowInsetsController(getWindow().getDecorView())
                .setAppearanceLightNavigationBars(!isDark);

        this.actionBarToolbar = findViewById(R.id.act_search_stops_toolbar);
        setSupportActionBar(this.actionBarToolbar);
        this.f4759f = this;

        this.mStopResultsListView = findViewById(R.id.act_search_stops_list_view);
        this.mProgressBar = findViewById(R.id.act_search_stops_loading);
        this.mNoResultsTextView = findViewById(R.id.act_search_stops_no_results);
        this.mNoConnectionTextView = findViewById(R.id.act_search_stops_no_connection);
        this.rootView = findViewById(R.id.act_search_stops_root_view);

        // Apply top window insets so toolbar clears status bar area
        ViewCompat.setOnApplyWindowInsetsListener(this.rootView, (v, insets) -> {
            int topInset = insets.getInsets(WindowInsetsCompat.Type.systemBars()).top;
            v.setPadding(v.getPaddingLeft(), topInset, v.getPaddingRight(), v.getPaddingBottom());
            return WindowInsetsCompat.CONSUMED;
        });

        this.mStopResultsListView.setOnItemClickListener(new AdapterView.OnItemClickListener() {
            @Override
            public void onItemClick(AdapterView<?> adapterView, View view, int i, long j) {
                Stop stop = (Stop) adapterView.getItemAtPosition(i);
                if (stop != null) {
                    startActivity(ArrivalUtils.a(f4759f, stop, true, -1));
                } else {
                    new androidx.appcompat.app.AlertDialog.Builder(SearchStops.this)
                            .setMessage("Error retrieving stop information.").show();
                }
            }
        });

        this.mStopResultsListView.setOnItemLongClickListener(new AdapterView.OnItemLongClickListener() {
            @Override
            public boolean onItemLongClick(AdapterView<?> adapterView, View view, int i, long j) {
                Stop stop = (Stop) adapterView.getItemAtPosition(i);
                if (stop != null) {
                    DatabaseHelper db = new DatabaseHelper(getApplicationContext());
                    db.addFavorite(stop, rootView);
                    return true;
                }
                new androidx.appcompat.app.AlertDialog.Builder(SearchStops.this)
                        .setMessage("Error retrieving stop information.").show();
                return true;
            }
        });

        ActionBar actionBar = getSupportActionBar();
        if (actionBar != null) {
            actionBar.setDisplayHomeAsUpEnabled(true);
            actionBar.setHomeButtonEnabled(true);
        }
    }

    @Override
    protected void onNewIntent(Intent intent) {
        super.onNewIntent(intent);
        searchStops(intent);
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem menuItem) {
        if (menuItem.getItemId() != 16908332) {
            return super.onOptionsItemSelected(menuItem);
        }
        finish();
        return true;
    }

    @Override
    protected void onStart() {
        super.onStart();
        searchStops(getIntent());
    }

    private void searchStops(Intent intent) {
        if (!"android.intent.action.SEARCH".equals(intent.getAction())) {
            return;
        }
        if (!ConnectionUtils.isOnline(this)) {
            this.mProgressBar.setVisibility(View.GONE);
            this.mNoConnectionTextView.setVisibility(View.VISIBLE);
            return;
        }

        final String query = intent.getStringExtra("query");
        if (query == null || query.trim().isEmpty()) {
            this.mProgressBar.setVisibility(View.GONE);
            this.mNoResultsTextView.setVisibility(View.VISIBLE);
            return;
        }

            this.mProgressBar.setVisibility(View.VISIBLE);
            this.mNoResultsTextView.setVisibility(View.GONE);
            this.mNoConnectionTextView.setVisibility(View.GONE);
            this.mStopResultsListView.setVisibility(View.GONE);

        // Search in background thread
        new Thread(new Runnable() {
            @Override
            public void run() {
                final ArrayList<Stop> results = doSearch(query.trim());
                runOnUiThread(new Runnable() {
                    @Override
                    public void run() {
                        mProgressBar.setVisibility(View.GONE);
                        if (results == null) {
                            mNoConnectionTextView.setVisibility(View.VISIBLE);
                        } else if (results.isEmpty()) {
                            mNoResultsTextView.setVisibility(View.VISIBLE);
                        } else {
                            mStopResultsListView.setAdapter(new StopSearchAdapter(results));
                            mStopResultsListView.setVisibility(View.VISIBLE);
                        }
                    }
                });
            }
        }).start();
    }

    private ArrayList<Stop> doSearch(String query) {
        try {
            // Fetch route config from TriMet API if not cached
            if (allStopsCache == null) {
                loadAllStops();
            }
            if (allStopsCache == null) {
                return null; // API error
            }

            String queryLower = query.toLowerCase(Locale.US);
            ArrayList<Stop> results = new ArrayList<>();

            // Try to parse query as stop ID
            Integer queryId = null;
            try {
                queryId = Integer.parseInt(query);
            } catch (NumberFormatException ignored) {}

            for (Stop stop : allStopsCache) {
                String desc = stop.getDesc();  // stop description/name
                if (desc != null && desc.toLowerCase(Locale.US).contains(queryLower)) {
                    results.add(stop);
                } else if (queryId != null && stop.getLocId() == queryId) {
                    results.add(stop);
                }
            }
            return results;
        } catch (Exception e) {
            e.printStackTrace();
            return null;
        }
    }

    private void loadAllStops() throws Exception {
        String url = getString(R.string.base_route_url) +
                "/dir/true/stops/true/appID/" + Constants2.TRIMET_API_KEY;
        JSONObject json = jsonParser.fetch(url);
        if (json == null) return;

        JSONArray routes = json.getJSONObject("resultSet").getJSONArray("route");
        allStopsCache = new ArrayList<>();

        for (int i = 0; i < routes.length(); i++) {
            JSONObject routeObj = routes.getJSONObject(i);
            String routeDesc = routeObj.optString("desc", "");
            int routeId = routeObj.optInt("route", 0);
            String routeType = routeObj.optString("type", "");

            JSONArray dirs = routeObj.optJSONArray("dir");
            if (dirs == null) continue;

            for (int j = 0; j < dirs.length(); j++) {
                JSONObject dirObj = dirs.getJSONObject(j);
                JSONArray stops = dirObj.optJSONArray("stop");
                if (stops == null) continue;

                for (int k = 0; k < stops.length(); k++) {
                    JSONObject stopObj = stops.getJSONObject(k);
                    int locid = stopObj.optInt("locid", 0);

                    // Deduplicate by stop ID
                    boolean found = false;
                    for (Stop existing : allStopsCache) {
                        if (existing.getLocId() == locid) {
                            found = true;
                            break;
                        }
                    }
                    if (found) continue;

                    Stop stop = new Stop();
                    stop.setDesc(stopObj.optString("desc", ""));
                    stop.setDirDesc(stopObj.optString("dir", ""));
                    stop.setLatitude(stopObj.optDouble("lat", 0));
                    stop.setLongitude(stopObj.optDouble("lng", 0));
                    stop.setLocId(locid);

                    Route route = new Route();
                    route.setStreetcarType(routeDesc, routeType);
                    route.setDesc(routeDesc);
                    route.setRouteId(routeId);
                    route.setType(routeType, routeDesc);
                    stop.addRoute(route);
                    stop.computeTransitType();

                    allStopsCache.add(stop);
                }
            }
        }
    }

    // Simple adapter reusing the stop_item layout
    private class StopSearchAdapter extends ArrayAdapter<Stop> {
        StopSearchAdapter(ArrayList<Stop> stops) {
            super(SearchStops.this, R.layout.stop_item, stops);
        }

        @Override
        public View getView(int position, View convertView, ViewGroup parent) {
            View view;
            ViewHolder holder;

            if (convertView == null) {
                view = LayoutInflater.from(getContext()).inflate(R.layout.stop_item, parent, false);
                holder = new ViewHolder();
                holder.circleBackground = view.findViewById(R.id.circle_background);
                holder.circleIconText = view.findViewById(R.id.circle_icon_text);
                holder.circleIcon = view.findViewById(R.id.circle_icon);
                holder.stopRouteName = view.findViewById(R.id.stop_route_name);
                holder.stopLocationId = view.findViewById(R.id.stop_location_id);
                holder.stopDirectionDesc = view.findViewById(R.id.stop_direction_description);
                view.setTag(holder);
            } else {
                view = convertView;
                holder = (ViewHolder) view.getTag();
            }

            Stop stop = getItem(position);
            if (stop == null) return view;

            StringBuilder sb = new StringBuilder();
            sb.append(stop.getDirDesc());
            sb.append(" - ");

            ArrayList<String> routeNames = new ArrayList<>();
            for (int i = 0; i < stop.getRoutes().size(); i++) {
                switch (stop.getRoutes().get(i).getRouteId()) {
                    case 90:
                        routeNames.add(getContext().getString(R.string.red_line_text));
                        break;
                    case 100:
                        routeNames.add(getContext().getString(R.string.blue_line_text));
                        break;
                    case 190:
                        routeNames.add(getContext().getString(R.string.yellow_line_text));
                        break;
                    case 193:
                        routeNames.add(getContext().getString(R.string.ns_line_text));
                        break;
                    case 194:
                        routeNames.add(getContext().getString(R.string.a_loop_text));
                        break;
                    case 195:
                        routeNames.add(getContext().getString(R.string.b_loop_text));
                        break;
                    case 200:
                        routeNames.add(getContext().getString(R.string.green_line_text));
                        break;
                    case 203:
                        routeNames.add(getContext().getString(R.string.wes_text));
                        break;
                    case 290:
                        routeNames.add(getContext().getString(R.string.orange_line_text));
                        break;
                }
            }
            if (!routeNames.isEmpty()) {
                sb.append(TextUtils.join(", ", routeNames));
            }

            String type = stop.getTransitType();
            if (type != null) {
                int icon = android.R.color.transparent;
                if ("B".equals(type) || "T".equals(type)) {
                    icon = R.drawable.ic_directions_bus_white_36dp;
                } else if ("R".equals(type)) {
                    icon = R.drawable.ic_train_white_36dp;
                } else if (type.equals("W")) {
                    icon = R.drawable.ic_train_white_36dp;
                }
                holder.circleIcon.setImageResource(icon);
                holder.circleIcon.setVisibility(View.VISIBLE);
                holder.circleIconText.setVisibility(View.GONE);
            }

            holder.stopRouteName.setText(stop.getDesc());
            holder.stopLocationId.setText(getContext().getString(R.string.stop_id_prefix) + " " + stop.getLocId());
            holder.stopDirectionDesc.setText(sb.toString());
            return view;
        }

        private class ViewHolder {
            ImageView circleBackground;
            TextView circleIconText;
            ImageView circleIcon;
            TextView stopRouteName;
            TextView stopLocationId;
            TextView stopDirectionDesc;
        }
    }
}
