package com.something15525.trimetgo.trimet_go.activities.qr_scanning;

import androidx.appcompat.widget.Toolbar;
import android.view.View;
import butterknife.Unbinder;
import butterknife.internal.Utils;
import com.something15525.trimetgo.trimet_go.R;
import com.something15525.trimetgo.trimet_go.camera.CameraSourcePreview;
import com.something15525.trimetgo.trimet_go.camera.GraphicOverlay;

public class QRScanningActivity_ViewBinding implements Unbinder {

        private QRScanningActivity f4787a;

    public QRScanningActivity_ViewBinding(QRScanningActivity qRScanningActivity, View view) {
        this.f4787a = qRScanningActivity;
        qRScanningActivity.toolbar = (Toolbar) Utils.findRequiredViewAsType(view, R.id.activity_qr_scanning_toolbar, "field 'toolbar'", Toolbar.class);
        qRScanningActivity.cameraGraphicOverlay = (GraphicOverlay) Utils.findRequiredViewAsType(view, R.id.activity_qr_scanning_graphic_overlay, "field 'cameraGraphicOverlay'", GraphicOverlay.class);
        qRScanningActivity.cameraSourcePreview = (CameraSourcePreview) Utils.findRequiredViewAsType(view, R.id.activity_qr_scanning_preview, "field 'cameraSourcePreview'", CameraSourcePreview.class);
    }

    @Override // butterknife.Unbinder
    public void unbind() {
        QRScanningActivity qRScanningActivity = this.f4787a;
        if (qRScanningActivity == null) {
            throw new IllegalStateException("Bindings already cleared.");
        }
        this.f4787a = null;
        qRScanningActivity.toolbar = null;
        qRScanningActivity.cameraGraphicOverlay = null;
        qRScanningActivity.cameraSourcePreview = null;
    }
}
