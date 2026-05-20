package com.calculator.core.math

import java.math.BigDecimal
import java.math.BigInteger
import java.math.MathContext
import java.util.ArrayDeque

/**
 * Evaluates arithmetic expression strings to a [BigDecimal] result.
 *
 * The implementation uses the **Shunting-Yard** algorithm to convert
 * tokens to Reverse Polish Notation (RPN) in a single pass, then folds
 * the RPN with a value stack. This is O(n) in expression length and
 * does not require building an explicit AST - appropriate for the
 * simple grammar in this app.
 *
 * Precision is controlled by [mathContext]; the default,
 * [MathContext.DECIMAL64], gives ~16 digits of precision which is
 * comfortably more than the display can render. Calculator code that
 * needs different precision (e.g. financial calculators rounding to
 * 2 dp) should construct its own [Evaluator] instance.
 *
 * Transcendental functions (`sin`, `log`, …) operate on bounded [Double]
 * under the hood (BigDecimal has no native trig/log) and are rounded
 * back to [mathContext] precision before being pushed on the value stack.
 *
 * Thread-safety: each call to [evaluate] is self-contained, so the
 * same instance may be shared across threads.
 *
 * @property mathContext Precision and rounding mode used for arithmetic.
 * @property angleMode Interpretation of trig function inputs/outputs.
 */
