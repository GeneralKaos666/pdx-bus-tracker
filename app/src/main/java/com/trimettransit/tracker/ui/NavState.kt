package com.trimettransit.tracker.ui

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import com.trimettransit.tracker.data.model.Direction
import com.trimettransit.tracker.data.model.Route

object NavState {
    var savedRoute by mutableStateOf<Route?>(null)
    var savedDirection by mutableStateOf<Direction?>(null)

    fun consumeRouteSelection(): Route? {
        val route = savedRoute
        savedRoute = null
        return route
    }

    fun consumeDirectionSelection(): Direction? {
        val direction = savedDirection
        savedDirection = null
        return direction
    }

    // Arrivals bridge — written by ArrivalsScreen, read by outer scaffold top bar
    var arrivalsStopName by mutableStateOf("")
    var arrivalsIsFavorite by mutableStateOf(false)
    var arrivalsLat by mutableStateOf(0.0)
    var arrivalsLng by mutableStateOf(0.0)
    var arrivalsOnRefresh: (() -> Unit)? = null

    fun clearArrivals() {
        arrivalsStopName = ""
        arrivalsIsFavorite = false
        arrivalsOnRefresh = null
        arrivalsLat = 0.0
        arrivalsLng = 0.0
    }
}
