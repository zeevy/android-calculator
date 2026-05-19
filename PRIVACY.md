# Privacy Policy

**Calculator** by zeevy. Effective date: 2026-05-19.

## TL;DR

- The app collects **no personally-identifiable information**.
- The app makes **one type of network request** — fetching public
  currency-exchange rates from [open.er-api.com](https://open.er-api.com).
- Crash reporting is **off by default**. You opt in from Settings.
  Nothing leaves the device until you do.
- Everything else (calculator history, settings, recent unit pair,
  cached currency rates) lives on-device only.

## What data the app handles

| Data | Where it lives | Where it goes | Why |
|---|---|---|---|
| Calculator history (expressions + results + timestamps) | On-device Room database | Stays on-device. Auto Backup may sync it to your Google account per Android's standard behaviour. | So you can recall past calculations. |
| User settings (theme, haptics, sound, precision, crash-opt-in) | On-device DataStore | Stays on-device. Auto Backup applies. | So the app remembers your preferences. |
| Last-used unit pair per category | On-device Room database | Stays on-device. | So the unit converter opens on the pair you used last. |
| Cached currency rates | On-device Room database | Stays on-device. | So the currency converter works offline. |
| LMP date (Ovulation calculator) | Local Compose state only | **Never persisted anywhere.** Forgotten when you leave the screen. | Sensitive input - kept ephemeral by design. |
| Crash reports | Only if you opt in (Settings → Privacy → Crash reporting) | Anonymous reports to the crash-reporting backend if and only if you've enabled the toggle. | To help fix bugs. The toggle is **off by default**. |

## Network usage

The only outbound network call the app makes is to
**`https://open.er-api.com/v6/latest/{base}`** to fetch daily fiat-currency
exchange rates. This call:

- Does **not** include any user identifier, account ID, device ID, or
  personal data.
- Is anonymous — the API endpoint is publicly accessible without
  authentication.
- Is gated by the standard `INTERNET` permission declared in the
  manifest.
- Can be skipped entirely if you never open the Currency converter.

## Third-party SDKs

The app does **not** include any analytics SDK, advertising SDK, or
behavioural-tracking library. The runtime dependencies are limited to:

- Jetpack Compose + Material 3 (UI)
- Room (local database)
- DataStore (preferences)
- Retrofit + OkHttp + kotlinx.serialization (currency API only)
- Hilt (dependency injection)
- AndroidX coroutines (concurrency)
- AndroidX SplashScreen, Glance, ProfileInstaller

A complete list is in
[`gradle/libs.versions.toml`](gradle/libs.versions.toml).

## Permissions

| Permission | Why |
|---|---|
| `INTERNET` | Currency rate refresh only. Every other feature works offline. |

There are **no runtime permission requests** — the calculator does not
read contacts, location, storage, camera, microphone, or any other
sensitive resource.

## Data Safety form (Play Console)

This app declares the following on the Google Play Data Safety form:

- **Data collected**: None.
- **Data shared with third parties**: None.
- **Data encrypted in transit**: N/A (only public, anonymous API
  endpoint is contacted).
- **Data deletion**: Uninstalling the app removes all on-device
  storage. There is no off-device data to delete.

## Children

The app is **rated for everyone** and contains no behaviour targeted at
children, but also makes no special arrangements for kids — it's a
generic calculator.

## Changes

If this policy changes, the new version will be posted at
[zeevy/android-calculator](https://github.com/zeevy/android-calculator)
and an in-app notice will appear on the next release.

## Contact

Questions or concerns? Open an issue at
[github.com/zeevy/android-calculator](https://github.com/zeevy/android-calculator/issues).
