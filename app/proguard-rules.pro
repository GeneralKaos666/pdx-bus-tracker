# PDX Bus Tracker R8 rules.
# OkHttp/Okio, MapLibre, Compose and AndroidX all ship consumer rules.
# Joda-Time (android.joda) bundles its own rules that keep the packaged
# timezone resources and the Serializable serialization protocol it uses, so
# no app-level keep is needed.

-dontwarn org.joda.**
