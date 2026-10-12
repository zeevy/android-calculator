package com.calculator.feature.tape

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.DeleteOutline
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.calculator.R
import com.calculator.core.data.tape.TapeEntry
import com.calculator.core.designsystem.component.LifeCalculatorScaffold

/**
 * Running session tape - one expression+result per row, newest at the
 * bottom (just like a printed receipt). Tap any row to recall its
 * result back into the basic calculator; "Clear" empties the tape.
 *
 * The tape is in-memory and process-scoped (see `TapeStore`) - it
 * survives navigation and configuration changes but resets on app
 * restart. Persistent history lives in the History sheet.
 *
 * @param onOpenMenu Opens the app's tools menu.
 * @param onReuseExpression Sends a tapped result to the basic calculator.
 * @param viewModel Holds the session tape.
 */
@Composable
fun TapeScreen(
    onOpenMenu: () -> Unit,
    onReuseExpression: (String) -> Unit,
    viewModel: TapeViewModel = hiltViewModel(),
) {
    val entries by viewModel.entries.collectAsStateWithLifecycle()
    val scheme = MaterialTheme.colorScheme

    LifeCalculatorScaffold(
        title = stringResource(R.string.tape_title),
        onOpenMenu = onOpenMenu,
        scrollable = false,
        actions = {
            if (entries.isNotEmpty()) {
                IconButton(onClick = viewModel::clear) {
                    Icon(
                        imageVector = Icons.Filled.DeleteOutline,
                        contentDescription = stringResource(R.string.tape_clear),
                        tint = scheme.onBackground,
                    )
                }
            }
        },
    ) {
        if (entries.isEmpty()) {
            Box(
                modifier = Modifier.fillMaxSize(),
                contentAlignment = Alignment.Center,
            ) {
                Text(
                    text = stringResource(R.string.tape_empty),
                    style = MaterialTheme.typography.bodyLarge,
                    color = scheme.onSurfaceVariant,
                )
            }
        } else {
            LazyColumn(
                modifier = Modifier.fillMaxSize(),
                verticalArrangement = Arrangement.spacedBy(8.dp),
                contentPadding = androidx.compose.foundation.layout
                    .PaddingValues(vertical = 8.dp),
            ) {
                items(entries, key = { it.id }) { entry ->
                    TapeRow(
                        entry = entry,
                        onTap = { onReuseExpression(entry.result) },
                    )
                }
            }
        }
    }
}

@Composable
private fun TapeRow(entry: TapeEntry, onTap: () -> Unit) {
    val scheme = MaterialTheme.colorScheme
    Column(
        modifier =
            Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(16.dp))
                .background(scheme.surfaceContainer)
                .clickable(onClick = onTap)
                .padding(horizontal = 16.dp, vertical = 12.dp),
    ) {
        Text(
            text = entry.expression,
            style = MaterialTheme.typography.bodyMedium.copy(fontFamily = FontFamily.Monospace),
            color = scheme.onSurfaceVariant,
        )
        Spacer(Modifier.size(2.dp))
        Text(
            text = "= ${entry.result}",
            style =
                MaterialTheme.typography.titleLarge.copy(
                    fontWeight = FontWeight.Bold,
                    fontFamily = FontFamily.Monospace,
                ),
            color = scheme.primary,
        )
    }
}
