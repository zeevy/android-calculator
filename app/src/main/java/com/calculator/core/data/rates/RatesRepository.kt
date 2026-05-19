package com.calculator.core.data.rates

import com.calculator.core.data.db.CurrencyRateDao
import com.calculator.core.data.db.CurrencyRateEntity
import com.calculator.core.data.db.FavoriteCurrencyDao
import com.calculator.core.data.db.FavoriteCurrencyEntity
import com.calculator.core.data.network.RatesApi
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Reads/writes currency rates and favourites.
 *
 * Two read paths:
 *  - [observeCached]: a Flow over the SQLite cache. The UI subscribes
 *    so it gets the current value on launch and live updates whenever
 *    [refresh] writes new data.
 *  - [refresh]: a one-shot suspend call that hits the network, on
 *    success writes the cache, on failure surfaces the exception so
 *    the caller can decide whether to show an error banner or just
 *    keep showing the cached rows.
 *
 * Conversion math lives in [Rates.convert] - tiny helper, kept in the
 * domain layer for testability.
 */
interface RatesRepository {
    /**
     * Emits the currently-cached rate table (or null if the cache is
     * empty). The Flow stays active for the lifetime of the subscriber
     * and re-emits when [refresh] writes new data or when [addFavorite]
     * / [removeFavorite] changes the favourites table - the UI does not
     * need to poll.
     */
    fun observeCached(): Flow<Rates?>

    /** Emits the favourite codes in stored order. */
    fun observeFavorites(): Flow<List<String>>

    /**
     * Hit the network and replace the cache with the new payload.
     *
     * @param base ISO 4217 base currency code (e.g. "USD", "INR"). All
     *   returned rates are expressed relative to this base; the cache
     *   stores it so the UI knows which currency to treat as 1.0.
     * @throws Exception when the network call fails or the response
     *   `result` field is anything other than "success". The caller
     *   typically reports this as a transient error while leaving the
     *   stale cache in place.
     */
    suspend fun refresh(base: String)

    /**
     * Pin [code] as a favourite. New favourites are appended; existing
     * favourites keep their stored position.
     */
    suspend fun addFavorite(code: String)

    /** Unpin [code]; no-op if it wasn't a favourite. */
    suspend fun removeFavorite(code: String)
}

@Singleton
class DefaultRatesRepository
    @Inject
    constructor(
        private val api: RatesApi,
        private val ratesDao: CurrencyRateDao,
        private val favouritesDao: FavoriteCurrencyDao,
    ) : RatesRepository {
        override fun observeCached(): Flow<Rates?> =
            ratesDao.observeAll().map { rows ->
                // Empty cache → expose null so callers can render an
                // "offline / never refreshed" placeholder instead of a
                // misleading empty Rates with base "" and no entries.
                if (rows.isEmpty()) {
                    null
                } else {
                    // `replaceAll` writes a single batch so every row shares
                    // baseCode and fetchedAtUtc; reading them from row 0 is
                    // safe and avoids scanning the list.
                    Rates(
                        base = rows.first().baseCode,
                        rates = rows.associate { it.code to it.rateVsBase },
                        fetchedAtUtc = rows.first().fetchedAtUtc,
                    )
                }
            }

        override fun observeFavorites(): Flow<List<String>> =
            favouritesDao.observeAll().map { rows -> rows.map { it.code } }

        override suspend fun refresh(base: String) {
            val response = api.latest(base)
            // Provider uses HTTP 200 with a `result` field. Treat
            // anything other than "success" as a failure so the caller
            // sees an exception rather than overwriting the cache with
            // a partial payload.
            check(response.result == "success") { "rates api returned ${response.result}" }
            val now = System.currentTimeMillis()
            val rows =
                response.rates.map { (code, rate) ->
                    CurrencyRateEntity(
                        code = code,
                        rateVsBase = rate,
                        baseCode = response.baseCode,
                        fetchedAtUtc = now,
                    )
                }
            ratesDao.replaceAll(rows)
        }

        override suspend fun addFavorite(code: String) {
            // Append after every existing favourite. We don't compact
            // positions on remove (so the sequence may have gaps), but
            // `maxPosition()` returns the largest stored value plus we
            // add 1, which preserves the relative ordering regardless
            // of gaps. Cheaper than re-numbering on every delete.
            val nextPos = favouritesDao.maxPosition() + 1
            favouritesDao.upsert(FavoriteCurrencyEntity(code = code, position = nextPos))
        }

        override suspend fun removeFavorite(code: String) {
            favouritesDao.delete(code)
        }
    }
