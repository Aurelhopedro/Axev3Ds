package com.example.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AccountCircle
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.BugReport
import androidx.compose.material.icons.filled.FolderSpecial
import androidx.compose.material.icons.filled.Logout
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import coil.compose.AsyncImage
import com.example.data.api.WorkflowRun
import com.example.ui.MainViewModel
import com.example.ui.SmokeTestState
import com.example.ui.components.SectionHeader
import com.example.ui.components.StatusBadge
import com.example.ui.components.TacticalCard
import com.example.ui.theme.TacticalOrange

@Composable
fun DashboardScreen(
    viewModel: MainViewModel,
    onNavigateToCreateRepo: (() -> Unit)? = null
) {
    val user           by viewModel.user.collectAsState()
    val repo           by viewModel.selectedRepo.collectAsState()
    val runs           by viewModel.workflowRuns.collectAsState()
    val smokeState     by viewModel.smokeTestState.collectAsState()
    val workflowOpts   by viewModel.workflowOptions.collectAsState()
    var showLogoutDialog by remember { mutableStateOf(false) }

    if (showLogoutDialog) {
        AlertDialog(
            onDismissRequest = { showLogoutDialog = false },
            icon    = { Icon(Icons.Default.Logout, null) },
            title   = { Text("Terminar sessão?") },
            text    = { Text("Vais ser desligado da tua conta GitHub.") },
            confirmButton = {
                Button(
                    onClick = { showLogoutDialog = false; viewModel.logout() },
                    colors  = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.error)
                ) { Text("Sair") }
            },
            dismissButton = {
                TextButton(onClick = { showLogoutDialog = false }) { Text("Cancelar") }
            }
        )
    }

    LazyColumn(
        modifier        = Modifier.fillMaxSize().padding(horizontal = 16.dp),
        contentPadding  = PaddingValues(vertical = 16.dp)
    ) {

        // ── Cabeçalho do utilizador ───────────────────────────────────────
        item {
            UserHeader(
                user      = user,
                repo      = repo,
                isLive    = workflowOpts.isLive,
                onRefresh = {
                    viewModel.fetchWorkflowRuns()
                    viewModel.loadWorkflowOptions()
                },
                onLogout  = { showLogoutDialog = true }
            )
            Spacer(modifier = Modifier.height(16.dp))
        }

        // ── Criar repo (se ainda não existe) ─────────────────────────────
        item {
            if (onNavigateToCreateRepo != null || repo == null) {
                CreateRepoPromptCard(onClick = onNavigateToCreateRepo ?: {})
                Spacer(modifier = Modifier.height(16.dp))
            }
        }

        // ── Smoke Test ────────────────────────────────────────────────────
        item {
            SmokeTestCard(
                state   = smokeState,
                hasRepo = repo != null,
                onRun   = { viewModel.runSmokeTest() },
                onReset = { viewModel.resetSmokeTestState() }
            )
            Spacer(modifier = Modifier.height(16.dp))
        }

        // ── Estatísticas rápidas ──────────────────────────────────────────
        item {
            QuickStats(runs)
            Spacer(modifier = Modifier.height(16.dp))
            SectionHeader(title = "Recent Activity")
        }

        // ── Lista de runs ─────────────────────────────────────────────────
        if (runs.isEmpty()) {
            item {
                Text(
                    text     = "No recent generations found.",
                    modifier = Modifier.padding(vertical = 32.dp),
                    style    = MaterialTheme.typography.bodyMedium,
                    color    = Color.Gray
                )
            }
        } else {
            items(runs) { run ->
                GenerationItem(run)
                Spacer(modifier = Modifier.height(8.dp))
            }
        }
    }
}

// ─── Card de Smoke Test ───────────────────────────────────────────────────────

@Composable
fun SmokeTestCard(
    state: SmokeTestState,
    hasRepo: Boolean,
    onRun: () -> Unit,
    onReset: () -> Unit
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape    = RoundedCornerShape(10.dp),
        colors   = CardDefaults.cardColors(containerColor = Color(0xFF1A1A2A)),
        border   = androidx.compose.foundation.BorderStroke(
            1.dp,
            when (state) {
                is SmokeTestState.Started -> Color(0xFF4CAF50)
                is SmokeTestState.Failed  -> Color(0xFFE53935)
                is SmokeTestState.Running -> TacticalOrange
                else                      -> Color(0xFF37474F)
            }
        )
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(
                    imageVector = Icons.Default.BugReport,
                    contentDescription = null,
                    tint     = TacticalOrange,
                    modifier = Modifier.size(24.dp)
                )
                Spacer(modifier = Modifier.width(8.dp))
                Column(modifier = Modifier.weight(1f)) {
                    Text("Smoke Test", fontWeight = FontWeight.Bold, color = Color.White)
                    Text(
                        "6 test cases — covers all critical combinations",
                        style = MaterialTheme.typography.labelSmall,
                        color = Color.Gray
                    )
                }
            }

            Spacer(modifier = Modifier.height(12.dp))

            when (state) {
                is SmokeTestState.Running -> {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        CircularProgressIndicator(
                            modifier    = Modifier.size(16.dp),
                            strokeWidth = 2.dp,
                            color       = TacticalOrange
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text("Dispatching to GitHub Actions…", color = Color.Gray)
                    }
                }
                is SmokeTestState.Started -> {
                    Text("✅ ${state.message}", color = Color(0xFF4CAF50), fontWeight = FontWeight.Medium)
                    Spacer(modifier = Modifier.height(8.dp))
                    OutlinedButton(onClick = onReset, modifier = Modifier.fillMaxWidth()) {
                        Text("OK")
                    }
                }
                is SmokeTestState.Failed -> {
                    Text("❌ ${state.message}", color = Color(0xFFE53935))
                    Spacer(modifier = Modifier.height(8.dp))
                    Button(
                        onClick  = onRun,
                        modifier = Modifier.fillMaxWidth(),
                        colors   = ButtonDefaults.buttonColors(containerColor = Color(0xFFE53935))
                    ) { Text("Retry Smoke Test") }
                }
                else -> {
                    Button(
                        onClick  = onRun,
                        enabled  = hasRepo,
                        modifier = Modifier.fillMaxWidth(),
                        colors   = ButtonDefaults.buttonColors(containerColor = TacticalOrange)
                    ) {
                        Icon(Icons.Default.BugReport, null, modifier = Modifier.size(16.dp))
                        Spacer(modifier = Modifier.width(8.dp))
                        Text("Run Smoke Test")
                    }
                    if (!hasRepo) {
                        Text(
                            "Create a repository first",
                            style = MaterialTheme.typography.labelSmall,
                            color = Color.Gray,
                            modifier = Modifier.padding(top = 4.dp)
                        )
                    }
                }
            }
        }
    }
}

