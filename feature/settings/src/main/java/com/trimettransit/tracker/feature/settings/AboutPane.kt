package com.trimettransit.tracker.feature.settings

import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import androidx.compose.foundation.Image
import androidx.compose.foundation.LocalIndication
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.OpenInNew
import androidx.compose.material.icons.filled.Info
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.core.graphics.drawable.toBitmap
import androidx.core.net.toUri
import com.trimettransit.tracker.ui.components.SettingsCard
import com.trimettransit.tracker.ui.components.SettingsIconCircle
import com.trimettransit.tracker.ui.components.pressScale
import com.trimettransit.tracker.ui.theme.appCardShape
/** About pane: app version, disclaimers, attribution, and the privacy-policy row. */
@Composable
internal fun AboutPane(apiKeyConfigured: Boolean)
{
    val context = LocalContext.current
    SettingsCard {
        val appIcon = remember {
            context.packageManager.getApplicationIcon(context.packageName)
                .toBitmap().asImageBitmap()
        }
        val versionName = remember {
            runCatching {
                val info = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                    context.packageManager.getPackageInfo(context.packageName, PackageManager.PackageInfoFlags.of(0))
                } else {
                    context.packageManager.getPackageInfo(context.packageName, 0)
                }
                info.versionName
            }.getOrNull() ?: "0.0.0"
        }
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Surface(
                modifier = Modifier.size(48.dp),
                shape = appCardShape(),
                color = MaterialTheme.colorScheme.surfaceContainerHighest
            ) {
                Box(contentAlignment = Alignment.Center) {
                    Image(
                        bitmap = appIcon,
                        contentDescription = null,
                        modifier = Modifier.size(32.dp)
                    )
                }
            }
            Spacer(modifier = Modifier.width(12.dp))
            Column {
                Text(
                    text = stringResource(R.string.app_title),
                    style = MaterialTheme.typography.titleMedium
                )
                Text(
                    text = stringResource(R.string.version_license, versionName),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
        ApiKeyStatusRow(apiKeyConfigured = apiKeyConfigured)

        Text(
            text = stringResource(R.string.unofficial_disclaimer),
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.padding(start = 16.dp, end = 16.dp, bottom = 4.dp)
        )
        Text(
            text = stringResource(R.string.data_provider),
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.padding(start = 16.dp, end = 16.dp, bottom = 4.dp)
        )
        Text(
            text = stringResource(R.string.trademark_notice),
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.padding(start = 16.dp, end = 16.dp, bottom = 16.dp)
        )

        HorizontalDivider(modifier = Modifier.padding(horizontal = 16.dp))

        val policyInteractionSource = remember { MutableInteractionSource() }
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .pressScale(policyInteractionSource)
                .clickable(
                    interactionSource = policyInteractionSource,
                    indication = LocalIndication.current
                ) {
                    val intent = Intent(
                        Intent.ACTION_VIEW,
                        "https://github.com/GeneralKaos666/pdx-bus-tracker/blob/master/docs/privacy-policy.md".toUri()
                    )
                    context.startActivity(intent)
                }
                .padding(horizontal = 16.dp, vertical = 12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            SettingsIconCircle(icon = Icons.Filled.Info, highlighted = false)
            Spacer(modifier = Modifier.width(16.dp))
            Text(
                text = stringResource(R.string.privacy_policy),
                style = MaterialTheme.typography.bodyLarge,
                modifier = Modifier.weight(1f)
            )
            Icon(
                imageVector = Icons.AutoMirrored.Filled.OpenInNew,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}

/**
 * Read-only status for the TriMet API key, so the "API key not configured" error points at
 * something the user can actually go and look at. Deliberately not interactive: the key is a
 * build-time value in this app, not something that can be set from here.
 */
@Composable
private fun ApiKeyStatusRow(apiKeyConfigured: Boolean) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(start = 16.dp, end = 16.dp, bottom = 12.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = stringResource(R.string.api_key_status_label),
                style = MaterialTheme.typography.bodyLarge
            )
            if (!apiKeyConfigured) {
                Text(
                    text = stringResource(R.string.api_key_status_missing_detail),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
        Spacer(modifier = Modifier.width(12.dp))
        Text(
            text = stringResource(
                if (apiKeyConfigured) R.string.api_key_status_configured
                else R.string.api_key_status_not_configured
            ),
            style = MaterialTheme.typography.labelLarge,
            color = if (apiKeyConfigured) MaterialTheme.colorScheme.onSurfaceVariant
                    else MaterialTheme.colorScheme.error
        )
    }
}
