package com.calculator.core.math

import java.math.BigDecimal

/**
 * Lexical token produced by [Tokenizer] and consumed by [Evaluator].
 *
 * Tokens are deliberately coarse: the expression grammar covers numbers,
 * binary operators, parentheses, and named functions like `sin(...)`.
 * Unary minus is handled at the tokenizer level by inspecting the previous
 * token, so it does not require a dedicated [Token] variant.
 */
sealed interface Token {
    /** A decimal literal. */
    data class Number(
        val value: BigDecimal,
    ) : Token

    /** Binary operator. See [Operator.symbol] for the supported set. */
    data class Op(
        val operator: Operator,
    ) : Token

    /** Named unary function (e.g. `sin`, `log`, `sqrt`). */
    data class Function(
        val func: FunctionId,
    ) : Token

    /** Opening parenthesis `(`. */
    data object LeftParen : Token

    /** Closing parenthesis `)`. */
    data object RightParen : Token
}

/**
 * Supported arithmetic operators.
 *
 * @property symbol Single-character representation used in the input expression.
 * @property precedence Higher value binds tighter (standard math precedence:
 *   multiplication/division beat addition/subtraction; exponentiation beats
 *   multiplication).
 * @property rightAssociative Whether the operator is right-associative. Only
 *   exponentiation is - `2^3^2` reads as `2^(3^2) = 512`, not `(2^3)^2 = 64`.
 */
enum class Operator(
    val symbol: Char,
    val precedence: Int,
    val rightAssociative: Boolean = false,
) {
    Add(symbol = '+', precedence = 1),
    Subtract(symbol = '-', precedence = 1),
    Multiply(symbol = '×', precedence = 2), // U+00D7 MULTIPLICATION SIGN
    Divide(symbol = '÷', precedence = 2), // U+00F7 DIVISION SIGN
    Power(symbol = '^', precedence = 3, rightAssociative = true),
    ;

    companion object {
        /** Resolve an [Operator] from its [symbol] or `null` if unknown. */
        fun fromSymbol(c: Char): Operator? =
            when (c) {
                '+' -> Add
                '-' -> Subtract
                '*', '×' -> Multiply
                '/', '÷' -> Divide
                '^' -> Power
                else -> null
            }
    }
}

/**
 * Named unary functions exposed in scientific mode.
 *
 * Trig functions read their argument in the active [AngleMode]; everything
 * else is mode-agnostic. Transcendental ops route through [Double] under the
 * hood (BigDecimal has no native sin/log) and are rounded back to the
 * evaluator's configured precision.
 *
 * @property keyword Lower-case identifier the tokenizer matches against.
 */
enum class FunctionId(val keyword: String) {
    Sin("sin"),
    Cos("cos"),
    Tan("tan"),
    Asin("asin"),
    Acos("acos"),
    Atan("atan"),
    Log("log"),
    Ln("ln"),
    Sqrt("sqrt"),
    Cbrt("cbrt"),
    ;

    companion object {
        private val byKeyword = entries.associateBy { it.keyword }

        /** Resolve a function by its lower-case keyword, or `null` if unknown. */
        fun fromKeyword(keyword: String): FunctionId? = byKeyword[keyword]
    }
}

/**
 * Angle interpretation for trigonometric inputs and outputs.
 *
 * Defaults to [Radian] because the underlying `java.lang.Math` trig ops
 * work in radians; [Degree] mode just converts at the boundary.
 */
enum class AngleMode {
    Radian,
    Degree,
}
