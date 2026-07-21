package com.something15525.trimetgo.trimet_go.activities;

import androidx.recyclerview.widget.RecyclerView;
import androidx.appcompat.widget.Toolbar;
import android.view.View;
import android.widget.TextView;
import butterknife.Unbinder;
import butterknife.internal.Utils;
import com.something15525.trimetgo.trimet_go.R;
import com.something15525.trimetgo.trimet_go.widget.FixedSwipeRefreshLayout;

public class DisplayArrivals_ViewBinding implements Unbinder {

        private DisplayArrivals f4757a;

    public DisplayArrivals_ViewBinding(DisplayArrivals displayArrivals, View view) {
        this.f4757a = displayArrivals;
        displayArrivals.rootView = Utils.findRequiredView(view, R.id.act_display_arrivals_coordinator, "field 'rootView'");
        displayArrivals.actionBarToolbar = (Toolbar) Utils.findRequiredViewAsType(view, R.id.act_display_arrivals_toolbar, "field 'actionBarToolbar'", Toolbar.class);
        displayArrivals.mSwipeRefreshLayout = (FixedSwipeRefreshLayout) Utils.findRequiredViewAsType(view, R.id.act_display_arrivals_ptr, "field 'mSwipeRefreshLayout'", FixedSwipeRefreshLayout.class);
        displayArrivals.mArrivalRecyclerView = (RecyclerView) Utils.findRequiredViewAsType(view, R.id.act_display_arrivals_list_view, "field 'mArrivalRecyclerView'", RecyclerView.class);
        displayArrivals.mLastRefreshedText = (TextView) Utils.findRequiredViewAsType(view, R.id.act_display_arrivals_last_refreshed, "field 'mLastRefreshedText'", TextView.class);
        displayArrivals.serviceAlertsButton = Utils.findRequiredView(view, R.id.act_display_arrivals_alert_bar, "field 'serviceAlertsButton'");
    }

    @Override // butterknife.Unbinder
    public void unbind() {
        DisplayArrivals displayArrivals = this.f4757a;
        if (displayArrivals == null) {
            throw new IllegalStateException("Bindings already cleared.");
        }
        this.f4757a = null;
        displayArrivals.rootView = null;
        displayArrivals.actionBarToolbar = null;
        displayArrivals.mSwipeRefreshLayout = null;
        displayArrivals.mArrivalRecyclerView = null;
        displayArrivals.mLastRefreshedText = null;
        displayArrivals.serviceAlertsButton = null;
    }
}
