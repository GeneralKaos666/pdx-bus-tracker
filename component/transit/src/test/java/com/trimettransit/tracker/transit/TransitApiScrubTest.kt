package com.trimettransit.tracker.transit

import java.io.PrintWriter
import java.io.StringWriter
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class TransitApiScrubTest {
    @Test fun scrubApiKeyBlankKeyIsNoOp() {
        val msg = "GET https://host/ws/appID/KEY123/locIDs/1 failed"
        assertEquals(msg, TransitApi.scrubApiKey(msg, ""))
        assertEquals(msg, TransitApi.scrubApiKey(msg, "   "))
    }

    @Test fun scrubApiKeyRedactsAppIdContext() {
        val out = TransitApi.scrubApiKey("GET https://host/ws/appID/KEY123/locIDs/1 failed", "KEY123")
        assertFalse(out.contains("KEY123"))
        assertTrue(out.contains("/appID/<redacted>"))
    }

    @Test fun scrubApiKeyRedactsBareKey() {
        val out = TransitApi.scrubApiKey("timeout for key KEY123 here", "KEY123")
        assertFalse(out.contains("KEY123"))
        assertTrue(out.contains("<redacted>"))
    }

    @Test fun scrubbedForLogRedactsFullCauseChain() {
        val inner = java.io.IOException("GET https://host/ws/appID/KEY123/locIDs/1 boom")
        val outer = java.io.IOException("wrapper failure", inner)
        val scrubbed = TransitApi.scrubbedForLog(outer, "KEY123")
        val sw = StringWriter()
        scrubbed.printStackTrace(PrintWriter(sw))
        val trace = sw.toString()
        assertFalse(trace.contains("KEY123"))
        assertTrue(trace.contains("wrapper failure"))
        assertTrue(trace.contains("/appID/<redacted>"))
    }
}
