package com.calculator.feature.basic.ui

import android.media.AudioManager
import android.media.ToneGenerator
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.gestures.awaitEachGesture
import androidx.compose.foundation.gestures.awaitFirstDown
import androidx.compose.foundation.gestures.waitForUpOrCancellation
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.IntrinsicSize
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBars
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBars
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Menu
import androidx.compose.material3.AssistChip
import androidx.compose.material3.AssistChipDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.compositionLocalOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.clipToBounds
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.lerp
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.rememberTextMeasurer
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.tooling.preview.PreviewLightDark
import androidx.compose.ui.unit.Constraints
import androidx.compose.ui.unit.TextUnit
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.calculator.R
import com.calculator.core.designsystem.theme.CalculatorTheme
import com.calculator.core.math.AngleMode
import com.calculator.feature.lifecalc.ToolsMenuOverlay
import com.calculator.feature.lifecalc.ToolsMenuSheet
import com.calculator.navigation.BasicCalculatorRoute
import kotlinx.coroutines.delay
import java.math.BigDecimal

/**
 * Top-level composable for the basic calculator screen.
 *
 * Pulls the [BasicCalculatorViewModel] from Hilt and renders the
 * stateless [BasicCalculatorScreenContent] - this split keeps previews
 * possible without a ViewModel and makes UI tests straightforward.
 */
@Composable
fun BasicCalculatorScreen(
    onNavigate: (Any) -> Unit = {},
    viewModel: BasicCalculatorViewModel = hiltViewModel(),
) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    // On first composition, pick up any expression or scientific-mode
    // hint a tool page stashed for us. The holder is one-shot - read
    // once, then null it out so re-navigating back doesn't re-apply.
    androidx.compose.runtime.LaunchedEffect(Unit) {
        val holder = com.calculator.feature.lifecalc.PendingExpressionHolder
        holder.scientificMode?.let { desired ->
            holder.scientificMode = null
            if (state.scientific != desired) {
                viewModel.onEvent(BasicCalculatorEvent.ToggleScientific)
            }
        }
        holder.expression?.let { pending ->
            holder.expression = null
            viewModel.onEvent(BasicCalculatorEvent.Clear)
            viewModel.onEvent(BasicCalculatorEvent.Append(pending))
        }
    }
    BasicCalculatorScreenContent(
        state = state,
        onEvent = viewModel::onEvent,
        onNavigate = onNavigate,
    )
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
    onNavigate: (Any) -> Unit = {},
) {
    var openSheet by remember { mutableStateOf<ToolsMenuSheet?>(null) }
    val tones = rememberKeyToneGenerator()
    // Settings drives sound and haptics toggles. Falling back to defaults
    // (both on) when the settings VM hasn't emitted yet keeps the UI
    // responsive during the first frame.
    val settingsViewModel: com.calculator.feature.settings.SettingsViewModel = hiltViewModel()
    val userSettings by settingsViewModel.settings.collectAsStateWithLifecycle()
    val tonesIfEnabled = if (userSettings.sound) tones else null

    CompositionLocalProvider(
        LocalKeyTones provides tonesIfEnabled,
        LocalHapticsEnabled provides userSettings.haptics,
    ) {
        Scaffold(
            // Hand out insets per-child: outer Column absorbs the status-bar
            // inset (so the display card sits below the system icons), and
            // the keypad card absorbs the navigation-bar inset on its bottom
            // edge (so the bottom row of keys doesn't collide with the
            // gesture pill).
            contentWindowInsets = WindowInsets(0.dp),
        ) { padding ->
            Column(
                modifier =
                    Modifier
                        .fillMaxSize()
                        .padding(padding)
                        .windowInsetsPadding(WindowInsets.statusBars)
                        .padding(horizontal = 12.dp)
                        .padding(top = 8.dp),
            ) {
                // Display sits directly on the screen background - no card,
                // no border. The typographic hierarchy inside (small muted
                // expression line, big bold result) carries the visual
                // weight on its own, and the gap before the keypad tray
                // does the section separation.
                DisplaySection(
                    state = state,
                    onEvent = onEvent,
                    onOpenMenu = { openSheet = ToolsMenuSheet.Tools },
                    modifier =
                        Modifier
                            .fillMaxWidth()
                            .weight(1f),
                )
                Spacer(Modifier.size(8.dp))
                HorizontalDivider(
                    color = MaterialTheme.colorScheme.outlineVariant,
                    thickness = 1.dp,
                )
                Spacer(Modifier.size(12.dp))
                // No keypad tray. iOS calculator puts keys directly on the
                // screen background; the colored keys carry the visual
                // structure on their own.
                Keypad(
                    scientific = state.scientific,
                    onEvent = onEvent,
                    modifier =
                        Modifier
                            .fillMaxWidth()
                            .windowInsetsPadding(WindowInsets.navigationBars)
                            .padding(bottom = 12.dp),
                )
            }
        }

        ToolsMenuOverlay(
            openSheet = openSheet,
            onDismiss = { openSheet = null },
            currentRoute = BasicCalculatorRoute,
            isOnBasicCalc = true,
            scientific = state.scientific,
            onToggleScientific = { onEvent(BasicCalculatorEvent.ToggleScientific) },
            onOpenHistory = { openSheet = ToolsMenuSheet.History },
            onOpenSettings = { openSheet = ToolsMenuSheet.Settings },
            onNavigate = onNavigate,
            onReuseExpression = { expr ->
                onEvent(BasicCalculatorEvent.Clear)
                onEvent(BasicCalculatorEvent.Append(expr))
            },
        )
    } // CompositionLocalProvider
}

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
    val clipboard = androidx.compose.ui.platform.LocalClipboardManager.current
    val context = androidx.compose.ui.platform.LocalContext.current
    Box(modifier = modifier) {
        Display(
            expression = state.expression,
            preview = state.liveResult,
            error = state.errorMessage,
            lastCommittedExpression = state.lastCommittedExpression,
            lastValidPreview = state.lastValidPreview,
            onCopyResult = { text ->
                if (text.isNotBlank()) {
                    clipboard.setText(
                        androidx.compose.ui.text
                            .AnnotatedString(text),
                    )
                    android.widget.Toast
                        .makeText(context, R.string.basic_copied, android.widget.Toast.LENGTH_SHORT)
                        .show()
                }
            },
            onPasteRequested = {
                val pasted = clipboard.getText()?.text.orEmpty()
                val number = extractLeadingNumber(pasted)
                if (number != null) {
                    onEvent(BasicCalculatorEvent.Append(number))
                } else {
                    android.widget.Toast
                        .makeText(context, R.string.basic_paste_no_number, android.widget.Toast.LENGTH_SHORT)
                        .show()
                }
            },
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

        // Hamburger: 48dp IconButton matches the tool-page scaffold's
        // top-right hamburger pixel-for-pixel so switching between basic
        // calc and a tool doesn't visibly shift the icon.
        androidx.compose.material3.IconButton(
            onClick = onOpenMenu,
            modifier =
                Modifier
                    .align(Alignment.TopEnd)
                    .windowInsetsPadding(WindowInsets.statusBars),
        ) {
            Icon(
                imageVector = Icons.Filled.Menu,
                contentDescription = stringResource(R.string.basic_open_menu),
                modifier = Modifier.size(24.dp),
            )
        }
    }
}

