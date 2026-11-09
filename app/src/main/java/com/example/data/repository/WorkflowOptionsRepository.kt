package com.example.data.repository

import android.content.Context
import android.util.Log
import com.example.data.auth.AuthTokenManager
import com.example.data.models.WorkflowOptions
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.OkHttpClient
import okhttp3.Request
import org.json.JSONObject
import java.util.Base64

private const val TAG = "AXEV/WorkflowOpts"

/**
 * Carrega as opções dos dropdowns com prioridade:
 *
 *   1. config/workflow-options.json do axev-config (remoto, público, sem token)
 *   2. YAML do repositório do utilizador (comportamento anterior)
 *   3. Listas estáticas do WorkflowOptions (fallback final no APK)
 *
 * A app nunca fica com dropdowns vazios.
 */
class WorkflowOptionsRepository(context: Context) {

    private val tokenManager = AuthTokenManager(context)
    private val http = OkHttpClient()

    // ── Repositório central de config ─────────────────────────────────────────

    private val CONFIG_OWNER  = "Aurelhopedro"
    private val CONFIG_REPO   = "axev-config"
    private val CONFIG_BRANCH = "main"
    private val CONFIG_OPTIONS_FILE = "config/workflow-options.json"
    private val RAW_BASE = "https://raw.githubusercontent.com/$CONFIG_OWNER/$CONFIG_REPO/$CONFIG_BRANCH"

    // ── Modelo de saída ───────────────────────────────────────────────────────

    data class DynamicWorkflowOptions(
        val presets: List<String>       = WorkflowOptions.PRESETS,
        val genderAge: List<String>     = WorkflowOptions.GENDER_AGE,
        val body: List<String>          = WorkflowOptions.BODY,
        val skinTone: List<String>      = WorkflowOptions.SKIN_TONE,
        val hair: List<String>          = WorkflowOptions.HAIR,
        val facialHair: List<String>    = WorkflowOptions.FACIAL_HAIR,
        val eyes: List<String>          = WorkflowOptions.EYES,
        val outfit: List<String>        = WorkflowOptions.OUTFIT,
        val shoesHat: List<String>      = WorkflowOptions.SHOES_HAT,
        val weapon: List<String>        = WorkflowOptions.WEAPON,
        val expression: List<String>    = WorkflowOptions.EXPRESSION,
        val rigAnim: List<String>       = WorkflowOptions.RIG_ANIM,
        val quality: List<String>       = WorkflowOptions.QUALITY,
        val exportFormats: List<String> = WorkflowOptions.EXPORT_FORMATS,
        // true = veio do axev-config; false = YAML do utilizador ou fallback estático
        val isLive: Boolean = false
    )

    // ── API pública ───────────────────────────────────────────────────────────

    /**
     * Tenta carregar as opções com a cadeia de prioridade descrita acima.
     * Devolve sempre um [DynamicWorkflowOptions] válido.
     */
    suspend fun load(
        repoFullName: String,
        workflowFile: String = "ara-axev-crime.yml"
    ): DynamicWorkflowOptions = withContext(Dispatchers.IO) {

        // ── 1. Tentar axev-config (JSON remoto) ───────────────────────────────
        val remoteOpts = tryLoadFromRemoteConfig()
        if (remoteOpts != null) {
            Log.i(TAG, "load: ✅ loaded from axev-config remote JSON")
            return@withContext remoteOpts
        }
        Log.w(TAG, "load: remote config unavailable — falling back to user YAML")

        // ── 2. Tentar YAML do repositório do utilizador ───────────────────────
        val yamlOpts = tryLoadFromUserYaml(repoFullName, workflowFile)
        if (yamlOpts != null) {
            Log.i(TAG, "load: ✅ loaded from user repo YAML")
            return@withContext yamlOpts
        }
        Log.w(TAG, "load: user YAML unavailable — using static fallback")

        // ── 3. Fallback estático do APK ───────────────────────────────────────
        Log.i(TAG, "load: using static WorkflowOptions fallback")
        DynamicWorkflowOptions()
    }

    // ── Fonte 1: axev-config JSON ─────────────────────────────────────────────

    private fun tryLoadFromRemoteConfig(): DynamicWorkflowOptions? {
        val url = "$RAW_BASE/$CONFIG_OPTIONS_FILE"
        Log.d(TAG, "tryLoadFromRemoteConfig: GET $url")
        return try {
            val request = Request.Builder().url(url).build()
            val body = http.newCall(request).execute().use { response ->
                if (!response.isSuccessful) {
                    Log.w(TAG, "tryLoadFromRemoteConfig: HTTP ${response.code()}")
                    return null
                }
                response.body?.string() ?: return null
            }
            parseRemoteOptionsJson(body)
        } catch (e: Exception) {
            Log.e(TAG, "tryLoadFromRemoteConfig: exception — ${e.message}", e)
            null
        }
    }

