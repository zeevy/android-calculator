package com.calculator.core.math

import java.math.BigDecimal

/**
 * Typed outcome of an expression evaluation.
 *
 * Using a sealed result instead of throwing across the API boundary keeps
 * the UI layer simple: callers pattern-match exhaustively and never have
 * to wrap calls in `try/catch`.
 */
sealed interface EvaluationResult {
    /** Successful evaluation. */
    data class Success(
        val value: BigDecimal,
    ) : EvaluationResult

    /**
     * Evaluation failed. Each subtype maps to a user-presentable message
     * declared in the design-system string resources; the engine itself
     * never produces localised strings.
     */
    sealed interface Error : EvaluationResult {
        /** Division by zero, e.g. `5 / 0`. */
        data object DivisionByZero : Error

        /** The expression is syntactically invalid (e.g. mismatched parentheses, dangling operator). */
        data class Syntax(
            val reason: String,
        ) : Error

        /** The input contained a character the tokenizer could not classify. */
        data class UnknownToken(
            val reason: String,
        ) : Error
    }
}
