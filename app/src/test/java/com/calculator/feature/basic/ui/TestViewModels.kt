package com.calculator.feature.basic.ui

import androidx.lifecycle.SavedStateHandle
import com.calculator.core.data.history.HistoryRepository
import com.calculator.core.data.settings.SettingsRepository
import com.calculator.core.data.tape.TapeStore
import com.calculator.testing.FakeHistoryRepository
import com.calculator.testing.FakeSettingsRepository

/** Builds a [BasicCalculatorViewModel] with in-memory fakes. Needs a test Main dispatcher. */
internal fun basicCalculatorViewModel(
    savedStateHandle: SavedStateHandle = SavedStateHandle(),
    historyRepository: HistoryRepository = FakeHistoryRepository(),
    settingsRepository: SettingsRepository = FakeSettingsRepository(),
    tapeStore: TapeStore = TapeStore(),
) = BasicCalculatorViewModel(savedStateHandle, historyRepository, settingsRepository, tapeStore)
