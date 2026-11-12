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
import androidx.compose.material.icons.outlined.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.blur
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.draw.shadow
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
import com.example.ui.theme.*

private val CtaGradient = Brush.horizontalGradient(listOf(PrimaryOrange, PrimaryOrangeEnd))

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
            containerColor   = CardSurfaceElevated,
            shape            = RoundedCornerShape(24.dp),
            icon    = { Icon(Icons.Outlined.Logout, null, tint = SoftError) },
            title   = { Text("Terminar sessão?", color = TextPrimary) },
            text    = { Text("Você será desconectado da sua conta GitHub.", color = TextSecondary) },
            confirmButton = {
                Button(
                    onClick = { showLogoutDialog = false; viewModel.logout() },
                    colors  = ButtonDefaults.buttonColors(containerColor = SoftError),
                    shape   = RoundedCornerShape(14.dp)
                ) { Text("Sair") }
            },
            dismissButton = {
                TextButton(onClick = { showLogoutDialog = false }) {
                    Text("Cancelar", color = PrimaryOrange)
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
                .background(AppBackground)
                .padding(horizontal = 20.dp),
            contentPadding = PaddingValues(vertical = 24.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {

            if (flags.maintenanceMode) {
                item { MaintenanceBanner(message = flags.maintenanceMessage) }
            }

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

            item {
                if (onNavigateToCreateRepo != null || repo == null) {
                    CreateRepoPromptCard(onClick = onNavigateToCreateRepo ?: {})
                }
            }

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

            item { QuickStats(runs) }

            item { SectionHeader(title = "Atividade recente") }

            if (runs.isEmpty()) {
                item {
                    Box(
                        modifier         = Modifier.fillMaxWidth().padding(vertical = 40.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            Icon(Icons.Outlined.AutoAwesome, contentDescription = null, tint = TextMuted, modifier = Modifier.size(28.dp))
                            Spacer(modifier = Modifier.height(8.dp))
                            Text(
                                "Nenhuma geração recente.",
                                style = MaterialTheme.typography.bodyMedium,
                                color = TextSecondary
                            )
                        }
                    }
                }
            } else {
                items(runs) { run -> GenerationItem(run) }
            }
        }
    }
}

@Composable
fun MaintenanceBanner(message: String) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .shadow(8.dp, RoundedCornerShape(18.dp), ambientColor = Color.Black.copy(0.15f), spotColor = Color.Black.copy(0.15f)),
        shape  = RoundedCornerShape(18.dp),
        colors = CardDefaults.cardColors(containerColor = CardSurfaceElevated)
    ) {
        Row(
            modifier          = Modifier.padding(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                modifier = Modifier.size(36.dp).background(PendingAmber.copy(alpha = 0.15f), RoundedCornerShape(10.dp)),
                contentAlignment = Alignment.Center
            ) {
                Icon(Icons.Outlined.Build, contentDescription = null, tint = PendingAmber, modifier = Modifier.size(18.dp))
            }
            Spacer(modifier = Modifier.width(12.dp))
            Text(
                text  = message.ifBlank { "Serviço em manutenção. Tente novamente mais tarde." },
                color = TextPrimary,
                style = MaterialTheme.typography.bodyMedium
            )
        }
    }
}

@Composable
fun GenerateButton(onClick: () -> Unit) {
    val infiniteTransition = rememberInfiniteTransition(label = "cta")
    val glowAlpha by infiniteTransition.animateFloat(
        initialValue = 0.25f,
        targetValue  = 0.55f,
        animationSpec = infiniteRepeatable(
            tween(1400, easing = EaseInOutSine),
            RepeatMode.Reverse
        ),
        label = "glowAlpha"
    )

    Box(
        modifier = Modifier
            .fillMaxWidth()
            .height(64.dp)
    ) {
        Box(
            modifier = Modifier
                .matchParentSize()
                .blur(22.dp)
                .background(PrimaryOrange.copy(alpha = glowAlpha), RoundedCornerShape(20.dp))
        )
        Button(
            onClick  = onClick,
            modifier = Modifier
                .fillMaxSize()
                .shadow(12.dp, RoundedCornerShape(20.dp), ambientColor = PrimaryGlow, spotColor = PrimaryGlow),
            shape    = RoundedCornerShape(20.dp),
            colors   = ButtonDefaults.buttonColors(containerColor = Color.Transparent),
            contentPadding = PaddingValues(0.dp)
        ) {
            Box(
                modifier         = Modifier.fillMaxSize().background(CtaGradient, RoundedCornerShape(20.dp)),
                contentAlignment = Alignment.Center
            ) {
                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.Center) {
                    Icon(Icons.Outlined.AutoAwesome, contentDescription = null, tint = AppBackground, modifier = Modifier.size(22.dp))
                    Spacer(modifier = Modifier.width(10.dp))
                    Text(
                        "GERAR PERSONAGEM",
                        fontWeight = FontWeight.Bold,
                        fontSize = 16.sp,
                        color = AppBackground,
                        letterSpacing = 0.6.sp
                    )
                }
            }
        }
    }
}

