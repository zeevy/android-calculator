package com.calculator.feature.datetime.datediff

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.calculator.R
import com.calculator.core.common.format.NumberFormatter
import com.calculator.core.designsystem.component.DateField
import com.calculator.core.designsystem.component.LifeCalcCard
import com.calculator.core.designsystem.component.LifeCalcNumberField
import com.calculator.core.designsystem.component.LifeCalcOutputRow
import com.calculator.core.designsystem.component.LifeCalcSectionLabel
import com.calculator.core.designsystem.component.LifeCalcSegmented
import com.calculator.core.designsystem.component.LifeCalculatorScaffold
import com.calculator.core.domain.datetime.DateDiffCalculator
import java.time.LocalDate
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
 * @param onOpenMenu Opens the app's tools menu.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DateDiffScreen(onOpenMenu: () -> Unit) {
    val scheme = MaterialTheme.colorScheme
    var mode by remember { mutableIntStateOf(0) } // 0 = two dates, 1 = date + offset
    // Defaults seed a sample 1-year span so the result card has
    // something to display on first render.
    var dateA by remember { mutableStateOf(LocalDate.now().minusYears(1)) }
    var dateB by remember { mutableStateOf(LocalDate.now()) }
    var offsetDays by remember { mutableStateOf("90") }

    LifeCalculatorScaffold(
        title = stringResource(R.string.datediff_title),
        onOpenMenu = onOpenMenu,
    ) {
        LifeCalcCard {
            LifeCalcSectionLabel(stringResource(R.string.datediff_section_mode))
            LifeCalcSegmented(
                options = listOf(
                    stringResource(R.string.datediff_mode_two),
                    stringResource(R.string.datediff_mode_offset),
                ),
                selectedIndex = mode,
                onSelect = { mode = it },
            )
            if (mode == 0) {
                LifeCalcSectionLabel(stringResource(R.string.datediff_from))
                DateField(date = dateA, onDateChange = { dateA = it })
                LifeCalcSectionLabel(stringResource(R.string.datediff_to_label))
                DateField(date = dateB, onDateChange = { dateB = it })
            } else {
                LifeCalcSectionLabel(stringResource(R.string.datediff_section_start_date))
                DateField(date = dateA, onDateChange = { dateA = it })
                LifeCalcNumberField(
                    label = stringResource(R.string.datediff_offset),
                    value = offsetDays,
                    onValueChange = { offsetDays = it },
                    suffix = stringResource(R.string.datediff_offset_unit),
                )
            }
        }

        LifeCalcCard {
            LifeCalcSectionLabel(stringResource(R.string.datediff_section_result))
            if (mode == 0) {
                val diff = DateDiffCalculator.difference(dateA, dateB)
                LifeCalcOutputRow(
                    label = stringResource(R.string.datediff_label_difference),
                    value = stringResource(
                        R.string.datediff_value_diff_format,
                        diff.years,
                        diff.months,
                        diff.days,
                    ),
                    accent = true,
                )
                LifeCalcOutputRow(
                    label = stringResource(R.string.datediff_label_total_days),
                    value = "${diff.totalDays}",
                )
                LifeCalcOutputRow(
                    label = stringResource(R.string.datediff_label_weeks),
                    value = stringResource(
                        R.string.datediff_value_weeks_format,
                        diff.totalWeeks,
                        diff.weekRemainderDays,
                    ),
                )
            } else {
                val result =
                    runCatching {
                        DateDiffCalculator.addOffset(dateA, requireNotNull(NumberFormatter.parseWholeNumberInput(offsetDays)))
                    }.getOrNull()
                if (result == null) {
                    Text(
                        text = stringResource(R.string.datediff_error_invalid_offset),
                        style = MaterialTheme.typography.bodyMedium,
                        color = scheme.error,
                    )
                } else {
                    // Stacked layout: full "Wednesday, 18 February 2026"
                    // strings are too wide to share a line with the
                    // label without ellipsising, so we drop the value to
                    // its own row.
                    Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                        Text(
                            text = stringResource(R.string.datediff_label_resulting_date),
                            style = MaterialTheme.typography.bodyLarge,
                            color = scheme.onSurfaceVariant,
                        )
                        Text(
                            text = result.format(DateTimeFormatter.ofPattern("EEEE, d MMM yyyy")),
                            style = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.Bold),
                            color = scheme.primary,
                            modifier = Modifier,
                        )
                    }
                }
            }
        }
    }
}
