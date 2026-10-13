package com.calculator.core.math

import java.math.BigDecimal

/**
 * Keypad input rules shared by the app, the home-screen widget and the
 * floating calculator, so all three build the same expression from the
 * same key presses and give the same answer.
 *
 * Pure Kotlin and stateless: each function takes the current expression
 * text and returns the new one.
 *
 * Rules (match common physical and iOS-style calculators):
 *  - Consecutive operators collapse: typing `1 + + ×` ends up as `1×`.
 *  - Leading `+`, `×`, `÷`, `^` are ignored; leading `-` is allowed (negation).
 *  - Only one decimal point per number segment.
 *  - A leading `.` is auto-prefixed with `0`, so `.5` reads as `0.5`.
 *  - A lone leading `0` is replaced by the next digit.
 *  - After `=`, a digit starts a fresh expression; an operator, `%`, `!`
 *    or `^2` chains on the result.
 *  - On `=`, open parentheses are closed and a trailing operator is
 *    completed with the number before it (`1 + =` is `1 + 1`).
 */
object ExpressionEditor {
    /** Characters the keypad treats as binary arithmetic operators. */
    val ArithmeticOperators: Set<Char> = setOf('+', '-', '×', '÷', '*', '/', '^')

    /**
     * Expression after typing [symbol] (a digit, `.`, an operator, a
     * parenthesis or a function name such as `sin(`).
     *
     * @param afterEquals True when [expression] is a result just produced
     *   by `=`, so a digit starts over instead of appending.
     */
    fun append(
        expression: String,
        symbol: String,
        afterEquals: Boolean = false,
    ): String = if (afterEquals) appendAfterEquals(expression, symbol) else appendDuringEdit(expression, symbol)

    /**
     * The equation to evaluate when `=` is pressed, and the `op + operand`
     * a further `=` should repeat (empty when there is nothing to repeat,
     * null when the expression is only an operator).
     */
    fun completeForEquals(expression: String): Pair<String, String?> {
        // `(1+2` evaluates as `(1+2)` and `((5+1` as `((5+1))`.
        val expr = closeParentheses(expression)
        if (expr.isNotEmpty() && expr.last() in ArithmeticOperators) {
            // `1+=` resolves as `1+1`, `10-3×=` as `10-3×3`; a further `=`
            // repeats the completed `+1`, `×3`.
            val precedingNumber = PrecedingNumberRegex.find(expr)?.groupValues?.getOrNull(1)
            return if (precedingNumber != null) {
                (expr + precedingNumber) to (expr.last() + precedingNumber)
            } else {
                // Just an operator (e.g. `-`); let the evaluator surface the error.
                expr to null
            }
        }
        // A complete expression: the empty repeat token arms nothing for
        // the next `=` but still marks "just evaluated", so a digit starts
        // a fresh expression.
        return expr to ""
    }

    /**
     * Toggle the sign of the rightmost number.
     *
     * Empty input or a trailing operator / `(` gets a `-` for the next
     * digit; a trailing `)` or `!` is left alone because the operand is
     * not a single number.
     */
    @Suppress("ReturnCount")
    fun flipSign(expression: String): String {
        if (expression.isEmpty()) return "-"
        val last = expression.last()
        if (last in ArithmeticOperators || last == '(') return expression + "-"
        if (last == ')' || last == '!') return expression
        val match = TrailingNumberRegex.find(expression) ?: return expression
        val numStart = match.range.first
        // A `-` right before the number is unary (and so removable) when it
        // starts the input or follows an operator or `(`.
        val minusPos = numStart - 1
        if (minusPos >= 0 && expression[minusPos] == '-') {
            val priorPos = minusPos - 1
            if (priorPos < 0 || expression[priorPos] in "+-×÷*/^(") {
                return expression.removeRange(minusPos, minusPos + 1)
            }
        }
        return expression.substring(0, numStart) + "-" + expression.substring(numStart)
    }

    /** Appends the `)` for every unbalanced `(`. */
    fun closeParentheses(expression: String): String {
        val opens = expression.count { it == '(' }
        val closes = expression.count { it == ')' }
        return if (opens > closes) expression + ")".repeat(opens - closes) else expression
    }

    /** A result as the display and the next keypress use it: `4.0` as `4`, never in E notation. */
    fun resultText(value: BigDecimal): String = value.stripTrailingZeros().toPlainString()

    private fun appendAfterEquals(
        expr: String,
        symbol: String,
    ): String {
        val isOperator = symbol.length == 1 && symbol[0] in ArithmeticOperators
        // `%`, `!` and `^2` act on the result, so they chain too.
        val actsOnResult = symbol.first() in "%!^"
        val startsFunction = symbol.length > 1 && symbol.last() == '('
        return when {
            isOperator || actsOnResult -> expr + symbol
            startsFunction -> symbol
            symbol == "." -> "0."
            else -> symbol
        }
    }

    private fun appendDuringEdit(
        expr: String,
        symbol: String,
    ): String {
        val isOperator = symbol.length == 1 && symbol[0] in ArithmeticOperators
        return when {
            // Replace the trailing operator instead of stacking another.
            isOperator && expr.isNotEmpty() && expr.last() in ArithmeticOperators -> expr.dropLast(1) + symbol
            // Drop a stray leading `+`, `×`, `÷`, `^` (but allow leading `-` for negation).
            isOperator && expr.isEmpty() && symbol != "-" -> expr
            symbol == "." -> appendDecimal(expr)
            symbol.length == 1 && symbol[0].isDigit() && currentNumberIsLoneZero(expr) -> expr.dropLast(1) + symbol
            else -> expr + symbol
        }
    }

    private fun appendDecimal(expr: String): String {
        if (currentNumberHasDot(expr)) return expr
        val needsZeroPrefix = expr.isEmpty() || expr.last() in ArithmeticOperators || expr.last() == '('
        return if (needsZeroPrefix) "${expr}0." else "$expr."
    }

    /** True if the number being typed already has a `.`. */
    private fun currentNumberHasDot(expr: String): Boolean {
        for (i in expr.length - 1 downTo 0) {
            val c = expr[i]
            if (c in ArithmeticOperators || c == '(' || c == ')') return false
            if (c == '.') return true
        }
        return false
    }

    /** True if the number being typed is exactly `0`. */
    private fun currentNumberIsLoneZero(expr: String): Boolean {
        if (expr.isEmpty() || expr.last() != '0') return false
        val prev = expr.getOrNull(expr.length - 2) ?: return true
        return prev in ArithmeticOperators || prev == '('
    }

    /** The number right before a trailing operator, for `1 + =` completion. */
    private val PrecedingNumberRegex = Regex("(\\d+(?:\\.\\d+)?)[+\\-×÷*/^]$")

    /** The trailing number of the expression, for [flipSign]. */
    private val TrailingNumberRegex = Regex("(\\d+(?:\\.\\d+)?|\\.\\d+)$")
}
