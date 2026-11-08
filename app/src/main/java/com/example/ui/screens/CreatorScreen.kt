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
import com.example.ui.MainViewModel
import com.example.ui.components.SectionHeader
import com.example.ui.components.TacticalButton
import com.example.ui.components.TacticalCard
import com.example.ui.theme.TacticalOrange

@Composable
fun CreatorScreen(viewModel: MainViewModel) {
    var currentStep by remember { mutableStateOf(1) }
    val character by viewModel.currentCharacter.collectAsState()

    Column(modifier = Modifier.fillMaxSize()) {
        StepperHeader(currentStep = currentStep)
        
        Box(modifier = Modifier.weight(1f).padding(horizontal = 16.dp)) {
            when (currentStep) {
                1 -> IdentityStep(character) { viewModel.updateCharacter(it) }
                2 -> AppearanceStep(character) { viewModel.updateCharacter(it) }
                3 -> ClothingStep(character) { viewModel.updateCharacter(it) }
                4 -> StyleStep(character) { viewModel.updateCharacter(it) }
                5 -> ExportStep(character) { viewModel.updateCharacter(it) }
                6 -> ReviewStep(character) { viewModel.generate() }
            }
        }

        NavigationFooter(
            currentStep = currentStep,
            onBack = { if (currentStep > 1) currentStep-- },
            onNext = { if (currentStep < 6) currentStep++ }
        )
    }
}

@Composable
fun StepperHeader(currentStep: Int) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(16.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        repeat(6) { index ->
            val step = index + 1
            val isActive = step == currentStep
            val isCompleted = step < currentStep
            
            Box(
                modifier = Modifier
                    .size(32.dp)
                    .background(
                        if (isActive) TacticalOrange else if (isCompleted) Color.Green else Color.Gray.copy(alpha = 0.3f),
                        MaterialTheme.shapes.small
                    ),
                contentAlignment = Alignment.Center
            ) {
                if (isCompleted) {
                    Icon(Icons.Default.Check, contentDescription = null, tint = Color.Black, modifier = Modifier.size(16.dp))
                } else {
                    Text(text = step.toString(), color = if (isActive) Color.Black else Color.White, fontWeight = FontWeight.Bold)
                }
            }
            if (index < 5) {
                Box(modifier = Modifier.weight(1f).height(2.dp).background(Color.Gray.copy(alpha = 0.3f)))
            }
        }
    }
}

@Composable
fun IdentityStep(config: CharacterConfig, onUpdate: (CharacterConfig) -> Unit) {
    Column(modifier = Modifier.verticalScroll(rememberScrollState())) {
        SectionHeader(title = "Character Identity")
        TacticalCard {
            Text("NAME", style = MaterialTheme.typography.labelSmall)
            OutlinedTextField(
                value = config.name,
                onValueChange = { onUpdate(config.copy(name = it)) },
                modifier = Modifier.fillMaxWidth()
            )
            Spacer(modifier = Modifier.height(16.dp))
            Text("CATEGORY", style = MaterialTheme.typography.labelSmall)
            val categories = listOf("Soldier", "Police", "Gangster", "Civil", "Special Forces")
            var expanded by remember { mutableStateOf(false) }
            Box {
                OutlinedButton(onClick = { expanded = true }, modifier = Modifier.fillMaxWidth()) {
                    Text(config.identity.category)
                }
                DropdownMenu(expanded = expanded, onDismissRequest = { expanded = false }) {
                    categories.forEach { cat ->
                        DropdownMenuItem(
                            text = { Text(cat) },
                            onClick = {
                                onUpdate(config.copy(identity = config.identity.copy(category = cat)))
                                expanded = false
                            }
                        )
                    }
                }
            }
        }
    }
}

