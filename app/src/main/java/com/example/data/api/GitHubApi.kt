package com.example.data.api

import com.squareup.moshi.JsonClass
import retrofit2.Response
import retrofit2.http.*

interface GitHubApi {

    // ── GitHub Device Flow ────────────────────────────────────────────────────

    @FormUrlEncoded
    @POST("login/device/code")
    suspend fun requestDeviceCode(
        @Field("client_id") clientId: String,
        @Field("scope") scope: String = "public_repo read:user"
    ): Response<DeviceCodeResponse>

    @FormUrlEncoded
    @POST("login/oauth/access_token")
    suspend fun pollAccessToken(
        @Field("client_id") clientId: String,
        @Field("device_code") deviceCode: String,
        @Field("grant_type") grantType: String = "urn:ietf:params:oauth:grant-type:device_code"
    ): Response<AccessTokenResponse>

    // ── User ──────────────────────────────────────────────────────────────────

    @GET("user")
    suspend fun getUser(
        @Header("Authorization") token: String
    ): Response<GitHubUser>

    @GET("user/repos")
    suspend fun getRepos(
        @Header("Authorization") token: String,
        @Query("sort") sort: String = "updated",
        @Query("per_page") perPage: Int = 50
    ): Response<List<GitHubRepo>>

    // ── Repositories ──────────────────────────────────────────────────────────

    @POST("user/repos")
    suspend fun createRepo(
        @Header("Authorization") token: String,
        @Body request: CreateRepoRequest
    ): Response<GitHubRepo>

    // ── Contents ──────────────────────────────────────────────────────────────

    @PUT("repos/{owner}/{repo}/contents/{path}")
    suspend fun createFile(
        @Header("Authorization") token: String,
        @Path("owner") owner: String,
        @Path("repo") repo: String,
        @Path(value = "path", encoded = true) path: String,
        @Body request: CreateFileRequest
    ): Response<CreateFileResponse>

    // ── Workflows ─────────────────────────────────────────────────────────────

    @POST("repos/{owner}/{repo}/actions/workflows/{workflow_id}/dispatches")
    suspend fun dispatchWorkflow(
        @Header("Authorization") token: String,
        @Path("owner") owner: String,
        @Path("repo") repo: String,
        @Path("workflow_id") workflowId: String,
        @Body request: WorkflowDispatchWithInputs
    ): Response<Unit>

    @GET("repos/{owner}/{repo}/actions/runs")
    suspend fun getWorkflowRuns(
        @Header("Authorization") token: String,
        @Path("owner") owner: String,
        @Path("repo") repo: String,
        @Query("per_page") perPage: Int = 10
    ): Response<WorkflowRunsResponse>

    // Runs filtrados por nome de workflow — para polling silencioso
    @GET("repos/{owner}/{repo}/actions/workflows/{workflow_id}/runs")
    suspend fun getRunsForWorkflow(
        @Header("Authorization") token: String,
        @Path("owner") owner: String,
        @Path("repo") repo: String,
        @Path("workflow_id") workflowId: String,
        @Query("per_page") perPage: Int = 5
    ): Response<WorkflowRunsResponse>

    @GET("repos/{owner}/{repo}/actions/runs/{run_id}/artifacts")
    suspend fun getArtifacts(
        @Header("Authorization") token: String,
        @Path("owner") owner: String,
        @Path("repo") repo: String,
        @Path("run_id") runId: Long
    ): Response<ArtifactsResponse>

    // Metadados de um artifact (tamanho em bytes)
    @GET("repos/{owner}/{repo}/actions/artifacts/{artifact_id}")
    suspend fun getArtifactMeta(
        @Header("Authorization") token: String,
        @Path("owner") owner: String,
        @Path("repo") repo: String,
        @Path("artifact_id") artifactId: Long
    ): Response<GitHubArtifact>
}

// ── Device Flow Models ────────────────────────────────────────────────────────

@JsonClass(generateAdapter = true)
data class DeviceCodeResponse(
    val device_code: String,
    val user_code: String,
    val verification_uri: String,
    val expires_in: Int,
    val interval: Int
)

@JsonClass(generateAdapter = true)
data class AccessTokenResponse(
    val access_token: String? = null,
    val token_type: String? = null,
    val scope: String? = null,
    val error: String? = null,
    val error_description: String? = null
)

// ── Repository Models ─────────────────────────────────────────────────────────

@JsonClass(generateAdapter = true)
data class CreateRepoRequest(
    val name: String,
    val description: String,
    val private: Boolean = false,
    val auto_init: Boolean = true
)

// ── Contents Models ───────────────────────────────────────────────────────────

@JsonClass(generateAdapter = true)
data class CreateFileRequest(
    val message: String,
    val content: String,
    val sha: String? = null,
    val branch: String = "main"
)

@JsonClass(generateAdapter = true)
data class CreateFileResponse(
    val content: FileContentMeta? = null
)

@JsonClass(generateAdapter = true)
data class FileContentMeta(
    val name: String,
    val path: String,
    val sha: String,
    val html_url: String = ""
)

// ── Core Models ───────────────────────────────────────────────────────────────

@JsonClass(generateAdapter = true)
data class GitHubUser(
    val login: String,
    val avatar_url: String,
    val name: String? = null
)

@JsonClass(generateAdapter = true)
data class GitHubRepo(
    val name: String,
    val full_name: String,
    val owner: GitHubUser,
    val default_branch: String = "main",
    val html_url: String = "",
    val description: String? = null,
    val private: Boolean = false,
    val created_at: String? = null
)

@JsonClass(generateAdapter = true)
data class WorkflowDispatchWithInputs(
    val ref: String,
    val inputs: Map<String, String>
)

@JsonClass(generateAdapter = true)
data class WorkflowRunsResponse(
    val total_count: Int,
    val workflow_runs: List<WorkflowRun>
)

@JsonClass(generateAdapter = true)
data class WorkflowRun(
    val id: Long,
    val name: String,
    val status: String,
    val conclusion: String? = null,
    val created_at: String,
    val updated_at: String,
    val html_url: String = ""
)

@JsonClass(generateAdapter = true)
data class ArtifactsResponse(
    val total_count: Int,
    val artifacts: List<GitHubArtifact>
)

@JsonClass(generateAdapter = true)
data class GitHubArtifact(
    val id: Long,
    val name: String,
    val size_in_bytes: Long = 0L,
    val archive_download_url: String,
    val expires_at: String? = null
)

// ── Modelo de ficheiro gerado (exposto ao UI) ─────────────────────────────────

data class GeneratedFile(
    val name: String,           // ex: "soldier_01.glb"
    val extension: String,      // ex: "glb"
    val sizeBytes: Long,        // tamanho real em bytes
    val downloadUrl: String,    // URL autenticado para download
    val artifactId: Long
) {
    val sizeMb: String get() {
        val mb = sizeBytes / 1_048_576.0
        return if (mb < 1.0) "${"%.0f".format(sizeBytes / 1024.0)} KB"
        else "${"%.1f".format(mb)} MB"
    }

    val icon: String get() = when (extension.lowercase()) {
        "glb"  -> "🗂"
        "fbx"  -> "🗂"
        "obj"  -> "🗂"
        "zip"  -> "📦"
        "md"   -> "📄"
        else   -> "📁"
    }
}
