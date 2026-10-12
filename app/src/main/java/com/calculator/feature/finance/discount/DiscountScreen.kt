package com.calculator.feature.finance.discount

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.res.stringResource
import com.calculator.R
import com.calculator.core.common.format.NumberFormatter
import com.calculator.core.designsystem.component.LifeCalcCard
import com.calculator.core.designsystem.component.LifeCalcNumberField
import com.calculator.core.designsystem.component.LifeCalcOutputRow
import com.calculator.core.designsystem.component.LifeCalcSectionLabel
import com.calculator.core.designsystem.component.LifeCalculatorScaffold
import com.calculator.core.domain.finance.DiscountCalculator

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
    var mrp by remember { mutableStateOf("2000") }
    var percentOff by remember { mutableStateOf("20") }
    var finalPrice by remember { mutableStateOf("1600") }
    // 0 = user is editing the percent field, 1 = user is editing the
    // final-price field. MRP edits don't flip this - they just
    // recompute the non-source side from the new MRP.
    var lastEdited by remember { mutableIntStateOf(0) }
    val scheme = MaterialTheme.colorScheme

    val result =
        runCatching {
            val mrpValue = requireNotNull(NumberFormatter.parseNumberInput(mrp))
            if (lastEdited == 0) {
                DiscountCalculator.forward(mrpValue, requireNotNull(NumberFormatter.parseNumberInput(percentOff)))
            } else {
                DiscountCalculator.reverse(mrpValue, requireNotNull(NumberFormatter.parseNumberInput(finalPrice)))
            }
        }.getOrNull()

    // Mirror the recomputed side into its text-field state so the user
    // sees the formatted value (until they tap into that field, at
    // which point lastEdited flips and the roles swap).
    val percentDisplay =
        if (lastEdited == 0) percentOff else result?.percentOff?.let { NumberFormatter.formatForInput(it) } ?: percentOff
    val finalDisplay =
        if (lastEdited == 1) finalPrice else result?.finalPrice?.let { NumberFormatter.formatForInput(it) } ?: finalPrice

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
            )
            LifeCalcNumberField(
                label = stringResource(R.string.discount_percent),
                value = percentDisplay,
                onValueChange = {
                    percentOff = it
                    lastEdited = 0
                },
                suffix = stringResource(R.string.discount_percent_suffix),
            )
            LifeCalcNumberField(
                label = stringResource(R.string.discount_final),
                value = finalDisplay,
                onValueChange = {
                    finalPrice = it
                    lastEdited = 1
                },
            )
        }

        LifeCalcCard {
            LifeCalcSectionLabel(stringResource(R.string.discount_section_result))
            if (result == null) {
                Text(
                    text = stringResource(R.string.discount_error_invalid),
                    style = MaterialTheme.typography.bodyMedium,
                    color = scheme.error,
                )
            } else {
                LifeCalcOutputRow(
                    label = stringResource(R.string.discount_savings),
                    value = NumberFormatter.money(result.savings),
                    accent = true,
                )
            }
        }
    }
}
