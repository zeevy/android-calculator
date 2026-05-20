package com.calculator.core.domain.health

import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertThrows
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Test
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.CsvSource
import java.time.LocalDate

/**
 * Golden catalogue for [OvulationCalculator].
 *
 * Predictions are simple arithmetic against the LMP, but the fertile-
 * window math and Naegele's rule for the due date are easy to fat-
 * finger. Each row pins one calendar arrangement that an obstetrics
 * reference would produce.
 */
class OvulationCalculatorGoldenTest {
    @DisplayName("next period = LMP + cycle length")
    @ParameterizedTest(name = "LMP={0} cycle={1} -> next period={2}")
    @CsvSource(
        "2025-01-01,    28,    2025-01-29",
        "2025-01-01,    21,    2025-01-22",
        "2025-01-01,    35,    2025-02-05",
        "2025-02-15,    28,    2025-03-15",
        // Across leap year boundary
        "2024-02-15,    28,    2024-03-14",
        "2024-02-29,    28,    2024-03-28",
        // Year boundary
        "2024-12-15,    28,    2025-01-12",
    )
    fun nextPeriod(lmp: String, cycle: Int, expected: String) {
        val r = OvulationCalculator.compute(LocalDate.parse(lmp), cycle)
        assertEquals(LocalDate.parse(expected), r.nextPeriod)
    }

    @DisplayName("ovulation = next period - 14 days")
    @ParameterizedTest(name = "LMP={0} cycle={1} -> ovulation={2}")
    @CsvSource(
        // 28-day cycle: ovulation is LMP + 14
        "2025-01-01,    28,    2025-01-15",
        // 21-day cycle: ovulation is LMP + 7
        "2025-01-01,    21,    2025-01-08",
        // 35-day cycle: ovulation is LMP + 21
        "2025-01-01,    35,    2025-01-22",
        // 26-day cycle: ovulation is LMP + 12
        "2025-01-01,    26,    2025-01-13",
    )
    fun ovulationDay(lmp: String, cycle: Int, expected: String) {
        val r = OvulationCalculator.compute(LocalDate.parse(lmp), cycle)
        assertEquals(LocalDate.parse(expected), r.ovulation)
    }

    @DisplayName("fertile window: [ovulation - 5, ovulation + 1]")
    @ParameterizedTest(name = "LMP={0} cycle={1} -> [{2}, {3}]")
    @CsvSource(
        "2025-01-01,    28,    2025-01-10,    2025-01-16",
        "2025-01-01,    21,    2025-01-03,    2025-01-09",
        "2025-01-01,    35,    2025-01-17,    2025-01-23",
    )
    fun fertileWindow(lmp: String, cycle: Int, start: String, end: String) {
        val r = OvulationCalculator.compute(LocalDate.parse(lmp), cycle)
        assertEquals(LocalDate.parse(start), r.fertileStart)
        assertEquals(LocalDate.parse(end), r.fertileEnd)
        // Window length is exactly 7 days (start..end inclusive)
        assertEquals(
            6,
            java.time.temporal.ChronoUnit.DAYS
                .between(r.fertileStart, r.fertileEnd),
        )
    }

    @DisplayName("due date = LMP + 280 days (Naegele's rule)")
    @ParameterizedTest(name = "LMP={0} -> due={1}")
    @CsvSource(
        "2025-01-01,    2025-10-08",
        "2025-05-20,    2026-02-24",
        "2024-02-29,    2024-12-05", // leap-year LMP
        "2024-12-31,    2025-10-07",
    )
    fun dueDate(lmp: String, expected: String) {
        val r = OvulationCalculator.compute(LocalDate.parse(lmp))
        assertEquals(LocalDate.parse(expected), r.estimatedDueDate)
    }

    @DisplayName("rejects cycle lengths outside the supported 21..35 range")
    @Test
    fun rejectsInvalidCycle() {
        val lmp = LocalDate.parse("2025-01-01")
        assertThrows(IllegalArgumentException::class.java) {
            OvulationCalculator.compute(lmp, cycleLengthDays = 20)
        }
        assertThrows(IllegalArgumentException::class.java) {
            OvulationCalculator.compute(lmp, cycleLengthDays = 36)
        }
        assertThrows(IllegalArgumentException::class.java) {
            OvulationCalculator.compute(lmp, cycleLengthDays = 0)
        }
        assertThrows(IllegalArgumentException::class.java) {
            OvulationCalculator.compute(lmp, cycleLengthDays = -5)
        }
    }

    @DisplayName("default cycle length is 28 days")
    @Test
    fun defaultCycleIs28Days() {
        val explicit28 = OvulationCalculator.compute(LocalDate.parse("2025-05-20"), 28)
        val defaultArg = OvulationCalculator.compute(LocalDate.parse("2025-05-20"))
        assertEquals(explicit28.nextPeriod, defaultArg.nextPeriod)
        assertEquals(explicit28.ovulation, defaultArg.ovulation)
        assertEquals(explicit28.estimatedDueDate, defaultArg.estimatedDueDate)
    }
}
