package com.calculator.core.data.tape

import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import java.util.concurrent.atomic.AtomicLong
import javax.inject.Inject
import javax.inject.Singleton

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
 * Running tape of basic-calculator evaluations for this app session.
 *
 * Lives in `core/` rather than `feature/tape/` because two features
 * read it - the standalone tape screen and the basic calculator's
 * inline tape - and a feature package never imports another feature
 * package. One instance per process (Hilt singleton), injected into
 * both ViewModels.
 *
 * Lives in-memory only - the tape is the *session* ledger, not the
 * persisted history. Cleared on process death by design: a fresh app
 * launch should feel like a clean sheet of paper, while the History
 * sheet still retains the durable record.
 */
@Singleton
class TapeStore
    @Inject
    constructor() {
        private val _entries = MutableStateFlow<List<TapeEntry>>(emptyList())

        /** Entries, oldest first, at most [MAX_ENTRIES]. */
        val entries: StateFlow<List<TapeEntry>> = _entries.asStateFlow()

        private val nextId = AtomicLong()

        /** Appends one line, dropping the oldest when the cap is reached. */
        fun add(
            expression: String,
            result: String,
        ) {
            val entry = TapeEntry(id = nextId.getAndIncrement(), expression = expression, result = result)
            _entries.update { (it + entry).takeLast(MAX_ENTRIES) }
        }

        /** Drop a single line, addressed by its [TapeEntry.id]. */
        fun remove(id: Long) {
            _entries.update { lines -> lines.filterNot { it.id == id } }
        }

        /** Empties the tape. */
        fun clear() {
            _entries.value = emptyList()
        }

        private companion object {
            const val MAX_ENTRIES = 200
        }
    }
