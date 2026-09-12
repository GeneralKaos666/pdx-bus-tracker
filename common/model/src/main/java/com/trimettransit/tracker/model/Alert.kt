package com.trimettransit.tracker.model

/**
 * A service alert currently in effect, parsed from the TriMet Alerts V2 feed
 * (`ws/v2/alerts`). [systemWide] marks alerts shown for the entire system;
 * otherwise [routeIds] lists the affected routes (both may coexist).
 */
data class Alert(
    val id: Int = 0,
    /** Short header text, may be empty when only [desc] is provided. */
    val header: String = "",
    val desc: String = "",
    val infoLinkUrl: String? = null,
    val systemWide: Boolean = false,
    val routeIds: List<Int> = emptyList()
) {
    val displayTitle: String
        get() = header.ifBlank { desc }
}