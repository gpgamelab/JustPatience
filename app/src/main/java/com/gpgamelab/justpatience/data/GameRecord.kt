package com.gpgamelab.justpatience.data

import androidx.room.Entity
import androidx.room.PrimaryKey

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
    val moves: Int,
    val timeMs: Long,  // Game duration in milliseconds
    val isWin: Boolean,
    val timestamp: Long,  // When the game was completed (System.currentTimeMillis())
    val dateString: String,  // Human-readable date string (e.g., "2024-03-04 14:30:45")
    val cardsDraw: Int? = null  // Number of cards drawn at once (1, 3, or any integer). Null/0 defaults to 1.
)

