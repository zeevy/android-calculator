package com.calculator.feature.datetime.datediff

import androidx.compose.material3.DatePicker
import androidx.compose.material3.DatePickerDialog
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.rememberDatePickerState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.graphics.Color
import com.calculator.core.domain.datetime.DateDiffCalculator
import com.calculator.feature.datetime.age.DateRow
import com.calculator.feature.lifecalc.LifeCalcCard
import com.calculator.feature.lifecalc.LifeCalcNumberField
import com.calculator.feature.lifecalc.LifeCalcOutputRow
import com.calculator.feature.lifecalc.LifeCalcSectionLabel
import com.calculator.feature.lifecalc.LifeCalcSegmented
import com.calculator.feature.lifecalc.LifeCalculatorScaffold
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneOffset
import java.time.format.DateTimeFormatter

/**
 * Date arithmetic: difference between two dates, or a date plus an
 * offset.
 *
 * Mode 0 (Two dates) - shows years/months/days, total days, and total
 * weeks + remainder. Order doesn't matter; [DateDiffCalculator] swaps
 * arguments to keep the result non-negative.
 *
 * Mode 1 (Date + offset) - takes a start date and a (possibly negative)
 * integer day count, returns the resulting calendar date. Offset is
 * stored as a string so the field can show "" or "-" mid-edit.
 *
 * @param onUp Pop the calculator from the back stack.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DateDiffScreen(onUp: () -> Unit) {
    var mode by remember { mutableIntStateOf(0) } // 0 = two dates, 1 = date + offset
    // Defaults seed a sample 1-year span so the result card has
    // something to display on first render. The single picker state
    // (`openPicker`) doubles as a flag (null = closed) and a routing
    // tag (which date to update on confirm) - simpler than carrying a
    // boolean + selector pair.
    var dateA by remember { mutableStateOf(LocalDate.now().minusYears(1)) }
    var dateB by remember { mutableStateOf(LocalDate.now()) }
    var offsetDays by remember { mutableStateOf("90") }
    var openPicker by remember { mutableStateOf<Picker?>(null) }

    LifeCalculatorScaffold(title = "Date difference", onUp = onUp) {
        LifeCalcCard {
            LifeCalcSectionLabel("Mode")
            LifeCalcSegmented(
                options = listOf("Two dates", "Date + offset"),
                selectedIndex = mode,
                onSelect = { mode = it },
            )
            if (mode == 0) {
                LifeCalcSectionLabel("From")
                DateRow(date = dateA, onClick = { openPicker = Picker.A })
                LifeCalcSectionLabel("To")
                DateRow(date = dateB, onClick = { openPicker = Picker.B })
            } else {
                LifeCalcSectionLabel("Start date")
                DateRow(date = dateA, onClick = { openPicker = Picker.A })
                LifeCalcNumberField(
                    label = "Offset",
                    value = offsetDays,
                    onValueChange = { offsetDays = it },
                    suffix = "days",
                )
            }
        }

        LifeCalcCard {
            LifeCalcSectionLabel("Result")
            if (mode == 0) {
                val diff = DateDiffCalculator.difference(dateA, dateB)
                LifeCalcOutputRow(
                    "Difference",
                    "${diff.years}y ${diff.months}m ${diff.days}d",
                    accent = true,
                )
                LifeCalcOutputRow("Total days", "${diff.totalDays}")
                LifeCalcOutputRow(
                    "Weeks",
                    "${diff.totalWeeks}w ${diff.weekRemainderDays}d",
                )
            } else {
                val result =
                    runCatching {
                        DateDiffCalculator.addOffset(dateA, offsetDays.toInt())
                    }.getOrNull()
                if (result == null) {
                    Text(
                        text = "Enter a whole number of days.",
                        style = MaterialTheme.typography.bodyMedium,
                        color = Color.White.copy(alpha = 0.55f),
                    )
                } else {
                    LifeCalcOutputRow(
                        "Resulting date",
                        result.format(DateTimeFormatter.ofPattern("EEEE, d MMM yyyy")),
                        accent = true,
                    )
                }
            }
        }
    }

    openPicker?.let { picker ->
        val initial =
            when (picker) {
                Picker.A -> dateA
                Picker.B -> dateB
            }.atStartOfDay().toInstant(ZoneOffset.UTC).toEpochMilli()
        val state = rememberDatePickerState(initialSelectedDateMillis = initial)
        DatePickerDialog(
            onDismissRequest = { openPicker = null },
            confirmButton = {
                TextButton(
                    onClick = {
                        state.selectedDateMillis?.let { ms ->
                            val d = Instant.ofEpochMilli(ms).atZone(ZoneOffset.UTC).toLocalDate()
                            when (picker) {
                                Picker.A -> dateA = d
                                Picker.B -> dateB = d
                            }
                        }
                        openPicker = null
                    },
                ) { Text("OK") }
            },
            dismissButton = {
                TextButton(onClick = { openPicker = null }) { Text("Cancel") }
            },
        ) { DatePicker(state = state) }
    }
}

/** Identifies which date the picker dialog should write back to on confirm. */
private enum class Picker { A, B }
