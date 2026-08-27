# Play Store listing text — PDX Bus Tracker

Copy-paste ready text for Google Play Console. Pair with the image assets in
[`docs/play-store-listing/`](play-store-listing/) (see checklist §3). Before
submitting, decide on a contact email (see note at the bottom).

---

## App name

```
PDX Bus Tracker
```

---

## Short description (80 chars max — this one is 78)

```
Real-time Portland TriMet arrivals, live vehicle map, routes & stop favorites.
```

---

## Full description

```
PDX Bus Tracker is an unofficial, community-built tracker for Portland, OR's
TriMet transit system — bus, MAX Light Rail, Streetcar, and WES Commuter Rail.
It is not affiliated with, sponsored by, or endorsed by TriMet.

Real-time arrivals
- Live departure countdowns at any stop, with current per-vehicle positions on
  a map card
- Pull-to-refresh and auto-refresh when you return to the app
- Picture-in-picture mini countdown window while you use other apps

Live "What's Nearby" map
- Watch nearby buses, MAX trains, streetcars, and stops on an interactive map,
  with your own location
- Tap a stop to jump straight to its arrivals

Routes & stops
- Browse routes → directions → stops in an animated drill-down
- Find stops around your current GPS location
- Instant stop search by name right on the Home screen

Favorites & recent stops
- Save stops and revisit them instantly; stored locally on your device

Service alerts & detours
- Active TriMet alerts shown right on each route's arrival card

Looks great, your way
- Material 3 design with Android dynamic color, plus system / light / dark
  themes
- Floating navigation pill that stays out of the way as you scroll

Free, ad-free, no accounts, no in-app purchases.

Requires Android 12 (API 31) or higher. Location is used only on-device and,
when you browse nearby stops or vehicles, sent as coordinates to TriMet's
public API to find stops near you.

Privacy policy:
https://github.com/GeneralKaos666/pdx-bus-tracker/blob/master/docs/privacy-policy.md

Project source: https://github.com/GeneralKaos666/pdx-bus-tracker

"TriMet" and "TransitTracker" are trademarks of the Tri-County Metropolitan
Transportation District of Oregon; they are referenced here solely to identify
the transit service the app reads data from.
```

---

## Release notes — "What's new in this version" (v4.9.5)

```
• The floating navigation bar now collapses to a single pill on Arrivals,
  Nearby Stops, and Settings — branded with that screen's own icon and a back
  button — instead of showing the last-viewed tab.
• Picture-in-picture countdowns are fresher: PiP pulls new data every 20
  seconds and keeps "X min" times rolling over between refreshes.
• Smaller installs: release builds now actually shrink (R8 + resource
  shrinking), and unused bundled fonts were removed.
```

---

## Contact email — decide before submitting

Play Console requires a contact email; it is NOT satisfied by the GitHub issue
tracker. Pick an address and enter it in Store listing → Contact details. A
`GMail`/`outlook`-style address at a domain you control is recommended; if you
choose an `@gmail.com` address it will be displayed publicly on the store page.
```

---

## Checklist §3 cross-reference

- App icon → `play-store-listing/app-icon-512.png`
- Feature graphic → `play-store-listing/feature-graphic-1024x500.png`
- Phone screenshots → `docs/screenshots/` (nine PNGs, one per UI state)
