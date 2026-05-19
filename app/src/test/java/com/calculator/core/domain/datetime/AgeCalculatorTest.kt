package com.calculator.core.domain.datetime

import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test
import java.time.DayOfWeek
import java.time.LocalDate

class AgeCalculatorTest {
    @Test
    fun `dob 1990-01-15 today 2026-05-18 gives 36y 4m 3d`() {
        val r = AgeCalculator.compute(
            dob = LocalDate.of(1990, 1, 15),
            today = LocalDate.of(2026, 5, 18),
        )
        assertEquals(36, r.years)
        assertEquals(4, r.months)
        assertEquals(3, r.days)
        // 1990-01-15 fell on a Monday.
        assertEquals(DayOfWeek.MONDAY, r.weekdayOfBirth)
    }

    @Test
    fun `next birthday countdown is in days`() {
        val r = AgeCalculator.compute(
            dob = LocalDate.of(1990, 12, 25),
            today = LocalDate.of(2026, 12, 20),
        )
        // 25 - 20 = 5 days to Christmas birthday.
        assertEquals(5, r.daysToNextBirthday)
    }

    @Test
    fun `today equals the birthday means zero days to next`() {
        val r = AgeCalculator.compute(
            dob = LocalDate.of(1990, 5, 18),
            today = LocalDate.of(2026, 5, 18),
        )
        assertEquals(0, r.daysToNextBirthday)
    }

    @Test
    fun `feb 29 birthday falls back to feb 28 in non-leap year`() {
        val r = AgeCalculator.compute(
            dob = LocalDate.of(2000, 2, 29),
            today = LocalDate.of(2026, 1, 1),
        )
        // 2026 is not a leap year. Next birthday should be 2026-02-28
        // (58 days after Jan 1).
        assertEquals(58, r.daysToNextBirthday)
    }
}
