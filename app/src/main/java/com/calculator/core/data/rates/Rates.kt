package com.calculator.core.data.rates

/**
 * Domain snapshot of fiat exchange rates.
 *
 * Rates are expressed against [base] - i.e. `rates[code]` is the
 * multiplier from [base] to [code]:
 *
 *     amount_in_code = amount_in_base * rates[code]
 *
 * @property base The base ISO 4217 code (e.g. "USD").
 * @property rates Map of code → rate-vs-base; includes the base itself
 *   with value 1.0 for convenience at call sites.
 * @property fetchedAtUtc Epoch milliseconds at which these rates were
 *   fetched. Drives the "last updated" timestamp and the stale-cache
 *   banner.
 */
data class Rates(
    val base: String,
    val rates: Map<String, Double>,
    val fetchedAtUtc: Long,
)
