package com.calculator.core.data.settings

import androidx.datastore.preferences.core.PreferenceDataStoreFactory
import androidx.datastore.preferences.preferencesDataStoreFile
import androidx.test.core.app.ApplicationProvider
import app.cash.turbine.test
import com.calculator.core.data.di.DataModule
import kotlinx.coroutines.flow.first
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
    fun corruptSettingsFileGivesDefaultsInsteadOfACrash() =
        runTest {
            val appFile = context.preferencesDataStoreFile("user_settings")
            appFile.parentFile?.mkdirs()
            appFile.writeBytes(byteArrayOf(0x7f, 0x00, 0x13, 0x37, 0x42))
            try {
                val repo = DataStoreSettingsRepository(DataModule.provideUserSettingsDataStore(context))
                assertEquals(UserSettings(), repo.settings.first())
                // The corrupt file is replaced, so new choices save again.
                repo.setTheme(UserSettings.ThemeOption.Dark)
                assertEquals(UserSettings.ThemeOption.Dark, repo.settings.first().theme)
            } finally {
                appFile.delete()
            }
        }

    @Test
    fun timezonePairIsSavedTogether() =
        runTest {
            val repo = DataStoreSettingsRepository(newDataStore())
            repo.setLastTzPair("Europe/Paris", "Asia/Tokyo")
            val saved = repo.settings.first()
            assertEquals("Europe/Paris", saved.lastTzFromZone)
            assertEquals("Asia/Tokyo", saved.lastTzToZone)
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
                assertEquals(UserSettings.DEFAULT_PRECISION, snapshot.precision)
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

            val second = DataStoreSettingsRepository(store)
            second.settings.test {
                val snapshot = awaitItem()
                assertEquals(UserSettings.ThemeOption.Dark, snapshot.theme)
                assertFalse(snapshot.sound)
                assertEquals(10, snapshot.precision)
                cancelAndIgnoreRemainingEvents()
            }
        }

    @Test
    fun precisionIsClampedToTheValidRange() =
        runTest {
            val repo = DataStoreSettingsRepository(newDataStore())
            repo.setPrecision(99) // out of range
            repo.settings.test {
                assertEquals(UserSettings.PRECISION_RANGE.last, awaitItem().precision)
                cancelAndIgnoreRemainingEvents()
            }
        }

    private fun newDataStore() =
        PreferenceDataStoreFactory.create { testFile }
}
