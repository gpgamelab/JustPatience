package com.gpgamelab.justpatience.api

import retrofit2.http.*
import retrofit2.Response

/**
 * Retrofit Interface for all API communication with the Just Patience FastAPI backend.
 * * Each function maps directly to an endpoint defined in the API Specification.
 * We use the 'suspend' keyword to integrate with Kotlin Coroutines for safe
 * asynchronous network calls.
 */
interface JustPatienceApiService {

    // --- 1. Authentication and User Management ---

    /**
     * POST /auth/register
     * Register a new user account.
     */
    @POST("auth/register")
    suspend fun registerUser(
        @Body request: UserRegistrationRequest
    ): Response<AuthResponse>

    /**
     * POST /auth/login
     * Authenticate a user and receive an AuthResponse containing the token.
     */
    @POST("auth/login")
    suspend fun loginUser(
        @Body request: UserLoginRequest
    ): Response<AuthResponse>

    /**
     * GET /user/me
     * Retrieve the current authenticated user's profile and lifetime statistics.
     * Requires Authorization header set by an Interceptor or manually.
     */
    @GET("user/me")
    suspend fun getUserProfile(
        @Header("Authorization") authToken: String
    ): Response<AuthResponse>


    // --- 2. Game Data and Scoring ---

    /**
     * POST /game/result
     * Submit the result of a single game (win or loss) to update global and user stats.
     */
    @POST("game/result")
    suspend fun postGameResult(
        @Header("Authorization") authToken: String,
        @Body request: PostGameResultRequest
    ): Response<StatusResponse>

    /**
     * GET /leaderboard
     * Retrieve the global leaderboard, optionally filtered by game style and decks.
     */
    @GET("leaderboard")
    suspend fun getLeaderboard(
        @Query("style") style: String,
        @Query("decks") decks: Int,
        @Query("limit") limit: Int? = 100 // Optional parameter
    ): Response<LeaderboardResponse>


    // --- 3. Logs and Telemetry ---

    /**
     * POST /log
     * Send application logs and telemetry data.
     * Note: Authorization header is optional for this endpoint per the spec.
     */
    @POST("log")
    suspend fun postAppLog(
        // @Header("Authorization") authToken: String? = null, // Can be sent anonymously
        @Body request: AppLogRequest
    ): Response<StatusResponse>


    // --- 4. Monetization (In-App Purchase) ---

    /**
     * POST /iap/verify
     * Verify a purchase token received from Google Play.
     */
    @POST("iap/verify")
    suspend fun verifyIAP(
        @Header("Authorization") authToken: String,
        @Body request: VerifyIAPRequest
    ): Response<VerifyIAPResponse>
}