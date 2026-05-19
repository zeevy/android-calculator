# Implementation Plan

**Status legend:** `[x]` = done · `[~]` = in progress · `[ ]` = not started
**Last reviewed:** 2026-05-20

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
- [x] First green CI run on `main` (CI workflow at `.github/workflows/ci.yml` runs build + unit tests + ktlint + detekt + lint)

### Phase 0 - Test cases

- [x] `./gradlew :app:assembleDebug` succeeds locally
- [x] `./gradlew test` succeeds (Evaluator tests pass - see Phase 1)
- [x] `./gradlew ktlintCheck` passes
- [x] `./gradlew detekt` passes
- [x] `./gradlew lint` passes with no `error` severity findings
- [x] App installs and launches without crash (verified on a connected Pixel 6a running Android 16 / API 36; Material You dynamic color active, keypad renders and responds)

### Phase 0 - Exit criteria

- Project builds, CI is green, and `BasicCalculatorScreen` placeholder renders on a device. - **Met**: `assembleDebug` succeeds, CI workflow exists and runs, basic calculator verified on Pixel 6a.

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
- [ ] Locale-aware grouping/decimal separator at the UI boundary (engine stays canonical) - **Deferred:** `NumberFormatter` exists with locale support but `BasicCalculatorScreen` still renders results via fixed-decimal display; adoption at the UI boundary is a Phase 8 follow-up
- [x] Haptic feedback hook on key press - via `LocalHapticsEnabled` CompositionLocal + `HapticFeedback.performHapticFeedback` in `BasicCalculatorScreen`, gated on `userSettings.haptics`

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
- [x] Locale formatting: `1234.5` rendered as `1,234.5` in en-IN, `1.234,5` in de-DE - via `NumberFormatterTest` (en-US/en-IN/de-DE assertions in `app/src/test/java/com/calculator/core/common/format/NumberFormatterTest.kt`)

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

**Deferred: no instrumented (`androidTest`) module yet - these tests require Compose UI test rules and aren't part of the JVM `:app:test` task.**

- [ ] Tapping `1`, `+`, `2`, `=` shows `3` as result
- [ ] Error state shows red preview text and clears on next digit
- [ ] Long-press on display copies the current result to clipboard
- [ ] Keypad keys all have `48dp` minimum touch target (Espresso bounds check)
- [ ] TalkBack content descriptions present on every key (`onAllNodes(hasContentDescription())` count = key count)

### Phase 1 - Exit criteria

- All math-engine unit tests pass, basic calculator demo records a 30-second video showing a calculation, history is **not** required yet (Phase 3). - **Met**: `EvaluatorTest` (32) + `BasicCalculatorViewModelTest` (extensive nested suites) all pass; basic calculator runs on device.

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

**Deferred: no instrumented (`androidTest`) module yet - these tests require Compose UI test rules and aren't part of the JVM `:app:test` task.**

- [ ] Rotating to landscape shows the scientific keypad (deferred along with landscape auto-show)
- [ ] Toggle chip in portrait swaps basic ↔ scientific keypads (verified manually, no instrumented test yet)
- [ ] DEG/RAD chip visible and tappable in scientific mode (verified manually)
- [ ] Memory chip (`M`) appears when memory is non-zero (verified manually)

### Phase 2 - Exit criteria

- A user can compute `sin(30°) + log(100) × π` and get a sensible answer; the Sci toggle exposes scientific keys (landscape auto-show deferred). **Met**: verified on Pixel 6a, 259 unit tests pass across the project.

---

## Phase 3 - Persistence: History + Settings

**Goal:** Calculations persist across launches; users can reuse and delete history. Settings store via DataStore.

### Phase 3 - Deliverables

