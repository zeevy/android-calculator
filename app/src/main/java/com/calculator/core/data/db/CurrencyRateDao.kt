package com.calculator.core.data.db

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Transaction
import kotlinx.coroutines.flow.Flow

@Dao
interface CurrencyRateDao {
    @Query("SELECT * FROM currency_rate ORDER BY code ASC")
    fun observeAll(): Flow<List<CurrencyRateEntity>>

    @Query("SELECT * FROM currency_rate ORDER BY code ASC")
    suspend fun allOnce(): List<CurrencyRateEntity>

    @Query("SELECT MAX(fetchedAtUtc) FROM currency_rate")
    suspend fun latestFetchedAt(): Long?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsertAll(rows: List<CurrencyRateEntity>)

    @Query("DELETE FROM currency_rate")
    suspend fun clear()

    /**
     * Atomically replace the whole table with [rows]. Useful when a
     * fresh fetch arrives - we never want stale codes mixing with new
     * ones inside the same view.
     */
    @Transaction
    suspend fun replaceAll(rows: List<CurrencyRateEntity>) {
        clear()
        upsertAll(rows)
    }
}

@Dao
interface FavoriteCurrencyDao {
    @Query("SELECT * FROM favorite_currency ORDER BY position ASC")
    fun observeAll(): Flow<List<FavoriteCurrencyEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsert(entry: FavoriteCurrencyEntity)

    @Query("DELETE FROM favorite_currency WHERE code = :code")
    suspend fun delete(code: String)

    @Query("SELECT COALESCE(MAX(position), -1) FROM favorite_currency")
    suspend fun maxPosition(): Int
}
