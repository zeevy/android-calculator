package com.calculator.feature.finance.discount

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
import com.calculator.core.designsystem.component.LifeCalculatorScaffold
import com.calculator.core.designsystem.component.errorFor

/**
 * Discount calculator with three editable fields.
 *
 * MRP is the always-editable reference price. Percent-off and final
 * price are both editable too: whichever the user typed into last is
 * the source, and the OTHER one is recomputed from MRP. Savings is a
 * derived display row.
 *
 * This replaces an earlier explicit "Forward / Reverse" mode toggle
 * the user had to set before typing - dropping that toggle made the
 * flow one-step shorter without giving up any expressiveness.
 *
 * Defaults render a sane example on entry (MRP 2,000, 20% off → 1,600
 * final) so the user immediately sees how the rows relate.
 *
 * @param onOpenMenu Opens the app's tools menu.
 */
@Composable
fun DiscountScreen(onOpenMenu: () -> Unit) {
    // Saveable, so typed values survive the app being killed in the
    // background.
    var mrp by rememberSaveable { mutableStateOf("2000") }
    var percentOff by rememberSaveable { mutableStateOf("20") }
    var finalPrice by rememberSaveable { mutableStateOf("1600") }
    // MRP edits don't change the source; they just recompute the other
    // side from the new MRP.
    var finalIsSource by rememberSaveable { mutableStateOf(false) }
    val input =
        remember(mrp, percentOff, finalPrice, finalIsSource) {
            discountInput(mrp, percentOff, finalPrice, finalIsSource)
        }
    val output = (input as? InputResult.Ok)?.value
    val scheme = MaterialTheme.colorScheme

    LifeCalculatorScaffold(
        title = stringResource(R.string.discount_title),
        onOpenMenu = onOpenMenu,
    ) {
        LifeCalcCard {
            LifeCalcSectionLabel(stringResource(R.string.discount_section_inputs))
            LifeCalcNumberField(
                label = stringResource(R.string.discount_mrp),
                value = mrp,
                onValueChange = { mrp = it },
                error = input.errorFor(R.string.discount_mrp),
            )
            LifeCalcNumberField(
                label = stringResource(R.string.discount_percent),
                value = output?.percentText ?: percentOff,
                onValueChange = {
                    percentOff = it
                    finalIsSource = false
                },
                suffix = stringResource(R.string.discount_percent_suffix),
                error = input.errorFor(R.string.discount_percent),
            )
            LifeCalcNumberField(
                label = stringResource(R.string.discount_final),
                value = output?.finalText ?: finalPrice,
                onValueChange = {
                    finalPrice = it
                    finalIsSource = true
                },
                error = input.errorFor(R.string.discount_final),
            )
        }

        LifeCalcCard {
            LifeCalcSectionLabel(stringResource(R.string.discount_section_result))
            if (output == null) {
                Text(
                    text = stringResource(R.string.discount_error_invalid),
                    style = MaterialTheme.typography.bodyMedium,
                    color = scheme.error,
                )
            } else {
                LifeCalcOutputRow(
                    label = stringResource(R.string.discount_savings),
                    value = NumberFormatter.money(output.result.savings),
                    accent = true,
                )
            }
        }
    }
}
