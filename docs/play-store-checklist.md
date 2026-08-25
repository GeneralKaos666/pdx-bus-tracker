# Google Play submission checklist — PDX Bus Tracker

Console-side steps for publishing the app as a **free, ad-free** release. Code/repo
prerequisites are already done: original launcher icon, trademark-safe branding,
privacy policy (`docs/privacy-policy.md`), third-party license notices, signed
release builds via env vars, and `./gradlew bundleRelease` producing the required
`.aab`.

## 1. Build artifacts

- [ ] `./gradlew clean test lint assembleDebug --stacktrace` passes (CI parity)
- [ ] Release credentials set: `STORE_PASSWORD`, `KEY_ALIAS`, `KEY_PASSWORD`
- [ ] `./gradlew bundleRelease assembleRelease` succeeds
- [ ] Upload `app/build/outputs/bundle/release/app-release.aab` (Play requires AAB, not APK)
- [ ] versionCode incremented past anything already on Play

## 2. App content declarations (Policy → App content)

| Question | Answer | Why |
|---|---|---|
| Privacy policy URL | `https://github.com/GeneralKaos666/pdx-bus-tracker/blob/master/docs/privacy-policy.md` | Required; hosted in-repo |
| Ads | **No ads** | No ad SDKs anywhere in the code |
| In-app purchases | Free, **no in-app purchases** | Nothing purchasable |
| Content rating questionnaire | General audience; no user-generated content, no violence/gambling/etc. → expect **Everyone / 3+** | Tracker utility app |
| Target audience | Select age groups **13+ only** | Avoids Families-policy obligations; app is directed at the general public |
| Data safety — does your app collect or share any of the required user data types? | **Yes** (location is transmitted) — see table below | Honesty first; details below |
| Data deletion | "No — my app doesn't allow users to create an account" / no server-side data exists | No accounts, no server storage |
| App access | All functionality available without restriction | Live data needs no login |
| News apps / COVID / government / financial features | No | Not applicable |

### Data safety form detail

The only off-device data flow besides anonymous tile/API fetches is the user-initiated
nearby search, which sends current coordinates to TriMet's API
(see privacy policy §1). Recommended entries:

| Type | Collected | Shared | Purpose | Notes |
|---|---|---|---|---|
| Approximate location | Yes | No | App functionality | Sent to TriMet only when you open Nearby Stops / What's Nearby |
| Precise location | Yes | No | App functionality | Same single request path |

Mark both as **processed ephemerally** (used in-memory for the lookup, never stored
by the app) and **optional** (all other features work without granting location).
No other data types apply: no accounts, analytics, crash reporting, advertising IDs,
contacts, photos, files, health, financial, messages, browsing, contacts, or
device IDs beyond what Android/OkHttp inherently transmit.

## 3. Main store listing assets

- [ ] **App name:** PDX Bus Tracker (30 chars max — fits)
- [ ] **Short description** (80 chars): e.g. "Real-time Portland TriMet arrivals, live vehicle map, routes & stop favorites."
- [ ] **Full description:** expand from README Features section
- [ ] **App icon:** 512×512 PNG (export from the original launcher artwork — route lines over USGS aerial)
- [ ] **Feature graphic:** 1024×500 JPG/PNG (needs creating; keep it original artwork — no TriMet logos/maps)
- [ ] **Phone screenshots:** 2–8; the nine in `docs/screenshots/` cover every tab
- [ ] 7" tablet screenshots: optional but recommended (Compose layout scales; capture later)
- [ ] **Contact email:** *required field* — GitHub issues alone do NOT satisfy this; decide on an address before submitting
- [ ] Website/social: optional (repo URL works)

## 4. Trademark guardrails (keep these true at submission time)

- Listing text may say "unofficial tracker for TriMet" descriptively but must not use
  TriMet's logo, system map imagery, or imply endorsement
- Screenshot set contains no TriMet-branded art (app UI is original)
- First line of the full description repeats the unofficial/attribution disclaimer

## 5. Release setup

- [ ] Enroll in **Play App Signing** (upload key = local `release.keystore`)
- [ ] Create production release, upload AAB, add release notes (paste CHANGELOG v4.9.5 section)
- [ ] Staged rollout 100% or start small at your discretion
- [ ] After approval: tag `v4.9.5` and push

## 6. Post-launch reminders

- Every update: bump `versionCode`, refresh CHANGELOG, re-upload AAB
- Keep `docs/privacy-policy.md` effective date in sync whenever data handling changes
