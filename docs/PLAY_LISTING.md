# Play Store listing

Everything the Google Play Console asks for, ready to copy. The step-by-step order is in [RELEASE.md](RELEASE.md). Play's limits: title 30 characters, short description 80, full description 4000.

## App details

| Field | Value |
|---|---|
| Package name | `in.co.gorest.calculator` (fixed for life once uploaded) |
| Default language | English (United States) - en-US |
| App or game | App |
| Free or paid | Free |

## Title

```
Calculator & Unit Converter
```

(27 / 30 chars.) The launcher name on the phone stays "Calculator".

## Short description

```
Scientific calculator, unit converter, loan EMI, BMI and more. Offline. No ads.
```

(79 / 80 chars)

## Full description

```
A modern, fast calculator with a unit converter and 15+ everyday calculators. No ads, no account, and it works fully offline.

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

## Graphics

All files are in [play/](play/). Each one meets Play's rules: PNG with no alpha (except the icon, which may have it), exact sizes, phone screenshots 9:16 at 1080x1920 so Play can use them in its promotions.

| Asset | File | Size |
|---|---|---|
| App icon | [icon-512.png](play/icon-512.png) | 512 x 512, full square (Play rounds the corners) |
| Feature graphic | [feature-graphic.png](play/feature-graphic.png) | 1024 x 500 |
| Phone screenshots | `play/phone-1-*.png` to `play/phone-8-*.png` | 1080 x 1920 each |

Tablet screenshots are optional and left out: the app has no tablet layout yet.

### Phone screenshots

Upload them in this order. Play shows the first three in search results, so they show the main screens. The alt text goes in the box under each screenshot (max 140 characters).

| # | File | Caption on the image | Alt text |
|---|---|---|---|
| 1 | [phone-1-basic.png](play/phone-1-basic.png) | Everyday maths with a live result | Basic calculator showing earlier results above the display and a live result of 30000 while typing 2500×12. |
| 2 | [phone-2-advanced.png](play/phone-2-advanced.png) | Scientific mode: trig, logs and powers | Advanced calculator in degrees with sin, cos, tan, log, roots and powers, working out sin(30)+sqrt(144)^2 = 144.5. |
| 3 | [phone-3-tools.png](play/phone-3-tools.png) | All your calculators in one place | Tools menu with tiles for loan, GST, discount, profit, investment, deposits, BMI, age, time zones and more. |
| 4 | [phone-4-units.png](play/phone-4-units.png) | Convert units in 11 categories | Unit converter changing 42.195 kilometers to 26.22 miles. |
| 5 | [phone-5-loan.png](play/phone-5-loan.png) | Loan EMI, total interest and total paid | Loan estimator with amount, rate and tenure, showing the monthly EMI, total interest and total paid. |
| 6 | [phone-6-floating.png](play/phone-6-floating.png) | A floating calculator over any app | Small floating calculator on top of a shopping bill in a browser, adding the items to 2364. |
| 7 | [phone-7-investment.png](play/phone-7-investment.png) | Plan SIP, lump sum and CAGR | SIP calculator with monthly amount, rate and years, showing total invested, gains and maturity value. |
| 8 | [phone-8-history.png](play/phone-8-history.png) | Full history, in light or dark | History sheet in dark theme listing past calculations with their results and times. |

The screenshots come from the release build on an emulator (1080x2400, systemui demo mode for a clean status bar), framed with the caption at 1080x1920.

## Store settings

| Field | Value |
|---|---|
| App category | Tools |
| Tags | Calculator, Productivity, Utilities (pick the closest ones Play offers) |
| Contact email | The contact email already set on the Play developer account (the same one the other apps on the account use) |
| Website | <https://github.com/zeevy/android-calculator> |
| Privacy policy URL | <https://github.com/zeevy/android-calculator/blob/main/PRIVACY.md> |

## App content answers

Play Console, Policy and programs, App content. Answer each section as below.

| Section | Answer |
|---|---|
| Privacy policy | <https://github.com/zeevy/android-calculator/blob/main/PRIVACY.md> |
| Sign in details (App access) | All functionality is available without any access restrictions |
| Ads | No, the app does not contain ads |
| Content rating | Category "All other app types". Answer No to every question. Expected rating: Everyone, PEGI 3, IARC 3+ |
| Target audience | 13-15, 16-17 and 18 and over. Not designed to appeal to children |
| News app | No |
| Data safety | See [Data safety](#data-safety) below |
| Government apps | No |
| Financial features | My app doesn't provide any financial features (the loan, GST, investment and deposit tools only calculate; see [Loan-screen wording check](#loan-screen-wording-check)) |
| Health apps | Health and fitness: **Activity and fitness** (the BMI calculator measures body composition) and **Period tracking** (the ovulation estimator predicts ovulation and the fertile window). Nothing in Medical |
| Advertising ID | No, the app does not use an advertising ID. Shown only after the first AAB upload |
| Foreground service permissions | See [Special-use foreground service](#special-use-foreground-service) below. Shown only after the first AAB upload |

### Data safety

The app collects no data and shares no data.

| Question | Answer |
|---|---|
| Does your app collect or share any of the required user data types? | **No** |
| Is all of the user data collected by your app encrypted in transit? | **N/A** (no user data is collected) |
| Do you provide a way for users to request that their data be deleted? | **N/A** (no user data is collected; uninstalling deletes all on-device data) |

### Special-use foreground service

| Field | Value |
|---|---|
| Service | `com.calculator.feature.floating.FloatingCalculatorService` |
| Foreground service type | `specialUse` |
| Subtype (`PROPERTY_SPECIAL_USE_FGS_SUBTYPE`) | `floating_calculator` |
| Task | Other: floating calculator window over other apps |
| Video | A link to the demo video (YouTube, unlisted). It shows: Settings, Open over other apps, the window over a browser, the ongoing notification with Stop, and the X that closes it |

"Describe permission use" takes 500 characters. This text is exactly 500:

```
The user opens a small floating calculator window from the app's Settings. It stays on top of other apps, so the user can add up numbers while looking at another app. The service only keeps this window alive: no background work, no network. It must start at once because the user has just asked for the window. It cannot be paused or restarted, because the window would vanish in the middle of a sum and the number would be lost. The user ends it with the X on the window or Stop in the notification.
```

## Permissions justification

These are all the permissions in the merged release manifest. The app makes no network calls and does not declare `INTERNET`.

Declared by the app, all for the floating calculator:

| Permission | Why |
|---|---|
| `SYSTEM_ALERT_WINDOW` | Draws the floating calculator window over other apps. The user grants it on the system settings screen that opens the first time they start the floating calculator. |
| `FOREGROUND_SERVICE` | Keeps the floating calculator window open in a foreground service after the user leaves the app. |
| `FOREGROUND_SERVICE_SPECIAL_USE` | The foreground service type that Android 14 and newer require for this service. See the declaration above. |
| `POST_NOTIFICATIONS` | Shows the floating calculator's ongoing notification with a Stop action. On Android 13 and newer it is asked at runtime the first time the user starts the floating calculator from Settings. |

Added by AndroidX libraries:

| Permission | Added by | Why |
|---|---|---|
| `WAKE_LOCK` | AndroidX WorkManager | The Glance home-screen widget library uses WorkManager to schedule widget updates. This keeps the phone awake for a short time while an update runs. |
| `ACCESS_NETWORK_STATE` | AndroidX WorkManager | Lets WorkManager read the connection state. The app makes no network calls, and this permission cannot send data. |
| `RECEIVE_BOOT_COMPLETED` | AndroidX WorkManager | Lets WorkManager reschedule widget updates after the phone restarts. |
| `in.co.gorest.calculator.DYNAMIC_RECEIVER_NOT_EXPORTED_PERMISSION` | AndroidX Core | A signature permission, so only this app can send broadcasts to its own receivers. |

No contacts, location, storage, camera or microphone permission.

## Loan-screen wording check

Google Play's personal-loans policy bans wording that says or suggests the app is a lender. The loan screen follows it. These are the exact strings shipped:

- Title: `Loan estimator`
- Disclaimer below the result card: `Estimator only - not a lending tool or quote.`
- Input labels: `Loan amount`, `Annual interest rate`, `Tenure`
- Result labels: `Monthly EMI`, `Total interest`, `Total paid`

The unit test `com.calculator.feature.finance.loan.LoanCopyTest` checks the loan strings, the loan tool label and the launcher shortcut labels in English and Hindi. It fails if any of these appear: `apply`, `qualify`, `lender`, `borrow`, `loan us`, `get a loan`, `eligib`, `approval`.
