package com.something15525.trimetgo.trimet_go.activities.qr_scanning;

import android.view.View;
import android.widget.Button;
import android.widget.LinearLayout;
import android.widget.ProgressBar;
import android.widget.TextView;
import butterknife.Unbinder;
import butterknife.internal.Utils;
import com.mikepenz.iconics.view.IconicsImageView;
import com.something15525.trimetgo.trimet_go.R;

public class QRLoadingActivity_ViewBinding implements Unbinder {

        private QRLoadingActivity f4780a;

    public QRLoadingActivity_ViewBinding(QRLoadingActivity qRLoadingActivity, View view) {
        this.f4780a = qRLoadingActivity;
        qRLoadingActivity.loadingBar = (ProgressBar) Utils.findRequiredViewAsType(view, R.id.activity_qr_loading_bar, "field 'loadingBar'", ProgressBar.class);
        qRLoadingActivity.loadingErrorLayout = (LinearLayout) Utils.findRequiredViewAsType(view, R.id.activity_qr_loading_error_layout, "field 'loadingErrorLayout'", LinearLayout.class);
        qRLoadingActivity.loadingErrorImageView = (IconicsImageView) Utils.findRequiredViewAsType(view, R.id.activity_qr_loading_error_image, "field 'loadingErrorImageView'", IconicsImageView.class);
        qRLoadingActivity.loadingErrorTextView = (TextView) Utils.findRequiredViewAsType(view, R.id.activity_qr_loading_error_text, "field 'loadingErrorTextView'", TextView.class);
        qRLoadingActivity.loadingErrorCloseButton = (Button) Utils.findRequiredViewAsType(view, R.id.activity_qr_loading_close_button, "field 'loadingErrorCloseButton'", Button.class);
    }

    @Override // butterknife.Unbinder
    public void unbind() {
        QRLoadingActivity qRLoadingActivity = this.f4780a;
        if (qRLoadingActivity == null) {
            throw new IllegalStateException("Bindings already cleared.");
        }
        this.f4780a = null;
        qRLoadingActivity.loadingBar = null;
        qRLoadingActivity.loadingErrorLayout = null;
        qRLoadingActivity.loadingErrorImageView = null;
        qRLoadingActivity.loadingErrorTextView = null;
        qRLoadingActivity.loadingErrorCloseButton = null;
    }
}
