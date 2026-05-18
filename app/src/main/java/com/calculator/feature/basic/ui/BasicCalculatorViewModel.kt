package com.calculator.feature.basic.ui

import androidx.lifecycle.ViewModel
import com.calculator.core.math.EvaluationResult
import com.calculator.core.math.Evaluator
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import javax.inject.Inject

/**
 * Holds the state of the basic calculator screen and forwards evaluation
 * to the [Evaluator] use-case.
 *
 * Design notes:
 *  - State is exposed as a single [StateFlow] of [BasicCalculatorUiState];
 *    the UI collects it with `collectAsStateWithLifecycle()`.
 *  - The evaluator is injected as a dependency rather than constructed
 *    inline, so tests can substitute a fake (e.g. to inject deterministic
 *    error outcomes without crafting input strings).
 *  - The evaluation engine is synchronous and microsecond-fast for any
 *    realistic expression length, so no coroutine dispatch is needed
 *    here. If we ever add long-running operations (currency fetch, big
 *    matrix math), they should be dispatched off the main thread.
 *
 * UX rules baked in (match common physical/iOS-style calculators):
 *  - Consecutive operators collapse: typing `1 + + ×` ends up as `1×`.
 *  - Leading `+`, `×`, `÷` are ignored; leading `-` is allowed (negation).
 *  - Only one decimal point per number segment.
 *  - A leading `.` is auto-prefixed with `0`, so `.5` reads as `0.5`.
 *  - Pressing a digit right after `=` starts a fresh expression.
 *  - Pressing an operator right after `=` chains on the result.
 *  - Pressing `=` with a trailing operator auto-completes the missing
 *    operand from the first number (`1 + =` → `1 + 1 = 2`).
 *  - Pressing `=` repeatedly re-applies the last `op + operand`
 *    (`1 + 5 = = =` → `6, 11, 16`).
 */
