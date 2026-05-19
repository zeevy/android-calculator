package com.calculator.core.data.network

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

/**
 * Wire-format DTO for `https://open.er-api.com/v6/latest/{base}`.
 *
 * Example response:
 * ```
 * {
 *   "result": "success",
 *   "base_code": "USD",
 *   "rates": { "USD": 1, "EUR": 0.92, "INR": 83.2 },
 *   "time_last_update_unix": 1700000000
 * }
 * ```
 *
 * Fields not used by the app are not declared - kotlinx.serialization
 * by default ignores unknown JSON keys via [Json.ignoreUnknownKeys].
 */
@Serializable
data class RatesDto(
    @SerialName("result") val result: String,
    @SerialName("base_code") val baseCode: String,
    @SerialName("rates") val rates: Map<String, Double>,
    @SerialName("time_last_update_unix") val timeLastUpdateUnix: Long,
)
