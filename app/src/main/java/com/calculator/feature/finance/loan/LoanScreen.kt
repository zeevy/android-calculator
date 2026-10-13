package com.calculator.feature.finance.loan

import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.size
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.calculator.R
import com.calculator.core.common.format.NumberFormatter
import com.calculator.core.common.input.InputResult
import com.calculator.core.designsystem.component.LifeCalcCard
import com.calculator.core.designsystem.component.LifeCalcNumberField
import com.calculator.core.designsystem.component.LifeCalcOutputRow
import com.calculator.core.designsystem.component.LifeCalcSectionLabel
import com.calculator.core.designsystem.component.LifeCalculatorScaffold
import com.calculator.core.designsystem.component.errorFor

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
 * @param onOpenMenu Opens the app's tools menu.
 */
@Composable
fun LoanScreen(onOpenMenu: () -> Unit) {
    // Sensible defaults that immediately render a populated result card
    // so first-time users see the shape of the output without having to
    // type anything. Numbers are stored as strings (not Double) because
    // the field is text-based and partial inputs ("1.", "100,000") need
    // to round-trip through the field without prematurely committing.
    // Saveable, so typed values survive the app being killed in the
    // background.
    var principal by rememberSaveable { mutableStateOf("100000") }
    var rate by rememberSaveable { mutableStateOf("10") }
    var months by rememberSaveable { mutableStateOf("12") }
    val input = remember(principal, rate, months) { loanInput(principal, rate, months) }
    val scheme = MaterialTheme.colorScheme

    LifeCalculatorScaffold(
        title = stringResource(R.string.loan_title),
        onOpenMenu = onOpenMenu,
    ) {
        LifeCalcCard {
            LifeCalcSectionLabel(stringResource(R.string.loan_section_inputs))
            LifeCalcNumberField(
                label = stringResource(R.string.loan_loan_amount),
                value = principal,
                onValueChange = { principal = it },
                error = input.errorFor(R.string.loan_loan_amount),
            )
            LifeCalcNumberField(
                label = stringResource(R.string.loan_annual_rate),
                value = rate,
                onValueChange = { rate = it },
                suffix = stringResource(R.string.loan_rate_suffix),
                error = input.errorFor(R.string.loan_annual_rate),
            )
            LifeCalcNumberField(
                label = stringResource(R.string.loan_tenure),
                value = months,
                onValueChange = { months = it },
                suffix = stringResource(R.string.loan_tenure_unit),
                error = input.errorFor(R.string.loan_tenure),
            )
        }

        LifeCalcCard {
            LifeCalcSectionLabel(stringResource(R.string.loan_section_result))
            when (input) {
                is InputResult.Ok -> {
                    val result = input.value
                    LifeCalcOutputRow(
                        label = stringResource(R.string.loan_monthly_emi),
                        value = NumberFormatter.money(result.emi),
                        accent = true,
                    )
                    LifeCalcOutputRow(
                        label = stringResource(R.string.loan_total_interest),
                        value = NumberFormatter.money(result.totalInterest),
                    )
                    LifeCalcOutputRow(
                        label = stringResource(R.string.loan_total_paid),
                        value = NumberFormatter.money(result.totalPayment),
                    )
                }
                is InputResult.Invalid ->
                    Text(
                        text = stringResource(R.string.loan_error_empty),
                        style = MaterialTheme.typography.bodyMedium,
                        color = scheme.error,
                    )
            }
        }
        if (input is InputResult.Ok) {
            Spacer(Modifier.size(4.dp))
            Text(
                text = stringResource(R.string.loan_disclaimer),
                style = MaterialTheme.typography.bodySmall,
                color = scheme.onSurfaceVariant,
            )
        }
    }
}
