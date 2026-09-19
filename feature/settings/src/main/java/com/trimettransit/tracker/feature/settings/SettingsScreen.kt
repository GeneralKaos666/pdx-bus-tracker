package com.trimettransit.tracker.feature.settings

import androidx.activity.compose.BackHandler
import androidx.annotation.StringRes
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.slideOutHorizontally
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.core.content.edit
import androidx.preference.PreferenceManager
import com.trimettransit.tracker.ui.appearance.AppearancePrefs
import com.trimettransit.tracker.ui.appearance.MapStyles
import com.trimettransit.tracker.ui.components.ContentEntrance
import com.trimettransit.tracker.ui.components.navPillBottomPadding
import com.trimettransit.tracker.ui.theme.AppMotion
import com.trimettransit.tracker.ui.theme.m3EffectsDefault
import com.trimettransit.tracker.ui.theme.m3SpatialDefault
import kotlinx.coroutines.launch
import kotlin.math.roundToInt

@Composable
fun SettingsScreen(
    widgetSection: (@Composable ColumnScope.() -> Unit)? = null,
    notificationsSection: (@Composable ColumnScope.() -> Unit)? = null,
    notificationsEnabled: Boolean? = null,
    widgetRefreshIntervalMin: Int? = null,
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
        mutableIntStateOf(prefs.getInt(AppearancePrefs.ARRIVALS_REFRESH_SECONDS, 30))
    }
    var showArrivalClock by remember {
        mutableStateOf(prefs.getBoolean(AppearancePrefs.ARRIVALS_SHOW_CLOCK, true))
    }
    var showArrivalRouteBadges by remember {
        mutableStateOf(prefs.getBoolean(AppearancePrefs.ARRIVALS_SHOW_ROUTE_BADGES, true))
    }
    var showArrivalVehicleInfo by remember {
        mutableStateOf(prefs.getBoolean(AppearancePrefs.ARRIVALS_SHOW_VEHICLE_INFO, true))
    }
    var motionRaw by remember {
        mutableStateOf(prefs.getString(AppearancePrefs.MOTION, "expressive") ?: "expressive")
    }
    var mapStyleRaw by remember {
        mutableStateOf(
            prefs.getString(AppearancePrefs.MAP_STYLE, MapStyles.DEFAULT)
                ?: MapStyles.DEFAULT
        )
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
    var currentSection by remember { mutableStateOf<SettingsSection?>(null) }

    val coroutineScope = rememberCoroutineScope()
    val scrollState = rememberScrollState()

    // Collapsed bottom-bar pill: scroll Settings back to the top.
    DisposableEffect(Unit) {
        onRegisterScrollToTop {
            coroutineScope.launch { scrollState.scrollTo(0) }
        }
        onDispose { onRegisterScrollToTop(null) }
    }

    // Panes share one scroll state; snap it back to the top before switching so a
    // pane never opens pre-scrolled at the previous pane's offset.
    fun switchPane(section: SettingsSection?) {
        coroutineScope.launch {
            scrollState.scrollTo(0)
            currentSection = section
        }
    }

    fun applyColour(target: ColourTarget, raw: String) {
        when (target) {
            ColourTarget.CARD_OUTLINE -> {
                cardOutlineColorRaw = raw
                prefs.edit { putString(AppearancePrefs.CARDS_OUTLINE_COLOR, raw) }
            }
            ColourTarget.ACCENT -> {
                accentColorRaw = raw
                prefs.edit { putString(AppearancePrefs.ACCENT_COLOR, raw) }
            }
            ColourTarget.PILL_ACCENT -> {
                pillAccentRaw = raw
                prefs.edit { putString(AppearancePrefs.PILL_ACCENT, raw) }
            }
            ColourTarget.TRANSIT_BUS -> {
                transitBusRaw = raw
                prefs.edit { putString(AppearancePrefs.TRANSIT_BUS, raw) }
            }
            ColourTarget.TRANSIT_RAIL -> {
                transitRailRaw = raw
                prefs.edit { putString(AppearancePrefs.TRANSIT_RAIL, raw) }
            }
            ColourTarget.TRANSIT_STREETCAR -> {
                transitStreetcarRaw = raw
                prefs.edit { putString(AppearancePrefs.TRANSIT_STREETCAR, raw) }
            }
            ColourTarget.TRANSIT_WES -> {
                transitWesRaw = raw
                prefs.edit { putString(AppearancePrefs.TRANSIT_WES, raw) }
            }
        }
    }

    // System back closes an open submenu before leaving the screen.
    BackHandler(enabled = currentSection != null) { switchPane(null) }

    ContentEntrance(modifier = Modifier.fillMaxSize()) {
        AnimatedContent(
            targetState = currentSection,
            transitionSpec = {
                if (AppMotion.reduceMotion) {
                    fadeIn(m3EffectsDefault()).togetherWith(fadeOut(m3EffectsDefault()))
                } else {
                    val opening = initialState == null
                    if (opening) {
                        (slideInHorizontally(m3SpatialDefault()) { it } + fadeIn(m3EffectsDefault()))
                            .togetherWith(
                                slideOutHorizontally(m3SpatialDefault()) { -it / 4 } +
                                    fadeOut(m3EffectsDefault())
                            )
                    } else {
                        (slideInHorizontally(m3SpatialDefault()) { -it / 4 } + fadeIn(m3EffectsDefault()))
                            .togetherWith(
                                slideOutHorizontally(m3SpatialDefault()) { it } +
                                    fadeOut(m3EffectsDefault())
                            )
                    }
                }
            },
            label = "settingsSubmenu"
        ) { section ->
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .statusBarsPadding()
                    .verticalScroll(scrollState)
                    .padding(bottom = navPillBottomPadding() + 8.dp)
            ) {
                if (section == null) {
                    SettingsMenu(
                        selectedTheme = selectedTheme,
                        accentMode = accentMode,
                        amoledDark = amoledDark,
                        densityRaw = densityRaw,
                        fontScaleRaw = fontScaleRaw,
                        cornerStyle = cornerStyle,
                        cornerRadius = cornerRadius,
                        mapStyleRaw = mapStyleRaw,
                        arrivalsRefreshSeconds = arrivalsRefreshSeconds,
                        notificationsSection = notificationsSection,
                        notificationsEnabled = notificationsEnabled,
                        widgetSection = widgetSection,
                        widgetRefreshIntervalMin = widgetRefreshIntervalMin,
                        onNavigate = { switchPane(it) }
                    )
                } else {
                    SubmenuHeader(
                        title = stringResource(section.titleRes),
                        subtitle = section.subtitleRes?.let { stringResource(it) },
                        onBack = { switchPane(null) }
                    )
                    when (section) {
                        SettingsSection.APPEARANCE -> AppearancePane(
                            selectedTheme = selectedTheme,
                            onThemeChange = {
                                selectedTheme = it
                                prefs.edit { putString(AppearancePrefs.THEME, it) }
                            }
                        )

                        SettingsSection.COLOURS -> ColoursPane(
                            accentMode = accentMode,
                            onAccentModeChange = {
                                accentMode = it
                                prefs.edit { putString(AppearancePrefs.COLOR_MODE, it) }
                            },
                            accentColorRaw = accentColorRaw,
                            onAccentColorChange = {
                                accentColorRaw = it
                                prefs.edit { putString(AppearancePrefs.ACCENT_COLOR, it) }
                            },
                            vibrancyRaw = vibrancyRaw,
                            onVibrancyChange = {
                                vibrancyRaw = it
                                prefs.edit { putString(AppearancePrefs.VIBRANCY, it) }
                            },
                            amoledDark = amoledDark,
                            onAmoledDarkChange = {
                                amoledDark = it
                                prefs.edit { putBoolean(AppearancePrefs.AMOLED_DARK, it) }
                            },
                            pillAccentRaw = pillAccentRaw,
                            transitBusRaw = transitBusRaw,
                            transitRailRaw = transitRailRaw,
                            transitStreetcarRaw = transitStreetcarRaw,
                            transitWesRaw = transitWesRaw,
                            onPickColour = { colourTarget = it }
                        )

                        SettingsSection.DISPLAY -> DisplayPane(
                            densityRaw = densityRaw,
                            onDensityChange = {
                                densityRaw = it
                                prefs.edit { putString(AppearancePrefs.DENSITY, it) }
                            },
                            fontScaleRaw = fontScaleRaw,
                            onFontScaleChange = {
                                fontScaleRaw = it
                                prefs.edit { putString(AppearancePrefs.FONT_SCALE, it) }
                            },
                            motionRaw = motionRaw,
                            onMotionChange = {
                                motionRaw = it
                                prefs.edit { putString(AppearancePrefs.MOTION, it) }
                            }
                        )

                        SettingsSection.CARDS -> CardsPane(
                            cardOutlines = cardOutlines,
                            onCardOutlinesChange = {
                                cardOutlines = it
                                prefs.edit { putBoolean("pref_key_card_outlines", it) }
                            },
                            cardOutlineColorRaw = cardOutlineColorRaw,
                            cornerRadius = cornerRadius,
                            onCornerRadiusChange = { cornerRadius = it },
                            onCornerRadiusFinished = {
                                prefs.edit { putInt("pref_key_card_corner_radius", cornerRadius.roundToInt()) }
                            },
                            cornerStyle = cornerStyle,
                            onCornerStyleChange = {
                                cornerStyle = it
                                prefs.edit { putString("pref_key_card_corner_style", it) }
                            },
                            onPickColour = { colourTarget = it }
                        )

                        SettingsSection.MAPS -> MapsPane(
                            mapStyleRaw = mapStyleRaw,
                            onMapStyleChange = {
                                mapStyleRaw = it
                                prefs.edit { putString(AppearancePrefs.MAP_STYLE, it) }
                            }
                        )

                        SettingsSection.ARRIVALS -> ArrivalsPane(
                            onlyShowSelectedRoute = onlyShowSelectedRoute,
                            onOnlyShowSelectedRouteChange = {
                                onlyShowSelectedRoute = it
                                prefs.edit { putBoolean("pref_key_only_show_route_selected", it) }
                            },
                            showArrivalClock = showArrivalClock,
                            onShowArrivalClockChange = {
                                showArrivalClock = it
                                prefs.edit { putBoolean(AppearancePrefs.ARRIVALS_SHOW_CLOCK, it) }
                            },
                            showArrivalRouteBadges = showArrivalRouteBadges,
                            onShowArrivalRouteBadgesChange = {
                                showArrivalRouteBadges = it
                                prefs.edit { putBoolean(AppearancePrefs.ARRIVALS_SHOW_ROUTE_BADGES, it) }
                            },
                            showArrivalVehicleInfo = showArrivalVehicleInfo,
                            onShowArrivalVehicleInfoChange = {
                                showArrivalVehicleInfo = it
                                prefs.edit { putBoolean(AppearancePrefs.ARRIVALS_SHOW_VEHICLE_INFO, it) }
                            },
                            arrivalsRefreshSeconds = arrivalsRefreshSeconds,
                            onArrivalsRefreshChange = {
                                arrivalsRefreshSeconds = it
                                prefs.edit { putInt(AppearancePrefs.ARRIVALS_REFRESH_SECONDS, it) }
                            }
                        )

                        SettingsSection.NOTIFICATIONS -> notificationsSection?.invoke(this)

                        SettingsSection.WIDGET -> widgetSection?.invoke(this)

                        SettingsSection.ABOUT -> AboutPane()

                        SettingsSection.LICENSES -> LicensesPane()
                    }
                }

                Spacer(modifier = Modifier.height(16.dp))
            }
        }
    }

    colourTarget?.let { target ->
        ColourPickerHost(
            target = target,
            cardOutlineColorRaw = cardOutlineColorRaw,
            accentColorRaw = accentColorRaw,
            pillAccentRaw = pillAccentRaw,
            transitBusRaw = transitBusRaw,
            transitRailRaw = transitRailRaw,
            transitStreetcarRaw = transitStreetcarRaw,
            transitWesRaw = transitWesRaw,
            onDismiss = { colourTarget = null },
            onPick = { raw -> applyColour(target, raw) }
        )
    }
}

/** Which colour pref the shared [ColourPickerDialog] is currently editing. */
internal enum class ColourTarget {
    CARD_OUTLINE, ACCENT, PILL_ACCENT, TRANSIT_BUS, TRANSIT_RAIL, TRANSIT_STREETCAR, TRANSIT_WES
}

/** The Settings categories reachable from the root menu as submenus. */
internal enum class SettingsSection(@StringRes val titleRes: Int, @StringRes val subtitleRes: Int? = null) {
    APPEARANCE(R.string.section_appearance, R.string.section_appearance_intro),
    COLOURS(R.string.section_colours, R.string.section_colours_intro),
    DISPLAY(R.string.section_display, R.string.section_display_intro),
    CARDS(R.string.section_cards, R.string.section_cards_intro),
    MAPS(R.string.section_maps, R.string.section_maps_intro),
    ARRIVALS(R.string.section_arrivals, R.string.section_arrivals_intro),
    NOTIFICATIONS(R.string.menu_notifications),
    WIDGET(R.string.menu_widget),
    ABOUT(R.string.section_about),
    LICENSES(R.string.open_source_licenses)
}