package com.trimettransit.tracker.model.domain

/**
 * Pure reimplementation of gradle-play-publisher 4.1.1's `ProcessArtifactVersionCodes.VersionCoder`
 * version-code math, so the release versioning policy can be unit tested and reasoned about without
 * a Play API round trip. The Gradle build (`app/build.gradle.kts` GPP wiring) is the live consumer
 * of the same formulas; keep this file in sync with GPP's `AUTO` / `AUTO_OFFSET` strategies.
 *
 * Inputs in real builds: the hardcoded `defaultConfig.versionCode`, the highest versionCode GPP
 * finds across ALL Play tracks (`liveMaxCode`; GPP uses `1` when Play has no releases), the variant's
 * output count (1 for this single-APK project), and the strategy in effect.
 *
 * - `AUTO` (offsetMode = false): each output gets `default + max(0, liveMax - min(defaults) + 1) + index`.
 *   With one output this equals `liveMax + 1` only when `liveMax >= default - 1`; otherwise the output
 *   is just the hardcoded default.
 * - `AUTO_OFFSET` (offsetMode = true): outputs stay at `default` when every default already exceeds
 *   `liveMax`, otherwise `default + liveMax`. The "full offset" mode that once inflated past versions.
 */

/**
 * Resolve the effective version codes for a release build.
 *
 * @param defaultCode the hardcoded `defaultConfig.versionCode`
 * @param liveMaxCode highest versionCode already on Play across all tracks (`1` when none)
 * @param outputCount number of APK/bundle outputs for the variant
 * @param offsetMode true for GPP `AUTO_OFFSET`, false for GPP `AUTO`
 */
fun effectiveVersionCodes(
    defaultCode: Int,
    liveMaxCode: Int,
    outputCount: Int,
    offsetMode: Boolean = false
): List<Int> {
    val defaults = List(outputCount) { defaultCode }
    val codes = if (offsetMode) {
        val keepDefaults = defaults.all { it.toLong() > liveMaxCode.toLong() }
        defaults.map { if (keepDefaults) it else it + liveMaxCode }
    } else {
        val smallest = defaults.minOrNull() ?: 1
        val patch = maxOf(0L, liveMaxCode.toLong() - smallest + 1L)
        defaults.mapIndexed { index, code -> (code + patch + index).toInt() }
    }
    return codes
}