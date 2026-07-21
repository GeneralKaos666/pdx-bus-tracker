package com.something15525.trimetgo.trimet_go.adapters;

import android.content.Context;
import android.graphics.drawable.GradientDrawable;
import androidx.recyclerview.widget.RecyclerView;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.animation.Animation;
import android.view.animation.AnimationUtils;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.RelativeLayout;
import android.widget.TextView;
import butterknife.BindView;
import butterknife.ButterKnife;
import butterknife.Unbinder;
import butterknife.internal.Utils;
import com.something15525.trimetgo.trimet_go.R;
import com.something15525.trimetgo.trimet_go.data.model.Arrival;
import com.something15525.trimetgo.trimet_go.util.Constants2;
import com.something15525.trimetgo.trimet_go.util.DateUtils;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

public class ArrivalAdapter extends RecyclerView.Adapter<RecyclerView.ViewHolder> {

        private List<Arrival> f4788c;

        private List<Arrival> f4789d;

        private Context f4790e;
    private boolean h;
    private boolean i;
    private int j;

        private int f4791f = -1;
    private int g = 0;
    private boolean k = false;

    static class ArrivalRecyclerViewHolder extends RecyclerView.ViewHolder {

        @BindView(R.id.arrival_item_container)
        protected RelativeLayout container;

        @BindView(R.id.arrival_time_diff)
        protected TextView diffText;

        @BindView(R.id.circle_background)
        protected ImageView icon;

        @BindView(R.id.circle_icon)
        protected ImageView iconPicture;

        @BindView(R.id.circle_icon_text)
        protected TextView iconText;

        @BindView(R.id.arrival_name)
        protected TextView name;

        @BindView(R.id.arrival_scheduled_time)
        protected TextView scheduledTime;

        @BindView(R.id.arrival_time)
        protected TextView time;

        ArrivalRecyclerViewHolder(View view) {
            super(view);
            container = (android.widget.RelativeLayout) view.findViewById(R.id.arrival_item_container);
            diffText = (android.widget.TextView) view.findViewById(R.id.arrival_time_diff);
            icon = (android.widget.ImageView) view.findViewById(R.id.circle_background);
            iconPicture = (android.widget.ImageView) view.findViewById(R.id.circle_icon);
            iconText = (android.widget.TextView) view.findViewById(R.id.circle_icon_text);
            name = (android.widget.TextView) view.findViewById(R.id.arrival_name);
            scheduledTime = (android.widget.TextView) view.findViewById(R.id.arrival_scheduled_time);
            time = (android.widget.TextView) view.findViewById(R.id.arrival_time);
        }
    }

    public class ArrivalRecyclerViewHolder_ViewBinding implements Unbinder {

                private ArrivalRecyclerViewHolder f4792a;

        public ArrivalRecyclerViewHolder_ViewBinding(ArrivalRecyclerViewHolder arrivalRecyclerViewHolder, View view) {
            this.f4792a = arrivalRecyclerViewHolder;
            arrivalRecyclerViewHolder.container = (RelativeLayout) Utils.findRequiredViewAsType(view, R.id.arrival_item_container, "field 'container'", RelativeLayout.class);
            arrivalRecyclerViewHolder.icon = (ImageView) Utils.findRequiredViewAsType(view, R.id.circle_background, "field 'icon'", ImageView.class);
            arrivalRecyclerViewHolder.iconText = (TextView) Utils.findRequiredViewAsType(view, R.id.circle_icon_text, "field 'iconText'", TextView.class);
            arrivalRecyclerViewHolder.iconPicture = (ImageView) Utils.findRequiredViewAsType(view, R.id.circle_icon, "field 'iconPicture'", ImageView.class);
            arrivalRecyclerViewHolder.name = (TextView) Utils.findRequiredViewAsType(view, R.id.arrival_name, "field 'name'", TextView.class);
            arrivalRecyclerViewHolder.diffText = (TextView) Utils.findRequiredViewAsType(view, R.id.arrival_time_diff, "field 'diffText'", TextView.class);
            arrivalRecyclerViewHolder.time = (TextView) Utils.findRequiredViewAsType(view, R.id.arrival_time, "field 'time'", TextView.class);
            arrivalRecyclerViewHolder.scheduledTime = (TextView) Utils.findRequiredViewAsType(view, R.id.arrival_scheduled_time, "field 'scheduledTime'", TextView.class);
        }

