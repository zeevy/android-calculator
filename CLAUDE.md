# CLAUDE.md

Project-level guidance for Claude Code when working in this repository. Read this before making changes.

## Project overview

An **open-source Android calculator** inspired by Mi Calculator (`com.miui.calculator` by Xiaomi Inc.). Goal: a modern, offline-first, multi-purpose calculator with basic + scientific math, unit/currency converters, and life calculators (loan/EMI, GST, BMI, age, discount, date diff). Ad-free, no analytics by default, Apache 2.0 licensed, published to GitHub.

See [REQUIREMENTS.md](REQUIREMENTS.md) for the full product specification - it is the source of truth for scope and non-functional targets.

## Tech stack (locked decisions)

| Area | Choice |
|---|---|
| Language | Kotlin 2.x (K2 compiler) |
| Min SDK | **API 31** (Android 12) - chosen so Material You dynamic color, native SplashScreen, and modern motion/shape are first-class with no compat branching |
| Target SDK | **API 36** (Android 16) |
| UI | Jetpack Compose + Material 3 with **Material 3 Expressive** components |
| Architecture | MVVM, single-activity, unidirectional data flow |
| Navigation | Navigation Compose with **type-safe routes** (serializable route classes) |
| DI | Hilt |
| Async | Kotlin Coroutines + Flow |
| Persistence | Room (history, currency rate cache, favourites) + DataStore Preferences (user settings only) |
| Network | Retrofit + OkHttp + kotlinx.serialization - **currency rates only**, no other network calls |
| Math engine | Custom evaluator: `BigDecimal` for arithmetic, bounded `Double` with rounding for transcendentals (sin/cos/log/exp). Revisit BigMath-style libs only if precision bugs appear. |
| Build | Gradle Kotlin DSL, **Version Catalogs** (`gradle/libs.versions.toml`), R8 minification + resource shrinking on release |
| Performance | Baseline Profiles via Macrobenchmark, AndroidX Startup for lightweight init |
| Widgets | Jetpack **Glance** (not legacy RemoteViews) |
| Splash | AndroidX SplashScreen API + adaptive icon |
| Testing | **JUnit5** (unit, via `android-junit5` plugin), **JUnit4 + AndroidX Test** (instrumented & Compose UI), Turbine, MockK, Robolectric where useful |
| Static analysis | ktlint + detekt + Android Lint (error severity) |
| CI | GitHub Actions |
| Distribution | Google Play (AAB), Play App Signing |

If you need to change any of the locked decisions above, raise the trade-off explicitly with the user before editing code or the requirements doc.

## Project layout

```
app/
  src/main/java/com/calculator/
    feature/            # User-facing features (each owns ui/, domain/, data/)
      basic/            # Basic + scientific calculator
      converter/        # Unit + currency
      finance/          # Loan, GST, discount
      health/           # BMI
      datetime/         # Age, date diff
    core/
      math/             # Expression parser + evaluator (pure Kotlin, no Android deps)
      data/             # Room DB, DAOs, DataStore, network (Retrofit)
      domain/           # Cross-feature use-cases (pure Kotlin)
      designsystem/     # Theme, colors, typography, shared Compose components
      common/           # Utilities, formatters, locale helpers
    navigation/         # NavHost + type-safe destination definitions
    CalculatorApplication.kt
    MainActivity.kt
```

Keep features self-contained. **A `feature/` module never imports another `feature/` module** - share via `core/`.

## Coding style and conventions

### Comments and KDoc

This is an open-source project; readability for new contributors matters more than for a private codebase. **Override the default "no comments" stance** and follow this:

- **KDoc on every public class, object, interface, and top-level function.** Lead with one short sentence describing purpose; add `@param`/`@return`/`@throws` where non-obvious.
- **KDoc on non-trivial public methods.** Skip getters/setters and one-liners whose name fully describes them.
- **Inline comments only where the WHY is non-obvious** - hidden constraints, workarounds, surprising invariants, references to specs (e.g. "Shunting-Yard handles left-associativity here").
- **Do not** describe WHAT the code does when the names already do.
- **Do not** add change-log style comments ("added for issue #42", "used by X") - those belong in PR descriptions and git history.

### Naming

- Use Kotlin idiomatic names: `PascalCase` for classes, `camelCase` for functions/properties, `SCREAMING_SNAKE_CASE` for top-level `const val`.
- Composables: `PascalCase` (e.g. `BasicCalculatorScreen`).
- ViewModels: `<Feature>ViewModel` (e.g. `BasicCalculatorViewModel`).
- UI state classes: `<Feature>UiState`. UI events: `<Feature>UiEvent`.