@Composable
fun AppearanceStep(config: CharacterConfig, onUpdate: (CharacterConfig) -> Unit) {
    Column(modifier = Modifier.verticalScroll(rememberScrollState())) {
        SectionHeader(title = "Physical Traits")
        TacticalCard {
            Text("BODY TYPE", style = MaterialTheme.typography.labelSmall)
            val types = listOf("Thin", "Normal", "Athletic", "Muscular", "Heavy")
            types.forEach { type ->
                Row(verticalAlignment = Alignment.CenterVertically) {
                    RadioButton(
                        selected = config.appearance.bodyType == type,
                        onClick = { onUpdate(config.copy(appearance = config.appearance.copy(bodyType = type))) }
                    )
                    Text(type)
                }
            }
        }
    }
}

@Composable
fun ClothingStep(config: CharacterConfig, onUpdate: (CharacterConfig) -> Unit) {
    Column(modifier = Modifier.verticalScroll(rememberScrollState())) {
        SectionHeader(title = "Uniform & Gear")
        TacticalCard {
            Text("UNIFORM TYPE", style = MaterialTheme.typography.labelSmall)
            val types = listOf("Military", "Police", "Civil")
            types.forEach { type ->
                Row(verticalAlignment = Alignment.CenterVertically) {
                    RadioButton(
                        selected = config.clothing.type == type,
                        onClick = { onUpdate(config.copy(clothing = config.clothing.copy(type = type))) }
                    )
                    Text(type)
                }
            }
        }
    }
}

@Composable
fun StyleStep(config: CharacterConfig, onUpdate: (CharacterConfig) -> Unit) {
    Column(modifier = Modifier.verticalScroll(rememberScrollState())) {
        SectionHeader(title = "Visual Direction")
        TacticalCard {
            Text("ART STYLE", style = MaterialTheme.typography.labelSmall)
            val styles = listOf("Realist", "Cinematic", "Anime 3D", "Stylized")
            styles.forEach { s ->
                Row(verticalAlignment = Alignment.CenterVertically) {
                    RadioButton(
                        selected = config.style.direction == s,
                        onClick = { onUpdate(config.copy(style = config.style.copy(direction = s))) }
                    )
                    Text(s)
                }
            }
        }
    }
}

@Composable
fun ExportStep(config: CharacterConfig, onUpdate: (CharacterConfig) -> Unit) {
    Column(modifier = Modifier.verticalScroll(rememberScrollState())) {
        SectionHeader(title = "Export Center")
        TacticalCard {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Checkbox(
                    checked = config.export.formats.contains("GLB"),
                    onCheckedChange = { /* Always GLB */ }
                )
                Text("GLB (Recommended for Godot)")
            }
            Row(verticalAlignment = Alignment.CenterVertically) {
                Checkbox(
                    checked = config.export.includeRig,
                    onCheckedChange = { onUpdate(config.copy(export = config.export.copy(includeRig = it))) }
                )
                Text("Include Rig (Skeleton)")
            }
            Row(verticalAlignment = Alignment.CenterVertically) {
                Checkbox(
                    checked = config.export.includeAnimations,
                    onCheckedChange = { onUpdate(config.copy(export = config.export.copy(includeAnimations = it))) }
                )
                Text("Include Animations")
            }
        }
    }
}

@Composable
fun ReviewStep(config: CharacterConfig, onDispatch: () -> Unit) {
    Column(modifier = Modifier.verticalScroll(rememberScrollState())) {
        SectionHeader(title = "Final Review")
        TacticalCard {
            Text("IDENTIDADE: ${config.name} (${config.identity.category})")
            Text("APARÊNCIA: ${config.appearance.bodyType}")
            Text("VESTUÁRIO: ${config.clothing.type}")
            Text("QUALIDADE: ${config.style.quality}")
            Spacer(modifier = Modifier.height(16.dp))
            TacticalButton(text = "Initialize Generation", onClick = onDispatch)
        }
    }
}

@Composable
fun NavigationFooter(currentStep: Int, onBack: () -> Unit, onNext: () -> Unit) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(16.dp),
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
            Button(onClick = onNext, colors = ButtonDefaults.buttonColors(containerColor = TacticalOrange)) {
                Text("NEXT")
                Spacer(modifier = Modifier.width(8.dp))
                Icon(Icons.Default.ArrowForward, contentDescription = null)
            }
        }
    }
}
