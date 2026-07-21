package com.something15525.trimetgo.trimet_go.util;

import android.content.Context;
import com.parse.ParseException;
import com.something15525.trimetgo.trimet_go.R;

public class Constants2 {

        public static String f4889a = "AIzaSyDmbTHKuf8jfoE6BMPkbW6udRv_wyAJnzo";

        public static String f4891c = "drGnMLdbtm2cIEqljH0eI3CqT0iv2IN7x0ILPKmB";

        public static String f4892d = "http://trimet-go-search.somex.app/parse/";

        public static int f4893e = 75;

    public static String TRIMET_API_KEY = "C7558610143A7E383B6843D0A";

        static /* synthetic */ class SwitchMaps {

                static final /* synthetic */ int[] f4894a = new int[b.values().length];

        static {
            try {
                f4894a[b.RED.ordinal()] = 1;
            } catch (NoSuchFieldError unused) {
            }
            try {
                f4894a[b.BLUE.ordinal()] = 2;
            } catch (NoSuchFieldError unused2) {
            }
            try {
                f4894a[b.YELLOW.ordinal()] = 3;
            } catch (NoSuchFieldError unused3) {
            }
            try {
                f4894a[b.NS.ordinal()] = 4;
            } catch (NoSuchFieldError unused4) {
            }
            try {
                f4894a[b.A.ordinal()] = 5;
            } catch (NoSuchFieldError unused5) {
            }
            try {
                f4894a[b.B.ordinal()] = 6;
            } catch (NoSuchFieldError unused6) {
            }
            try {
                f4894a[b.GREEN.ordinal()] = 7;
            } catch (NoSuchFieldError unused7) {
            }
            try {
                f4894a[b.WES.ordinal()] = 8;
            } catch (NoSuchFieldError unused8) {
            }
            try {
                f4894a[b.ORANGE.ordinal()] = 9;
            } catch (NoSuchFieldError unused9) {
            }
        }
    }

            public enum b {
        RED(90),
        BLUE(100),
        YELLOW(190),
        NS(193),
        A(194),
        B(195),
        GREEN(ParseException.USERNAME_MISSING),
        WES(ParseException.EMAIL_TAKEN),
        ORANGE(290);

                private final int f4899b;

        b(int i) {
            this.f4899b = i;
        }

        public static b a(int i) {
            for (b bVar : values()) {
                if (bVar.f4899b == i) {
                    return bVar;
                }
            }
            return null;
        }
    }

    public static String a(Context context, b bVar) {
        if (bVar == null) {
            return null;
        }
        switch (SwitchMaps.f4894a[bVar.ordinal()]) {
            case 1:
                return context.getString(R.string.red_line_text);
            case 2:
                return context.getString(R.string.blue_line_text);
            case 3:
                return context.getString(R.string.yellow_line_text);
            case 4:
                return context.getString(R.string.ns_line_text);
            case 5:
                return context.getString(R.string.a_loop_text);
            case 6:
                return context.getString(R.string.b_loop_text);
            case 7:
                return context.getString(R.string.green_line_text);
            case 8:
                return context.getString(R.string.wes_text);
            case 9:
                return context.getString(R.string.orange_line_text);
            default:
                return null;
        }
    }
}
