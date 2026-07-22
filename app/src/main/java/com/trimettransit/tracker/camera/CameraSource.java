package com.trimettransit.tracker.camera;

import android.annotation.SuppressLint;
import android.content.Context;
import android.graphics.ImageFormat;
import android.hardware.Camera;
import android.os.Build;
import android.os.SystemClock;
import android.util.Log;
import android.view.SurfaceHolder;
import android.view.WindowManager;
import java.nio.ByteBuffer;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

public class CameraSource {

        private Context f4836a;

        private final Object f4837b;

        private Camera f4838c;

        private int f4839d;

        private int f4840e;

        private com.google.android.gms.common.n.a f4841f;
    private float g;
    private int h;
    private int i;
    private String j;
    private String k;
    private Thread l;
    private d m;
    private Map<byte[], ByteBuffer> n;

            private class c implements Camera.PreviewCallback {
        private c() {
        }

        @Override // android.hardware.Camera.PreviewCallback
        public void onPreviewFrame(byte[] bArr, Camera camera) {
            CameraSource.this.m.a(bArr, camera);
        }
    }

            private static class e {

                private com.google.android.gms.common.n.a f4850a;

                private com.google.android.gms.common.n.a f4851b;

        public e(Camera.Size size, Camera.Size size2) {
            this.f4850a = new com.google.android.gms.common.n.a(size.width, size.height);
            if (size2 != null) {
                this.f4851b = new com.google.android.gms.common.n.a(size2.width, size2.height);
            }
        }

        public com.google.android.gms.common.n.a a() {
            return this.f4851b;
        }

        public com.google.android.gms.common.n.a b() {
            return this.f4850a;
        }
    }

            public static class b {

                private final Object f4842a;

        private static com.google.android.gms.vision.a<?> castDetector(Object o) {
            return (com.google.android.gms.vision.a<?>) o;
        }

                private CameraSource f4843b = new CameraSource();

        public b(Context context, Object aVar) {
            if (context == null) {
                throw new IllegalArgumentException("No context supplied.");
            }
            if (aVar == null) {
                throw new IllegalArgumentException("No detector supplied.");
            }
            this.f4842a = aVar;
            this.f4843b.f4836a = context;
        }

        public b a(float f2) {
            if (f2 > 0.0f) {
                this.f4843b.g = f2;
                return this;
            }
            throw new IllegalArgumentException("Invalid fps: " + f2);
        }

        public b a(String str) {
            this.f4843b.j = str;
            return this;
        }

        public b a(int i, int i2) {
            if (i > 0 && i <= 1000000 && i2 > 0 && i2 <= 1000000) {
                this.f4843b.h = i;
                this.f4843b.i = i2;
                return this;
            }
            throw new IllegalArgumentException("Invalid preview size: " + i + "x" + i2);
        }

        public b a(int i) {
            if (i == 0 || i == 1) {
                this.f4843b.f4839d = i;
                return this;
            }
            throw new IllegalArgumentException("Invalid camera: " + i);
        }

        public CameraSource a() {
            CameraSource cameraSource = this.f4843b;
            cameraSource.getClass();
            cameraSource.m = cameraSource.new d(this.f4842a);
            return this.f4843b;
        }
    }

            private class d implements Runnable {

        private Object f4845b;

        
        private ByteBuffer h;

                private long f4846c = SystemClock.elapsedRealtime();

                private final Object f4847d = new Object();

                private boolean f4848e = true;
        private int g = 0;
        private long f4849f = 0;

        d(Object aVar) {
            this.f4845b = aVar;
        }

        @SuppressLint({"Assert"})
        void a() {
            ((com.google.android.gms.vision.a)this.f4845b).b();
            this.f4845b = null;
        }

