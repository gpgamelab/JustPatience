package com.gpgamelab.justpatience.viewmodel

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.gpgamelab.justpatience.data.GameRecord
import com.gpgamelab.justpatience.data.GameStatsManager
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.launch
import java.util.Locale

/**
 * ViewModel for displaying game statistics and history.
 */
class StatsViewModel(application: Application) : AndroidViewModel(application) {

    private val statsManager = GameStatsManager(application.applicationContext)

    // Expose stats as Flows for UI binding
    val totalGamesPlayed: Flow<Int> = statsManager.getTotalGamesPlayed()
    val totalGamesWon: Flow<Int> = statsManager.getTotalGamesWon()
    val highestScore: Flow<Int?> = statsManager.getHighestScore()
    val averageScore: Flow<Double?> = statsManager.getAverageScore()
    val averageTimeMs: Flow<Long?> = statsManager.getAverageTimeMs()

    // For detailed game history
    val allGameRecords: Flow<List<GameRecord>> = statsManager.getAllGameRecords()

    /**
     * Computed win rate as a Flow combining wins and total games.
     */
    val winRate: Flow<Double> = combine(
        statsManager.getTotalGamesWon(),
        statsManager.getTotalGamesPlayed()
    ) { wins, total ->
        if (total == 0) 0.0 else (wins.toDouble() / total) * 100
    }

    /**
     * Delete all stats and history.
     */
    fun resetAllStats() {
        viewModelScope.launch {
            statsManager.deleteAllGameRecords()
        }
    }

    /**
     * Format milliseconds into a readable time string (MM:SS).
     */
    fun formatTime(timeMs: Long): String {
        val seconds = timeMs / 1000
        val minutes = seconds / 60
        val secs = seconds % 60
        return String.format(Locale.getDefault(), "%02d:%02d", minutes, secs)
    }
}

