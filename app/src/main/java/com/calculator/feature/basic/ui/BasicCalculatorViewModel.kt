package com.calculator.feature.basic.ui

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.calculator.core.data.history.HistoryEntry
import com.calculator.core.data.history.HistoryRepository
import com.calculator.core.math.AngleMode
import com.calculator.core.math.EvaluationResult
import com.calculator.core.math.Evaluator
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import java.math.BigDecimal
import javax.inject.Inject

/**
 * Holds the state of the basic calculator screen and forwards evaluation
 * to the [Evaluator] use-case.
 *
 * Design notes:
 *  - State is exposed as a single [StateFlow] of [BasicCalculatorUiState];
 *    the UI collects it with `collectAsStateWithLifecycle()`.
 *  - A fresh [Evaluator] is constructed per evaluation with the current
 *    [AngleMode] so the trig functions react to the DEG/RAD toggle without
 *    plumbing the mode through every call site.
 *  - The evaluation engine is synchronous and microsecond-fast for any
 *    realistic expression length, so no coroutine dispatch is needed.
 *
 * UX rules baked in (match common physical/iOS-style calculators):
 *  - Consecutive operators collapse: typing `1 + + ×` ends up as `1×`.
 *  - Leading `+`, `×`, `÷` are ignored; leading `-` is allowed (negation).
 *  - Only one decimal point per number segment.
 *  - A leading `.` is auto-prefixed with `0`, so `.5` reads as `0.5`.
 *  - Pressing a digit right after `=` starts a fresh expression.
 *  - Pressing an operator right after `=` chains on the result.
 *  - Pressing `=` with a trailing operator auto-completes the missing
 *    operand from the last typed number (`1 + =` → `1 + 1 = 2`).
 *  - Pressing `=` repeatedly re-applies the last `op + operand`.
 *  - Memory keys M+, M-, MR, MC operate on a single stored value.
 *
 * Process-death survival: expression, error, repeat token, scientific
 * toggle, angle mode, and memory are all persisted via [SavedStateHandle].
 */
