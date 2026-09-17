package com.trimettransit.tracker.model

/**
 * Pure helpers for favorite-stop edits. Kept free of Android/SQL so the
 * ordering rules stay unit-testable on the JVM.
 */
object FavoriteEdits {
    fun moveStops(stops: List<Stop>, from: Int, to: Int): List<Stop> {
        if (from !in stops.indices || to !in stops.indices || from == to) return stops
        val reordered = stops.toMutableList()
        reordered.add(to, reordered.removeAt(from))
        return reordered
    }

    /**
     * Clamps a drag-derived move to valid list bounds so gestures past either
     * end land on the first/last item instead of silently doing nothing.
     * Unlike [moveStops], out-of-range targets are pulled into range rather
     * than rejected, which is what pointer input needs; [moveStops] stays
     * strict for programmatic moves. Returns [from] when the list is empty.
     */
    fun reorderTarget(from: Int, delta: Int, size: Int): Int {
        if (size <= 0) return from
        return (from + delta).coerceIn(0, size - 1)
    }

    /**
     * The one-time welcome shows only on a true first run: never shown before,
     * list loaded, and empty. In particular, upgraders with existing favorites
     * never see it.
     */
    fun shouldShowWelcome(alreadyShown: Boolean, isEmpty: Boolean, isLoading: Boolean): Boolean {
        return !alreadyShown && isEmpty && !isLoading
    }
}