/**
 * Result display: always two lines.
 *
 *  - **Top line** (smaller, muted): the *input* - either the just-
 *    committed expression after `=`, or the expression being typed
 *    when there's a live preview to show below. Empty otherwise (but
 *    the layout slot is still allocated so the bottom line never
 *    shifts mid-typing).
 *  - **Bottom line** (large bold): the *result* - either the live
 *    preview while typing, the canonical result after `=`, the error
 *    message (red), or `0` as a default when the calculator is empty.
 *
 * Both lines are always rendered so the display never "switches"
 * between one-line and two-line layouts as the user types.
 *
 * State → (top, bottom) mapping:
 *
 *     | Calculator state              | Top line       | Bottom line       |
 *     |-------------------------------|----------------|-------------------|
 *     | Empty / just digits ("5")     | (blank)        | expression / "0"  |
 *     | Mid-expression ("5+3")        | expression     | liveResult        |
 *     | Post-equals ("5+3=8")         | committed expr | expression (8)    |
 *     | After-= chain ("8+2")         | expression     | liveResult        |
 *     | Error                         | expression     | error (red)       |
 *
 * `internal` (not `private`) so the headless Robolectric Compose UI
 * tests can host this composable directly with synthetic state and
 * assert the rendered text. It's still hidden from other modules.
 */
