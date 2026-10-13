package com.calculator.feature.finance.profit

import androidx.annotation.StringRes
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
import com.calculator.core.domain.finance.ProfitResult

/**
 * Profit and markup calculator with three modes. Each takes the cost price
 * plus one more value: the selling price, a markup on cost, or a margin on
 * the selling price. The result card shows the profit (or loss) with the
 * markup and margin, or the selling price they lead to.
 *
 * The cost price is shared by all modes; the selling price, markup and
 * margin are kept apart, so switching modes keeps what was typed in each.
 * Defaults (cost 800, selling 1000, markup 25, margin 20) fill the result
 * card on first open.
 *
 * @param onOpenMenu Opens the app's tools menu.
 */
@Composable
fun ProfitScreen(onOpenMenu: () -> Unit) {
    // Saveable, so the mode and typed values survive the app being
    // killed in the background.
    var mode by rememberSaveable { mutableStateOf(ProfitMode.Prices) }
    var cost by rememberSaveable { mutableStateOf("800") }
    var selling by rememberSaveable { mutableStateOf("1000") }
    var markup by rememberSaveable { mutableStateOf("25") }
    var margin by rememberSaveable { mutableStateOf("20") }
    val second =
        when (mode) {
            ProfitMode.Prices -> selling
            ProfitMode.Markup -> markup
            ProfitMode.Margin -> margin
        }
    val input = remember(mode, cost, second) { profitInput(mode, cost, second) }
    val secondLabel = profitSecondLabel(mode)

    LifeCalculatorScaffold(
        title = stringResource(R.string.profit_title),
        onOpenMenu = onOpenMenu,
    ) {
        LifeCalcCard {
            LifeCalcSectionLabel(stringResource(R.string.profit_section_mode))
            LifeCalcSegmented(
                options = listOf(
                    stringResource(R.string.profit_mode_prices),
                    stringResource(R.string.profit_mode_markup),
                    stringResource(R.string.profit_mode_margin),
                ),
                selectedIndex = mode.ordinal,
                onSelect = { mode = ProfitMode.entries[it] },
            )
            LifeCalcNumberField(
                label = stringResource(R.string.profit_cost),
                value = cost,
                onValueChange = { cost = it },
                error = input.errorFor(R.string.profit_cost),
            )
            LifeCalcNumberField(
                label = stringResource(secondLabel),
                value = second,
                onValueChange = {
                    when (mode) {
                        ProfitMode.Prices -> selling = it
                        ProfitMode.Markup -> markup = it
                        ProfitMode.Margin -> margin = it
                    }
                },
                suffix = if (mode == ProfitMode.Prices) null else "%",
                error = input.errorFor(secondLabel),
            )
        }

        LifeCalcCard {
            LifeCalcSectionLabel(stringResource(R.string.profit_section_result))
            when (input) {
                is InputResult.Invalid ->
                    Text(
                        text = stringResource(R.string.profit_error_invalid),
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.error,
                    )
                is InputResult.Ok ->
                    when {
                        mode != ProfitMode.Prices -> SellingRows(input.value, showMargin = mode == ProfitMode.Markup)
                        input.value.profit < 0 -> LossRows(input.value)
                        else -> ProfitRows(input.value)
                    }
            }
        }
    }
}

@Composable
private fun ProfitRows(result: ProfitResult) {
    result.markupPercent?.let { PercentRow(R.string.profit_markup, it) }
    result.marginPercent?.let { PercentRow(R.string.profit_margin, it) }
    LifeCalcOutputRow(
        label = stringResource(R.string.profit_profit),
        value = NumberFormatter.money(result.profit),
        accent = true,
    )
}

/** A loss is shown as a positive amount under "Loss", never as a negative profit. */
@Composable
private fun LossRows(result: ProfitResult) {
    result.markupPercent?.let { PercentRow(R.string.profit_loss_percent, -it) }
    LifeCalcOutputRow(
        label = stringResource(R.string.profit_loss),
        value = NumberFormatter.money(-result.profit),
        accent = true,
    )
}

/** Rows for the markup and margin modes; the percentage not typed in is shown. */
@Composable
private fun SellingRows(
    result: ProfitResult,
    showMargin: Boolean,
) {
    LifeCalcOutputRow(
        label = stringResource(R.string.profit_profit),
        value = NumberFormatter.money(result.profit),
    )
    if (showMargin) {
        result.marginPercent?.let { PercentRow(R.string.profit_margin, it) }
    } else {
        result.markupPercent?.let { PercentRow(R.string.profit_markup, it) }
    }
    LifeCalcOutputRow(
        label = stringResource(R.string.profit_selling_result),
        value = NumberFormatter.money(result.selling),
        accent = true,
    )
}

@Composable
private fun PercentRow(
    @StringRes label: Int,
    percent: Double,
) {
    LifeCalcOutputRow(label = stringResource(label), value = NumberFormatter.percent(percent))
}
