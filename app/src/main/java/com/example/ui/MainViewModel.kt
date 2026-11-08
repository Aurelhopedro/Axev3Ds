package com.example.ui

import android.app.Application
import android.util.Log
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.data.api.GeneratedFile
import com.example.data.api.GitHubRepo
import com.example.data.api.GitHubUser
import com.example.data.api.WorkflowRun
import com.example.data.auth.AuthResult
import com.example.data.auth.GitHubAuthRepository
import com.example.data.models.CharacterConfig
import com.example.data.repository.GenerationRepository
import com.example.data.repository.GenerationRepository.DownloadStatus
import com.example.data.repository.GenerationRepository.GenerationStatus
import com.example.data.repository.GitHubRepository
import com.example.data.repository.RepoCreationResult
import com.example.data.repository.WorkflowOptionsRepository
import com.example.data.repository.WorkflowOptionsRepository.DynamicWorkflowOptions
import com.example.data.repository.WorkflowUpdateResult
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch

private const val TAG = "AXEV/VM"

// ─── Estados da UI ────────────────────────────────────────────────────────────

sealed class UiState {
    object Idle : UiState()
    object Loading : UiState()
    data class Success(val message: String) : UiState()
    data class Error(val message: String) : UiState()
}

sealed class AuthState {
    object Idle : AuthState()
    data class OpenBrowser(val authUrl: String) : AuthState()
    object WaitingForCallback : AuthState()
    object Authenticated : AuthState()
    data class Failed(val message: String) : AuthState()

    // Device Flow (legado)
    object RequestingCode : AuthState()
    data class WaitingForUser(
        val userCode: String,
        val verificationUri: String,
        val deviceCode: String,
        val interval: Int,
        val expiresIn: Int
    ) : AuthState()
    object Polling : AuthState()
}

sealed class RepoCreationState {
    object Idle : RepoCreationState()
    object Creating : RepoCreationState()
    data class Success(val repo: GitHubRepo) : RepoCreationState()
    data class AlreadyExists(val name: String) : RepoCreationState()
    data class Failed(val message: String) : RepoCreationState()
}

sealed class SmokeTestState {
    object Idle : SmokeTestState()
    object Running : SmokeTestState()
    data class Started(val message: String) : SmokeTestState()
    data class Failed(val message: String) : SmokeTestState()
}

sealed class WorkspaceSetupState {
    object Idle : WorkspaceSetupState()
    object Setting : WorkspaceSetupState()
    object Done : WorkspaceSetupState()
    data class Failed(val message: String) : WorkspaceSetupState()
}

sealed class WorkflowUpdateState {
    object Idle : WorkflowUpdateState()
    object Updating : WorkflowUpdateState()
    object Done : WorkflowUpdateState()
    data class Failed(val details: List<String>) : WorkflowUpdateState()
}

// ─── ViewModel ────────────────────────────────────────────────────────────────

class MainViewModel(application: Application) : AndroidViewModel(application) {

    private val repository        = GitHubRepository(application)
    private val authRepository    = GitHubAuthRepository(application)
    private val optionsRepository = WorkflowOptionsRepository(application)
    private val generationRepo    = GenerationRepository(application)

    val token: StateFlow<String?> = repository.tokenFlow
        .stateIn(viewModelScope, SharingStarted.Lazily, null)

    val selectedRepo: StateFlow<String?> = repository.selectedRepoFlow
        .stateIn(viewModelScope, SharingStarted.Lazily, null)

    private val _uiState = MutableStateFlow<UiState>(UiState.Idle)
    val uiState: StateFlow<UiState> = _uiState.asStateFlow()

    private val _authState = MutableStateFlow<AuthState>(AuthState.Idle)
    val authState: StateFlow<AuthState> = _authState.asStateFlow()

    private val _repoCreationState = MutableStateFlow<RepoCreationState>(RepoCreationState.Idle)
    val repoCreationState: StateFlow<RepoCreationState> = _repoCreationState.asStateFlow()

