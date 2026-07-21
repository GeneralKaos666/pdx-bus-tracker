package com.something15525.trimetgo.trimet_go.activities;

import android.app.Activity;
import android.content.Context;
import android.content.res.Configuration;
import android.os.Build;
import android.os.Bundle;
import android.os.Handler;
import android.graphics.Color;
import android.text.TextUtils;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.TextView;
import android.widget.Toast;
import androidx.appcompat.app.ActionBar;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;
import androidx.core.view.ViewCompat;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import androidx.swiperefreshlayout.widget.SwipeRefreshLayout;
import butterknife.BindView;
import butterknife.ButterKnife;
import com.google.android.material.snackbar.Snackbar;
import com.something15525.trimetgo.trimet_go.R;
import com.something15525.trimetgo.trimet_go.task.GetArrivalDataAsync;
import com.something15525.trimetgo.trimet_go.adapters.ArrivalAdapter;
import com.something15525.trimetgo.trimet_go.data.local.DatabaseHelper;
import com.something15525.trimetgo.trimet_go.data.model.Arrival;
import com.something15525.trimetgo.trimet_go.data.model.ArrivalsResult;
import com.something15525.trimetgo.trimet_go.data.model.Detour;
import com.something15525.trimetgo.trimet_go.data.model.Stop;
import com.something15525.trimetgo.trimet_go.util.Constants2;
import com.something15525.trimetgo.trimet_go.util.DateUtils;
import com.something15525.trimetgo.trimet_go.util.SecurityUtils;
import com.something15525.trimetgo.trimet_go.util.TicketingUtils;
import com.something15525.trimetgo.trimet_go.fragments.ServiceAlertDialogFragment;
import com.something15525.trimetgo.trimet_go.widget.FixedSwipeRefreshLayout;
import java.util.ArrayList;
import java.util.List;
import org.joda.time.DateTime;

public class DisplayArrivals extends AppCompatActivity {

    @BindView(R.id.act_display_arrivals_toolbar)
    protected Toolbar actionBarToolbar;

        private String f4743e;

        private String f4744f;
    private int g;
    private String h;
    private String i;
    private String j;
    private int k;
    private ArrivalAdapter l;
    private List<Arrival> m;

    @BindView(R.id.act_display_arrivals_list_view)
    protected RecyclerView mArrivalRecyclerView;

    @BindView(R.id.act_display_arrivals_last_refreshed)
    protected TextView mLastRefreshedText;

    @BindView(R.id.act_display_arrivals_ptr)
    protected FixedSwipeRefreshLayout mSwipeRefreshLayout;
    private ServiceAlertDialogFragment p;
    private MenuItem r;

    @BindView(R.id.act_display_arrivals_coordinator)
    protected View rootView;
    private double s;

    @BindView(R.id.act_display_arrivals_alert_bar)
    protected View serviceAlertsButton;

    private double t;

    private Snackbar u;
    private boolean v = true;
    private DatabaseHelper w;

    class RefreshListener implements SwipeRefreshLayout.OnRefreshListener {
        RefreshListener() {
        }

        @Override
        public void onRefresh() {
            DisplayArrivals.this.a(false);
        }
    }

    class AlertClickListener implements View.OnClickListener {
        AlertClickListener() {
        }

        @Override // android.view.View.OnClickListener
        public void onClick(View view) {
            if (DisplayArrivals.this.p.isAdded()) {
                return;
            }
            DisplayArrivals.this.p.show(DisplayArrivals.this.getSupportFragmentManager(), "ServiceAlertDialogFragment");
        }
    }

    class ArrivalLoader extends GetArrivalDataAsync {

                final /* synthetic */ boolean f4751b;

        class ScrollToTopRunnable implements Runnable {
            ScrollToTopRunnable() {
            }

            @Override // java.lang.Runnable
            public void run() {
                if (DisplayArrivals.this.serviceAlertsButton.getVisibility() == View.GONE) {
                    DisplayArrivals.this.serviceAlertsButton.setVisibility(View.VISIBLE);
                }
            }
        }

        class FavoriteClickListener implements View.OnClickListener {
            FavoriteClickListener() {
            }

            @Override // android.view.View.OnClickListener
            public void onClick(View view) {
                DisplayArrivals.this.a(true);
            }
        }

                class ViewOnClickListenerC0061d implements View.OnClickListener {
            ViewOnClickListenerC0061d() {
            }

            @Override // android.view.View.OnClickListener
            public void onClick(View view) {
                DisplayArrivals.this.a(true);
            }
        }

