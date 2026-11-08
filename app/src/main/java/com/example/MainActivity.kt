package com.example

import android.content.Intent
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.viewModels
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Scaffold
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import com.example.ui.AuthState
import com.example.ui.MainViewModel
import com.example.ui.screens.CreateRepoScreen
import com.example.ui.screens.DashboardScreen
import com.example.ui.screens.LoginScreen
import com.example.ui.screens.RepoSelectionScreen
import com.example.ui.theme.AXEVCRIME3DSTUDIOTheme

sealed class Screen(val route: String) {
    object Login : Screen("login")
    object RepoSelection : Screen("repo_selection")
    object CreateRepo : Screen("create_repo")
    object Dashboard : Screen("dashboard")
}

class MainActivity : ComponentActivity() {

    private val viewModel: MainViewModel by viewModels()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        // Tratar o deep link axev3d://callback?token=xxx se a app abriu via esse link
        handleDeepLinkIntent(intent)

        setContent {
            AXEVCRIME3DSTUDIOTheme {
                val navController = rememberNavController()
                val authState by viewModel.authState.collectAsState()
                val selectedRepo by viewModel.selectedRepo.collectAsState()

                // Navegação reativa
                LaunchedEffect(authState, selectedRepo) {
                    when {
                        authState !is AuthState.Authenticated -> {
                            navController.navigate(Screen.Login.route) {
                                popUpTo(0) { inclusive = true }
                            }
                        }
                        selectedRepo == null -> {
                            navController.navigate(Screen.RepoSelection.route) {
                                popUpTo(0) { inclusive = true }
                            }
                        }
                        else -> {
                            navController.navigate(Screen.Dashboard.route) {
                                popUpTo(0) { inclusive = true }
                            }
                        }
                    }
                }

                val startDestination = when {
                    authState !is AuthState.Authenticated -> Screen.Login.route
                    selectedRepo == null -> Screen.RepoSelection.route
                    else -> Screen.Dashboard.route
                }

                Scaffold(modifier = Modifier.fillMaxSize()) { innerPadding ->
                    NavHost(
                        navController = navController,
                        startDestination = startDestination,
                        modifier = Modifier.padding(innerPadding)
                    ) {
                        composable(Screen.Login.route) {
                            LoginScreen(viewModel = viewModel)
                        }
                        composable(Screen.RepoSelection.route) {
                            RepoSelectionScreen(
                                viewModel = viewModel,
                                onRepoSelected = {
                                    navController.navigate(Screen.Dashboard.route) {
                                        popUpTo(Screen.RepoSelection.route) { inclusive = true }
                                    }
                                },
                                onCreateNew = {
                                    navController.navigate(Screen.CreateRepo.route)
                                }
                            )
                        }
                        composable(Screen.CreateRepo.route) {
                            CreateRepoScreen(
                                viewModel = viewModel,
                                onSuccess = {
                                    navController.navigate(Screen.Dashboard.route) {
                                        popUpTo(Screen.RepoSelection.route) { inclusive = true }
                                    }
                                }
                            )
                        }
                        composable(Screen.Dashboard.route) {
                            DashboardScreen(
                                viewModel = viewModel,
                                onNavigateToCreateRepo = {
                                    navController.navigate(Screen.CreateRepo.route)
                                }
                            )
                        }
                    }
                }
            }
        }
    }

    /**
     * Chamado quando a app é retomada via deep link (ex: browser redireciona axev3d://callback?token=xxx).
     * launchMode="singleTop" garante que esta instância é reutilizada em vez de criar uma nova.
     */
    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        handleDeepLinkIntent(intent)
    }

    /**
     * Extrai o token do deep link axev3d://callback?token=xxx e passa-o ao ViewModel.
     */
    private fun handleDeepLinkIntent(intent: Intent?) {
        val uri = intent?.data ?: return
        if (uri.scheme == "axev3d" && uri.host == "callback") {
            val token = uri.getQueryParameter("token")
            if (!token.isNullOrBlank()) {
                viewModel.handleOAuthCallback(token)
            }
        }
    }
}
