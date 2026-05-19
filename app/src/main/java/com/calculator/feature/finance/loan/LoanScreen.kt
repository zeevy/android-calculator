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
import java.text.DecimalFormat
import java.text.DecimalFormatSymbols
import java.util.Locale

/**
 * Loan/EMI **estimator**. Strict wording: this is not a lending tool.
 *
 * Inputs: principal, annual interest rate (%), tenure in months.
 * Outputs: monthly EMI, total interest, total paid.
 *
 * Per Phase 7 plan: no "you qualify for", "apply for a loan", "borrow"
 * language anywhere in the UI copy.
 */
@Composable
fun LoanScreen(onUp: () -> Unit) {
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
                LifeCalcOutputRow("Monthly EMI", money.format(result.emi), accent = true)
                LifeCalcOutputRow("Total interest", money.format(result.totalInterest))
                LifeCalcOutputRow("Total paid", money.format(result.totalPayment))
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

private val money: DecimalFormat by lazy {
    DecimalFormat("#,##0.00", DecimalFormatSymbols(Locale.US))
}

// Avoid unused import warning; Compose padding is referenced above.
private fun Modifier.padding(): Modifier = this
