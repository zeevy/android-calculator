package com.calculator.core.domain.health

import java.time.LocalDate

/**
 * Cycle estimator built around the **last menstrual period** (LMP)
 * date and an average **cycle length** (default 28 days, accepted
 * range 21..35 - anything outside that range varies too much from
 * person to person for a generic estimator to be useful).
 *
 * Outputs (all estimates, not medical advice):
 *  - Next period: `LMP + cycle length`.
 *  - Upcoming periods: the next [UPCOMING_PERIOD_COUNT] period start
 *    dates, each one cycle length apart, so the user sees roughly the
 *    next three months at a glance.
 *  - Predicted ovulation: `next period − 14` (the luteal-phase length
 *    is the more stable end of the cycle).
 *  - Fertile window: 6-day span ending the day after ovulation
 *    (`[ovulation − 5, ovulation + 1]`).
 *  - Estimated due date if conception this cycle: `LMP + 280 days`
 *    (Naegele's rule).
 *
 * **Explicitly an estimator.** UI must show a disclaimer that the
 * dates are statistical, not a diagnosis, and that the app is not a
 * contraception tool.
 */
object OvulationCalculator {
    const val MIN_CYCLE_DAYS = 21
    const val MAX_CYCLE_DAYS = 35
    const val DEFAULT_CYCLE_DAYS = 28

    /** How many upcoming period start dates [compute] projects. */
    const val UPCOMING_PERIOD_COUNT = 3
    private const val LUTEAL_PHASE_DAYS = 14
    private const val FERTILE_WINDOW_BEFORE_OVULATION = 5L
    private const val FERTILE_WINDOW_AFTER_OVULATION = 1L
    private const val GESTATION_DAYS = 280L

    /**
     * @throws IllegalArgumentException if [cycleLengthDays] is outside
     *   the [MIN_CYCLE_DAYS] .. [MAX_CYCLE_DAYS] range.
     */
    fun compute(lmp: LocalDate, cycleLengthDays: Int = DEFAULT_CYCLE_DAYS): OvulationResult {
        require(cycleLengthDays in MIN_CYCLE_DAYS..MAX_CYCLE_DAYS) {
            "cycle length must be in $MIN_CYCLE_DAYS..$MAX_CYCLE_DAYS days"
        }
        val nextPeriod = lmp.plusDays(cycleLengthDays.toLong())
        val ovulation = nextPeriod.minusDays(LUTEAL_PHASE_DAYS.toLong())
        val fertileStart = ovulation.minusDays(FERTILE_WINDOW_BEFORE_OVULATION)
        val fertileEnd = ovulation.plusDays(FERTILE_WINDOW_AFTER_OVULATION)
        val dueDate = lmp.plusDays(GESTATION_DAYS)
        // Each subsequent period is one cycle length after the previous;
        // the first entry is always [nextPeriod].
        val upcomingPeriods = (0 until UPCOMING_PERIOD_COUNT).map { i ->
            nextPeriod.plusDays(cycleLengthDays.toLong() * i)
        }
        return OvulationResult(
            ovulation = ovulation,
            fertileStart = fertileStart,
            fertileEnd = fertileEnd,
            nextPeriod = nextPeriod,
            upcomingPeriods = upcomingPeriods,
            estimatedDueDate = dueDate,
        )
    }
}

data class OvulationResult(
    val ovulation: LocalDate,
    val fertileStart: LocalDate,
    val fertileEnd: LocalDate,
    val nextPeriod: LocalDate,
    /**
     * The next [OvulationCalculator.UPCOMING_PERIOD_COUNT] period start
     * dates, one cycle apart. The first element equals [nextPeriod].
     */
    val upcomingPeriods: List<LocalDate>,
    val estimatedDueDate: LocalDate,
)
