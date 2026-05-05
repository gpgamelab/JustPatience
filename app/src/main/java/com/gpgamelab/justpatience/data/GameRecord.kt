package com.gpgamelab.justpatience.data

import androidx.room.Entity
import androidx.room.PrimaryKey
import com.gpgamelab.justpatience.model.ScoreMethod

/**
 * Represents a single completed game record.
 * Stored in the local database for stats tracking and history.
 */
@Entity(tableName = "game_records")
data class GameRecord(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val playerName: String? = null,
    val score: Int,
    val windowsScore: Int? = null,
    val vegasScore: Int? = null,
    val vegasCumulativeScore: Int? = null,
    val completionPercentage: Int? = null,
    val moves: Int,
    val timeMs: Long,  // Game duration in milliseconds
    val isWin: Boolean,
    val timestamp: Long,  // When the game was completed (System.currentTimeMillis())
    val dateString: String,  // Human-readable date string (e.g., "2024-03-04 14:30:45")
    val cardsDraw: Int? = null,  // Number of cards drawn at once (1, 3, or any integer). Null/0 defaults to 1.
    val deckCount: Int? = null   // Number of decks used for this game (1 or 2). Null defaults to 1.
) {
    fun scoreForMethod(scoreMethod: String): Int {
        return when (ScoreMethod.normalize(scoreMethod)) {
            // Older records pre-dating the Vegas columns have null here.
            // Falling back to `score` (a Windows score) would be misleading, so show 0 instead.
            ScoreMethod.VEGAS -> vegasScore ?: 0
            ScoreMethod.VEGAS_CUMULATIVE -> vegasCumulativeScore ?: vegasScore ?: 0
            ScoreMethod.COMPLETION -> completionPercentage ?: 0
            else -> windowsScore ?: score
        }
    }
}

