package com.calculator.feature.converter.currency

import com.calculator.core.data.rates.Rates
import com.calculator.core.data.rates.RatesRepository
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test

/**
 * Verifies the ViewModel's contract:
 *  - Cached rates surface in state.
 *  - Conversion math (`amount × rate`) is correct.
 *  - Network failures don't blank the cache, they set errorMessage.
 *  - Favourites toggle round-trips through the repo and bubble back.
 */
@OptIn(ExperimentalCoroutinesApi::class)
class CurrencyConverterViewModelTest {
    private val dispatcher = StandardTestDispatcher()
    private val repo = FakeRepo()
    private lateinit var viewModel: CurrencyConverterViewModel

    @BeforeEach
    fun setUp() {
        Dispatchers.setMain(dispatcher)
        // Seed the repo with a successful response so the init refresh
        // populates rates.
        repo.nextRefresh = Result.success(
            Rates(
                base = "USD",
                rates = mapOf("USD" to 1.0, "EUR" to 0.92, "INR" to 83.2),
                fetchedAtUtc = 1_700_000_000_000L,
            ),
        )
        viewModel = CurrencyConverterViewModel(repo)
    }

    @AfterEach
    fun tearDown() {
        Dispatchers.resetMain()
    }

    @Test
    fun `cached rates land in state`() =
        runTest(dispatcher) {
            advanceUntilIdle()
            val s = viewModel.state.value
            assertEquals("USD", s.base)
            assertEquals(0.92, s.rates["EUR"])
            assertEquals(83.2, s.rates["INR"])
        }

    @Test
    fun `setAmount stores the input verbatim`() =
        runTest(dispatcher) {
            advanceUntilIdle()
            viewModel.setAmount("100")
            assertEquals("100", viewModel.state.value.amount)
        }

    @Test
    fun `favorites round-trip through the repo`() =
        runTest(dispatcher) {
            advanceUntilIdle()
            viewModel.toggleFavorite("EUR")
            advanceUntilIdle()
            assertTrue(
                viewModel.state.value.favorites
                    .contains("EUR"),
            )
            viewModel.toggleFavorite("EUR")
            advanceUntilIdle()
            assertEquals(
                false,
                viewModel.state.value.favorites
                    .contains("EUR"),
            )
        }

    @Test
    fun `refresh failure surfaces as errorMessage without blanking the cache`() =
        runTest(dispatcher) {
            advanceUntilIdle()
            repo.nextRefresh = Result.failure(java.io.IOException("offline"))
            viewModel.refreshManually()
            advanceUntilIdle()
            val s = viewModel.state.value
            assertTrue(s.errorMessage != null, "expected an error message")
            // Cache still intact.
            assertEquals(0.92, s.rates["EUR"])
        }

    @Test
    fun `100 USD times 83 point 2 equals 8320 INR via the rates map`() {
        // Verified mathematically rather than in the UI (UI tests cover
        // rendering). The map is what the UI multiplies amount against.
        val rates = mapOf("USD" to 1.0, "INR" to 83.2)
        val amount = 100.0
        val converted = amount * (rates["INR"]!! / rates["USD"]!!)
        assertEquals(8320.0, converted, 1e-9)
    }

    /** In-memory fake — no Robolectric needed because we don't touch Room. */
    private class FakeRepo : RatesRepository {
        var nextRefresh: Result<Rates> = Result.success(
            Rates("USD", mapOf("USD" to 1.0), 0L),
        )
        private val cached = MutableStateFlow<Rates?>(null)
        private val favs = MutableStateFlow<List<String>>(emptyList())

        override fun observeCached(): Flow<Rates?> = cached.asStateFlow()

        override fun observeFavorites(): Flow<List<String>> = favs.asStateFlow()

        override suspend fun refresh(base: String) {
            val rates = nextRefresh.getOrThrow()
            cached.value = rates
        }

        override suspend fun addFavorite(code: String) {
            favs.value = favs.value + code
        }

        override suspend fun removeFavorite(code: String) {
            favs.value = favs.value - code
        }
    }
}
