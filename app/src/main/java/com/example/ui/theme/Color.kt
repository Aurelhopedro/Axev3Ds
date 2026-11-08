package com.example.ui.theme

import androidx.compose.ui.graphics.Color

// ── Cor principal — laranja tático ──────────────────────────────────────────
val TacticalOrange      = Color(0xFFFF6B00)
val TacticalOrangeLight = Color(0xFFFF9A4D)
val TacticalOrangeDark  = Color(0xFFCC5500)
val TacticalOrangeGlow  = Color(0x33FF6B00)   // para efeito brilho/glow

// ── Fundos premium (escuro profundo) ────────────────────────────────────────
val PremiumDark         = Color(0xFF0A0A0F)   // fundo principal — quase preto
val SurfaceDark         = Color(0xFF13131A)   // cards base
val SurfaceElevated     = Color(0xFF1C1C27)   // cards elevados
val SurfaceHighlight    = Color(0xFF24243A)   // cards em destaque

// ── Bordas e divisores ───────────────────────────────────────────────────────
val BorderSubtle        = Color(0xFF2A2A3A)
val BorderAccent        = Color(0xFF3A3A5A)
val BorderOrange        = Color(0x66FF6B00)   // borda laranja semitransparente

// ── Texto ────────────────────────────────────────────────────────────────────
val TextPrimary         = Color(0xFFF0F0F0)
val TextSecondary       = Color(0xFF8A8AA0)
val TextMuted           = Color(0xFF55556A)

// ── Estados ──────────────────────────────────────────────────────────────────
val SuccessGreen        = Color(0xFF00E676)
val SuccessGreenDark    = Color(0xFF00C853)
val SuccessGlow         = Color(0x2200E676)
val ErrorRed            = Color(0xFFFF3D57)
val ErrorRedDark        = Color(0xFFD50000)
val ErrorGlow           = Color(0x22FF3D57)
val WarningAmber        = Color(0xFFFFAB00)
val WarningGlow         = Color(0x22FFAB00)

// ── Neon accent (para highlights especiais) ──────────────────────────────────
val NeonCyan            = Color(0xFF00E5FF)
val NeonCyanGlow        = Color(0x1100E5FF)

// ── Gradientes (usar com Brush.linearGradient) ───────────────────────────────
// Exemplo: Brush.linearGradient(listOf(GradientStart, GradientEnd))
val GradientStart       = Color(0xFFFF6B00)
val GradientMid         = Color(0xFFFF3D57)
val GradientEnd         = Color(0xFFCC00FF)

// Legacy aliases (compatibilidade com código existente)
val Amber               = WarningAmber
