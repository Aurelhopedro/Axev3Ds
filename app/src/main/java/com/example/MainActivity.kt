package com.example

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavDestination.Companion.hierarchy
import androidx.navigation.NavGraph.Companion.findStartDestination
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import com.example.ui.AuthState
import com.example.ui.MainViewModel
import com.example.ui.screens.*
import com.example.ui.components.*
import com.example.ui.theme.AXEVCRIME3DSTUDIOTheme
import com.example.ui.theme.TacticalOrange
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.fillMaxWidth

sealed class Screen(val route: String, val label: String, val icon: ImageVector) {
    object Dashboard : Screen("dashboard", "Home", Icons.Default.Dashboard)
    object Creator : Screen("creator", "Create", Icons.Default.AddBox)
    object History : Screen("history", "Gerações", Icons.Default.History)
    object Library : Screen("library", "Library", Icons.Default.PhotoLibrary)
    object Setup : Screen("setup", "Setup", Icons.Default.Build)
    object Settings : Screen("settings", "Studio", Icons.Default.Settings)
    object Login : Screen("login", "Login", Icons.Default.Login)
}

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            AXEVCRIME3DSTUDIOTheme {
                MainApp()
            }
        }
    }
}

@Composable
fun MainApp(viewModel: MainViewModel = viewModel()) {
    val navController = rememberNavController()
    val authState by viewModel.authState.collectAsState()
    val selectedRepo by viewModel.selectedRepo.collectAsState()

    // Determine start destination based on auth and repo state
    val startDestination = when {
        authState !is AuthState.Authenticated -> Screen.Login.route
        selectedRepo == null -> "repo_selection"
        else -> Screen.Dashboard.route
    }

    Scaffold(
        bottomBar = {
            if (authState is AuthState.Authenticated && selectedRepo != null) {
                BottomNavigationBar(navController)
            }
        }
    ) { innerPadding ->
        NavHost(
            navController = navController,
            startDestination = startDestination,
            modifier = Modifier.padding(innerPadding)
        ) {
            composable(Screen.Login.route) {
                LoginScreen(viewModel)
            }
            composable("repo_selection") {
                RepoSelectionScreen(viewModel) {
                    navController.navigate(Screen.Dashboard.route) {
                        popUpTo("repo_selection") { inclusive = true }
                    }
                }
            }
            composable("create_repo") {
                CreateRepositoryScreen(viewModel) {
                    // After repo created, go to Dashboard
                    navController.navigate(Screen.Dashboard.route) {
                        popUpTo("create_repo") { inclusive = true }
                    }
                }
            }
            composable(Screen.Dashboard.route) {
                DashboardScreen(viewModel)
            }
            composable(Screen.Creator.route) {
                CreatorScreen(viewModel)
            }
            composable(Screen.History.route) {
                DashboardScreen(viewModel)
            }
            composable(Screen.Library.route) {
                LibraryScreen()
            }
            composable(Screen.Setup.route) {
                SetupScreen()
            }
            composable(Screen.Settings.route) {
                SettingsScreen(viewModel, onNavigateToSetup = { navController.navigate(Screen.Setup.route) })
            }
        }
    }

    // React to auth state changes
    LaunchedEffect(authState) {
        when (authState) {
            is AuthState.Authenticated -> {
                val currentRoute = navController.currentDestination?.route
                if (currentRoute == Screen.Login.route || currentRoute == null) {
                    if (selectedRepo == null) {
                        navController.navigate("repo_selection") {
                            popUpTo(Screen.Login.route) { inclusive = true }
                        }
                    } else {
                        navController.navigate(Screen.Dashboard.route) {
                            popUpTo(Screen.Login.route) { inclusive = true }
                        }
                    }
                }
            }
            is AuthState.Idle -> {
                // Logged out — go to login
                val currentRoute = navController.currentDestination?.route
                if (currentRoute != Screen.Login.route) {
                    navController.navigate(Screen.Login.route) {
                        popUpTo(0) { inclusive = true }
                    }
                }
            }
            else -> { /* waiting states — no navigation */ }
        }
    }
}

@Composable
fun BottomNavigationBar(navController: androidx.navigation.NavHostController) {
    val items = listOf(
        Screen.Dashboard,
        Screen.Creator,
        Screen.History,
        Screen.Library,
        Screen.Setup,
        Screen.Settings
    )
    NavigationBar(
        containerColor = MaterialTheme.colorScheme.surface,
        contentColor = Color.White
    ) {
        val navBackStackEntry by navController.currentBackStackEntryAsState()
        val currentDestination = navBackStackEntry?.destination
        items.forEach { screen ->
            NavigationBarItem(
                icon = { Icon(screen.icon, contentDescription = null) },
                label = { Text(screen.label) },
                selected = currentDestination?.hierarchy?.any { it.route == screen.route } == true,
                onClick = {
                    navController.navigate(screen.route) {
                        popUpTo(navController.graph.findStartDestination().id) {
                            saveState = true
                        }
                        launchSingleTop = true
                        restoreState = true
                    }
                },
                colors = NavigationBarItemDefaults.colors(
                    selectedIconColor = TacticalOrange,
                    selectedTextColor = TacticalOrange,
                    indicatorColor = MaterialTheme.colorScheme.surfaceVariant
                )
            )
        }
    }
}

@Composable
fun LibraryScreen() {
    Text("Library Screen - Coming Soon")
}

@Composable
fun SettingsScreen(viewModel: MainViewModel, onNavigateToSetup: () -> Unit) {
    val user by viewModel.user.collectAsState()
    val authState by viewModel.authState.collectAsState()

    Column(modifier = Modifier.padding(16.dp)) {
        SectionHeader(title = "STUDIO CONFIGURATION")

        // User info card
        if (authState is AuthState.Authenticated && user != null) {
            TacticalCard {
                Text("CONNECTED ACCOUNT", style = MaterialTheme.typography.labelSmall, color = Color.Gray)
                Spacer(modifier = Modifier.height(8.dp))
                Text(
                    text = "@${user!!.login}",
                    style = MaterialTheme.typography.titleMedium,
                    color = TacticalOrange
                )
                user!!.name?.let {
                    Text(text = it, style = MaterialTheme.typography.bodySmall, color = Color.LightGray)
                }
                Spacer(modifier = Modifier.height(4.dp))
                Text("● Connected via GitHub", style = MaterialTheme.typography.labelSmall, color = Color.Green)
            }
            Spacer(modifier = Modifier.height(16.dp))
        }

        TacticalCard {
            Text("WORKFLOW STATUS", style = MaterialTheme.typography.labelSmall)
            Spacer(modifier = Modifier.height(8.dp))
            Button(
                onClick = onNavigateToSetup,
                modifier = Modifier.fillMaxWidth(),
                colors = ButtonDefaults.buttonColors(containerColor = Color.DarkGray)
            ) {
                Icon(Icons.Default.Build, contentDescription = null)
                Spacer(modifier = Modifier.width(8.dp))
                Text("MANUAL WORKFLOW SETUP")
            }
        }

        Spacer(modifier = Modifier.height(24.dp))

        Button(
            onClick = { viewModel.logout() },
            modifier = Modifier.fillMaxWidth(),
            colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.error)
        ) {
            Icon(Icons.Default.Logout, contentDescription = null)
            Spacer(modifier = Modifier.width(8.dp))
            Text("SIGN OUT")
        }
    }
}
