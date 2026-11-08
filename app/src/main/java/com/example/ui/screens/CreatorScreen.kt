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
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.example.data.models.CharacterConfig
import com.example.ui.CharacterTranslations
import com.example.ui.MainViewModel
import com.example.ui.UiState
import com.example.ui.components.SectionHeader
import com.example.ui.components.TacticalButton
import com.example.ui.components.TacticalCard
import com.example.ui.theme.TacticalOrange

// ─── Ecrã principal do criador ────────────────────────────────────────────────

@Composable
fun CreatorScreen(
    viewModel: MainViewModel,
    onGenerate: () -> Unit = {},
    onBack: () -> Unit = {}
) {
    var currentStep by remember { mutableStateOf(1) }
    val character by viewModel.currentCharacter.collectAsState()
    val uiState   by viewModel.uiState.collectAsState()
    val opts      by viewModel.workflowOptions.collectAsState()

    // Traduções baseadas na língua do dispositivo
    val context = LocalContext.current
    val t = remember(context) { CharacterTranslations.get(context) }

    Column(modifier = Modifier.fillMaxSize()) {

        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 8.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            IconButton(onClick = {
                if (currentStep > 1) currentStep-- else onBack()
            }) {
                Icon(Icons.Default.ArrowBack, contentDescription = "Back", tint = Color.White)
            }
            Text(
                text = "Create Character",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold,
                color = Color.White
            )
        }

        StepperHeader(currentStep = currentStep)

        Box(modifier = Modifier.weight(1f).padding(horizontal = 16.dp)) {
            when (currentStep) {
                1 -> IdentityStep(character, opts.presets, opts.genderAge, t)
                        { viewModel.updateCharacter(it) }
                2 -> AppearanceStep(character, opts.body, opts.skinTone,
                        opts.hair, opts.facialHair, opts.eyes, t)
                        { viewModel.updateCharacter(it) }
                3 -> StyleStep(character, opts.expression, opts.rigAnim, opts.quality, t)
                        { viewModel.updateCharacter(it) }
                4 -> ClothingStep(character, opts.outfit, opts.shoesHat, opts.weapon, t)
                        { viewModel.updateCharacter(it) }
                5 -> ExportStep(character, opts.exportFormats, t)
                        { viewModel.updateCharacter(it) }
                6 -> ReviewStep(character, uiState, t, onDispatch = onGenerate)
            }
        }

        NavigationFooter(
            currentStep = currentStep,
            onBack = { if (currentStep > 1) currentStep-- else onBack() },
            onNext = { if (currentStep < 6) currentStep++ }
        )
    }
}

// ─── Cabeçalho de passos ──────────────────────────────────────────────────────

