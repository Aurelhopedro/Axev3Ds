package com.example.data.repository

import android.content.Context
import android.util.Log
import com.example.data.github.WorkflowTemplates
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.OkHttpClient
import okhttp3.Request
import org.json.JSONObject
import java.util.Base64

private const val TAG = "AXEV/RemoteConfig"

/**
 * Busca os workflows do repositório central axev-config (Aurelhopedro/axev-config).
 *
 * Antes de cada geração, o MainViewModel chama [fetchWorkflowIfNewer].
 * Se a versão remota for mais recente que a local, devolve o conteúdo novo.
 * Se falhar (sem rede, repo privado sem token, etc.) devolve null —
 * e o caller usa o WorkflowTemplates do APK como fallback.
 *
 * Quando o repositório for tornado privado, basta adicionar AXEV_CONFIG_TOKEN
 * nas variáveis de ambiente do backend e passar o token aqui.
 */
class RemoteConfigRepository(private val context: Context) {

    private val http = OkHttpClient()

    // ── Configuração do repositório central ───────────────────────────────────

    private val CONFIG_OWNER   = "Aurelhopedro"
    private val CONFIG_REPO    = "axev-config"
    private val CONFIG_BRANCH  = "main"
    private val CONFIG_FILE    = "config/workflow-config.json"
    private val RAW_BASE       = "https://raw.githubusercontent.com/$CONFIG_OWNER/$CONFIG_REPO/$CONFIG_BRANCH"

    // Versão local embutida no APK — incrementar quando WorkflowTemplates mudar
    private val LOCAL_VERSION  = "1.0.0"

    // ── Modelo ────────────────────────────────────────────────────────────────

    data class RemoteWorkflow(
        val filename: String,
        val version: String,
        val content: String       // conteúdo YAML em texto
    )

    data class RemoteConfig(
        val version: String,
        val workflows: List<RemoteWorkflow>
    )

    // ── API pública ───────────────────────────────────────────────────────────

    /**
     * Verifica se há uma versão mais recente do [workflowFilename] no config remoto.
     * Devolve o conteúdo YAML se for mais recente, null se falhar ou for igual.
     *
     * O caller (MainViewModel / GitHubRepository) usa este conteúdo para fazer
     * updateWorkflows() antes do dispatch.
     */
    suspend fun fetchWorkflowIfNewer(
        workflowFilename: String = WorkflowTemplates.WORKFLOW_GENERATE,
        token: String? = null
    ): String? = withContext(Dispatchers.IO) {
        try {
            // 1. Buscar o manifesto de versões
            val config = fetchConfig(token) ?: run {
                Log.w(TAG, "fetchWorkflowIfNewer: could not fetch config manifest")
                return@withContext null
            }

            // 2. Verificar se há versão mais recente
            val remoteWorkflow = config.workflows.find { it.filename == workflowFilename }
            if (remoteWorkflow == null) {
                Log.w(TAG, "fetchWorkflowIfNewer: '$workflowFilename' not found in remote config")
                return@withContext null
            }

            if (!isNewer(remoteWorkflow.version, LOCAL_VERSION)) {
                Log.d(TAG, "fetchWorkflowIfNewer: local ($LOCAL_VERSION) >= remote (${remoteWorkflow.version}) — no update needed")
                return@withContext null
            }

            Log.i(TAG, "fetchWorkflowIfNewer: remote ${remoteWorkflow.version} > local $LOCAL_VERSION — fetching content")

            // 3. Buscar o conteúdo do workflow
            val yaml = fetchWorkflowContent(workflowFilename, token) ?: run {
                Log.w(TAG, "fetchWorkflowIfNewer: could not fetch workflow content")
                return@withContext null
            }

            Log.i(TAG, "fetchWorkflowIfNewer: ✅ got remote workflow ($workflowFilename) ${yaml.length} chars")
            yaml

        } catch (e: Exception) {
            Log.e(TAG, "fetchWorkflowIfNewer: exception — ${e.message}", e)
            null
        }
    }

