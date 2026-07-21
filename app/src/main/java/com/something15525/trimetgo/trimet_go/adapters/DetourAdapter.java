package com.something15525.trimetgo.trimet_go.adapters;

import android.content.Context;
import androidx.recyclerview.widget.RecyclerView;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;
import butterknife.BindView;
import butterknife.ButterKnife;
import butterknife.Unbinder;
import butterknife.internal.Utils;
import com.something15525.trimetgo.trimet_go.R;
import com.something15525.trimetgo.trimet_go.data.model.Detour;
import com.something15525.trimetgo.trimet_go.util.Constants2;
import java.util.List;

public class DetourAdapter extends RecyclerView.Adapter {

        private Context f4796c;

        private List<Detour> f4797d;

    static class DetourViewHolder extends RecyclerView.ViewHolder {

        @BindView(R.id.detour_list_view_item_description)
        TextView detourDescription;

        @BindView(R.id.detour_list_view_item_routes)
        TextView detourRoutes;

        DetourViewHolder(View view) {
            super(view);
            detourDescription = (android.widget.TextView) view.findViewById(R.id.detour_list_view_item_description);
            detourRoutes = (android.widget.TextView) view.findViewById(R.id.detour_list_view_item_routes);
        }
    }

    public class DetourViewHolder_ViewBinding implements Unbinder {

                private DetourViewHolder f4798a;

        public DetourViewHolder_ViewBinding(DetourViewHolder detourViewHolder, View view) {
            this.f4798a = detourViewHolder;
            detourViewHolder.detourDescription = (TextView) Utils.findRequiredViewAsType(view, R.id.detour_list_view_item_description, "field 'detourDescription'", TextView.class);
            detourViewHolder.detourRoutes = (TextView) Utils.findRequiredViewAsType(view, R.id.detour_list_view_item_routes, "field 'detourRoutes'", TextView.class);
        }

        @Override // butterknife.Unbinder
        public void unbind() {
            DetourViewHolder detourViewHolder = this.f4798a;
            if (detourViewHolder == null) {
                throw new IllegalStateException("Bindings already cleared.");
            }
            this.f4798a = null;
            detourViewHolder.detourDescription = null;
            detourViewHolder.detourRoutes = null;
        }
    }

    public DetourAdapter(Context context, List<Detour> list) {
        this.f4796c = context;
        this.f4797d = list;
    }

            public void onBindViewHolder(RecyclerView.ViewHolder viewHolder, int i) {
        DetourViewHolder detourViewHolder = (DetourViewHolder) viewHolder;
        Detour detour = this.f4797d.get(i);
        detourViewHolder.detourDescription.setText(detour.getDesc());
        if (detour.getRoutes() == null || detour.getRoutes().length <= 0) {
            return;
        }
        StringBuilder sb = new StringBuilder();
        int[] iArrA = detour.getRoutes();
        int i2 = 0;
        while (i2 < iArrA.length) {
            Object objA = Constants2.a(this.f4796c, Constants2.b.a(iArrA[i2]));
            if (objA == null) {
                objA = Integer.valueOf(iArrA[i2]);
            }
            sb.append(objA);
            i2++;
            if (i2 < iArrA.length) {
                sb.append(", ");
            }
        }
        detourViewHolder.detourRoutes.setText(sb.toString());
    }

        public RecyclerView.ViewHolder onCreateViewHolder(ViewGroup viewGroup, int i) {
        return new DetourViewHolder(LayoutInflater.from(viewGroup.getContext()).inflate(R.layout.detour_list_view_item, viewGroup, false));
    }

        public void c() { notifyDataSetChanged(); }

    public int getItemCount() {
        List<Detour> list = this.f4797d;
        if (list != null) {
            return list.size();
        }
        return 0;
    }
}
