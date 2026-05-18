package com.calculator.core.math

import java.math.BigDecimal

/**
 * Converts a raw expression string into a flat list of [Token]s.
 *
 * The tokenizer is intentionally permissive:
 *  - Whitespace is ignored.
 *  - Both ASCII `*` `/` and the typographic `×` `÷` are accepted for multiplication and division.
 *  - A leading `-` (or one immediately after an operator or open paren) is
 *    folded into the following numeric literal so the evaluator does not
 *    need a dedicated unary-minus rule.
 *
 * Anything it cannot classify produces a [TokenizationException] - the
 * evaluator wraps this into a typed [EvaluationResult.Error] for callers.
 *
 * The tokenizer is **pure Kotlin** with no Android dependencies, so it
 * runs in plain JVM unit tests without Robolectric.
 */
class Tokenizer {
    /**
     * Tokenize [expression] into [Token]s.
     *
     * @throws TokenizationException when an unexpected character is encountered
     *   or a number literal is malformed (e.g. contains two decimal points).
     */
    fun tokenize(expression: String): List<Token> {
        val tokens = mutableListOf<Token>()
        var index = 0

        while (index < expression.length) {
            val c = expression[index]
            when {
                c.isWhitespace() -> index++

                c.isDigit() || c == '.' -> {
                    // Detect unary minus by inspecting the previous token: a `-`
                    // is unary if it appears at the start of input, immediately
                    // after an operator, or immediately after `(`.
                    val (literal, consumed) = readNumber(expression, index)
                    tokens += Token.Number(BigDecimal(literal))
                    index += consumed
                }

                c == '-' && isUnaryMinusContext(tokens) -> {
                    val nextIdx = index + 1
                    val nextChar = expression.getOrNull(nextIdx)
                    if (nextChar != null && (nextChar.isDigit() || nextChar == '.')) {
                        // Common case: fold the sign into the following numeric literal.
                        val (literal, consumed) = readNumber(expression, nextIdx)
                        tokens += Token.Number(BigDecimal("-$literal"))
                        index += consumed + 1
                    } else {
                        // Unary minus before `(` or another `-`: rewrite as
                        // `-1 ×` so the existing binary-operator pipeline
                        // handles it correctly. `-(2+3)` becomes `-1×(2+3) = -5`
                        // and `--5` becomes `-1×-5 = 5`.
                        tokens += Token.Number(BigDecimal.ONE.negate())
                        tokens += Token.Op(Operator.Multiply)
                        index++
                    }
                }

                c == '(' -> {
                    tokens += Token.LeftParen
                    index++
                }

                c == ')' -> {
                    tokens += Token.RightParen
                    index++
                }

                c == '%' -> {
                    // Postfix percentage: rewrite `N%` as `N ÷ 100`. So
                    // `5%` → `0.05` and `100+10%` → `100 + 10÷100` → `100.1`.
                    tokens += Token.Op(Operator.Divide)
                    tokens += Token.Number(BigDecimal(PERCENT_DIVISOR))
                    index++
                }

                else -> {
                    val op =
                        Operator.fromSymbol(c)
                            ?: throw TokenizationException("Unexpected character '$c' at index $index")
                    tokens += Token.Op(op)
                    index++
                }
            }
        }

        return tokens
    }

    /**
     * Read a numeric literal starting at [start].
     *
     * @return the literal text and how many characters were consumed.
     * @throws TokenizationException if the literal contains more than one decimal point.
     */
    private fun readNumber(
        source: String,
        start: Int,
    ): Pair<String, Int> {
        var end = start
        var dotSeen = false
        while (end < source.length) {
            val c = source[end]
            when {
                c.isDigit() -> end++
                c == '.' -> {
                    if (dotSeen) {
                        throw TokenizationException("Malformed number at index $start: two decimal points")
                    }
                    dotSeen = true
                    end++
                }
                else -> break
            }
        }
        val literal = source.substring(start, end)
        if (literal.isEmpty() || literal == ".") {
            throw TokenizationException("Malformed number at index $start: '$literal'")
        }
        return literal to (end - start)
    }

    /**
     * A `-` is unary when nothing precedes it that could be its left operand:
     * the start of input, an operator, or an open parenthesis.
     */
    private fun isUnaryMinusContext(soFar: List<Token>): Boolean =
        when (soFar.lastOrNull()) {
            null, is Token.Op, Token.LeftParen -> true
            else -> false
        }

    private companion object {
        /** Divisor used to rewrite postfix `%`: `N%` becomes `N ÷ 100`. */
        const val PERCENT_DIVISOR = "100"
    }
}

/** Thrown when the input cannot be tokenized. */
class TokenizationException(
    message: String,
) : IllegalArgumentException(message)
