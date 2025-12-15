package com.gpgamelab.justpatience.repository

import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.map
import com.gpgamelab.justpatience.data.SettingsManager
import com.gpgamelab.justpatience.data.SettingsManager.UserSettings // Import specific class from manager
import com.gpgamelab.justpatience.data.SettingsManager.UserStats // Import specific class from manager
import com.gpgamelab.justpatience.data.TokenManager

/**
 * Repository layer responsible for managing all non-authentication related user data,
 * including game settings, stats, and the current saved game state.
 *
 * This repository is the sole source of truth for local game data persistence.
 *
 * @param settingsManager The DataStore manager for persistent settings and stats.
 * @param tokenManager The DataStore manager for authentication status (used to determine if data is cleared).
 */
class GameRepository(
    // Removed FirebaseFirestore dependency to align with local persistence focus.
    private val settingsManager: SettingsManager,
    private val tokenManager: TokenManager
) {
    // --- Combined Data Flow ---

    /**
     * A combined Flow that merges the latest UserSettings, UserStats, and login status.
     * This provides a single, stateful object for the ViewModel to observe.
     */
    val userDataFlow: Flow<UserData> = combine(
        settingsManager.settingsFlow,
        settingsManager.statsFlow,
        tokenManager.getAuthToken.map { it != null } // Map token flow to a simple isLoggedIn boolean
    ) { settings, stats, isLoggedIn ->
        UserData(settings, stats, isLoggedIn)
    }.distinctUntilChanged() // Only emit when the contents of UserData actually change


    // --- Settings Actions ---

    /** Toggles the sound setting state. */
    suspend fun toggleSound() {
        settingsManager.toggleSound()
    }

    /** Toggles the hints setting state. */
    suspend fun toggleHints() {
        settingsManager.toggleHints()
    }

    // --- Statistics Actions ---

    /** Resets all persistent game statistics (games played, high score, etc.). */
    suspend fun resetStats() {
        settingsManager.resetStats()
    }

    /**
     * Updates statistics after a game is won.
     */
    suspend fun recordGameWin(score: Int) {
        settingsManager.recordGameWin(score)
    }

    /**
     * Updates statistics after a game is lost.
     */
    suspend fun recordGameLoss() {
        settingsManager.recordGameLoss()
    }

    // --- Saved Game Operations ---

    /**
     * Saves the current game state (e.g., the deck and stack positions)
     * as a serialized JSON string.
     * @param gameStateJson The serialized state of the current game.
     */
    suspend fun saveCurrentGameState(gameStateJson: String) {
        settingsManager.saveCurrentGameState(gameStateJson)
    }

    /**
     * Retrieves the current saved game state.
     * @return A flow emitting the saved game state JSON string, or null if none is saved.
     */
    fun getCurrentGameState(): Flow<String?> = settingsManager.getCurrentGameState()

    /**
     * Clears the current saved game state (e.g., when a game is finished or discarded).
     */
    suspend fun clearSavedGame() {
        settingsManager.clearSavedGame()
    }
}

// --- Data Transfer Object (DTO) ---

/**
 * A data class to combine all user settings and statistics for easy state consumption.
 * This prevents the ViewModel from having to subscribe to multiple DataStore flows.
 */
data class UserData(
    val settings: UserSettings,
    val stats: UserStats,
    val isLoggedIn: Boolean
)