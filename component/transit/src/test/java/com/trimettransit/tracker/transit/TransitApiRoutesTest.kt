package com.trimettransit.tracker.transit

import com.trimettransit.tracker.model.Route
import org.json.JSONObject
import org.junit.Assert.assertEquals
import org.junit.Test

class TransitApiRoutesTest {

    @Test
    fun `missing route payload is an empty successful listing`() {
        assertEquals(emptyList<Route>(), TransitApi.parseRoutes(JSONObject()))
        assertEquals(emptyList<Route>(), TransitApi.parseRoutes(null))
    }

    @Test
    fun `route payload keeps valid routes and excludes the aerial tram`() {
        val result = TransitApi.parseRoutes(
            JSONObject(
                """
                {
                  "route": [
                    {"route": 9, "desc": "Powell", "type": "B"},
                    {"route": 90, "desc": "Portland Aerial Tram", "type": "R"}
                  ]
                }
                """
            )
        )
        assertEquals(listOf(9), result.map { it.routeId })
    }
}
