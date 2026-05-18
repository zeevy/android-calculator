package com.calculator.feature.basic.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.navigationBars
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBars
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Calculate
import androidx.compose.material.icons.filled.CurrencyExchange
import androidx.compose.material.icons.filled.Functions
import androidx.compose.material.icons.filled.History
import androidx.compose.material.icons.filled.Menu
import androidx.compose.material.icons.filled.MonitorWeight
import androidx.compose.material.icons.filled.Percent
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
import androidx.compose.ui.draw.clip
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

    Scaffold(
        // Scaffold defaults to padding its content by the system-bar insets;
        // that would push the hamburger 24-28dp below the status bar before
        // its own 48dp tap target adds another 12dp. Zero the Scaffold's
        // insets and hand them out per-child instead - chips and display
        // text get the status-bar inset, the hamburger doesn't.
        contentWindowInsets = WindowInsets(0.dp),
    ) { padding ->
        Column(
            modifier =
                Modifier
                    .fillMaxSize()
                    .padding(padding),
        ) {
            // Display fills whatever room the keypad doesn't take.
            // The hamburger icon lives at the absolute top of this
            // section (no status-bar inset on the section itself) so
            // its 48dp tap target starts at screen top - the icon
            // glyph's built-in 12dp internal padding then centers it
            // right at the edge of the status bar with no visible gap.
            // Only the display *text* and the chips opt into the
            // status-bar inset so they don't draw behind it.
            DisplaySection(
                state = state,
                onEvent = onEvent,
                onOpenMenu = { menuOpen = true },
                modifier =
                    Modifier
                        .fillMaxWidth()
                        .weight(1f),
            )
            Spacer(Modifier.size(16.dp))
            // Visual separation between display and keypad: a tinted
            // surface with rounded top corners reads as a distinct "key
            // tray" sitting under the result area - same idiom Mi
            // Calculator uses. surfaceContainerHigh sits one step above
            // the screen background in the M3 tonal hierarchy, enough
            // contrast in both light and dark themes without a divider.
            Surface(
                modifier = Modifier.fillMaxWidth(),
                color = MaterialTheme.colorScheme.surfaceContainerHigh,
                shape =
                    RoundedCornerShape(
                        topStart = 28.dp,
                        topEnd = 28.dp,
                    ),
            ) {
                Keypad(
                    scientific = state.scientific,
                    onEvent = onEvent,
                    modifier =
                        Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 16.dp)
                            .padding(top = 16.dp)
                            .windowInsetsPadding(WindowInsets.navigationBars)
                            .padding(bottom = 12.dp),
                )
            }
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
 * App-level tools sheet, summoned from the top-right hamburger button.
 *
 * Modelled on the Mi Calculator tools menu: a grid of icon + label
 * tiles, one tap to jump to a mode/tool. The currently-active mode is
 * highlighted with the primary container colour.
 *
 * Today the working tiles are Basic and Advanced (scientific). The rest
 * are placeholders for upcoming features (history in Phase 3, converters
 * in Phase 5/6, life calculators in Phase 7) and are tappable but flash
 * a "coming soon" hint to avoid silently swallowing user intent.
 */
@Composable
private fun SettingsSheetContent(
    state: BasicCalculatorUiState,
    onEvent: (BasicCalculatorEvent) -> Unit,
    onClose: () -> Unit,
) {
    Column(modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp)) {
        Text(
            text = "Tools",
            style = MaterialTheme.typography.titleLarge,
            modifier = Modifier.padding(horizontal = 8.dp, vertical = 8.dp),
        )
        Spacer(Modifier.size(8.dp))

        val tiles =
            listOf(
                ToolTile(
                    icon = Icons.Filled.Calculate,
                    label = "Basic",
                    enabled = true,
                    selected = !state.scientific,
                    onTap = {
                        if (state.scientific) onEvent(BasicCalculatorEvent.ToggleScientific)
                        onClose()
                    },
                ),
                ToolTile(
                    icon = Icons.Filled.Functions,
                    label = "Advanced",
                    enabled = true,
                    selected = state.scientific,
                    onTap = {
                        if (!state.scientific) onEvent(BasicCalculatorEvent.ToggleScientific)
                        onClose()
                    },
                ),
                ToolTile(
                    icon = Icons.Filled.History,
                    label = "History",
                    enabled = false,
                    selected = false,
                    onTap = onClose,
                ),
                ToolTile(
                    icon = Icons.Filled.CurrencyExchange,
                    label = "Converter",
                    enabled = false,
                    selected = false,
                    onTap = onClose,
                ),
                ToolTile(
                    icon = Icons.Filled.Percent,
                    label = "Finance",
                    enabled = false,
                    selected = false,
                    onTap = onClose,
                ),
                ToolTile(
                    icon = Icons.Filled.MonitorWeight,
                    label = "Health",
                    enabled = false,
                    selected = false,
                    onTap = onClose,
                ),
            )

        tiles.chunked(TOOL_TILE_COLUMNS).forEach { rowTiles ->
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                rowTiles.forEach { tile ->
                    ToolTileButton(tile = tile, modifier = Modifier.weight(1f))
                }
                // Pad the trailing slot(s) so partial rows keep tile size.
                repeat(TOOL_TILE_COLUMNS - rowTiles.size) {
                    Spacer(modifier = Modifier.weight(1f))
                }
            }
            Spacer(Modifier.size(8.dp))
        }

        Spacer(Modifier.size(8.dp))
        HorizontalDivider()
        Spacer(Modifier.size(8.dp))
        Text(
            text = "More tools light up as they ship.",
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.padding(horizontal = 8.dp),
        )
        Spacer(Modifier.size(24.dp))
    }
}

