package com.trimettransit.tracker.model.domain

/**
 * One stored favourite in the order the database will return it.
 *
 * [sortOrder] is the hand-arranged position (0 = never arranged); [id] is the SQLite row id
 * used as the stable tie-break and as the fallback position.
 */
data class FavoriteOrderRow(val locId: Int, val sortOrder: Int, val id: Long)

/**
 * The favourites order the SQL contract promises
 * (`ORDER BY CASE WHEN sort_order > 0 THEN sort_order ELSE id END, id`), expressed as pure
 * code so the rule is testable without Android: arranged rows first by their position,
 * never-arranged rows by insertion id, ties broken by row id.
 */
fun orderedFavoriteIds(rows: List<FavoriteOrderRow>): List<Int> =
    rows.sortedWith(
        compareBy<FavoriteOrderRow> { if (it.sortOrder > 0) it.sortOrder.toLong() else it.id }
            .thenBy { it.id }
    ).map { it.locId }
