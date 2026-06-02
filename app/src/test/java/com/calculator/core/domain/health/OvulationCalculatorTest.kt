package com.calculator.core.domain.health

import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertThrows
import org.junit.jupiter.api.Test
import java.time.LocalDate

class OvulationCalculatorTest {
    @Test
    fun `default 28-day cycle from 2026-05-01 matches the plan example`() {
        val r = OvulationCalculator.compute(
            lmp = LocalDate.of(2026, 5, 1),
            cycleLengthDays = 28,
        )
        // 2026-05-01 + 28 = 2026-05-29 (next period)
        // 2026-05-29 - 14 = 2026-05-15 (ovulation)
        // fertile: ovulation - 5 .. ovulation + 1 = 2026-05-10 .. 2026-05-16
        // due:    2026-05-01 + 280 = 2027-02-05
        assertEquals(LocalDate.of(2026, 5, 29), r.nextPeriod)
        assertEquals(LocalDate.of(2026, 5, 15), r.ovulation)
        assertEquals(LocalDate.of(2026, 5, 10), r.fertileStart)
        assertEquals(LocalDate.of(2026, 5, 16), r.fertileEnd)
        assertEquals(LocalDate.of(2027, 2, 5), r.estimatedDueDate)
    }

    @Test
    fun `projects the next three period dates one cycle apart`() {
        val r = OvulationCalculator.compute(
            lmp = LocalDate.of(2026, 5, 1),
            cycleLengthDays = 28,
        )
        // First entry is the next period; each subsequent one is +28 days.
        assertEquals(
            listOf(
                LocalDate.of(2026, 5, 29),
                LocalDate.of(2026, 6, 26),
                LocalDate.of(2026, 7, 24),
            ),
            r.upcomingPeriods,
        )
        // The list always leads with nextPeriod.
        assertEquals(r.nextPeriod, r.upcomingPeriods.first())
    }

    @Test
    fun `cycle length 35 shifts ovulation by seven days from default`() {
        val r28 = OvulationCalculator.compute(LocalDate.of(2026, 5, 1), 28)
        val r35 = OvulationCalculator.compute(LocalDate.of(2026, 5, 1), 35)
        // 35-day cycle: nextPeriod = LMP + 35 -> ovulation = LMP + 21.
        // That's 7 days later than the 28-day case.
        assertEquals(r28.ovulation.plusDays(7), r35.ovulation)
        assertEquals(r28.fertileStart.plusDays(7), r35.fertileStart)
    }

    @Test
    fun `cycle below the supported range is rejected`() {
        assertThrows(IllegalArgumentException::class.java) {
            OvulationCalculator.compute(LocalDate.of(2026, 5, 1), 20)
        }
    }

    @Test
    fun `cycle above the supported range is rejected`() {
        assertThrows(IllegalArgumentException::class.java) {
            OvulationCalculator.compute(LocalDate.of(2026, 5, 1), 36)
        }
    }
}
