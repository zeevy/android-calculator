package com.calculator.feature.basic.ui

import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBars
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Menu
import androidx.compose.material3.AssistChip
import androidx.compose.material3.AssistChipDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clipToBounds
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.onClick
import androidx.compose.ui.semantics.onLongClick
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.rememberTextMeasurer
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Constraints
import androidx.compose.ui.unit.TextUnit
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.calculator.R
import com.calculator.core.common.clipboard.plainTextClipEntry
import com.calculator.core.common.clipboard.readPlainText
import com.calculator.core.common.format.NumberFormatter
import com.calculator.core.data.tape.TapeEntry
import com.calculator.core.math.AngleMode
import kotlinx.coroutines.launch
import java.math.BigDecimal

/**
 * Display section: expression + live preview bottom-right, mode chips
 * top-left, hamburger icon top-right. Implemented as a Box so each
 * piece sits exactly where it should without padding rows wasting space.
 */
@Composable
internal fun DisplaySection(
    state: BasicCalculatorUiState,
    tape: List<TapeEntry>,
    onEvent: (BasicCalculatorEvent) -> Unit,
    onOpenMenu: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val clipboard = androidx.compose.ui.platform.LocalClipboard.current
    val context = androidx.compose.ui.platform.LocalContext.current
    // The Clipboard API is suspend-based (it may hop to the system
    // clipboard service), so copy/paste run in a composition-scoped
    // coroutine rather than inline in the click handler.
    val clipboardScope = rememberCoroutineScope()
    Box(modifier = modifier) {
        // Column so the tape stacks above the display lines: the tape
        // takes the leftover height (weight) while the display keeps its
        // intrinsic two-line height, which means a growing tape can never
        // squeeze the result text.
        Column(
            modifier =
                Modifier
                    .fillMaxSize()
                    .windowInsetsPadding(WindowInsets.statusBars)
                    .padding(horizontal = 16.dp)
                    // Clears the chip row / hamburger overlaid at the top
                    // of this Box.
                    .padding(top = 48.dp),
        ) {
            InlineTape(
                entries = tape,
                // Tapping a tape line appends its result, matching the
                // recall behaviour of the History sheet and Tape screen.
                onRecall = { result -> onEvent(BasicCalculatorEvent.Append(result)) },
                // Deleting one line goes through the event channel like
                // every other interaction on this screen.
                onDelete = { id -> onEvent(BasicCalculatorEvent.DeleteTapeLine(id)) },
                modifier = Modifier.fillMaxWidth().weight(1f),
            )
            Display(
                expression = state.expression,
                preview = state.liveResult,
                error = state.errorMessage?.let { stringResource(it) },
                lastCommittedExpression = state.lastCommittedExpression,
                lastValidPreview = state.lastValidPreview,
                onCopyResult = { text ->
                    if (text.isNotBlank()) {
                        clipboardScope.launch {
                            clipboard.setClipEntry(plainTextClipEntry(text))
                            android.widget.Toast
                                .makeText(context, R.string.basic_copied, android.widget.Toast.LENGTH_SHORT)
                                .show()
                        }
                    }
                },
                onPasteRequested = {
                    clipboardScope.launch {
                        val pasted = clipboard.getClipEntry().readPlainText()
                        val number = NumberFormatter.extractPastedNumber(pasted)
                        if (number != null) {
                            onEvent(BasicCalculatorEvent.Append(number))
                        } else {
                            android.widget.Toast
                                .makeText(context, R.string.basic_paste_no_number, android.widget.Toast.LENGTH_SHORT)
                                .show()
                        }
                    }
                },
                modifier = Modifier.fillMaxWidth(),
            )
        }

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
                    label = {
                        Text(
                            stringResource(
                                if (state.angleMode ==
                                    AngleMode.Degree
                                ) {
                                    R.string.basic_angle_degree
                                } else {
                                    R.string.basic_angle_radian
                                },
                            ),
                        )
                    },
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
 *     | Post-equals ("5+3=8")         | (blank)        | expression (8)    |
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
    // Top (input) line: the expression as typed. We do NOT gate this on
    // `preview != null` because the live preview can disappear mid-
    // typing - typing the next operator (`5+3` -> `5+3+`) makes the
    // preview unevaluable, and gating on it would blank the top line on
    // every operator press. Showing the expression unconditionally keeps
    // the layout stable across every keystroke.
    //
    // The one exception is the post-`=` state, where the line goes
    // blank: the inline tape directly above already carries the
    // `5+3 = 8` line, so echoing `5+3` here printed the same input
    // twice, one line apart. The slot itself stays (an empty Text still
    // occupies its line height) so the result never shifts vertically,
    // the long-press-to-paste target survives, and the gap gives the
    // result room to breathe under the tape.
    val topText: String = if (lastCommittedExpression != null) "" else expression
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
