package com.calculator.feature.health.ovulation

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.DatePicker
import androidx.compose.material3.DatePickerDialog
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Slider
import androidx.compose.material3.SliderDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.rememberDatePickerState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.calculator.R
import com.calculator.core.domain.health.OvulationCalculator
import com.calculator.feature.datetime.age.DateRow
import com.calculator.feature.lifecalc.LifeCalcAccent
import com.calculator.feature.lifecalc.LifeCalcCard
import com.calculator.feature.lifecalc.LifeCalcSectionLabel
import com.calculator.feature.lifecalc.LifeCalculatorScaffold
import com.calculator.navigation.OvulationRoute
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneOffset
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
 * @param onNavigate Jump to another tool / home. Wired to the scaffold's
 *   hamburger menu.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun OvulationScreen(onNavigate: (Any) -> Unit) {
    // Seed the LMP to ~two weeks ago so on first open the user sees a
    // populated result roughly centred on today's date. They will replace
    // it with their actual LMP via the picker.
    var lmp by remember { mutableStateOf(LocalDate.now().minusDays(DEFAULT_LMP_OFFSET_DAYS)) }
    var cycleDays by remember { mutableIntStateOf(OvulationCalculator.DEFAULT_CYCLE_DAYS) }
    var pickerOpen by remember { mutableStateOf(false) }

    LifeCalculatorScaffold(
        title = stringResource(R.string.ovulation_title),
        currentRoute = OvulationRoute,
        onNavigate = onNavigate,
    ) {
        LifeCalcCard {
            LifeCalcSectionLabel(stringResource(R.string.ovulation_lmp))
            DateRow(date = lmp, onClick = { pickerOpen = true })

            LifeCalcSectionLabel(stringResource(R.string.ovulation_cycle))
            Text(
                text = stringResource(R.string.ovulation_cycle_days_format, cycleDays),
                style = MaterialTheme.typography.titleLarge,
                color = LifeCalcAccent,
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
                        thumbColor = LifeCalcAccent,
                        activeTrackColor = LifeCalcAccent,
                        inactiveTrackColor = Color.White.copy(alpha = 0.2f),
                        activeTickColor = Color.Transparent,
                        inactiveTickColor = Color.Transparent,
                    ),
                modifier = Modifier.padding(top = 4.dp),
            )
        }

        val result = runCatching { OvulationCalculator.compute(lmp, cycleDays) }.getOrNull()
        LifeCalcCard {
            LifeCalcSectionLabel(stringResource(R.string.ovulation_section_estimates))
            if (result == null) {
                Text(
                    text = stringResource(R.string.ovulation_error_invalid),
                    style = MaterialTheme.typography.bodyMedium,
                    color = Color.White.copy(alpha = 0.55f),
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
                StackedOutput(
                    label = stringResource(R.string.ovulation_upcoming_periods),
                    // One formatted date per line; the first is the
                    // immediate next period, the rest project ~3 months out.
                    value = result.upcomingPeriods.joinToString("\n", transform = ::format),
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
            color = Color.White.copy(alpha = 0.45f),
            modifier = Modifier.padding(horizontal = 4.dp),
        )
    }

    if (pickerOpen) {
        // M3's DatePicker speaks epoch-millis in UTC. We feed it the
        // current LMP converted to UTC midnight, and convert back via
        // UTC so we get the same calendar date the user actually
        // selected - using the device zone would slide the date by one
        // day for users in negative-offset zones (e.g. Hawaii UTC-10
        // picking "May 18" would land on "May 17" if we round-tripped
        // via system zone).
        val state =
            rememberDatePickerState(
                initialSelectedDateMillis = lmp.atStartOfDay().toInstant(ZoneOffset.UTC).toEpochMilli(),
            )
        DatePickerDialog(
            onDismissRequest = { pickerOpen = false },
            confirmButton = {
                TextButton(
                    onClick = {
                        state.selectedDateMillis?.let { ms ->
                            lmp = Instant.ofEpochMilli(ms).atZone(ZoneOffset.UTC).toLocalDate()
                        }
                        pickerOpen = false
                    },
                ) { Text(stringResource(R.string.action_ok)) }
            },
            dismissButton = {
                TextButton(onClick = { pickerOpen = false }) {
                    Text(stringResource(R.string.action_cancel))
                }
            },
        ) { DatePicker(state = state) }
    }
}

// Human-friendly format: weekday, day, short month, year (e.g.
// "Tue, 19 May 2026"). The leading weekday matters for next-period and
// ovulation rows because users frequently want to know what day of the
// week to plan around.
private fun format(date: LocalDate): String =
    date.format(DateTimeFormatter.ofPattern("EEE, d MMM yyyy"))

/**
 * Label-above-value output row. Used for the result card here because
 * the date strings include the weekday and don't fit cleanly to the
 * right of their labels on a phone-width line.
 */
@Composable
private fun StackedOutput(label: String, value: String, accent: Boolean = false) {
    Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
        Text(
            text = label,
            style = MaterialTheme.typography.bodyLarge,
            color = Color.White.copy(alpha = 0.7f),
        )
        Text(
            text = value,
            style = MaterialTheme.typography.titleLarge.copy(
                fontWeight = if (accent) FontWeight.Bold else FontWeight.SemiBold,
            ),
            color = if (accent) LifeCalcAccent else Color.White,
        )
    }
}

// Default seed of "two weeks ago" gives the chart a sensible starting
// position centred around an average ovulation date.
private const val DEFAULT_LMP_OFFSET_DAYS = 14L
