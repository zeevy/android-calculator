package com.calculator.core.math

import java.math.BigDecimal
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
 * Thread-safety: each call to [evaluate] is self-contained, so the
 * same instance may be shared across threads.
 *
 * @property mathContext Precision and rounding mode used for arithmetic.
 */
class Evaluator(
    private val mathContext: MathContext = MathContext.DECIMAL64,
) {
    private val tokenizer = Tokenizer()

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
     *  - Numbers are emitted directly to the output queue.
     *  - Operators pop from the operator stack while the top of the stack
     *    has higher (or equal, for left-associative ops) precedence.
     *  - `(` is pushed unconditionally.
     *  - `)` pops operators until the matching `(` is found.
     */
    private fun toReversePolishNotation(tokens: List<Token>): List<Token> {
        val output = mutableListOf<Token>()
        val operators = ArrayDeque<Token>()

        for (token in tokens) {
            when (token) {
                is Token.Number -> output += token

                is Token.Op -> {
                    while (operators.isNotEmpty()) {
                        val top = operators.peek()
                        if (top !is Token.Op) break
                        val popTop =
                            top.operator.precedence > token.operator.precedence ||
                                (top.operator.precedence == token.operator.precedence && !token.operator.rightAssociative)
                        if (popTop) output += operators.pop() else break
                    }
                    operators.push(token)
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
     * Fold an RPN token list to a final [BigDecimal].
     *
     * @throws IllegalStateException when the expression is malformed
     *   (e.g. operator with no operands).
     * @throws ArithmeticException on division by zero - caller wraps this
     *   into [EvaluationResult.Error.DivisionByZero].
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
            Operator.Add -> left.add(right, mathContext)
            Operator.Subtract -> left.subtract(right, mathContext)
            Operator.Multiply -> left.multiply(right, mathContext)
            Operator.Divide -> {
                // BigDecimal.divide with non-terminating decimals requires a MathContext,
                // which we always provide. Division by zero is rethrown as ArithmeticException.
                left.divide(right, mathContext)
            }
        }
}
