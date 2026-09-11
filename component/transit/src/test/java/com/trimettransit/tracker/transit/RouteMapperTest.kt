package com.trimettransit.tracker.transit

import org.json.JSONObject
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class RouteMapperTest {

    // parseRoute(desc, routeId, type)

    @Test
    fun `type B maps to bus`() {
        val route = TransitJsonMapper.parseRoute("Line 4", 4, "B")
        assertTrue(route.isBus)
        assertFalse(route.isMax)
        assertFalse(route.isStreetcar)
        assertFalse(route.isWes)
        assertEquals("B", route.typeLetter)
    }

    @Test
    fun `type R with MAX in the name maps to MAX`() {
        val route = TransitJsonMapper.parseRoute("MAX Red Line", 90, "R")
        assertTrue(route.isMax)
        assertFalse(route.isBus)
        assertEquals("M", route.typeLetter)
    }

    @Test
    fun `type R with Portland Streetcar maps to streetcar`() {
        val route = TransitJsonMapper.parseRoute("Portland Streetcar - B Loop", 195, "R")
        assertTrue(route.isStreetcar)
        assertEquals("S", route.typeLetter)
    }

    @Test
    fun `type R with WES maps to commuter rail`() {
        val route = TransitJsonMapper.parseRoute("WES Commuter Rail", 200, "R")
        assertTrue(route.isWes)
        assertEquals("W", route.typeLetter)
    }

    @Test
    fun `type R without a recognizable name maps to no mode`() {
        val route = TransitJsonMapper.parseRoute("Loop Route", 1, "R")
        assertFalse(route.isBus)
        assertFalse(route.isMax)
        assertFalse(route.isStreetcar)
        assertFalse(route.isWes)
        assertEquals("Z", route.typeLetter)
    }

    @Test
    fun `type B named Shuttle stays a bus`() {
        val route = TransitJsonMapper.parseRoute("Division Shuttle", 42, "B")
        assertTrue(route.isBus)
    }

    // parseRoute(obj)

    @Test
    fun `parseRoute reads desc route and type fields`() {
        val obj = JSONObject(
            "{\"desc\":\"MAX Red Line\",\"route\":90,\"type\":\"R\"}"
        )
        val route = TransitJsonMapper.parseRoute(obj)
        assertEquals("MAX Red Line", route.desc)
        assertEquals(90, route.routeId)
        assertTrue(route.isMax)
    }

    @Test
    fun `parseRoute defaults missing fields`() {
        val route = TransitJsonMapper.parseRoute(JSONObject("{}"))
        assertEquals(0, route.routeId)
        assertEquals("", route.desc)
        assertFalse(route.isMax)
    }

    // isValidCoordinate

    @Test
    fun `isValidCoordinate accepts Portland-like coordinates`() {
        assertTrue(TransitJsonMapper.isValidCoordinate(45.5231, -122.6765))
        assertTrue(TransitJsonMapper.isValidCoordinate(-90.0, -180.0))
        assertTrue(TransitJsonMapper.isValidCoordinate(90.0, 180.0))
    }

    @Test
    fun `isValidCoordinate rejects the 0,0 sentinel`() {
        assertFalse(TransitJsonMapper.isValidCoordinate(0.0, 0.0))
    }

    @Test
    fun `isValidCoordinate rejects out-of-range values`() {
        assertFalse(TransitJsonMapper.isValidCoordinate(90.1, 0.0))
        assertFalse(TransitJsonMapper.isValidCoordinate(0.0, -180.1))
        assertFalse(TransitJsonMapper.isValidCoordinate(-45.0, 200.0))
    }
}