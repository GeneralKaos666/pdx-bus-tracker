package com.trimettransit.tracker.util;

import java.net.URI;
import java.util.Arrays;
import java.util.HashSet;
import java.util.Locale;
import java.util.Set;

public final class SecurityUtils {
    private static final Set<String> QR_ALLOWED_HOSTS = new HashSet<>(Arrays.asList(
            "qr2.it",
            "trimet.org",
            "www.trimet.org",
            "developer.trimet.org"
    ));

    private static final int MAX_SEARCH_QUERY_LENGTH = 64;
    private static final int MAX_STOP_ID_LENGTH = 8;

    private SecurityUtils() {
    }

    public static boolean hasConfiguredTrimetApiKey() {
        return Constants2.hasTrimetApiKey();
    }

    public static String sanitizeSearchQuery(String query) {
        if (query == null) {
            return null;
        }
        String trimmed = query.trim();
        if (trimmed.isEmpty()) {
            return null;
        }
        if (trimmed.length() > MAX_SEARCH_QUERY_LENGTH) {
            return null;
        }
        return trimmed;
    }

    public static boolean isValidHttpsUri(URI uri) {
        if (uri == null || uri.getScheme() == null || uri.getHost() == null) {
            return false;
        }
        return "https".equalsIgnoreCase(uri.getScheme()) && !uri.getHost().trim().isEmpty();
    }

    public static boolean isAllowedQrHost(String host) {
        if (host == null) {
            return false;
        }
        return QR_ALLOWED_HOSTS.contains(host.toLowerCase(Locale.US));
    }

    public static String extractStopIdFromPath(String path) {
        if (path == null || path.trim().isEmpty()) {
            return null;
        }
        String[] pathSegments = path.split("/");
        if (pathSegments.length == 0) {
            return null;
        }
        // Skip empty trailing segments (e.g. from paths ending with '/')
        int lastIndex = pathSegments.length - 1;
        while (lastIndex >= 0 && pathSegments[lastIndex].isEmpty()) {
            lastIndex--;
        }
        if (lastIndex < 0) {
            return null;
        }
        String candidate = pathSegments[lastIndex];
        boolean allZeroes = true;
        for (int i = 0; i < candidate.length(); i++) {
            if (candidate.charAt(i) != '0') {
                allZeroes = false;
                break;
            }
        }
        if (allZeroes) {
            return null;
        }
        while (candidate.startsWith("0") && candidate.length() > 1) {
            candidate = candidate.substring(1);
        }
        if (candidate.length() > MAX_STOP_ID_LENGTH) {
            return null;
        }
        for (int i = 0; i < candidate.length(); i++) {
            if (!Character.isDigit(candidate.charAt(i))) {
                return null;
            }
        }
        return candidate;
    }
}
