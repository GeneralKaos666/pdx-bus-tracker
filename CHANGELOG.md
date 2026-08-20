# Changelog

## What's New in v4.9.1

### Bug fixes & code-quality pass
- **Build fixed:** the v4.9.0 merge left unresolved conflict markers in `app/build.gradle` (release signing) and `MainActivity.kt` (What's Nearby page wiring) that broke Gradle configuration and compilation. Both are resolved (HEAD side kept), and the README conflict is cleaned up too.
- **Rail routes show as rail again:** Yellow/Orange MAX (190/290), WES (203), the streetcar lines (193–195), and the Vintage Trolley (196) were drawn with bus badges, bus icons, and bus colors. All MAX lines, streetcar, and WES now get their correct badge letter, icon, and color everywhere (arrival rows, map markers, legends, route list). A stop served by a shuttle bus no longer masks a real rail type behind the generic "Z" type.
- **Stop names with spaces survive navigation:** stop names were URL-encoded with `URLEncoder` (spaces become `+`, which Navigation never decodes), so "SW 6th & Washington" arrived at Arrivals as "SW+6th+&+Washington" in the title, PiP header, map card, and saved favorites. Names are now encoded with `Uri.encode`.
- **Arrivals countdowns tick in the foreground:** the "8 min" badges no longer sit frozen until you refresh — the list recomputes every 30 seconds, so "Due" appears on its own.
- **Arrivals refresh can't show stale data:** a slow older response can no longer overwrite a newer pull-to-refresh; the in-flight fetch is cancelled first. Also: "Show all arrivals" now works for stops served by a single route with more than 5 arrivals, and no longer bypasses the "only show selected route" setting.
- **Delay labels are honest:** a bus 1:59 late was shown as "On time" (integer truncation); delays are now rounded so "1 min late"/"1 min early" can actually appear.
- **Map fixes:** tapping the map card and closing it within a second no longer risks touching a destroyed map (deferred camera fits are dropped once the view detaches); vehicles with missing coordinates (0,0 — the Gulf of Guinea) are filtered out of markers and camera fits on both maps.
- **What's Nearby refresh keeps last-known stops:** a failed refresh no longer blanks every stop marker (vehicles already kept stale data; stops now do too), and a tap that misses every stop dot no longer opens a bogus arrivals screen for stop #0.
- **Bottom pill scroll behavior fixed:** the Arrivals pill auto-hide was inverted (it appeared while scrolling down and hid while scrolling up); it now hides on scroll-down and returns on scroll-up, and no longer flickers at list-item boundaries.
- **Location handling:** the Nearby Stops screen no longer leaks a live GPS listener on every successful refresh (battery drain) and a cancelled refresh no longer kills the newer load's spinner; What's Nearby survives permission revocation between check and call.
- **Search polish:** typing no longer re-downloads the full stop list per keystroke, offline search shows "No connection." instead of "No stops found.", and stale results are cleared while the new query is computing.
- **Misc:** stop cards no longer stay permanently enlarged after the first tap; an arrival exactly one week out now shows its day name (previously omitted for same-weekday-next-week); directions/stops fetch failures in the Routes accordion now offer a Try Again button; Settings can't be pushed twice by a fast double-tap; a favorite saved before the stop's coordinates resolve is fetched on the spot instead of being parked at 0,0; overlapping resume loads on Favorites/Recent are deduped.

## What's New in v4.9.0

### Stop search moves to the Home screen
- The standalone Search screen (top-pager page 3) and its `feature:search` module are gone. Search is now a search field pinned at the top of the Home screen — typing a query opens a floating results dropdown over the Favorites/Recent tabs, with loading / connection-error / "No stops found." states; tapping a result navigates to arrivals and clears the query. All stops lazy-load on the first keystroke instead of on screen open. This also kills the old screen's bug: the M3 `DockedSearchBar` expanded over the screen with an empty panel, so results showed through behind the scrim on the background — nothing expands anymore.
- The bottom pill drops its Search item: Home / Routes / What's Nearby, and the top-level pager shrinks from 4 to 3 pages.
  (`feature/home/.../HomeSearchBar.kt`, new; `feature/home/.../HomeScreen.kt`; `app/.../activities/MainActivity.kt`; `feature/search/` deleted; `settings.gradle`, `app/build.gradle`)

### Bottom pill stays put everywhere but Arrivals
- The scroll auto-hide is now Arrivals-only: the pill no longer slides away while scrolling through the Home lists, Routes, Nearby Stops, or Settings. The `ScrollState` variant of `AutoHideBottomBarEffect` was removed; the `LazyListState` variant remains in use solely on the Arrivals screen.
  (`common/ui/.../components/ScrollHideEffect.kt`; `common/ui/.../NavState.kt`; `feature/home/HomeStopListScreen.kt`, `feature/settings/SettingsScreen.kt`, `feature/stops/StopListContent.kt` + `NearbyStopsScreen.kt`)

### Favorites and Recent are separate screens
- The Favorites/Recent tabs inside the Home screen are gone. They're now two top-level screens of their own: Favorites (with the stop-search field pinned on top, as before) and Recent Stops (a bold "Recent Stops" title header above the list, matching the What's Nearby screen's layout). The inner 2-page home pager (`homePagerState`) and the Home tab-swap in the pill are removed.
- The bottom pill no longer morphs between a Home item and Favorites/Recent tab items depending on which page you're on — it's now a fixed 4-item bar: Favorites / Recent / Routes / What's Nearby.
  (`feature/home/.../FavoritesScreen.kt`, new; `feature/home/.../RecentStopsScreen.kt`, new; `feature/home/.../HomeScreen.kt` deleted; `app/.../activities/MainActivity.kt`)

## What's New in v4.8.2

### Alerts are a dialog now
- The detours "Alerts (N)" card that expanded inline at the top of the arrivals list is gone. Alerts are now a small round button — warning icon in error-container colors, with a count badge straddling the circle's top-right edge like a notification badge — right-aligned at the top of the arrivals list; tapping it opens a standard dialog listing every detour description, with a Close button. (The icon is a vector drawable rendered via `painterResource` — the material `Icons.Default.Warning` glyph did not render on some devices.)
- As you scroll, the button itself is the strip: one pinned element continuously morphs between the 40dp round button and a 4dp full-width red bar at the top of the list — size, offset, and color interpolate with the scroll progress while the icon and badge shrink and fade away. The collapsed bar stays fully visible for the rest of the scroll and freely overlaps the arrival cards as they pass beneath it; it is purely decorative (not tappable). Scroll back up and the button stretches out of the bar again.
  (`feature/arrivals/.../ArrivalsScreen.kt`)

## What's New in v4.8.1

### Bug fixes & cleanup
- **Lint baselines cleared:** every one of the 38 baselined lint issues across all 12 modules is fixed — all `lint-baseline.xml` files are now empty and the full CI gate (`clean test lint assembleDebug`) passes with no warnings.
- **Pictures-in-picture:** pressing Home (or swiping away) while on the Arrivals screen now auto-enters PiP with the countdown still visible; the PiP entry animation zooms from the mini-window button itself. The manual button still works.
  (`app/.../activities/MainActivity.kt`)
- **State correctness:** lat/lng, tracking IDs, and the alerts-bar alpha now use primitive state (`mutableDoubleStateOf`/`mutableIntStateOf`/`mutableFloatStateOf`) instead of boxed `mutableStateOf`, and the location age in What's Nearby uses `mutableLongStateOf` — no autoboxing churn on every recomposition.
  (`common/ui/.../NavState.kt`; `feature/arrivals/ArrivalsScreen.kt`; `feature/vehicles/WhatsNearbyScreen.kt`)
- **Logging:** migrated the last two `android.util.Log` calls to Timber (5.0.1), now planted in `TrimetTransitTracker.onCreate` when debuggable; `TAG` constants are gone.
  (`app/.../TrimetTransitTracker.kt`; `feature/arrivals/ArrivalsScreen.kt`)
- **Modernized APIs:** `Bitmap.createBitmap` → core-ktx `createBitmap` (×3); `Configuration.screenWidthDp` → `LocalWindowInfo.current.containerSize` for the bottom-pill label hide threshold (same 400dp behavior); dead pre-minSdk-31 `SDK_INT` branches removed; the map's touch-consumer now calls `performClick()` (accessibility).
  (`feature/arrivals/ArrivalsScreen.kt`; `app/.../activities/MainActivity.kt`; `common/ui/.../theme/Theme.kt`; `common/utils/.../ConnectionUtils.kt`)
- **Manifest & resources:** removed the redundant activity label and `enableOnBackInvokedCallback`; added `android:dataExtractionRules` (backup/transfer stay disabled, matching `allowBackup="false"`); merged `mipmap-anydpi-v26` into `mipmap-anydpi` (minSdk 31 makes the qualifier dead); deleted the unused `trimet_blue_dark` color; declared `ACCESS_NETWORK_STATE` in `common:utils` so the offline check is permission-visible at the module level.
  (`app/.../AndroidManifest.xml`, `app/src/main/res/`; `common/utils/.../AndroidManifest.xml`)
- **Dependencies:** Gradle wrapper 9.5.0 → 9.7.0, appcompat 1.7.0 → 1.8.0, core 1.15.0 → 1.19.0 (feature/home), Timber 5.0.1 added; release builds now also shrink resources (`shrinkResources true`).
  (`gradle/wrapper/`; `app/build.gradle`, `feature/home/build.gradle`, `feature/arrivals/build.gradle`)
- **Conventions:** `rememberOnResume` renamed to `RememberOnResume` (Compose naming rule); `LoadingState`/`StopMapCard`/`ArrivalItem` modifier params moved first; the arrivals list's odd indentation fixed.
  (`common/ui/.../components/AutoRefreshEffect.kt`, `StateComponents.kt`; `feature/arrivals/ArrivalsScreen.kt`, `feature/home/HomeScreen.kt`, `feature/stops/NearbyStopsScreen.kt`, `feature/vehicles/WhatsNearbyScreen.kt`)

## What's New in v4.8.0

### Auto-hiding bottom bar
- The bottom pill now slides away while you scroll down through arrivals, stop lists, search results, and settings, and slides back in as soon as you scroll up — more room for content with no extra taps. A new `AutoHideBottomBarEffect` watches scroll offsets (a `LazyListState` variant for lists, a `ScrollState` variant for Settings) and writes `NavState.bottomBarVisible`, which the outer scaffold animates into the pill's slide/fade. The bar always stays put in picture-in-picture, and resets to visible whenever you change screens.
  (`common/ui/.../components/ScrollHideEffect.kt`, new; `common/ui/.../NavState.kt`; `app/.../activities/MainActivity.kt`; `feature/arrivals/ArrivalsScreen.kt`, `feature/home/HomeStopListScreen.kt`, `feature/stops/StopListContent.kt` + `NearbyStopsScreen.kt`, `feature/search/SearchStopsScreen.kt`, `feature/settings/SettingsScreen.kt`)

### Bottom pill: FAB gone, Back button on Settings
- The standalone Settings FAB is removed entirely — Settings is opened from the pill's trailing button on every screen, and while the Settings screen is open that same trailing button becomes a Back button. The Settings top bar is gone, so Settings now navigates like every other screen.
- Tapping a pill item or a Home tab now always returns to the top-level pager first (popping the nav stack back to `"home"` if needed) before switching pages.
  (`app/.../activities/MainActivity.kt`)

### Dependency updates
- Compose BOM 2024.12.01 → 2026.08.00, material3 1.4.0-alpha12 → 1.4.0 (stable), navigation-compose 2.8.5 → 2.9.8, activity-compose 1.9.3 → 1.13.0, core/core-ktx 1.15.0 → 1.19.0, material 1.12.0 → 1.14.0, MapLibre OpenGL 13.4.1 → 13.5.0, okhttp 4.12.0 → 5.5.0, coroutines-android 1.7.3 → 1.11.0, android.joda 2.12.1 → 2.14.2.1, Compose compiler plugin 2.2.10 → 2.4.10.
  (per-module `build.gradle`; root `build.gradle`)

### Settings: real version number
- The About card's version string is now read live from `PackageManager` instead of a hardcoded `BuildConfig.VERSION_NAME` that had frozen at 4.6.5 — it can no longer drift from the installed APK (the settings module's `buildConfig` feature was removed).
  (`feature/settings/SettingsScreen.kt`; `feature/settings/build.gradle`)

