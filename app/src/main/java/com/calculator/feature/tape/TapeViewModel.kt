package com.calculator.feature.tape

import androidx.lifecycle.ViewModel
import com.calculator.core.data.tape.TapeEntry
import com.calculator.core.data.tape.TapeStore
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.StateFlow
import javax.inject.Inject

/** Backs the tape screen: the session's lines and the clear action. */
@HiltViewModel
class TapeViewModel
    @Inject
    constructor(
        private val tapeStore: TapeStore,
    ) : ViewModel() {
        /** Tape lines, oldest first. */
        val entries: StateFlow<List<TapeEntry>> = tapeStore.entries

        /** Empties the tape. */
        fun clear() = tapeStore.clear()
    }
