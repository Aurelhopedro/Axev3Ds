package com.example.ui.screens

import android.content.Intent
import android.net.Uri
import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.R
import com.example.ui.AuthState
import com.example.ui.MainViewModel
import com.example.ui.components.TacticalCard

@Composable
fun LoginScreen(viewModel: MainViewModel) {
    val authState by viewModel.authState.collectAsState()
    val context = LocalContext.current

    // Quando o estado passa a OpenBrowser, abrimos o browser e passamos para WaitingForCallback
    LaunchedEffect(authState) {
        if (authState is AuthState.OpenBrowser) {
            val url = (authState as AuthState.OpenBrowser).authUrl
            val intent = Intent(Intent.ACTION_VIEW, Uri.parse(url))
            context.startActivity(intent)
            viewModel.onBrowserOpened()
        }
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(24.dp)
            .verticalScroll(rememberScrollState()),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        // ── Logo ──────────────────────────────────────────────────────────────
        Image(
            painter = painterResource(id = R.drawable.ic_app_foreground_asset),
            contentDescription = "AXEV 3D Studio Logo",
            modifier = Modifier.size(100.dp)
        )
        Spacer(modifier = Modifier.height(12.dp))
        Text(
            text = "AXEV 3D STUDIO",
            style = MaterialTheme.typography.headlineMedium.copy(fontWeight = FontWeight.Bold)
        )
        Text(
            text = "Sign in to start generating characters",
            style = MaterialTheme.typography.bodySmall,
            color = Color.Gray,
            textAlign = TextAlign.Center
        )
        Spacer(modifier = Modifier.height(40.dp))

        // ── Estado ────────────────────────────────────────────────────────────
        when (val state = authState) {

            is AuthState.Idle,
            is AuthState.OpenBrowser,
            is AuthState.Failed -> {
                GitHubSignInButton(
                    isLoading = false,
                    onClick = { viewModel.startGitHubAuth() }
                )
                if (state is AuthState.Failed) {
                    Spacer(modifier = Modifier.height(16.dp))
                    TacticalCard {
                        Text(
                            text = state.message,
                            color = MaterialTheme.colorScheme.error,
                            style = MaterialTheme.typography.bodySmall,
                            textAlign = TextAlign.Center,
                            modifier = Modifier.fillMaxWidth()
                        )
                        Spacer(modifier = Modifier.height(8.dp))
                        TextButton(
                            onClick = { viewModel.startGitHubAuth() },
                            modifier = Modifier.align(Alignment.CenterHorizontally)
                        ) {
                            Icon(Icons.Default.Refresh, contentDescription = null)
                            Spacer(modifier = Modifier.width(4.dp))
                            Text("Try Again")
                        }
                    }
                }
                Spacer(modifier = Modifier.height(32.dp))
                Divider(color = Color.DarkGray)
                Spacer(modifier = Modifier.height(16.dp))
                Text(
                    text = "FOR TESTING ONLY",
                    style = MaterialTheme.typography.labelSmall,
                    color = Color.Gray,
                    textAlign = TextAlign.Center
                )
                Spacer(modifier = Modifier.height(8.dp))
                OutlinedButton(
                    onClick = { viewModel.login("123456789") },
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(6.dp),
                    colors = ButtonDefaults.outlinedButtonColors(contentColor = Color.Gray)
                ) {
                    Text("Demo Mode (simulated account)", style = MaterialTheme.typography.labelSmall)
                }
            }

            is AuthState.WaitingForCallback -> {
                TacticalCard {
                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally,
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        CircularProgressIndicator(color = MaterialTheme.colorScheme.primary)
                        Spacer(modifier = Modifier.height(16.dp))
                        Text(
                            text = "Authorize in the browser",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold
                        )
                        Spacer(modifier = Modifier.height(8.dp))
                        Text(
                            text = "After clicking \"Authorize\" on GitHub,\nyou'll be automatically redirected back.",
                            style = MaterialTheme.typography.bodySmall,
                            color = Color.LightGray,
                            textAlign = TextAlign.Center
                        )
                        Spacer(modifier = Modifier.height(16.dp))
                        TextButton(onClick = { viewModel.cancelAuth() }) {
                            Text("Cancel", color = Color.Gray)
                        }
                    }
                }
            }

            is AuthState.Authenticated -> {
                CircularProgressIndicator()
                Text("Authenticated. Loading...", color = Color.Gray, style = MaterialTheme.typography.bodySmall)
            }

            // ── Device Flow (legado — mantido caso seja necessário) ────────────
            is AuthState.RequestingCode -> {
                GitHubSignInButton(isLoading = true, onClick = {})
                Spacer(modifier = Modifier.height(16.dp))
                Text("Requesting device code...", color = Color.Gray, style = MaterialTheme.typography.bodySmall)
            }
            is AuthState.Polling -> {
                TacticalCard {
                    Column(horizontalAlignment = Alignment.CenterHorizontally, modifier = Modifier.fillMaxWidth()) {
                        CircularProgressIndicator(color = MaterialTheme.colorScheme.primary)
                        Spacer(modifier = Modifier.height(12.dp))
                        Text("Waiting for GitHub authorization...", style = MaterialTheme.typography.bodyMedium)
                        Spacer(modifier = Modifier.height(8.dp))
                        TextButton(onClick = { viewModel.cancelAuth() }) {
                            Text("Cancel", color = Color.Gray)
                        }
                    }
                }
            }
            else -> { /* WaitingForUser do Device Flow — não usado no Web Flow */ }
        }
    }
}

@Composable
private fun GitHubSignInButton(isLoading: Boolean, onClick: () -> Unit) {
    Button(
        onClick = onClick,
        enabled = !isLoading,
        modifier = Modifier
            .fillMaxWidth()
            .height(52.dp),
        shape = RoundedCornerShape(8.dp),
        colors = ButtonDefaults.buttonColors(
            containerColor = Color(0xFF24292E),
            contentColor = Color.White,
            disabledContainerColor = Color(0xFF444444)
        )
    ) {
        if (isLoading) {
            CircularProgressIndicator(
                modifier = Modifier.size(20.dp),
                color = Color.White,
                strokeWidth = 2.dp
            )
            Spacer(modifier = Modifier.width(12.dp))
            Text("Connecting...", fontWeight = FontWeight.SemiBold)
        } else {
            Text("⬛", fontSize = 18.sp)
            Spacer(modifier = Modifier.width(10.dp))
            Text("Continue with GitHub", fontWeight = FontWeight.SemiBold, fontSize = 16.sp)
        }
    }
}
