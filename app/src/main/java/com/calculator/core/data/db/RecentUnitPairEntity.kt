package com.calculator.core.data.db

import androidx.room.Entity
import androidx.room.PrimaryKey

/**
 * Persisted last-used (from, to) pair per unit-converter category.
 *
 * Category name is the primary key so each category has exactly one
 * row - inserting another with the same category replaces it (via
 * Room's REPLACE conflict strategy on the DAO).
 *
 * @property category The [UnitCategory] name; one row per category.
 * @property fromSymbol Symbol of the "from" unit (e.g. "km").
 * @property toSymbol Symbol of the "to" unit (e.g. "mi").
 */
@Entity(tableName = "recent_unit_pair")
data class RecentUnitPairEntity(
    @PrimaryKey val category: String,
    val fromSymbol: String,
    val toSymbol: String,
)