### Official TriMet logo icon
- The launcher icon is now TriMet's official brand mark — an orange (`#E0651F`) disc with the three white strokes of the "T" swirl logo — adapted from the public-domain `TriMet logo 2.svg` (the same logo trimet.org uses). The old white bus glyph on dark blue is gone. The strokes are exact copies of the official vector's bezier paths, so the icon matches the brand mark pixel-for-pixel; the monochrome layer still reuses them for themed icons.
  (`app/src/main/res/values/colors.xml`; `ic_launcher_background.xml` / `ic_launcher_foreground.xml`)

### Housekeeping
- The TriMet base route URL is resolved once via `stringResource` instead of per-composable `context.getString` calls in Search and the Stops screens.
  (`feature/search/SearchStopsScreen.kt`; `feature/stops/StopsScreen.kt`, `StopsRouteList.kt`)

## What's New in v4.7.0

### Floating navigation pill
- The bottom navigation bar is now a Material 3 Expressive floating pill (material3 1.4.0-alpha12 `HorizontalFloatingToolbar`) that hovers above the system navigation bar, using the vibrant expressive color tokens — primaryContainer pill, surfaceContainer pop-out for the selected item, tertiaryContainer action button — so it follows the app's dynamic color scheme in light and dark.
- The FAB moved into the pill as its trailing button: a plain Settings button that opens the Settings screen directly (no expanding menu).
- Home's Favorites/Recent tabs moved into the pill: on Home, the pill's first slot becomes the two tab buttons.
  (`app/.../activities/MainActivity.kt`)

