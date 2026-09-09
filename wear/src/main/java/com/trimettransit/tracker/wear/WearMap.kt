package com.trimettransit.tracker.wear

import android.content.Context
import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Paint
import androidx.annotation.DrawableRes
import androidx.compose.ui.graphics.Color
import androidx.core.content.ContextCompat
import androidx.core.graphics.createBitmap
import androidx.core.graphics.drawable.toBitmap
import androidx.wear.compose.material3.ColorScheme
import com.trimettransit.tracker.R
import org.maplibre.android.camera.CameraUpdateFactory
import org.maplibre.android.geometry.LatLng
import org.maplibre.android.maps.MapLibreMap
import com.trimettransit.tracker.model.BlockPosition

/**
 * Map marker helpers for the watch's live bus map, ported from the phone's `common/ui`
 * (TransitColors.kt / MapBitmaps.kt) and arrivals `StopMapCard` so the wear module needs no
 * dependency on phone-UI modules. Markers are drawn from the current Wear M3 scheme colors so
 * they follow theming, and no TriMet artwork is used.
 */

/** Returns the route badge letter for a route number ("M" for MAX, "S" for
 *  Streetcar, "W" for WES, "B" for buses; "" when unknown). */
internal fun wearTransitBadgeLetter(routeNumber: Int): String = when {
    routeNumber == 90 || routeNumber == 100 || routeNumber == 190 ||
        routeNumber == 200 || routeNumber == 290 || routeNumber == 196 -> "M"
    routeNumber == 203 -> "W"
    routeNumber in 193..195 -> "S"
    routeNumber in 1..99 -> "B"
    else -> ""
}

/** Returns a transit-type color derived from the Wear M3 color scheme. */
internal fun wearTransitColor(type: String?, scheme: ColorScheme): Color = when (type) {
    "B", "S", "T" -> scheme.primary          // Bus, Streetcar
    "R" -> scheme.tertiary               // Rail
    "M" -> scheme.secondary              // MAX Light Rail
    "W" -> scheme.outline                // WES (alt)
    else -> scheme.primary
}

/** Returns the M3 on-color that pairs with [wearTransitColor] for a transit type. */
internal fun wearTransitOnColor(type: String?, scheme: ColorScheme): Color = when (type) {
    "R" -> scheme.onTertiary              // Rail
    "M" -> scheme.onSecondary             // MAX Light Rail
    "W" -> Color.White                    // WES: unchanged for now
    else -> scheme.onPrimary              // Bus, Streetcar
}

/** Returns the drawable resource ID for a transit-type icon. */
@DrawableRes
internal fun wearTransitIconResource(type: String?): Int = when (type) {
    "B" -> R.drawable.ic_transit_bus
    "M", "R" -> R.drawable.ic_transit_rail
    "S", "T" -> R.drawable.ic_transit_streetcar
    "W" -> R.drawable.ic_transit_rail
    else -> R.drawable.ic_transit_bus
}

/** Colored circle badge with a transit glyph, used as the vehicle/bus marker image. */
internal fun wearBadgeBitmap(
    context: Context,
    fillColor: Int,
    glyphRes: Int,
    density: Float,
    glyphColor: Int = android.graphics.Color.WHITE
): Bitmap {
    val size = (34 * density).toInt()
    val out = createBitmap(size, size, Bitmap.Config.ARGB_8888)
    val c = Canvas(out)
    c.drawCircle(
        size / 2f,
        size / 2f,
        size / 2f,
        Paint(Paint.ANTI_ALIAS_FLAG).apply { this.color = fillColor })
    val glyphSize = (20 * density).toInt()
    val tinted = ContextCompat.getDrawable(context, glyphRes)
        ?.mutate()
        ?.apply { setTint(glyphColor) }
    val glyph = tinted?.toBitmap(glyphSize, glyphSize, Bitmap.Config.ARGB_8888)
    if (glyph != null) {
        c.drawBitmap(glyph, (size - glyph.width) / 2f, (size - glyph.height) / 2f, null)
    }
    return out
}

/** Primary-colored dot with a contrasting center, used as the stop marker image. */
internal fun wearStopDotBitmap(
    context: Context,
    fillColor: Int,
    centerColor: Int = android.graphics.Color.WHITE,
    density: Float
): Bitmap {
    val size = (34 * density).toInt()
    val out = createBitmap(size, size, Bitmap.Config.ARGB_8888)
    val c = Canvas(out)
    c.drawCircle(
        size / 2f,
        size / 2f,
        size / 2f,
        Paint(Paint.ANTI_ALIAS_FLAG).apply { this.color = fillColor })
    val dotRadius = (6 * density).toInt().toFloat()
    c.drawCircle(
        size / 2f,
        size / 2f,
        dotRadius,
        Paint(Paint.ANTI_ALIAS_FLAG).apply { this.color = centerColor })
    return out
}

/** The bus to follow: the tracked vehicle, else the first live position on that route. */
internal fun wearTrackedTarget(
    blockPositions: List<BlockPosition>,
    trackedVehicleId: Int
): LatLng? {
    val valid = blockPositions.filter { it.lat != 0.0 || it.lng != 0.0 }
    if (valid.isEmpty()) return null
    return valid.firstOrNull { it.vehicleID == trackedVehicleId }
        ?.let { LatLng(it.lat, it.lng) }
        ?: LatLng(valid.first().lat, valid.first().lng)
}

/**
 * Pans the camera back onto the bus only when it drifts outside a centered band.
 * The top margin is larger so the "N min" label above the icon stays on screen;
 * user zoom is preserved and the stop is no longer kept in frame.
 */
internal fun wearKeepBusCentered(
    map: MapLibreMap,
    target: LatLng,
    viewWidth: Int,
    viewHeight: Int,
    density: Float
) {
    val marginPx = (24 * density).toInt()
    val topMarginPx = (72 * density).toInt()
    val p = map.projection.toScreenLocation(target)
    val outside = p.x < marginPx || p.x > viewWidth - marginPx ||
        p.y < topMarginPx || p.y > viewHeight - marginPx
    if (outside) {
        map.easeCamera(CameraUpdateFactory.newLatLng(target), 400)
    }
}