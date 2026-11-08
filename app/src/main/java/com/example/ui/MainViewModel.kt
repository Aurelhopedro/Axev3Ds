package com.example.ui

import android.app.Application
import android.content.Intent
import android.net.Uri
import android.util.Log
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.data.api.GitHubRepo
import com.example.data.api.GitHubUser
import com.example.data.api.WorkflowRun
import com.example.data.auth.GitHubAuthManager
import com.example.data.models.CharacterConfig
import com.example.data.repository.GitHubRepository
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch

sealed class UiState {
    object Idle : UiState()
    object Loading : UiState()
    data class Success(val message: String) : UiState()
    data class Error(val message: String) : UiState()
}

/** Estado do fluxo Device Flow */
sealed class AuthState {
    object Idle : AuthState()
    object Loading : AuthState()
    /** Mostrar user_code ao utilizador e abrir browser */
    data class WaitingForUser(
        val userCode: String,
        val verificationUri: String
    ) : AuthState()
    /** Polling em curso */
    object Polling : AuthState()
    object Authenticated : AuthState()
    data class Error(val message: String) : AuthState()
}

/** Estado da criação de repositório */
sealed class RepoCreateState {
    object Idle : RepoCreateState()
    object Loading : RepoCreateState()
    data class Success(val repo: GitHubRepo) : RepoCreateState()
    data class Error(val message: String) : RepoCreateState()
}

class MainViewModel(application: Application) : AndroidViewModel(application) {

    companion object {
        private const val TAG = "MainViewModel"
        // Client ID da GitHub App — configurado no .env como GITHUB_CLIENT_ID
        // Usar BuildConfig.GITHUB_CLIENT_ID após configurar secrets no .env
        // Por agora placeholder; substituir pelo Client ID real da GitHub App
        private const val GITHUB_CLIENT_ID = "GITHUB_CLIENT_ID_AQUI"
    }

    private val repository = GitHubRepository(application)
    private val authManager = GitHubAuthManager(application)

    // ── Flows de estado persistido ────────────────────────────────────────────

    val token = repository.tokenFlow.stateIn(viewModelScope, SharingStarted.Lazily, null)
    val selectedRepo = repository.selectedRepoFlow.stateIn(viewModelScope, SharingStarted.Lazily, null)
    val selectedRepoUrl = repository.selectedRepoUrlFlow.stateIn(viewModelScope, SharingStarted.Lazily, null)

    // ── Estados de UI ─────────────────────────────────────────────────────────

    private val _uiState = MutableStateFlow<UiState>(UiState.Idle)
    val uiState: StateFlow<UiState> = _uiState.asStateFlow()

    private val _authState = MutableStateFlow<AuthState>(AuthState.Idle)
    val authState: StateFlow<AuthState> = _authState.asStateFlow()

    private val _repoCreateState = MutableStateFlow<RepoCreateState>(RepoCreateState.Idle)
    val repoCreateState: StateFlow<RepoCreateState> = _repoCreateState.asStateFlow()

    // ── Dados do utilizador ───────────────────────────────────────────────────

    private val _user = MutableStateFlow<GitHubUser?>(null)
    val user: StateFlow<GitHubUser?> = _user.asStateFlow()

    private val _repos = MutableStateFlow<List<GitHubRepo>>(emptyList())
    val repos: StateFlow<List<GitHubRepo>> = _repos.asStateFlow()

    private val _workflowRuns = MutableStateFlow<List<WorkflowRun>>(emptyList())
    val workflowRuns: StateFlow<List<WorkflowRun>> = _workflowRuns.asStateFlow()

    // ── Character Creator ────────────────────────────────────────────────────

    private val _currentCharacter = MutableStateFlow(CharacterConfig())
    val currentCharacter: StateFlow<CharacterConfig> = _currentCharacter.asStateFlow()

    // ── Init ──────────────────────────────────────────────────────────────────

    init {
        viewModelScope.launch {
            token.collect { flag ->
                if (flag != null) {
                    // Recuperar token seguro e dados do utilizador
                    val secureToken = repository.getSecureToken()
                    if (secureToken != null) {
                        fetchUser(secureToken)
                        fetchRepos(secureToken)
                    }
                }
            }
        }
        viewModelScope.launch {
            combine(token, selectedRepo) { t, r -> t to r }.collect { (t, r) ->
                if (t != null && r != null) {
                    val secureToken = repository.getSecureToken()
                    if (secureToken != null) fetchWorkflowRuns(secureToken, r)
                }
            }
        }
    }

    // ── Autenticação Device Flow ──────────────────────────────────────────────

