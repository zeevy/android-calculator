package com.calculator.core.data.rates

import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import app.cash.turbine.test
import com.calculator.core.data.db.CalculatorDatabase
import com.calculator.core.data.network.RatesApi
import com.calculator.core.data.network.RatesDto
import kotlinx.coroutines.test.runTest
import org.junit.After
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import kotlin.test.assertEquals
import kotlin.test.assertNull
import kotlin.test.fail

/**
 * Exercises the repository's read/write contract against a real
 * in-memory Room database and a swappable fake [RatesApi].
 */
@RunWith(RobolectricTestRunner::class)
@Config(sdk = [34])
class RatesRepositoryTest {
    private lateinit var db: CalculatorDatabase
    private lateinit var api: FakeRatesApi
    private lateinit var repo: RatesRepository

    @Before
    fun setUp() {
        db =
            Room
                .inMemoryDatabaseBuilder(
                    ApplicationProvider.getApplicationContext(),
                    CalculatorDatabase::class.java,
                ).allowMainThreadQueries()
                .build()
        api = FakeRatesApi()
        repo =
            DefaultRatesRepository(
                api = api,
                ratesDao = db.currencyRateDao(),
                favouritesDao = db.favoriteCurrencyDao(),
            )
    }

    @After
    fun tearDown() {
        db.close()
    }

    @Test
    fun refreshWritesTheCache() =
        runTest {
            api.next =
                RatesDto(
                    result = "success",
                    baseCode = "USD",
                    rates = mapOf("USD" to 1.0, "EUR" to 0.92, "INR" to 83.2),
                    timeLastUpdateUnix = 1_700_000_000L,
                )

            repo.observeCached().test {
                assertNull(awaitItem())
                repo.refresh("USD")
                val rates = awaitItem()!!
                assertEquals("USD", rates.base)
                assertEquals(0.92, rates.rates["EUR"])
                assertEquals(83.2, rates.rates["INR"])
                cancelAndIgnoreRemainingEvents()
            }
        }

    @Test
    fun refreshFailureLeavesCacheIntact() =
        runTest {
            // Seed a cache, then make the next API call throw.
            api.next =
                RatesDto(
                    result = "success",
                    baseCode = "USD",
                    rates = mapOf("USD" to 1.0, "EUR" to 0.9),
                    timeLastUpdateUnix = 1L,
                )
            repo.refresh("USD")
            api.throwOnNext = true

            try {
                repo.refresh("USD")
                fail("expected an exception when the API throws")
            } catch (_: Exception) {
                // expected
            }
            // Cache should still hold the previous good snapshot.
            repo.observeCached().test {
                val rates = awaitItem()!!
                assertEquals(0.9, rates.rates["EUR"])
                cancelAndIgnoreRemainingEvents()
            }
        }

    @Test
    fun nonSuccessResultIsTreatedAsFailure() =
        runTest {
            api.next =
                RatesDto(
                    result = "error",
                    baseCode = "USD",
                    rates = emptyMap(),
                    timeLastUpdateUnix = 0L,
                )
            try {
                repo.refresh("USD")
                fail("expected non-success result to throw")
            } catch (_: IllegalStateException) {
                // expected
            }
        }

    @Test
    fun favouritesPersistAndAppendAtEnd() =
        runTest {
            repo.observeFavorites().test {
                assertEquals(emptyList(), awaitItem())
                repo.addFavorite("EUR")
                assertEquals(listOf("EUR"), awaitItem())
                repo.addFavorite("INR")
                assertEquals(listOf("EUR", "INR"), awaitItem())
                repo.removeFavorite("EUR")
                assertEquals(listOf("INR"), awaitItem())
                cancelAndIgnoreRemainingEvents()
            }
        }

    private class FakeRatesApi : RatesApi {
        var next: RatesDto? = null
        var throwOnNext: Boolean = false

        override suspend fun latest(base: String): RatesDto {
            if (throwOnNext) {
                throwOnNext = false
                throw java.io.IOException("simulated network failure")
            }
            return next ?: error("no response staged on FakeRatesApi")
        }
    }
}
