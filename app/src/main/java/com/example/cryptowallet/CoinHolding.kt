package com.example.cryptowallet

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "Coins")
data class CoinHolding(
    @PrimaryKey(autoGenerate = true)  val id: Int = 0,
    @ColumnInfo(name = "name") val name: String,
    @ColumnInfo(name = "symbol") val symbol: String,
    @ColumnInfo(name = "quantity") val quantity: Double,
    @ColumnInfo(name = "pricePaid") val pricePaid: Double
)
