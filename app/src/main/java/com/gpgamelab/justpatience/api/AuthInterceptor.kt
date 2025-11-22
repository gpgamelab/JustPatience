package com.gpgamelab.justpatience.api

import com.gpgamelab.justpatience.data.TokenManager
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import okhttp3.Interceptor
import okhttp3.Response

/**
 * An OkHttp Interceptor responsible for attaching the JWT Authentication token
 * to every request that requires it.
 *
 * @param tokenManager The manager used to retrieve the stored JWT token.
 */
class AuthInterceptor(private val tokenManager: TokenManager) : Interceptor {

    override fun intercept(chain: Interceptor.Chain): Response {
        // 1. Get the current token synchronously from the DataStore Flow.
        // We use runBlocking here because OkHttp Interceptors are synchronous,
        // but DataStore's reading mechanism is non-blocking internally.
        val token = runBlocking {
            tokenManager.getAuthToken.first()
        }

        val request = chain.request()
        val requestBuilder = request.newBuilder()

        // 2. Attach the Authorization header if a token exists.
        if (token != null) {
            // The API spec requires 'Bearer <token>' format
            requestBuilder.header("Authorization", "Bearer $token")
        }

        // 3. Proceed with the modified request.
        return chain.proceed(requestBuilder.build())
    }
}