package com.trimettransit.tracker.transit

import org.junit.Assert.assertEquals
import org.junit.Test

class VehiclesUrlBuilderTest {

    private val baseUrl = "https://developer.trimet.org/ws/v2/vehicles"

    @Test fun `builds minimal url with defaults`() {
        val url = buildVehiclesUrl(baseUrl = baseUrl, apiKey = "KEY")
        assertEquals("https://developer.trimet.org/ws/v2/vehicles/appID/KEY", url)
    }

    @Test fun `appends route block id filters`() {
        val url = buildVehiclesUrl(
            baseUrl = baseUrl,
            apiKey = "KEY",
            routes = listOf(4, 9),
            blocks = listOf(3401),
            ids = listOf(3518, 3519)
        )
        assertEquals(
            "https://developer.trimet.org/ws/v2/vehicles/appID/KEY/routes/4,9/blocks/3401/ids/3518,3519",
            url
        )
    }

    @Test fun `appends bbox and since`() {
        val url = buildVehiclesUrl(
            baseUrl = baseUrl,
            apiKey = "KEY",
            bbox = "-122.8,45.4,-122.5,45.6",
            since = 1_700_000_000_000L
        )
        assertEquals(
            "https://developer.trimet.org/ws/v2/vehicles/appID/KEY/bbox/-122.8,45.4,-122.5,45.6/since/1700000000000",
            url
        )
    }

    @Test fun `omits onRouteOnly false only, keeps default true implicit`() {
        val url = buildVehiclesUrl(baseUrl = baseUrl, apiKey = "KEY", onRouteOnly = true)
        assertEquals("https://developer.trimet.org/ws/v2/vehicles/appID/KEY", url)
    }

    @Test fun `appends showNonRevenue onRouteOnly false and showStale when set`() {
        val url = buildVehiclesUrl(
            baseUrl = baseUrl,
            apiKey = "KEY",
            showNonRevenue = true,
            onRouteOnly = false,
            showStale = true
        )
        assertEquals(
            "https://developer.trimet.org/ws/v2/vehicles/appID/KEY/showNonRevenue/true/onRouteOnly/false/showStale/true",
            url
        )
    }

    @Test fun `ignores empty filter lists and blank bbox`() {
        val url = buildVehiclesUrl(
            baseUrl = baseUrl,
            apiKey = "KEY",
            routes = emptyList(),
            blocks = emptyList(),
            ids = emptyList(),
            bbox = ""
        )
        assertEquals("https://developer.trimet.org/ws/v2/vehicles/appID/KEY", url)
    }
}
