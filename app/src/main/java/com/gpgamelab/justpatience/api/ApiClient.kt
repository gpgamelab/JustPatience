package com.gpgamelab.justpatience.api

import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory
import okhttp3.OkHttpClient
import okhttp3.logging.HttpLoggingInterceptor
import java.util.concurrent.TimeUnit
import com.gpgamelab.justpatience.data.TokenManager // Import for dependency

/**
 * Singleton object responsible for configuring and providing the Retrofit API service instance.
 * It must be initialized with the TokenManager before making authenticated requests.
 */
object ApiClient {

    private const val BASE_URL = "http://10.0.2.2:8000/api/v1/"

    // Default client, will be lazily initialized or initialized on first access
    private var okHttpClient: OkHttpClient? = null

    // We make 'service' nullable and lateinit because it depends on initialization
    @Volatile
    var service: JustPatienceApiService? = null
        private set // Private setter to enforce initialization via the function

    /**
     * Initializes the API client by providing the necessary TokenManager dependency.
     * This MUST be called once before any authenticated API calls are made.
     */
    fun initialize(tokenManager: TokenManager) {

        // 1. Setup logging interceptor for debugging network calls
        val loggingInterceptor = HttpLoggingInterceptor().apply {
            level = HttpLoggingInterceptor.Level.BODY
        }

        // 2. Setup the authentication interceptor
        val authInterceptor = AuthInterceptor(tokenManager)

        // 3. Create a base OkHttpClient for network configurations
        okHttpClient = OkHttpClient.Builder()
            .connectTimeout(30, TimeUnit.SECONDS)
            .readTimeout(30, TimeUnit.SECONDS)
            .writeTimeout(30, TimeUnit.SECONDS)
            // Add interceptors: Auth first, then logging (to see the header)
            .addInterceptor(authInterceptor)
            .addInterceptor(loggingInterceptor)
            .build()

        // 4. Configure the Retrofit instance
        val retrofit: Retrofit = Retrofit.Builder()
            .baseUrl(BASE_URL)
            .client(okHttpClient!!)
            .addConverterFactory(GsonConverterFactory.create())
            .build()

        // 5. Create the service instance
        service = retrofit.create(JustPatienceApiService::class.java)
    }

}