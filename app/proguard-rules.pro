# PDX Bus Tracker R8 rules.
# OkHttp/Okio, MapLibre, Compose and AndroidX all ship consumer rules; only
# Joda-Time (android.joda) needs explicit keeps for its enum/valueOf reflection.

-keep class org.joda.** { *; }
-dontwarn org.joda.**