### Architecture rules

- **ViewModels expose `StateFlow<UiState>`**, never `LiveData`, never mutable state to the UI.
- UI is **stateless** where possible; hoist state to ViewModel.
- **Repositories return `Flow`** for observable data and `suspend fun` for one-shots.
- **Use cases are pure Kotlin** (no Android deps) and live under `core/domain/` when shared, otherwise inside the feature.
- **No `runBlocking` in production code.** Tests may use `runTest`.

### Compose specifics

- Prefer `collectAsStateWithLifecycle()` over `collectAsState()`.
- Pass primitive parameters down; avoid passing whole ViewModels into deep composables.
- Use **stable** data classes for state; mark unstable types with `@Stable`/`@Immutable` only when justified.
- Previews live next to their composable, annotated with `@PreviewLightDark` (or a custom multi-preview annotation).

### Math engine

- Arithmetic precision: use `BigDecimal` with `MathContext.DECIMAL64` unless a feature explicitly needs more or less.
- Transcendentals (sin/cos/log/exp): operate on `Double`, then round to the configured decimal precision before returning a `BigDecimal`.
- Reject division by zero with a typed error (`EvaluationResult.Error.DivisionByZero`), never throw across the API boundary.
- The engine is **pure Kotlin** - no Android imports, fully unit-testable on JVM.

## User preferences (apply everywhere)

These come from the user's global preferences and apply to every commit, file, and chat reply in this repo:

- **Never use em dash (`-`) or en dash (`-`).** Always use a regular ASCII hyphen (`-`). Applies to code comments, commit messages, doc files, PR descriptions, chat replies.
- **Never add `Co-Authored-By: Claude` (or any other Claude/Anthropic attribution)** to commit messages, PR descriptions, or any git artifact. The user maintains repos under their own name.

## Common commands

```bash
# Build debug
./gradlew :app:assembleDebug

# Build release (R8-shrunk AAB)
./gradlew :app:bundleRelease

# Install on connected device/emulator
./gradlew :app:installDebug

# Unit tests
./gradlew test

# Instrumented tests (needs running emulator/device)
./gradlew connectedAndroidTest

# Static analysis
./gradlew ktlintCheck
./gradlew detekt
./gradlew lint

# Generate Baseline Profile (when the macrobenchmark module is added)
./gradlew :baselineprofile:generateBaselineProfile

# Format code with ktlint
./gradlew ktlintFormat
```

## Things to know (gotchas)

- **GST calculator is India-specific.** Hide or expose by locale, never hardcode it as universally available.
- **Loan/EMI calculator: use neutral language.** Google Play's personal-loans policy bans wording that implies the app is a lender. Strings must read as a calculator, not a lending tool.
- **Currency rates require `INTERNET`.** It is the only network-gated feature. Everything else must work fully offline; rate-converter falls back to the last cached `currency_rate` table when offline.
- **Locale decimal separator.** Parse and render numbers per-locale (`,` in many EU locales, `.` in US/IN). The math engine accepts both via a locale-aware formatter at the UI boundary - the engine itself works on canonical `.`-separated strings.
- **Crash reporting is opt-in.** Default off, disclosed in settings. Do not wire up Crashlytics or Firebase without an explicit user toggle.
- **No analytics in v1.** Do not add Firebase Analytics, no third-party SDKs that phone home.
- **Backups.** Android Auto Backup is on; nothing sensitive is stored, but `backup_rules.xml` is authored explicitly so the policy is reviewable.

## Working with the requirements doc

[REQUIREMENTS.md](REQUIREMENTS.md) is the product spec. Treat it as load-bearing:

- When a decision changes (new library, dropped feature, version bump), **update REQUIREMENTS.md in the same change** that makes the technical change.
- Tech-stack rows in this CLAUDE.md must stay in sync with the stack table in REQUIREMENTS.md - update both together.
- When unsure about scope, defer to REQUIREMENTS.md over assumptions.

## When in doubt

- **Prefer editing existing files over creating new ones.**
- **Don't add abstractions for hypothetical futures.** A single calculator doesn't need an interface; three of them might.
- **Don't add error handling for impossible states.** Validate at boundaries (user input, network), trust internal calls.
- **Ask before** deleting branches, force-pushing, dropping DB tables, or bumping major dependencies.
