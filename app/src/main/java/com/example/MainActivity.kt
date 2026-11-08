            composable("repo_selection") {
                RepoSelectionScreen(
                    viewModel = viewModel,
                    onRepoSelected = {
                        navController.navigate(Screen.Dashboard.route) {
                            popUpTo("repo_selection") { inclusive = true }
                        }
                    },
                    onCreateNew = {
                        navController.navigate("create_repo")
                    }
                )
            }