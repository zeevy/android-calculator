package com.calculator.feature.datetime.age

import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.res.stringResource
import com.calculator.R
import com.calculator.core.designsystem.component.DateField
import com.calculator.core.designsystem.component.LifeCalcCard
import com.calculator.core.designsystem.component.LifeCalcOutputRow
import com.calculator.core.designsystem.component.LifeCalcSectionLabel
import com.calculator.core.designsystem.component.LifeCalculatorScaffold
import com.calculator.core.domain.datetime.AgeCalculator
import java.time.LocalDate

/**
 * Age calculator: takes a date of birth and reports elapsed
 * years/months/days, weekday of birth, and days remaining until the
 * next birthday.
 *
 * "Today" is read once per composition via [LocalDate.now]; the
 * underlying [AgeCalculator] takes "today" as a parameter so the unit
 * tests can pass a fixed date and assert deterministic output.
 *
 * @param onOpenMenu Opens the app's tools menu.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AgeScreen(onOpenMenu: () -> Unit) {
    val scheme = MaterialTheme.colorScheme
    var dob by remember { mutableStateOf(LocalDate.of(DEFAULT_DOB_YEAR, 1, DEFAULT_DOB_DAY)) }
    // Cached for this composition. The result card recomputes when
    // either `dob` or `today` changes; since `today` only changes when
    // recomposition happens past midnight, reading it once is fine for
    // a foreground calculator screen.
    val today = LocalDate.now()

    LifeCalculatorScaffold(
        title = stringResource(R.string.age_title),
        onOpenMenu = onOpenMenu,
    ) {
        LifeCalcCard {
            LifeCalcSectionLabel(stringResource(R.string.age_section_dob))
            DateField(date = dob, onDateChange = { dob = it })
        }

        val result = runCatching { AgeCalculator.compute(dob, today) }.getOrNull()
        LifeCalcCard {
            LifeCalcSectionLabel(stringResource(R.string.age_section_result))
            if (result == null) {
                Text(
                    stringResource(R.string.age_error_past_date),
                    style = MaterialTheme.typography.bodyMedium,
                    color = scheme.error,
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
}

// Placeholder birthdate used when the user first opens the screen; the
// exact value is unimportant as long as it's a recognisable, valid date.
private const val DEFAULT_DOB_YEAR = 1990
private const val DEFAULT_DOB_DAY = 15
