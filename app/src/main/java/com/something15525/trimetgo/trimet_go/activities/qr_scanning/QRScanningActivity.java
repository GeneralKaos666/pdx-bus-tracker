package com.something15525.trimetgo.trimet_go.activities.qr_scanning;

import androidx.core.content.ContextCompat;
import android.annotation.SuppressLint;
import android.app.Activity;
import android.content.DialogInterface;
import android.content.Intent;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import androidx.core.app.ActivityCompat;
import androidx.appcompat.app.ActionBar;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;
import android.util.Log;
import android.util.SparseArray;
import android.view.Menu;
import android.view.MenuItem;
import butterknife.BindView;
import butterknife.ButterKnife;
import com.google.android.gms.common.GoogleApiAvailability;
import com.google.android.gms.vision.Tracker;
import com.google.android.gms.vision.MultiProcessor;
import com.google.android.gms.vision.barcode.Barcode;
import com.google.android.gms.vision.barcode.BarcodeDetector;
import com.something15525.trimetgo.trimet_go.R;
import com.something15525.trimetgo.trimet_go.camera.CameraSource;
import com.something15525.trimetgo.trimet_go.camera.CameraSourcePreview;
import com.something15525.trimetgo.trimet_go.camera.GraphicOverlay;
import com.something15525.trimetgo.trimet_go.util.RequestCodeGenerator;
import java.io.IOException;

public class QRScanningActivity extends AppCompatActivity {
    private static final String g = QRScanningActivity.class.getSimpleName();
    private static final int h = RequestCodeGenerator.a();
    private static final int i = RequestCodeGenerator.a();
    private static boolean j = false;

    @BindView(R.id.activity_qr_scanning_graphic_overlay)
    GraphicOverlay<?> cameraGraphicOverlay;

    @BindView(R.id.activity_qr_scanning_preview)
    CameraSourcePreview cameraSourcePreview;

        private CameraSource f4781e;

        private MenuItem f4782f;

    @BindView(R.id.activity_qr_scanning_toolbar)
    Toolbar toolbar;

    class CloseClickListener implements DialogInterface.OnClickListener {
        CloseClickListener() {
        }

        @Override // android.content.DialogInterface.OnClickListener
        public void onClick(DialogInterface dialogInterface, int i) {
            QRScanningActivity.this.finish();
        }
    }

    class SettingsClickListener implements DialogInterface.OnClickListener {
        SettingsClickListener() {
        }

        @Override // android.content.DialogInterface.OnClickListener
        public void onClick(DialogInterface dialogInterface, int i) {
            QRScanningActivity.this.startActivity(new Intent("android.settings.APPLICATION_DETAILS_SETTINGS", Uri.parse("package:" + QRScanningActivity.this.getPackageName())));
            QRScanningActivity.this.finish();
        }
    }

    private class BarcodeTracker extends Tracker<Barcode> {
        private BarcodeTracker() {
        }

        /* synthetic */ BarcodeTracker(QRScanningActivity qRScanningActivity, CloseClickListener closeClickListener) {
            this();
        }

        @Override
        public void onNewItem(int i, Barcode barcode) {
            QRScanningActivity.this.a(barcode);
        }
    }

    private class BarcodeFactory implements MultiProcessor.Factory<Barcode> {
        private BarcodeFactory() {
        }

        /* synthetic */ BarcodeFactory(QRScanningActivity qRScanningActivity, CloseClickListener closeClickListener) {
            this();
        }

        @Override
        public Tracker<Barcode> create(Barcode barcode) {
            return new BarcodeTracker(QRScanningActivity.this, null);
        }
    }

    private void f() {
        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        builder.setTitle(R.string.activity_qr_scanning_camera_permission_dialog_title);
        builder.setMessage(R.string.activity_qr_scanning_camera_permission_dialog_message);
        builder.setPositiveButton(R.string.settings, new SettingsClickListener());
        builder.setNegativeButton(R.string.close, new CloseClickListener());
        builder.setCancelable(false);
        builder.show();
    }

