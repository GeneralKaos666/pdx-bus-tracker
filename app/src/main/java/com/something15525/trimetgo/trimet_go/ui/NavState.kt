package com.something15525.trimetgo.trimet_go.ui

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import com.something15525.trimetgo.trimet_go.data.model.Route

object NavState {
    var preselectedRoute by mutableStateOf<Route?>(null)

    fun consumeRouteSelection(): Route? {
        val route = preselectedRoute
        preselectedRoute = null
        return route
    }
}
