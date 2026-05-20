package com.calculator.core.data.db

import androidx.room.Database
import androidx.room.RoomDatabase

/**
 * Room database for the calculator.
 *
 * Schema version bumps **must** be paired with a [Migration] once any
 * public release has shipped - users have personal calculation history
 * they may want to keep across upgrades.
 *
 * Version history:
 *  - v1: `history` table.
 *  - v2: adds `recent_unit_pair` for the Phase 5 unit converter.
 *  - v3: adds `currency_rate` + `favorite_currency` for Phase 6.
 *  - v4: drops `currency_rate` + `favorite_currency` because the
 *    currency-converter feature was removed.
 *
 * `exportSchema = false` while pre-release; flip to true and configure
 * a schema location in `build.gradle` once a migration test suite is
 * added (then every schema bump shows up in diff).
 */
@Database(
    entities = [
        HistoryEntity::class,
        RecentUnitPairEntity::class,
    ],
    version = 4,
    exportSchema = false,
)
abstract class CalculatorDatabase : RoomDatabase() {
    abstract fun historyDao(): HistoryDao

    abstract fun recentUnitPairDao(): RecentUnitPairDao

    companion object {
        const val NAME = "calculator.db"
    }
}
