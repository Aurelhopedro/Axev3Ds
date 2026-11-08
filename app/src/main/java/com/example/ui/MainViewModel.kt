    fun createRepository(
        name: String,
        description: String = "Personal AXEV 3D STUDIO workspace for character generation and 3D assets.",
        private: Boolean = false
    ) {
        viewModelScope.launch {
            _repoCreationState.value = RepoCreationState.Creating

            // Demo mode — simulate success without calling the real API
            if (token.value == "123456789") {
                kotlinx.coroutines.delay(1500)
                val demoRepo = com.example.data.api.GitHubRepo(
                    name = name,
                    full_name = "demo/$name",
                    owner = com.example.data.api.GitHubUser("demo", "https://github.com/identicons/demo.png", "Demo User"),
                    default_branch = "main",
                    html_url = "https://github.com/demo/$name",
                    created_at = "2026-11-08T00:00:00Z"
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
                }
                is RepoCreationResult.AlreadyExists -> {
                    _repoCreationState.value = RepoCreationState.AlreadyExists(result.name)
                }
                is RepoCreationResult.Error -> {
                    _repoCreationState.value = RepoCreationState.Failed(result.message)
                }
            }
        }
    }
