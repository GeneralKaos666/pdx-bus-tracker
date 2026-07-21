package com.something15525.trimetgo.trimet_go.util;

import android.app.AlertDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.SharedPreferences;
import android.net.Uri;
import com.something15525.trimetgo.trimet_go.R;

public class AppRater {

        static class RateNowClickListener implements DialogInterface.OnClickListener {
        RateNowClickListener() {
        }

        @Override // android.content.DialogInterface.OnClickListener
        public void onClick(DialogInterface dialogInterface, int i) {
            dialogInterface.dismiss();
        }
    }

        static class NeverRateClickListener implements DialogInterface.OnClickListener {

                final /* synthetic */ SharedPreferences.Editor f4886b;

        NeverRateClickListener(SharedPreferences.Editor editor) {
            this.f4886b = editor;
        }

        @Override // android.content.DialogInterface.OnClickListener
        public void onClick(DialogInterface dialogInterface, int i) {
            SharedPreferences.Editor editor = this.f4886b;
            if (editor != null) {
                editor.putBoolean("dontshowagain", true);
                this.f4886b.commit();
            }
            dialogInterface.dismiss();
        }
    }

        static class LaterRateClickListener implements DialogInterface.OnClickListener {

                final /* synthetic */ Context f4887b;

                final /* synthetic */ SharedPreferences.Editor f4888c;

        LaterRateClickListener(Context context, SharedPreferences.Editor editor) {
            this.f4887b = context;
            this.f4888c = editor;
        }

        @Override // android.content.DialogInterface.OnClickListener
        public void onClick(DialogInterface dialogInterface, int i) {
            this.f4887b.startActivity(new Intent("android.intent.action.VIEW", Uri.parse("market://details?id=com.something15525.trimetgo.trimet_go")));
            SharedPreferences.Editor editor = this.f4888c;
            if (editor != null) {
                editor.putBoolean("dontshowagain", true);
                this.f4888c.commit();
            }
            dialogInterface.dismiss();
        }
    }

    public static void a(Context context) {
        SharedPreferences sharedPreferences = context.getSharedPreferences("apprater", 0);
        if (sharedPreferences.getBoolean("dontshowagain", false)) {
            return;
        }
        SharedPreferences.Editor editorEdit = sharedPreferences.edit();
        long j = sharedPreferences.getLong("launch_count", 0L) + 1;
        editorEdit.putLong("launch_count", j);
        Long lValueOf = Long.valueOf(sharedPreferences.getLong("date_firstlaunch", 0L));
        if (lValueOf.longValue() == 0) {
            lValueOf = Long.valueOf(System.currentTimeMillis());
            editorEdit.putLong("date_firstlaunch", lValueOf.longValue());
        }
        if (j >= 5 && System.currentTimeMillis() >= lValueOf.longValue() + 259200000) {
            a(context, editorEdit);
        }
        editorEdit.apply();
    }

    public static void a(Context context, SharedPreferences.Editor editor) {
        AlertDialog.Builder builder = new AlertDialog.Builder(context);
        builder.setTitle(R.string.app_rater_dialog_title);
        builder.setMessage(R.string.app_rater_dialog_message);
        builder.setNegativeButton(context.getString(R.string.app_rater_dialog_negative_button), new RateNowClickListener());
        builder.setNeutralButton(context.getString(R.string.app_rater_dialog_neutral_button), new NeverRateClickListener(editor));
        builder.setPositiveButton(context.getString(R.string.app_rater_dialog_positive_button), new LaterRateClickListener(context, editor));
        builder.show();
    }
}
