package com.trimettransit.tracker.activities.qr_scanning;

import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.os.Bundle;

import com.trimettransit.tracker.R;
import com.trimettransit.tracker.activities.MainActivity;

/**
 * Bridge activity — receives QR scan result URI, passes it to MainActivity
 * which handles the loading and navigation in Compose.
 */
public class QRLoadingActivity extends Activity {
    public static final String EXTRA_QR_URI = "qr_uri";

    public static Intent a(Context context, String uri) {
        Intent intent = new Intent(context, QRLoadingActivity.class);
        intent.putExtra(EXTRA_QR_URI, uri);
        return intent;
    }

    @Override
    protected void onCreate(Bundle bundle) {
        super.onCreate(bundle);
        String qrUri = getIntent().getStringExtra(EXTRA_QR_URI);
        if (qrUri == null || qrUri.isEmpty()) {
            finish();
            return;
        }
        Intent mainIntent = new Intent(this, MainActivity.class);
        mainIntent.putExtra(EXTRA_QR_URI, qrUri);
        mainIntent.setFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP | Intent.FLAG_ACTIVITY_SINGLE_TOP);
        startActivity(mainIntent);
        finish();
    }
}
