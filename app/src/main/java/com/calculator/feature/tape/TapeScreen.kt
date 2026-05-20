package com.calculator.feature.tape

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
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
import androidx.compose.material.icons.filled.Menu
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
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
import androidx.compose.foundation.layout.systemBarsPadding
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.calculator.R
import com.calculator.feature.lifecalc.LifeCalcAccent
import com.calculator.feature.lifecalc.PendingExpressionHolder
import com.calculator.feature.lifecalc.ToolsMenuOverlay
import com.calculator.feature.lifecalc.ToolsMenuSheet
import com.calculator.navigation.BasicCalculatorRoute
import com.calculator.navigation.TapeRoute

/**
 * Running session tape - one expression+result per row, newest at the
 * bottom (just like a printed receipt). Tap any row to recall its
 * result back into the basic calculator; "Clear" empties the tape.
 *
 * The tape is in-memory and process-scoped (see [TapeHolder]) - it
 * survives navigation and configuration changes but resets on app
 * restart. Persistent history lives in the History sheet.
 *
 * @param onNavigate Jump to another tool / home.
 */
@Composable
fun TapeScreen(onNavigate: (Any) -> Unit) {
    val entries by TapeHolder.entries.collectAsState()
    var openSheet by remember { mutableStateOf<ToolsMenuSheet?>(null) }

    Column(
        modifier =
            Modifier
                .fillMaxSize()
                .background(Color.Black)
                .systemBarsPadding()
                .padding(horizontal = 12.dp),
    ) {
        Row(
            modifier = Modifier.fillMaxWidth().padding(vertical = 8.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Text(
                text = stringResource(R.string.tape_title),
                style = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.SemiBold),
                color = Color.White,
                modifier = Modifier.weight(1f).padding(start = 8.dp),
            )
            if (entries.isNotEmpty()) {
                IconButton(onClick = { TapeHolder.clear() }) {
                    Icon(
                        imageVector = Icons.Filled.DeleteOutline,
                        contentDescription = stringResource(R.string.tape_clear),
                        tint = Color.White,
                    )
                }
            }
            IconButton(onClick = { openSheet = ToolsMenuSheet.Tools }) {
                Icon(
                    imageVector = Icons.Filled.Menu,
                    contentDescription = stringResource(R.string.basic_open_menu),
                    tint = Color.White,
                )
            }
        }

        if (entries.isEmpty()) {
            Box(
                modifier = Modifier.fillMaxSize(),
                contentAlignment = Alignment.Center,
            ) {
                Text(
                    text = stringResource(R.string.tape_empty),
                    style = MaterialTheme.typography.bodyLarge,
                    color = Color.White.copy(alpha = 0.45f),
                )
            }
        } else {
            LazyColumn(
                modifier = Modifier.fillMaxSize(),
                verticalArrangement = Arrangement.spacedBy(8.dp),
                contentPadding = androidx.compose.foundation.layout.PaddingValues(vertical = 8.dp),
            ) {
                items(entries, key = { it.id }) { entry ->
                    TapeRow(
                        entry = entry,
                        onTap = {
                            // Push the result back onto the basic calc as
                            // the next expression - same one-shot pattern
                            // we use for history-row reuse.
                            PendingExpressionHolder.expression = entry.result
                            onNavigate(BasicCalculatorRoute)
                        },
                    )
                }
            }
        }
    }

    ToolsMenuOverlay(
        openSheet = openSheet,
        onDismiss = { openSheet = null },
        currentRoute = TapeRoute,
        isOnBasicCalc = false,
        scientific = false,
        onToggleScientific = {},
        onOpenHistory = { openSheet = ToolsMenuSheet.History },
        onOpenSettings = { openSheet = ToolsMenuSheet.Settings },
        onNavigate = onNavigate,
        onReuseExpression = { expr ->
            PendingExpressionHolder.expression = expr
            onNavigate(BasicCalculatorRoute)
        },
    )
}

@Composable
private fun TapeRow(entry: TapeEntry, onTap: () -> Unit) {
    Column(
        modifier =
            Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(16.dp))
                .background(TapeRowBackground)
                .clickable(onClick = onTap)
                .padding(horizontal = 16.dp, vertical = 12.dp),
    ) {
        Text(
            text = entry.expression,
            style = MaterialTheme.typography.bodyMedium.copy(fontFamily = FontFamily.Monospace),
            color = Color.White.copy(alpha = 0.7f),
        )
        Spacer(Modifier.size(2.dp))
        Text(
            text = "= ${entry.result}",
            style =
                MaterialTheme.typography.titleLarge.copy(
                    fontWeight = FontWeight.Bold,
                    fontFamily = FontFamily.Monospace,
                ),
            color = LifeCalcAccent,
        )
    }
}

// Card background for each tape row. Same near-black as the tools
// sheet so the rows read as part of the same visual system.
private val TapeRowBackground = Color(0xFF1C1C1E)
