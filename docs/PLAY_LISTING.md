# Play Store Listing Copy

Copy these into the Google Play Console fields. Play's current limits: title up to 30 characters, short description up to 80 characters, full description up to 4000 characters.

## Title

```
Calculator - CA
```

(15 / 30 chars)

## Short description

```
Scientific calculator, unit converter and life calculators. Offline. Ad-free.
```

(77 / 80 chars)

## Long description

```
A modern, fast, ad-free calculator that does more than add and subtract. It works fully offline.

CORE
• Basic mode: arithmetic, percentage, parentheses, sign flip, repeat equals
• Advanced mode: trig (sin / cos / tan and inverses), logarithms, square and cube roots, powers, π and e constants, factorial
• Memory keys: M+ / M- / MR / MC
• Live result preview as you type
• A tape of this session's results above the display, and a History of past calculations

UNIT CONVERTER
• 11 categories: length, area, volume, mass, temperature, speed, time, data, pressure, energy, power
• ~70 units; NIST-accurate constants
• Last-used pair per category remembered
• Number bases: decimal, hexadecimal, octal and binary

LIFE CALCULATORS
• Loan EMI estimator: monthly EMI, total interest and total paid
• GST (India) with CGST / SGST / IGST split, forward and reverse (shown on phones set up for India)
• Discount: forward (% off → final) and reverse (final → %)
• Profit and markup: profit or loss, markup and margin, or the selling price from a markup or margin
• Tip and bill split
• Investment: SIP, lump sum and CAGR
• FD, RD and PPF deposits with a year-by-year PPF table (shown on phones set up for India)
• Salary hike: new salary per month and per year, or the hike % between two salaries
• Percentage calculator
• BMI in metric or imperial
• Age in years / months / days, days to next birthday
• Date difference and date + offset
• Time zone converter
• Ovulation estimator: ovulation date, fertile window, next period, due date
• Tape: a paper-roll list of your calculations

EXTRAS
• Floating calculator: a small calculator window that stays on top of other apps
• Home-screen widget and Quick Settings tile
• Launcher shortcuts for the floating calculator and your recent tools
• English and Hindi

SETTINGS
• Theme: System / Light / Dark
• Dynamic colour from your wallpaper (Android 12+)
• Haptics, sound, math precision

PRIVACY
• Fully offline: no network calls
• No analytics, no ads, no trackers, no account
• Your history and settings stay on your phone
• Apache 2.0 open-source on GitHub: zeevy/android-calculator
```

## Categorisation

| Field | Value |
|---|---|
| Category | Tools |
| Tags | Calculator, Productivity, Utilities |
| Content rating | Everyone |
| Target age | 13+ (Tools default) |

## Contact details

| Field | Value |
|---|---|
| Email | venkateswara.rao@zaggle.in |
| Privacy policy URL | <https://github.com/zeevy/android-calculator/blob/main/PRIVACY.md> |
| Website | <https://github.com/zeevy/android-calculator> |

## Data Safety declaration

Answers for the Play Console Data Safety form. The app collects no data and shares no data.

| Section | Answer |
|---|---|
| Does your app collect or share any of the required user data types? | **No** |
| Is all of the user data collected by your app encrypted in transit? | **N/A** (no user data is collected) |
| Do you provide a way for users to request that their data be deleted? | **N/A** (no user data is collected; uninstalling deletes all on-device data) |

## Permissions justification

These are all the permissions in the merged release manifest. The app makes no network calls and does not declare `INTERNET`.

Declared by the app, all for the floating calculator:

| Permission | Why |
|---|---|
| `SYSTEM_ALERT_WINDOW` | Draws the floating calculator window over other apps. The user grants it on the system settings screen that opens the first time they start the floating calculator. |
| `FOREGROUND_SERVICE` | Keeps the floating calculator window open in a foreground service after the user leaves the app. |
| `FOREGROUND_SERVICE_SPECIAL_USE` | The foreground service type that Android 14 and newer require for this service. See the declaration below. |
| `POST_NOTIFICATIONS` | Shows the floating calculator's ongoing notification with a Stop action. On Android 13 and newer it is asked at runtime the first time the user starts the floating calculator from Settings. |

Added by AndroidX libraries:

| Permission | Added by | Why |
|---|---|---|
| `WAKE_LOCK` | AndroidX WorkManager | The Glance home-screen widget library uses WorkManager to schedule widget updates. This keeps the phone awake for a short time while an update runs. |
| `ACCESS_NETWORK_STATE` | AndroidX WorkManager | Lets WorkManager read the connection state. The app makes no network calls, and this permission cannot send data. |
| `RECEIVE_BOOT_COMPLETED` | AndroidX WorkManager | Lets WorkManager reschedule widget updates after the phone restarts. |
| `com.calculator.DYNAMIC_RECEIVER_NOT_EXPORTED_PERMISSION` | AndroidX Core | A signature permission, so only this app can send broadcasts to its own receivers. |

No contacts, location, storage, camera or microphone permission.

### Special-use foreground service declaration

Play Console asks for this under App content, Foreground service permissions.

| Field | Value |
|---|---|
| Service | `com.calculator.feature.floating.FloatingCalculatorService` |
| Foreground service type | `specialUse` |
| Subtype (`PROPERTY_SPECIAL_USE_FGS_SUBTYPE`) | `floating_calculator` |

Description:

```
A small calculator window the user opens from Settings stays on top of other apps until the user closes it with its X button or the Stop action in the notification. The foreground service only keeps that window alive; it does no background work.
```

## Loan-screen wording check

Google Play's personal-loans policy bans wording that says or suggests the app is a lender. The loan screen follows it. These are the exact strings shipped:

- Title: `Loan estimator`
- Disclaimer below the result card: `Estimator only - not a lending tool or quote.`
- Input labels: `Loan amount`, `Annual interest rate`, `Tenure`
- Result labels: `Monthly EMI`, `Total interest`, `Total paid`

The unit test `com.calculator.feature.finance.loan.LoanCopyTest` checks the loan strings, the loan tool label and the launcher shortcut labels in English and Hindi. It fails if any of these appear: `apply`, `qualify`, `lender`, `borrow`, `loan us`, `get a loan`, `eligib`, `approval`.

## Screenshots (TODO)

The listing needs 8 phone screenshots, 1 screenshot for a 7" tablet and 1 for a 10" tablet. Take them on a Pixel 6a class phone with the release build installed. Suggested set:

1. Basic calculator with a multi-line expression in progress.
2. Advanced calculator showing trig keys and the memory chip.
3. Tools menu on a phone set up for India (all 21 tiles: Basic, Advanced, the 17 tools, History and Settings).
4. Floating calculator on top of another app.
5. Unit converter on the Length category.
6. History sheet with several entries.
7. Loan estimator filled in.
8. Settings sheet showing the appearance and feedback controls.

Put the files in `docs/screenshots/`. Each file should be 1080x2400 (Pixel 6a native).
