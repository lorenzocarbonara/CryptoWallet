package com.example.cryptowallet

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase

@Database(entities = [CoinHolding::class], version = 1)
abstract class CoinsDatabase : RoomDatabase(){

    abstract fun getDao(): CoinDao

    companion object {
        @Volatile
        private var INSTANCE: CoinsDatabase? = null
        fun getInstance(context: Context): CoinsDatabase {

            return INSTANCE ?: synchronized(this) {
                INSTANCE ?: Room.databaseBuilder(
                    context.applicationContext,
                    CoinsDatabase::class.java, "Coins_db"
                )
                    .build()
            }
        }
    }
}