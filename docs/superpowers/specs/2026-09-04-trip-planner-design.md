# Trip Planner — Map-First From→To Itineraries (Trips Tab)

Date: 2026-09-04 · Status: Approved · Module work: `:feature:vehicles` → `:feature:trips`

## Overview

Rework the dormant "What's Nearby" live map screen into a map-first trip planner
for the TriMet Trip Planner web service (`ws/V1/trips/tripplanner`). It becomes
the new 4th top-level tab ("Trips") in the bottom pill / pager.

The user picks an origin and destination — from My Location (origin only), a map
pin tap, or stop search — chooses depart-now or arrive-by, and gets up to 3
itinerary options. Selecting an option renders it on the map (legs, stops,
walking connections) with a leg-by-leg breakdown.

## Goals / Non-goals

Goals:
- Full from→to trip planning against TriMet's Trip Planner API.
- Map-first interaction, reusing the MapLibre infrastructure from What's Nearby.
- 4th top-level tab (Favorites / Recent / Routes / Trips).
- Depart-now vs arrive-by time modes.
- Richer itinerary detail: transfers, walk/transit split, fare, leg breakdown,
  "Stay on board" interline chips.

Non-goals (v1):
- Date picker (always "today"); arrive-by time is the only custom scheduling.
- Exact route geometry on the map (the API returns no polyline; v1 draws a
  "stick" route between leg endpoints plus stop dots).
- Free-text geocode entry (origin/dest come from stops, pins, or my location).
- Caching or offline plans; on-demand requests only.

## Data layer (`component:transit`)

- New resource string `base_trip_planner_url` =
  `https://developer.trimet.org/ws/V1/trips/tripplanner` (verified live in 2026;
  requires appID).
- `JSONParser.fetchXml(url): String`: same HTTPS-only guards as `fetch`, returns
  the raw response body. Convert with `org.json.XML.toJSONObject(...)` (org.json
  ships in `android.jar`, no new dependency), then parse with the existing
  JSONObject idiom.
- `TransitApi.fetchTripPlan(...)`:
  - Params: `fromPlace`/`fromCoord`, `toPlace`/`toCoord` (coord = `lon,lat`;
    place = human label used verbatim when a coord is given), `time` =
    `h:mm am/pm`, `date` = `M-d-yyyy`, `arr` = `D` (depart by) or `A` (arrive
    by), `min` = `T` (quickest), `mode` = `A` (all modes), `walk` = `0.5` miles,
    `maxIntineraries` = `3`, `format` = `xml`.
  - Depart-now uses device-local current time; arrive-by uses the chosen time.
    `time`/`date` are always sent (required by the service).
  - Parse `<response>` → `<itineraries>` → `<itinerary>` list; parse per-leg
    `<time-distance>` dates/times into joda `DateTime` (matches `Arrival`).
- Error mapping from documented `<error code="...">` values:
  20003/20004 no stop within walking distance of origin/destination;
  20005/20006 no service at origin/destination at requested time;
  20007 trip not possible; 20020/20021 unknown/ambiguous origin+destination;
  20022/20023 ambiguous origin/destination; 20024/20025 could not find
  origin/destination; 20026 origin/destination trivially close; 21000 C-TRAN
  (Vancouver); 21001 SAM (Sandy). Others → generic failure.
- `TransitRepository` interface + `TransitRepositoryImpl` gain
  `suspend fun planTrip(from: TripPoint, to: TripPoint, time: TripRequestTime): TripPlan?`.

## Models (`common/model`)

- `TripPoint(lat: Double, lon: Double, description: String)`.
- `TripLegMode` enum: `WALK`, `BUS`, `LIGHT_RAIL`, `COMMUTER_RAIL`, `STREETCAR`,
  `RAIL` (fallback), mapped from the GTFS-style numeric `mode` attr (3 bus,
  10 light rail, 13 streetcar, 11 commuter rail, 8 walk, etc.).