        @Override // butterknife.Unbinder
        public void unbind() {
            ArrivalRecyclerViewHolder arrivalRecyclerViewHolder = this.f4792a;
            if (arrivalRecyclerViewHolder == null) {
                throw new IllegalStateException("Bindings already cleared.");
            }
            this.f4792a = null;
            arrivalRecyclerViewHolder.container = null;
            arrivalRecyclerViewHolder.icon = null;
            arrivalRecyclerViewHolder.iconText = null;
            arrivalRecyclerViewHolder.iconPicture = null;
            arrivalRecyclerViewHolder.name = null;
            arrivalRecyclerViewHolder.diffText = null;
            arrivalRecyclerViewHolder.time = null;
            arrivalRecyclerViewHolder.scheduledTime = null;
        }
    }

    static class ShowOtherArrivalRecyclerViewHolder extends RecyclerView.ViewHolder {

        @BindView(R.id.show_other_arrivals_button)
        Button showOtherArrivalsButton;

        ShowOtherArrivalRecyclerViewHolder(View view) {
            super(view);
            showOtherArrivalsButton = (android.widget.Button) view.findViewById(R.id.show_other_arrivals_button);
        }
    }

    public class ShowOtherArrivalRecyclerViewHolder_ViewBinding implements Unbinder {

                private ShowOtherArrivalRecyclerViewHolder f4793a;

        public ShowOtherArrivalRecyclerViewHolder_ViewBinding(ShowOtherArrivalRecyclerViewHolder showOtherArrivalRecyclerViewHolder, View view) {
            this.f4793a = showOtherArrivalRecyclerViewHolder;
            showOtherArrivalRecyclerViewHolder.showOtherArrivalsButton = (Button) Utils.findRequiredViewAsType(view, R.id.show_other_arrivals_button, "field 'showOtherArrivalsButton'", Button.class);
        }

        @Override // butterknife.Unbinder
        public void unbind() {
            ShowOtherArrivalRecyclerViewHolder showOtherArrivalRecyclerViewHolder = this.f4793a;
            if (showOtherArrivalRecyclerViewHolder == null) {
                throw new IllegalStateException("Bindings already cleared.");
            }
            this.f4793a = null;
            showOtherArrivalRecyclerViewHolder.showOtherArrivalsButton = null;
        }
    }

    class a implements View.OnClickListener {
        a() {
        }

        @Override // android.view.View.OnClickListener
        public void onClick(View view) {
            ArrivalAdapter.this.k = true;
            ArrivalAdapter.this.h = true;
            ArrivalAdapter.this.notifyDataSetChanged();
        }
    }

    static /* synthetic */ class b {

                static final /* synthetic */ int[] f4795a = new int[Constants2.b.values().length];

        static {
            try {
                f4795a[Constants2.b.RED.ordinal()] = 1;
            } catch (NoSuchFieldError unused) {
            }
            try {
                f4795a[Constants2.b.BLUE.ordinal()] = 2;
            } catch (NoSuchFieldError unused2) {
            }
            try {
                f4795a[Constants2.b.YELLOW.ordinal()] = 3;
            } catch (NoSuchFieldError unused3) {
            }
            try {
                f4795a[Constants2.b.NS.ordinal()] = 4;
            } catch (NoSuchFieldError unused4) {
            }
            try {
                f4795a[Constants2.b.A.ordinal()] = 5;
            } catch (NoSuchFieldError unused5) {
            }
            try {
                f4795a[Constants2.b.B.ordinal()] = 6;
            } catch (NoSuchFieldError unused6) {
            }
            try {
                f4795a[Constants2.b.GREEN.ordinal()] = 7;
            } catch (NoSuchFieldError unused7) {
            }
            try {
                f4795a[Constants2.b.WES.ordinal()] = 8;
            } catch (NoSuchFieldError unused8) {
            }
            try {
                f4795a[Constants2.b.ORANGE.ordinal()] = 9;
            } catch (NoSuchFieldError unused9) {
            }
        }
    }

    public ArrivalAdapter(Context context, List<Arrival> list, boolean z, int i) {
        this.f4790e = context;
        this.f4788c = list;
        this.h = z;
        this.j = i;
        d();
    }

    private void d() {
        this.f4789d = new ArrayList();
        this.i = false;
        if (this.j != -1) {
            for (Arrival arrival : this.f4788c) {
                if (arrival.getRouteId() == this.j) {
                    this.f4789d.add(arrival);
                }
            }
            this.i = this.f4788c.size() != this.f4789d.size();
        }
    }