@Suppress("LongParameterList")
@OptIn(ExperimentalFoundationApi::class)
@Composable
internal fun Display(
    expression: String,
    preview: String?,
    error: String?,
    lastCommittedExpression: String?,
    lastValidPreview: String?,
    modifier: Modifier = Modifier,
    onCopyResult: (String) -> Unit = {},
    onPasteRequested: () -> Unit = {},
) {
    // Top (input) line: always the expression, with the committed-
    // expression preferred when it's set (i.e. after `=`). We do NOT
    // gate this on `preview != null` because the live preview can
    // disappear mid-typing - typing the next operator (`5+3` -> `5+3+`)
    // makes the preview unevaluable, and gating on it would blank the
    // top line on every operator press. Showing the expression
    // unconditionally keeps the layout stable across every keystroke.
    val topText: String = lastCommittedExpression ?: expression
    // Bottom (result) line: result only - never the expression itself.
    // Priority order:
    //   1. error message (red)
    //   2. live preview from the current expression
    //   3. lastValidPreview - the most recent computable result,
    //      held across non-evaluable intermediate states so typing
    //      `5+3` → `5+3+` keeps showing `8` instead of blanking
    //   4. "0" if the calculator is at rest (expression blank and
    //      nothing held)
    //   5. "" otherwise - shouldn't happen in practice because
    //      lastValidPreview persists whenever the expression isn't
    //      blank, but a defensive fallback keeps the line non-null
    val bottomText: String =
        error
            ?: preview
            ?: lastValidPreview
            ?: if (expression.isBlank()) "0" else ""
    val bottomColor =
        if (error != null) {
            MaterialTheme.colorScheme.error
        } else {
            MaterialTheme.colorScheme.onSurface
        }
    val bottomFontWeight =
        if (error != null) FontWeight.SemiBold else FontWeight.Bold
    val bottomBaseStyle =
        if (error != null) {
            MaterialTheme.typography.displaySmall
        } else {
            MaterialTheme.typography.displayLarge
        }

    Box(
        modifier = modifier.clipToBounds(),
        contentAlignment = Alignment.BottomEnd,
    ) {
        Column(horizontalAlignment = Alignment.End, modifier = Modifier.fillMaxWidth()) {
            // Top line always rendered (even when blank) so the bottom
            // line doesn't bounce between vertical positions as the user
            // moves between "fresh digit" and "expression with preview"
            // states.
            AutoSizeText(
                text = topText,
                style = MaterialTheme.typography.headlineSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                maxFontSize = MaterialTheme.typography.headlineSmall.fontSize,
                minFontSize = DISPLAY_EXPRESSION_MIN_SIZE,
                maxLines = DISPLAY_EXPRESSION_MAX_LINES,
                modifier =
                    Modifier
                        .fillMaxWidth()
                        // Long-press the expression line to paste a number
                        // from the clipboard. A plain tap is a no-op so the
                        // pasted text doesn't fire on every accidental tap.
                        .combinedClickable(
                            onClick = {},
                            onLongClick = onPasteRequested,
                            indication = null,
                            interactionSource =
                                remember {
                                    androidx.compose.foundation.interaction
                                        .MutableInteractionSource()
                                },
                        ),
            )
            Spacer(Modifier.size(4.dp))
            // Bottom line: the dominant element. Style flips to the
            // error palette + slightly smaller ramp when an error is
            // showing so the user sees the message comfortably.
            //
            // Fixed font size (no auto-shrink). If the text would overflow
            // two lines at the base size, Compose clips with an ellipsis
            // at the trailing edge - by design, per user preference, to
            // keep the typography rhythm stable across every keystroke
            // instead of having the result jiggle smaller as digits grow.
            Text(
                text = bottomText,
                style = bottomBaseStyle.copy(fontWeight = bottomFontWeight),
                color = bottomColor,
                maxLines = DISPLAY_RESULT_MAX_LINES,
                softWrap = true,
                overflow = TextOverflow.Ellipsis,
                textAlign = TextAlign.End,
                modifier =
                    Modifier
                        .fillMaxWidth()
                        // Long-press the result line to copy it to the
                        // clipboard. Tap does nothing - the result line
                        // isn't an action surface, just a value.
                        .combinedClickable(
                            onClick = {},
                            onLongClick = { onCopyResult(bottomText) },
                            indication = null,
                            interactionSource =
                                remember {
                                    androidx.compose.foundation.interaction
                                        .MutableInteractionSource()
                                },
                        ),
            )
        }
    }
}

/**
 * Right-aligned auto-sizing text. Wraps to up to [maxLines] lines and
 * scales the font size down only when even the full line count would
 * overflow the available width. Measured once per
 * (text, width, lines) tuple via a [TextMeasurer] so there's no
 * recompose loop.
 *
 * Strategy:
 *  1. Try the max font size with up to maxLines of soft-wrapped text.
 *  2. If that still overflows width OR needs more than maxLines, drop
 *     [AUTO_SIZE_STEP_SP] and re-measure.
 *  3. Stop at [minFontSize] - if the string still doesn't fit there,
 *     the trailing characters clip (the alternative is illegible 8sp
 *     text, which is worse than clipping).
 *
 * Each parameter maps to a distinct visual lever (size bounds, color,
 * line cap, modifier) that callers tweak independently, hence the
 * suppression of [LongParameterList].
 */
@Suppress("LongParameterList")
@Composable
private fun AutoSizeText(
    text: String,
    style: TextStyle,
    color: Color,
    maxFontSize: TextUnit,
    minFontSize: TextUnit,
    maxLines: Int = 1,
    modifier: Modifier = Modifier,
) {
    val measurer = rememberTextMeasurer()
    BoxWithConstraints(modifier = modifier, contentAlignment = Alignment.BottomEnd) {
        val maxWidthPx = constraints.maxWidth
        // The display box also has a bounded height (in scientific mode
        // the keypad takes most of the screen, leaving only ~130dp for
        // the display). Pass maxHeight into the measurer too so the
        // shrink loop terminates when text overflows *either* axis,
        // not just width. Without this, three lines of displayLarge
        // would overflow the box vertically and crowd the keypad.
        val maxHeightPx =
            if (constraints.hasBoundedHeight) constraints.maxHeight else Int.MAX_VALUE
        val measureConstraints = Constraints(maxWidth = maxWidthPx, maxHeight = maxHeightPx)
        val fontSize =
            remember(text, maxWidthPx, maxHeightPx, maxLines, maxFontSize, minFontSize, style) {
                var candidate = maxFontSize
                while (candidate.value > minFontSize.value) {
                    val measured =
                        measurer.measure(
                            text = AnnotatedString(text),
                            style = style.copy(fontSize = candidate),
                            maxLines = maxLines,
                            softWrap = maxLines > 1,
                            constraints = measureConstraints,
                        )
                    if (!measured.didOverflowWidth && !measured.didOverflowHeight) break
                    candidate = (candidate.value - AUTO_SIZE_STEP_SP).sp
                }
                candidate
            }
        Text(
            text = text,
            style = style.copy(fontSize = fontSize),
            color = color,
            maxLines = maxLines,
            softWrap = maxLines > 1,
            textAlign = TextAlign.End,
            modifier = Modifier.fillMaxWidth(),
        )
    }
}

