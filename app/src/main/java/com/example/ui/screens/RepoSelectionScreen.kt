package com.example.ui.screens

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.ChevronRight
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.example.ui.MainViewModel
import com.example.ui.components.SectionHeader
import com.example.ui.components.TacticalCard
import com.example.ui.theme.TacticalOrange

@Composable
fun RepoSelectionScreen(
    viewModel: MainViewModel,
    onRepoSelected: () -> Unit,
    onCreateNew: (() -> Unit)? = null
) {
    val repos by viewModel.repos.collectAsState()
    var filter by remember { mutableStateOf("") }

    val filteredRepos = repos.filter { it.full_name.contains(filter, ignoreCase = true) }

    Column(modifier = Modifier.fillMaxSize().padding(16.dp)) {
        SectionHeader(title = "SELECT REPOSITORY")

        OutlinedTextField(
            value = filter,
            onValueChange = { filter = it },
            modifier = Modifier.fillMaxWidth(),
            placeholder = { Text("Search repositories...") },
            leadingIcon = { Icon(Icons.Default.Search, contentDescription = null) }
        )

        Spacer(modifier = Modifier.height(12.dp))

        // Create new repo option
        if (onCreateNew != null) {
            TacticalCard(
                modifier = Modifier.clickable { onCreateNew() }
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(Icons.Default.Add, contentDescription = null, tint = Color(0xFF4CAF50))
                    Spacer(modifier = Modifier.width(12.dp))
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            "Create My 3D Repository",
                            fontWeight = FontWeight.Bold,
                            color = Color.White
                        )
                        Text(
                            "Set up a new axev-3d-workspace on your GitHub",
                            style = MaterialTheme.typography.labelSmall,
                            color = Color.Gray
                        )
                    }
                    Icon(Icons.Default.ChevronRight, contentDescription = null, tint = Color(0xFF4CAF50))
                }
            }
            Spacer(modifier = Modifier.height(12.dp))
            Divider(color = Color.DarkGray)
            Spacer(modifier = Modifier.height(12.dp))
        }

        if (filteredRepos.isEmpty() && filter.isBlank()) {
            Text(
                text = "No repositories found. Create one above to get started.",
                style = MaterialTheme.typography.bodySmall,
                color = Color.Gray,
                modifier = Modifier.padding(vertical = 24.dp)
            )
        } else {
            LazyColumn(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                items(filteredRepos) { repo ->
                    TacticalCard(
                        modifier = Modifier.clickable {
                            viewModel.selectRepo(repo.full_name)
                            onRepoSelected()
                        }
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    text = repo.name,
                                    style = MaterialTheme.typography.titleSmall,
                                    fontWeight = FontWeight.Bold
                                )
                                Text(
                                    text = repo.full_name,
                                    style = MaterialTheme.typography.labelSmall,
                                    color = Color.Gray
                                )
                            }
                            Icon(Icons.Default.ChevronRight, contentDescription = null, tint = Color.Gray)
                        }
                    }
                }
            }
        }
    }
}
