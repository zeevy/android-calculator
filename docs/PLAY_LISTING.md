# Play Store Listing Copy

Drop these into the Google Play Console fields. Word counts assume
Play's current limits (title ≤ 30 chars, short description ≤ 80 chars,
long description ≤ 4000 chars).

## Title

```
Calculator – CA
```

(13 / 30 chars)

## Short description

```
Basic + scientific calculator, unit converter, life calculators. Offline. Ad-free.
```

(80 / 80 chars)

## Long description

```
A modern, fast, ad-free calculator that does more than just add and subtract.

CORE
• Basic mode: arithmetic, percentage, parentheses, sign flip, history
• Advanced mode: trig (sin / cos / tan and inverses), logarithms, square and cube roots, powers, π and e constants, factorial, memory (M+ / M- / MR / MC)
• Live result preview as you type; expressions and results saved in History

UNIT CONVERTER
• 11 categories: length, area, volume, mass, temperature, speed, time, data, pressure, energy, power
• ~70 units; NIST-accurate constants
• Last-used pair per category remembered

CURRENCY CONVERTER
• 150+ fiat currencies, refreshed daily from a public free API
• Works offline using the last cached rates
• Favourite codes pin to the top

LIFE CALCULATORS
• Loan EMI estimator with full amortisation
• GST (India) with CGST / SGST / IGST split, forward and reverse
• Discount: forward (% off → final) and reverse (final → %)
• BMI in metric or imperial
• Age in years / months / days, days to next birthday
• Date difference and date + offset
• Ovulation estimator: ovulation date, fertile window, next period, due date

SETTINGS
• Theme: System / Light / Dark
• Dynamic colour from your wallpaper (Android 12+)
• Haptics, sound, math precision, opt-in crash reporting (off by default)

PRIVACY
• No analytics, no ads, no third-party trackers
• The only network call is to fetch currency rates from a public anonymous API
• Crash reports are off by default; nothing leaves the device until you opt in
• Apache 2.0 open-source on GitHub: zeevy/android-calculator
```

## Categorisation

| Field | Value |
|---|---|
| Category | Tools |
| Tags | Calculator, Productivity, Utilities |
| Content rating | Everyone |
| Target age | 13+ (Tools default) |

## Contact details

| Field | Value |
|---|---|
| Email | venkateswara.rao@zaggle.in |
| Privacy policy URL | <https://github.com/zeevy/android-calculator/blob/main/PRIVACY.md> |
| Website | <https://github.com/zeevy/android-calculator> |

## Data Safety declaration

Verbatim answers for the Play Console Data Safety form:

| Section | Answer |
|---|---|
| Does your app collect or share any of the required user data types? | **No** |
| Is all of the user data collected by your app encrypted in transit? | **N/A** (no user data is collected) |
| Do you provide a way for users to request that their data be deleted? | **N/A** (no user data is collected; uninstalling clears all on-device storage) |

## Permissions justification

The app declares one permission:

- `INTERNET` — used **exclusively** by the currency-rate converter to
  fetch publicly-available daily exchange rates from `open.er-api.com`.
  No identifiers are sent. Every other feature works fully offline.

## Loan-screen wording check

Google Play's personal-loans policy bans wording that implies the app
is a lender. The loan screen complies; the literal strings shipped:

- Title: `Loan estimator`
- Disclaimer pinned below the result card: `Estimator only - not a lending tool or quote.`
- Input labels: `Loan amount`, `Annual interest rate`, `Tenure`
- Result labels: `Monthly EMI`, `Total interest`, `Total paid`

The string `apply`, `qualify`, `lender`, and `borrow` do **not** appear
anywhere in the loan UI - verified by the unit test
`com.calculator.feature.finance.loan.LoanCopyTest`.

## Screenshots (TODO)

Need 8 phone + 1 7" tablet + 1 10" tablet screenshots for the listing.
Recommended set (capture from a Pixel 6a-class device with the
release build installed):

1. Basic calculator with a multi-line expression mid-calculation.
2. Advanced calculator showing trig + memory chip.
3. Tools menu (all 13 tiles visible).
4. Currency converter with a fresh refresh.
5. Unit converter on Length category.
6. History sheet with several entries.
7. Loan estimator filled in.
8. Settings sheet showing the appearance + feedback controls.

`docs/screenshots/` is the canonical folder; each file should be
1080x2400 (Pixel 6a native).
