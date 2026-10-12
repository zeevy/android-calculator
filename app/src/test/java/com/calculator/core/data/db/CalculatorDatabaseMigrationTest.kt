package com.calculator.core.data.db

import androidx.room.testing.MigrationTestHelper
import androidx.test.platform.app.InstrumentationRegistry
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

/**
 * Guards user data across schema changes.
 *
 * Opens the first exported schema (version 4) and runs every migration in
 * [CalculatorDatabase.MIGRATIONS] up to [CalculatorDatabase.VERSION], then
 * checks the result matches the exported schema of that version. A version
 * bump without a matching migration fails here instead of wiping history on
 * the user's phone.
 */
@RunWith(RobolectricTestRunner::class)
@Config(sdk = [34])
class CalculatorDatabaseMigrationTest {
    @get:Rule
    val helper =
        MigrationTestHelper(
            InstrumentationRegistry.getInstrumentation(),
            CalculatorDatabase::class.java,
        )

    @Test
    fun migratesFromFirstExportedSchemaToCurrentVersion() {
        helper.createDatabase(DB_NAME, FIRST_EXPORTED_VERSION).close()

        helper
            .runMigrationsAndValidate(
                DB_NAME,
                CalculatorDatabase.VERSION,
                true,
                *CalculatorDatabase.MIGRATIONS,
            ).close()
    }

    private companion object {
        const val DB_NAME = "migration-test.db"
        const val FIRST_EXPORTED_VERSION = 4
    }
}
