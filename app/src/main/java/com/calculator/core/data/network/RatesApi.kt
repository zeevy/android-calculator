package com.calculator.core.data.network

import retrofit2.http.GET
import retrofit2.http.Path

/**
 * Retrofit interface for fetching live exchange rates.
 *
 * Uses **open.er-api.com**, a free, no-key, no-attribution-required
 * service that publishes daily fiat-currency rates. The choice of API
 * is local to this interface; swapping providers later only touches
 * this file plus the Hilt module that supplies the base URL.
 */
interface RatesApi {
    /**
     * Latest rates with [base] as the reference currency.
     * Base should be a 3-letter ISO 4217 code (e.g. "USD", "EUR", "INR").
     */
    @GET("v6/latest/{base}")
    suspend fun latest(@Path("base") base: String): RatesDto

    companion object {
        const val BASE_URL = "https://open.er-api.com/"
    }
}
