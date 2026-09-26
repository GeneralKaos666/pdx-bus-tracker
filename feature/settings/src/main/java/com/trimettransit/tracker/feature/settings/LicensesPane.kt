package com.trimettransit.tracker.feature.settings

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.padding
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.trimettransit.tracker.ui.components.SettingsCard
/** Licenses pane: static third-party license entries. */
@Composable
internal fun LicensesPane()
{
    SettingsCard {
            Column(
                modifier = Modifier.padding(start = 16.dp, end = 16.dp, bottom = 16.dp)
            ) {
                LicenseEntry(stringResource(R.string.license_androidx), stringResource(R.string.license_apache_2))
                LicenseEntry(stringResource(R.string.license_kotlin), stringResource(R.string.license_apache_2))
                LicenseEntry(stringResource(R.string.license_okhttp), stringResource(R.string.license_apache_2))
                LicenseEntry(stringResource(R.string.license_maplibre), stringResource(R.string.license_bsd_2))
                LicenseEntry(stringResource(R.string.license_joda), stringResource(R.string.license_apache_2))
                LicenseEntry(stringResource(R.string.license_timber), stringResource(R.string.license_apache_2))
                LicenseEntry(stringResource(R.string.license_glance), stringResource(R.string.license_apache_2))
                LicenseEntry(stringResource(R.string.license_workmanager), stringResource(R.string.license_apache_2))
                LicenseEntry(stringResource(R.string.license_gtfs_bindings), stringResource(R.string.license_apache_2))
                LicenseEntry(stringResource(R.string.license_materialkolor), stringResource(R.string.license_mit))
                LicenseEntry(stringResource(R.string.license_bricolage), stringResource(R.string.license_ofl))
                LicenseEntry(
                    stringResource(R.string.license_map_data),
                    "",
                    isNote = true
                )
                LicenseEntry(
                    stringResource(R.string.license_full_texts),
                    "",
                    isNote = true
                )
            }
    }
}
