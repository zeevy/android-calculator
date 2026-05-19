package com.calculator.feature.basic.ui

import androidx.lifecycle.SavedStateHandle
import com.calculator.core.data.history.HistoryEntry
import com.calculator.core.data.history.HistoryRepository
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test

/**
 * Verifies that the ViewModel records a history entry per successful
 * `=`, and not on errors, blanks, or repeat-equals replays.
 */
@OptIn(ExperimentalCoroutinesApi::class)
class BasicCalculatorHistoryTest {
    private val dispatcher = StandardTestDispatcher()
    private val repo = FakeHistoryRepository()
    private lateinit var viewModel: BasicCalculatorViewModel

    @BeforeEach
    fun setUp() {
        Dispatchers.setMain(dispatcher)
        viewModel = BasicCalculatorViewModel(SavedStateHandle(), repo)
    }

    @AfterEach
    fun tearDown() {
        Dispatchers.resetMain()
    }

    @Test
    fun `successful equals records a history entry`() =
        runTest(dispatcher) {
            viewModel.onEvent(BasicCalculatorEvent.Append("2"))
            viewModel.onEvent(BasicCalculatorEvent.Append("+"))
            viewModel.onEvent(BasicCalculatorEvent.Append("3"))
            viewModel.onEvent(BasicCalculatorEvent.Equals)
            advanceUntilIdle()

            assertEquals(1, repo.added.size)
            val recorded = repo.added.single()
            assertEquals("2+3", recorded.expression)
            assertEquals("5", recorded.result)
            assertEquals(HistoryEntry.Type.Basic, recorded.type)
        }

    @Test
    fun `scientific mode records with the scientific type`() =
        runTest(dispatcher) {
            viewModel.onEvent(BasicCalculatorEvent.ToggleScientific)
            viewModel.onEvent(BasicCalculatorEvent.Append("π"))
            viewModel.onEvent(BasicCalculatorEvent.Equals)
            advanceUntilIdle()

            assertEquals(HistoryEntry.Type.Scientific, repo.added.single().type)
        }

    @Test
    fun `error does not record history`() =
        runTest(dispatcher) {
            viewModel.onEvent(BasicCalculatorEvent.Append("5"))
            viewModel.onEvent(BasicCalculatorEvent.Append("÷"))
            viewModel.onEvent(BasicCalculatorEvent.Append("0"))
            viewModel.onEvent(BasicCalculatorEvent.Equals)
            advanceUntilIdle()

            assertEquals(0, repo.added.size)
        }

    @Test
    fun `repeat-equals replay does not insert a second row`() =
        runTest(dispatcher) {
            // Dangling-operator path arms the repeat token.
            viewModel.onEvent(BasicCalculatorEvent.Append("2"))
            viewModel.onEvent(BasicCalculatorEvent.Append("+"))
            viewModel.onEvent(BasicCalculatorEvent.Equals)
            advanceUntilIdle()
            assertEquals(1, repo.added.size)

            // Subsequent `=` presses keep adding 2 to the running total but
            // should NOT spam the history table.
            viewModel.onEvent(BasicCalculatorEvent.Equals)
            advanceUntilIdle()
            viewModel.onEvent(BasicCalculatorEvent.Equals)
            advanceUntilIdle()
            assertEquals(1, repo.added.size)
        }

    @Test
    fun `blank equals does not record`() =
        runTest(dispatcher) {
            viewModel.onEvent(BasicCalculatorEvent.Equals)
            advanceUntilIdle()
            assertEquals(0, repo.added.size)
        }

    /** Stand-in [HistoryRepository] that captures every [add] call. */
    private class FakeHistoryRepository : HistoryRepository {
        data class AddCall(val expression: String, val result: String, val type: HistoryEntry.Type)

        val added = mutableListOf<AddCall>()
        private val backing = MutableStateFlow<List<HistoryEntry>>(emptyList())

        override fun observe(): Flow<List<HistoryEntry>> = backing.asStateFlow()

        override suspend fun add(expression: String, result: String, type: HistoryEntry.Type) {
            added += AddCall(expression, result, type)
        }

        override suspend fun delete(id: Long) = Unit

        override suspend fun clearAll() = Unit
    }
}