    private val _smokeTestState = MutableStateFlow<SmokeTestState>(SmokeTestState.Idle)
    val smokeTestState: StateFlow<SmokeTestState> = _smokeTestState.asStateFlow()

    private val _workspaceSetupState = MutableStateFlow<WorkspaceSetupState>(WorkspaceSetupState.Idle)
    val workspaceSetupState: StateFlow<WorkspaceSetupState> = _workspaceSetupState.asStateFlow()

    private val _workflowUpdateState = MutableStateFlow<WorkflowUpdateState>(WorkflowUpdateState.Idle)
    val workflowUpdateState: StateFlow<WorkflowUpdateState> = _workflowUpdateState.asStateFlow()

    private val _user = MutableStateFlow<GitHubUser?>(null)
    val user: StateFlow<GitHubUser?> = _user.asStateFlow()

    private val _repos = MutableStateFlow<List<GitHubRepo>>(emptyList())
    val repos: StateFlow<List<GitHubRepo>> = _repos.asStateFlow()

    private val _workflowRuns = MutableStateFlow<List<WorkflowRun>>(emptyList())
    val workflowRuns: StateFlow<List<WorkflowRun>> = _workflowRuns.asStateFlow()

    private val _currentCharacter = MutableStateFlow(CharacterConfig())
    val currentCharacter: StateFlow<CharacterConfig> = _currentCharacter.asStateFlow()

    private val _workflowOptions = MutableStateFlow(DynamicWorkflowOptions())
    val workflowOptions: StateFlow<DynamicWorkflowOptions> = _workflowOptions.asStateFlow()

    private val _generationStatus = MutableStateFlow<GenerationStatus?>(null)
    val generationStatus: StateFlow<GenerationStatus?> = _generationStatus.asStateFlow()

    private val _downloadStates = MutableStateFlow<Map<Long, DownloadStatus>>(emptyMap())
    val downloadStates: StateFlow<Map<Long, DownloadStatus>> = _downloadStates.asStateFlow()

    private var lastDispatchedAt: Long = 0L
    private var pollingJob:    Job? = null
    private var generationJob: Job? = null

    init {
        if (authRepository.isAuthenticated()) {
            _authState.value = AuthState.Authenticated
            viewModelScope.launch {
                refreshUserData()
                loadWorkflowOptions()
            }
        }
    }

    // ── Opções dinâmicas do workflow ──────────────────────────────────────────

    fun loadWorkflowOptions() {
        val repo = selectedRepo.value ?: return
        viewModelScope.launch {
            val opts = optionsRepository.load(repo)
            _workflowOptions.value = opts
        }
    }

    // ── Web Flow ──────────────────────────────────────────────────────────────

    fun startGitHubAuth() {
        when (val result = authRepository.buildWebAuthUrl()) {
            is AuthResult.WebFlowStarted -> _authState.value = AuthState.OpenBrowser(result.authUrl)
            is AuthResult.Error          -> _authState.value = AuthState.Failed(result.message)
            else                         -> _authState.value = AuthState.Failed("Unexpected error starting auth")
        }
    }

    fun handleOAuthCallback(token: String) {
        when (val result = authRepository.handleWebCallback(token)) {
            is AuthResult.Success -> {
                // FIX: also save the token in GitHubRepository's tokenManager
                // so that resolveToken() finds it immediately without waiting
                // for the StateFlow to propagate.
                viewModelScope.launch {
                    repository.saveToken(result.accessToken)
                    _authState.value = AuthState.Authenticated
                    refreshUserData()
                    loadWorkflowOptions()
                }
            }
            is AuthResult.Error -> _authState.value = AuthState.Failed(result.message)
            else                -> _authState.value = AuthState.Failed("Unexpected callback error")
        }
    }

    fun onBrowserOpened() {
        _authState.value = AuthState.WaitingForCallback
    }

    fun cancelAuth() {
        pollingJob?.cancel()
        _authState.value = AuthState.Idle
    }

    // ── Demo / Legacy login ───────────────────────────────────────────────────

