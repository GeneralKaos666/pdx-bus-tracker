package com.trimettransit.tracker.model

import org.junit.Assert.assertEquals
import org.junit.Test

class TripRequestOptionsTest {

    @Test
    fun `defaults match the current hardcoded request parameters`() {
        val options = TripRequestOptions()
        assertEquals(TripPlannerMode.ALL, options.mode)
        assertEquals(0.5f, options.maxWalkMiles)
        assertEquals(3, options.itineraryCount)
    }

    @Test
    fun `mode exposes the TriMet wire code`() {
        assertEquals("A", TripPlannerMode.ALL.wsCode)
        assertEquals("B", TripPlannerMode.BUS.wsCode)
        assertEquals("T", TripPlannerMode.TRAIN.wsCode)
    }

    @Test(expected = IllegalArgumentException::class)
    fun `walk distance below the API floor is rejected`() {
        TripRequestOptions(maxWalkMiles = 0.009f)
    }

    @Test(expected = IllegalArgumentException::class)
    fun `walk distance above the API ceiling is rejected`() {
        TripRequestOptions(maxWalkMiles = 1f)
    }

    @Test(expected = IllegalArgumentException::class)
    fun `itinerary count below one is rejected`() {
        TripRequestOptions(itineraryCount = 0)
    }

    @Test(expected = IllegalArgumentException::class)
    fun `itinerary count above six is rejected`() {
        TripRequestOptions(itineraryCount = 7)
    }
}