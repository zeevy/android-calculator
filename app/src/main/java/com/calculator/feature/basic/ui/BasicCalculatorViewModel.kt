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
                val next = current.expression + symbol
                current.copy(
                    expression = next,
                    liveResult = preview(next),
                    errorMessage = null,
                )
            }

        private fun backspace() =
            _state.update { current ->
                val next = current.expression.dropLast(1)
                current.copy(
                    expression = next,
                    liveResult = preview(next),
                    errorMessage = null,
                )
            }

        private fun clear() = _state.update { BasicCalculatorUiState() }

        private fun commit() =
            _state.update { current ->
                if (current.expression.isBlank()) return@update current

                when (val result = evaluator.evaluate(current.expression)) {
                    is EvaluationResult.Success ->
                        current.copy(
                            // Replace the expression with the canonical result so the
                            // user can keep chaining (e.g. press `+` after `=`).
                            expression = result.value.toPlainString(),
                            liveResult = null,
                            errorMessage = null,
                        )
                    is EvaluationResult.Error ->
                        current.copy(
                            liveResult = null,
                            errorMessage = errorToMessage(result),
                        )
                }
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
    }
