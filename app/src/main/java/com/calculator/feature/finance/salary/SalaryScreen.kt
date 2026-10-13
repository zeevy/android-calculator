package com.calculator.feature.finance.salary

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
import com.calculator.core.domain.finance.SalaryChange
import com.calculator.core.domain.finance.SalaryHikeResult

/**
 * Salary hike calculator with two modes: the new salary after a hike of
 * a given percent, and the hike percent between an old and a new salary.
 *
 * In the new salary mode the salary can be typed per month or per year;
 * the result shows both. Each mode keeps what was typed in it.
 *
 * Defaults: ₹6,00,000 a year with a 10% hike, and ₹6,00,000 to
 * ₹6,60,000, so the result card is filled on first render. Yearly is the
 * default period because Indian salaries are usually quoted per year.
 *
 * @param onOpenMenu Opens the app's tools menu.
 */
@Composable
fun SalaryScreen(onOpenMenu: () -> Unit) {
    // Saveable, so the mode and typed values survive the app being
    // killed in the background.
    var mode by rememberSaveable { mutableStateOf(SalaryMode.NewSalary) }
    var current by rememberSaveable { mutableStateOf("600000") }
    var hike by rememberSaveable { mutableStateOf("10") }
    var yearly by rememberSaveable { mutableStateOf(true) }
    var oldSalary by rememberSaveable { mutableStateOf("600000") }
    var newSalary by rememberSaveable { mutableStateOf("660000") }
    val input =
        remember(mode, current, hike, yearly, oldSalary, newSalary) {
            if (mode == SalaryMode.NewSalary) newSalaryInput(current, hike, yearly) else hikeInput(oldSalary, newSalary)
        }

    LifeCalculatorScaffold(
        title = stringResource(R.string.salary_title),
        onOpenMenu = onOpenMenu,
    ) {
        LifeCalcCard {
            LifeCalcSectionLabel(stringResource(R.string.salary_section_mode))
            LifeCalcSegmented(
                options = listOf(stringResource(R.string.salary_mode_new), stringResource(R.string.salary_mode_hike)),
                selectedIndex = mode.ordinal,
                onSelect = { mode = SalaryMode.entries[it] },
            )
            if (mode == SalaryMode.NewSalary) {
                LifeCalcSectionLabel(stringResource(R.string.salary_section_period))
                LifeCalcSegmented(
                    options =
                        listOf(stringResource(R.string.salary_period_monthly), stringResource(R.string.salary_period_yearly)),
                    selectedIndex = if (yearly) 1 else 0,
                    onSelect = { yearly = it == 1 },
                )
                LifeCalcNumberField(
                    label = stringResource(R.string.salary_current),
                    value = current,
                    onValueChange = { current = it },
                    error = input.errorFor(R.string.salary_current),
                )
                LifeCalcNumberField(
                    label = stringResource(R.string.salary_hike_input),
                    value = hike,
                    onValueChange = { hike = it },
                    suffix = "%",
                    error = input.errorFor(R.string.salary_hike_input),
                )
            } else {
                LifeCalcNumberField(
                    label = stringResource(R.string.salary_old),
                    value = oldSalary,
                    onValueChange = { oldSalary = it },
                    error = input.errorFor(R.string.salary_old),
                )
                LifeCalcNumberField(
                    label = stringResource(R.string.salary_new),
                    value = newSalary,
                    onValueChange = { newSalary = it },
                    error = input.errorFor(R.string.salary_new),
                )
            }
        }

        LifeCalcCard {
            LifeCalcSectionLabel(stringResource(R.string.salary_section_result))
            when (input) {
                is InputResult.Invalid ->
                    Text(
                        text = stringResource(R.string.salary_error_invalid),
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.error,
                    )
                is InputResult.Ok ->
                    when (val result = input.value) {
                        is SalaryHikeResult -> NewSalaryRows(result, yearly)
                        is SalaryChange -> HikeRows(result)
                    }
            }
        }
    }
}

/** The headline is the new salary in the period the salary was typed in. */
@Composable
private fun NewSalaryRows(
    result: SalaryHikeResult,
    yearly: Boolean,
) {
    LifeCalcOutputRow(
        label = stringResource(R.string.salary_new_monthly),
        value = NumberFormatter.money(result.newMonthly),
        accent = !yearly,
    )
    LifeCalcOutputRow(
        label = stringResource(R.string.salary_new_yearly),
        value = NumberFormatter.money(result.newYearly),
        accent = yearly,
    )
    LifeCalcOutputRow(
        label = stringResource(R.string.salary_increase_monthly),
        value = NumberFormatter.money(result.increaseMonthly),
    )
    LifeCalcOutputRow(
        label = stringResource(R.string.salary_increase_yearly),
        value = NumberFormatter.money(result.increaseYearly),
    )
}

@Composable
private fun HikeRows(result: SalaryChange) {
    LifeCalcOutputRow(
        label = stringResource(R.string.salary_increase),
        value = NumberFormatter.money(result.increase),
    )
    LifeCalcOutputRow(
        label = stringResource(R.string.salary_hike_result),
        value = NumberFormatter.percent(result.hikePercent),
        accent = true,
    )
}
