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
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import java.util.TimeZone
import kotlin.math.max

/**
 * Faz polling silencioso ao GitHub Actions e devolve os ficheiros gerados.
 * O UI recebe [GenerationStatus] com progresso real baseado no run + tempo estimado.
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

    // ── Modelo de estado ──────────────────────────────────────────────────────

    sealed class GenerationStatus {
        /** Fase inicial — a aguardar o run aparecer no GitHub */
        data class Preparing(val message: String = "Preparing your character…") : GenerationStatus()

        /**
         * A processar — fase intermédia.
         * @param message      Etapa atual (ex: "Building mesh structure…")
         * @param progress     0.0–1.0
         * @param elapsedSec   Segundos decorridos desde o dispatch
         * @param estimatedSec Estimativa total em segundos (baseada em runs anteriores ou 15 min)
         */
        data class Processing(
            val message: String,
            val progress: Float,
            val elapsedSec: Int = 0,
            val estimatedSec: Int = 900   // default: 15 min
        ) : GenerationStatus()

        /** Concluído — lista de ficheiros prontos a descarregar */
        data class Ready(val files: List<GeneratedFile>) : GenerationStatus()

        /** Erro — mensagem amigável */
        data class Failed(val message: String) : GenerationStatus()
    }

    // Mapeamento de etapas do run → mensagem + progresso aproximado
    // Quando o GitHub Actions não devolve a etapa em curso, usamos o tempo decorrido
    private val progressMessages = listOf(
        0.05f to "Preparing your character…",
        0.15f to "Setting up environment…",
        0.25f to "Building mesh structure…",
        0.40f to "Applying skin & textures…",
        0.55f to "Adding clothing & accessories…",
        0.68f to "Rigging skeleton…",
        0.80f to "Baking animations…",
        0.90f to "Exporting 3D files…",
        0.97f to "Finalising…"
    )

    // Duração típica por qualidade (segundos) — ajusta-se com o histórico
    private val estimatedDurationByQuality = mapOf(
        "low"    to 480,
        "medium" to 720,
        "high"   to 960
    )

    // ── API pública ───────────────────────────────────────────────────────────

    fun pollUntilReady(
        repoFullName: String,
        characterId: String,
        dispatchedAt: Long = System.currentTimeMillis(),
        quality: String = "high"
    ): Flow<GenerationStatus> = flow {

        val bearer = tokenManager.getBearerToken()
            ?: run { emit(GenerationStatus.Failed("Not authenticated")); return@flow }

        val parts = repoFullName.split("/")
        if (parts.size != 2) {
            emit(GenerationStatus.Failed("Invalid repository")); return@flow
        }
        val (owner, repo) = parts

        emit(GenerationStatus.Preparing())

        val estimatedTotal = estimatedDurationByQuality[quality] ?: 900
        val pollInterval   = 8_000L
        val timeout        = 55 * 60 * 1000L
        var elapsedMs      = 0L
        var runStartedAt: Long? = null

        while (elapsedMs < timeout) {

            delay(pollInterval)
            elapsedMs += pollInterval

            val elapsedSec = (elapsedMs / 1000).toInt()

            // Progresso baseado no tempo decorrido vs estimativa
            val approxProgress = (elapsedSec.toFloat() / estimatedTotal.toFloat())
                .coerceIn(0.05f, 0.95f)

            val msgPair = progressMessages.lastOrNull { it.first <= approxProgress }
                ?: progressMessages.first()

            // Calcular tempo restante estimado
            val remainingSec = max(0, estimatedTotal - elapsedSec)

            emit(GenerationStatus.Processing(
                message      = msgPair.second,
                progress     = approxProgress,
                elapsedSec   = elapsedSec,
                estimatedSec = remainingSec
            ))

            // Verificar run no GitHub
            val runsResp = try {
                api.getRunsForWorkflow(bearer, owner, repo,
                    WorkflowTemplates.WORKFLOW_GENERATE, perPage = 5)
            } catch (_: Exception) { continue }

            if (!runsResp.isSuccessful) continue

            val run = runsResp.body()?.workflow_runs
                ?.filter { it.created_at >= toIso(dispatchedAt - 30_000L) }
                ?.maxByOrNull { it.created_at }
                ?: continue

            // Registar quando o run realmente começou para melhorar a estimativa
            if (runStartedAt == null && run.status == "in_progress") {
                runStartedAt = fromIso(run.created_at) ?: dispatchedAt
            }

            // Calcular progresso usando tempo real do run, se disponível
            if (runStartedAt != null) {
                val runElapsedSec = ((System.currentTimeMillis() - runStartedAt!!) / 1000).toInt()
                val runProgress = (runElapsedSec.toFloat() / estimatedTotal.toFloat())
                    .coerceIn(0.05f, 0.95f)
                val runMsg = progressMessages.lastOrNull { it.first <= runProgress }
                    ?: progressMessages.first()
                val runRemaining = max(0, estimatedTotal - runElapsedSec)

                emit(GenerationStatus.Processing(
                    message      = runMsg.second,
                    progress     = runProgress,
                    elapsedSec   = runElapsedSec,
                    estimatedSec = runRemaining
                ))
            }

            when {
                run.status == "completed" && run.conclusion == "success" -> {
                    emit(GenerationStatus.Processing(
                        message      = "Preparing your files…",
                        progress     = 0.98f,
                        elapsedSec   = elapsedSec,
                        estimatedSec = 0
                    ))
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
                ?.map { artifact ->
                    val ext = when {
                        artifact.name.contains("glb", true) -> "glb"
                        artifact.name.contains("fbx", true) -> "fbx"
                        artifact.name.contains("obj", true) -> "obj"
                        else -> "zip"
                    }
                    GeneratedFile(
                        name        = "${characterId}.${ext}",
                        extension   = ext,
                        sizeBytes   = artifact.size_in_bytes,
                        downloadUrl = artifact.archive_download_url,
                        artifactId  = artifact.id
                    )
                } ?: emptyList()
        } catch (_: Exception) {
            emptyList()
        }
    }

    // ── Utilitários de data ───────────────────────────────────────────────────

    private fun toIso(ms: Long): String {
        val sdf = SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss'Z'", Locale.US).apply {
            timeZone = TimeZone.getTimeZone("UTC")
        }
        return sdf.format(Date(ms))
    }

    private fun fromIso(iso: String): Long? = try {
        val sdf = SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss'Z'", Locale.US).apply {
            timeZone = TimeZone.getTimeZone("UTC")
        }
        sdf.parse(iso)?.time
    } catch (_: Exception) { null }
}
