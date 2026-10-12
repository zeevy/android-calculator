package com.calculator.feature.settings

import app.cash.turbine.test
import com.calculator.core.data.settings.UserSettings
import com.calculator.testing.FakeSettingsRepository
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Assertions.assertNull
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
    fun `settings are null until read, then the saved values`() =
        runTest(dispatcher) {
            assertNull(viewModel.settings.value)
            viewModel.settings.test {
                assertNull(awaitItem())
                assertEquals(UserSettings(), awaitItem())
                cancelAndIgnoreRemainingEvents()
            }
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
}