private const val AUTO_SIZE_STEP_SP = 2f

// Floor for the muted expression-history line. Small so the shrink
// loop can actually find a size that fits in advanced mode (where the
// 9-row keypad leaves only ~100dp for the entire display). The result
// line below does not shrink - it renders at a fixed displayLarge size
// and ellipsizes if the value doesn't fit in two lines.
private val DISPLAY_EXPRESSION_MIN_SIZE = 10.sp

// Two lines apiece. The result line stays at its base font size and
// ellipsizes if the value can't fit in two lines (rare in practice).
// The expression-history line above shrinks instead of ellipsizing so
// the user can always see the whole expression they've typed. Both
// rows share the same vertical budget, so the keypad position doesn't
// shift between modes.
private const val DISPLAY_RESULT_MAX_LINES = 2
private const val DISPLAY_EXPRESSION_MAX_LINES = 2

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
 *
 * `internal` so the headless Compose UI test in src/test/ can host
 * the keypad and drive button presses against a fake onEvent capture.
 */
@Composable
internal fun Keypad(
    scientific: Boolean,
    onEvent: (BasicCalculatorEvent) -> Unit,
    modifier: Modifier = Modifier,
) {
    // The standalone Clear (C) key is gone. Long-pressing the backspace
    // key clears the whole expression - same gesture as a physical
    // calculator's "Clear" press-and-hold on the C/CE key.
    //
    // Memory keys and operators are interleaved across the top two
    // compact rows. The original layout grouped all memory keys
    // together; splitting them out gives the most-used operators
    // (÷ and %) the prominent top-right slots where the eye lands
    // first, and keeps M+/M- adjacent so the user can still treat
    // them as a pair.
    //
    // Basic-mode layout (rows numbered from the BOTTOM per user
    // direction):
    //   top  : MC MR ÷  %
    //   row 5: M+ M- ±  ×
    //   row 4: 7  8  9  -
    //   row 3: 4  5  6  [+ tall starts here, spans rows 2-3]
    //   row 2: 1  2  3  [+ continues]
    //   row 1: ⌫  0  .  =    (bottom)
    //
    // The tall + button lives in column 4 of rows 2 and 3, between -
    // (row 4 col 4) and = (row 1 col 4). The middle rows around it
    // (rows 2 and 3) are rendered together by [TallPlusBlock]; the
    // rows above and below it use the regular KeypadRow path.
    // % sits in column 3 and ÷ in column 4 so that the four arithmetic
    // operators (÷ × - +) and = line up as a single orange column down the
    // right edge; % is a grey modifier (iOS-style), not part of that column.
    val memoryRow =
        KeypadRowSpec(
            keys = listOf(Key.MemoryClear, Key.MemoryRecall, Key.Symbol("%"), Key.Symbol("÷")),
            compact = true,
        )
    val aboveTallPlusRows =
        listOf(
            KeypadRowSpec(
                listOf(Key.MemoryAdd, Key.MemorySubtract, Key.SignFlip, Key.Symbol("×")),
                compact = true,
            ),
            KeypadRowSpec(listOf(Key.Symbol("7"), Key.Symbol("8"), Key.Symbol("9"), Key.Symbol("-"))),
        )
    val belowTallPlusRow =
        KeypadRowSpec(listOf(Key.Backspace, Key.Symbol("0"), Key.Symbol("."), Key.Equals))
    // Four scientific rows, all compact. Parens slot into row 3
    // (replacing π/e there); π and e move to row 4 alongside x² and x³
    // shortcuts so every cell in advanced mode does something useful -
    // no empty placeholders.
    val scientificRows =
        listOf(
            KeypadRowSpec(
                listOf(Key.Function("sin"), Key.Function("cos"), Key.Function("tan"), Key.Function("sqrt", "√")),
                compact = true,
            ),
            KeypadRowSpec(
                listOf(Key.Function("asin", "sin⁻¹"), Key.Function("acos", "cos⁻¹"), Key.Function("atan", "tan⁻¹"), Key.Symbol("^")),
                compact = true,
            ),
            KeypadRowSpec(
                listOf(Key.Function("log"), Key.Function("ln"), Key.LeftParen, Key.RightParen),
                compact = true,
            ),
            KeypadRowSpec(
                listOf(Key.Symbol("π"), Key.Symbol("e"), Key.Squared, Key.Cubed),
                compact = true,
            ),
        )

    // Render order (top -> bottom):
    //   1. Scientific rows (advanced mode only).
    //   2. Memory row: `MC MR % ÷`.
    //   3. aboveTallPlusRows: `M+ M- ± ×`, then `7 8 9 -`.
    //   4. TallPlusBlock: `4 5 6` / `1 2 3` on the left, tall + on
    //      the right covering both digit rows.
    //   5. belowTallPlusRow: `⌫ 0 . =`.
    // Columns 4 of rows 2-5 (÷ × - + =) form the single orange operator
    // column down the right edge.
    Column(
        modifier = modifier,
        verticalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        if (scientific) {
            scientificRows.forEach { spec ->
                KeypadRow(
                    row = spec.keys,
                    aspectRatio =
                        if (spec.compact) BUTTON_ASPECT_RATIO_COMPACT else BUTTON_ASPECT_RATIO,
                    compact = spec.compact,
                    modifier = Modifier.fillMaxWidth(),
                    onEvent = onEvent,
                )
            }
        }
        KeypadRow(
            row = memoryRow.keys,
            aspectRatio = BUTTON_ASPECT_RATIO_COMPACT,
            compact = true,
            modifier = Modifier.fillMaxWidth(),
            onEvent = onEvent,
        )
        aboveTallPlusRows.forEach { spec ->
            KeypadRow(
                row = spec.keys,
                aspectRatio =
                    if (spec.compact) BUTTON_ASPECT_RATIO_COMPACT else BUTTON_ASPECT_RATIO,
                compact = spec.compact,
                modifier = Modifier.fillMaxWidth(),
                onEvent = onEvent,
            )
        }
        TallPlusBlock(onEvent = onEvent)
        KeypadRow(
            row = belowTallPlusRow.keys,
            aspectRatio = BUTTON_ASPECT_RATIO,
            compact = false,
            modifier = Modifier.fillMaxWidth(),
            onEvent = onEvent,
        )
    }
}

