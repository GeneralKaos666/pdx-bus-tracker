package com.trimettransit.tracker.ui.components

import org.junit.Assert.assertEquals
import org.junit.Test

class TransitColorsTest {

    @Test
    fun `MAX light rail and vintage trolley map to the M badge`() {
        listOf(90, 100, 190, 200, 290, 196).forEach { routeNumber ->
            assertEquals("route $routeNumber should be MAX", "M", transitBadgeLetter(routeNumber))
        }
    }

    @Test
    fun `MAX shuttle buses take the letter of the MAX line they substitute`() {
        listOf(287, 288, 291, 292, 293).forEach { routeNumber ->
            assertEquals("route $routeNumber should be MAX", "M", transitBadgeLetter(routeNumber))
        }
    }

    @Test
    fun `WES maps to the W badge`() {
        assertEquals("W", transitBadgeLetter(203))
    }

    @Test
    fun `streetcar loops map to the S badge`() {
        listOf(193, 194, 195).forEach { routeNumber ->
            assertEquals("route $routeNumber should be streetcar", "S", transitBadgeLetter(routeNumber))
        }
    }

    @Test
    fun `bus routes map to the B badge`() {
        listOf(1, 4, 9, 12, 99).forEach { routeNumber ->
            assertEquals("route $routeNumber should be bus", "B", transitBadgeLetter(routeNumber))
        }
    }

    @Test
    fun `bus routes numbered above ninety nine map to the B badge`() {
        // TriMet's 155-Sunnyside and 156-Mather Rd are bus lines with numbers over 99.
        listOf(155, 156).forEach { routeNumber ->
            assertEquals("route $routeNumber should be bus", "B", transitBadgeLetter(routeNumber))
        }
    }

    @Test
    fun `non positive or unknown route numbers map to no badge`() {
        assertEquals("", transitBadgeLetter(0))
        assertEquals("", transitBadgeLetter(-1))
    }
}