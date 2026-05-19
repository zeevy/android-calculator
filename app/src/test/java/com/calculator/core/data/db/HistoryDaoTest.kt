package com.calculator.core.data.db

import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import app.cash.turbine.test
import kotlinx.coroutines.test.runTest
import org.junit.After
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import kotlin.test.assertEquals
import kotlin.test.assertTrue

/**
 * Round-trip tests for [HistoryDao] against an in-memory Room database.
 *
 * Uses Robolectric so the SQLite driver and Android context plumbing
 * Room expects are available without booting an emulator. Robolectric
 * is JUnit4-only - the rest of the project's tests are JUnit5 but
 * the android-junit5 plugin runs both engines from one task.
 */
@RunWith(RobolectricTestRunner::class)
@Config(sdk = [34]) // Robolectric 4.14 ships shadows up through API 34
class HistoryDaoTest {
    private lateinit var db: CalculatorDatabase
    private lateinit var dao: HistoryDao

    @Before
    fun setUp() {
        db =
            Room
                .inMemoryDatabaseBuilder(
                    ApplicationProvider.getApplicationContext(),
                    CalculatorDatabase::class.java,
                ).allowMainThreadQueries()
                .build()
        dao = db.historyDao()
    }

    @After
    fun tearDown() {
        db.close()
    }

    @Test
    fun insertEmitsViaObserveAll() =
        runTest {
            dao.observeAll().test {
                assertEquals(emptyList(), awaitItem())
                dao.insert(entity("2+3", "5"))
                val rows = awaitItem()
                assertEquals(1, rows.size)
                assertEquals("2+3", rows[0].expression)
                assertEquals("5", rows[0].result)
                cancelAndIgnoreRemainingEvents()
            }
        }

    @Test
    fun observeAllOrdersNewestFirst() =
        runTest {
            dao.insert(entity("1+1", "2", timestamp = 1_000))
            dao.insert(entity("2+2", "4", timestamp = 2_000))
            dao.insert(entity("3+3", "6", timestamp = 3_000))

            dao.observeAll().test {
                val rows = awaitItem()
                assertEquals(listOf("3+3", "2+2", "1+1"), rows.map { it.expression })
                cancelAndIgnoreRemainingEvents()
            }
        }

    @Test
    fun deleteByIdRemovesOnlyTheTargetedRow() =
        runTest {
            val a = dao.insert(entity("1+1", "2"))
            val b = dao.insert(entity("2+2", "4"))
            dao.deleteById(a)

            dao.observeAll().test {
                val rows = awaitItem()
                assertEquals(1, rows.size)
                assertEquals(b, rows[0].id)
                cancelAndIgnoreRemainingEvents()
            }
        }

    @Test
    fun clearAllEmptiesTheTable() =
        runTest {
            dao.insert(entity("1+1", "2"))
            dao.insert(entity("2+2", "4"))
            dao.clearAll()

            dao.observeAll().test {
                assertTrue(awaitItem().isEmpty())
                cancelAndIgnoreRemainingEvents()
            }
        }

    private fun entity(
        expression: String,
        result: String,
        timestamp: Long = System.currentTimeMillis(),
    ) = HistoryEntity(
        expression = expression,
        result = result,
        timestampUtc = timestamp,
        type = HistoryEntity.TYPE_BASIC,
    )
}
