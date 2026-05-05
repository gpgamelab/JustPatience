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
@Database(entities = [GameRecord::class], version = 5, exportSchema = false)
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
                    .addMigrations(MIGRATION_1_2, MIGRATION_2_3, MIGRATION_3_4, MIGRATION_4_5)
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

        private val MIGRATION_3_4 = object : Migration(3, 4) {
            override fun migrate(db: SupportSQLiteDatabase) {
                if (!hasColumn(db, "game_records", "deckCount")) {
                    db.execSQL("ALTER TABLE game_records ADD COLUMN deckCount INTEGER")
                }
            }
        }

        private val MIGRATION_4_5 = object : Migration(4, 5) {
            override fun migrate(db: SupportSQLiteDatabase) {
                if (!hasColumn(db, "game_records", "windowsScore")) {
                    db.execSQL("ALTER TABLE game_records ADD COLUMN windowsScore INTEGER")
                }
                if (!hasColumn(db, "game_records", "vegasScore")) {
                    db.execSQL("ALTER TABLE game_records ADD COLUMN vegasScore INTEGER")
                }
                if (!hasColumn(db, "game_records", "vegasCumulativeScore")) {
                    db.execSQL("ALTER TABLE game_records ADD COLUMN vegasCumulativeScore INTEGER")
                }
                if (!hasColumn(db, "game_records", "completionPercentage")) {
                    db.execSQL("ALTER TABLE game_records ADD COLUMN completionPercentage INTEGER")
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