        public void onViewRecycled(RecyclerView.ViewHolder d0Var) {
        if (d0Var instanceof ArrivalRecyclerViewHolder) {
            ((ArrivalRecyclerViewHolder) d0Var).container.clearAnimation();
            this.g = 0;
        }
    }

    private void a(ArrivalRecyclerViewHolder arrivalRecyclerViewHolder, Arrival arrival, int i) {
        Object objValueOf;
        if (arrival.getStatus().equals("estimated")) {
            long jB = DateUtils.b(arrival.getEstimated());
            StringBuilder sb = new StringBuilder();
            long j = jB % 60;
            sb.append(j);
            sb.append(" min");
            String string = sb.toString();
            if (j != 0) {
                arrivalRecyclerViewHolder.time.setText(string);
            } else {
                arrivalRecyclerViewHolder.time.setText(R.string.act_display_arrivals_due);
            }
            boolean zIsAfter = arrival.getEstimated().isAfter(arrival.getScheduled());
            int i2 = zIsAfter ? R.string.arrival_late_in_minutes_header : R.string.arrival_early_in_minutes_header;
            String string2 = Integer.toString(zIsAfter ? arrival.getEstimated().minus(arrival.getScheduled().getMillis()).getMinuteOfHour() : arrival.getScheduled().minus(arrival.getEstimated().getMillis()).getMinuteOfHour());
            arrivalRecyclerViewHolder.diffText.setText(string2.equals("0") ? this.f4790e.getString(R.string.arrival_on_time) : this.f4790e.getString(i2, string2));
        } else {
            arrivalRecyclerViewHolder.time.setText(arrival.getStatus().equals("canceled") ? this.f4790e.getString(R.string.act_display_arrivals_canceled) : DateUtils.a(arrival.getScheduled()));
            arrivalRecyclerViewHolder.diffText.setText(this.f4790e.getString(R.string.act_display_arrivals_no_eta));
        }
        if (arrivalRecyclerViewHolder.icon.getDrawable() instanceof GradientDrawable) {
            GradientDrawable gradientDrawable = (GradientDrawable) arrivalRecyclerViewHolder.icon.getDrawable();
            Constants2.b bVarA = Constants2.b.a(arrival.getRouteId());
            arrivalRecyclerViewHolder.iconText.setVisibility(View.VISIBLE);
            arrivalRecyclerViewHolder.iconPicture.setVisibility(View.GONE);
            if (bVarA != null) {
                switch (b.f4795a[bVarA.ordinal()]) {
                    case 1:
                        arrivalRecyclerViewHolder.iconText.setText(this.f4790e.getString(R.string.circle_red_line));
                        gradientDrawable.setColor(this.f4790e.getResources().getColor(R.color.circle_red));
                        break;
                    case 2:
                        arrivalRecyclerViewHolder.iconText.setText(this.f4790e.getString(R.string.circle_blue_line));
                        gradientDrawable.setColor(this.f4790e.getResources().getColor(R.color.circle_blue));
                        break;
                    case 3:
                        arrivalRecyclerViewHolder.iconText.setText(this.f4790e.getString(R.string.circle_yellow_line));
                        gradientDrawable.setColor(this.f4790e.getResources().getColor(R.color.circle_yellow));
                        break;
                    case 4:
                        arrivalRecyclerViewHolder.iconText.setText(this.f4790e.getString(R.string.circle_ns_line));
                        gradientDrawable.setColor(this.f4790e.getResources().getColor(R.color.circle_ns));
                        break;
                    case 5:
                        arrivalRecyclerViewHolder.iconText.setText(this.f4790e.getString(R.string.circle_a_loop));
                        gradientDrawable.setColor(this.f4790e.getResources().getColor(R.color.circle_a));
                        break;
                    case 6:
                        arrivalRecyclerViewHolder.iconText.setText(this.f4790e.getString(R.string.circle_b_loop));
                        gradientDrawable.setColor(this.f4790e.getResources().getColor(R.color.circle_b));
                        break;
                    case 7:
                        arrivalRecyclerViewHolder.iconText.setText(this.f4790e.getString(R.string.circle_green_line));
                        gradientDrawable.setColor(this.f4790e.getResources().getColor(R.color.circle_green));
                        break;
                    case 8:
                        arrivalRecyclerViewHolder.iconText.setText(this.f4790e.getString(R.string.circle_wes));
                        gradientDrawable.setColor(this.f4790e.getResources().getColor(R.color.circle_default));
                        break;
                    case 9:
                        arrivalRecyclerViewHolder.iconText.setText(this.f4790e.getString(R.string.circle_orange_line));
                        gradientDrawable.setColor(this.f4790e.getResources().getColor(R.color.circle_orange));
                        break;
                    default:
                        arrivalRecyclerViewHolder.iconText.setText("");
                        gradientDrawable.setColor(this.f4790e.getResources().getColor(R.color.circle_default));
                        break;
                }
                String strC = arrival.getFullSign();
                if (strC != null) {
                    if (strC.contains(" To ")) {
                        arrivalRecyclerViewHolder.name.setText(strC.substring(strC.indexOf(" To") + 4));
                    } else if (strC.contains(" to ")) {
                        arrivalRecyclerViewHolder.name.setText(strC.substring(strC.indexOf(" to") + 4));
                    } else {
                        arrivalRecyclerViewHolder.name.setText(arrival.getShortSign());
                    }
                }
            } else {
                gradientDrawable.setColor(this.f4790e.getResources().getColor(R.color.circle_default));
                boolean zContains = arrival.getFullSign().contains(Integer.toString(arrival.getRouteId()) + "E");
                TextView textView = arrivalRecyclerViewHolder.iconText;
                Locale locale = Locale.ENGLISH;
                Object[] objArr = new Object[1];
                if (zContains) {
                    objValueOf = arrival.getRouteId() + "E";
                } else {
                    objValueOf = Integer.valueOf(arrival.getRouteId());
                }
                objArr[0] = objValueOf;
                textView.setText(String.format(locale, "%s", objArr));
                String strF = arrival.getShortSign();
                String[] strArrSplit = strF.split(" ", 2);
                if (strArrSplit.length > 1) {
                    String strSubstring = strArrSplit[1];
                    if (strSubstring.startsWith(" ")) {
                        strSubstring = strSubstring.substring(1, strSubstring.length());
                    }
                    arrivalRecyclerViewHolder.name.setText(strSubstring);
                } else {
                    arrivalRecyclerViewHolder.name.setText(strF);
                }
            }
        } else {
            arrivalRecyclerViewHolder.name.setText(arrival.getShortSign());
        }
        arrivalRecyclerViewHolder.scheduledTime.setText(this.f4790e.getString(R.string.scheduled_time_header, DateUtils.a(arrival.getScheduled(), this.f4790e)));
        if (this.h) {
            a((View) arrivalRecyclerViewHolder.container, i);
        }
    }

