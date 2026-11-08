package com.example.data.repository

import android.content.Context
import com.example.data.api.CreateFileRequest
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
import kotlinx.coroutines.delay
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

    val tokenFlow: Flow<String?> = MutableStateFlow(tokenManager.getBearerToken()).asStateFlow()
    val selectedRepoFlow: Flow<String?> = MutableStateFlow(tokenManager.getSelectedRepo()).asStateFlow()

    suspend fun saveToken(token: String) {
        tokenManager.saveAccessToken(token.trim().removePrefix("Bearer ").removePrefix("bearer "))
    }

    suspend fun saveRepo(repoFullName: String) {
        tokenManager.saveSelectedRepo(repoFullName)
    }

    suspend fun clearSession() {
        tokenManager.clearAll()
    }

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

    suspend fun setupRepository(): SetupResult {
        val bearer = tokenManager.getBearerToken()
            ?: return SetupResult.Error("auth", "Não autenticado")
        val user = getUser(bearer)
            ?: return SetupResult.Error("auth", "Não foi possível obter o utilizador")
        return setupService.setupRepository(bearer, user.login)
    }

    suspend fun createRepository(
        name: String,
        description: String,
        private: Boolean = false
    ): RepoCreationResult {
        val bearer = tokenManager.getBearerToken()
            ?: return RepoCreationResult.Error("Not authenticated")
        return try {
            val request = CreateRepoRequest(
                name        = name,
                description = description,
                private     = private,
                auto_init   = true
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

    /**
     * Envia os workflows AXEV para .github/workflows/ do repositório.
     * Tenta até 3 vezes com 5s de espera entre tentativas,
     * para dar tempo ao GitHub de inicializar o branch main.
     */
    suspend fun setupWorkspace(owner: String, repoName: String): Boolean {
        val bearer = tokenManager.getBearerToken() ?: return false

        val workflows = listOf(
            ".github/workflows/${WorkflowTemplates.WORKFLOW_GENERATE}" to
                WorkflowTemplates.generateWorkflowBase64(),
            ".github/workflows/${WorkflowTemplates.WORKFLOW_SMOKE}" to
                WorkflowTemplates.smokeWorkflowBase64()
        )

        // Tenta até 3 vezes — o GitHub pode demorar a inicializar o branch main
        repeat(3) { attempt ->
            if (attempt > 0) delay(5_000) // 5s entre tentativas

            var allSuccess = true
            for ((path, content) in workflows) {
                try {
                    val response = api.createFile(
                        token   = bearer,
                        owner   = owner,
                        repo    = repoName,
                        path    = path,
                        request = CreateFileRequest(
                            message = "chore: add AXEV workflow — ${path.substringAfterLast("/")}",
                            content = content
                        )
                    )
                    // 201 = criado, 422 = já existe (também é ok)
                    if (!response.isSuccessful && response.code() != 422) {
                        allSuccess = false
                    }
                } catch (_: Exception) {
                    allSuccess = false
                }
            }

            if (allSuccess) return true
        }

        return false
    }

    suspend fun generateCharacter(
        token: String,
        repoFullName: String,
        config: CharacterConfig
    ): Boolean {
        val parts = repoFullName.split("/")
        if (parts.size != 2) return false
        val owner  = parts[0]
        val repo   = parts[1]
        val bearer = token.asBearerToken()

        val inputs = mapOf(
            "character_id"    to config.id,
            "character_name"  to config.name,
            "preset"          to config.preset,
            "gender_age"      to config.genderAge,
            "body"            to config.body,
            "skin_tone"       to config.skinTone,
            "hair"            to config.hair,
            "facial_hair"     to config.facialHair,
            "eyes"            to config.eyes,
            "outfit"          to config.outfit,
            "shoes_hat"       to config.shoesHat,
            "weapon"          to config.weapon,
            "expression"      to config.expression,
            "rig_anim"        to config.rigAnim,
            "quality"         to config.quality,
            "export_formats"  to config.exportFormats,
            "config_json"     to moshi.adapter(CharacterConfig::class.java).toJson(config)
        )

        val request = WorkflowDispatchWithInputs(ref = "main", inputs = inputs)
        return try {
            val response = api.dispatchWorkflow(
                bearer, owner, repo,
                WorkflowTemplates.WORKFLOW_GENERATE,
                request
            )
            response.isSuccessful
        } catch (e: Exception) {
            false
        }
    }

    suspend fun runSmokeTest(repoFullName: String): Boolean {
        val bearer = tokenManager.getBearerToken() ?: return false
        val parts  = repoFullName.split("/")
        if (parts.size != 2) return false
        return try {
            val response = api.dispatchWorkflow(
                token      = bearer,
                owner      = parts[0],
                repo       = parts[1],
                workflowId = WorkflowTemplates.WORKFLOW_SMOKE,
                request    = WorkflowDispatchWithInputs(ref = "main", inputs = emptyMap())
            )
            response.isSuccessful
        } catch (e: Exception) {
            false
        }
    }

    suspend fun getWorkflowRuns(token: String, repoFullName: String): List<WorkflowRun> {
        val parts  = repoFullName.split("/")
        if (parts.size != 2) return emptyList()
        val bearer = token.asBearerToken()
        return try {
            val response = api.getWorkflowRuns(bearer, parts[0], parts[1])
            if (response.isSuccessful) response.body()?.workflow_runs ?: emptyList() else emptyList()
        } catch (e: Exception) {
            emptyList()
        }
    }

    private fun String.asBearerToken() =
        if (startsWith("Bearer ")) this else "Bearer $this"
}
