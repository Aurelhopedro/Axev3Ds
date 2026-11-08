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

private const val TAG = "AXEV/Repo"

sealed class RepoCreationResult {
    data class Success(val repo: GitHubRepo) : RepoCreationResult()
    data class AlreadyExists(val name: String) : RepoCreationResult()
    data class Error(val message: String) : RepoCreationResult()
}

/**
 * Resultado do update de workflows num repositório já existente.
 */
sealed class WorkflowUpdateResult {
    /** Ambos os workflows actualizados com sucesso. */
    object Success : WorkflowUpdateResult()
    /** Falha parcial ou total. [details] lista o estado de cada workflow. */
    data class PartialFailure(val details: List<String>) : WorkflowUpdateResult()
    /** Erro antes de começar (sem token, sem repo, etc.). */
    data class Error(val message: String) : WorkflowUpdateResult()
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
            if (response.isSuccessful) {
                response.body()
            } else {
                Log.e(TAG, "getUser: HTTP ${response.code()} — ${response.errorBody()?.string()}")
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
                Log.e(TAG, "getRepos: HTTP ${response.code()} — ${response.errorBody()?.string()}")
                emptyList()
            }
        } catch (e: Exception) {
            Log.e(TAG, "getRepos: network error — ${e.message}", e)
            emptyList()
        }
    }

    suspend fun setupRepository(): SetupResult {
        val bearer = tokenManager.getBearerToken()
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
        val bearer = tokenManager.getBearerToken()
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
                response.code() == 422 -> {
                    Log.w(TAG, "createRepository: repo '$name' already exists")
                    RepoCreationResult.AlreadyExists(name)
                }
                response.code() == 401 -> {
                    Log.e(TAG, "createRepository: 401 Unauthorized")
                    RepoCreationResult.Error("Unauthorized — token may be expired")
                }
                response.code() == 403 -> {
                    val err = response.errorBody()?.string() ?: ""
                    Log.e(TAG, "createRepository: 403 Forbidden — $err")
                    RepoCreationResult.Error("Permission denied")
                }
                else -> {
                    val err = response.errorBody()?.string() ?: ""
                    Log.e(TAG, "createRepository: HTTP ${response.code()} — $err")
                    RepoCreationResult.Error("GitHub error: HTTP ${response.code()}")
                }
            }
        } catch (e: Exception) {
            Log.e(TAG, "createRepository: network error — ${e.message}", e)
            RepoCreationResult.Error("Network error: ${e.message}")
        }
    }

    /**
     * Injicta os workflows num repositório recém-criado.
     * Tenta até 3 vezes com 5s de espera entre tentativas,
     * para dar tempo ao GitHub de inicializar o branch main.
     *
     * Para repositórios já existentes usa [updateWorkflows] em vez deste método.
     */
    suspend fun setupWorkspace(owner: String, repoName: String): Boolean {
        val bearer = tokenManager.getBearerToken() ?: run {
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
                    when (response.code()) {
                        200, 201 -> Log.i(TAG, "setupWorkspace[$fileName]: created OK")
                        422      -> Log.w(TAG, "setupWorkspace[$fileName]: already exists (422) — skipping")
                        else     -> {
                            val err = response.errorBody()?.string() ?: ""
                            Log.e(TAG, "setupWorkspace[$fileName]: HTTP ${response.code()} — $err")
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

    /**
     * Actualiza os workflows num repositório já existente.
     *
     * Faz GET de cada workflow para obter o SHA actual e depois PUT com o novo conteúdo.
     * Se o ficheiro não existir ainda, cria-o.
     *
     * Devolve [WorkflowUpdateResult.Success] apenas se ambos os ficheiros forem
     * actualizados/criados com sucesso. Falhas são detalhadas em [WorkflowUpdateResult.PartialFailure].
     */
    suspend fun updateWorkflows(repoFullName: String): WorkflowUpdateResult {
        val bearer = tokenManager.getBearerToken() ?: run {
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

            // ── 1. GET SHA ────────────────────────────────────────────────────
            Log.d(TAG, "updateWorkflows[$fileName]: GET sha from $path")
            val existingSha: String? = try {
                val r = api.getFileContent(bearer, owner, repo, path)
                when (r.code()) {
                    200  -> {
                        val sha = r.body()?.sha
                        if (sha == null) {
                            Log.w(TAG, "updateWorkflows[$fileName]: GET 200 but sha=null, will create")
                        } else {
                            Log.d(TAG, "updateWorkflows[$fileName]: existing sha=$sha")
                        }
                        sha
                    }
                    404  -> {
                        Log.i(TAG, "updateWorkflows[$fileName]: not found (404) — will create")
                        null
                    }
                    401  -> {
                        val err = r.errorBody()?.string() ?: ""
                        Log.e(TAG, "updateWorkflows[$fileName]: GET 401 — $err")
                        details.add("❌ $fileName: token inválido (GET 401)")
                        anyFailed = true
                        continue
                    }
                    403  -> {
                        val err = r.errorBody()?.string() ?: ""
                        Log.e(TAG, "updateWorkflows[$fileName]: GET 403 — $err")
                        details.add("❌ $fileName: sem permissão (GET 403) — $err")
                        anyFailed = true
                        continue
                    }
                    else -> {
                        val err = r.errorBody()?.string() ?: ""
                        Log.w(TAG, "updateWorkflows[$fileName]: GET ${r.code()} — $err — proceeding without SHA")
                        null
                    }
                }
            } catch (e: Exception) {
                Log.e(TAG, "updateWorkflows[$fileName]: GET network error — ${e.message}", e)
                details.add("❌ $fileName: erro de rede ao verificar (${e.message})")
                anyFailed = true
                continue
            }

            // ── 2. PUT (create or update) ─────────────────────────────────────
            val action    = if (existingSha != null) "update" else "create"
            val commitMsg = if (existingSha != null)
                "ci: update $fileName to latest AXEV version"
            else
                "ci: add $fileName"

            Log.d(TAG, "updateWorkflows[$fileName]: PUT ($action)")
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
                when (r.code()) {
                    200, 201 -> {
                        val newSha = r.body()?.content?.sha ?: "unknown"
                        Log.i(TAG, "updateWorkflows[$fileName]: ✅ $action OK — new sha=$newSha")
                        details.add("✅ $fileName: ${if (existingSha != null) "actualizado" else "criado"}")
                    }
                    401  -> {
                        val err = r.errorBody()?.string() ?: ""
                        Log.e(TAG, "updateWorkflows[$fileName]: PUT 401 — $err")
                        details.add("❌ $fileName: token inválido ao $action (PUT 401)")
                        anyFailed = true
                    }
                    403  -> {
                        val err = r.errorBody()?.string() ?: ""
                        Log.e(TAG, "updateWorkflows[$fileName]: PUT 403 — $err")
                        details.add("❌ $fileName: sem permissão ao $action (PUT 403) — $err")
                        anyFailed = true
                    }
                    409  -> {
                        val err = r.errorBody()?.string() ?: ""
                        Log.e(TAG, "updateWorkflows[$fileName]: PUT 409 Conflict (SHA stale) — $err")
                        details.add("❌ $fileName: conflito de SHA (PUT 409) — tenta novamente")
                        anyFailed = true
                    }
                    422  -> {
                        val err = r.errorBody()?.string() ?: ""
                        if (existingSha == null) {
                            // ficheiro já existia mas não foi detectado no GET — tratar como OK
                            Log.w(TAG, "updateWorkflows[$fileName]: PUT 422 on create (file existed) — OK")
                            details.add("✅ $fileName: já existia (sem alterações)")
                        } else {
                            Log.e(TAG, "updateWorkflows[$fileName]: PUT 422 — $err")
                            details.add("❌ $fileName: validação falhou (PUT 422) — $err")
                            anyFailed = true
                        }
                    }
                    else -> {
                        val err = r.errorBody()?.string() ?: ""
                        Log.e(TAG, "updateWorkflows[$fileName]: PUT ${r.code()} — $err")
                        details.add("❌ $fileName: HTTP ${r.code()} ao $action — $err")
                        anyFailed = true
                    }
                }
            } catch (e: Exception) {
                Log.e(TAG, "updateWorkflows[$fileName]: PUT network error — ${e.message}", e)
                details.add("❌ $fileName: erro de rede ao $action (${e.message})")
                anyFailed = true
            }
        }

        Log.i(TAG, "updateWorkflows: done. details=$details anyFailed=$anyFailed")
        return if (anyFailed) WorkflowUpdateResult.PartialFailure(details)
               else           WorkflowUpdateResult.Success
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
            "config_json"     to ""
        )

        Log.d(TAG, "generateCharacter: dispatching workflow for character_id=${config.id} in $repoFullName")
        Log.d(TAG, "generateCharacter: inputs=$inputs")

        val request = WorkflowDispatchWithInputs(ref = "main", inputs = inputs)
        return try {
            val response = api.dispatchWorkflow(
                bearer, owner, repo,
                WorkflowTemplates.WORKFLOW_GENERATE,
                request
            )
            if (response.isSuccessful) {
                Log.i(TAG, "generateCharacter: dispatch OK (HTTP ${response.code()})")
                true
            } else {
                val err = response.errorBody()?.string() ?: "(sem body)"
                Log.e(TAG, "generateCharacter: dispatch FAILED — HTTP ${response.code()} — $err")
                false
            }
        } catch (e: Exception) {
            Log.e(TAG, "generateCharacter: dispatch exception — ${e.message}", e)
            false
        }
    }

    suspend fun runSmokeTest(repoFullName: String): Boolean {
        val bearer = tokenManager.getBearerToken() ?: run {
            Log.e(TAG, "runSmokeTest: no token")
            return false
        }
        val parts  = repoFullName.split("/")
        if (parts.size != 2) {
            Log.e(TAG, "runSmokeTest: invalid repoFullName='$repoFullName'")
            return false
        }
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
                val err = response.errorBody()?.string() ?: ""
                Log.e(TAG, "runSmokeTest: HTTP ${response.code()} — $err")
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
        val bearer = token.asBearerToken()
        return try {
            val response = api.getWorkflowRuns(bearer, parts[0], parts[1])
            if (response.isSuccessful) {
                response.body()?.workflow_runs ?: emptyList()
            } else {
                Log.e(TAG, "getWorkflowRuns: HTTP ${response.code()} — ${response.errorBody()?.string()}")
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
