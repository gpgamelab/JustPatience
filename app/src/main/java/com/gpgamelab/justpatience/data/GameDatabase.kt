package com.gpgamelab.justpatience.data

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase

/**
 * Room database for storing game records and history.
 * Provides a singleton instance for accessing game statistics.
 */
@Database(entities = [GameRecord::class], version = 1)
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
                ).build().also { instance = it }
            }
        }
    }
}

