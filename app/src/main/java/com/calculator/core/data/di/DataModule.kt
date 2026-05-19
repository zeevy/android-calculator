package com.calculator.core.data.di

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
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
    private val Context.userSettingsDataStore: DataStore<Preferences> by preferencesDataStore(
        name = SETTINGS_DATASTORE_NAME,
    )

    private const val SETTINGS_DATASTORE_NAME = "user_settings"

    @Provides
    @Singleton
    fun provideCalculatorDatabase(@ApplicationContext context: Context): CalculatorDatabase =
        Room.databaseBuilder(
            context,
            CalculatorDatabase::class.java,
            CalculatorDatabase.NAME,
        )
            // Acceptable while no public release has shipped. Pre-release
            // builds reset their on-device DB across schema bumps; once
            // we ship to Play, every schema bump must come with a
            // Migration object.
            .fallbackToDestructiveMigration()
            .build()

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
