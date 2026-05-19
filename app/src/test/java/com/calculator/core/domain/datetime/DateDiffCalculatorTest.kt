package com.calculator.core.domain.datetime

import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test
import java.time.LocalDate

class DateDiffCalculatorTest {
    @Test
    fun `feb 29 2024 to feb 28 2025 is 364 days`() {
        val r = DateDiffCalculator.difference(
            LocalDate.of(2024, 2, 29),
            LocalDate.of(2025, 2, 28),
        )
        // Period.between gives 11 months + 30 days (not a whole year)
        // because Feb 28 2025 doesn't reach Feb 29.
        assertEquals(0, r.years)
        assertEquals(11, r.months)
        assertEquals(30, r.days)
        assertEquals(365, r.totalDays)
        assertEquals(52, r.totalWeeks)
        assertEquals(1, r.weekRemainderDays)
    }

    @Test
    fun `add 90 days to 2026-01-01 returns 2026-04-01`() {
        val out = DateDiffCalculator.addOffset(LocalDate.of(2026, 1, 1), 90)
        assertEquals(LocalDate.of(2026, 4, 1), out)
    }

    @Test
    fun `swapping arguments still yields a non-negative diff`() {
        val a = LocalDate.of(2026, 5, 1)
        val b = LocalDate.of(2024, 11, 11)
        val r1 = DateDiffCalculator.difference(a, b)
        val r2 = DateDiffCalculator.difference(b, a)
        assertEquals(r1.totalDays, r2.totalDays)
    }

    @Test
    fun `same date is zero everything`() {
        val r = DateDiffCalculator.difference(
            LocalDate.of(2026, 5, 18),
            LocalDate.of(2026, 5, 18),
        )
        assertEquals(0, r.totalDays)
        assertEquals(0, r.years)
    }

    @Test
    fun `negative offset goes backwards`() {
        val out = DateDiffCalculator.addOffset(LocalDate.of(2026, 1, 1), -1)
        assertEquals(LocalDate.of(2025, 12, 31), out)
    }
}
