package com.trimettransit.tracker.transit

import com.trimettransit.tracker.model.GtfsStaticSnapshot
import com.trimettransit.tracker.model.repository.GtfsStaticStore
import java.io.File
import java.io.IOException
import java.io.InputStream
import java.nio.file.Files
import java.nio.file.StandardCopyOption
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.OkHttpClient
import okhttp3.Request
import timber.log.Timber

/**
 * Hard bound on the GTFS static feed download. TriMet's feed includes a ~44 MB shapes table;
 * the parser deliberately skips that table (see [GtfsStaticParser]), but the raw bytes still
 * hit disk and memory during download — cap them so a bloated feed aborts the refresh
 * instead of exhausting the device.
 */
const val MAX_GTFS_STATIC_BYTES: Long = 32L * 1024L * 1024L

class FileGtfsStaticStore(
    private val root: File,
    private val feedUrl: String,
    private val client: OkHttpClient = OkHttpClient()
) : GtfsStaticStore {
    private val feedFile get() = File(root, "current.zip")
    private val tempFile get() = File(root, "current.zip.download")

    companion object {
        /**
         * Streams [source] to [dest], enforcing [MAX_GTFS_STATIC_BYTES] both up front against
         * the declared size and incrementally as bytes arrive (the server may lie about or
         * omit `Content-Length`). Throws [IOException] naming the byte count on breach, so
         * the caller can abort the refresh and keep the last good cache.
         */
        fun writeBounded(source: InputStream, declaredBytes: Long, dest: File) {
            if (declaredBytes > MAX_GTFS_STATIC_BYTES) {
                throw IOException(
                    "GTFS static feed is $declaredBytes bytes, " +
                        "over the $MAX_GTFS_STATIC_BYTES bound; aborting refresh"
                )
            }
            var written = 0L
            dest.outputStream().use { out ->
                val buf = ByteArray(8192)
                while (true) {
                    val n = source.read(buf)
                    if (n < 0) break
                    written += n
                    if (written > MAX_GTFS_STATIC_BYTES) {
                        throw IOException(
                            "GTFS static feed exceeded $MAX_GTFS_STATIC_BYTES bytes " +
                                "mid-download; aborting refresh"
                        )
                    }
                    out.write(buf, 0, n)
                }
            }
        }
    }

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
            try {
                writeBounded(body.byteStream(), body.contentLength(), tempFile)
            } catch (e: IOException) {
                tempFile.delete()
                Timber.w(e, "GTFS static refresh aborted; retaining the last valid feed")
                throw e
            }
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
