package com.example.data.repository

import android.content.Context
import android.util.Log
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import androidx.security.crypto.EncryptedSharedPreferences
import androidx.security.crypto.MasterKey
import com.example.data.api.CreateRepoRequest
import com.example.data.api.GitHubApi
import com.example.data.api.GitHubUser
import com.example.data.api.GitHubRepo
import com.example.data.api.WorkflowRun
import com.example.data.api.WorkflowDispatchWithInputs
import com.example.data.models.CharacterConfig
import com.squareup.moshi.Moshi
import com.squareup.moshi.kotlin.reflect.KotlinJsonAdapterFactory
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import retrofit2.Retrofit
import retrofit2.converter.moshi.MoshiConverterFactory

private val Context.dataStore by preferencesDataStore(name = "github_prefs")

class GitHubRepository(private val context: Context) {

    companion object {
        private const val TAG = "GitHubRepository"
        private const val PREFS_FILE = "axev_secure_prefs"
        private const val KEY_TOKEN = "github_access_token"
    }

    private val moshi = Moshi.Builder()
        .add(KotlinJsonAdapterFactory())
        .build()

    private val api: GitHubApi = Retrofit.Builder()
        .baseUrl("https://api.github.com/")
        .addConverterFactory(MoshiConverterFactory.create(moshi))
        .build()
        .create(GitHubApi::class.java)

    // ── DataStore (apenas para dados não-sensíveis) ───────────────────────────

    private val SELECTED_REPO_KEY = stringPreferencesKey("selected_repo")
    private val SELECTED_REPO_URL_KEY = stringPreferencesKey("selected_repo_url")
    private val TOKEN_SAVED_KEY = stringPreferencesKey("token_saved_flag")

    val selectedRepoFlow: Flow<String?> = context.dataStore.data.map { it[SELECTED_REPO_KEY] }
    val selectedRepoUrlFlow: Flow<String?> = context.dataStore.data.map { it[SELECTED_REPO_URL_KEY] }

    // Flag que indica se há token guardado (sem expor o token em si)
    val tokenFlow: Flow<String?> = context.dataStore.data.map { it[TOKEN_SAVED_KEY] }

    // ── EncryptedSharedPreferences (para o token) ─────────────────────────────

    private val encryptedPrefs by lazy {
        try {
            val masterKey = MasterKey.Builder(context)
                .setKeyScheme(MasterKey.KeyScheme.AES256_GCM)
                .build()
            EncryptedSharedPreferences.create(
                context,
                PREFS_FILE,
                masterKey,
                EncryptedSharedPreferences.PrefKeyEncryptionScheme.AES256_SIV,
                EncryptedSharedPreferences.PrefValueEncryptionScheme.AES256_GCM
            )
        } catch (e: Exception) {
            Log.e(TAG, "Erro ao criar EncryptedSharedPreferences", e)
            null
        }
    }

    fun getSecureToken(): String? {
        return encryptedPrefs?.getString(KEY_TOKEN, null)
    }

    suspend fun saveToken(token: String) {
        // Guardar o token encriptado
        encryptedPrefs?.edit()?.putString(KEY_TOKEN, token)?.apply()
        // Guardar flag no DataStore (não o token)
        context.dataStore.edit { it[TOKEN_SAVED_KEY] = "true" }
    }

    suspend fun saveRepo(repoFullName: String, repoUrl: String = "") {
        context.dataStore.edit {
            it[SELECTED_REPO_KEY] = repoFullName
            if (repoUrl.isNotBlank()) it[SELECTED_REPO_URL_KEY] = repoUrl
        }
    }

    suspend fun clearSession() {
        encryptedPrefs?.edit()?.remove(KEY_TOKEN)?.apply()
        context.dataStore.edit {
            it.remove(TOKEN_SAVED_KEY)
            it.remove(SELECTED_REPO_KEY)
            it.remove(SELECTED_REPO_URL_KEY)
        }
    }

    // ── API calls ─────────────────────────────────────────────────────────────

    suspend fun getUser(token: String): GitHubUser? {
        return try {
            val response = api.getUser("Bearer $token")
            if (response.isSuccessful) response.body() else null
        } catch (e: Exception) {
            Log.e(TAG, "Erro ao obter utilizador", e)
            null
        }
    }

    suspend fun getRepos(token: String): List<GitHubRepo> {
        return try {
            val response = api.getRepos("Bearer $token")
            if (response.isSuccessful) response.body() ?: emptyList() else emptyList()
        } catch (e: Exception) {
            Log.e(TAG, "Erro ao obter repositórios", e)
            emptyList()
        }
    }

    /**
     * Criar repositório público na conta do utilizador autenticado.
     * O repositório é criado na conta de quem fez login — NÃO na conta do developer.
     */
    suspend fun createRepository(
        token: String,
        name: String,
        description: String
    ): Result<GitHubRepo> {
        return try {
            val request = CreateRepoRequest(
                name = name,
                description = description,
                private = false,
                auto_init = true
            )
            val response = api.createRepo("Bearer $token", request)
            if (response.isSuccessful) {
                val repo = response.body()
                if (repo != null) {
                    Result.success(repo)
                } else {
                    Result.failure(Exception("Resposta vazia do servidor"))
                }
            } else {
                val errorCode = response.code()
                val errorBody = response.errorBody()?.string() ?: ""
                when (errorCode) {
                    401 -> Result.failure(Exception("Token inválido ou expirado. Faz login novamente."))
                    403 -> Result.failure(Exception("Sem permissão para criar repositórios."))
                    422 -> Result.failure(Exception("Nome de repositório já existe ou inválido."))
                    else -> Result.failure(Exception("Erro $errorCode: $errorBody"))
                }
            }
        } catch (e: Exception) {
            Log.e(TAG, "Excepção ao criar repositório", e)
            Result.failure(e)
        }
    }

    suspend fun generateCharacter(
        token: String,
        repoFullName: String,
        config: CharacterConfig
    ): Boolean {
        val parts = repoFullName.split("/")
        if (parts.size != 2) return false
        val owner = parts[0]
        val repo = parts[1]
        val inputs = mapOf(
            "character_id" to config.id,
            "character_name" to config.name,
            "preset" to config.identity.category,
            "quality" to config.style.quality,
            "export_formats" to config.export.formats.joinToString(","),
            "config_json" to moshi.adapter(CharacterConfig::class.java).toJson(config)
        )
        val request = WorkflowDispatchWithInputs(ref = "main", inputs = inputs)
        return try {
            val response = api.dispatchWorkflow("Bearer $token", owner, repo, "ara-axev-crime.yml", request)
            response.isSuccessful
        } catch (e: Exception) {
            Log.e(TAG, "Erro ao disparar workflow", e)
            false
        }
    }

    suspend fun getWorkflowRuns(token: String, repoFullName: String): List<WorkflowRun> {
        val parts = repoFullName.split("/")
        if (parts.size != 2) return emptyList()
        return try {
            val response = api.getWorkflowRuns("Bearer $token", parts[0], parts[1])
            if (response.isSuccessful) response.body()?.workflow_runs ?: emptyList() else emptyList()
        } catch (e: Exception) {
            Log.e(TAG, "Erro ao obter workflow runs", e)
            emptyList()
        }
    }
}