### Swipe between top-level screens
- Home, Routes, What's Nearby, and Search stops are now pages of one swipeable pager — swipe left/right to move between the main screens. The pill highlights the current screen and tapping a pill item animates the pager; system back walks back to Home before leaving the app.
- Settings moved off the nav bar into the FAB, opening as its own screen with a back button.
- Swipes keep their context: Home swipes its Favorites/Recent tabs, the map pans on What's Nearby, and Routes/Search swipe between screens.
- Pressing Home in the pill from any other screen always opens the Favorites tab.
  (`app/.../activities/MainActivity.kt`)

### Routes accordion
- The Routes screen's three tabs (Routes / Directions / Stops) are gone. Tapping a route now expands its directions inline under the row — the same expand-and-fade pattern as the arrivals map card — and tapping a direction expands its stops; tap again to collapse.
- The expanded drill-down survives swiping to another screen and back, and returns from Arrivals intact.
  (`feature/stops/StopsScreen.kt`, `StopsRouteList.kt`; `AnimatedTabRow.kt` removed)

### QR scanning removed
- QR code scanning is gone: the CameraX + ML Kit camera scanner (`QRCameraActivity`), the loading/security screen (`feature/qr` module), the `qr2.it` network-security domain, and the CAMERA permission were all removed, along with the repo's only unit test. The FAB is now a plain Settings button.
  (`feature/qr` deleted; `app/.../activities/MainActivity.kt`, `AndroidManifest.xml`, `network_security_config.xml`)

