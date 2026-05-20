package com.calculator.feature.math.percent

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
import com.calculator.core.domain.math.PercentCalculator
import com.calculator.feature.lifecalc.LifeCalcCard
import com.calculator.feature.lifecalc.LifeCalcNumberField
import com.calculator.feature.lifecalc.LifeCalcOutputRow
import com.calculator.feature.lifecalc.LifeCalcSectionLabel
import com.calculator.feature.lifecalc.LifeCalcSegmented
import com.calculator.feature.lifecalc.LifeCalculatorScaffold
import com.calculator.navigation.PercentRoute
import java.util.Locale
import kotlin.math.absoluteValue

/**
 * Three classic percentage scenarios behind a single segmented control:
 *
 *  - Mode 0 ("X% of Y"): scale a value by a percent.
 *  - Mode 1 ("X of Y is what %"): derive the percent.
 *  - Mode 2 ("% change A → B"): percent change between two values.
 *
 * Two text fields adjust to the mode (labels change), one accent-coloured
 * output row reports the answer. Defaults pre-seed each mode with
 * representative numbers so the result line is never blank.
 *
 * @param onNavigate Jump to another tool / home.
 */
@Composable
fun PercentScreen(onNavigate: (Any) -> Unit) {
    var mode by remember { mutableIntStateOf(0) }
    var a by remember { mutableStateOf("20") }
    var b by remember { mutableStateOf("250") }

    // Compute lazily per recompose - inputs are small and parsing
    // cheap. runCatching swallows NaN / divide-by-zero (require()
    // throwing IllegalArgumentException), surfaced via a null result.
    val result =
        runCatching {
            val x = a.toDouble()
            val y = b.toDouble()
            when (mode) {
                0 -> PercentCalculator.percentOf(x, y)
                1 -> PercentCalculator.whatPercent(x, y)
                else -> PercentCalculator.percentChange(x, y)
            }
        }.getOrNull()

    LifeCalculatorScaffold(
        title = stringResource(R.string.percent_title),
        currentRoute = PercentRoute,
        onNavigate = onNavigate,
    ) {
        LifeCalcCard {
            LifeCalcSectionLabel(stringResource(R.string.percent_section_mode))
            LifeCalcSegmented(
                options = listOf(
                    stringResource(R.string.percent_mode_of),
                    stringResource(R.string.percent_mode_what),
                    stringResource(R.string.percent_mode_change),
                ),
                selectedIndex = mode,
                onSelect = { mode = it },
            )
            // Input labels change with mode; the value strings stay so
            // the user can flip between modes and keep their numbers.
            val (labelA, suffixA, labelB, suffixB) =
                when (mode) {
                    0 ->
                        Quad(
                            stringResource(R.string.percent_label_percent),
                            "%",
                            stringResource(R.string.percent_label_value),
                            null,
                        )
                    1 ->
                        Quad(
                            stringResource(R.string.percent_label_part),
                            null,
                            stringResource(R.string.percent_label_whole),
                            null,
                        )
                    else ->
                        Quad(
                            stringResource(R.string.percent_label_from),
                            null,
                            stringResource(R.string.percent_label_to),
                            null,
                        )
                }
            LifeCalcNumberField(
                label = labelA,
                value = a,
                onValueChange = { a = it },
                suffix = suffixA,
            )
            LifeCalcNumberField(
                label = labelB,
                value = b,
                onValueChange = { b = it },
                suffix = suffixB,
            )
        }

        LifeCalcCard {
            LifeCalcSectionLabel(stringResource(R.string.percent_section_result))
            if (result == null || result.isNaN() || result.isInfinite()) {
                Text(
                    text = stringResource(R.string.percent_error_invalid),
                    style = MaterialTheme.typography.bodyMedium,
                    color = Color.White.copy(alpha = 0.55f),
                )
            } else {
                val (label, formatted) =
                    when (mode) {
                        0 -> stringResource(R.string.percent_result_value) to plain(result)
                        1 -> stringResource(R.string.percent_result_percent) to "${plain(result)} %"
                        else -> {
                            // Lead the change value with an explicit
                            // +/- so direction reads at a glance.
                            val sign = if (result >= 0) "+" else "-"
                            stringResource(R.string.percent_result_change) to
                                "$sign${plain(result.absoluteValue)} %"
                        }
                    }
                LifeCalcOutputRow(label = label, value = formatted, accent = true)
            }
        }
    }
}

/** Tiny 4-tuple used to bundle field label/suffix for both inputs. */
private data class Quad(
    val labelA: String,
    val suffixA: String?,
    val labelB: String,
    val suffixB: String?,
)

// Plain-number formatter for the result: trims trailing .0 on whole
// numbers, otherwise keeps two decimal places. Money formatting would
// be wrong here - percentages and free-form values aren't currency.
private fun plain(value: Double): String =
    NumberFormatter.format(
        value = value,
        locale = Locale.getDefault(),
        minFractionDigits = 0,
        maxFractionDigits = 2,
    )
