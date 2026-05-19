package com.calculator.feature.history

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.DeleteOutline
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
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
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import com.calculator.core.data.history.HistoryEntry
import java.text.DateFormat
import java.util.Date

/**
 * History sheet shown when the user taps the History tile in the tools
 * menu. Lists entries newest-first, lets the user tap a row to reuse
 * its expression, tap the trash icon to delete a row, or hit
 * "Clear all" at the bottom (with a confirmation dialog).
 *
 * @param onReuseExpression Invoked with the expression string when the
 *   user taps a history row. The caller is responsible for closing the
 *   sheet and applying the expression to the calculator state.
 */
@Composable
fun HistorySheetContent(
    onReuseExpression: (String) -> Unit,
    onClose: () -> Unit,
    viewModel: HistoryViewModel = hiltViewModel(),
) {
    val entries by viewModel.entries.collectAsState()
    var confirmClearAll by remember { mutableStateOf(false) }

    Column(
        modifier =
            Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 8.dp),
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Text(
                text = "History",
                style = MaterialTheme.typography.titleLarge,
                modifier = Modifier.weight(1f).padding(horizontal = 8.dp),
            )
            if (entries.isNotEmpty()) {
                TextButton(onClick = { confirmClearAll = true }) {
                    Text("Clear all")
                }
            }
        }
        Spacer(Modifier.size(8.dp))

        if (entries.isEmpty()) {
            EmptyHistory()
        } else {
            LazyColumn(
                modifier = Modifier.heightIn(max = 400.dp),
                verticalArrangement = Arrangement.spacedBy(4.dp),
            ) {
                items(entries, key = { it.id }) { entry ->
                    HistoryRow(
                        entry = entry,
                        onTap = {
                            onReuseExpression(entry.expression)
                            onClose()
                        },
                        onDelete = { viewModel.delete(entry.id) },
                    )
                }
            }
        }

        Spacer(Modifier.size(16.dp))
    }

    if (confirmClearAll) {
        AlertDialog(
            onDismissRequest = { confirmClearAll = false },
            title = { Text("Clear all history?") },
            text = { Text("This removes every saved calculation. It can't be undone.") },
            confirmButton = {
                TextButton(
                    onClick = {
                        viewModel.clearAll()
                        confirmClearAll = false
                    },
                ) { Text("Clear") }
            },
            dismissButton = {
                TextButton(onClick = { confirmClearAll = false }) { Text("Cancel") }
            },
        )
    }
}

/**
 * Single history row: expression, "= result", timestamp, and a trash
 * icon on the right.
 *
 * Whole-row tap fires [onTap] (reuse), but the icon button sits inside
 * the same Row and gets first dibs on the gesture by virtue of being
 * an explicit clickable - tapping it does not also fire the row's
 * onClick. This is Compose's standard nested-clickable contract.
 *
 * @param entry History entry to render.
 * @param onTap Reuse this expression in the calculator.
 * @param onDelete Delete this entry from the DAO.
 */
@Composable
private fun HistoryRow(
    entry: HistoryEntry,
    onTap: () -> Unit,
    onDelete: () -> Unit,
) {
    Row(
        modifier =
            Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(16.dp))
                .background(MaterialTheme.colorScheme.surfaceContainer)
                .clickable(onClick = onTap)
                .padding(start = 12.dp, end = 4.dp, top = 8.dp, bottom = 8.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = entry.expression,
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                maxLines = 2,
            )
            Text(
                text = "= ${entry.result}",
                style =
                    MaterialTheme.typography.titleLarge.copy(
                        fontWeight = FontWeight.Bold,
                    ),
                color = MaterialTheme.colorScheme.onSurface,
            )
            Text(
                text = formatTimestamp(entry.timestampUtc),
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
        IconButton(onClick = onDelete) {
            Icon(
                imageVector = Icons.Filled.DeleteOutline,
                contentDescription = "Delete entry",
                tint = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
    }
}

@Composable
private fun EmptyHistory() {
    Box(
        modifier =
            Modifier
                .fillMaxWidth()
                .padding(vertical = 32.dp),
        contentAlignment = Alignment.Center,
    ) {
        Text(
            text = "No history yet. Calculations show up here when you press =.",
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            textAlign = TextAlign.Center,
            modifier = Modifier.padding(horizontal = 24.dp),
        )
    }
}

// Localised short-form date + time (e.g. "5/19/26, 2:31 PM" in en-US,
// "19/05/26 14:31" in en-GB). `DateFormat.getDateTimeInstance` picks
// the locale-correct pattern automatically, so we don't need to hand-
// roll one and lose i18n.
private fun formatTimestamp(epochMillis: Long): String =
    DateFormat.getDateTimeInstance(DateFormat.SHORT, DateFormat.SHORT).format(Date(epochMillis))

@Suppress("unused")
private val IosSheetBackgroundTextColor = Color.White
