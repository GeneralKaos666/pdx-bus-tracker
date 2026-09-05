package com.trimettransit.tracker.ui

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableDoubleStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue

object NavState {
    // Arrivals bridge — written by ArrivalsScreen, read by outer scaffold top bar
    var arrivalsStopName by mutableStateOf("")
    var arrivalsIsFavorite by mutableStateOf(false)
    var arrivalsLat by mutableDoubleStateOf(0.0)
    var arrivalsLng by mutableDoubleStateOf(0.0)
    var arrivalsOnRefresh by mutableStateOf<(() -> Unit)?>(null)

    // Scroll-to-top bridge — written by whichever sub-screen is active, invoked
    // by the collapsed bottom-bar context pill. Scrolls the list back to the top
    // (and refreshes, on Arrivals).
    var onScrollToTop by mutableStateOf<(() -> Unit)?>(null)

    fun clearArrivals() {
        arrivalsStopName = ""
        arrivalsIsFavorite = false
        arrivalsOnRefresh = null
        onScrollToTop = null
        arrivalsLat = 0.0
        arrivalsLng = 0.0
    }
}
