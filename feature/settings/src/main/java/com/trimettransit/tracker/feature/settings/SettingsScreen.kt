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
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.preference.PreferenceManager
import com.trimettransit.tracker.ui.components.ContentEntrance
import com.trimettransit.tracker.ui.components.navPillBottomPadding
import com.trimettransit.tracker.ui.theme.AppMotion
import com.trimettransit.tracker.ui.theme.m3EffectsDefault
import com.trimettransit.tracker.ui.theme.m3SpatialDefault
import kotlinx.coroutines.launch


@Composable
fun SettingsScreen(
    widgetSection: (@Composable ColumnScope.() -> Unit)? = null,
    notificationsSection: (@Composable ColumnScope.() -> Unit)? = null,
    notificationsEnabled: Boolean? = null,
    widgetRefreshIntervalMin: Int? = null,
    apiKeyConfigured: Boolean = true,
    onRegisterScrollToTop: ((() -> Unit)?) -> Unit,
    onRegisterBackAction: ((() -> Boolean)?) -> Unit
) {
    val context = LocalContext.current
    val prefs = remember { PreferenceManager.getDefaultSharedPreferences(context) }

    var settings by remember { mutableStateOf(SettingsPreferenceState.read(prefs)) }
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

    // Lets the collapsed bottom bar's back arrow close an open pane before it pops the whole
    // Settings destination, so it agrees with the system back button below.
    DisposableEffect(Unit) {
        onRegisterBackAction {
            if (currentSection != null) {
                switchPane(null)
                true
            } else {
                false
            }
        }
        onDispose { onRegisterBackAction(null) }
    }

    fun updateSettings(transform: (SettingsPreferenceState) -> SettingsPreferenceState) {
        settings = transform(settings)
        settings.save(prefs)
    }

    fun applyColour(target: ColourTarget, raw: String) {
        when (target) {
            ColourTarget.CARD_OUTLINE -> {
                updateSettings { it.copy(cardOutlineColor = raw) }
            }
            ColourTarget.ACCENT -> {
                updateSettings { it.copy(accentColor = raw) }
            }
            ColourTarget.PILL_ACCENT -> {
                updateSettings { it.copy(pillAccent = raw) }
            }
            ColourTarget.TRANSIT_BUS -> {
                updateSettings { it.copy(transitBus = raw) }
            }
            ColourTarget.TRANSIT_RAIL -> {
                updateSettings { it.copy(transitRail = raw) }
            }
            ColourTarget.TRANSIT_STREETCAR -> {
                updateSettings { it.copy(transitStreetcar = raw) }
            }
            ColourTarget.TRANSIT_WES -> {
                updateSettings { it.copy(transitWes = raw) }
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
                        settings = settings,
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
                            selectedTheme = settings.theme,
                            onThemeChange = { updateSettings { state -> state.copy(theme = it) } }
                        )

                        SettingsSection.COLOURS -> ColoursPane(
                            accentMode = settings.colorMode,
                            onAccentModeChange = { updateSettings { state -> state.copy(colorMode = it) } },
                            accentColorRaw = settings.accentColor,
                            onAccentColorChange = { updateSettings { state -> state.copy(accentColor = it) } },
                            vibrancyRaw = settings.vibrancy,
                            onVibrancyChange = { updateSettings { state -> state.copy(vibrancy = it) } },
                            amoledDark = settings.amoledDark,
                            onAmoledDarkChange = { updateSettings { state -> state.copy(amoledDark = it) } },
                            pillAccentRaw = settings.pillAccent,
                            transitBusRaw = settings.transitBus,
                            transitRailRaw = settings.transitRail,
                            transitStreetcarRaw = settings.transitStreetcar,
                            transitWesRaw = settings.transitWes,
                            onPickColour = { colourTarget = it }
                        )

                        SettingsSection.DISPLAY -> DisplayPane(
                            densityRaw = settings.density,
                            onDensityChange = { updateSettings { state -> state.copy(density = it) } },
                            fontScaleRaw = settings.fontScale,
                            onFontScaleChange = { updateSettings { state -> state.copy(fontScale = it) } },
                            motionRaw = settings.motion,
                            onMotionChange = { updateSettings { state -> state.copy(motion = it) } }
                        )

                        SettingsSection.CARDS -> CardsPane(
                            cardOutlines = settings.cardOutlines,
                            onCardOutlinesChange = { updateSettings { state -> state.copy(cardOutlines = it) } },
                            cardOutlineColorRaw = settings.cardOutlineColor,
                            cornerRadius = settings.cornerRadius,
                            onCornerRadiusChange = { settings = settings.copy(cornerRadius = it) },
                            onCornerRadiusFinished = { settings.save(prefs) },
                            cornerStyle = settings.cornerStyle,
                            onCornerStyleChange = { updateSettings { state -> state.copy(cornerStyle = it) } },
                            onPickColour = { colourTarget = it }
                        )

                        SettingsSection.MAPS -> MapsPane(
                            mapStyleRaw = settings.mapStyle,
                            onMapStyleChange = { updateSettings { state -> state.copy(mapStyle = it) } }
                        )

                        SettingsSection.ARRIVALS -> ArrivalsPane(
                            onlyShowSelectedRoute = settings.onlyShowSelectedRoute,
                            onOnlyShowSelectedRouteChange = {
                                updateSettings { state -> state.copy(onlyShowSelectedRoute = it) }
                            },
                            showArrivalClock = settings.showArrivalClock,
                            onShowArrivalClockChange = {
                                updateSettings { state -> state.copy(showArrivalClock = it) }
                            },
                            showArrivalRouteBadges = settings.showArrivalRouteBadges,
                            onShowArrivalRouteBadgesChange = {
                                updateSettings { state -> state.copy(showArrivalRouteBadges = it) }
                            },
                            showArrivalVehicleInfo = settings.showArrivalVehicleInfo,
                            onShowArrivalVehicleInfoChange = {
                                updateSettings { state -> state.copy(showArrivalVehicleInfo = it) }
                            },
                            arrivalsRefreshSeconds = settings.arrivalsRefreshSeconds,
                            onArrivalsRefreshChange = {
                                updateSettings { state -> state.copy(arrivalsRefreshSeconds = it) }
                            }
                        )

                        SettingsSection.NOTIFICATIONS -> notificationsSection?.invoke(this)

                        SettingsSection.WIDGET -> widgetSection?.invoke(this)

                        SettingsSection.ABOUT -> AboutPane(apiKeyConfigured = apiKeyConfigured)

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
            cardOutlineColorRaw = settings.cardOutlineColor,
            accentColorRaw = settings.accentColor,
            pillAccentRaw = settings.pillAccent,
            transitBusRaw = settings.transitBus,
            transitRailRaw = settings.transitRail,
            transitStreetcarRaw = settings.transitStreetcar,
            transitWesRaw = settings.transitWes,
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