package com.example.data.auth

import android.content.Context
import com.example.BuildConfig
import com.example.data.api.AccessTokenResponse
import com.example.data.api.DeviceCodeResponse
import com.example.data.api.GitHubApi
import com.example.data.api.GitHubUser
import com.squareup.moshi.Moshi
import com.squareup.moshi.kotlin.reflect.KotlinJsonAdapterFactory
import kotlinx.coroutines.delay
import okhttp3.OkHttpClient
import okhttp3.logging.HttpLoggingInterceptor
import retrofit2.Retrofit
import retrofit2.converter.moshi.MoshiConverterFactory

sealed class AuthResult {
    data class DeviceCodeIssued(
        val userCode: String,
        val verificationUri: String,
        val deviceCode: String,
        val expiresIn: Int,
        val interval: Int
    ) : AuthResult()

    data class Success(val accessToken: String) : AuthResult()
    data class Pending(val error: String) : AuthResult()
    data class Error(val message: String) : AuthResult()
    object Expired : AuthResult()
}

/**
 * Handles GitHub Device Flow authentication.
 *
 * Flow:
 * 1. requestDeviceCode() → returns user_code + verification_uri to show in UI
 * 2. User opens browser, enters user_code on github.com/login/device
 * 3. pollForToken() → polls until user authorizes or code expires
 * 4. On success, token is saved securely via AuthTokenManager
 */
class GitHubAuthRepository(context: Context) {

    private val tokenManager = AuthTokenManager(context)

    private val moshi = Moshi.Builder()
        .add(KotlinJsonAdapterFactory())
        .build()

    // Device Flow uses github.com (not api.github.com)
    private val githubOAuthApi: GitHubApi by lazy {
        val logging = HttpLoggingInterceptor().apply {
            level = HttpLoggingInterceptor.Level.BODY
        }
        val client = OkHttpClient.Builder()
            .addInterceptor(logging)
            .addInterceptor { chain ->
                val request = chain.request().newBuilder()
                    .header("Accept", "application/json")
                    .build()
                chain.proceed(request)
            }
            .build()

        Retrofit.Builder()
            .baseUrl("https://github.com/")
            .client(client)
            .addConverterFactory(MoshiConverterFactory.create(moshi))
            .build()
            .create(GitHubApi::class.java)
    }

    private val githubApiClient: GitHubApi by lazy {
        val client = OkHttpClient.Builder().build()
        Retrofit.Builder()
            .baseUrl("https://api.github.com/")
            .client(client)
            .addConverterFactory(MoshiConverterFactory.create(moshi))
            .build()
            .create(GitHubApi::class.java)
    }

    private val clientId: String
        get() = BuildConfig.GITHUB_CLIENT_ID

    /**
     * Step 1: Request device code from GitHub.
     * Scopes requested: public_repo (create repos) + read:user (profile data)
     */
    suspend fun requestDeviceCode(): AuthResult {
        if (clientId.isBlank()) {
            return AuthResult.Error(
                "GITHUB_CLIENT_ID is not configured. " +
                "Add it to your .env file or Secrets panel."
            )
        }
        return try {
            val response = githubOAuthApi.requestDeviceCode(
                clientId = clientId,
                scope = "public_repo read:user"
            )
            val body = response.body()
            if (response.isSuccessful && body != null) {
                AuthResult.DeviceCodeIssued(
                    userCode = body.user_code,
                    verificationUri = body.verification_uri,
                    deviceCode = body.device_code,
                    expiresIn = body.expires_in,
                    interval = body.interval
                )
            } else {
                AuthResult.Error("Failed to request device code: HTTP ${response.code()}")
            }
        } catch (e: Exception) {
            AuthResult.Error("Network error: ${e.message}")
        }
    }

    /**
     * Step 2: Poll GitHub for the access token.
     * Respects the interval returned by GitHub (default 5s).
     * Returns AuthResult.Success, AuthResult.Pending, AuthResult.Expired, or AuthResult.Error.
     */
    suspend fun pollForToken(deviceCode: String, intervalSeconds: Int = 5): AuthResult {
        return try {
            delay(intervalSeconds * 1000L)
            val response = githubOAuthApi.pollAccessToken(
                clientId = clientId,
                deviceCode = deviceCode
            )
            val body = response.body()
            when {
                !response.isSuccessful -> AuthResult.Error("HTTP ${response.code()}")
                body?.access_token != null -> {
                    // Save token securely
                    tokenManager.saveAccessToken(body.access_token)
                    AuthResult.Success(body.access_token)
                }
                body?.error == "authorization_pending" -> AuthResult.Pending("authorization_pending")
                body?.error == "slow_down" -> AuthResult.Pending("slow_down")
                body?.error == "expired_token" -> AuthResult.Expired
                body?.error == "access_denied" -> AuthResult.Error("Access denied by user")
                else -> AuthResult.Pending(body?.error ?: "unknown")
            }
        } catch (e: Exception) {
            AuthResult.Error("Network error: ${e.message}")
        }
    }

    /**
     * Fetch authenticated user profile using saved token.
     */
    suspend fun fetchUser(): GitHubUser? {
        val bearer = tokenManager.getBearerToken() ?: return null
        return try {
            val response = githubApiClient.getUser(bearer)
            if (response.isSuccessful) response.body() else null
        } catch (e: Exception) {
            null
        }
    }

    fun isAuthenticated(): Boolean = tokenManager.hasToken()

    fun logout() = tokenManager.clearAll()

    fun getTokenManager(): AuthTokenManager = tokenManager
}
