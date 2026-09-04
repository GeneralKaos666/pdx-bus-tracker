package com.trimettransit.tracker.feature.settings

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.animation.expandVertically
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.shrinkVertically
import androidx.compose.foundation.Image
import androidx.compose.foundation.LocalIndication
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.BrightnessAuto
import androidx.compose.material.icons.filled.DarkMode
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.KeyboardArrowDown
import androidx.compose.material.icons.filled.LightMode
import androidx.compose.material.icons.filled.Palette
import androidx.compose.material.icons.filled.Route
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.RadioButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import android.content.pm.PackageManager
import android.os.Build
import androidx.core.content.edit
import androidx.core.graphics.drawable.toBitmap
import androidx.preference.PreferenceManager
import com.trimettransit.tracker.ui.NavState
import com.trimettransit.tracker.ui.components.ContentEntrance
import com.trimettransit.tracker.ui.components.pressScale

import kotlinx.coroutines.launch

@Composable
fun SettingsScreen() {
    val context = LocalContext.current
    val prefs = remember { PreferenceManager.getDefaultSharedPreferences(context) }

    var selectedTheme by remember { mutableStateOf(prefs.getString("theme", "system") ?: "system") }
    var dynamicColor by remember { mutableStateOf(prefs.getBoolean("pref_key_dynamic_color", true)) }
    var onlyShowSelectedRoute by remember {
        mutableStateOf(prefs.getBoolean("pref_key_only_show_route_selected", true))
    }

    val coroutineScope = rememberCoroutineScope()
    val scrollState = rememberScrollState()

    // Collapsed bottom-bar pill: scroll Settings back to the top.
    DisposableEffect(Unit) {
        NavState.onScrollToTop = {
            coroutineScope.launch { scrollState.scrollTo(0) }
        }
        onDispose { NavState.onScrollToTop = null }
    }

    ContentEntrance(modifier = Modifier.fillMaxSize()) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .verticalScroll(scrollState)
        ) {
            SectionHeader(title = stringResource(R.string.section_appearance))

            SettingsCard {
                SettingsRadioOption(
                    label = stringResource(R.string.theme_system),
                    subtitle = stringResource(R.string.theme_system_subtitle),
                    icon = Icons.Filled.BrightnessAuto,
                    selected = selectedTheme == "system",
                    onClick = {
                        selectedTheme = "system"
                        prefs.edit { putString("theme", "system") }
                    }
                )
                SettingsRadioOption(
                    label = stringResource(R.string.theme_light),
                    subtitle = stringResource(R.string.theme_light_subtitle),
                    icon = Icons.Filled.LightMode,
                    selected = selectedTheme == "light",
                    onClick = {
                        selectedTheme = "light"
                        prefs.edit { putString("theme", "light") }
                    }
                )
                SettingsRadioOption(
                    label = stringResource(R.string.theme_dark),
                    subtitle = stringResource(R.string.theme_dark_subtitle),
                    icon = Icons.Filled.DarkMode,
                    selected = selectedTheme == "dark",
                    onClick = {
                        selectedTheme = "dark"
                        prefs.edit { putString("theme", "dark") }
                    }
                )

                HorizontalDivider(modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp))

                SettingsSwitchOption(
                    label = stringResource(R.string.dynamic_colors),
                    subtitle = stringResource(R.string.dynamic_colors_subtitle),
                    icon = Icons.Filled.Palette,
                    checked = dynamicColor,
                    onCheckedChange = {
                        dynamicColor = it
                        prefs.edit { putBoolean("pref_key_dynamic_color", it) }
                    }
                )
            }

            SectionHeader(title = stringResource(R.string.section_arrivals))

            SettingsCard {
                SettingsSwitchOption(
                    label = stringResource(R.string.only_show_selected_route),
                    subtitle = stringResource(R.string.only_show_selected_route_subtitle),
                    icon = Icons.Filled.Route,
                    checked = onlyShowSelectedRoute,
                    onCheckedChange = {
                        onlyShowSelectedRoute = it
                        prefs.edit { putBoolean("pref_key_only_show_route_selected", it) }
                    }
                )
            }

            SectionHeader(title = stringResource(R.string.section_about))

            SettingsCard {
                val appIcon = remember {
                    context.packageManager.getApplicationIcon(context.packageName)
                        .toBitmap().asImageBitmap()
                }
                val versionName = remember {
                    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                        runCatching {
                            context.packageManager
                                .getPackageInfo(context.packageName, PackageManager.PackageInfoFlags.of(0))
                                .versionName
                        }.getOrNull() ?: "0.0.0"
                    } else {
                        runCatching {
                            context.packageManager
                                .getPackageInfo(context.packageName, 0)
                                .versionName
                        }.getOrNull() ?: "0.0.0"
                    }
                }
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp, vertical = 16.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Surface(
                        modifier = Modifier.size(48.dp),
                        shape = MaterialTheme.shapes.medium,
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
                    modifier = Modifier.padding(start = 16.dp, end = 16.dp, bottom = 16.dp)
                )
            }

            SectionHeader(title = stringResource(R.string.open_source_licenses))

            SettingsCard {
                var licensesExpanded by remember { mutableStateOf(false) }
                val licenseInteractionSource = remember { MutableInteractionSource() }
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .pressScale(licenseInteractionSource)
                        .clickable(
                            interactionSource = licenseInteractionSource,
                            indication = LocalIndication.current
                        ) { licensesExpanded = !licensesExpanded }
                        .padding(horizontal = 16.dp, vertical = 12.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    SettingsIconCircle(icon = Icons.Filled.Info, highlighted = licensesExpanded)
                    Spacer(modifier = Modifier.width(16.dp))
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = stringResource(R.string.open_source_licenses),
                            style = MaterialTheme.typography.bodyLarge,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                        Text(
                            text = stringResource(R.string.libraries_terms),
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            maxLines = 2,
                            overflow = TextOverflow.Ellipsis
                        )
                    }
                    Spacer(modifier = Modifier.width(16.dp))
                    val chevronRotation by animateFloatAsState(
                        targetValue = if (licensesExpanded) 180f else 0f,
                        animationSpec = tween(durationMillis = 250, easing = FastOutSlowInEasing),
                        label = "licensesChevron"
                    )
                    Icon(
                        imageVector = Icons.Filled.KeyboardArrowDown,
                        contentDescription = if (licensesExpanded) stringResource(R.string.collapse) else stringResource(R.string.expand),
                        tint = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.rotate(chevronRotation)
                    )
                }
                AnimatedVisibility(
                    visible = licensesExpanded,
                    enter = expandVertically(
                        animationSpec = tween(durationMillis = 250, easing = FastOutSlowInEasing)
                    ) + fadeIn(tween(durationMillis = 250, easing = FastOutSlowInEasing)),
                    exit = shrinkVertically(
                        animationSpec = tween(durationMillis = 180, easing = FastOutSlowInEasing)
                    ) + fadeOut(tween(durationMillis = 180, easing = FastOutSlowInEasing))
                ) {
                    Column(
                        modifier = Modifier.padding(start = 72.dp, end = 16.dp, bottom = 16.dp)
                    ) {
                        LicenseEntry(stringResource(R.string.license_androidx), stringResource(R.string.license_apache_2))
                        LicenseEntry(stringResource(R.string.license_kotlin), stringResource(R.string.license_apache_2))
                        LicenseEntry(stringResource(R.string.license_okhttp), stringResource(R.string.license_apache_2))
                        LicenseEntry(stringResource(R.string.license_maplibre), stringResource(R.string.license_bsd_2))
                        LicenseEntry(stringResource(R.string.license_joda), stringResource(R.string.license_apache_2))
                        LicenseEntry(stringResource(R.string.license_timber), stringResource(R.string.license_apache_2))
                        LicenseEntry(stringResource(R.string.license_wear), stringResource(R.string.license_apache_2))
                        LicenseEntry(stringResource(R.string.license_glance), stringResource(R.string.license_apache_2))
                        LicenseEntry(stringResource(R.string.license_workmanager), stringResource(R.string.license_apache_2))
                        LicenseEntry(
                            stringResource(R.string.license_full_texts),
                            "",
                            isNote = true
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(16.dp))
        }
    }
}

