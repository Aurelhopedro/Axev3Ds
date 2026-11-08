package com.example.ui.screens

import androidx.compose.animation.core.*
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Download
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.api.GeneratedFile
import com.example.data.repository.GenerationRepository.DownloadStatus
import com.example.data.repository.GenerationRepository.GenerationStatus
import com.example.ui.MainViewModel
import com.example.ui.theme.TacticalOrange

// ─────────────────────────────────────────────────────────────────────────────
// ViewerScreen — "Magic Box"
// ─────────────────────────────────────────────────────────────────────────────

@Composable
fun ViewerScreen(
    viewModel: MainViewModel,
    onBack: () -> Unit
) {
    val generationStatus by viewModel.generationStatus.collectAsState()
    val downloadStates   by viewModel.downloadStates.collectAsState()

    Scaffold(
        topBar = {
            @OptIn(ExperimentalMaterial3Api::class)
            TopAppBar(
                title = { Text("Character Studio", fontWeight = FontWeight.Bold) },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.Default.ArrowBack, contentDescription = "Back")
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = Color(0xFF0A0A0A)
                )
            )
        },
        containerColor = Color(0xFF0A0A0A)
    ) { padding ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding),
            contentAlignment = Alignment.Center
        ) {
            when (val status = generationStatus) {
                is GenerationStatus.Preparing ->
                    LoadingContent(message = status.message, progress = 0.05f, remainingSec = null)

                is GenerationStatus.Processing ->
                    LoadingContent(
                        message      = status.message,
                        progress     = status.progress,
                        remainingSec = status.estimatedSec.takeIf { it > 0 }
                    )

                is GenerationStatus.Ready ->
                    ReadyContent(
                        files          = status.files,
                        downloadStates = downloadStates,
                        onDownload     = { file -> viewModel.downloadFile(file) },
                        onRetry        = { viewModel.retryGeneration() }
                    )

                is GenerationStatus.Failed ->
                    FailedContent(message = status.message, onRetry = { viewModel.retryGeneration() })

                null ->
                    IdleContent(onBack = onBack)
            }
        }
    }
}

// ─── Loading ──────────────────────────────────────────────────────────────────

@Composable
private fun LoadingContent(message: String, progress: Float, remainingSec: Int?) {
    val animatedProgress by animateFloatAsState(
        targetValue  = progress,
        animationSpec = tween(durationMillis = 600, easing = EaseInOutCubic),
        label        = "progress"
    )

    val pulse by rememberInfiniteTransition(label = "pulse").animateFloat(
        initialValue = 0.6f,
        targetValue  = 1f,
        animationSpec = infiniteRepeatable(
            animation  = tween(900, easing = EaseInOutSine),
            repeatMode = RepeatMode.Reverse
        ),
        label = "pulseAlpha"
    )

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(32.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(24.dp)
    ) {
        Text(text = "⚡", fontSize = 48.sp, color = TacticalOrange.copy(alpha = pulse))

        Text(
            text       = "Generating your character…",
            color      = Color.White,
            fontWeight = FontWeight.Bold,
            fontSize   = 18.sp
        )

        ProgressBar(label = message, progress = animatedProgress)

        SubStepBars(overallProgress = animatedProgress)

        // Fix 2 — mostrar tempo restante real quando disponível
        if (remainingSec != null && remainingSec > 0) {
            val minutes = remainingSec / 60
            val seconds = remainingSec % 60
            val timeText = when {
                minutes > 0 -> "~$minutes min ${seconds}s remaining"
                else        -> "~${seconds}s remaining"
            }
            Text(
                text     = timeText,
                color    = TacticalOrange.copy(alpha = 0.85f),
                fontSize = 12.sp,
                fontFamily = FontFamily.Monospace
            )
        } else {
            Text(
                text     = "This may take a few minutes.",
                color    = Color.Gray,
                fontSize = 12.sp
            )
        }
    }
}

@Composable
private fun ProgressBar(label: String, progress: Float) {
    Column(modifier = Modifier.fillMaxWidth(), verticalArrangement = Arrangement.spacedBy(6.dp)) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Text(label, color = Color(0xFFCCCCCC), fontSize = 13.sp, fontFamily = FontFamily.Monospace)
            Text(
                "${(progress * 100).toInt()}%",
                color      = TacticalOrange,
                fontSize   = 13.sp,
                fontWeight = FontWeight.Bold,
                fontFamily = FontFamily.Monospace
            )
        }
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(8.dp)
                .clip(RoundedCornerShape(4.dp))
                .background(Color(0xFF1E1E1E))
        ) {
            Box(
                modifier = Modifier
                    .fillMaxWidth(progress.coerceIn(0f, 1f))
                    .fillMaxHeight()
                    .clip(RoundedCornerShape(4.dp))
                    .background(TacticalOrange)
            )
        }
    }
}

@Composable
private fun SubStepBars(overallProgress: Float) {
    val steps = listOf(
        "Preparing mesh"    to 0.30f,
        "Applying textures" to 0.60f,
        "Exporting GLB"     to 0.90f
    )
    Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
        steps.forEach { (name, threshold) ->
            val stepProgress = when {
                overallProgress >= threshold        -> 1f
                overallProgress >= threshold - 0.3f ->
                    (overallProgress - (threshold - 0.3f)) / 0.3f
                else -> 0f
            }.coerceIn(0f, 1f)

            val animated by animateFloatAsState(
                targetValue   = stepProgress,
                animationSpec = tween(500),
                label         = "sub_$name"
            )

            Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                Text(name, color = Color(0xFF888888), fontSize = 11.sp, fontFamily = FontFamily.Monospace)
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(4.dp)
                        .clip(RoundedCornerShape(2.dp))
                        .background(Color(0xFF1E1E1E))
                ) {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth(animated)
                            .fillMaxHeight()
                            .clip(RoundedCornerShape(2.dp))
                            .background(
                                if (animated >= 1f) Color(0xFF4CAF50) else TacticalOrange.copy(alpha = 0.7f)
                            )
                    )
                }
            }
        }
    }
}

