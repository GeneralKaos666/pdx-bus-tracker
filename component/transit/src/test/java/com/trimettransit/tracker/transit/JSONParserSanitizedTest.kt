package com.trimettransit.tracker.transit

import okhttp3.HttpUrl.Companion.toHttpUrl
import org.junit.Assert.assertFalse
import org.junit.Test

class JSONParserSanitizedTest {
    @Test fun sanitizedUrlIsHostOnly() {
        val url = "https://developer.trimet.org/ws/V2/arrivals/appID/SECRET123/locIDs/123".toHttpUrl()
        val m = JSONParser::class.java.getDeclaredMethod("sanitizedUrl", okhttp3.HttpUrl::class.java)
        m.isAccessible = true
        val out = m.invoke(JSONParser, url) as String
        assertFalse(out.contains("SECRET123"))
        assertFalse(out.contains("appID"))
        assertFalse(out.contains("locIDs"))
    }
}
