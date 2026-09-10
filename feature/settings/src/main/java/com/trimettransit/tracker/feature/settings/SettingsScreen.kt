package com.trimettransit.tracker.feature.settings

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.expandVertically
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.shrinkVertically
import androidx.compose.foundation.BorderStroke
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
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.OpenInNew
import androidx.compose.material.icons.filled.BorderAll
import androidx.compose.material.icons.filled.BrightnessAuto
import androidx.compose.material.icons.filled.Colorize
import androidx.compose.material.icons.filled.DarkMode
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.KeyboardArrowDown
import androidx.compose.material.icons.filled.LightMode
import androidx.compose.material.icons.filled.Palette
import androidx.compose.material.icons.filled.Route
import androidx.compose.material.icons.filled.Tune
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Card
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.RadioButton
import androidx.compose.material3.Slider
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.rememberSliderState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import android.content.Intent
import android.content.pm.PackageManager
import android.graphics.Color as AndroidColor
import androidx.core.graphics.toColorInt
import android.os.Build
import androidx.core.content.edit
import androidx.core.net.toUri
import androidx.core.graphics.drawable.toBitmap
import androidx.preference.PreferenceManager
import com.trimettransit.tracker.ui.NavState
import com.trimettransit.tracker.ui.components.ContentEntrance
import com.trimettransit.tracker.ui.components.SectionHeader
import com.trimettransit.tracker.ui.components.SettingsCard
import com.trimettransit.tracker.ui.components.SettingsIconCircle
import com.trimettransit.tracker.ui.components.SettingsRadioOption
import com.trimettransit.tracker.ui.components.SettingsRowOption
import com.trimettransit.tracker.ui.components.SettingsSwitchOption
import com.trimettransit.tracker.ui.components.pressScale
import com.trimettransit.tracker.ui.theme.LocalCardStyle
import com.trimettransit.tracker.ui.theme.appCardBorder
import com.trimettransit.tracker.ui.theme.m3EffectsDefault
import com.trimettransit.tracker.ui.theme.m3EffectsFast
import com.trimettransit.tracker.ui.theme.m3SpatialDefault
import com.trimettransit.tracker.ui.theme.m3SpatialFast

import java.util.Locale

import kotlinx.coroutines.launch
import kotlin.math.roundToInt

