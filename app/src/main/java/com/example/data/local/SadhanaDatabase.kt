package com.example.data.local

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase

@Database(
    entities = [SankalpEntity::class, JapaSessionEntity::class],
    version = 1,
    exportSchema = false
)
abstract class SadhanaDatabase : RoomDatabase() {
    abstract fun sadhanaDao(): SadhanaDao

    companion object {
        @Volatile
        private var INSTANCE: SadhanaDatabase? = null

        fun getInstance(context: Context): SadhanaDatabase {
            return INSTANCE ?: synchronized(this) {
                INSTANCE ?: Room.databaseBuilder(
                    context.applicationContext,
                    SadhanaDatabase::class.java,
                    "sadhana_sankalp_db"
                ).build().also { INSTANCE = it }
            }
        }
    }
}
