package com.calculator.core.data.db

import androidx.room.Database
import androidx.room.RoomDatabase

/**
 * Room database for the calculator. Currently a single table; future
 * phases will add a currency-rate cache (Phase 6) and unit-converter
 * favourites (Phase 5).
 *
 * Schema version bumps **must** be paired with a [Migration] - never
 * destructive migration on a release build, since users have personal
 * calculation history that they may want to keep.
 */
// exportSchema = false until the project adds a migration test suite;
// at that point we'll set up a schema location (build.gradle) and flip
// this to true so schema changes are reviewable in diff.
@Database(
    entities = [HistoryEntity::class],
    version = 1,
    exportSchema = false,
)
abstract class CalculatorDatabase : RoomDatabase() {
    abstract fun historyDao(): HistoryDao

    companion object {
        const val NAME = "calculator.db"
    }
}
