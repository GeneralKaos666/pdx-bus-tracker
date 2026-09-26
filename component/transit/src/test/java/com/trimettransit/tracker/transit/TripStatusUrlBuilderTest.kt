package com.trimettransit.tracker.transit

import org.junit.Assert.assertEquals
import org.junit.Test

class TripStatusUrlBuilderTest {
    @Test
    fun `builds trip status url with filters`() {
        assertEquals(
            "https://developer.trimet.org/ws/v2/tripStatus/appID/KEY/tripIDs/T1,T2/blockIDs/42/showRoutes/true/showStops/true",
            buildTripStatusUrl(
                "https://developer.trimet.org/ws/v2/tripStatus",
                "KEY",
                tripIds = listOf("T1", "T2"),
                blockIds = listOf(42)
            )
        )
    }

    @Test
    fun `encodes tripIds path segments`() {
        assertEquals(
            "https://developer.trimet.org/ws/v2/tripStatus/appID/KEY/tripIDs/A%2FB,C%3FD,E%23F/showRoutes/true/showStops/true",
            buildTripStatusUrl(
                "https://developer.trimet.org/ws/v2/tripStatus",
                "KEY",
                tripIds = listOf("A/B", "C?D", "E#F")
            )
        )
    }

    @Test
    fun `builds block status url and omits disabled flags`() {
        assertEquals(
            "https://developer.trimet.org/ws/v2/blockStatus/appID/KEY/blockID/42/blockIDs/43,44",
            buildBlockStatusUrl(
                "https://developer.trimet.org/ws/v2/blockStatus",
                "KEY",
                blockId = 42,
                blockIds = listOf(43, 44),
                showRoutes = false,
                showStops = false
            )
        )
    }
}
