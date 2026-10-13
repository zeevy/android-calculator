package com.calculator.feature.math.percent

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
 * @param onOpenMenu Opens the app's tools menu.
 */
@Composable
fun PercentScreen(onOpenMenu: () -> Unit) {
    // Saveable, so the mode and typed values survive the app being
    // killed in the background.
    var mode by rememberSaveable { mutableStateOf(PercentMode.PercentOf) }
    var a by rememberSaveable { mutableStateOf("20") }
    var b by rememberSaveable { mutableStateOf("250") }
    val input = remember(mode, a, b) { percentInput(mode, a, b) }
    val scheme = MaterialTheme.colorScheme

    LifeCalculatorScaffold(
        title = stringResource(R.string.percent_title),
        onOpenMenu = onOpenMenu,
    ) {
        LifeCalcCard {
            LifeCalcSectionLabel(stringResource(R.string.percent_section_mode))
            LifeCalcSegmented(
                options = listOf(
                    stringResource(R.string.percent_mode_of),
                    stringResource(R.string.percent_mode_what),
                    stringResource(R.string.percent_mode_change),
                ),
                selectedIndex = mode.ordinal,
                onSelect = { mode = PercentMode.entries[it] },
            )
            // Input labels change with mode; the value strings stay so
            // the user can flip between modes and keep their numbers.
            val (labelA, labelB) = percentLabels(mode)
            LifeCalcNumberField(
                label = stringResource(labelA),
                value = a,
                onValueChange = { a = it },
                suffix = if (mode == PercentMode.PercentOf) "%" else null,
                error = input.errorFor(labelA),
            )
            LifeCalcNumberField(
                label = stringResource(labelB),
                value = b,
                onValueChange = { b = it },
                error = input.errorFor(labelB),
            )
        }

        LifeCalcCard {
            LifeCalcSectionLabel(stringResource(R.string.percent_section_result))
            when (input) {
                is InputResult.Invalid ->
                    Text(
                        text = stringResource(R.string.percent_error_invalid),
                        style = MaterialTheme.typography.bodyMedium,
                        color = scheme.error,
                    )
                is InputResult.Ok -> {
                    val result = input.value
                    val (label, formatted) =
                        when (mode) {
                            PercentMode.PercentOf -> stringResource(R.string.percent_result_value) to plain(result)
                            PercentMode.WhatPercent -> stringResource(R.string.percent_result_percent) to "${plain(result)} %"
                            PercentMode.Change -> {
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
}

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
