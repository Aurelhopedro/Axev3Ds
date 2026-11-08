            composable(Screen.Dashboard.route) {
                DashboardScreen(viewModel, onNavigateToCreateRepo = {
                    navController.navigate("create_repo")
                })
            }
            composable(Screen.Creator.route) {
                CreatorScreen(viewModel)
            }
            composable(Screen.History.route) {
                DashboardScreen(viewModel, onNavigateToCreateRepo = {
                    navController.navigate("create_repo")
                })
            }