package com.trimettransit.tracker.feature.qr;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;

import java.net.URI;

import org.junit.Test;

public class SecurityUtilsTest {

    @Test
    public void sanitizeSearchQuery_rejectsBlankAndLongInput() {
        assertNull(SecurityUtils.sanitizeSearchQuery(" "));
        assertNull(SecurityUtils.sanitizeSearchQuery("a".repeat(65)));
    }

    @Test
    public void sanitizeSearchQuery_trimsValidInput() {
        assertEquals("1234", SecurityUtils.sanitizeSearchQuery(" 1234 "));
    }

    @Test
    public void isValidHttpsUri_acceptsOnlyHttpsWithHost() {
        assertTrue(SecurityUtils.isValidHttpsUri(URI.create("https://trimet.org/stops/1234")));
        assertFalse(SecurityUtils.isValidHttpsUri(URI.create("http://trimet.org/stops/1234")));
    }

    @Test
    public void isAllowedQrHost_checksAllowList() {
        assertTrue(SecurityUtils.isAllowedQrHost("qr2.it"));
        assertTrue(SecurityUtils.isAllowedQrHost("Trimet.org"));
        assertFalse(SecurityUtils.isAllowedQrHost("example.com"));
    }

    @Test
    public void extractStopIdFromPath_extractsValidNumericId() {
        assertEquals("1234", SecurityUtils.extractStopIdFromPath("/stops/01234"));
        assertEquals("1234", SecurityUtils.extractStopIdFromPath("/stops/01234/"));
        assertNull(SecurityUtils.extractStopIdFromPath("/stops/not-a-number"));
        assertNull(SecurityUtils.extractStopIdFromPath("/stops/0"));
    }

    @Test
    public void extractStopIdFromPath_handlesTrailingSlash() {
        assertEquals("1234", SecurityUtils.extractStopIdFromPath("/stops/01234/"));
    }

    @Test
    public void isAllowedQrHost_rejectsSpoofedHosts() {
        assertFalse(SecurityUtils.isAllowedQrHost("trimet.org.qq.com"));
        assertFalse(SecurityUtils.isAllowedQrHost("nottrimet.org"));
        assertFalse(SecurityUtils.isAllowedQrHost("stop.trimet.org"));
        assertFalse(SecurityUtils.isAllowedQrHost("www.trimet.org."));
    }

    @Test
    public void isValidHttpsUri_rejectsNull() {
        assertFalse(SecurityUtils.isValidHttpsUri(null));
    }

    @Test
    public void isValidHttpsUri_acceptsUppercaseScheme() {
        assertTrue(SecurityUtils.isValidHttpsUri(URI.create("HTTPS://trimet.org/stops/1234")));
    }

    @Test
    public void extractStopIdFromPath_rejectsOverflowLength() {
        assertNull(SecurityUtils.extractStopIdFromPath("/stops/123456789"));
        assertNull(SecurityUtils.extractStopIdFromPath("/stops/0123456789"));
    }

    @Test
    public void extractStopIdFromPath_stripsLeadingZeros() {
        assertEquals("1234", SecurityUtils.extractStopIdFromPath("/stops/0001234"));
        assertEquals("99999999", SecurityUtils.extractStopIdFromPath("/stops/099999999"));
    }
}
