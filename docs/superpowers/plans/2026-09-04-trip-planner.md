# Trip Planner (Trips Tab) Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** Replace the dormant What's Nearby map screen with a map-first from→to trip planner as the new 4th top-level "Trips" tab, backed by TriMet's Trip Planner WS.

**Architecture:** New data path `component:transit` (XML fetch + parse into `common/model` trip types) consumed by a new `feature:trips` module (renamed from `:feature:vehicles`) holding `TripPlannerScreen` (MapLibre map, endpoint picking, depart-now/arrive-by, itinerary results rendered on the map). Wired as pager page 3 in `app`'s bottom pill.

**Tech Stack:** Kotlin, Jetpack Compose + Material 3, MapLibre GL (OpenGL SDK 13.6.0), OkHttp, org.json (`XML.toJSONObject`), android.joda, AGP 9.3.1.

**Spec:** `docs/superpowers/specs/2026-09-04-trip-planner-design.md`

## Global Constraints

- No ViewModels, no DI, no Room. Screens use `remember { mutableStateOf(...) }` + suspend API calls; dedupe in-flight jobs.
- No test source files exist in the repo and none are added (AGENTS.md: `./gradlew test` stays in CI but empty). Each task verifies by compiling + lint instead of unit tests; task 9 runs the full CI command.
- Module layering: `feature/*` → `component/*` → `common/*`; no feature→feature edges.
- Hardcoded versions (no version catalog). Reuse: compose BOM 2026.08.00, material3 1.5.0-alpha27, activity-compose 1.13.0, core(-ktx) 1.19.0, MapLibre `org.maplibre.gl:android-sdk-opengl:13.6.0`, android.joda 2.14.2.1, material-icons-extended.
- Every module: `lint { abortOnError = true; baseline = file("lint-baseline.xml") }`.
- `compileSdk`/`targetSdk` 37, `minSdk` 31, Java 21.
- API base URLs live in `component:transit/src/main/res/values/strings.xml`; `TRIMET_API_KEY` flows via `ApiKeys`.
- README/CHANGELOG entries in plain language, no file-path references in CHANGELOG.
- Branding: origin/route marker artwork is app-drawn; no TriMet logo.
- No version catalogs; add `android.joda` explicitly to any module that compiles joda `DateTime` types (common/model already has it `implementation`-scoped — NOT inherited by downstream modules).

---

### Task 1: Data path — base URL, XML fetch, models

**Files:**
- Modify: `component/transit/src/main/res/values/strings.xml`
- Modify: `component/transit/src/main/java/com/trimettransit/tracker/transit/JSONParser.kt`
- Create: `common/model/src/main/java/com/trimettransit/tracker/model/TripPlan.kt`

**Interfaces:**
- Produces: `JSONParser.fetchXml(url: String): String` (throws like `fetch`);
  models `TripPoint`, `TripLegMode` (+`fromCode`, `transitTypeLetter`),
  `TripLeg`, `TripItinerary`, `TripPlan`, `TripRequestTime`,
  `TripPlannerError`, `TripPlanResult`.

- [ ] **Step 1: Add the trip planner base URL string**

Add to `component/transit/src/main/res/values/strings.xml`:
```xml
<string name="base_trip_planner_url">https://developer.trimet.org/ws/V1/trips/tripplanner</string>
```

- [ ] **Step 2: Add `fetchXml` to `JSONParser`**

Add after `fetch(url)` in `JSONParser.kt` (same HTTPS guards; Accept header `application/xml`):
```kotlin
@Throws(IllegalArgumentException::class, IOException::class)
fun fetchXml(url: String): String {
    val uri = URI.create(url)
    val scheme = uri.scheme
    if (scheme == null || !"https".equals(scheme, ignoreCase = true)) {
        throw IllegalArgumentException("Only HTTPS endpoints are allowed.")
    }

    val request = Request.Builder()
        .url(url)
        .addHeader("Accept", "application/xml")
        .build()

    try {
        httpClient.newCall(request).execute().use { response ->
            if (!response.request.url.isHttps) {
                throw IOException(
                    "Only HTTPS endpoints are allowed; final URL was ${response.request.url}"
                )
            }
            if (!response.isSuccessful) {
                throw IOException("Unsuccessful response code: ${response.code}")
            }
            val responseBody = response.body.string()
            if (responseBody.trim().isEmpty()) {
                throw IOException("Response body is empty.")
            }
            return responseBody
        }
    } catch (e: Exception) {
        Timber.e(e, "Failed to fetch XML")
        throw e
    }
}
```

