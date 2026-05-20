package com.calculator.core.domain.math

/**
 * Pure-Kotlin helpers for the three percentage scenarios most people
 * reach for: scaling a value by a percent, deriving a percent from two
 * values, and computing the percent change between two values.
 */
object PercentCalculator {
    /** `percent`% of `value` (e.g. 20% of 250 = 50). */
    fun percentOf(percent: Double, value: Double): Double = percent / 100.0 * value

    /** What percent is `part` of `whole` (e.g. 30 of 120 = 25%). */
    fun whatPercent(part: Double, whole: Double): Double {
        require(whole != 0.0) { "whole must not be zero" }
        return part / whole * 100.0
    }

    /**
     * Percent change from `from` to `to` (e.g. 80 → 100 = +25%, 100 →
     * 80 = -20%). The base for the percentage is the FROM side.
     */
    fun percentChange(from: Double, to: Double): Double {
        require(from != 0.0) { "from must not be zero" }
        return (to - from) / from * 100.0
    }
}
