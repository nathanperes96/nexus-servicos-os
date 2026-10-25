package com.nexus.servicos.os.ui.theme

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.runtime.Composable

private val NexusDarkColorScheme = darkColorScheme(
    background = DarkBackground,
    surface = DarkSurface,
    surfaceVariant = DarkSurfaceVariant,
    onBackground = TextPrimary,
    onSurface = TextPrimary,
    primary = OrangeNeon,
    secondary = CyanNeon
)

@Composable
fun NexusServiçosOSTheme(content: @Composable () -> Unit) {
    MaterialTheme(
        colorScheme = NexusDarkColorScheme,
        content = content
    )
}