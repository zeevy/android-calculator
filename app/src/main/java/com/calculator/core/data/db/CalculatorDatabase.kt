package com.calculator.core.data.db

import androidx.room.Database
import androidx.room.RoomDatabase
import androidx.room.migration.Migration

/**
 * Room database for the calculator.
 *
 * Public releases have shipped, so every schema version bump **must** come
 * with a [Migration] in [MIGRATIONS] and a newly exported schema JSON under
 * `app/schemas/`. There is no destructive fallback: a missing migration
 * fails `CalculatorDatabaseMigrationTest` instead of wiping the user's
 * history on upgrade.
 *
 * Version history:
 *  - v1: `history` table.
 *  - v2: adds `recent_unit_pair` for the Phase 5 unit converter.
 *  - v3: adds `currency_rate` + `favorite_currency` for Phase 6.
 *  - v4: drops `currency_rate` + `favorite_currency` because the
 *    currency-converter feature was removed. First exported schema.
 */
@Database(
    entities = [
        HistoryEntity::class,
        RecentUnitPairEntity::class,
    ],
    version = CalculatorDatabase.VERSION,
    exportSchema = true,
)
abstract class CalculatorDatabase : RoomDatabase() {
    abstract fun historyDao(): HistoryDao

    abstract fun recentUnitPairDao(): RecentUnitPairDao

    companion object {
        const val NAME = "calculator.db"

        /** Current schema version. Bump it only together with a new entry in [MIGRATIONS]. */
        const val VERSION = 4

        /** Every migration from version 4 (the first exported schema) up to [VERSION]. */
        val MIGRATIONS: Array<Migration> = emptyArray()
    }
}