    private void g() {
        int iC = GoogleApiAvailability.getInstance().isGooglePlayServicesAvailable(getApplicationContext());
        if (iC != 0) {
            GoogleApiAvailability.getInstance().getErrorDialog(this, iC, i).show();
        }
        CameraSource cameraSource = this.f4781e;
        if (cameraSource != null) {
            try {
                this.cameraSourcePreview.a(cameraSource, this.cameraGraphicOverlay);
            } catch (RuntimeException e2) {
                Log.e(g, "Unable to start camera source.", e2);
                this.f4781e.c();
                this.f4781e = null;
            }
        }
    }

    private void h() {
        CameraSourcePreview cameraSourcePreview = this.cameraSourcePreview;
        if (cameraSourcePreview != null) {
            cameraSourcePreview.b();
        }
    }

    private void i() {
        if (j) {
            MenuItem menuItem = this.f4782f;
            if (menuItem != null) {
                menuItem.setIcon(R.drawable.ic_flash_off_white_24dp);
            }
            this.f4781e.a("off");
        } else {
            MenuItem menuItem2 = this.f4782f;
            if (menuItem2 != null) {
                menuItem2.setIcon(R.drawable.ic_flash_on_white_24dp);
            }
            this.f4781e.a("torch");
        }
        j = !j;
    }

    @Override
    protected void onCreate(Bundle bundle) {
        super.onCreate(bundle);
        setContentView(R.layout.activity_qr_scanning);
        ButterKnife.bind(this);
        setSupportActionBar(this.toolbar);
        ActionBar actionBarC = getSupportActionBar();
        if (actionBarC != null) {
            actionBarC.setDisplayHomeAsUpEnabled(true);
            actionBarC.setHomeButtonEnabled(true);
        }
        if (ContextCompat.checkSelfPermission(this, "android.permission.CAMERA") == 0) {
            a(true, false);
            return;
        }
        String[] strArr = {"android.permission.CAMERA"};
        if (ActivityCompat.shouldShowRequestPermissionRationale((Activity) this, "android.permission.CAMERA")) {
            f();
        } else {
            ActivityCompat.requestPermissions(this, strArr, h);
        }
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(R.menu.qr_scanning, menu);
        this.f4782f = menu.findItem(R.id.action_flashlight_toggle);
        if (getPackageManager().hasSystemFeature("android.hardware.camera.flash")) {
            this.f4782f.setVisible(true);
        }
        return true;
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        CameraSourcePreview cameraSourcePreview = this.cameraSourcePreview;
        if (cameraSourcePreview != null) {
            cameraSourcePreview.a();
        }
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem menuItem) {
        int itemId = menuItem.getItemId();
        if (itemId == 16908332) {
            finish();
            return true;
        }
        if (itemId != R.id.action_flashlight_toggle) {
            return super.onOptionsItemSelected(menuItem);
        }
        i();
        return true;
    }

    @Override
    protected void onPause() {
        super.onPause();
        h();
    }

    @Override
    public void onRequestPermissionsResult(int i2, String[] strArr, int[] iArr) {
        if (i2 != h) {
            super.onRequestPermissionsResult(i2, strArr, iArr);
        } else if (iArr.length <= 0 || iArr[0] != 0) {
            f();
        } else {
            a(true, false);
        }
    }

    @Override
    protected void onResume() {
        super.onResume();
        g();
    }

    @SuppressLint({"InlinedApi"})
    private void a(boolean z, boolean z2) {
        BarcodeDetector.Builder builder = new BarcodeDetector.Builder(getApplicationContext());
        builder.setBarcodeFormats(Barcode.QR_CODE);
        BarcodeDetector barcodeDetector = builder.build();
        barcodeDetector.setProcessor(new MultiProcessor.Builder<>(new BarcodeFactory()).build());
        if (!barcodeDetector.isOperational()) {
            Log.e(g, "Barcode detector not operational.");
        }
        CameraSource.b bVar = new CameraSource.b(getApplicationContext(), barcodeDetector);
        bVar.a(0);
        bVar.a(1600, 1024);
        bVar.a(15.0f);
        if (Build.VERSION.SDK_INT >= 14) {
            bVar.a(z ? "continuous-picture" : null);
        }
        this.f4781e = bVar.a();
    }

        public void a(Barcode barcode) {
        startActivity(QRLoadingActivity.a(this, barcode.rawValue));
        finish();
    }
}
