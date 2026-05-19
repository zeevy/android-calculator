package com.calculator.feature.converter.currency

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.systemBarsPadding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Star
import androidx.compose.material.icons.outlined.StarBorder
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Text
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import java.text.DecimalFormat
import java.text.DecimalFormatSymbols
import java.time.Instant
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import java.util.Locale

/**
 * Currency converter. Top: amount + base picker + refresh button +
 * last-updated timestamp / error banner. Below: scrollable list of
 * cached currencies (favourites first), each showing the equivalent
 * of the typed amount.
 */
@Composable
fun CurrencyConverterScreen(
    onUp: () -> Unit,
    viewModel: CurrencyConverterViewModel = hiltViewModel(),
) {
    val state by viewModel.state.collectAsState()
    var pickerOpen by remember { mutableStateOf(false) }

    Column(
        modifier =
            Modifier
                .fillMaxSize()
                .background(Color.Black)
                .systemBarsPadding()
                .padding(horizontal = 16.dp),
    ) {
        Row(
            modifier = Modifier.fillMaxWidth().padding(vertical = 8.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            IconButton(onClick = onUp) {
                Icon(
                    imageVector = Icons.Filled.ArrowBack,
                    contentDescription = "Back",
                    tint = Color.White,
                )
            }
            Text(
                text = "Currency",
                style = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.SemiBold),
                color = Color.White,
                modifier = Modifier.weight(1f).padding(start = 8.dp),
            )
            IconButton(
                onClick = viewModel::refreshManually,
                enabled = !state.isRefreshing,
            ) {
                if (state.isRefreshing) {
                    CircularProgressIndicator(
                        color = CurrencyAccent,
                        strokeWidth = 2.dp,
                        modifier = Modifier.size(20.dp),
                    )
                } else {
                    Icon(
                        imageVector = Icons.Filled.Refresh,
                        contentDescription = "Refresh rates",
                        tint = Color.White,
                    )
                }
            }
        }

        // Amount + base card.
        Column(
            modifier =
                Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(20.dp))
                    .background(CardBackground)
                    .padding(16.dp),
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Text(
                    text = state.base,
                    style =
                        MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.SemiBold),
                    color = CurrencyAccent,
                    modifier =
                        Modifier
                            .clip(RoundedCornerShape(8.dp))
                            .clickable(enabled = state.allCodes.isNotEmpty()) {
                                pickerOpen = true
                            }
                            .padding(horizontal = 8.dp, vertical = 4.dp),
                )
                Spacer(Modifier.weight(1f))
                BasicTextField(
                    value = state.amount,
                    onValueChange = viewModel::setAmount,
                    singleLine = true,
                    textStyle =
                        TextStyle(
                            color = Color.White,
                            fontSize = 32.sp,
                            fontWeight = FontWeight.Bold,
                            textAlign = TextAlign.End,
                        ),
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                    cursorBrush = androidx.compose.ui.graphics.SolidColor(CurrencyAccent),
                )
            }
            Spacer(Modifier.size(8.dp))
            HeaderInfoLine(state = state)
        }

        Spacer(Modifier.size(12.dp))

        if (state.rates.isEmpty()) {
            EmptyState(isRefreshing = state.isRefreshing)
        } else {
            CurrencyList(
                state = state,
                onToggleFavorite = viewModel::toggleFavorite,
            )
        }
    }

    if (pickerOpen) {
        CodePickerSheet(
            codes = state.allCodes,
            currentBase = state.base,
            onPick = {
                viewModel.setBase(it)
                pickerOpen = false
            },
            onDismiss = { pickerOpen = false },
        )
    }
}

@Composable
private fun HeaderInfoLine(state: CurrencyConverterUiState) {
    when {
        state.errorMessage != null ->
            Text(
                text = state.errorMessage,
                style = MaterialTheme.typography.bodySmall,
                color = Color(0xFFFF6B6B),
            )
        state.fetchedAtUtc != null ->
            Text(
                text = "Updated ${formatRelative(state.fetchedAtUtc)}",
                style = MaterialTheme.typography.bodySmall,
                color = Color.White.copy(alpha = 0.55f),
            )
        else ->
            Text(
                text = "No data yet.",
                style = MaterialTheme.typography.bodySmall,
                color = Color.White.copy(alpha = 0.55f),
            )
    }
}

