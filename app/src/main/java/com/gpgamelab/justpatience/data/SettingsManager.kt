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
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.runBlocking
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
        val TOTAL_GEMS = intPreferencesKey("total_gems")

        // Game Play Settings
        val DRAW_SIZE = intPreferencesKey("draw_size")                          // 1 or 3
        val RECYCLE_COUNT = intPreferencesKey("recycle_count")                  // 0..99
        val INFINITE_RECYCLES = booleanPreferencesKey("infinite_recycles")
        val SHOW_CARD_ANIMATIONS = booleanPreferencesKey("show_card_animations")
        val SHOW_WIN_ANIMATION = booleanPreferencesKey("show_win_animation")
        val ALLOW_FOUNDATION_TO_TABLEAU_DRAG = booleanPreferencesKey("allow_foundation_to_tableau_drag")
        val PREMIUM_ACCT = booleanPreferencesKey("premium_acct")
        val SHOW_GAME_TIMER = booleanPreferencesKey("show_game_timer")
        val SHOW_SCORE = booleanPreferencesKey("show_score")
        val SHOW_MOVES = booleanPreferencesKey("show_moves")
        val SHOW_HINTS = booleanPreferencesKey("show_hints")
        val HINT_DELAY_SECONDS = intPreferencesKey("hint_delay_seconds")
        val AUTO_COMPLETE = booleanPreferencesKey("auto_complete")
        val HAPTICS = booleanPreferencesKey("haptics")
        val TAP_TO_MOVE = booleanPreferencesKey("tap_to_move")
        val FULL_SCREEN = booleanPreferencesKey("full_screen")
        val MUTE_MUSIC = booleanPreferencesKey("mute_music")
        val MUTE_CARD_SOUND = booleanPreferencesKey("mute_card_sound")
        val MUTE_WIN_SOUND = booleanPreferencesKey("mute_win_sound")
        val ORIENTATION_LOCK = stringPreferencesKey("orientation_lock")         // "device", "portrait", "landscape"
        val BOARD_LAYOUT = stringPreferencesKey("board_layout")                 // "right_hand", "left_hand"
        val SCORE_METHOD = stringPreferencesKey("score_method")                 // "windows", "vegas", "vegas_cumulative", "completion"
        val PLAYER_DISPLAY_NAME = stringPreferencesKey("player_display_name")

        // Session tracking: true while an IN_PROGRESS game is saved; false after normal game end.
        // If still true on next cold start it means the process was killed mid-session.
        val GAME_SESSION_ACTIVE = booleanPreferencesKey("game_session_active")
    }

    /**
     * All user-configurable game-play and display settings.
     */
    data class GamePlaySettings(
        val drawSize: Int = 3,
        val recycleCount: Int = 3,
        val infiniteRecycles: Boolean = true,
        val showHints: Boolean = true,
        val hintDelaySeconds: Int = 5,
        val showCardAnimations: Boolean = true,
        val showWinAnimation: Boolean = true,
        val allowFoundationToTableauDrag: Boolean = false,
        val premiumAcct: Boolean = false,
        val showGameTimer: Boolean = true,
        val showScore: Boolean = true,
        val showMoves: Boolean = true,
        val muteMusic: Boolean = false,
        val muteCardSound: Boolean = false,
        val muteWinSound: Boolean = false,
        val autoComplete: Boolean = true,
        val haptics: Boolean = false,
        val tapToMove: Boolean = true,
        val fullScreen: Boolean = false,
        val orientationLock: String = "device",
        val boardLayout: String = "right_hand",
        val scoreMethod: String = "windows",
        val playerDisplayName: String = ""
    )

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
            preferences[PreferencesKeys.GAMES_PLAYED] =
                (preferences[PreferencesKeys.GAMES_PLAYED] ?: 0) + 1
            preferences[PreferencesKeys.GAMES_WON] =
                (preferences[PreferencesKeys.GAMES_WON] ?: 0) + 1
            preferences[PreferencesKeys.TOTAL_SCORE] =
                (preferences[PreferencesKeys.TOTAL_SCORE] ?: 0) + score

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
            preferences[PreferencesKeys.GAMES_PLAYED] =
                (preferences[PreferencesKeys.GAMES_PLAYED] ?: 0) + 1
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
            if (exception is IOException) {
                emit(null)
            } else {
                throw exception
            }
        }

    suspend fun clearSavedGame() {
        dataStore.edit { preferences ->
            preferences.remove(PreferencesKeys.SAVED_GAME_STATE_JSON)
        }
    }

    fun getTotalGemsFlow(): Flow<Int> = dataStore.data
        .map { preferences ->
            (preferences[PreferencesKeys.TOTAL_GEMS] ?: 0).coerceAtLeast(0)
        }
        .catch { exception ->
            if (exception is IOException) {
                emit(0)
            } else {
                throw exception
            }
        }

    suspend fun setTotalGems(total: Int) {
        dataStore.edit { preferences ->
            preferences[PreferencesKeys.TOTAL_GEMS] = total.coerceAtLeast(0)
        }
    }

    // --- Session-Active Flag ---

    /**
     * Synchronously checks whether a game session was still active when the process last stopped.
     * Intended for use in ViewModel.init() only; a brief runBlocking read is acceptable here
     * because DataStore dispatches I/O on its own thread and won't deadlock the main thread.
     *
     * Returns true  → the process was killed while an IN_PROGRESS game was saved
     *         false → the game ended normally or no session had been started yet
     */
    fun isGameSessionActive(): Boolean = runBlocking {
        try {
            dataStore.data.first()[PreferencesKeys.GAME_SESSION_ACTIVE] ?: false
        } catch (e: Exception) {
            false
        }
    }

    /**
     * Sets or clears the game-session-active flag.
     *   true  → an IN_PROGRESS game is currently saved (set each time the game is persisted)
     *   false → the game ended (win / recorded loss / explicit user exit)
     */
    suspend fun setGameSessionActive(active: Boolean) {
        dataStore.edit { prefs ->
            prefs[PreferencesKeys.GAME_SESSION_ACTIVE] = active
        }
    }

    fun getGameSessionActiveFlow(): Flow<Boolean> = dataStore.data
        .map { prefs -> prefs[PreferencesKeys.GAME_SESSION_ACTIVE] ?: false }
        .catch { exception ->
            if (exception is IOException) {
                emit(false)
            } else {
                throw exception
            }
        }

    // --- Game Play Settings Flow ---

    val gamePlaySettingsFlow: Flow<GamePlaySettings> = dataStore.data
        .catch { exception ->
            if (exception is IOException) {
                Log.e("SettingsManager", "Error reading game play settings.", exception)
                emit(androidx.datastore.preferences.core.emptyPreferences())
            } else {
                throw exception
            }
        }
        .map { preferences ->
            GamePlaySettings(
                drawSize = preferences[PreferencesKeys.DRAW_SIZE] ?: 3,
                recycleCount = preferences[PreferencesKeys.RECYCLE_COUNT] ?: 3,
                infiniteRecycles = preferences[PreferencesKeys.INFINITE_RECYCLES] ?: true,
                showHints = preferences[PreferencesKeys.SHOW_HINTS] ?: true,
                hintDelaySeconds = preferences[PreferencesKeys.HINT_DELAY_SECONDS] ?: 5,
                showCardAnimations = preferences[PreferencesKeys.SHOW_CARD_ANIMATIONS] ?: true,
                showWinAnimation = preferences[PreferencesKeys.SHOW_WIN_ANIMATION] ?: true,
                allowFoundationToTableauDrag = preferences[PreferencesKeys.ALLOW_FOUNDATION_TO_TABLEAU_DRAG] ?: false,
                premiumAcct = preferences[PreferencesKeys.PREMIUM_ACCT] ?: false,
                showGameTimer = preferences[PreferencesKeys.SHOW_GAME_TIMER] ?: true,
                showScore = preferences[PreferencesKeys.SHOW_SCORE] ?: true,
                showMoves = preferences[PreferencesKeys.SHOW_MOVES] ?: true,
                muteMusic = preferences[PreferencesKeys.MUTE_MUSIC] ?: false,
                muteCardSound = preferences[PreferencesKeys.MUTE_CARD_SOUND] ?: false,
                muteWinSound = preferences[PreferencesKeys.MUTE_WIN_SOUND] ?: false,
                autoComplete = preferences[PreferencesKeys.AUTO_COMPLETE] ?: true,
                haptics = preferences[PreferencesKeys.HAPTICS] ?: false,
                tapToMove = preferences[PreferencesKeys.TAP_TO_MOVE] ?: true,
                fullScreen = preferences[PreferencesKeys.FULL_SCREEN] ?: false,
                orientationLock = preferences[PreferencesKeys.ORIENTATION_LOCK] ?: "device",
                boardLayout = preferences[PreferencesKeys.BOARD_LAYOUT] ?: "right_hand",
                scoreMethod = preferences[PreferencesKeys.SCORE_METHOD] ?: "windows",
                playerDisplayName = preferences[PreferencesKeys.PLAYER_DISPLAY_NAME] ?: ""
            )
        }

    suspend fun saveGamePlaySettings(settings: GamePlaySettings) {
        dataStore.edit { preferences ->
            preferences[PreferencesKeys.DRAW_SIZE] = settings.drawSize
            preferences[PreferencesKeys.RECYCLE_COUNT] = settings.recycleCount
            preferences[PreferencesKeys.INFINITE_RECYCLES] = settings.infiniteRecycles
            preferences[PreferencesKeys.SHOW_HINTS] = settings.showHints
            preferences[PreferencesKeys.HINT_DELAY_SECONDS] = settings.hintDelaySeconds
            preferences[PreferencesKeys.SHOW_CARD_ANIMATIONS] = settings.showCardAnimations
            preferences[PreferencesKeys.SHOW_WIN_ANIMATION] = settings.showWinAnimation
            preferences[PreferencesKeys.ALLOW_FOUNDATION_TO_TABLEAU_DRAG] = settings.allowFoundationToTableauDrag
            preferences[PreferencesKeys.PREMIUM_ACCT] = settings.premiumAcct
            preferences[PreferencesKeys.SHOW_GAME_TIMER] = settings.showGameTimer
            preferences[PreferencesKeys.SHOW_SCORE] = settings.showScore
            preferences[PreferencesKeys.SHOW_MOVES] = settings.showMoves
            preferences[PreferencesKeys.MUTE_MUSIC] = settings.muteMusic
            preferences[PreferencesKeys.MUTE_CARD_SOUND] = settings.muteCardSound
            preferences[PreferencesKeys.MUTE_WIN_SOUND] = settings.muteWinSound
            preferences[PreferencesKeys.AUTO_COMPLETE] = settings.autoComplete
            preferences[PreferencesKeys.HAPTICS] = settings.haptics
            preferences[PreferencesKeys.TAP_TO_MOVE] = settings.tapToMove
            preferences[PreferencesKeys.FULL_SCREEN] = settings.fullScreen
            preferences[PreferencesKeys.ORIENTATION_LOCK] = settings.orientationLock
            preferences[PreferencesKeys.BOARD_LAYOUT] = settings.boardLayout
            preferences[PreferencesKeys.SCORE_METHOD] = settings.scoreMethod
            preferences[PreferencesKeys.PLAYER_DISPLAY_NAME] = settings.playerDisplayName
        }
    }

    suspend fun setPlayerDisplayName(displayName: String) {
        dataStore.edit { preferences ->
            preferences[PreferencesKeys.PLAYER_DISPLAY_NAME] = displayName.trim()
        }
    }
}