        @Override // java.lang.Runnable
        public void run() {
            com.google.android.gms.vision.b bVarA;
            ByteBuffer byteBuffer;
            while (true) {
                synchronized (this.f4847d) {
                    while (this.f4848e && this.h == null) {
                        try {
                            this.f4847d.wait();
                        } catch (InterruptedException e2) {
                            Log.d("OpenCameraSource", "Frame processing loop terminated.", e2);
                            return;
                        }
                    }
                    if (!this.f4848e) {
                        return;
                    }
                    com.google.android.gms.vision.b.a aVar = new com.google.android.gms.vision.b.a();
                    aVar.a(this.h.array(), CameraSource.this.f4841f.b(), CameraSource.this.f4841f.a(), 17);
                    aVar.a(this.g);
                    aVar.a(this.f4849f);
                    aVar.b(CameraSource.this.f4840e);
                    bVarA = aVar.a();
                    byteBuffer = this.h;
                    this.h = null;
                }
                try {
                    ((com.google.android.gms.vision.a<?>)this.f4845b).b(bVarA);
                    CameraSource.this.f4838c.addCallbackBuffer(byteBuffer.array());
                } catch (Throwable th) {
                    try {
                        Log.e("OpenCameraSource", "Exception thrown from receiver.", th);
                        CameraSource.this.f4838c.addCallbackBuffer(byteBuffer.array());
                    } catch (Throwable th2) {
                        CameraSource.this.f4838c.addCallbackBuffer(byteBuffer.array());
                        throw th2;
                    }
                }
            }
        }

        void a(boolean z) {
            synchronized (this.f4847d) {
                this.f4848e = z;
                this.f4847d.notifyAll();
            }
        }

        void a(byte[] bArr, Camera camera) {
            synchronized (this.f4847d) {
                if (this.h != null) {
                    camera.addCallbackBuffer(this.h.array());
                    this.h = null;
                }
                if (!CameraSource.this.n.containsKey(bArr)) {
                    Log.d("OpenCameraSource", "Skipping frame.  Could not find ByteBuffer associated with the image data from the camera.");
                    return;
                }
                this.f4849f = SystemClock.elapsedRealtime() - this.f4846c;
                this.g++;
                this.h = (ByteBuffer) CameraSource.this.n.get(bArr);
                this.f4847d.notifyAll();
            }
        }
    }

    private CameraSource() {
        this.f4837b = new Object();
        this.f4839d = 0;
        this.g = 30.0f;
        this.h = 1024;
        this.i = 768;
        this.j = null;
        this.k = null;
        this.n = new HashMap();
    }

    @SuppressLint({"InlinedApi"})
    private Camera e() {
        int iA = a(this.f4839d);
        if (iA == -1) {
            throw new RuntimeException("Could not find requested camera.");
        }
        Camera cameraOpen = Camera.open(iA);
        e eVarA = a(cameraOpen, this.h, this.i);
        if (eVarA == null) {
            throw new RuntimeException("Could not find suitable preview size.");
        }
        com.google.android.gms.common.n.a aVarA = eVarA.a();
        this.f4841f = eVarA.b();
        int[] iArrA = a(cameraOpen, this.g);
        if (iArrA == null) {
            throw new RuntimeException("Could not find suitable preview frames per second range.");
        }
        Camera.Parameters parameters = cameraOpen.getParameters();
        if (aVarA != null) {
            parameters.setPictureSize(aVarA.b(), aVarA.a());
        }
        parameters.setPreviewSize(this.f4841f.b(), this.f4841f.a());
        parameters.setPreviewFpsRange(iArrA[0], iArrA[1]);
        parameters.setPreviewFormat(17);
        a(cameraOpen, parameters, iA);
        if (this.j != null) {
            if (parameters.getSupportedFocusModes().contains(this.j)) {
                parameters.setFocusMode(this.j);
            } else {
                Log.i("OpenCameraSource", "Camera focus mode: " + this.j + " is not supported on this device.");
            }
        }
        this.j = parameters.getFocusMode();
        if (this.k != null) {
            if (parameters.getSupportedFlashModes().contains(this.k)) {
                parameters.setFlashMode(this.k);
            } else {
                Log.i("OpenCameraSource", "Camera flash mode: " + this.k + " is not supported on this device.");
            }
        }
        this.k = parameters.getFlashMode();
        cameraOpen.setParameters(parameters);
        cameraOpen.setPreviewCallbackWithBuffer(new c());
        cameraOpen.addCallbackBuffer(a(this.f4841f));
        cameraOpen.addCallbackBuffer(a(this.f4841f));
        cameraOpen.addCallbackBuffer(a(this.f4841f));
        cameraOpen.addCallbackBuffer(a(this.f4841f));
        return cameraOpen;
    }

