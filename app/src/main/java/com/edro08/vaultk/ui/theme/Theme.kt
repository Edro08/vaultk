package com.edro08.vaultk.ui.theme

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color

// Keep the brand palette in one place. A future appearance preference only needs to
// select the scheme here; components use MaterialTheme rather than fixed colors.
private val DarkColors = darkColorScheme(
    primary = Violet,
    onPrimary = Ink,
    primaryContainer = Color(0xFF322449),
    onPrimaryContainer = VioletLight,
    secondary = VioletLight,
    onSecondary = Ink,
    tertiary = Color(0xFF44DB98),
    background = Ink,
    onBackground = Color(0xFFF7F5FF),
    surface = Panel,
    onSurface = Color(0xFFF7F5FF),
    surfaceVariant = Field,
    onSurfaceVariant = Muted,
    outline = Border,
    outlineVariant = Color(0xFF212639),
    error = Color(0xFFFF6B80),
)

private val LightColors = lightColorScheme(
    primary = VioletDark,
    onPrimary = Color.White,
    primaryContainer = Color(0xFFEEE1FF),
    onPrimaryContainer = Color(0xFF392257),
    secondary = Color(0xFF635188),
    onSecondary = Color.White,
    tertiary = Color(0xFF147847),
    background = Color(0xFFFBF9FF),
    onBackground = Color(0xFF1B1822),
    surface = Color.White,
    onSurface = Color(0xFF1B1822),
    surfaceVariant = Color(0xFFF3F0FA),
    onSurfaceVariant = Color(0xFF57516A),
    outline = Color(0xFFD9D2E5),
    outlineVariant = Color(0xFFEAE5F1),
    error = Color(0xFFB4233B),
)

@Composable
fun VaultkTheme(
    darkTheme: Boolean = true,
    content: @Composable () -> Unit,
) {
    MaterialTheme(
        colorScheme = if (darkTheme) DarkColors else LightColors,
        typography = VaultkTypography,
        shapes = VaultkShapes,
        content = content,
    )
}
