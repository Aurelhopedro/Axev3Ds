package com.example.ui.screens

import android.content.Intent
import android.net.Uri
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.AutoAwesome
import androidx.compose.material.icons.outlined.Refresh
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Brush
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
import com.example.ui.theme.*

@Composable
fun LoginScreen(viewModel: MainViewModel) {
    val authState by viewModel.authState.collectAsState()
    val context = LocalContext.current

    LaunchedEffect(authState) {
        if (authState is AuthState.OpenBrowser) {
            val url = (authState as AuthState.OpenBrowser).authUrl
            val intent = Intent(Intent.ACTION_VIEW, Uri.parse(url))
            context.startActivity(intent)
            viewModel.onBrowserOpened()
        }
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(AppBackground)
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(24.dp)
                .verticalScroll(rememberScrollState()),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            Box(
                modifier = Modifier
                    .size(88.dp)
                    .shadow(16.dp, CircleShape, ambientColor = PrimaryGlow, spotColor = PrimaryGlow)
                    .background(Brush.linearGradient(listOf(PrimaryOrange, PrimaryOrangeEnd)), CircleShape),
                contentAlignment = Alignment.Center
            ) {
                Image(
                    painter = painterResource(id = R.drawable.ic_app_foreground_asset),
                    contentDescription = "AXEV 3D Studio",
                    modifier = Modifier.size(56.dp)
                )
            }
            Spacer(modifier = Modifier.height(20.dp))
            Text(
                text = "AXEV 3D Studio",
                style = MaterialTheme.typography.headlineSmall.copy(fontWeight = FontWeight.Bold),
                color = TextPrimary
            )
            Spacer(modifier = Modifier.height(6.dp))
            Text(
                text = "Entre para criar e acompanhar seus personagens em 3D",
                style = MaterialTheme.typography.bodyMedium,
                color = TextSecondary,
                textAlign = TextAlign.Center
            )
            Spacer(modifier = Modifier.height(36.dp))

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
                                color = SoftError,
                                style = MaterialTheme.typography.bodySmall,
                                textAlign = TextAlign.Center,
                                modifier = Modifier.fillMaxWidth()
                            )
                            Spacer(modifier = Modifier.height(8.dp))
                            TextButton(
                                onClick = { viewModel.startGitHubAuth() },
                                modifier = Modifier.align(Alignment.CenterHorizontally)
                            ) {
                                Icon(Icons.Outlined.Refresh, contentDescription = null, tint = PrimaryOrange)
                                Spacer(modifier = Modifier.width(4.dp))
                                Text("Tentar novamente", color = PrimaryOrange)
                            }
                        }
                    }
                    Spacer(modifier = Modifier.height(28.dp))
                    HorizontalDivider(color = BorderSoft)
                    Spacer(modifier = Modifier.height(16.dp))
                    Text(
                        text = "Modo de demonstração",
                        style = MaterialTheme.typography.labelMedium,
                        color = TextMuted,
                        textAlign = TextAlign.Center
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    OutlinedButton(
                        onClick = { viewModel.login("123456789") },
                        modifier = Modifier.fillMaxWidth().height(48.dp),
                        shape = RoundedCornerShape(14.dp),
                        colors = ButtonDefaults.outlinedButtonColors(contentColor = TextSecondary),
                        border = androidx.compose.foundation.BorderStroke(1.dp, BorderSoft)
                    ) {
                        Icon(Icons.Outlined.AutoAwesome, contentDescription = null, modifier = Modifier.size(16.dp))
                        Spacer(modifier = Modifier.width(8.dp))
                        Text("Entrar no modo demo", style = MaterialTheme.typography.labelLarge)
                    }
                }

                is AuthState.WaitingForCallback -> {
                    TacticalCard {
                        Column(
                            horizontalAlignment = Alignment.CenterHorizontally,
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            CircularProgressIndicator(color = PrimaryOrange)
                            Spacer(modifier = Modifier.height(16.dp))
                            Text(
                                text = "Aguardando autorização",
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.SemiBold,
                                color = TextPrimary
                            )
                            Spacer(modifier = Modifier.height(8.dp))
                            Text(
                                text = "Conclua a autorização no navegador. Você será redirecionado automaticamente.",
                                style = MaterialTheme.typography.bodySmall,
                                color = TextSecondary,
                                textAlign = TextAlign.Center
                            )
                            Spacer(modifier = Modifier.height(16.dp))
                            TextButton(onClick = { viewModel.cancelAuth() }) {
                                Text("Cancelar", color = TextSecondary)
                            }
                        }
                    }
                }

                is AuthState.Authenticated -> {
                    CircularProgressIndicator(color = PrimaryOrange)
                    Spacer(modifier = Modifier.height(12.dp))
                    Text("Autenticado. Carregando...", color = TextSecondary, style = MaterialTheme.typography.bodySmall)
                }

                is AuthState.RequestingCode -> {
                    GitHubSignInButton(isLoading = true, onClick = {})
                    Spacer(modifier = Modifier.height(16.dp))
                    Text("Solicitando código...", color = TextSecondary, style = MaterialTheme.typography.bodySmall)
                }
                is AuthState.Polling -> {
                    TacticalCard {
                        Column(horizontalAlignment = Alignment.CenterHorizontally, modifier = Modifier.fillMaxWidth()) {
                            CircularProgressIndicator(color = PrimaryOrange)
                            Spacer(modifier = Modifier.height(12.dp))
                            Text("Aguardando autorização do GitHub...", style = MaterialTheme.typography.bodyMedium, color = TextPrimary)
                            Spacer(modifier = Modifier.height(8.dp))
                            TextButton(onClick = { viewModel.cancelAuth() }) {
                                Text("Cancelar", color = TextSecondary)
                            }
                        }
                    }
                }
                else -> {}
            }
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
            .height(54.dp)
            .shadow(10.dp, RoundedCornerShape(16.dp), ambientColor = Color.Black.copy(0.2f), spotColor = Color.Black.copy(0.2f)),
        shape = RoundedCornerShape(16.dp),
        colors = ButtonDefaults.buttonColors(
            containerColor = CardSurfaceElevated,
            contentColor = TextPrimary,
            disabledContainerColor = CardSurface
        )
    ) {
        if (isLoading) {
            CircularProgressIndicator(
                modifier = Modifier.size(20.dp),
                color = TextPrimary,
                strokeWidth = 2.dp
            )
            Spacer(modifier = Modifier.width(12.dp))
            Text("Conectando...", fontWeight = FontWeight.SemiBold)
        } else {
            Box(
                modifier = Modifier.size(22.dp).background(TextPrimary, RoundedCornerShape(6.dp)),
                contentAlignment = Alignment.Center
            ) {
                Text("GH", fontSize = 9.sp, fontWeight = FontWeight.Bold, color = AppBackground)
            }
            Spacer(modifier = Modifier.width(10.dp))
            Text("Continuar com GitHub", fontWeight = FontWeight.SemiBold, fontSize = 16.sp)
        }
    }
}