@Composable
private fun CurrencyList(
    state: CurrencyConverterUiState,
    onToggleFavorite: (String) -> Unit,
) {
    val amountValue = state.amount.toDoubleOrNull() ?: 0.0
    val baseRate = state.rates[state.base] ?: 1.0
    LazyColumn(
        modifier = Modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(4.dp),
    ) {
        items(state.visibleCodes, key = { it }) { code ->
            // Re-base the rate when the user picks a non-USD base
            // and the cached payload was fetched against a different
            // base (rare; we re-fetch on base change, but the brief
            // transitional window can land here).
            val rate = (state.rates[code] ?: 0.0) / baseRate
            CurrencyRow(
                code = code,
                converted = amountValue * rate,
                pinned = state.favorites.contains(code),
                onTogglePin = { onToggleFavorite(code) },
            )
            HorizontalDivider(color = Color.White.copy(alpha = 0.08f))
        }
    }
}

@Composable
private fun CurrencyRow(
    code: String,
    converted: Double,
    pinned: Boolean,
    onTogglePin: () -> Unit,
) {
    Row(
        modifier =
            Modifier
                .fillMaxWidth()
                .padding(vertical = 14.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        IconButton(
            onClick = onTogglePin,
            modifier = Modifier.size(36.dp),
        ) {
            Icon(
                imageVector =
                    if (pinned) Icons.Filled.Star else Icons.Outlined.StarBorder,
                contentDescription = if (pinned) "Unpin" else "Pin",
                tint = if (pinned) CurrencyAccent else Color.White.copy(alpha = 0.55f),
            )
        }
        Spacer(Modifier.size(4.dp))
        Text(
            text = code,
            style = MaterialTheme.typography.titleMedium,
            color = Color.White,
            modifier = Modifier.weight(1f),
        )
        Text(
            text = formatMoney(converted),
            style =
                MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.SemiBold),
            color = Color.White,
        )
    }
}

@Composable
private fun EmptyState(isRefreshing: Boolean) {
    Box(
        modifier = Modifier.fillMaxWidth().padding(vertical = 64.dp),
        contentAlignment = Alignment.Center,
    ) {
        if (isRefreshing) {
            CircularProgressIndicator(color = CurrencyAccent)
        } else {
            Text(
                text = "No cached rates. Tap refresh to fetch.",
                style = MaterialTheme.typography.bodyMedium,
                color = Color.White.copy(alpha = 0.55f),
            )
        }
    }
}

@OptIn(androidx.compose.material3.ExperimentalMaterial3Api::class)
@Composable
private fun CodePickerSheet(
    codes: List<String>,
    currentBase: String,
    onPick: (String) -> Unit,
    onDismiss: () -> Unit,
) {
    val sheetState = rememberModalBottomSheetState()
    ModalBottomSheet(
        sheetState = sheetState,
        onDismissRequest = onDismiss,
        containerColor = SheetBackground,
    ) {
        LazyColumn(
            modifier = Modifier.fillMaxWidth().heightIn(max = 480.dp),
            verticalArrangement = Arrangement.spacedBy(2.dp),
        ) {
            items(codes, key = { it }) { code ->
                Text(
                    text = code,
                    style =
                        MaterialTheme.typography.bodyLarge.copy(
                            fontWeight = if (code == currentBase) FontWeight.SemiBold else FontWeight.Normal,
                        ),
                    color = if (code == currentBase) CurrencyAccent else Color.White,
                    modifier =
                        Modifier
                            .fillMaxWidth()
                            .clickable { onPick(code) }
                            .padding(horizontal = 24.dp, vertical = 14.dp),
                )
                HorizontalDivider(color = Color.White.copy(alpha = 0.08f))
            }
        }
    }
}

private val moneyFormat: DecimalFormat by lazy {
    DecimalFormat("#,##0.00####", DecimalFormatSymbols(Locale.US))
}

private fun formatMoney(value: Double): String =
    if (value == 0.0) "0.00" else moneyFormat.format(value)

private fun formatRelative(epochMillis: Long): String {
    val instant = Instant.ofEpochMilli(epochMillis)
    val now = Instant.now()
    val seconds = (now.toEpochMilli() - epochMillis) / 1000
    return when {
        seconds < 60 -> "just now"
        seconds < 3_600 -> "${seconds / 60}m ago"
        seconds < 86_400 -> "${seconds / 3_600}h ago"
        else ->
            DateTimeFormatter.ofPattern("d MMM yyyy")
                .withZone(ZoneId.systemDefault())
                .format(instant)
    }
}

private val CardBackground = Color(0xFF2C2C2E)
private val SheetBackground = Color(0xFF1C1C1E)
private val CurrencyAccent = Color(0xFFFF9F0A)
