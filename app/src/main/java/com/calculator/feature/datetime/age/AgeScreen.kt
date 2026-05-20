package com.calculator.feature.datetime.age

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.DatePicker
import androidx.compose.material3.DatePickerDialog
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.rememberDatePickerState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.calculator.R
import com.calculator.core.domain.datetime.AgeCalculator
import com.calculator.feature.lifecalc.LifeCalcAccent
import com.calculator.feature.lifecalc.LifeCalcCard
import com.calculator.feature.lifecalc.LifeCalcOutputRow
import com.calculator.feature.lifecalc.LifeCalcSectionLabel
import com.calculator.feature.lifecalc.LifeCalcSegmentBackground
import com.calculator.feature.lifecalc.LifeCalculatorScaffold
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneOffset
import java.time.format.DateTimeFormatter

/**
 * Age calculator: takes a date of birth and reports elapsed
 * years/months/days, weekday of birth, and days remaining until the
 * next birthday.
 *
 * "Today" is read once per composition via [LocalDate.now]; the
 * underlying [AgeCalculator] takes "today" as a parameter so the unit
 * tests can pass a fixed date and assert deterministic output.
 *
 * @param onUp Pop the calculator from the back stack.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AgeScreen(onUp: () -> Unit) {
    var dob by remember { mutableStateOf(LocalDate.of(DEFAULT_DOB_YEAR, 1, DEFAULT_DOB_DAY)) }
    var pickerOpen by remember { mutableStateOf(false) }
    // Cached for this composition. The result card recomputes when
    // either `dob` or `today` changes; since `today` only changes when
    // recomposition happens past midnight, reading it once is fine for
    // a foreground calculator screen.
    val today = LocalDate.now()

    LifeCalculatorScaffold(title = stringResource(R.string.age_title), onUp = onUp) {
        LifeCalcCard {
            LifeCalcSectionLabel(stringResource(R.string.age_section_dob))
            DateRow(date = dob, onClick = { pickerOpen = true })
        }

        val result = runCatching { AgeCalculator.compute(dob, today) }.getOrNull()
        LifeCalcCard {
            LifeCalcSectionLabel(stringResource(R.string.age_section_result))
            if (result == null) {
                Text(
                    stringResource(R.string.age_error_past_date),
                    style = MaterialTheme.typography.bodyMedium,
                    color = Color.White.copy(alpha = 0.55f),
                )
            } else {
                LifeCalcOutputRow(
                    label = stringResource(R.string.age_label_age),
                    value = stringResource(
                        R.string.age_value_format,
                        result.years,
                        result.months,
                        result.days,
                    ),
                    accent = true,
                )
                LifeCalcOutputRow(
                    label = stringResource(R.string.age_weekday),
                    value = result.weekdayOfBirth.name
                        .lowercase()
                        .replaceFirstChar { it.titlecase() },
                )
                LifeCalcOutputRow(
                    label = stringResource(R.string.age_to_next),
                    value = if (result.daysToNextBirthday == 0) {
                        stringResource(R.string.age_to_next_today)
                    } else {
                        stringResource(R.string.age_days_format, result.daysToNextBirthday)
                    },
                )
            }
        }
    }

    if (pickerOpen) {
        val state =
            rememberDatePickerState(
                initialSelectedDateMillis = dob.atStartOfDay().toInstant(ZoneOffset.UTC).toEpochMilli(),
            )
        DatePickerDialog(
            onDismissRequest = { pickerOpen = false },
            confirmButton = {
                TextButton(
                    onClick = {
                        state.selectedDateMillis?.let { ms ->
                            dob = Instant.ofEpochMilli(ms).atZone(ZoneOffset.UTC).toLocalDate()
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

/**
 * Tappable date row. Renders a friendly full date with weekday and a
 * "Tap to change" hint, styled to match the rest of the life-calc cards.
 *
 * Shared with the ovulation screen (and any future screen that needs a
 * "pick a date" affordance with the same look) - hence `internal`
 * visibility rather than `private`.
 *
 * @param date Date currently selected (also seeds the picker dialog).
 * @param onClick Open the date-picker dialog.
 */
@Composable
internal fun DateRow(date: LocalDate, onClick: () -> Unit) {
    Column(
        modifier =
            Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(12.dp))
                .background(LifeCalcSegmentBackground)
                .clickable(onClick = onClick)
                .padding(horizontal = 16.dp, vertical = 14.dp),
    ) {
        Text(
            text = date.format(DateTimeFormatter.ofPattern("EEEE, d MMMM yyyy")),
            style = MaterialTheme.typography.bodyLarge.copy(fontWeight = FontWeight.SemiBold),
            color = LifeCalcAccent,
        )
        Spacer(Modifier.size(2.dp))
        Text(
            text = stringResource(R.string.age_tap_to_change),
            style = MaterialTheme.typography.bodySmall,
            color = Color.White.copy(alpha = 0.5f),
        )
    }
}

// Placeholder birthdate used when the user first opens the screen; the
// exact value is unimportant as long as it's a recognisable, valid date.
private const val DEFAULT_DOB_YEAR = 1990
private const val DEFAULT_DOB_DAY = 15
