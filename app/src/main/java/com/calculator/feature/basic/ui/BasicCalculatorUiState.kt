package com.calculator.feature.basic.ui

import androidx.compose.runtime.Immutable
import com.calculator.core.math.AngleMode
import java.math.BigDecimal

/**
 * UI state for the basic calculator screen.
 *
 * Marked [Immutable] so Compose can skip recomposition when the same
 * instance is passed back during state propagation - cheap correctness win
 * because all properties are val and the types are stable primitives.
 *
 * @property expression Current expression as the user has entered it,
 *   in display form (e.g. `12 × 3 + 4`).
 * @property liveResult Live preview of the result if the expression is
 *   currently evaluable, otherwise `null`. Shown beneath the expression.
 * @property errorMessage Human-readable error string, or `null` for the
 *   happy path. Surfaces division-by-zero, mismatched parens etc.
 */
@Immutable
data class BasicCalculatorUiState(
    val expression: String = "",
    val liveResult: String? = null,
    val errorMessage: String? = null,
    /**
     * Trailing `op + operand` from the most recent successful evaluation.
     *
     * Re-applied on every subsequent `=` press so the user can chain
     * `1 + 5 = = =` to get `6, 11, 16, ...`. Any other input
     * (digit, operator, backspace, clear) resets this back to `null`.
     */
    val pendingRepeat: String? = null,
    /** Whether the scientific keypad is shown above the basic keypad. */
    val scientific: Boolean = false,
    /** Angle mode passed to the trig functions in the engine. */
    val angleMode: AngleMode = AngleMode.Radian,
    /** Stored value behind the M+ / M- / MR / MC keys. */
    val memory: BigDecimal = BigDecimal.ZERO,
    /**
     * Expression that was last committed via `=`, preserved so the
     * display's top line can continue to show it after the bottom
     * line collapses to the canonical result.
     *
     * - Set to the auto-completed expression on every successful `=`.
     * - Cleared the moment the user starts a fresh expression (any
     *   digit or `.` after `=` resets to `null`).
     * - Continues to update when the user chains an operator after `=`
     *   (the new committed expression replaces the old one on the
     *   next `=`).
     */
    val lastCommittedExpression: String? = null,
    /**
     * Most recent successfully computed preview, preserved across
     * intermediate states where the live expression isn't evaluable.
     *
     * Used by the display's bottom (result) line as a sticky fallback
     * so typing the next operator (`5+3` → `5+3+`) doesn't blank the
     * result row - the previous `8` stays visible until the user
     * provides enough input for a new preview to compute. Cleared
     * when the expression is emptied (Clear / Backspace-to-empty)
     * because there's no longer any "previous result" to remember.
     */
    val lastValidPreview: String? = null,
)

/**
 * User-driven events the basic calculator screen can dispatch.
 *
 * Sealed so the ViewModel's `when` branch stays exhaustive when new
 * events are added.
 */
sealed interface BasicCalculatorEvent {
    /** Append a digit, decimal point, operator, or parenthesis to the expression. */
    data class Append(
        val symbol: String,
    ) : BasicCalculatorEvent

    /** Remove the last character from the expression. */
    data object Backspace : BasicCalculatorEvent

    /** Clear the expression entirely. */
    data object Clear : BasicCalculatorEvent

    /** Evaluate the expression and replace it with the result. */
    data object Equals : BasicCalculatorEvent

    /** Show / hide the scientific row above the basic keypad. */
    data object ToggleScientific : BasicCalculatorEvent

    /** Switch trig interpretation between [AngleMode.Radian] and [AngleMode.Degree]. */
    data object ToggleAngleMode : BasicCalculatorEvent

    /** Add the current display value to memory. */
    data object MemoryAdd : BasicCalculatorEvent

    /** Subtract the current display value from memory. */
    data object MemorySubtract : BasicCalculatorEvent

    /** Append the stored memory value to the expression. */
    data object MemoryRecall : BasicCalculatorEvent

    /** Reset memory to zero. */
    data object MemoryClear : BasicCalculatorEvent

    /** Flip the sign of the trailing operand in the expression. */
    data object SignFlip : BasicCalculatorEvent
}
