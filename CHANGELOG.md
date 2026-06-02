# Changelog

All notable changes to this project are documented here.

The format follows [Keep a Changelog](https://keepachangelog.com/en/1.1.0/),
and this project adheres to [Semantic Versioning](https://semver.org/spec/v2.0.0.html).

## [Unreleased]

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
