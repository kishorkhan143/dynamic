package com.example.ui.theme

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color

private val AuraDarkColorScheme = darkColorScheme(
    primary = AuraElectricAqua,
    onPrimary = Color(0xFF003733),
    primaryContainer = Color(0xFF073834),
    onPrimaryContainer = AuraElectricAqua,
    secondary = AuraNeonViolet,
    onSecondary = Color.White,
    secondaryContainer = Color(0xFF32144F),
    onSecondaryContainer = Color(0xFFE9D5FF),
    tertiary = AuraCyberCyan,
    onTertiary = Color.Black,
    background = AuraBackground,
    onBackground = AuraTextPrimary,
    surface = AuraSurface,
    onSurface = AuraTextPrimary,
    surfaceVariant = AuraSurfaceVariant,
    onSurfaceVariant = AuraTextSecondary,
    outline = AuraBorder,
    outlineVariant = AuraBorderBright,
    error = AuraError,
    onError = Color.White
)

@Composable
fun AuraTheme(
    content: @Composable () -> Unit
) {
    MaterialTheme(
        colorScheme = AuraDarkColorScheme,
        typography = AuraTypography,
        content = content
    )
}
