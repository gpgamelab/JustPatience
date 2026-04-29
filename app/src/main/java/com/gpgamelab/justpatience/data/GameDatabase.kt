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
@Database(entities = [GameRecord::class], version = 3)
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
                    .addMigrations(MIGRATION_1_2, MIGRATION_2_3)
                    .build()
                    .also { instance = it }
            }
        }

        private val MIGRATION_1_2 = object : Migration(1, 2) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL("ALTER TABLE game_records ADD COLUMN playerName TEXT")
            }
        }

        private val MIGRATION_2_3 = object : Migration(2, 3) {
            override fun migrate(db: SupportSQLiteDatabase) {
                if (!hasColumn(db, "game_records", "cardsDraw")) {
                    db.execSQL("ALTER TABLE game_records ADD COLUMN cardsDraw INTEGER")
                }
                if (!hasColumn(db, "game_records", "playerName")) {
                    db.execSQL("ALTER TABLE game_records ADD COLUMN playerName TEXT")
                }
            }
        }

        private fun hasColumn(
            database: SupportSQLiteDatabase,
            tableName: String,
            columnName: String
        ): Boolean {
            val cursor = database.query("PRAGMA table_info($tableName)")
            cursor.use {
                val nameIndex = it.getColumnIndex("name")
                while (it.moveToNext()) {
                    if (it.getString(nameIndex) == columnName) return true
                }
            }
            return false
        }
    }
}
