package com.calculator.core.data.db

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.Query
import kotlinx.coroutines.flow.Flow

/**
 * Data-access methods for the history table.
 *
 * Read paths return [Flow] so observers (history sheet, M chip, etc.)
 * auto-refresh when a new calculation is committed. Write paths are
 * `suspend` and run on the dispatcher the caller chooses.
 */
@Dao
interface HistoryDao {
    /** Observe all entries, newest first. */
    @Query("SELECT * FROM history ORDER BY timestampUtc DESC")
    fun observeAll(): Flow<List<HistoryEntity>>

    /** Insert a new entry; returns the generated row id. */
    @Insert
    suspend fun insert(entry: HistoryEntity): Long

    /** Delete a single entry by id. No-op if the id is unknown. */
    @Query("DELETE FROM history WHERE id = :id")
    suspend fun deleteById(id: Long)

    /** Wipe the table. Used by the "Clear all" gesture. */
    @Query("DELETE FROM history")
    suspend fun clearAll()
}
