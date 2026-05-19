package com.calculator.core.data.settings

import androidx.datastore.preferences.core.PreferenceDataStoreFactory
import androidx.datastore.preferences.preferencesDataStoreFile
import androidx.test.core.app.ApplicationProvider
import app.cash.turbine.test
import kotlinx.coroutines.test.runTest
import org.junit.After
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

/**
 * Verifies that [DataStoreSettingsRepository] writes round-trip through
 * DataStore and that values survive a fresh repository instance pointed
 * at the same file (the persistence guarantee that matters for
 * settings).
 */
@RunWith(RobolectricTestRunner::class)
@Config(sdk = [34])
class SettingsRepositoryTest {
    private val context = ApplicationProvider.getApplicationContext<android.content.Context>()
    private val testFile = context.preferencesDataStoreFile("settings_test")

    @Before
    fun cleanFile() {
        if (testFile.exists()) testFile.delete()
    }

    @After
    fun cleanUp() {
        if (testFile.exists()) testFile.delete()
    }

    @Test
    fun defaultsAreReturnedWhenNoValuesAreStored() =
        runTest {
            val repo = DataStoreSettingsRepository(newDataStore())
            repo.settings.test {
                val snapshot = awaitItem()
                assertEquals(UserSettings.ThemeOption.System, snapshot.theme)
                assertTrue(snapshot.dynamicColor)
                assertTrue(snapshot.haptics)
                assertTrue(snapshot.sound)
                assertEquals(DataStoreSettingsRepository.DEFAULT_PRECISION, snapshot.precision)
                assertFalse(snapshot.crashOptIn)
                cancelAndIgnoreRemainingEvents()
            }
        }

    @Test
    fun writesPersistAcrossRepositoryInstances() =
        runTest {
            val store = newDataStore()
            val first = DataStoreSettingsRepository(store)
            first.setTheme(UserSettings.ThemeOption.Dark)
            first.setSound(false)
            first.setPrecision(10)
            first.setCrashOptIn(true)

            val second = DataStoreSettingsRepository(store)
            second.settings.test {
                val snapshot = awaitItem()
                assertEquals(UserSettings.ThemeOption.Dark, snapshot.theme)
                assertFalse(snapshot.sound)
                assertEquals(10, snapshot.precision)
                assertTrue(snapshot.crashOptIn)
                cancelAndIgnoreRemainingEvents()
            }
        }

    @Test
    fun precisionIsClampedToTheValidRange() =
        runTest {
            val repo = DataStoreSettingsRepository(newDataStore())
            repo.setPrecision(99) // out of range
            repo.settings.test {
                assertEquals(DataStoreSettingsRepository.MAX_PRECISION, awaitItem().precision)
                cancelAndIgnoreRemainingEvents()
            }
        }

    private fun newDataStore() =
        PreferenceDataStoreFactory.create { testFile }
}
