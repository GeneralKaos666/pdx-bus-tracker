# Changelog

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