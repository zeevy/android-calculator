package com.calculator.feature.history

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.calculator.core.data.history.HistoryEntry
import com.calculator.core.data.history.HistoryRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import javax.inject.Inject

/**
 * Backs the History sheet. Holds the latest list as a [StateFlow] so the
 * UI can render synchronously; deletes/clears are fire-and-forget into
 * the repository - the Flow subscription will re-emit the updated list.
 */
@HiltViewModel
class HistoryViewModel
    @Inject
    constructor(
        private val repository: HistoryRepository,
    ) : ViewModel() {
        val entries: StateFlow<List<HistoryEntry>> =
            repository.observe().stateIn(
                scope = viewModelScope,
                started = SharingStarted.WhileSubscribed(stopTimeoutMillis = 5_000),
                initialValue = emptyList(),
            )

        fun delete(id: Long) {
            viewModelScope.launch { repository.delete(id) }
        }

        fun clearAll() {
            viewModelScope.launch { repository.clearAll() }
        }
    }
