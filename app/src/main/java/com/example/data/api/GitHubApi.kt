package com.example.data.api

import com.squareup.moshi.JsonClass
import retrofit2.Response
import retrofit2.http.*

interface GitHubApi {

    // ── GitHub Device Flow ────────────────────────────────────────────────────
    // These endpoints hit https://github.com/ (not api.github.com)

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
    // These endpoints hit https://api.github.com/

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

    // ── Contents (bootstrap de ficheiros) ────────────────────────────────────

    /**
     * Cria ou substitui um ficheiro num repositório via GitHub Contents API.
     * [path] é relativo à raiz do repo (ex: ".github/workflows/smoke.yml").
     * [request.content] deve estar em Base64 (NO_WRAP).
     * [request.sha] é obrigatório apenas para actualizar um ficheiro existente.
     */
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

    @GET("repos/{owner}/{repo}/actions/runs/{run_id}/artifacts")
    suspend fun getArtifacts(
        @Header("Authorization") token: String,
        @Path("owner") owner: String,
        @Path("repo") repo: String,
        @Path("run_id") runId: Long
    ): Response<ArtifactsResponse>
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
    /** Mensagem do commit. */
    val message: String,
    /** Conteúdo do ficheiro em Base64 (Base64.NO_WRAP). */
    val content: String,
    /** SHA do ficheiro existente — obrigatório para substituir, omitir para criar. */
    val sha: String? = null,
    /** Branch de destino (default: "main"). */
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
    val archive_download_url: String,
    val expires_at: String
)
