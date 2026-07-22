package com.trimettransit.tracker.camera;

import android.content.Context;
import android.graphics.Canvas;
import android.util.AttributeSet;
import android.view.View;
import com.trimettransit.tracker.camera.GraphicOverlay.a;
import java.util.HashSet;
import java.util.Iterator;
import java.util.Set;

public class GraphicOverlay<T extends a> extends View {

        private final Object f4831b;

        private int f4832c;

        private int f4833d;

        private Set<T> f4834e;

        private T f4835f;

    public static abstract class a {
        public abstract void a(Canvas canvas);
    }

    public GraphicOverlay(Context context, AttributeSet attributeSet) {
        super(context, attributeSet);
        this.f4831b = new Object();
        this.f4834e = new HashSet();
    }

    public void a() {
        synchronized (this.f4831b) {
            this.f4834e.clear();
            this.f4835f = null;
        }
        postInvalidate();
    }

    public T getFirstGraphic() {
        T t;
        synchronized (this.f4831b) {
            t = this.f4835f;
        }
        return t;
    }

    @Override // android.view.View
    protected void onDraw(Canvas canvas) {
        super.onDraw(canvas);
        synchronized (this.f4831b) {
            if (this.f4832c != 0 && this.f4833d != 0) {
                canvas.getWidth();
                canvas.getHeight();
            }
            Iterator<T> it = this.f4834e.iterator();
            while (it.hasNext()) {
                it.next().a(canvas);
            }
        }
    }

    public void a(int i, int i2, int i3) {
        synchronized (this.f4831b) {
            this.f4832c = i;
            this.f4833d = i2;
        }
        postInvalidate();
    }
}