@Composable
fun SettingsScreen(
    widgetSection: (@Composable ColumnScope.() -> Unit)? = null
) {
    val context = LocalContext.current
    val prefs = remember { PreferenceManager.getDefaultSharedPreferences(context) }

    var selectedTheme by remember { mutableStateOf(prefs.getString("theme", "system") ?: "system") }
    var dynamicColor by remember { mutableStateOf(prefs.getBoolean("pref_key_dynamic_color", true)) }
    var onlyShowSelectedRoute by remember {
        mutableStateOf(prefs.getBoolean("pref_key_only_show_route_selected", true))
    }
    var cardOutlines by remember {
        mutableStateOf(prefs.getBoolean("pref_key_card_outlines", true))
    }
    var cardOutlineColorRaw by remember {
        mutableStateOf(prefs.getString("pref_key_card_outline_color", "auto") ?: "auto")
    }
    var cornerRadius by remember {
        mutableFloatStateOf(prefs.getInt("pref_key_card_corner_radius", 16).toFloat())
    }
    var showColorPicker by remember { mutableStateOf(false) }

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

                HorizontalDivider(modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp))

                SettingsSwitchOption(
                    label = stringResource(R.string.card_outlines),
                    subtitle = stringResource(R.string.card_outlines_subtitle),
                    icon = Icons.Filled.BorderAll,
                    checked = cardOutlines,
                    onCheckedChange = {
                        cardOutlines = it
                        prefs.edit { putBoolean("pref_key_card_outlines", it) }
                    }
                )
                AnimatedVisibility(
                    visible = cardOutlines,
                    enter = expandVertically(m3SpatialDefault()) + fadeIn(m3EffectsDefault()),
                    exit = shrinkVertically(m3SpatialFast()) + fadeOut(m3EffectsFast())
                ) {
                    SettingsColourOption(
                        label = stringResource(R.string.card_outline_colour),
                        subtitle = stringResource(R.string.card_outline_colour_subtitle),
                        icon = Icons.Filled.Colorize,
                        colour = outlinePreviewColour(cardOutlineColorRaw),
                        onClick = { showColorPicker = true }
                    )
                }
                SettingsSliderOption(
                    label = stringResource(R.string.card_corner_radius),
                    icon = Icons.Filled.Tune,
                    value = cornerRadius,
                    valueLabel = stringResource(R.string.card_corner_radius_dp, cornerRadius.roundToInt()),
                    valueRange = 0f..28f,
                    onValueChange = { cornerRadius = it },
                    onValueChangeFinished = {
                        prefs.edit { putInt("pref_key_card_corner_radius", cornerRadius.roundToInt()) }
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
                        shape = RoundedCornerShape(LocalCardStyle.current.cornerRadius),
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

            // App-owned section (e.g. Widget settings) injected from the host module.
            if (widgetSection != null) {
                widgetSection()
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
                        animationSpec = m3SpatialDefault(),
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
                        animationSpec = m3SpatialDefault()
                    ) + fadeIn(m3EffectsDefault()),
                    exit = shrinkVertically(
                        animationSpec = m3SpatialFast()
                    ) + fadeOut(m3EffectsFast())
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

    if (showColorPicker) {
        CardOutlineColourDialog(
            initial = cardOutlineColorRaw,
            onDismiss = { showColorPicker = false },
            onAuto = {
                cardOutlineColorRaw = "auto"
                prefs.edit { putString("pref_key_card_outline_color", "auto") }
                showColorPicker = false
            },
            onConfirm = { argb ->
                cardOutlineColorRaw = argb
                prefs.edit { putString("pref_key_card_outline_color", argb) }
                showColorPicker = false
            }
        )
    }
}

/** The preview colour of the outline option row, or the scheme's outlineVariant when "auto". */
@Composable
private fun outlinePreviewColour(raw: String): Color =
    parseOutlineColour(raw, MaterialTheme.colorScheme.outlineVariant)

/** Parses a persisted outline-colour pref into a [Color]; "auto" (or an unparseable value) → [fallback]. */
private fun parseOutlineColour(raw: String, fallback: Color): Color {
    if (raw == "auto") return fallback
    return runCatching { Color(raw.toColorInt()) }.getOrElse { fallback }
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

@Composable
private fun SettingsColourOption(
    label: String,
    subtitle: String,
    icon: ImageVector,
    colour: Color,
    onClick: () -> Unit
) {
    SettingsRowOption(
        label = label,
        subtitle = subtitle,
        icon = icon,
        highlighted = false,
        onClick = onClick,
        trailing = {
            Surface(
                modifier = Modifier.size(32.dp),
                shape = RoundedCornerShape(LocalCardStyle.current.cornerRadius),
                color = colour,
                border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant)
            ) {}
        }
    )
}

@Composable
private fun SettingsSliderOption(
    label: String,
    icon: ImageVector,
    value: Float,
    valueLabel: String,
    valueRange: ClosedFloatingPointRange<Float>,
    onValueChange: (Float) -> Unit,
    onValueChangeFinished: () -> Unit
) {
    val sliderState = rememberSliderState(value, 27, valueRange)
    LaunchedEffect(value) { sliderState.value = value }
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 4.dp)
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            SettingsIconCircle(icon = icon, highlighted = false)
            Spacer(modifier = Modifier.width(16.dp))
            Text(
                text = label,
                style = MaterialTheme.typography.bodyLarge,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
                modifier = Modifier.weight(1f)
            )
            Spacer(modifier = Modifier.width(16.dp))
            Text(
                text = valueLabel,
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
        Slider(
            state = sliderState,
            onValueChange = onValueChange,
            onValueChangeFinished = onValueChangeFinished,
            modifier = Modifier
                .fillMaxWidth()
                .padding(start = 56.dp, end = 8.dp)
        )
    }
}

