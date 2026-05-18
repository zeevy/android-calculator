package com.calculator.feature.basic.ui

import androidx.compose.runtime.Immutable

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
}
