package com.something15525.trimetgo.trimet_go.fragments;

import androidx.recyclerview.widget.RecyclerView;
import android.view.View;
import butterknife.Unbinder;
import butterknife.internal.Utils;
import com.something15525.trimetgo.trimet_go.R;

public class ServiceAlertDialogFragment_ViewBinding implements Unbinder {

        private ServiceAlertDialogFragment f4938a;

    public ServiceAlertDialogFragment_ViewBinding(ServiceAlertDialogFragment serviceAlertDialogFragment, View view) {
        this.f4938a = serviceAlertDialogFragment;
        serviceAlertDialogFragment.detoursRecyclerView = (RecyclerView) Utils.findRequiredViewAsType(view, R.id.fragment_service_alert_dialog_recycler_view, "field 'detoursRecyclerView'", RecyclerView.class);
    }

    @Override // butterknife.Unbinder
    public void unbind() {
        ServiceAlertDialogFragment serviceAlertDialogFragment = this.f4938a;
        if (serviceAlertDialogFragment == null) {
            throw new IllegalStateException("Bindings already cleared.");
        }
        this.f4938a = null;
        serviceAlertDialogFragment.detoursRecyclerView = null;
    }
}
