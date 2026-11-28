package com.trimettransit.tracker.ui.components

/**
 * The mutually exclusive states a stop-search panel can show. An enum, not an Int: the
 * numeric version needed a comment to decode and a `when` that silently fell through.
 */
enum class SearchPanelState {
    /** Catalogue still loading. */
    Loading,

    /** Catalogue load failed. */
    Error,

    /** Nothing typed yet, so the panel offers quick picks. */
    Prompt,

    /** Query matched nothing. */
    NoResults,

    /** Query matched stops. */
    Results
}
