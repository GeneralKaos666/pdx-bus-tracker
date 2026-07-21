package com.mikepenz.iconics;

import android.content.Context;
import android.graphics.Canvas;
import android.graphics.ColorFilter;
import android.graphics.Paint;
import android.graphics.PixelFormat;
import android.graphics.Rect;
import android.graphics.drawable.Drawable;
import androidx.annotation.ColorInt;
import androidx.annotation.Dimension;

/**
 * Minimal stub — renders a filled circle with the icon's initial letter.
 * Replaces the full Mikepenz IconicsDrawable which requires unavailable typeface assets.
 */
public class IconicsDrawable extends Drawable {

    private final Paint paint;
    private int sizePx;
    private IIcon icon;

    public IconicsDrawable(Context context) {
        this.paint = new Paint(Paint.ANTI_ALIAS_FLAG);
        this.paint.setStyle(Paint.Style.FILL);
        this.sizePx = context.getResources().getDimensionPixelSize(
                android.R.dimen.app_icon_size);
    }

    public IconicsDrawable icon(IIcon icon) {
        this.icon = icon;
        invalidateSelf();
        return this;
    }

    public IconicsDrawable sizeDp(@Dimension(unit = Dimension.DP) int dp) {
        this.sizePx = (int) (dp * 3); // rough px approximation
        invalidateSelf();
        return this;
    }

    public IconicsDrawable color(@ColorInt int color) {
        paint.setColor(color);
        invalidateSelf();
        return this;
    }

    @Override
    public void draw(Canvas canvas) {
        Rect bounds = getBounds();
        float cx = bounds.exactCenterX();
        float cy = bounds.exactCenterY();
        float radius = Math.min(bounds.width(), bounds.height()) / 2f;
        canvas.drawCircle(cx, cy, radius, paint);
    }

    @Override
    public void setAlpha(int alpha) {
        paint.setAlpha(alpha);
    }

    @Override
    public void setColorFilter(ColorFilter colorFilter) {
        paint.setColorFilter(colorFilter);
    }

    @Override
    public int getOpacity() {
        return PixelFormat.OPAQUE;
    }
}