- [x] Room DB `core/data/db/CalculatorDatabase.kt`
- [x] Entity `HistoryEntity(id, expression, result, timestampUtc, type)` + domain mirror `HistoryEntry`
- [x] DAO `HistoryDao` (`observeAll`, `insert`, `deleteById`, `clearAll`)
- [x] `HistoryRepository` (Flow-based interface, `RoomHistoryRepository` impl)
- [x] Hilt `DataModule` + `RepositoryModule` providing `Database`, DAOs, Repository, DataStore
- [x] DataStore `SettingsRepository` (theme, dynamicColor, haptics, sound, precision, crashOptIn) - currency/unit-system defer to Phase 5/6
- [x] History sheet/screen: scroll list, per-row delete icon, tap-to-reuse, "Clear all" confirm dialog
- [ ] Swipe-to-delete (deferred; per-row trash icon ships first)
- [ ] Long-press a history row → copy to clipboard (deferred)
- [x] Wire ViewModel: on a *fresh* `=`, insert into history (repeat-equals replays do not spam)

### Phase 3 - Unit tests

- [x] `HistoryDaoTest` (Robolectric in-memory Room): insert + observe emits the row
- [x] `HistoryDaoTest`: delete by id removes only that row
- [x] `HistoryDaoTest`: `clearAll` empties the table; also `observeAll` orders newest-first
- [x] `HistoryRepositoryTest`: emits a new value per insert (Turbine); type round-trip; `clearAll`
- [x] `SettingsRepositoryTest`: defaults, write-persists-across-instances, precision clamp
- [x] `BasicCalculatorHistoryTest`: `=` records (fresh only, not on replays/errors/blank); scientific type tagged

### Phase 3 - Compose UI tests

**Deferred: no instrumented (`androidTest`) module yet - these tests require Compose UI test rules and aren't part of the JVM `:app:test` task.**

- [ ] History sheet shows last 5 inserts in reverse chronological order
- [ ] Swipe-left on a row deletes it (toast/snackbar with Undo)
- [ ] Tap a row restores its expression into the active calculator
- [ ] "Clear all" prompts a confirmation dialog before deletion

### Phase 3 - Exit criteria

- History survives app restart, settings survive app restart, no `runBlocking` in production code. - **Met**: `HistoryDaoTest`, `HistoryRepositoryTest`, `SettingsRepositoryTest`, and `BasicCalculatorHistoryTest` all pass; detekt rule forbids `runBlocking` in production sources.

---

## Phase 4 - Settings UI + Themes

**Goal:** A Settings screen lets the user choose theme, dynamic color, haptics, decimal precision, default currency/unit system, and crash-reporting opt-in.

### Phase 4 - Deliverables

- [x] `feature/settings/SettingsSheet.kt` (bottom-sheet content, not a full screen - reuses the existing modal-sheet container)
- [x] `feature/settings/SettingsViewModel.kt`
- [x] Theme system reads from `SettingsRepository`: `system` / `light` / `dark` segmented picker + dynamicColor toggle (live, no restart)
- [x] Haptics wired via `LocalHapticsEnabled` CompositionLocal + `HapticFeedback.performHapticFeedback`
- [x] Decimal precision slider (6..16) feeds `Evaluator` `MathContext` per-evaluation
- [x] About rows: version, license, GitHub link
- [x] Crash-reporting toggle is **off by default** with disclosure text
- [x] Sound toggle gates the existing DTMF tones (set `LocalKeyTones` to null when disabled)

### Phase 4 - Unit tests

- [x] `SettingsViewModelTest`: theme write propagates to the repository
- [x] `SettingsViewModelTest`: dynamic-color toggle persists via `SettingsRepository`
- [x] `SettingsViewModelTest`: precision and crash-opt-in writes propagate
- [ ] Precision change rebuilds the Evaluator with new MathContext - **verified via wiring** (Evaluator is constructed per-evaluation from `precision.value`); a dedicated test that asserts the rebuilt Evaluator picks up the new precision is a follow-up

### Phase 4 - Compose UI tests

**Deferred: no instrumented (`androidTest`) module yet - these tests require Compose UI test rules and aren't part of the JVM `:app:test` task.**

- [ ] Theme picker (`system`/`light`/`dark`) switches palette live
- [ ] Dynamic-color toggle pulls wallpaper colors on Android 12+
- [ ] Disabling haptics stops vibration on key press (verify via mocked `HapticFeedback`)
- [ ] Crash-reporting toggle defaults to OFF; disclosure text is visible

