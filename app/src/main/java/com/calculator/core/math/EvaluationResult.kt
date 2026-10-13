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

        /**
         * The input was well-formed but operated on a value outside its
         * domain - e.g. `log(-1)`, `sqrt(-1)`, `(-1)!`.
         *
         * @property kind Which rule was broken, for choosing a message.
         * @property function The function that rejected its argument, for [DomainKind.Function].
         * @property reason Technical detail for logs and tests; not for display.
         */
        data class Domain(
            val kind: DomainKind,
            val function: FunctionId? = null,
            val reason: String = "",
        ) : Error
    }
}

/** The rule an out-of-domain input broke. */
enum class DomainKind {
    /** A function got an argument it is not defined for, e.g. `sqrt(-1)`, `tan(90°)`. */
    Function,

    /** `n!` with a negative `n`. */
    FactorialOfNegative,

    /** `n!` with a fractional `n`. */
    FactorialOfNonInteger,

    /** `n!` above the engine's cap. */
    FactorialTooLarge,

    /** `x^y` too large for a Double. */
    PowerOverflow,

    /** `x^y` with no real result, e.g. `(-8)^0.5`. */
    InvalidPower,
}
