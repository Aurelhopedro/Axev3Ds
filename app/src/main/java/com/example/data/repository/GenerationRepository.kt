package com.example.data.repository

import android.content.ContentValues
import android.content.Context
import android.os.Build
import android.provider.MediaStore
import com.example.data.api.GeneratedFile
import com.example.data.api.GitHubApi
import com.example.data.auth.AuthTokenManager
import com.example.data.github.WorkflowTemplates
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.withContext
import okhttp3.OkHttpClient
import okhttp3.Request
import retrofit2.Retrofit
import retrofit2.converter.moshi.MoshiConverterFactory
import com.squareup.moshi.Moshi
import com.squareup.moshi.kotlin.reflect.KotlinJsonAdapterFactory
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import java.util.TimeZone
import java.util.concurrent.TimeUnit
import kotlin.math.max

/**
 * Faz polling silencioso ao GitHub Actions e devolve os ficheiros gerados.
 * O UI recebe [GenerationStatus] com progresso real baseado no run + tempo estimado.
 * Inclui download nativo de artifacts via MediaStore (sem abrir browser).
 *
 * [pollIntervalMs] e [timeoutMs] são controláveis remotamente via FlagsRepository.
 */
class GenerationRepository(private val context: Context) {

    private val tokenManager = AuthTokenManager(context)

    private val api: GitHubApi by lazy {
        val moshi = Moshi.Builder().add(KotlinJsonAdapterFactory()).build()
        Retrofit.Builder()
            .baseUrl("https://api.github.com/")
            .addConverterFactory(MoshiConverterFactory.create(moshi))
            .build()
            .create(GitHubApi::class.java)
    }

    // Cliente OkHttp dedicado ao download — NÃO segue redirects automaticamente.
    private val downloadClient: OkHttpClient by lazy {
        OkHttpClient.Builder()
            .followRedirects(false)
            .followSslRedirects(false)
            .connectTimeout(30, TimeUnit.SECONDS)
            .readTimeout(5, TimeUnit.MINUTES)
            .build()
    }

    // ── Modelo de estado ──────────────────────────────────────────────────────

    sealed class GenerationStatus {
        data class Preparing(val message: String = "Preparing your character…") : GenerationStatus()
        data class Processing(
            val message: String,
            val progress: Float,
            val elapsedSec: Int = 0,
            val estimatedSec: Int = 900
        ) : GenerationStatus()
        data class Ready(val files: List<GeneratedFile>) : GenerationStatus()
        data class Failed(val message: String) : GenerationStatus()
    }

    sealed class DownloadStatus {
        object Downloading : DownloadStatus()
        data class Done(val fileName: String) : DownloadStatus()
        data class Failed(val message: String) : DownloadStatus()
    }

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

    private val estimatedDurationByQuality = mapOf(
        "low"    to 480,
        "medium" to 720,
        "high"   to 960
    )

    // ── Polling ───────────────────────────────────────────────────────────────

    /**
     * @param pollIntervalMs  Intervalo de polling em ms — controlável via flags (default 8s)
     * @param timeoutMs       Timeout total em ms — controlável via flags (default 55min)
     */
    fun pollUntilReady(
        repoFullName: String,
        characterId: String,
        dispatchedAt: Long = System.currentTimeMillis(),
        quality: String = "high",
        pollIntervalMs: Long = 8_000L,
        timeoutMs: Long = 55 * 60 * 1000L
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
        var elapsedMs      = 0L
        var runStartedAt: Long? = null

        while (elapsedMs < timeoutMs) {

            delay(pollIntervalMs)
            elapsedMs += pollIntervalMs

            val elapsedSec = (elapsedMs / 1000).toInt()
            val approxProgress = (elapsedSec.toFloat() / estimatedTotal.toFloat())
                .coerceIn(0.05f, 0.95f)
            val msgPair = progressMessages.lastOrNull { it.first <= approxProgress }
                ?: progressMessages.first()
            val remainingSec = max(0, estimatedTotal - elapsedSec)

            emit(GenerationStatus.Processing(
                message      = msgPair.second,
                progress     = approxProgress,
                elapsedSec   = elapsedSec,
                estimatedSec = remainingSec
            ))

            val runsResp = try {
                api.getRunsForWorkflow(bearer, owner, repo,
                    WorkflowTemplates.WORKFLOW_GENERATE, perPage = 5)
            } catch (_: Exception) { continue }

            if (!runsResp.isSuccessful) continue

            val run = runsResp.body()?.workflow_runs
                ?.filter { it.created_at >= toIso(dispatchedAt - 30_000L) }
                ?.maxByOrNull { it.created_at }
                ?: continue

            if (runStartedAt == null && run.status == "in_progress") {
                runStartedAt = fromIso(run.created_at) ?: dispatchedAt
            }

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

    // ── Download nativo de artifact ───────────────────────────────────────────

    suspend fun downloadArtifact(
        file: GeneratedFile,
        repoFullName: String
    ): DownloadStatus = withContext(Dispatchers.IO) {

        val bearer = tokenManager.getBearerToken()
            ?: return@withContext DownloadStatus.Failed("Not authenticated")

        val parts = repoFullName.split("/")
        if (parts.size != 2) return@withContext DownloadStatus.Failed("Invalid repository")
        val (owner, repo) = parts

        try {
            val redirectReq = Request.Builder()
                .url("https://api.github.com/repos/$owner/$repo/actions/artifacts/${file.artifactId}/zip")
                .addHeader("Authorization", bearer)
                .addHeader("Accept", "application/vnd.github+json")
                .addHeader("X-GitHub-Api-Version", "2022-11-28")
                .build()

            val redirectResp = downloadClient.newCall(redirectReq).execute()

            if (redirectResp.code != 302) {
                return@withContext DownloadStatus.Failed(
                    "Could not get download link (${redirectResp.code})"
                )
            }

            val locationUrl = redirectResp.header("Location")
                ?: return@withContext DownloadStatus.Failed("No download URL received")

            redirectResp.close()

            val downloadReq = Request.Builder()
                .url(locationUrl)
                .build()

            val downloadResp = downloadClient.newCall(downloadReq).execute()

            if (!downloadResp.isSuccessful) {
                return@withContext DownloadStatus.Failed(
                    "Download failed (${downloadResp.code})"
                )
            }

            val body = downloadResp.body
                ?: return@withContext DownloadStatus.Failed("Empty response")

            val fileName = "${file.name.substringBeforeLast(".")}.zip"

            val contentValues = ContentValues().apply {
                put(MediaStore.Downloads.DISPLAY_NAME, fileName)
                put(MediaStore.Downloads.MIME_TYPE, "application/zip")
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                    put(MediaStore.Downloads.IS_PENDING, 1)
                }
            }

            val resolver = context.contentResolver
            val collection = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                MediaStore.Downloads.getContentUri(MediaStore.VOLUME_EXTERNAL_PRIMARY)
            } else {
                MediaStore.Downloads.EXTERNAL_CONTENT_URI
            }

            val uri = resolver.insert(collection, contentValues)
                ?: return@withContext DownloadStatus.Failed("Could not create file in Downloads")

            resolver.openOutputStream(uri)?.use { out ->
                body.byteStream().copyTo(out)
            } ?: return@withContext DownloadStatus.Failed("Could not write file")

            downloadResp.close()

            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                contentValues.clear()
                contentValues.put(MediaStore.Downloads.IS_PENDING, 0)
                resolver.update(uri, contentValues, null, null)
            }

            DownloadStatus.Done(fileName)

        } catch (e: Exception) {
            DownloadStatus.Failed("Download error: ${e.message ?: "Unknown error"}")
        }
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
