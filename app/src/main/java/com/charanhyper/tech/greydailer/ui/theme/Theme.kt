package com.charanhyper.tech.greydailer.ui.theme

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable

private val WhiteColorScheme = lightColorScheme(
    primary = Pink,
    onPrimary = White,
    primaryContainer = PinkLight,
    onPrimaryContainer = NearBlack,
    secondary = PinkDark,
    onSecondary = White,
    secondaryContainer = PinkSoft,
    onSecondaryContainer = NearBlack,
    tertiary = AccentGreen,
    onTertiary = White,
    tertiaryContainer = LightGreen,
    onTertiaryContainer = NearBlack,
    background = White,
    onBackground = NearBlack,
    surface = White,
    onSurface = NearBlack,
    surfaceVariant = OffWhite,
    onSurfaceVariant = DarkGray,
    error = AccentRed,
    onError = White,
    errorContainer = LightRed,
    onErrorContainer = AccentRed,
    outline = LightGray,
    outlineVariant = LightGray,
    surfaceContainerLowest = White,
    surfaceContainerLow = OffWhite,
    surfaceContainer = OffWhite,
    surfaceContainerHigh = LightGray,
    surfaceContainerHighest = LightGray,
)

@Composable
fun GreydailerTheme(
    content: @Composable () -> Unit
) {
    MaterialTheme(
        colorScheme = WhiteColorScheme,
        typography = Typography,
        content = content
    )
}