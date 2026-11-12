package com.example.ui.screens

import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.blur
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage
import com.example.data.api.WorkflowRun
import com.example.data.repository.RemoteConfigSyncManager
import com.example.ui.MainViewModel
import com.example.ui.SmokeTestState
import com.example.ui.components.SectionHeader
import com.example.ui.components.StatusBadge
import com.example.ui.components.TacticalCard
import com.example.ui.theme.*

private val OrangeGradient = Brush.linearGradient(listOf(GradientStart, GradientMid))
private val SubtleBorder   = Brush.linearGradient(listOf(BorderOrange, BorderSubtle, BorderOrange))

@Composable
fun DashboardScreen(
    viewModel: MainViewModel,
    onNavigateToCreateRepo: (() -> Unit)? = null,
    onNavigateToViewer: (() -> Unit)? = null
) {
    val user           by viewModel.user.collectAsState()
    val repo           by viewModel.selectedRepo.collectAsState()
    val runs           by viewModel.workflowRuns.collectAsState()
    val smokeState     by viewModel.smokeTestState.collectAsState()
    val workflowOpts   by viewModel.workflowOptions.collectAsState()
    val flags          by viewModel.flags.collectAsState()
    val syncState      by viewModel.syncState.collectAsState()
    var showLogoutDialog by remember { mutableStateOf(false) }

    var visible by remember { mutableStateOf(false) }
    LaunchedEffect(Unit) { visible = true }

    if (showLogoutDialog) {
        AlertDialog(
            onDismissRequest = { showLogoutDialog = false },
            containerColor   = SurfaceElevated,
            shape            = RoundedCornerShape(20.dp),
            icon    = { Icon(Icons.Default.Logout, null, tint = ErrorRed) },
            title   = { Text("Terminar sessão?", color = TextPrimary) },
            text    = { Text("Vais ser desligado da tua conta GitHub.", color = TextSecondary) },
            confirmButton = {
                Button(
                    onClick = { showLogoutDialog = false; viewModel.logout() },
                    colors  = ButtonDefaults.buttonColors(containerColor = ErrorRed),
                    shape   = RoundedCornerShape(12.dp)
                ) { Text("Sair") }
            },
            dismissButton = {
                TextButton(onClick = { showLogoutDialog = false }) {
                    Text("Cancelar", color = TacticalOrange)
                }
            }
        )
    }

    AnimatedVisibility(
        visible = visible,
        enter   = fadeIn(tween(400)) + slideInVertically(tween(400)) { it / 8 }
    ) {
        LazyColumn(
            modifier       = Modifier
                .fillMaxSize()
                .background(PremiumDark)
                .padding(horizontal = 16.dp),
            contentPadding = PaddingValues(vertical = 20.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {

            // ── Banner de manutenção (flag remota) ────────────────────────
            if (flags.maintenanceMode) {
                item {
                    MaintenanceBanner(message = flags.maintenanceMessage)
                }
            }

            // ── Cabeçalho ─────────────────────────────────────────────────
            item {
                UserHeader(
                    user       = user,
                    repo       = repo,
                    isLive     = workflowOpts.isLive,
                    syncState  = syncState,
                    onRefresh  = {
                        viewModel.fetchWorkflowRuns()
                        viewModel.loadWorkflowOptions()
                        viewModel.loadFlags()
                    },
                    onSync     = { viewModel.syncNow() },
                    onLogout   = { showLogoutDialog = true }
                )
            }

            // ── Botão principal (oculto em manutenção) ────────────────────
            if (!flags.maintenanceMode) {
                item {
                    if (repo != null && onNavigateToViewer != null) {
                        GenerateButton(onClick = {
                            viewModel.generate()
                            onNavigateToViewer()
                        })
                    }
                }
            }

            // ── Criar repo ────────────────────────────────────────────────
            item {
                if (onNavigateToCreateRepo != null || repo == null) {
                    CreateRepoPromptCard(onClick = onNavigateToCreateRepo ?: {})
                }
            }

            // ── Smoke Test (controlado por flag) ──────────────────────────
            if (flags.smokeTestEnabled) {
                item {
                    SmokeTestCard(
                        state   = smokeState,
                        hasRepo = repo != null,
                        onRun   = { viewModel.runSmokeTest() },
                        onReset = { viewModel.resetSmokeTestState() }
                    )
                }
            }

            // ── Stats rápidas ─────────────────────────────────────────────
            item { QuickStats(runs) }

            // ── Recent Activity ───────────────────────────────────────────
            item { SectionHeader(title = "Recent Activity") }

            if (runs.isEmpty()) {
                item {
                    Box(
                        modifier          = Modifier.fillMaxWidth().padding(vertical = 32.dp),
                        contentAlignment  = Alignment.Center
                    ) {
                        Text(
                            "No recent generations found.",
                            style = MaterialTheme.typography.bodyMedium,
                            color = TextMuted
                        )
                    }
                }
            } else {
                items(runs) { run -> GenerationItem(run) }
            }
        }
    }
}

// ─── Banner de manutenção ─────────────────────────────────────────────────────

@Composable
fun MaintenanceBanner(message: String) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .border(1.dp, Color(0xFFFFC107).copy(alpha = 0.6f), RoundedCornerShape(12.dp)),
        shape  = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(containerColor = Color(0xFF2A2200))
    ) {
        Row(
            modifier          = Modifier.padding(14.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text("🔧", fontSize = 20.sp)
            Spacer(modifier = Modifier.width(10.dp))
            Text(
                text  = message.ifBlank { "Service is under maintenance. Please check back later." },
                color = Color(0xFFFFC107),
                style = MaterialTheme.typography.bodySmall,
                fontWeight = FontWeight.Medium
            )
        }
    }
}

// ─── Botão Generate premium ───────────────────────────────────────────────────

@Composable
fun GenerateButton(onClick: () -> Unit) {
    val infiniteTransition = rememberInfiniteTransition(label = "glow")
    val glowAlpha by infiniteTransition.animateFloat(
        initialValue = 0.4f,
        targetValue  = 1f,
        animationSpec = infiniteRepeatable(
            tween(1200, easing = EaseInOutSine),
            RepeatMode.Reverse
        ),
        label = "glowAlpha"
    )

    Box(modifier = Modifier.fillMaxWidth().height(60.dp)) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(60.dp)
                .blur(16.dp)
                .background(TacticalOrange.copy(alpha = glowAlpha * 0.35f), RoundedCornerShape(16.dp))
        )
        Button(
            onClick  = onClick,
            modifier = Modifier.fillMaxSize(),
            shape    = RoundedCornerShape(16.dp),
            colors   = ButtonDefaults.buttonColors(containerColor = Color.Transparent),
            contentPadding = PaddingValues(0.dp)
        ) {
            Box(
                modifier          = Modifier.fillMaxSize().background(OrangeGradient, RoundedCornerShape(16.dp)),
                contentAlignment  = Alignment.Center
            ) {
                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.Center) {
                    Text("⚡", fontSize = 20.sp)
                    Spacer(modifier = Modifier.width(10.dp))
                    Text("Generate Character", fontWeight = FontWeight.ExtraBold, fontSize = 16.sp, color = Color.White, letterSpacing = 0.5.sp)
                }
            }
        }
    }
}

