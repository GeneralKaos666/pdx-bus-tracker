# TriMet Go ProGuard Rules

# Keep Google Play Services
-keep class com.google.android.gms.** { *; }
-dontwarn com.google.android.gms.**

# Keep Firebase
-keep class com.google.firebase.** { *; }
-dontwarn com.google.firebase.**

# Keep Mikepenz Iconics
-keep class com.mikepenz.** { *; }
-dontwarn com.mikepenz.**

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

# Keep the app's R class

# Keep obfuscated referenced classes
-keep class com.trimettransit.tracker.** { *; }
