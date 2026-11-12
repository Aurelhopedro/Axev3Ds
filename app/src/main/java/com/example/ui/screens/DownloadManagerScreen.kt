package com.example.ui.screens

import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.api.GeneratedFile
import com.example.data.api.WorkflowRun
import com.example.data.repository.GenerationRepository.DownloadStatus
import com.example.ui.ArtifactGroup
import com.example.ui.MainViewModel
import com.example.ui.theme.*

// ─────────────────────────────────────────────────────────────────────────────
// DownloadManagerScreen — lista todos os runs com artifacts disponíveis e
// permite descarregar diretamente para a pasta Downloads, sem abrir browser.
// ─────────────────────────────────────────────────────────────────────────────

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DownloadManagerScreen(
    viewModel: MainViewModel,
    onBack: () -> Unit
) {
    val artifactGroups  by viewModel.artifactGroups.collectAsState()
    val downloadStates  by viewModel.downloadStates.collectAsState()
    val isLoadingGroups by viewModel.isLoadingArtifacts.collectAsState()

    // Carregar ao entrar no ecrã
    LaunchedEffect(Unit) { viewModel.loadArtifactGroups() }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Column {
                        Text("Downloads", fontWeight = FontWeight.Bold, color = TextPrimary)
                        Text("Modelos 3D gerados", style = MaterialTheme.typography.labelSmall, color = TextSecondary)
                    }
                },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.Default.ArrowBack, contentDescription = "Voltar", tint = TextPrimary)
                    }
                },
                actions = {
                    IconButton(onClick = { viewModel.loadArtifactGroups() }) {
                        Icon(Icons.Default.Refresh, contentDescription = "Atualizar", tint = TextSecondary)
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = PremiumDark)
            )
        },
        containerColor = PremiumDark
    ) { padding ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
        ) {
            when {
                isLoadingGroups -> {
                    Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                        Column(horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(16.dp)) {
                            CircularProgressIndicator(color = TacticalOrange)
                            Text("A carregar artifacts…", color = TextSecondary, fontSize = 13.sp)
                        }
                    }
                }

                artifactGroups.isEmpty() -> {
                    Box(modifier = Modifier.fillMaxSize().padding(32.dp), contentAlignment = Alignment.Center) {
                        Column(horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(12.dp)) {
                            Text("📦", fontSize = 48.sp)
                            Text("Nenhum modelo disponível", color = TextPrimary, fontWeight = FontWeight.Bold, fontSize = 16.sp)
                            Text(
                                "Gera o teu primeiro personagem 3D para o ver aqui.",
                                color = TextSecondary,
                                fontSize = 13.sp
                            )
                            Spacer(modifier = Modifier.height(8.dp))
                            Button(
                                onClick = onBack,
                                colors = ButtonDefaults.buttonColors(containerColor = TacticalOrange),
                                shape = RoundedCornerShape(12.dp)
                            ) {
                                Text("Ir para o Dashboard", color = Color.Black, fontWeight = FontWeight.Bold)
                            }
                        }
                    }
                }

                else -> {
                    LazyColumn(
                        modifier = Modifier.fillMaxSize().padding(horizontal = 16.dp),
                        contentPadding = PaddingValues(vertical = 16.dp),
                        verticalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        item {
                            // Sumário no topo
                            val totalFiles = artifactGroups.sumOf { it.files.size }
                            Text(
                                "${artifactGroups.size} geração${if (artifactGroups.size != 1) "ões" else ""} · $totalFiles ficheiro${if (totalFiles != 1) "s" else ""}",
                                color = TextMuted,
                                fontSize = 12.sp,
                                modifier = Modifier.padding(bottom = 4.dp)
                            )
                        }

                        items(artifactGroups, key = { it.run.id }) { group ->
                            ArtifactGroupCard(
                                group          = group,
                                downloadStates = downloadStates,
                                onDownload     = { file -> viewModel.downloadFile(file) }
                            )
                        }
                    }
                }
            }
        }
    }
}

// ─── Card de um run com os seus artifacts ─────────────────────────────────────

