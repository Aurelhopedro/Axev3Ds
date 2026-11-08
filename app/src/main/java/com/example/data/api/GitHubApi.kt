package com.example.data.api

import com.squareup.moshi.JsonClass
import retrofit2.Response
import retrofit2.http.*

interface GitHubApi {
    @GET("user")
    suspend fun getUser(
        @Header("Authorization") token: String
    ): Response<GitHubUser>

    @GET("user/repos")
    suspend fun getRepos(
        @Header("Authorization") token: String,
        @Query("sort") sort: String = "updated"
    ): Response<List<GitHubRepo>>

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

@JsonClass(generateAdapter = true)
data class GitHubUser(
    val login: String,
    val avatar_url: String,
    val name: String?
)

@JsonClass(generateAdapter = true)
data class GitHubRepo(
    val name: String,
    val full_name: String,
    val owner: GitHubUser,
    val default_branch: String
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
    val conclusion: String?,
    val created_at: String,
    val updated_at: String,
    val html_url: String
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
