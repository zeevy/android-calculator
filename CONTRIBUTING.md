# Contributing to Calculator

Thanks for your interest in contributing. This document describes how to set up a development environment, the conventions the project follows, and how to get a change merged.

If you only have a few minutes, the short version is: open an issue first for anything non-trivial, then submit a PR that passes `./gradlew ktlintCheck detekt test`.

## Table of contents

- [Code of conduct](#code-of-conduct)
- [Getting started](#getting-started)
- [How to contribute](#how-to-contribute)
- [Coding conventions](#coding-conventions)
- [Tests](#tests)
- [Static analysis](#static-analysis)
- [Commit messages](#commit-messages)
- [Opening a pull request](#opening-a-pull-request)

## Code of conduct

We follow the [Contributor Covenant](https://www.contributor-covenant.org/version/2/1/code_of_conduct/). Be kind, be welcoming, and assume good faith.

## Getting started

1. **Fork** the repository and clone your fork.
2. **Install prerequisites**:
   - JDK 17 (Temurin or any other distribution)
   - Android Studio Ladybug or newer
   - Android SDK with platform 36 (Android 16) installed
3. **Generate the Gradle wrapper** if it is not checked in:
   ```bash
   gradle wrapper --gradle-version 8.11.1
   ```
4. **Verify the build**:
   ```bash
   ./gradlew :app:assembleDebug test
   ```

If something does not work out of the box, that is a bug worth filing - please open an issue.

## How to contribute

- **Bug reports**: open an issue with steps to reproduce, expected vs actual behaviour, device + Android version, and a stack trace if available.
- **Feature requests**: open an issue describing the use case before writing code. We will discuss scope and design there to save you a wasted PR.
- **Documentation**: typos, missing context, broken links - PRs welcome with no prior issue needed.
- **Small fixes** (one-line typos, obvious bugs): jump straight to a PR.

For anything that touches architecture or adds a dependency, please discuss it in an issue first.

## Coding conventions

This project ships under the Apache License 2.0 and is intended to be read by contributors who may be new to Android, Kotlin, or both. **Readability matters more than terseness.** Follow these rules:

### KDoc and comments

- **Every public class, object, interface, and top-level function gets a KDoc.** Lead with a one-sentence summary; expand only when the contract is non-obvious.
- **Non-trivial public methods get a KDoc.** Skip one-liners whose name is self-explanatory.
- **Inline comments** explain the WHY, never the WHAT. If the code reads clearly, do not narrate it.
- **No change-log style comments** ("added for issue #42", "used by FooScreen") - those belong in PR descriptions and git history.

### Style

- Kotlin official code style (set in `gradle.properties`).
- 4-space indentation, max line length 140.
- Prefer expression bodies for trivial functions, block bodies for everything else.
- No `runBlocking` in production code. Tests may use `runTest`.
- ViewModels expose `StateFlow<UiState>`; UI collects with `collectAsStateWithLifecycle`.
- Single-activity architecture; new screens are `composable<RouteType> { }` blocks in `CalculatorNavHost`.
- Features under `feature/<name>/` never import another feature. Share via `core/`.

### Punctuation in source files

- Use ASCII hyphens (`-`), never em dashes (`-`) or en dashes (`-`). This applies to KDoc, code comments, commit messages, and any markdown file.

### Locale and i18n

- Never hardcode user-visible strings; add them to `app/src/main/res/values/strings.xml` and reference by ID.
- Numeric formatting respects the device locale - use the locale helpers in `core/common/` rather than `String.format` with hard-coded patterns.

## Tests

- **Unit tests live in `app/src/test/`** and run on the JVM using JUnit5.
- **Instrumented tests live in `app/src/androidTest/`** and run on an emulator/device using JUnit4 + AndroidX Test.
- Every bug fix must include a regression test.
- Every new feature must include unit tests for its domain logic and a smoke-level Compose UI test for the screen.

Run tests locally:

```bash
./gradlew test                    # unit tests
./gradlew connectedAndroidTest    # instrumented tests (needs a running device/emulator)
```

## Static analysis

The CI pipeline enforces ktlint, detekt, and Android Lint on every PR. Run them locally before pushing:

```bash
./gradlew ktlintCheck
./gradlew detekt
./gradlew lint
```

Auto-format trivial style issues:

```bash
./gradlew ktlintFormat
```

If a detekt rule trips on legitimate code, **prefer changing the code over suppressing the rule**. If a suppression is genuinely warranted, annotate with `@Suppress("RuleName")` at the smallest possible scope and add a comment explaining why.

## Commit messages

We follow **Conventional Commits**:

```
type(scope): short summary

Longer body if needed, wrapped at ~72 chars. Explain WHY,
not WHAT (the diff already shows what).
```

Common types:
- `feat` - new user-facing functionality
- `fix` - bug fix
- `refactor` - code restructuring with no behavioural change
- `test` - tests only
- `docs` - documentation only
- `chore` - build, CI, dependency bumps
- `perf` - performance improvement

Examples:
- `feat(currency): cache exchange rates in Room with 24h TTL`
- `fix(math): treat -(-5) as 5 instead of a syntax error`
- `docs(readme): add screenshot of basic calculator`

## Opening a pull request

1. **Branch** from `main` with a descriptive name: `feat/currency-converter`, `fix/parse-leading-decimal`.
2. **Keep PRs focused** - one logical change per PR. Big diffs are harder to review and slower to land.
3. **Run the local verification** before pushing:
   ```bash
   ./gradlew ktlintCheck detekt lint test
   ```
4. **Open the PR** against `main` with:
   - A summary of what changed and **why**.
   - A test plan listing what you verified manually (if applicable).
   - Linked issue (`Closes #123`) where relevant.
5. **Address review feedback** by pushing follow-up commits, not force-pushes. We squash on merge so commit history stays clean.

Thanks again for contributing!
