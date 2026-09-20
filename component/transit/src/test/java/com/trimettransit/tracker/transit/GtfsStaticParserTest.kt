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
        assertEquals(listOf(1, 2), snapshot.shapes.map { it.sequence })
        assertTrue(snapshot.fetchedAtMillis == 42L)
    }

    private fun add(zip: ZipOutputStream, name: String, content: String) {
        zip.putNextEntry(ZipEntry(name))
        zip.write(content.toByteArray())
        zip.closeEntry()
    }
}