@Composable
fun SmokeTestCard(state: SmokeTestState, hasRepo: Boolean, onRun: () -> Unit, onReset: () -> Unit) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .shadow(10.dp, RoundedCornerShape(20.dp), ambientColor = Color.Black.copy(0.16f), spotColor = Color.Black.copy(0.16f)),
        shape  = RoundedCornerShape(20.dp),
        colors = CardDefaults.cardColors(containerColor = CardSurface)
    ) {
        Column(modifier = Modifier.padding(20.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Box(
                    modifier = Modifier.size(44.dp).background(PrimaryGlow, RoundedCornerShape(14.dp)),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(Icons.Outlined.VerifiedUser, null, tint = PrimaryOrange, modifier = Modifier.size(22.dp))
                }
                Spacer(modifier = Modifier.width(14.dp))
                Column(modifier = Modifier.weight(1f)) {
                    Text("Verificação Rápida", fontWeight = FontWeight.SemiBold, color = TextPrimary, fontSize = 16.sp)
                    Text("6 testes • fluxos principais", style = MaterialTheme.typography.bodySmall, color = TextSecondary)
                }
            }
            Spacer(modifier = Modifier.height(16.dp))
            when (state) {
                is SmokeTestState.Running -> {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        CircularProgressIndicator(modifier = Modifier.size(18.dp), strokeWidth = 2.dp, color = PrimaryOrange)
                        Spacer(modifier = Modifier.width(10.dp))
                        Text("Enviando para o GitHub Actions…", color = TextSecondary, fontSize = 14.sp)
                    }
                }
                is SmokeTestState.Started -> {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(Icons.Outlined.CheckCircle, null, tint = MintSuccess, modifier = Modifier.size(18.dp))
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(state.message, color = MintSuccess, fontWeight = FontWeight.Medium, fontSize = 14.sp)
                    }
                    Spacer(modifier = Modifier.height(12.dp))
                    OutlinedButton(
                        onClick = onReset,
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(14.dp),
                        colors = ButtonDefaults.outlinedButtonColors(contentColor = MintSuccess),
                        border = androidx.compose.foundation.BorderStroke(1.dp, MintSuccess.copy(0.4f))
                    ) { Text("OK") }
                }
                is SmokeTestState.Failed -> {
                    Row(verticalAlignment = Alignment.Top) {
                        Icon(Icons.Outlined.ErrorOutline, null, tint = SoftError, modifier = Modifier.size(18.dp))
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(state.message, color = SoftError, fontSize = 14.sp)
                    }
                    Spacer(modifier = Modifier.height(12.dp))
                    Button(
                        onClick = onRun,
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(14.dp),
                        colors = ButtonDefaults.buttonColors(containerColor = SoftError, contentColor = TextPrimary)
                    ) { Text("Tentar novamente") }
                }
                else -> {
                    OutlinedButton(
                        onClick = onRun,
                        enabled = hasRepo,
                        modifier = Modifier.fillMaxWidth().height(48.dp),
                        shape = RoundedCornerShape(14.dp),
                        colors = ButtonDefaults.outlinedButtonColors(contentColor = PrimaryOrange, disabledContentColor = TextMuted),
                        border = androidx.compose.foundation.BorderStroke(1.dp, if (hasRepo) PrimaryOrange.copy(0.5f) else BorderSoft)
                    ) {
                        Icon(Icons.Outlined.VerifiedUser, null, modifier = Modifier.size(18.dp))
                        Spacer(modifier = Modifier.width(8.dp))
                        Text("Verificação Rápida", fontWeight = FontWeight.Medium)
                    }
                    if (!hasRepo) Text("Crie um repositório primeiro", style = MaterialTheme.typography.labelSmall, color = TextMuted, modifier = Modifier.padding(top = 8.dp))
                }
            }
        }
    }
}

