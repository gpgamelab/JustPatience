package com.gpgamelab.justpatience.viewmodel

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.gpgamelab.justpatience.data.SettingsManager
import com.gpgamelab.justpatience.data.TokenManager
import com.gpgamelab.justpatience.repository.GameRepository
import com.gpgamelab.justpatience.repository.UserData
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

/**
 * ViewModel responsible for providing combined game data (settings and stats)
 * to the SettingsActivity and handling user actions (toggles, reset).
 */
class SettingsViewModel(application: Application) : AndroidViewModel(application) {

    // Dependencies: Manually instantiated for simplicity.
    private val settingsManager = SettingsManager(application.applicationContext)
    private val tokenManager = TokenManager(application.applicationContext)

    // Instantiate the repository with its required dependencies
    private val repository = GameRepository(settingsManager, tokenManager)

    /**
     * StateFlow exposing all combined user data (settings, stats, login status) to the UI.
     * Starts collecting immediately and shares the latest value.
     */
    val userData: StateFlow<UserData?> = repository.userDataFlow
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = null
        )

    /**
     * Public accessor for settings, mapped from userData for convenience.
     */
    val userSettings = userData.map { it?.settings }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000),
        initialValue = null
    )

    /**
     * Public accessor for stats, mapped from userData for convenience.
     */
    val userStats = userData.map { it?.stats }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000),
        initialValue = null
    )

    // --- Actions ---

    /** Toggles the sound setting. */
    fun toggleSound() = viewModelScope.launch {
        repository.toggleSound()
    }

    /** Toggles the hints setting. */
    fun toggleHints() = viewModelScope.launch {
        repository.toggleHints()
    }

//    /** Resets all persistent game statistics. */
//    fun resetStats() = viewModelScope.launch {
//        // This action should be guarded by a confirmation dialog in the UI.
//        repository.resetStats()
//    }

    // Implemented: Clears game statistics using SettingsManager
    fun resetStats() {
        viewModelScope.launch {
            // Clear local stats stored in Shared Preferences
            settingsManager.resetStats()
            // Note: If stats are also on the server, you would call a repository function here
            // (e.g., gameRepository.resetUserStats())
        }
    }

}