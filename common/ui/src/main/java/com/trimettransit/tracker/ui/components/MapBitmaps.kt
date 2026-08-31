package com.trimettransit.tracker.ui.components

import android.content.Context
import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Paint
import androidx.core.content.ContextCompat
import androidx.core.graphics.createBitmap
import androidx.core.graphics.drawable.toBitmap

/** Renders a drawable resource into a square ARGB_8888 bitmap for map marker images. */
fun drawableBitmap(context: Context, resId: Int, sizePx: Int): Bitmap {
    val d = ContextCompat.getDrawable(context, resId)
    return d?.toBitmap(sizePx, sizePx, Bitmap.Config.ARGB_8888)
        ?: createBitmap(1, 1, Bitmap.Config.ARGB_8888)
}

/**
 * Colored circle badge with a transit glyph, used as the vehicle/bus marker image. The glyph is
 * tinted [glyphColor] (defaults to white) so it stays legible against the badge fill in both the
 * light and dark map styles — pair it with the matching M3 on-color for the given transit type.
 */
fun badgeBitmap(
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
