package com.trimettransit.tracker.transit

import com.trimettransit.tracker.model.GtfsRoute
import com.trimettransit.tracker.model.GtfsStaticSnapshot
import com.trimettransit.tracker.model.GtfsStop
import com.trimettransit.tracker.model.GtfsShapePoint
import java.io.InputStream
import java.util.zip.ZipInputStream

internal object GtfsStaticParser {
    fun parse(input: InputStream, fetchedAtMillis: Long): GtfsStaticSnapshot {
        val tables = mutableMapOf<String, List<Map<String, String>>>()
        ZipInputStream(input.buffered()).use { zip ->
            while (true) {
                val entry = zip.nextEntry ?: break
                if (!entry.isDirectory && entry.name.substringAfterLast('/') in TABLES) {
                    tables[entry.name.substringAfterLast('/')] = readCsv(zip)
                }
                zip.closeEntry()
            }
        }
        return GtfsStaticSnapshot(
            routes = tables["routes.txt"].orEmpty().mapNotNull { row ->
                val id = row["route_id"].orEmpty()
                if (id.isBlank()) null else GtfsRoute(
                    id = id,
                    shortName = row["route_short_name"].orEmpty(),
                    longName = row["route_long_name"].orEmpty(),
                    type = row["route_type"]?.toIntOrNull() ?: 3
                )
            },
            stops = tables["stops.txt"].orEmpty().mapNotNull { row ->
                val id = row["stop_id"].orEmpty()
                val lat = row["stop_lat"]?.toDoubleOrNull()
                val lon = row["stop_lon"]?.toDoubleOrNull()
                if (id.isBlank() || lat == null || lon == null) null else GtfsStop(
                    id = id,
                    name = row["stop_name"].orEmpty(),
                    latitude = lat,
                    longitude = lon
                )
            },
            shapes = tables["shapes.txt"].orEmpty().mapNotNull { row ->
                val id = row["shape_id"].orEmpty()
                val lat = row["shape_pt_lat"]?.toDoubleOrNull()
                val lon = row["shape_pt_lon"]?.toDoubleOrNull()
                val sequence = row["shape_pt_sequence"]?.toIntOrNull()
                if (id.isBlank() || lat == null || lon == null || sequence == null) null else
                    GtfsShapePoint(id, lat, lon, sequence)
            }.sortedWith(compareBy<GtfsShapePoint> { it.shapeId }.thenBy { it.sequence }),
            fetchedAtMillis = fetchedAtMillis
        )
    }

    private fun readCsv(input: InputStream): List<Map<String, String>> {
        val lines = String(input.readBytes(), Charsets.UTF_8).lineSequence().toList()
        if (lines.isEmpty()) return emptyList()
        val headers = parseLine(lines.first()).map { it.trim().removePrefix("\uFEFF") }
        return lines.drop(1).asSequence()
            .filter { it.isNotBlank() }
            .map { values -> parseLine(values).let { fields ->
                headers.indices.associateWith { fields.getOrNull(it).orEmpty() }
                    .mapKeys { headers[it.key] }
            } }
            .toList()
    }

    private fun parseLine(line: String): List<String> {
        val result = mutableListOf<String>()
        val field = StringBuilder()
        var quoted = false
        var i = 0
        while (i < line.length) {
            when (val char = line[i]) {
                '"' -> if (quoted && i + 1 < line.length && line[i + 1] == '"') {
                    field.append('"')
                    i++
                } else quoted = !quoted
                ',' -> if (quoted) field.append(char) else {
                    result += field.toString()
                    field.clear()
                }
                else -> field.append(char)
            }
            i++
        }
        result += field.toString()
        return result
    }

    private val TABLES = setOf("routes.txt", "stops.txt", "shapes.txt")
}
