package com.calculator.core.data.db

import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import kotlinx.coroutines.test.runTest
import org.junit.After
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import kotlin.test.assertEquals
import kotlin.test.assertNull

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [34])
class RecentUnitPairDaoTest {
    private lateinit var db: CalculatorDatabase
    private lateinit var dao: RecentUnitPairDao

    @Before
    fun setUp() {
        db =
            Room
                .inMemoryDatabaseBuilder(
                    ApplicationProvider.getApplicationContext(),
                    CalculatorDatabase::class.java,
                ).allowMainThreadQueries()
                .build()
        dao = db.recentUnitPairDao()
    }

    @After
    fun tearDown() {
        db.close()
    }

    @Test
    fun emptyTableReturnsNull() =
        runTest {
            assertNull(dao.forCategory("Length"))
        }

    @Test
    fun upsertThenReadRoundTrip() =
        runTest {
            dao.upsert(RecentUnitPairEntity("Length", "km", "mi"))
            val row = dao.forCategory("Length")
            assertEquals("km", row?.fromSymbol)
            assertEquals("mi", row?.toSymbol)
        }

    @Test
    fun upsertReplacesExistingRow() =
        runTest {
            dao.upsert(RecentUnitPairEntity("Mass", "kg", "lb"))
            dao.upsert(RecentUnitPairEntity("Mass", "g", "oz"))
            val row = dao.forCategory("Mass")
            assertEquals("g", row?.fromSymbol)
            assertEquals("oz", row?.toSymbol)
        }

    @Test
    fun differentCategoriesCoexist() =
        runTest {
            dao.upsert(RecentUnitPairEntity("Length", "m", "ft"))
            dao.upsert(RecentUnitPairEntity("Mass", "kg", "lb"))
            assertEquals("m", dao.forCategory("Length")?.fromSymbol)
            assertEquals("kg", dao.forCategory("Mass")?.fromSymbol)
        }
}
