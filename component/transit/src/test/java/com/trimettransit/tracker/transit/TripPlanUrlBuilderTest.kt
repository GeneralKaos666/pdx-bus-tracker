package com.trimettransit.tracker.transit

import com.trimettransit.tracker.model.TripPlannerMode
import com.trimettransit.tracker.model.TripRequestOptions
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class TripPlanUrlBuilderTest {

    private fun build(
        options: TripRequestOptions = TripRequestOptions(),
        arriveBy: Boolean = false
    ): String = buildTripPlannerRequestUrl(
        baseUrl = "https://developer.trimet.org/ws/V1/trips/tripplanner",
        apiKey = "demo-key",
        fromPlace = "Home",
        fromCoord = "-122.68295,45.52306",
        toPlace = "Work",
        toCoord = "-122.67621,45.52025",
        date = "9-11-2026",
        clock = "2:00 pm",
        arriveBy = arriveBy,
        options = options
    )

    @Test
    fun `default options reproduce the current request parameters exactly`() {
        val url = build()
        assertEquals("https://developer.trimet.org/ws/V1/trips/tripplanner", url.substringBefore("/fromPlace/"))
        assertTrue(url.contains("/fromPlace/Home"))
        assertTrue(url.contains("/fromCoord/-122.68295,45.52306"))
        assertTrue(url.contains("/toPlace/Work"))
        assertTrue(url.contains("/toCoord/-122.67621,45.52025"))
        assertTrue(url.contains("/date/9-11-2026"))
        assertTrue(url.contains("/time/2:00 pm"))
        assertTrue(url.contains("/arr/D"))
        assertTrue(url.contains("/min/T"))
        assertTrue(url.contains("/mode/A"))
        assertTrue(url.contains("/walk/0.5"))
        assertTrue(url.contains("/maxIntineraries/3"))
        assertTrue(url.endsWith("/format/xml/appID/demo-key"))
    }

    @Test
    fun `arrive by emits arr A`() {
        val url = build(arriveBy = true)
        assertTrue(url.contains("/arr/A"))
    }

    @Test
    fun `bus mode emits mode B`() {
        assertTrue(build(TripRequestOptions(mode = TripPlannerMode.BUS)).contains("/mode/B"))
    }

    @Test
    fun `train mode emits mode T`() {
        assertTrue(build(TripRequestOptions(mode = TripPlannerMode.TRAIN)).contains("/mode/T"))
    }

    @Test
    fun `custom walk distance is emitted to one decimal`() {
        assertTrue(build(TripRequestOptions(maxWalkMiles = 0.7f)).contains("/walk/0.7"))
    }

    @Test
    fun `custom itinerary count is emitted`() {
        assertTrue(build(TripRequestOptions(itineraryCount = 5)).contains("/maxIntineraries/5"))
    }
}