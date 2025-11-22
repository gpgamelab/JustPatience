package com.gpgamelab.justpatience.repository

import com.gpgamelab.justpatience.api.ApiClient
import com.gpgamelab.justpatience.api.AuthResponse
import com.gpgamelab.justpatience.api.JustPatienceApiService
import com.gpgamelab.justpatience.api.UserLoginRequest
import com.gpgamelab.justpatience.api.UserRegistrationRequest
import com.gpgamelab.justpatience.data.TokenManager
import kotlinx.coroutines.flow.Flow

/**
 * Repository layer responsible for handling all user authentication-related data operations.
 * * This layer abstracts the network source (Retrofit API calls) from the ViewModel/UI layer,
 * and handles things like token storage and error conversion.
 */
class AuthRepository(
    // TokenManager is now a mandatory dependency for the repository
    private val tokenManager: TokenManager
) {

    // We now initialize the ApiClient when the repository is created
    init {
        // CRUCIAL: Initialize the ApiClient with the TokenManager so the AuthInterceptor
        // can access the saved token before making requests.
        ApiClient.initialize(tokenManager)
    }

    // We safely access the service, relying on the 'init' block above to guarantee initialization.
    private val apiService: JustPatienceApiService
        get() = ApiClient.service ?: throw IllegalStateException("API Client not initialized.")


    /**
     * Attempts to register a new user by calling POST /auth/register.
     * @return The AuthResponse containing the new user's ID and token on success.
     * @throws Exception if the network call fails or returns a non-200 HTTP code.
     */
    suspend fun registerUser(request: UserRegistrationRequest): AuthResponse {
        return try {
            // Note: apiService is accessed via the getter, ensuring it's initialized.
            val response = apiService.registerUser(request)

            if (response.isSuccessful && response.body() != null) {
                val authResponse = response.body()!!

                // --- SAVING THE TOKEN ---
                tokenManager.saveAuthToken(authResponse.authToken)

                authResponse
            } else {
                // Convert non-200 HTTP status codes into an application-level exception.
                val errorBody = response.errorBody()?.string() ?: "Unknown registration error"
                throw Exception("Registration failed: ${response.code()} - $errorBody")
            }
        } catch (e: Exception) {
            // Handle network-specific exceptions (e.g., DNS resolution, timeout).
            throw Exception("Network error during registration: ${e.message}")
        }
    }

    /**
     * Attempts to log in an existing user by calling POST /auth/login.
     * @return The AuthResponse containing the user's ID and token on success.
     * @throws Exception if the network call fails or returns a non-200 HTTP code.
     */
    suspend fun loginUser(request: UserLoginRequest): AuthResponse {
        return try {
            val response = apiService.loginUser(request)

            if (response.isSuccessful && response.body() != null) {
                val authResponse = response.body()!!

                // --- SAVING THE TOKEN ---
                tokenManager.saveAuthToken(authResponse.authToken)

                authResponse
            } else {
                // Convert non-200 HTTP status codes into an application-level exception.
                val errorBody = response.errorBody()?.string() ?: "Unknown login error"
                throw Exception("Login failed: ${response.code()} - $errorBody")
            }
        } catch (e: Exception) {
            // Handle network-specific exceptions.
            throw Exception("Network error during login: ${e.message}")
        }
    }

    // Future repository function to retrieve the current token for API calls
    fun getAuthTokenFlow(): Flow<String?> = tokenManager.getAuthToken

    /**
     * Clears the stored token for user logout.
     */
    suspend fun logout() {
        tokenManager.saveAuthToken(null)
    }

    // suspend fun getUserProfile(): AuthResponse { ... }
}