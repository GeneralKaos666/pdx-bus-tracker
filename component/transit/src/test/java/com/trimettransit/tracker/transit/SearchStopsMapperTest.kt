package com.trimettransit.tracker.transit

import org.json.JSONObject
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class SearchStopsMapperTest {

    @Test
    fun `parseSearchStops dedupes stops shared by routes and picks the primary route`() {
        val json = """
            {
              "route": [
                {
                  "route": 9, "desc": "Division", "type": "B",
                  "dir": [
                    {
                      "desc": "Eastbound",
                      "stop": [
                        { "locid": 5, "desc": "SE Division & 60th", "lat": 45.1, "lng": -122.5 },
                        { "locid": 7, "desc": "SE Division & 70th", "lat": 45.2, "lng": -122.6 }
                      ]
                    },
                    {
                      "desc": "Westbound",
                      "stop": [
                        { "locid": 5, "desc": "SE Division & 60th", "lat": 45.1, "lng": -122.5, "dir": "Westbound" }
                      ]
                    }
                  ]
                },
                {
                  "route": 19, "desc": "Division", "type": "B",
                  "dir": [
                    {
                      "desc": "Eastbound",
                      "stop": [
                        { "locid": 5, "desc": "SE Division & 60th", "lat": 45.1, "lng": -122.5 },
                        { "locid": 8, "desc": "POINTLESS", "lat": 0, "lng": 0 }
                      ]
                    }
                  ]
                },
                {
                  "route": 30, "desc": "Estacada", "type": "B",
                  "dir": [
                    {
                      "desc": "Eastbound",
                      "stop": [
                        { "locid": 5, "desc": "SE Division & 60th", "lat": 45.1, "lon": -122.5 }
                      ]
                    }
                  ]
                }
              ]
            }
        """.trimIndent()

        val stops = TransitJsonMapper.parseSearchStops(JSONObject(json))!!

        // The 0,0 "stop" is malformed and must be dropped.
        assertEquals(2, stops.size)

        val s5 = stops.first { it.locId == 5 }
        // Production appends the route once per direction that lists the stop, so an
        // opposite-direction pair for the same route shows the route twice.
        assertEquals(listOf(9, 9, 19, 30), s5.routes.map { it.routeId })
        assertEquals(9, s5.routeNum)
        assertEquals("SE Division & 60th", s5.desc)
        // First-seen direction description wins over a later stop-level "dir".
        assertEquals("Eastbound", s5.dirDesc)
        // Longitude falls back to the "lon" key on the last route's listing.
        assertEquals(45.1, s5.latitude, 0.0)
        assertEquals(-122.5, s5.longitude, 0.0)
        assertEquals("B", s5.transitType)

        val s7 = stops.first { it.locId == 7 }
        assertEquals(listOf(9), s7.routes.map { it.routeId })
        assertEquals(9, s7.routeNum)
        assertEquals("Eastbound", s7.dirDesc)
    }

    @Test
    fun `parseSearchStops returns null when routes are missing`() {
        assertNull(TransitJsonMapper.parseSearchStops(JSONObject("{}")))
    }

    @Test
    fun `parseSearchStops returns empty list when routes hold no stops`() {
        val json = """
            { "route": [ { "route": 9, "desc": "Division", "type": "B", "dir": [] } ] }
        """.trimIndent()
        assertTrue(TransitJsonMapper.parseSearchStops(JSONObject(json)).isNullOrEmpty())
    }

    @Test
    fun `parseStopsByLocation maps stops with their routes`() {
        val json = """
            {
              "location": [
                {
                  "desc": "NW 6th & Davis", "dir": "Northbound",
                  "lat": 45.52, "lng": -122.6762, "locid": 9978,
                  "route": [
                    { "route": 15, "desc": "NW 23rd", "type": "B" },
                    { "route": 35, "desc": "Macadam", "type": "B" }
                  ]
                }
              ]
            }
        """.trimIndent()

        val stops = TransitJsonMapper.parseStopsByLocation(JSONObject(json))
        assertEquals(1, stops.size)
        val stop = stops[0]
        assertEquals(9978, stop.locId)
        assertEquals("NW 6th & Davis", stop.desc)
        assertEquals("Northbound", stop.dirDesc)
        assertEquals(45.52, stop.latitude, 0.0)
        assertEquals(-122.6762, stop.longitude, 0.0)
        assertEquals(listOf(15, 35), stop.routes.map { it.routeId })
        assertEquals("B", stop.transitType)
    }

    @Test
    fun `parseStopsByLocation returns empty when no locations present`() {
        assertTrue(TransitJsonMapper.parseStopsByLocation(JSONObject("{}")).isEmpty())
    }

    @Test
    fun `parseStopById maps the first location`() {
        val json = """
            {
              "location": [
                { "desc": "NW 6th & Davis", "dir": "Northbound", "lat": 45.52, "lng": -122.6762,
                  "locid": 9978,
                  "route": [ { "route": 15, "desc": "NW 23rd", "type": "B" } ] }
              ]
            }
        """.trimIndent()

        val stop = TransitJsonMapper.parseStopById(JSONObject(json))
        assertEquals(9978, stop!!.locId)
        assertEquals(listOf(15), stop.routes.map { it.routeId })
        assertEquals("B", stop.transitType)
    }

    @Test
    fun `parseStopById returns null when there is no location`() {
        assertNull(TransitJsonMapper.parseStopById(JSONObject("{}")))
        assertNull(TransitJsonMapper.parseStopById(JSONObject("""{ "location": [] }""")))
    }
}