package com.gpgamelab.justpatience.data

import android.content.Context
import kotlinx.coroutines.flow.Flow
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

/**
 * Manager for recording and retrieving game statistics.
 * Acts as an intermediary between the ViewModel and the Room database.
 */
class GameStatsManager(context: Context) {

    private val dao = GameDatabase.getInstance(context).gameRecordDao()

    /**
     * Record a completed game in the database.
     * @param score The final score
     * @param moves The number of moves made
     * @param timeMs The game duration in milliseconds
     * @param isWin Whether the game was won
     */
    suspend fun recordGame(score: Int, moves: Int, timeMs: Long, isWin: Boolean) {
        val now = System.currentTimeMillis()
        val dateString = formatTimestamp(now)

        val record = GameRecord(
            score = score,
            moves = moves,
            timeMs = timeMs,
            isWin = isWin,
            timestamp = now,
            dateString = dateString
        )

        dao.insertGameRecord(record)
    }

    /**
     * Get all game records as a Flow.
     */
    fun getAllGameRecords(): Flow<List<GameRecord>> = dao.getAllGameRecords()

    /**
     * Get all game records once (not a Flow).
     */
    suspend fun getAllGameRecordsOnce(): List<GameRecord> = dao.getAllGameRecordsOnce()

    /**
     * Get won games only.
     */
    fun getWonGameRecords(): Flow<List<GameRecord>> = dao.getWonGameRecords()

    /**
     * Get lost games only.
     */
    fun getLostGameRecords(): Flow<List<GameRecord>> = dao.getLostGameRecords()

    /**
     * Get total games played.
     */
    fun getTotalGamesPlayed(): Flow<Int> = dao.getTotalGamesPlayed()

    /**
     * Get total games won.
     */
    fun getTotalGamesWon(): Flow<Int> = dao.getTotalGamesWon()

    /**
     * Get the highest score.
     */
    fun getHighestScore(): Flow<Int?> = dao.getHighestScore()

    /**
     * Get the average score.
     */
    fun getAverageScore(): Flow<Double?> = dao.getAverageScore()

    /**
     * Get the average game duration.
     */
    fun getAverageTimeMs(): Flow<Long?> = dao.getAverageTimeMs()

    /**
     * Delete all game records (typically called when resetting stats).
     */
    suspend fun deleteAllGameRecords() {
        dao.deleteAllGameRecords()
    }

    /**
     * Get games played within a date range.
     */
    suspend fun getGameRecordsBetween(startTime: Long, endTime: Long): List<GameRecord> {
        return dao.getGameRecordsBetween(startTime, endTime)
    }

    /**
     * Format a timestamp into a human-readable date string.
     */
    private fun formatTimestamp(timestamp: Long): String {
        val sdf = SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.getDefault())
        return sdf.format(Date(timestamp))
    }
}

