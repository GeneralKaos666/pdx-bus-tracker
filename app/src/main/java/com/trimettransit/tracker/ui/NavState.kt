package com.trimettransit.tracker.ui

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import com.trimettransit.tracker.data.model.Route

object NavState {
    var preselectedRoute by mutableStateOf<Route?>(null)

    fun consumeRouteSelection(): Route? {
        val route = preselectedRoute
        preselectedRoute = null
        return route
    }
}
