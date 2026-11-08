package com.example.data.repository

import android.content.Context
import com.example.data.api.GeneratedFile
import com.example.data.api.GitHubApi
import com.example.data.auth.AuthTokenManager
import com.example.data.github.WorkflowTemplates
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
import retrofit2.Retrofit
import retrofit2.converter.moshi.MoshiConverterFactory
import com.squareup.moshi.Moshi
import com.squareup.moshi.kotlin.reflect.KotlinJsonAdapterFactory

/**
 * Faz polling silencioso ao GitHub Actions e devolve os ficheiros gerados
 * como se tivessem sido processados localmente.
 *
 * O UI nunca vê referências a "workflow", "Actions" ou "GitHub" —
 * recebe apenas [GenerationStatus] com progresso e ficheiros finais.
 */
class GenerationRepository(context: Context) {

    private val tokenManager = AuthTokenManager(context)

    private val api: GitHubApi by lazy {
        val moshi = Moshi.Builder().add(KotlinJsonAdapterFactory()).build()
        Retrofit.Builder()
            .baseUrl("https://api.github.com/")
            .addConverterFactory(MoshiConverterFactory.create(moshi))
            .build()
            .create(GitHubApi::class.java)
    }

    // ── Modelo de estado (sem linguagem de workflow) ───────────────────────────

    sealed class GenerationStatus {
        /** A preparar — fase inicial (0–20 %) */
        data class Preparing(val message: String = "Preparing your character…") : GenerationStatus()
        /** A processar — fase intermédia com % de progresso */
        data class Processing(val message: String, val progress: Float) : GenerationStatus()
        /** Concluído — lista de ficheiros prontos */
        data class Ready(val files: List<GeneratedFile>) : GenerationStatus()
        /** Erro — mensagem amigável sem termos técnicos */
        data class Failed(val message: String) : GenerationStatus()
    }

    // Mensagens de progresso — sem nenhuma referência a GitHub/workflow
    private val progressMessages = listOf(
        0.10f to "Preparing your character…",
        0.25f to "Building mesh structure…",
        0.40f to "Applying skin & textures…",
        0.55f to "Adding clothing & accessories…",
        0.70f to "Rigging skeleton…",
        0.82f to "Baking animations…",
        0.92f to "Exporting 3D files…",
        0.97f to "Finalising…"
    )

    // ── API pública ───────────────────────────────────────────────────────────

    /**
     * Polling contínuo desde o dispatch até os ficheiros estarem prontos.
     * Emite [GenerationStatus] a cada ciclo para o UI mostrar progresso.
     *
     * @param repoFullName  "owner/repo"
     * @param characterId   ID do personagem (usado para identificar o artifact)
     * @param dispatchedAt  timestamp (ms) do momento em que o dispatch foi feito
     */
    fun pollUntilReady(
        repoFullName: String,
        characterId: String,
        dispatchedAt: Long = System.currentTimeMillis()
    ): Flow<GenerationStatus> = flow {

        val bearer = tokenManager.getBearerToken()
            ?: run { emit(GenerationStatus.Failed("Not authenticated")); return@flow }

        val parts = repoFullName.split("/")
        if (parts.size != 2) {
            emit(GenerationStatus.Failed("Invalid repository")); return@flow
        }
        val (owner, repo) = parts

        emit(GenerationStatus.Preparing())

        var elapsedMs   = 0L
        var progressIdx = 0
        val pollInterval = 8_000L   // verificar a cada 8 s
        val timeout      = 55 * 60 * 1000L  // 55 min (workflow tem timeout de 60)

        while (elapsedMs < timeout) {

            delay(pollInterval)
            elapsedMs += pollInterval

            // Progresso visual simulado — avança suavemente
            val approxProgress = (elapsedMs.toFloat() / 180_000f).coerceIn(0.05f, 0.95f)
            val msgPair = progressMessages.lastOrNull { it.first <= approxProgress }
                ?: progressMessages.first()
            emit(GenerationStatus.Processing(msgPair.second, approxProgress))

            // Verificar se o run mais recente terminou
            val runsResp = try {
                api.getRunsForWorkflow(bearer, owner, repo,
                    WorkflowTemplates.WORKFLOW_GENERATE, perPage = 3)
            } catch (e: Exception) { continue }

            if (!runsResp.isSuccessful) continue

            val run = runsResp.body()?.workflow_runs
                ?.filter { it.created_at >= toIso(dispatchedAt - 30_000L) }
                ?.maxByOrNull { it.created_at }
                ?: continue

            when {
                run.status == "completed" && run.conclusion == "success" -> {
                    emit(GenerationStatus.Processing("Preparing your files…", 0.98f))
                    val files = fetchFiles(bearer, owner, repo, run.id, characterId)
                    if (files.isNotEmpty()) {
                        emit(GenerationStatus.Ready(files))
                        return@flow
                    }
                    // artifacts ainda a propagar — aguardar mais um ciclo
                }
                run.status == "completed" && run.conclusion != "success" -> {
                    emit(GenerationStatus.Failed(
                        "Something went wrong while generating your character. Please try again."
                    ))
                    return@flow
                }
                // in_progress / queued — continua a aguardar
            }
        }

        emit(GenerationStatus.Failed("Generation is taking longer than expected. Please try again."))
    }

    // ── Buscar e mapear ficheiros ─────────────────────────────────────────────

    private suspend fun fetchFiles(
        bearer: String,
        owner: String,
        repo: String,
        runId: Long,
        characterId: String
    ): List<GeneratedFile> {
        return try {
            val resp = api.getArtifacts(bearer, owner, repo, runId)
            if (!resp.isSuccessful) return emptyList()

            resp.body()?.artifacts
                ?.filter { it.name.contains(characterId, ignoreCase = true) }
                ?.flatMap { artifact ->
                    // Um artifact é um ZIP — apresentamos como ficheiro único
                    // com o formato principal derivado do nome
                    val ext = when {
                        artifact.name.endsWith(".glb", true) -> "glb"
                        artifact.name.endsWith(".fbx", true) -> "fbx"
                        artifact.name.endsWith(".obj", true) -> "obj"
                        artifact.name.contains("glb", true)  -> "glb"
                        artifact.name.contains("fbx", true)  -> "fbx"
                        else -> "zip"
                    }
                    listOf(
                        GeneratedFile(
                            name         = "${characterId}.${ext}",
                            extension    = ext,
                            sizeBytes    = artifact.size_in_bytes,
                            downloadUrl  = artifact.archive_download_url,
                            artifactId   = artifact.id
                        )
                    )
                } ?: emptyList()
        } catch (e: Exception) {
            emptyList()
        }
    }

    // ── Utilitário ────────────────────────────────────────────────────────────

    private fun toIso(ms: Long): String {
        val sdf = java.text.SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss'Z'",
            java.util.Locale.US).apply {
            timeZone = java.util.TimeZone.getTimeZone("UTC")
        }
        return sdf.format(java.util.Date(ms))
    }
}
