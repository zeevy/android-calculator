# Calculator App - High-Level Requirements

**Version:** 0.3 (Draft)
**Date:** 2026-10-13
**Inspiration:** Mi Calculator (com.miui.calculator) by Xiaomi Inc.

---

## 1. Product Vision

A modern, fully-offline, multi-purpose calculator for Android that bundles a standard calculator, scientific mode, a unit converter, and a suite of everyday "life" calculators (loan, GST, BMI, age, discount, date diff, ovulation) into a single clean, fast, ad-free experience.

## 2. Goals & Non-Goals

### Goals
- Cover the day-to-day math needs of an average user without forcing them to install separate apps.
- Work fully offline. No network calls and no `INTERNET` permission. The app declares only the permissions the floating calculator needs (see Permissions in section 5).
- Look and feel native on modern Android (Material 3 / Material You, dynamic color, light & dark themes).
- Keep the app lightweight (target installed size < 15 MB).

### Non-Goals (v1)
- Graphing / equation plotting.
- Symbolic math, calculus, matrices.
- Programmer mode with bitwise operations - deferred to v2. Base conversion (decimal, hex, octal, binary) ships as the Bases tool (section 4.5).
- Account sync / cloud history.
- Multi-platform (iOS, web) - Android only for now.
- Currency conversion - removed during development; live FX rates would require network + a permission, which conflicts with the "fully offline" goal. Deferred indefinitely.

## 3. Target Platform & Tech Stack

| Area | Choice |
|---|---|
| Language | Kotlin (latest stable, 2.x) |
| Min SDK | API 31 (Android 12) - chosen to make Material You dynamic color, native SplashScreen API, and modern motion/shape primitives first-class without compat branching |
| Target SDK | API 36 (Android 16) - latest stable |
| Compile SDK | API 37 - required by the AndroidX/Compose artifacts in use (they declare a minCompileSdk of 37). Bumped independently of Target SDK, which stays at API 36 until Android 17 runtime behavior changes are reviewed and device-tested. |
| UI Toolkit | Jetpack Compose + Material 3 (adopting Material 3 Expressive components) |
| Architecture | MVVM + Unidirectional Data Flow, single-activity |
| DI | Hilt |
| Async | Kotlin Coroutines + Flow |
| Persistence | Room (history, recent unit pairs) + DataStore Preferences (user settings and per-tool memory) |
| Navigation | Navigation Compose with type-safe routes (serializable route classes) |
| Networking | None. App is fully offline; no `INTERNET` permission. It declares only the floating calculator permissions (see Permissions in section 5). |
| Math Engine | Custom evaluator: `BigDecimal` for arithmetic/precision-sensitive ops; bounded `Double` with rounding for transcendental ops (sin/cos/log/exp). Optionally back transcendentals with a BigMath-style library if precision is insufficient. |
| Build | Gradle Kotlin DSL, Version Catalogs (libs.versions.toml), R8 minification + resource shrinking on release |
| Static analysis | ktlint + detekt + Android Lint (error severity) |
| Performance | Startup library for lightweight init |
| Widgets | Jetpack Glance (Compose-style App Widget API) |
| Splash | AndroidX SplashScreen API + adaptive icon |
| Testing | JUnit5 (unit tests via `android-junit5` plugin), JUnit4 + Robolectric (Compose UI tests on the JVM), Turbine, MockK. No instrumented tests yet |
| CI | GitHub Actions: debug build, unit and JVM Compose UI tests, ktlint + detekt + Android Lint, R8 release build with a 15 MB AAB size check. An emulator job is added with the first test that needs a device |
| Distribution | Google Play (AAB), Play App Signing |

## 4. Functional Requirements

### 4.1 Basic Calculator (must-have)
- Operations: + - × ÷, parentheses, percentage, sign toggle, decimal.
- Live preview of the result as the user types.
- Inline session tape above the display: one `expression = result` line per committed `=`, newest at the bottom, scrollable (with a scroll indicator) once the lines overflow the display area. Tap a line to recall its result. In-memory only - a long-press on backspace clears it along with the expression; the durable record stays in the History view.
- Clear (C) and backspace.
- Editable expression (tap to position cursor; edit anywhere).
- History view: scroll past calculations, tap to reuse, swipe to delete, clear all.
- Copy result to clipboard; long-press to copy any history row.
- Haptic feedback on key press (configurable).

