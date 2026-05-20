package com.calculator.feature.finance.investment

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import com.calculator.R
import com.calculator.core.common.format.NumberFormatter
import com.calculator.core.domain.finance.InvestmentCalculator
import com.calculator.feature.lifecalc.LifeCalcCard
import com.calculator.feature.lifecalc.LifeCalcNumberField
import com.calculator.feature.lifecalc.LifeCalcOutputRow
import com.calculator.feature.lifecalc.LifeCalcSectionLabel
import com.calculator.feature.lifecalc.LifeCalcSegmented
import com.calculator.feature.lifecalc.LifeCalculatorScaffold
import com.calculator.navigation.InvestmentRoute
import java.util.Locale

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
 * @param onNavigate Jump to another tool / home.
 */
@Composable
fun InvestmentScreen(onNavigate: (Any) -> Unit) {
    var mode by remember { mutableIntStateOf(0) } // 0 = SIP, 1 = lump-sum
    var amount by remember { mutableStateOf("5000") }
    var rate by remember { mutableStateOf("12") }
    var years by remember { mutableStateOf("10") }

    val result =
        runCatching {
            val a = amount.toDouble()
            val r = rate.toDouble()
            val y = years.toDouble()
            if (mode == 0) {
                InvestmentCalculator.sip(a, r, y)
            } else {
                InvestmentCalculator.lumpSum(a, r, y, timesPerYear = 1)
            }
        }.getOrNull()

    LifeCalculatorScaffold(
        title = stringResource(R.string.investment_title),
        currentRoute = InvestmentRoute,
        onNavigate = onNavigate,
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
                label = stringResource(
                    if (mode == 0) R.string.investment_monthly else R.string.investment_principal,
                ),
                value = amount,
                onValueChange = { amount = it },
            )
            LifeCalcNumberField(
                label = stringResource(R.string.investment_rate),
                value = rate,
                onValueChange = { rate = it },
                suffix = "%",
            )
            LifeCalcNumberField(
                label = stringResource(R.string.investment_years),
                value = years,
                onValueChange = { years = it },
                suffix = stringResource(R.string.investment_years_suffix),
            )
        }

        LifeCalcCard {
            LifeCalcSectionLabel(stringResource(R.string.investment_section_result))
            if (result == null) {
                Text(
                    text = stringResource(R.string.investment_error_invalid),
                    style = MaterialTheme.typography.bodyMedium,
                    color = Color.White.copy(alpha = 0.55f),
                )
            } else {
                LifeCalcOutputRow(
                    label = stringResource(R.string.investment_invested),
                    value = money(result.invested),
                )
                LifeCalcOutputRow(
                    label = stringResource(R.string.investment_gains),
                    value = money(result.gains),
                )
                LifeCalcOutputRow(
                    label = stringResource(R.string.investment_maturity),
                    value = money(result.maturity),
                    accent = true,
                )
            }
        }
    }
}

private fun money(value: Double): String =
    NumberFormatter.money(value, Locale.getDefault())
