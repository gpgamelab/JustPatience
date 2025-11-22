package com.gpgamelab.justpatience.viewmodel

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.viewModelScope
import com.gpgamelab.justpatience.api.AuthResponse
import com.gpgamelab.justpatience.api.UserLoginRequest
import com.gpgamelab.justpatience.api.UserRegistrationRequest
import com.gpgamelab.justpatience.data.TokenManager
import com.gpgamelab.justpatience.repository.AuthRepository
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

/**
 * ViewModel responsible for handling authentication state, exposing network results to the UI,
 * and communicating with the AuthRepository.
 */
class AuthViewModel(application: Application) : AndroidViewModel(application) {

    // --- Dependencies ---
    // In a real app, these would be injected (e.g., using Hilt/Koin).
    // For now, we manually create them using the Application context.
    private val tokenManager = TokenManager(application.applicationContext)
    private val repository = AuthRepository(tokenManager)


    // --- UI State Variables ---

    /** Tracks the current loading status of a network request. */
    private val _isLoading = MutableLiveData(false)
    val isLoading: LiveData<Boolean> = _isLoading

    /** Holds any error messages resulting from a network request. */
    private val _error = MutableLiveData<String?>(null)
    val error: LiveData<String?> = _error

    /** Holds the result of a successful authentication attempt. */
    private val _authResult = MutableLiveData<AuthResponse?>(null)
    val authResult: LiveData<AuthResponse?> = _authResult


    // --- Global Auth State ---

    /** * StateFlow exposing the user's current authentication status (token presence).
     * The UI can use this to navigate immediately if a token exists.
     */
    val authToken: StateFlow<String?> = repository.getAuthTokenFlow()
        .stateIn(
            scope = viewModelScope,
            // Start collecting immediately and share the latest value with new subscribers
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = null
        )


    // --- Public Actions ---

    /**
     * Attempts to register a user.
     */
    fun register(request: UserRegistrationRequest) {
        // Clear previous state before starting
        _error.value = null
        _isLoading.value = true

        viewModelScope.launch {
            try {
                val result = repository.registerUser(request)
                _authResult.value = result // Success, token is now saved by repository
            } catch (e: Exception) {
                _error.value = e.message
            } finally {
                _isLoading.value = false
            }
        }
    }

    /**
     * Attempts to log in a user.
     */
    fun login(request: UserLoginRequest) {
        // Clear previous state before starting
        _error.value = null
        _isLoading.value = true

        viewModelScope.launch {
            try {
                val result = repository.loginUser(request)
                _authResult.value = result // Success, token is now saved by repository
            } catch (e: Exception) {
                _error.value = e.message
            } finally {
                _isLoading.value = false
            }
        }
    }

    /**
     * Logs the user out by clearing the token.
     */
    fun logout() {
        viewModelScope.launch {
            repository.logout()
            // Clear any old UI state
            _authResult.value = null
            _error.value = null
        }
    }

}