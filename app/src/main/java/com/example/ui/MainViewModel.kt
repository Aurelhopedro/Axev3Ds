package com.example.ui

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.data.api.GitHubRepo
import com.example.data.api.GitHubUser
import com.example.data.api.WorkflowRun
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

class MainViewModel(application: Application) : AndroidViewModel(application) {
    private val repository = GitHubRepository(application)

    val token = repository.tokenFlow.stateIn(viewModelScope, SharingStarted.Lazily, null)
    val selectedRepo = repository.selectedRepoFlow.stateIn(viewModelScope, SharingStarted.Lazily, null)

    private val _uiState = MutableStateFlow<UiState>(UiState.Idle)
    val uiState: StateFlow<UiState> = _uiState.asStateFlow()

    private val _user = MutableStateFlow<GitHubUser?>(null)
    val user: StateFlow<GitHubUser?> = _user.asStateFlow()

    private val _repos = MutableStateFlow<List<GitHubRepo>>(emptyList())
    val repos: StateFlow<List<GitHubRepo>> = _repos.asStateFlow()

    private val _workflowRuns = MutableStateFlow<List<WorkflowRun>>(emptyList())
    val workflowRuns: StateFlow<List<WorkflowRun>> = _workflowRuns.asStateFlow()

    // Character Creator State
    private val _currentCharacter = MutableStateFlow(CharacterConfig())
    val currentCharacter: StateFlow<CharacterConfig> = _currentCharacter.asStateFlow()

    init {
        viewModelScope.launch {
            token.collect { t ->
                if (t != null) {
                    fetchUser(t)
                    fetchRepos(t)
                }
            }
        }
        viewModelScope.launch {
            combine(token, selectedRepo) { t, r -> t to r }.collect { (t, r) ->
                if (t != null && r != null) {
                    fetchWorkflowRuns(t, r)
                }
            }
        }
    }

    fun login(newToken: String) {
        val sanitizedToken = newToken.trim()
        viewModelScope.launch {
            _uiState.value = UiState.Loading
            if (sanitizedToken == "123456789") {
                // Demo Mode for testing
                repository.saveToken(sanitizedToken)
                _user.value = GitHubUser("axev_dev", "https://github.com/identicons/axev.png", "AXEV Developer")
                _repos.value = listOf(GitHubRepo("axev-crime-assets", "axev-studio/axev-crime-assets", _user.value!!, "main"))
                _uiState.value = UiState.Success("Logged in as Demo User")
                return@launch
            }
            
            val user = repository.getUser("Bearer $sanitizedToken")
            if (user != null) {
                repository.saveToken(sanitizedToken)
                _uiState.value = UiState.Success("Logged in as ${user.login}")
            } else {
                _uiState.value = UiState.Error("Invalid Token")
            }
        }
    }

    fun selectRepo(repoFullName: String) {
        viewModelScope.launch {
            repository.saveRepo(repoFullName)
            if (token.value == "123456789") {
                fetchWorkflowRuns()
            }
        }
    }

    fun logout() {
        viewModelScope.launch {
            repository.clearSession()
            _user.value = null
            _repos.value = emptyList()
            _workflowRuns.value = emptyList()
        }
    }

    private suspend fun fetchUser(t: String) {
        if (t == "123456789") {
            _user.value = GitHubUser("axev_dev", "https://github.com/identicons/axev.png", "AXEV Developer")
            return
        }
        _user.value = repository.getUser(t)
    }

    private suspend fun fetchRepos(t: String) {
        if (t == "123456789") {
            _repos.value = listOf(GitHubRepo("axev-crime-assets", "axev-studio/axev-crime-assets", _user.value!!, "main"))
            return
        }
        _repos.value = repository.getRepos(t)
    }

    fun fetchWorkflowRuns(t: String? = token.value, r: String? = selectedRepo.value) {
        if (t == null || r == null) return
        viewModelScope.launch {
            if (t == "123456789") {
                _workflowRuns.value = listOf(
                    WorkflowRun(1001, "ARA 3D GEN - SOLDIER-01", "completed", "success", "2026-11-07T10:00:00Z", "2026-11-07T10:05:00Z", ""),
                    WorkflowRun(1002, "ARA 3D GEN - DETECTIVE-X", "in_progress", null, "2026-11-07T11:00:00Z", "2026-11-07T11:01:00Z", "")
                )
                return@launch
            }
            _workflowRuns.value = repository.getWorkflowRuns(t, r)
        }
    }

    fun updateCharacter(config: CharacterConfig) {
        _currentCharacter.value = config
    }

    fun generate() {
        val t = token.value ?: return
        val r = selectedRepo.value ?: return
        viewModelScope.launch {
            _uiState.value = UiState.Loading
            if (t == "123456789") {
                kotlinx.coroutines.delay(2000)
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
}
