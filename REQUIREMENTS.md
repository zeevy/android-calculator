# Calculator App - High-Level Requirements

**Version:** 0.2 (Draft)
**Date:** 2026-05-18
**Inspiration:** Mi Calculator (com.miui.calculator) by Xiaomi Inc.

---

## 1. Product Vision

A modern, fully-offline, multi-purpose calculator for Android that bundles a standard calculator, scientific mode, a unit converter, and a suite of everyday "life" calculators (loan, GST, BMI, age, discount, date diff, ovulation) into a single clean, fast, ad-free experience.

## 2. Goals & Non-Goals

### Goals
- Cover the day-to-day math needs of an average user without forcing them to install separate apps.
- Work fully offline. No network calls. No permissions declared.
- Look and feel native on modern Android (Material 3 / Material You, dynamic color, light & dark themes).
- Keep the app lightweight (target installed size < 15 MB).

### Non-Goals (v1)
- Graphing / equation plotting.
- Symbolic math, calculus, matrices.
- Programmer mode (hex / bin / bitwise) - deferred to v2.
- Account sync / cloud history.
- Multi-platform (iOS, web) - Android only for now.
- Currency conversion - removed during development; live FX rates would require network + a permission, which conflicts with the "fully offline, zero permissions" goal. Deferred indefinitely.

## 3. Target Platform & Tech Stack

| Area | Choice |
|---|---|
| Language | Kotlin (latest stable, 2.x) |
| Min SDK | API 31 (Android 12) - chosen to make Material You dynamic color, native SplashScreen API, and modern motion/shape primitives first-class without compat branching |
| Target SDK | API 36 (Android 16) - latest stable |
| UI Toolkit | Jetpack Compose + Material 3 (adopting Material 3 Expressive components) |
| Architecture | MVVM + Unidirectional Data Flow, single-activity |
| DI | Hilt |
| Async | Kotlin Coroutines + Flow |
| Persistence | Room (history, recent unit pairs) + DataStore Preferences (user settings) |
| Networking | None. App is fully offline; no permissions declared. |
| Math Engine | Custom evaluator: `BigDecimal` for arithmetic/precision-sensitive ops; bounded `Double` with rounding for transcendental ops (sin/cos/log/exp). Optionally back transcendentals with a BigMath-style library if precision is insufficient. |
| Build | Gradle Kotlin DSL, Version Catalogs (libs.versions.toml), R8 minification + resource shrinking on release |
| Performance | Baseline Profiles (Macrobenchmark-generated), Startup library for lightweight init |
| Widgets | Jetpack Glance (Compose-style App Widget API) |
| Splash | AndroidX SplashScreen API + adaptive icon |
| Testing | JUnit5 (unit tests via `android-junit5` plugin), JUnit4 + AndroidX Test (instrumented & Compose UI tests), Turbine, MockK, Robolectric where useful |
| CI | GitHub Actions: build, unit tests, instrumented tests on emulator, ktlint + detekt static analysis, R8-shrunk APK size check |
| Distribution | Google Play (AAB), Play App Signing |

## 4. Functional Requirements

### 4.1 Basic Calculator (must-have)
- Operations: + - × ÷, parentheses, percentage, sign toggle, decimal.
- Live preview of the result as the user types.
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
- Toggle between basic and scientific (rotate device, or button).

### 4.3 Unit Converter (must-have)
- Categories: Length, Area, Volume, Mass / Weight, Temperature, Speed, Time, Data (bytes), Pressure, Energy, Power.
- Two-pane input (from / to), tap to swap, decimal precision configurable.
- Last used unit pair remembered per category.

### 4.4 Currency Converter (removed)
Originally specified as a must-have. Removed during development to keep
the app fully offline with zero permissions. Live FX rates require
network access, which conflicts with the "no INTERNET permission"
positioning. See section 2 (Non-Goals).

