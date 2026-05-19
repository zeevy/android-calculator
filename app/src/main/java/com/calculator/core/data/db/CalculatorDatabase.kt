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
//
// v2: adds the recent_unit_pair table for the Phase 5 unit converter.
// Migration is destructive-fallback for now; once a real release ships
// we'll add explicit Migration objects so users keep their history.
@Database(
    entities = [HistoryEntity::class, RecentUnitPairEntity::class],
    version = 2,
    exportSchema = false,
)
abstract class CalculatorDatabase : RoomDatabase() {
    abstract fun historyDao(): HistoryDao

    abstract fun recentUnitPairDao(): RecentUnitPairDao

    companion object {
        const val NAME = "calculator.db"
    }
}
