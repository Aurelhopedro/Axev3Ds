package com.example.ui.theme

import androidx.compose.ui.graphics.Color

// ── Paleta "Monge + Vibrante" (dark suave) ────────────────────────────────
val AppBackground       = Color(0xFF12151C)   // fundo principal
val CardSurface         = Color(0xFF1C202A)   // cards/painéis
val CardSurfaceElevated = Color(0xFF232836)   // superfície elevada
val SurfaceHighlight    = Color(0xFF2A3142)

// ── Ações e estados ──────────────────────────────────────────────────────
val PrimaryOrange       = Color(0xFFFF7A00)   // ação principal
val PrimaryOrangeEnd    = Color(0xFFFF5E00)   // gradiente do CTA
val PrimaryGlow         = Color(0x33FF7A00)

val MintSuccess         = Color(0xFF00E5A0)   // sucesso/calma
val MintGlow            = Color(0x2200E5A0)

val SoftError           = Color(0xFFFF4D4D)
val SoftErrorDark       = Color(0xFFD93D3D)
val SoftErrorGlow       = Color(0x22FF4D4D)

val PendingAmber        = Color(0xFFE0B35A)   // pendente neutro/amarelado
val WarningGlow         = Color(0x22E0B35A)

// ── Texto ────────────────────────────────────────────────────────────────
val TextPrimary         = Color(0xFFF0F2F5)
val TextSecondary       = Color(0xFF8B93A1)
val TextMuted           = Color(0xFF5C6475)

// ── Bordas/divisores sutis ───────────────────────────────────────────────
val BorderSubtle        = Color(0x1AFFFFFF)
val BorderSoft          = Color(0x263A4256)

// ── Aliases de compatibilidade (código existente) ────────────────────────
val TacticalOrange      = PrimaryOrange
val TacticalOrangeLight = Color(0xFFFF9A4D)
val TacticalOrangeDark  = Color(0xFFCC6200)
val TacticalOrangeGlow  = PrimaryGlow

val PremiumDark         = AppBackground
val SurfaceDark         = CardSurface
val SurfaceElevated     = CardSurfaceElevated

val BorderAccent        = BorderSoft
val BorderOrange        = Color(0x66FF7A00)

val SuccessGreen        = MintSuccess
val SuccessGreenDark    = Color(0xFF00B880)
val SuccessGlow         = MintGlow
val ErrorRed            = SoftError
val ErrorRedDark        = SoftErrorDark
val ErrorGlow           = SoftErrorGlow
val WarningAmber        = PendingAmber

val NeonCyan            = MintSuccess
val NeonCyanGlow        = MintGlow

val GradientStart       = PrimaryOrange
val GradientMid         = PrimaryOrangeEnd
val GradientEnd         = Color(0xFFFF8A3D)

val Amber               = WarningAmber
