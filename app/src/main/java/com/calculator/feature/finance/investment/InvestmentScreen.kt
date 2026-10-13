package com.calculator.feature.finance.investment

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.res.stringResource
import com.calculator.R
import com.calculator.core.common.format.NumberFormatter
import com.calculator.core.common.input.InputResult
import com.calculator.core.designsystem.component.LifeCalcCard
import com.calculator.core.designsystem.component.LifeCalcNumberField
import com.calculator.core.designsystem.component.LifeCalcOutputRow
import com.calculator.core.designsystem.component.LifeCalcSectionLabel
import com.calculator.core.designsystem.component.LifeCalcSegmented
import com.calculator.core.designsystem.component.LifeCalculatorScaffold
import com.calculator.core.designsystem.component.errorFor

/**
 * Returns calculator for both SIP (monthly contribution) and lump-sum
 * compound-interest scenarios.
 *
 * Mode toggle at the top switches the input row: SIP shows "Monthly
 * amount", lump-sum shows "Principal". The rate and tenure rows are
 * shared. Output card reports maturity, total invested, and gains.
 *
 * Defaults: ₹5,000/month at 12% for 10y (SIP) - a common reference -
 * so the result card is populated on first render.
 *
 * @param onOpenMenu Opens the app's tools menu.
 */
@Composable
fun InvestmentScreen(onOpenMenu: () -> Unit) {
    // Saveable, so the mode and typed values survive the app being
    // killed in the background.
    var mode by rememberSaveable { mutableIntStateOf(0) } // 0 = SIP, 1 = lump-sum
    var amount by rememberSaveable { mutableStateOf("5000") }
    var rate by rememberSaveable { mutableStateOf("12") }
    var years by rememberSaveable { mutableStateOf("10") }
    val lumpSum = mode == 1
    val input = remember(lumpSum, amount, rate, years) { investmentInput(lumpSum, amount, rate, years) }
    val amountLabel = investmentAmountLabel(lumpSum)
    val scheme = MaterialTheme.colorScheme

    LifeCalculatorScaffold(
        title = stringResource(R.string.investment_title),
        onOpenMenu = onOpenMenu,
    ) {
        LifeCalcCard {
            LifeCalcSectionLabel(stringResource(R.string.investment_section_mode))
            LifeCalcSegmented(
                options = listOf(
                    stringResource(R.string.investment_mode_sip),
                    stringResource(R.string.investment_mode_lumpsum),
                ),
                selectedIndex = mode,
                onSelect = { mode = it },
            )
            LifeCalcNumberField(
                label = stringResource(amountLabel),
                value = amount,
                onValueChange = { amount = it },
                error = input.errorFor(amountLabel),
            )
            LifeCalcNumberField(
                label = stringResource(R.string.investment_rate),
                value = rate,
                onValueChange = { rate = it },
                suffix = "%",
                error = input.errorFor(R.string.investment_rate),
            )
            LifeCalcNumberField(
                label = stringResource(R.string.investment_years),
                value = years,
                onValueChange = { years = it },
                suffix = stringResource(R.string.investment_years_suffix),
                error = input.errorFor(R.string.investment_years),
            )
        }

        LifeCalcCard {
            LifeCalcSectionLabel(stringResource(R.string.investment_section_result))
            when (input) {
                is InputResult.Invalid ->
                    Text(
                        text = stringResource(R.string.investment_error_invalid),
                        style = MaterialTheme.typography.bodyMedium,
                        color = scheme.error,
                    )
                is InputResult.Ok -> {
                    val result = input.value
                    LifeCalcOutputRow(
                        label = stringResource(R.string.investment_invested),
                        value = NumberFormatter.money(result.invested),
                    )
                    LifeCalcOutputRow(
                        label = stringResource(R.string.investment_gains),
                        value = NumberFormatter.money(result.gains),
                    )
                    LifeCalcOutputRow(
                        label = stringResource(R.string.investment_maturity),
                        value = NumberFormatter.money(result.maturity),
                        accent = true,
                    )
                }
            }
        }
    }
}
