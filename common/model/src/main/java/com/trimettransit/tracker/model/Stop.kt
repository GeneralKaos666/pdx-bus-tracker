package com.trimettransit.tracker.model

data class Stop(
    var desc: String = "",
    var dirDesc: String = "",
    var latitude: Double = 0.0,
    var longitude: Double = 0.0,
    var transitType: String = "",
    var routeNum: Int = 0,
    var locId: Int = 0,
    var routes: MutableList<Route>? = null
) {
    fun computeTransitType() {
        routes?.forEach { route ->
            if (transitType.isNullOrEmpty()) {
                when {
                    route.isStreetcar -> transitType = "S"
                    route.isBus && !route.desc.contains("Shuttle") -> transitType = "B"
                    route.isMax || route.desc.contains("Vintage Trolley") -> transitType = "M"
                    route.isWes -> transitType = "W"
                    else -> transitType = "Z"
                }
            }
        }
    }

    fun addRoute(route: Route) {
        val list = routes ?: mutableListOf<Route>().also { routes = it }
        list.add(route)
    }
}
