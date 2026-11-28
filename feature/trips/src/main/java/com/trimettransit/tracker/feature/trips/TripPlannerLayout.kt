package com.trimettransit.tracker.feature.trips

/**
 * Whether the planner card starts collapsed.
 *
 * Expanded is reserved for height-expanded windows (tablets, landscape large screens,
 * >= 900dp per Material's window size classes). Every phone window — compact or medium
 * height — starts collapsed so the map keeps a usable area, which is what the changelog
 * promises. The earlier `>= 700dp` threshold expanded the card on ordinary portrait
 * phones and defeated the rule.
 */
fun plannerStartsCollapsed(screenHeightDp: Int): Boolean = screenHeightDp < 900
