package com.edro08.vaultk.ui.theme

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.Immutable
import androidx.compose.runtime.ReadOnlyComposable
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.graphics.Color

@Immutable
data class VaultkCustomColors(
    val iconTileBackground: Color,
    val iconTileBorder: Color,
    val iconTileTint: Color,
    val iconNormal: Color,
    val buttonSecondaryBackground: Color,
    val buttonSecondaryText: Color,
    val buttonSecondaryBorder: Color,
    val inputBackground: Color,
    val inputBorder: Color,
    val inputBorderFocused: Color,
    val textMuted: Color,
)

val LocalVaultkCustomColors = staticCompositionLocalOf {
    VaultkCustomColors(
        iconTileBackground = VaultTileBackground,
        iconTileBorder = VaultTileBorder,
        iconTileTint = VaultLavender,
        iconNormal = ZincLight,
        buttonSecondaryBackground = SecondaryButtonBg,
        buttonSecondaryText = WhitePure,
        buttonSecondaryBorder = SecondaryButtonBorder,
        inputBackground = Color(0xFF0D0D10),
        inputBorder = VaultBorder,
        inputBorderFocused = VaultIndigo,
        textMuted = ZincDark,
    )
}

val MaterialTheme.vaultkCustomColors: VaultkCustomColors
    @Composable
    @ReadOnlyComposable
    get() = LocalVaultkCustomColors.current

// Paleta Inspirada en el Icono Oficial de Vaultk (Propuesta 1: Electric Indigo & Obsidian)
private val DarkColors = darkColorScheme(
    primary = VaultIndigo, // #582BF5 (violeta eléctrico del pliegue de la V)
    onPrimary = WhitePure,
    primaryContainer = VaultTileBackground, // #1E1738
    onPrimaryContainer = VaultLavender, // #C185FC
    secondary = VaultLavender, // #C185FC
    onSecondary = PureBlack,
    secondaryContainer = VaultNavActivePill, // #221846
    onSecondaryContainer = VaultLavender, // #C185FC
    tertiary = StatusGreen,
    background = PureBlack, // #000000 (fondo idéntico al icono)
    onBackground = WhitePure,
    surface = VaultObsidian, // #131316 (cajas obsidiana)
    onSurface = WhitePure,
    surfaceVariant = VaultField, // #1A1A1E
    onSurfaceVariant = ZincMedium, // #A1A1AA
    outline = VaultBorderInteractive, // #332763
    outlineVariant = VaultBorder, // #23222B
    error = StatusRed,
)

private val LightColors = lightColorScheme(
    primary = VaultIndigo, // #582BF5 (índigo de la V, contraste > 8:1 sobre blanco)
    onPrimary = WhitePure,
    primaryContainer = LightTileBackground, // #F0EBFE (lavanda suave)
    onPrimaryContainer = VaultIndigo, // #582BF5
    secondary = VaultIndigo,
    onSecondary = WhitePure,
    secondaryContainer = LightNavActivePill, // #EDE8FE (píldora activa de navegación)
    onSecondaryContainer = VaultIndigo, // #582BF5
    tertiary = StatusGreen,
    background = LightBackground, // #F8F9FB (blanco nieve limpio)
    onBackground = LightTextPrimary, // #09090B
    surface = LightSurface, // #FFFFFF (tarjeta blanco puro)
    onSurface = LightTextPrimary, // #09090B
    surfaceVariant = LightField, // #F4F4F6
    onSurfaceVariant = LightTextSecondary, // #6B7280
    outline = LightBorderInteractive, // #E4D7FC
    outlineVariant = LightBorder, // #E5E7EB
    error = StatusRed,
)

@Composable
fun VaultkTheme(
    darkTheme: Boolean = true,
    content: @Composable () -> Unit,
) {
    val customColors = if (darkTheme) {
        VaultkCustomColors(
            iconTileBackground = VaultTileBackground,
            iconTileBorder = VaultTileBorder,
            iconTileTint = VaultLavender,
            iconNormal = ZincLight,
            buttonSecondaryBackground = SecondaryButtonBg,
            buttonSecondaryText = WhitePure,
            buttonSecondaryBorder = SecondaryButtonBorder,
            inputBackground = Color(0xFF0D0D10),
            inputBorder = VaultBorder,
            inputBorderFocused = VaultIndigo,
            textMuted = ZincDark,
        )
    } else {
        VaultkCustomColors(
            iconTileBackground = LightTileBackground,
            iconTileBorder = LightBorderInteractive,
            iconTileTint = VaultIndigo,
            iconNormal = LightTextSecondary,
            buttonSecondaryBackground = LightButtonSecondary,
            buttonSecondaryText = LightTextPrimary,
            buttonSecondaryBorder = LightButtonSecondaryBorder,
            inputBackground = LightSurface,
            inputBorder = LightBorder,
            inputBorderFocused = VaultIndigo,
            textMuted = LightTextSecondary,
        )
    }

    CompositionLocalProvider(LocalVaultkCustomColors provides customColors) {
        MaterialTheme(
            colorScheme = if (darkTheme) DarkColors else LightColors,
            typography = VaultkTypography,
            content = content,
        )
    }
}
