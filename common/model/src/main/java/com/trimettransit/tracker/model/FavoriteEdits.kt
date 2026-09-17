package com.trimettransit.tracker.model

/**
 * Pure helpers for favorite-stop edits. Kept free of Android/SQL so the
 * ordering rules stay unit-testable on the JVM.
 */
object FavoriteEdits {
    fun reorder(current: List<Int>, from: Int, to: Int): List<Int> {
        if (from !in current.indices || to !in current.indices || from == to) return current
        val reordered = current.toMutableList()
        reordered.add(to, reordered.removeAt(from))
        return reordered
    }

    fun moveStops(stops: List<Stop>, from: Int, to: Int): List<Stop> {
        if (from !in stops.indices || to !in stops.indices || from == to) return stops
        val reordered = stops.toMutableList()
        reordered.add(to, reordered.removeAt(from))
        return reordered
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
