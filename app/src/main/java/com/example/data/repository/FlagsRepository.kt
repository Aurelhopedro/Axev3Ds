package com.example.data.repository

import android.content.Context
import android.util.Log
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.OkHttpClient
import okhttp3.Request
import org.json.JSONObject

private const val TAG = "AXEV/Flags"

/**
 * Busca as feature flags do repositório central axev-config.
 *
 * Prioridade:
 *   1. config/flags.json do axev-config (remoto, público, sem token)
 *   2. [AppFlags] com valores por defeito (fallback — a app nunca quebra)
 *
 * Uso no ViewModel:
 *   val flags = flagsRepository.load()
 *   if (!flags.downloadEnabled) { ... }
 *   if (flags.maintenanceMode) { mostrar banner }
 */
class FlagsRepository(private val context: Context) {

    private val http = OkHttpClient()

    private val CONFIG_OWNER  = "Aurelhopedro"
    private val CONFIG_REPO   = "axev-config"
    private val CONFIG_BRANCH = "main"
    private val FLAGS_FILE    = "config/flags.json"
    private val RAW_BASE      = "https://raw.githubusercontent.com/$CONFIG_OWNER/$CONFIG_REPO/$CONFIG_BRANCH"

    // ── Modelo de flags ───────────────────────────────────────────────────────

    data class AppFlags(
        // Geração
        val maxQuality: String              = "high",
        val pollIntervalSeconds: Int        = 8,
        val generationTimeoutMinutes: Int   = 55,

        // Downloads
        val downloadEnabled: Boolean        = true,
        val maxArtifactRetentionDays: Int   = 7,

        // UI
        val smokeTestEnabled: Boolean       = true,
        val demoModeEnabled: Boolean        = true,
        val viewer3dEnabled: Boolean        = true,

        // Manutenção
        val maintenanceMode: Boolean        = false,
        val maintenanceMessage: String      = "",

        // App
        val minAppVersion: String           = "1.0.0",

        // Workflows
        val multiWorkflowEnabled: Boolean   = true,
        val allowUserAddWorkflow: Boolean   = false,

        // true = veio do axev-config; false = defeito local
        val isLive: Boolean                 = false
    )

    // ── API pública ───────────────────────────────────────────────────────────

    suspend fun load(): AppFlags = withContext(Dispatchers.IO) {
        val url = "$RAW_BASE/$FLAGS_FILE"
        Log.d(TAG, "load: GET $url")

        try {
            val request = Request.Builder().url(url).build()
            val body = http.newCall(request).execute().use { response ->
                if (!response.isSuccessful) {
                    Log.w(TAG, "load: HTTP ${response.code} — using defaults")
                    return@withContext AppFlags()
                }
                response.body?.string() ?: return@withContext AppFlags()
            }
            parseFlags(body)
        } catch (e: Exception) {
            Log.e(TAG, "load: exception — ${e.message}", e)
            AppFlags()
        }
    }

    // ── Parser ────────────────────────────────────────────────────────────────

    private fun parseFlags(json: String): AppFlags {
        return try {
            val root = JSONObject(json)

            val gen   = root.optJSONObject("generation")
            val dl    = root.optJSONObject("downloads")
            val ui    = root.optJSONObject("ui")
            val maint = root.optJSONObject("maintenance")
            val app   = root.optJSONObject("app")
            val wf    = root.optJSONObject("workflows")

            AppFlags(
                maxQuality                = gen?.optString("max_quality", "high") ?: "high",
                pollIntervalSeconds       = gen?.optInt("poll_interval_seconds", 8) ?: 8,
                generationTimeoutMinutes  = gen?.optInt("generation_timeout_minutes", 55) ?: 55,

                downloadEnabled           = dl?.optBoolean("download_enabled", true) ?: true,
                maxArtifactRetentionDays  = dl?.optInt("max_artifact_retention_days", 7) ?: 7,

                smokeTestEnabled          = ui?.optBoolean("smoke_test_enabled", true) ?: true,
                demoModeEnabled           = ui?.optBoolean("demo_mode_enabled", true) ?: true,
                viewer3dEnabled           = ui?.optBoolean("viewer_3d_enabled", true) ?: true,

                maintenanceMode           = maint?.optBoolean("maintenance_mode", false) ?: false,
                maintenanceMessage        = maint?.optString("maintenance_message", "") ?: "",

                minAppVersion             = app?.optString("min_app_version", "1.0.0") ?: "1.0.0",

                multiWorkflowEnabled      = wf?.optBoolean("multi_workflow_enabled", true) ?: true,
                allowUserAddWorkflow      = wf?.optBoolean("allow_user_add_workflow", false) ?: false,

                isLive = true
            ).also {
                Log.i(TAG, "load: ✅ flags loaded from axev-config (maintenance=${it.maintenanceMode}, download=${it.downloadEnabled})")
            }
        } catch (e: Exception) {
            Log.e(TAG, "parseFlags: parse error — ${e.message}", e)
            AppFlags()
        }
    }
}