### Phase 4 - Exit criteria

- Settings persist, theming responds without restart, accessibility traversal order is logical. - **Met** (persistence/live theme): `SettingsViewModelTest` covers write-propagation; theming responds without restart via `Theme.kt` reading from `SettingsRepository`. Accessibility traversal verification still pending the instrumented module.

---

## Phase 5 - Unit Converter

**Goal:** Length, Area, Volume, Mass, Temperature, Speed, Time, Data, Pressure, Energy, Power conversions with persistent last-used pair per category.

### Phase 5 - Deliverables

- [x] `core/domain/converter/UnitCategory.kt`, `ConverterUnit.kt` (pure Kotlin, no Android)
- [x] `ConversionTable` per category (canonical-unit ratios; Temperature uses an affine offset, all others pass offset=0)
- [x] `feature/converter/unit/UnitConverterScreen.kt` (scrollable category tabs, two-pane From/To cards, circular swap button, ModalBottomSheet unit picker) - note: actual path is `feature/converter/unit/UnitConverterScreen.kt` directly (no `/ui/` subfolder; only `feature/basic/` uses the `/ui/` subfolder convention)
- [x] `feature/converter/unit/UnitConverterViewModel.kt`
- [x] Room entity `RecentUnitPairEntity(category PK, fromSymbol, toSymbol)` + `RecentUnitPairDao`
- [x] Precision honors `settings.precision` via a collect on SettingsRepository in the VM

### Phase 5 - Unit tests

- [x] Length: `1 km = 1000 m`, `1 mi = 1609.344 m`, `1 in = 2.54 cm`
- [x] Mass: `1 kg ≈ 2.20462 lb` (within 1e-5), `1 st = 14 lb`
- [x] Temperature: `0 °C = 32 °F`, `100 °C = 212 °F`, `0 °C = 273.15 K`, `-40 °C = -40 °F`
- [x] Volume: `1 L = 1000 mL`, `1 US gal ≈ 3.78541 L`
- [x] Data: `1 GB = 1024 MB` (binary), `1 B = 8 bit` - binary convention documented in ConversionTable
- [x] Round-trip: 5 forward+back pairs per category for every (from, to) combination
- [x] Swap from/to inverts the result (asserted via post-swap state)
- [x] Last-used pair persists per category (DAO upsert + VM recall test)

### Phase 5 - Compose UI tests

**Deferred: no instrumented (`androidTest`) module yet - these tests require Compose UI test rules and aren't part of the JVM `:app:test` task.**

- [ ] Tapping a category tab shows that category's units
- [ ] Typing in the `from` field updates the `to` field live
- [ ] Swap button exchanges values and units
- [ ] Empty input shows `0` not an error

### Phase 5 - Exit criteria

- All 11 categories functional offline, recents persist, formatting is locale-aware. - **Met** (offline + persistence): `ConverterTest` covers all 11 categories with round-trip; `RecentUnitPairDaoTest` covers persistence; `UnitConverterViewModelTest` covers recall. Locale-aware formatting at the converter UI boundary uses fixed DecimalFormat - migration to `NumberFormatter` is a Phase 8 follow-up.

---

## Phase 6 - Currency Converter (Online + Cached)

**Goal:** Daily rates for ~150 fiat currencies, with last-cached fallback when offline, multi-currency view, favourites pinning.

### Phase 6 - Deliverables