### 4.2 Scientific Calculator (must-have)
- Trigonometric: sin, cos, tan and inverses; deg/rad toggle.
- Logarithms: log, ln, e^x, 10^x.
- Powers & roots: x², x^y, √, ∛, x!.
- Constants: π, e.
- Memory keys: MC, MR, M+, M-.
- Switch between basic and scientific with the Basic and Advanced tiles in the Tools menu. The app is portrait only; rotate-to-scientific is deferred (section 4.7).

### 4.3 Unit Converter (must-have)
- Categories: Length, Area, Volume, Mass / Weight, Temperature, Speed, Time, Data (bytes), Pressure, Energy, Power.
- Two-pane input (from / to), tap to swap, decimal precision configurable.
- Last used unit pair remembered per category.

### 4.4 (Removed)
This feature was removed during development to keep the app fully offline. See section 2 (Non-Goals).

### 4.5 Life Calculators and Tools (must-have)
Each is a focused screen with its own inputs and outputs. All of them open from the Tools menu, in this order:
- **Units** - the unit converter (section 4.3).
- **Loan / EMI** - principal, rate, tenure → EMI, total interest, amortisation table.
- **GST** (India) - amount + rate (5/12/18/28% presets) → CGST, SGST/IGST, net & gross. Shown only on devices set up for India (section 11).
- **Discount** - MRP + discount % → savings + final price; reverse calc (final + MRP → %).
- **Profit** - cost price + selling price → profit or loss, markup % (on cost), margin % (on selling price); or cost + markup % / margin % → selling price and profit. A loss shows as "Loss" with loss % on cost; a margin of 100 % or more is an input error.
- **Tip & split** - bill + tip + number of people → tip, total, per-person share, with an option to round up.
- **Investment** - SIP (monthly amount) or lump sum + annual rate + tenure → total invested, estimated gains, maturity value; CAGR mode: start value + end value + tenure → CAGR %, absolute return %, gain.
- **Deposits** (India) - FD (amount, rate, years + months, compounding monthly/quarterly/half-yearly/yearly), RD (monthly deposit, rate, months) and PPF (yearly deposit 500 to 1,50,000, rate, 15 years) → maturity and total interest (RD and PPF also total deposited); PPF also shows a year-by-year table. FD follows the bank rules: simple interest below 6 months and for the months after the last full period. RD compounds every quarter. PPF assumes each deposit by 5 April. Shown only on devices set up for India (section 11).
- **Salary hike** - current salary + hike % (per month or per year) → new salary and increase, both per month and per year; or old + new salary → hike % and increase.
- **Percentage** - percentage calculator.
- **Bases** - number-base converter: decimal, hexadecimal, octal, binary.
- **BMI** - height + weight (metric + imperial) → BMI value + category.
- **Age** - DOB → years/months/days, next birthday countdown, weekday of birth.
- **Date Difference** - two dates → days/weeks/months/years; add/subtract days from a date.
- **Timezones** - time zone converter between two zones.
- **Ovulation** - last menstrual period (LMP) + average cycle length → predicted ovulation date, six-day fertile window, next period date, estimated due date (Naegele's rule). Educational estimate only with an in-screen disclaimer; explicitly not a contraception tool.
- **Tape** - a paper-roll list of past calculations.

### 4.6 Cross-Cutting Features
- Settings: theme (system / light / dark), dynamic color toggle, haptics, sound, decimal precision, a button to open the floating calculator, and an About section.
- **Floating calculator**: a small calculator window that stays on top of other apps. It runs in a foreground service with an ongoing notification that has a Stop action. The user closes it with its X button or that Stop action.
- **Launcher shortcuts** (long-press app icon): Floating calculator, plus up to three recently opened tools. All are dynamic shortcuts. The ovulation tool is never added.
- **Quick Settings tile**: tap from the notification shade to open the basic calculator.
- **Home-screen widget**: small result/quick-calc tile built with Jetpack Glance.

### 4.7 Deferred
These were planned but are not in the app. They may come in a later version:
- Search bar on the home screen to jump straight to any tool.
- A default unit system setting (metric or imperial).
- Rotate to scientific mode. The app is portrait only, and scientific mode is the Advanced tile.
- A "New calculation" static launcher shortcut.

## 5. Non-Functional Requirements

| Concern | Target |
|---|---|
| Cold start | < 600 ms on a mid-range device (Pixel 6a class) |
| Calculation latency | < 16 ms (one frame) for expressions up to 50 chars |
| Crash-free rate | ≥ 99.5%. The app has no crash reporter, so this is checked with Play Console Android vitals and pre-release testing |
| Memory footprint | < 100 MB resident |
| Install size | < 15 MB (release AAB after R8 + resource shrinking) |
| Accessibility | TalkBack labels on every key, min touch target 48dp, dynamic font scaling, contrast AA, high-contrast mode support (Android 14+) |
| Internationalisation | English + Hindi at launch; string-resource based, RTL-safe; locale-aware number formatting and date pickers |
| Privacy | No personal data collected; no network calls; no analytics; no crash reporter. If a crash reporter is ever added, it must be opt-in with a Settings toggle, off by default |
| Permissions | The app declares four, all for the floating calculator: `SYSTEM_ALERT_WINDOW` (draw the window over other apps), `FOREGROUND_SERVICE` (keep it alive in a foreground service), `FOREGROUND_SERVICE_SPECIAL_USE` (the service type Android 14+ requires) and `POST_NOTIFICATIONS` (its ongoing notification with a Stop action, asked at runtime on Android 13+ the first time the user starts the overlay from Settings). AndroidX WorkManager, which the Glance widget uses to schedule updates, adds `WAKE_LOCK`, `ACCESS_NETWORK_STATE` and `RECEIVE_BOOT_COMPLETED` to the merged manifest; the app still makes no network calls. AndroidX also adds the signature permission `com.calculator.DYNAMIC_RECEIVER_NOT_EXPORTED_PERMISSION`. No contacts, location, storage, camera or microphone. See [PRIVACY.md](PRIVACY.md#permissions). |
| Offline | Fully offline. Every feature works without a network connection. |
| Backup | Android Auto Backup is on. The rules are in `data_extraction_rules.xml`: cloud backup and device transfer include the Room database, shared preferences and DataStore. Nothing sensitive is stored |

## 6. UX & Design Principles

- Material 3 + **Material 3 Expressive** components (shape morphing, motion physics, emphasized typography) with Material You dynamic color guaranteed (min SDK = API 31).
- Large, thumb-reachable keypad. Portrait only; scientific keys come from the Advanced tile.
- Edge-to-edge with predictive back gesture support.
- Compose-driven, single activity, **type-safe Navigation Compose** (serializable route classes).
- AndroidX SplashScreen API with adaptive launcher icon.
- Numeric font: tabular figures so digits don't shift.

## 7. Architecture (High Level)

```
app/
  src/main/java/com/calculator/
    feature/            # User-facing features: screens and their ViewModels
      basic/            # Basic calculator (ui/)
      math/             # percent/ - percentage calculator
      converter/        # unit/ converter + base/ (number-base) converter
      finance/          # loan/, gst/, discount/, profit/, investment/, deposit/, salary/, tipsplit/
      health/           # bmi/, ovulation/
      datetime/         # age/, datediff/, timezone/
      history/          # calculation history sheet
      tape/             # running tape/paper-roll view
      floating/         # floating overlay calculator (foreground service)
      settings/         # settings + about sheet
    core/
      math/             # Expression parser + evaluator (pure Kotlin, no Android deps); di/
      data/             # Room DB, DAOs, DataStore (history/, converter/, settings/, tape/, db/, di/)
      domain/           # Pure Kotlin calculators (converter/, datetime/, health/, math/, finance/)
      designsystem/     # theme/ (colors, typography) + component/ (scaffold, cards, fields, date picker)
      common/           # format/ and clipboard/ helpers
      widget/           # Glance home-screen widget + Quick Settings tile
    navigation/         # App shell: CalculatorNavHost, routes, tools menu, launcher shortcuts
    CalculatorApplication.kt
    MainActivity.kt
```

- Everything ships from a single Gradle module (`:app`). `feature/` and `core/` are Kotlin packages, not Gradle modules; they can become library modules later if build times or isolation need it.
- A `feature/` package imports only `core/`, never another feature or `navigation/`. `navigation/` is the app shell that knows every feature.
- Single-activity, type-safe Navigation Compose.
- ViewModels expose `StateFlow<UiState>` consumed by Compose via `collectAsStateWithLifecycle`.

## 8. Data & APIs

- **Room DB**
  - `history` (id, expression, result, timestampUtc, type), newest 1000 rows kept.
  - `recent_unit_pair` (category, fromUnit, toUnit): the unit converter's last pair per category.
- **DataStore Preferences** (`user_settings`)
  - App settings shown in the Settings sheet: theme, dynamic-color flag, haptics, sound, precision.
  - Per-tool memory, so a tool opens where the user left it: last unit category, GST intra/inter and rate, BMI height and weight units, timezone converter zone pair (saved in one write).
  - A corrupt file is replaced with defaults; a failed read or write never crashes the app.
- **SharedPreferences** (`recent_tools`) - up to three recently opened tool ids, used only to publish launcher shortcuts. The ovulation tool is never saved here.
- **Screen saved state** - tool inputs, including the ovulation LMP date, are kept only in the screen's saved state while the app is open. They survive Android closing the app in the background, but are not written to a database or file.
- **Backup** - Auto Backup copies the database, shared preferences and DataStore (rules in `data_extraction_rules.xml`).
- **External APIs** - none. No network access is wired up.

## 9. Testing Strategy

- **Unit tests (JUnit5)** for the math engine - precedence, unary minus, division by zero, very large/small numbers, transcendental rounding, locale-specific decimal separators.
- **Unit tests** for each life-calculator's formula (deterministic golden-input/golden-output cases) and for each tool's input function (empty, partial and out-of-range input).
- **Compose UI tests (JUnit4 + Robolectric, on the JVM)** for key flows: basic calculation, scientific mode, unit conversion, and a smoke or save/restore test for the life calculators.
- **Locale**: the unit test task pins the default locale to en-US; tests for other locales set them explicitly, so results do not depend on the machine.
- Screenshot tests are not used; looks are checked on a device before a release.
- **Static analysis** in CI: ktlint, detekt, Android Lint at error severity.

## 10. Release Plan (Tentative)

> Rough order for a single developer working full-time. Treat week counts as t-shirt sizes, not commitments - revisit after M1.

| Milestone | Scope |
|---|---|
| M1 (Week 1-2) | Project skeleton, design system, basic calculator + history, CI pipeline |
| M2 (Week 3) | Scientific mode, settings, themes (dynamic color, dark/light) |
| M3 (Week 4-5) | Unit converter |
| M4 (Week 6) | Life calculators (loan, GST, BMI, age, discount, date diff) |
| M5 (Week 7) | Accessibility, i18n (en + hi), Play Console internal track |
| M6 (Week 8) | Closed beta → Production rollout |

## 11. Risks & Open Questions

- **Math precision for transcendentals** - `BigDecimal` has no native sin/cos/log. Decision: arithmetic on `BigDecimal`; transcendental ops on bounded `Double` with rounding. Revisit (BigMath / mpmath-style) if user-reported precision bugs appear.
- **Locale decimal separator** (`,` vs `.`) - must parse and render per-locale without breaking the engine. Date pickers must also be locale-aware.
- **Regional features** (GST and Deposits are India-only) - shown only when the device locale country is IN or the time zone is India's (many Indian phones run English (United States), so the locale alone is not enough). The rule lives in `navigation/Tools.kt` (`isIndia`).
- **Play policy compliance** - the "loan" calculator must use neutral language (no implication that the app is a lender) per Google's personal-loans policy.
- **Crash-free metric vs no-analytics stance** - resolved by shipping no crash reporter. The crash rate comes from Play Console Android vitals and pre-release testing. If a crash reporter is ever added, it must be opt-in with a Settings toggle, off by default.
- **Solo-dev velocity** - the 8-week plan assumes no major blockers; the Glance widget has a learning-curve tail that may push M5/M6.

---

*This document is a starting point. Items here will be refined into per-feature specs and tracked as tasks before implementation begins.*
