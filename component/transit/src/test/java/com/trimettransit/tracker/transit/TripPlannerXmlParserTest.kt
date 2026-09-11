package com.trimettransit.tracker.transit

import com.trimettransit.tracker.model.TripLegMode
import com.trimettransit.tracker.model.TripPlannerError
import com.trimettransit.tracker.model.TripPlanResult
import org.joda.time.DateTime
import org.joda.time.DateTimeZone
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

class TripPlannerXmlParserTest {

    private var originalZone: DateTimeZone? = null

    @Before
    fun pinToUtc() {
        originalZone = DateTimeZone.getDefault()
        DateTimeZone.setDefault(DateTimeZone.UTC)
    }

    @After
    fun restoreZone() {
        originalZone?.let(DateTimeZone::setDefault)
    }

    private val successXml = """
        <response>
          <from>
            <pos><lat>45.51877</lat><lon>-122.65746</lon></pos>
            <description>SE 39th%20and%20Hawthorne</description>
          </from>
          <to>
            <pos><lat>45.5231</lat><lon>-122.6765</lon></pos>
            <description>Pioneer%20Courthouse%20Square</description>
          </to>
          <itineraries>
            <itinerary id="00000188.d0b3d9e8">
              <time-distance>
                <date>4/26/26</date>
                <startTime>10:09 AM</startTime>
                <endTime>10:43 AM</endTime>
                <duration>34</duration>
                <distance>9.2</distance>
                <walkingTime>11</walkingTime>
                <transitTime>18</transitTime>
                <waitingTime>5</waitingTime>
                <numberOfTransfers>1</numberOfTransfers>
              </time-distance>
              <fare><regular>2.50</regular></fare>
              <leg mode="8" order="1">
                <from>
                  <pos><lat>45.51877</lat><lon>-122.65746</lon></pos>
                  <description>SE 39th%20and%20Hawthorne</description>
                </from>
                <to>
                  <pos><lat>45.52000</lat><lon>-122.66000</lon></pos>
                  <description>SE 39th and Main</description>
                </to>
                <time-distance>
                  <date>4/26/26</date>
                  <startTime>10:09 AM</startTime>
                  <endTime>10:20 AM</endTime>
                </time-distance>
              </leg>
              <leg mode="3" order="thru-route">
                <from>
                  <pos><lat>45.52000</lat><lon>-122.66000</lon></pos>
                  <description>SE 39th and Main</description>
                </from>
                <to>
                  <pos><lat>45.5231</lat><lon>-122.6765</lon></pos>
                  <description>Pioneer%20Courthouse%20Square</description>
                </to>
                <route>
                  <number>14</number>
                  <name>Fremont</name>
                  <direction>Westbound</direction>
                </route>
                <time-distance>
                  <date>4/26/26</date>
                  <startTime>10:20 AM</startTime>
                  <endTime>10:43 AM</endTime>
                </time-distance>
              </leg>
            </itinerary>
          </itineraries>
        </response>
    """.trimIndent()

    // Success path

    @Test
    fun `parseTripPlanResponse maps a full response`() {
        val result = TripPlannerXmlParser.parseTripPlanResponse(successXml) as TripPlanResult.Success
        val plan = result.plan!!

        assertEquals(45.51877, plan.from.latitude, 0.0)
        assertEquals(-122.65746, plan.from.longitude, 0.0)
        assertEquals(45.5231, plan.to.latitude, 0.0)
        assertEquals(-122.6765, plan.to.longitude, 0.0)

        assertEquals(1, plan.itineraries.size)
        val itinerary = plan.itineraries[0]
        assertEquals("00000188.d0b3d9e8", itinerary.id)
        assertEquals(DateTime(2026, 4, 26, 10, 9), itinerary.departure)
        assertEquals(DateTime(2026, 4, 26, 10, 43), itinerary.arrival)
        assertEquals(34 * 60_000L, itinerary.durationMillis)
        assertEquals(9.2 * 1609.344, itinerary.distanceMeters, 0.001)
        assertEquals(11 * 60_000L, itinerary.walkTimeMillis)
        assertEquals(18 * 60_000L, itinerary.transitTimeMillis)
        assertEquals(5 * 60_000L, itinerary.waitingTimeMillis)
        assertEquals(1, itinerary.numberOfTransfers)
        assertEquals("2.50", itinerary.fare)

        assertEquals(2, itinerary.legs.size)
        val walk = itinerary.legs[0]
        assertEquals(TripLegMode.WALK, walk.mode)
        assertFalse(walk.stayOnBoard)
        assertNull(walk.routeNumber)
        assertEquals(DateTime(2026, 4, 26, 10, 9), walk.departure)
        assertEquals(DateTime(2026, 4, 26, 10, 20), walk.arrival)

        val bus = itinerary.legs[1]
        assertEquals(TripLegMode.BUS, bus.mode)
        assertTrue(bus.stayOnBoard)
        assertEquals("14", bus.routeNumber)
        assertEquals("Fremont", bus.routeName)
        assertEquals("Westbound", bus.direction)
        assertEquals(45.52000, bus.from.latitude, 0.0)
        assertEquals(45.5231, bus.to.latitude, 0.0)
    }

