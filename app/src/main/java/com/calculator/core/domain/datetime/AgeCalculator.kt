package com.calculator.core.domain.datetime

import java.time.LocalDate
import java.time.Period
import java.time.temporal.ChronoUnit

/**
 * Pure calendar math around a date of birth.
 *
 * Returns the elapsed years/months/days, the user's next-birthday
 * countdown (in days, including the case where today *is* the
 * birthday), and the weekday of birth - all deterministic given
 * (dob, today), so the unit test can pass an explicit "today".
 */
object AgeCalculator {
    fun compute(dob: LocalDate, today: LocalDate): AgeResult {
        require(!dob.isAfter(today)) { "date of birth is in the future" }
        val period = Period.between(dob, today)
        val nextBirthday = nextBirthdayOnOrAfter(dob, today)
        return AgeResult(
            years = period.years,
            months = period.months,
            days = period.days,
            weekdayOfBirth = dob.dayOfWeek,
            daysToNextBirthday = ChronoUnit.DAYS.between(today, nextBirthday).toInt(),
        )
    }

    /**
     * Next anniversary of [dob] on or after [today]. Special-cases Feb
     * 29 birthdays in non-leap years by mapping to Feb 28 - matches
     * what most calendar apps do.
     */
    private fun nextBirthdayOnOrAfter(dob: LocalDate, today: LocalDate): LocalDate {
        val candidate =
            try {
                dob.withYear(today.year)
            } catch (_: java.time.DateTimeException) {
                // Feb 29 in a non-leap year -> fall back to Feb 28.
                dob.withYear(today.year).withDayOfMonth(28)
            }
        return if (!candidate.isBefore(today)) candidate else candidate.plusYears(1)
    }
}

data class AgeResult(
    val years: Int,
    val months: Int,
    val days: Int,
    val weekdayOfBirth: java.time.DayOfWeek,
    val daysToNextBirthday: Int,
)
