package com.example.data.auth

import android.content.Context
import android.util.Log
import com.example.data.api.AccessTokenResponse
import com.example.data.api.DeviceCodeResponse
import com.example.data.api.GitHubUser
import com.squareup.moshi.Moshi
import com.squareup.moshi.kotlin.reflect.KotlinJsonAdapterFactory
import kotlinx.coroutines.delay
import okhttp3.FormBody
import okhttp3.OkHttpClient
import okhttp3.Request
import retrofit2.Retrofit
import retrofit2.converter.moshi.MoshiConverterFactory
import com.example.data.api.GitHubApi

/**
 * Gere o fluxo de autenticação GitHub Device Flow.
 *
 * Fluxo escolhido: GitHub Device Flow (RFC 8628)
 * - Não requer backend
 * - Não expõe Client Secret no APK
 * - Oficial e suportado pelo GitHub para apps nativas/CLI
 * - O utilizador autentica-se no browser da sua escolha
 * - A app faz polling até receber o token
 *
 * CONFIGURAÇÃO NECESSÁRIA:
 * 1. Ir a https://github.com/settings/developers
 * 2. "New GitHub App" OU "New OAuth App"
 * 3. Para Device Flow: usar GitHub App com "Device flow" activado
 *    OU OAuth App (Device flow está disponível para ambos)
 * 4. Em "Authorization callback URL": axev3d://callback
 * 5. Copiar o Client ID para GITHUB_CLIENT_ID no ficheiro .env
 * 6. NUNCA colocar o Client Secret no APK
 */
class GitHubAuthManager(private val context: Context) {

    companion object {
        private const val TAG = "GitHubAuthManager"
        // Client ID da GitHub App/OAuth App — configurado em .env / BuildConfig
        // Lido de BuildConfig.GITHUB_CLIENT_ID (injectado pelo Secrets Gradle Plugin)
        private const val GITHUB_CLIENT_ID_FIELD = "GITHUB_CLIENT_ID"
    }

    private val moshi = Moshi.Builder()
        .add(KotlinJsonAdapterFactory())
        .build()

    private val deviceFlowApi: GitHubApi = Retrofit.Builder()
        .baseUrl("https://github.com/")
        .addConverterFactory(MoshiConverterFactory.create(moshi))
        .build()
        .create(GitHubApi::class.java)

    private val githubApi: GitHubApi = Retrofit.Builder()
        .baseUrl("https://api.github.com/")
        .addConverterFactory(MoshiConverterFactory.create(moshi))
        .build()
        .create(GitHubApi::class.java)

    /**
     * Passo 1: Pedir código de dispositivo ao GitHub.
     * Devolve os dados para mostrar ao utilizador (user_code + verification_uri).
     */
    suspend fun requestDeviceCode(clientId: String): DeviceCodeResponse? {
        return try {
            val response = deviceFlowApi.requestDeviceCode(
                clientId = clientId,
                scope = "repo public_repo read:user"
            )
            if (response.isSuccessful) {
                response.body()
            } else {
                Log.e(TAG, "Erro ao pedir device code: ${response.code()} ${response.errorBody()?.string()}")
                null
            }
        } catch (e: Exception) {
            Log.e(TAG, "Excepção ao pedir device code", e)
            null
        }
    }

    /**
     * Passo 2: Fazer polling até o utilizador autorizar no browser.
     * Devolve o access_token quando pronto, ou null em caso de erro/timeout.
     */
    suspend fun pollForToken(
        clientId: String,
        deviceCode: String,
        intervalSeconds: Int,
        onWaiting: () -> Unit = {}
    ): String? {
        var attempts = 0
        val maxAttempts = 60 // 5 minutos com intervalo de 5s

        while (attempts < maxAttempts) {
            delay(intervalSeconds * 1000L)
            onWaiting()

            try {
                val response = deviceFlowApi.pollAccessToken(
                    clientId = clientId,
                    deviceCode = deviceCode
                )

                if (response.isSuccessful) {
                    val body = response.body()
                    when (body?.error) {
                        null -> {
                            // Sucesso!
                            if (!body?.access_token.isNullOrBlank()) {
                                return body!!.access_token
                            }
                        }
                        "authorization_pending" -> {
                            // Utilizador ainda não autorizou — continuar polling
                            Log.d(TAG, "Aguardando autorização... tentativa $attempts")
                        }
                        "slow_down" -> {
                            // GitHub pediu para abrandar
                            delay(5000)
                        }
                        "expired_token" -> {
                            Log.e(TAG, "Device code expirou")
                            return null
                        }
                        "access_denied" -> {
                            Log.e(TAG, "Utilizador negou acesso")
                            return null
                        }
                        else -> {
                            Log.e(TAG, "Erro desconhecido: ${body?.error} - ${body?.error_description}")
                            return null
                        }
                    }
                } else {
                    Log.e(TAG, "Erro HTTP no polling: ${response.code()}")
                }
            } catch (e: Exception) {
                Log.e(TAG, "Excepção no polling", e)
            }

            attempts++
        }

        Log.e(TAG, "Timeout no polling (60 tentativas)")
        return null
    }

    /**
     * Validar token e obter dados do utilizador.
     */
    suspend fun getUserFromToken(token: String): GitHubUser? {
        return try {
            val response = githubApi.getUser("Bearer $token")
            if (response.isSuccessful) response.body() else null
        } catch (e: Exception) {
            Log.e(TAG, "Erro ao obter utilizador", e)
            null
        }
    }
}