- [x] `core/data/network/RatesApi.kt` (Retrofit + kotlinx.serialization against open.er-api.com)
- [x] `core/data/network/RatesDto.kt` + repository mapper to domain `Rates`
- [x] `core/data/db/CurrencyRateEntity(code PK, rateVsBase, baseCode, fetchedAtUtc)` + DAO with atomic `replaceAll`
- [x] `RatesRepository` interface; `DefaultRatesRepository` impl - refresh hits API, cache stays the source for UI reads via observeCached
- [x] `FakeRatesApi` for tests (in-class inside `RatesRepositoryTest`)
- [x] `feature/converter/currency/CurrencyConverterScreen.kt` (amount field, base picker, multi-row list with pinned favourites first, refresh button, last-updated relative timestamp)
- [x] Favourites pinning: `FavoriteCurrencyEntity(code PK, position)` + DAO with append-at-end semantics
- [x] Manual `Refresh` action; error banner when the latest refresh fails (stale-cache banner > 24h - deferred follow-up, current relative timestamp lets the user see if it's been a while)

### Phase 6 - Unit tests

- [x] `RatesRepositoryTest` with `FakeRatesApi`: fresh fetch updates cache (`refreshWritesTheCache`)
- [x] Offline path: API throws → cache stays intact (`refreshFailureLeavesCacheIntact`)
- [x] Provider sends `result != "success"` → repository throws, cache not overwritten (`nonSuccessResultIsTreatedAsFailure`)
- [x] Conversion math: `100 USD × 83.2 = 8320 INR` (`100 USD times 83 point 2 equals 8320 INR via the rates map`)
- [x] Favourites: insert appends at end of position, remove deletes (`favouritesPersistAndAppendAtEnd`)
- [x] ViewModel: refresh failure surfaces as errorMessage without blanking cache

### Phase 6 - Compose UI tests

**Deferred: no instrumented (`androidTest`) module yet - these tests require Compose UI test rules and aren't part of the JVM `:app:test` task.**

- [ ] First launch with no cache shows a loading spinner, then the rates
- [ ] Airplane mode → cached rates render with a "stale" badge if older than 24h
- [ ] Tap a currency code → picker dialog → selection updates the row
- [ ] Pinned favourites appear above non-favourites and persist across launches
- [ ] Manual refresh updates the `last updated` timestamp

### Phase 6 - Integration tests

- [ ] One real network call (CI-skipped, `@DisabledIfEnvironmentVariable` for offline CI) to confirm API contract - **Deferred:** no `@DisabledIfEnvironmentVariable` wiring yet; API contract is exercised indirectly via `FakeRatesApi` in `RatesRepositoryTest`.

### Phase 6 - Exit criteria

- Airplane-mode test passes (cached rates render), no network call on cold launch if cache is fresh (<24h). - **Met** (cache integrity): `RatesRepositoryTest.refreshFailureLeavesCacheIntact` verifies offline path; `CurrencyConverterViewModelTest` covers refresh-failure UX. End-to-end airplane-mode instrumented assertion pending the `androidTest` module.

---

## Phase 7 - Life Calculators

**Goal:** Seven focused calculators: Loan/EMI, GST (India), BMI, Age, Discount, Date Difference, Ovulation.

### Phase 7.1 - Loan / EMI deliverables

- [x] `feature/finance/loan/LoanScreen.kt` (local Compose state; the math is simple enough that a ViewModel is overkill)
- [x] `core/domain/finance/EmiCalculator.kt`: `EMI = P × r × (1+r)^n / ((1+r)^n − 1)` with a zero-interest fast path
- [x] Amortisation table (month-by-month principal/interest/balance) - rendered as part of `EmiResult` (UI shows headline figures; full table available as `result.amortisation`)
- [x] Neutral copy: pinned disclaimer "Estimator only - not a lending tool or quote."

### Phase 7.2 - GST (India) deliverables

- [x] `feature/finance/gst/GstScreen.kt`
- [x] Presets: 5/12/18/28% chips + free-text custom rate field
- [x] Output: CGST/SGST (intra-state) or IGST (inter-state), net, gross; forward and reverse modes
- [ ] Visible only in `IN` locale - deferred to Phase 8 (i18n pass)

### Phase 7.3 - BMI deliverables

- [x] `feature/health/bmi/BmiScreen.kt`
- [x] Metric (cm/kg) + Imperial (ft+in/lb) units (last-choice persistence deferred to a Phase 7 follow-up; current screen defaults to Metric)
- [x] Category: Underweight / Normal / Overweight / Obese (WHO ranges)

### Phase 7.4 - Age deliverables

- [x] `feature/datetime/age/AgeScreen.kt` (Material3 DatePickerDialog)
- [x] DOB picker, output years/months/days, next birthday countdown (days), weekday of birth

### Phase 7.5 - Discount deliverables

- [x] `feature/finance/discount/DiscountScreen.kt`
- [x] Forward: MRP + % → savings + final
- [x] Reverse: MRP + final → %

### Phase 7.6 - Date Difference deliverables

- [x] `feature/datetime/datediff/DateDiffScreen.kt`
- [x] Mode 1: two dates → y/m/d + total days + weeks remainder
- [x] Mode 2: date + offset days → resulting date

### Phase 7.7 - Ovulation deliverables

- [x] `feature/health/ovulation/OvulationScreen.kt`
- [x] `core/domain/health/OvulationCalculator.kt` (pure Kotlin)
- [x] Inputs: LMP date + average cycle length slider (default 28, range 21-35)
- [x] Outputs: ovulation date, six-day fertile window, next period date, estimated due date (Naegele's rule)
- [x] Educational disclaimer: "Estimator only. Cycles vary; this is not medical advice and not a contraception tool."
- [x] No data leaves the device; LMP is in local Compose state only, never persisted

### Phase 7 - Unit tests (one suite per calculator)

- [x] **EMI:** `P=100000, r=10%/yr, n=12` → EMI ≈ `8791.59`, total paid ≈ `105499.07`
- [x] **EMI:** zero-interest loan (`r=0`) → `EMI = P/n`
- [x] **EMI:** amortisation: final balance = 0 within rounding; rows sum to total paid
- [x] **GST:** `1000 @ 18%` intra-state → CGST `90`, SGST `90`, gross `1180`
- [x] **GST:** inter-state `1000 @ 18%` → IGST `180`, no CGST/SGST; reverse `1180@18%` recovers `1000` net
- [x] **BMI:** `70kg, 1.70m` → `24.22` → `Normal`; each category boundary asserted
- [x] **BMI:** `170lb, 5'10"` matches metric equivalent within 0.1
- [x] **Age:** DOB `1990-01-15`, today `2026-05-18` → `36y 4m 3d`, Monday
- [x] **Age:** Feb 29 in non-leap year falls back to Feb 28
- [x] **Discount:** MRP `2000`, 20% off → final `1600`, savings `400`; 100% off → 0
- [x] **Discount reverse:** MRP `2000`, final `1500` → `25%`
- [x] **DateDiff:** `2024-02-29` to `2025-02-28` → `0y 11m 30d` (365 days / 52w 1d)
- [x] **DateDiff:** add `90d` to `2026-01-01` → `2026-04-01`; -1d goes to 2025-12-31; argument-swap symmetry
- [x] **Ovulation:** LMP `2026-05-01`, cycle `28` → ovulation `2026-05-15`, fertile `2026-05-10`..`2026-05-16`, next period `2026-05-29`, due date `2027-02-05`
- [x] **Ovulation:** cycle `35` → ovulation +7d from default
- [x] **Ovulation:** rejects cycle length outside 21-35 with `IllegalArgumentException`

### Phase 7 - Compose UI tests

**Deferred: no instrumented (`androidTest`) module yet - these tests require Compose UI test rules and aren't part of the JVM `:app:test` task.**

- [ ] Each calculator screen has at least one test that types inputs and asserts the displayed output
- [ ] GST screen is **hidden** in `Locale.US`, visible in `Locale("en", "IN")` (also pending the Phase 8 locale-gating work)
- [x] Loan screen wording does not contain any of: `apply`, `qualify`, `lender`, `borrow` (lint-style assertion) - via `LoanCopyTest` (JVM-side grep over `LoanScreen.kt`, no instrumented rig required)

### Phase 7 - Exit criteria

- All six calculators ship with unit + UI tests, GST locale-gating works, loan copy passes the wording check. - **Partially met**: seven calculators (now including Ovulation) ship with unit-test suites (`EmiCalculatorTest`, `GstCalculatorTest`, `DiscountCalculatorTest`, `BmiCalculatorTest`, `AgeCalculatorTest`, `DateDiffCalculatorTest`, `OvulationCalculatorTest`). `LoanCopyTest` enforces the wording rule. GST locale-gating and per-screen Compose UI tests still deferred to Phase 8 + the instrumented module.

---

## Phase 8 - Accessibility, i18n, Launcher Shortcuts

**Goal:** TalkBack-friendly, English + Hindi strings, locale-aware numbers/dates, launcher shortcuts and home search.

### Phase 8 - Deliverables

- [~] Content descriptions on every key and actionable icon - keypad, hamburger, swap, back, refresh, history-delete, currency pin all have descriptions; audit pass against every screen is a follow-up
- [~] Minimum touch target `48dp` - keypad buttons use `aspectRatio(1.6f)` which on portrait keeps them well above 48dp; explicit modifier helper still pending
- [x] Locale-aware `NumberFormatter` utility (en-US, en-IN with lakh grouping, de-DE, plus parse). Adoption at every UI boundary is a follow-up (currently in place at `NumberFormatter.format`/`money`; screens still use locale-fixed DecimalFormat)
- [x] String resources extracted into `values/strings.xml` (~95 keys covering shell, tools menu, settings, history, life-calculator titles + disclaimers) + `values-hi/strings.xml` Hindi translations
- [~] RTL-safe layouts - Compose uses logical start/end by default; explicit RTL preview audit pending
- [ ] Home-screen search bar (deferred - tools menu already gives one-tap access; search is a power-user addition)
- [x] Static launcher shortcuts: new calculation, unit converter, currency converter (`shortcuts.xml` + manifest + NavHost deep-link wiring)
- [ ] Dynamic shortcuts driven by recent usage
- [ ] High-contrast mode honored on Android 14+

### Phase 8 - Unit tests

- [x] Number formatter: en-US/en-IN/de-DE assertions all pass; money formatter keeps two decimals; parse round-trip via the German locale; negative-sign placement
- [ ] Date formatter test (system DateFormat is in use but no explicit suite yet)
- [ ] Search index returns "Loan" for query `emi`, `loan`, `कर्ज` (deferred with the search bar)

### Phase 8 - Compose UI / instrumented tests

**Deferred: no instrumented (`androidTest`) module yet - these tests require Compose UI test rules and aren't part of the JVM `:app:test` task.**

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

- [x] New module `:baselineprofile` (Macrobenchmark) - scaffolded at `baselineprofile/`
- [x] Cold-start, warm-start, frame-timing benchmarks for basic calculator + history - via `baselineprofile/src/main/java/com/calculator/baselineprofile/{BaselineProfileGenerator,StartupBenchmark,FrameTimingBenchmark}.kt`
- [x] `androidx.startup` initialisers for any eager work - `androidx.startup` is wired in `app/build.gradle.kts`; current eager work is minimal (Hilt handles app init)
- [x] Baseline profile committed at `app/src/main/baseline-prof.txt`
- [x] CI step: regenerate profile on `main` - via `.github/workflows/baseline-profile.yml` (cold-start regression gate is the follow-up; current workflow uploads the profile as an artifact)

### Phase 9 - Test cases

**Deferred: Macrobenchmark runs on emulator/device only, not part of `:app:test` JVM suite.**

- [ ] Macrobenchmark cold-start: P50 < 600 ms on the chosen device - **Deferred:** runs on emulator/device only
- [ ] Macrobenchmark warm-start: P50 < 200 ms - **Deferred:** runs on emulator/device only
- [ ] Frame timing for keypad scroll: no janky frames over 16 ms at P95 - **Deferred:** runs on emulator/device only
- [x] CI artifact contains a fresh `baseline-prof.txt` on every `main` build - via `.github/workflows/baseline-profile.yml`

### Phase 9 - Exit criteria

- Profile checked in, CI gates regressions, app size still under 15 MB after R8. - **Partially met**: profile checked in at `app/src/main/baseline-prof.txt`; CI regenerates on `main`; AAB measured at 5.1 MB (well under 15 MB). The "fail PR if cold-start regresses > 10%" gate is the remaining follow-up.

---

## Phase 10 - Widgets + Quick Settings Tile (Stretch)

**Goal:** Home-screen Glance widget for a quick calc; QS tile to open basic calculator.

### Phase 10 - Deliverables

- [x] `core/widget/QuickCalcWidget.kt` (Glance `GlanceAppWidget`)
- [x] Widget UI: display row + 5x4 keypad (digits, +, -, ×, ÷, (, ), C, ⌫, =). Same Evaluator the main app uses.
- [x] `QuickCalculatorTileService` (QS tile) opens MainActivity via `startActivityAndCollapse` (PendingIntent on API 34+, legacy Intent on older)
- [x] Manifest entries (Glance receiver + `BIND_QUICK_SETTINGS_TILE` service) + `res/xml/quick_calc_widget_info.xml`

### Phase 10 - Test cases

**Deferred: no instrumented (`androidTest`) module yet - Glance widget + QS tile assertions require a running device.**

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

- [ ] Play App Signing enrolled, release keystore secured - **out-of-band**: docs/RELEASE.md "Signing" section is the runbook; needs the user to generate the keystore and add GitHub Actions secrets.
- [x] Versioning: SemVer `versionName` (1.0.0), monotonic `versionCode` (10000); formula in docs/RELEASE.md
- [x] Release `bundleRelease` AAB measured at **5.1 MB** - 3× under the 15 MB ceiling (R8 + resource shrinking on)
- [x] Privacy policy at [PRIVACY.md](../PRIVACY.md) covering `INTERNET`, opt-in crash reporting, on-device-only storage
- [x] Play Console listing copy at [docs/PLAY_LISTING.md](PLAY_LISTING.md) (title / short + long descriptions / Data Safety verbatim answers); screenshots still need to be captured from a release build on a Pixel 6a-class device
- [x] Data Safety form: verbatim answers in docs/PLAY_LISTING.md (no data collected)
- [ ] Internal → closed-beta → staged production rollout - **out-of-band**: needs Play Console access; runbook in docs/RELEASE.md
- [x] GitHub Release workflow ([.github/workflows/release.yml](../.github/workflows/release.yml)) attaches AAB + APK on every `v*.*.*` tag push
- [x] LoanCopyTest grep-checks LoanScreen.kt user-visible strings for Play's banned personal-loans wording (apply / qualify / lender / borrow / etc.)

### Phase 11 - Test cases

- [ ] Release AAB installs and launches from a Play Internal Testing link - **out-of-band:** needs Play Console upload
- [ ] All features work on real-device matrix: phone (API 31, 34, 36), tablet, foldable, RTL locale - **out-of-band:** real-device matrix QA
- [ ] Pre-launch report from Play Console has zero `error` crashes - **out-of-band:** Play pre-launch report
- [ ] No third-party SDK appears in the AAB's dependency report beyond what is listed in REQUIREMENTS.md
- [ ] App passes Play's pre-launch policy scan (loan-copy check, data-safety form match) - **out-of-band:** Play submission gate; `LoanCopyTest` already enforces the wording check at JVM-test time

### Phase 11 - Exit criteria

- v1.0.0 live on Production track, GitHub `v1.0.0` tag + release published. - **out-of-band:** awaiting Play Console upload + tag push.

---

## Cross-phase running checklists

These run continuously, not as phase gates:

- [x] Every PR keeps `./gradlew ktlintCheck detekt lint test` green - enforced by `.github/workflows/ci.yml`
- [x] Every PR that changes the stack updates **both** `CLAUDE.md` and `REQUIREMENTS.md` - convention documented in CLAUDE.md
- [x] No `runBlocking` in production sources (detekt rule enforces)
- [x] No `Co-Authored-By: Claude` trailers on commits
- [x] No em dashes / en dashes - ASCII hyphens only
- [x] No `Crashlytics`, `Firebase Analytics`, or third-party telemetry SDK added without an explicit user toggle
