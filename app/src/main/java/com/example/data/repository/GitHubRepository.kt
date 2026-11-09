package com.example.data.repository

import android.content.Context
import android.util.Log
import com.example.data.api.CreateFileRequest
import com.example.data.api.CreateRepoRequest
import com.example.data.api.GitHubApi
import com.example.data.api.GitHubRepo
import com.example.data.api.GitHubUser
import com.example.data.api.WorkflowDispatchWithInputs
import com.example.data.api.WorkflowRun
import com.example.data.auth.AuthTokenManager
import com.example.data.auth.GitHubAuthRepository
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
import java.util.Base64

private const val TAG = "AXEV/Repo"

sealed class RepoCreationResult {
    data class Success(val repo: GitHubRepo) : RepoCreationResult()
    data class AlreadyExists(val name: String) : RepoCreationResult()
    data class Error(val message: String) : RepoCreationResult()
}

sealed class WorkflowUpdateResult {
    object Success : WorkflowUpdateResult()
    data class PartialFailure(val details: List<String>) : WorkflowUpdateResult()
    data class Error(val message: String) : WorkflowUpdateResult()
}

class GitHubRepository(private val context: Context) {

    val tokenManager = AuthTokenManager(context)

    private val authTokenManager = AuthTokenManager(context)

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

    // ── Token resolution (unified) ────────────────────────────────────────────

