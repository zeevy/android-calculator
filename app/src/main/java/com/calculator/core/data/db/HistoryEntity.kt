package com.calculator.core.data.db

import androidx.room.Entity
import androidx.room.PrimaryKey

/**
 * One row of calculation history.
 *
 * Stored verbatim - the expression that was committed via `=` and the
 * string-form result. Storing the result as text rather than recomputing
 * keeps the history stable even if the math engine's precision settings
 * change in a future release.
 *
 * @property id Auto-generated row id.
 * @property expression The canonical expression as the user committed it
 *   (after any auto-close-parens or trailing-operator auto-complete).
 * @property result The displayed result string for that expression.
 * @property timestampUtc Epoch milliseconds at insert time, in UTC. Sorted
 *   descending in the UI so the most recent entry is on top.
 * @property type Coarse category - basic, scientific, or one of the life
 *   calculators - so future screens can filter without re-parsing the
 *   expression. For now everything inserted is BASIC or SCIENTIFIC.
 */
@Entity(tableName = "history")
data class HistoryEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val expression: String,
    val result: String,
    val timestampUtc: Long,
    val type: String,
) {
    companion object {
        const val TYPE_BASIC = "basic"
        const val TYPE_SCIENTIFIC = "scientific"
    }
}