    fun login(newToken: String) {
        val sanitizedToken = newToken.trim()
        viewModelScope.launch {
            _uiState.value = UiState.Loading
            if (sanitizedToken == "123456789") {
                repository.saveToken(sanitizedToken)
                _user.value  = GitHubUser("axev_dev", "https://github.com/identicons/axev.png", "AXEV Developer")
                _repos.value = listOf(GitHubRepo("axev-crime-assets", "axev-studio/axev-crime-assets", _user.value!!, "main"))
                _authState.value = AuthState.Authenticated
                _uiState.value   = UiState.Success("Demo mode active")
                return@launch
            }
            val user = repository.getUser("Bearer $sanitizedToken")
            if (user != null) {
                repository.saveToken(sanitizedToken)
                _authState.value = AuthState.Authenticated
                _uiState.value   = UiState.Success("Logged in as ${user.login}")
                loadWorkflowOptions()
            } else {
                _uiState.value = UiState.Error("Invalid Token")
            }
        }
    }

    // ── Dados do utilizador ───────────────────────────────────────────────────

    private suspend fun refreshUserData() {
        // FIX: use resolveToken() so Web-Flow auth is picked up immediately
        val bearer = repository.resolveToken()
            ?: authRepository.tokenManager.getBearerToken()
        if (bearer != null) {
            _user.value  = repository.getUser(bearer)
            _repos.value = repository.getRepos(bearer)
        } else {
            val user = authRepository.fetchUser()
            if (user != null) {
                _user.value = user
                val t = authRepository.tokenManager.getBearerToken() ?: return
                _repos.value = repository.getRepos(t)
            }
        }
    }

    // ── Criação de repositório ────────────────────────────────────────────────

    fun createRepository(
        name: String,
        description: String = "Personal AXEV 3D STUDIO workspace for character generation and 3D assets.",
        private: Boolean = false
    ) {
        viewModelScope.launch {
            _repoCreationState.value = RepoCreationState.Creating

            if (token.value == "123456789") {
                delay(1500)
                val demoRepo = GitHubRepo(
                    name          = name,
                    full_name     = "demo/$name",
                    owner         = GitHubUser("demo", "https://github.com/identicons/demo.png", "Demo User"),
                    default_branch = "main",
                    html_url      = "https://github.com/demo/$name",
                    created_at    = "2026-11-08T00:00:00Z"
                )
                _repoCreationState.value = RepoCreationState.Success(demoRepo)
                repository.saveRepo("demo/$name")
                return@launch
            }

            when (val result = repository.createRepository(name, description, private)) {
                is RepoCreationResult.Success -> {
                    _repoCreationState.value = RepoCreationState.Success(result.repo)
                    val bearer = repository.resolveToken()
                    if (bearer != null) _repos.value = repository.getRepos(bearer)
                    loadWorkflowOptions()
                }
                is RepoCreationResult.AlreadyExists ->
                    _repoCreationState.value = RepoCreationState.AlreadyExists(result.name)
                is RepoCreationResult.Error ->
                    _repoCreationState.value = RepoCreationState.Failed(result.message)
            }
        }
    }

    fun resetRepoCreationState() {
        _repoCreationState.value = RepoCreationState.Idle
        _workspaceSetupState.value = WorkspaceSetupState.Idle
    }

    // ── Criar Ambiente de Trabalho ────────────────────────────────────────────

    fun setupWorkspace(ownerLogin: String, repoName: String) {
        viewModelScope.launch {
            _workspaceSetupState.value = WorkspaceSetupState.Setting

            if (token.value == "123456789") {
                delay(1500)
                _workspaceSetupState.value = WorkspaceSetupState.Done
                return@launch
            }

            val success = repository.setupWorkspace(ownerLogin, repoName)
            _workspaceSetupState.value = if (success) {
                WorkspaceSetupState.Done
            } else {
                WorkspaceSetupState.Failed("Não foi possível enviar os workflows. Verifica a ligação e tenta novamente.")
            }
        }
    }

    // ── Actualizar workflows ──────────────────────────────────────────────────

