package com.trimettransit.tracker.model

/**
 * A service alert currently in effect, parsed from the TriMet Alerts V2 feed
 * (`ws/v2/alerts`). Fetches of this feed must always be scoped to specific
 * routes and/or stops (see [com.trimettransit.tracker.model.repository.TransitRepository.getAlerts]);
 * there is no unscoped/system-wide alerts UI in this app. [systemWide] reflects
 * the feed's own `system_wide_flag` for completeness, but per-line/per-stop UI
 * should match on [routeIds] rather than surfacing system-wide alerts globally.
 */
data class TransitAlert(
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
