package com.trimettransit.tracker.transit

import com.trimettransit.tracker.model.GtfsStaticSnapshot
import com.trimettransit.tracker.model.repository.GtfsStaticStore
import java.io.File
import java.io.IOException
import java.nio.file.Files
import java.nio.file.StandardCopyOption
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.OkHttpClient
import okhttp3.Request
import timber.log.Timber

class FileGtfsStaticStore(
    private val root: File,
    private val feedUrl: String,
    private val client: OkHttpClient = OkHttpClient()
) : GtfsStaticStore {
    private val feedFile get() = File(root, "current.zip")
    private val tempFile get() = File(root, "current.zip.download")

    override suspend fun read(): GtfsStaticSnapshot? = withContext(Dispatchers.IO) {
        if (!feedFile.isFile) return@withContext null
        runCatching { feedFile.inputStream().use { GtfsStaticParser.parse(it, feedFile.lastModified()) } }.getOrNull()
    }

    override suspend fun refresh(): GtfsStaticSnapshot = withContext(Dispatchers.IO) {
        root.mkdirs()
        val request = Request.Builder().url(feedUrl).build()
        client.newCall(request).execute().use { response ->
            if (!response.isSuccessful) throw IOException("GTFS static download failed: HTTP ${response.code}")
            val body = response.body
            tempFile.outputStream().use { output -> body.byteStream().copyTo(output) }
        }
        val parsed = tempFile.inputStream().use { GtfsStaticParser.parse(it, System.currentTimeMillis()) }
        if (parsed.routes.isEmpty() && parsed.stops.isEmpty()) {
            tempFile.delete()
            throw IOException("GTFS static feed contained no routes or stops")
        }
        try {
            Files.move(
                tempFile.toPath(),
                feedFile.toPath(),
                StandardCopyOption.ATOMIC_MOVE,
                StandardCopyOption.REPLACE_EXISTING
            )
        } catch (e: Exception) {
            tempFile.delete()
            throw IOException("GTFS static feed could not be committed atomically", e)
        }
        parsed
    }

    override suspend fun refreshIfStale(maxAgeMillis: Long): GtfsStaticSnapshot? {
        val cached = read()
        return if (cached != null && System.currentTimeMillis() - cached.fetchedAtMillis < maxAgeMillis) {
            cached
        } else {
            try {
                refresh()
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                Timber.w(e, "GTFS static refresh failed; retaining the last valid feed")
                cached
            }
        }
    }
}

fun createGtfsStaticStore(context: android.content.Context): FileGtfsStaticStore =
    FileGtfsStaticStore(
        root = File(context.applicationContext.filesDir, "gtfs"),
        feedUrl = context.getString(R.string.gtfs_static_url)
    )
