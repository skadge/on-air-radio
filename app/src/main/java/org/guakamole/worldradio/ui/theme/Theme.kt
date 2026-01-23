package org.guakamole.worldradio.ui.theme

import android.app.Activity
import android.os.Build
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.dynamicDarkColorScheme
import androidx.compose.material3.dynamicLightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.SideEffect
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalView
import androidx.core.view.WindowCompat

// Material 3 Color Scheme
// Premium Midnight & Neon Palette
val MidnightBackground = Color(0xFF0F111A)
val MidnightSurface = Color(0xFF1E2130)
val ElectricBlue = Color(0xFF4B7BFF) // Primary
val NeonPurple = Color(0xFFE040FB) // Secondary / Accents
val SoftWhite = Color(0xFFEBEBF5) // OnBackground / OnSurface
val SubtleGrey = Color(0xFF2C2F42) // Surface Variant
val GlassBlack = Color(0xCC000000) // For glassmorphism

// We focus on a premium Dark Theme for this app identity
private val PremiumDarkColorScheme =
        darkColorScheme(
                primary = ElectricBlue,
                onPrimary = Color.White,
                primaryContainer = Color(0xFF243460),
                onPrimaryContainer = Color.White,
                secondary = NeonPurple,
                onSecondary = Color.White,
                secondaryContainer = Color(0xFF4D1F60),
                onSecondaryContainer = Color.White,
                background = MidnightBackground,
                onBackground = SoftWhite,
                surface = MidnightSurface,
                onSurface = SoftWhite,
                surfaceVariant = SubtleGrey,
                onSurfaceVariant = Color(0xFFB0B0C0),
                error = Color(0xFFFF453A)
        )

// We can map light theme to something similar or keep it standard,
// but for "Vibrant" requests, enforcing the dark aesthetic usually wins.
private val PremiumLightColorScheme = PremiumDarkColorScheme

@Composable
fun RadioTheme(
        darkTheme: Boolean = isSystemInDarkTheme(),
        dynamicColor: Boolean = true,
        content: @Composable () -> Unit
) {
    val colorScheme =
            when {
                dynamicColor && Build.VERSION.SDK_INT >= Build.VERSION_CODES.S -> {
                    val context = LocalContext.current
                    if (darkTheme) dynamicDarkColorScheme(context)
                    else dynamicLightColorScheme(context)
                }
                darkTheme -> PremiumDarkColorScheme
                else -> PremiumLightColorScheme
            }

    val view = LocalView.current
    if (!view.isInEditMode) {
        SideEffect {
            val window = (view.context as Activity).window
            window.statusBarColor = colorScheme.primary.toArgb()
            WindowCompat.getInsetsController(window, view).isAppearanceLightStatusBars = !darkTheme
        }
    }

    MaterialTheme(colorScheme = colorScheme, content = content)
}
