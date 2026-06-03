package com.calculator.feature.datetime.timezone

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.SwapVert
import androidx.compose.material3.DatePicker
import androidx.compose.material3.DatePickerDialog
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TimePicker
import androidx.compose.material3.rememberDatePickerState
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.material3.rememberTimePickerState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.calculator.R
import com.calculator.core.domain.datetime.TimezoneConverter
import com.calculator.feature.lifecalc.LifeCalcCard
import com.calculator.feature.lifecalc.LifeCalcSectionLabel
import com.calculator.feature.lifecalc.LifeCalculatorScaffold
import com.calculator.navigation.TimezoneRoute
import java.time.Instant
import java.time.LocalDate
import java.time.LocalTime
import java.time.ZoneId
import java.time.ZoneOffset
import java.time.format.DateTimeFormatter

/**
 * Bidirectional timezone converter.
 *
 * Two cards (From / To). Each card has:
 *  - A zone chip (tap opens a searchable bottom-sheet zone picker).
 *  - A date row (tap opens a Material 3 DatePicker dialog).
 *  - A time row (tap opens a Material 3 TimePicker dialog).
 *
 * Typing on either side propagates the change to the other side via
 * the engine; `lastEdited` in the state tracks which side is the
 * source. The swap button between cards exchanges the (zone, time)
 * pairs.
 *
 * @param onNavigate Jump to another tool / home. Wired to the scaffold's
 *   hamburger menu.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TimezoneScreen(
    onNavigate: (Any) -> Unit,
    viewModel: TimezoneViewModel = hiltViewModel(),
) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    val scheme = MaterialTheme.colorScheme

    LifeCalculatorScaffold(
        title = stringResource(R.string.tz_title),
        currentRoute = TimezoneRoute,
        onNavigate = onNavigate,
    ) {
        // From card
        ZoneCard(
            sectionLabel = stringResource(R.string.tz_from),
            zoneId = state.fromZone,
            dateTime = state.fromTime,
            onZoneTap = { viewModel.openPicker(TimezoneUiState.Picker.FromZone) },
            onDateTap = { viewModel.openPicker(TimezoneUiState.Picker.FromDate) },
            onTimeTap = { viewModel.openPicker(TimezoneUiState.Picker.FromTime) },
        )
        // Swap button
        Box(modifier = Modifier.fillMaxWidth(), contentAlignment = Alignment.Center) {
            IconButton(
                onClick = viewModel::swap,
                modifier =
                    Modifier
                        .size(48.dp)
                        .clip(RoundedCornerShape(24.dp))
                        .background(scheme.primary),
            ) {
                Icon(
                    imageVector = Icons.Filled.SwapVert,
                    contentDescription = stringResource(R.string.tz_swap),
                    tint = scheme.onPrimary,
                )
            }
        }
        // To card
        ZoneCard(
            sectionLabel = stringResource(R.string.tz_to),
            zoneId = state.toZone,
            dateTime = state.toTime,
            onZoneTap = { viewModel.openPicker(TimezoneUiState.Picker.ToZone) },
            onDateTap = { viewModel.openPicker(TimezoneUiState.Picker.ToDate) },
            onTimeTap = { viewModel.openPicker(TimezoneUiState.Picker.ToTime) },
        )
    }

    // Picker sub-dialogs.
    when (state.pickerOpen) {
        TimezoneUiState.Picker.FromZone ->
            ZonePickerSheet(
                selected = state.fromZone,
                onPick = viewModel::setFromZone,
                onDismiss = viewModel::dismissPicker,
            )
        TimezoneUiState.Picker.ToZone ->
            ZonePickerSheet(
                selected = state.toZone,
                onPick = viewModel::setToZone,
                onDismiss = viewModel::dismissPicker,
            )
        TimezoneUiState.Picker.FromDate ->
            DateDialog(
                initial = state.fromTime.toLocalDate(),
                onPick = viewModel::setFromDate,
                onDismiss = viewModel::dismissPicker,
            )
        TimezoneUiState.Picker.ToDate ->
            DateDialog(
                initial = state.toTime.toLocalDate(),
                onPick = viewModel::setToDate,
                onDismiss = viewModel::dismissPicker,
            )
        TimezoneUiState.Picker.FromTime ->
            TimeDialog(
                initial = state.fromTime.toLocalTime(),
                onPick = viewModel::setFromTime,
                onDismiss = viewModel::dismissPicker,
            )
        TimezoneUiState.Picker.ToTime ->
            TimeDialog(
                initial = state.toTime.toLocalTime(),
                onPick = viewModel::setToTime,
                onDismiss = viewModel::dismissPicker,
            )
        null -> Unit
    }
}

/** A From or To card showing the zone, date, and time rows. */
@Composable
private fun ZoneCard(
    sectionLabel: String,
    zoneId: String,
    dateTime: java.time.LocalDateTime,
    onZoneTap: () -> Unit,
    onDateTap: () -> Unit,
    onTimeTap: () -> Unit,
) {
    LifeCalcCard {
        LifeCalcSectionLabel(sectionLabel)
        TappableRow(text = zoneId, onClick = onZoneTap)
        Spacer(Modifier.size(8.dp))
        TappableRow(
            text = dateTime.toLocalDate().format(DateTimeFormatter.ofPattern("EEEE, d MMMM yyyy")),
            onClick = onDateTap,
        )
        Spacer(Modifier.size(8.dp))
        TappableRow(
            text = dateTime.toLocalTime().format(DateTimeFormatter.ofPattern("HH:mm")),
            onClick = onTimeTap,
        )
    }
}