@Composable
fun CreateRepoPromptCard(onClick: () -> Unit) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .shadow(10.dp, RoundedCornerShape(20.dp), ambientColor = Color.Black.copy(0.16f), spotColor = Color.Black.copy(0.16f))
            .clickable(onClick = onClick),
        shape  = RoundedCornerShape(20.dp),
        colors = CardDefaults.cardColors(containerColor = CardSurface)
    ) {
        Row(modifier = Modifier.padding(18.dp), verticalAlignment = Alignment.CenterVertically) {
            Box(
                modifier = Modifier.size(46.dp).background(MintGlow, RoundedCornerShape(14.dp)),
                contentAlignment = Alignment.Center
            ) {
                Icon(Icons.Outlined.ViewInAr, null, tint = MintSuccess, modifier = Modifier.size(22.dp))
            }
            Spacer(modifier = Modifier.width(14.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text("Criar repositório 3D", fontWeight = FontWeight.SemiBold, color = TextPrimary, fontSize = 16.sp)
                Spacer(modifier = Modifier.height(2.dp))
                Text("Configure seu espaço no GitHub para o AXEV 3D Studio", style = MaterialTheme.typography.bodySmall, color = TextSecondary)
            }
            Icon(Icons.Outlined.ChevronRight, null, tint = TextMuted, modifier = Modifier.size(20.dp))
        }
    }
}

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
    val infiniteTransition = rememberInfiniteTransition(label = "sync")
    val syncRotation by infiniteTransition.animateFloat(
        initialValue  = 0f,
        targetValue   = 360f,
        animationSpec = infiniteRepeatable(tween(900, easing = LinearEasing)),
        label         = "syncRotation"
    )
    val isSyncing = syncState is RemoteConfigSyncManager.SyncState.Syncing

    val syncIconColor = when (syncState) {
        is RemoteConfigSyncManager.SyncState.Syncing -> PrimaryOrange
        is RemoteConfigSyncManager.SyncState.Success -> MintSuccess
        is RemoteConfigSyncManager.SyncState.Error   -> SoftError
        else                                         -> TextSecondary
    }

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .shadow(12.dp, RoundedCornerShape(24.dp), ambientColor = Color.Black.copy(0.18f), spotColor = Color.Black.copy(0.18f)),
        shape  = RoundedCornerShape(24.dp),
        colors = CardDefaults.cardColors(containerColor = CardSurface)
    ) {
        Row(modifier = Modifier.padding(18.dp), verticalAlignment = Alignment.CenterVertically) {
            Box(
                modifier = Modifier
                    .size(54.dp)
                    .background(Brush.linearGradient(listOf(PrimaryOrange.copy(0.5f), PrimaryOrangeEnd.copy(0.2f))), CircleShape)
                    .padding(2.dp)
                    .clip(CircleShape)
            ) {
                if (user?.avatar_url != null) {
                    AsyncImage(model = user.avatar_url, contentDescription = null, modifier = Modifier.fillMaxSize().clip(CircleShape), contentScale = ContentScale.Crop)
                } else {
                    Box(Modifier.fillMaxSize().background(CardSurfaceElevated, CircleShape), contentAlignment = Alignment.Center) {
                        Icon(Icons.Outlined.Person, null, tint = TextSecondary)
                    }
                }
            }
            Spacer(modifier = Modifier.width(14.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text(text = user?.name ?: user?.login ?: "Usuário", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.SemiBold, color = TextPrimary)
                Text("@${user?.login ?: \"demo\"}", style = MaterialTheme.typography.bodySmall, color = TextSecondary)
                Spacer(modifier = Modifier.height(2.dp))
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(Icons.Outlined.ViewInAr, contentDescription = null, tint = PrimaryOrange, modifier = Modifier.size(14.dp))
                    Spacer(modifier = Modifier.width(4.dp))
                    Text(repo ?: "Nenhum repositório", style = MaterialTheme.typography.labelMedium, color = PrimaryOrange)
                    if (isLive) {
                        Spacer(modifier = Modifier.width(8.dp))
                        LiveDot()
                        Spacer(modifier = Modifier.width(4.dp))
                        Text("LIVE", style = MaterialTheme.typography.labelSmall, color = MintSuccess, fontWeight = FontWeight.SemiBold, letterSpacing = 0.8.sp)
                    }
                }
            }
            IconButton(onClick = onSync, enabled = !isSyncing) {
                Icon(
                    imageVector = Icons.Outlined.Sync,
                    contentDescription = "Sincronizar",
                    tint = syncIconColor,
                    modifier = if (isSyncing) Modifier.rotate(syncRotation) else Modifier
                )
            }
            IconButton(onClick = onRefresh) { Icon(Icons.Outlined.Refresh, null, tint = TextSecondary) }
            IconButton(onClick = onLogout) { Icon(Icons.Outlined.Logout, null, tint = TextMuted) }
        }
    }
}

