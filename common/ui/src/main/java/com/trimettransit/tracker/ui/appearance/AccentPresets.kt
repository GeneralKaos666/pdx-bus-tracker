package com.trimettransit.tracker.ui.appearance

import androidx.annotation.StringRes
import androidx.compose.ui.graphics.Color
import com.trimettransit.tracker.ui.R

/** A hand-tuned accent preset offered in the color picker. */
data class AccentPreset(
    @StringRes val labelRes: Int,
    val color: Color
)

/** Curated accent presets for users who don't want to design their own seed color. */
val accentPresets: List<AccentPreset> = listOf(
    AccentPreset(R.string.accent_trimet_blue, Color(0xFF0079C1)),
    AccentPreset(R.string.accent_transit_orange, Color(0xFFFF7F00)),
    AccentPreset(R.string.accent_emerald, Color(0xFF00A86B)),
    AccentPreset(R.string.accent_violet, Color(0xFF8E24AA)),
    AccentPreset(R.string.accent_crimson, Color(0xFFC62828)),
    AccentPreset(R.string.accent_amber, Color(0xFFFFB300)),
    AccentPreset(R.string.accent_teal, Color(0xFF00838F)),
    AccentPreset(R.string.accent_slate, Color(0xFF546E7A))
)