    /**
     * Força a busca de todos os workflows remotos, independente de versão.
     * Usado pelo updateWorkflows() quando o utilizador carrega em "Atualizar".
     */
    suspend fun fetchAllWorkflows(token: String? = null): Map<String, String> =
        withContext(Dispatchers.IO) {
            val result = mutableMapOf<String, String>()
            try {
                val config = fetchConfig(token) ?: return@withContext result
                for (wf in config.workflows) {
                    val yaml = fetchWorkflowContent(wf.filename, token)
                    if (yaml != null) {
                        result[wf.filename] = yaml
                        Log.i(TAG, "fetchAllWorkflows: ✅ ${wf.filename} (${yaml.length} chars)")
                    } else {
                        Log.w(TAG, "fetchAllWorkflows: ⚠️ could not fetch ${wf.filename}")
                    }
                }
            } catch (e: Exception) {
                Log.e(TAG, "fetchAllWorkflows: exception — ${e.message}", e)
            }
            result
        }

    // ── Internos ──────────────────────────────────────────────────────────────

    private fun fetchConfig(token: String?): RemoteConfig? {
        val url = "$RAW_BASE/$CONFIG_FILE"
        Log.d(TAG, "fetchConfig: GET $url")
        return try {
            val request = buildRequest(url, token)
            val body = http.newCall(request).execute().use { response ->
                if (!response.isSuccessful) {
                    Log.w(TAG, "fetchConfig: HTTP ${response.code()}")
                    return null
                }
                response.body?.string() ?: return null
            }
            parseConfig(body)
        } catch (e: Exception) {
            Log.e(TAG, "fetchConfig: exception — ${e.message}", e)
            null
        }
    }

    private fun fetchWorkflowContent(filename: String, token: String?): String? {
        // Tenta raw primeiro (mais rápido, sem autenticação para repos públicos)
        val rawUrl = "$RAW_BASE/workflows/$filename"
        Log.d(TAG, "fetchWorkflowContent: GET $rawUrl")
        return try {
            val request = buildRequest(rawUrl, token)
            http.newCall(request).execute().use { response ->
                if (!response.isSuccessful) {
                    Log.w(TAG, "fetchWorkflowContent: HTTP ${response.code()} for $filename")
                    return null
                }
                response.body?.string()
            }
        } catch (e: Exception) {
            Log.e(TAG, "fetchWorkflowContent: exception — ${e.message}", e)
            null
        }
    }

    private fun buildRequest(url: String, token: String?): Request {
        val builder = Request.Builder().url(url)
        if (token != null) {
            val bearer = if (token.startsWith("Bearer ")) token else "Bearer $token"
            builder.header("Authorization", bearer)
        }
        return builder.build()
    }

    private fun parseConfig(json: String): RemoteConfig? {
        return try {
            val obj      = JSONObject(json)
            val version  = obj.getString("version")
            val wfArray  = obj.getJSONArray("workflows")
            val workflows = mutableListOf<RemoteWorkflow>()
            for (i in 0 until wfArray.length()) {
                val wf = wfArray.getJSONObject(i)
                workflows.add(
                    RemoteWorkflow(
                        filename    = wf.getString("filename"),
                        version     = wf.getString("version"),
                        content     = ""   // conteúdo é buscado separadamente
                    )
                )
            }
            RemoteConfig(version = version, workflows = workflows)
        } catch (e: Exception) {
            Log.e(TAG, "parseConfig: JSON parse error — ${e.message}", e)
            null
        }
    }

    /**
     * Compara versões semver simples (major.minor.patch).
     * Devolve true se [remote] > [local].
     */
    private fun isNewer(remote: String, local: String): Boolean {
        return try {
            val r = remote.split(".").map { it.toInt() }
            val l = local.split(".").map { it.toInt() }
            for (i in 0 until maxOf(r.size, l.size)) {
                val rv = r.getOrElse(i) { 0 }
                val lv = l.getOrElse(i) { 0 }
                if (rv > lv) return true
                if (rv < lv) return false
            }
            false
        } catch (e: Exception) {
            false
        }
    }
}