        /* JADX WARN: 'super' call moved to the top of the method (can break code semantics) */
        ArrivalLoader(Context context, boolean z) {
            super(context);
            this.f4751b = z;
        }

                @Override // com.something15525.trimetgo.trimet_go.task.GetArrivalDataAsync, android.os.AsyncTask
                public void onPostExecute(ArrivalsResult arrivalsResult) {
            if (arrivalsResult != null && arrivalsResult.getArrivals() != null) {
                DisplayArrivals.this.m = arrivalsResult.getArrivals();
                DateTime dateTime = new DateTime();
                DisplayArrivals.this.mLastRefreshedText.setText(DisplayArrivals.this.getString(R.string.last_refreshed_text) + DateUtils.a(dateTime, DisplayArrivals.this.getApplicationContext()));
                if (DisplayArrivals.this.l == null) {
                    DisplayArrivals displayArrivals = DisplayArrivals.this;
                    displayArrivals.l = new ArrivalAdapter(displayArrivals, displayArrivals.m, this.f4751b, DisplayArrivals.this.k);
                } else {
                    DisplayArrivals.this.l.a(DisplayArrivals.this.m);
                }
                DisplayArrivals displayArrivals2 = DisplayArrivals.this;
                displayArrivals2.mArrivalRecyclerView.setAdapter(displayArrivals2.l);
                if (DisplayArrivals.this.m.size() != 0) {
                    DisplayArrivals.this.mArrivalRecyclerView.setVisibility(View.VISIBLE);
                    if (DisplayArrivals.this.u != null) {
                        DisplayArrivals.this.u.dismiss();
                    }
                    Arrival arrival = (Arrival) DisplayArrivals.this.m.get(0);
                    if (arrival.getDetours().size() > 0) {
                        ArrayList<Detour> arrayListA = arrival.getDetours();
                        DisplayArrivals.this.p = new ServiceAlertDialogFragment();
                        Bundle bundle = new Bundle();
                        bundle.putParcelableArrayList("ExtraDetoursListData", arrayListA);
                        DisplayArrivals.this.p.setArguments(bundle);
                        new Handler().postDelayed(new ScrollToTopRunnable(), 700L);
                    } else if (DisplayArrivals.this.serviceAlertsButton.getVisibility() == View.VISIBLE) {
                        DisplayArrivals.this.serviceAlertsButton.setVisibility(View.GONE);
                    }
                } else {
                    DisplayArrivals displayArrivals3 = DisplayArrivals.this;
                    Snackbar snackbarA = Snackbar.make(displayArrivals3.rootView, R.string.no_upcoming_arrivals_text, -2);
                    snackbarA.setAction(R.string.retry, new FavoriteClickListener());
                    displayArrivals3.u = snackbarA;
                    DisplayArrivals.this.u.show();
                }
            } else if (arrivalsResult != null && !arrivalsResult.isQueryError()) {
                DisplayArrivals displayArrivals4 = DisplayArrivals.this;
                Snackbar snackbarA2 = Snackbar.make(displayArrivals4.rootView, R.string.server_unavailable_text, -2);
                snackbarA2.setAction(R.string.retry, new ViewOnClickListenerC0061d());
                displayArrivals4.u = snackbarA2;
                DisplayArrivals.this.u.show();
            }
            DisplayArrivals.this.mSwipeRefreshLayout.setRefreshing(false);
        }
    }

    private void i() {
        boolean zA = this.w.isFavorite(this.g);
        this.r.setTitle(zA ? getString(R.string.display_arrivals_favorites_button_remove_from_text) : getString(R.string.display_arrivals_favorites_button_add_to_text));
        this.r.setIcon(zA ? R.drawable.ic_star_filled : R.drawable.ic_star_outline);
    }

