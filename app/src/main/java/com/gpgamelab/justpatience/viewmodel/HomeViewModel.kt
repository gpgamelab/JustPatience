package com.gpgamelab.justpatience.viewmodel

import android.app.Application
import android.util.Log
import androidx.lifecycle.AndroidViewModel
import com.gpgamelab.justpatience.data.GameStatsManager
import com.gpgamelab.justpatience.data.SettingsManager
import com.gpgamelab.justpatience.data.TokenManager
import com.gpgamelab.justpatience.repository.GameRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.map
import java.util.Locale

private const val TAG = "HomeViewModel"

/**
 * ViewModel for the Home/Landing page.
 * Displays quick stats and game status.
 */
class HomeViewModel(application: Application) : AndroidViewModel(application) {

    private var statsManager: GameStatsManager? = null
    private var repository: GameRepository? = null

    init {
        try {
            val settingsManager = SettingsManager(application.applicationContext)
            val tokenManager = TokenManager(application.applicationContext)
            repository = GameRepository(settingsManager, tokenManager)
            statsManager = GameStatsManager(application.applicationContext)
        } catch (e: Exception) {
            Log.e(TAG, "Failed to initialize managers", e)
        }
    }

    // Expose stats as Flows with actual data
    val totalGamesPlayed: Flow<Int> = statsManager?.getTotalGamesPlayed()
        ?: kotlinx.coroutines.flow.flowOf(0)

    val totalGamesWon: Flow<Int> = statsManager?.getTotalGamesWon()
        ?: kotlinx.coroutines.flow.flowOf(0)

    val highestScore: Flow<Int?> = statsManager?.getHighestScore()
        ?: kotlinx.coroutines.flow.flowOf(null)

    /**
     * Computed win rate as a Flow combining wins and total games.
     */
    val winRate: Flow<Double> = statsManager?.let { sm ->
        combine(
            sm.getTotalGamesWon(),
            sm.getTotalGamesPlayed()
        ) { wins, total ->
            if (total == 0) 0.0 else (wins.toDouble() / total) * 100
        }
    } ?: kotlinx.coroutines.flow.flowOf(0.0)

    /**
     * Check if there's a game in progress that can be resumed.
     */
    val hasGameInProgress: Flow<Boolean> = repository?.getCurrentGameState()
        ?.map { !it.isNullOrEmpty() }
        ?: kotlinx.coroutines.flow.flowOf(false)

    /**
     * Format the last game summary as a readable string.
     * Example: "Last game: 42 moves, 3:45, Lost"
     */
    val lastGameSummary: Flow<String?> = statsManager?.getAllGameRecords()
        ?.map { records ->
            records.firstOrNull()?.let { lastGame ->
                val status = if (lastGame.isWin) "Won" else "Lost"
                val time = formatTime(lastGame.timeMs)
                "Last game: ${lastGame.moves} moves, $time, $status"
            }
        }
        ?: kotlinx.coroutines.flow.flowOf(null)

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