@HiltViewModel
class BasicCalculatorViewModel
    @Inject
    constructor(
        private val evaluator: Evaluator,
    ) : ViewModel() {
        private val _state = MutableStateFlow(BasicCalculatorUiState())
        val state: StateFlow<BasicCalculatorUiState> = _state.asStateFlow()

        /** Dispatch a user event - typically called from the UI on key press. */
        fun onEvent(event: BasicCalculatorEvent) {
            when (event) {
                is BasicCalculatorEvent.Append -> append(event.symbol)
                BasicCalculatorEvent.Backspace -> backspace()
                BasicCalculatorEvent.Clear -> clear()
                BasicCalculatorEvent.Equals -> commit()
            }
        }

        private fun append(symbol: String) =
            _state.update { current ->
                val next = nextExpressionAfterAppend(current, symbol)
                current.copy(
                    expression = next,
                    liveResult = preview(next),
                    errorMessage = null,
                    // Any non-`=` input breaks the repeat-equals chain.
                    pendingRepeat = null,
                )
            }

        private fun nextExpressionAfterAppend(current: BasicCalculatorUiState, symbol: String): String {
            val expr = current.expression
            val isOperator = symbol.length == 1 && symbol[0] in ArithmeticOperators

            // After `=`, the next input either starts a new calculation
            // (digit / `.`) or chains on the current result (operator).
            if (current.pendingRepeat != null) {
                return when {
                    isOperator -> expr + symbol
                    symbol == "." -> "0."
                    else -> symbol
                }
            }

            return when {
                // Replace the trailing operator instead of stacking another.
                isOperator && expr.isNotEmpty() && expr.last() in ArithmeticOperators ->
                    expr.dropLast(1) + symbol

                // Drop a stray leading `+`, `×`, `÷` (but allow leading `-` for negation).
                isOperator && expr.isEmpty() && symbol != "-" -> expr

                // Only one `.` per number segment.
                symbol == "." && currentNumberHasDot(expr) -> expr

                // Auto-prefix `0` so `.5` reads as `0.5` and `1+.5` reads as `1+0.5`.
                symbol == "." && (expr.isEmpty() || expr.last() in ArithmeticOperators || expr.last() == '(') ->
                    expr + "0."

                else -> expr + symbol
            }
        }

        private fun backspace() =
            _state.update { current ->
                val next = current.expression.dropLast(1)
                current.copy(
                    expression = next,
                    liveResult = preview(next),
                    errorMessage = null,
                    pendingRepeat = null,
                )
            }

        private fun clear() = _state.update { BasicCalculatorUiState() }

        private fun commit() =
            _state.update { current ->
                if (current.expression.isBlank()) return@update current

                val (toEvaluate, repeatToken) = buildEquationToEvaluate(current)

                when (val result = evaluator.evaluate(toEvaluate)) {
                    is EvaluationResult.Success ->
                        current.copy(
                            // Replace the expression with the canonical result so the
                            // user can keep chaining (e.g. press `+` after `=`).
                            // `stripTrailingZeros` collapses `4.0` to `4` for display,
                            // and `toPlainString` keeps it out of scientific notation so
                            // the next keypress chains naturally.
                            expression = result.value.stripTrailingZeros().toPlainString(),
                            liveResult = null,
                            errorMessage = null,
                            pendingRepeat = repeatToken,
                        )

                    is EvaluationResult.Error ->
                        current.copy(
                            liveResult = null,
                            errorMessage = errorToMessage(result),
                            pendingRepeat = null,
                        )
                }
            }

        /**
         * Resolve the canonical equation string to evaluate on `=`.
         *
         * Returns `(expressionToEvaluate, repeatTokenForNextEquals)`. The
         * repeat token is the trailing `op+operand` to be re-applied when
         * the user keeps hitting `=`.
         */
        private fun buildEquationToEvaluate(current: BasicCalculatorUiState): Pair<String, String?> {
            // Replay path: previous `=` set a repeat token, and the user
            // pressed `=` again with the result still on the display.
            current.pendingRepeat?.let { repeat ->
                return (current.expression + repeat) to repeat
            }

            val expr = current.expression

            // Trailing operator: auto-complete with the operand the user just
            // typed (the number immediately before that operator). So `1+=`
            // resolves as `1+1=2`, `10-3×=` as `10-3×3=1`, and so on.
            if (expr.last() in ArithmeticOperators) {
                val precedingNumber = PrecedingNumberRegex.find(expr)?.groupValues?.getOrNull(1)
                return if (precedingNumber != null) {
                    val repeat = expr.last() + precedingNumber
                    (expr + precedingNumber) to repeat
                } else {
                    // Just an operator (e.g. `-`); let the evaluator surface the error.
                    expr to null
                }
            }

            // Normal path: extract trailing `op+operand` for future replays.
            val repeat = TrailingOpRegex.find(expr)?.value
            return expr to repeat
        }

        /**
         * Compute a non-binding preview result.
         *
         * Errors during preview are intentionally swallowed: the user is still
         * typing, and partial expressions like `5 +` should not light up
         * an error banner mid-input.
         */
        private fun preview(expression: String): String? {
            if (expression.isBlank()) return null
            return when (val result = evaluator.evaluate(expression)) {
                is EvaluationResult.Success -> result.value.toPlainString()
                is EvaluationResult.Error -> null
            }
        }

        /**
         * Translate engine errors to user-facing strings.
         *
         * In a fully internationalised build these would be string resource
         * IDs resolved at the composable layer; for the scaffold we ship
         * English literals which the i18n pass (M5) will replace.
         */
        private fun errorToMessage(error: EvaluationResult.Error): String =
            when (error) {
                EvaluationResult.Error.DivisionByZero -> "Can't divide by zero"
                is EvaluationResult.Error.Syntax -> "Check your expression"
                is EvaluationResult.Error.UnknownToken -> "Unsupported character"
            }

        /**
         * Walk back through the current number segment; return true if a `.`
         * already exists before any operator or paren is hit.
         */
        private fun currentNumberHasDot(expr: String): Boolean {
            for (i in expr.length - 1 downTo 0) {
                val c = expr[i]
                if (c in ArithmeticOperators || c == '(' || c == ')') return false
                if (c == '.') return true
            }
            return false
        }

        private companion object {
            /** Characters the keypad treats as binary arithmetic operators. */
            val ArithmeticOperators = setOf('+', '-', '×', '÷', '*', '/')

            /** Trailing `op + operand` for repeat-equals. */
            val TrailingOpRegex = Regex("([+\\-×÷*/])(\\d+(?:\\.\\d+)?)$")

            /**
             * The numeric literal sitting immediately before a trailing
             * operator. Used to auto-complete an incomplete expression on
             * `=` (e.g. `10-3×` → use `3`, the operand just entered).
             */
            val PrecedingNumberRegex = Regex("(\\d+(?:\\.\\d+)?)[+\\-×÷*/]$")
        }
    }
