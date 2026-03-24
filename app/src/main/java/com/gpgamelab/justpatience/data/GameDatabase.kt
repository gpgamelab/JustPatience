package com.gpgamelab.justpatience.data

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase

/**
 * Room database for storing game records and history.
 * Provides a singleton instance for accessing game statistics.
 */
@Database(entities = [GameRecord::class], version = 2)
abstract class GameDatabase : RoomDatabase() {

    abstract fun gameRecordDao(): GameRecordDao

    companion object {
        @Volatile
        private var instance: GameDatabase? = null

        /**
         * Get or create the singleton database instance.
         */
        fun getInstance(context: Context): GameDatabase {
            return instance ?: synchronized(this) {
                instance ?: Room.databaseBuilder(
                    context.applicationContext,
                    GameDatabase::class.java,
                    "game_database"
                )
                    .addMigrations(MIGRATION_1_2)
                    .build()
                    .also { instance = it }
            }
        }

        private val MIGRATION_1_2 = object : Migration(1, 2) {
            override fun migrate(database: SupportSQLiteDatabase) {
                database.execSQL("ALTER TABLE game_records ADD COLUMN playerName TEXT")
            }
        }
    }
}

