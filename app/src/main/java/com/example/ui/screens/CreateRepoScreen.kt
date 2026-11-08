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
import androidx.compose.ui.unit.sp
import com.example.data.api.GitHubRepo
import com.example.ui.MainViewModel
import com.example.ui.RepoCreateState
import com.example.ui.components.TacticalCard

@Composable
fun CreateRepoScreen(
    viewModel: MainViewModel,
    onSuccess: () -> Unit
) {
    val repoCreateState by viewModel.repoCreateState.collectAsState()
    val context = LocalContext.current

    var repoName by remember { mutableStateOf("axev-3d-workspace") }
    var repoDescription by remember { mutableStateOf("Personal AXEV 3D STUDIO workspace for character generation and 3D assets.") }
    var showConfirmDialog by remember { mutableStateOf(false) }

    // Navegar para o dashboard quando criado com sucesso
    LaunchedEffect(repoCreateState) {
        if (repoCreateState is RepoCreateState.Success) {
            // Aguardar o utilizador confirmar e clicar em "Continue"
        }
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(24.dp)
            .verticalScroll(rememberScrollState()),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {

        when (val state = repoCreateState) {

            is RepoCreateState.Idle, is RepoCreateState.Error -> {
                Spacer(modifier = Modifier.height(16.dp))

                Icon(
                    Icons.Default.FolderOpen,
                    contentDescription = null,
                    modifier = Modifier.size(56.dp),
                    tint = MaterialTheme.colorScheme.primary
                )
                Spacer(modifier = Modifier.height(12.dp))
                Text(
                    text = "CREATE MY 3D REPOSITORY",
                    style = MaterialTheme.typography.headlineSmall.copy(fontWeight = FontWeight.Black),
                    textAlign = TextAlign.Center
                )
                Text(
                    text = "Cria o teu espaço de trabalho no GitHub para guardar assets 3D e personagens.",
                    style = MaterialTheme.typography.bodyMedium,
                    color = Color.Gray,
                    textAlign = TextAlign.Center
                )

                Spacer(modifier = Modifier.height(32.dp))

                TacticalCard {
                    // Nome do repositório
                    Text("NOME DO REPOSITÓRIO", style = MaterialTheme.typography.labelSmall, color = Color.Gray)
                    Spacer(modifier = Modifier.height(4.dp))
                    OutlinedTextField(
                        value = repoName,
                        onValueChange = { repoName = it.trim().lowercase().replace(" ", "-") },
                        modifier = Modifier.fillMaxWidth(),
                        singleLine = true,
                        leadingIcon = { Icon(Icons.Default.FolderOpen, null) },
                        supportingText = {
                            Text(
                                "Apenas letras minúsculas, números e hífens",
                                style = MaterialTheme.typography.labelSmall
                            )
                        }
                    )

                    Spacer(modifier = Modifier.height(16.dp))

                    // Descrição
                    Text("DESCRIÇÃO (opcional)", style = MaterialTheme.typography.labelSmall, color = Color.Gray)
                    Spacer(modifier = Modifier.height(4.dp))
                    OutlinedTextField(
                        value = repoDescription,
                        onValueChange = { repoDescription = it },
                        modifier = Modifier.fillMaxWidth(),
                        minLines = 2,
                        maxLines = 3
                    )

                    Spacer(modifier = Modifier.height(16.dp))

                    // Visibilidade (apenas pública nesta versão)
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Icon(Icons.Default.Public, null, tint = Color(0xFF4CAF50), modifier = Modifier.size(20.dp))
                        Spacer(modifier = Modifier.width(8.dp))
                        Column {
                            Text("Repositório Público", fontWeight = FontWeight.Bold, style = MaterialTheme.typography.bodyMedium)
                            Text("Visível para toda a gente no GitHub", style = MaterialTheme.typography.labelSmall, color = Color.Gray)
                        }
                    }
                }

                Spacer(modifier = Modifier.height(8.dp))

                // Erros
                if (state is RepoCreateState.Error) {
                    TacticalCard {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(Icons.Default.Error, null, tint = MaterialTheme.colorScheme.error, modifier = Modifier.size(20.dp))
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(
                                text = state.message,
                                color = MaterialTheme.colorScheme.error,
                                style = MaterialTheme.typography.bodySmall
                            )
                        }
                    }
                    Spacer(modifier = Modifier.height(8.dp))
                }

                Spacer(modifier = Modifier.height(16.dp))

                Button(
                    onClick = { showConfirmDialog = true },
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(52.dp),
                    enabled = repoName.isNotBlank(),
                    shape = RoundedCornerShape(8.dp)
                ) {
                    Icon(Icons.Default.Add, null)
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("CRIAR REPOSITÓRIO", fontWeight = FontWeight.Bold)
                }
            }

            is RepoCreateState.Loading -> {
                Spacer(modifier = Modifier.height(80.dp))
                CircularProgressIndicator()
                Spacer(modifier = Modifier.height(16.dp))
                Text(
                    "A criar repositório no GitHub...",
                    style = MaterialTheme.typography.bodyMedium,
                    textAlign = TextAlign.Center
                )
                Text(
                    "Aguarda um momento",
                    style = MaterialTheme.typography.labelSmall,
                    color = Color.Gray
                )
            }

            is RepoCreateState.Success -> {
                RepoSuccessCard(
                    repo = state.repo,
                    onOpenGitHub = {
                        val intent = Intent(Intent.ACTION_VIEW, Uri.parse(state.repo.html_url))
                        context.startActivity(intent)
                    },
                    onRefresh = {
                        viewModel.fetchWorkflowRuns()
                    },
                    onContinue = {
                        viewModel.resetRepoCreateState()
                        onSuccess()
                    }
                )
            }
        }

        Spacer(modifier = Modifier.height(32.dp))
    }

    // Diálogo de confirmação
    if (showConfirmDialog) {
        AlertDialog(
            onDismissRequest = { showConfirmDialog = false },
            icon = { Icon(Icons.Default.FolderOpen, null) },
            title = { Text("Confirmar criação") },
            text = {
                Column {
                    Text("Vai ser criado o seguinte repositório na tua conta GitHub:")
                    Spacer(modifier = Modifier.height(8.dp))
                    Text("📁 $repoName", fontWeight = FontWeight.Bold)
                    Text("🌐 Público", color = Color(0xFF4CAF50))
                    if (repoDescription.isNotBlank()) {
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(repoDescription, style = MaterialTheme.typography.bodySmall, color = Color.Gray)
                    }
                }
            },
            confirmButton = {
                Button(onClick = {
                    showConfirmDialog = false
                    viewModel.createRepository(repoName, repoDescription)
                }) {
                    Text("Criar repositório")
                }
            },
            dismissButton = {
                TextButton(onClick = { showConfirmDialog = false }) {
                    Text("Cancelar")
                }
            }
        )
    }
}