    fun updateWorkflows(repoFullName: String? = null) {
        val repo = repoFullName ?: selectedRepo.value ?: return

        if (token.value == "123456789") {
            Log.d(TAG, "updateWorkflows: demo mode — skipping")
            _workflowUpdateState.value = WorkflowUpdateState.Done
            return
        }

        viewModelScope.launch {
            Log.i(TAG, "updateWorkflows: starting for $repo")
            _workflowUpdateState.value = WorkflowUpdateState.Updating

            when (val result = repository.updateWorkflows(repo)) {
                is WorkflowUpdateResult.Success -> {
                    Log.i(TAG, "updateWorkflows: ✅ all workflows up to date")
                    _workflowUpdateState.value = WorkflowUpdateState.Done
                }
                is WorkflowUpdateResult.PartialFailure -> {
                    Log.e(TAG, "updateWorkflows: ❌ partial failure — ${result.details}")
                    _workflowUpdateState.value = WorkflowUpdateState.Failed(result.details)
                }
                is WorkflowUpdateResult.Error -> {
                    Log.e(TAG, "updateWorkflows: ❌ error — ${result.message}")
                    _workflowUpdateState.value = WorkflowUpdateState.Failed(listOf(result.message))
                }
            }
        }
    }

    fun resetWorkflowUpdateState() {
        _workflowUpdateState.value = WorkflowUpdateState.Idle
    }

    // ── Selecção de repositório ───────────────────────────────────────────────

    fun selectRepo(repoFullName: String) {
        viewModelScope.launch {
            repository.saveRepo(repoFullName)
            loadWorkflowOptions()
            if (token.value == "123456789") {
                fetchWorkflowRuns()
                return@launch
            }
            Log.i(TAG, "selectRepo: auto-updating workflows for $repoFullName")
            updateWorkflows(repoFullName)
        }
    }

    fun logout() {
        pollingJob?.cancel()
        generationJob?.cancel()
        viewModelScope.launch {
            repository.clearSession()
            authRepository.logout()
            _user.value                = null
            _repos.value               = emptyList()
            _workflowRuns.value        = emptyList()
            _workflowOptions.value     = DynamicWorkflowOptions()
            _authState.value           = AuthState.Idle
            _uiState.value             = UiState.Idle
            _repoCreationState.value   = RepoCreationState.Idle
            _smokeTestState.value      = SmokeTestState.Idle
            _workspaceSetupState.value = WorkspaceSetupState.Idle
            _workflowUpdateState.value = WorkflowUpdateState.Idle
            _generationStatus.value    = null
            _downloadStates.value      = emptyMap()
        }
    }

    fun fetchWorkflowRuns(t: String? = token.value, r: String? = selectedRepo.value) {
        if (r == null) return
        viewModelScope.launch {
            if (t == "123456789") {
                _workflowRuns.value = listOf(
                    WorkflowRun(1001, "ARA 3D GEN - SOLDIER-01", "completed", "success",
                        "2026-11-07T10:00:00Z", "2026-11-07T10:05:00Z", ""),
                    WorkflowRun(1002, "ARA 3D GEN - DETECTIVE-X", "in_progress", null,
                        "2026-11-07T11:00:00Z", "2026-11-07T11:01:00Z", "")
                )
                return@launch
            }
            // FIX: use a non-null token from resolveToken()
            val bearer = repository.resolveToken() ?: return@launch
            _workflowRuns.value = repository.getWorkflowRuns(bearer, r)
        }
    }

    fun updateCharacter(config: CharacterConfig) {
        _currentCharacter.value = config
    }

    // ── Geração de personagem ─────────────────────────────────────────────────