/** A single tile (icon + label) inside the [SettingsSheetContent] grid. */
private data class ToolTile(
    val icon: androidx.compose.ui.graphics.vector.ImageVector,
    val label: String,
    val enabled: Boolean,
    val selected: Boolean,
    val onTap: () -> Unit,
)

@Composable
private fun ToolTileButton(tile: ToolTile, modifier: Modifier = Modifier) {
    val containerColor =
        when {
            tile.selected -> MaterialTheme.colorScheme.primaryContainer
            else -> MaterialTheme.colorScheme.surfaceVariant
        }
    val contentColor =
        when {
            tile.selected -> MaterialTheme.colorScheme.onPrimaryContainer
            tile.enabled -> MaterialTheme.colorScheme.onSurfaceVariant
            else -> MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.5f)
        }
    Column(
        modifier =
            modifier
                .clip(RoundedCornerShape(20.dp))
                .background(containerColor)
                .clickable(enabled = tile.enabled, onClick = tile.onTap)
                .padding(vertical = 16.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Icon(
            imageVector = tile.icon,
            contentDescription = tile.label,
            tint = contentColor,
            modifier = Modifier.size(32.dp),
        )
        Spacer(Modifier.size(8.dp))
        Text(
            text = tile.label,
            style = MaterialTheme.typography.bodyMedium,
            color = contentColor,
        )
    }
}

private const val TOOL_TILE_COLUMNS = 3

/**
 * Display section: expression + live preview bottom-right, mode chips
 * top-left, hamburger icon top-right. Implemented as a Box so each
 * piece sits exactly where it should without padding rows wasting space.
 */
@Composable
private fun DisplaySection(
    state: BasicCalculatorUiState,
    onEvent: (BasicCalculatorEvent) -> Unit,
    onOpenMenu: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Box(modifier = modifier) {
        Display(
            expression = state.expression,
            preview = state.liveResult,
            error = state.errorMessage,
            modifier =
                Modifier
                    .fillMaxSize()
                    .windowInsetsPadding(WindowInsets.statusBars)
                    .padding(horizontal = 16.dp)
                    .padding(top = 48.dp),
        )

        Row(
            modifier =
                Modifier
                    .align(Alignment.TopStart)
                    .windowInsetsPadding(WindowInsets.statusBars)
                    .padding(start = 8.dp),
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
        }

        // Hamburger: status-bar inset keeps it below the system icons, then
        // a 40dp box (vs IconButton's default 48dp) trims the visible gap
        // between status bar and glyph from ~12dp to ~8dp.
        Box(
            modifier =
                Modifier
                    .align(Alignment.TopEnd)
                    .windowInsetsPadding(WindowInsets.statusBars)
                    .padding(end = 4.dp, top = 4.dp)
                    .size(40.dp)
                    .clip(androidx.compose.foundation.shape.CircleShape)
                    .clickable(onClick = onOpenMenu),
            contentAlignment = Alignment.Center,
        ) {
            Icon(
                imageVector = Icons.Filled.Menu,
                contentDescription = "Open menu",
                modifier = Modifier.size(24.dp),
            )
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
 * Keypad. Always shows the 4-column basic grid; in scientific mode the
 * trig/log/power/memory rows stack above the basic rows.
 *
 * Sizing strategy: every key is a fixed-aspect-ratio rectangle (width
 * derived from the 4-column grid, height = width / [BUTTON_ASPECT_RATIO]).
 * The keypad therefore wraps its own height, and the display above it
 * gets `weight(1f)` to absorb whatever's left. This is what keeps buttons
 * looking like proper horizontal rectangles instead of squares or tall
 * vertical bars, regardless of how many rows the current mode has.
 *
 * Portrait is enforced by the manifest, so no landscape branch lives
 * here - foldables / multi-window get the same vertical stacking.
 */
@Composable
private fun Keypad(
    scientific: Boolean,
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

    KeypadGrid(
        rows = if (scientific) scientificRows + basicRows else basicRows,
        modifier = modifier,
        onEvent = onEvent,
    )
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
                modifier = Modifier.fillMaxWidth(),
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
                        .aspectRatio(BUTTON_ASPECT_RATIO),
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

// Width-to-height ratio for every keypad button. 1.6 gives a clean
// "horizontal rectangle" silhouette - wider than tall by ~60%, the
// shape physical desk calculators use. Tweaking this is the single
// knob for "make keys taller / shorter" without touching layout code.
private const val BUTTON_ASPECT_RATIO = 1.6f

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