// ─── Card de Smoke Test ───────────────────────────────────────────────────────

@Composable
fun SmokeTestCard(state: SmokeTestState, hasRepo: Boolean, onRun: () -> Unit, onReset: () -> Unit) {
    val borderColor = when (state) {
        is SmokeTestState.Started -> SuccessGreen
        is SmokeTestState.Failed  -> ErrorRed
        is SmokeTestState.Running -> TacticalOrange
        else                      -> BorderSubtle
    }

    Card(
        modifier = Modifier.fillMaxWidth().border(1.dp, borderColor.copy(alpha = 0.6f), RoundedCornerShape(16.dp)),
        shape  = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = SurfaceElevated)
    ) {
        Column(modifier = Modifier.padding(18.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Box(modifier = Modifier.size(40.dp).background(TacticalOrangeGlow, RoundedCornerShape(10.dp)), contentAlignment = Alignment.Center) {
                    Icon(Icons.Default.BugReport, null, tint = TacticalOrange, modifier = Modifier.size(22.dp))
                }
                Spacer(modifier = Modifier.width(12.dp))
                Column(modifier = Modifier.weight(1f)) {
                    Text("Smoke Test", fontWeight = FontWeight.Bold, color = TextPrimary, fontSize = 15.sp)
                    Text("6 test cases · all critical paths", style = MaterialTheme.typography.labelSmall, color = TextSecondary)
                }
            }
            Spacer(modifier = Modifier.height(14.dp))
            when (state) {
                is SmokeTestState.Running -> {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        CircularProgressIndicator(modifier = Modifier.size(18.dp), strokeWidth = 2.dp, color = TacticalOrange)
                        Spacer(modifier = Modifier.width(10.dp))
                        Text("Dispatching to GitHub Actions…", color = TextSecondary, fontSize = 13.sp)
                    }
                }
                is SmokeTestState.Started -> {
                    Text("✅  ${state.message}", color = SuccessGreen, fontWeight = FontWeight.SemiBold, fontSize = 14.sp)
                    Spacer(modifier = Modifier.height(10.dp))
                    OutlinedButton(onClick = onReset, modifier = Modifier.fillMaxWidth(), shape = RoundedCornerShape(12.dp),
                        colors = ButtonDefaults.outlinedButtonColors(contentColor = SuccessGreen),
                        border = androidx.compose.foundation.BorderStroke(1.dp, SuccessGreen.copy(0.5f))
                    ) { Text("OK") }
                }
                is SmokeTestState.Failed -> {
                    Text("❌  ${state.message}", color = ErrorRed, fontSize = 14.sp)
                    Spacer(modifier = Modifier.height(10.dp))
                    Button(onClick = onRun, modifier = Modifier.fillMaxWidth(), shape = RoundedCornerShape(12.dp),
                        colors = ButtonDefaults.buttonColors(containerColor = ErrorRed)
                    ) { Text("Retry Smoke Test") }
                }
                else -> {
                    Button(onClick = onRun, enabled = hasRepo, modifier = Modifier.fillMaxWidth(), shape = RoundedCornerShape(12.dp),
                        colors = ButtonDefaults.buttonColors(containerColor = TacticalOrange, disabledContainerColor = BorderSubtle)
                    ) {
                        Icon(Icons.Default.BugReport, null, modifier = Modifier.size(16.dp))
                        Spacer(modifier = Modifier.width(8.dp))
                        Text("Run Smoke Test")
                    }
                    if (!hasRepo) Text("Create a repository first", style = MaterialTheme.typography.labelSmall, color = TextMuted, modifier = Modifier.padding(top = 6.dp))
                }
            }
        }
    }
}