    /**
     * Inicia o Device Flow:
     * 1. Pede device_code ao GitHub
     * 2. Mostra o user_code ao utilizador
     * 3. Abre o browser em github.com/login/device
     * 4. Faz polling até receber o token
     */
    fun startGitHubLogin() {
        viewModelScope.launch {
            _authState.value = AuthState.Loading

            val deviceCodeData = authManager.requestDeviceCode(GITHUB_CLIENT_ID)
            if (deviceCodeData == null) {
                _authState.value = AuthState.Error("Não foi possível contactar o GitHub. Verifica a ligação à internet.")
                return@launch
            }

            // Mostrar o código ao utilizador
            _authState.value = AuthState.WaitingForUser(
                userCode = deviceCodeData.user_code,
                verificationUri = deviceCodeData.verification_uri
            )

            // Abrir o browser automaticamente
            try {
                val intent = Intent(Intent.ACTION_VIEW, Uri.parse(deviceCodeData.verification_uri))
                intent.flags = Intent.FLAG_ACTIVITY_NEW_TASK
                getApplication<Application>().startActivity(intent)
            } catch (e: Exception) {
                Log.w(TAG, "Não foi possível abrir o browser automaticamente", e)
            }

            // Iniciar polling
            _authState.value = AuthState.Polling

            val accessToken = authManager.pollForToken(
                clientId = GITHUB_CLIENT_ID,
                deviceCode = deviceCodeData.device_code,
                intervalSeconds = deviceCodeData.interval
            )

            if (accessToken.isNullOrBlank()) {
                _authState.value = AuthState.Error("Autorização não recebida. Tenta novamente.")
                return@launch
            }

            // Token recebido — validar e guardar
            val user = authManager.getUserFromToken(accessToken)
            if (user == null) {
                _authState.value = AuthState.Error("Token inválido. Tenta novamente.")
                return@launch
            }

            repository.saveToken(accessToken)
            _user.value = user
            fetchRepos(accessToken)
            _authState.value = AuthState.Authenticated
            _uiState.value = UiState.Success("Autenticado como @${user.login}")
        }
    }

    fun resetAuthState() {
        _authState.value = AuthState.Idle
    }

    // ── Criação de Repositório ────────────────────────────────────────────────

    fun createRepository(name: String, description: String) {
        val token = repository.getSecureToken() ?: run {
            _repoCreateState.value = RepoCreateState.Error("Sessão expirada. Faz login novamente.")
            return
        }
        viewModelScope.launch {
            _repoCreateState.value = RepoCreateState.Loading
            val result = repository.createRepository(token, name, description)
            result.fold(
                onSuccess = { repo ->
                    // Guardar referência do repositório criado
                    repository.saveRepo(repo.full_name, repo.html_url)
                    _repoCreateState.value = RepoCreateState.Success(repo)
                    _uiState.value = UiState.Success("Repositório '${repo.name}' criado com sucesso!")
                },
                onFailure = { error ->
                    _repoCreateState.value = RepoCreateState.Error(error.message ?: "Erro desconhecido")
                }
            )
        }
    }

    fun resetRepoCreateState() {
        _repoCreateState.value = RepoCreateState.Idle
    }

    // ── Logout ────────────────────────────────────────────────────────────────

    fun logout() {
        viewModelScope.launch {
            repository.clearSession()
            _user.value = null
            _repos.value = emptyList()
            _workflowRuns.value = emptyList()
            _authState.value = AuthState.Idle
            _repoCreateState.value = RepoCreateState.Idle
        }
    }

    // ── Selecção de repo existente ────────────────────────────────────────────

    fun selectRepo(repoFullName: String, repoUrl: String = "") {
        viewModelScope.launch {
            repository.saveRepo(repoFullName, repoUrl)
        }
    }

    // ── Dados ─────────────────────────────────────────────────────────────────

    private suspend fun fetchUser(t: String) {
        _user.value = repository.getUser(t)
    }

    private suspend fun fetchRepos(t: String) {
        _repos.value = repository.getRepos(t)
    }

    fun fetchWorkflowRuns(t: String? = repository.getSecureToken(), r: String? = selectedRepo.value) {
        if (t == null || r == null) return
        viewModelScope.launch {
            _workflowRuns.value = repository.getWorkflowRuns(t, r)
        }
    }

    fun updateCharacter(config: CharacterConfig) {
        _currentCharacter.value = config
    }

    fun generate() {
        val t = repository.getSecureToken() ?: return
        val r = selectedRepo.value ?: return
        viewModelScope.launch {
            _uiState.value = UiState.Loading
            val success = repository.generateCharacter(t, r, _currentCharacter.value)
            if (success) {
                _uiState.value = UiState.Success("Geração iniciada com sucesso")
                fetchWorkflowRuns(t, r)
            } else {
                _uiState.value = UiState.Error("Falha ao iniciar geração")
            }
        }
    }

    // ── Login legacy (PAT manual — mantido para compatibilidade) ─────────────
    @Deprecated("Usar startGitHubLogin() com Device Flow")
    fun login(newToken: String) {
        viewModelScope.launch {
            _uiState.value = UiState.Loading
            val user = repository.getUser(newToken.trim())
            if (user != null) {
                repository.saveToken(newToken.trim())
                _user.value = user
                _uiState.value = UiState.Success("Autenticado como ${user.login}")
            } else {
                _uiState.value = UiState.Error("Token inválido")
            }
        }
    }
}
