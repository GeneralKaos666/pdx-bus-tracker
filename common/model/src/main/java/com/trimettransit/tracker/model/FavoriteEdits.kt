package com.trimettransit.tracker.model

/**
 * Pure helpers for favorite-stop edits. Kept free of Android/SQL so the
 * ordering and label rules stay unit-testable on the JVM.
 */
object FavoriteEdits {
    const val MAX_LABEL_LENGTH = 60

    fun reorder(current: List<Int>, from: Int, to: Int): List<Int> {
        if (from !in current.indices || to !in current.indices || from == to) return current
        val reordered = current.toMutableList()
        reordered.add(to, reordered.removeAt(from))
        return reordered
    }

    fun sanitizeLabel(raw: String): String {
        val trimmed = raw.trim()
        if (trimmed.isEmpty()) return ""
        return trimmed.take(MAX_LABEL_LENGTH)
    }

    fun displayName(desc: String, label: String): String {
        val clean = sanitizeLabel(label)
        return if (clean.isEmpty()) desc else clean
    }

    fun moveStops(stops: List<Stop>, from: Int, to: Int): List<Stop> {
        if (from !in stops.indices || to !in stops.indices || from == to) return stops
        val reordered = stops.toMutableList()
        reordered.add(to, reordered.removeAt(from))
        return reordered
    }
}