    @Test
    fun `parseTripPlanResponse parses multiple itineraries`() {
        val xml = """
            <response>
              <from/><to/>
              <itineraries>
                <itinerary id="i1"><time-distance><date>4/26/26</date><startTime>10:09 AM</startTime><endTime>10:43 AM</endTime><duration>34</duration></time-distance><leg mode="8" order="1"/></itinerary>
                <itinerary id="i2"><time-distance><date>4/26/26</date><startTime>11:09 AM</startTime><endTime>11:43 AM</endTime><duration>34</duration></time-distance><leg mode="8" order="1"/></itinerary>
              </itineraries>
            </response>
        """.trimIndent()

        val result = TripPlannerXmlParser.parseTripPlanResponse(xml) as TripPlanResult.Success
        assertEquals(listOf("i1", "i2"), result.plan!!.itineraries.map { it.id })
    }

    @Test
    fun `durationMillis falls back to end minus start`() {
        val xml = """
            <response>
              <from/><to/>
              <itineraries>
                <itinerary id="i1">
                  <time-distance><date>4/26/26</date><startTime>10:09 AM</startTime><endTime>10:43 AM</endTime></time-distance>
                  <leg mode="8" order="1"/>
                </itinerary>
              </itineraries>
            </response>
        """.trimIndent()

        val result = TripPlannerXmlParser.parseTripPlanResponse(xml) as TripPlanResult.Success
        assertEquals(34 * 60_000L, result.plan!!.itineraries[0].durationMillis)
    }

    @Test
    fun `walk legs without times map to null departure and arrival`() {
        val xml = """
            <response>
              <from/><to/>
              <itineraries>
                <itinerary id="i1">
                  <time-distance><date>4/26/26</date><startTime>10:09 AM</startTime><endTime>10:20 AM</endTime><duration>11</duration><distance>1.0</distance></time-distance>
                  <leg mode="8" order="1"/>
                </itinerary>
              </itineraries>
            </response>
        """.trimIndent()

        val result = TripPlannerXmlParser.parseTripPlanResponse(xml) as TripPlanResult.Success
        val leg = result.plan!!.itineraries[0].legs.single()
        assertEquals(TripLegMode.WALK, leg.mode)
        assertNull(leg.departure)
        assertNull(leg.arrival)
    }

    @Test
    fun `itinerary without time-distance is skipped`() {
        val xml = """
            <response>
              <from/><to/>
              <itineraries>
                <itinerary id="i1"><leg mode="8" order="1"/></itinerary>
              </itineraries>
            </response>
        """.trimIndent()

        val result = TripPlannerXmlParser.parseTripPlanResponse(xml) as TripPlanResult.Success
        assertTrue(result.plan!!.itineraries.isEmpty())
    }

    @Test
    fun `leg mode maps streetcar codes and textual modes`() {
        val xml = """
            <response>
              <from/><to/>
              <itineraries>
                <itinerary id="i1">
                  <time-distance><date>4/26/26</date><startTime>10:09 AM</startTime><endTime>10:20 AM</endTime><duration>11</duration></time-distance>
                  <leg mode="13" order="1"/>
                  <leg mode="WALK" order="2"/>
                </itinerary>
              </itineraries>
            </response>
        """.trimIndent()

        val result = TripPlannerXmlParser.parseTripPlanResponse(xml) as TripPlanResult.Success
        val legs = result.plan!!.itineraries[0].legs
        assertEquals(TripLegMode.STREETCAR, legs[0].mode)
        assertEquals(TripLegMode.WALK, legs[1].mode)
    }