    /* JADX WARN: Code duplicated, block: B:13:0x0026 A[Catch: all -> 0x0062, TRY_LEAVE, TryCatch #1 {, blocks: (B:4:0x0003, B:7:0x000e, B:10:0x001b, B:11:0x001d, B:13:0x0026, B:14:0x0030, B:16:0x0036, B:21:0x0059, B:17:0x003c, B:20:0x0043, B:22:0x0060, B:9:0x0014), top: B:29:0x0003, inners: #0, #2 }] */
    /* JADX WARN: Code duplicated, block: B:16:0x0036 A[Catch: Exception -> 0x0042, all -> 0x0062, TryCatch #2 {Exception -> 0x0042, blocks: (B:14:0x0030, B:16:0x0036, B:17:0x003c), top: B:32:0x0030, outer: #1 }] */
    /* JADX WARN: Code duplicated, block: B:17:0x003c A[Catch: Exception -> 0x0042, all -> 0x0062, TRY_LEAVE, TryCatch #2 {Exception -> 0x0042, blocks: (B:14:0x0030, B:16:0x0036, B:17:0x003c), top: B:32:0x0030, outer: #1 }] */
    public void d() {
        synchronized (this.f4837b) {
            this.m.a(false);
            if (this.l != null) {
                try {
                    this.l.join();
                } catch (InterruptedException unused) {
                    Log.d("OpenCameraSource", "Frame processing thread interrupted on release.");
                }
                this.l = null;
                this.n.clear();
                if (this.f4838c != null) {
                    this.f4838c.stopPreview();
                    this.f4838c.setPreviewCallbackWithBuffer(null);
                    try {
                        if (Build.VERSION.SDK_INT >= 11) {
                            this.f4838c.setPreviewTexture(null);
                        } else {
                            this.f4838c.setPreviewDisplay(null);
                        }
                    } catch (Exception e2) {
                        Log.e("OpenCameraSource", "Failed to clear camera preview: " + e2);
                    }
                    this.f4838c.release();
                    this.f4838c = null;
                }
            } else {
                this.n.clear();
                if (this.f4838c != null) {
                    this.f4838c.stopPreview();
                    this.f4838c.setPreviewCallbackWithBuffer(null);
                    if (Build.VERSION.SDK_INT >= 11) {
                        try {
                            this.f4838c.setPreviewTexture(null);
                        } catch (java.io.IOException e) {
                            // Ignore
                        }
                    } else {
                        try {
                            this.f4838c.setPreviewDisplay(null);
                        } catch (java.io.IOException e) {
                            // Ignore
                        }
                    }
                    this.f4838c.release();
                    this.f4838c = null;
                }
            }
        }
    }

    public com.google.android.gms.common.n.a b() {
        return this.f4841f;
    }

    public void c() {
        synchronized (this.f4837b) {
            d();
            this.m.a();
        }
    }

    public CameraSource a(SurfaceHolder surfaceHolder) {
        synchronized (this.f4837b) {
            if (this.f4838c != null) {
                return this;
            }
            this.f4838c = e();
            try {
                this.f4838c.setPreviewDisplay(surfaceHolder);
            } catch (java.io.IOException e) {
                throw new RuntimeException(e);
            }
            this.f4838c.startPreview();
            this.l = new Thread(this.m);
            this.m.a(true);
            this.l.start();
            return this;
        }
    }

    public int a() {
        return this.f4839d;
    }

    public boolean a(String str) {
        synchronized (this.f4837b) {
            if (this.f4838c != null && str != null) {
                Camera.Parameters parameters = this.f4838c.getParameters();
                if (parameters.getSupportedFlashModes().contains(str)) {
                    parameters.setFlashMode(str);
                    this.f4838c.setParameters(parameters);
                    this.k = str;
                    return true;
                }
            }
            return false;
        }
    }

