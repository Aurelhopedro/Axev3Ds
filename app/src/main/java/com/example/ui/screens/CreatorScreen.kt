package com.example.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.ArrowForward
import androidx.compose.material.icons.filled.Check
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.example.data.models.CharacterConfig
import com.example.data.models.WorkflowOptions
import com.example.ui.MainViewModel
import com.example.ui.UiState
import com.example.ui.components.SectionHeader
import com.example.ui.components.TacticalButton
import com.example.ui.components.TacticalCard
import com.example.ui.theme.TacticalOrange

// ─── Ecrã principal do criador ────────────────────────────────────────────────

@Composable
fun CreatorScreen(viewModel: MainViewModel) {
    var currentStep by remember { mutableStateOf(1) }
    val character by viewModel.currentCharacter.collectAsState()
    val uiState   by viewModel.uiState.collectAsState()

    Column(modifier = Modifier.fillMaxSize()) {

        StepperHeader(currentStep = currentStep)

        Box(modifier = Modifier.weight(1f).padding(horizontal = 16.dp)) {
            when (currentStep) {
                1 -> IdentityStep(character)   { viewModel.updateCharacter(it) }
                2 -> AppearanceStep(character) { viewModel.updateCharacter(it) }
                3 -> StyleStep(character)      { viewModel.updateCharacter(it) }
                4 -> ClothingStep(character)   { viewModel.updateCharacter(it) }
                5 -> ExportStep(character)     { viewModel.updateCharacter(it) }
                6 -> ReviewStep(character, uiState) { viewModel.generate() }
            }
        }

        NavigationFooter(
            currentStep = currentStep,
            onBack = { if (currentStep > 1) currentStep-- },
            onNext = { if (currentStep < 6) currentStep++ }
        )
    }
}

// ─── Cabeçalho de passos ──────────────────────────────────────────────────────

