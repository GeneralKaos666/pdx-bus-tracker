package com.trimettransit.tracker.transit

import com.google.transit.realtime.GtfsRealtime
import java.io.IOException
import java.net.URI
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.OkHttpClient
import okhttp3.Request

/** Hard bound on the GTFS realtime feed download (normally small; abort if bloated). */
const val MAX_GTFS_REALTIME_BYTES: Long = 8L * 1024L * 1024L

internal fun checkGtfsRealtimeSize(bytes: Long) {
    if (bytes > MAX_GTFS_REALTIME_BYTES) {
        throw IOException(
            "GTFS realtime feed is $bytes bytes, over the $MAX_GTFS_REALTIME_BYTES bound; aborting refresh"
        )
    }
}

internal fun requireHttpsFeedUrl(url: String) {
    val scheme = runCatching { URI.create(url).scheme }.getOrNull()
    if (scheme == null || !"https".equals(scheme, ignoreCase = true)) {
        throw IllegalArgumentException("Only HTTPS endpoints are allowed.")
    }
}

class GtfsRealtimeClient(
    private val feedUrl: String,
    private val client: OkHttpClient = JSONParser.newHardenedHttpClient()
) {
    suspend fun fetchVehicles(): List<com.trimettransit.tracker.model.Vehicle> = withContext(Dispatchers.IO) {
        requireHttpsFeedUrl(feedUrl)
        val request = Request.Builder().url(feedUrl).build()
        client.newCall(request).execute().use { response ->
            if (!response.isSuccessful) throw IOException("GTFS realtime download failed: HTTP ${response.code}")
            if (!response.request.url.isHttps) {
                throw IOException("Only HTTPS endpoints are allowed; redirect downgraded GTFS realtime feed")
            }
            val body = response.body
            checkGtfsRealtimeSize(body.contentLength().takeIf { it >= 0 } ?: 0)
            val bytes = body.bytes()
            checkGtfsRealtimeSize(bytes.size.toLong())
            GtfsRealtimeMapper.vehicles(GtfsRealtime.FeedMessage.parseFrom(bytes))
        }
    }

    fun createGtfsRealtimeClient(context: android.content.Context): GtfsRealtimeClient {
        val baseUrl = context.getString(R.string.gtfs_realtime_url)
        val apiKey = ApiKeys.getTrimetApiKey()
        val url = if (apiKey.isBlank()) baseUrl else "$baseUrl?appID=$apiKey"
        return GtfsRealtimeClient(url)
    }
}