### Dead code cleanup
- Removed the unused `TransitApi.fetchAlerts` function and its `base_alerts_url` string (detours already come through `fetchArrivals`), the empty `values/integers.xml`, and five empty drawable density folders left over from the old PNG launcher icons.
  (`component/transit/.../TransitApi.kt`, `component/transit/.../res/values/strings.xml`; `app/src/main/res/`)

## What's New in v4.6.5

### New app icon
- The launcher icon is now an adaptive icon: a white bus glyph on the dark TriMet blue (`#005A91`), with a monochrome variant for themed icons. The old static PNG icons are gone.
  (`app/src/main/res/mipmap-anydpi-v26/ic_launcher.xml`, new `ic_launcher_background.xml` / `ic_launcher_foreground.xml`; old `drawable-*/ic_launcher.png` removed)

### Better "What's Nearby" map
- Stop markers now show the stop name as a label right on the map, styled to stay readable over any tile background.
- Nearby stop markers are larger (44dp) with a dark outline ring and a bigger white center dot, so they stand out on light and dark map backgrounds alike.
- The map's search radius around you is wider — 800 ft instead of 500 ft — so more nearby transit appears.
  (`feature/vehicles/WhatsNearbyScreen.kt`)

## What's New in v4.6.1

### Smoother animations
- Press feedback on cards, tabs, buttons, and icon buttons is now softer — the shared `pressScale` spring uses a gentler stiffness, so press/release feel smoother everywhere.
- Screen navigation now springs in (velocity-matched slide + fade) instead of a linear tween, so arriving screens ease to rest naturally.
- The FAB menu rows (Scan QR code / Search stops) animate with the same standardized fade + slide timing as the FAB, bottom bar, and top bar.
- Home → Arrivals and Routes → Arrivals navigation is a hair shorter (300ms vs 350ms) with a snappier slide-in, so tapping a stop gets you to arrivals faster.
  (`common/ui/.../components/Animations.kt`; `app/.../activities/MainActivity.kt`)