@Composable
fun StepperHeader(currentStep: Int) {
    Row(
        modifier = Modifier.fillMaxWidth().padding(16.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        repeat(6) { index ->
            val step        = index + 1
            val isActive    = step == currentStep
            val isCompleted = step < currentStep
            Box(
                modifier = Modifier
                    .size(32.dp)
                    .background(
                        when {
                            isActive    -> TacticalOrange
                            isCompleted -> Color.Green
                            else        -> Color.Gray.copy(alpha = 0.3f)
                        },
                        MaterialTheme.shapes.small
                    ),
                contentAlignment = Alignment.Center
            ) {
                if (isCompleted) {
                    Icon(Icons.Default.Check, contentDescription = null,
                        tint = Color.Black, modifier = Modifier.size(16.dp))
                } else {
                    Text(text = step.toString(),
                        color = if (isActive) Color.Black else Color.White,
                        fontWeight = FontWeight.Bold)
                }
            }
            if (index < 5) {
                Box(modifier = Modifier.weight(1f).height(2.dp)
                    .background(Color.Gray.copy(alpha = 0.3f)))
            }
        }
    }
}

// ─── Utilitário: dropdown genérico ────────────────────────────────────────────

@Composable
fun WorkflowDropdown(
    label: String,
    selected: String,
    options: List<String>,
    onSelect: (String) -> Unit
) {
    var expanded by remember { mutableStateOf(false) }
    Column(modifier = Modifier.fillMaxWidth().padding(vertical = 8.dp)) {
        Text(label, style = MaterialTheme.typography.labelSmall)
        Spacer(modifier = Modifier.height(4.dp))
        Box {
            OutlinedButton(
                onClick   = { expanded = true },
                modifier  = Modifier.fillMaxWidth()
            ) {
                Text(selected, maxLines = 1)
            }
            DropdownMenu(expanded = expanded, onDismissRequest = { expanded = false }) {
                options.forEach { opt ->
                    DropdownMenuItem(
                        text    = { Text(opt) },
                        onClick = { onSelect(opt); expanded = false }
                    )
                }
            }
        }
    }
}

// ─── Passo 1 — Identidade ─────────────────────────────────────────────────────

@Composable
fun IdentityStep(config: CharacterConfig, onUpdate: (CharacterConfig) -> Unit) {
    Column(modifier = Modifier.verticalScroll(rememberScrollState())) {
        SectionHeader(title = "Character Identity")
        TacticalCard {
            Text("NAME", style = MaterialTheme.typography.labelSmall)
            OutlinedTextField(
                value         = config.name,
                onValueChange = { onUpdate(config.copy(name = it)) },
                modifier      = Modifier.fillMaxWidth()
            )
            WorkflowDropdown(
                label    = "PRESET",
                selected = config.preset,
                options  = WorkflowOptions.PRESETS
            ) { onUpdate(config.copy(preset = it)) }

            WorkflowDropdown(
                label    = "GENDER & AGE",
                selected = config.genderAge,
                options  = WorkflowOptions.GENDER_AGE
            ) { onUpdate(config.copy(genderAge = it)) }
        }
    }
}

// ─── Passo 2 — Aparência ──────────────────────────────────────────────────────

@Composable
fun AppearanceStep(config: CharacterConfig, onUpdate: (CharacterConfig) -> Unit) {
    Column(modifier = Modifier.verticalScroll(rememberScrollState())) {
        SectionHeader(title = "Physical Traits")
        TacticalCard {
            WorkflowDropdown(
                label    = "BODY (muscle:weight:height)",
                selected = config.body,
                options  = WorkflowOptions.BODY
            ) { onUpdate(config.copy(body = it)) }

            WorkflowDropdown(
                label    = "SKIN TONE",
                selected = config.skinTone,
                options  = WorkflowOptions.SKIN_TONE
            ) { onUpdate(config.copy(skinTone = it)) }

            WorkflowDropdown(
                label    = "HAIR (style:colour)",
                selected = config.hair,
                options  = WorkflowOptions.HAIR
            ) { onUpdate(config.copy(hair = it)) }

            WorkflowDropdown(
                label    = "FACIAL HAIR",
                selected = config.facialHair,
                options  = WorkflowOptions.FACIAL_HAIR
            ) { onUpdate(config.copy(facialHair = it)) }

            WorkflowDropdown(
                label    = "EYE COLOUR",
                selected = config.eyes,
                options  = WorkflowOptions.EYES
            ) { onUpdate(config.copy(eyes = it)) }
        }
    }
}

// ─── Passo 3 — Estilo Visual ──────────────────────────────────────────────────

@Composable
fun StyleStep(config: CharacterConfig, onUpdate: (CharacterConfig) -> Unit) {
    Column(modifier = Modifier.verticalScroll(rememberScrollState())) {
        SectionHeader(title = "Visual Direction")
        TacticalCard {
            WorkflowDropdown(
                label    = "EXPRESSION",
                selected = config.expression,
                options  = WorkflowOptions.EXPRESSION
            ) { onUpdate(config.copy(expression = it)) }

            WorkflowDropdown(
                label    = "RIG & ANIMATIONS (rig:anim:pose)",
                selected = config.rigAnim,
                options  = WorkflowOptions.RIG_ANIM
            ) { onUpdate(config.copy(rigAnim = it)) }

            WorkflowDropdown(
                label    = "QUALITY",
                selected = config.quality,
                options  = WorkflowOptions.QUALITY
            ) { onUpdate(config.copy(quality = it)) }
        }
    }
}

// ─── Passo 4 — Vestuário & Equipamento ───────────────────────────────────────

@Composable
fun ClothingStep(config: CharacterConfig, onUpdate: (CharacterConfig) -> Unit) {
    Column(modifier = Modifier.verticalScroll(rememberScrollState())) {
        SectionHeader(title = "Uniform & Gear")
        TacticalCard {
            WorkflowDropdown(
                label    = "OUTFIT",
                selected = config.outfit,
                options  = WorkflowOptions.OUTFIT
            ) { onUpdate(config.copy(outfit = it)) }

            WorkflowDropdown(
                label    = "SHOES : HAT",
                selected = config.shoesHat,
                options  = WorkflowOptions.SHOES_HAT
            ) { onUpdate(config.copy(shoesHat = it)) }

            WorkflowDropdown(
                label    = "WEAPON",
                selected = config.weapon,
                options  = WorkflowOptions.WEAPON
            ) { onUpdate(config.copy(weapon = it)) }
        }
    }
}

// ─── Passo 5 — Exportação ─────────────────────────────────────────────────────

@Composable
fun ExportStep(config: CharacterConfig, onUpdate: (CharacterConfig) -> Unit) {
    Column(modifier = Modifier.verticalScroll(rememberScrollState())) {
        SectionHeader(title = "Export Center")
        TacticalCard {
            WorkflowDropdown(
                label    = "EXPORT FORMAT(S)",
                selected = config.exportFormats,
                options  = WorkflowOptions.EXPORT_FORMATS
            ) { onUpdate(config.copy(exportFormats = it)) }
        }
    }
}

// ─── Passo 6 — Revisão e Envio ────────────────────────────────────────────────

@Composable
fun ReviewStep(
    config: CharacterConfig,
    uiState: UiState,
    onDispatch: () -> Unit
) {
    Column(modifier = Modifier.verticalScroll(rememberScrollState())) {
        SectionHeader(title = "Final Review")
        TacticalCard {
            ReviewRow("NAME",    config.name)
            ReviewRow("PRESET",  config.preset)
            ReviewRow("GENDER/AGE", config.genderAge)
            ReviewRow("BODY",    config.body)
            ReviewRow("SKIN",    config.skinTone)
            ReviewRow("HAIR",    config.hair)
            ReviewRow("BEARD",   config.facialHair)
            ReviewRow("EYES",    config.eyes)
            ReviewRow("OUTFIT",  config.outfit)
            ReviewRow("SHOES/HAT", config.shoesHat)
            ReviewRow("WEAPON",  config.weapon)
            ReviewRow("EXPRESSION", config.expression)
            ReviewRow("RIG/ANIM",  config.rigAnim)
            ReviewRow("QUALITY", config.quality)
            ReviewRow("FORMATS", config.exportFormats)

            Spacer(modifier = Modifier.height(16.dp))

            when (uiState) {
                is UiState.Loading -> {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        CircularProgressIndicator(modifier = Modifier.size(20.dp), strokeWidth = 2.dp)
                        Spacer(modifier = Modifier.width(12.dp))
                        Text("Dispatching workflow…")
                    }
                }
                is UiState.Success -> {
                    Text("✅ ${uiState.message}",
                        color = Color.Green, fontWeight = FontWeight.Bold)
                    Spacer(modifier = Modifier.height(8.dp))
                    TacticalButton(text = "Generate Again", onClick = onDispatch)
                }
                is UiState.Error -> {
                    Text("❌ ${uiState.message}",
                        color = MaterialTheme.colorScheme.error)
                    Spacer(modifier = Modifier.height(8.dp))
                    TacticalButton(text = "Retry", onClick = onDispatch)
                }
                else -> {
                    TacticalButton(text = "⚡ Initialize Generation", onClick = onDispatch)
                }
            }
        }
    }
}

