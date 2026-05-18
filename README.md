# Calculator

A modern, offline-first, multi-purpose calculator for Android - inspired by Mi Calculator, built with Kotlin and Jetpack Compose.

> Status: **scaffold** - core architecture in place, features being filled in. See [REQUIREMENTS.md](REQUIREMENTS.md) for the full product spec.

## Features (target v1)

- Basic and scientific calculator with editable history
- Unit converter (length, area, volume, mass, temperature, speed, time, data, pressure, energy, power)
- Currency converter (offline-cached, daily rates)
- Life calculators: Loan/EMI, GST, BMI, Age, Discount, Date difference
- Material 3 + Material 3 Expressive UI, Material You dynamic color, light/dark themes
- Fully offline for non-currency features, no analytics, no ads

## Tech stack

| Area | Choice |
|---|---|
| Language | Kotlin 2.x |
| Min SDK | API 31 (Android 12) |
| Target SDK | API 36 (Android 16) |
| UI | Jetpack Compose, Material 3 (with Expressive components) |
| Architecture | MVVM, single-activity, unidirectional data flow |
| DI | Hilt |
| Async | Coroutines + Flow |
| Persistence | Room + DataStore |
| Network | Retrofit + OkHttp + kotlinx.serialization (currency rates only) |
| Build | Gradle Kotlin DSL, Version Catalogs |
| Testing | JUnit5 (unit), AndroidX Test + Compose UI Test (instrumented), Turbine, MockK |
| CI | GitHub Actions |

## Project layout

```
app/
  src/main/java/com/calculator/
    feature/        # User-facing features (basic, converter, finance, health, datetime)
    core/
      math/         # Expression parser + evaluator (pure Kotlin, no Android deps)
      designsystem/ # Theme, colors, typography, shared Compose components
      data/         # Room, DataStore, network
      domain/       # Cross-feature use-cases
      common/       # Utilities, formatters
    navigation/     # Type-safe Navigation Compose host
    CalculatorApplication.kt
    MainActivity.kt
```

## Getting started

### Prerequisites

- **JDK 17** (or newer)
- **Android Studio** Ladybug | 2024.2.1 or newer
- **Android SDK** with platform 36 (Android 16) installed

### First-time setup

```bash
# Clone
git clone https://github.com/YOUR_USERNAME/calculator.git
cd calculator

# If the Gradle wrapper isn't checked in, generate it once:
gradle wrapper --gradle-version 8.11.1

# Build the debug APK
./gradlew :app:assembleDebug

# Install on a connected device or emulator
./gradlew :app:installDebug

# Run unit tests
./gradlew test

# Run instrumented tests (requires an emulator/device)
./gradlew connectedAndroidTest

# Static analysis
./gradlew detekt ktlintCheck lint
```

### Opening in Android Studio

1. **File → Open** and select the project root.
2. Android Studio will sync Gradle and download dependencies.
3. Select the `app` run configuration and press Run.

## Contributing

We welcome contributions of any size. See [CONTRIBUTING.md](CONTRIBUTING.md) for the workflow, coding style, and how to run the test/lint suite locally before opening a pull request.

## License

Licensed under the Apache License, Version 2.0. See [LICENSE](LICENSE) for the full text.

---

*This project is not affiliated with, endorsed by, or sponsored by Xiaomi Inc. "Mi Calculator" is referenced only as design inspiration.*
