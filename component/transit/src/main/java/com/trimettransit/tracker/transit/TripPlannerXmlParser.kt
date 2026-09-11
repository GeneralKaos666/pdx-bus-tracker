package com.trimettransit.tracker.transit

import android.net.Uri
import com.trimettransit.tracker.model.TripItinerary
import com.trimettransit.tracker.model.TripLeg
import com.trimettransit.tracker.model.TripLegMode
import com.trimettransit.tracker.model.TripPlan
import com.trimettransit.tracker.model.TripPlannerError
import com.trimettransit.tracker.model.TripPlanResult
import com.trimettransit.tracker.model.TripPoint
import kotlinx.coroutines.CancellationException
import org.joda.time.DateTime
import org.joda.time.format.DateTimeFormat
import org.w3c.dom.Element
import org.xml.sax.InputSource
import timber.log.Timber
import java.io.StringReader
import javax.xml.parsers.DocumentBuilderFactory

/**
 * Pure XML→model mapping for the Trip Planner WS response. Keeps [TransitApi] as a
 * thin network shell and makes the parse logic directly unit-testable.
 */
object TripPlannerXmlParser {

    private const val TRIP_TIME_12H = "M/d/yy h:mm a"
    private const val TRIP_TIME_24H = "M/d/yy HH:mm"

    internal fun parseMillis(date: String, timeValue: String): Long? {
        val t = timeValue.trim()
        if (t.isEmpty()) return null
        val patterns = listOf(
            TRIP_TIME_12H, "M-d-yy h:mm a", "M/d/yyyy h:mm a", "M-d-yyyy h:mm a",
            TRIP_TIME_24H, "M/d/yyyy HH:mm", "M-d-yyyy HH:mm"
        )
        for (pattern in patterns) {
            try {
                return DateTime.parse("$date $t", DateTimeFormat.forPattern(pattern)).millis
            } catch (_: Exception) {
            }
        }
        return null
    }

    internal fun parseTripPlanResponse(xml: String): TripPlanResult? {
        val response = try {
            val factory = DocumentBuilderFactory.newInstance()
            factory.isNamespaceAware = false
            factory.isExpandEntityReferences = false
            factory.setFeature("http://apache.org/xml/features/disallow-doctype-decl", true)
            factory.setFeature("http://xml.org/sax/features/external-general-entities", false)
            factory.setFeature("http://xml.org/sax/features/external-parameter-entities", false)
            factory.newDocumentBuilder()
                .parse(InputSource(StringReader(xml)))
                .documentElement
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            Timber.e(e, "Failed to parse trip planner XML")
            return TripPlanResult.Error(TripPlannerError.SYSTEM_OUTAGE)
        }
        if (response.tagName != "response") return null

        response.directChild("error")?.let { error ->
            val code = error.getAttribute("code")
            val msg = error.textContent?.trim() ?: ""
            Timber.w("Trip planner error [$code]: $msg")
            return TripPlanResult.Error(TripPlannerError.fromCode(code))
        }

        val itineraries = response.directChild("itineraries")
            ?.directChildren("itinerary")
            .orEmpty()
            .mapNotNull { parseItinerary(it) }
        return TripPlanResult.Success(
            TripPlan(
                from = parsePoint(response.directChild("from")),
                to = parsePoint(response.directChild("to")),
                itineraries = itineraries
            )
        )
    }

    private fun parsePoint(obj: Element?): TripPoint {
        if (obj == null) return TripPoint(0.0, 0.0)
        val pos = obj.directChild("pos")
        return TripPoint(
            latitude = pos?.textOf("lat")?.toDoubleOrNull() ?: 0.0,
            longitude = pos?.textOf("lon")?.toDoubleOrNull() ?: 0.0,
            // TriMet echoes back the URL-encoded fromPlace/toPlace label we sent as the
            // leg/trip description, so undo that encoding (bear minimum: decode %HH only).
            // The fallback keeps the JVM-defined contract (never null) for stub mocks.
            description = Uri.decode(obj.textOf("description")).orEmpty()
        )
    }

    private fun parseItinerary(obj: Element): TripItinerary? {
        val timeDistance = obj.directChild("time-distance") ?: return null
        val date = timeDistance.textOf("date")
        val start = parseMillis(date, timeDistance.textOf("startTime"))
        val end = parseMillis(date, timeDistance.textOf("endTime"))
        val legs = obj.directChildren("leg").mapNotNull { parseLeg(it, date) }
        if (legs.isEmpty()) return null
        val fare = obj.directChild("fare")?.textOf("regular")?.takeIf { it.isNotBlank() }
        // time-distance's duration/walking/transit/waiting values are in MINUTES.
        val walkTimeMillis = (timeDistance.textOf("walkingTime").toLongOrNull() ?: 0L) * 60_000L
        val transitTimeMillis = (timeDistance.textOf("transitTime").toLongOrNull() ?: 0L) * 60_000L
        val waitingTimeMillis = (timeDistance.textOf("waitingTime").toLongOrNull() ?: 0L) * 60_000L
        val durationMillis = timeDistance.textOf("duration").toLongOrNull()?.let { it * 60_000L }
            ?: when {
                start != null && end != null -> end - start
                else -> walkTimeMillis + transitTimeMillis
            }
        return TripItinerary(
            id = obj.getAttribute("id"),
            departure = start?.let(::DateTime),
            arrival = end?.let(::DateTime),
            durationMillis = durationMillis,
            distanceMeters = timeDistance.textOf("distance").toDoubleOrNull()?.let { it * 1609.344 } ?: 0.0,
            numberOfTransfers = timeDistance.textOf("numberOfTransfers").toIntOrNull() ?: 0,
            walkTimeMillis = walkTimeMillis,
            transitTimeMillis = transitTimeMillis,
            waitingTimeMillis = waitingTimeMillis,
            fare = fare,
            legs = legs
        )
    }

    private fun parseLeg(obj: Element, date: String): TripLeg? {
        val timeDistance = obj.directChild("time-distance")
        val start = parseMillis(date, timeDistance?.textOf("startTime").orEmpty())
        val end = parseMillis(date, timeDistance?.textOf("endTime").orEmpty())
        val from = parsePoint(obj.directChild("from"))
        val to = parsePoint(obj.directChild("to"))
        var routeNumber: String? = null
        var routeName: String? = null
        var direction = ""
        obj.directChild("route")?.let { route ->
            routeNumber = route.textOf("number").takeIf { it.isNotBlank() }
            routeName = route.textOf("name").takeIf { it.isNotBlank() }
            direction = route.textOf("direction")
        }
        if (direction.isBlank()) direction = obj.textOf("direction")
        return TripLeg(
            mode = TripLegMode.fromCode(obj.getAttribute("mode")),
            routeNumber = routeNumber,
            routeName = routeName,
            direction = direction,
            from = from,
            to = to,
            departure = start?.let(::DateTime),
            arrival = end?.let(::DateTime),
            stayOnBoard = obj.getAttribute("order") == "thru-route"
        )
    }

    private fun Element.directChild(name: String): Element? = directChildren(name).firstOrNull()

    private fun Element.directChildren(name: String): List<Element> =
        (0 until childNodes.length)
            .mapNotNull { childNodes.item(it) as? Element }
            .filter { it.tagName == name }

    private fun Element.textOf(name: String): String =
        directChild(name)?.textContent?.trim() ?: ""
}