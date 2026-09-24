package com.trimettransit.tracker

import android.app.Application
import com.trimettransit.tracker.gtfs.GtfsScheduler
import com.trimettransit.tracker.map.initializeMapRuntime
import java.io.File
import timber.log.Timber

class TrimetTransitTracker : Application() {
    override fun onCreate() {
        super.onCreate()
        if (BuildConfig.DEBUG) {
            Timber.plant(Timber.DebugTree())
            // Debug-only crash recorder. Android does not let an ordinary app read its own
            // crash from logcat, so a debug build writes the trace where it can be pulled
            // with `adb shell run-as <pkg> cat files/last_crash.txt`. Never runs in release.
            installCrashRecorder()
        }
        // Dynamic color is applied by the Compose theme (TriMetGoTheme) so it can honor
        // the user's dynamic-Color preference toggle. Applying it app-wide here too would
        // override the XML window theme unconditionally and fight the Compose side.
        initializeMapRuntime(this)
        GtfsScheduler.schedulePeriodic(this)
    }

    private fun installCrashRecorder() {
        val previous = Thread.getDefaultUncaughtExceptionHandler()
        Thread.setDefaultUncaughtExceptionHandler { thread, throwable ->
            // Best effort: a failure here must never mask the original crash.
            runCatching {
                val runtime = Runtime.getRuntime()
                File(filesDir, CRASH_FILE).appendText(
                    buildString {
                        appendLine("===== crash =====")
                        appendLine("app: ${BuildConfig.VERSION_NAME} (${BuildConfig.VERSION_CODE})")
                        appendLine("time: ${System.currentTimeMillis()}")
                        appendLine("thread: ${thread.name}")
                        appendLine(
                            "heap: used=${runtime.totalMemory() - runtime.freeMemory()} " +
                                "total=${runtime.totalMemory()} max=${runtime.maxMemory()}"
                        )
                        appendLine()
                        appendLine(throwable.stackTraceToString())
                        appendLine()
                    }
                )
            }
            previous?.uncaughtException(thread, throwable)
        }
    }

    private companion object {
        const val CRASH_FILE = "last_crash.txt"
    }
}
