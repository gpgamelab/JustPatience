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

class SettingsViewModel(application: Application) : AndroidViewModel(application) {

    private val settingsManager = SettingsManager(application.applicationContext)
    private val tokenManager = TokenManager(application.applicationContext)
    private val repository = GameRepository(settingsManager, tokenManager)

    val userData: StateFlow<UserData?> = repository.userDataFlow
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = null
        )

    val userSettings = userData.map { it?.settings }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000),
        initialValue = null
    )

    val userStats = userData.map { it?.stats }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000),
        initialValue = null
    )

    /** Exposes game-play / display settings to the UI. */
    val gamePlaySettings: StateFlow<SettingsManager.GamePlaySettings> =
        settingsManager.gamePlaySettingsFlow
            .stateIn(
                scope = viewModelScope,
                started = SharingStarted.WhileSubscribed(5000),
                initialValue = SettingsManager.GamePlaySettings()
            )

    // --- Actions ---

    fun toggleSound() = viewModelScope.launch { repository.toggleSound() }
    fun toggleHints() = viewModelScope.launch { repository.toggleHints() }

    fun resetStats() {
        viewModelScope.launch { settingsManager.resetStats() }
    }

    /** Persists all game-play settings at once. */
    fun saveGamePlaySettings(settings: SettingsManager.GamePlaySettings) {
        viewModelScope.launch { settingsManager.saveGamePlaySettings(settings) }
    }
}

