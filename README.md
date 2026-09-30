# PDX Bus Tracker

Real-time transit for Portland, OR's TriMet system: bus, MAX Light Rail, Streetcar, and WES, plus a trip planner that draws your options on a map. Built with Jetpack Compose and Material 3.

PDX Bus Tracker is an unofficial, community-built app. It is not affiliated with, sponsored by, or endorsed by TriMet. "TriMet" and "TransitTracker" are trademarks of the Tri-County Metropolitan Transportation District of Oregon, used here only to name the transit service this app reads data from. TriMet's logos are not used, and all transit data remains the property of TriMet.

*TriMet and TransitTracker are registered trademarks of TriMet. All rights reserved.*

## Features

- **Live arrivals.** You pick a stop and read the countdown. The app refreshes the list when you come back, and you pull for a fresh read when you want.
- **Trip planner.** You set a start and an end from stop search or a map pin. Your location works too. You leave now or pick an arrival time, then compare itineraries drawn on the map.
- **Line and stop browser.** You browse lines down to directions and stops.
- **Nearby stops and search.** You find stops near you, with distance and direction, and search stops by name from Home. The cached stop list keeps search working offline.
- **Favorites and recents.** You save stops from search, recents, or the lines browser. Saved stops land on your Home list.
- **Detour pills.** You see a pill on arrival rows for your line when TriMet reports a detour.
- **Home-screen widget.** You pin favorite stops and read departures without opening the app.
- **Departure alerts.** You opt in per stop and get a ping minutes before your ride leaves.
- **Pill navigation.** One bottom pill holds Home, Recents, Lines, and Trips. Settings is one tap away.
- **Picture-in-picture.** You keep a countdown floating while you use other apps.
- **Your colors.** You pick the accent and tune its vibrancy. True-black AMOLED is there for OLED screens. You can also recolor each line badge.
- **Line-pinned mode.** You narrow the board to the line you came from.
- **Spanish.** The full app follows your device language.

## Screenshots

| | | | |
|---|---|---|---|
| <img src="docs/screenshots/play-phone-01-arrivals.png" width="190" alt="Real-time arrivals"> | <img src="docs/screenshots/play-phone-02-trip-planner.png" width="190" alt="Trip planner"> | <img src="docs/screenshots/play-phone-03-search.png" width="190" alt="Stop search"> | <img src="docs/screenshots/play-phone-04-lines.png" width="190" alt="Lines browser"> |
| Arrivals | Trip planner | Search | Lines |
| <img src="docs/screenshots/play-phone-05-favorites.png" width="190" alt="Favorites"> | <img src="docs/screenshots/play-phone-06-recents.png" width="190" alt="Recents"> | <img src="docs/screenshots/play-phone-07-trip-results.png" width="190" alt="Trip results"> | <img src="docs/screenshots/play-phone-08-settings.png" width="190" alt="Settings"> |
| Favorites | Recents | Trip results | Settings |

## Requirements

- Android 12 or newer (minSdk 31)
- JDK 21
- Android SDK platform 37

## Architecture

The app is 12 Gradle modules in five layers with strictly downward dependencies: a module can depend on the layers below it, never above it, and no feature depends on another feature.

| Layer | Modules |
|---|---|
| `app` | Launcher, single activity, navigation, floating pill bar, picture-in-picture |
| `feature/*` | `home`, `stops`, `trips`, `arrivals`, `settings` (one screen area per module) |
| `component/*` | `transit` (TriMet API client: OkHttp + JSON/XML parsing, including the Trip Planner web service), `localdata` (SQLite favorites/recent stops) |
| `common/*` | `model` (domain models), `utils` (connectivity, date helpers), `ui` (theme, shared components), `map` (sole MapLibre SDK host and shared phone-map facade) |

There are no ViewModels, no DI framework, and no Room. Screens keep their own state and call the API functions directly.

## Building

1. **Prerequisites:** JDK 21 and Android SDK platform 37.
2. **Get an API key.** Register for a free key at [developer.trimet.org](https://developer.trimet.org/appid/registration/). You only need it for real-time data; the app builds fine without one.
3. **Set the key** (either works):
   ```sh
   export TRIMET_API_KEY=your_key_here
   # or
   ./gradlew assembleDebug -PTRIMET_API_KEY=your_key_here
   ```
4. **Build the debug APK:**
   ```sh
   ./gradlew assembleDebug
   ```
5. **Find the APK** at `app/build/outputs/apk/debug/`.
6. **Run the repo's full check** (tests, lint, and a debug build):
   ```sh
   ./gradlew clean test lint assembleDebug --stacktrace
   ```

### Release builds

Release builds are signed with a local keystore (`app/release.keystore`, not checked in). Set the signing credentials as environment variables; the build fails fast if they are missing:

```sh
export STORE_PASSWORD=... KEY_ALIAS=... KEY_PASSWORD=...
./gradlew assembleRelease bundleRelease
```

- **Signed APK:** `app/build/outputs/apk/release/` (a copy named `PdxBusTracker-release-<version>.apk` also lands in `app/build/outputs/renamed_apks/release/`)
- **Android App Bundle (what Play Console accepts):** `app/build/outputs/bundle/release/app-release.aab`

**Publishing a GitHub release:** bump `versionName` and add a "What's New" section to the changelog, commit and push, then run `scripts/release.sh`. It reads the version from `app/build.gradle.kts`, pulls the latest changelog section, and publishes the release with the signed APK attached (requires the `gh` CLI). `--dry-run` prints what it would do without publishing.

For a local smoke test without real credentials, build with `-PreleaseSigningFallback=true`. Never upload a build signed with the fallback keystore.

## Tech stack

| Stack | Version |
|---|---|
| Gradle / AGP | 9.7.1 / 9.4.0 |
| Kotlin / Compose compiler plugin | 2.4.20 |
| Jetpack Compose | BOM 2026.09.00 (Material 3 1.5.0-alpha28, Navigation 2.10.1) |
| OkHttp | 5.5.0 |
| Joda-Time (android.joda) | 2.14.3.1 |
| Kotlin coroutines | 1.11.0 |
| MapLibre GL Native (OpenGL backend) | 13.6.1 + OpenFreeMap tiles |

## Privacy

PDX Bus Tracker collects no accounts, no analytics, and no advertising data. Location stays on your device, except that browsing nearby stops or planning a trip from where you are sends your coordinates to TriMet's public API to find stops and lines near you. Full details: [Privacy Policy](docs/privacy-policy.md).

## License

This project is licensed under the MIT License. See [LICENSE](LICENSE). Third-party libraries keep their own licenses; see [THIRD-PARTY-NOTICES.md](THIRD-PARTY-NOTICES.md) for the list. Basemap tiles come from [OpenFreeMap](https://openfreemap.org/) (OpenStreetMap data), with attribution shown in the app.

The launcher icon is original artwork: route lines drawn by hand over aerial imagery of Portland from the [USGS National Map](https://basemap.nationalmap.gov/), which is U.S. Geological Survey public-domain material.

TriMet data remains the property of TriMet. PDX Bus Tracker is an unofficial project; TriMet does not sponsor, endorse, or maintain it.