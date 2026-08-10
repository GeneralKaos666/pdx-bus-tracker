package com.trimettransit.tracker.transit

import android.util.Log
import okhttp3.OkHttpClient
import okhttp3.Request
import org.json.JSONException
import org.json.JSONObject
import java.io.IOException
import java.net.URI
import java.util.concurrent.TimeUnit

object JSONParser {

    private val httpClient = OkHttpClient.Builder()
        .followRedirects(true)
        .followSslRedirects(true)
        .connectTimeout(10, TimeUnit.SECONDS)
        .readTimeout(10, TimeUnit.SECONDS)
        .callTimeout(20, TimeUnit.SECONDS)
        .build()

    private const val TAG = "JSONParser"

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

        try {
            httpClient.newCall(request).execute().use { response ->
                // The client follows redirects, so response.request.url is the FINAL url
                // after any chain — re-enforce HTTPS there too, otherwise a redirect to
                // http:// would silently downgrade the caller's data in transit.
                if (!response.request.url.isHttps) {
                    throw IOException(
                        "Only HTTPS endpoints are allowed; final URL was ${response.request.url}"
                    )
                }
                if (!response.isSuccessful) {
                    throw IOException("Unsuccessful response code: ${response.code}")
                }
                val body = response.body ?: throw IOException("Response body is null.")
                val responseBody = body.string()
                if (responseBody.trim().isEmpty()) {
                    throw JSONException("Response body is empty.")
                }
                return JSONObject(responseBody)
            }
        } catch (e: Exception) {
            Log.e(TAG, "Failed to fetch JSON", e)
            throw e
        }
    }
}