@Composable
private fun SectionHeader(title: String) {
    Text(
        text = title,
        style = MaterialTheme.typography.titleSmall,
        fontWeight = FontWeight.SemiBold,
        color = MaterialTheme.colorScheme.primary,
        modifier = Modifier.padding(start = 16.dp, top = 24.dp, bottom = 8.dp)
    )
}

@Composable
private fun SettingsCard(content: @Composable ColumnScope.() -> Unit) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 4.dp),
        shape = MaterialTheme.shapes.medium,
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceContainerLow
        ),
        elevation = CardDefaults.cardElevation(defaultElevation = 1.dp)
    ) {
        Column(content = content)
    }
}

@Composable
private fun SettingsIconCircle(icon: ImageVector, highlighted: Boolean) {
    val containerColor by animateColorAsState(
        targetValue = if (highlighted) MaterialTheme.colorScheme.primaryContainer
                      else MaterialTheme.colorScheme.surfaceContainerHighest,
        animationSpec = tween(durationMillis = 250, easing = FastOutSlowInEasing),
        label = "settingsIconContainer"
    )
    val contentColor by animateColorAsState(
        targetValue = if (highlighted) MaterialTheme.colorScheme.onPrimaryContainer
                      else MaterialTheme.colorScheme.onSurfaceVariant,
        animationSpec = tween(durationMillis = 250, easing = FastOutSlowInEasing),
        label = "settingsIconContent"
    )
    Surface(
        modifier = Modifier.size(40.dp),
        shape = CircleShape,
        color = containerColor
    ) {
        Box(contentAlignment = Alignment.Center) {
            Icon(
                imageVector = icon,
                contentDescription = null,
                tint = contentColor,
                modifier = Modifier.size(24.dp)
            )
        }
    }
}

