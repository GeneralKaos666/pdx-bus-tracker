package com.trimettransit.tracker.transit

import okhttp3.OkHttpClient
import okhttp3.Request
import org.json.JSONException
import org.json.JSONObject
import java.io.IOException
import java.net.URI
import java.util.concurrent.TimeUnit

internal object JSONParser {

    internal fun newHardenedHttpClient(): OkHttpClient =
        OkHttpClient.Builder()
            .followRedirects(true)
            .followSslRedirects(true)
            .connectTimeout(10, TimeUnit.SECONDS)
            .readTimeout(10, TimeUnit.SECONDS)
            .callTimeout(20, TimeUnit.SECONDS)
            .build()

    private val httpClient = newHardenedHttpClient()

    @Throws(IllegalArgumentException::class, IOException::class, JSONException::class)
    fun fetch(url: String): JSONObject {
        val uri = URI.create(url)
        val scheme = uri.scheme
        if (scheme == null || !"https".equals(scheme, ignoreCase = true)) {
            throw IllegalArgumentException("Only HTTPS endpoints are allowed.")
        }

        val request = Request.Builder()
            .url(url)
            .addHeader("Accept", "application/json")
            .build()

        httpClient.newCall(request).execute().use { response ->
            // The client follows redirects, so response.request.url is the FINAL url
            // after any chain — re-enforce HTTPS there too, otherwise a redirect to
            // http:// would silently downgrade the caller's data in transit.
            if (!response.request.url.isHttps) {
                throw IOException(
                    "Only HTTPS endpoints are allowed; final URL was ${sanitizedUrl(response.request.url)}"
                )
            }
            if (!response.isSuccessful) {
                throw HttpResponseCodeException(response.code)
            }
            val responseBody = readBounded(response, MAX_JSON_BYTES)
            if (responseBody.trim().isEmpty()) {
                throw JSONException("Response body is empty.")
            }
            return JSONObject(responseBody)
        }
    }

    @Throws(IllegalArgumentException::class, IOException::class)
    fun fetchXml(url: String): String {
        val uri = URI.create(url)
        val scheme = uri.scheme
        if (scheme == null || !"https".equals(scheme, ignoreCase = true)) {
            throw IllegalArgumentException("Only HTTPS endpoints are allowed.")
        }

        val request = Request.Builder()
            .url(url)
            .addHeader("Accept", "application/xml")
            .build()

        httpClient.newCall(request).execute().use { response ->
            // The client follows redirects, so response.request.url is the FINAL url
            // after any chain — re-enforce HTTPS there too, otherwise a redirect to
            // http:// would silently downgrade the caller's data in transit.
            if (!response.request.url.isHttps) {
                throw IOException(
                    "Only HTTPS endpoints are allowed; final URL was ${sanitizedUrl(response.request.url)}"
                )
            }
            if (!response.isSuccessful) {
                throw HttpResponseCodeException(response.code)
            }
            val responseBody = readBounded(response, MAX_XML_BYTES)
            if (responseBody.trim().isEmpty()) {
                throw IOException("Response body is empty.")
            }
            return responseBody
        }
    }

    /**
     * Reads the body with a hard cap. A hostile or broken upstream can return a multi-GB
     * body; buffering it with `body.string()` would OOM the app. GTFS paths already bound
     * their downloads, this closes the same hole for JSON and trip-planner XML.
     */
    private fun readBounded(response: okhttp3.Response, maxBytes: Long): String {
        val declared = response.body.contentLength()
        if (declared > maxBytes) {
            throw IOException("Response body is too large ($declared bytes)")
        }
        val stream = response.body.byteStream()
        val out = java.io.ByteArrayOutputStream()
        val buffer = ByteArray(16 * 1024)
        var total = 0L
        while (true) {
            val read = stream.read(buffer)
            if (read < 0) break
            total += read
            if (total > maxBytes) throw IOException("Response body exceeded $maxBytes bytes")
            out.write(buffer, 0, read)
        }
        return out.toString("UTF-8")
    }

    private const val MAX_JSON_BYTES = 8L * 1024 * 1024
    private const val MAX_XML_BYTES = 8L * 1024 * 1024

    private fun sanitizedUrl(url: okhttp3.HttpUrl): String =
        url.scheme + "://" + url.host + "/"
}