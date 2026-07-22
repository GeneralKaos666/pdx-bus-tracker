package com.trimettransit.tracker.camera;

import android.content.Context;
import android.util.AttributeSet;
import android.util.Log;
import android.view.SurfaceHolder;
import android.view.SurfaceView;
import android.view.ViewGroup;
import java.io.IOException;

public class CameraSourcePreview extends ViewGroup {

        private Context f4825b;

        private SurfaceView f4826c;

        private boolean f4827d;

        private boolean f4828e;

        private CameraSource f4829f;
    private GraphicOverlay g;

    private class b implements SurfaceHolder.Callback {
        private b() {
        }

        @Override // android.view.SurfaceHolder.Callback
        public void surfaceChanged(SurfaceHolder surfaceHolder, int i, int i2, int i3) {
        }

        @Override // android.view.SurfaceHolder.Callback
        public void surfaceCreated(SurfaceHolder surfaceHolder) {
            CameraSourcePreview.this.f4828e = true;
            try {
                CameraSourcePreview.this.d();
            } catch (SecurityException e3) {
                Log.e("CameraSourcePreview", "Do not have permission to start the camera", e3);
            } catch (RuntimeException e4) {
                Log.e("CameraSourcePreview", "Could not start camera source.", e4);
            }
        }

        @Override // android.view.SurfaceHolder.Callback
        public void surfaceDestroyed(SurfaceHolder surfaceHolder) {
            CameraSourcePreview.this.f4828e = false;
        }
    }

    public CameraSourcePreview(Context context, AttributeSet attributeSet) {
        super(context, attributeSet);
        this.f4825b = context;
        this.f4827d = false;
        this.f4828e = false;
        this.f4826c = new SurfaceView(context);
        this.f4826c.getHolder().addCallback(new b());
        addView(this.f4826c);
    }

    private boolean c() {
        int i = this.f4825b.getResources().getConfiguration().orientation;
        if (i == 2) {
            return false;
        }
        if (i == 1) {
            return true;
        }
        Log.d("CameraSourcePreview", "isPortraitMode returning false by default");
        return false;
    }

        public void d() {
        if (this.f4827d && this.f4828e) {
            this.f4829f.a(this.f4826c.getHolder());
            if (this.g != null) {
                com.google.android.gms.common.n.a aVarB = this.f4829f.b();
                int iMin = Math.min(aVarB.b(), aVarB.a());
                int iMax = Math.max(aVarB.b(), aVarB.a());
                if (c()) {
                    this.g.a(iMin, iMax, this.f4829f.a());
                } else {
                    this.g.a(iMax, iMin, this.f4829f.a());
                }
                this.g.a();
            }
            this.f4827d = false;
        }
    }

    public void b() {
        CameraSource cameraSource = this.f4829f;
        if (cameraSource != null) {
            cameraSource.d();
        }
    }

    @Override // android.view.ViewGroup, android.view.View
    protected void onLayout(boolean z, int i, int i2, int i3, int i4) {
        int iB;
        int iA;
        com.google.android.gms.common.n.a aVarB;
        CameraSource cameraSource = this.f4829f;
        if (cameraSource == null || (aVarB = cameraSource.b()) == null) {
            iB = 320;
            iA = 240;
        } else {
            iB = aVarB.b();
            iA = aVarB.a();
        }
        if (!c()) {
            int i5 = iB;
            iB = iA;
            iA = i5;
        }
        int i6 = i3 - i;
        int i7 = i4 - i2;
        float f2 = iA;
        float f3 = iB;
        int i8 = (int) ((i6 / f2) * f3);
        if (i8 > i7) {
            i6 = (int) ((i7 / f3) * f2);
            i8 = i7;
        }
        for (int i9 = 0; i9 < getChildCount(); i9++) {
            getChildAt(i9).layout(0, 0, i6, i8);
        }
        try {
            d();
        } catch (SecurityException e3) {
            Log.e("CameraSourcePreview", "Do not have permission to start the camera", e3);
        } catch (RuntimeException e4) {
            Log.e("CameraSourcePreview", "Could not start camera source.", e4);
        }
    }

    public void a(CameraSource cameraSource) {
        if (cameraSource == null) {
            b();
        }
        this.f4829f = cameraSource;
        if (this.f4829f != null) {
            this.f4827d = true;
            d();
        }
    }

    public void a(CameraSource cameraSource, GraphicOverlay graphicOverlay) {
        this.g = graphicOverlay;
        a(cameraSource);
    }

    public void a() {
        CameraSource cameraSource = this.f4829f;
        if (cameraSource != null) {
            cameraSource.c();
            this.f4829f = null;
        }
    }
}