/**
 * Two-row block in the middle of the basic keypad. The left three
 * columns carry the `4 5 6` and `1 2 3` digit rows; the right column
 * is a single double-height `+` button covering both rows, sitting
 * directly above the `=` key (which lives in the regular bottom row
 * just below this block).
 *
 * Implementation note: the surrounding KeypadRow calls use weight=1
 * across 4 cells per Row. This block uses a horizontal Row with the
 * left subtree at weight 3 (three keys plus their internal gaps) and
 * the right cell at weight 1. The aspect-ratio match between inner
 * and outer cells is approximate to within ~2dp (invisible), and
 * everything stays on the same 8dp baseline grid.
 */
@Composable
private fun TallPlusBlock(onEvent: (BasicCalculatorEvent) -> Unit) {
    Row(
        // height(IntrinsicSize.Min) pins the Row's height to the left
        // Column's wrap-content height (= 2 rows + 1 gap). Without
        // this, fillMaxHeight on the + button would expand it to take
        // whatever vertical space the parent Column has left, which is
        // usually a lot more than the 2-row equivalent we want.
        modifier = Modifier.fillMaxWidth().height(IntrinsicSize.Min),
        horizontalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        // Left: 3 cols x 2 rows of digit keys (4 5 6 / 1 2 3).
        Column(
            modifier = Modifier.weight(3f),
            verticalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            KeypadRow(
                row = listOf(Key.Symbol("4"), Key.Symbol("5"), Key.Symbol("6")),
                aspectRatio = BUTTON_ASPECT_RATIO,
                compact = false,
                modifier = Modifier.fillMaxWidth(),
                onEvent = onEvent,
            )
            KeypadRow(
                row = listOf(Key.Symbol("1"), Key.Symbol("2"), Key.Symbol("3")),
                aspectRatio = BUTTON_ASPECT_RATIO,
                compact = false,
                modifier = Modifier.fillMaxWidth(),
                onEvent = onEvent,
            )
        }
        // Right: single tall + button. fillMaxHeight matches the left
        // column's total height (2 rows + 1 gap).
        KeyButton(
            key = Key.Symbol("+"),
            onEvent = onEvent,
            compact = false,
            modifier =
                Modifier
                    .weight(1f)
                    .fillMaxHeight(),
        )
    }
}

/** A keypad row + per-row layout hints. [compact] uses a wider aspect ratio so the row is shorter. */
private data class KeypadRowSpec(val keys: List<Key>, val compact: Boolean = false)

