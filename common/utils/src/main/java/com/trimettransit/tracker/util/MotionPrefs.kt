package com.trimettransit.tracker.util

import android.content.Context
import android.provider.Settings

/**
 * True when the system animation scales are off ("remove animations" accessibility
 * setting), signaling the app to drop movement while keeping opacity/color changes.
 * Mirrors the animator/transition scales instead of relying on an OS API level gate.
 */
fun systemReduceMotion(context: Context): Boolean {
    val resolver = context.contentResolver
    val animatorScale = Settings.Global.getFloat(resolver, Settings.Global.ANIMATOR_DURATION_SCALE, 1f)
    val transitionScale = Settings.Global.getFloat(resolver, Settings.Global.TRANSITION_ANIMATION_SCALE, 1f)
    return animatorScale == 0f || transitionScale == 0f
}