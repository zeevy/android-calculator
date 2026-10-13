package com.calculator.core.domain.finance

import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test
import kotlin.math.ceil

/** Bill split and round up rules in [TipSplitCalculator]. */
class TipSplitCalculatorTest {
    @Test
    fun `plain split shares the bill and tip equally`() {
        val result = TipSplitCalculator.compute(bill = 1000.0, tipAmount = 200.0, people = 4)

        assertEquals(1200.0, result.total, 1e-9)
        assertEquals(300.0, result.perPerson, 1e-9)
        assertEquals(200.0, result.tip, 1e-9)
    }

    @Test
    fun `round up gives each person the next whole amount and adds the extra to the tip`() {
        val bill = 1000.0
        val tip = 100.0
        val people = 3

        val result = TipSplitCalculator.compute(bill = bill, tipAmount = tip, people = people, roundUpPerPerson = true)

        // 1100 / 3 = 366.67, so each person pays 367.
        assertEquals(ceil((bill + tip) / people), result.perPerson, 1e-9)
        assertEquals(367.0, result.perPerson, 1e-9)
        assertEquals(result.perPerson * people, result.total, 1e-9)
        assertTrue(result.total >= bill + tip)
        assertTrue(result.total - (bill + tip) < people)
        assertEquals(result.total - bill, result.tip, 1e-9)
        assertEquals(101.0, result.tip, 1e-9)
    }

    @Test
    fun `round up changes nothing when the share is already whole`() {
        val result = TipSplitCalculator.compute(bill = 1000.0, tipAmount = 200.0, people = 4, roundUpPerPerson = true)

        assertEquals(300.0, result.perPerson, 1e-9)
        assertEquals(1200.0, result.total, 1e-9)
        assertEquals(200.0, result.tip, 1e-9)
    }

    @Test
    fun `one person pays the whole bill and tip`() {
        val result = TipSplitCalculator.compute(bill = 845.5, tipAmount = 84.55, people = 1)

        assertEquals(930.05, result.perPerson, 1e-9)
        assertEquals(930.05, result.total, 1e-9)
    }

    @Test
    fun `zero tip splits only the bill`() {
        val result = TipSplitCalculator.compute(bill = 900.0, tipAmount = 0.0, people = 3)

        assertEquals(300.0, result.perPerson, 1e-9)
        assertEquals(900.0, result.total, 1e-9)
        assertEquals(0.0, result.tip, 1e-9)
    }
}
