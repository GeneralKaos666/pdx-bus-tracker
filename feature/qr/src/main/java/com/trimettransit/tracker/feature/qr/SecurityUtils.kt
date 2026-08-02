package com.trimettransit.tracker.feature.qr

import com.trimettransit.tracker.transit.ApiKeys
import java.net.URI
import java.util.Locale

object SecurityUtils {
    private val QR_ALLOWED_HOSTS = setOf(
        "qr2.it",
        "trimet.org",
        "www.trimet.org",
        "developer.trimet.org"
    )
    private const val MAX_SEARCH_QUERY_LENGTH = 64
    private const val MAX_STOP_ID_LENGTH = 8

    @JvmStatic
    fun hasConfiguredTrimetApiKey(): Boolean = ApiKeys.hasTrimetApiKey()

    @JvmStatic
    fun sanitizeSearchQuery(query: String?): String? {
        if (query == null) return null
        val trimmed = query.trim()
        if (trimmed.isEmpty() || trimmed.length > MAX_SEARCH_QUERY_LENGTH) return null
        return trimmed
    }

    @JvmStatic
    fun isValidHttpsUri(uri: URI?): Boolean {
        if (uri == null || uri.scheme == null || uri.host == null) return false
        return "https".equals(uri.scheme, ignoreCase = true) && uri.host.trim().isNotEmpty()
    }

    @JvmStatic
    fun isAllowedQrHost(host: String?): Boolean {
        if (host == null) return false
        return QR_ALLOWED_HOSTS.contains(host.lowercase(Locale.US))
    }

    @JvmStatic
    fun extractStopIdFromPath(path: String?): String? {
        if (path == null || path.trim().isEmpty()) return null
        val segments = path.split("/")
        if (segments.isEmpty()) return null

        var lastIdx = segments.size - 1
        while (lastIdx >= 0 && segments[lastIdx].isEmpty()) lastIdx--
        if (lastIdx < 0) return null

        var candidate = segments[lastIdx]

        var allZeroes = true
        for (c in candidate) {
            if (c != '0') {
                allZeroes = false
                break
            }
        }
        if (allZeroes) return null

        while (candidate.startsWith("0") && candidate.length > 1) candidate = candidate.substring(1)
        if (candidate.length > MAX_STOP_ID_LENGTH) return null

        for (c in candidate) if (!c.isDigit()) return null

        return candidate
    }
}
