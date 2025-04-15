// Datebase.kt:
package com.example.startup_etnofit_2

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase

@Database(entities = [ChecksData::class, ReckoningData::class], version = 1, exportSchema = false)
abstract class AppDatabase : RoomDatabase() {
    abstract fun checksDataDao(): ChecksDataDao
    abstract fun reckoningDataDao(): ReckoningDataDao

    companion object {
        @Volatile
        private var INSTANCE: AppDatabase? = null

        fun getDatabase(context: Context): AppDatabase {
            return INSTANCE ?: synchronized(this) {
                val instance = Room.databaseBuilder(
                    context.applicationContext,
                    AppDatabase::class.java,
                    "app_database" // Имя базы данных
                ).build()
                INSTANCE = instance
                instance
            }
        }
    }
}