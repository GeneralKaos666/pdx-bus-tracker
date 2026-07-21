package com.something15525.trimetgo.trimet_go.activities.qr_scanning;

import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import androidx.appcompat.app.AppCompatActivity;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.LinearLayout;
import android.widget.ProgressBar;
import android.widget.TextView;
import butterknife.BindView;
import butterknife.ButterKnife;
import com.mikepenz.iconics.view.IconicsImageView;
import com.mikepenz.ionicons_typeface_library.Ionicons;
import com.something15525.trimetgo.trimet_go.R;
import com.something15525.trimetgo.trimet_go.task.GetQRArrivalDataAsync;
import com.something15525.trimetgo.trimet_go.util.Constants2;
import com.something15525.trimetgo.trimet_go.data.model.Stop;
import com.something15525.trimetgo.trimet_go.util.ArrivalUtils;
import com.something15525.trimetgo.trimet_go.util.ConnectionUtils;
import com.something15525.trimetgo.trimet_go.util.SecurityUtils;
import java.io.IOException;
import java.net.URI;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.Response;
import java.util.concurrent.TimeUnit;

public class QRLoadingActivity extends AppCompatActivity {

        public static final String f4773e = QRLoadingActivity.class.getSimpleName();

    @BindView(R.id.activity_qr_loading_bar)
    ProgressBar loadingBar;

    @BindView(R.id.activity_qr_loading_close_button)
    Button loadingErrorCloseButton;

    @BindView(R.id.activity_qr_loading_error_image)
    IconicsImageView loadingErrorImageView;

    @BindView(R.id.activity_qr_loading_error_layout)
    LinearLayout loadingErrorLayout;

    @BindView(R.id.activity_qr_loading_error_text)
    TextView loadingErrorTextView;

    class ErrorCloseClickListener implements View.OnClickListener {
        ErrorCloseClickListener() {
        }

        @Override // android.view.View.OnClickListener
        public void onClick(View view) {
            QRLoadingActivity.this.finish();
        }
    }

    class QRLoaderThread extends Thread {

                final /* synthetic */ URI f4775b;

        class QRArrivalLoader extends GetQRArrivalDataAsync {
            QRArrivalLoader(Context context) {
                super(context);
            }

                        @Override // android.os.AsyncTask
                        public void onPostExecute(Stop stop) {
                QRLoadingActivity qRLoadingActivity = QRLoadingActivity.this;
                qRLoadingActivity.startActivity(ArrivalUtils.a(qRLoadingActivity, stop, true, -1));
                QRLoadingActivity.this.finish();
            }
        }

        QRLoaderThread(URI uri) {
            this.f4775b = uri;
        }

        @Override // java.lang.Thread, java.lang.Runnable
        public void run() {
            if (!SecurityUtils.isValidHttpsUri(this.f4775b) || !SecurityUtils.isAllowedQrHost(this.f4775b.getHost())) {
                Log.e(QRLoadingActivity.f4773e, "Rejected QR URI due to invalid scheme or host.");
                QRLoadingActivity.this.f();
                return;
            }
            if (!SecurityUtils.hasConfiguredTrimetApiKey()) {
                Log.e(QRLoadingActivity.f4773e, "TriMet API key is not configured.");
                QRLoadingActivity.this.f();
                return;
            }
            try {
                OkHttpClient client = new OkHttpClient.Builder()
                        .followRedirects(true)
                        .followSslRedirects(true)
                        .connectTimeout(10, TimeUnit.SECONDS)
                        .readTimeout(10, TimeUnit.SECONDS)
                        .build();
                Request request = new Request.Builder()
                        .url(this.f4775b.toURL())
                        .build();
                try (Response response = client.newCall(request).execute()) {
                    if (!response.isSuccessful()) {
                        throw new IOException("Unexpected code " + response.code());
                    }
                    okhttp3.HttpUrl finalUrl = response.request().url();
                    if (!"https".equalsIgnoreCase(finalUrl.scheme()) || !SecurityUtils.isAllowedQrHost(finalUrl.host())) {
                        Log.e(QRLoadingActivity.f4773e, "Rejected redirected QR URL host or scheme.");
                        QRLoadingActivity.this.f();
                        return;
                    }
                    String stopId = SecurityUtils.extractStopIdFromPath(finalUrl.encodedPath());
                    if (stopId == null) {
                        Log.e(QRLoadingActivity.f4773e, "Unable to parse locId from redirected URL.");
                        QRLoadingActivity.this.f();
                        return;
                    }
                    new QRArrivalLoader(QRLoadingActivity.this).execute(
                            QRLoadingActivity.this.getString(R.string.base_arrival_url) +
                                    "/appID/" + Constants2.getTrimetApiKey() +
                                    "/locIDs/" + stopId
                    );
                }
            } catch (IOException | IllegalStateException e3) {
                Log.e(QRLoadingActivity.f4773e, "Error performing redirect request: ", e3);
                QRLoadingActivity.this.f();
            }
        }
    }

    class TimeoutRunnable implements Runnable {
        TimeoutRunnable() {
        }

        @Override // java.lang.Runnable
        public void run() {
            QRLoadingActivity.this.loadingBar.setVisibility(View.GONE);
            QRLoadingActivity.this.loadingErrorLayout.setVisibility(View.VISIBLE);
            QRLoadingActivity.this.loadingErrorImageView.setIcon(Ionicons.Icon.ion_android_sad);
            QRLoadingActivity.this.loadingErrorTextView.setText(R.string.activity_qr_loading_qr_error_unrecognizable_text);
        }
    }

    class DismissLoadingRunnable implements Runnable {
        DismissLoadingRunnable() {
        }

        @Override // java.lang.Runnable
        public void run() {
            QRLoadingActivity.this.loadingBar.setVisibility(View.GONE);
            QRLoadingActivity.this.loadingErrorLayout.setVisibility(View.VISIBLE);
            QRLoadingActivity.this.loadingErrorImageView.setIcon(Ionicons.Icon.ion_outlet);
            QRLoadingActivity.this.loadingErrorTextView.setText(R.string.activity_qr_loading_qr_error_offline_text);
        }
    }

        public void f() {
        if (isFinishing()) {
            return;
        }
        runOnUiThread(new TimeoutRunnable());
    }

    private void g() {
        if (isFinishing()) {
            return;
        }
        runOnUiThread(new DismissLoadingRunnable());
    }

    @Override // android.support.v7.app.AppCompatActivity, android.support.v4.app.FragmentActivity, android.support.v4.app.ComponentActivity, android.app.Activity
    protected void onCreate(Bundle bundle) {
        super.onCreate(bundle);
        setContentView(R.layout.activity_qr_loading);
        ButterKnife.bind(this);
        this.loadingErrorCloseButton.setOnClickListener(new ErrorCloseClickListener());
        if (!ConnectionUtils.isOnline(this)) {
            g();
            return;
        }
        Bundle extras = getIntent().getExtras();
        if (extras == null) {
            Log.e(f4773e, "No extras passed in intent.");
            f();
            return;
        }
        String string = extras.getString("extra_raw_qr_data");
        if (string == null) {
            Log.e(f4773e, "No uri passed to activity: ");
            f();
            return;
        }
        try {
            new QRLoaderThread(URI.create(string)).start();
        } catch (IllegalArgumentException | NullPointerException e2) {
            Log.e(f4773e, "Error parsing raw uri: ", e2);
            f();
        }
    }

    public static Intent a(Context context, String str) {
        Intent intent = new Intent(context, (Class<?>) QRLoadingActivity.class);
        intent.putExtra("extra_raw_qr_data", str);
        return intent;
    }
}
