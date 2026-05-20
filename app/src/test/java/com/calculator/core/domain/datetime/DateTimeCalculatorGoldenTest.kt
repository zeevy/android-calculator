package com.calculator.core.domain.datetime

import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertThrows
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Test
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.CsvSource
import java.time.LocalDate

/**
 * Golden catalogue for [DateDiffCalculator] and [AgeCalculator].
 *
 * Date math is gotcha-rich (DST shifts the actual elapsed *seconds*
 * but not the *days* count; leap years invalidate "Feb 30"; period
 * arithmetic is non-commutative in years/months even though `days
 * between` is). The catalogue is dense around those gotchas: leap-day
 * birthdays, exact-day-of-month anniversaries, ranges spanning Feb 29.
 */
class DateTimeCalculatorGoldenTest {
    @DisplayName("date difference: years / months / days")
    @ParameterizedTest(name = "{0} .. {1} -> {2}y {3}m {4}d")
    @CsvSource(
        // Exact whole years
        "2020-01-01,    2025-01-01,    5,   0,   0",
        "2020-06-15,    2025-06-15,    5,   0,   0",
        // Month boundary
        "2025-01-01,    2025-02-01,    0,   1,   0",
        "2025-01-31,    2025-02-28,    0,   0,   28",
        // Crossing year boundary
        "2024-12-15,    2025-01-15,    0,   1,   0",
        "2024-12-31,    2025-01-01,    0,   0,   1",
        // Leap-year span (2024 is leap, Feb 29 exists)
        "2024-02-29,    2025-02-28,    0,   11,  30",
        "2024-02-29,    2025-03-01,    1,   0,   1",
        "2024-01-01,    2025-01-01,    1,   0,   0",
        // Across many years
        "2000-01-01,    2025-01-01,    25,  0,   0",
        "1990-06-30,    2025-05-20,    34,  10,  20",
        // Same date
        "2025-05-20,    2025-05-20,    0,   0,   0",
        // Order-insensitive: smaller date second still yields non-negative diff
        "2025-01-01,    2020-01-01,    5,   0,   0",
    )
    fun dateDifferenceYearsMonthsDays(
        start: String,
        end: String,
        expectedYears: Int,
        expectedMonths: Int,
        expectedDays: Int,
    ) {
        val r = DateDiffCalculator.difference(LocalDate.parse(start), LocalDate.parse(end))
        assertEquals(expectedYears, r.years, "years")
        assertEquals(expectedMonths, r.months, "months")
        assertEquals(expectedDays, r.days, "days")
    }

    @DisplayName("date difference: totalDays")
    @ParameterizedTest(name = "{0} .. {1} -> {2} days")
    @CsvSource(
        "2020-01-01,    2020-01-02,    1",
        "2020-01-01,    2020-02-01,    31",
        "2020-01-01,    2021-01-01,    366", // 2020 is a leap year
        "2021-01-01,    2022-01-01,    365",
        "2024-02-28,    2024-03-01,    2", // Feb 29 exists in 2024
        "2025-02-28,    2025-03-01,    1", // Feb 29 does not exist in 2025
        "2025-05-20,    2025-05-20,    0",
    )
    fun dateDifferenceTotalDays(start: String, end: String, expectedDays: Int) {
        val r = DateDiffCalculator.difference(LocalDate.parse(start), LocalDate.parse(end))
        assertEquals(expectedDays, r.totalDays)
    }

    @DisplayName("totalWeeks + weekRemainderDays = totalDays")
    @ParameterizedTest(name = "{0} .. {1}")
    @CsvSource(
        "2025-01-01,    2025-01-08",
        "2025-01-01,    2025-01-15",
        "2025-01-01,    2025-02-01",
        "2024-01-01,    2025-01-01",
    )
    fun weeksDecomposition(start: String, end: String) {
        val r = DateDiffCalculator.difference(LocalDate.parse(start), LocalDate.parse(end))
        assertEquals(r.totalDays, r.totalWeeks * 7 + r.weekRemainderDays)
    }

    @DisplayName("addOffset: date + N days")
    @ParameterizedTest(name = "{0} + {1}d -> {2}")
    @CsvSource(
        "2025-01-01,    1,      2025-01-02",
        "2025-01-01,    31,     2025-02-01",
        "2025-01-01,    365,    2026-01-01",
        "2024-02-28,    1,      2024-02-29", // leap year
        "2024-02-28,    2,      2024-03-01",
        "2025-02-28,    1,      2025-03-01", // non-leap year
        "2025-12-31,    1,      2026-01-01",
        // Negative offsets (subtract)
        "2025-01-01,    -1,     2024-12-31",
        "2025-05-20,    -30,    2025-04-20",
        // Zero offset
        "2025-05-20,    0,      2025-05-20",
    )
    fun addOffset(base: String, days: Int, expected: String) {
        val r = DateDiffCalculator.addOffset(LocalDate.parse(base), days)
        assertEquals(LocalDate.parse(expected), r)
    }

    // ------------------------------------------------------------------
    // AgeCalculator
    // ------------------------------------------------------------------

    @DisplayName("age in years / months / days")
    @ParameterizedTest(name = "dob={0} today={1} -> {2}y {3}m {4}d")
    @CsvSource(
        // Exact whole years
        "1990-06-15,    2025-06-15,    35,  0,   0",
        "2000-01-01,    2025-01-01,    25,  0,   0",
        // The day before a birthday
        "1990-06-15,    2025-06-14,    34,  11,  30",
        // Just turned today
        "1990-06-15,    2025-06-15,    35,  0,   0",
        // Several months past birthday
        "1990-06-15,    2025-12-15,    35,  6,   0",
        // Newborn
        "2025-05-19,    2025-05-20,    0,   0,   1",
        "2025-05-20,    2025-05-20,    0,   0,   0",
        // Toddler
        "2023-05-20,    2025-05-20,    2,   0,   0",
        // Leap-year birthday in a non-leap "today" year
        "2000-02-29,    2025-02-28,    24,  11,  30",
        "2000-02-29,    2025-03-01,    25,  0,   1",
    )
    fun ageYearsMonthsDays(
        dob: String,
        today: String,
        expectedYears: Int,
        expectedMonths: Int,
        expectedDays: Int,
    ) {
        val r = AgeCalculator.compute(LocalDate.parse(dob), LocalDate.parse(today))
        assertEquals(expectedYears, r.years, "years")
        assertEquals(expectedMonths, r.months, "months")
        assertEquals(expectedDays, r.days, "days")
    }

    @DisplayName("days to next birthday")
    @ParameterizedTest(name = "dob={0} today={1} -> {2} days")
    @CsvSource(
        // Birthday today
        "1990-05-20,    2025-05-20,    0",
        // Day after birthday
        "1990-05-19,    2025-05-20,    364",
        // Day before birthday
        "1990-05-21,    2025-05-20,    1",
        // Several months out
        "1990-12-31,    2025-05-20,    225",
        // Leap-year birthday: in non-leap year falls back to Feb 28
        "2000-02-29,    2025-01-01,    58",
    )
    fun daysToNextBirthday(dob: String, today: String, expectedDays: Int) {
        val r = AgeCalculator.compute(LocalDate.parse(dob), LocalDate.parse(today))
        assertEquals(expectedDays, r.daysToNextBirthday)
    }

    @DisplayName("future date-of-birth is rejected")
    @Test
    fun futureDobRejected() {
        assertThrows(IllegalArgumentException::class.java) {
            AgeCalculator.compute(
                dob = LocalDate.parse("2026-01-01"),
                today = LocalDate.parse("2025-05-20"),
            )
        }
    }
}
