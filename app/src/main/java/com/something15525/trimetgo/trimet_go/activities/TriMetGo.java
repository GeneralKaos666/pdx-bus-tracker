package com.something15525.trimetgo.trimet_go.activities;

import android.content.Intent;
import android.content.res.Configuration;
import android.os.Build;
import android.os.Bundle;
import android.graphics.Color;
import android.util.Log;
import android.view.MenuInflater;
import android.view.View;
import android.widget.PopupMenu;
import com.google.android.material.floatingactionbutton.FloatingActionButton;
import android.view.MenuItem;
import androidx.appcompat.app.ActionBar;
import androidx.appcompat.app.ActionBarDrawerToggle;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;
import androidx.compose.ui.platform.ComposeView;
import androidx.core.view.ViewCompat;
import androidx.drawerlayout.widget.DrawerLayout;
import com.something15525.trimetgo.trimet_go.R;
import com.something15525.trimetgo.trimet_go.activities.qr_scanning.QRScanningActivity;
import com.something15525.trimetgo.trimet_go.util.AppRater;
import com.something15525.trimetgo.trimet_go.ui.ComposeBridge;

public class TriMetGo extends AppCompatActivity {

    private CharSequence f4770e;
    private DrawerLayout drawerLayout;
    private ActionBarDrawerToggle drawerToggle;

    public void f() {
        ActionBar actionBarC = getSupportActionBar();
        if (actionBarC != null) {
            actionBarC.setDisplayShowTitleEnabled(true);
            actionBarC.setTitle(this.f4770e);
        }
    }

    @Override
    protected void onActivityResult(int i, int i2, Intent intent) {
        super.onActivityResult(i, i2, intent);
        if (i == 9000) {
            if (i2 != -1) {
                Log.d("LocationSample", getString(R.string.no_resolution));
            } else {
                Log.d("LocationSample", getString(R.string.resolved));
            }
        }
        Log.d("LocationSample", getString(R.string.unknown_activity_request_code, new Object[]{Integer.valueOf(i)}));
    }

    @Override
    protected void onCreate(Bundle bundle) {
        androidx.core.view.WindowCompat.setDecorFitsSystemWindows(getWindow(), false);
        super.onCreate(bundle);
        setContentView(R.layout.activity_tri_met_go);

        // Transparent system bars (API < 35 handled automatically on 35+)
        if (Build.VERSION.SDK_INT < 35) {
            getWindow().setStatusBarColor(Color.TRANSPARENT);
            getWindow().setNavigationBarColor(Color.TRANSPARENT);
        }
        // System bar icon appearance matches theme
        int nightMode = getResources().getConfiguration().uiMode & Configuration.UI_MODE_NIGHT_MASK;
        boolean isDark = nightMode == Configuration.UI_MODE_NIGHT_YES;
        ViewCompat.getWindowInsetsController(getWindow().getDecorView())
                .setAppearanceLightStatusBars(!isDark);
        ViewCompat.getWindowInsetsController(getWindow().getDecorView())
                .setAppearanceLightNavigationBars(!isDark);

        setSupportActionBar((Toolbar) findViewById(R.id.action_bar_toolbar));

        drawerLayout = findViewById(R.id.drawer_layout);
        drawerToggle = new ActionBarDrawerToggle(this, drawerLayout,
                R.string.navigation_drawer_open, R.string.navigation_drawer_close);
        drawerLayout.addDrawerListener(drawerToggle);
        ActionBar actionBar = getSupportActionBar();
        if (actionBar != null) {
            actionBar.setDisplayHomeAsUpEnabled(true);
            actionBar.setHomeButtonEnabled(true);
        }

        ComposeView drawerComposeView = findViewById(R.id.drawer_compose_view);
        ComposeBridge.INSTANCE.setupDrawerContent(drawerComposeView, () -> drawerLayout.closeDrawers());

        this.f4770e = getTitle();
        AppRater.a(this);

        FloatingActionButton fab = findViewById(R.id.fab_actions);
        fab.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                PopupMenu popup = new PopupMenu(TriMetGo.this, view);
                popup.getMenuInflater().inflate(R.menu.fab_actions, popup.getMenu());
                popup.setOnMenuItemClickListener(new PopupMenu.OnMenuItemClickListener() {
                    @Override
                    public boolean onMenuItemClick(MenuItem item) {
                        int id = item.getItemId();
                        if (id == R.id.action_search_stops) {
                            startActivity(new Intent(TriMetGo.this, SearchStops.class));
                            return true;
                        } else if (id == R.id.action_scan_qr) {
                            startActivity(new Intent(TriMetGo.this, QRScanningActivity.class));
                            return true;
                        }
                        return false;
                    }
                });
                popup.show();
            }
        });
    }

    @Override
    protected void onPostCreate(Bundle savedInstanceState) {
        super.onPostCreate(savedInstanceState);
        if (drawerToggle != null) drawerToggle.syncState();
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        if (drawerToggle != null && drawerToggle.onOptionsItemSelected(item)) {
            return true;
        }
        return super.onOptionsItemSelected(item);
    }
}