// ─── Resto do ecrã (inalterado) ───────────────────────────────────────────────

@Composable
fun CreateRepoPromptCard(onClick: () -> Unit) {
    Card(
        modifier = Modifier.fillMaxWidth().clickable(onClick = onClick),
        shape    = RoundedCornerShape(10.dp),
        colors   = CardDefaults.cardColors(containerColor = Color(0xFF1A2A1A)),
        border   = androidx.compose.foundation.BorderStroke(1.dp, Color(0xFF2E7D32))
    ) {
        Row(
            modifier          = Modifier.padding(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(Icons.Default.FolderSpecial, null, tint = Color(0xFF4CAF50), modifier = Modifier.size(32.dp))
            Spacer(modifier = Modifier.width(12.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text("Create My 3D Repository", fontWeight = FontWeight.Bold, color = Color.White)
                Text("Set up your GitHub workspace for AXEV 3D Studio",
                    style = MaterialTheme.typography.bodySmall, color = Color.Gray)
            }
            Icon(Icons.Default.Add, null, tint = Color(0xFF4CAF50))
        }
    }
}

@Composable
fun UserHeader(
    user: com.example.data.api.GitHubUser?,
    repo: String?,
    isLive: Boolean = false,
    onRefresh: () -> Unit,
    onLogout: () -> Unit
) {
    TacticalCard {
        Row(verticalAlignment = Alignment.CenterVertically) {
            if (user?.avatar_url != null) {
                AsyncImage(
                    model             = user.avatar_url,
                    contentDescription = null,
                    modifier          = Modifier.size(48.dp).clip(CircleShape),
                    contentScale      = ContentScale.Crop
                )
            } else {
                Icon(Icons.Default.AccountCircle, null,
                    modifier = Modifier.size(48.dp), tint = Color.Gray)
            }
            Spacer(modifier = Modifier.width(16.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text  = user?.name ?: user?.login ?: "Demo User",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold
                )
                Text("@${user?.login ?: "demo"}",
                    style = MaterialTheme.typography.labelSmall, color = Color.Gray)
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(repo ?: "No repository selected",
                        style = MaterialTheme.typography.labelSmall, color = TacticalOrange)
                    if (isLive) {
                        Spacer(modifier = Modifier.width(6.dp))
                        Box(
                            modifier = Modifier
                                .size(6.dp)
                                .background(Color(0xFF4CAF50), CircleShape)
                        )
                        Spacer(modifier = Modifier.width(3.dp))
                        Text("LIVE", style = MaterialTheme.typography.labelSmall,
                            color = Color(0xFF4CAF50), fontWeight = FontWeight.Bold)
                    }
                }
            }
            IconButton(onClick = onRefresh) {
                Icon(Icons.Default.Refresh, null, tint = Color.White)
            }
            IconButton(onClick = onLogout) {
                Icon(Icons.Default.Logout, null, tint = Color.Gray)
            }
        }
    }
}

@Composable
fun QuickStats(runs: List<WorkflowRun>) {
    val completed  = runs.count { it.conclusion == "success" }
    val inProgress = runs.count { it.status == "in_progress" || it.status == "queued" }
    val failed     = runs.count { it.conclusion == "failure" }

    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
        StatItem(label = "DONE",    value = completed.toString(),  modifier = Modifier.weight(1f))
        StatItem(label = "PENDING", value = inProgress.toString(), modifier = Modifier.weight(1f))
        StatItem(label = "FAILED",  value = failed.toString(),     modifier = Modifier.weight(1f))
    }
}

@Composable
fun StatItem(label: String, value: String, modifier: Modifier = Modifier) {
    TacticalCard(modifier = modifier) {
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Text(text = value, style = MaterialTheme.typography.headlineMedium,
                fontWeight = FontWeight.Black, color = TacticalOrange)
            Text(text = label, style = MaterialTheme.typography.labelSmall, color = Color.Gray)
        }
    }
}

@Composable
fun GenerationItem(run: WorkflowRun) {
    TacticalCard {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Column(modifier = Modifier.weight(1f)) {
                Text(text = run.name, style = MaterialTheme.typography.bodyLarge,
                    fontWeight = FontWeight.Bold)
                Text(text = "ID: ${run.id}", style = MaterialTheme.typography.labelSmall,
                    color = Color.Gray)
            }
            StatusBadge(status = run.conclusion ?: run.status)
        }
    }
}
