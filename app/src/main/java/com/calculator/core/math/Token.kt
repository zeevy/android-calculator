package com.calculator.core.math

import java.math.BigDecimal

/**
 * Lexical token produced by [Tokenizer] and consumed by [Evaluator].
 *
 * Tokens are deliberately coarse: the expression grammar for the basic
 * calculator only needs numbers, binary operators and parentheses. Unary
 * minus is handled at the tokenizer level by inspecting the previous token,
 * so it does not require a dedicated [Token] variant.
 */
sealed interface Token {
    /** A decimal literal. */
    data class Number(
        val value: BigDecimal,
    ) : Token

    /** Binary or unary operator. See [Operator.symbol] for the set of supported operators. */
    data class Op(
        val operator: Operator,
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
 *   multiplication and division have higher precedence than addition and subtraction).
 * @property rightAssociative Whether the operator is right-associative.
 *   None of the basic operators are right-associative; the flag exists so that
 *   future exponentiation support (`^`) can opt in without touching the parser.
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
    ;

    companion object {
        /** Resolve an [Operator] from its [symbol] or `null` if unknown. */
        fun fromSymbol(c: Char): Operator? =
            when (c) {
                '+' -> Add
                '-' -> Subtract
                '*', '×' -> Multiply
                '/', '÷' -> Divide
                else -> null
            }
    }
}
