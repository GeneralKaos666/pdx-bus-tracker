package com.trimettransit.tracker.transit

import org.junit.Assert.assertEquals
import org.junit.Test

class StopsByLocationUrlBuilderTest {

    private val baseUrl = "https://developer.trimet.org/ws/v2/stops"

    @Test fun `builds minimal url with defaults, unchanged from before arrivals params existed`() {
        val url = buildStopsByLocationUrl(baseUrl = baseUrl, apiKey = "KEY", ll = "45.52,-122.67")
        assertEquals(
            "https://developer.trimet.org/ws/v2/stops/appID/KEY/ll/45.52,-122.67/showRoutes/true",
            url
        )
    }

    @Test fun `omits arrivals params entirely when includeArrivals is false`() {
        val url = buildStopsByLocationUrl(
            baseUrl = baseUrl,
            apiKey = "KEY",
            ll = "45.52,-122.67",
            maxStopArrivals = 25,
            minutes = 30,
            maxArrivals = 1,
            showRouteDirs = true
        )
        // includeArrivals defaults to false: maxStopArrivals/minutes/maxArrivals/embedArrivals
        // must not leak into the URL, but showRouteDirs is independent and still applies.
        assertEquals(
            "https://developer.trimet.org/ws/v2/stops/appID/KEY/ll/45.52,-122.67/showRoutes/true/showRouteDirs/true",
            url
        )
    }

    @Test fun `appends embedArrivals and stop arrival tuning when includeArrivals is true`() {
        val url = buildStopsByLocationUrl(
            baseUrl = baseUrl,
            apiKey = "KEY",
            ll = "45.52,-122.67",
            includeArrivals = true,
            maxStopArrivals = 25,
            minutes = 30,
            maxArrivals = 1
        )
        assertEquals(
            "https://developer.trimet.org/ws/v2/stops/appID/KEY/ll/45.52,-122.67/showRoutes/true" +
                "/embedArrivals/true/maxStopArrivals/25/minutes/30/arrivals/1",
            url
        )
    }

    @Test fun `omits embedArrivals segment when embedArrivals is false`() {
        val url = buildStopsByLocationUrl(
            baseUrl = baseUrl,
            apiKey = "KEY",
            ll = "45.52,-122.67",
            includeArrivals = true,
            embedArrivals = false,
            maxStopArrivals = 10
        )
        assertEquals(
            "https://developer.trimet.org/ws/v2/stops/appID/KEY/ll/45.52,-122.67/showRoutes/true" +
                "/maxStopArrivals/10",
            url
        )
    }

    @Test fun `appends feet, meters, bbox, maxStops, and omits showRoutes when false`() {
        val url = buildStopsByLocationUrl(
            baseUrl = baseUrl,
            apiKey = "KEY",
            ll = "45.52,-122.67",
            feet = 500,
            meters = 1000,
            bbox = "-122.8,45.4,-122.5,45.6",
            maxStops = 25,
            showRoutes = false
        )
        assertEquals(
            "https://developer.trimet.org/ws/v2/stops/appID/KEY/ll/45.52,-122.67/feet/500/meters/1000" +
                "/bbox/-122.8,45.4,-122.5,45.6/maxStops/25",
            url
        )
    }
}
