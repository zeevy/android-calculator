package com.calculator.core.data.db

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query

/**
 * Read/write the one-row-per-category recent unit pairs.
 */
@Dao
interface RecentUnitPairDao {
    @Query("SELECT * FROM recent_unit_pair WHERE category = :category LIMIT 1")
    suspend fun forCategory(category: String): RecentUnitPairEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsert(entry: RecentUnitPairEntity)
}