@Composable
private fun ArtifactGroupCard(
    group: ArtifactGroup,
    downloadStates: Map<Long, DownloadStatus>,
    onDownload: (GeneratedFile) -> Unit
) {
    var expanded by remember { mutableStateOf(true) }
    val rotationAngle by animateFloatAsState(
        targetValue = if (expanded) 180f else 0f,
        label = "chevron"
    )

    // Cor do border conforme o conclusion do run
    val borderColor = when (group.run.conclusion) {
        "success" -> SuccessGreen.copy(alpha = 0.4f)
        "failure" -> ErrorRed.copy(alpha = 0.4f)
        else      -> BorderSubtle
    }

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .border(1.dp, borderColor, RoundedCornerShape(16.dp)),
        shape  = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = SurfaceElevated)
    ) {
        Column(modifier = Modifier.padding(16.dp)) {

            // ── Cabeçalho do run ──────────────────────────────────────────
            Row(
                modifier          = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                // Ícone de estado
                Box(
                    modifier = Modifier
                        .size(38.dp)
                        .background(
                            when (group.run.conclusion) {
                                "success" -> SuccessGlow
                                "failure" -> ErrorGlow
                                else      -> TacticalOrangeGlow
                            },
                            RoundedCornerShape(10.dp)
                        ),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        when (group.run.conclusion) {
                            "success" -> "✅"
                            "failure" -> "❌"
                            else      -> "⏳"
                        },
                        fontSize = 18.sp
                    )
                }

                Spacer(modifier = Modifier.width(12.dp))

                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        group.run.name,
                        color      = TextPrimary,
                        fontWeight = FontWeight.Bold,
                        fontSize   = 14.sp,
                        maxLines   = 1
                    )
                    Text(
                        "Run #${group.run.id} · ${group.files.size} ficheiro${if (group.files.size != 1) "s" else ""}",
                        color    = TextMuted,
                        fontSize = 11.sp
                    )
                }

                // Chevron expand/collapse
                if (group.files.isNotEmpty()) {
                    IconButton(onClick = { expanded = !expanded }) {
                        Icon(
                            Icons.Default.ExpandMore,
                            contentDescription = if (expanded) "Recolher" else "Expandir",
                            tint     = TextSecondary,
                            modifier = Modifier.rotate(rotationAngle)
                        )
                    }
                }
            }

            // ── Lista de ficheiros ────────────────────────────────────────
            AnimatedVisibility(
                visible = expanded && group.files.isNotEmpty(),
                enter   = expandVertically() + fadeIn(),
                exit    = shrinkVertically() + fadeOut()
            ) {
                Column(
                    modifier = Modifier.padding(top = 12.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    HorizontalDivider(color = BorderSubtle, thickness = 1.dp)
                    Spacer(modifier = Modifier.height(4.dp))
                    group.files.forEach { file ->
                        ArtifactFileRow(
                            file           = file,
                            downloadStatus = downloadStates[file.artifactId],
                            onDownload     = { onDownload(file) }
                        )
                    }
                }
            }

            // Sem artifacts disponíveis
            if (group.files.isEmpty()) {
                Spacer(modifier = Modifier.height(8.dp))
                Text(
                    "Nenhum artifact disponível para este run.",
                    color    = TextMuted,
                    fontSize = 12.sp
                )
            }
        }
    }
}

// ─── Linha de um ficheiro individual ─────────────────────────────────────────

@Composable
private fun ArtifactFileRow(
    file: GeneratedFile,
    downloadStatus: DownloadStatus?,
    onDownload: () -> Unit
) {
    Row(
        modifier          = Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically
    ) {
        // Ícone do tipo de ficheiro
        Text(file.icon, fontSize = 22.sp)
        Spacer(modifier = Modifier.width(10.dp))

        // Info
        Column(modifier = Modifier.weight(1f)) {
            Text(file.name, color = TextPrimary, fontWeight = FontWeight.Medium, fontSize = 13.sp)
            Text(file.sizeMb, color = TextMuted, fontSize = 11.sp)
            // Feedback de estado
            when (downloadStatus) {
                is DownloadStatus.Downloading ->
                    Text("A transferir…", color = TacticalOrange, fontSize = 11.sp)
                is DownloadStatus.Done ->
                    Text("✓ Guardado em Downloads", color = SuccessGreen, fontSize = 11.sp)
                is DownloadStatus.Failed ->
                    Text("⚠ ${downloadStatus.message}", color = ErrorRed, fontSize = 11.sp)
                null -> Unit
            }
        }

        Spacer(modifier = Modifier.width(8.dp))

        // Botão de download
        when (downloadStatus) {
            is DownloadStatus.Downloading -> {
                CircularProgressIndicator(
                    modifier    = Modifier.size(32.dp),
                    color       = TacticalOrange,
                    strokeWidth = 3.dp
                )
            }
            is DownloadStatus.Done -> {
                FilledIconButton(
                    onClick = {},
                    modifier = Modifier.size(36.dp),
                    colors   = IconButtonDefaults.filledIconButtonColors(containerColor = SuccessGreen)
                ) {
                    Icon(Icons.Default.Check, contentDescription = "Transferido", tint = Color.Black, modifier = Modifier.size(18.dp))
                }
            }
            else -> {
                // idle ou erro
                FilledIconButton(
                    onClick  = onDownload,
                    modifier = Modifier.size(36.dp),
                    colors   = IconButtonDefaults.filledIconButtonColors(containerColor = TacticalOrange)
                ) {
                    Icon(Icons.Default.Download, contentDescription = "Descarregar ${file.name}", tint = Color.Black, modifier = Modifier.size(18.dp))
                }
            }
        }
    }
}
