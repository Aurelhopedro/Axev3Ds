package com.example.data.github

import android.util.Log
import com.example.data.api.CreateFileRequest
import com.example.data.api.CreateRepoRequest
import com.example.data.api.GitHubApi
import com.example.data.api.GitHubRepo

private const val TAG = "AXEV/Setup"

/**
 * Resultado do bootstrap completo de um repositório AXEV.
 */
sealed class SetupResult {
    /** Repositório criado e workflows injectados com sucesso. */
    data class Success(val repo: GitHubRepo) : SetupResult()

    /** Repositório já existia — workflows foram actualizados. */
    data class AlreadyExisted(val repo: GitHubRepo) : SetupResult()

    /** Falha em alguma etapa. [step] indica onde falhou. */
    data class Error(val step: String, val message: String) : SetupResult()
}

/**
 * Resultado interno de um upsert de um único workflow.
 */
private sealed class UpsertResult {
    object Created : UpsertResult()
    object Updated : UpsertResult()
    data class Failed(val step: String, val message: String) : UpsertResult()
}

/**
 * Orquestra a criação de um repositório AXEV e o upsert automático
 * dos dois workflows em `.github/workflows/`.
 *
 * "Upsert" significa: se o ficheiro não existe → cria-o (POST equivalente via PUT sem SHA);
 * se já existe → obtém o SHA actual e actualiza-o (PUT com SHA).
 * Desta forma repositórios antigos recebem sempre a versão mais recente dos workflows.
 */
class RepositorySetupService(private val api: GitHubApi) {

    suspend fun setupRepository(bearer: String, owner: String): SetupResult {

        // ── Passo 1: criar repositório ────────────────────────────────────────
        Log.d(TAG, "setupRepository: owner=$owner repo=${WorkflowTemplates.REPO_NAME}")

        val repoResult = runCatching {
            api.createRepo(
                token = bearer,
                request = CreateRepoRequest(
                    name        = WorkflowTemplates.REPO_NAME,
                    description = WorkflowTemplates.REPO_DESCRIPTION,
                    private     = false,
                    auto_init   = true
                )
            )
        }.getOrElse {
            Log.e(TAG, "create_repo: network error — ${it.message}", it)
            return SetupResult.Error("create_repo", "Network error: ${it.message}")
        }

        val alreadyExisted: Boolean
        val repo: GitHubRepo

        when (repoResult.code()) {
            201  -> {
                alreadyExisted = false
                repo = repoResult.body()!!
                Log.i(TAG, "create_repo: created ${repo.full_name}")
            }
            422  -> {
                alreadyExisted = true
                Log.i(TAG, "create_repo: repo already exists, fetching...")
                val existing = runCatching { api.getRepos(bearer) }.getOrElse {
                    Log.e(TAG, "fetch_existing_repo: network error — ${it.message}", it)
                    return SetupResult.Error("fetch_existing_repo", "Network error: ${it.message}")
                }
                val body = existing.body()
                if (!existing.isSuccessful || body == null) {
                    Log.e(TAG, "fetch_existing_repo: HTTP ${existing.code()} — ${existing.errorBody()?.string()}")
                    return SetupResult.Error("fetch_existing_repo", "HTTP ${existing.code()} ao listar repositórios")
                }
                repo = body.firstOrNull { it.name == WorkflowTemplates.REPO_NAME }
                    ?: run {
                        Log.e(TAG, "find_existing_repo: '${WorkflowTemplates.REPO_NAME}' not found in account")
                        return SetupResult.Error("find_existing_repo", "Repositório '${WorkflowTemplates.REPO_NAME}' não encontrado na conta")
                    }
                Log.i(TAG, "find_existing_repo: found ${repo.full_name}")
            }
            401  -> {
                val err = repoResult.errorBody()?.string() ?: "(sem body)"
                Log.e(TAG, "create_repo: 401 Unauthorized — $err")
                return SetupResult.Error("create_repo", "Token inválido ou expirado")
            }
            403  -> {
                val err = repoResult.errorBody()?.string() ?: "(sem body)"
                Log.e(TAG, "create_repo: 403 Forbidden — $err")
                return SetupResult.Error("create_repo", "Permissão negada — verifica o scope do token")
            }
            else -> {
                val err = repoResult.errorBody()?.string() ?: "(sem body)"
                Log.e(TAG, "create_repo: unexpected HTTP ${repoResult.code()} — $err")
                return SetupResult.Error("create_repo", "GitHub HTTP ${repoResult.code()}: $err")
            }
        }

        // ── Passos 2 e 3: upsert dos dois workflows ───────────────────────────
        val workflows = listOf(
            WorkflowTemplates.WORKFLOW_GENERATE to WorkflowTemplates.generateWorkflowBase64(),
            WorkflowTemplates.WORKFLOW_SMOKE    to WorkflowTemplates.smokeWorkflowBase64()
        )

        for ((fileName, content) in workflows) {
            when (val r = upsertWorkflow(bearer, owner, repo.name, fileName, content)) {
                is UpsertResult.Created -> Log.i(TAG, "upsert[$fileName]: ✅ created")
                is UpsertResult.Updated -> Log.i(TAG, "upsert[$fileName]: ✅ updated (workflow was outdated)")
                is UpsertResult.Failed  -> {
                    Log.e(TAG, "upsert[$fileName]: ❌ ${r.step} — ${r.message}")
                    return SetupResult.Error(r.step, r.message)
                }
            }
        }

        return if (alreadyExisted) SetupResult.AlreadyExisted(repo)
               else                SetupResult.Success(repo)
    }

