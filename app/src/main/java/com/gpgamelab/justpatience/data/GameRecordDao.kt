package com.gpgamelab.justpatience.data

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.Query
import kotlinx.coroutines.flow.Flow

/**
 * Data Access Object for game records.
 * Provides methods to insert, query, and manage game history.
 */
@Dao
interface GameRecordDao {

    /**
     * Insert a new game record into the database.
     */
    @Insert
    suspend fun insertGameRecord(record: GameRecord): Long

    /**
     * Get all game records, ordered by most recent first.
     */
    @Query("SELECT * FROM game_records ORDER BY timestamp DESC")
    fun getAllGameRecords(): Flow<List<GameRecord>>

    /**
     * Get all game records as a one-time query (not a Flow).
     */
    @Query("SELECT * FROM game_records ORDER BY timestamp DESC")
    suspend fun getAllGameRecordsOnce(): List<GameRecord>

    /**
     * Get only won games, ordered by most recent first.
     */
    @Query("SELECT * FROM game_records WHERE isWin = 1 ORDER BY timestamp DESC")
    fun getWonGameRecords(): Flow<List<GameRecord>>

    /**
     * Get only lost games, ordered by most recent first.
     */
    @Query("SELECT * FROM game_records WHERE isWin = 0 ORDER BY timestamp DESC")
    fun getLostGameRecords(): Flow<List<GameRecord>>

    /**
     * Get the count of all games played.
     */
    @Query("SELECT COUNT(*) FROM game_records")
    fun getTotalGamesPlayed(): Flow<Int>

    /**
     * Get the count of games won.
     */
    @Query("SELECT COUNT(*) FROM game_records WHERE isWin = 1")
    fun getTotalGamesWon(): Flow<Int>

    /**
     * Get the highest score ever recorded.
     */
    @Query("SELECT MAX(score) FROM game_records")
    fun getHighestScore(): Flow<Int?>

    /**
     * Get the average score across all games.
     */
    @Query("SELECT AVG(score) FROM game_records")
    fun getAverageScore(): Flow<Double?>

    /**
     * Get the average game duration in milliseconds.
     */
    @Query("SELECT AVG(timeMs) FROM game_records")
    fun getAverageTimeMs(): Flow<Long?>

    /**
     * Delete a specific game record.
     */
    @Delete
    suspend fun deleteGameRecord(record: GameRecord)

    /**
     * Delete all game records (for reset stats functionality).
     */
    @Query("DELETE FROM game_records")
    suspend fun deleteAllGameRecords()

    /**
     * Get games played within a date range.
     */
    @Query("SELECT * FROM game_records WHERE timestamp BETWEEN :startTime AND :endTime ORDER BY timestamp DESC")
    suspend fun getGameRecordsBetween(startTime: Long, endTime: Long): List<GameRecord>
}