@Composable
fun RepoSuccessCard(
    repo: GitHubRepo,
    onOpenGitHub: () -> Unit,
    onRefresh: () -> Unit,
    onContinue: () -> Unit
) {
    Spacer(modifier = Modifier.height(16.dp))
    Icon(
        Icons.Default.CheckCircle,
        contentDescription = null,
        modifier = Modifier.size(64.dp),
        tint = Color(0xFF4CAF50)
    )
    Spacer(modifier = Modifier.height(12.dp))
    Text(
        "Repositório criado!",
        style = MaterialTheme.typography.headlineSmall.copy(fontWeight = FontWeight.Black),
        color = Color(0xFF4CAF50)
    )
    Spacer(modifier = Modifier.height(24.dp))

    TacticalCard {
        Column(modifier = Modifier.fillMaxWidth()) {
            RepoInfoRow(Icons.Default.FolderOpen, "Nome", repo.name)
            RepoInfoRow(Icons.Default.Person, "Proprietário", repo.owner.login)
            RepoInfoRow(Icons.Default.Link, "URL", repo.html_url)
            RepoInfoRow(Icons.Default.Public, "Visibilidade", "Público")
            if (repo.created_at != null) {
                RepoInfoRow(Icons.Default.CalendarToday, "Criado em", repo.created_at.take(10))
            }
        }
    }

    Spacer(modifier = Modifier.height(16.dp))

    Button(
        onClick = onOpenGitHub,
        modifier = Modifier.fillMaxWidth().height(48.dp),
        colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF24292E))
    ) {
        Icon(Icons.Default.OpenInBrowser, null, modifier = Modifier.size(18.dp))
        Spacer(modifier = Modifier.width(8.dp))
        Text("OPEN ON GITHUB")
    }

    Spacer(modifier = Modifier.height(8.dp))

    OutlinedButton(
        onClick = onRefresh,
        modifier = Modifier.fillMaxWidth().height(48.dp)
    ) {
        Icon(Icons.Default.Refresh, null, modifier = Modifier.size(18.dp))
        Spacer(modifier = Modifier.width(8.dp))
        Text("REFRESH REPOSITORY")
    }

    Spacer(modifier = Modifier.height(8.dp))

    Button(
        onClick = onContinue,
        modifier = Modifier.fillMaxWidth().height(48.dp)
    ) {
        Text("CONTINUE TO DASHBOARD", fontWeight = FontWeight.Bold)
        Spacer(modifier = Modifier.width(8.dp))
        Icon(Icons.Default.ArrowForward, null, modifier = Modifier.size(18.dp))
    }
}

@Composable
fun RepoInfoRow(icon: androidx.compose.ui.graphics.vector.ImageVector, label: String, value: String) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 4.dp)
    ) {
        Icon(icon, null, modifier = Modifier.size(16.dp), tint = Color.Gray)
        Spacer(modifier = Modifier.width(8.dp))
        Text(label, style = MaterialTheme.typography.labelSmall, color = Color.Gray, modifier = Modifier.width(80.dp))
        Text(value, style = MaterialTheme.typography.bodySmall, fontWeight = FontWeight.Medium)
    }
}