- [ ] **Step 3: Create the trip-plan models**

Create `common/model/src/main/java/com/trimettransit/tracker/model/TripPlan.kt` (= old package `com.trimettransit.tracker.model`):
```kotlin
package com.trimettransit.tracker.model

import org.joda.time.DateTime

data class TripPoint(
    val latitude: Double,
    val longitude: Double,
    val description: String = ""
)

enum class TripLegMode {
    WALK, BUS, LIGHT_RAIL, COMMUTER_RAIL, STREETCAR, RAIL;

    companion object {
        fun fromCode(mode: String): TripLegMode {
            val trimmed = mode.trim()
            trimmed.toIntOrNull()?.let { code ->
                return when (code) {
                    8 -> WALK
                    3 -> BUS
                    10, 0, 4 -> LIGHT_RAIL
                    13 -> STREETCAR
                    11, 2 -> COMMUTER_RAIL
                    else -> RAIL
                }
            }
            return when (trimmed.lowercase()) {
                "walk" -> WALK
                "bus" -> BUS
                "streetcar" -> STREETCAR
                "light rail" -> LIGHT_RAIL
                "commuter rail" -> COMMUTER_RAIL
                else -> RAIL
            }
        }
    }

    fun transitTypeLetter(): String = when (this) {
        BUS -> "B"
        LIGHT_RAIL, COMMUTER_RAIL, RAIL -> "M"
        STREETCAR -> "S"
        WALK -> "Z"
    }
}

data class TripLeg(
    val mode: TripLegMode,
    val routeNumber: String? = null,
    val routeName: String? = null,
    val direction: String = "",
    val from: TripPoint,
    val to: TripPoint,
    val departure: DateTime,
    val arrival: DateTime,
    val stayOnBoard: Boolean = false
) {
    val isWalk: Boolean get() = mode == TripLegMode.WALK
}

data class TripItinerary(
    val id: String = "",
    val departure: DateTime,
    val arrival: DateTime,
    val durationMillis: Long,
    val distanceMeters: Double,
    val numberOfTransfers: Int,
    val walkTimeMillis: Long,
    val transitTimeMillis: Long,
    val waitingTimeMillis: Long,
    val fare: String? = null,
    val legs: List<TripLeg> = emptyList()
)

data class TripPlan(
    val from: TripPoint,
    val to: TripPoint,
    val itineraries: List<TripItinerary> = emptyList()
)

data class TripRequestTime(
    val arriveBy: Boolean = false,
    val timeMillis: Long? = null
)

/** Domain errors reported by the Trip Planner WS `<error code="...">`. */
enum class TripPlannerError {
    NO_STOPS_NEAR_ORIGIN, NO_STOPS_NEAR_DESTINATION,
    NO_SERVICE_AT_ORIGIN, NO_SERVICE_AT_DESTINATION,
    TRIP_NOT_POSSIBLE, TRIVIAL_DISTANCE,
    AMBIGUOUS_ORIGIN, AMBIGUOUS_DESTINATION, ORIGIN_NOT_FOUND, DESTINATION_NOT_FOUND,
    OUTSIDE_DISTRICT, SYSTEM_OUTAGE, UNKNOWN;

    companion object {
        fun fromCode(code: String): TripPlannerError {
            return when (code.toIntOrNull()) {
                20003 -> NO_STOPS_NEAR_ORIGIN
                20004 -> NO_STOPS_NEAR_DESTINATION
                20005 -> NO_SERVICE_AT_ORIGIN
                20006 -> NO_SERVICE_AT_DESTINATION
                20007 -> TRIP_NOT_POSSIBLE
                20022, 20020, 20021 -> AMBIGUOUS_ORIGIN
                20023 -> AMBIGUOUS_DESTINATION
                20024 -> ORIGIN_NOT_FOUND
                20025 -> DESTINATION_NOT_FOUND
                20026 -> TRIVIAL_DISTANCE
                21000, 21001 -> OUTSIDE_DISTRICT
                20001, 20002 -> SYSTEM_OUTAGE
                else -> UNKNOWN
            }
        }
    }
}

sealed interface TripPlanResult {
    data class Success(val plan: TripPlan?) : TripPlanResult
    data class Error(val error: TripPlannerError) : TripPlanResult
}
```

