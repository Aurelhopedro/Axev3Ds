package com.example.ui

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.data.api.GitHubRepo
import com.example.data.api.GitHubUser
import com.example.data.api.WorkflowRun
import com.example.data.auth.AuthResult
import com.example.data.auth.GitHubAuthRepository
import com.example.data.models.CharacterConfig
import com.example.data.repository.GitHubRepository
import com.example.data.repository.RepoCreationResult
import com.example.data.repository.WorkflowOptionsRepository
import com.example.data.repository.WorkflowOptionsRepository.DynamicWorkflowOptions
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch

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

// ─── ViewModel ────────────────────────────────────────────────────────────────

class MainViewModel(application: Application) : AndroidViewModel(application) {

    private val repository        = GitHubRepository(application)
    private val authRepository    = GitHubAuthRepository(application)
    private val optionsRepository = WorkflowOptionsRepository(application)

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

    private val _user = MutableStateFlow<GitHubUser?>(null)
    val user: StateFlow<GitHubUser?> = _user.asStateFlow()

    private val _repos = MutableStateFlow<List<GitHubRepo>>(emptyList())
    val repos: StateFlow<List<GitHubRepo>> = _repos.asStateFlow()

    private val _workflowRuns = MutableStateFlow<List<WorkflowRun>>(emptyList())
    val workflowRuns: StateFlow<List<WorkflowRun>> = _workflowRuns.asStateFlow()

    private val _currentCharacter = MutableStateFlow(CharacterConfig())
    val currentCharacter: StateFlow<CharacterConfig> = _currentCharacter.asStateFlow()

    // Opções dinâmicas carregadas do YAML do workflow
    private val _workflowOptions = MutableStateFlow(DynamicWorkflowOptions())
    val workflowOptions: StateFlow<DynamicWorkflowOptions> = _workflowOptions.asStateFlow()

    private var pollingJob: Job? = null

    init {
        if (authRepository.isAuthenticated()) {
            _authState.value = AuthState.Authenticated
            viewModelScope.launch {
                refreshUserData()
                loadWorkflowOptions()   // carrega opções logo ao iniciar
            }
        }
    }

    // ── Opções dinâmicas do workflow ──────────────────────────────────────────

    /**
     * Carrega as opções directamente do YAML do workflow no repo do utilizador.
     * Se falhar, as opções estáticas ficam como fallback — sem erro visível.
     */
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
                _authState.value = AuthState.Authenticated
                viewModelScope.launch {
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
        val bearer = repository.tokenManager.getBearerToken()
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
                    val bearer = repository.tokenManager.getBearerToken()
                    if (bearer != null) _repos.value = repository.getRepos(bearer)
                    loadWorkflowOptions()   // carrega opções logo após criar o repo
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
    }

    // ── Acções existentes ─────────────────────────────────────────────────────

    fun selectRepo(repoFullName: String) {
        viewModelScope.launch {
            repository.saveRepo(repoFullName)
            loadWorkflowOptions()   // actualiza opções ao mudar de repo
            if (token.value == "123456789") fetchWorkflowRuns()
        }
    }

    fun logout() {
        pollingJob?.cancel()
        viewModelScope.launch {
            repository.clearSession()
            authRepository.logout()
            _user.value              = null
            _repos.value             = emptyList()
            _workflowRuns.value      = emptyList()
            _workflowOptions.value   = DynamicWorkflowOptions()
            _authState.value         = AuthState.Idle
            _uiState.value           = UiState.Idle
            _repoCreationState.value = RepoCreationState.Idle
            _smokeTestState.value    = SmokeTestState.Idle
        }
    }

    fun fetchWorkflowRuns(t: String? = token.value, r: String? = selectedRepo.value) {
        if (t == null || r == null) return
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
            _workflowRuns.value = repository.getWorkflowRuns(t, r)
        }
    }

    fun updateCharacter(config: CharacterConfig) {
        _currentCharacter.value = config
    }

    // ── Geração de personagem ─────────────────────────────────────────────────

    fun generate() {
        val t = token.value ?: return
        val r = selectedRepo.value ?: return
        viewModelScope.launch {
            _uiState.value = UiState.Loading
            if (t == "123456789") {
                delay(2000)
                _uiState.value = UiState.Success("Generation started (Demo Mode)")
                fetchWorkflowRuns(t, r)
                return@launch
            }
            val success = repository.generateCharacter(t, r, _currentCharacter.value)
            if (success) {
                _uiState.value = UiState.Success("Generation started successfully")
                fetchWorkflowRuns(t, r)
            } else {
                _uiState.value = UiState.Error("Failed to start generation")
            }
        }
    }

    // ── Smoke Test ────────────────────────────────────────────────────────────

    /**
     * Dispara o workflow de smoke test (axev-smoke-test.yml) no repo do utilizador.
     * Os 6 casos de teste correm em paralelo no GitHub Actions.
     */
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
                SmokeTestState.Failed("Failed to dispatch smoke test")
            }
            // Refresca os runs para mostrar o novo smoke test na dashboard
            fetchWorkflowRuns()
        }
    }

    fun resetSmokeTestState() {
        _smokeTestState.value = SmokeTestState.Idle
    }
}
