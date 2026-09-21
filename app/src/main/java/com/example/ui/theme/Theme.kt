package com.example.ui.theme

import android.os.Build
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.dynamicDarkColorScheme
import androidx.compose.material3.dynamicLightColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext

private val GuardianDarkColorScheme = darkColorScheme(
    primary = GuardianCyanPrimary,
    onPrimary = GuardianCyanOnPrimary,
    primaryContainer = GuardianCyanContainer,
    onPrimaryContainer = GuardianCyanOnContainer,

    secondary = GuardianEmeraldSuccess,
    onSecondary = GuardianEmeraldOnSuccess,
    secondaryContainer = GuardianEmeraldContainer,
    onSecondaryContainer = GuardianEmeraldOnContainer,

    tertiary = GuardianAmberWarning,
    onTertiary = GuardianAmberOnWarning,
    tertiaryContainer = GuardianAmberContainer,
    onTertiaryContainer = GuardianAmberOnContainer,

    error = GuardianAlertDanger,
    onError = GuardianAlertOnDanger,
    errorContainer = GuardianAlertContainer,
    onErrorContainer = GuardianAlertOnContainer,

    background = GuardianDarkBackground,
    onBackground = GuardianTextPrimary,

    surface = GuardianDarkSurface,
    onSurface = GuardianTextPrimary,
    surfaceVariant = GuardianDarkSurfaceVariant,
    onSurfaceVariant = GuardianTextSecondary,

    outline = GuardianBorderLight,
    outlineVariant = GuardianDarkSurfaceCard
)

private val GuardianLightColorScheme = lightColorScheme(
    primary = GuardianLightPrimary,
    onPrimary = GuardianLightOnPrimary,
    primaryContainer = Color(0xFFBCEBF0),
    onPrimaryContainer = Color(0xFF002024),

    secondary = Color(0xFF006D37),
    onSecondary = Color.White,
    secondaryContainer = Color(0xFF95F9B1),
    onSecondaryContainer = Color(0xFF00210C),

    error = Color(0xFFBA1A1A),
    onError = Color.White,

    background = GuardianLightBackground,
    onBackground = Color(0xFF0F172A),

    surface = GuardianLightSurface,
    onSurface = Color(0xFF0F172A),
    surfaceVariant = GuardianLightSurfaceVariant,
    onSurfaceVariant = Color(0xFF475569),

    outline = Color(0xFFCBD5E1)
)

@Composable
fun GuardianXTheme(
    darkTheme: Boolean = true, // Default to Cybersecurity tactical dark theme
    dynamicColor: Boolean = false, // Keep high-contrast custom identity
    content: @Composable () -> Unit
) {
    val colorScheme = when {
        dynamicColor && Build.VERSION.SDK_INT >= Build.VERSION_CODES.S -> {
            val context = LocalContext.current
            if (darkTheme) dynamicDarkColorScheme(context) else dynamicLightColorScheme(context)
        }
        darkTheme -> GuardianDarkColorScheme
        else -> GuardianLightColorScheme
    }

    MaterialTheme(
        colorScheme = colorScheme,
        typography = Typography,
        content = content
    )
}

// Backwards compatibility alias
@Composable
fun MyApplicationTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    dynamicColor: Boolean = false,
    content: @Composable () -> Unit
) {
    GuardianXTheme(darkTheme = darkTheme, dynamicColor = dynamicColor, content = content)
}
