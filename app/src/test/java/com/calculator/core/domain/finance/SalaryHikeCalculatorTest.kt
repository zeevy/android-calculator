package com.calculator.core.domain.finance

import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertThrows

/** Known answers for the salary hike math in [SalaryHikeCalculator]. */
class SalaryHikeCalculatorTest {
    @Test
    fun `600000 a year with a 10 percent hike`() {
        val result = SalaryHikeCalculator.withHike(salary = 600_000.0, hikePercent = 10.0, yearly = true)

        assertEquals(660_000.0, result.newYearly, 1e-6)
        assertEquals(55_000.0, result.newMonthly, 1e-6)
        assertEquals(60_000.0, result.increaseYearly, 1e-6)
        assertEquals(5_000.0, result.increaseMonthly, 1e-6)
    }

    @Test
    fun `50000 a month with a 10 percent hike gives the same answer`() {
        val result = SalaryHikeCalculator.withHike(salary = 50_000.0, hikePercent = 10.0, yearly = false)

        assertEquals(55_000.0, result.newMonthly, 1e-6)
        assertEquals(660_000.0, result.newYearly, 1e-6)
        assertEquals(5_000.0, result.increaseMonthly, 1e-6)
        assertEquals(60_000.0, result.increaseYearly, 1e-6)
    }

    @Test
    fun `a hike of 0 gives no increase`() {
        val result = SalaryHikeCalculator.withHike(salary = 600_000.0, hikePercent = 0.0, yearly = true)

        assertEquals(600_000.0, result.newYearly, 1e-9)
        assertEquals(0.0, result.increaseYearly, 1e-9)
        assertEquals(0.0, result.increaseMonthly, 1e-9)
    }

    @Test
    fun `a negative hike or salary throws`() {
        assertThrows<IllegalArgumentException> { SalaryHikeCalculator.withHike(600_000.0, -1.0, yearly = true) }
        assertThrows<IllegalArgumentException> { SalaryHikeCalculator.withHike(-1.0, 10.0, yearly = true) }
    }

    @Test
    fun `hike between two salaries`() {
        val raise = SalaryHikeCalculator.hike(oldSalary = 600_000.0, newSalary = 660_000.0)
        assertEquals(60_000.0, raise.increase, 1e-9)
        assertEquals(10.0, raise.hikePercent, 1e-9)

        assertEquals(-100.0, SalaryHikeCalculator.hike(oldSalary = 600_000.0, newSalary = 0.0).hikePercent, 1e-9)
        assertThrows<IllegalArgumentException> { SalaryHikeCalculator.hike(0.0, 100.0) }
    }
}
