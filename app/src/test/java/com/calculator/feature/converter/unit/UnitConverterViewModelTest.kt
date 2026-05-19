package com.calculator.feature.converter.unit

import com.calculator.core.data.converter.UnitConverterRepository
import com.calculator.core.data.settings.SettingsRepository
import com.calculator.core.data.settings.UserSettings
import com.calculator.core.domain.converter.ConversionTable
import com.calculator.core.domain.converter.UnitCategory
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
 * Tests the conversion + UI-state semantics: typing into "from", picking
 * different units, swap, category change, and recent-pair recall.
 */
@OptIn(ExperimentalCoroutinesApi::class)
class UnitConverterViewModelTest {
    private val dispatcher = StandardTestDispatcher()
    private val repo = FakeRepo()
    private val settings = FakeSettings()
    private lateinit var viewModel: UnitConverterViewModel

    @BeforeEach
    fun setUp() {
        Dispatchers.setMain(dispatcher)
        viewModel = UnitConverterViewModel(repo, settings)
    }

    @AfterEach
    fun tearDown() {
        Dispatchers.resetMain()
    }

    @Test
    fun `typing in the from field updates the to output`() =
        runTest(dispatcher) {
            advanceUntilIdle()
            viewModel.selectCategory(UnitCategory.Length)
            advanceUntilIdle()
            // Default pair for Length: m -> km (units[0] vs units[1]).
            viewModel.setFromInput("1000")
            advanceUntilIdle()
            assertEquals("1", viewModel.state.value.toOutput)
        }

    @Test
    fun `picking a different to-unit recomputes`() =
        runTest(dispatcher) {
            advanceUntilIdle()
            viewModel.selectCategory(UnitCategory.Length)
            advanceUntilIdle()
            viewModel.setFromInput("1")
            val mile = ConversionTable.unitsFor(UnitCategory.Length).single { it.symbol == "mi" }
            viewModel.setToUnit(mile)
            advanceUntilIdle()
            // 1 m -> mile is 6.21371e-4. At precision 6 we render the
            // first 6 significant figures as "0.000621371".
            val out = viewModel.state.value.toOutput
            assertTrue(out.startsWith("0.000621"), "got $out")
        }

    @Test
    fun `swap exchanges units and shows the inverse`() =
        runTest(dispatcher) {
            advanceUntilIdle()
            viewModel.selectCategory(UnitCategory.Length)
            advanceUntilIdle()
            viewModel.setFromInput("1000")
            advanceUntilIdle()
            // Before swap: from = m, to = km. After swap: from = km, to = m
            // and the "from" field carries the previous output (1 km).
            viewModel.swap()
            advanceUntilIdle()
            assertEquals(
                "km",
                viewModel.state.value.fromUnit
                    ?.symbol,
            )
            assertEquals(
                "m",
                viewModel.state.value.toUnit
                    ?.symbol,
            )
            assertEquals("1", viewModel.state.value.fromInput)
            // toOutput goes through NumberFormatter so the running
            // locale adds grouping ("1,000" in en-US, "1.000" in de-DE).
            // Test JVM defaults to en-US; expect the comma-grouped form.
            assertEquals("1,000", viewModel.state.value.toOutput)
        }

    @Test
    fun `empty input emits an empty output`() =
        runTest(dispatcher) {
            advanceUntilIdle()
            viewModel.selectCategory(UnitCategory.Length)
            advanceUntilIdle()
            viewModel.setFromInput("")
            advanceUntilIdle()
            assertEquals("", viewModel.state.value.toOutput)
        }

    @Test
    fun `category change replaces unit list`() =
        runTest(dispatcher) {
            advanceUntilIdle()
            viewModel.selectCategory(UnitCategory.Temperature)
            advanceUntilIdle()
            val units = viewModel.state.value.units
            assertEquals(
                ConversionTable.unitsFor(UnitCategory.Temperature).map { it.symbol },
                units.map { it.symbol },
            )
        }

    @Test
    fun `recent pair is persisted on unit change`() =
        runTest(dispatcher) {
            advanceUntilIdle()
            viewModel.selectCategory(UnitCategory.Mass)
            advanceUntilIdle()
            val lb = ConversionTable.unitsFor(UnitCategory.Mass).single { it.symbol == "lb" }
            viewModel.setToUnit(lb)
            advanceUntilIdle()
            assertEquals("kg" to "lb", repo.records[UnitCategory.Mass])
        }

    @Test
    fun `recent pair is recalled when reopening a category`() =
        runTest(dispatcher) {
            advanceUntilIdle()
            // Seed the repo as if a previous session saved this pair.
            repo.records[UnitCategory.Volume] = "L" to "gal US"
            viewModel.selectCategory(UnitCategory.Volume)
            advanceUntilIdle()
            assertEquals(
                "L",
                viewModel.state.value.fromUnit
                    ?.symbol,
            )
            assertEquals(
                "gal US",
                viewModel.state.value.toUnit
                    ?.symbol,
            )
        }

    private class FakeRepo : UnitConverterRepository {
        val records = mutableMapOf<UnitCategory, Pair<String, String>>()

        override suspend fun recent(category: UnitCategory): Pair<String, String>? = records[category]

        override suspend fun record(category: UnitCategory, fromSymbol: String, toSymbol: String) {
            records[category] = fromSymbol to toSymbol
        }
    }

    private class FakeSettings : SettingsRepository {
        private val snapshot = MutableStateFlow(UserSettings(precision = 6))
        override val settings: Flow<UserSettings> = snapshot.asStateFlow()

        override suspend fun setTheme(theme: UserSettings.ThemeOption) = Unit

        override suspend fun setDynamicColor(enabled: Boolean) = Unit

        override suspend fun setHaptics(enabled: Boolean) = Unit

        override suspend fun setSound(enabled: Boolean) = Unit

        override suspend fun setPrecision(precision: Int) {
            snapshot.value = snapshot.value.copy(precision = precision)
        }

        override suspend fun setCrashOptIn(enabled: Boolean) = Unit
    }
}
