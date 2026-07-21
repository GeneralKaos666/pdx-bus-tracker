package com.something15525.trimetgo.trimet_go.util;

import android.content.Context;
import com.something15525.trimetgo.trimet_go.R;
import org.joda.time.DateTime;
import org.joda.time.format.DateTimeFormat;

public class DateUtils {
    public static String a(DateTime dateTime, Context context) {
        StringBuilder sb = new StringBuilder();
        String[] stringArray = context.getResources().getStringArray(R.array.days_of_week);
        if (dateTime.getDayOfWeek() != DateTime.now().getDayOfWeek()) {
            sb.append(stringArray[dateTime.getDayOfWeek() - 1]);
            sb.append(", ");
        }
        sb.append(dateTime.toString(DateTimeFormat.forPattern("h:mm aa")));
        return sb.toString();
    }

    public static long b(DateTime dateTime) {
        return (Math.abs(dateTime.getMillis() - new DateTime().getMillis()) / 1000) / 60;
    }

    public static String a(DateTime dateTime) {
        return dateTime.toString(DateTimeFormat.forPattern("h:mm aa"));
    }
}
