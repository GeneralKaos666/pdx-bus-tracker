# Copilot instructions for PDX Bus Tracker

## Project shape

PDX Bus Tracker is an unofficial Android app for TriMet real-time transit data, built with Kotlin, Jetpack Compose, and Material 3. The repository also contains `screenshot-editor/`, a separate Next.js app used to compose store screenshots.

The Android project has strict one-way module dependencies:

```text
app / wear
  -> feature/*
  -> component/*
  -> common/*
```

The modules are declared in `settings.gradle`. Keep dependencies flowing downward; do not add `feature`-to-`feature` or library-to-`app` dependencies.

- `app` owns the phone activity, Compose navigation, floating pill navigation, PiP, and the home-screen widget.
- `wear` is a standalone Wear OS app. It owns its screens, SQLite-backed favorites/recent stops, direct transit API access, and the "Next departure" tile. It does not communicate with the phone app.
- `feature/home`, `feature/stops`, `feature/trips`, `feature/arrivals`, and `feature/settings` each own one phone screen area.
- `component/transit` owns the transit repository, OkHttp client, and hand-written API parsing.
- `component/localdata` owns SQLite favorites and recent-stop persistence.
- `common/model`, `common/utils`, `common/ui`, and `common/map` contain shared domain models, utilities, Compose/theme components, and the sole MapLibre host.

There are no ViewModels, DI framework, or Room. Screens keep UI state with `remember`/`mutableStateOf` and call suspend repository APIs. Keep raw `TransitApi` and `DatabaseHelper` calls behind repository interfaces, including in widget workers, Wear code, and tile workers.

## Build, test, and lint

The supported local environment is Termux with JDK 21 and Android SDK platform 37. Use the repository Gradle wrapper:

```sh
# Debug APKs for phone and Wear
./gradlew assembleDebug

# Only the Wear debug APK
./gradlew :wear:assembleDebug

# Canonical repository gate
./gradlew clean test lint assembleDebug --stacktrace

# Existing unit-test suite
./gradlew :app:testDebugUnitTest

# Run one JUnit 4 test method
./gradlew :app:testDebugUnitTest --tests 'com.trimettransit.tracker.widget.WidgetConfigTest.round trips the full config'
```

The current test suite is small and is under `app/src/test`; it uses plain JUnit 4 rather than Robolectric or Espresso. Every Android module enables lint with `abortOnError = true` and a module-local `lint-baseline.xml`; do not add new baseline entries casually.

The transit module reads `TRIMET_API_KEY` from a Gradle property first, then the environment, and finally uses an empty value. An empty key is valid for compilation but live API data will not work. Keep secrets out of tracked properties; machine-specific values belong in the user's Gradle properties or environment.

For release builds, signing credentials are supplied with `STORE_PASSWORD`, `KEY_ALIAS`, and `KEY_PASSWORD`, or use `-PreleaseSigningFallback=true` only for a local smoke test. Phone release version codes can be resolved from Play by the Gradle Play Publisher plugin when credentials are configured; Wear release version codes are supplied with `-PreleaseVersionCode=...` and otherwise use the hardcoded fallback.

The screenshot editor has its own commands:

```sh
cd screenshot-editor
bun install
bun dev
bun build
```

Its canonical editable state is `app-store-screenshots.json`; uploaded screenshots under `public/screenshots/uploaded/` must be committed with that file.

## Code and dependency conventions

- Use the existing inline dependency versions in each module; there is no version catalog.
- Android modules target/compile SDK 37, require min SDK 31, and compile with Java 21.
- Use `org.maplibre.gl:android-sdk-opengl`, not the Vulkan-only `android-sdk` artifact. MapLibre integration belongs in `common:map`.
- Shared arrival behavior belongs in `common/model` helpers. Preserve `arrivalKey`/`dedupeArrivals` so phone, widget, Wear, and tile lists cannot produce duplicate keys.
- Detour data uses the API's singular `"route"` field first, with `"routes"` only as a compatibility fallback. Detour filtering is per route; do not restore a global alerts strip.
- The Wear UI uses Wear Compose 1.6.2 APIs: use `MaterialTheme(colorScheme = ...)`, `Chip` instead of `TextButton`, and import list `items` extensions from `androidx.wear.compose.material` when needed. Wear navigation comes from Wear Compose navigation; do not add phone `navigation-compose` to the Wear module.
- Wear tile rendering must not throw from `tileResponse`; log failures and return a fallback tile. Keep tile cache data in `SharedPreferences` because tile requests run on the system render thread. Preserve the manifest tile-provider permission `com.google.android.wearable.permission.BIND_TILE_PROVIDER` and the preview metadata.
- Keep third-party license notices synchronized between `THIRD-PARTY-NOTICES.md` and the Settings/About licenses UI when dependencies change.
- Update `CHANGELOG.md` for user-visible behavior changes. Changelog and README entries describe behavior in plain language and should not include source file paths.

## Product and asset constraints

The product name is **PDX Bus Tracker**. "TriMet" may appear only as a nominative reference to the transit service or data source, never as the product name. Do not add TriMet logos, the TransitTracker clock mark, system-map artwork, brand-derived paths, or confusingly similar assets. New drawable and map artwork must be original or clearly licensed/public domain.

When user-facing text names the TriMet or TransitTracker marks, include the existing trademark attribution string from the phone/Wear string resources:

> TriMet and TransitTracker are registered trademarks of TriMet. All rights reserved.

Keep the original launcher artwork and OpenFreeMap/USGS attribution intact. The app package/application ID remains `com.trimettransit.tracker`.

## Local repository instructions

If a local `AGENTS.md` is present, read it for device-specific build details and current implementation gotchas. It is intentionally ignored by Git; do not commit it or local credential/config files such as `local.properties`, keystores, `fastlane/`, or `.gplay/`.
