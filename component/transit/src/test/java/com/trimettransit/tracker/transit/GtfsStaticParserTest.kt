package com.trimettransit.tracker.transit

import java.io.ByteArrayInputStream
import java.io.ByteArrayOutputStream
import java.util.zip.ZipEntry
import java.util.zip.ZipOutputStream
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class GtfsStaticParserTest {
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

    private fun add(zip: ZipOutputStream, name: String, content: String) {
        zip.putNextEntry(ZipEntry(name))
        zip.write(content.toByteArray())
        zip.closeEntry()
    }
}