    private fun parseRemoteOptionsJson(json: String): DynamicWorkflowOptions? {
        return try {
            val obj = JSONObject(json)

            fun getList(key: String, fallback: List<String>): List<String> {
                val arr = obj.optJSONArray(key) ?: return fallback
                val list = mutableListOf<String>()
                for (i in 0 until arr.length()) list.add(arr.getString(i))
                return list.takeIf { it.isNotEmpty() } ?: fallback
            }

            DynamicWorkflowOptions(
                presets       = getList("presets",        WorkflowOptions.PRESETS),
                genderAge     = getList("gender_age",     WorkflowOptions.GENDER_AGE),
                body          = getList("body",           WorkflowOptions.BODY),
                skinTone      = getList("skin_tone",      WorkflowOptions.SKIN_TONE),
                hair          = getList("hair",           WorkflowOptions.HAIR),
                facialHair    = getList("facial_hair",    WorkflowOptions.FACIAL_HAIR),
                eyes          = getList("eyes",           WorkflowOptions.EYES),
                outfit        = getList("outfit",         WorkflowOptions.OUTFIT),
                shoesHat      = getList("shoes_hat",      WorkflowOptions.SHOES_HAT),
                weapon        = getList("weapon",         WorkflowOptions.WEAPON),
                expression    = getList("expression",     WorkflowOptions.EXPRESSION),
                rigAnim       = getList("rig_anim",       WorkflowOptions.RIG_ANIM),
                quality       = getList("quality",        WorkflowOptions.QUALITY),
                exportFormats = getList("export_formats", WorkflowOptions.EXPORT_FORMATS),
                isLive        = true
            )
        } catch (e: Exception) {
            Log.e(TAG, "parseRemoteOptionsJson: parse error — ${e.message}", e)
            null
        }
    }

    // ── Fonte 2: YAML do repositório do utilizador ────────────────────────────

    private fun tryLoadFromUserYaml(
        repoFullName: String,
        workflowFile: String
    ): DynamicWorkflowOptions? {
        val bearer = tokenManager.getBearerToken() ?: return null
        val parts  = repoFullName.split("/")
        if (parts.size != 2) return null
        val (owner, repo) = parts
        val url = "https://api.github.com/repos/$owner/$repo/contents/.github/workflows/$workflowFile"

        return try {
            val request = Request.Builder()
                .url(url)
                .header("Authorization", "Bearer $bearer")
                .header("Accept", "application/vnd.github+json")
                .build()

            val body = http.newCall(request).execute().use { response ->
                if (!response.isSuccessful) return null
                response.body?.string() ?: return null
            }

            val jsonObj  = JSONObject(body)
            val encoded  = jsonObj.getString("content").replace("\n", "")
            val yaml     = String(Base64.getDecoder().decode(encoded))

            val opts = parseWorkflowOptions(yaml)
            // Devolve null se o parse não trouxe nada útil (todas as listas são fallback)
            opts.takeIf { !it.isLive }?.let { return null }
            opts
        } catch (e: Exception) {
            Log.e(TAG, "tryLoadFromUserYaml: exception — ${e.message}", e)
            null
        }
    }

    // ── Parser YAML leve (mantido do código original) ─────────────────────────

    private fun parseWorkflowOptions(yaml: String): DynamicWorkflowOptions {
        val map = mutableMapOf<String, MutableList<String>>()
        var currentInput: String? = null
        var insideOptions = false

        for (rawLine in yaml.lines()) {
            val line = rawLine.trimEnd()

            val inputMatch = Regex("""^ {6}(\w+):$""").find(line)
            if (inputMatch != null) {
                currentInput = inputMatch.groupValues[1]
                insideOptions = false
                continue
            }

            if (line.trimStart() == "options:" && currentInput != null) {
                insideOptions = true
                map.getOrPut(currentInput) { mutableListOf() }
                continue
            }

            if (insideOptions && currentInput != null) {
                val itemMatch = Regex("""^ {10}- ["']?(.+?)["']?$""").find(line)
                if (itemMatch != null) {
                    map.getOrPut(currentInput) { mutableListOf() }
                        .add(itemMatch.groupValues[1])
                    continue
                }
                if (line.isNotBlank() && !line.startsWith("          ")) {
                    insideOptions = false
                }
            }
        }

        fun get(key: String, fallback: List<String>) =
            map[key]?.takeIf { it.isNotEmpty() } ?: fallback

        return DynamicWorkflowOptions(
            presets       = get("preset",         WorkflowOptions.PRESETS),
            genderAge     = get("gender_age",     WorkflowOptions.GENDER_AGE),
            body          = get("body",           WorkflowOptions.BODY),
            skinTone      = get("skin_tone",      WorkflowOptions.SKIN_TONE),
            hair          = get("hair",           WorkflowOptions.HAIR),
            facialHair    = get("facial_hair",    WorkflowOptions.FACIAL_HAIR),
            eyes          = get("eyes",           WorkflowOptions.EYES),
            outfit        = get("outfit",         WorkflowOptions.OUTFIT),
            shoesHat      = get("shoes_hat",      WorkflowOptions.SHOES_HAT),
            weapon        = get("weapon",         WorkflowOptions.WEAPON),
            expression    = get("expression",     WorkflowOptions.EXPRESSION),
            rigAnim       = get("rig_anim",       WorkflowOptions.RIG_ANIM),
            quality       = get("quality",        WorkflowOptions.QUALITY),
            exportFormats = get("export_formats", WorkflowOptions.EXPORT_FORMATS),
            isLive        = true
        )
    }
}