/** Light-grey pill row that opens a picker when tapped. */
@Composable
private fun TappableRow(text: String, onClick: () -> Unit) {
    val scheme = MaterialTheme.colorScheme
    Text(
        text = text,
        style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.SemiBold),
        color = scheme.primary,
        modifier =
            Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(12.dp))
                .background(scheme.surfaceContainerHigh)
                .clickable(onClick = onClick)
                .padding(horizontal = 16.dp, vertical = 14.dp),
    )
}

/**
 * Searchable zone picker.
 *
 * Lists [TimezoneConverter.COMMON_ZONES] at the top under a heading,
 * then every other IANA id under a second heading. A text field filters
 * both lists by case-insensitive substring match - typing "tok" surfaces
 * Asia/Tokyo, "indi" surfaces Asia/Calcutta + Asia/Kolkata, etc.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun ZonePickerSheet(
    selected: String,
    onPick: (String) -> Unit,
    onDismiss: () -> Unit,
) {
    val scheme = MaterialTheme.colorScheme
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    var query by remember { mutableStateOf("") }
    val all = remember { TimezoneConverter.allZoneIds() }
    val commonFiltered = remember(query) {
        TimezoneConverter.COMMON_ZONES.filter { it.contains(query, ignoreCase = true) }
    }
    val otherFiltered = remember(query) {
        val common = TimezoneConverter.COMMON_ZONES.toSet()
        all.filter { it !in common && it.contains(query, ignoreCase = true) }
    }

    ModalBottomSheet(
        sheetState = sheetState,
        onDismissRequest = onDismiss,
        containerColor = scheme.surfaceContainerLow,
        contentColor = scheme.onSurface,
    ) {
        Column(modifier = Modifier.fillMaxSize().padding(bottom = 16.dp)) {
            Text(
                text = stringResource(R.string.tz_picker_title),
                style = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.SemiBold),
                color = scheme.onSurface,
                modifier = Modifier.padding(horizontal = 24.dp, vertical = 8.dp),
            )
            // Search field. Plain BasicTextField for the iOS look.
            BasicTextField(
                value = query,
                onValueChange = { query = it },
                singleLine = true,
                textStyle = TextStyle(color = scheme.onSurface, fontSize = 16.sp),
                cursorBrush = androidx.compose.ui.graphics
                    .SolidColor(scheme.primary),
                modifier =
                    Modifier
                        .padding(horizontal = 24.dp)
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(12.dp))
                        .background(scheme.surfaceContainerHigh)
                        .padding(horizontal = 16.dp, vertical = 12.dp),
                decorationBox = { inner ->
                    Box(modifier = Modifier.fillMaxWidth()) {
                        if (query.isEmpty()) {
                            Text(
                                text = stringResource(R.string.tz_picker_search_hint),
                                color = scheme.onSurfaceVariant,
                                style = TextStyle(fontSize = 16.sp),
                            )
                        }
                        inner()
                    }
                },
            )
            Spacer(Modifier.size(8.dp))
            LazyColumn(modifier = Modifier.heightIn(max = 500.dp)) {
                if (commonFiltered.isNotEmpty()) {
                    item { SectionHeader(stringResource(R.string.tz_picker_common)) }
                    items(commonFiltered) { id ->
                        ZoneRow(id = id, isSelected = id == selected, onClick = { onPick(id) })
                    }
                    item { HorizontalDivider(modifier = Modifier.padding(vertical = 4.dp)) }
                }
                if (otherFiltered.isNotEmpty()) {
                    item { SectionHeader(stringResource(R.string.tz_picker_all)) }
                    items(otherFiltered) { id ->
                        ZoneRow(id = id, isSelected = id == selected, onClick = { onPick(id) })
                    }
                }
            }
        }
    }
}

@Composable
private fun SectionHeader(text: String) {
    Text(
        text = text,
        style = MaterialTheme.typography.labelMedium,
        color = MaterialTheme.colorScheme.onSurfaceVariant,
        modifier = Modifier.padding(horizontal = 24.dp, vertical = 8.dp),
    )
}

@Composable
private fun ZoneRow(id: String, isSelected: Boolean, onClick: () -> Unit) {
    val scheme = MaterialTheme.colorScheme
    // Show the current offset next to the id so the user can pick the
    // closest match without having to know the IANA name from memory.
    val offset =
        runCatching {
            ZoneId.of(id).rules.getOffset(Instant.now()).id.let { raw ->
                // ZoneOffset.id returns "+05:30", "Z" for UTC. Make "Z"
                // a more recognisable "UTC".
                if (raw == "Z") "UTC" else "UTC$raw"
            }
        }.getOrDefault("")
    Row(
        verticalAlignment = Alignment.CenterVertically,
        modifier =
            Modifier
                .fillMaxWidth()
                .clickable(onClick = onClick)
                .padding(horizontal = 24.dp, vertical = 12.dp),
    ) {
        Text(
            text = id,
            style = MaterialTheme.typography.bodyLarge,
            color = if (isSelected) scheme.primary else scheme.onSurface,
            modifier = Modifier.weight(1f),
        )
        Text(
            text = offset,
            style = MaterialTheme.typography.bodySmall,
            color = scheme.onSurfaceVariant,
        )
    }
}

/** Material 3 DatePicker dialog wrapper. */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun DateDialog(
    initial: LocalDate,
    onPick: (LocalDate) -> Unit,
    onDismiss: () -> Unit,
) {
    val state =
        rememberDatePickerState(
            initialSelectedDateMillis =
                initial.atStartOfDay(ZoneOffset.UTC).toInstant().toEpochMilli(),
        )
    DatePickerDialog(
        onDismissRequest = onDismiss,
        confirmButton = {
            TextButton(
                onClick = {
                    val millis = state.selectedDateMillis ?: return@TextButton
                    val date = Instant.ofEpochMilli(millis).atZone(ZoneOffset.UTC).toLocalDate()
                    onPick(date)
                },
            ) { Text(stringResource(R.string.action_ok)) }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text(stringResource(R.string.action_cancel)) }
        },
    ) {
        DatePicker(state = state)
    }
}

/** Material 3 TimePicker wrapped in a custom Dialog look. */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun TimeDialog(
    initial: LocalTime,
    onPick: (LocalTime) -> Unit,
    onDismiss: () -> Unit,
) {
    val state =
        rememberTimePickerState(
            initialHour = initial.hour,
            initialMinute = initial.minute,
            is24Hour = true,
        )
    // Use a DatePickerDialog shell since Compose Material3 doesn't ship
    // a dedicated TimePickerDialog. The shell only provides the action
    // buttons + a content slot, so it suits any picker UI.
    DatePickerDialog(
        onDismissRequest = onDismiss,
        confirmButton = {
            TextButton(
                onClick = { onPick(LocalTime.of(state.hour, state.minute)) },
            ) { Text(stringResource(R.string.action_ok)) }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text(stringResource(R.string.action_cancel)) }
        },
    ) {
        Box(modifier = Modifier.fillMaxWidth().padding(16.dp), contentAlignment = Alignment.Center) {
            TimePicker(state = state)
        }
    }
}
