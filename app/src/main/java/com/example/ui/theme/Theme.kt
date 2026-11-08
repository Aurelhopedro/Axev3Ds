package com.example.ui.theme

import android.app.Activity
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.SideEffect
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.platform.LocalView
import androidx.core.view.WindowCompat

private val DarkColorScheme = darkColorScheme(
    primary           = TacticalOrange,
    primaryContainer  = TacticalOrangeGlow,
    onPrimary         = PremiumDark,
    onPrimaryContainer = TacticalOrangeLight,

    secondary         = NeonCyan,
    secondaryContainer = NeonCyanGlow,
    onSecondary       = PremiumDark,

    tertiary          = WarningAmber,
    tertiaryContainer = WarningGlow,
    onTertiary        = PremiumDark,

    background        = PremiumDark,
    surface           = SurfaceDark,
    surfaceVariant    = SurfaceElevated,
    surfaceTint       = TacticalOrange,

    onBackground      = TextPrimary,
    onSurface         = TextPrimary,
    onSurfaceVariant  = TextSecondary,

    outline           = BorderSubtle,
    outlineVariant    = BorderAccent,

    error             = ErrorRed,
    errorContainer    = ErrorGlow,
    onError           = Color.White,
)

@Composable
fun AXEVCRIME3DSTUDIOTheme(
    darkTheme: Boolean = true,
    content: @Composable () -> Unit
) {
    val colorScheme = DarkColorScheme
    val view = LocalView.current

    if (!view.isInEditMode) {
        SideEffect {
            val window = (view.context as Activity).window
            // Fundo da status bar = fundo do app (edge-to-edge)
            window.statusBarColor = PremiumDark.toArgb()
            window.navigationBarColor = PremiumDark.toArgb()
            WindowCompat.getInsetsController(window, view).apply {
                isAppearanceLightStatusBars = false
                isAppearanceLightNavigationBars = false
            }
        }
    }

    MaterialTheme(
        colorScheme = colorScheme,
        typography  = Typography,
        content     = content
    )
}
