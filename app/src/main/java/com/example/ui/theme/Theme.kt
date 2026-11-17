package com.example.ui.theme

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Shapes
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp

private val LightColorScheme = lightColorScheme(
    primary = EmeraldPrimary,
    onPrimary = EmeraldOnPrimary,
    primaryContainer = EmeraldContainer,
    onPrimaryContainer = EmeraldOnContainer,
    secondary = WarmAmberSecondary,
    onSecondary = Color.White,
    secondaryContainer = WarmAmberContainer,
    onSecondaryContainer = WarmAmberOnContainer,
    background = SoftIvoryBackground,
    onBackground = DeepCharcoalInk,
    surface = CrispCardSurface,
    onSurface = DeepCharcoalInk,
    surfaceVariant = SoftSurfaceVariant,
    onSurfaceVariant = MutedSlateInk,
    outline = SubtleOutline,
    error = AlertCrimson,
    onError = Color.White,
    errorContainer = AlertCrimsonContainer,
    onErrorContainer = AlertOnCrimsonContainer
)

private val DarkColorScheme = darkColorScheme(
    primary = EmeraldPrimaryDark,
    onPrimary = Color(0xFF003734),
    primaryContainer = Color(0xFF004F4B),
    onPrimaryContainer = Color(0xFFB2F5EA),
    secondary = Color(0xFFFBBF24),
    onSecondary = Color(0xFF3F2300),
    secondaryContainer = Color(0xFF5C3400),
    onSecondaryContainer = Color(0xFFFEF3C7),
    background = DeepSlateBackground,
    onBackground = CrispOffWhiteInk,
    surface = DeepSlateSurface,
    onSurface = CrispOffWhiteInk,
    surfaceVariant = DeepSlateSurfaceVariant,
    onSurfaceVariant = MutedSilverInk,
    outline = DarkOutline,
    error = Color(0xFFFCA5A5),
    onError = Color(0xFF680003),
    errorContainer = Color(0xFF7F1D1D),
    onErrorContainer = Color(0xFFFEE2E2)
)

val AppShapes = Shapes(
    small = RoundedCornerShape(12.dp),
    medium = RoundedCornerShape(20.dp),
    large = RoundedCornerShape(28.dp),
    extraLarge = RoundedCornerShape(32.dp)
)

@Composable
fun SohojHomeTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    content: @Composable () -> Unit
) {
    val colorScheme = if (darkTheme) DarkColorScheme else LightColorScheme

    MaterialTheme(
        colorScheme = colorScheme,
        typography = AppTypography,
        shapes = AppShapes,
        content = content
    )
}
