package com.example.ui.screens

import android.content.Intent
import android.net.Uri
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.example.data.api.GitHubRepo
import com.example.ui.MainViewModel
import com.example.ui.RepoCreationState
import com.example.ui.WorkspaceSetupState
import com.example.ui.components.SectionHeader
import com.example.ui.components.TacticalButton
import com.example.ui.components.TacticalCard
import com.example.ui.theme.TacticalOrange

@Composable
fun CreateRepositoryScreen(
    viewModel: MainViewModel,
    onRepositoryCreated: () -> Unit
) {
    val creationState by viewModel.repoCreationState.collectAsState()
    val workspaceState by viewModel.workspaceSetupState.collectAsState()
    val user by viewModel.user.collectAsState()
    val context = LocalContext.current

    var repoName by remember { mutableStateOf("axev-3d-workspace") }
    var description by remember { mutableStateOf("Personal AXEV 3D STUDIO workspace for character generation and 3D assets.") }
    var showConfirmDialog by remember { mutableStateOf(false) }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp)
            .verticalScroll(rememberScrollState())
    ) {
        SectionHeader(title = "CREATE MY 3D REPOSITORY")

        Spacer(modifier = Modifier.height(8.dp))

        Text(
            text = "Create a public repository in your GitHub account to store your AXEV 3D project.",
            style = MaterialTheme.typography.bodySmall,
            color = Color.Gray
        )

        Spacer(modifier = Modifier.height(24.dp))

        when (val state = creationState) {

            is RepoCreationState.Idle, is RepoCreationState.Failed, is RepoCreationState.AlreadyExists -> {
                TacticalCard {
                    // Repo name
                    Text("Repository Name", style = MaterialTheme.typography.labelMedium, fontWeight = FontWeight.Bold)
                    Spacer(modifier = Modifier.height(4.dp))
                    OutlinedTextField(
                        value = repoName,
                        onValueChange = { repoName = it.trim().replace(" ", "-") },
                        modifier = Modifier.fillMaxWidth(),
                        singleLine = true,
                        leadingIcon = { Icon(Icons.Default.FolderOpen, contentDescription = null) },
                        supportingText = {
                            val ownerLogin = user?.login ?: "you"
                            Text("github.com/$ownerLogin/$repoName", color = TacticalOrange)
                        }
                    )

                    Spacer(modifier = Modifier.height(16.dp))

                    // Description
                    Text("Description (optional)", style = MaterialTheme.typography.labelMedium, fontWeight = FontWeight.Bold)
                    Spacer(modifier = Modifier.height(4.dp))
                    OutlinedTextField(
                        value = description,
                        onValueChange = { description = it },
                        modifier = Modifier.fillMaxWidth(),
                        minLines = 2,
                        maxLines = 4
                    )

                    Spacer(modifier = Modifier.height(16.dp))

                    // Visibility
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(Icons.Default.Public, contentDescription = null, tint = Color.Green, modifier = Modifier.size(18.dp))
                        Spacer(modifier = Modifier.width(8.dp))
                        Text("Public repository", style = MaterialTheme.typography.bodySmall)
                        Spacer(modifier = Modifier.weight(1f))
                        Text("(required)", style = MaterialTheme.typography.labelSmall, color = Color.Gray)
                    }
                }

                // Error messages
                if (state is RepoCreationState.Failed) {
                    Spacer(modifier = Modifier.height(12.dp))
                    TacticalCard {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(Icons.Default.Error, contentDescription = null, tint = MaterialTheme.colorScheme.error)
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(state.message, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.error)
                        }
                    }
                }
                if (state is RepoCreationState.AlreadyExists) {
                    Spacer(modifier = Modifier.height(12.dp))
                    TacticalCard {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(Icons.Default.Warning, contentDescription = null, tint = Color(0xFFFF9800))
                            Spacer(modifier = Modifier.width(8.dp))
                            Column {
                                Text("Repository already exists", fontWeight = FontWeight.Bold, color = Color(0xFFFF9800))
                                Text(
                                    "A repository named \"${state.name}\" already exists in your account. Try a different name.",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = Color.Gray
                                )
                            }
                        }
                    }
                }

                Spacer(modifier = Modifier.height(24.dp))

                TacticalButton(
                    text = "Create Repository",
                    onClick = { showConfirmDialog = true },
                    enabled = repoName.isNotBlank()
                )
            }

            is RepoCreationState.Creating -> {
                Box(modifier = Modifier.fillMaxWidth().padding(32.dp), contentAlignment = Alignment.Center) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        CircularProgressIndicator(color = TacticalOrange)
                        Spacer(modifier = Modifier.height(16.dp))
                        Text("Creating repository on GitHub...", style = MaterialTheme.typography.bodyMedium)
                        Text("Please wait", style = MaterialTheme.typography.bodySmall, color = Color.Gray)
                    }
                }
            }

            is RepoCreationState.Success -> {
                RepoCreatedCard(
                    repo = state.repo,
                    workspaceState = workspaceState,
                    onOpenGitHub = {
                        val intent = Intent(Intent.ACTION_VIEW, Uri.parse(state.repo.html_url))
                        context.startActivity(intent)
                    },
                    onRefresh = { viewModel.resetRepoCreationState() },
                    onSetupWorkspace = {
                        viewModel.setupWorkspace(state.repo.owner.login, state.repo.name)
                    },
                    onContinue = {
                        viewModel.selectRepo(state.repo.full_name)
                        onRepositoryCreated()
                    }
                )
            }
        }
    }

    // Confirmation dialog
    if (showConfirmDialog) {
        AlertDialog(
            onDismissRequest = { showConfirmDialog = false },
            icon = { Icon(Icons.Default.FolderSpecial, contentDescription = null, tint = TacticalOrange) },
            title = { Text("Create Repository?") },
            text = {
                Column {
                    Text("This will create a public repository in your GitHub account:")
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(
                        text = "${user?.login ?: "you"}/$repoName",
                        fontWeight = FontWeight.Bold,
                        color = TacticalOrange
                    )
                    Spacer(modifier = Modifier.height(4.dp))
                    Text("Visibility: Public", style = MaterialTheme.typography.bodySmall, color = Color.Gray)
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        showConfirmDialog = false
                        viewModel.createRepository(repoName, description)
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = TacticalOrange)
                ) {
                    Text("Create")
                }
            },
            dismissButton = {
                TextButton(onClick = { showConfirmDialog = false }) {
                    Text("Cancel")
                }
            }
        )
    }
}

