package com.calculator.feature.tape

import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow

/**
 * One row of the running calculator tape.
 *
 * Mirrors a desk-calculator printout: the [expression] the user typed
 * and the [result] it evaluated to. [id] is just a monotonic counter
 * so Compose's LazyColumn has a stable key.
 */
data class TapeEntry(
    val id: Long,
    val expression: String,
    val result: String,
)

/**
 * Process-wide running tape of basic-calculator evaluations.
 *
 * Lives in-memory only - the tape is the *session* ledger, not the
 * persisted history. Cleared on process death by design: a fresh app
 * launch should feel like a clean sheet of paper, while the History
 * sheet still retains the durable record.
 *
 * The [BasicCalculatorViewModel] pushes a new entry on every successful
 * `=` press; the [TapeScreen] observes [entries] and renders them.
 */
object TapeHolder {
    private val _entries = MutableStateFlow<List<TapeEntry>>(emptyList())
    val entries: StateFlow<List<TapeEntry>> = _entries

    private var nextId = 0L

    @Synchronized
    fun add(expression: String, result: String) {
        val entry = TapeEntry(id = nextId++, expression = expression, result = result)
        // Trim from the front when the cap is reached so the most-recent
        // entries are always visible without unbounded memory growth.
        val updated =
            (_entries.value + entry).let {
                if (it.size > MAX_ENTRIES) it.drop(it.size - MAX_ENTRIES) else it
            }
        _entries.value = updated
    }

    fun clear() {
        _entries.value = emptyList()
    }

    private const val MAX_ENTRIES = 200
}