### 4.5 Life Calculators (must-have)
Each is a focused screen with its own inputs/outputs:
- **Loan / EMI** - principal, rate, tenure → EMI, total interest, amortisation table.
- **GST** (India) - amount + rate (5/12/18/28% presets) → CGST, SGST/IGST, net & gross.
- **BMI** - height + weight (metric + imperial) → BMI value + category.
- **Age** - DOB → years/months/days, next birthday countdown, weekday of birth.
- **Discount** - MRP + discount % → savings + final price; reverse calc (final + MRP → %).
- **Date Difference** - two dates → days/weeks/months/years; add/subtract days from a date.
- **Ovulation** - last menstrual period (LMP) + average cycle length → predicted ovulation date, six-day fertile window, next period date, estimated due date (Naegele's rule). Educational estimate only with an in-screen disclaimer; explicitly not a contraception tool.

### 4.6 Cross-Cutting Features
- Settings: theme (system / light / dark), dynamic color toggle, haptics, sound, decimal precision, default unit system, opt-in crash reporting.
- Search bar on home to jump straight to any tool.
- **Launcher shortcuts** (long-press app icon) for "New calculation" and top 2 tools (Android 7.1+ static + dynamic shortcuts).
- **Quick Settings tile** (stretch): tap from notification shade to open the basic calculator.
- **Home-screen widget** (stretch): small result/quick-calc tile built with Jetpack Glance.

## 5. Non-Functional Requirements

| Concern | Target |
|---|---|
| Cold start | < 600 ms on a mid-range device (Pixel 6a class), enforced via Baseline Profiles + Macrobenchmark in CI |
| Calculation latency | < 16 ms (one frame) for expressions up to 50 chars |
| Crash-free rate | ≥ 99.5% - measured only if the user opts in to crash reporting; otherwise treated as an internal pre-release target |
| Memory footprint | < 100 MB resident |
| Install size | < 15 MB (release AAB after R8 + resource shrinking) |
| Accessibility | TalkBack labels on every key, min touch target 48dp, dynamic font scaling, contrast AA, high-contrast mode support (Android 14+) |
| Internationalisation | English + Hindi at launch; string-resource based, RTL-safe; locale-aware number formatting and date pickers |
| Privacy | No PII collected; no network calls; no third-party analytics by default; any crash reporting is strictly opt-in and disclosed in settings |
| Permissions | None. Manifest declares zero permissions; no runtime permissions either. |
| Offline | Fully offline. Every feature works without a network connection. |
| Backup | Android Auto Backup enabled for settings & history; sensitive data excluded via `backup_rules.xml` (no sensitive data expected, but rules file authored for clarity) |

## 6. UX & Design Principles

- Material 3 + **Material 3 Expressive** components (shape morphing, motion physics, emphasized typography) with Material You dynamic color guaranteed (min SDK = API 31).
- Large, thumb-reachable keypad; landscape exposes scientific keys.
- Edge-to-edge with predictive back gesture support.
- Compose-driven, single activity, **type-safe Navigation Compose** (serializable route classes).
- AndroidX SplashScreen API with adaptive launcher icon.
- Numeric font: tabular figures so digits don't shift.

## 7. Architecture (High Level)

```
app/
  feature/
    basic/         Basic + scientific calculator (ui/, domain/, data/)
    converter/     Unit converter (ui/, domain/, data/)
    finance/       Loan, GST, discount (ui/, domain/, data/)
    health/        BMI, ovulation (ui/, domain/)
    datetime/      Age, date diff (ui/, domain/)
  core/
    math/          Expression parser + evaluator (BigDecimal + bounded Double)
    data/          Room DB, DAOs, DataStore
    domain/        Shared use-cases (pure Kotlin, no Android deps)
    designsystem/  Theme, colors, typography, shared Compose components
    common/        Utilities, formatters, locale helpers
```

- Single-activity, Compose Navigation, feature-modularised when complexity warrants.
- ViewModels expose `StateFlow<UiState>` consumed by Compose via `collectAsStateWithLifecycle`.

## 8. Data & APIs

- **Room DB**
  - `history` (id, expression, result, timestampUtc, type)
  - `recent_unit_pair` (category, fromUnit, toUnit)
- **DataStore Preferences** - user settings only (theme, dynamic-color flag, haptics, sound, precision, default unit system, crash-reporting opt-in).
- **External APIs** - none. The currency-rates provider was removed when the currency converter feature was deleted; no network access is wired up.

## 9. Testing Strategy

- **Unit tests (JUnit5)** for the math engine - precedence, unary minus, division by zero, very large/small numbers, transcendental rounding, locale-specific decimal separators.
- **Unit tests** for each life-calculator's formula (deterministic golden-input/golden-output cases).
- **Compose UI tests (JUnit4 + AndroidX Test or Robolectric)** for key flows: basic calculation, scientific mode, unit conversion, every life calculator.
- **Screenshot tests** (Paparazzi or Roborazzi) for the design system in light / dark / dynamic-color variants.
- **Macrobenchmark** module to measure cold-start and generate Baseline Profiles in CI.
- **Static analysis** in CI: ktlint, detekt, Android Lint at error severity.

## 10. Release Plan (Tentative)

> Rough order for a single developer working full-time. Treat week counts as t-shirt sizes, not commitments - revisit after M1.

| Milestone | Scope |
|---|---|
| M1 (Week 1-2) | Project skeleton, design system, basic calculator + history, CI pipeline |
| M2 (Week 3) | Scientific mode, settings, themes (dynamic color, dark/light) |
| M3 (Week 4-5) | Unit converter |
| M4 (Week 6) | Life calculators (loan, GST, BMI, age, discount, date diff) |
| M5 (Week 7) | Accessibility, i18n (en + hi), Baseline Profile generation, Play Console internal track |
| M6 (Week 8) | Closed beta → Production rollout |

## 11. Risks & Open Questions

- **Math precision for transcendentals** - `BigDecimal` has no native sin/cos/log. Decision: arithmetic on `BigDecimal`; transcendental ops on bounded `Double` with rounding. Revisit (BigMath / mpmath-style) if user-reported precision bugs appear.
- **Locale decimal separator** (`,` vs `.`) - must parse and render per-locale without breaking the engine. Date pickers must also be locale-aware.
- **Regional features** (GST is India-only) - hide/show based on locale or expose as an opt-in tool.
- **Play policy compliance** - the "loan" calculator must use neutral language (no implication that the app is a lender) per Google's personal-loans policy.
- **Crash-free metric vs no-analytics stance** - resolved by making crash reporting strictly opt-in; pre-launch we'll rely on internal testing instead of telemetry.
- **Solo-dev velocity** - the 8-week plan assumes no major blockers; Baseline Profile tooling and Glance widget have a learning-curve tail that may push M5/M6.

---

*This document is a starting point. Items here will be refined into per-feature specs and tracked as tasks before implementation begins.*