@HiltViewModel
class BasicCalculatorViewModel
    @Inject
    constructor(
        private val savedStateHandle: SavedStateHandle,
        private val historyRepository: HistoryRepository? = null,
    ) : ViewModel() {
        // Restore from the SavedStateHandle so calculator state survives
        // process death and config changes. `liveResult` is derived from
        // `expression`, so we recompute it rather than persist it.
        private val _state = MutableStateFlow(loadFromSavedState())
        val state: StateFlow<BasicCalculatorUiState> = _state.asStateFlow()

        private fun loadFromSavedState(): BasicCalculatorUiState {
            val expression = savedStateHandle.get<String>(KEY_EXPRESSION).orEmpty()
            val angleMode =
                savedStateHandle
                    .get<String>(KEY_ANGLE_MODE)
                    ?.let { runCatching { AngleMode.valueOf(it) }.getOrNull() }
                    ?: AngleMode.Radian
            val memory =
                savedStateHandle
                    .get<String>(KEY_MEMORY)
                    ?.let { runCatching { BigDecimal(it) }.getOrNull() }
                    ?: BigDecimal.ZERO
            return BasicCalculatorUiState(
                expression = expression,
                liveResult = preview(expression, angleMode),
                errorMessage = savedStateHandle.get<String>(KEY_ERROR),
                pendingRepeat = savedStateHandle.get<String>(KEY_PENDING_REPEAT),
                scientific = savedStateHandle.get<Boolean>(KEY_SCIENTIFIC) ?: false,
                angleMode = angleMode,
                memory = memory,
            )
        }

        private fun persist(state: BasicCalculatorUiState) {
            savedStateHandle[KEY_EXPRESSION] = state.expression
            savedStateHandle[KEY_ERROR] = state.errorMessage
            savedStateHandle[KEY_PENDING_REPEAT] = state.pendingRepeat
            savedStateHandle[KEY_SCIENTIFIC] = state.scientific
            savedStateHandle[KEY_ANGLE_MODE] = state.angleMode.name
            savedStateHandle[KEY_MEMORY] = state.memory.toPlainString()
        }

        /** Dispatch a user event - typically called from the UI on key press. */
        fun onEvent(event: BasicCalculatorEvent) {
            when (event) {
                is BasicCalculatorEvent.Append -> append(event.symbol)
                BasicCalculatorEvent.Backspace -> backspace()
                BasicCalculatorEvent.Clear -> clear()
                BasicCalculatorEvent.Equals -> commit()
                BasicCalculatorEvent.ToggleScientific -> toggleScientific()
                BasicCalculatorEvent.ToggleAngleMode -> toggleAngleMode()
                BasicCalculatorEvent.MemoryAdd -> memoryDelta(addToMemory = true)
                BasicCalculatorEvent.MemorySubtract -> memoryDelta(addToMemory = false)
                BasicCalculatorEvent.MemoryRecall -> memoryRecall()
                BasicCalculatorEvent.MemoryClear -> memoryClear()
                BasicCalculatorEvent.SignFlip -> signFlip()
            }
        }

        private fun append(symbol: String) =
            _state.update { current ->
                val next = nextExpressionAfterAppend(current, symbol)
                current
                    .copy(
                        expression = next,
                        liveResult = preview(next, current.angleMode),
                        errorMessage = null,
                        // Any non-`=` input breaks the repeat-equals chain.
                        pendingRepeat = null,
                    ).also(::persist)
            }

        private fun nextExpressionAfterAppend(current: BasicCalculatorUiState, symbol: String): String {
            current.pendingRepeat?.let { return appendAfterEquals(current.expression, symbol) }
            return appendDuringEdit(current.expression, symbol)
        }

        private fun appendAfterEquals(expr: String, symbol: String): String {
            // After `=`, a number/decimal starts fresh; an operator or a
            // function name chains on the result.
            val isOperator = symbol.length == 1 && symbol[0] in ArithmeticOperators
            val startsFunction = symbol.length > 1 && symbol.last() == '('
            return when {
                isOperator -> expr + symbol
                startsFunction -> symbol
                symbol == "." -> "0."
                else -> symbol
            }
        }

        private fun appendDuringEdit(expr: String, symbol: String): String {
            val isOperator = symbol.length == 1 && symbol[0] in ArithmeticOperators
            return when {
                // Replace the trailing operator instead of stacking another.
                isOperator && expr.isNotEmpty() && expr.last() in ArithmeticOperators ->
                    expr.dropLast(1) + symbol

                // Drop a stray leading `+`, `×`, `÷`, `^` (but allow leading `-` for negation).
                isOperator && expr.isEmpty() && symbol != "-" -> expr

                // Decimal-point rules.
                symbol == "." -> appendDecimal(expr)

                // Leading-zero trim: replace a lone `0` segment with the typed digit.
                symbol.length == 1 && symbol[0].isDigit() && currentNumberIsLoneZero(expr) ->
                    expr.dropLast(1) + symbol

                else -> expr + symbol
            }
        }

        private fun appendDecimal(expr: String): String {
            if (currentNumberHasDot(expr)) return expr
            val needsZeroPrefix = expr.isEmpty() || expr.last() in ArithmeticOperators || expr.last() == '('
            return if (needsZeroPrefix) "$expr${"0."}" else "$expr."
        }

        private fun backspace() =
            _state.update { current ->
                val next = current.expression.dropLast(1)
                current
                    .copy(
                        expression = next,
                        liveResult = preview(next, current.angleMode),
                        errorMessage = null,
                        pendingRepeat = null,
                    ).also(::persist)
            }

        private fun clear() =
            _state.update { current ->
                // Clear wipes the expression and error/repeat, but keeps user
                // preferences (scientific mode, angle mode, memory).
                BasicCalculatorUiState(
                    scientific = current.scientific,
                    angleMode = current.angleMode,
                    memory = current.memory,
                ).also(::persist)
            }

        private fun commit() =
            _state.update { current ->
                if (current.expression.isBlank()) return@update current

                val (toEvaluate, repeatToken) = buildEquationToEvaluate(current)

                when (val result = evaluatorFor(current.angleMode).evaluate(toEvaluate)) {
                    is EvaluationResult.Success -> {
                        val canonicalResult = result.value.stripTrailingZeros().toPlainString()
                        // Record into history only on a fresh equals (not on
                        // a replay through pendingRepeat) so the repeat-equals
                        // chain doesn't spam the table. The repository call is
                        // a fire-and-forget; failures are non-fatal because
                        // the user already saw their result.
                        if (current.pendingRepeat == null) {
                            recordHistory(toEvaluate, canonicalResult, current.scientific)
                        }
                        current
                            .copy(
                                // Replace the expression with the canonical result so the
                                // user can keep chaining (e.g. press `+` after `=`).
                                // `stripTrailingZeros` collapses `4.0` to `4` for display,
                                // and `toPlainString` keeps it out of scientific notation so
                                // the next keypress chains naturally.
                                expression = canonicalResult,
                                liveResult = null,
                                errorMessage = null,
                                pendingRepeat = repeatToken,
                            ).also(::persist)
                    }

                    is EvaluationResult.Error ->
                        current
                            .copy(
                                liveResult = null,
                                errorMessage = errorToMessage(result),
                                pendingRepeat = null,
                            ).also(::persist)
                }
            }

        private fun recordHistory(expression: String, result: String, scientific: Boolean) {
            val repo = historyRepository ?: return
            viewModelScope.launch {
                runCatching {
                    repo.add(
                        expression = expression,
                        result = result,
                        type = if (scientific) HistoryEntry.Type.Scientific else HistoryEntry.Type.Basic,
                    )
                }
            }
        }

        private fun toggleScientific() =
            _state.update { current ->
                current.copy(scientific = !current.scientific).also(::persist)
            }

        private fun toggleAngleMode() =
            _state.update { current ->
                val next =
                    when (current.angleMode) {
                        AngleMode.Radian -> AngleMode.Degree
                        AngleMode.Degree -> AngleMode.Radian
                    }
                current
                    .copy(
                        angleMode = next,
                        liveResult = preview(current.expression, next),
                    ).also(::persist)
            }

        /** Add or subtract the current display value to the stored memory. */
        private fun memoryDelta(addToMemory: Boolean) =
            _state.update { current ->
                val currentValue = currentDisplayValue(current) ?: return@update current
                val nextMemory =
                    if (addToMemory) current.memory + currentValue else current.memory - currentValue
                current.copy(memory = nextMemory).also(::persist)
            }

        private fun memoryRecall() =
            _state.update { current ->
                // Empty memory means M+/M- was never pressed (or MC was).
                // Recalling it would just append "0" - and on the next
                // tap "×0", building "0×0×0×..." indefinitely. Treat it
                // as a no-op instead so MR is harmless before any value
                // is stored.
                if (current.memory.signum() == 0) return@update current
                val mem = current.memory.stripTrailingZeros().toPlainString()
                val next =
                    when {
                        current.pendingRepeat != null -> mem
                        current.expression.isEmpty() -> mem
                        current.expression.last() in ArithmeticOperators ||
                            current.expression.last() == '(' -> current.expression + mem
                        else -> current.expression + "×" + mem
                    }
                current
                    .copy(
                        expression = next,
                        liveResult = preview(next, current.angleMode),
                        errorMessage = null,
                        pendingRepeat = null,
                    ).also(::persist)
            }

        private fun memoryClear() =
            _state.update { current ->
                current.copy(memory = BigDecimal.ZERO).also(::persist)
            }

        /**
         * Toggle the sign of the trailing numeric operand in the expression.
         *
         * Examples (`|` marks where the user's caret would be):
         *  - `5|`         -> `-5|`
         *  - `-5|`        -> `5|`
         *  - `10+5|`      -> `10+-5|`   (the engine parses `+-5` as `+(-5)`)
         *  - `10+-5|`     -> `10+5|`
         *  - `|` (empty)  -> `-|`       (so the next digit lands as `-digit`)
         *  - `10+|`       -> `10+-|`    (no operand yet; arm a unary minus
         *                                for the next digit)
         *
         * After equals, the expression IS the result, so `signFlip` just
         * flips its sign - same code path because the result is a single
         * numeric token.
         */
        private fun signFlip() =
            _state.update { current ->
                val flipped = flipSignOfTrailingOperand(current.expression)
                current
                    .copy(
                        expression = flipped,
                        liveResult = preview(flipped, current.angleMode),
                        errorMessage = null,
                        pendingRepeat = null,
                    ).also(::persist)
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

            // Auto-close unbalanced opens before any other transformation.
            // `(1+2` evaluates as `(1+2)` and `((5+1` as `((5+1))`.
            val expr = autoCloseParens(current.expression)

            // Trailing operator: auto-complete with the operand the user just
            // typed (the number immediately before that operator). So `1+=`
            // resolves as `1+1=2`, `10-3×=` as `10-3×3=1`, and so on. This
            // is the *only* path that arms a repeat-equals token: pressing
            // `=` again replays the auto-completed `+1`, `×3`, etc.
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

            // Normal path: a complete expression like `2+3`. We arm an
            // empty-string repeat token: it does NOT add anything on
            // subsequent `=` presses (the user has to type a dangling
            // operator to opt into the repeat chain) but it still signals
            // "we just completed an equals" so digit/decimal presses
            // correctly start a fresh expression (see [appendAfterEquals]).
            return expr to ""
        }

        /**
         * Compute a non-binding preview result.
         *
         * Errors during preview are intentionally swallowed: the user is still
         * typing, and partial expressions like `5 +` should not light up
         * an error banner mid-input.
         */
        private fun preview(expression: String, angleMode: AngleMode): String? {
            if (expression.isBlank()) return null
            return when (val result = evaluatorFor(angleMode).evaluate(expression)) {
                is EvaluationResult.Success -> result.value.stripTrailingZeros().toPlainString()
                is EvaluationResult.Error -> null
            }
        }

        /** Build a fresh evaluator pinned to the current angle mode. */
        private fun evaluatorFor(angleMode: AngleMode): Evaluator = Evaluator(angleMode = angleMode)

        /** Best-effort current numeric value: the live result if available, else null. */
        private fun currentDisplayValue(state: BasicCalculatorUiState): BigDecimal? =
            state.liveResult?.let { runCatching { BigDecimal(it) }.getOrNull() }
                ?: runCatching { BigDecimal(state.expression) }.getOrNull()

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
                is EvaluationResult.Error.Domain -> "Math out of domain"
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

        /**
         * Return true if the current number segment is exactly `"0"`
         * (i.e. ends in `0`, and the character before it - if any - is an
         * operator or open paren). Used to power leading-zero trimming.
         */
        private fun currentNumberIsLoneZero(expr: String): Boolean {
            if (expr.isEmpty() || expr.last() != '0') return false
            val prev = expr.getOrNull(expr.length - 2) ?: return true
            return prev in ArithmeticOperators || prev == '('
        }

        /**
         * Append the matching `)` for every unbalanced `(` so `(1+2` reads
         * as `(1+2)` when `=` fires. A naive paren count is enough: the
         * tokenizer is the source of truth on mismatch, so this is just a
         * convenience the user expects from a calculator.
         */
        private fun autoCloseParens(expr: String): String {
            val opens = expr.count { it == '(' }
            val closes = expr.count { it == ')' }
            return if (opens > closes) expr + ")".repeat(opens - closes) else expr
        }

        /**
         * Toggle the leading `-` of the rightmost numeric operand.
         *
         * "Operand" here is whatever the cursor is on - the trailing
         * digits/dot string, optionally preceded by a unary `-`. If that
         * unary `-` is present we remove it; otherwise we insert one.
         *
         * Three edge cases handled at the boundary:
         *  - Empty expression: there's nothing to flip, but the user
         *    pressing `±` first wants to start a negative number - so
         *    we return `"-"` so the next digit appends to it.
         *  - Expression ends with an operator (`10+`): no operand yet;
         *    insert `-` after the operator (`10+-`) so the next digit
         *    becomes the negative operand.
         *  - Expression ends with `)` or `!`: don't try to flip; the
         *    operand isn't a single numeric token.
         */
        private fun flipSignOfTrailingOperand(expr: String): String {
            if (expr.isEmpty()) return "-"
            val last = expr.last()
            if (last in ArithmeticOperators || last == '(') {
                // Arm a unary minus for the next digit.
                return expr + "-"
            }
            if (last == ')' || last == '!') return expr // operand isn't a flat number
            val match = TrailingNumberRegex.find(expr) ?: return expr
            val numStart = match.range.first
            // Is there a leading `-` directly before the number that we should
            // remove? It counts as unary if it sits at the start of input, or
            // right after an operator/`(`.
            val minusPos = numStart - 1
            if (minusPos >= 0 && expr[minusPos] == '-') {
                val priorPos = minusPos - 1
                val isUnary = priorPos < 0 || expr[priorPos] in "+-×÷*/^("
                if (isUnary) {
                    return expr.removeRange(minusPos, minusPos + 1)
                }
            }
            return expr.substring(0, numStart) + "-" + expr.substring(numStart)
        }

        private companion object {
            const val KEY_EXPRESSION = "calculator.expression"
            const val KEY_ERROR = "calculator.error"
            const val KEY_PENDING_REPEAT = "calculator.pendingRepeat"
            const val KEY_SCIENTIFIC = "calculator.scientific"
            const val KEY_ANGLE_MODE = "calculator.angleMode"
            const val KEY_MEMORY = "calculator.memory"

            /** Characters the keypad treats as binary arithmetic operators. */
            val ArithmeticOperators = setOf('+', '-', '×', '÷', '*', '/', '^')

            /** Trailing `op + operand` for repeat-equals. */
            val TrailingOpRegex = Regex("([+\\-×÷*/^])(\\d+(?:\\.\\d+)?)$")

            /**
             * The numeric literal sitting immediately before a trailing
             * operator. Used to auto-complete an incomplete expression on
             * `=` (e.g. `10-3×` → use `3`, the operand just entered).
             */
            val PrecedingNumberRegex = Regex("(\\d+(?:\\.\\d+)?)[+\\-×÷*/^]$")

            /** The trailing numeric literal of the expression. Used by [flipSignOfTrailingOperand]. */
            val TrailingNumberRegex = Regex("(\\d+(?:\\.\\d+)?|\\.\\d+)$")
        }
    }
