package com.calculator.core.domain.datetime

import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test
import java.time.DayOfWeek
import java.time.LocalDate
import java.time.temporal.ChronoUnit

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

    @Test
    fun `a Feb 29 birthday comes back on Feb 29 in the next leap year`() {
        val dob = LocalDate.of(2000, 2, 29)
        // 2027-03-10: this year's birthday (Feb 28) has passed; 2028 is a leap year.
        val result = AgeCalculator.compute(dob, LocalDate.of(2027, 3, 10))
        assertEquals(ChronoUnit.DAYS.between(LocalDate.of(2027, 3, 10), LocalDate.of(2028, 2, 29)).toInt(), result.daysToNextBirthday)
    }

    @Test
    fun `a Feb 29 birthday is on Feb 28 in a year that is not a leap year`() {
        val result = AgeCalculator.compute(LocalDate.of(2000, 2, 29), LocalDate.of(2027, 2, 28))
        assertEquals(0, result.daysToNextBirthday)
    }
}