        public RecyclerView.ViewHolder onCreateViewHolder(ViewGroup viewGroup, int i) {
        boolean z = i == 1;
        View viewInflate = LayoutInflater.from(viewGroup.getContext()).inflate(z ? R.layout.show_other_arrivals_button : R.layout.arrival_item, viewGroup, false);
        return z ? new ShowOtherArrivalRecyclerViewHolder(viewInflate) : new ArrivalRecyclerViewHolder(viewInflate);
    }

        public int getItemViewType(int i) {
        return i == ((!this.i || this.k) ? this.f4788c : this.f4789d).size() ? 1 : 0;
    }

        public void onBindViewHolder(RecyclerView.ViewHolder d0Var, int i) {
        Arrival arrival;
        if (d0Var instanceof ArrivalRecyclerViewHolder) {
            if (this.i && !this.k) {
                arrival = this.f4789d.get(i);
            } else {
                arrival = this.f4788c.get(i);
            }
            a((ArrivalRecyclerViewHolder) d0Var, arrival, i);
            return;
        }
        if (d0Var instanceof ShowOtherArrivalRecyclerViewHolder) {
            ((ShowOtherArrivalRecyclerViewHolder) d0Var).showOtherArrivalsButton.setOnClickListener(new a());
        }
    }

    private void a(View view, int i) {
        if (i > this.f4791f) {
            Animation animationLoadAnimation = AnimationUtils.loadAnimation(this.f4790e, android.R.anim.slide_in_left);
            this.g += Constants2.f4893e;
            animationLoadAnimation.setStartOffset(this.g);
            view.startAnimation(animationLoadAnimation);
            this.f4791f = i;
        }
    }

        public int getItemCount() {
        List<Arrival> list;
        if (this.f4788c == null || (list = this.f4789d) == null) {
            return 0;
        }
        return (!this.i || this.k) ? this.f4788c.size() : list.size() + 1;
    }

    public void a(List<Arrival> list) {
        this.f4788c = list;
        d();
        notifyDataSetChanged();
    }
}