### Zoom on tap
- Tapping a favorite or recent stop on Home, or a stop on the Routes screen, now zooms the card in slightly before the Arrivals screen slides in — a quick 200ms scale-up that makes the selection feel direct.
  (`common/ui/.../components/StopListItem.kt`, new `zoomOnTap` option; `feature/home/HomeStopListScreen.kt`; `feature/stops/StopsStopList.kt`)

### Animations where there were none
- The arrivals countdown ("5 min" → "4 min") now crossfades on each refresh instead of snapping.
- The Settings radio icons animate their highlight color when switching theme options (System / Light / Dark).
  (`feature/arrivals/ArrivalsScreen.kt`; `feature/settings/SettingsScreen.kt`)

## What's New in v4.5.0

### New "What's Nearby" map
- Rewrote the old Vehicles screen into a live interactive MapLibre map of nearby buses, MAX trains, and streetcars on OpenFreeMap vector tiles (no API key required). Shows each vehicle with a transit-type badge, nearby stops, and your location, auto-frames the vehicles, and refreshes when the app returns to the foreground. Requests location permission on first use and prefers a fresh GPS fix, falling back to the last known location.
  (`feature/vehicles/WhatsNearbyScreen.kt`, new; `VehiclePositionsScreen.kt` removed)

### Bottom navigation
- Replaced the hamburger navigation drawer with a Material 3 bottom navigation bar: Home, Routes, What's Nearby, Settings. The top bar (with back button) now appears only on Search, Nearby Stops, and Arrivals screens and animates in with fade + slide transitions, along with the FAB.
  (`app/.../activities/MainActivity.kt`; `DrawerContent.kt` removed)

### Search & Nearby Stops
- Stop search and nearby-stops states (loading / error / empty / content) now crossfade instead of popping.
- "Nearby stops" requests a fresh high-accuracy location fix with a 10-second timeout before falling back to the last known location.
  (`feature/search/SearchStopsScreen.kt`; `feature/stops/NearbyStopsScreen.kt`; new `TransitApi.fetchSearchStops`)

### QR scanning
- Scanning now shows a clear "Could not start camera" message when the camera can't start, and invalid stop IDs are rejected before any network call.
- Stop details loaded after a scan now include route and transit-type info.
  (`feature/qr/QRScannerCameraScreen.kt`; `feature/qr/QRLoadingScreen.kt`; `TransitApi.fetchStopById`)

### Theme & appearance
- Changing the theme (System / Light / Dark) in Settings now applies immediately — no app restart needed.
- Corrected error-container colors in the light and dark color schemes.
  (`app/.../activities/MainActivity.kt` theme pref listener; `common/ui/.../theme/Color.kt`)

### Scrolling & polish
- Smoother fling scrolling that hands leftover velocity to pull-to-refresh instead of swallowing the gesture.
- Home favorites/recent lists now show an error state if loading fails, instead of silently appearing empty.
- Arrivals detours button and loading states animate smoothly.
  (`common/ui/.../components/SmoothFlingBehavior.kt`; `feature/home/HomeScreen.kt`; `feature/arrivals/ArrivalsScreen.kt`)

### Security & stability
- HTTPS is re-verified after any redirect so a redirect to `http://` can never silently downgrade API data in transit.
- Added QR-security unit tests; arrivals/vehicle/detour models cleaned up (`isQueryError` removed, mutable collections, `heading` renamed `bearing`).
  (`component/transit/.../JSONParser.kt`; `feature/qr/src/test/.../SecurityUtilsTest.java`)