package com.calculator.feature.health.ovulation

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
import androidx.compose.ui.unit.dp
import com.calculator.core.domain.health.OvulationCalculator
import com.calculator.feature.datetime.age.DateRow
import com.calculator.feature.lifecalc.LifeCalcAccent
import com.calculator.feature.lifecalc.LifeCalcCard
import com.calculator.feature.lifecalc.LifeCalcOutputRow
import com.calculator.feature.lifecalc.LifeCalcSectionLabel
import com.calculator.feature.lifecalc.LifeCalculatorScaffold
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneOffset
import java.time.format.DateTimeFormatter

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun OvulationScreen(onUp: () -> Unit) {
    var lmp by remember { mutableStateOf(LocalDate.now().minusDays(14)) }
    var cycleDays by remember { mutableIntStateOf(OvulationCalculator.DEFAULT_CYCLE_DAYS) }
    var pickerOpen by remember { mutableStateOf(false) }

    LifeCalculatorScaffold(title = "Ovulation", onUp = onUp) {
        LifeCalcCard {
            LifeCalcSectionLabel("Last menstrual period")
            DateRow(date = lmp, onClick = { pickerOpen = true })

            LifeCalcSectionLabel("Average cycle length")
            Text(
                text = "$cycleDays days",
                style = MaterialTheme.typography.titleLarge,
                color = LifeCalcAccent,
            )
            Slider(
                value = cycleDays.toFloat(),
                onValueChange = { cycleDays = it.toInt() },
                valueRange =
                    OvulationCalculator.MIN_CYCLE_DAYS.toFloat()..
                        OvulationCalculator.MAX_CYCLE_DAYS.toFloat(),
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
            LifeCalcSectionLabel("Estimates")
            if (result == null) {
                Text(
                    text = "Pick an LMP date and a cycle length between 21 and 35 days.",
                    style = MaterialTheme.typography.bodyMedium,
                    color = Color.White.copy(alpha = 0.55f),
                )
            } else {
                LifeCalcOutputRow("Ovulation", format(result.ovulation), accent = true)
                LifeCalcOutputRow(
                    "Fertile window",
                    "${format(result.fertileStart)} – ${format(result.fertileEnd)}",
                )
                LifeCalcOutputRow("Next period", format(result.nextPeriod))
                LifeCalcOutputRow("Estimated due date", format(result.estimatedDueDate))
            }
        }

        Text(
            text = "Estimator only. Cycles vary; this is not medical advice and not a contraception tool.",
            style = MaterialTheme.typography.bodySmall,
            color = Color.White.copy(alpha = 0.45f),
            modifier = Modifier.padding(horizontal = 4.dp),
        )
    }

    if (pickerOpen) {
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
                ) { Text("OK") }
            },
            dismissButton = {
                TextButton(onClick = { pickerOpen = false }) { Text("Cancel") }
            },
        ) { DatePicker(state = state) }
    }
}

private fun format(date: LocalDate): String =
    date.format(DateTimeFormatter.ofPattern("EEE, d MMM yyyy"))
