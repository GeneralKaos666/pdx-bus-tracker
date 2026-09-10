package com.trimettransit.tracker.feature.trips

import android.graphics.Bitmap
import android.graphics.Color
import com.trimettransit.tracker.ui.components.DotCircle
import com.trimettransit.tracker.ui.components.circleMarker

/**
 * Map marker artwork for the trip planner. Everything is drawn from the current M3
 * scheme colors so the markers follow theming and no TriMet artwork is used. Sizes are in
 * dp so text scale factors (user font scaling) don't inflate the markers.
 */

/** "Trip origin" marker: a filled circle with a white center dot and dark outline. */
internal fun originDotBitmap(fillColor: Int, density: Float): Bitmap =
    circleMarker(
        backDp = 24f, backColor = Color.BLACK,
        fillDp = 22f, fillColor = fillColor, density = density,
        foreground = listOf(DotCircle(9f, Color.WHITE))
    )

/** "Trip destination" marker: a filled circle with a white ring and a white center dot. */
internal fun destDotBitmap(fillColor: Int, density: Float): Bitmap =
    circleMarker(
        backDp = 24f, backColor = Color.BLACK,
        fillDp = 22f, fillColor = fillColor, density = density,
        foreground = listOf(DotCircle(15f, Color.WHITE), DotCircle(9f, fillColor))
    )

/** Small secondary-colored dot for a leg boarding/alighting point. */
internal fun stopDotBitmap(fillColor: Int, centerColor: Int, density: Float): Bitmap =
    circleMarker(
        backDp = 18f, backColor = Color.BLACK,
        fillDp = 16f, fillColor = fillColor, density = density,
        foreground = listOf(DotCircle(6f, centerColor))
    )

/** "You are here" marker: a primary-colored dot with a white ring inside a translucent halo. */
internal fun meDotBitmap(fillColor: Int, density: Float): Bitmap =
    circleMarker(
        backDp = 28f, backColor = (fillColor and 0x00FFFFFF) or (0x40 shl 24),
        fillDp = 28f, fillColor = fillColor, density = density,
        foreground = listOf(DotCircle(13f, Color.WHITE), DotCircle(10f, fillColor))
    )