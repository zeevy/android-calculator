package com.calculator.feature.finance.salary

import com.calculator.R
import com.calculator.core.common.input.InputResult
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test
import java.util.Locale

/** Salary fields as typed, to a result or a per-field error. */
class SalaryInputTest {
    private fun newSalary(
        salary: String = "600000",
        hike: String = "10",
        yearly: Boolean = true,
    ) = newSalaryInput(salary, hike, yearly, Locale.US)

    private fun hike(
        old: String = "600000",
        new: String = "660000",
    ) = hikeInput(old, new, Locale.US)

    private fun error(result: InputResult<*>) = result as InputResult.Invalid

    @Test
    fun `valid new salary fields give the new salary`() {
        val ok = newSalary() as InputResult.Ok
        assertEquals(660_000.0, ok.value.newYearly, 1e-6)
        assertEquals(55_000.0, ok.value.newMonthly, 1e-6)
    }

    @Test
    fun `valid hike fields give the hike`() {
        val ok = hike() as InputResult.Ok
        assertEquals(60_000.0, ok.value.increase, 1e-9)
        assertEquals(10.0, ok.value.hikePercent, 1e-9)
    }

    @Test
    fun `each new salary field error names that field`() {
        val salary = error(newSalary(salary = ""))
        assertEquals(R.string.salary_current, salary.field)
        assertEquals(R.string.input_error_required, salary.message)
        val percent = error(newSalary(hike = "-5"))
        assertEquals(R.string.salary_hike_input, percent.field)
        assertEquals(R.string.input_error_negative, percent.message)
    }

    @Test
    fun `each hike field error names that field`() {
        assertEquals(R.string.salary_old, error(hike(old = "abc")).field)
        val new = error(hike(new = "-1"))
        assertEquals(R.string.salary_new, new.field)
        assertEquals(R.string.input_error_negative, new.message)
    }

    @Test
    fun `an old salary of 0 is rejected`() {
        val old = error(hike(old = "0"))
        assertEquals(R.string.salary_old, old.field)
        assertEquals(R.string.input_error_positive, old.message)
    }

    @Test
    fun `a pay cut gives a negative hike`() {
        val ok = hike(new = "540000") as InputResult.Ok
        assertEquals(-60_000.0, ok.value.increase, 1e-9)
        assertEquals(-10.0, ok.value.hikePercent, 1e-9)
    }

    @Test
    fun `a result that overflows says it is too large`() {
        // 2e307 a month, times 1.1 and times 12, is above Double.MAX_VALUE.
        val error = error(newSalary(salary = "2" + "0".repeat(307), yearly = false))
        assertEquals(R.string.salary_current, error.field)
        assertEquals(R.string.input_error_too_large, error.message)
    }
}
