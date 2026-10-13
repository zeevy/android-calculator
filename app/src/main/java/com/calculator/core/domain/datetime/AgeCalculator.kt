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
    /**
     * Age on [today] of someone born on [dob].
     *
     * @throws IllegalArgumentException when [dob] is after [today].
     */
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
     * Next anniversary of [dob] on or after [today]. A Feb 29 birthday
     * falls on Feb 28 in years that are not leap years (as most calendar
     * apps do), and on Feb 29 again in leap years.
     */
    private fun nextBirthdayOnOrAfter(
        dob: LocalDate,
        today: LocalDate,
    ): LocalDate {
        // Pick the year first; withYear() then moves Feb 29 to Feb 28 only
        // when that year has no Feb 29.
        val thisYear = dob.withYear(today.year)
        return if (!thisYear.isBefore(today)) thisYear else dob.withYear(today.year + 1)
    }
}

/**
 * Result of [AgeCalculator.compute].
 *
 * @property years Whole years since birth.
 * @property months Months after the whole years.
 * @property days Days after the whole months.
 * @property weekdayOfBirth Day of the week the person was born on.
 * @property daysToNextBirthday Days until the next birthday; 0 on the birthday.
 */
data class AgeResult(
    val years: Int,
    val months: Int,
    val days: Int,
    val weekdayOfBirth: java.time.DayOfWeek,
    val daysToNextBirthday: Int,
)
