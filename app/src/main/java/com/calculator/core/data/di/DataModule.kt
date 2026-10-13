package com.calculator.core.data.di

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.core.handlers.ReplaceFileCorruptionHandler
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.emptyPreferences
import androidx.datastore.preferences.preferencesDataStore
import androidx.room.Room
import com.calculator.core.data.converter.RoomUnitConverterRepository
import com.calculator.core.data.converter.UnitConverterRepository
import com.calculator.core.data.db.CalculatorDatabase
import com.calculator.core.data.db.HistoryDao
import com.calculator.core.data.history.HistoryRepository
import com.calculator.core.data.history.RoomHistoryRepository
import com.calculator.core.data.settings.DataStoreSettingsRepository
import com.calculator.core.data.settings.SettingsRepository
import dagger.Binds
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import java.time.Clock
import javax.inject.Singleton

/**
 * Hilt bindings for the persistence layer.
 *
 * - Room database + DAOs as singletons (Room manages its own connection
 *   pool; one instance per process is the recommended pattern).
 * - DataStore as a process-wide singleton via Android's
 *   `preferencesDataStore` delegate so the app never opens more than
 *   one handle to the underlying file.
 */
@Module
@InstallIn(SingletonComponent::class)
object DataModule {
    // A corrupt file is replaced with empty preferences (all defaults)
    // instead of crashing every launch until the user clears app data.
    private val Context.userSettingsDataStore: DataStore<Preferences> by preferencesDataStore(
        name = SETTINGS_DATASTORE_NAME,
        corruptionHandler = ReplaceFileCorruptionHandler { emptyPreferences() },
    )

    private const val SETTINGS_DATASTORE_NAME = "user_settings"

    // The spread copies a tiny array once per process start.
    @Suppress("SpreadOperator")
    @Provides
    @Singleton
    fun provideCalculatorDatabase(
        @ApplicationContext context: Context,
    ): CalculatorDatabase =
        Room
            .databaseBuilder(
                context,
                CalculatorDatabase::class.java,
                CalculatorDatabase.NAME,
            ).addMigrations(*CalculatorDatabase.MIGRATIONS)
            // A downgrade (installing an older build over a newer one) has
            // no migration path, so only that case may reset the database.
            .fallbackToDestructiveMigrationOnDowngrade(dropAllTables = true)
            .build()

    /**
     * The clock for "now". Not a singleton, so each user picks up the
     * device time zone current at that moment; tests pass a fixed clock.
     */
    @Provides
    fun provideClock(): Clock = Clock.systemDefaultZone()

    @Provides
    @Singleton
    fun provideHistoryDao(db: CalculatorDatabase): HistoryDao = db.historyDao()

    @Provides
    @Singleton
    fun provideRecentUnitPairDao(db: CalculatorDatabase): com.calculator.core.data.db.RecentUnitPairDao =
        db.recentUnitPairDao()

    @Provides
    @Singleton
    fun provideUserSettingsDataStore(
        @ApplicationContext context: Context,
    ): DataStore<Preferences> = context.userSettingsDataStore
}

/**
 * Binds the concrete repository implementations to their interfaces.
 * Kept separate from [DataModule] so the `@Provides` and `@Binds`
 * styles don't have to share a module declaration.
 */
@Module
@InstallIn(SingletonComponent::class)
abstract class RepositoryModule {
    @Binds
    @Singleton
    abstract fun bindHistoryRepository(impl: RoomHistoryRepository): HistoryRepository

    @Binds
    @Singleton
    abstract fun bindSettingsRepository(impl: DataStoreSettingsRepository): SettingsRepository

    @Binds
    @Singleton
    abstract fun bindUnitConverterRepository(impl: RoomUnitConverterRepository): UnitConverterRepository
}