    @Override // android.support.v7.app.AppCompatActivity, android.support.v4.app.FragmentActivity, android.support.v4.app.ComponentActivity, android.app.Activity
    public void onCreate(Bundle bundle) {
        // Edge-to-edge: draw behind system bars
        androidx.core.view.WindowCompat.setDecorFitsSystemWindows(getWindow(), false);
        super.onCreate(bundle);
        // Transparent system bars (API < 35 handled automatically on 35+)
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
        setContentView(R.layout.activity_displayarrivals);
        ButterKnife.bind(this);
        setSupportActionBar(this.actionBarToolbar);
        this.w = new DatabaseHelper(this);
        this.mSwipeRefreshLayout.setColorSchemeResources(R.color.trimet_blue, R.color.trimet_orange);
        this.mSwipeRefreshLayout.setOnRefreshListener(new RefreshListener());
        this.mArrivalRecyclerView.setHasFixedSize(true);
        LinearLayoutManager linearLayoutManager = new LinearLayoutManager(this);
        linearLayoutManager.setOrientation(RecyclerView.VERTICAL);
        this.mArrivalRecyclerView.setLayoutManager(linearLayoutManager);
        if (!g()) {
            Toast.makeText(this, R.string.server_unavailable_text, Toast.LENGTH_SHORT).show();
            finish();
            return;
        }
        ActionBar actionBarC = getSupportActionBar();
        if (actionBarC != null) {
            actionBarC.setDisplayHomeAsUpEnabled(true);
            actionBarC.setHomeButtonEnabled(true);
            actionBarC.setTitle(this.f4743e);
            actionBarC.setSubtitle(this.f4744f);
        }
        this.serviceAlertsButton.setOnClickListener(new AlertClickListener());
        if (!SecurityUtils.hasConfiguredTrimetApiKey()) {
            Toast.makeText(this, R.string.server_unavailable_text, Toast.LENGTH_SHORT).show();
            finish();
            return;
        }
        this.j = getString(R.string.base_arrival_url) + "/appID/" + Constants2.getTrimetApiKey() + "/locIDs/" + this.g;
        a(true);
    }

    @Override // android.app.Activity
    public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(R.menu.display_arrivals, menu);
        MenuItem menuItemFindItem = menu.findItem(R.id.action_get_tickets);
        menuItemFindItem.setIcon(R.drawable.ic_ticket);
        this.r = menu.findItem(R.id.action_favorite);
        i();
        return true;
    }
    @Override // android.app.Activity
    public boolean onOptionsItemSelected(MenuItem menuItem) {
        switch (menuItem.getItemId()) {
            case android.R.id.home:
                finish();
                return true;
            case R.id.action_favorite:
                if (this.w.isFavorite(this.g)) {
                    this.w.removeFavorite(this.g, this.rootView);
                    i();
                } else {
                    Stop stop = new Stop(this.f4743e, this.h, this.s, this.t, this.i, this.g, null);
                    this.w.addFavorite(stop, this.rootView);
                    i();
                }
                return true;
            case R.id.action_get_tickets:
                TicketingUtils.a(this);
                return true;
            case R.id.action_refresh:
                a(false);
                return true;
            default:
                return super.onOptionsItemSelected(menuItem);
        }
    }

    @Override // android.support.v4.app.FragmentActivity, android.app.Activity
    protected void onResume() {
        super.onResume();
    }

    @Override // android.support.v7.app.AppCompatActivity, android.support.v4.app.FragmentActivity, android.app.Activity
    public void onStart() {
        super.onStart();
        if (this.v) {
            this.v = false;
        } else {
            a(false);
        }
    }

    private boolean g() {
        Bundle extras = getIntent().getExtras();
        if (extras == null) {
            return false;
        }
        this.f4743e = extras.getString("EXTRA_STOP_DESCRIPTION");
        this.h = extras.getString("EXTRA_STOP_ROUTE_DIRECTION_DESCRIPTION");
        this.s = extras.getDouble("EXTRA_STOP_LATITUDE");
        this.t = extras.getDouble("EXTRA_STOP_LONGITUDE");
        this.i = extras.getString("EXTRA_STOP_TRANSIT_TYPE");
        this.g = extras.getInt("EXTRA_STOP_LOC_ID");
        if (this.f4743e != null) {
            this.f4743e = this.f4743e.trim();
        }
        if (this.h != null) {
            this.h = this.h.trim();
        }
        if (this.g <= 0 || TextUtils.isEmpty(this.f4743e) || TextUtils.isEmpty(this.h)) {
            return false;
        }
        this.f4744f = getString(R.string.stop) + " " + this.g + " - " + this.h;
        this.k = extras.getInt("EXTRA_STOP_ROUTE_TO_SHOW", -1);
        return true;
    }

        public void a(boolean z) {
        if (this.j == null || this.j.trim().isEmpty()) {
            this.mSwipeRefreshLayout.setRefreshing(false);
            Snackbar.make(this.rootView, R.string.server_unavailable_text, Snackbar.LENGTH_LONG).show();
            return;
        }
        this.mSwipeRefreshLayout.setRefreshing(true);
        new ArrivalLoader(this, z).execute(this.j);
    }
}
