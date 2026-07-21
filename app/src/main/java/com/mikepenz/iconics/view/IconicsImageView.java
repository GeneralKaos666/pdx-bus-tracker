package com.mikepenz.iconics.view;

import android.content.Context;
import android.graphics.drawable.Drawable;
import android.util.AttributeSet;
import androidx.appcompat.widget.AppCompatImageView;
import com.mikepenz.iconics.IIcon;
import com.mikepenz.iconics.IconicsDrawable;

/** Minimal stub — wraps IconicsDrawable to display icons. */
public class IconicsImageView extends AppCompatImageView {

    private IIcon icon;

    public IconicsImageView(Context context) {
        super(context);
    }

    public IconicsImageView(Context context, AttributeSet attrs) {
        super(context, attrs);
    }

    public IconicsImageView(Context context, AttributeSet attrs, int defStyle) {
        super(context, attrs, defStyle);
    }

    public void setIcon(IIcon icon) {
        this.icon = icon;
        IconicsDrawable d = new IconicsDrawable(getContext());
        d.icon(icon);
        d.color(0xFF888888); // default gray
        setImageDrawable(d);
    }

    public IIcon getIcon() {
        return icon;
    }
}
