# TriMet Go ProGuard Rules

# Keep Joda-Time
-keep class org.joda.** { *; }
-dontwarn org.joda.**

# Keep EventBus
-keep class org.greenrobot.** { *; }
-dontwarn org.greenrobot.**

# Keep OkHttp/Okio
-keep class okhttp3.** { *; }
-keep class okio.** { *; }
-dontwarn okhttp3.**
-dontwarn okio.**

# Keep obfuscated referenced classes
-keep class com.trimettransit.tracker.** { *; }
