package com.trimettransit.tracker.feature.settings

import androidx.compose.runtime.Composable
import androidx.compose.ui.res.stringResource
/**
 * Colour-picker overlay shared by the Colours and Cards panes.
 * A blank raw means "follow scheme". [onPick] writes state+prefs; the shell
 * clears the target via [onDismiss].
 */
@Composable
internal fun ColourPickerHost(
    target: ColourTarget,
    cardOutlineColorRaw: String,
    accentColorRaw: String,
    pillAccentRaw: String,
    transitBusRaw: String,
    transitRailRaw: String,
    transitStreetcarRaw: String,
    transitWesRaw: String,
    onDismiss: () -> Unit,
    onPick: (raw: String) -> Unit
) {
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
    ColourPickerDialog(
        title = title,
        autoLabel = stringResource(R.string.follow_scheme),
        initialArgb = initial.takeUnless { it.isBlank() },
        onDismiss = onDismiss,
        onAuto = { onPick(""); onDismiss() },
        onConfirm = { argb -> onPick(argb); onDismiss() }
    )
}
