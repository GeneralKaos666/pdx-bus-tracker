package com.trimettransit.tracker.transit

import com.trimettransit.tracker.model.Route
import com.trimettransit.tracker.model.Stop
import kotlinx.coroutines.FlowPreview
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.flow.debounce
import kotlinx.coroutines.launch
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class StopSearchStoreTest {

    private val sampleStops = listOf(
        Stop(
            desc = "Max SB & 1st Ave",
            dirDesc = "Southbound",
            latitude = 45.523062,
            longitude = -122.679252,
            transitType = "M",
            routeNum = 1,
            locId = 7783,
            routes = listOf(
                Route(desc = "MAX Blue Line", routeId = 1, isMax = true),
                Route(desc = "Portland Streetcar", routeId = 300, isStreetcar = true)
            )
        ),
        Stop(
            desc = "Everett & 5th",
            dirDesc = "Eastbound",
            latitude = 45.5251,
            longitude = -122.6766,
            transitType = "B",
            routeNum = 20,
            locId = 8233,
            routes = listOf(Route(desc = "20-Burnside/Stark", routeId = 20, isBus = true))
        )
    )

    @Test
    fun `serialize and deserialize round-trip stops with their routes`() {
        val restored = deserializeStops(serializeStops(sampleStops))
        assertEquals(sampleStops, restored)
    }

    @Test
    fun `an empty list round-trips as an empty list`() {
        val restored = deserializeStops(serializeStops(emptyList()))
        assertEquals(emptyList<Stop>(), restored)
    }

    @Test
    fun `malformed json deserializes to null`() {
        assertNull(deserializeStops("this is not json"))
        assertNull(deserializeStops("{\"wrongShape\": true}"))
        assertNull(deserializeStops("[null]"))
        assertNull(deserializeStops("[{\"routes\": [true]}]"))
    }

    @Test
    fun `order and duplicate stops are preserved`() {
        val stops = listOf(sampleStops[0], sampleStops[1], sampleStops[0])
        assertEquals(stops, deserializeStops(serializeStops(stops)))
    }

    private fun validJson(): String = serializeStops(sampleStops)

    @Test
    fun diskCacheBeyondTtlReturnsNull() {
        val old = System.currentTimeMillis() - 25L * 60 * 60 * 1000
        assertNull(deserializeWithTtl(validJson(), updatedAt = old, maxAgeMillis = 24L * 60 * 60 * 1000))
    }

    @Test
    fun diskCacheWithinTtlReturnsStops() {
        val fresh = System.currentTimeMillis() - 1L * 60 * 60 * 1000
        assertEquals(sampleStops, deserializeWithTtl(validJson(), updatedAt = fresh, maxAgeMillis = 24L * 60 * 60 * 1000))
    }

    @OptIn(FlowPreview::class)
    @Test
    fun debounceCancelsStaleQuery() = runBlocking {
        // Mirrors the Sheets/HomeSearchBar wiring:
        // snapshotFlow { query }.debounce(200).collectLatest { searchStops(...) }
        // Rapid keystrokes must cancel stale searches so only the last query runs.
        val queries = MutableStateFlow("")
        val finished = mutableListOf<String>()
        val job = launch {
            queries.debounce(200).collectLatest { q ->
                if (q.isBlank()) return@collectLatest
                delay(50)
                finished.add(q)
            }
        }
        queries.value = "a"
        queries.value = "ab"
        queries.value = "abc"
        delay(600)
        job.cancel()
        assertTrue(finished.size <= 1)
        if (finished.isNotEmpty()) assertEquals("abc", finished.last())
    }
}