@Composable
private fun ReviewRow(label: String, value: String) {
    Row(
        modifier = Modifier.fillMaxWidth().padding(vertical = 2.dp),
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Text(label, style = MaterialTheme.typography.labelSmall,
            color = Color.Gray, modifier = Modifier.weight(1f))
        Text(value, style = MaterialTheme.typography.bodySmall,
            fontWeight = FontWeight.Medium, modifier = Modifier.weight(2f))
    }
}

// ─── Rodapé de navegação ──────────────────────────────────────────────────────

@Composable
fun NavigationFooter(currentStep: Int, onBack: () -> Unit, onNext: () -> Unit) {
    Row(
        modifier = Modifier.fillMaxWidth().padding(16.dp),
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        if (currentStep > 1) {
            OutlinedButton(onClick = onBack) {
                Icon(Icons.Default.ArrowBack, contentDescription = null)
                Spacer(modifier = Modifier.width(8.dp))
                Text("BACK")
            }
        } else {
            Spacer(modifier = Modifier.width(1.dp))
        }

        if (currentStep < 6) {
            Button(
                onClick = onNext,
                colors  = ButtonDefaults.buttonColors(containerColor = TacticalOrange)
            ) {
                Text("NEXT")
                Spacer(modifier = Modifier.width(8.dp))
                Icon(Icons.Default.ArrowForward, contentDescription = null)
            }
        }
    }
}
