package com.example.ui.theme

import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Shapes
import androidx.compose.material3.darkColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp

private val SuSaDarkColorScheme = darkColorScheme(
    primary = IndigoPrimary,
    onPrimary = Color.White,
    primaryContainer = IndigoHover,
    onPrimaryContainer = TextMain,
    secondary = VioletAccent,
    onSecondary = ObsidianBg,
    secondaryContainer = StudioSurfaceHigh,
    onSecondaryContainer = VioletAccent,
    tertiary = EmeraldSuccess,
    onTertiary = ObsidianBg,
    tertiaryContainer = Color(0xFF064E3B),
    onTertiaryContainer = EmeraldSuccess,
    background = ObsidianBg,
    onBackground = TextMain,
    surface = StudioSurface,
    onSurface = TextMain,
    surfaceVariant = StudioSurfaceElevated,
    onSurfaceVariant = TextMuted,
    outline = StudioBorderStrong,
    outlineVariant = StudioBorder,
    error = RoseError,
    onError = Color.White,
)

private val SuSaShapes = Shapes(
    extraSmall = RoundedCornerShape(8.dp),
    small = RoundedCornerShape(10.dp),
    medium = RoundedCornerShape(14.dp),
    large = RoundedCornerShape(18.dp),
    extraLarge = RoundedCornerShape(24.dp),
)

@Composable
fun SuSaStudioTheme(
    content: @Composable () -> Unit,
) {
    MaterialTheme(
        colorScheme = SuSaDarkColorScheme,
        typography = SuSaTypography,
        shapes = SuSaShapes,
        content = content,
    )
}
