# TriMet Bus Tracker

Real-time transit tracker for Portland, OR's TriMet system — bus, MAX Light Rail, Streetcar, and WES Commuter Rail.

*TriMet and TransitTracker are registered trademarks of TriMet. All rights reserved.*

---

## Features

- **Real-time arrivals** — check upcoming departures at any stop
- **Route browser** — explore routes, directions, and stops in a 3-tab view
- **Nearby stops** — find stops around your current location via GPS
- **Favorites & recent stops** — save frequent stops locally in SQLite
- **QR code scanning** — scan a stop QR code to jump straight to arrivals (CameraX + ML Kit)
- **Service alerts & detours** — view active TriMet alerts for routes and stops
- **Dynamic theming** — Material 3 with Android 12+ dynamic color support

## Screenshots

***

## Building

1. **Prerequisites:** JDK 21 and Android SDK platform 37.
2. **Get an API key:** Register for a free key at [developer.trimet.org](https://developer.trimet.org/appid/registration/).
3. **Set the key:**
   ```sh
   export TRIMET_API_KEY=your_key_here
   ```
4. **Build the debug APK:**
   ```sh
   ./gradlew assembleDebug
   ```
5. **APK location:** `app/build/outputs/apk/debug/`

## License

This project is licensed under the MIT License — see the [LICENSE](LICENSE) file for details.

The app uses TriMet's public Web Services API. TriMet data and trademarks remain the property of TriMet.
