package com.gpgamelab.justpatience.data

import android.content.Context
import android.util.Log
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.intPreferencesKey
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.map
import java.io.IOException

// --- File-Level DataStore Initialization ---
// This is the correct, recommended way to create a singleton DataStore instance in Kotlin.
// It uses a property delegate to handle the initialization and lifecycle management automatically.
private const val DATASTORE_NAME = "game_preferences"
private val Context.dataStore: DataStore<Preferences> by preferencesDataStore(
    name = DATASTORE_NAME
)

/**
 * Manages all game settings and statistics persistence using DataStore Preferences.
 */
class SettingsManager(private val context: Context) {
    // We now correctly access the singleton DataStore instance defined above.
    private val dataStore = context.dataStore
    private val gson = Gson()

    // --- DataStore Keys ---
    private object PreferencesKeys {
        // Settings
        val SOUND_ON = booleanPreferencesKey("sound_on")
        val HINTS_ON = booleanPreferencesKey("hints_on")
        val SFX_ENABLED = booleanPreferencesKey("sfx_enabled")
        val MUSIC_ENABLED = booleanPreferencesKey("music_enabled")

        // Stats
        val GAMES_PLAYED = intPreferencesKey("games_played")
        val GAMES_WON = intPreferencesKey("games_won")
        val TOTAL_SCORE = intPreferencesKey("total_score")
        val HIGH_SCORES_JSON = stringPreferencesKey("high_scores_json")

        // Saved Game State
        val SAVED_GAME_STATE_JSON = stringPreferencesKey("saved_game_state_json")
    }

    // --- Data Classes ---

    /**
     * Structure defining user game settings.
     */
    data class UserSettings(
        val isSoundOn: Boolean = true,
        val isHintsOn: Boolean = false,
        val sfxEnabled: Boolean = true,
        val musicEnabled: Boolean = true
    )

    /**
     * Structure defining user game statistics.
     */
    data class UserStats(
        val gamesPlayed: Int = 0,
        val gamesWon: Int = 0,
        val totalScore: Int = 0,
        val highScores: List<Int> = emptyList() // Example: top 10 scores
    )

    // --- Flowing Settings Data ---

    val settingsFlow: Flow<UserSettings> = dataStore.data
        .catch { exception ->
            // DataStore throws IOException on file corruption
            if (exception is IOException) {
                Log.e("SettingsManager", "Error reading preferences.", exception)
                // Emit empty preferences on failure so map can apply defaults
                emit(androidx.datastore.preferences.core.emptyPreferences())
            } else {
                throw exception
            }
        }
        .map { preferences ->
            UserSettings(
                isSoundOn = preferences[PreferencesKeys.SOUND_ON] ?: true,
                isHintsOn = preferences[PreferencesKeys.HINTS_ON] ?: false,
                sfxEnabled = preferences[PreferencesKeys.SFX_ENABLED] ?: true,
                musicEnabled = preferences[PreferencesKeys.MUSIC_ENABLED] ?: true
            )
        }

    // --- Flowing Stats Data ---

    val statsFlow: Flow<UserStats> = dataStore.data
        .catch { exception ->
            if (exception is IOException) {
                Log.e("SettingsManager", "Error reading stats.", exception)
                emit(androidx.datastore.preferences.core.emptyPreferences())
            } else {
                throw exception
            }
        }
        .map { preferences ->
            val highScoresJson = preferences[PreferencesKeys.HIGH_SCORES_JSON] ?: "[]"
            val type = object : TypeToken<List<Int>>() {}.type
            val highScores = try {
                gson.fromJson<List<Int>>(highScoresJson, type) ?: emptyList()
            } catch (e: Exception) {
                Log.e("SettingsManager", "Error parsing high scores JSON.", e)
                emptyList<Int>()
            }

            UserStats(
                gamesPlayed = preferences[PreferencesKeys.GAMES_PLAYED] ?: 0,
                gamesWon = preferences[PreferencesKeys.GAMES_WON] ?: 0,
                totalScore = preferences[PreferencesKeys.TOTAL_SCORE] ?: 0,
                highScores = highScores.sortedDescending().take(10)
            )
        }

    // --- Settings Update Methods ---

    suspend fun toggleSound() {
        dataStore.edit { preferences ->
            val current = preferences[PreferencesKeys.SOUND_ON] ?: true
            preferences[PreferencesKeys.SOUND_ON] = !current
            // You might want to also toggle SFX and MUSIC here if SOUND_ON is meant as a master switch
        }
    }

    suspend fun toggleSfx(enabled: Boolean) {
        dataStore.edit { preferences ->
            preferences[PreferencesKeys.SFX_ENABLED] = enabled
        }
    }

    suspend fun toggleMusic(enabled: Boolean) {
        dataStore.edit { preferences ->
            preferences[PreferencesKeys.MUSIC_ENABLED] = enabled
        }
    }

    suspend fun toggleHints() {
        dataStore.edit { preferences ->
            val current = preferences[PreferencesKeys.HINTS_ON] ?: false
            preferences[PreferencesKeys.HINTS_ON] = !current
        }
    }

    // --- Stats Update Methods ---

    suspend fun resetStats() {
        dataStore.edit { preferences ->
            preferences[PreferencesKeys.GAMES_PLAYED] = 0
            preferences[PreferencesKeys.GAMES_WON] = 0
            preferences[PreferencesKeys.TOTAL_SCORE] = 0
            preferences[PreferencesKeys.HIGH_SCORES_JSON] = "[]"
        }
    }

    suspend fun recordGameWin(score: Int) {
        dataStore.edit { preferences ->
            // Update counts and total score
            preferences[PreferencesKeys.GAMES_PLAYED] = (preferences[PreferencesKeys.GAMES_PLAYED] ?: 0) + 1
            preferences[PreferencesKeys.GAMES_WON] = (preferences[PreferencesKeys.GAMES_WON] ?: 0) + 1
            preferences[PreferencesKeys.TOTAL_SCORE] = (preferences[PreferencesKeys.TOTAL_SCORE] ?: 0) + score

            // Update high scores (Deserialization/Serialization logic)
            val highScoresJson = preferences[PreferencesKeys.HIGH_SCORES_JSON] ?: "[]"
            val type = object : TypeToken<List<Int>>() {}.type
            val currentScores = try {
                gson.fromJson<MutableList<Int>>(highScoresJson, type) ?: mutableListOf()
            } catch (e: Exception) {
                mutableListOf<Int>()
            }

            currentScores.add(score)
            val newHighScores = currentScores.sortedDescending().take(10)

            preferences[PreferencesKeys.HIGH_SCORES_JSON] = gson.toJson(newHighScores)
        }
    }

    suspend fun recordGameLoss() {
        dataStore.edit { preferences ->
            preferences[PreferencesKeys.GAMES_PLAYED] = (preferences[PreferencesKeys.GAMES_PLAYED] ?: 0) + 1
        }
    }

    // --- Saved Game Methods ---

    suspend fun saveCurrentGameState(gameStateJson: String) {
        dataStore.edit { preferences ->
            preferences[PreferencesKeys.SAVED_GAME_STATE_JSON] = gameStateJson
        }
    }

    fun getCurrentGameState(): Flow<String?> = dataStore.data
        .map { preferences ->
            preferences[PreferencesKeys.SAVED_GAME_STATE_JSON]
        }
        .catch { exception ->
            if (exception is IOException) { emit(null) } else { throw exception }
        }

    suspend fun clearSavedGame() {
        dataStore.edit { preferences ->
            preferences.remove(PreferencesKeys.SAVED_GAME_STATE_JSON)
        }
    }
}