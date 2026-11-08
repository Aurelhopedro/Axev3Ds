package com.example.ui.screens

import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.unit.dp
import com.example.R
import com.example.ui.MainViewModel
import com.example.ui.UiState
import com.example.ui.components.TacticalButton
import com.example.ui.components.TacticalCard

@Composable
fun LoginScreen(viewModel: MainViewModel) {
    var token by remember { mutableStateOf("") }
    val uiState by viewModel.uiState.collectAsState()

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(24.dp)
            .verticalScroll(rememberScrollState()),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Image(
            painter = painterResource(id = R.drawable.ic_app_foreground_asset),
            contentDescription = "AXEV Logo",
            modifier = Modifier.size(120.dp)
        )

        Spacer(modifier = Modifier.height(32.dp))

        Text(
            text = "GITHUB CONNECTION",
            style = MaterialTheme.typography.headlineMedium.copy(fontWeight = FontWeight.Bold)
        )
        Text(
            text = "Connect your account to start generating characters",
            style = MaterialTheme.typography.bodyMedium,
            color = Color.Gray
        )

        Spacer(modifier = Modifier.height(48.dp))

        TacticalCard {
            Text(
                text = "PERSONAL ACCESS TOKEN",
                style = MaterialTheme.typography.labelLarge,
                fontWeight = FontWeight.Bold
            )
            Spacer(modifier = Modifier.height(8.dp))
            OutlinedTextField(
                value = token,
                onValueChange = { token = it },
                modifier = Modifier.fillMaxWidth(),
                placeholder = { Text("ghp_xxxxxxxxxxxx") },
                visualTransformation = PasswordVisualTransformation(),
                leadingIcon = { Icon(Icons.Default.Lock, contentDescription = null) },
                colors = OutlinedTextFieldDefaults.colors(
                    focusedBorderColor = MaterialTheme.colorScheme.primary,
                    unfocusedBorderColor = Color.Gray
                )
            )
            Spacer(modifier = Modifier.height(16.dp))
            Text(
                text = "Ensure your token has 'repo' and 'workflow' scopes.",
                style = MaterialTheme.typography.labelSmall,
                color = Color.Gray
            )
        }

        Spacer(modifier = Modifier.height(24.dp))

        if (uiState is UiState.Error) {
            Text(
                text = (uiState as UiState.Error).message,
                color = MaterialTheme.colorScheme.error,
                style = MaterialTheme.typography.bodySmall
            )
            Spacer(modifier = Modifier.height(8.dp))
        }

        TacticalButton(
            text = if (uiState is UiState.Loading) "Authenticating..." else "Initialize Studio",
            onClick = { viewModel.login(token) },
            enabled = token.isNotEmpty() && uiState !is UiState.Loading
        )

        Spacer(modifier = Modifier.height(16.dp))

        OutlinedButton(
            onClick = { viewModel.login("123456789") },
            modifier = Modifier.fillMaxWidth(),
            shape = MaterialTheme.shapes.small
        ) {
            Text("TRY DEMO MODE (TEST TOKEN)")
        }

        Spacer(modifier = Modifier.height(32.dp))

        TextButton(onClick = { /* Open GitHub help */ }) {
            Text("How to create a PAT?", color = MaterialTheme.colorScheme.primary)
        }
    }
}
