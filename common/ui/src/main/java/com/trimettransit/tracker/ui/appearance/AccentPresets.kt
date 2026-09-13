package com.trimettransit.tracker.ui.appearance

import androidx.compose.ui.graphics.Color

/** A hand-tuned accent preset offered in the color picker. */
data class AccentPreset(
    val name: String,
    val color: Color
)

/** Curated accent presets for users who don't want to design their own seed color. */
val accentPresets: List<AccentPreset> = listOf(
    AccentPreset("TriMet blue", Color(0xFF0079C1)),
    AccentPreset("Transit orange", Color(0xFFFF7F00)),
    AccentPreset("Emerald", Color(0xFF00A86B)),
    AccentPreset("Violet", Color(0xFF8E24AA)),
    AccentPreset("Crimson", Color(0xFFC62828)),
    AccentPreset("Amber", Color(0xFFFFB300)),
    AccentPreset("Teal", Color(0xFF00838F)),
    AccentPreset("Slate", Color(0xFF546E7A))
)