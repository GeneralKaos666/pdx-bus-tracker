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

    // Bottom bar scroll-hide bridge — written by AutoHideBottomBarEffect, read by the outer scaffold
    var bottomBarVisible by mutableStateOf(true)

    fun clearArrivals() {
        arrivalsStopName = ""
        arrivalsIsFavorite = false
        arrivalsOnRefresh = null
        arrivalsLat = 0.0
        arrivalsLng = 0.0
    }
}