    private static int a(int i) {
        Camera.CameraInfo cameraInfo = new Camera.CameraInfo();
        for (int i2 = 0; i2 < Camera.getNumberOfCameras(); i2++) {
            Camera.getCameraInfo(i2, cameraInfo);
            if (cameraInfo.facing == i) {
                return i2;
            }
        }
        return -1;
    }

    private static e a(Camera camera, int i, int i2) {
        e eVar = null;
        int i3 = Integer.MAX_VALUE;
        for (e eVar2 : a(camera)) {
            com.google.android.gms.common.n.a aVarB = eVar2.b();
            int iAbs = Math.abs(aVarB.b() - i) + Math.abs(aVarB.a() - i2);
            if (iAbs < i3) {
                eVar = eVar2;
                i3 = iAbs;
            }
        }
        return eVar;
    }

    private static List<e> a(Camera camera) {
        Camera.Parameters parameters = camera.getParameters();
        List<Camera.Size> supportedPreviewSizes = parameters.getSupportedPreviewSizes();
        List<Camera.Size> supportedPictureSizes = parameters.getSupportedPictureSizes();
        ArrayList arrayList = new ArrayList();
        for (Camera.Size size : supportedPreviewSizes) {
            float f2 = size.width / size.height;
            for (Camera.Size size2 : supportedPictureSizes) {
                if (Math.abs(f2 - (size2.width / size2.height)) < 0.01f) {
                    arrayList.add(new e(size, size2));
                    break;
                }
            }
        }
        if (arrayList.size() == 0) {
            Log.w("OpenCameraSource", "No preview sizes have a corresponding same-aspect-ratio picture size");
            Iterator<Camera.Size> it = supportedPreviewSizes.iterator();
            while (it.hasNext()) {
                arrayList.add(new e(it.next(), null));
            }
        }
        return arrayList;
    }

    private int[] a(Camera camera, float f2) {
        int i = (int) (f2 * 1000.0f);
        int[] iArr = null;
        int i2 = Integer.MAX_VALUE;
        for (int[] iArr2 : camera.getParameters().getSupportedPreviewFpsRange()) {
            int iAbs = Math.abs(i - iArr2[0]) + Math.abs(i - iArr2[1]);
            if (iAbs < i2) {
                iArr = iArr2;
                i2 = iAbs;
            }
        }
        return iArr;
    }

    private void a(Camera camera, Camera.Parameters parameters, int i) {
        int i2;
        int i3;
        int rotation = ((WindowManager) this.f4836a.getSystemService(Context.WINDOW_SERVICE)).getDefaultDisplay().getRotation();
        int i4 = 0;
        if (rotation != 0) {
            if (rotation == 1) {
                i4 = 90;
            } else if (rotation == 2) {
                i4 = 180;
            } else if (rotation != 3) {
                Log.e("OpenCameraSource", "Bad rotation value: " + rotation);
            } else {
                i4 = 270;
            }
        }
        Camera.CameraInfo cameraInfo = new Camera.CameraInfo();
        Camera.getCameraInfo(i, cameraInfo);
        if (cameraInfo.facing == 1) {
            i2 = (cameraInfo.orientation + i4) % 360;
            i3 = 360 - i2;
        } else {
            i2 = ((cameraInfo.orientation - i4) + 360) % 360;
            i3 = i2;
        }
        this.f4840e = i2 / 90;
        camera.setDisplayOrientation(i3);
        parameters.setRotation(i2);
    }

    private byte[] a(com.google.android.gms.common.n.a aVar) {
        double dA = aVar.a() * aVar.b() * ImageFormat.getBitsPerPixel(ImageFormat.NV21);
        Double.isNaN(dA);
        byte[] bArr = new byte[((int) Math.ceil(dA / 8.0d)) + 1];
        ByteBuffer byteBufferWrap = ByteBuffer.wrap(bArr);
        if (byteBufferWrap.hasArray() && byteBufferWrap.array() == bArr) {
            this.n.put(bArr, byteBufferWrap);
            return bArr;
        }
        throw new IllegalStateException("Failed to create valid buffer for camera source.");
    }
}
