# TriMet Go ProGuard Rules

# Keep Joda-Time
-keep class org.joda.** { *; }
-dontwarn org.joda.**

# Keep OkHttp/Okio
-keep class okhttp3.** { *; }
-keep class okio.** { *; }
-dontwarn okhttp3.**
-dontwarn okio.**

# Keep obfuscated referenced classes
-keep class com.trimettransit.tracker.** { *; }
