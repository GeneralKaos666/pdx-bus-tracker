package com.trimettransit.tracker.widget

/** Canonical widget layout bucket derived from the host size. */
enum class WidgetLayout {
    COMPACT,
    LIST,
    TALL
}

/**
 * Pure size-bucket mapping, kept out of the Glance composition so it is
 * unit-testable on the JVM. Thresholds mirror the provider XML bounds:
 * short (<128dp tall) or narrow (<128dp wide) hosts get a single compact
 * row; tall hosts (>=320dp) get the full list with header.
 */
fun layoutForSize(widthDp: Int, heightDp: Int): WidgetLayout = when {
    heightDp < 128 || widthDp < 128 -> WidgetLayout.COMPACT
    heightDp >= 320 -> WidgetLayout.TALL
    else -> WidgetLayout.LIST
}

/**
 * Grid columns per layout bucket: single column when compact (half-width cells
 * would truncate every stop name), two side-by-side otherwise.
 */
fun columnsForLayout(layout: WidgetLayout): Int = when (layout) {
    WidgetLayout.COMPACT -> 1
    WidgetLayout.LIST, WidgetLayout.TALL -> 2
}