    fun generate() {
        val r = selectedRepo.value ?: run {
            Log.e(TAG, "generate: no repository selected")
            _generationStatus.value = GenerationStatus.Failed("No repository selected. Please select one first.")
            return
        }

        // FIX: use resolveToken() instead of the StateFlow value.
        // The StateFlow is initialized once at ViewModel creation from the
        // tokenManager and never re-emits, so a token saved after init
        // (e.g. Web Flow callback) would be invisible to it.
        val t = repository.resolveToken() ?: run {
            Log.e(TAG, "generate: no token available")
            _generationStatus.value = GenerationStatus.Failed("Not authenticated. Please log in again.")
            return
        }

        generationJob?.cancel()
        _generationStatus.value = GenerationStatus.Preparing()
        _downloadStates.value = emptyMap()

        generationJob = viewModelScope.launch {
            _uiState.value = UiState.Loading

            if (token.value == "123456789") {
                simulateDemoGeneration()
                return@launch
            }

            val config  = _currentCharacter.value
            Log.i(TAG, "generate: starting for character_id=${config.id} in $r")

            val success = repository.generateCharacter(t, r, config)
            if (!success) {
                _generationStatus.value = GenerationStatus.Failed(
                    "Failed to start generation. Check your GitHub token permissions (needs 'workflow' scope)."
                )
                _uiState.value = UiState.Error("Failed to start generation")
                return@launch
            }

            lastDispatchedAt = System.currentTimeMillis()
            _uiState.value = UiState.Success("Generation started")
            fetchWorkflowRuns(t, r)

            val characterId = config.id
            generationRepo.pollUntilReady(r, characterId, lastDispatchedAt, config.quality)
                .collect { status -> _generationStatus.value = status }
        }
    }

    fun retryGeneration() {
        generationJob?.cancel()
        _generationStatus.value = null
        _uiState.value = UiState.Idle
        _downloadStates.value = emptyMap()
    }

    // ── Download de artifact ──────────────────────────────────────────────────

    fun downloadFile(file: GeneratedFile) {
        val repo = selectedRepo.value ?: return

        if (token.value == "123456789") {
            viewModelScope.launch {
                _downloadStates.value = _downloadStates.value + (file.artifactId to DownloadStatus.Downloading)
                delay(2000)
                _downloadStates.value = _downloadStates.value + (file.artifactId to DownloadStatus.Done(file.name))
            }
            return
        }

        viewModelScope.launch {
            _downloadStates.value = _downloadStates.value + (file.artifactId to DownloadStatus.Downloading)
            val result = generationRepo.downloadArtifact(file, repo)
            _downloadStates.value = _downloadStates.value + (file.artifactId to result)
        }
    }

    // ── Demo mode ─────────────────────────────────────────────────────────────

    private suspend fun simulateDemoGeneration() {
        val steps = listOf(
            0.10f to "Preparing your character…",
            0.30f to "Building mesh structure…",
            0.50f to "Applying skin & textures…",
            0.70f to "Rigging skeleton…",
            0.90f to "Exporting 3D files…",
            0.98f to "Finalising…"
        )
        for ((progress, msg) in steps) {
            _generationStatus.value = GenerationStatus.Processing(msg, progress)
            delay(1200)
        }
        val demoFiles = listOf(
            GeneratedFile("soldier_01.glb",  "glb", 19_267_584L, "https://github.com/demo", 1001L),
            GeneratedFile("soldier_01.fbx",  "fbx", 25_296_896L, "https://github.com/demo", 1002L)
        )
        _generationStatus.value = GenerationStatus.Ready(demoFiles)
        _uiState.value = UiState.Success("Demo generation complete")
        fetchWorkflowRuns()
    }

    // ── Smoke Test ────────────────────────────────────────────────────────────

    fun runSmokeTest() {
        val r = selectedRepo.value ?: run {
            _smokeTestState.value = SmokeTestState.Failed("No repository selected")
            return
        }
        viewModelScope.launch {
            _smokeTestState.value = SmokeTestState.Running

            if (token.value == "123456789") {
                delay(1500)
                _smokeTestState.value = SmokeTestState.Started(
                    "Smoke test dispatched (Demo Mode) — 6 test cases running"
                )
                return@launch
            }

            val success = repository.runSmokeTest(r)
            _smokeTestState.value = if (success) {
                SmokeTestState.Started("Smoke test dispatched — 6 cases running on GitHub Actions")
            } else {
                SmokeTestState.Failed("Failed to dispatch smoke test. Check token permissions.")
            }
            fetchWorkflowRuns()
        }
    }

    fun resetSmokeTestState() {
        _smokeTestState.value = SmokeTestState.Idle
    }
}
