package com.trimettransit.tracker.feature.trips

import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Paint
import androidx.core.graphics.createBitmap

/**
 * Map marker artwork for the trip planner. Everything is drawn here from the current M3
 * scheme colors so the markers follow theming and no TriMet artwork is used. Sizes are in
 * dp so text scale factors (user font scaling) don't inflate the markers.
 */

/** "Trip origin" marker: a filled circle with a white center dot and dark outline. */
internal fun originDotBitmap(fillColor: Int, density: Float): Bitmap {
    val size = (48 * density).toInt()
    val out = createBitmap(size, size, Bitmap.Config.ARGB_8888)
    val c = Canvas(out)
    val center = size / 2f
    c.drawCircle(center, center, size / 2f, Paint(Paint.ANTI_ALIAS_FLAG).apply {
        this.color = android.graphics.Color.BLACK
    })
    c.drawCircle(
        center, center, size / 2f - (2 * density),
        Paint(Paint.ANTI_ALIAS_FLAG).apply { this.color = fillColor }
    )
    c.drawCircle(
        center, center, (9 * density).toInt().toFloat(),
        Paint(Paint.ANTI_ALIAS_FLAG).apply { this.color = android.graphics.Color.WHITE }
    )
    return out
}

/** "Trip destination" marker: a filled circle with a white ring and a white center dot. */
internal fun destDotBitmap(fillColor: Int, density: Float): Bitmap {
    val size = (48 * density).toInt()
    val out = createBitmap(size, size, Bitmap.Config.ARGB_8888)
    val c = Canvas(out)
    val center = size / 2f
    c.drawCircle(center, center, size / 2f, Paint(Paint.ANTI_ALIAS_FLAG).apply {
        this.color = android.graphics.Color.BLACK
    })
    c.drawCircle(
        center, center, size / 2f - (2 * density),
        Paint(Paint.ANTI_ALIAS_FLAG).apply { this.color = fillColor }
    )
    c.drawCircle(
        center, center, (15 * density).toInt().toFloat(),
        Paint(Paint.ANTI_ALIAS_FLAG).apply { this.color = android.graphics.Color.WHITE }
    )
    c.drawCircle(
        center, center, (9 * density).toInt().toFloat(),
        Paint(Paint.ANTI_ALIAS_FLAG).apply { this.color = fillColor }
    )
    return out
}

/** Small secondary-colored dot for a leg boarding/alighting point. */
internal fun stopDotBitmap(fillColor: Int, centerColor: Int, density: Float): Bitmap {
    val size = (36 * density).toInt()
    val out = createBitmap(size, size, Bitmap.Config.ARGB_8888)
    val c = Canvas(out)
    val center = size / 2f
    c.drawCircle(center, center, size / 2f, Paint(Paint.ANTI_ALIAS_FLAG).apply {
        this.color = android.graphics.Color.BLACK
    })
    c.drawCircle(
        center, center, size / 2f - (2 * density),
        Paint(Paint.ANTI_ALIAS_FLAG).apply { this.color = fillColor }
    )
    c.drawCircle(
        center, center, (6 * density).toInt().toFloat(),
        Paint(Paint.ANTI_ALIAS_FLAG).apply { this.color = centerColor }
    )
    return out
}

/** "You are here" marker: a primary-colored dot with a white ring inside a translucent halo. */
internal fun meDotBitmap(fillColor: Int, density: Float): Bitmap {
    val size = (56 * density).toInt()
    val out = createBitmap(size, size, Bitmap.Config.ARGB_8888)
    val c = Canvas(out)
    val center = size / 2f
    val halo = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        this.color = (fillColor and 0x00FFFFFF) or (0x40 shl 24)
    }
    c.drawCircle(center, center, size / 2f, halo)
    c.drawCircle(
        center, center, (13 * density).toInt().toFloat(),
        Paint(Paint.ANTI_ALIAS_FLAG).apply { this.color = android.graphics.Color.WHITE }
    )
    c.drawCircle(
        center, center, (10 * density).toInt().toFloat(),
        Paint(Paint.ANTI_ALIAS_FLAG).apply { this.color = fillColor }
    )
    return out
}