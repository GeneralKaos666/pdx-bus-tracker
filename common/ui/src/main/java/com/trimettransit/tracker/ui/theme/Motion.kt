package com.trimettransit.tracker.ui.theme

import androidx.compose.animation.core.SpringSpec
import androidx.compose.animation.core.spring

/**
 * Material 3 Expressive motion tokens used across phone + Wear transitions.
 *
 * Values mirror `androidx.compose.material3`'s `ExpressiveMotionTokens` (motion emitters 1.1.0):
 *   spatial mechanics — starting/destination/easing coefficient 360, shape coefficient 720, etc. —
 *   resolve to the spring constants below. They are exposed as plain generic specs (instead of the
 *   composable `MotionSchemeKeyTokens.value(...)` reader) because several call sites — NavHost
 *   enter/exit getters — are non-composable.
 *
 * Spatial (slide/scale/reposition) springs overshoot slightly (damping < 1) for the expressive
 * feel; effects (fade/color) are critically damped and snappy.
 *
 *   Spatial default: damping 0.8, stiffness 380
 *   Spatial fast:    damping 0.6, stiffness 800
 *   Spatial slow:    damping 0.8, stiffness 200
 *   Effects default: damping 1.0, stiffness 1600
 *   Effects fast:    damping 1.0, stiffness 3800
 *   Effects slow:    damping 1.0, stiffness 800
 */
fun <T> m3SpatialDefault(): SpringSpec<T> = spring(dampingRatio = 0.8f, stiffness = 380f)
fun <T> m3SpatialFast(): SpringSpec<T> = spring(dampingRatio = 0.6f, stiffness = 800f)
fun <T> m3SpatialSlow(): SpringSpec<T> = spring(dampingRatio = 0.8f, stiffness = 200f)
fun <T> m3EffectsDefault(): SpringSpec<T> = spring(dampingRatio = 1.0f, stiffness = 1600f)
fun <T> m3EffectsFast(): SpringSpec<T> = spring(dampingRatio = 1.0f, stiffness = 3800f)
fun <T> m3EffectsSlow(): SpringSpec<T> = spring(dampingRatio = 1.0f, stiffness = 800f)