@Composable
private fun KeypadRow(
    row: List<Key>,
    aspectRatio: Float,
    compact: Boolean,
    modifier: Modifier,
    onEvent: (BasicCalculatorEvent) -> Unit,
) {
    Row(
        modifier = modifier,
        horizontalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        row.forEach { key ->
            if (key is Key.Empty) {
                // Reserve the slot so the row's remaining buttons stay
                // the same width as a fully-populated row.
                Spacer(modifier = Modifier.weight(1f).aspectRatio(aspectRatio))
            } else {
                KeyButton(
                    key = key,
                    onEvent = onEvent,
                    compact = compact,
                    modifier =
                        Modifier
                            .weight(1f)
                            .aspectRatio(aspectRatio),
                )
            }
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

    /** Toggle the sign of the trailing operand (`5` <-> `-5`). */
    data object SignFlip : Key {
        override val label: String = "±"
    }

    /** Postfix factorial. Appends `!` to the expression. */
    data object Factorial : Key {
        override val label: String = "x!"
    }

    /** Squares the current operand. Appends `^2` to the expression. */
    data object Squared : Key {
        override val label: String = "x²"
    }

    /** Cubes the current operand. Appends `^3` to the expression. */
    data object Cubed : Key {
        override val label: String = "x³"
    }

    /** Renders as empty space; used to pad partial rows in the keypad grid. */
    data object Empty : Key {
        override val label: String = ""
    }
}

// Dispatcher for the keypad: one `when` arm per `Key` subtype. Adding a
// dispatcher class to split this purely for the linter would only obscure
// the table-style mapping from key to event.
@Suppress("CyclomaticComplexMethod")
@OptIn(ExperimentalFoundationApi::class)
@Composable
private fun KeyButton(
    key: Key,
    onEvent: (BasicCalculatorEvent) -> Unit,
    modifier: Modifier = Modifier,
    compact: Boolean = false,
) {
    val tones = LocalKeyTones.current
    val hapticsEnabled = LocalHapticsEnabled.current
    val haptics = androidx.compose.ui.platform.LocalHapticFeedback.current
    val rawClick: () -> Unit = {
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
            Key.SignFlip -> onEvent(BasicCalculatorEvent.SignFlip)
            // Factorial is a plain char append - the tokenizer turns
            // trailing `!` into Token.Factorial during evaluation.
            Key.Factorial -> onEvent(BasicCalculatorEvent.Append("!"))
            Key.Squared -> onEvent(BasicCalculatorEvent.Append("^2"))
            Key.Cubed -> onEvent(BasicCalculatorEvent.Append("^3"))
            Key.Empty -> Unit
        }
    }
    // Every click plays its DTMF tone (if sound is enabled in settings)
    // and fires a light haptic tick (if haptics are enabled). Both are
    // silently no-op if disabled or unavailable.
    val click: () -> Unit = {
        tones?.play(key)
        if (hapticsEnabled) {
            haptics.performHapticFeedback(
                androidx.compose.ui.hapticfeedback.HapticFeedbackType.LongPress,
            )
        }
        rawClick()
    }

    // Long-press wiring. Only Backspace defines a long-press action - it
    // clears the whole expression, in place of the standalone C key.
    val longClick: (() -> Unit)? =
        when (key) {
            Key.Backspace -> {
                { onEvent(BasicCalculatorEvent.Clear) }
            }
            else -> null
        }

    // Keys that auto-repeat while held: digits, dot, parens, arithmetic
    // operators. Backspace deliberately does not repeat (its long-press
    // is bound to Clear) and equals / memory / function keys are
    // one-shot actions where a repeat doesn't make sense.
    val repeatOnHold = when (key) {
        is Key.Symbol -> true
        Key.LeftParen, Key.RightParen -> true
        else -> false
    }

    val category = keyCategoryOf(key)
    val keyShape = RoundedCornerShape(20.dp)

    // iOS-style keypad palette. Only operators carry an accent (primary);
    // every other key is a NEUTRAL grey so the pad reads as "greys + one
    // accent" rather than a multi-hue patchwork. The greys are derived by
    // blending surface -> onSurface at a fixed lightness instead of using
    // the dynamic surface-container roles directly: that keeps the keys at
    // a controlled, clearly-visible brightness in every scheme (the raw
    // dynamic neutrals come out near-black in dark mode), while still
    // picking up a subtle wallpaper tint. The fractions reproduce the
    // original iOS greys (0x50 / 0x70 / 0xA5) in the static dark scheme.
    val scheme = MaterialTheme.colorScheme
    val keyGreyFraction =
        when (category) {
            KeyCategory.Digit -> 0.31f
            KeyCategory.Function -> 0.44f
            KeyCategory.Modifier -> 0.65f
            KeyCategory.Operator, KeyCategory.Equals -> 0f // unused; operators use primary
        }
    val containerColor =
        when (category) {
            KeyCategory.Operator, KeyCategory.Equals -> scheme.primary
            else -> lerp(scheme.surface, scheme.onSurface, keyGreyFraction)
        }
    val contentColor =
        when (category) {
            KeyCategory.Operator, KeyCategory.Equals -> scheme.onPrimary
            // Modifier keys are the light end of the grey ramp, so they take
            // the background color as their text (dark text in dark mode,
            // light text in light mode) for guaranteed contrast.
            KeyCategory.Modifier -> scheme.surface
            else -> scheme.onSurface
        }

    // Operators and equals get the larger display ramp so the action keys
    // read as visually heavier than digits; functions and memory use a
    // slightly tighter ramp because their labels are longer (sin⁻¹, M+).
    // In compact rows (height ~33dp), the displaySmall ramp would
    // exceed the button height and clip - so every category drops one
    // step down in the type ramp when compact is true. This keeps keys
    // like the constants and x²/x³ in the compact scientific rows from
    // being half-cut.
    val labelStyle =
        if (compact) {
            when (category) {
                KeyCategory.Operator, KeyCategory.Equals -> MaterialTheme.typography.titleLarge
                KeyCategory.Function -> MaterialTheme.typography.titleMedium
                KeyCategory.Modifier -> MaterialTheme.typography.titleLarge
                KeyCategory.Digit -> MaterialTheme.typography.titleLarge
            }
        } else {
            when (category) {
                KeyCategory.Operator, KeyCategory.Equals -> MaterialTheme.typography.displaySmall
                KeyCategory.Function -> MaterialTheme.typography.titleLarge
                KeyCategory.Modifier -> MaterialTheme.typography.headlineSmall
                KeyCategory.Digit -> MaterialTheme.typography.headlineLarge
            }
        }
    val labelWeight = if (category == KeyCategory.Equals) FontWeight.Bold else FontWeight.Medium

    // Three click modes:
    //   - repeatOnHold (digits, dot, parens, operators): immediate first
    //     fire on press, then auto-repeat after 400ms while still held.
    //   - longClick != null (backspace): tap to delete, long-press to
    //     clear the whole expression.
    //   - everything else (equals, functions, memory): plain click.
    //
    // Box rather than Material Button because Button exposes neither
    // long-press nor repeat. The visual press feedback for repeating
    // keys is the digit appearing in the expression - acceptable
    // tradeoff for the gesture.
    val clickModifier =
        when {
            repeatOnHold -> Modifier.repeatingClickable(onClick = click)
            longClick != null ->
                Modifier.combinedClickable(onClick = click, onLongClick = longClick)
            else -> Modifier.clickable(onClick = click)
        }
    Box(
        modifier =
            modifier
                .clip(keyShape)
                .background(containerColor)
                .then(clickModifier),
        contentAlignment = Alignment.Center,
    ) {
        Text(
            text = key.label,
            style = labelStyle.copy(fontWeight = labelWeight),
            color = contentColor,
        )
    }
}

/**
 * Auto-repeating clickable. Fires [onClick] immediately on press, then
 * repeats every [intervalMillis] after an [initialDelayMillis] hold,
 * stopping on release or cancel.
 *
 * This is what makes holding a digit key emit "555555..." the way a
 * physical keyboard's auto-repeat works. The 400ms / 80ms defaults
 * mirror Android's IME long-press behaviour so it feels familiar.
 */
@Composable
private fun Modifier.repeatingClickable(
    enabled: Boolean = true,
    initialDelayMillis: Long = 400L,
    intervalMillis: Long = 80L,
    onClick: () -> Unit,
): Modifier {
    val currentOnClick by rememberUpdatedState(onClick)
    var pressed by remember { mutableStateOf(false) }
    // The pointerInput sets `pressed` on down and clears it on up/cancel.
    // The LaunchedEffect watches that flag - delay first (so a quick tap
    // doesn't trigger any repeats), then fire onClick on an interval as
    // long as the finger is still down.
    LaunchedEffect(pressed) {
        if (pressed && enabled) {
            delay(initialDelayMillis)
            while (pressed) {
                currentOnClick()
                delay(intervalMillis)
            }
        }
    }
    return this.pointerInput(enabled) {
        awaitEachGesture {
            awaitFirstDown(requireUnconsumed = false)
            if (!enabled) return@awaitEachGesture
            currentOnClick() // immediate first click
            pressed = true
            waitForUpOrCancellation()
            pressed = false
        }
    }
}

/**
 * DTMF tone generator for the keypad.
 *
 * Maps digits to the standard phone touch-tones (so 7 sounds like 7
 * and 9 sounds like 9), and gives operators, equals, and modifier keys
 * their own tones so the audio cues differentiate by key category, not
 * just by pitch. Falls back silently if the system refuses to allocate
 * a [ToneGenerator] (some OEM devices restrict STREAM_DTMF).
 */
private class KeyToneGenerator(private val tg: ToneGenerator) {
    fun play(key: Key) {
        val tone = toneFor(key) ?: return
        // 80ms is short enough that overlapping repeats don't smear into
        // one long buzz when a digit auto-repeats.
        runCatching { tg.startTone(tone, KEY_TONE_DURATION_MS) }
    }

    private fun toneFor(key: Key): Int? =
        when (key) {
            is Key.Symbol -> {
                val label = key.label
                when {
                    label.length == 1 && label[0].isDigit() ->
                        ToneGenerator.TONE_DTMF_0 + (label[0] - '0')
                    label == "." -> ToneGenerator.TONE_DTMF_P
                    else -> ToneGenerator.TONE_PROP_BEEP
                }
            }
            Key.Equals -> ToneGenerator.TONE_PROP_ACK
            Key.Backspace -> ToneGenerator.TONE_PROP_PROMPT
            Key.LeftParen, Key.RightParen -> ToneGenerator.TONE_PROP_BEEP
            Key.Clear -> ToneGenerator.TONE_PROP_NACK
            Key.MemoryClear, Key.MemoryRecall, Key.MemoryAdd, Key.MemorySubtract ->
                ToneGenerator.TONE_PROP_BEEP
            Key.SignFlip, Key.Factorial, Key.Squared, Key.Cubed -> ToneGenerator.TONE_PROP_BEEP
            Key.Empty -> null
            is Key.Function -> ToneGenerator.TONE_PROP_BEEP
        }

    fun release() {
        runCatching { tg.release() }
    }
}

private const val KEY_TONE_DURATION_MS = 80
private const val KEY_TONE_VOLUME = 80

/**
 * Constructs and remembers a [KeyToneGenerator] scoped to the current
 * composition. Released automatically on dispose. Returns null if the
 * device wouldn't give us a tone generator (we then play no audio at
 * all rather than crashing).
 */
@Composable
private fun rememberKeyToneGenerator(): KeyToneGenerator? {
    val generator =
        remember {
            runCatching {
                ToneGenerator(AudioManager.STREAM_DTMF, KEY_TONE_VOLUME)
            }.getOrNull()
        }
    val keyTones = remember(generator) { generator?.let(::KeyToneGenerator) }
    DisposableEffect(keyTones) {
        onDispose { keyTones?.release() }
    }
    return keyTones
}

/** CompositionLocal so any key in the tree can play tones without prop drilling. */
private val LocalKeyTones = compositionLocalOf<KeyToneGenerator?> { null }

/**
 * Whether key-press haptic feedback should fire. Defaults to true so
 * existing code paths and previews continue to vibrate.
 */
private val LocalHapticsEnabled = compositionLocalOf { true }

private enum class KeyCategory { Digit, Operator, Modifier, Function, Equals }

private fun keyCategoryOf(key: Key): KeyCategory =
    when (key) {
        Key.Equals -> KeyCategory.Equals
        Key.Clear, Key.LeftParen, Key.RightParen, Key.Backspace -> KeyCategory.Modifier
        // ± is a grey modifier (it mutates the trailing operand rather than
        // combining two operands), matching how iOS colors +/- and keeping
        // the orange confined to the ÷ × - + = column.
        Key.SignFlip -> KeyCategory.Modifier
        Key.MemoryClear, Key.MemoryRecall, Key.MemoryAdd, Key.MemorySubtract -> KeyCategory.Function
        Key.Factorial, Key.Squared, Key.Cubed -> KeyCategory.Function
        Key.Empty -> KeyCategory.Digit // unused; Empty is rendered separately
        is Key.Function -> KeyCategory.Function
        is Key.Symbol ->
            when {
                key.label in OperatorLabels -> KeyCategory.Operator
                // % is a grey modifier (iOS-style), not part of the orange
                // operator column.
                key.label == "%" -> KeyCategory.Modifier
                // ^ (power) reads as an advanced function, so it takes the
                // function grey rather than the operator orange.
                key.label == "^" -> KeyCategory.Function
                // Constants (pi, e) sit among the scientific keys, so they
                // take the function color rather than the digit color.
                key.label in ConstantLabels -> KeyCategory.Function
                else -> KeyCategory.Digit
            }
    }

/**
 * Labels of arithmetic-operator keys that get the operator (orange) color:
 * the four binary operators that form the right-hand column (= is handled
 * separately as [Key.Equals]). %, ^ and ± are deliberately excluded so the
 * orange stays a single column.
 */
private val OperatorLabels = setOf("+", "-", "×", "÷")

/**
 * Labels of constant keys (pi, e). They insert a literal value like a digit,
 * but visually they belong with the scientific function keys they sit among,
 * so they take the function color rather than the digit color.
 */
private val ConstantLabels = setOf("π", "e")

// Width-to-height ratio for every keypad button. 1.6 gives a clean
// "horizontal rectangle" silhouette - wider than tall by ~60%, the
// shape physical desk calculators use. Tweaking this is the single
// knob for "make keys taller / shorter" without touching layout code.
private const val BUTTON_ASPECT_RATIO = 1.6f

// Wider aspect ratio for scientific-function and memory rows: same
// width, smaller height, so those rows are visibly less prominent
// than the digit/operator rows and free up vertical space for the
// display. 2.2 (vs the old 2.6) bumps the top two rows about 5dp
// taller so the labels - MC/MR/M+/M-, ÷/%/±/× - aren't cramped.
private const val BUTTON_ASPECT_RATIO_COMPACT = 2.2f

// iOS palette tokens, used by both the keypad and the tools sheet so
// the calculator reads as a single visual system. Literal colors rather
// than theme roles because the iOS feel is recognisable specifically by
// these hexes; M3 dynamic-color would dilute it.

/**
 * Pull the first plausibly-numeric token out of a pasted string.
 *
 * Accepts optional leading minus, digit groups (commas / spaces / Indian
 * grouping), and an optional decimal point. Currency symbols and unit
 * suffixes are skipped silently so pasting "₹1,234.50" yields "1234.50".
 * Returns null if nothing numeric is found.
 */
internal fun extractLeadingNumber(text: String): String? {
    val pattern = Regex("""-?\d[\d,\s]*(?:\.\d+)?""")
    val match = pattern.find(text) ?: return null
    val cleaned = match.value.replace(",", "").replace(" ", "")
    // Reject if it's just "-" or empty after cleanup (regex shouldn't
    // produce this but the explicit guard makes the intent obvious).
    return cleaned.takeIf { it.toDoubleOrNull() != null }
}

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
