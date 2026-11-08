package com.example.ui.screens

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ContentCopy
import androidx.compose.material.icons.filled.Info
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.example.ui.components.SectionHeader
import com.example.ui.components.TacticalCard

@Composable
fun SetupScreen() {
    val context = LocalContext.current
    val workflowContent = remember {
        try {
            context.assets.open("github/ara-axev-crime.yml").bufferedReader().use { it.readText() }
        } catch (e: Exception) {
            "Workflow file not found in assets."
        }
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp)
            .verticalScroll(rememberScrollState())
    ) {
        SectionHeader(title = "MANUAL WORKFLOW SETUP")
        
        TacticalCard {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(Icons.Default.Info, contentDescription = null, tint = Color.Yellow)
                Spacer(modifier = Modifier.width(12.dp))
                Text(
                    text = "ACTION REQUIRED",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    color = Color.Yellow
                )
            }
            Spacer(modifier = Modifier.height(8.dp))
            Text(
                text = "Due to GitHub permission limitations, the automation workflow could not be synced automatically. You must add it manually to your repository to enable 3D generation.",
                style = MaterialTheme.typography.bodySmall,
                color = Color.White
            )
        }

        Spacer(modifier = Modifier.height(24.dp))

        Text(
            text = "INSTRUCTIONS:",
            style = MaterialTheme.typography.labelLarge,
            fontWeight = FontWeight.Black,
            color = Color.Gray
        )
        
        Spacer(modifier = Modifier.height(8.dp))
        
        Text(
            text = "1. Go to your GitHub repository.\n" +
                   "2. Create a folder named '.github/workflows' if it doesn't exist.\n" +
                   "3. Create a new file named 'ara-axev-crime.yml'.\n" +
                   "4. Paste the content below into that file.\n" +
                   "5. Commit the changes.",
            style = MaterialTheme.typography.bodyMedium
        )

        Spacer(modifier = Modifier.height(24.dp))

        SectionHeader(title = "WORKFLOW CONTENT")
        
        TacticalCard {
            Box(modifier = Modifier.fillMaxWidth()) {
                Text(
                    text = workflowContent,
                    style = MaterialTheme.typography.bodySmall,
                    color = Color.LightGray,
                    modifier = Modifier.padding(bottom = 48.dp)
                )
                
                // Note: Real clipboard copy would go here
                Button(
                    onClick = { /* Simulated Copy */ },
                    modifier = Modifier.align(Alignment.BottomEnd),
                    colors = ButtonDefaults.buttonColors(containerColor = Color.DarkGray)
                ) {
                    Icon(Icons.Default.ContentCopy, contentDescription = null)
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("COPY")
                }
            }
        }
    }
}