@Composable
private fun LiveDot() {
    val infinite = rememberInfiniteTransition(label = "live")
    val alpha by infinite.animateFloat(0.4f, 1f, infiniteRepeatable(tween(900), RepeatMode.Reverse), label = "a")
    Box(modifier = Modifier.size(7.dp).background(MintSuccess.copy(alpha = alpha), CircleShape))
}

@Composable
fun QuickStats(runs: List<WorkflowRun>) {
    val completed  = runs.count { it.conclusion == "success" }
    val inProgress = runs.count { it.status == "in_progress" || it.status == "queued" }
    val failed     = runs.count { it.conclusion == "failure" }
    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
        StatItem("Concluídos", completed.toString(),  MintSuccess,   Modifier.weight(1f))
        StatItem("Em execução", inProgress.toString(), PrimaryOrange, Modifier.weight(1f), running = inProgress > 0)
        StatItem("Falhas",  failed.toString(),     SoftError,       Modifier.weight(1f))
    }
}

@Composable
fun StatItem(label: String, value: String, color: Color, modifier: Modifier = Modifier, running: Boolean = false) {
    Card(
        modifier = modifier.shadow(8.dp, RoundedCornerShape(18.dp), ambientColor = Color.Black.copy(0.12f), spotColor = Color.Black.copy(0.12f)),
        shape    = RoundedCornerShape(18.dp),
        colors   = CardDefaults.cardColors(containerColor = CardSurface)
    ) {
        Column(
            modifier = Modifier.padding(vertical = 16.dp, horizontal = 10.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                if (running) {
                    val inf = rememberInfiniteTransition(label = "run")
                    val a by inf.animateFloat(0.3f, 1f, infiniteRepeatable(tween(700), RepeatMode.Reverse), label = "ra")
                    Box(modifier = Modifier.size(8.dp).background(color.copy(alpha = a), CircleShape))
                    Spacer(modifier = Modifier.width(6.dp))
                }
                Text(text = value, style = MaterialTheme.typography.headlineSmall, fontWeight = FontWeight.Bold, color = color)
            }
            Spacer(modifier = Modifier.height(4.dp))
            Text(text = label, style = MaterialTheme.typography.labelMedium, color = TextSecondary)
        }
    }
}

@Composable
fun GenerationItem(run: WorkflowRun) {
    val status = run.conclusion ?: run.status
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .shadow(8.dp, RoundedCornerShape(18.dp), ambientColor = Color.Black.copy(0.12f), spotColor = Color.Black.copy(0.12f)),
        shape  = RoundedCornerShape(18.dp),
        colors = CardDefaults.cardColors(containerColor = CardSurface)
    ) {
        Row(modifier = Modifier.padding(16.dp), verticalAlignment = Alignment.CenterVertically) {
            Column(modifier = Modifier.weight(1f)) {
                Text(text = run.name, style = MaterialTheme.typography.bodyLarge, fontWeight = FontWeight.SemiBold, color = TextPrimary)
                Spacer(modifier = Modifier.height(2.dp))
                Text(text = "ID ${run.id}", style = MaterialTheme.typography.labelSmall, color = TextMuted)
            }
            StatusBadge(status = status)
        }
    }
}
