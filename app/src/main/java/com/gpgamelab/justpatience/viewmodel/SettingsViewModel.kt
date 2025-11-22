package com.gpgamelab.justpatience.viewmodel

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.gpgamelab.justpatience.data.SettingsManager
import com.gpgamelab.justpatience.data.TokenManager
import com.gpgamelab.justpatience.repository.GameRepository
import com.gpgamelab.justpatience.repository.UserData
import com.google.firebase.firestore.FirebaseFirestore
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.map // Required import
import kotlinx.coroutines.flow.stateIn // Required import
import kotlinx.coroutines.launch

/**
 * ViewModel responsible for providing combined game data (settings and stats)
 * to the SettingsActivity and handling user actions (toggles, reset).
 */
class SettingsViewModel(application: Application) : AndroidViewModel(application) {

    // --- Dependencies ---
    private val settingsManager = SettingsManager(application.applicationContext)
    private val tokenManager = TokenManager(application.applicationContext)
    private val firestore = FirebaseFirestore.getInstance()
    // FIX: Ensure GameRepository constructor matches the one defined in GameRepository.kt
    private val repository = GameRepository(firestore, settingsManager, tokenManager)

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
     * FIX: Corrected the map function to resolve 'Cannot infer type' and 'Unresolved reference 'settings'' errors.
     */
    val userSettings = userData.map { userData: UserData? -> userData?.settings }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000),
        initialValue = null
    )

    /**
     * Public accessor for stats, mapped from userData for convenience.
     * FIX: Corrected the map function to resolve 'Cannot infer type' and 'Unresolved reference 'stats'' errors.
     */
    val userStats = userData.map { userData: UserData? -> userData?.stats }.stateIn(
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

    /** Resets all persistent game statistics. */
    fun resetStats() = viewModelScope.launch {
        repository.resetStats()
    }
}