package com.trimettransit.tracker.feature.settings

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.animateFloatAsState
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
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.OpenInNew
import androidx.compose.material.icons.filled.BorderAll
import androidx.compose.material.icons.filled.BrightnessAuto
import androidx.compose.material.icons.filled.Colorize
import androidx.compose.material.icons.filled.DarkMode
import androidx.compose.material.icons.filled.FormatSize
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.KeyboardArrowDown
import androidx.compose.material.icons.filled.LightMode
import androidx.compose.material.icons.filled.Palette
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Route
import androidx.compose.material.icons.filled.TextDecrease
import androidx.compose.material.icons.filled.TextIncrease
import androidx.compose.material.icons.filled.Tune
import androidx.compose.material.icons.filled.ViewAgenda
import androidx.compose.material.icons.filled.ViewStream
import androidx.compose.material3.Card
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
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
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import androidx.core.content.edit
import androidx.core.net.toUri
import androidx.core.graphics.drawable.toBitmap
import androidx.preference.PreferenceManager
import com.trimettransit.tracker.ui.appearance.AppearancePrefs
import com.trimettransit.tracker.ui.appearance.colorToSpec
import com.trimettransit.tracker.ui.appearance.parseColorSpec
import com.trimettransit.tracker.ui.components.ContentEntrance
import com.trimettransit.tracker.ui.components.navPillBottomPadding
import com.trimettransit.tracker.ui.components.SectionHeader
import com.trimettransit.tracker.ui.components.SettingsCard
import com.trimettransit.tracker.ui.components.SettingsCornerOption
import com.trimettransit.tracker.ui.components.SettingsIconCircle
import com.trimettransit.tracker.ui.components.SettingsRadioOption
import com.trimettransit.tracker.ui.components.SettingsSwitchOption
import com.trimettransit.tracker.ui.components.pressScale
import com.trimettransit.tracker.ui.components.transitColor
import com.trimettransit.tracker.ui.theme.LocalCardStyle
import com.trimettransit.tracker.ui.theme.TrimetBlue
import com.trimettransit.tracker.ui.theme.appCardShape
import com.trimettransit.tracker.ui.theme.appCardBorder
import com.trimettransit.tracker.ui.theme.m3EffectsDefault
import com.trimettransit.tracker.ui.theme.m3EffectsFast
import com.trimettransit.tracker.ui.theme.m3SpatialDefault
import com.trimettransit.tracker.ui.theme.m3SpatialFast

import kotlinx.coroutines.launch
import kotlin.math.roundToInt

