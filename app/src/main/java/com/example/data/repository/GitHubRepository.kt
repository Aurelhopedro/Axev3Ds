package com.example.data.repository

import android.content.Context
import com.example.data.api.CreateRepoRequest
import com.example.data.api.GitHubApi
import com.example.data.api.GitHubRepo
import com.example.data.api.GitHubUser
import com.example.data.api.WorkflowDispatchWithInputs
import com.example.data.api.WorkflowRun
import com.example.data.auth.AuthTokenManager
import com.example.data.github.RepositorySetupService
import com.example.data.github.SetupResult
import com.example.data.github.WorkflowTemplates
import com.example.data.models.CharacterConfig
import com.squareup.moshi.Moshi
import com.squareup.moshi.kotlin.reflect.KotlinJsonAdapterFactory
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import retrofit2.Retrofit
import retrofit2.converter.moshi.MoshiConverterFactory

sealed class RepoCreationResult {
    data class Success(val repo: GitHubRepo) : RepoCreationResult()
    data class AlreadyExists(val name: String) : RepoCreationResult()
    data class Error(val message: String) : RepoCreationResult()
}

class GitHubRepository(private val context: Context) {

    val tokenManager = AuthTokenManager(context)

    private val moshi = Moshi.Builder()
        .add(KotlinJsonAdapterFactory())
        .build()

    private val api: GitHubApi by lazy {
        Retrofit.Builder()
            .baseUrl("https://api.github.com/")
            .addConverterFactory(MoshiConverterFactory.create(moshi))
            .build()
            .create(GitHubApi::class.java)
    }

    private val setupService: RepositorySetupService by lazy {
        RepositorySetupService(api)
    }

    // ── Compatibility flows for MainViewModel ─────────────────────────────────

    val tokenFlow: Flow<String?> = MutableStateFlow(tokenManager.getBearerToken()).asStateFlow()

    val selectedRepoFlow: Flow<String?> = MutableStateFlow(tokenManager.getSelectedRepo()).asStateFlow()

    // ── Token / session helpers ───────────────────────────────────────────────

    suspend fun saveToken(token: String) {
        tokenManager.saveAccessToken(token.trim().removePrefix("Bearer ").removePrefix("bearer "))
    }

    suspend fun saveRepo(repoFullName: String) {
        tokenManager.saveSelectedRepo(repoFullName)
    }

    suspend fun clearSession() {
        tokenManager.clearAll()
    }

    // ── API calls ─────────────────────────────────────────────────────────────

    suspend fun getUser(token: String): GitHubUser? {
        val bearer = token.asBearerToken()
        return try {
            val response = api.getUser(bearer)
            if (response.isSuccessful) response.body() else null
        } catch (e: Exception) {
            null
        }
    }

    suspend fun getRepos(token: String): List<GitHubRepo> {
        val bearer = token.asBearerToken()
        return try {
            val response = api.getRepos(bearer)
            if (response.isSuccessful) response.body() ?: emptyList() else emptyList()
        } catch (e: Exception) {
            emptyList()
        }
    }

    /**
     * Cria um repositório público e injicta os dois workflows AXEV.
     * Delega toda a lógica de bootstrap para [RepositorySetupService].
     *
     * @return [SetupResult.Success] ou [SetupResult.AlreadyExisted] em caso de êxito;
     *         [SetupResult.Error] com o passo e mensagem em caso de falha.
     */
    suspend fun setupRepository(): SetupResult {
        val bearer = tokenManager.getBearerToken()
            ?: return SetupResult.Error("auth", "Não autenticado")
        val user = getUser(bearer)
            ?: return SetupResult.Error("auth", "Não foi possível obter o utilizador")
        return setupService.setupRepository(bearer, user.login)
    }

    /**
     * Cria apenas o repositório (sem injectar workflows).
     * Mantido para compatibilidade com código existente.
     */
    suspend fun createRepository(
        name: String,
        description: String,
        private: Boolean = false
    ): RepoCreationResult {
        val bearer = tokenManager.getBearerToken()
            ?: return RepoCreationResult.Error("Not authenticated")
        return try {
            val request = CreateRepoRequest(
                name = name,
                description = description,
                private = private,
                auto_init = true
            )
            val response = api.createRepo(bearer, request)
            when {
                response.isSuccessful -> {
                    val repo = response.body()!!
                    tokenManager.saveCreatedRepo(repo.name, repo.owner.login, repo.html_url)
                    tokenManager.saveSelectedRepo(repo.full_name)
                    RepoCreationResult.Success(repo)
                }
                response.code() == 422 -> RepoCreationResult.AlreadyExists(name)
                response.code() == 401 -> RepoCreationResult.Error("Unauthorized — token may be expired")
                response.code() == 403 -> RepoCreationResult.Error("Permission denied")
                else -> RepoCreationResult.Error("GitHub error: HTTP ${response.code()}")
            }
        } catch (e: Exception) {
            RepoCreationResult.Error("Network error: ${e.message}")
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
        val bearer = token.asBearerToken()

        val inputs = mapOf(
            "character_id"   to config.id,
            "character_name" to config.name,
            "preset"         to config.identity.category,
            "quality"        to config.style.quality,
            "export_formats" to config.export.formats.joinToString(","),
            "config_json"    to moshi.adapter(CharacterConfig::class.java).toJson(config)
        )

        val request = WorkflowDispatchWithInputs(ref = "main", inputs = inputs)
        return try {
            val response = api.dispatchWorkflow(
                bearer, owner, repo,
                WorkflowTemplates.WORKFLOW_GENERATE,   // "ara-axev-crime.yml"
                request
            )
            response.isSuccessful
        } catch (e: Exception) {
            false
        }
    }

    /**
     * Dispara o smoke test manualmente.
     * Usa o repositório seleccionado na sessão actual.
     */
    suspend fun runSmokeTest(repoFullName: String): Boolean {
        val bearer = tokenManager.getBearerToken() ?: return false
        val parts = repoFullName.split("/")
        if (parts.size != 2) return false
        return try {
            val response = api.dispatchWorkflow(
                token      = bearer,
                owner      = parts[0],
                repo       = parts[1],
                workflowId = WorkflowTemplates.WORKFLOW_SMOKE,  // "axev-smoke-test.yml"
                request    = WorkflowDispatchWithInputs(ref = "main", inputs = emptyMap())
            )
            response.isSuccessful
        } catch (e: Exception) {
            false
        }
    }

    suspend fun getWorkflowRuns(token: String, repoFullName: String): List<WorkflowRun> {
        val parts = repoFullName.split("/")
        if (parts.size != 2) return emptyList()
        val bearer = token.asBearerToken()
        return try {
            val response = api.getWorkflowRuns(bearer, parts[0], parts[1])
            if (response.isSuccessful) response.body()?.workflow_runs ?: emptyList() else emptyList()
        } catch (e: Exception) {
            emptyList()
        }
    }

    // ── Helpers ───────────────────────────────────────────────────────────────

    private fun String.asBearerToken() =
        if (startsWith("Bearer ")) this else "Bearer $this"
}