    fun resolveToken(): String? =
        tokenManager.getBearerToken() ?: authTokenManager.getBearerToken()

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
            if (response.isSuccessful) {
                response.body()
            } else {
                Log.e(TAG, "getUser: HTTP ${response.code} — ${response.errorBody()?.string()}")
                null
            }
        } catch (e: Exception) {
            Log.e(TAG, "getUser: network error — ${e.message}", e)
            null
        }
    }

    suspend fun getRepos(token: String): List<GitHubRepo> {
        val bearer = token.asBearerToken()
        return try {
            val response = api.getRepos(bearer)
            if (response.isSuccessful) {
                response.body() ?: emptyList()
            } else {
                Log.e(TAG, "getRepos: HTTP ${response.code} — ${response.errorBody()?.string()}")
                emptyList()
            }
        } catch (e: Exception) {
            Log.e(TAG, "getRepos: network error — ${e.message}", e)
            emptyList()
        }
    }

    suspend fun setupRepository(): SetupResult {
        val bearer = resolveToken()
            ?: run {
                Log.e(TAG, "setupRepository: no token saved")
                return SetupResult.Error("auth", "Não autenticado")
            }
        val user = getUser(bearer)
            ?: run {
                Log.e(TAG, "setupRepository: could not fetch user")
                return SetupResult.Error("auth", "Não foi possível obter o utilizador")
            }
        return setupService.setupRepository(bearer, user.login)
    }

    suspend fun createRepository(
        name: String,
        description: String,
        private: Boolean = false
    ): RepoCreationResult {
        val bearer = resolveToken()
            ?: run {
                Log.e(TAG, "createRepository: no token")
                return RepoCreationResult.Error("Not authenticated")
            }
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
                    Log.i(TAG, "createRepository: created ${repo.full_name}")
                    RepoCreationResult.Success(repo)
                }
                response.code == 422 -> {
                    Log.w(TAG, "createRepository: repo '$name' already exists")
                    RepoCreationResult.AlreadyExists(name)
                }
                response.code == 401 -> {
                    Log.e(TAG, "createRepository: 401 Unauthorized")
                    RepoCreationResult.Error("Unauthorized — token may be expired")
                }
                response.code == 403 -> {
                    val err = response.errorBody()?.string() ?: ""
                    Log.e(TAG, "createRepository: 403 Forbidden — $err")
                    RepoCreationResult.Error("Permission denied")
                }
                else -> {
                    val err = response.errorBody()?.string() ?: ""
                    Log.e(TAG, "createRepository: HTTP ${response.code} — $err")
                    RepoCreationResult.Error("GitHub error: HTTP ${response.code}")
                }
            }
        } catch (e: Exception) {
            Log.e(TAG, "createRepository: network error — ${e.message}", e)
            RepoCreationResult.Error("Network error: ${e.message}")
        }
    }

    suspend fun setupWorkspace(owner: String, repoName: String): Boolean {
        val bearer = resolveToken() ?: run {
            Log.e(TAG, "setupWorkspace: no token")
            return false
        }

        val workflows = listOf(
            ".github/workflows/${WorkflowTemplates.WORKFLOW_GENERATE}" to
                WorkflowTemplates.generateWorkflowBase64(),
            ".github/workflows/${WorkflowTemplates.WORKFLOW_SMOKE}" to
                WorkflowTemplates.smokeWorkflowBase64()
        )

        repeat(3) { attempt ->
            if (attempt > 0) {
                Log.d(TAG, "setupWorkspace: retry attempt ${attempt + 1}/3 after 5s")
                delay(5_000)
            }

            var allSuccess = true
            for ((path, content) in workflows) {
                val fileName = path.substringAfterLast("/")
                try {
                    val response = api.createFile(
                        token   = bearer,
                        owner   = owner,
                        repo    = repoName,
                        path    = path,
                        request = CreateFileRequest(
                            message = "chore: add AXEV workflow — $fileName",
                            content = content
                        )
                    )
                    when (response.code) {
                        200, 201 -> Log.i(TAG, "setupWorkspace[$fileName]: created OK")
                        422      -> Log.w(TAG, "setupWorkspace[$fileName]: already exists (422) — skipping")
                        else     -> {
                            val err = response.errorBody()?.string() ?: ""
                            Log.e(TAG, "setupWorkspace[$fileName]: HTTP ${response.code} — $err")
                            allSuccess = false
                        }
                    }
                } catch (e: Exception) {
                    Log.e(TAG, "setupWorkspace[$fileName]: network error — ${e.message}", e)
                    allSuccess = false
                }
            }

            if (allSuccess) {
                Log.i(TAG, "setupWorkspace: all workflows injected on attempt ${attempt + 1}")
                return true
            }
        }

        Log.e(TAG, "setupWorkspace: failed after 3 attempts for $owner/$repoName")
        return false
    }

    suspend fun updateWorkflows(repoFullName: String): WorkflowUpdateResult {
        val bearer = resolveToken() ?: run {
            Log.e(TAG, "updateWorkflows: no token saved")
            return WorkflowUpdateResult.Error("Não autenticado — faz login novamente")
        }

        val parts = repoFullName.split("/")
        if (parts.size != 2) {
            Log.e(TAG, "updateWorkflows: invalid repoFullName='$repoFullName'")
            return WorkflowUpdateResult.Error("Nome do repositório inválido: '$repoFullName'")
        }
        val owner = parts[0]
        val repo  = parts[1]

        Log.i(TAG, "updateWorkflows: starting for $repoFullName")

        val workflows = listOf(
            WorkflowTemplates.WORKFLOW_GENERATE to WorkflowTemplates.generateWorkflowBase64(),
            WorkflowTemplates.WORKFLOW_SMOKE    to WorkflowTemplates.smokeWorkflowBase64()
        )

        val details = mutableListOf<String>()
        var anyFailed = false

        for ((fileName, newContent) in workflows) {
            val path = ".github/workflows/$fileName"

            val existingSha: String? = try {
                val r = api.getFileContent(bearer, owner, repo, path)
                when (r.code) {
                    200  -> r.body()?.sha
                    404  -> null
                    else -> {
                        val err = r.errorBody()?.string() ?: ""
                        Log.e(TAG, "updateWorkflows[$fileName]: GET ${r.code} — $err")
                        details.add("❌ $fileName: erro ao verificar (${r.code})")
                        anyFailed = true
                        continue
                    }
                }
            } catch (e: Exception) {
                Log.e(TAG, "updateWorkflows[$fileName]: GET network error — ${e.message}", e)
                details.add("❌ $fileName: erro de rede ao verificar (${e.message})")
                anyFailed = true
                continue
            }

            val action    = if (existingSha != null) "update" else "create"
            val commitMsg = if (existingSha != null)
                "ci: update $fileName to latest AXEV version"
            else
                "ci: add $fileName"

            try {
                val r = api.createFile(
                    token   = bearer,
                    owner   = owner,
                    repo    = repo,
                    path    = path,
                    request = CreateFileRequest(
                        message = commitMsg,
                        content = newContent,
                        sha     = existingSha,
                        branch  = "main"
                    )
                )
                when (r.code) {
                    200, 201 -> {
                        Log.i(TAG, "updateWorkflows[$fileName]: ✅ $action OK")
                        details.add("✅ $fileName: ${if (existingSha != null) "actualizado" else "criado"}")
                    }
                    422 -> {
                        if (existingSha == null) {
                            Log.w(TAG, "updateWorkflows[$fileName]: PUT 422 on create (file existed) — OK")
                            details.add("✅ $fileName: já existia (sem alterações)")
                        } else {
                            val err = r.errorBody()?.string() ?: ""
                            Log.e(TAG, "updateWorkflows[$fileName]: PUT 422 — $err")
                            details.add("❌ $fileName: validação falhou (PUT 422) — $err")
                            anyFailed = true
                        }
                    }
                    else -> {
                        val err = r.errorBody()?.string() ?: ""
                        Log.e(TAG, "updateWorkflows[$fileName]: PUT ${r.code} — $err")
                        details.add("❌ $fileName: HTTP ${r.code} ao $action — $err")
                        anyFailed = true
                    }
                }
            } catch (e: Exception) {
                Log.e(TAG, "updateWorkflows[$fileName]: PUT network error — ${e.message}", e)
                details.add("❌ $fileName: erro de rede ao $action (${e.message})")
                anyFailed = true
            }
        }

        return if (anyFailed) WorkflowUpdateResult.PartialFailure(details)
               else           WorkflowUpdateResult.Success
    }

    /**
     * Injeta um conteúdo YAML remoto num workflow específico do repositório do utilizador.
     * Chamado pelo MainViewModel quando o RemoteConfigRepository encontra uma versão mais recente.
     * Falha silenciosamente — o dispatch continua mesmo que a actualização falhe.
     */
    suspend fun updateWorkflowContent(
        repoFullName: String,
        workflowFilename: String,
        yamlContent: String
    ) {
        val bearer = resolveToken() ?: run {
            Log.e(TAG, "updateWorkflowContent: no token")
            return
        }
        val parts = repoFullName.split("/")
        if (parts.size != 2) return
        val owner = parts[0]
        val repo  = parts[1]
        val path  = ".github/workflows/$workflowFilename"

        try {
            // Obter SHA actual (se existir)
            val existingSha: String? = try {
                val r = api.getFileContent(bearer, owner, repo, path)
                if (r.code == 200) r.body()?.sha else null
            } catch (e: Exception) { null }

            val encodedContent = Base64.getEncoder().encodeToString(
                yamlContent.toByteArray(Charsets.UTF_8)
            )

            val r = api.createFile(
                token   = bearer,
                owner   = owner,
                repo    = repo,
                path    = path,
                request = CreateFileRequest(
                    message = "ci: update $workflowFilename from axev-config remote",
                    content = encodedContent,
                    sha     = existingSha,
                    branch  = "main"
                )
            )
            if (r.isSuccessful) {
                Log.i(TAG, "updateWorkflowContent: ✅ $workflowFilename updated from remote config")
            } else {
                val err = r.errorBody()?.string() ?: ""
                Log.w(TAG, "updateWorkflowContent: HTTP ${r.code} — $err (non-fatal, dispatch continues)")
            }
        } catch (e: Exception) {
            Log.w(TAG, "updateWorkflowContent: exception — ${e.message} (non-fatal, dispatch continues)", e)
        }
    }

    suspend fun generateCharacter(
        token: String,
        repoFullName: String,
        config: CharacterConfig
    ): Boolean {
        val parts = repoFullName.split("/")
        if (parts.size != 2) {
            Log.e(TAG, "generateCharacter: invalid repoFullName='$repoFullName'")
            return false
        }
        val owner = parts[0]
        val repo  = parts[1]

        val bearer = resolveToken() ?: run {
            Log.e(TAG, "generateCharacter: no token available after resolution")
            return false
        }

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
            "config_json"     to ""
        )

        Log.d(TAG, "generateCharacter: dispatching '${WorkflowTemplates.WORKFLOW_GENERATE}'")
        Log.d(TAG, "generateCharacter: repo=$repoFullName  character_id=${config.id}")

        val request = WorkflowDispatchWithInputs(ref = "main", inputs = inputs)
        return try {
            val response = api.dispatchWorkflow(
                bearer, owner, repo,
                WorkflowTemplates.WORKFLOW_GENERATE,
                request
            )
            if (response.isSuccessful) {
                Log.i(TAG, "generateCharacter: dispatch OK (HTTP ${response.code})")
                true
            } else {
                val errBody = response.errorBody()?.string() ?: "(sem body)"
                Log.e(TAG, "generateCharacter: dispatch FAILED")
                Log.e(TAG, "  → HTTP ${response.code}")
                Log.e(TAG, "  → body: $errBody")
                Log.e(TAG, "  → token starts with: ${bearer.take(20)}…")
                false
            }
        } catch (e: Exception) {
            Log.e(TAG, "generateCharacter: dispatch exception — ${e.message}", e)
            false
        }
    }

    suspend fun runSmokeTest(repoFullName: String): Boolean {
        val bearer = resolveToken() ?: run {
            Log.e(TAG, "runSmokeTest: no token")
            return false
        }
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
            if (response.isSuccessful) {
                Log.i(TAG, "runSmokeTest: dispatch OK")
                true
            } else {
                val errBody = response.errorBody()?.string() ?: ""
                Log.e(TAG, "runSmokeTest: HTTP ${response.code} — body: $errBody")
                false
            }
        } catch (e: Exception) {
            Log.e(TAG, "runSmokeTest: exception — ${e.message}", e)
            false
        }
    }

    suspend fun getWorkflowRuns(token: String, repoFullName: String): List<WorkflowRun> {
        val parts  = repoFullName.split("/")
        if (parts.size != 2) return emptyList()
        val bearer = resolveToken() ?: token.asBearerToken()
        return try {
            val response = api.getWorkflowRuns(bearer, parts[0], parts[1])
            if (response.isSuccessful) {
                response.body()?.workflow_runs ?: emptyList()
            } else {
                Log.e(TAG, "getWorkflowRuns: HTTP ${response.code} — ${response.errorBody()?.string()}")
                emptyList()
            }
        } catch (e: Exception) {
            Log.e(TAG, "getWorkflowRuns: exception — ${e.message}", e)
            emptyList()
        }
    }

    private fun String.asBearerToken() =
        if (startsWith("Bearer ")) this else "Bearer $this"
}
