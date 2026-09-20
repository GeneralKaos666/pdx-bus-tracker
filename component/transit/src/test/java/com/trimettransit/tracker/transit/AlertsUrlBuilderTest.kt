package com.trimettransit.tracker.transit

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class AlertsUrlBuilderTest {

    private val baseUrl = "https://developer.trimet.org/ws/v2/alerts"

    @Test fun `refuses an unscoped request with both filters null`() {
        assertFalse(isAlertsScopeValid(routes = null, locIds = null))
    }

    @Test fun `refuses an unscoped request with both filters empty`() {
        assertFalse(isAlertsScopeValid(routes = emptyList(), locIds = emptyList()))
    }

    @Test fun `accepts a routes-only scope`() {
        assertTrue(isAlertsScopeValid(routes = listOf(4), locIds = null))
    }

    @Test fun `accepts a locIDs-only scope`() {
        assertTrue(isAlertsScopeValid(routes = null, locIds = listOf(5001)))
    }

    @Test fun `accepts both filters supplied`() {
        assertTrue(isAlertsScopeValid(routes = listOf(4), locIds = listOf(5001)))
    }

    @Test fun `builds url with routes only`() {
        val url = buildAlertsUrl(baseUrl = baseUrl, apiKey = "KEY", routes = listOf(4, 9))
        assertEquals("https://developer.trimet.org/ws/v2/alerts/appID/KEY/routes/4,9", url)
    }

    @Test fun `builds url with locIDs only`() {
        val url = buildAlertsUrl(baseUrl = baseUrl, apiKey = "KEY", locIds = listOf(5001, 7603))
        assertEquals("https://developer.trimet.org/ws/v2/alerts/appID/KEY/locIDs/5001,7603", url)
    }

    @Test fun `builds url with both routes and locIDs`() {
        val url = buildAlertsUrl(
            baseUrl = baseUrl,
            apiKey = "KEY",
            routes = listOf(8, 15),
            locIds = listOf(5001)
        )
        assertEquals(
            "https://developer.trimet.org/ws/v2/alerts/appID/KEY/routes/8,15/locIDs/5001",
            url
        )
    }

    @Test fun `ignores empty filter lists`() {
        val url = buildAlertsUrl(baseUrl = baseUrl, apiKey = "KEY", routes = emptyList(), locIds = emptyList())
        assertEquals("https://developer.trimet.org/ws/v2/alerts/appID/KEY", url)
    }
}
