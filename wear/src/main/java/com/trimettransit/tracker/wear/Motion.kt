package com.trimettransit.tracker.wear

import androidx.compose.animation.core.SpringSpec
import androidx.compose.animation.core.spring

/**
 * Material 3 Expressive motion tokens for the standalone Wear module.
 *
 * Wear deliberately does not depend on `common/ui` (it ships its own animation helpers in
 * [WearAnimations] — see `wear/build.gradle.kts`'s module list), so these mirror
 * `common/ui/.../theme/Motion.kt` exactly. Values are the `androidx.compose.material3`
 * `ExpressiveMotionTokens` spring constants:
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