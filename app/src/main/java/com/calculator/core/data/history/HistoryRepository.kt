package com.calculator.core.data.history

import com.calculator.core.data.db.HistoryDao
import com.calculator.core.data.db.HistoryEntity
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Mediates between the Room DAO and the rest of the app.
 *
 * Keeps the engine/feature layers free of Room types ([HistoryEntity] is
 * private to the data package) and gives us a place to add caching or
 * cross-feature wiring later (e.g. emitting an "M chip" derivation when
 * a memory-bearing entry lands).
 */
interface HistoryRepository {
    /** Newest first; replays the latest list to every new subscriber. */
    fun observe(): Flow<List<HistoryEntry>>

    /** Record a new calculation. The repository assigns the timestamp. */
    suspend fun add(expression: String, result: String, type: HistoryEntry.Type)

    /** Delete a single entry by id. */
    suspend fun delete(id: Long)

    /** Wipe all history. */
    suspend fun clearAll()
}

@Singleton
class RoomHistoryRepository
    @Inject
    constructor(
        private val dao: HistoryDao,
    ) : HistoryRepository {
        override fun observe(): Flow<List<HistoryEntry>> =
            dao.observeAll().map { rows -> rows.map { it.toEntry() } }

        override suspend fun add(expression: String, result: String, type: HistoryEntry.Type) {
            dao.insert(
                HistoryEntity(
                    expression = expression,
                    result = result,
                    timestampUtc = System.currentTimeMillis(),
                    type =
                        when (type) {
                            HistoryEntry.Type.Basic -> HistoryEntity.TYPE_BASIC
                            HistoryEntry.Type.Scientific -> HistoryEntity.TYPE_SCIENTIFIC
                        },
                ),
            )
        }

        override suspend fun delete(id: Long) = dao.deleteById(id)

        override suspend fun clearAll() = dao.clearAll()

        private fun HistoryEntity.toEntry(): HistoryEntry =
            HistoryEntry(
                id = id,
                expression = expression,
                result = result,
                timestampUtc = timestampUtc,
                type =
                    when (type) {
                        HistoryEntity.TYPE_SCIENTIFIC -> HistoryEntry.Type.Scientific
                        else -> HistoryEntry.Type.Basic
                    },
            )
    }
