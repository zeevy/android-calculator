package com.calculator.feature.health.ovulation

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Slider
import androidx.compose.material3.SliderDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.calculator.R
import com.calculator.core.designsystem.component.DateField
import com.calculator.core.designsystem.component.LifeCalcCard
import com.calculator.core.designsystem.component.LifeCalcSectionLabel
import com.calculator.core.designsystem.component.LifeCalculatorScaffold
import com.calculator.core.domain.health.OvulationCalculator
import java.time.LocalDate
import java.time.format.DateTimeFormatter

/**
 * Cycle estimator UI. Wraps [OvulationCalculator].
 *
 * Inputs:
 *  - Last menstrual period (LMP) date - picked via the M3 DatePicker
 *    dialog.
 *  - Average cycle length, default 28, slider clamped to the
 *    [OvulationCalculator.MIN_CYCLE_DAYS] .. [OvulationCalculator.MAX_CYCLE_DAYS]
 *    range (so the input can never raise the IllegalArgumentException
 *    the calculator throws for out-of-band values).
 *
 * Outputs: predicted ovulation date, fertile window, next period,
 * estimated due date. A footer disclaimer is shown unconditionally
 * because the figures are statistical and the screen must not read as
 * either medical advice or a contraception tool.
 *
 * @param onOpenMenu Opens the app's tools menu.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun OvulationScreen(onOpenMenu: () -> Unit) {
    // Seed the LMP to ~two weeks ago so on first open the user sees a
    // populated result roughly centred on today's date. They will replace
    // it with their actual LMP via the picker.
    val scheme = MaterialTheme.colorScheme
    var lmp by rememberSaveable { mutableStateOf(LocalDate.now().minusDays(DEFAULT_LMP_OFFSET_DAYS)) }
    var cycleDays by rememberSaveable { mutableIntStateOf(OvulationCalculator.DEFAULT_CYCLE_DAYS) }

    LifeCalculatorScaffold(
        title = stringResource(R.string.ovulation_title),
        onOpenMenu = onOpenMenu,
    ) {
        LifeCalcCard {
            LifeCalcSectionLabel(stringResource(R.string.ovulation_lmp))
            DateField(date = lmp, onDateChange = { lmp = it })

            LifeCalcSectionLabel(stringResource(R.string.ovulation_cycle))
            Text(
                text = stringResource(R.string.ovulation_cycle_days_format, cycleDays),
                style = MaterialTheme.typography.titleLarge,
                color = scheme.primary,
            )
            Slider(
                value = cycleDays.toFloat(),
                onValueChange = { cycleDays = it.toInt() },
                valueRange =
                    OvulationCalculator.MIN_CYCLE_DAYS.toFloat()..OvulationCalculator.MAX_CYCLE_DAYS.toFloat(),
                steps =
                    OvulationCalculator.MAX_CYCLE_DAYS -
                        OvulationCalculator.MIN_CYCLE_DAYS - 1,
                colors =
                    SliderDefaults.colors(
                        thumbColor = scheme.primary,
                        activeTrackColor = scheme.primary,
                        inactiveTrackColor = scheme.onSurface.copy(alpha = 0.2f),
                        activeTickColor = Color.Transparent,
                        inactiveTickColor = Color.Transparent,
                    ),
                modifier = Modifier.padding(top = 4.dp),
            )
        }

        // The last period cannot start in the future.
        val valid =
            !lmp.isAfter(LocalDate.now()) &&
                cycleDays in OvulationCalculator.MIN_CYCLE_DAYS..OvulationCalculator.MAX_CYCLE_DAYS
        val result = if (valid) OvulationCalculator.compute(lmp, cycleDays) else null
        LifeCalcCard {
            LifeCalcSectionLabel(stringResource(R.string.ovulation_section_estimates))
            if (result == null) {
                Text(
                    text = stringResource(R.string.ovulation_error_invalid),
                    style = MaterialTheme.typography.bodyMedium,
                    color = scheme.error,
                )
            } else {
                // Stacked rows: weekday + full date strings (and the
                // fertile-window range "Tue, 19 May - Sun, 24 May 2026")
                // are too wide to share a line with their labels without
                // wrapping awkwardly, so each value sits below its label.
                StackedOutput(
                    label = stringResource(R.string.ovulation_ovulation_date),
                    value = format(result.ovulation),
                    accent = true,
                )
                StackedOutput(
                    label = stringResource(R.string.ovulation_fertile_window),
                    value = stringResource(
                        R.string.ovulation_fertile_window_format,
                        format(result.fertileStart),
                        format(result.fertileEnd),
                    ),
                )
                UpcomingPeriods(
                    label = stringResource(R.string.ovulation_upcoming_periods),
                    dates = result.upcomingPeriods,
                )
                StackedOutput(
                    label = stringResource(R.string.ovulation_due_date),
                    value = format(result.estimatedDueDate),
                )
            }
        }

        Text(
            text = stringResource(R.string.ovulation_disclaimer),
            style = MaterialTheme.typography.bodySmall,
            color = scheme.onSurfaceVariant,
            modifier = Modifier.padding(horizontal = 4.dp),
        )
    }
}

// Human-friendly format: weekday, day, short month, year (e.g.
// "Tue, 19 May 2026"). The leading weekday matters for next-period and
// ovulation rows because users frequently want to know what day of the
// week to plan around.
private fun format(date: LocalDate): String =
    date.format(DateTimeFormatter.ofPattern("EEE, d MMM yyyy"))

/**
 * The next few projected period start dates, one per row under a shared
 * label. The soonest (first) date is highlighted in the accent colour
 * since it's the immediate next period; the rest are the ~3-month
 * projection.
 */
@Composable
private fun UpcomingPeriods(label: String, dates: List<LocalDate>) {
    val scheme = MaterialTheme.colorScheme
    Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
        Text(
            text = label,
            style = MaterialTheme.typography.bodyLarge,
            color = scheme.onSurfaceVariant,
        )
        dates.forEachIndexed { index, date ->
            Text(
                text = format(date),
                style = MaterialTheme.typography.titleLarge.copy(
                    fontWeight = if (index == 0) FontWeight.Bold else FontWeight.SemiBold,
                ),
                color = if (index == 0) scheme.primary else scheme.onSurface,
            )
        }
    }
}

/**
 * Label-above-value output row. Used for the result card here because
 * the date strings include the weekday and don't fit cleanly to the
 * right of their labels on a phone-width line.
 */
@Composable
private fun StackedOutput(label: String, value: String, accent: Boolean = false) {
    val scheme = MaterialTheme.colorScheme
    Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
        Text(
            text = label,
            style = MaterialTheme.typography.bodyLarge,
            color = scheme.onSurfaceVariant,
        )
        Text(
            text = value,
            style = MaterialTheme.typography.titleLarge.copy(
                fontWeight = if (accent) FontWeight.Bold else FontWeight.SemiBold,
            ),
            color = if (accent) scheme.primary else scheme.onSurface,
        )
    }
}

// Default seed of "two weeks ago" gives the chart a sensible starting
// position centred around an average ovulation date.
private const val DEFAULT_LMP_OFFSET_DAYS = 14L
