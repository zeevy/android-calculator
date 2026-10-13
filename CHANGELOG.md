# Changelog

All notable changes to this project are documented here.

The format follows [Keep a Changelog](https://keepachangelog.com/en/1.1.0/),
and this project adheres to [Semantic Versioning](https://semver.org/spec/v2.0.0.html).

## [Unreleased]

### Removed

- The "Crash reporting" switch in Settings. The app has no crash reporter, so the switch did nothing; the privacy policy and Play listing now say the app sends nothing anywhere.

### Changed

- The GST calculator is shown only on devices set up for India (an Indian locale, or India's time zone), in the tools menu and in launcher shortcuts.

- History keeps the newest 1000 calculations. Older ones are removed when a new one is saved, so the History sheet stays fast.

### Fixed

- A Feb 29 birthday now counts down to Feb 29 in the next leap year (it stopped at Feb 28).
- Percent change from a negative start has the right sign (-100 to -150 is -50%).
- The ovulation tool is never saved as a recent tool or shown as a launcher shortcut, since its use is private.
- Paste reads only the clipboard's text and at most 200 characters, so pasting a large file can no longer freeze the app.
- The timezone converter opens on the real current time: on a phone in India it no longer shows "UTC 10:00 = India 15:30" at 10:00 local time.
- Hindi users see Hindi everywhere: error messages, BMI categories, unit and category names, and the tools added after the first translation (percentage, investment, tip split, bases, tape, floating calculator).
- TalkBack, Switch Access and Voice Access can use the keypad: every key is a button with a spoken name (for example "Divide", "Memory recall"), and backspace offers "Clear all" as its long-press action. Number fields are read with their label, and mode switches are read as radio buttons with their state.
- Keys and switches are at least 48dp tall, including the short rows of the scientific keypad and the floating calculator.
- The home-screen widget follows the light or dark theme and wallpaper colours.
- The home-screen widget shows all 5 keypad rows. The last row (`⌫ 0 . =`) was missing, so the widget could not give an answer. The keys now fill the widget.
- TalkBack reads each tools menu tile once (it read "Loan, Loan").
- Typed values in every tool survive the app being closed in the background (for example while you switch to another app), and a launcher shortcut no longer jumps back to its tool after such a restart.
- Tool screens say which field is wrong (for example "Must be from 0 to 11.99" under inches) instead of one general message, and BMI rejects inches of 12 or more. A loan tenure above 600 months is rejected instead of slowing the app down.
- The floating calculator and the home-screen widget follow the same rules as the app: `2 + 3 × 4 =` gives 14 everywhere (the floating calculator gave 20), the widget no longer accepts `5++` or `1..2`, and both use the precision from Settings. The floating calculator shows the whole expression while you type and a readable error message.
- Number fields accept the decimal separator of the phone's language, so `10,5` works in German or French. A number with misplaced group separators (such as `10.5` in German) shows an error instead of being read as 105.
- Money amounts use lakh and crore grouping (`12,34,567.89`) on phones set to English (India) or Hindi, in every tool.
- The unit converter keeps the exact value when you swap units, also for results of 1,000 or more; before, swap blanked the result or read `1.000` as 1 in German.
- Pasting a number into the calculator reads it in the phone's language (`1.234,50` in German is 1234.5).
- A loan or investment term long enough to overflow now shows an error instead of `NaN`.
- A damaged settings file no longer crashes the app on every launch; the settings go back to their defaults and save normally again.
- On a cold start the app no longer shows one frame in the wrong theme when the chosen theme differs from the system theme.
- On Android 13 and later, starting the floating calculator from Settings now asks for the notification permission, so its notification and Stop action show in the shade.
- The floating calculator service stops quietly instead of crashing when the overlay permission is missing.
- No activity other than the main screen can be started by other apps.
- A future database change can no longer wipe saved history and unit pairs on upgrade. The database schema is now exported and checked by a migration test, and the destructive fallback is gone (it remains only for a downgrade).

## [1.2.0] - 2026-08-25

### Added

- **Inline calculation history on the basic calculator.** Every `=` prints an
  `expression = result` line above the display, oldest at the top and newest
  just above the result, so a chain of calculations stays visible without
  opening the History sheet. The lines scroll once they outgrow the space,
  with a scroll indicator on the right and the top edge fading out to show
  there is more above. Tap a line to reuse its result; long-press one to copy
  the whole equation or delete just that line. The lines are per-session and
  are not written to disk - the History sheet remains the durable record.

### Changed

- After `=`, the display no longer repeats the expression above the result.
  The new history line already shows `5+6 = 11`, so printing `5+6` again
  directly underneath was the same input twice, one line apart.
- Press-and-hold on backspace now clears the inline history along with the
  expression, for a genuine blank sheet. Reusing an expression from the
  History sheet or a life calculator leaves the history intact.

### Fixed

- Pressing `=` on a value with nothing left to compute no longer records
  anything. `5+6 =` followed by more `=` presses was saving a `11 = 11` row
  to History on every press.

## [1.1.0] - 2026-08-24

The `v1.0.3` tag was cut without a changelog entry or a `versionName` bump,
so everything it carried is folded into this release.

### Added

- Ovulation calculator: the single "Next period" row is now an "Upcoming
  periods" list showing the next three period start dates, one cycle apart.
- Settings > About shows the real version from `BuildConfig` instead of a
  hardcoded `1.0.0-dev`, and the GitHub row opens the repository.

### Changed

- **Every screen now takes its colors from Material 3 color roles** instead of
  private hardcoded palettes, so the whole app follows Material You dynamic
  color and the light/dark setting: the keypad, the three bottom sheets, the
  life-calculator screens and their shared scaffold, the unit converter, and
  the tape view.
- **The orange accent is confined to one column.** The four arithmetic
  operators plus equals run down the right edge; `%`, `±`, and `^` become grey
  modifier/function keys. Applies to both basic and advanced modes.
- The floating calculator overlay follows the system light/dark setting. It is
  a Service-hosted XML view and cannot read the Compose color scheme, so the
  palette moved into color resources with `values-night` overrides.
- Copy and paste use the suspend-based Compose `Clipboard` API, replacing the
  deprecated `LocalClipboardManager`.
- The debug variant is labelled "Calculator (Debug)" so it is distinguishable
  from a release install and the stock device calculator.
- Toolchain and dependencies: AGP 9.3.2, Kotlin 2.4.10, KSP 2.3.11, Gradle
  9.7.0, Compose BOM 2026.08.00, Hilt 2.60.1, and the AndroidX, junit5, and
  kotest stacks. `compileSdk` moves to 37 because the new AndroidX and Compose
  artifacts require it; `targetSdk` stays at 36.

### Fixed

- The Settings sheet and the Tools menu grid no longer clip their lower rows -
  both were unscrollable Columns inside a `ModalBottomSheet`.

### Removed

- Unused Retrofit and OkHttp catalog entries. The app has been fully offline
  since the currency converter was dropped; these were dead weight.

## [1.0.2] - 2026-06-02

### Changed

- Migrated to Android Gradle Plugin 9.2.1 (Gradle 9.4.1) with built-in
  Kotlin; the standalone `kotlin-android` plugin is no longer applied.
- Bumped Kotlin 2.3.21, KSP 2.3.9, Hilt 2.59.2, Compose BOM 2026.05.01,
  the AndroidX core/lifecycle/navigation/room/datastore stack, coroutines,
  kotlinx.serialization, kotest 6, mockk, and the AndroidX test libraries.

### Removed

- Baseline Profile + Macrobenchmark apparatus (the `:baselineprofile`
  module, the `benchmark` build type, the ProfileInstaller dependency,
  and the release-workflow profile-generation job). Not worth the build
  surface for an app this small.

## [1.0.0] - 2026-05-20

First public release. All Phase 1 → Phase 10 deliverables shipped.

### Added

#### Calculator

- Basic mode: arithmetic, percentage, parentheses, sign-flip, hold-to-repeat,
  long-press backspace to clear, DTMF tones, live result preview.
- Advanced mode: trig (sin/cos/tan + inverses), logarithms, sqrt + cbrt,
  power, π and e constants, factorial (`!`), memory (M+/M-/MR/MC), DEG/RAD
  toggle, x²/x³ shortcuts.
- Auto-shrink + multi-line display that wraps long results and shrinks the
  font when even the wrapped form would clip.
- iOS-flavoured colour palette (operator orange + dark-grey digits).

#### Persistence

- History sheet: every committed `=` is recorded; tap to reuse, swipe-or-
  tap-icon to delete, "Clear all" with confirmation.
- DataStore-backed settings: theme (System/Light/Dark), dynamic color,
  haptics, sound, math precision (6-16 sig figs), opt-in crash reporting.

#### Converters

- Unit converter across 11 categories (Length, Area, Volume, Mass,
  Temperature, Speed, Time, Data, Pressure, Energy, Power) with NIST-
  accurate constants. Last-used pair per category is persisted.
- Currency converter: 150+ fiat currencies via open.er-api.com with
  offline cached fallback; pin favourites; relative "Updated" timestamp.

#### Life calculators

- Loan / EMI **estimator** (with explicit "not a lending tool" disclaimer).
- GST (India) calculator: CGST/SGST/IGST split, intra- and inter-state
  toggle, forward and reverse.
- Discount: forward (MRP + % off) and reverse (MRP + final → %).
- BMI: metric (cm/kg) and imperial (ft+in/lb) with WHO categories.
- Age: years/months/days, days to next birthday, weekday of birth.
- Date difference: two-dates mode and date+offset mode.
- Ovulation estimator: ovulation, fertile window, next period, due date
  (Naegele's rule); explicit disclaimer about not being medical advice
  or a contraception tool.

#### Platform integrations

- Material 3 dynamic-color theme (Android 12+).
- Native splash screen tied to the app's dark canvas.
- Static launcher shortcuts: New calculation, Unit converter, Currency
  converter (long-press app icon).
- Home-screen Glance widget: mini calculator running the same evaluator.
- Quick Settings tile to launch the app from the notification shade.
- English + Hindi string resources; locale-aware number formatting
  (en-US, en-IN with lakh/crore grouping, de-DE).
- Baseline profile module (`:baselineprofile`) with startup + frame-
  timing macrobenchmarks; CI workflow regenerates the profile.

#### Engineering

- 259 unit tests across math engine, life-calculator domains, Room
  DAOs, DataStore repositories, view models.
- Static analysis: ktlint + detekt + Android Lint, all green on CI.
- Open-source housekeeping: SECURITY, CODE_OF_CONDUCT, dependabot,
  CODEQL, FUNDING, issue/PR templates, EditorConfig.
- Release-build size: 5.1 MB AAB (3× under the 15 MB target).

### Notes

- App icon redesigned to a forest-green disk + white calculator + "CA"
  badge.
- Crash reporting is **off by default**. Nothing leaves the device until
  the user enables it from Settings.
- The only network permission declared is `INTERNET`, used exclusively
  by the currency converter.