    @Test
    fun `leg direction falls back to the leg element`() {
        val xml = """
            <response>
              <from/><to/>
              <itineraries>
                <itinerary id="i1">
                  <time-distance><date>4/26/26</date><startTime>10:09 AM</startTime><endTime>10:20 AM</endTime><duration>11</duration></time-distance>
                  <leg mode="3" order="1"><direction>Northbound</direction></leg>
                </itinerary>
              </itineraries>
            </response>
        """.trimIndent()

        val result = TripPlannerXmlParser.parseTripPlanResponse(xml) as TripPlanResult.Success
        assertEquals("Northbound", result.plan!!.itineraries[0].legs[0].direction)
    }

    @Test
    fun `blank fare maps to null`() {
        val xml = """
            <response>
              <from/><to/>
              <itineraries>
                <itinerary id="i1">
                  <time-distance><date>4/26/26</date><startTime>10:09 AM</startTime><endTime>10:20 AM</endTime><duration>11</duration></time-distance>
                  <leg mode="8" order="1"/>
                  <fare><regular> </regular></fare>
                </itinerary>
              </itineraries>
            </response>
        """.trimIndent()

        val result = TripPlannerXmlParser.parseTripPlanResponse(xml) as TripPlanResult.Success
        assertNull(result.plan!!.itineraries[0].fare)
    }

    // Error + edge cases

    @Test
    fun `parseTripPlanResponse maps error codes`() {
        val codeTest = fun(code: String, expected: TripPlannerError) {
            val xml = """<response><error code="$code">Something happened</error></response>"""
            val result = TripPlannerXmlParser.parseTripPlanResponse(xml) as TripPlanResult.Error
            assertEquals(expected, result.error)
        }
        codeTest("20007", TripPlannerError.TRIP_NOT_POSSIBLE)
        codeTest("20003", TripPlannerError.NO_STOPS_NEAR_ORIGIN)
        codeTest("20001", TripPlannerError.SYSTEM_OUTAGE)
        codeTest("99999", TripPlannerError.UNKNOWN)
        codeTest("", TripPlannerError.UNKNOWN)
    }

    @Test
    fun `parseTripPlanResponse returns null for a foreign root`() {
        assertNull(TripPlannerXmlParser.parseTripPlanResponse("<foo/>"))
    }

    @Test
    fun `parseTripPlanResponse reports system outage on malformed xml`() {
        val result = TripPlannerXmlParser.parseTripPlanResponse("<response><itineraries>") as TripPlanResult.Error
        assertEquals(TripPlannerError.SYSTEM_OUTAGE, result.error)
    }

    @Test
    fun `parseTripPlanResponse succeeds with no itineraries`() {
        val result = TripPlannerXmlParser.parseTripPlanResponse("<response><from/><to/></response>") as TripPlanResult.Success
        assertTrue(result.plan!!.itineraries.isEmpty())
    }

    // parseMillis

    @Test
    fun `parseMillis parses 12 hour and 24 hour clock times`() {
        val am = TripPlannerXmlParser.parseMillis("4/26/26", "10:09 AM")
        assertEquals(DateTime(2026, 4, 26, 10, 9).millis, am)

        val pm = TripPlannerXmlParser.parseMillis("4/26/26", "10:15 PM")
        assertEquals(DateTime(2026, 4, 26, 22, 15).millis, pm)

        val twentyFour = TripPlannerXmlParser.parseMillis("4/26/26", "07:05")
        assertEquals(DateTime(2026, 4, 26, 7, 5).millis, twentyFour)
    }

    @Test
    fun `parseMillis handles dash-separated dates`() {
        val dash = TripPlannerXmlParser.parseMillis("4-26-26", "9:05 AM")
        assertEquals(DateTime(2026, 4, 26, 9, 5).millis, dash)
    }

    @Test
    fun `parseMillis returns null for blank or garbage input`() {
        assertNull(TripPlannerXmlParser.parseMillis("4/26/26", ""))
        assertNull(TripPlannerXmlParser.parseMillis("4/26/26", "   "))
        assertNull(TripPlannerXmlParser.parseMillis("4/26/26", "garbage"))
        assertNull(TripPlannerXmlParser.parseMillis("4/26/26", "45:99"))
    }
}