@Composable
fun StepperHeader(currentStep: Int) {
    Row(
        modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 4.dp),
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

// ─── Dropdown traduzido ───────────────────────────────────────────────────────
//
// [enOptions]  — lista de valores EN (vindos do workflow / CharacterConfig)
// [selectedEn] — valor EN actualmente seleccionado
// [t]          — traduções para a língua do dispositivo
// [onSelect]   — devolve SEMPRE o valor EN ao caller (para guardar no config)

@Composable
fun WorkflowDropdown(
    label: String,
    selectedEn: String,
    enOptions: List<String>,
    t: CharacterTranslations.Translations,
    onSelect: (String) -> Unit
) {
    var expanded by remember { mutableStateOf(false) }

    Column(modifier = Modifier.fillMaxWidth().padding(vertical = 8.dp)) {
        Text(label, style = MaterialTheme.typography.labelSmall, color = Color.Gray)
        Spacer(modifier = Modifier.height(4.dp))
        Box {
            OutlinedButton(
                onClick  = { expanded = true },
                modifier = Modifier.fillMaxWidth()
            ) {
                Text(t.label(selectedEn), maxLines = 1)
            }
            DropdownMenu(expanded = expanded, onDismissRequest = { expanded = false }) {
                enOptions.forEach { enValue ->
                    DropdownMenuItem(
                        text    = { Text(t.label(enValue)) },
                        onClick = {
                            onSelect(enValue)   // guarda sempre o valor EN
                            expanded = false
                        }
                    )
                }
            }
        }
    }
}

// ─── Passo 1 — Identidade ─────────────────────────────────────────────────────

@Composable
fun IdentityStep(
    config: CharacterConfig,
    presets: List<String>,
    genderAge: List<String>,
    t: CharacterTranslations.Translations,
    onUpdate: (CharacterConfig) -> Unit
) {
    Column(modifier = Modifier.verticalScroll(rememberScrollState())) {
        SectionHeader(title = "Character Identity")
        TacticalCard {
            Text("NAME", style = MaterialTheme.typography.labelSmall, color = Color.Gray)
            Spacer(modifier = Modifier.height(4.dp))
            OutlinedTextField(
                value         = config.name,
                onValueChange = { onUpdate(config.copy(name = it)) },
                modifier      = Modifier.fillMaxWidth(),
                singleLine    = true,
                placeholder   = { Text("ex: AXEV Soldier") }
            )
            WorkflowDropdown("PRESET", config.preset, presets, t)
                { onUpdate(config.copy(preset = it)) }
            WorkflowDropdown("GENDER & AGE", config.genderAge, genderAge, t)
                { onUpdate(config.copy(genderAge = it)) }
        }
    }
}

// ─── Passo 2 — Aparência ──────────────────────────────────────────────────────

@Composable
fun AppearanceStep(
    config: CharacterConfig,
    body: List<String>,
    skinTone: List<String>,
    hair: List<String>,
    facialHair: List<String>,
    eyes: List<String>,
    t: CharacterTranslations.Translations,
    onUpdate: (CharacterConfig) -> Unit
) {
    Column(modifier = Modifier.verticalScroll(rememberScrollState())) {
        SectionHeader(title = "Physical Traits")
        TacticalCard {
            WorkflowDropdown("BODY", config.body, body, t)
                { onUpdate(config.copy(body = it)) }
            WorkflowDropdown("SKIN TONE", config.skinTone, skinTone, t)
                { onUpdate(config.copy(skinTone = it)) }
            WorkflowDropdown("HAIR", config.hair, hair, t)
                { onUpdate(config.copy(hair = it)) }
            WorkflowDropdown("FACIAL HAIR", config.facialHair, facialHair, t)
                { onUpdate(config.copy(facialHair = it)) }
            WorkflowDropdown("EYE COLOUR", config.eyes, eyes, t)
                { onUpdate(config.copy(eyes = it)) }
        }
    }
}

// ─── Passo 3 — Estilo Visual ──────────────────────────────────────────────────

@Composable
fun StyleStep(
    config: CharacterConfig,
    expression: List<String>,
    rigAnim: List<String>,
    quality: List<String>,
    t: CharacterTranslations.Translations,
    onUpdate: (CharacterConfig) -> Unit
) {
    Column(modifier = Modifier.verticalScroll(rememberScrollState())) {
        SectionHeader(title = "Visual Direction")
        TacticalCard {
            WorkflowDropdown("EXPRESSION", config.expression, expression, t)
                { onUpdate(config.copy(expression = it)) }
            WorkflowDropdown("RIG & ANIMATIONS", config.rigAnim, rigAnim, t)
                { onUpdate(config.copy(rigAnim = it)) }
            WorkflowDropdown("QUALITY", config.quality, quality, t)
                { onUpdate(config.copy(quality = it)) }
        }
    }
}

// ─── Passo 4 — Vestuário & Equipamento ───────────────────────────────────────

@Composable
fun ClothingStep(
    config: CharacterConfig,
    outfit: List<String>,
    shoesHat: List<String>,
    weapon: List<String>,
    t: CharacterTranslations.Translations,
    onUpdate: (CharacterConfig) -> Unit
) {
    Column(modifier = Modifier.verticalScroll(rememberScrollState())) {
        SectionHeader(title = "Uniform & Gear")
        TacticalCard {
            WorkflowDropdown("OUTFIT", config.outfit, outfit, t)
                { onUpdate(config.copy(outfit = it)) }
            WorkflowDropdown("SHOES : HAT", config.shoesHat, shoesHat, t)
                { onUpdate(config.copy(shoesHat = it)) }
            WorkflowDropdown("WEAPON", config.weapon, weapon, t)
                { onUpdate(config.copy(weapon = it)) }
        }
    }
}

// ─── Passo 5 — Exportação ─────────────────────────────────────────────────────

@Composable
fun ExportStep(
    config: CharacterConfig,
    exportFormats: List<String>,
    t: CharacterTranslations.Translations,
    onUpdate: (CharacterConfig) -> Unit
) {
    Column(modifier = Modifier.verticalScroll(rememberScrollState())) {
        SectionHeader(title = "Export Center")
        TacticalCard {
            WorkflowDropdown("EXPORT FORMAT(S)", config.exportFormats, exportFormats, t)
                { onUpdate(config.copy(exportFormats = it)) }
        }
    }
}

// ─── Passo 6 — Revisão e Envio ────────────────────────────────────────────────

@Composable
fun ReviewStep(
    config: CharacterConfig,
    uiState: UiState,
    t: CharacterTranslations.Translations,
    onDispatch: () -> Unit
) {
    Column(modifier = Modifier.verticalScroll(rememberScrollState())) {
        SectionHeader(title = "Final Review")
        TacticalCard {
            // Na revisão mostramos o label traduzido para o utilizador confirmar,
            // mas o CharacterConfig continua com os valores EN para enviar ao workflow
            ReviewRow("NAME",        config.name)
            ReviewRow("PRESET",      t.label(config.preset))
            ReviewRow("GENDER/AGE",  t.label(config.genderAge))
            ReviewRow("BODY",        t.label(config.body))
            ReviewRow("SKIN",        t.label(config.skinTone))
            ReviewRow("HAIR",        t.label(config.hair))
            ReviewRow("BEARD",       t.label(config.facialHair))
            ReviewRow("EYES",        t.label(config.eyes))
            ReviewRow("OUTFIT",      t.label(config.outfit))
            ReviewRow("SHOES/HAT",   t.label(config.shoesHat))
            ReviewRow("WEAPON",      t.label(config.weapon))
            ReviewRow("EXPRESSION",  t.label(config.expression))
            ReviewRow("RIG/ANIM",    t.label(config.rigAnim))
            ReviewRow("QUALITY",     t.label(config.quality))
            ReviewRow("FORMATS",     t.label(config.exportFormats))

            Spacer(modifier = Modifier.height(16.dp))

            when (uiState) {
                is UiState.Loading -> {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        CircularProgressIndicator(modifier = Modifier.size(20.dp), strokeWidth = 2.dp)
                        Spacer(modifier = Modifier.width(12.dp))
                        Text("Starting generation…", color = Color.Gray)
                    }
                }
                is UiState.Error -> {
                    Text("❌ ${uiState.message}", color = MaterialTheme.colorScheme.error)
                    Spacer(modifier = Modifier.height(8.dp))
                    TacticalButton(text = "Retry", onClick = onDispatch)
                }
                else -> TacticalButton(text = "⚡ Initialize Generation", onClick = onDispatch)
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
        OutlinedButton(onClick = onBack) {
            Icon(Icons.Default.ArrowBack, contentDescription = null)
            Spacer(modifier = Modifier.width(8.dp))
            Text("BACK")
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
