# Wear Changelog

Changelog for the standalone Wear OS companion app (independent version line from the phone app).

## What's New in Wear v2.1.1

### Signing
- **Debug builds now use the release keystore:** debug and release installs share the same signing identity, so switching between them no longer requires an uninstall. When release signing credentials aren't set, debug builds gracefully fall back to the default debug key.

## What's New in Wear v2.1.0

- **Smaller release builds:** the watch app now ships with R8 code shrinking and resource shrinking enabled on release builds, trimming APK size.

## What's New in Wear v2.0.0

- **The watch app is now fully standalone:** favorites and recent stops live on the watch itself — every stop's arrivals screen has a **heart button** to favorite it (first favorite powers the new tile below), and any stop you open is remembered in **Recent stops**. Nothing is synced from the phone anymore; the watch talks straight to TriMet.
- **New "Next departure" Tile:** add the one-handed **PDX Bus** tile to your watch face for a live, per-minute countdown to the soonest bus at your first favorite stop. Tap the tile to jump straight to that stop's arrivals. It refreshes in the background every half hour, so it stays current even when you haven't opened the app.
- **Material 3 Expressive styling:** the watch now follows your watch face's color accent and uses the updated expressive type scale, with a branded navy/teal look when no dynamic accent is available.

## What's New in Wear v1.1.0

- **Wear Routes browser:** a new **Routes** screen on the watch mirrors the phone's route drill-down — pick a route, then a direction, then a stop to open its live arrivals. Route, direction and stop data is fetched live from TriMet (the same data source the phone uses), so it stays current without any phone-side syncing.
- **Wear About screen:** a new **About** screen on the watch shows the app name, version and license at a glance.
- **Smoother, animated watch screens:** the main menu, routes browser, stop lists and arrivals now animate in and out with fade/slide entrance transitions, and list rows respond with a subtle press animation — matching the phone app's feel across every screen.
- Worth noting: the Wear module now carries its own version line (1.1.0) separate from the phone app; a **Routes** entry and an **About** entry join **Favorites**, **Recent stops** and **Arrivals** on the watch's home menu.
