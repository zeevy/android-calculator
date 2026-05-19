package com.calculator.core.data.converter

import com.calculator.core.data.db.RecentUnitPairDao
import com.calculator.core.data.db.RecentUnitPairEntity
import com.calculator.core.domain.converter.UnitCategory
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Persists the most recently selected (from, to) unit pair per
 * [UnitCategory]. Pure data layer - the math itself lives in
 * [com.calculator.core.domain.converter.Converter].
 */
interface UnitConverterRepository {
    /** Recall the saved pair for [category], or null if none yet. */
    suspend fun recent(category: UnitCategory): Pair<String, String>?

    /** Record a new last-used pair for [category]. */
    suspend fun record(category: UnitCategory, fromSymbol: String, toSymbol: String)
}

@Singleton
class RoomUnitConverterRepository
    @Inject
    constructor(
        private val dao: RecentUnitPairDao,
    ) : UnitConverterRepository {
        override suspend fun recent(category: UnitCategory): Pair<String, String>? =
            dao.forCategory(category.name)?.let { it.fromSymbol to it.toSymbol }

        override suspend fun record(category: UnitCategory, fromSymbol: String, toSymbol: String) {
            dao.upsert(
                RecentUnitPairEntity(
                    category = category.name,
                    fromSymbol = fromSymbol,
                    toSymbol = toSymbol,
                ),
            )
        }
    }
