package com.calculator.feature.settings

import com.calculator.core.data.settings.SettingsRepository
import com.calculator.core.data.settings.UserSettings
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
import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test

/**
 * Tests that [SettingsViewModel] correctly forwards writes into the
 * repository and exposes the resulting Flow as a StateFlow.
 */
@OptIn(ExperimentalCoroutinesApi::class)
class SettingsViewModelTest {
    private val dispatcher = StandardTestDispatcher()
    private val repo = FakeSettingsRepository()
    private lateinit var viewModel: SettingsViewModel

    @BeforeEach
    fun setUp() {
        Dispatchers.setMain(dispatcher)
        viewModel = SettingsViewModel(repo)
    }

    @AfterEach
    fun tearDown() {
        Dispatchers.resetMain()
    }

    @Test
    fun `default settings are exposed`() =
        runTest(dispatcher) {
            advanceUntilIdle()
            assertEquals(UserSettings(), viewModel.settings.value)
        }

    @Test
    fun `setTheme propagates to the repository`() =
        runTest(dispatcher) {
            viewModel.setTheme(UserSettings.ThemeOption.Dark)
            advanceUntilIdle()
            assertEquals(UserSettings.ThemeOption.Dark, repo.snapshot.value.theme)
        }

    @Test
    fun `setDynamicColor propagates to the repository`() =
        runTest(dispatcher) {
            viewModel.setDynamicColor(false)
            advanceUntilIdle()
            assertFalse(repo.snapshot.value.dynamicColor)
        }

    @Test
    fun `setPrecision propagates to the repository`() =
        runTest(dispatcher) {
            viewModel.setPrecision(8)
            advanceUntilIdle()
            assertEquals(8, repo.snapshot.value.precision)
        }

    @Test
    fun `setCrashOptIn propagates to the repository`() =
        runTest(dispatcher) {
            viewModel.setCrashOptIn(true)
            advanceUntilIdle()
            assertTrue(repo.snapshot.value.crashOptIn)
        }

    /** Tiny in-memory fake so the test doesn't need DataStore + Robolectric. */
    private class FakeSettingsRepository : SettingsRepository {
        val snapshot = MutableStateFlow(UserSettings())

        override val settings: Flow<UserSettings> = snapshot.asStateFlow()

        override suspend fun setTheme(theme: UserSettings.ThemeOption) {
            snapshot.value = snapshot.value.copy(theme = theme)
        }

        override suspend fun setDynamicColor(enabled: Boolean) {
            snapshot.value = snapshot.value.copy(dynamicColor = enabled)
        }

        override suspend fun setHaptics(enabled: Boolean) {
            snapshot.value = snapshot.value.copy(haptics = enabled)
        }

        override suspend fun setSound(enabled: Boolean) {
            snapshot.value = snapshot.value.copy(sound = enabled)
        }

        override suspend fun setPrecision(precision: Int) {
            snapshot.value = snapshot.value.copy(precision = precision)
        }

        override suspend fun setCrashOptIn(enabled: Boolean) {
            snapshot.value = snapshot.value.copy(crashOptIn = enabled)
        }

        override suspend fun setLastUnitCategory(category: String) {
            snapshot.value = snapshot.value.copy(lastUnitCategory = category)
        }

        override suspend fun setGstIntraState(intra: Boolean) {
            snapshot.value = snapshot.value.copy(gstIntraState = intra)
        }

        override suspend fun setGstRate(rate: String) {
            snapshot.value = snapshot.value.copy(gstRate = rate)
        }

        override suspend fun setBmiHeightImperial(imperial: Boolean) {
            snapshot.value = snapshot.value.copy(bmiHeightImperial = imperial)
        }

        override suspend fun setBmiWeightImperial(imperial: Boolean) {
            snapshot.value = snapshot.value.copy(bmiWeightImperial = imperial)
        }
    }
}
