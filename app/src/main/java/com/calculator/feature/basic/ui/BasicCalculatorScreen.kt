package com.calculator.feature.basic.ui

import android.content.res.Configuration
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.systemBarsPadding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Menu
import androidx.compose.material3.AssistChip
import androidx.compose.material3.AssistChipDefaults
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.PreviewLightDark
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.calculator.core.designsystem.theme.CalculatorTheme
import com.calculator.core.math.AngleMode
import kotlinx.coroutines.launch
import java.math.BigDecimal

/**
 * Top-level composable for the basic calculator screen.
 *
 * Pulls the [BasicCalculatorViewModel] from Hilt and renders the
 * stateless [BasicCalculatorScreenContent] - this split keeps previews
 * possible without a ViewModel and makes UI tests straightforward.
 */
@Composable
fun BasicCalculatorScreen(viewModel: BasicCalculatorViewModel = hiltViewModel()) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    BasicCalculatorScreenContent(state = state, onEvent = viewModel::onEvent)
}

/**
 * Stateless screen content.
 *
 * Decoupling state from the ViewModel here is the single most useful
 * thing for testability: previews and Compose UI tests construct a
 * [BasicCalculatorUiState] directly and pass a no-op `onEvent` lambda.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
internal fun BasicCalculatorScreenContent(
    state: BasicCalculatorUiState,
    onEvent: (BasicCalculatorEvent) -> Unit,
) {
    var menuOpen by remember { mutableStateOf(false) }
    val sheetState = rememberModalBottomSheetState()
    val scope = rememberCoroutineScope()

    val configuration = LocalConfiguration.current
    val isLandscape = configuration.orientation == Configuration.ORIENTATION_LANDSCAPE

    Scaffold { padding ->
        Column(
            modifier =
                Modifier
                    .fillMaxSize()
                    .padding(padding)
                    .systemBarsPadding()
                    .padding(horizontal = 16.dp, vertical = 8.dp),
        ) {
            ModeHeader(
                state = state,
                onEvent = onEvent,
                onOpenMenu = { menuOpen = true },
            )
            Display(
                expression = state.expression,
                preview = state.liveResult,
                error = state.errorMessage,
                modifier =
                    Modifier
                        .fillMaxWidth()
                        // Smaller display weight in landscape so the keypad
                        // (the more space-hungry component, especially with
                        // 9 rows in scientific mode) actually fits.
                        .weight(if (isLandscape) DISPLAY_WEIGHT_LANDSCAPE else DISPLAY_WEIGHT_PORTRAIT),
            )
            Spacer(Modifier.size(8.dp))
            Keypad(
                scientific = state.scientific,
                isLandscape = isLandscape,
                onEvent = onEvent,
                // Keypad takes the remaining vertical space; weighted
                // rows below then divide it so nothing overflows the
                // viewport regardless of orientation or screen size.
                modifier = Modifier.weight(if (isLandscape) 1f else 2f),
            )
        }
    }

    if (menuOpen) {
        ModalBottomSheet(
            sheetState = sheetState,
            onDismissRequest = { menuOpen = false },
        ) {
            SettingsSheetContent(
                state = state,
                onEvent = onEvent,
                onClose = {
                    scope.launch { sheetState.hide() }
                    menuOpen = false
                },
            )
        }
    }
}

/**
 * App-level settings sheet, summoned from the top-right hamburger button.
 *
 * Houses the basic-vs-advanced toggle today; future settings (theme,
 * haptics, decimal precision) will land here too rather than crowding
 * the main screen.
 */
