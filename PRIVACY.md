# Privacy Policy

**Calculator** by zeevy. Effective date: 2026-10-13.

## Summary

- The app does not collect any personal data.
- The app works fully offline. It makes no network calls and does not declare the `INTERNET` permission.
- There are no ads, no analytics and no crash reporter.
- What you enter stays on your device. Only Android's own backup can copy it, to your own Google account.

## What the app stores on your device

| Data | Where it is stored | Why |
|---|---|---|
| Calculation history: expression, result, time and type. The newest 1000 entries are kept. | Room database, `history` table | So you can see and reuse past calculations. |
| The last unit pair used in each unit category | Room database, `recent_unit_pair` table | So the unit converter opens on the pair you used last. |
| Settings: theme, dynamic colour, haptics, sound and math precision | DataStore, `user_settings` | So the app remembers your choices. |
| Tool choices: last unit category, GST intra-state or inter-state and rate, BMI height and weight units, the two time zones in the timezone converter | DataStore, `user_settings` | So each tool opens the way you left it. |
| Up to three recently opened tools | SharedPreferences, `recent_tools` | So the launcher shortcuts show your recent tools. The ovulation tool is never saved here. |

Numbers and dates you type into a tool, including the last period date in the ovulation tool, are not written to a database or a file. Android keeps them only in the screen's saved state while the app is open. They come back if Android closes the app in the background.

Uninstalling the app deletes all of this data.

## Backup

Android Auto Backup is on. The rules are in [`data_extraction_rules.xml`](app/src/main/res/xml/data_extraction_rules.xml). Android can back up the database, the shared preferences and the DataStore files listed above to your Google account. It can also copy them to a new phone when you move to it. Android and Google do this, not the app, and the developer never receives this data. You can turn off backup in your phone's system settings.

## Network

The app makes no network calls. It does not declare the `INTERNET` permission and does not include any network library.

The final app manifest has `ACCESS_NETWORK_STATE` because AndroidX WorkManager adds it (see [Permissions](#permissions)). It only lets WorkManager check whether the phone is connected. It cannot send or receive data.

## Third-party libraries

The app does not include any analytics, advertising, tracking or crash-reporting library. It uses only these libraries:

- Jetpack Compose and Material 3 (screens)
- Room (local database)
- DataStore (settings)
- Hilt (dependency injection)
- Navigation Compose and kotlinx.serialization (moving between screens)
- Kotlin coroutines
- AndroidX Core, Activity, Lifecycle, SplashScreen and Startup
- Glance (home-screen widget), which brings in AndroidX WorkManager

The full list is in [`gradle/libs.versions.toml`](gradle/libs.versions.toml).

## Permissions

The app itself declares four permissions. All four are for the floating calculator, a small calculator window that stays on top of other apps.

| Permission | Why |
|---|---|
| `SYSTEM_ALERT_WINDOW` | Draws the floating calculator window over other apps. You grant it on the system settings screen that opens the first time you start the floating calculator. |
| `FOREGROUND_SERVICE` | Keeps the floating calculator window open in a foreground service after you leave the app. |
| `FOREGROUND_SERVICE_SPECIAL_USE` | The foreground service type that Android 14 and newer require for this kind of service. |
| `POST_NOTIFICATIONS` | Shows the floating calculator's ongoing notification with a Stop action. On Android 13 and newer the app asks for it the first time you start the floating calculator from Settings. |

AndroidX libraries add four more to the final manifest. The Glance home-screen widget library uses AndroidX WorkManager to schedule widget updates, and WorkManager adds the first three.

| Permission | Added by | Why |
|---|---|---|
| `WAKE_LOCK` | AndroidX WorkManager | Keeps the phone awake for a short time while WorkManager runs a widget update. |
| `ACCESS_NETWORK_STATE` | AndroidX WorkManager | Lets WorkManager check whether the phone is connected. The app makes no network calls, and this permission cannot send data. |
| `RECEIVE_BOOT_COMPLETED` | AndroidX WorkManager | Lets WorkManager reschedule widget updates after the phone restarts. |
| `io.github.zeevy.calculator.DYNAMIC_RECEIVER_NOT_EXPORTED_PERMISSION` | AndroidX Core | A signature permission. Only this app can send broadcasts to its own receivers. |

The app does not read your contacts, location, storage, camera or microphone.

## Data Safety form (Play Console)

The app gives these answers on the Google Play Data Safety form:

- **Data collected**: none.
- **Data shared with third parties**: none.
- **Data encrypted in transit**: not applicable. No data is collected or sent.
- **Data deletion**: not applicable. No data leaves the device. Uninstalling the app deletes everything it stored.

## Children

The app is rated for everyone. It is a general calculator. It is not made for children and has no special features for them.

## Changes

If this policy changes, the new version will be posted at [zeevy/android-calculator](https://github.com/zeevy/android-calculator) with a new effective date.

## Contact

For questions, open an issue at [github.com/zeevy/android-calculator](https://github.com/zeevy/android-calculator/issues).
