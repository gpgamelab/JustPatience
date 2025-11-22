package com.gpgamelab.justpatience.data

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

/**
 * Creates a singleton DataStore instance associated with the application context.
 * The file name in device storage will be "auth_prefs".
 */
private val Context.dataStore: DataStore<Preferences> by preferencesDataStore(name = "auth_prefs")

/**
 * Manages the secure storage and retrieval of the JWT authentication token using DataStore.
 * * Note: While DataStore is modern and non-blocking, for maximum security of sensitive
 * data like JWTs, the EncryptedSharedPreferences (which uses Android Keystore) is
 * an alternative, but DataStore is generally sufficient for tokens in most game apps.
 */
class TokenManager(private val context: Context) {
    // Logic to handle user tokens (e.g., refreshing, storing) goes here
    fun getToken(): String = "dummy_token"

    // Key used to store the auth token in DataStore
    companion object {
        private val AUTH_TOKEN_KEY = stringPreferencesKey("auth_token")
    }

    /**
     * Saves the authentication token (JWT string) to the secure preferences.
     * The token is received upon successful login or registration.
     */
    suspend fun saveAuthToken(token: String?) {
        context.dataStore.edit { preferences ->
            if (token != null) {
                preferences[AUTH_TOKEN_KEY] = token
            } else {
                preferences.remove(AUTH_TOKEN_KEY) // Used for logging out
            }
        }
    }

    /**
     * Retrieves the authentication token as a Kotlin Flow, allowing the repository or
     * other components to react to token changes in real-time.
     */
    val getAuthToken: Flow<String?> = context.dataStore.data
        .map { preferences ->
            preferences[AUTH_TOKEN_KEY]
        }
}