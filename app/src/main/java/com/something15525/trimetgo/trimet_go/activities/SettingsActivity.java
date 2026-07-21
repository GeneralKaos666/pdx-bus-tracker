package com.something15525.trimetgo.trimet_go.activities;

import android.content.pm.PackageManager;
import android.content.res.Configuration;
import android.graphics.Color;
import android.os.Build;
import android.os.Bundle;
import android.preference.ListPreference;
import android.preference.Preference;
import android.util.Log;
import android.util.TypedValue;
import android.view.LayoutInflater;
import android.view.MenuItem;
import android.view.ViewGroup;
import android.widget.ListView;
import androidx.appcompat.app.AppCompatDelegate;
import androidx.appcompat.widget.Toolbar;
import androidx.core.view.ViewCompat;
import com.something15525.trimetgo.trimet_go.activities.AppCompatPreferenceActivity;

public class SettingsActivity extends AppCompatPreferenceActivity {
    private static final String h = SettingsActivity.class.getSimpleName();

        private Preference f4766d;

        private String f4767e;

    private void b() {
        Toolbar toolbar;
        if (Build.VERSION.SDK_INT >= 14) {
            ViewGroup viewGroup = (ViewGroup) findViewById(android.R.id.list).getParent().getParent().getParent();
            toolbar = (Toolbar) LayoutInflater.from(this).inflate(com.something15525.trimetgo.trimet_go.R.layout.toolbar, viewGroup, false);
            viewGroup.addView(toolbar, 0);
        } else {
            ViewGroup viewGroup2 = (ViewGroup) findViewById(android.R.id.content);
            ListView listView = (ListView) viewGroup2.getChildAt(0);
            viewGroup2.removeAllViews();
            toolbar = (Toolbar) LayoutInflater.from(this).inflate(com.something15525.trimetgo.trimet_go.R.layout.toolbar, viewGroup2, false);
            TypedValue typedValue = new TypedValue();
            listView.setPadding(0, getTheme().resolveAttribute(androidx.appcompat.R.attr.actionBarSize, typedValue, true) ? TypedValue.complexToDimensionPixelSize(typedValue.data, getResources().getDisplayMetrics()) : toolbar.getHeight(), 0, 0);
            viewGroup2.addView(listView);
            viewGroup2.addView(toolbar);
        }
        setSupportActionBar(toolbar);
        getSupportActionBar().setDisplayHomeAsUpEnabled(true);
    }

    @Override // com.something15525.trimetgo.trimet_go.activities.AppCompatPreferenceActivity, android.preference.PreferenceActivity, android.app.Activity
    public void onCreate(Bundle bundle) {
        // Edge-to-edge: draw behind system bars
        androidx.core.view.WindowCompat.setDecorFitsSystemWindows(getWindow(), false);
        super.onCreate(bundle);
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
        b();
        addPreferencesFromResource(com.something15525.trimetgo.trimet_go.R.xml.preferences);
        this.f4766d = findPreference("pref_key_version_info");
        try {
            this.f4767e = getPackageManager().getPackageInfo(getPackageName(), 0).versionName;
        } catch (PackageManager.NameNotFoundException e2) {
            Log.e(h, e2.getMessage());
        }
        String str = this.f4767e;
        if (str != null) {
            this.f4766d.setSummary(str);
        }
        ListPreference listPreference = (ListPreference) findPreference("pref_key_theme");
        if (listPreference != null) {
            listPreference.setSummary(listPreference.getEntry());
            listPreference.setOnPreferenceChangeListener(new Preference.OnPreferenceChangeListener() {
                @Override
                public boolean onPreferenceChange(Preference preference, Object obj) {
                    String str = (String) obj;
                    ListPreference listPreference2 = (ListPreference) preference;
                    listPreference2.setSummary(listPreference2.getEntries()[listPreference2.findIndexOfValue(str)]);
                    if (str.equals("dark")) {
                        AppCompatDelegate.setDefaultNightMode(AppCompatDelegate.MODE_NIGHT_YES);
                    } else if (str.equals("light")) {
                        AppCompatDelegate.setDefaultNightMode(AppCompatDelegate.MODE_NIGHT_NO);
                    } else {
                        AppCompatDelegate.setDefaultNightMode(AppCompatDelegate.MODE_NIGHT_FOLLOW_SYSTEM);
                    }
                    recreate();
                    return true;
                }
            });
        }
    }

    @Override // android.preference.PreferenceActivity, android.app.Activity
    public boolean onOptionsItemSelected(MenuItem menuItem) {
        if (menuItem.getItemId() != 16908332) {
            return super.onOptionsItemSelected(menuItem);
        }
        finish();
        return true;
    }

}
