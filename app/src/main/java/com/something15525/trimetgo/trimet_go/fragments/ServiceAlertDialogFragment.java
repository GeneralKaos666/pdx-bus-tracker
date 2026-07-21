package com.something15525.trimetgo.trimet_go.fragments;

import android.app.AlertDialog;
import android.app.Dialog;
import android.content.DialogInterface;
import android.os.Bundle;
import androidx.fragment.app.DialogFragment;
import androidx.fragment.app.FragmentActivity;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import android.view.View;
import android.view.ViewGroup;
import butterknife.BindView;
import butterknife.ButterKnife;
import com.something15525.trimetgo.trimet_go.R;
import com.something15525.trimetgo.trimet_go.adapters.DetourAdapter;
import java.util.ArrayList;

public class ServiceAlertDialogFragment extends DialogFragment {

    @BindView(R.id.fragment_service_alert_dialog_recycler_view)
    RecyclerView detoursRecyclerView;

    @Override // android.support.v4.app.DialogFragment
    public Dialog onCreateDialog(Bundle bundle) {
        Bundle arguments = getArguments();
        FragmentActivity activity = getActivity();
        ArrayList parcelableArrayList = arguments.getParcelableArrayList("ExtraDetoursListData");
        AlertDialog.Builder builder = new AlertDialog.Builder(activity);
        View viewInflate = getActivity().getLayoutInflater().inflate(R.layout.fragment_service_alert_dialog, (ViewGroup) null);
        detoursRecyclerView = (androidx.recyclerview.widget.RecyclerView) viewInflate.findViewById(R.id.fragment_service_alert_dialog_recycler_view);
        DetourAdapter detourAdapter = new DetourAdapter(activity, parcelableArrayList);
        detoursRecyclerView.setLayoutManager(new androidx.recyclerview.widget.LinearLayoutManager(activity));
        detoursRecyclerView.setAdapter(detourAdapter);
        builder.setTitle(R.string.display_arrivals_detours_title).setNeutralButton(android.R.string.ok, (DialogInterface.OnClickListener) null);
        builder.setView(viewInflate);
        return builder.create();
    }
}
