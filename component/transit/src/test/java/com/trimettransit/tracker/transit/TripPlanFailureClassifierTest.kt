package com.trimettransit.tracker.transit

import com.trimettransit.tracker.model.TripPlannerError
import org.junit.Assert.assertEquals
import org.junit.Test
import java.io.IOException
import java.net.SocketTimeoutException
import java.net.UnknownHostException
import javax.net.ssl.SSLHandshakeException

class TripPlanFailureClassifierTest {

    @Test
    fun `connection timeout is a network failure`() {
        assertEquals(TripPlannerError.NETWORK, TripPlanFailureClassifier.classify(SocketTimeoutException()))
    }

    @Test
    fun `unknown host is a network failure`() {
        assertEquals(TripPlannerError.NETWORK, TripPlanFailureClassifier.classify(UnknownHostException("nope")))
    }

    @Test
    fun `tls handshake failure is a network failure`() {
        assertEquals(TripPlannerError.NETWORK, TripPlanFailureClassifier.classify(SSLHandshakeException("tls")))
    }

    @Test
    fun `empty response body is a network failure`() {
        assertEquals(
            TripPlannerError.NETWORK,
            TripPlanFailureClassifier.classify(IOException("Response body is empty."))
        )
    }

    @Test
    fun `ws 5xx is a system outage`() {
        assertEquals(TripPlannerError.SYSTEM_OUTAGE, TripPlanFailureClassifier.classify(HttpResponseCodeException(503)))
    }

    @Test
    fun `ws 4xx is not an outage`() {
        assertEquals(TripPlannerError.UNKNOWN, TripPlanFailureClassifier.classify(HttpResponseCodeException(400)))
        assertEquals(TripPlannerError.UNKNOWN, TripPlanFailureClassifier.classify(HttpResponseCodeException(404)))
    }

    @Test
    fun `blocked https downgrade is a network failure`() {
        assertEquals(
            TripPlannerError.NETWORK,
            TripPlanFailureClassifier.classify(IOException("Only HTTPS endpoints are allowed"))
        )
    }
}