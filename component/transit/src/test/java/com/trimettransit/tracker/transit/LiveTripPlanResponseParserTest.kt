package com.trimettransit.tracker.transit

import com.trimettransit.tracker.model.TripPlanResult
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Test
import org.xml.sax.InputSource
import java.io.File
import java.io.StringReader
import javax.xml.parsers.DocumentBuilderFactory

class LiveTripPlanResponseParserTest {

    private val fixture =
        File("src/test/resources/live_trip_plan_response.xml").readText()

    @Test
    fun parsesCurrentLiveTriMetResponseShape() {
        val result = TripPlannerXmlParser.parseTripPlanResponse(fixture)
        assertNotNull("expected parse() to succeed, got $result", result)
        val success = result as? TripPlanResult.Success
        assertNotNull("expected Success, got $result", success)
        val plan = success!!.plan
        assertNotNull(plan)
        assertEquals(3, plan!!.itineraries.size)
        assertTrue(plan.itineraries.first().legs.isNotEmpty())
    }

    @Test
    fun unsupportedFactoryFeaturesDoNotBreakParsing() {
        // Android's DocumentBuilderFactory rejects Apache/SAX feature flags; every
        // request used to die with SYSTEM_OUTAGE because of that. The hardened
        // parser must continue parsing when a flag is unsupported.
        val factory = DocumentBuilderFactory.newInstance()
        factory.tryFeature("http://bogus.invalid/feature/definitely-not-supported", true)
        factory.tryFeature("http://apache.org/xml/features/disallow-doctype-decl", true)
        val parsed = factory.newDocumentBuilder()
            .parse(InputSource(StringReader("<root><itineraries/><leg/></root>")))
        assertEquals("root", parsed.documentElement.tagName)
    }
}