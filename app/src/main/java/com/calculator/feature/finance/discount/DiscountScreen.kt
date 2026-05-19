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
import com.calculator.core.domain.finance.DiscountCalculator
import com.calculator.feature.lifecalc.LifeCalcCard
import com.calculator.feature.lifecalc.LifeCalcNumberField
import com.calculator.feature.lifecalc.LifeCalcOutputRow
import com.calculator.feature.lifecalc.LifeCalcSectionLabel
import com.calculator.feature.lifecalc.LifeCalcSegmented
import com.calculator.feature.lifecalc.LifeCalculatorScaffold
import java.text.DecimalFormat
import java.text.DecimalFormatSymbols
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

    LifeCalculatorScaffold(title = "Discount", onUp = onUp) {
        LifeCalcCard {
            LifeCalcSectionLabel("Mode")
            LifeCalcSegmented(
                options = listOf("MRP + % off", "MRP + final price"),
                selectedIndex = mode,
                onSelect = { mode = it },
            )
            LifeCalcNumberField("MRP", mrp, { mrp = it })
            if (mode == 0) {
                LifeCalcNumberField("Discount", percentOff, { percentOff = it }, suffix = "%")
            } else {
                LifeCalcNumberField("Final price", finalPrice, { finalPrice = it })
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
            LifeCalcSectionLabel("Result")
            if (result == null) {
                Text(
                    text = "Enter valid numbers (final must be in 0..MRP).",
                    style = MaterialTheme.typography.bodyMedium,
                    color = Color.White.copy(alpha = 0.55f),
                )
            } else {
                LifeCalcOutputRow("Savings", money.format(result.savings))
                LifeCalcOutputRow("Final price", money.format(result.finalPrice), accent = true)
                LifeCalcOutputRow("Discount", "${percent.format(result.percentOff)} %")
            }
        }
    }
}

// Fixed 2-decimal money format - sane default for any currency. Lazy
// because DecimalFormat instances are not thread-safe and constructing
// one is cheap relative to keeping a singleton; lazy + by-property
// avoids the warm-up cost on screens that never render the discount.
private val money: DecimalFormat by lazy {
    DecimalFormat("#,##0.00", DecimalFormatSymbols(Locale.US))
}

// Percent shows up to two decimals only when the trailing fractional
// part is non-zero ("20" not "20.00", but "12.34" stays). The `##`
// suffix is DecimalFormat shorthand for "include if significant".
private val percent: DecimalFormat by lazy {
    DecimalFormat("#,##0.##", DecimalFormatSymbols(Locale.US))
}
