package com.calculator.feature.history

import app.cash.turbine.test
import com.calculator.core.data.history.HistoryEntry
import com.calculator.testing.FakeHistoryRepository
import com.calculator.testing.MainDispatcherExtension
import kotlinx.coroutines.test.runTest
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.extension.RegisterExtension

/** [HistoryViewModel] shows the saved list and passes deletes to the repository. */
class HistoryViewModelTest {
    @JvmField
    @RegisterExtension
    val mainDispatcher = MainDispatcherExtension()

    private val repository = FakeHistoryRepository()

    // Built on first use, after the extension has swapped in the test Main dispatcher.
    private val viewModel by lazy { HistoryViewModel(repository) }

    private val first = HistoryEntry(id = 1, expression = "2+3", result = "5", timestampUtc = 1_000, type = HistoryEntry.Type.Basic)
    private val second =
        HistoryEntry(id = 2, expression = "sin(0)", result = "0", timestampUtc = 2_000, type = HistoryEntry.Type.Scientific)

    @Test
    fun `entries show the list from the repository`() =
        runTest {
            viewModel.entries.test {
                assertEquals(emptyList<HistoryEntry>(), awaitItem())

                repository.entries.value = listOf(second, first)
                assertEquals(listOf(second, first), awaitItem())

                repository.entries.value = listOf(first)
                assertEquals(listOf(first), awaitItem())
            }
        }

    @Test
    fun `delete reaches the repository with the entry id`() {
        viewModel.delete(2)

        assertEquals(listOf(2L), repository.deleted)
    }

    @Test
    fun `clear all reaches the repository`() {
        viewModel.clearAll()

        assertEquals(1, repository.clearAllCalls)
    }
}
