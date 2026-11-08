package com.example.data.auth

import android.content.Context
import com.example.BuildConfig
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

    data class WebFlowStarted(val authUrl: String) : AuthResult()
    data class Success(val accessToken: String) : AuthResult()
    data class Pending(val error: String) : AuthResult()
    data class Error(val message: String) : AuthResult()
    object Expired : AuthResult()
}

class GitHubAuthRepository(context: Context) {

    val tokenManager = AuthTokenManager(context)

    private val moshi = Moshi.Builder()
        .add(KotlinJsonAdapterFactory())
        .build()

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

    // ── Web Flow ──────────────────────────────────────────────────────────────

    fun buildWebAuthUrl(): AuthResult {
        if (clientId.isBlank()) {
            return AuthResult.Error("GITHUB_CLIENT_ID is not configured.")
        }
        val redirectUri = "https://provedoraxev3ds.vercel.app/api/auth/callback"
        val scope = "public_repo read:user workflow"
        val url = "https://github.com/login/oauth/authorize" +
                "?client_id=${clientId}" +
                "&redirect_uri=${android.net.Uri.encode(redirectUri)}" +
                "&scope=${android.net.Uri.encode(scope)}"
        return AuthResult.WebFlowStarted(url)
    }

    fun handleWebCallback(token: String): AuthResult {
        if (token.isBlank()) return AuthResult.Error("Token is empty")
        tokenManager.saveAccessToken(token)
        return AuthResult.Success(token)
    }

    // ── Device Flow (legado) ──────────────────────────────────────────────────

    suspend fun requestDeviceCode(): AuthResult {
        if (clientId.isBlank()) {
            return AuthResult.Error("GITHUB_CLIENT_ID is not configured.")
        }
        return try {
            val response = githubOAuthApi.requestDeviceCode(
                clientId = clientId,
                scope = "public_repo read:user workflow"
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

    // ── Utils ─────────────────────────────────────────────────────────────────

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
}
