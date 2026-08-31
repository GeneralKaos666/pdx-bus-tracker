# PDX Bus Tracker Wear R8 rules.
# Mirrors app/proguard-rules.pro: OkHttp/Compose, Wear Tiles/Protolayout and
# WorkManager all ship consumer rules; only Joda-Time (android.joda) needs
# explicit keeps for its enum/valueOf reflection.
-keep class org.joda.** { *; }
-dontwarn org.joda.**
