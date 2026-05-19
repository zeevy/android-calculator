package com.calculator.core.data.db

import androidx.room.Entity
import androidx.room.PrimaryKey

/**
 * Pinned favourite currency. The pin reordering UX uses [position]
 * (ascending, 0 first) so the user can drag-reorder later without a
 * schema change.
 */
@Entity(tableName = "favorite_currency")
data class FavoriteCurrencyEntity(
    @PrimaryKey val code: String,
    val position: Int,
)
