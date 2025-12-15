package com.gpgamelab.justpatience.api

import com.google.gson.annotations.SerializedName

// NOTE: We are using '@SerializedName' from Gson to map the API's snake_case
// fields (e.g., 'auth_token') to Kotlin's preferred camelCase properties.

// --- 1. Authentication and User Management ---

/**
 * Request body for POST /auth/register
 */
data class UserRegistrationRequest(
    @SerializedName("username") val username: String,
    @SerializedName("password") val password: String,
    @SerializedName("email") val email: String? = null
)

/**
 * Request body for POST /auth/login
 */
data class UserLoginRequest(
    @SerializedName("username") val username: String,
    @SerializedName("password") val password: String
)

/**
 * Response body for POST /auth/login and GET /user/me
 */
data class AuthResponse(
    @SerializedName("user_id") val userId: String,
    @SerializedName("username") val username: String,
    @SerializedName("auth_token") val authToken: String? = null, // Token only present on login/registration
    @SerializedName("iap_no_ads") val iapNoAds: Boolean,
    @SerializedName("stats_sync_token") val statsSyncToken: String? = null, // Sync token only present on login
    @SerializedName("lifetime_stats") val lifetimeStats: UserStats? = null // Detailed stats only present on GET /user/me
)

/**
 * Nested object for detailed user stats (part of GET /user/me response)
 */
data class UserStats(
    @SerializedName("games_played") val gamesPlayed: Int,
    @SerializedName("games_won") val gamesWon: Int,
    @SerializedName("win_streak") val winStreak: Int,
    @SerializedName("best_score_ms") val bestScoreMs: Long, // Using Long to safely store milliseconds
    @SerializedName("best_time_ms") val bestTimeMs: Long
)


// --- 2. Game Data and Scoring ---

/**
 * Request body for POST /game/result
 */
data class PostGameResultRequest(
    @SerializedName("score_style") val scoreStyle: String, // "VEGAS" or "MICROSOFT"
    @SerializedName("decks") val decks: Int, // 1, 2, or 3
    @SerializedName("draw_style") val drawStyle: Int, // 1 or 3
    @SerializedName("moves") val moves: Int,
    @SerializedName("time_ms") val timeMs: Long,
    @SerializedName("final_score") val finalScore: Int,
    @SerializedName("is_win") val isWin: Boolean,
    @SerializedName("start_seed") val startSeed: String // Game board identifier
)

/**
 * Generic success response for POST /game/result (and others)
 */
data class StatusResponse(
    @SerializedName("status") val status: String, // "success"
    @SerializedName("message") val message: String
)

/**
 * Nested object for a single leaderboard entry
 */
data class LeaderboardEntry(
    @SerializedName("cardRank") val rank: Int,
    @SerializedName("username") val username: String,
    @SerializedName("score") val score: Int,
    @SerializedName("time_ms") val timeMs: Long
)

/**
 * Response body for GET /leaderboard
 */
data class LeaderboardResponse(
    @SerializedName("leaderboard") val leaderboard: List<LeaderboardEntry>
)


// --- 3. Logs and Telemetry ---

/**
 * Request body for POST /log
 * Note: 'data' is modeled as a simple Map<String, Any> since its structure is dynamic (Object in spec)
 */
data class AppLogRequest(
    @SerializedName("log_level") val logLevel: String,
    @SerializedName("event_type") val eventType: String,
    @SerializedName("data") val data: Map<String, Any?> // Can hold detailed JSON data
)


// --- 4. Monetization (In-App Purchase) ---

/**
 * Request body for POST /iap/verify
 */
data class VerifyIAPRequest(
    @SerializedName("purchase_token") val purchaseToken: String,
    @SerializedName("product_id") val productId: String
)

/**
 * Response body for POST /iap/verify (Same as StatusResponse but includes iapNoAds status)
 */
data class VerifyIAPResponse(
    @SerializedName("status") val status: String,
    @SerializedName("message") val message: String,
    @SerializedName("iap_no_ads") val iapNoAds: Boolean
)