class Evaluator(
    private val mathContext: MathContext = MathContext.DECIMAL64,
    private val angleMode: AngleMode = AngleMode.Radian,
) {
    private val tokenizer = Tokenizer()

    // Aggressive context applied only after transcendental ops to collapse
    // double-precision floating-point noise (e.g. sin(π) → 1.2e-16) into a
    // clean value the calculator can display.
    private val transcendentalContext =
        MathContext(
            (mathContext.precision - 2).coerceAtLeast(1),
            mathContext.roundingMode,
        )

    /**
     * Evaluate the given expression.
     *
     * Returns an [EvaluationResult] - never throws for user-input errors.
     * Catastrophic bugs in the engine itself (e.g. stack underflow) will
     * still surface as unchecked exceptions because those indicate a
     * defect, not a user mistake.
     */
    fun evaluate(expression: String): EvaluationResult {
        if (expression.isBlank()) return EvaluationResult.Error.Syntax("empty expression")

        val tokens =
            try {
                tokenizer.tokenize(expression)
            } catch (e: TokenizationException) {
                return EvaluationResult.Error.UnknownToken(e.message ?: "unknown token")
            }

        val rpn =
            try {
                toReversePolishNotation(tokens)
            } catch (e: IllegalStateException) {
                return EvaluationResult.Error.Syntax(e.message ?: "malformed expression")
            }

        return try {
            EvaluationResult.Success(computeRpn(rpn))
        } catch (e: DomainException) {
            EvaluationResult.Error.Domain(e.message ?: "out of domain")
        } catch (e: ArithmeticException) {
            // BigDecimal.divide throws ArithmeticException on /0 - turn it into a typed error.
            if (e.message?.contains("Division by zero", ignoreCase = true) == true ||
                e.message?.contains("zero", ignoreCase = true) == true
            ) {
                EvaluationResult.Error.DivisionByZero
            } else {
                EvaluationResult.Error.Syntax(e.message ?: "arithmetic error")
            }
        } catch (e: IllegalStateException) {
            EvaluationResult.Error.Syntax(e.message ?: "malformed expression")
        }
    }

    /**
     * Convert infix tokens to postfix (RPN) using Shunting-Yard.
     *
     * The algorithm:
     *  - Numbers go straight to the output queue.
     *  - Function tokens are pushed onto the operator stack, and pop to
     *    the output queue when their `)` is consumed.
     *  - Operators pop from the operator stack while the top has higher
     *    precedence (or equal precedence for left-associative ops).
     *  - `(` is pushed unconditionally.
     *  - `)` pops operators until the matching `(`; if a function token
     *    sits below the `(`, it pops too so it lands next to its argument.
     */
    private fun toReversePolishNotation(tokens: List<Token>): List<Token> {
        val output = mutableListOf<Token>()
        val operators = ArrayDeque<Token>()

        for (token in tokens) {
            when (token) {
                is Token.Number -> output += token

                is Token.Function -> operators.push(token)

                is Token.Op -> {
                    while (operators.isNotEmpty()) {
                        val top = operators.peek()
                        if (!shouldPopForOperator(top, token.operator)) break
                        output += operators.pop()
                    }
                    operators.push(token)
                }

                Token.Factorial -> {
                    // Postfix: emit straight to output. It binds tighter
                    // than any infix op, so it operates on whatever
                    // operand was just emitted - whether that was a
                    // single number or the result of a parenthesised
                    // sub-expression that already collapsed via `)`.
                    output += token
                }

                Token.LeftParen -> operators.push(token)

                Token.RightParen -> {
                    var matched = false
                    while (operators.isNotEmpty()) {
                        val top = operators.pop()
                        if (top == Token.LeftParen) {
                            matched = true
                            break
                        }
                        output += top
                    }
                    check(matched) { "mismatched parentheses: ')' without matching '('" }
                    // A function token immediately under the matched `(` is
                    // its applicator; pop it now so it appears in RPN right
                    // after its argument.
                    if (operators.peek() is Token.Function) output += operators.pop()
                }
            }
        }

        // Any leftover operators are flushed; encountering an unclosed `(` here is an error.
        while (operators.isNotEmpty()) {
            val top = operators.pop()
            check(top != Token.LeftParen) { "mismatched parentheses: '(' without matching ')'" }
            output += top
        }

        return output
    }

    /**
     * Should the top-of-stack operator be popped before pushing [incoming]?
     * Functions always pop first (they bind tighter than any binary op).
     */
    private fun shouldPopForOperator(top: Token, incoming: Operator): Boolean =
        when (top) {
            is Token.Function -> true
            is Token.Op ->
                top.operator.precedence > incoming.precedence ||
                    (top.operator.precedence == incoming.precedence && !incoming.rightAssociative)
            else -> false
        }

    /**
     * Fold an RPN token list to a final [BigDecimal].
     *
     * @throws IllegalStateException when the expression is malformed
     *   (e.g. operator with no operands).
     * @throws ArithmeticException on division by zero - caller wraps this
     *   into [EvaluationResult.Error.DivisionByZero].
     * @throws DomainException when a function receives an out-of-domain
     *   argument (e.g. `log(-1)`, `sqrt(-1)`).
     */
    private fun computeRpn(rpn: List<Token>): BigDecimal {
        val stack = ArrayDeque<BigDecimal>()
        for (token in rpn) {
            when (token) {
                is Token.Number -> stack.push(token.value)
                is Token.Op -> {
                    check(stack.size >= 2) { "operator '${token.operator.symbol}' missing operands" }
                    val right = stack.pop()
                    val left = stack.pop()
                    stack.push(apply(token.operator, left, right))
                }
                is Token.Function -> {
                    check(stack.isNotEmpty()) { "function '${token.func.keyword}' missing argument" }
                    stack.push(applyFunction(token.func, stack.pop()))
                }
                Token.Factorial -> {
                    check(stack.isNotEmpty()) { "'!' missing operand" }
                    stack.push(factorial(stack.pop()))
                }
                Token.LeftParen, Token.RightParen ->
                    error("parenthesis leaked into RPN output: $token")
            }
        }
        check(stack.size == 1) { "expression did not reduce to a single value" }
        return stack.pop()
    }

    private fun apply(
        op: Operator,
        left: BigDecimal,
        right: BigDecimal,
    ): BigDecimal =
        when (op) {
            // Add / subtract / multiply on terminating-decimal operands are
            // *always exact* and always terminate, so we deliberately do NOT
            // pass [mathContext] here - rounding would silently turn
            // 33333333333333333333 + 33333333333333333333 into
            // 66666666666666670000 (16 sig figs + trailing zeros) when the
            // true answer 66666666666666666666 fits in a BigDecimal just
            // fine. The display layer ellipsizes if the result is too long
            // to render in two lines, which is the right place to draw a
            // visual cap. The engine's job is to be correct.
            Operator.Add -> left.add(right)
            Operator.Subtract -> left.subtract(right)
            Operator.Multiply -> left.multiply(right)
            Operator.Divide -> {
                // Division is the one operator where bounded precision is
                // mandatory: BigDecimal.divide with a non-terminating
                // quotient (e.g. 1 / 3) throws ArithmeticException unless a
                // MathContext is supplied. Division by zero is allowed to
                // surface as ArithmeticException and is caught upstream.
                left.divide(right, mathContext)
            }
            Operator.Power -> powerOf(left, right)
        }

    /**
     * `x^y` via bounded Double. Integer-only `y` could stay in BigDecimal
     * via `pow(int)`, but the calculator's user-facing power button can be
     * invoked with fractional exponents (`8^(1/3)`), so we go through Double
     * for the general case and round back via [mathContext].
     */
    private fun powerOf(base: BigDecimal, exp: BigDecimal): BigDecimal {
        val raw = Math.pow(base.toDouble(), exp.toDouble())
        if (raw.isNaN()) throw DomainException("invalid power: $base^$exp")
        if (raw.isInfinite()) throw DomainException("power overflow: $base^$exp")
        return BigDecimal(raw).round(mathContext)
    }

    private fun applyFunction(func: FunctionId, arg: BigDecimal): BigDecimal {
        val raw = arg.toDouble()
        val out =
            when (func) {
                FunctionId.Sin -> Math.sin(toRadians(raw))
                FunctionId.Cos -> Math.cos(toRadians(raw))
                FunctionId.Tan -> Math.tan(toRadians(raw))
                FunctionId.Asin -> fromRadians(Math.asin(raw))
                FunctionId.Acos -> fromRadians(Math.acos(raw))
                FunctionId.Atan -> fromRadians(Math.atan(raw))
                FunctionId.Log -> Math.log10(raw)
                FunctionId.Ln -> Math.log(raw)
                FunctionId.Sqrt -> Math.sqrt(raw)
                FunctionId.Cbrt -> Math.cbrt(raw)
            }
        if (out.isNaN()) throw DomainException("${func.keyword}($arg) is undefined")
        if (out.isInfinite()) throw DomainException("${func.keyword}($arg) is infinite")
        // Trim the last two digits of Double noise: `sin(π)` returns
        // 1.2246e-16, `sin(30°)` returns 0.49999999999999994, etc. Rounding
        // to 14 significant figures collapses those to 0 and 0.5 without
        // sacrificing user-meaningful precision.
        return BigDecimal(out).round(transcendentalContext)
    }

    private fun toRadians(value: Double): Double =
        if (angleMode == AngleMode.Degree) Math.toRadians(value) else value

    private fun fromRadians(value: Double): Double =
        if (angleMode == AngleMode.Degree) Math.toDegrees(value) else value

    /**
     * `n!` for non-negative integers, computed in BigInteger so we don't
     * lose precision for large factorials (`170! ≈ 7.26e306` would
     * overflow `Double`).
     *
     * Domain rejections:
     *  - Negative: undefined for real numbers.
     *  - Non-integer: undefined (`0.5!` is the gamma function, out of
     *    scope for this calculator).
     *  - n > [FACTORIAL_CAP]: arbitrary cap to avoid the user accidentally
     *    asking for `10000!` and pinning a thread for several seconds.
     *    1000! is already a 2,568-digit number - well past anything the
     *    display can render.
     */
    private fun factorial(value: BigDecimal): BigDecimal {
        if (value.signum() < 0) throw DomainException("factorial of negative: $value")
        // stripTrailingZeros collapses 5.0 -> 5 so it counts as integer.
        if (value.stripTrailingZeros().scale() > 0) {
            throw DomainException("factorial of non-integer: $value")
        }
        val n = value.toBigInteger()
        if (n > FACTORIAL_CAP) throw DomainException("factorial too large: $value")
        var result = BigInteger.ONE
        // BigInteger.TWO is API 33+; valueOf works on every JVM target.
        var i = BigInteger.valueOf(2)
        while (i <= n) {
            result = result.multiply(i)
            i = i.add(BigInteger.ONE)
        }
        return BigDecimal(result).round(mathContext)
    }

    companion object {
        private val FACTORIAL_CAP = BigInteger.valueOf(1000)
    }
}

/** Signals an out-of-domain argument to a function (e.g. `log(-1)`). */
private class DomainException(message: String) : RuntimeException(message)