@Composable
private fun CardOutlineColourDialog(
    initial: String,
    onDismiss: () -> Unit,
    onAuto: () -> Unit,
    onConfirm: (String) -> Unit
) {
    val scheme = MaterialTheme.colorScheme
    val fallback = scheme.outlineVariant
    val startColor = parseOutlineColour(initial, fallback)
    val initialHsv = remember(startColor) {
        FloatArray(3).also { AndroidColor.colorToHSV(startColor.toArgb(), it) }
    }
    var isAuto by remember(initial) { mutableStateOf(initial == "auto") }
    var hue by remember(initial) { mutableFloatStateOf(initialHsv[0]) }
    var sat by remember(initial) { mutableFloatStateOf(initialHsv[1]) }
    var value by remember(initial) { mutableFloatStateOf(initialHsv[2]) }
    var alpha by remember(initial) { mutableFloatStateOf(startColor.alpha) }

    val draft = remember(hue, sat, value, alpha) {
        Color(AndroidColor.HSVToColor((alpha * 255).roundToInt(), floatArrayOf(hue, sat, value)))
    }

    fun markCustom() {
        isAuto = false
    }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(stringResource(R.string.card_outline_colour)) },
        text = {
            Column(modifier = Modifier.fillMaxWidth()) {
                val autoSource = remember { MutableInteractionSource() }
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .pressScale(autoSource)
                        .clickable(
                            interactionSource = autoSource,
                            indication = LocalIndication.current
                        ) { isAuto = true }
                        .padding(vertical = 8.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = stringResource(R.string.card_outline_auto),
                        style = MaterialTheme.typography.bodyLarge,
                        modifier = Modifier.weight(1f)
                    )
                    RadioButton(selected = isAuto, onClick = null)
                }
                Surface(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(48.dp),
                    shape = RoundedCornerShape(LocalCardStyle.current.cornerRadius),
                    color = draft,
                    border = BorderStroke(1.dp, scheme.outlineVariant)
                ) {}
                Spacer(modifier = Modifier.height(16.dp))
                ColourSlider(
                    label = stringResource(R.string.color_hue),
                    value = hue,
                    valueRange = 0f..360f,
                    steps = 35,
                    onValueChange = {
                        hue = it
                        markCustom()
                    }
                )
                ColourSlider(
                    label = stringResource(R.string.color_saturation),
                    value = sat,
                    onValueChange = {
                        sat = it
                        markCustom()
                    }
                )
                ColourSlider(
                    label = stringResource(R.string.color_value),
                    value = value,
                    onValueChange = {
                        value = it
                        markCustom()
                    }
                )
                ColourSlider(
                    label = stringResource(R.string.color_opacity),
                    value = alpha,
                    onValueChange = {
                        alpha = it
                        markCustom()
                    }
                )
            }
        },
        confirmButton = {
            TextButton(
                onClick = {
                    if (isAuto) onAuto() else onConfirm(String.format(Locale.US, "#%08X", draft.toArgb()))
                }
            ) {
                Text(stringResource(R.string.done))
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text(stringResource(R.string.cancel))
            }
        }
    )
}

@Composable
private fun ColourSlider(
    label: String,
    value: Float,
    valueRange: ClosedFloatingPointRange<Float> = 0f..1f,
    steps: Int = 0,
    onValueChange: (Float) -> Unit
) {
    val sliderState = rememberSliderState(value, steps, valueRange)
    LaunchedEffect(value) { sliderState.value = value }
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 4.dp)
    ) {
        Text(
            text = label,
            style = MaterialTheme.typography.labelMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
        Slider(
            state = sliderState,
            modifier = Modifier.fillMaxWidth(),
            onValueChange = onValueChange
        )
    }
}
