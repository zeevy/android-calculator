package com.calculator.core.data.db

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.Query
import androidx.room.Transaction
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
    /**
     * Observe all entries, newest first. Sorting by the auto-increment id
     * gives the commit order and uses the primary key, so no index is needed.
     */
    @Query("SELECT * FROM history ORDER BY id DESC")
    fun observeAll(): Flow<List<HistoryEntity>>

    /** Insert a new entry; returns the generated row id. */
    @Insert
    suspend fun insert(entry: HistoryEntity): Long

    /**
     * Insert [entry] and remove the oldest rows so at most [keep] remain,
     * in one transaction.
     *
     * @return the generated row id.
     */
    @Transaction
    suspend fun insertKeepingNewest(
        entry: HistoryEntity,
        keep: Int,
    ): Long {
        val id = insert(entry)
        deleteAllButNewest(keep)
        return id
    }

    /** Remove every row except the [keep] newest ones. */
    @Query("DELETE FROM history WHERE id NOT IN (SELECT id FROM history ORDER BY id DESC LIMIT :keep)")
    suspend fun deleteAllButNewest(keep: Int)

    /** Delete a single entry by id. No-op if the id is unknown. */
    @Query("DELETE FROM history WHERE id = :id")
    suspend fun deleteById(id: Long)

    /** Wipe the table. Used by the "Clear all" gesture. */
    @Query("DELETE FROM history")
    suspend fun clearAll()
}
