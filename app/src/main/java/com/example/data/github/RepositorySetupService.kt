package com.example.data.github

import com.example.data.api.CreateFileRequest
import com.example.data.api.CreateRepoRequest
import com.example.data.api.GitHubApi
import com.example.data.api.GitHubRepo

/**
 * Resultado do bootstrap completo de um repositório AXEV.
 */
sealed class SetupResult {
    /** Repositório criado e workflows injectados com sucesso. */
    data class Success(val repo: GitHubRepo) : SetupResult()

    /** Repositório já existia — workflows foram injectados na mesma. */
    data class AlreadyExisted(val repo: GitHubRepo) : SetupResult()

    /** Falha em alguma etapa. [step] indica onde falhou. */
    data class Error(val step: String, val message: String) : SetupResult()
}

/**
 * Orquestra a criação de um repositório AXEV e a injecção automática
 * dos dois workflows em `.github/workflows/`.
 *
 * Uso típico (a partir do GitHubRepository ou de um ViewModel):
 * ```kotlin
 * val service = RepositorySetupService(api)
 * val result = service.setupRepository(bearer, owner)
 * ```
 */
class RepositorySetupService(private val api: GitHubApi) {

    /**
     * Passo 1 — Cria o repositório (ou detecta que já existe).
     * Passo 2 — Injicta `ara-axev-crime.yml` em `.github/workflows/`.
     * Passo 3 — Injicta `axev-smoke-test.yml` em `.github/workflows/`.
     *
     * @param bearer  Token no formato "Bearer ghp_xxxx"
     * @param owner   Login do utilizador autenticado
     */
    suspend fun setupRepository(bearer: String, owner: String): SetupResult {

        // ── Passo 1: criar repositório ────────────────────────────────────────
        val repoResult = runCatching {
            api.createRepo(
                token = bearer,
                request = CreateRepoRequest(
                    name        = WorkflowTemplates.REPO_NAME,
                    description = WorkflowTemplates.REPO_DESCRIPTION,
                    private     = false,
                    auto_init   = true          // garante branch main + README inicial
                )
            )
        }.getOrElse {
            return SetupResult.Error("create_repo", "Network error: ${it.message}")
        }

        val alreadyExisted: Boolean
        val repo: GitHubRepo

        when (repoResult.code()) {
            201  -> { alreadyExisted = false; repo = repoResult.body()!! }
            422  -> {
                // Repositório já existe — tentar obter os dados pelo nome
                alreadyExisted = true
                val existing = runCatching {
                    api.getRepos(bearer)
                }.getOrElse {
                    return SetupResult.Error("fetch_existing_repo", "Network error: ${it.message}")
                }
                val body = existing.body()
                if (!existing.isSuccessful || body == null) {
                    return SetupResult.Error(
                        "fetch_existing_repo",
                        "HTTP ${existing.code()} ao listar repositórios"
                    )
                }
                repo = body.firstOrNull { it.name == WorkflowTemplates.REPO_NAME }
                    ?: return SetupResult.Error(
                        "find_existing_repo",
                        "Repositório '${WorkflowTemplates.REPO_NAME}' não encontrado na conta"
                    )
            }
            401  -> return SetupResult.Error("create_repo", "Token inválido ou expirado")
            403  -> return SetupResult.Error("create_repo", "Permissão negada — verifica o scope do token")
            else -> return SetupResult.Error(
                "create_repo",
                "GitHub HTTP ${repoResult.code()}"
            )
        }

        // ── Passos 2 e 3: injectar workflows ─────────────────────────────────
        injectWorkflow(
            bearer  = bearer,
            owner   = owner,
            repo    = repo.name,
            fileName = WorkflowTemplates.WORKFLOW_GENERATE,
            content  = WorkflowTemplates.generateWorkflowBase64(),
            commitMsg = "ci: add AXEV character generation workflow"
        )?.let { return it }   // propagate error se não for null

        injectWorkflow(
            bearer  = bearer,
            owner   = owner,
            repo    = repo.name,
            fileName = WorkflowTemplates.WORKFLOW_SMOKE,
            content  = WorkflowTemplates.smokeWorkflowBase64(),
            commitMsg = "ci: add AXEV smoke test workflow"
        )?.let { return it }

        return if (alreadyExisted) SetupResult.AlreadyExisted(repo)
               else                SetupResult.Success(repo)
    }

    // ── Helpers ───────────────────────────────────────────────────────────────

    /**
     * Faz PUT do ficheiro em `.github/workflows/[fileName]`.
     * Retorna um [SetupResult.Error] se falhar, ou `null` em caso de sucesso
     * (padrão "retorna erro ou null" para encadear com ?.let).
     */
    private suspend fun injectWorkflow(
        bearer:    String,
        owner:     String,
        repo:      String,
        fileName:  String,
        content:   String,
        commitMsg: String
    ): SetupResult.Error? {
        val path = ".github/workflows/$fileName"
        val response = runCatching {
            api.createFile(
                token   = bearer,
                owner   = owner,
                repo    = repo,
                path    = path,
                request = CreateFileRequest(
                    message = commitMsg,
                    content = content,
                    sha     = null,      // null = criar; se já existir a API devolve 422
                    branch  = "main"
                )
            )
        }.getOrElse {
            return SetupResult.Error("inject_workflow:$fileName", "Network error: ${it.message}")
        }

        // 201 Created ou 200 OK são ambos sucesso
        // 422 significa que o ficheiro já existe — aceitamos e continuamos
        return when (response.code()) {
            201, 200, 422 -> null
            401 -> SetupResult.Error("inject_workflow:$fileName", "Token inválido")
            403 -> SetupResult.Error("inject_workflow:$fileName", "Permissão negada")
            else -> SetupResult.Error(
                "inject_workflow:$fileName",
                "GitHub HTTP ${response.code()}"
            )
        }
    }
}
