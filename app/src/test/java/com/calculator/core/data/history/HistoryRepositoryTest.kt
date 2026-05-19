package com.calculator.core.data.history

import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import app.cash.turbine.test
import com.calculator.core.data.db.CalculatorDatabase
import kotlinx.coroutines.test.runTest
import org.junit.After
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import kotlin.test.assertEquals

/**
 * Repository-level tests against a real in-memory Room DB. Verifies the
 * entity <-> domain mapping and that the Flow emits per-insert.
 */
@RunWith(RobolectricTestRunner::class)
@Config(sdk = [34])
class HistoryRepositoryTest {
    private lateinit var db: CalculatorDatabase
    private lateinit var repo: HistoryRepository

    @Before
    fun setUp() {
        db =
            Room.inMemoryDatabaseBuilder(
                ApplicationProvider.getApplicationContext(),
                CalculatorDatabase::class.java,
            ).allowMainThreadQueries().build()
        repo = RoomHistoryRepository(db.historyDao())
    }

    @After
    fun tearDown() {
        db.close()
    }

    @Test
    fun addEmitsViaObserve() =
        runTest {
            repo.observe().test {
                assertEquals(emptyList(), awaitItem())
                repo.add("2+3", "5", HistoryEntry.Type.Basic)
                val rows = awaitItem()
                assertEquals(1, rows.size)
                assertEquals("2+3", rows[0].expression)
                assertEquals("5", rows[0].result)
                assertEquals(HistoryEntry.Type.Basic, rows[0].type)
                cancelAndIgnoreRemainingEvents()
            }
        }

    @Test
    fun scientificTypeMapsRoundTrip() =
        runTest {
            repo.add("sin(30)", "0.5", HistoryEntry.Type.Scientific)
            repo.observe().test {
                val rows = awaitItem()
                assertEquals(HistoryEntry.Type.Scientific, rows[0].type)
                cancelAndIgnoreRemainingEvents()
            }
        }

    @Test
    fun clearAllEmptiesTheRepository() =
        runTest {
            repo.add("1+1", "2", HistoryEntry.Type.Basic)
            repo.add("2+2", "4", HistoryEntry.Type.Basic)
            repo.clearAll()
            repo.observe().test {
                assertEquals(emptyList(), awaitItem())
                cancelAndIgnoreRemainingEvents()
            }
        }
}
