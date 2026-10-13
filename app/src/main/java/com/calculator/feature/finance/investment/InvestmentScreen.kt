package com.calculator.feature.finance.investment

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
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
import com.calculator.core.domain.finance.CagrResult
import com.calculator.core.domain.finance.InvestmentResult

/**
 * Investment calculator with three modes: a monthly SIP, a one-time lump
 * sum, and CAGR (the yearly growth rate between a start and an end value).
 *
 * SIP and lump sum share the amount, rate and tenure fields; the output
 * card reports maturity, total invested and gains. CAGR has its own
 * start value, end value and tenure, so switching modes keeps what was
 * typed in each.
 *
 * Defaults: ₹5,000/month at 12% for 10y (SIP) and ₹1,00,000 growing to
 * ₹2,00,000 in 5y (CAGR), so the result card is populated on first render.
 *
 * @param onOpenMenu Opens the app's tools menu.
 */
@Composable
fun InvestmentScreen(onOpenMenu: () -> Unit) {
    // Saveable, so the mode and typed values survive the app being
    // killed in the background.
    var mode by rememberSaveable { mutableStateOf(InvestmentMode.Sip) }
    var amount by rememberSaveable { mutableStateOf("5000") }
    var rate by rememberSaveable { mutableStateOf("12") }
    var years by rememberSaveable { mutableStateOf("10") }
    var cagrStart by rememberSaveable { mutableStateOf("100000") }
    var cagrEnd by rememberSaveable { mutableStateOf("200000") }
    var cagrYears by rememberSaveable { mutableStateOf("5") }
    val lumpSum = mode == InvestmentMode.LumpSum
    val input =
        remember(mode, amount, rate, years, cagrStart, cagrEnd, cagrYears) {
            if (mode == InvestmentMode.Cagr) {
                cagrInput(cagrStart, cagrEnd, cagrYears)
            } else {
                investmentInput(lumpSum, amount, rate, years)
            }
        }
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
                    stringResource(R.string.investment_mode_cagr),
                ),
                selectedIndex = mode.ordinal,
                onSelect = { mode = InvestmentMode.entries[it] },
            )
            if (mode == InvestmentMode.Cagr) {
                LifeCalcNumberField(
                    label = stringResource(R.string.investment_start),
                    value = cagrStart,
                    onValueChange = { cagrStart = it },
                    error = input.errorFor(R.string.investment_start),
                )
                LifeCalcNumberField(
                    label = stringResource(R.string.investment_end),
                    value = cagrEnd,
                    onValueChange = { cagrEnd = it },
                    error = input.errorFor(R.string.investment_end),
                )
            } else {
                val amountLabel = investmentAmountLabel(lumpSum)
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
            }
            LifeCalcNumberField(
                label = stringResource(R.string.investment_years),
                value = if (mode == InvestmentMode.Cagr) cagrYears else years,
                onValueChange = { if (mode == InvestmentMode.Cagr) cagrYears = it else years = it },
                suffix = stringResource(R.string.investment_years_suffix),
                error = input.errorFor(R.string.investment_years),
            )
        }

        LifeCalcCard {
            LifeCalcSectionLabel(stringResource(R.string.investment_section_result))
            when (input) {
                is InputResult.Invalid ->
                    Text(
                        text =
                            stringResource(
                                if (mode == InvestmentMode.Cagr) {
                                    R.string.investment_cagr_error_invalid
                                } else {
                                    R.string.investment_error_invalid
                                },
                            ),
                        style = MaterialTheme.typography.bodyMedium,
                        color = scheme.error,
                    )
                is InputResult.Ok ->
                    when (val result = input.value) {
                        is CagrResult -> CagrRows(result)
                        is InvestmentResult -> MaturityRows(result)
                    }
            }
        }
    }
}

@Composable
private fun MaturityRows(result: InvestmentResult) {
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

@Composable
private fun CagrRows(result: CagrResult) {
    LifeCalcOutputRow(
        label = stringResource(R.string.investment_absolute_return),
        value = NumberFormatter.percent(result.absoluteReturnPercent),
    )
    LifeCalcOutputRow(
        label = stringResource(R.string.investment_gain),
        value = NumberFormatter.money(result.gain),
    )
    LifeCalcOutputRow(
        label = stringResource(R.string.investment_cagr),
        value = NumberFormatter.percent(result.cagrPercent),
        accent = true,
    )
}
