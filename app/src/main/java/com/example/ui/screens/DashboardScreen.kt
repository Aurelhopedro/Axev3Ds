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
import androidx.compose.material.icons.filled.FolderSpecial
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
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
import com.example.ui.components.SectionHeader
import com.example.ui.components.StatusBadge
import com.example.ui.components.TacticalCard
import com.example.ui.theme.TacticalOrange

@Composable
fun DashboardScreen(viewModel: MainViewModel, onNavigateToCreateRepo: (() -> Unit)? = null) {
    val user by viewModel.user.collectAsState()
    val repo by viewModel.selectedRepo.collectAsState()
    val runs by viewModel.workflowRuns.collectAsState()

    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .padding(horizontal = 16.dp),
        contentPadding = PaddingValues(vertical = 16.dp)
    ) {
        item {
            UserHeader(user, repo, onRefresh = { viewModel.fetchWorkflowRuns() })
            Spacer(modifier = Modifier.height(16.dp))
        }

        // "Create My 3D Repository" card — shown when no repo selected or as a shortcut
        item {
            if (onNavigateToCreateRepo != null || repo == null) {
                CreateRepoPromptCard(onClick = onNavigateToCreateRepo ?: {})
                Spacer(modifier = Modifier.height(16.dp))
            }
        }

        item {
            QuickStats(runs)
            Spacer(modifier = Modifier.height(16.dp))
            SectionHeader(title = "Recent Activity")
        }

        if (runs.isEmpty()) {
            item {
                Text(
                    text = "No recent generations found.",
                    modifier = Modifier.padding(vertical = 32.dp),
                    style = MaterialTheme.typography.bodyMedium,
                    color = Color.Gray
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

@Composable
fun CreateRepoPromptCard(onClick: () -> Unit) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick),
        shape = RoundedCornerShape(10.dp),
        colors = CardDefaults.cardColors(
            containerColor = Color(0xFF1A2A1A)
        ),
        border = androidx.compose.foundation.BorderStroke(1.dp, Color(0xFF2E7D32))
    ) {
        Row(
            modifier = Modifier.padding(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(
                Icons.Default.FolderSpecial,
                contentDescription = null,
                tint = Color(0xFF4CAF50),
                modifier = Modifier.size(32.dp)
            )
            Spacer(modifier = Modifier.width(12.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    "Create My 3D Repository",
                    fontWeight = FontWeight.Bold,
                    color = Color.White
                )
                Text(
                    "Set up your GitHub workspace for AXEV 3D Studio",
                    style = MaterialTheme.typography.bodySmall,
                    color = Color.Gray
                )
            }
            Icon(
                Icons.Default.Add,
                contentDescription = null,
                tint = Color(0xFF4CAF50)
            )
        }
    }
}

@Composable
fun UserHeader(user: com.example.data.api.GitHubUser?, repo: String?, onRefresh: () -> Unit) {
    TacticalCard {
        Row(verticalAlignment = Alignment.CenterVertically) {
            if (user?.avatar_url != null) {
                AsyncImage(
                    model = user.avatar_url,
                    contentDescription = null,
                    modifier = Modifier
                        .size(48.dp)
                        .clip(CircleShape),
                    contentScale = ContentScale.Crop
                )
            } else {
                Icon(
                    Icons.Default.AccountCircle,
                    contentDescription = null,
                    modifier = Modifier.size(48.dp),
                    tint = Color.Gray
                )
            }
            Spacer(modifier = Modifier.width(16.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = user?.name ?: user?.login ?: "Unknown User",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold
                )
                Text(
                    text = "@${user?.login ?: "—"}",
                    style = MaterialTheme.typography.labelSmall,
                    color = Color.Gray
                )
                Text(
                    text = repo ?: "No repository selected",
                    style = MaterialTheme.typography.labelSmall,
                    color = TacticalOrange
                )
            }
            IconButton(onClick = onRefresh) {
                Icon(Icons.Default.Refresh, contentDescription = "Refresh", tint = Color.White)
            }
        }
    }
}

@Composable
fun QuickStats(runs: List<WorkflowRun>) {
    val completed = runs.count { it.conclusion == "success" }
    val inProgress = runs.count { it.status == "in_progress" || it.status == "queued" }
    val failed = runs.count { it.conclusion == "failure" }

    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
        StatItem(label = "DONE", value = completed.toString(), modifier = Modifier.weight(1f))
        StatItem(label = "PENDING", value = inProgress.toString(), modifier = Modifier.weight(1f))
        StatItem(label = "FAILED", value = failed.toString(), modifier = Modifier.weight(1f))
    }
}

@Composable
fun StatItem(label: String, value: String, modifier: Modifier = Modifier) {
    TacticalCard(modifier = modifier) {
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Text(
                text = value,
                style = MaterialTheme.typography.headlineMedium,
                fontWeight = FontWeight.Black,
                color = TacticalOrange
            )
            Text(
                text = label,
                style = MaterialTheme.typography.labelSmall,
                color = Color.Gray
            )
        }
    }
}

@Composable
fun GenerationItem(run: WorkflowRun) {
    TacticalCard {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = run.name,
                    style = MaterialTheme.typography.bodyLarge,
                    fontWeight = FontWeight.Bold
                )
                Text(
                    text = "ID: ${run.id}",
                    style = MaterialTheme.typography.labelSmall,
                    color = Color.Gray
                )
            }
            StatusBadge(status = run.conclusion ?: run.status)
        }
    }
}
