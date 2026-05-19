package com.calculator.core.domain.datetime

import java.time.LocalDate
import java.time.Period
import java.time.temporal.ChronoUnit

/**
 * Two operations over `LocalDate`s:
 *  - [difference]: how far apart two dates are, broken down into
 *    years/months/days *and* into raw days/weeks.
 *  - [addOffset]: starting date plus a signed number of days. Used by
 *    the UI's "what's the date after N days?" mode.
 *
 * Order-insensitive: passing the smaller date as the second argument
 * still yields a non-negative result.
 */
object DateDiffCalculator {
    fun difference(a: LocalDate, b: LocalDate): DateDiffResult {
        val (start, end) = if (a.isAfter(b)) b to a else a to b
        val period = Period.between(start, end)
        val days = ChronoUnit.DAYS.between(start, end)
        return DateDiffResult(
            years = period.years,
            months = period.months,
            days = period.days,
            totalDays = days.toInt(),
            totalWeeks = (days / 7).toInt(),
            weekRemainderDays = (days % 7).toInt(),
        )
    }

    fun addOffset(date: LocalDate, days: Int): LocalDate = date.plusDays(days.toLong())
}

data class DateDiffResult(
    val years: Int,
    val months: Int,
    val days: Int,
    val totalDays: Int,
    val totalWeeks: Int,
    val weekRemainderDays: Int,
)
