package com.trimettransit.tracker.wear

import android.content.Context
import android.content.pm.PackageManager
import android.os.Build
import com.trimettransit.tracker.R
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.wear.compose.foundation.lazy.TransformingLazyColumn
import androidx.wear.compose.foundation.lazy.rememberTransformingLazyColumnState
import androidx.wear.compose.material3.ListHeader
import androidx.wear.compose.material3.ScreenScaffold
import androidx.wear.compose.material3.Text

/** Minimal watch About screen — version info + license. No TriMet logo/branding. */
@Composable
fun AboutScreen() {
    val context = LocalContext.current
    val version = remember { versionName(context) }
    val listState = rememberTransformingLazyColumnState()

    ScreenScaffold(scrollState = listState) { contentPadding ->
        WearContentEntrance(modifier = Modifier.fillMaxSize().padding(contentPadding)) {
            TransformingLazyColumn(state = listState, contentPadding = contentPadding) {
                item { ListHeader { Text(stringResource(R.string.about)) } }
                item {
                    Text(
                        text = stringResource(R.string.app_name),
                        style = androidx.wear.compose.material3.MaterialTheme.typography.titleMedium,
                        modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp)
                    )
                }
                item {
                    Text(
                        text = stringResource(R.string.version_format, version),
                        color = androidx.wear.compose.material3.MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp)
                    )
                }
                item {
                    Text(
                        text = stringResource(R.string.about_description),
                        textAlign = TextAlign.Start,
                        color = androidx.wear.compose.material3.MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 4.dp)
                    )
                }
                item {
                    Text(
                        text = stringResource(R.string.mit_license),
                        color = androidx.wear.compose.material3.MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 4.dp)
                    )
                }
            }
        }
    }
}

private fun versionName(context: Context): String {
    val pm = context.packageManager
    return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
        runCatching {
            pm.getPackageInfo(context.packageName, PackageManager.PackageInfoFlags.of(0)).versionName
        }.getOrNull() ?: "0.0.0"
    } else {
        @Suppress("DEPRECATION")
        runCatching {
            pm.getPackageInfo(context.packageName, 0).versionName
        }.getOrNull() ?: "0.0.0"
    }
}
