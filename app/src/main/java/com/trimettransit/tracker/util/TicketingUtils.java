package com.trimettransit.tracker.util;

import android.app.AlertDialog;
import android.content.ActivityNotFoundException;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.net.Uri;
import com.trimettransit.tracker.R;

public class TicketingUtils {

        static class GetTicketsClickListener implements DialogInterface.OnClickListener {

                final /* synthetic */ Context f4901b;

        GetTicketsClickListener(Context context) {
            this.f4901b = context;
        }

        @Override // android.content.DialogInterface.OnClickListener
        public void onClick(DialogInterface dialogInterface, int i) {
            try {
                this.f4901b.startActivity(new Intent("android.intent.action.VIEW", Uri.parse("market://details?id=org.trimet.mt.accounts")));
            } catch (ActivityNotFoundException unused) {
                this.f4901b.startActivity(new Intent("android.intent.action.VIEW", Uri.parse("https://play.google.com/store/apps/details?id=org.trimet.mt.accounts")));
            }
        }
    }

    public static void a(Context context) {
        try {
            context.getPackageManager().getPackageInfo("org.trimet.mt.accounts", 0);
            // App is installed — launch directly
            Intent intent = new Intent();
            intent.setClassName("org.trimet.mt.accounts", "com.hansecom.abt.presentation.main.MainActivity");
            context.startActivity(intent);
        } catch (android.content.pm.PackageManager.NameNotFoundException e) {
            new AlertDialog.Builder(context)
                .setMessage(context.getString(R.string.get_tickets_no_app_description))
                .setPositiveButton(android.R.string.ok, new GetTicketsClickListener(context))
                .setNegativeButton(android.R.string.cancel, (DialogInterface.OnClickListener) null)
                .show();
        }
    }
}
