# Privacy Policy — PDX Bus Tracker

**Effective date:** September 4, 2026

PDX Bus Tracker is a free, open-source, unofficial transit tracker for the Portland,
Oregon area. This policy describes what information the app handles and where it goes.
The short version: **the app has no accounts, no ads, and no analytics — your data
stays on your phone except for the specific transit lookups described below.**

## 1. Location information

**When it's used:** only when you open the "Nearby Stops" feature, use your current
location as a trip origin in the Trip Planner, or grant location permission to center
one of the maps. The app never tracks your location in the background.

**Where it goes:** when you search for nearby stops, or start a planned trip from your
current location, your GPS coordinates are sent as part of a query to **TriMet's public
Developer API** (`developer.trimet.org`) — as `ll=latitude,longitude` for nearby-stops
lookups, or as the origin coordinates of a trip-planning request — so TriMet's servers
can return the stops or routes within reach. This is required for the feature to
work.

**What we do with it:** nothing else. Coordinates are used in memory, are never stored
by the app, and are never shared with advertisers, analytics providers, or any other
third party. Location permission is optional: without it you can still browse routes,
stops, favorites, and arrivals by name.

## 2. Information stored on your device

- **Favorites and recent stops** are saved in the app's local SQLite database on your
  device (including stop names/IDs and their map coordinates). This data never leaves
  your device and is removed if you uninstall the app or clear its data.
- **Settings** (theme, display preferences) are stored in local app preferences.

## 3. Network requests

- **TriMet API** (`developer.trimet.org`): arrival times, vehicle positions, routes,
  stop lists, service alerts, and trip-planning requests. Requests include your API key
  registration ID and standard server log data such as your IP address.
- **Map tiles:** the in-app maps use [MapLibre Native](https://maplibre.org/) with
  vector tiles from [OpenFreeMap](https://openfreemap.org/) (built on OpenStreetMap
  data). Requesting tiles necessarily reveals your IP address and the approximate area
  you are viewing; OpenFreeMap does not require accounts or personal data. See
  [OpenFreeMap's privacy policy](https://openfreemap.org/legal/privacy/) and
  OpenStreetMap's terms for details.
- Map attribution (© OpenStreetMap contributors, © OpenFreeMap) is displayed in-app at
  all times.

## 4. What the app does NOT do

- No user accounts or sign-ups
- No advertising or ad SDKs
- No analytics, crash-reporting, or tracking SDKs
- No selling, renting, or sharing of personal data with third parties for their own use
- No collection of data from children under 13; the app is directed at the general public

## 5. Permissions

| Permission | Why |
|---|---|
| `ACCESS_COARSE_LOCATION` / `ACCESS_FINE_LOCATION` | Find nearby stops, plan trips from your location, and show your position on the map (foreground use only, always initiated by you) |
| `INTERNET`, `ACCESS_NETWORK_STATE` | Fetch live transit data and detect offline state |

## 6. Data deletion

Uninstalling the app (or clearing its storage from Android Settings → Apps) deletes all
favorites, recent stops, and settings stored on the device. There is no server-side
account or stored profile to delete.

## 7. Open source

PDX Bus Tracker is MIT-licensed open source; you can verify every claim in this policy
by reading the code at
[github.com/GeneralKaos666/pdx-bus-tracker](https://github.com/GeneralKaos666/pdx-bus-tracker).

## 8. Contact

Questions about this policy? Open an issue at
[github.com/GeneralKaos666/pdx-bus-tracker/issues](https://github.com/GeneralKaos666/pdx-bus-tracker/issues).

## 9. Changes to this policy

If the app's data handling changes, this policy will be updated here and in the app's
release notes with a new effective date.

---

*PDX Bus Tracker is an unofficial project and is not affiliated with, sponsored by, or
endorsed by TriMet. Transit data is provided by TriMet's public Developer API and
remains the property of TriMet.*
