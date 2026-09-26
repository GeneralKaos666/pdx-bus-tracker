package com.trimettransit.tracker.model.domain

/**
 * Per-stop line-pin default: a nullable `pinned_line` on the favorites row.
 *
 * Null means "no default" — every pre-existing row reads back null, so arrivals
 * behave exactly as before the column existed. The SQL lives here (rather than
 * next to [DatabaseHelper]) so the migration string and the null/empty mapping
 * stay JVM-testable without spinning up SQLite; `DatabaseHelper` references
 * these constants as its single source of truth.
 */
const val PINNED_LINE_COLUMN = "pinned_line"

/** Additive, data-preserving migration: existing rows keep NULL (no default). */
const val ADD_PINNED_LINE_COLUMN_SQL =
    "ALTER TABLE favorites ADD COLUMN pinned_line INTEGER"

/**
 * Maps a raw `pinned_line` cell to the stored default: a SQL NULL (or a missing
 * row) is [isNull] and yields null; any stored int passes through.
 */
fun mapPinnedLine(isNull: Boolean, value: Int): Int? =
    if (isNull) null else value

/**
 * Initial session line filter for a stop open.
 *
 * An explicit navigation context ([navRouteId] > 0, e.g. tapped from a line's
 * stop list) wins: the user just chose that line. Otherwise a stored pin
 * applies; non-positive pins are ignored. Returns 0 for "no filter", matching
 * [filterArrivalsByRoute]'s convention.
 */
fun resolveSessionLineFilter(pinnedLine: Int?, navRouteId: Int): Int =
    when {
        navRouteId > 0 -> navRouteId
        pinnedLine != null && pinnedLine > 0 -> pinnedLine
        else -> 0
    }
