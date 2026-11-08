package com.example.data.repository

import android.content.Context
import com.example.data.auth.AuthTokenManager
import com.example.data.models.WorkflowOptions
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.OkHttpClient
import okhttp3.Request
import org.json.JSONObject
import java.util.Base64

/**
 * Carrega as opções dos workflows directamente do repositório do utilizador
 * via GitHub Contents API, faz parse do YAML de forma leve (sem biblioteca
 * externa) e devolve um [DynamicWorkflowOptions] pronto a usar nos dropdowns.
 *
 * Se a chamada falhar (sem rede, repo ainda não criado, etc.) devolve as
 * opções estáticas de [WorkflowOptions] como fallback — o app nunca fica vazio.
 */
class WorkflowOptionsRepository(context: Context) {

    private val tokenManager = AuthTokenManager(context)
    private val http = OkHttpClient()

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
        // true = veio do GitHub; false = fallback estático
        val isLive: Boolean = false
    )

    // ── API pública ───────────────────────────────────────────────────────────

    /**
     * Tenta carregar as opções do workflow [workflowFile] no [repoFullName].
     * Devolve sempre um [DynamicWorkflowOptions] válido.
     */
    suspend fun load(
        repoFullName: String,
        workflowFile: String = "ara-axev-crime.yml"
    ): DynamicWorkflowOptions = withContext(Dispatchers.IO) {
        val bearer = tokenManager.getBearerToken()
            ?: return@withContext DynamicWorkflowOptions()

        val parts = repoFullName.split("/")
        if (parts.size != 2) return@withContext DynamicWorkflowOptions()

        val (owner, repo) = parts
        val url = "https://api.github.com/repos/$owner/$repo/contents/.github/workflows/$workflowFile"

        return@withContext try {
            val request = Request.Builder()
                .url(url)
                .header("Authorization", "Bearer $bearer")
                .header("Accept", "application/vnd.github+json")
                .build()

            val body = http.newCall(request).execute().use { response ->
                if (!response.isSuccessful) return@withContext DynamicWorkflowOptions()
                response.body?.string() ?: return@withContext DynamicWorkflowOptions()
            }

            // A API do GitHub devolve o conteúdo em base64
            val json    = JSONObject(body)
            val encoded = json.getString("content").replace("\n", "")
            val yaml    = String(Base64.getDecoder().decode(encoded))

            parseWorkflowOptions(yaml)
        } catch (e: Exception) {
            DynamicWorkflowOptions()   // fallback silencioso
        }
    }

    // ── Parser YAML leve ──────────────────────────────────────────────────────
    //
    // O workflow segue um padrão consistente:
    //
    //   inputs:
    //     preset:
    //       description: "..."
    //       type: choice
    //       options:
    //         - "Option A"
    //         - "Option B"
    //
    // Fazemos parse linha a linha sem dependência externa.

    private fun parseWorkflowOptions(yaml: String): DynamicWorkflowOptions {
        val map = mutableMapOf<String, MutableList<String>>()
        var currentInput: String? = null
        var insideOptions = false

        for (rawLine in yaml.lines()) {
            val line = rawLine.trimEnd()

            // Detectar bloco de inputs (nível 2 — 6 espaços)
            val inputMatch = Regex("""^ {6}(\w+):$""").find(line)
            if (inputMatch != null) {
                currentInput = inputMatch.groupValues[1]
                insideOptions = false
                continue
            }

            // Detectar bloco "options:" (nível 3 — 8 espaços)
            if (line.trimStart() == "options:" && currentInput != null) {
                insideOptions = true
                map.getOrPut(currentInput) { mutableListOf() }
                continue
            }

            // Recolher itens da lista de opções (nível 4 — 10 espaços + "- ")
            if (insideOptions && currentInput != null) {
                val itemMatch = Regex("""^ {10}- ["']?(.+?)["']?$""").find(line)
                if (itemMatch != null) {
                    map.getOrPut(currentInput) { mutableListOf() }
                        .add(itemMatch.groupValues[1])
                    continue
                }
                // Saímos do bloco de opções se a indentação mudar
                if (line.isNotBlank() && !line.startsWith("          ")) {
                    insideOptions = false
                }
            }
        }

        // Mapear nomes dos inputs do workflow para campos do modelo
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
