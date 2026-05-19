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
    fun observeCached(): Flow<Rates?>

    fun observeFavorites(): Flow<List<String>>

    suspend fun refresh(base: String)

    suspend fun addFavorite(code: String)

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
                if (rows.isEmpty()) {
                    null
                } else {
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
            val nextPos = favouritesDao.maxPosition() + 1
            favouritesDao.upsert(FavoriteCurrencyEntity(code = code, position = nextPos))
        }

        override suspend fun removeFavorite(code: String) {
            favouritesDao.delete(code)
        }
    }
