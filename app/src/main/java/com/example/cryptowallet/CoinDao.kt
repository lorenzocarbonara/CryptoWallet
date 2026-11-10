package com.example.cryptowallet

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.Query

@Dao
interface CoinDao {
    @Query("SELECT * FROM Coins")
    suspend fun getAll(): List<CoinHolding>

    @Insert
    suspend fun insert(coin: CoinHolding)

    @Delete
    suspend fun delete(coin: CoinHolding)
}