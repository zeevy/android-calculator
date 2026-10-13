package com.calculator.feature.basic.ui

import androidx.annotation.StringRes
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.gestures.awaitEachGesture
import androidx.compose.foundation.gestures.awaitFirstDown
import androidx.compose.foundation.gestures.waitForUpOrCancellation
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.IntrinsicSize
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.lerp
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.onClick
import androidx.compose.ui.semantics.onLongClick
import androidx.compose.ui.semantics.role
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.calculator.R
import kotlinx.coroutines.delay

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
    val configuration = LocalConfiguration.current
    val minKeyHeight = minKeyHeightDp(configuration.screenWidthDp, configuration.screenHeightDp, scientific).dp
    CompositionLocalProvider(LocalMinKeyHeight provides minKeyHeight) {
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
internal data class KeypadRowSpec(val keys: List<Key>, val compact: Boolean = false)

@Composable
private fun KeypadRow(
    row: List<Key>,
    aspectRatio: Float,
    compact: Boolean,
    modifier: Modifier,
    onEvent: (BasicCalculatorEvent) -> Unit,
) {
    val minKeyHeight = LocalMinKeyHeight.current
    Row(
        modifier = modifier,
        horizontalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        row.forEach { key ->
            if (key is Key.Empty) {
                // Reserve the slot so the row's remaining buttons stay
                // the same width as a fully-populated row.
                Spacer(modifier = Modifier.weight(1f).keySize(aspectRatio, minKeyHeight))
            } else {
                KeyButton(
                    key = key,
                    onEvent = onEvent,
                    compact = compact,
                    modifier =
                        Modifier
                            .weight(1f)
                            .keySize(aspectRatio, minKeyHeight),
                )
            }
        }
    }
}

/**
 * Sealed key descriptor: one variant per kind of key on the keypad.
 *
 * What a key does, how it looks and sounds is decided by exhaustive
 * `when` blocks over this type (in [KeyButton], [keyCategoryOf] and the
 * tone generator), so adding a variant fails to compile until every
 * place handles it.
 */
internal sealed interface Key {
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
    // clears the whole expression plus the inline tape, in place of the
    // standalone C key.
    val longClick: (() -> Unit)? =
        when (key) {
            Key.Backspace -> {
                { onEvent(BasicCalculatorEvent.ClearAll) }
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
            KeyCategory.Digit -> KEY_GREY_FRACTION_DIGIT
            KeyCategory.Function -> KEY_GREY_FRACTION_FUNCTION
            KeyCategory.Modifier -> KEY_GREY_FRACTION_MODIFIER
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
    // TalkBack, Switch Access and Voice Access see each key as a button
    // with a spoken name, and can press it even though the repeating keys
    // only read raw pointer input.
    val description = key.descriptionRes()?.let { stringResource(it) } ?: key.label
    val clearAllLabel = stringResource(R.string.basic_key_clear_all)
    Box(
        modifier =
            modifier
                // Compact rows are shorter than a finger needs.
                .clip(keyShape)
                .background(containerColor)
                .semantics(mergeDescendants = true) {
                    role = Role.Button
                    contentDescription = description
                    onClick {
                        click()
                        true
                    }
                    if (longClick != null) {
                        onLongClick(label = clearAllLabel) {
                            longClick()
                            true
                        }
                    }
                }.then(clickModifier),
        contentAlignment = Alignment.Center,
    ) {
        Text(
            text = key.label,
            style = labelStyle.copy(fontWeight = labelWeight),
            color = contentColor,
        )
    }
}

private val SymbolNames =
    mapOf(
        "+" to R.string.basic_key_plus,
        "-" to R.string.basic_key_minus,
        "×" to R.string.basic_key_multiply,
        "÷" to R.string.basic_key_divide,
        "%" to R.string.basic_key_percent,
        "." to R.string.basic_key_point,
        "^" to R.string.basic_key_power,
        "π" to R.string.basic_key_pi,
        "e" to R.string.basic_key_e,
    )

private val FunctionNames =
    mapOf(
        "sin" to R.string.basic_key_sin,
        "cos" to R.string.basic_key_cos,
        "tan" to R.string.basic_key_tan,
        "asin" to R.string.basic_key_asin,
        "acos" to R.string.basic_key_acos,
        "atan" to R.string.basic_key_atan,
        "sqrt" to R.string.basic_key_sqrt,
        "log" to R.string.basic_key_log,
        "ln" to R.string.basic_key_ln,
    )

/** Spoken name of a key, or null when its label already reads well (digits). */
@StringRes
private fun Key.descriptionRes(): Int? =
    when (this) {
        is Key.Symbol -> SymbolNames[label]
        is Key.Function -> FunctionNames[keyword]
        Key.LeftParen -> R.string.basic_key_left_paren
        Key.RightParen -> R.string.basic_key_right_paren
        Key.Clear -> R.string.basic_button_clear
        Key.Backspace -> R.string.basic_button_backspace
        Key.Equals -> R.string.basic_button_equals
        Key.MemoryClear -> R.string.basic_key_memory_clear
        Key.MemoryRecall -> R.string.basic_key_memory_recall
        Key.MemoryAdd -> R.string.basic_key_memory_add
        Key.MemorySubtract -> R.string.basic_key_memory_subtract
        Key.SignFlip -> R.string.basic_key_sign
        Key.Factorial -> R.string.basic_key_factorial
        Key.Squared -> R.string.basic_key_square
        Key.Cubed -> R.string.basic_key_cube
        Key.Empty -> null
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
internal fun Modifier.repeatingClickable(
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

internal enum class KeyCategory { Digit, Operator, Modifier, Function, Equals }

internal fun keyCategoryOf(key: Key): KeyCategory =
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
internal val OperatorLabels = setOf("+", "-", "×", "÷")

/**
 * Labels of constant keys (pi, e). They insert a literal value like a digit,
 * but visually they belong with the scientific function keys they sit among,
 * so they take the function color rather than the digit color.
 */
internal val ConstantLabels = setOf("π", "e")

// Blend fractions used to derive the neutral key greys (surface -> onSurface).
// They reproduce the original iOS greys - 0x50 / 0x70 / 0xA5 - in the static
// dark scheme, and stay legible in every dynamic scheme.
private const val KEY_GREY_FRACTION_DIGIT = 0.31f
private const val KEY_GREY_FRACTION_FUNCTION = 0.44f
private const val KEY_GREY_FRACTION_MODIFIER = 0.65f

// Width-to-height ratio for every keypad button. 1.6 gives a clean
// "horizontal rectangle" silhouette - wider than tall by ~60%, the
// shape physical desk calculators use. Tweaking this is the single
// knob for "make keys taller / shorter" without touching layout code.
internal const val BUTTON_ASPECT_RATIO = 1.6f

// Wider aspect ratio for scientific-function and memory rows: same
// width, smaller height, so those rows are visibly less prominent
// than the digit/operator rows and free up vertical space for the
// display. 2.2 (vs the old 2.6) bumps the top two rows about 5dp
// taller so the labels - MC/MR/M+/M-, ÷/%/±/× - aren't cramped.
internal const val BUTTON_ASPECT_RATIO_COMPACT = 2.2f
