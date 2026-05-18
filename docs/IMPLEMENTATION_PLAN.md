# Implementation Plan

**Status legend:** `[x]` = done · `[~]` = in progress · `[ ]` = not started
**Last reviewed:** 2026-05-18

This plan breaks the calculator app down into deliverable phases. Each phase has:

- a **scope** of what ships
- a **deliverables** checklist (source + integration items)
- a **test cases** checklist (unit, integration, UI)
- an **exit criteria** block that must be true before the phase is closed

Phases are sequential by default. Where two phases are independent (e.g. Settings vs Converter), the order can be swapped without rework.

Update this file in the same change that completes a checkbox. Do not retro-edit historical phases - if something is reopened, add a note rather than flipping a `[x]` back to `[ ]`.

---

## Phase 0 - Project Bootstrap

**Goal:** Repo, build, CI, and design system skeleton compile cleanly and produce a runnable debug APK.

### Phase 0 - Deliverables

- [x] `REQUIREMENTS.md` (product spec, source of truth for scope)
- [x] `CLAUDE.md` (assistant guidance, tech-stack table)
- [x] `README.md`, `CONTRIBUTING.md`, `LICENSE` (Apache 2.0)
- [x] `.gitignore`
- [x] Gradle KTS root: `build.gradle.kts`, `settings.gradle.kts`, `gradle.properties`
- [x] Version catalog `gradle/libs.versions.toml` (Kotlin 2.1, AGP 8.7, compose-bom)
- [x] Gradle wrapper (`gradle/wrapper/`)
- [x] App module `app/build.gradle.kts` with plugins (Compose, Hilt, KSP, ktlint, detekt, android-junit5, serialization)
- [x] `app/proguard-rules.pro`
- [x] `app/src/main/AndroidManifest.xml` (`INTERNET` perm, single activity, splash theme)
- [x] `CalculatorApplication.kt` (Hilt entry point)
- [x] `MainActivity.kt` (single activity, edge-to-edge)
- [x] Design system: `core/designsystem/theme/{Color,Theme,Type}.kt`
- [x] Navigation skeleton: `navigation/{CalculatorNavHost,Destinations}.kt` (type-safe routes)
- [x] Resources: launcher icons (adaptive), themes (light + night), colors, strings
- [x] Backup policy: `backup_rules.xml`, `data_extraction_rules.xml`
- [x] Static analysis config: `config/detekt.yml`
- [x] CI workflow `.github/workflows/ci.yml` (build + unit tests + ktlint + detekt + lint)
- [x] Initialise repo as `git` and push to GitHub ([zeevy/android-calculator](https://github.com/zeevy/android-calculator))
- [x] Open-source housekeeping: SECURITY.md, CODE_OF_CONDUCT.md, CHANGELOG.md, dependabot, issue/PR templates, CODEOWNERS, CodeQL workflow, release workflow
- [x] Branch protection on `main` (required status check, linear history, no force-push, no deletions, conversation resolution required, stale reviews dismissed)
- [x] Repo hardening (Dependabot alerts + security updates, secret scanning, push protection, squash-only merge, delete-branch-on-merge)
- [ ] First green CI run on `main` (in progress - watch on GitHub Actions)

### Phase 0 - Test cases

- [x] `./gradlew :app:assembleDebug` succeeds locally
- [x] `./gradlew test` succeeds (Evaluator tests pass - see Phase 1)
- [x] `./gradlew ktlintCheck` passes
- [x] `./gradlew detekt` passes
- [x] `./gradlew lint` passes with no `error` severity findings
- [x] App installs and launches without crash (verified on a connected Pixel 6a running Android 16 / API 36; Material You dynamic color active, keypad renders and responds)

### Phase 0 - Exit criteria

- Project builds, CI is green, and `BasicCalculatorScreen` placeholder renders on a device.

---

## Phase 1 - Math Engine + Basic Calculator

**Goal:** A user can enter and evaluate arithmetic expressions on the basic calculator screen, with live preview and parentheses support.

### Phase 1 - Deliverables

- [x] `core/math/Token.kt` (sealed token hierarchy + `Operator` enum)
- [x] `core/math/Tokenizer.kt` (lexer; accepts ASCII `* /` and typographic `× ÷`; emits `TokenizationException` on unknown chars)
- [x] `core/math/Evaluator.kt` (Shunting-Yard → RPN, `BigDecimal` with `MathContext.DECIMAL64`)
- [x] `core/math/EvaluationResult.kt` (typed `Success` / `Error.{Syntax,UnknownToken,DivisionByZero}`)
- [x] `core/math/di/MathModule.kt` (Hilt module exposing `Evaluator`)
- [x] `feature/basic/ui/BasicCalculatorUiState.kt` (`expression`, `liveResult`, `errorMessage`, `pendingRepeat`)
- [x] `feature/basic/ui/BasicCalculatorViewModel.kt` (StateFlow, `onEvent` reducer, full input-rule set)
- [x] `feature/basic/ui/BasicCalculatorScreen.kt` (Display + 4x5 rectangular keypad, `@PreviewLightDark`)
- [x] **Real-calculator input rules**: consecutive-operator collapse, leading-operator drop with `-` exception, single decimal per number segment, leading-`.` auto-zero, after-`=` chain vs fresh-start, trailing-operator auto-complete, repeat-`=` re-applies last `op+operand` for `+ - × ÷`
- [x] Result formatting: `stripTrailingZeros()` so `1.5+2.5` shows `4` not `4.0`
- [x] Unary-minus support: leading `-`, after an operator (`2*-3 = -6`, with the keypad's collapse rule the user types `2 × ( - 3 )`), before a paren (`-(2+3) = -5`), and chained (`--5 = 5` engine-level)
- [x] Percentage key (`%`) - postfix `÷100` semantics; keypad has a `%` button in the top row
- [x] Leading-zero trim (`05` → `5`, `1+05` → `1+5`, but `0.5` is preserved)
- [x] Auto-close unbalanced parens on `=` (e.g. `(1+2` evaluates as `(1+2)`)
- [x] Clear-on-error UX (digit/operator after an error message clears it - test in `Errors > error clears once the user starts typing again`)
- [x] Process-death restoration via `SavedStateHandle` + `launchMode="singleTask"`
- [ ] Locale-aware grouping/decimal separator at the UI boundary (engine stays canonical)
- [ ] Haptic feedback hook on key press (no-op until Phase 4 settings wire it up)

### Phase 1 - Unit tests (JUnit5, `app/src/test/`)

#### Evaluator tests (`EvaluatorTest`)

- [x] Precedence: `2+3×4 = 14`
- [x] Parentheses: `(2+3)×4 = 20`
- [x] Decimal arithmetic: `1.5+2.25 = 3.75`
- [x] Division produces `BigDecimal` at DECIMAL64 precision (`100÷3`)
- [x] Leading unary minus over a digit: `-5+3 = -2`
- [x] Division by zero → `Error.DivisionByZero`
- [x] Mismatched parens → `Error.Syntax`
- [x] Dangling operator → `Error.Syntax`
- [x] Empty expression → `Error.Syntax`
- [x] Unknown character → `Error.UnknownToken`
- [x] Whitespace is ignored
- [x] ASCII (`*` `/`) and typographic (`×` `÷`) operators are equivalent
- [x] Very large products are exact within DECIMAL64 (`99999999×99999999 = 9999999800000001`)
- [x] Very small products preserve significant digits (`1e-7 × 1e-7 = 1e-14`)
- [x] Chained division is left-associative (`24÷4÷2 = 3`)
- [x] Negative results render correctly (`2-10 = -8`)
- [x] Zero accepted as an operand (`0+0 = 0`, `0×5 = 0`)
- [x] Multiple decimal points in one number rejected → `Error.UnknownToken`
- [x] Inline unary minus: `2*-3 = -6`, `5+-2 = 3`
- [x] Chained unary minus: `--5 = 5`
- [x] Unary minus before parens: `-(2+3) = -5`, `10+-(2+3) = 5`
- [x] Percentage standalone: `5% = 0.05`
- [x] Percentage with `+`: `100+10% = 100.1` (postfix-divide semantics, documented)
- [x] Percentage with `×`: `100×10% = 10` (matches iOS for this case)
- [x] Percentage inside parens: `(2+3)% = 0.05`
- [ ] Locale formatting: `1234.5` rendered as `1,234.5` in en-IN, `1.234,5` in de-DE (UI layer)

#### ViewModel tests (`BasicCalculatorViewModelTest`)

##### Operator collapse

- [x] Consecutive plus operators collapse to one (`1+++5` → `1+5`)
- [x] Swap retains the last operator pressed (`1+5` → `1+5×` → `1+5÷` → `1+5-`)
- [x] Mixed operator run collapses to the last one (`9-×÷+3` → `9+3`)
- [x] Swap works for all four operator pairs (`+`, `-`, `×`, `÷`)
- [x] Leading `+`, `×`, `÷` are dropped
- [x] Leading `-` is allowed for negation

##### Decimal handling

- [x] Only one decimal per number segment (`1.2.3` → `1.23`)
- [x] Each number segment gets its own decimal (`1.5+2.5 = 4`)
- [x] Leading decimal auto-prefixes zero (`.5` → `0.5`)
- [x] Decimal after operator auto-prefixes zero (`1+.5` → `1+0.5`)
- [x] Decimal after open paren auto-prefixes zero

##### Equals (basic)

- [x] Equals replaces expression with canonical result
- [x] Equals on blank expression is a no-op
- [x] Equals on a single number leaves it unchanged
- [x] Digit after equals starts a fresh expression
- [x] Decimal after equals starts a fresh `0.` number
- [x] Operator after equals chains on the result

##### Equals (repeat)

- [x] Repeat-= replays trailing operator for `+` (`1+5=` → 6, 11, 16)
- [x] Repeat-= replays for `-` (`10-3=` → 7, 4, 1)
- [x] Repeat-= replays for `×` (`2×3=` → 6, 18, 54)
- [x] Repeat-= replays for `÷` (`80÷2=` → 40, 20, 10)
- [x] Trailing operator at = auto-completes for `+` (`1+=` → 2, 3, 4)
- [x] Trailing operator at = auto-completes for `×` (`2×=` → 4, 8, 16, doubling)
- [x] Trailing operator after a chain uses the operand just typed (`10-3×=` → `10-3×3 = 1`)
- [x] Typing any other event breaks the repeat chain
- [x] Backspace also breaks the repeat chain

##### Backspace

- [x] Backspace on empty expression is a no-op
- [x] Backspace removes the last character
- [x] Backspace updates the live preview (null on incomplete, value on complete)
- [x] Backspace clears the error message

##### Clear

- [x] Clear resets every field to defaults

##### Live preview

- [x] Preview updates as the user types a complete expression
- [x] Preview is null for an incomplete expression (trailing operator)
- [x] Preview reflects a bare number
- [x] Preview is null when expression is empty

##### Errors

- [x] Division by zero surfaces a typed message
- [x] Mismatched parens surface a syntax message
- [x] Error clears once the user starts typing again

##### Parentheses

- [x] Parens compose normally (`(2+3)×4 = 20`)
- [x] Open paren followed by an operator is preserved (engine-level error path)

##### Leading-zero trim

- [x] Digit on a lone zero replaces it (`0` → `5` not `05`)
- [x] Lone zero stays until something replaces it
- [x] `0` followed by `.` is kept (`0.5` works)
- [x] Zero after an operator is trimmed by the next digit (`1+0` → `1+5`)
- [x] Zero after operator followed by `.` is kept (`1+0.5`)

##### Inline unary minus (keypad path)

- [x] Unary minus before paren evaluates correctly (`-(2+3) = -5`)
- [x] Consecutive operator-then-minus collapses to single minus (keypad UX)
- [x] Unary minus into open paren applies negation to the group (`2×(-3) = -6`)

##### Percent

- [x] Bare `N%` equals `N/100` (`50% = 0.5`)
- [x] Percent after `+` behaves as postfix divide (`100+10% = 100.1`)
- [x] Percent after `×` matches iOS arithmetic (`100×10% = 10`)

##### Auto-close parens

- [x] Unclosed paren is closed on `=` (`(1+2 = 3`)
- [x] Two unclosed parens are closed on `=` (`((5+1 = 6`)
- [x] Balanced parens are unchanged (`(1+2)×4 = 12`)

##### Process-death restoration

- [x] Expression survives process death via `SavedStateHandle`
- [x] Error message survives restoration
- [x] Repeat-equals chain continues after restoration
- [x] Cleared state is persisted across restoration

### Phase 1 - Compose UI tests (JUnit4, `app/src/androidTest/`)

- [ ] Tapping `1`, `+`, `2`, `=` shows `3` as result
- [ ] Error state shows red preview text and clears on next digit
- [ ] Long-press on display copies the current result to clipboard
- [ ] Keypad keys all have `48dp` minimum touch target (Espresso bounds check)
- [ ] TalkBack content descriptions present on every key (`onAllNodes(hasContentDescription())` count = key count)

### Phase 1 - Exit criteria

- All math-engine unit tests pass, basic calculator demo records a 30-second video showing a calculation, history is **not** required yet (Phase 3).

---

## Phase 2 - Scientific Calculator

**Goal:** Toggle the basic keypad into scientific mode with trig, logs, powers, factorial, constants, memory keys.

### Phase 2 - Deliverables

- [x] Added `Token.Function` plus `FunctionId` enum for `sin`, `cos`, `tan`, `asin`, `acos`, `atan`, `log`, `ln`, `sqrt`, `cbrt`
- [x] Right-associative `^` operator (precedence 3)
- [x] Angle mode (`DEG` / `RAD`) on the engine via `Evaluator(angleMode = …)` constructor
- [x] Constants `π` (and keyword `pi`) and `e` parsed as named numbers
- [x] Memory: `MC`, `MR`, `M+`, `M-` (state on ViewModel; persisted across process death)
- [x] Scientific keypad layout: in-screen `Sci` toggle that surfaces 4 extra rows above the basic keypad
- [x] DEG / RAD chip in the header (visible only in scientific mode)
- [x] `M` chip in the header when memory holds a non-zero value (tap to recall)
- [x] Transcendentals route through `Double` then round at 14 sig figs so `sin(30°) = 0.5` exactly (not `0.4999…`)
- [x] `EvaluationResult.Error.Domain` for `log(-1)`, `sqrt(-1)`, NaN/infinite power
- [x] Factorial key (`x!`) - postfix unary on the engine, rejects negative / non-integer / n > 1000
- [x] Sign-flip (`+/-`) key toggles the trailing operand's leading `-`
- [ ] Landscape auto-show of scientific keypad - **superseded** (manifest locks orientation to portrait)

### Phase 2 - Unit tests

#### Engine (`ScientificEvaluatorTest`)

- [x] `sin(0) = 0`, `sin(π/2) = 1` (within 1e-10)
- [x] `cos(0)`, `cos(π) = -1`
- [x] `tan(π/4) = 1`
- [x] Angle mode `DEG`: `sin(30) = 0.5`, `cos(60) = 0.5`
- [x] Inverse trig round-trip: `asin(sin(0.3)) ≈ 0.3`
- [x] `log(100) = 2`, `ln(e) = 1`
- [x] `sqrt(2)^2 ≈ 2` within DECIMAL64 precision
- [x] `cbrt(27) = 3`
- [x] `2^3^2 = 512` (right-associative)
- [x] `10^3 = 1000`
- [x] Fractional exponent: `8^(1/3) ≈ 2`
- [x] `log(-1)` → `Error.Domain`
- [x] `sqrt(-1)` → `Error.Domain`
- [x] `π` and `e` match `Math.PI` / `Math.E`
- [x] `pi` keyword resolves to `π`
- [x] Composed expression: `sin(30°) + log(100) × π` matches reference
- [x] Unknown identifier rejected as `Error.UnknownToken`
- [x] Factorial cases: `5! = 120`, `0! = 1`, `1! = 1`, `5.0! = 120`, `(2+3)! = 120`, `5! + 3 = 123`, `(-3)!`/`2.5!` → Domain, `!5` → UnknownToken

#### ViewModel (`BasicCalculatorViewModelTest`)

##### Scientific mode

- [x] Toggle scientific flips the flag
- [x] Toggle angle mode cycles RAD ↔ DEG
- [x] `sin(30°)` typed via keypad evaluates to `0.5`
- [x] Auto-close brings `sin(30` home without explicit `)`
- [x] `π` key recalls the constant

##### Memory

- [x] `M+` stores the current result
- [x] `M-` subtracts from memory
- [x] `MR` appends the stored value to the expression
- [x] `MR` after an operator appends without inserting an extra `×`
- [x] `MR` after a number multiplies through
- [x] `MC` zeroes the stored value
- [x] Clear preserves memory and angle mode

### Phase 2 - Compose UI tests

- [ ] Rotating to landscape shows the scientific keypad (deferred along with landscape auto-show)
- [ ] Toggle chip in portrait swaps basic ↔ scientific keypads (verified manually, no instrumented test yet)
- [ ] DEG/RAD chip visible and tappable in scientific mode (verified manually)
- [ ] Memory chip (`M`) appears when memory is non-zero (verified manually)

### Phase 2 - Exit criteria

- A user can compute `sin(30°) + log(100) × π` and get a sensible answer; the Sci toggle exposes scientific keys (landscape auto-show deferred). **Met**: verified on Pixel 6a, 125 unit tests pass.

---

## Phase 3 - Persistence: History + Settings

**Goal:** Calculations persist across launches; users can reuse and delete history. Settings store via DataStore.

### Phase 3 - Deliverables

- [ ] Room DB `core/data/db/CalculatorDatabase.kt`
- [ ] Entity `HistoryEntry(id, expression, result, timestampUtc, type)`
- [ ] DAO `HistoryDao` (`observeAll`, `insert`, `delete`, `clearAll`)
- [ ] `HistoryRepository` (Flow-based interface)
- [ ] Hilt `DataModule` providing `Database`, DAOs, Repository
- [ ] DataStore `SettingsRepository` (theme, dynamicColor, haptics, sound, precision, defaultCurrency, defaultUnitSystem, crashOptIn)
- [ ] History sheet/screen: scroll, swipe-to-delete, tap-to-reuse, "Clear all" confirm
- [ ] Long-press a history row → copy to clipboard
- [ ] Wire ViewModel: on `=`, insert into history

### Phase 3 - Unit tests

- [ ] `HistoryDaoTest` (Robolectric in-memory Room): insert + observe emits the row
- [ ] `HistoryDaoTest`: delete by id removes only that row
- [ ] `HistoryDaoTest`: `clearAll` empties the table
- [ ] `HistoryRepositoryTest`: emits a new value every time a row is inserted (Turbine)
- [ ] `SettingsRepositoryTest`: writes survive a process recreation (test-scope DataStore)
- [ ] `BasicCalculatorViewModelTest`: tapping `=` writes to history (verify with MockK)

### Phase 3 - Compose UI tests

- [ ] History sheet shows last 5 inserts in reverse chronological order
- [ ] Swipe-left on a row deletes it (toast/snackbar with Undo)
- [ ] Tap a row restores its expression into the active calculator
- [ ] "Clear all" prompts a confirmation dialog before deletion

### Phase 3 - Exit criteria

- History survives app restart, settings survive app restart, no `runBlocking` in production code.

---

## Phase 4 - Settings UI + Themes

**Goal:** A Settings screen lets the user choose theme, dynamic color, haptics, decimal precision, default currency/unit system, and crash-reporting opt-in.

### Phase 4 - Deliverables

- [ ] `feature/settings/ui/SettingsScreen.kt`
- [ ] `feature/settings/ui/SettingsViewModel.kt`
- [ ] Theme system reads from `SettingsRepository`: `system` / `light` / `dark` + dynamicColor toggle
- [ ] Haptic engine wraps `HapticFeedback` and reads `settings.haptics`
- [ ] Decimal precision (2..10) feeds `Evaluator` `MathContext`
- [ ] About row: version, license, GitHub link
- [ ] Crash-reporting toggle is **off by default** with disclosure text

### Phase 4 - Unit tests

- [ ] `SettingsViewModelTest`: changing theme emits a new `UiState` with the new theme
- [ ] `SettingsViewModelTest`: dynamic-color toggle persists via `SettingsRepository`
- [ ] Precision change rebuilds the Evaluator with new MathContext

### Phase 4 - Compose UI tests

- [ ] Theme picker (`system`/`light`/`dark`) switches palette live
- [ ] Dynamic-color toggle pulls wallpaper colors on Android 12+
- [ ] Disabling haptics stops vibration on key press (verify via mocked `HapticFeedback`)
- [ ] Crash-reporting toggle defaults to OFF; disclosure text is visible

### Phase 4 - Exit criteria

- Settings persist, theming responds without restart, accessibility traversal order is logical.

---

## Phase 5 - Unit Converter

**Goal:** Length, Area, Volume, Mass, Temperature, Speed, Time, Data, Pressure, Energy, Power conversions with persistent last-used pair per category.

### Phase 5 - Deliverables

- [ ] `core/domain/converter/UnitCategory.kt`, `Unit.kt` (pure Kotlin, no Android)
- [ ] `ConversionTable` per category (canonical-unit ratios; Temperature uses formulas)
- [ ] `feature/converter/unit/UnitConverterScreen.kt` (category tabs, two-pane from/to, swap button, recents)
- [ ] `feature/converter/unit/UnitConverterViewModel.kt`
- [ ] Room entity `RecentUnitPair(category, fromUnit, toUnit)` + DAO
- [ ] Precision honors `settings.precision`

### Phase 5 - Unit tests

- [ ] Length: `1 km = 1000 m`, `1 mi ≈ 1609.344 m`
- [ ] Mass: `1 kg = 2.20462 lb` (within 1e-5)
- [ ] Temperature: `0 °C = 32 °F = 273.15 K`, `100 °C = 212 °F`
- [ ] Volume: `1 L = 1000 mL`, `1 US gal = 3.78541 L`
- [ ] Data: `1 GB = 1024 MB` (binary) - document choice (binary vs decimal) per category
- [ ] Round-trip: converting `x` from A→B→A returns `x` within precision
- [ ] Swap from/to inverts the result
- [ ] Last-used pair persists per category

### Phase 5 - Compose UI tests

- [ ] Tapping a category tab shows that category's units
- [ ] Typing in the `from` field updates the `to` field live
- [ ] Swap button exchanges values and units
- [ ] Empty input shows `0` not an error

### Phase 5 - Exit criteria

- All 11 categories functional offline, recents persist, formatting is locale-aware.

---

## Phase 6 - Currency Converter (Online + Cached)

**Goal:** Daily rates for ~150 fiat currencies, with last-cached fallback when offline, multi-currency view, favourites pinning.

### Phase 6 - Deliverables

- [ ] `core/data/network/RatesApi.kt` (Retrofit + kotlinx.serialization, e.g. open.er-api.com)
- [ ] `core/data/network/RatesDto.kt` + mapper to domain `Rates`
- [ ] `core/data/db` entity `CurrencyRate(code, rateVsBase, baseCode, fetchedAtUtc)` + DAO
- [ ] `RatesRepository` interface; `DefaultRatesRepository` impl chooses API → cache fallback
- [ ] `FakeRatesApi` for tests (in `app/src/test/`)
- [ ] `feature/converter/currency/CurrencyConverterScreen.kt` (amount field, base, target, multi-row view, refresh, last-updated timestamp)
- [ ] Favourites pinning: `FavoriteCurrency(code, position)` entity + DAO
- [ ] Manual `Refresh` action; stale-rate banner if cache > 24h

### Phase 6 - Unit tests

- [ ] `RatesRepositoryTest` with `FakeRatesApi`: fresh fetch updates cache
- [ ] Offline path: API throws → repository returns cached rates
- [ ] Mapping: API response with `rates: {EUR: 0.91, INR: 83.2}` produces a `Rates` with base `USD`
- [ ] Conversion math: `100 USD × 83.2 = 8320 INR`
- [ ] Favourites DAO: insert, reorder by position, delete

### Phase 6 - Compose UI tests

- [ ] First launch with no cache shows a loading spinner, then the rates
- [ ] Airplane mode → cached rates render with a "stale" badge if older than 24h
- [ ] Tap a currency code → picker dialog → selection updates the row
- [ ] Pinned favourites appear above non-favourites and persist across launches
- [ ] Manual refresh updates the `last updated` timestamp

### Phase 6 - Integration tests

- [ ] One real network call (CI-skipped, `@DisabledIfEnvironmentVariable` for offline CI) to confirm API contract

### Phase 6 - Exit criteria

- Airplane-mode test passes (cached rates render), no network call on cold launch if cache is fresh (<24h).

---

## Phase 7 - Life Calculators

**Goal:** Seven focused calculators: Loan/EMI, GST (India), BMI, Age, Discount, Date Difference, Ovulation.

### Phase 7.1 - Loan / EMI deliverables

- [ ] `feature/finance/loan/LoanScreen.kt` + ViewModel
- [ ] `core/domain/finance/EmiCalculator.kt`: `EMI = P × r × (1+r)^n / ((1+r)^n − 1)`
- [ ] Amortisation table (month-by-month principal/interest/balance)
- [ ] Neutral copy: never "you qualify for", "apply for a loan", etc.

### Phase 7.2 - GST (India) deliverables

- [ ] `feature/finance/gst/GstScreen.kt` + ViewModel
- [ ] Presets: 5/12/18/28%; custom rate also allowed
- [ ] Output: CGST, SGST, IGST (intra- vs inter-state toggle), net, gross
- [ ] Visible only in `IN` locale (or via Settings opt-in)

### Phase 7.3 - BMI deliverables

- [ ] `feature/health/bmi/BmiScreen.kt` + ViewModel
- [ ] Metric (cm/kg) + Imperial (ft+in/lb) units, persists last choice
- [ ] Category: Underweight / Normal / Overweight / Obese (WHO ranges)

### Phase 7.4 - Age deliverables

- [ ] `feature/datetime/age/AgeScreen.kt` + ViewModel
- [ ] DOB picker (locale-aware), output years/months/days, next birthday countdown, weekday of birth

### Phase 7.5 - Discount deliverables

- [ ] `feature/finance/discount/DiscountScreen.kt` + ViewModel
- [ ] Forward: MRP + % → savings + final
- [ ] Reverse: MRP + final → %

### Phase 7.6 - Date Difference deliverables

- [ ] `feature/datetime/datediff/DateDiffScreen.kt` + ViewModel
- [ ] Mode 1: two dates → days/weeks/months/years
- [ ] Mode 2: date + offset → resulting date

### Phase 7.7 - Ovulation deliverables

- [ ] `feature/health/ovulation/OvulationScreen.kt` + ViewModel
- [ ] `core/domain/health/OvulationCalculator.kt` (pure Kotlin)
- [ ] Inputs: last menstrual period (LMP) date + average cycle length (default 28, range 21-35)
- [ ] Outputs:
    - Next period start date (LMP + cycle length)
    - Predicted ovulation date (next period - 14)
    - Fertile window (ovulation - 5 to ovulation + 1, six-day span)
    - Estimated due date if conception occurs this cycle (LMP + 280 days, Naegele's rule)
- [ ] **Educational copy only.** Surface a one-line disclaimer that estimates are statistical and not a substitute for medical advice; not a contraception tool.
- [ ] No data leaves the device; LMP is never persisted to history without an explicit "save" action (Phase 3 history layer).

### Phase 7 - Unit tests (one suite per calculator)

- [ ] **EMI:** `P=100000, r=10%/yr, n=12` → EMI ≈ `8791.59`, sum of payments ≈ `105499.1`
- [ ] **EMI:** zero-interest loan (`r=0`) → `EMI = P/n`
- [ ] **EMI:** amortisation: final balance = 0 (within ₹1 rounding)
- [ ] **GST:** `1000 @ 18%` → CGST `90`, SGST `90`, gross `1180`
- [ ] **GST:** inter-state `1000 @ 18%` → IGST `180`, no CGST/SGST
- [ ] **BMI:** `70kg, 1.70m` → `24.22` → `Normal`
- [ ] **BMI:** `170lb, 5'10"` matches metric equivalent within 0.1
- [ ] **Age:** DOB `1990-01-15`, today `2026-05-18` → `36y 4m 3d`
- [ ] **Age:** leap-year DOB Feb 29 → correct in non-leap-year today
- [ ] **Discount:** MRP `2000`, 20% off → final `1600`, savings `400`
- [ ] **Discount reverse:** MRP `2000`, final `1500` → `25%`
- [ ] **DateDiff:** `2024-02-29` to `2025-02-28` → `0y 11m 30d` (or document spec)
- [ ] **DateDiff:** add `90d` to `2026-01-01` → `2026-04-01`
- [ ] **Ovulation:** LMP `2026-05-01`, cycle `28` → ovulation `2026-05-15`, fertile `2026-05-10`..`2026-05-16`, next period `2026-05-29`, due date `2027-02-04`
- [ ] **Ovulation:** non-default cycle length `35` → ovulation = LMP + 21 (next period - 14), fertile window shifts accordingly
- [ ] **Ovulation:** rejects cycle length outside 21-35 with a typed error

### Phase 7 - Compose UI tests

- [ ] Each calculator screen has at least one test that types inputs and asserts the displayed output
- [ ] GST screen is **hidden** in `Locale.US`, visible in `Locale("en", "IN")`
- [ ] Loan screen wording does not contain any of: `apply`, `qualify`, `lender`, `borrow` (lint-style assertion)

### Phase 7 - Exit criteria

- All six calculators ship with unit + UI tests, GST locale-gating works, loan copy passes the wording check.

---

## Phase 8 - Accessibility, i18n, Launcher Shortcuts

**Goal:** TalkBack-friendly, English + Hindi strings, locale-aware numbers/dates, launcher shortcuts and home search.

### Phase 8 - Deliverables

- [ ] Content descriptions on every key (basic + scientific) and every actionable icon
- [ ] Minimum touch target `48dp` enforced via a custom modifier or design-system button
- [ ] Locale-aware `NumberFormat` at all UI display boundaries (engine stays canonical)
- [ ] String resources moved to `values/strings.xml` (English) + `values-hi/strings.xml` (Hindi)
- [ ] RTL-safe layouts (`start`/`end` instead of `left`/`right`)
- [ ] Home screen: search bar that jumps to any tool by name
- [ ] Static launcher shortcuts: "New calculation", top 2 tools (`shortcuts.xml`)
- [ ] Dynamic shortcuts updated to recent tools
- [ ] High-contrast mode honored on Android 14+

### Phase 8 - Unit tests

- [ ] Number formatter: `1234567.89` → `1,234,567.89` (en-US), `12,34,567.89` (en-IN), `1.234.567,89` (de-DE)
- [ ] Date formatter: `2026-05-18` rendered per locale
- [ ] Search index returns "Loan" for query `emi`, `loan`, `कर्ज`

### Phase 8 - Compose UI / instrumented tests

- [ ] TalkBack traversal hits every key once, in row-major order
- [ ] Every interactive node reports a content description (`hasContentDescription()`)
- [ ] No touch target smaller than 48dp (Espresso `Visibility.VISIBLE` + bounds)
- [ ] App switches to Hindi when the system locale is `hi`
- [ ] Launcher shortcut "New calculation" deep-links into basic calculator

### Phase 8 - Exit criteria

- AndroidX `accessibility-test-framework` reports no violations on basic, scientific, settings, history, and each life calculator screen.

---

## Phase 9 - Performance: Baseline Profiles + Macrobenchmark

**Goal:** Cold-start under 600ms on a Pixel 6a-class device; profile generated in CI.

### Phase 9 - Deliverables

- [ ] New module `:baselineprofile` (Macrobenchmark)
- [ ] Cold-start, warm-start, frame-timing benchmarks for basic calculator + history
- [ ] `androidx.startup` initialisers for any eager work
- [ ] Baseline profile committed at `app/src/main/baseline-prof.txt`
- [ ] CI step: regenerate profile on `main` and fail PR if cold start regresses > 10%

### Phase 9 - Test cases

- [ ] Macrobenchmark cold-start: P50 < 600 ms on the chosen device
- [ ] Macrobenchmark warm-start: P50 < 200 ms
- [ ] Frame timing for keypad scroll: no janky frames over 16 ms at P95
- [ ] CI artifact contains a fresh `baseline-prof.txt` on every `main` build

### Phase 9 - Exit criteria

- Profile checked in, CI gates regressions, app size still under 15 MB after R8.

---

## Phase 10 - Widgets + Quick Settings Tile (Stretch)

**Goal:** Home-screen Glance widget for a quick calc; QS tile to open basic calculator.

### Phase 10 - Deliverables

- [ ] `core/widget/QuickCalcWidget.kt` (Glance `GlanceAppWidget`)
- [ ] Widget UI: result display + 0-9, +, -, =
- [ ] `QuickCalculatorTileService` (QS tile) opens the basic calculator
- [ ] Manifest entries + resource configs

### Phase 10 - Test cases

- [ ] Widget renders on home screen in light, dark, and dynamic-color
- [ ] Tapping `=` in the widget computes and shows the result
- [ ] QS tile activation launches `MainActivity` with the basic calculator destination
- [ ] Widget survives configuration change (rotate, theme switch)

### Phase 10 - Exit criteria

- Widget passes manual QA on at least one launcher (Pixel default); QS tile addable from the shade.

---

## Phase 11 - Release: Internal → Beta → Production

**Goal:** Ship to Google Play with closed beta, then promote to production.

### Phase 11 - Deliverables

- [ ] Play App Signing enrolled, release keystore secured
- [ ] Versioning strategy: SemVer for `versionName`, monotonic `versionCode`
- [ ] Release `bundleRelease` AAB under 15 MB (R8 + resource shrinking verified)
- [ ] Privacy policy hosted (covers `INTERNET` permission, opt-in crash reporting)
- [ ] Play Console listing: title, short + long descriptions, screenshots (phone + tablet + foldable), feature graphic
- [ ] Data Safety form: declare no PII collected; currency API is anonymous
- [ ] Internal track release → 10-tester closed beta → production rollout 10% → 100%
- [ ] GitHub release with signed APK attached for sideload users

### Phase 11 - Test cases

- [ ] Release AAB installs and launches from a Play Internal Testing link
- [ ] All features work on real-device matrix: phone (API 31, 34, 36), tablet, foldable, RTL locale
- [ ] Pre-launch report from Play Console has zero `error` crashes
- [ ] No third-party SDK appears in the AAB's dependency report beyond what is listed in REQUIREMENTS.md
- [ ] App passes Play's pre-launch policy scan (loan-copy check, data-safety form match)

### Phase 11 - Exit criteria

- v1.0.0 live on Production track, GitHub `v1.0.0` tag + release published.

---

## Cross-phase running checklists

These run continuously, not as phase gates:

- [ ] Every PR keeps `./gradlew ktlintCheck detekt lint test` green
- [ ] Every PR that changes the stack updates **both** `CLAUDE.md` and `REQUIREMENTS.md`
- [ ] No `runBlocking` in production sources (detekt rule enforces)
- [ ] No `Co-Authored-By: Claude` trailers on commits
- [ ] No em dashes / en dashes - ASCII hyphens only
- [ ] No `Crashlytics`, `Firebase Analytics`, or third-party telemetry SDK added without an explicit user toggle
