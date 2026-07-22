package com.trimettransit.tracker.util;

import java.util.concurrent.atomic.AtomicInteger;

public class RequestCodeGenerator {

        private static final AtomicInteger f4900a = new AtomicInteger();

    public static int a() {
        return f4900a.incrementAndGet();
    }
}