- `TripLeg(mode, routeNumber: String?, routeName: String?, direction: String,
  from: TripPoint, to: TripPoint, departure: DateTime, arrival: DateTime,
  stayOnBoard: Boolean)` — `stayOnBoard` from `order="thru-route"` (interline).
- `TripItinerary(id, departure, arrival, durationMillis, distanceMeters,
  numberOfTransfers, walkTimeMillis, transitTimeMillis, waitingTimeMillis,
  fare: String?, legs: List<TripLeg>)`.
- `TripPlan(from, to, itineraries)`.
- `TripRequestTime(arriveBy: Boolean, timeMillis: Long?)` — null time = leave
  now (depart-by).

## Feature module: `:feature:vehicles` → `:feature:trips`

- Rename directory `feature/vehicles` → `feature/trips`; `namespace` →
  `com.trimettransit.tracker.feature.trips`; `settings.gradle` include
  `:feature:trips`; app dependency `implementation project(':feature:trips')`.
  Nothing else references `:feature:vehicles`.
- Delete the dormant `WhatsNearbyScreen.kt` (and obsolete `whats_nearby_*`
  strings) — fully replaced by `TripPlannerScreen`.
- Keep MapLibre, compose, activity-compose deps (from the old module's
  build.gradle). Both feature modules' screenshot workflow unaffected.

## Trip Planner screen

State lives in the composable per repo convention (`remember { mutableStateOf }`,
job-dedupe on in-flight fetches, no ViewModels). Reuse location helpers and the
MapLibre `mapState` pattern from the dormant screen.

Layout:
- Full-bleed MapLibre map with baseline light/dark switch (OpenFreeMap Liberty /
  Dark), me-dot, stop-dot and `badgeBitmap` image helpers.
- Top overlay card: From / To rows (tap to edit), swap button, a depart-now vs
  arrive-by segmented control, and a Plan button (enabled once both endpoints
  set; shows an inline spinner while planning).
- Endpoint picker (bottom sheet): tabs **Search stops** (reuses shared
  stop-search) and **Map pin** (switches the map into tap-to-pick mode with a
  hint chip); "My Location" offered for the origin slot.
- Results sheet: up to 3 itinerary cards — departure → arrival, total duration,
  transfers, walk/transit split, walking distance, route badges for each line,
  fare. Tap to select.
- Selected itinerary: leg-by-leg strip with walk legs (time/distance/`<direction>`
  text) and transit legs (badge + number/name, direction, board/alight stop +
  time), and "Stay on board" chips for interlines.

Map layers (added on top of the existing source/layer set):
- `origin-layer` / `dest-layer`: green / red endpoint markers with labels.
- `itinerary-line`: `LineLayer` — transit legs drawn as route-colored solid
  lines between leg endpoints; walk legs drawn dashed gray. Route color via
  `transitColor` on the mode-derived letter.
- `itinerary-stop`: stop dots (reuse `stopDotBitmap`) at leg endpoints.
- Route badges (`badgeBitmap`) at boarding points.
- Camera fits the selected itinerary bounds; an empty state fits origin/dest.

Location permission: reuse the explainer-dialog → system dialog pattern; origin
defaults to My Location when granted.

## App wiring (`app`)

- 4th `BottomNavItem` (label `nav_trips`, `Icons.Filled.Directions`); pager page
  3 renders `TripPlannerScreen`. Keep `isDark`/`pageVisible` threading.
- Move/refresh feature strings; delete orphaned `whats_nearby_*` strings.
- `docs/privacy-policy.md`: update "What's Nearby" wording to the trip planner.
- `CHANGELOG.md`: user-facing entry, plain language, no file paths.
- README feature bullets updated.

## Verification

- `./gradlew clean test lint assembleDebug --stacktrace` (exact CI command).
- Live on-device plan with the configured `TRIMET_API_KEY`: depart-now and
  arrive-by plans; stop-search, map-pin, and My Location origins; itinerary
  selection renders on the map; error states (no service / offline).