@Composable
private fun RepoCreatedCard(
    repo: GitHubRepo,
    workspaceState: WorkspaceSetupState,
    onOpenGitHub: () -> Unit,
    onRefresh: () -> Unit,
    onSetupWorkspace: () -> Unit,
    onContinue: () -> Unit
) {
    Column {
        // Success header
        TacticalCard {
            Column(horizontalAlignment = Alignment.CenterHorizontally, modifier = Modifier.fillMaxWidth()) {
                Icon(
                    Icons.Default.CheckCircle,
                    contentDescription = null,
                    tint = Color.Green,
                    modifier = Modifier.size(48.dp)
                )
                Spacer(modifier = Modifier.height(8.dp))
                Text("Repository Created!", fontWeight = FontWeight.Bold, style = MaterialTheme.typography.titleLarge)
                Spacer(modifier = Modifier.height(16.dp))

                RepoInfoRow(label = "Name", value = repo.name)
                RepoInfoRow(label = "Owner", value = repo.owner.login)
                RepoInfoRow(label = "Visibility", value = if (repo.private) "Private" else "Public")
                if (repo.html_url.isNotBlank()) {
                    RepoInfoRow(label = "URL", value = repo.html_url, valueColor = TacticalOrange)
                }
                if (repo.created_at != null) {
                    RepoInfoRow(label = "Created", value = repo.created_at.take(10))
                }
            }
        }

        Spacer(modifier = Modifier.height(16.dp))

        // Open on GitHub
        Button(
            onClick = onOpenGitHub,
            modifier = Modifier.fillMaxWidth(),
            colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF24292E))
        ) {
            Icon(Icons.Default.OpenInBrowser, contentDescription = null)
            Spacer(modifier = Modifier.width(8.dp))
            Text("Open on GitHub")
        }

        Spacer(modifier = Modifier.height(8.dp))

        // Refresh
        OutlinedButton(
            onClick = onRefresh,
            modifier = Modifier.fillMaxWidth()
        ) {
            Icon(Icons.Default.Refresh, contentDescription = null)
            Spacer(modifier = Modifier.width(8.dp))
            Text("Refresh Repository")
        }

        Spacer(modifier = Modifier.height(8.dp))

        // ── Botão "Criar Ambiente de Trabalho" ────────────────────────────────
        when (workspaceState) {
            is WorkspaceSetupState.Idle -> {
                Button(
                    onClick = onSetupWorkspace,
                    modifier = Modifier.fillMaxWidth(),
                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF1A6B3A))
                ) {
                    Icon(Icons.Default.Build, contentDescription = null)
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("Criar Ambiente de Trabalho")
                }
            }

            is WorkspaceSetupState.Setting -> {
                Button(
                    onClick = {},
                    enabled = false,
                    modifier = Modifier.fillMaxWidth(),
                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF1A6B3A))
                ) {
                    CircularProgressIndicator(
                        modifier = Modifier.size(18.dp),
                        color = Color.White,
                        strokeWidth = 2.dp
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("A configurar ambiente...")
                }
            }

            is WorkspaceSetupState.Done -> {
                TacticalCard {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(Icons.Default.CheckCircle, contentDescription = null, tint = Color.Green)
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            "Ambiente configurado com sucesso!",
                            style = MaterialTheme.typography.bodySmall,
                            color = Color.Green,
                            fontWeight = FontWeight.Bold
                        )
                    }
                }
            }

            is WorkspaceSetupState.Failed -> {
                TacticalCard {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(Icons.Default.Error, contentDescription = null, tint = MaterialTheme.colorScheme.error)
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            workspaceState.message,
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.error
                        )
                    }
                }
                Spacer(modifier = Modifier.height(8.dp))
                OutlinedButton(
                    onClick = onSetupWorkspace,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Icon(Icons.Default.Refresh, contentDescription = null)
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("Tentar novamente")
                }
            }
        }

        Spacer(modifier = Modifier.height(8.dp))

        // Continue to Dashboard
        Button(
            onClick = onContinue,
            modifier = Modifier.fillMaxWidth(),
            colors = ButtonDefaults.buttonColors(containerColor = TacticalOrange)
        ) {
            Icon(Icons.Default.ArrowForward, contentDescription = null)
            Spacer(modifier = Modifier.width(8.dp))
            Text("Continue to Dashboard")
        }
    }
}

@Composable
private fun RepoInfoRow(label: String, value: String, valueColor: Color = Color.White) {
    Row(modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp)) {
        Text(
            text = "$label:",
            style = MaterialTheme.typography.bodySmall,
            color = Color.Gray,
            modifier = Modifier.width(70.dp)
        )
        Text(
            text = value,
            style = MaterialTheme.typography.bodySmall,
            color = valueColor,
            fontWeight = FontWeight.Medium,
            modifier = Modifier.weight(1f)
        )
    }
}
