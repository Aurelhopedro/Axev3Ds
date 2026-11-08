package com.example.ui.screens

import android.content.Intent
import android.net.Uri
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.ContentCopy
import androidx.compose.material.icons.filled.OpenInBrowser
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalClipboardManager
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.R
import com.example.ui.AuthState
import com.example.ui.MainViewModel
import com.example.ui.UiState
import com.example.ui.components.TacticalButton
import com.example.ui.components.TacticalCard

@Composable
fun LoginScreen(viewModel: MainViewModel) {
    val authState by viewModel.authState.collectAsState()
    val uiState by viewModel.uiState.collectAsState()
    val context = LocalContext.current
    val clipboardManager = LocalClipboardManager.current

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
            contentDescription = "AXEV 3D STUDIO",
            modifier = Modifier.size(100.dp)
        )
        Spacer(modifier = Modifier.height(16.dp))
        Text(
            text = "AXEV 3D STUDIO",
            style = MaterialTheme.typography.headlineSmall.copy(fontWeight = FontWeight.Black),
            letterSpacing = 2.sp
        )
        Text(
            text = "Character Generation Platform",
            style = MaterialTheme.typography.bodySmall,
            color = Color.Gray
        )

        Spacer(modifier = Modifier.height(48.dp))

        // ── Estados da autenticação ───────────────────────────────────────────

        when (val state = authState) {

            is AuthState.Idle -> {
                // Botão principal "Continue with GitHub"
                GitHubLoginButton(
                    onClick = { viewModel.startGitHubLogin() },
                    enabled = true
                )
            }

            is AuthState.Loading -> {
                TacticalCard {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.Center,
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        CircularProgressIndicator(
                            modifier = Modifier.size(20.dp),
                            strokeWidth = 2.dp,
                            color = MaterialTheme.colorScheme.primary
                        )
                        Spacer(modifier = Modifier.width(12.dp))
                        Text("A contactar o GitHub...")
                    }
                }
            }

            is AuthState.WaitingForUser -> {
                // Mostrar código ao utilizador
                DeviceCodeCard(
                    userCode = state.userCode,
                    verificationUri = state.verificationUri,
                    onCopyCode = {
                        clipboardManager.setText(AnnotatedString(state.userCode))
                    },
                    onOpenBrowser = {
                        val intent = Intent(Intent.ACTION_VIEW, Uri.parse(state.verificationUri))
                        context.startActivity(intent)
                    }
                )
            }

            is AuthState.Polling -> {
                TacticalCard {
                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally,
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        CircularProgressIndicator(color = MaterialTheme.colorScheme.primary)
                        Spacer(modifier = Modifier.height(12.dp))
                        Text(
                            text = "A aguardar autorização no GitHub...",
                            style = MaterialTheme.typography.bodyMedium,
                            textAlign = TextAlign.Center
                        )
                        Text(
                            text = "Autoriza no browser e volta aqui",
                            style = MaterialTheme.typography.labelSmall,
                            color = Color.Gray,
                            textAlign = TextAlign.Center
                        )
                    }
                }
                Spacer(modifier = Modifier.height(16.dp))
                TextButton(onClick = { viewModel.resetAuthState() }) {
                    Text("Cancelar")
                }
            }

            is AuthState.Authenticated -> {
                TacticalCard {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Icon(
                            Icons.Default.CheckCircle,
                            contentDescription = null,
                            tint = Color(0xFF4CAF50),
                            modifier = Modifier.size(32.dp)
                        )
                        Spacer(modifier = Modifier.width(12.dp))
                        Text(
                            text = "Autenticado com sucesso!",
                            fontWeight = FontWeight.Bold
                        )
                    }
                }
            }

            is AuthState.Error -> {
                TacticalCard {
                    Column {
                        Text(
                            text = "⚠ Erro de autenticação",
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.error
                        )
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(
                            text = state.message,
                            style = MaterialTheme.typography.bodySmall,
                            color = Color.Gray
                        )
                    }
                }
                Spacer(modifier = Modifier.height(16.dp))
                GitHubLoginButton(
                    onClick = { viewModel.startGitHubLogin() },
                    enabled = true,
                    label = "Tentar novamente"
                )
            }
        }

        // Erros gerais de UiState
        if (uiState is UiState.Error) {
            Spacer(modifier = Modifier.height(8.dp))
            Text(
                text = (uiState as UiState.Error).message,
                color = MaterialTheme.colorScheme.error,
                style = MaterialTheme.typography.bodySmall,
                textAlign = TextAlign.Center
            )
        }

        Spacer(modifier = Modifier.height(32.dp))
    }
}