// ─── Pronto ───────────────────────────────────────────────────────────────────

@Composable
private fun ReadyContent(
    files: List<GeneratedFile>,
    downloadStates: Map<Long, DownloadStatus>,
    onDownload: (GeneratedFile) -> Unit,
    onRetry: () -> Unit
) {
    LazyColumn(
        modifier = Modifier
            .fillMaxWidth()
            .padding(24.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        item {
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                modifier = Modifier.fillMaxWidth()
            ) {
                Text("✅", fontSize = 48.sp)
                Spacer(modifier = Modifier.height(8.dp))
                Text(
                    "Your character is ready!",
                    color      = Color.White,
                    fontWeight = FontWeight.Bold,
                    fontSize   = 20.sp
                )
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    "${files.size} file${if (files.size != 1) "s" else ""} generated",
                    color    = Color.Gray,
                    fontSize = 13.sp
                )
            }
        }

        items(files) { file ->
            FileCard(
                file           = file,
                downloadStatus = downloadStates[file.artifactId],
                onDownload     = { onDownload(file) }
            )
        }

        item {
            Spacer(modifier = Modifier.height(8.dp))
            OutlinedButton(
                onClick  = onRetry,
                modifier = Modifier.fillMaxWidth(),
                colors   = ButtonDefaults.outlinedButtonColors(contentColor = TacticalOrange)
            ) {
                Icon(Icons.Default.Refresh, contentDescription = null, modifier = Modifier.size(16.dp))
                Spacer(modifier = Modifier.width(8.dp))
                Text("Generate Another")
            }
        }
    }
}

// Fix 3 — FileCard com lógica de download real
@Composable
private fun FileCard(
    file: GeneratedFile,
    downloadStatus: DownloadStatus?,
    onDownload: () -> Unit
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors   = CardDefaults.cardColors(containerColor = Color(0xFF1A1A1A)),
        shape    = RoundedCornerShape(12.dp)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalAlignment     = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            // Info do ficheiro
            Row(
                verticalAlignment     = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(12.dp),
                modifier              = Modifier.weight(1f)
            ) {
                Text(file.icon, fontSize = 28.sp)
                Column {
                    Text(
                        file.name,
                        color      = Color.White,
                        fontWeight = FontWeight.SemiBold,
                        fontSize   = 14.sp
                    )
                    Text(
                        file.sizeMb,
                        color    = Color.Gray,
                        fontSize = 12.sp
                    )
                    // Feedback de estado do download
                    when (downloadStatus) {
                        is DownloadStatus.Downloading ->
                            Text("Downloading…", color = TacticalOrange, fontSize = 11.sp)
                        is DownloadStatus.Done ->
                            Text("✓ Saved to Downloads", color = Color(0xFF4CAF50), fontSize = 11.sp)
                        is DownloadStatus.Failed ->
                            Text("⚠ ${downloadStatus.message}", color = Color(0xFFFF6B6B), fontSize = 11.sp)
                        null -> Unit
                    }
                }
            }

            // Botão de download — estados: idle / a transferir / concluído / erro
            when (downloadStatus) {
                is DownloadStatus.Downloading -> {
                    CircularProgressIndicator(
                        modifier  = Modifier.size(36.dp),
                        color     = TacticalOrange,
                        strokeWidth = 3.dp
                    )
                }
                is DownloadStatus.Done -> {
                    FilledIconButton(
                        onClick = { /* já descarregado */ },
                        colors  = IconButtonDefaults.filledIconButtonColors(
                            containerColor = Color(0xFF4CAF50)
                        )
                    ) {
                        Icon(Icons.Default.Check, contentDescription = "Downloaded", tint = Color.Black)
                    }
                }
                else -> {
                    // idle ou erro — mostrar botão de download
                    FilledIconButton(
                        onClick = onDownload,
                        colors  = IconButtonDefaults.filledIconButtonColors(
                            containerColor = TacticalOrange
                        )
                    ) {
                        Icon(Icons.Default.Download, contentDescription = "Download ${file.name}", tint = Color.Black)
                    }
                }
            }
        }
    }
}

// ─── Erro ─────────────────────────────────────────────────────────────────────

@Composable
private fun FailedContent(message: String, onRetry: () -> Unit) {
    Column(
        modifier = Modifier.padding(32.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        Text("⚠️", fontSize = 48.sp)
        Text(message, color = Color(0xFFFF6B6B), fontWeight = FontWeight.Medium, fontSize = 15.sp)
        Button(
            onClick = onRetry,
            colors  = ButtonDefaults.buttonColors(containerColor = TacticalOrange)
        ) {
            Icon(Icons.Default.Refresh, contentDescription = null, modifier = Modifier.size(16.dp))
            Spacer(modifier = Modifier.width(8.dp))
            Text("Try Again", color = Color.Black)
        }
    }
}

// ─── Estado inicial ───────────────────────────────────────────────────────────

@Composable
private fun IdleContent(onBack: () -> Unit) {
    Column(
        modifier = Modifier.padding(32.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        Text("🎭", fontSize = 48.sp)
        Text("No generation in progress.", color = Color.Gray, fontSize = 15.sp)
        OutlinedButton(onClick = onBack) {
            Text("Go back to Creator", color = TacticalOrange)
        }
    }
}
