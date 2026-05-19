package com.calculator.core.data.db

import androidx.room.Entity
import androidx.room.PrimaryKey

/**
 * Cached fiat rate: how much of [code] you get for **one unit of**
 * [baseCode]. The whole table refreshes atomically when a new fetch
 * succeeds (see `RatesRepository.refresh`).
 *
 * @property code ISO 4217 currency code (e.g. "EUR", "INR").
 * @property rateVsBase Multiplier: `amountInCode = amountInBase * rateVsBase`.
 * @property baseCode The base currency the rate is expressed against.
 *   All rows in one cache refresh share the same base.
 * @property fetchedAtUtc When this row was inserted, epoch millis (UTC).
 *   Drives the stale-cache banner in the UI.
 */
@Entity(tableName = "currency_rate")
data class CurrencyRateEntity(
    @PrimaryKey val code: String,
    val rateVsBase: Double,
    val baseCode: String,
    val fetchedAtUtc: Long,
)
