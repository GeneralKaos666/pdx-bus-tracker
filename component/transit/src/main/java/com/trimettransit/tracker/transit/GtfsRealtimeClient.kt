package com.trimettransit.tracker.transit

import com.google.transit.realtime.GtfsRealtime
import java.io.IOException
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.OkHttpClient
import okhttp3.Request

class GtfsRealtimeClient(
    private val feedUrl: String,
    private val client: OkHttpClient = OkHttpClient()
) {
    suspend fun fetchVehicles(): List<com.trimettransit.tracker.model.Vehicle> = withContext(Dispatchers.IO) {
        val request = Request.Builder().url(feedUrl).build()
        client.newCall(request).execute().use { response ->
            if (!response.isSuccessful) throw IOException("GTFS realtime download failed: HTTP ${response.code}")
            val body = response.body
            GtfsRealtimeMapper.vehicles(GtfsRealtime.FeedMessage.parseFrom(body.bytes()))
        }
    }

    fun createGtfsRealtimeClient(context: android.content.Context): GtfsRealtimeClient {
        val baseUrl = context.getString(R.string.gtfs_realtime_url)
        val apiKey = ApiKeys.getTrimetApiKey()
        val url = if (apiKey.isBlank()) baseUrl else "$baseUrl?appID=$apiKey"
        return GtfsRealtimeClient(url)
    }
}