// ─── Card Criar Repo ──────────────────────────────────────────────────────────

@Composable
fun CreateRepoPromptCard(onClick: () -> Unit) {
    Card(
        modifier = Modifier.fillMaxWidth().clickable(onClick = onClick)
            .border(1.dp, SuccessGreen.copy(0.3f), RoundedCornerShape(16.dp)),
        shape  = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = SurfaceElevated)
    ) {
        Row(modifier = Modifier.padding(16.dp), verticalAlignment = Alignment.CenterVertically) {
            Box(modifier = Modifier.size(42.dp).background(SuccessGlow, RoundedCornerShape(12.dp)), contentAlignment = Alignment.Center) {
                Icon(Icons.Default.FolderSpecial, null, tint = SuccessGreen, modifier = Modifier.size(22.dp))
            }
            Spacer(modifier = Modifier.width(14.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text("Create My 3D Repository", fontWeight = FontWeight.Bold, color = TextPrimary, fontSize = 15.sp)
                Text("Set up your GitHub workspace for AXEV 3D Studio", style = MaterialTheme.typography.bodySmall, color = TextSecondary)
            }
            Icon(Icons.Default.ArrowForwardIos, null, tint = TextMuted, modifier = Modifier.size(16.dp))
        }
    }
}

// ─── Cabeçalho do utilizador ──────────────────────────────────────────────────

