package com.calculator.feature.finance.loan

import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.size
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import com.calculator.core.domain.finance.EmiCalculator
import com.calculator.feature.lifecalc.LifeCalcCard
import com.calculator.feature.lifecalc.LifeCalcNumberField
import com.calculator.feature.lifecalc.LifeCalcOutputRow
import com.calculator.feature.lifecalc.LifeCalcSectionLabel
import com.calculator.feature.lifecalc.LifeCalculatorScaffold
import com.calculator.core.common.format.NumberFormatter
import java.util.Locale

/**
 * Loan/EMI **estimator**. Strict wording: this is not a lending tool.
 *
 * Inputs: principal, annual interest rate (%), tenure in months.
 * Outputs: monthly EMI, total interest, total paid.
 *
 * Per Phase 7 plan: no "you qualify for", "apply for a loan", "borrow"
 * language anywhere in the UI copy. The footnote below the result and
 * the screen title both reinforce that this is an arithmetic estimator
 * - required by Google Play's personal-loans policy (the moment the
 * copy implies the app *is* a lender or pre-qualifier, the listing is
 * subject to additional review and possible takedown).
 *
 * @param onUp Pop the calculator from the back stack. Wired to the
 *   scaffold's back affordance; defined as a `() -> Unit` rather than a
 *   `NavController` so previews can pass a no-op.
 */
@Composable
fun LoanScreen(onUp: () -> Unit) {
    // Sensible defaults that immediately render a populated result card
    // so first-time users see the shape of the output without having to
    // type anything. Numbers are stored as strings (not Double) because
    // the field is text-based and partial inputs ("1.", "100,000") need
    // to round-trip through the field without prematurely committing.
    var principal by remember { mutableStateOf("100000") }
    var rate by remember { mutableStateOf("10") }
    var months by remember { mutableStateOf("12") }

    LifeCalculatorScaffold(title = "Loan estimator", onUp = onUp) {
        LifeCalcCard {
            LifeCalcSectionLabel("Inputs")
            LifeCalcNumberField("Loan amount", principal, { principal = it })
            LifeCalcNumberField("Annual interest rate", rate, { rate = it }, suffix = "%")
            LifeCalcNumberField("Tenure", months, { months = it }, suffix = "months")
        }

        // Re-evaluate on every recomposition. The math is microseconds
        // for any realistic tenure, so caching it behind `remember`
        // would cost more in correctness (forgetting a key) than it
        // saves. `runCatching` swallows the NumberFormatException raised
        // when a field is mid-edit (e.g. "" or "1.") and the require()
        // inside `emi()` for non-positive values - both render as
        // "enter values" rather than an exception.
        val result =
            runCatching {
                EmiCalculator.emi(
                    principal = principal.toDouble(),
                    annualRatePercent = rate.toDouble(),
                    months = months.toInt(),
                )
            }.getOrNull()

        if (result != null) {
            LifeCalcCard {
                LifeCalcSectionLabel("Result")
                LifeCalcOutputRow("Monthly EMI", money(result.emi), accent = true)
                LifeCalcOutputRow("Total interest", money(result.totalInterest))
                LifeCalcOutputRow("Total paid", money(result.totalPayment))
            }
            Spacer(Modifier.size(4.dp))
            Text(
                text = "Estimator only - not a lending tool or quote.",
                style = MaterialTheme.typography.bodySmall,
                color = Color.White.copy(alpha = 0.45f),
                modifier = Modifier.padding(),
            )
        } else {
            LifeCalcCard {
                LifeCalcSectionLabel("Result")
                Text(
                    text = "Enter a loan amount, rate, and tenure to see your EMI.",
                    style = MaterialTheme.typography.bodyMedium,
                    color = Color.White.copy(alpha = 0.55f),
                )
            }
        }
    }
}

// Money formatting routes through [NumberFormatter.money] so a user
// running the app in en-IN sees lakh/crore grouping ("1,23,456.78"),
// en-US sees thousands grouping ("123,456.78"), de-DE sees swapped
// separators ("123.456,78"). Currency symbols are deliberately
// omitted - the EMI value is unitless until the user picks a locale.
private fun money(value: Double): String =
    NumberFormatter.money(value, Locale.getDefault())

// No-op extension to keep the Compose padding import valid without
// applying any extra padding. The footnote Text composable references
// `Modifier.padding()` defensively; the actual padding it would apply
// is zero so a future tweak can replace this with real padding without
// having to add a new import.
private fun Modifier.padding(): Modifier = this
