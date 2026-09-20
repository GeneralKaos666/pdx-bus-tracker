package com.trimettransit.tracker.transit

import org.json.JSONObject
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class AlertsJsonMapperTest {

    private fun parse(payload: String) = TransitJsonMapper.parseAlerts(JSONObject(payload))

    @Test
    fun `parseAlerts maps route-scoped and system-wide alerts`() {
        val json = """
            {
              "alert": [
                {
                  "id": 7001,
                  "header_text": "MAX Green Line reroute",
                  "desc": "Green to City Center uses the Steel Bridge.",
                  "info_link_url": "https://trimet.org/alerts/7001",
                  "route": [ { "route": 200 } ]
                },
                {
                  "id": 7002,
                  "desc": "TriMet service update",
                  "system_wide_flag": true
                }
              ]
            }
        """.trimIndent()

        val alerts = parse(json)
        assertEquals(2, alerts.size)

        val routeScoped = alerts[0]
        assertEquals(7001, routeScoped.id)
        assertEquals("MAX Green Line reroute", routeScoped.header)
        assertEquals("Green to City Center uses the Steel Bridge.", routeScoped.desc)
        assertEquals("https://trimet.org/alerts/7001", routeScoped.infoLinkUrl)
        assertFalse(routeScoped.systemWide)
        assertEquals(listOf(200), routeScoped.routeIds)
        assertEquals("MAX Green Line reroute", routeScoped.displayTitle)

        val systemWide = alerts[1]
        assertEquals(7002, systemWide.id)
        assertEquals("TriMet service update", systemWide.desc)
        assertTrue(systemWide.systemWide)
        assertTrue(systemWide.routeIds.isEmpty())
        assertNull(systemWide.infoLinkUrl)
        assertEquals("TriMet service update", systemWide.displayTitle)
    }

    @Test
    fun `parseAlerts prefers singular route before routes fallback`() {
        val json = """
            {
              "alert": [
                { "id": 10, "desc": "Fallback routes", "routes": [4, 9] },
                { "id": 11, "desc": "No routes" }
              ]
            }
        """.trimIndent()

        val alerts = parse(json)
        assertEquals(listOf(4, 9), alerts[0].routeIds)
        assertTrue(alerts[1].routeIds.isEmpty())
    }

    @Test
    fun `parseAlerts ignores an empty singular route array and falls back to routes`() {
        val json = """
            {
              "alert": [
                { "id": 20, "desc": "Empty singular", "route": [], "routes": [15] }
              ]
            }
        """.trimIndent()

        val alerts = parse(json)
        assertEquals(listOf(15), alerts[0].routeIds)
    }

    @Test
    fun `parseAlerts skips malformed elements without voiding the response`() {
        val json = """
            { "alert": [ "not-an-object", { "id": 30, "desc": "Valid" } ] }
        """.trimIndent()

        val alerts = parse(json)
        assertEquals(1, alerts.size)
        assertEquals(30, alerts[0].id)
    }

    @Test
    fun `parseAlerts treats missing alert array as empty`() {
        assertTrue(parse("{}").isEmpty())
    }
}
