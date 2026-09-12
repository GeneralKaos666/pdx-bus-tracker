package com.trimettransit.tracker.transit

import com.trimettransit.tracker.model.TripPlannerError
import java.io.IOException

/**
 * Thrown by [JSONParser] when the served HTTP status is not successful (e.g., 4xx/5xx).
 * Carries the code so callers can classify the failure without parsing messages.
 */
class HttpResponseCodeException(val code: Int) : IOException(
    "Unsuccessful response code: $code"
)

/**
 * Classifies a transport/parse failure from the Trip Planner WS into a user-visible
 * [TripPlannerError]. Keeps [TransitApi] thin and makes the error taxonomy directly
 * unit-testable:
 *  - WS HTTP 5xx            -> SYSTEM_OUTAGE (server-side trouble; retryable)
 *  - WS HTTP 4xx / 3xx      -> UNKNOWN (the planner declined our request content)
 *  - non-HTTP failures      -> NETWORK (timeouts, DNS, TLS, empty body, blocked upgrade)
 * Parse failures and genuine TriMet `<error code="20001|20002">` are mapped to
 * SYSTEM_OUTAGE at their call sites, so they never flow through here.
 */
object TripPlanFailureClassifier {

    internal fun classify(t: Throwable): TripPlannerError = when (t) {
        is HttpResponseCodeException -> if (t.code in 500..599) {
            TripPlannerError.SYSTEM_OUTAGE
        } else {
            TripPlannerError.UNKNOWN
        }
        is IllegalArgumentException -> TripPlannerError.UNKNOWN
        else -> TripPlannerError.NETWORK
    }
}