- [ ] **Step 4: Verify**

Run: `./gradlew :common:model:compileDebugKotlin :component:transit:compileDebugKotlin`
Expected: BUILD SUCCESSFUL.

---

### Task 2: `TransitApi.fetchTripPlan` + repository interface

**Files:**
- Modify: `component/transit/src/main/java/com/trimettransit/tracker/transit/TransitApi.kt`
- Modify: `common/model/src/main/java/com/trimettransit/tracker/model/repository/TransitRepository.kt`
- Modify: `component/transit/src/main/java/com/trimettransit/tracker/transit/TransitRepositoryImpl.kt`

**Interfaces:**
- Consumes: models from Task 1; `JSONParser.fetchXml`.
- Produces: `TransitApi.fetchTripPlan(context, from: TripPoint, to: TripPoint, time: TripRequestTime): TripPlanResult?` (null = offline/API-key-missing); `TransitRepository.planTrip(from, to, time): TripPlanResult?`; `TransitRepositoryImpl.planTrip` delegation.

- [ ] **Step 1: Add imports + `fetchTripPlan` + private parse helpers to `TransitApi.kt`**

Add imports (`android.net.Uri`, `org.joda.time.DateTime`, `org.joda.time.format.DateTimeFormat`, `org.json.JSONArray`, `com.trimettransit.tracker.model.*`:

```kotlin
suspend fun fetchTripPlan(
    context: Context,
    from: TripPoint,
    to: TripPoint,
    time: TripRequestTime
): TripPlanResult? = withContext(Dispatchers.IO) {
    if (!ConnectionUtils.isOnline(context)) return@withContext null
    val apiKey = ApiKeys.getTrimetApiKey()
    if (apiKey.isBlank()) {
        Timber.w("TriMet API key not configured")
        return@withContext null
    }
    try {
        val now = DateTime.now()
        val requested = time.timeMillis?.let { DateTime(it) } ?: now
        val date = DateTimeFormat.forPattern("M-d-yyyy").print(requested)
        val clock = DateTimeFormat.forPattern("h:mm a").print(requested)
        val baseUrl = context.getString(R.string.base_trip_planner_url)
        val url = buildString {
            append(baseUrl)
            append("/fromPlace/").append(Uri.encode(from.description))
            append("/fromCoord/").append("${from.longitude},${from.latitude}")
            append("/toPlace/").append(Uri.encode(to.description))
            append("/toCoord/").append("${to.longitude},${to.latitude}")
            append("/date/").append(date)
            append("/time/").append(Uri.encode(clock))
            append("/arr/").append(if (time.arriveBy) "A" else "D")
            append("/min/T")
            append("/mode/A")
            append("/walk/0.5")
            append("/maxIntineraries/3")
            append("/format/xml")
            append("/appID/").append(apiKey)
        }
        val xml = parser.fetchXml(url)
        parseTripPlanResponse(org.json.XML.toJSONObject(xml))
    } catch (e: Exception) {
        Timber.e(e, "Failed to fetch trip plan")
        TripPlanResult.Error(TripPlannerError.SYSTEM_OUTAGE)
    }
}

private fun optList(root: JSONObject, key: String): List<JSONObject> {
    val node = root.opt(key) ?: return emptyList()
    return when (node) {
        is JSONArray -> List(node.length()) { node.getJSONObject(it) }
        is JSONObject -> listOf(node)
        else -> emptyList()
    }
}

private fun parseTripPlanResponse(json: JSONObject): TripPlanResult? {
    val response = json.optJSONObject("response") ?: return null
    response.optJSONObject("error")?.let { error ->
        val code = error.optString("code", "")
        val msg = error.optString("message", "")
        Timber.w("Trip planner error [$code]: $msg")
        return TripPlanResult.Error(TripPlannerError.fromCode(code))
    }
    val itineraries = optList(response, "itineraries")
        .flatMap { optList(it, "itinerary") }
        .mapNotNull { parseItinerary(it) }
    return TripPlanResult.Success(TripPlan(
        from = parsePoint(response.optJSONObject("from")),
        to = parsePoint(response.optJSONObject("to")),
        itineraries = itineraries
    ))
}

private const val TRIP_TIME_12H = "M-d-yyyy h:mm a"
private const val TRIP_TIME_24H = "M-d-yyyy HH:mm"

private fun parseMillis(date: String, timeValue: String): Long {
    val t = timeValue.trim()
    for (pattern in listOf(TRIP_TIME_12H, TRIP_TIME_24H)) {
        try {
            return DateTime.parse("$date $t", DateTimeFormat.forPattern(pattern)).millis
        } catch (_: Exception) {
        }
    }
    return DateTime.now().millis
}

private fun parsePoint(obj: JSONObject?): TripPoint {
    if (obj == null) return TripPoint(0.0, 0.0)
    val pos = obj.optJSONObject("pos")
    return TripPoint(
        latitude = pos?.optDouble("lat", 0.0) ?: 0.0,
        longitude = pos?.optDouble("lon", 0.0) ?: 0.0,
        description = obj.optString("description", "")
    )
}

private fun parseItinerary(obj: JSONObject): TripItinerary? {
    val timeDistance = obj.optJSONObject("time-distance") ?: return null
    val date = timeDistance.optString("date", "")
    val start = parseMillis(date, timeDistance.optString("startTime", ""))
    val end = parseMillis(date, timeDistance.optString("endTime", ""))
    val legs = optList(obj, "leg").mapNotNull { parseLeg(it, date) }
    if (legs.isEmpty()) return null
    val fare = obj.optJSONObject("fare")
        ?.optString("regular", "")
        ?.takeIf { it.isNotBlank() }
    return TripItinerary(
        id = obj.optString("id", ""),
        departure = DateTime(start),
        arrival = DateTime(end),
        durationMillis = (timeDistance.optLong("duration", (end - start) / 1000L)) * 1000L,
        distanceMeters = timeDistance.optDouble("distance", 0.0) * 1609.344,
        numberOfTransfers = timeDistance.optInt("numberOfTransfers", 0),
        walkTimeMillis = timeDistance.optLong("walkingTime", 0) * 1000L,
        transitTimeMillis = timeDistance.optLong("transitTime", 0) * 1000L,
        waitingTimeMillis = timeDistance.optLong("waitingTime", 0) * 1000L,
        fare = fare,
        legs = legs
    )
}

private fun parseLeg(obj: JSONObject, date: String): TripLeg? {
    val timeDistance = obj.optJSONObject("time-distance")
    val start = parseMillis(date, timeDistance?.optString("startTime", "") ?: "")
    val end = parseMillis(date, timeDistance?.optString("endTime", "") ?: "")
    val from = parsePoint(obj.optJSONObject("from"))
    val to = parsePoint(obj.optJSONObject("to"))
    var routeNumber: String? = null
    var routeName: String? = null
    var direction = ""
    obj.optJSONObject("route")?.let { route ->
        routeNumber = route.optString("number", "").takeIf { it.isNotBlank() }
        routeName = route.optString("name", "").takeIf { it.isNotBlank() }
        direction = route.optString("direction", "")
    }
    if (direction.isBlank()) direction = obj.optString("direction", "")
    return TripLeg(
        mode = TripLegMode.fromCode(obj.optString("mode", "")),
        routeNumber = routeNumber,
        routeName = routeName,
        direction = direction,
        from = from,
        to = to,
        departure = DateTime(start),
        arrival = DateTime(end),
        stayOnBoard = obj.optString("order", "") == "thru-route"
    )
}
```

- [ ] **Step 2: Add `planTrip` to `TransitRepository`**

Add to the interface (with new imports — the interface file already imports the model package but add `TripPoint`/`TripRequestTime`/`TripPlanResult` in the same `com.trimettransit.tracker.model.*` style):
```kotlin
/** Plans a from→to trip via the TriMet Trip Planner WS. Null = offline or missing API key. */
suspend fun planTrip(
    from: TripPoint,
    to: TripPoint,
    time: TripRequestTime = TripRequestTime()
): TripPlanResult?
```

- [ ] **Step 3: Implement in `TransitRepositoryImpl`**

```kotlin
override suspend fun planTrip(
    from: TripPoint,
    to: TripPoint,
    time: TripRequestTime
): TripPlanResult? = TransitApi.fetchTripPlan(context, from, to, time)
```

- [ ] **Step 4: Verify**

Run: `./gradlew :component:transit:compileDebugKotlin :common:model:compileDebugKotlin`
Expected: BUILD SUCCESSFUL.

---

### Task 3: Shared stop-search extraction to `common/ui`

**Files:**
- Create: `common/ui/src/main/java/com/trimettransit/tracker/ui/components/StopSearch.kt`
- Modify: `feature/home/src/main/java/com/trimettransit/tracker/feature/home/HomeSearchBar.kt`

**Interfaces:**
- Produces: `com.trimettransit.tracker.ui.components.searchStops(allStops: List<Stop>, query: String): List<Stop>`; `@Composable StopSearchItem(stop: Stop, onClick: () -> Unit, modifier: Modifier = Modifier)`.
- Consumes: `transitColor`, `transitIconResource`, `transitTypeLabel`, `pressScale` (already in common/ui).

- [ ] **Step 1: Create `StopSearch.kt` with the moved helpers**

Move the private `searchStops()` function and `StopSearchItem` composable from `HomeSearchBar.kt` verbatim (same imports; convert to `internal`-visible `public`), package `com.trimettransit.tracker.ui.components`:
```kotlin
package com.trimettransit.tracker.ui.components

import androidx.compose.foundation.LocalIndication
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.unit.dp
import com.trimettransit.tracker.model.Stop
import java.util.Locale

const val STOP_SEARCH_MAX_RESULTS = 250

fun searchStops(allStops: List<Stop>, query: String): List<Stop> {
    val trimmed = query.trim().lowercase(Locale.US)
    if (trimmed.isEmpty()) return emptyList()
    val queryAsInt = trimmed.toIntOrNull()
    return allStops.filter { stop ->
        val matchDesc = stop.desc.lowercase(Locale.US).contains(trimmed)
        val matchDir = stop.dirDesc.lowercase(Locale.US).contains(trimmed)
        val matchId = queryAsInt != null && stop.locId.toString().contains(queryAsInt.toString())
        matchDesc || matchDir || matchId
    }.take(STOP_SEARCH_MAX_RESULTS)
}

@Composable
fun StopSearchItem(
    stop: Stop,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val interactionSource = remember { MutableInteractionSource() }
    Row(
        modifier = modifier
            .fillMaxWidth()
            .pressScale(interactionSource)
            .clickable(interactionSource = interactionSource, indication = LocalIndication.current, onClick = onClick)
            .padding(horizontal = 16.dp, vertical = 12.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        val colorScheme = MaterialTheme.colorScheme
        val typeColor = remember(stop.transitType, colorScheme) {
            transitColor(stop.transitType, colorScheme)
        }
        Surface(
            modifier = Modifier.size(40.dp),
            shape = CircleShape,
            color = typeColor
        ) {
            Box(contentAlignment = Alignment.Center) {
                Icon(
                    painter = painterResource(id = transitIconResource(stop.transitType)),
                    contentDescription = transitTypeLabel(stop.transitType),
                    tint = MaterialTheme.colorScheme.surface,
                    modifier = Modifier.size(24.dp)
                )
            }
        }
        Spacer(modifier = Modifier.width(12.dp))
        Column(modifier = Modifier.weight(1f)) {
            Text(text = stop.desc, style = MaterialTheme.typography.titleSmall)
            Spacer(modifier = Modifier.height(2.dp))
            Text(
                text = stop.dirDesc,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}
```

- [ ] **Step 2: Rewire `HomeSearchBar.kt`**

Remove the private `searchStops` function and `StopSearchItem` composable (lines ~248-311 and the `MAX_RESULTS` const); drop now-unused imports. Update the result-item call to:
```kotlin
StopSearchItem(
    stop = stop,
    onClick = { onStopClick(stop) },
    modifier = Modifier.animateItem()
)
```
(the name is already the same, so the call site is unchanged once the private one is gone). Replace `searchStops(allStops!!, query)` calls with the imported `com.trimettransit.tracker.ui.components.searchStops`.

- [ ] **Step 3: Verify**

Run: `./gradlew :common:ui:compileDebugKotlin :feature:home:compileDebugKotlin`
Expected: BUILD SUCCESSFUL.

---

### Task 4: Rename `:feature:vehicles` → `:feature:trips`; drop dormant screen

**Files:**
- Rename: `feature/vehicles` → `feature/trips` (dir tree, incl. `lint-baseline.xml`)
- Modify: `settings.gradle`, `app/build.gradle`
- Modify: `feature/trips/build.gradle` (namespace → `com.trimettransit.tracker.feature.trips`; add `android.joda`)
- Delete: `feature/trips/src/main/java/com/trimettransit/tracker/feature/vehicles/WhatsNearbyScreen.kt`
- Rewrite: `feature/trips/src/main/res/values/strings.xml`

**Interfaces:**
- Consumes: nothing yet.
- Produces: module `:feature:trips` (namespace `com.trimettransit.tracker.feature.trips`) with empty package dirs; strings file holding all trip-planner strings (Task 5-6 reference them by name).

- [ ] **Step 1: Rename the module**

```bash
git mv feature/vehicles feature/trips
```
Update `settings.gradle`: `include ':feature:vehicles'` → `include ':feature:trips'`.
Update `app/build.gradle`: `implementation project(':feature:vehicles')` → `implementation project(':feature:trips')`.

- [ ] **Step 2: Update `feature/trips/build.gradle`**

- `namespace = 'com.trimettransit.tracker.feature.trips'`
- Add dependency: `implementation 'net.danlew:android.joda:2.14.2.1'`

- [ ] **Step 3: Delete the dormant screen**

Delete `feature/trips/src/main/java/com/trimettransit/tracker/feature/vehicles/WhatsNearbyScreen.kt` and the now-empty package dirs. The old `strings.xml` is replaced by Task 7's strings — for now keep a minimal placeholder `whats_nearby_title`? No: rewrite `strings.xml` to an empty `<resources/>` shell so the module still resolves; Task 5-6 add real strings.

- [ ] **Step 4: Verify rename**

Run: `./gradlew :feature:trips:compileDebugKotlin`
Expected: BUILD SUCCESSFUL (empty module compiles).

---

### Task 5: `TripPlannerScreen` — screen shell, map, endpoint picking

**Files:**
- Create: `feature/trips/src/main/java/com/trimettransit/tracker/feature/trips/TripPlannerScreen.kt`
- Modify: `feature/trips/src/main/res/values/strings.xml`

**Interfaces:**
- Consumes: `TransitRepository.planTrip`, models, `LocationHelper` (see below), MapLibre + compose deps.
- Produces: `@Composable fun TripPlannerScreen(transitRepository: TransitRepository, pageVisible: Boolean, isDark: Boolean = false)`.

- [ ] **Step 1: Add a shared location helper in the feature**

Create `feature/trips/src/main/java/com/trimettransit/tracker/feature/trips/LocationHelper.kt` (adapted from the dormant screen's private helpers):
```kotlin
@android.annotation.SuppressLint("MissingPermission")
internal fun readLastKnownLocation(context: Context): Pair<LatLng, Long>?
@android.annotation.SuppressLint("MissingPermission")
internal suspend fun requestCurrentLocation(context: Context): LatLng?
```
(identical bodies to the old `WhatsNearbyScreen` privates, returning `LatLng`).

- [ ] **Step 2: Map state class + bitmap helpers**

Create `feature/trips/.../TripMapState.kt` + `TripMapAssets.kt` mirroring the old `VehicleMapState`/`stopDotBitmap`/`meDotBitmap` (endpoint dots, me-dot, stop-dot) — a `TripMapState` holding GeoJSON sources for: `origin-source`, `dest-source`, `transit-source` (LineLayer, data-driven color), `walk-source` (dashed LineLayer), `stop-source`, `me-source`; method `push(origin, dest, itinerary)`.

- [ ] **Step 3: Screen shell + picking state**

`TripPlannerScreen(transitRepository, pageVisible, isDark)`:
- State (all `remember { mutableStateOf }`): `origin: TripPoint?`, `dest: TripPoint?`, `picking: enum { NONE, ORIGIN, DEST }`, `myLocation: LatLng?`, `locationPermissionGranted`, `hasAskedPermission`, `showLocationExplainer`, job refs (`locationJob`, `planJob`), `planResult: TripPlanResult?`, `selectedIndex: Int?`, `isPlanning`.
- Permission flow: reuse the explainer-dialog → `permissionLauncher` pattern from the old screen; on grant, read last-known or request a fresh fix into `myLocation`; default `origin` to `TripPoint(myLat, myLng, "My location")`.
- Map composable `TripMap(...)` (AndroidView MapView, light/dark basemap swap via `appliedStyleUrl`, onStop/onPause/onDestroy teardown) with:
  - tap → if `picking == ORIGIN` set `origin = TripPoint(lat, lng, "Pinned location")`; if `DEST` set `dest` likewise; then `picking = NONE`.
  - layers rendered from current `origin`/`dest`/selected itinerary.
- Overlay:
  - Top `Surface` card: From row / To row (each opens the picker sheet for that slot), Swap button, segmented Dep-now | Arrive-by control, Plan button (enabled when origin+dest set; shows `CircularProgressIndicator` when `isPlanning`).
  - Map-pin hint chip when `picking != NONE`.

Strings (in `feature/trips/.../strings.xml`): `trips_title`, `from_label`, `to_label`, `my_location`, `pinned_location`, `plan_trip`, `depart_now`, `arrive_by`, `swap`, `tap_map_to_set`, `search_stops_tab`, `map_pin_tab`, `trip_planner_*` etc.

- [ ] **Step 4: Verify**

Run: `./gradlew :feature:trips:compileDebugKotlin`
Expected: BUILD SUCCESSFUL.

---

### Task 6: `TripPlannerScreen` — pickers, plan fetch, results sheet, leg breakdown

**Files:**
- Modify: `feature/trips/src/main/java/com/trimettransit/tracker/feature/trips/TripPlannerScreen.kt`
- Modify: `feature/trips/src/main/res/values/strings.xml`

- [ ] **Step 1: Endpoint picker sheet**

`EndpointPickerSheet(...)` `ModalBottomSheet` with tabs "Search stops" / "Map pin":
- Search tab: lazy-loads `transitRepository.searchStops()` once on first non-blank query (mirror `HomeSearchBar` `LaunchedEffect` pattern), filters via `searchStops()`, renders `StopSearchItem`; on tap sets the slot to `TripPoint(stop.latitude, stop.longitude, stop.desc)` and closes.
- Map pin tab: closes the sheet and sets `picking = ORIGIN` (or `DEST`); the hint chip appears.
- For the origin slot, add a "Use my location" row.

- [ ] **Step 2: Plan fetch**

`fun planTrip()`: cancel prior `planJob`; `isPlanning = true`; `planResult = transitRepository.planTrip(origin!!, dest!!, TripRequestTime(arriveBy, arriveByTimeMillis))`; `selectedIndex` reset to first itinerary; on `Success(plan)` push itinerary to map + fit camera to bounds; on `Error(e)` show snackbar/inline error via `errorMessage`; `isPlanning = false`. Add `RememberOnResume` re-plan only if a plan already exists.

- [ ] **Step 3: Results sheet + leg breakdown**

`ItineraryResultsSheet(plan, selectedIndex, onSelect)` — LazyColumn of up to 3 itinerary cards: departure → arrival, duration, transfers, walk/transit split, walk distance, route badges (letter via `TripLegMode.transitTypeLetter()` + `transitColor`), fare line. Selecting renders on the map.
Selected itinerary UI (bottom sheet): per-leg rows —
- walk: "Walk · 12 min · 0.4 mi" + `<direction>` text.
- transit: badge (letter + color), "Route 20 Burns Rd" / MAX name + direction, board stop @ time → alight stop @ time; "Stay on board" chip when `leg.stayOnBoard`.

- [ ] **Step 4: Error/empty states**

Empty → `EmptyState("No trips found")`; `TripPlannerError` → friendly string per code (map enum → `R.string` in the feature strings file: no stops near origin/dest, no service at origin/dest, trip not possible, trivial distance, ambiguous/not-found, outside district, system outage); blank API key → the existing inline "API key not configured" message the rest of the app uses; offline/null from `planTrip` → "no connection" wording used elsewhere.

- [ ] **Step 5: Verify**

Run: `./gradlew :feature:trips:compileDebugKotlin`
Expected: BUILD SUCCESSFUL (lint clean).

---

### Task 7: Wire the 4th "Trips" tab in `app`

**Files:**
- Modify: `app/src/main/java/com/trimettransit/tracker/activities/MainActivity.kt`
- Modify: `app/src/main/res/values/strings.xml`

- [ ] **Step 1: Nav item + pager page**

- Add `BottomNavItem(3, R.string.nav_trips, Icons.Filled.Directions)` to `bottomNavItems`.
- Add case `3 -> TripPlannerScreen(transitRepository = transitRepository, pageVisible = topPagerState.isScrollInProgress... )` — use the same `pageVisible` threading the pager already supports; pass `isDark = isDark`.
- Adjust the initial `BackHandler`/pill logic so page 3 behaves like other top-level pages (it already does — `isTopLevel` is route-based, unaffected).

- [ ] **Step 2: Strings**

Add to `app/src/main/res/values/strings.xml`: `<string name="nav_trips">Trips</string>`.

- [ ] **Step 3: Verify**

Run: `./gradlew :app:assembleDebug`
Expected: BUILD SUCCESSFUL.

---

### Task 8: Docs (README, CHANGELOG, privacy policy)

**Files:**
- Modify: `README.md`, `CHANGELOG.md`, `docs/privacy-policy.md`

- [ ] **Step 1: README bullets**

Update the live-map bullet to describe the trip planner (map-first from→to planning, depart-now/arrive-by); update the bottom-pill line to list Trips as the 4th item; keep screenshot row/text pointing at `play-phone-*` files (a future screenshot can be added later — do not break/write new images).

- [ ] **Step 2: CHANGELOG**

Top entry: "Trip planner: the new Trips tab plans a from→to trip on the map (tap for origin/destination, pick stops by search, or use your location), with depart-now vs arrive-by, itinerary options with transfers/walk/fare detail, and each step drawn on the map." Plain language, no file paths.

- [ ] **Step 3: privacy-policy**

Reword the "What's Nearby" reference (docs/privacy-policy.md line ~12) to describe location use under the trip planner ("when you set your location as the trip origin").

---

### Task 9: Full CI verification

- [ ] **Step 1: Run the exact CI command**

Run: `./gradlew clean test lint assembleDebug --stacktrace`
Expected: BUILD SUCCESSFUL, lint clean (no new findings; baseline untouched).

- [ ] **Step 2: Live on-device smoke test**

With `TRIMET_API_KEY` configured in `~/.gradle/gradle.properties`: open Trips tab, plan a depart-now trip from My Location to a searched stop; confirm map draws the itinerary; toggle arrive-by and re-plan; drop pins error paths (no service / no connection).