@Composable
fun UserHeader(
    user: com.example.data.api.GitHubUser?,
    repo: String?,
    isLive: Boolean = false,
    syncState: RemoteConfigSyncManager.SyncState = RemoteConfigSyncManager.SyncState.Idle,
    onRefresh: () -> Unit,
    onSync: () -> Unit,
    onLogout: () -> Unit
) {
    // Animação de rotação quando está a sincronizar
    val infiniteTransition = rememberInfiniteTransition(label = "sync")
    val syncRotation by infiniteTransition.animateFloat(
        initialValue  = 0f,
        targetValue   = 360f,
        animationSpec = infiniteRepeatable(tween(800, easing = LinearEasing)),
        label         = "syncRotation"
    )
    val isSyncing = syncState is RemoteConfigSyncManager.SyncState.Syncing

    val syncIconColor = when (syncState) {
        is RemoteConfigSyncManager.SyncState.Syncing -> TacticalOrange
        is RemoteConfigSyncManager.SyncState.Success -> SuccessGreen
        is RemoteConfigSyncManager.SyncState.Error   -> ErrorRed
        else                                         -> TextSecondary
    }

    Card(
        modifier = Modifier.fillMaxWidth().border(1.dp, BorderSubtle, RoundedCornerShape(20.dp)),
        shape  = RoundedCornerShape(20.dp),
        colors = CardDefaults.cardColors(containerColor = SurfaceElevated)
    ) {
        Row(modifier = Modifier.padding(16.dp), verticalAlignment = Alignment.CenterVertically) {
            Box(modifier = Modifier.size(52.dp).border(2.dp, OrangeGradient, CircleShape).padding(2.dp).clip(CircleShape)) {
                if (user?.avatar_url != null) {
                    AsyncImage(model = user.avatar_url, contentDescription = null, modifier = Modifier.fillMaxSize(), contentScale = ContentScale.Crop)
                } else {
                    Icon(Icons.Default.AccountCircle, null, modifier = Modifier.fillMaxSize(), tint = TextSecondary)
                }
            }
            Spacer(modifier = Modifier.width(14.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text(text = user?.name ?: user?.login ?: "Demo User", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold, color = TextPrimary)
                Text("@${user?.login ?: "demo"}", style = MaterialTheme.typography.labelSmall, color = TextSecondary)
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(repo ?: "No repository", style = MaterialTheme.typography.labelSmall, color = TacticalOrange)
                    if (isLive) {
                        Spacer(modifier = Modifier.width(6.dp))
                        Box(modifier = Modifier.size(6.dp).background(SuccessGreen, CircleShape))
                        Spacer(modifier = Modifier.width(4.dp))
                        Text("LIVE", style = MaterialTheme.typography.labelSmall, color = SuccessGreen, fontWeight = FontWeight.Bold, letterSpacing = 1.sp)
                    }
                }
            }
            // Botão Sincronizar config remota
            IconButton(onClick = onSync, enabled = !isSyncing) {
                Icon(
                    imageVector = Icons.Default.Sync,
                    contentDescription = "Sincronizar config",
                    tint = syncIconColor,
                    modifier = if (isSyncing) Modifier.rotate(syncRotation) else Modifier
                )
            }
            IconButton(onClick = onRefresh) { Icon(Icons.Default.Refresh, null, tint = TextSecondary) }
            IconButton(onClick = onLogout) { Icon(Icons.Default.Logout, null, tint = TextMuted) }
        }
    }
}

// ─── Stats rápidas ────────────────────────────────────────────────────────────

@Composable
fun QuickStats(runs: List<WorkflowRun>) {
    val completed  = runs.count { it.conclusion == "success" }
    val inProgress = runs.count { it.status == "in_progress" || it.status == "queued" }
    val failed     = runs.count { it.conclusion == "failure" }
    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
        StatItem("DONE",    completed.toString(),  SuccessGreen,   Modifier.weight(1f))
        StatItem("RUNNING", inProgress.toString(), TacticalOrange, Modifier.weight(1f))
        StatItem("FAILED",  failed.toString(),     ErrorRed,       Modifier.weight(1f))
    }
}

@Composable
fun StatItem(label: String, value: String, color: Color, modifier: Modifier = Modifier) {
    Card(
        modifier = modifier.border(1.dp, color.copy(0.25f), RoundedCornerShape(14.dp)),
        shape    = RoundedCornerShape(14.dp),
        colors   = CardDefaults.cardColors(containerColor = SurfaceElevated)
    ) {
        Column(modifier = Modifier.padding(vertical = 14.dp, horizontal = 8.dp), horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.Center) {
            Text(text = value, style = MaterialTheme.typography.headlineMedium, fontWeight = FontWeight.Black, color = color)
            Spacer(modifier = Modifier.height(2.dp))
            Text(text = label, style = MaterialTheme.typography.labelSmall, color = TextMuted, letterSpacing = 1.sp)
        }
    }
}

// ─── Item de geração ──────────────────────────────────────────────────────────

@Composable
fun GenerationItem(run: WorkflowRun) {
    Card(
        modifier = Modifier.fillMaxWidth().border(1.dp, BorderSubtle, RoundedCornerShape(14.dp)),
        shape  = RoundedCornerShape(14.dp),
        colors = CardDefaults.cardColors(containerColor = SurfaceElevated)
    ) {
        Row(modifier = Modifier.padding(14.dp), verticalAlignment = Alignment.CenterVertically) {
            Column(modifier = Modifier.weight(1f)) {
                Text(text = run.name, style = MaterialTheme.typography.bodyLarge, fontWeight = FontWeight.Bold, color = TextPrimary)
                Text(text = "ID: ${run.id}", style = MaterialTheme.typography.labelSmall, color = TextMuted)
            }
            StatusBadge(status = run.conclusion ?: run.status)
        }
    }
}
