package com.calculator.core.domain.finance

/**
 * Thrown when a calculation overflows a [Double] (for example a very long
 * loan tenure or investment term), so the screen can say the value is too
 * large instead of showing "NaN" or "∞".
 */
class ResultTooLargeException : ArithmeticException("result too large")

/** Returns this value, or throws [ResultTooLargeException] when it is NaN or infinite. */
internal fun Double.requireFinite(): Double = if (isFinite()) this else throw ResultTooLargeException()
