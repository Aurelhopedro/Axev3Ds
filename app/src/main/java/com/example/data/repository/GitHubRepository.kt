package com.example.data.repository

import android.content.Context
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import com.example.data.api.GitHubApi
import com.example.data.api.GitHubUser
import com.example.data.api.GitHubRepo
import com.example.data.api.WorkflowRun
import com.example.data.api.WorkflowDispatchWithInputs
import com.example.data.models.CharacterConfig
import com.squareup.moshi.Moshi
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import retrofit2.Retrofit
import retrofit2.converter.moshi.MoshiConverterFactory

private val Context.dataStore by preferencesDataStore(name = "github_prefs")

class GitHubRepository(private val context: Context) {
    private val moshi = Moshi.Builder().build()
    private val api = Retrofit.Builder()
        .baseUrl("https://api.github.com/")
        .addConverterFactory(MoshiConverterFactory.create(moshi))
        .build()
        .create(GitHubApi::class.java)

    private val GITHUB_TOKEN_KEY = stringPreferencesKey("github_token")
    private val SELECTED_REPO_KEY = stringPreferencesKey("selected_repo")

    val tokenFlow: Flow<String?> = context.dataStore.data.map { it[GITHUB_TOKEN_KEY] }
    val selectedRepoFlow: Flow<String?> = context.dataStore.data.map { it[SELECTED_REPO_KEY] }

    suspend fun saveToken(token: String) {
        val sanitized = token.trim()
        context.dataStore.edit { it[GITHUB_TOKEN_KEY] = "Bearer $sanitized" }
    }

    suspend fun saveRepo(repoFullName: String) {
        context.dataStore.edit { it[SELECTED_REPO_KEY] = repoFullName }
    }

    suspend fun clearSession() {
        context.dataStore.edit {
            it.remove(GITHUB_TOKEN_KEY)
            it.remove(SELECTED_REPO_KEY)
        }
    }

    suspend fun getUser(token: String): GitHubUser? {
        val response = api.getUser(token.trim())
        return if (response.isSuccessful) response.body() else null
    }

    suspend fun getRepos(token: String): List<GitHubRepo> {
        val response = api.getRepos(token.trim())
        return if (response.isSuccessful) response.body() ?: emptyList() else emptyList()
    }

    suspend fun generateCharacter(
        token: String,
        repoFullName: String,
        config: CharacterConfig
    ): Boolean {
        val parts = repoFullName.split("/")
        if (parts.size != 2) return false
        val owner = parts[0]
        val repo = parts[1]

        val inputs = mapOf(
            "character_id" to config.id,
            "character_name" to config.name,
            "preset" to config.identity.category,
            "quality" to config.style.quality,
            "export_formats" to config.export.formats.joinToString(","),
            "config_json" to moshi.adapter(CharacterConfig::class.java).toJson(config)
        )

        val request = WorkflowDispatchWithInputs(
            ref = "main", // or default branch
            inputs = inputs
        )

        val response = api.dispatchWorkflow(token.trim(), owner, repo, "ara-axev-crime.yml", request)
        return response.isSuccessful
    }

    suspend fun getWorkflowRuns(token: String, repoFullName: String): List<WorkflowRun> {
        val parts = repoFullName.split("/")
        if (parts.size != 2) return emptyList()
        val response = api.getWorkflowRuns(token.trim(), parts[0], parts[1])
        return if (response.isSuccessful) response.body()?.workflow_runs ?: emptyList() else emptyList()
    }
}