    // ── Core: upsert de um único workflow ─────────────────────────────────────

    /**
     * Faz upsert do ficheiro em `.github/workflows/[fileName]`:
     *
     * 1. GET do ficheiro → extrai o SHA (se 404, o ficheiro ainda não existe).
     * 2. PUT com SHA (update) ou sem SHA (create).
     *
     * Registos detalhados em cada passo para nunca falhar silenciosamente.
     */
    private suspend fun upsertWorkflow(
        bearer:   String,
        owner:    String,
        repo:     String,
        fileName: String,
        content:  String
    ): UpsertResult {

        val path = ".github/workflows/$fileName"
        Log.d(TAG, "upsertWorkflow: GET $path")

        // ── 1. Obter SHA do ficheiro existente ────────────────────────────────
        val existingSha: String? = runCatching {
            api.getFileContent(bearer, owner, repo, path)
        }.fold(
            onSuccess = { response ->
                when (response.code()) {
                    200  -> {
                        val sha = response.body()?.sha
                        if (sha != null) {
                            Log.d(TAG, "upsertWorkflow[$fileName]: existing SHA=$sha")
                        } else {
                            Log.w(TAG, "upsertWorkflow[$fileName]: GET 200 but sha is null — will attempt create")
                        }
                        sha
                    }
                    404  -> {
                        Log.d(TAG, "upsertWorkflow[$fileName]: file does not exist yet — will create")
                        null
                    }
                    401  -> {
                        Log.e(TAG, "upsertWorkflow[$fileName]: GET 401 — token inválido ou expirado")
                        return UpsertResult.Failed(
                            "get_sha:$fileName",
                            "Token inválido ao verificar $fileName (HTTP 401)"
                        )
                    }
                    403  -> {
                        val err = response.errorBody()?.string() ?: "(sem body)"
                        Log.e(TAG, "upsertWorkflow[$fileName]: GET 403 — $err")
                        return UpsertResult.Failed(
                            "get_sha:$fileName",
                            "Permissão negada ao verificar $fileName (HTTP 403): $err"
                        )
                    }
                    else -> {
                        val err = response.errorBody()?.string() ?: "(sem body)"
                        Log.w(TAG, "upsertWorkflow[$fileName]: GET ${response.code()} — $err — will attempt create without SHA")
                        null // tenta criar mesmo assim
                    }
                }
            },
            onFailure = { ex ->
                Log.e(TAG, "upsertWorkflow[$fileName]: GET network error — ${ex.message}", ex)
                return UpsertResult.Failed(
                    "get_sha:$fileName",
                    "Erro de rede ao verificar $fileName: ${ex.message}"
                )
            }
        )

        // ── 2. PUT (create se sha==null, update se sha!=null) ─────────────────
        val action = if (existingSha != null) "update" else "create"
        val commitMsg = if (existingSha != null)
            "ci: update $fileName to latest AXEV version"
        else
            "ci: add $fileName"

        Log.d(TAG, "upsertWorkflow[$fileName]: PUT ($action) sha=$existingSha")

        val putResponse = runCatching {
            api.createFile(
                token   = bearer,
                owner   = owner,
                repo    = repo,
                path    = path,
                request = CreateFileRequest(
                    message = commitMsg,
                    content = content,
                    sha     = existingSha,  // null → create, non-null → update
                    branch  = "main"
                )
            )
        }.getOrElse { ex ->
            Log.e(TAG, "upsertWorkflow[$fileName]: PUT network error — ${ex.message}", ex)
            return UpsertResult.Failed(
                "put:$fileName",
                "Erro de rede ao $action $fileName: ${ex.message}"
            )
        }

        return when (putResponse.code()) {
            200, 201 -> {
                val newSha = putResponse.body()?.content?.sha ?: "unknown"
                Log.i(TAG, "upsertWorkflow[$fileName]: PUT $action OK — new SHA=$newSha")
                if (existingSha != null) UpsertResult.Updated else UpsertResult.Created
            }
            401 -> {
                val err = putResponse.errorBody()?.string() ?: "(sem body)"
                Log.e(TAG, "upsertWorkflow[$fileName]: PUT 401 — $err")
                UpsertResult.Failed("put:$fileName", "Token inválido ao $action $fileName (HTTP 401)")
            }
            403 -> {
                val err = putResponse.errorBody()?.string() ?: "(sem body)"
                Log.e(TAG, "upsertWorkflow[$fileName]: PUT 403 — $err")
                UpsertResult.Failed("put:$fileName", "Permissão negada ao $action $fileName (HTTP 403): $err")
            }
            409 -> {
                // Conflict: SHA desactualizado — raro mas possível se houver commit concorrente
                val err = putResponse.errorBody()?.string() ?: "(sem body)"
                Log.e(TAG, "upsertWorkflow[$fileName]: PUT 409 Conflict — SHA stale. $err")
                UpsertResult.Failed("put:$fileName", "Conflito de SHA ao actualizar $fileName (HTTP 409) — tenta novamente")
            }
            422 -> {
                // Pode acontecer se o SHA passado estiver errado ou o conteúdo for idêntico
                val err = putResponse.errorBody()?.string() ?: "(sem body)"
                Log.w(TAG, "upsertWorkflow[$fileName]: PUT 422 — $err")
                // 422 ao criar sem SHA significa que o ficheiro já existia — tratar como OK
                if (existingSha == null) {
                    Log.w(TAG, "upsertWorkflow[$fileName]: 422 on create (file already existed) — treating as updated")
                    UpsertResult.Updated
                } else {
                    UpsertResult.Failed("put:$fileName", "Validação falhou ao actualizar $fileName (HTTP 422): $err")
                }
            }
            else -> {
                val err = putResponse.errorBody()?.string() ?: "(sem body)"
                Log.e(TAG, "upsertWorkflow[$fileName]: PUT ${putResponse.code()} — $err")
                UpsertResult.Failed("put:$fileName", "GitHub HTTP ${putResponse.code()} ao $action $fileName: $err")
            }
        }
    }
}