@Composable
private fun SettingsSheetContent(
    state: BasicCalculatorUiState,
    onEvent: (BasicCalculatorEvent) -> Unit,
    onClose: () -> Unit,
) {
    Column(modifier = Modifier.padding(24.dp)) {
        Text("Calculator", style = MaterialTheme.typography.titleLarge)
        Spacer(Modifier.size(24.dp))

        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween,
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text("Advanced mode", style = MaterialTheme.typography.titleMedium)
                Text(
                    text = "Show scientific functions (sin, cos, log, ^, memory)",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
            Switch(
                checked = state.scientific,
                onCheckedChange = {
                    onEvent(BasicCalculatorEvent.ToggleScientific)
                    onClose()
                },
            )
        }

        Spacer(Modifier.size(16.dp))
        HorizontalDivider()
        Spacer(Modifier.size(16.dp))
        Text(
            text = "More settings (theme, haptics, currency, …) land here in a future build.",
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
        Spacer(Modifier.size(24.dp))
    }
}

/**
 * Top header: the DEG/RAD chip when scientific mode is on, an `M` chip
 * when memory holds a non-zero value, and a hamburger icon on the right
 * that opens the settings bottom sheet.
 */
@Composable
private fun ModeHeader(
    state: BasicCalculatorUiState,
    onEvent: (BasicCalculatorEvent) -> Unit,
    onOpenMenu: () -> Unit,
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(8.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        if (state.scientific) {
            AssistChip(
                onClick = { onEvent(BasicCalculatorEvent.ToggleAngleMode) },
                label = { Text(if (state.angleMode == AngleMode.Degree) "DEG" else "RAD") },
            )
        }
        if (state.memory != BigDecimal.ZERO) {
            AssistChip(
                onClick = { onEvent(BasicCalculatorEvent.MemoryRecall) },
                label = { Text("M") },
                colors =
                    AssistChipDefaults.assistChipColors(
                        containerColor = MaterialTheme.colorScheme.secondaryContainer,
                        labelColor = MaterialTheme.colorScheme.onSecondaryContainer,
                    ),
            )
        }
        Spacer(Modifier.weight(1f))
        IconButton(onClick = onOpenMenu) {
            Icon(imageVector = Icons.Filled.Menu, contentDescription = "Open menu")
        }
    }
}

/**
 * Result display: the entered expression up top, with a smaller live
 * preview (or error) beneath it.
 */
@Composable
private fun Display(
    expression: String,
    preview: String?,
    error: String?,
    modifier: Modifier = Modifier,
) {
    Box(
        modifier = modifier,
        contentAlignment = Alignment.BottomEnd,
    ) {
        Column(horizontalAlignment = Alignment.End) {
            Text(
                text = expression.ifBlank { "0" },
                style = MaterialTheme.typography.displayMedium,
                textAlign = TextAlign.End,
            )
            when {
                error != null ->
                    Text(
                        text = error,
                        style = MaterialTheme.typography.titleMedium,
                        color = MaterialTheme.colorScheme.error,
                    )
                preview != null ->
                    Text(
                        text = "= $preview",
                        style = MaterialTheme.typography.titleMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
            }
        }
    }
}

/**
 * Keypad. Always shows the 4-column basic grid; in scientific mode adds
 * trig/log/power/memory keys.
 *
 * Layout adapts to orientation:
 *  - **Portrait**: scientific rows stack on top of the basic rows.
 *  - **Landscape**: scientific block sits to the left of the basic
 *    block so neither overflows when the viewport is short.
 *
 * Rows use `Modifier.weight(1f)` so they always divide the available
 * vertical space, never the other way around - this is what stops the
 * keypad from blowing through the viewport when the height shrinks
 * (landscape, foldables, small phones).
 */
@Composable
private fun Keypad(
    scientific: Boolean,
    isLandscape: Boolean,
    onEvent: (BasicCalculatorEvent) -> Unit,
    modifier: Modifier = Modifier,
) {
    val basicRows =
        listOf(
            listOf(Key.Clear, Key.LeftParen, Key.RightParen, Key.Symbol("%"), Key.Symbol("÷")),
            listOf(Key.Symbol("7"), Key.Symbol("8"), Key.Symbol("9"), Key.Symbol("×")),
            listOf(Key.Symbol("4"), Key.Symbol("5"), Key.Symbol("6"), Key.Symbol("-")),
            listOf(Key.Symbol("1"), Key.Symbol("2"), Key.Symbol("3"), Key.Symbol("+")),
            listOf(Key.Backspace, Key.Symbol("0"), Key.Symbol("."), Key.Equals),
        )
    val scientificRows =
        listOf(
            listOf(Key.Function("sin"), Key.Function("cos"), Key.Function("tan"), Key.Function("sqrt", "√")),
            listOf(Key.Function("asin", "sin⁻¹"), Key.Function("acos", "cos⁻¹"), Key.Function("atan", "tan⁻¹"), Key.Symbol("^")),
            listOf(Key.Function("log"), Key.Function("ln"), Key.Symbol("π"), Key.Symbol("e")),
            listOf(Key.MemoryClear, Key.MemoryRecall, Key.MemoryAdd, Key.MemorySubtract),
        )

    if (scientific && isLandscape) {
        // Landscape scientific: sci keys on the left, basic on the right.
        Row(
            modifier = modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            KeypadGrid(
                rows = scientificRows,
                modifier = Modifier.weight(1f).fillMaxHeight(),
                onEvent = onEvent,
            )
            KeypadGrid(
                rows = basicRows,
                modifier = Modifier.weight(1f).fillMaxHeight(),
                onEvent = onEvent,
            )
        }
    } else {
        KeypadGrid(
            rows = if (scientific) scientificRows + basicRows else basicRows,
            modifier = modifier.fillMaxWidth(),
            onEvent = onEvent,
        )
    }
}

@Composable
private fun KeypadGrid(
    rows: List<List<Key>>,
    modifier: Modifier,
    onEvent: (BasicCalculatorEvent) -> Unit,
) {
    Column(
        modifier = modifier,
        verticalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        rows.forEach { row ->
            KeypadRow(
                row = row,
                modifier = Modifier.fillMaxWidth().weight(1f).heightIn(min = MIN_KEY_HEIGHT),
                onEvent = onEvent,
            )
        }
    }
}

@Composable
private fun KeypadRow(
    row: List<Key>,
    modifier: Modifier,
    onEvent: (BasicCalculatorEvent) -> Unit,
) {
    Row(
        modifier = modifier,
        horizontalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        row.forEach { key ->
            KeyButton(
                key = key,
                onEvent = onEvent,
                modifier =
                    Modifier
                        .weight(1f)
                        .fillMaxHeight(),
            )
        }
    }
}

/**
 * Sealed key descriptor.
 *
 * Each variant knows how to render itself and which [BasicCalculatorEvent]
 * to dispatch, so the [KeypadRow] does not need a `when` over every label.
 */
private sealed interface Key {
    val label: String

    /** A character key (digit, operator, parenthesis, constant). */
    data class Symbol(
        override val label: String,
    ) : Key

    /**
     * A named function key. Tapping the key appends `<keyword>(` to the
     * expression; the closing `)` is the user's responsibility (or comes
     * from auto-close on `=`).
     */
    data class Function(
        val keyword: String,
        override val label: String = keyword,
    ) : Key

    data object LeftParen : Key {
        override val label: String = "("
    }

    data object RightParen : Key {
        override val label: String = ")"
    }

    data object Clear : Key {
        override val label: String = "C"
    }

    data object Backspace : Key {
        override val label: String = "⌫"
    }

    data object Equals : Key {
        override val label: String = "="
    }

    data object MemoryClear : Key {
        override val label: String = "MC"
    }

    data object MemoryRecall : Key {
        override val label: String = "MR"
    }

    data object MemoryAdd : Key {
        override val label: String = "M+"
    }

    data object MemorySubtract : Key {
        override val label: String = "M-"
    }
}

@Composable
private fun KeyButton(
    key: Key,
    onEvent: (BasicCalculatorEvent) -> Unit,
    modifier: Modifier = Modifier,
) {
    val click: () -> Unit = {
        when (key) {
            is Key.Symbol -> onEvent(BasicCalculatorEvent.Append(key.label))
            is Key.Function -> onEvent(BasicCalculatorEvent.Append("${key.keyword}("))
            Key.LeftParen -> onEvent(BasicCalculatorEvent.Append("("))
            Key.RightParen -> onEvent(BasicCalculatorEvent.Append(")"))
            Key.Clear -> onEvent(BasicCalculatorEvent.Clear)
            Key.Backspace -> onEvent(BasicCalculatorEvent.Backspace)
            Key.Equals -> onEvent(BasicCalculatorEvent.Equals)
            Key.MemoryClear -> onEvent(BasicCalculatorEvent.MemoryClear)
            Key.MemoryRecall -> onEvent(BasicCalculatorEvent.MemoryRecall)
            Key.MemoryAdd -> onEvent(BasicCalculatorEvent.MemoryAdd)
            Key.MemorySubtract -> onEvent(BasicCalculatorEvent.MemorySubtract)
        }
    }

    // Rounded-square keys: 20dp radius keeps the soft Material 3 feel while
    // reading as a square rather than the default near-circular pill shape.
    val keyShape = RoundedCornerShape(20.dp)

    // Operators and the equals key get a larger type ramp than digits so the
    // calculator's "action" keys read as visually heavier; functions and
    // memory keys use a slightly smaller style so longer labels still fit.
    val labelStyle = when {
        key is Key.Symbol && key.label in OperatorLabels -> MaterialTheme.typography.displaySmall
        key is Key.Equals -> MaterialTheme.typography.displaySmall
        key is Key.Function -> MaterialTheme.typography.titleLarge
        key is Key.MemoryClear ||
            key is Key.MemoryRecall ||
            key is Key.MemoryAdd ||
            key is Key.MemorySubtract -> MaterialTheme.typography.titleLarge
        else -> MaterialTheme.typography.headlineLarge
    }

    when (key) {
        Key.Equals ->
            Button(
                onClick = click,
                modifier = modifier,
                shape = keyShape,
                contentPadding = PaddingValues(0.dp),
                colors =
                    ButtonDefaults.buttonColors(
                        containerColor = MaterialTheme.colorScheme.primary,
                        contentColor = MaterialTheme.colorScheme.onPrimary,
                    ),
            ) { Text(key.label, style = labelStyle) }

        else ->
            FilledTonalButton(
                onClick = click,
                modifier = modifier,
                shape = keyShape,
                contentPadding = PaddingValues(0.dp),
            ) {
                Text(key.label, style = labelStyle)
            }
    }
}

/** Labels of arithmetic-operator keys that get the bigger display type ramp. */
private val OperatorLabels = setOf("+", "-", "×", "÷", "%", "^", "π", "e")

/**
 * Minimum height per keypad row. 48dp matches the Material 3 touch-target
 * minimum so we never collapse below comfortable thumb-reach even on
 * tightly packed landscape devices.
 */
private val MIN_KEY_HEIGHT = 48.dp

// Weights for the Display vs Keypad split inside the screen Column. The
// display is more flexible in portrait (more vertical real estate) and
// is squeezed to a sliver in landscape so the keypad can breathe.
private const val DISPLAY_WEIGHT_PORTRAIT = 1f
private const val DISPLAY_WEIGHT_LANDSCAPE = 0.6f

// ----- Previews -----

@PreviewLightDark
@Composable
private fun BasicCalculatorPreview() {
    CalculatorTheme {
        Surface {
            BasicCalculatorScreenContent(
                state =
                    BasicCalculatorUiState(
                        expression = "12 × 3 + 4",
                        liveResult = "40",
                    ),
                onEvent = {},
            )
        }
    }
}

@PreviewLightDark
@Composable
private fun ScientificCalculatorPreview() {
    CalculatorTheme {
        Surface {
            BasicCalculatorScreenContent(
                state =
                    BasicCalculatorUiState(
                        expression = "sin(30)+log(100)",
                        liveResult = "2.5",
                        scientific = true,
                        angleMode = AngleMode.Degree,
                        memory = BigDecimal("42"),
                    ),
                onEvent = {},
            )
        }
    }
}

@PreviewLightDark
@Composable
private fun BasicCalculatorErrorPreview() {
    CalculatorTheme {
        Surface {
            BasicCalculatorScreenContent(
                state =
                    BasicCalculatorUiState(
                        expression = "5 ÷ 0",
                        errorMessage = "Can't divide by zero",
                    ),
                onEvent = {},
            )
        }
    }
}