@Composable
private fun SettingsRadioOption(
    label: String,
    subtitle: String,
    icon: ImageVector,
    selected: Boolean,
    onClick: () -> Unit
) {
    val interactionSource = remember { MutableInteractionSource() }
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .pressScale(interactionSource)
            .clickable(
                interactionSource = interactionSource,
                indication = LocalIndication.current,
                onClick = onClick
            )
            .padding(horizontal = 16.dp, vertical = 12.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        SettingsIconCircle(icon = icon, highlighted = selected)
        Spacer(modifier = Modifier.width(16.dp))
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = label,
                style = MaterialTheme.typography.bodyLarge,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
            Text(
                text = subtitle,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                maxLines = 2,
                overflow = TextOverflow.Ellipsis
            )
        }
        Spacer(modifier = Modifier.width(16.dp))
        RadioButton(
            selected = selected,
            onClick = null
        )
    }
}

@Composable
private fun SettingsSwitchOption(
    label: String,
    subtitle: String,
    icon: ImageVector,
    checked: Boolean,
    onCheckedChange: (Boolean) -> Unit
) {
    val interactionSource = remember { MutableInteractionSource() }
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .pressScale(interactionSource)
            .clickable(
                interactionSource = interactionSource,
                indication = LocalIndication.current
            ) { onCheckedChange(!checked) }
            .padding(horizontal = 16.dp, vertical = 12.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        SettingsIconCircle(icon = icon, highlighted = false)
        Spacer(modifier = Modifier.width(16.dp))
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = label,
                style = MaterialTheme.typography.bodyLarge,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
            Text(
                text = subtitle,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                maxLines = 2,
                overflow = TextOverflow.Ellipsis
            )
        }
        Spacer(modifier = Modifier.width(16.dp))
        Switch(
            checked = checked,
            onCheckedChange = null
        )
    }
}

@Composable
private fun LicenseEntry(name: String, license: String, isNote: Boolean = false) {
    Column(modifier = Modifier.padding(vertical = 4.dp)) {
        Text(
            text = name,
            style = if (isNote) MaterialTheme.typography.bodySmall else MaterialTheme.typography.bodyMedium,
            color = if (isNote) MaterialTheme.colorScheme.onSurfaceVariant else MaterialTheme.colorScheme.onSurface,
            fontStyle = if (isNote) androidx.compose.ui.text.font.FontStyle.Italic else androidx.compose.ui.text.font.FontStyle.Normal
        )
        if (license.isNotEmpty()) {
            Text(
                text = license,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}
