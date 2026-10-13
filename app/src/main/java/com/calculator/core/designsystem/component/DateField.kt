package com.calculator.core.designsystem.component

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
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.calculator.R
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneOffset
import java.time.format.DateTimeFormatter

/**
 * Tappable date row that opens a Material 3 date picker.
 *
 * Shows the full date with weekday and a "Tap to change" hint, styled to
 * match the life-calculator cards.
 *
 * @param date Date currently selected; also seeds the picker.
 * @param onDateChange Invoked with the date the user confirms.
 */
@Composable
fun DateField(
    date: LocalDate,
    onDateChange: (LocalDate) -> Unit,
) {
    var pickerOpen by rememberSaveable { mutableStateOf(false) }
    val scheme = MaterialTheme.colorScheme
    Column(
        modifier =
            Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(12.dp))
                .background(scheme.surfaceContainerHigh)
                .clickable { pickerOpen = true }
                .padding(horizontal = 16.dp, vertical = 14.dp),
    ) {
        Text(
            text = date.format(DateTimeFormatter.ofPattern("EEEE, d MMMM yyyy")),
            style = MaterialTheme.typography.bodyLarge.copy(fontWeight = FontWeight.SemiBold),
            color = scheme.primary,
        )
        Spacer(Modifier.size(2.dp))
        Text(
            text = stringResource(R.string.date_tap_to_change),
            style = MaterialTheme.typography.bodySmall,
            color = scheme.onSurfaceVariant,
        )
    }
    if (pickerOpen) {
        DatePickerFor(
            date = date,
            onDismiss = { pickerOpen = false },
            onConfirm = {
                onDateChange(it)
                pickerOpen = false
            },
        )
    }
}

/**
 * Date picker dialog that speaks [LocalDate].
 *
 * The M3 picker works in epoch milliseconds at UTC midnight. Converting
 * both ways through UTC keeps the calendar date the user picked; going
 * through the device zone would move it by one day in zones behind UTC.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun DatePickerFor(
    date: LocalDate,
    onDismiss: () -> Unit,
    onConfirm: (LocalDate) -> Unit,
) {
    val state =
        rememberDatePickerState(
            initialSelectedDateMillis = date.atStartOfDay().toInstant(ZoneOffset.UTC).toEpochMilli(),
        )
    DatePickerDialog(
        onDismissRequest = onDismiss,
        confirmButton = {
            TextButton(
                onClick = {
                    val ms = state.selectedDateMillis
                    if (ms == null) {
                        onDismiss()
                    } else {
                        onConfirm(Instant.ofEpochMilli(ms).atZone(ZoneOffset.UTC).toLocalDate())
                    }
                },
            ) { Text(stringResource(R.string.action_ok)) }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text(stringResource(R.string.action_cancel)) }
        },
    ) { DatePicker(state = state) }
}
