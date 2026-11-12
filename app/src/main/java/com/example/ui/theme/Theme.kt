package com.example.ui.theme

import android.app.Activity
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.SideEffect
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.platform.LocalView
import androidx.core.view.WindowCompat

private val DarkColorScheme = darkColorScheme(
    primary            = PrimaryOrange,
    primaryContainer   = PrimaryGlow,
    onPrimary          = AppBackground,
    onPrimaryContainer = TextPrimary,

    secondary          = MintSuccess,
    secondaryContainer = MintGlow,
    onSecondary        = AppBackground,

    tertiary           = PendingAmber,
    tertiaryContainer  = WarningGlow,
    onTertiary         = AppBackground,

    background         = AppBackground,
    surface            = CardSurface,
    surfaceVariant     = CardSurfaceElevated,
    surfaceTint        = PrimaryOrange,

    onBackground       = TextPrimary,
    onSurface          = TextPrimary,
    onSurfaceVariant   = TextSecondary,

    outline            = BorderSubtle,
    outlineVariant     = BorderSoft,

    error              = SoftError,
    errorContainer     = SoftErrorGlow,
    onError            = TextPrimary,
)

@Composable
fun AXEVCRIME3DSTUDIOTheme(
    darkTheme: Boolean = true,
    content: @Composable () -> Unit
) {
    val view = LocalView.current
    if (!view.isInEditMode) {
        SideEffect {
            val window = (view.context as Activity).window
            window.statusBarColor = AppBackground.toArgb()
            window.navigationBarColor = AppBackground.toArgb()
            WindowCompat.getInsetsController(window, view).apply {
                isAppearanceLightStatusBars = false
                isAppearanceLightNavigationBars = false
            }
        }
    }

    MaterialTheme(
        colorScheme = DarkColorScheme,
        typography  = Typography,
        content     = content
    )
}
