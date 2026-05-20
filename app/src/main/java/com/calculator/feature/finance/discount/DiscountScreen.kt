package com.calculator.feature.finance.discount

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
import com.calculator.core.domain.finance.DiscountCalculator
import com.calculator.feature.lifecalc.LifeCalcCard
import com.calculator.feature.lifecalc.LifeCalcNumberField
import com.calculator.feature.lifecalc.LifeCalcOutputRow
import com.calculator.feature.lifecalc.LifeCalcSectionLabel
import com.calculator.feature.lifecalc.LifeCalcSegmented
import com.calculator.feature.lifecalc.LifeCalculatorScaffold
import java.util.Locale

/**
 * Two-way discount calculator.
 *
 * **Forward** (mode 0): MRP + percent off → savings + final price.
 * **Reverse** (mode 1): MRP + paid price → savings + effective percent.
 *
 * Defaults render a sane example on entry (MRP 2,000, 20% off → 1,600
 * final) so the user immediately sees how the rows relate.
 *
 * @param onUp Pop the calculator from the back stack.
 */
@Composable
fun DiscountScreen(onUp: () -> Unit) {
    // Both inputs are kept in state simultaneously rather than being
    // derived from each other - flipping mode preserves whatever the
    // user last typed in the other branch instead of overwriting it.
    var mode by remember { mutableIntStateOf(0) } // 0=forward, 1=reverse
    var mrp by remember { mutableStateOf("2000") }
    var percentOff by remember { mutableStateOf("20") }
    var finalPrice by remember { mutableStateOf("1500") }

    LifeCalculatorScaffold(title = stringResource(R.string.discount_title), onUp = onUp) {
        LifeCalcCard {
            LifeCalcSectionLabel(stringResource(R.string.discount_section_mode))
            LifeCalcSegmented(
                options = listOf(
                    stringResource(R.string.discount_mode_forward),
                    stringResource(R.string.discount_mode_reverse),
                ),
                selectedIndex = mode,
                onSelect = { mode = it },
            )
            LifeCalcNumberField(
                label = stringResource(R.string.discount_mrp),
                value = mrp,
                onValueChange = { mrp = it },
            )
            if (mode == 0) {
                LifeCalcNumberField(
                    label = stringResource(R.string.discount_percent),
                    value = percentOff,
                    onValueChange = { percentOff = it },
                    suffix = stringResource(R.string.discount_percent_suffix),
                )
            } else {
                LifeCalcNumberField(
                    label = stringResource(R.string.discount_final),
                    value = finalPrice,
                    onValueChange = { finalPrice = it },
                )
            }
        }

        val result =
            runCatching {
                if (mode == 0) {
                    DiscountCalculator.forward(mrp.toDouble(), percentOff.toDouble())
                } else {
                    DiscountCalculator.reverse(mrp.toDouble(), finalPrice.toDouble())
                }
            }.getOrNull()

        LifeCalcCard {
            LifeCalcSectionLabel(stringResource(R.string.discount_section_result))
            if (result == null) {
                Text(
                    text = stringResource(R.string.discount_error_invalid),
                    style = MaterialTheme.typography.bodyMedium,
                    color = Color.White.copy(alpha = 0.55f),
                )
            } else {
                LifeCalcOutputRow(
                    label = stringResource(R.string.discount_savings),
                    value = money(result.savings),
                )
                LifeCalcOutputRow(
                    label = stringResource(R.string.discount_final),
                    value = money(result.finalPrice),
                    accent = true,
                )
                LifeCalcOutputRow(
                    label = stringResource(R.string.discount_percent),
                    value = stringResource(
                        R.string.discount_value_percent_format,
                        percent(result.percentOff),
                    ),
                )
            }
        }
    }
}

// Money formatter routes through [NumberFormatter.money] for locale-
// aware grouping (en-IN lakh, de-DE swapped separators, etc.).
private fun money(value: Double): String =
    NumberFormatter.money(value, Locale.getDefault())

// Percent shows up to two decimals only when the trailing fractional
// part is non-zero ("20" not "20.00", but "12.34" stays). Min-fraction
// 0 lets [NumberFormatter] omit the decimal point on whole percents.
private fun percent(value: Double): String =
    NumberFormatter.format(
        value = value,
        locale = Locale.getDefault(),
        minFractionDigits = 0,
        maxFractionDigits = 2,
    )