@Composable
fun GitHubLoginButton(
    onClick: () -> Unit,
    enabled: Boolean,
    label: String = "Continue with GitHub"
) {
    Button(
        onClick = onClick,
        enabled = enabled,
        modifier = Modifier
            .fillMaxWidth()
            .height(52.dp),
        shape = RoundedCornerShape(8.dp),
        colors = ButtonDefaults.buttonColors(
            containerColor = Color(0xFF24292E),
            contentColor = Color.White
        )
    ) {
        // Ícone do GitHub (octicons/mark-github)
        Icon(
            painter = painterResource(id = R.drawable.ic_github),
            contentDescription = "GitHub",
            modifier = Modifier.size(22.dp),
            tint = Color.White
        )
        Spacer(modifier = Modifier.width(12.dp))
        Text(
            text = label,
            fontWeight = FontWeight.SemiBold,
            fontSize = 15.sp
        )
    }
}

@Composable
fun DeviceCodeCard(
    userCode: String,
    verificationUri: String,
    onCopyCode: () -> Unit,
    onOpenBrowser: () -> Unit
) {
    TacticalCard {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            modifier = Modifier.fillMaxWidth()
        ) {
            Text(
                text = "AUTORIZA NO GITHUB",
                style = MaterialTheme.typography.labelLarge,
                fontWeight = FontWeight.Bold,
                letterSpacing = 1.sp
            )
            Spacer(modifier = Modifier.height(16.dp))

            // Código de verificação
            Text(
                text = "O teu código de acesso:",
                style = MaterialTheme.typography.bodySmall,
                color = Color.Gray
            )
            Spacer(modifier = Modifier.height(8.dp))
            Box(
                modifier = Modifier
                    .background(Color(0xFF1A1A2E), RoundedCornerShape(8.dp))
                    .border(1.dp, MaterialTheme.colorScheme.primary, RoundedCornerShape(8.dp))
                    .padding(horizontal = 24.dp, vertical = 12.dp)
            ) {
                Text(
                    text = userCode,
                    fontFamily = FontFamily.Monospace,
                    fontSize = 28.sp,
                    fontWeight = FontWeight.Black,
                    color = MaterialTheme.colorScheme.primary,
                    letterSpacing = 4.sp
                )
            }

            Spacer(modifier = Modifier.height(16.dp))

            // Instruções
            Text(
                text = "1. Vai a ${verificationUri}",
                style = MaterialTheme.typography.bodySmall,
                textAlign = TextAlign.Center
            )
            Text(
                text = "2. Introduz o código acima",
                style = MaterialTheme.typography.bodySmall,
                textAlign = TextAlign.Center
            )
            Text(
                text = "3. Autoriza o AXEV 3D STUDIO",
                style = MaterialTheme.typography.bodySmall,
                textAlign = TextAlign.Center
            )

            Spacer(modifier = Modifier.height(20.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                OutlinedButton(
                    onClick = onCopyCode,
                    modifier = Modifier.weight(1f)
                ) {
                    Icon(Icons.Default.ContentCopy, contentDescription = null, modifier = Modifier.size(16.dp))
                    Spacer(modifier = Modifier.width(4.dp))
                    Text("Copiar código")
                }
                Button(
                    onClick = onOpenBrowser,
                    modifier = Modifier.weight(1f),
                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF24292E))
                ) {
                    Icon(Icons.Default.OpenInBrowser, contentDescription = null, modifier = Modifier.size(16.dp))
                    Spacer(modifier = Modifier.width(4.dp))
                    Text("Abrir browser")
                }
            }
        }
    }
}
