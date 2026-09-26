package com.trimettransit.tracker.transit

import java.io.ByteArrayInputStream
import java.io.ByteArrayOutputStream
import java.io.File
import java.io.InputStream
import java.util.zip.ZipEntry
import java.util.zip.ZipOutputStream
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.rules.TemporaryFolder

class GtfsStaticParserTest {
    @get:Rule
    val tempFolder = TemporaryFolder()
    @Test
    fun `parses the minimal static tables and quoted commas`() {
        val bytes = ByteArrayOutputStream()
        ZipOutputStream(bytes).use { zip ->
            add(zip, "routes.txt", "route_id,route_short_name,route_long_name,route_type\n4,4,\"Division, Fessenden\",3\n")
            add(zip, "stops.txt", "stop_id,stop_name,stop_lat,stop_lon\n100,\"Main, St\",45.5,-122.6\n")
            add(zip, "shapes.txt", "shape_id,shape_pt_lat,shape_pt_lon,shape_pt_sequence\ns1,45.5,-122.6,2\ns1,45.4,-122.5,1\n")
        }
        val snapshot = GtfsStaticParser.parse(ByteArrayInputStream(bytes.toByteArray()), 42L)
        assertEquals("Division, Fessenden", snapshot.routes.single().longName)
        assertEquals("Main, St", snapshot.stops.single().name)
        assertTrue(snapshot.fetchedAtMillis == 42L)
    }

    @Test
    fun `a feed's shapes table is not parsed`() {
        // TriMet's real shapes.txt is 1.08 million rows / 44 MB. Turning every row into a
        // Map<String, String> exhausts the app's 256 MB heap, and nothing consumes the result,
        // so the parser must leave that table alone. This test fails if shapes parsing is ever
        // re-added through the table list.
        val bytes = ByteArrayOutputStream()
        ZipOutputStream(bytes).use { zip ->
            add(zip, "routes.txt", "route_id,route_short_name,route_long_name,route_type\n4,4,Division,3\n")
            add(
                zip,
                "shapes.txt",
                "shape_id,shape_pt_lat,shape_pt_lon,shape_pt_sequence\ns1,45.5,-122.6,2\ns1,45.4,-122.5,1\n"
            )
        }

        val snapshot = GtfsStaticParser.parse(ByteArrayInputStream(bytes.toByteArray()), 42L)

        assertEquals(1, snapshot.routes.size)
        assertTrue("shapes must not be parsed", snapshot.shapes.isEmpty())
    }

    @Test
    fun `store rejects a feed larger than the byte bound`() {
        // The declared size alone exceeds the bound, so no bytes are read and dest is
        // never touched — no large allocation needed to exercise this check.
        val dest = File("bounded-rejected.zip")
        try {
            val result = runCatching {
                FileGtfsStaticStore.writeBounded(ByteArray(0).inputStream(), MAX_GTFS_STATIC_BYTES + 1, dest)
            }
            assertTrue(result.isFailure)
            assertFalse(dest.exists())
        } finally {
            dest.delete()
        }
    }

    @Test
    fun `store aborts a mid-download breach when the declared size lies`() {
        // A server may omit (declared -1) or understate Content-Length; the chunk loop must
        // still abort once the actual streamed bytes exceed the bound, leaving no complete feed.
        val actualBytes = MAX_GTFS_STATIC_BYTES + 1
        val dest = tempFolder.newFile("bounded-streamed.zip")
        val result = runCatching {
            FileGtfsStaticStore.writeBounded(ZeroStream(actualBytes), -1, dest)
        }
        assertTrue(result.isFailure)
        assertTrue(dest.length() < actualBytes)
    }

    @Test
    fun `parser skips an oversized shapes table and still returns stops`() {
        // Pins the v4.19.0 OOM lesson: TriMet's real shapes.txt is ~1.08M rows / 44 MB, and
        // materializing every row as a Map exhausts the heap while nothing consumes the
        // result — so the parser must leave that table alone no matter how large it gets.
        val parsed = GtfsStaticParser.parse(gtfsZipWithHugeShapesTable(), 42L)
        assertTrue(parsed.stops.isNotEmpty())
        assertTrue("shapes must not be parsed", parsed.shapes.isEmpty())
    }

    private fun gtfsZipWithHugeShapesTable(): ByteArrayInputStream {
        val bytes = ByteArrayOutputStream()
        ZipOutputStream(bytes).use { zip ->
            add(zip, "routes.txt", "route_id,route_short_name,route_long_name,route_type\n4,4,Division,3\n")
            add(zip, "stops.txt", "stop_id,stop_name,stop_lat,stop_lon\n100,Main St,45.5,-122.6\n")
            zip.putNextEntry(ZipEntry("shapes.txt"))
            zip.write("shape_id,shape_pt_lat,shape_pt_lon,shape_pt_sequence\n".toByteArray())
            repeat(10_000) { i ->
                zip.write("s1,45.5,-122.6,$i\n".toByteArray())
            }
            zip.closeEntry()
        }
        return ByteArrayInputStream(bytes.toByteArray())
    }

    /** Zero-filled stream of [remaining] bytes without allocating them on the heap. */
    private class ZeroStream(private var remaining: Long) : InputStream() {
        override fun read(b: ByteArray, off: Int, len: Int): Int {
            if (remaining <= 0) return -1
            val n = minOf(len.toLong(), remaining).toInt()
            java.util.Arrays.fill(b, off, off + n, 0.toByte())
            remaining -= n
            return n
        }

        override fun read(): Int = if (remaining-- > 0) 0 else -1
    }

    private fun add(zip: ZipOutputStream, name: String, content: String) {
        zip.putNextEntry(ZipEntry(name))
        zip.write(content.toByteArray())
        zip.closeEntry()
    }
}