@Composable
fun SettingsScreen(
    widgetSection: (@Composable ColumnScope.() -> Unit)? = null,
    notificationsSection: (@Composable ColumnScope.() -> Unit)? = null,
    onRegisterScrollToTop: ((() -> Unit)?) -> Unit
) {
    val context = LocalContext.current
    val prefs = remember { PreferenceManager.getDefaultSharedPreferences(context) }

    var selectedTheme by remember { mutableStateOf(prefs.getString(AppearancePrefs.THEME, "system") ?: "system") }
    var accentMode by remember {
        mutableStateOf(
            prefs.getString(AppearancePrefs.COLOR_MODE, null) ?: if (
                prefs.getBoolean(AppearancePrefs.DYNAMIC_COLOR, true)
            ) "dynamic" else "seed"
        )
    }
    var accentColorRaw by remember {
        mutableStateOf(prefs.getString(AppearancePrefs.ACCENT_COLOR, "") ?: "")
    }
    var vibrancyRaw by remember {
        mutableStateOf(prefs.getString(AppearancePrefs.VIBRANCY, "default") ?: "default")
    }
    var amoledDark by remember {
        mutableStateOf(prefs.getBoolean(AppearancePrefs.AMOLED_DARK, false))
    }
    var pillAccentRaw by remember {
        mutableStateOf(prefs.getString(AppearancePrefs.PILL_ACCENT, "") ?: "")
    }
    var transitBusRaw by remember {
        mutableStateOf(prefs.getString(AppearancePrefs.TRANSIT_BUS, "") ?: "")
    }
    var transitRailRaw by remember {
        mutableStateOf(prefs.getString(AppearancePrefs.TRANSIT_RAIL, "") ?: "")
    }
    var transitStreetcarRaw by remember {
        mutableStateOf(prefs.getString(AppearancePrefs.TRANSIT_STREETCAR, "") ?: "")
    }
    var transitWesRaw by remember {
        mutableStateOf(prefs.getString(AppearancePrefs.TRANSIT_WES, "") ?: "")
    }
    var densityRaw by remember {
        mutableStateOf(prefs.getString(AppearancePrefs.DENSITY, "comfortable") ?: "comfortable")
    }
    var fontScaleRaw by remember {
        mutableStateOf(prefs.getString(AppearancePrefs.FONT_SCALE, "default") ?: "default")
    }
    var arrivalsRefreshSeconds by remember {
        mutableStateOf(prefs.getInt(AppearancePrefs.ARRIVALS_REFRESH_SECONDS, 30))
    }
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
    var cornerStyle by remember {
        mutableStateOf(prefs.getString("pref_key_card_corner_style", "rounded") ?: "rounded")
    }
    var colourTarget by remember { mutableStateOf<ColourTarget?>(null) }

    val coroutineScope = rememberCoroutineScope()
    val scrollState = rememberScrollState()

    // Collapsed bottom-bar pill: scroll Settings back to the top.
    DisposableEffect(Unit) {
        onRegisterScrollToTop {
            coroutineScope.launch { scrollState.scrollTo(0) }
        }
        onDispose { onRegisterScrollToTop(null) }
    }

    ContentEntrance(modifier = Modifier.fillMaxSize()) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .verticalScroll(scrollState)
                .padding(bottom = navPillBottomPadding() + 8.dp)
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
                        prefs.edit { putString(AppearancePrefs.THEME, "system") }
                    }
                )
                SettingsRadioOption(
                    label = stringResource(R.string.theme_light),
                    subtitle = stringResource(R.string.theme_light_subtitle),
                    icon = Icons.Filled.LightMode,
                    selected = selectedTheme == "light",
                    onClick = {
                        selectedTheme = "light"
                        prefs.edit { putString(AppearancePrefs.THEME, "light") }
                    }
                )
                SettingsRadioOption(
                    label = stringResource(R.string.theme_dark),
                    subtitle = stringResource(R.string.theme_dark_subtitle),
                    icon = Icons.Filled.DarkMode,
                    selected = selectedTheme == "dark",
                    onClick = {
                        selectedTheme = "dark"
                        prefs.edit { putString(AppearancePrefs.THEME, "dark") }
                    }
                )
            }

            SectionHeader(title = stringResource(R.string.section_colours))

            SettingsCard {
                var coloursExpanded by remember { mutableStateOf(false) }
                val coloursSource = remember { MutableInteractionSource() }
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .pressScale(coloursSource)
                        .clickable(
                            interactionSource = coloursSource,
                            indication = LocalIndication.current
                        ) { coloursExpanded = !coloursExpanded }
                        .padding(horizontal = 16.dp, vertical = 12.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    SettingsIconCircle(icon = Icons.Filled.Palette, highlighted = coloursExpanded)
                    Spacer(modifier = Modifier.width(16.dp))
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = stringResource(R.string.colour_accent),
                            style = MaterialTheme.typography.bodyLarge,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                        Text(
                            text = stringResource(R.string.colour_accent_subtitle),
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            maxLines = 2,
                            overflow = TextOverflow.Ellipsis
                        )
                    }
                    Spacer(modifier = Modifier.width(16.dp))
                    val coloursChevron by animateFloatAsState(
                        targetValue = if (coloursExpanded) 180f else 0f,
                        animationSpec = m3SpatialDefault(),
                        label = "coloursChevron"
                    )
                    Icon(
                        imageVector = Icons.Filled.KeyboardArrowDown,
                        contentDescription = if (coloursExpanded) stringResource(R.string.collapse) else stringResource(R.string.expand),
                        tint = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.rotate(coloursChevron)
                    )
                }
                AnimatedVisibility(
                    visible = coloursExpanded,
                    enter = expandVertically(
                        animationSpec = m3SpatialDefault()
                    ) + fadeIn(m3EffectsDefault()),
                    exit = shrinkVertically(
                        animationSpec = m3SpatialFast()
                    ) + fadeOut(m3EffectsFast())
                ) {
                    Column {
                        SettingsRadioOption(
                            label = stringResource(R.string.colour_mode_dynamic),
                            subtitle = stringResource(R.string.colour_mode_dynamic_subtitle),
                            icon = Icons.Filled.BrightnessAuto,
                            selected = accentMode == "dynamic",
                            onClick = {
                                accentMode = "dynamic"
                                prefs.edit { putString(AppearancePrefs.COLOR_MODE, "dynamic") }
                            }
                        )
                        SettingsRadioOption(
                            label = stringResource(R.string.colour_mode_seed),
                            subtitle = stringResource(R.string.colour_mode_seed_subtitle),
                            icon = Icons.Filled.Colorize,
                            selected = accentMode == "seed",
                            onClick = {
                                accentMode = "seed"
                                prefs.edit { putString(AppearancePrefs.COLOR_MODE, "seed") }
                            }
                        )
                        AnimatedVisibility(
                            visible = accentMode == "seed",
                            enter = expandVertically(m3SpatialDefault()) + fadeIn(m3EffectsDefault()),
                            exit = shrinkVertically(m3SpatialFast()) + fadeOut(m3EffectsFast())
                        ) {
                            Column {
                                AccentPresetRow(
                                    selected = parseColorSpec(accentColorRaw),
                                    onSelect = { color ->
                                        accentColorRaw = colorToSpec(color)
                                        prefs.edit { putString(AppearancePrefs.ACCENT_COLOR, colorToSpec(color)) }
                                    }
                                )
                                SettingsColourOption(
                                    label = stringResource(R.string.accent_custom),
                                    subtitle = stringResource(R.string.accent_custom_subtitle),
                                    icon = Icons.Filled.Colorize,
                                    colour = parseColorSpec(accentColorRaw) ?: TrimetBlue,
                                    onClick = { colourTarget = ColourTarget.ACCENT }
                                )
                            }
                        }

                        HorizontalDivider(modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp))

                        SettingsRadioOption(
                            label = stringResource(R.string.vibrancy_muted),
                            subtitle = stringResource(R.string.vibrancy_muted_subtitle),
                            icon = Icons.Filled.BrightnessAuto,
                            selected = vibrancyRaw == "muted",
                            onClick = {
                                vibrancyRaw = "muted"
                                prefs.edit { putString(AppearancePrefs.VIBRANCY, "muted") }
                            }
                        )
                        SettingsRadioOption(
                            label = stringResource(R.string.vibrancy_default),
                            subtitle = stringResource(R.string.vibrancy_default_subtitle),
                            icon = Icons.Filled.Palette,
                            selected = vibrancyRaw == "default",
                            onClick = {
                                vibrancyRaw = "default"
                                prefs.edit { putString(AppearancePrefs.VIBRANCY, "default") }
                            }
                        )
                        SettingsRadioOption(
                            label = stringResource(R.string.vibrancy_vibrant),
                            subtitle = stringResource(R.string.vibrancy_vibrant_subtitle),
                            icon = Icons.Filled.Colorize,
                            selected = vibrancyRaw == "vibrant",
                            onClick = {
                                vibrancyRaw = "vibrant"
                                prefs.edit { putString(AppearancePrefs.VIBRANCY, "vibrant") }
                            }
                        )

                        HorizontalDivider(modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp))

                        SettingsSwitchOption(
                            label = stringResource(R.string.amoled_dark),
                            subtitle = stringResource(R.string.amoled_dark_subtitle),
                            icon = Icons.Filled.DarkMode,
                            checked = amoledDark,
                            onCheckedChange = {
                                amoledDark = it
                                prefs.edit { putBoolean(AppearancePrefs.AMOLED_DARK, it) }
                            }
                        )

                        HorizontalDivider(modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp))

                        val scheme = MaterialTheme.colorScheme
                        SettingsColourOption(
                            label = stringResource(R.string.pill_accent),
                            subtitle = stringResource(R.string.pill_accent_subtitle),
                            icon = Icons.Filled.Colorize,
                            colour = parseColorSpec(pillAccentRaw) ?: scheme.primary,
                            onClick = { colourTarget = ColourTarget.PILL_ACCENT }
                        )

                        HorizontalDivider(modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp))

                        SettingsColourOption(
                            label = stringResource(R.string.transit_bus),
                            subtitle = stringResource(R.string.transit_type_subtitle),
                            icon = Icons.Filled.Route,
                            colour = parseColorSpec(transitBusRaw) ?: transitColor("B", scheme),
                            onClick = { colourTarget = ColourTarget.TRANSIT_BUS }
                        )
                        SettingsColourOption(
                            label = stringResource(R.string.transit_rail),
                            subtitle = stringResource(R.string.transit_type_subtitle),
                            icon = Icons.Filled.Route,
                            colour = parseColorSpec(transitRailRaw) ?: transitColor("M", scheme),
                            onClick = { colourTarget = ColourTarget.TRANSIT_RAIL }
                        )
                        SettingsColourOption(
                            label = stringResource(R.string.transit_streetcar),
                            subtitle = stringResource(R.string.transit_type_subtitle),
                            icon = Icons.Filled.Route,
                            colour = parseColorSpec(transitStreetcarRaw) ?: transitColor("S", scheme),
                            onClick = { colourTarget = ColourTarget.TRANSIT_STREETCAR }
                        )
                        SettingsColourOption(
                            label = stringResource(R.string.transit_wes),
                            subtitle = stringResource(R.string.transit_type_subtitle),
                            icon = Icons.Filled.Route,
                            colour = parseColorSpec(transitWesRaw) ?: transitColor("W", scheme),
                            onClick = { colourTarget = ColourTarget.TRANSIT_WES }
                        )
                    }
                }
            }

            SectionHeader(title = stringResource(R.string.section_display))

            SettingsCard {
                var displayExpanded by remember { mutableStateOf(true) }
                val displaySource = remember { MutableInteractionSource() }
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .pressScale(displaySource)
                        .clickable(
                            interactionSource = displaySource,
                            indication = LocalIndication.current
                        ) { displayExpanded = !displayExpanded }
                        .padding(horizontal = 16.dp, vertical = 12.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    SettingsIconCircle(icon = Icons.Filled.ViewStream, highlighted = displayExpanded)
                    Spacer(modifier = Modifier.width(16.dp))
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = stringResource(R.string.section_display),
                            style = MaterialTheme.typography.bodyLarge,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                        Text(
                            text = stringResource(R.string.section_display_subtitle),
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            maxLines = 2,
                            overflow = TextOverflow.Ellipsis
                        )
                    }
                    Spacer(modifier = Modifier.width(16.dp))
                    val displayChevronRotation by animateFloatAsState(
                        targetValue = if (displayExpanded) 180f else 0f,
                        animationSpec = m3SpatialDefault(),
                        label = "displayChevron"
                    )
                    Icon(
                        imageVector = Icons.Filled.KeyboardArrowDown,
                        contentDescription = if (displayExpanded) stringResource(R.string.collapse) else stringResource(R.string.expand),
                        tint = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.rotate(displayChevronRotation)
                    )
                }
                AnimatedVisibility(
                    visible = displayExpanded,
                    enter = expandVertically(
                        animationSpec = m3SpatialDefault()
                    ) + fadeIn(m3EffectsDefault()),
                    exit = shrinkVertically(
                        animationSpec = m3SpatialFast()
                    ) + fadeOut(m3EffectsFast())
                ) {
                    Column {
                        SettingsRadioOption(
                            label = stringResource(R.string.density_comfortable),
                            subtitle = stringResource(R.string.density_comfortable_subtitle),
                            icon = Icons.Filled.ViewStream,
                            selected = densityRaw == "comfortable",
                            onClick = {
                                densityRaw = "comfortable"
                                prefs.edit { putString(AppearancePrefs.DENSITY, "comfortable") }
                            }
                        )
                        SettingsRadioOption(
                            label = stringResource(R.string.density_compact),
                            subtitle = stringResource(R.string.density_compact_subtitle),
                            icon = Icons.Filled.ViewAgenda,
                            selected = densityRaw == "compact",
                            onClick = {
                                densityRaw = "compact"
                                prefs.edit { putString(AppearancePrefs.DENSITY, "compact") }
                            }
                        )

                        HorizontalDivider(modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp))

                        SettingsRadioOption(
                            label = stringResource(R.string.font_smaller),
                            subtitle = stringResource(R.string.font_smaller_subtitle),
                            icon = Icons.Filled.TextDecrease,
                            selected = fontScaleRaw == "smaller",
                            onClick = {
                                fontScaleRaw = "smaller"
                                prefs.edit { putString(AppearancePrefs.FONT_SCALE, "smaller") }
                            }
                        )
                        SettingsRadioOption(
                            label = stringResource(R.string.font_default),
                            subtitle = stringResource(R.string.font_default_subtitle),
                            icon = Icons.Filled.FormatSize,
                            selected = fontScaleRaw == "default",
                            onClick = {
                                fontScaleRaw = "default"
                                prefs.edit { putString(AppearancePrefs.FONT_SCALE, "default") }
                            }
                        )
                        SettingsRadioOption(
                            label = stringResource(R.string.font_larger),
                            subtitle = stringResource(R.string.font_larger_subtitle),
                            icon = Icons.Filled.TextIncrease,
                            selected = fontScaleRaw == "larger",
                            onClick = {
                                fontScaleRaw = "larger"
                                prefs.edit { putString(AppearancePrefs.FONT_SCALE, "larger") }
                            }
                        )
                    }
                }
            }

            SectionHeader(title = stringResource(R.string.section_cards))

            SettingsCard {
                var cardsExpanded by remember { mutableStateOf(false) }
                val cardsInteractionSource = remember { MutableInteractionSource() }
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .pressScale(cardsInteractionSource)
                        .clickable(
                            interactionSource = cardsInteractionSource,
                            indication = LocalIndication.current
                        ) { cardsExpanded = !cardsExpanded }
                        .padding(horizontal = 16.dp, vertical = 12.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    SettingsIconCircle(icon = Icons.Filled.BorderAll, highlighted = cardsExpanded)
                    Spacer(modifier = Modifier.width(16.dp))
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = stringResource(R.string.card_style),
                            style = MaterialTheme.typography.bodyLarge,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                        Text(
                            text = stringResource(R.string.card_style_subtitle),
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            maxLines = 2,
                            overflow = TextOverflow.Ellipsis
                        )
                    }
                    Spacer(modifier = Modifier.width(16.dp))
                    val cardsChevronRotation by animateFloatAsState(
                        targetValue = if (cardsExpanded) 180f else 0f,
                        animationSpec = m3SpatialDefault(),
                        label = "cardsChevron"
                    )
                    Icon(
                        imageVector = Icons.Filled.KeyboardArrowDown,
                        contentDescription = if (cardsExpanded) stringResource(R.string.collapse) else stringResource(R.string.expand),
                        tint = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.rotate(cardsChevronRotation)
                    )
                }
                AnimatedVisibility(
                    visible = cardsExpanded,
                    enter = expandVertically(
                        animationSpec = m3SpatialDefault()
                    ) + fadeIn(m3EffectsDefault()),
                    exit = shrinkVertically(
                        animationSpec = m3SpatialFast()
                    ) + fadeOut(m3EffectsFast())
                ) {
                    Column {
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
                                onClick = { colourTarget = ColourTarget.CARD_OUTLINE }
                            )
                        }
                        SettingsCornerOption(
                            label = stringResource(R.string.corner_style_rounded),
                            subtitle = stringResource(R.string.corner_style_rounded_subtitle),
                            cut = false,
                            selected = cornerStyle == "rounded",
                            onClick = {
                                cornerStyle = "rounded"
                                prefs.edit { putString("pref_key_card_corner_style", "rounded") }
                            }
                        )
                        SettingsCornerOption(
                            label = stringResource(R.string.corner_style_cut),
                            subtitle = stringResource(R.string.corner_style_cut_subtitle),
                            cut = true,
                            selected = cornerStyle == "cut",
                            onClick = {
                                cornerStyle = "cut"
                                prefs.edit { putString("pref_key_card_corner_style", "cut") }
                            }
                        )
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
                }
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

                HorizontalDivider(modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp))

                SettingsRadioOption(
                    label = stringResource(R.string.refresh_cadence_15),
                    subtitle = stringResource(R.string.refresh_cadence_subtitle),
                    icon = Icons.Filled.Refresh,
                    selected = arrivalsRefreshSeconds == 15,
                    onClick = {
                        arrivalsRefreshSeconds = 15
                        prefs.edit { putInt(AppearancePrefs.ARRIVALS_REFRESH_SECONDS, 15) }
                    }
                )
                SettingsRadioOption(
                    label = stringResource(R.string.refresh_cadence_30),
                    subtitle = stringResource(R.string.refresh_cadence_subtitle),
                    icon = Icons.Filled.Refresh,
                    selected = arrivalsRefreshSeconds == 30,
                    onClick = {
                        arrivalsRefreshSeconds = 30
                        prefs.edit { putInt(AppearancePrefs.ARRIVALS_REFRESH_SECONDS, 30) }
                    }
                )
                SettingsRadioOption(
                    label = stringResource(R.string.refresh_cadence_60),
                    subtitle = stringResource(R.string.refresh_cadence_subtitle),
                    icon = Icons.Filled.Refresh,
                    selected = arrivalsRefreshSeconds == 60,
                    onClick = {
                        arrivalsRefreshSeconds = 60
                        prefs.edit { putInt(AppearancePrefs.ARRIVALS_REFRESH_SECONDS, 60) }
                    }
                )
                SettingsRadioOption(
                    label = stringResource(R.string.refresh_cadence_120),
                    subtitle = stringResource(R.string.refresh_cadence_subtitle),
                    icon = Icons.Filled.Refresh,
                    selected = arrivalsRefreshSeconds == 120,
                    onClick = {
                        arrivalsRefreshSeconds = 120
                        prefs.edit { putInt(AppearancePrefs.ARRIVALS_REFRESH_SECONDS, 120) }
                    }
                )
            }

            // App-owned sections (e.g. Departure alerts, Widget settings) injected from the host module.
            if (notificationsSection != null) {
                notificationsSection()
            }
            if (widgetSection != null) {
                widgetSection()
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
                        LicenseEntry(stringResource(R.string.license_glance), stringResource(R.string.license_apache_2))
                        LicenseEntry(stringResource(R.string.license_workmanager), stringResource(R.string.license_apache_2))
                        LicenseEntry(stringResource(R.string.license_materialkolor), stringResource(R.string.license_mit))
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

    colourTarget?.let { target ->
        val title = when (target) {
            ColourTarget.CARD_OUTLINE -> stringResource(R.string.card_outline_colour)
            ColourTarget.ACCENT -> stringResource(R.string.accent_colour)
            ColourTarget.PILL_ACCENT -> stringResource(R.string.pill_accent)
            ColourTarget.TRANSIT_BUS -> stringResource(R.string.transit_bus)
            ColourTarget.TRANSIT_RAIL -> stringResource(R.string.transit_rail)
            ColourTarget.TRANSIT_STREETCAR -> stringResource(R.string.transit_streetcar)
            ColourTarget.TRANSIT_WES -> stringResource(R.string.transit_wes)
        }
        val initial = when (target) {
            ColourTarget.CARD_OUTLINE -> cardOutlineColorRaw
            ColourTarget.ACCENT -> accentColorRaw
            ColourTarget.PILL_ACCENT -> pillAccentRaw
            ColourTarget.TRANSIT_BUS -> transitBusRaw
            ColourTarget.TRANSIT_RAIL -> transitRailRaw
            ColourTarget.TRANSIT_STREETCAR -> transitStreetcarRaw
            ColourTarget.TRANSIT_WES -> transitWesRaw
        }
        fun applyRaw(raw: String) {
            when (target) {
                ColourTarget.CARD_OUTLINE -> cardOutlineColorRaw = raw
                ColourTarget.ACCENT -> accentColorRaw = raw
                ColourTarget.PILL_ACCENT -> pillAccentRaw = raw
                ColourTarget.TRANSIT_BUS -> transitBusRaw = raw
                ColourTarget.TRANSIT_RAIL -> transitRailRaw = raw
                ColourTarget.TRANSIT_STREETCAR -> transitStreetcarRaw = raw
                ColourTarget.TRANSIT_WES -> transitWesRaw = raw
            }
        }
        fun applyPref(raw: String) {
            when (target) {
                ColourTarget.CARD_OUTLINE -> prefs.edit { putString(AppearancePrefs.CARDS_OUTLINE_COLOR, raw) }
                ColourTarget.ACCENT -> prefs.edit { putString(AppearancePrefs.ACCENT_COLOR, raw) }
                ColourTarget.PILL_ACCENT -> prefs.edit { putString(AppearancePrefs.PILL_ACCENT, raw) }
                ColourTarget.TRANSIT_BUS -> prefs.edit { putString(AppearancePrefs.TRANSIT_BUS, raw) }
                ColourTarget.TRANSIT_RAIL -> prefs.edit { putString(AppearancePrefs.TRANSIT_RAIL, raw) }
                ColourTarget.TRANSIT_STREETCAR -> prefs.edit { putString(AppearancePrefs.TRANSIT_STREETCAR, raw) }
                ColourTarget.TRANSIT_WES -> prefs.edit { putString(AppearancePrefs.TRANSIT_WES, raw) }
            }
        }
        ColourPickerDialog(
            title = title,
            autoLabel = stringResource(R.string.follow_scheme),
            initialArgb = initial.takeUnless { it.isBlank() },
            onDismiss = { colourTarget = null },
            onAuto = {
                applyRaw("")
                applyPref("")
                colourTarget = null
            },
            onConfirm = { argb ->
                applyRaw(argb)
                applyPref(argb)
                colourTarget = null
            }
        )
    }
}

/** Which colour pref the shared [ColourPickerDialog] is currently editing. */
private enum class ColourTarget {
    CARD_OUTLINE, ACCENT, PILL_ACCENT, TRANSIT_BUS, TRANSIT_RAIL, TRANSIT_STREETCAR, TRANSIT_WES
}
