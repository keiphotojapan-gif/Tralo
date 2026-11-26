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

private val TraloLightColorScheme = lightColorScheme(
    primary = ForestGreen,
    onPrimary = Color.White,
    primaryContainer = SageTint,
    onPrimaryContainer = ForestGreenDeep,
    secondary = PineGreen,
    onSecondary = Color.White,
    secondaryContainer = SageSubtle,
    onSecondaryContainer = ForestGreen,
    tertiary = ChampagneGold,
    onTertiary = Color.White,
    tertiaryContainer = ChampagneGoldSoft,
    onTertiaryContainer = CharcoalInk,
    background = WarmOffWhite,
    onBackground = CharcoalInk,
    surface = CardWhite,
    onSurface = CharcoalInk,
    surfaceVariant = GreigeSurface,
    onSurfaceVariant = MutedSlate,
    surfaceContainerLowest = CardWhite,
    surfaceContainerLow = WarmOffWhite,
    surfaceContainer = GreigeSurface,
    surfaceContainerHigh = GreigeSurfaceHigh,
    outline = HairlineBorder,
    outlineVariant = HairlineBorder,
    error = StatusDisruptionRed,
    onError = Color.White,
    errorContainer = StatusDisruptionRedBg,
    onErrorContainer = StatusDisruptionRed
)

private val TraloDarkColorScheme = darkColorScheme(
    primary = DarkPrimarySage,
    onPrimary = DarkOnPrimary,
    primaryContainer = ForestGreen,
    onPrimaryContainer = SageTint,
    secondary = SageGreen,
    onSecondary = Color.White,
    secondaryContainer = DarkSurfaceHigh,
    onSecondaryContainer = SageTint,
    tertiary = ChampagneGold,
    onTertiary = CharcoalInk,
    tertiaryContainer = Color(0xFF3E3118),
    onTertiaryContainer = ChampagneGoldSoft,
    background = DarkBackground,
    onBackground = DarkTextPrimary,
    surface = DarkSurface,
    onSurface = DarkTextPrimary,
    surfaceVariant = DarkSurfaceHigh,
    onSurfaceVariant = DarkTextMuted,
    surfaceContainerLowest = DarkBackground,
    surfaceContainerLow = DarkSurface,
    surfaceContainer = DarkSurfaceHigh,
    surfaceContainerHigh = Color(0xFF2B3731),
    outline = DarkOutline,
    outlineVariant = DarkOutline,
    error = Color(0xFFE57373),
    onError = Color(0xFF3E0E0E),
    errorContainer = Color(0xFF4F1B1B),
    onErrorContainer = Color(0xFFFFDAD6)
)

val TraloShapes = Shapes(
    extraSmall = RoundedCornerShape(8.dp),
    small = RoundedCornerShape(12.dp),
    medium = RoundedCornerShape(16.dp),
    large = RoundedCornerShape(22.dp),
    extraLarge = RoundedCornerShape(28.dp)
)

@Composable
fun TraloTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    content: @Composable () -> Unit
) {
    val colorScheme = if (darkTheme) TraloDarkColorScheme else TraloLightColorScheme

    MaterialTheme(
        colorScheme = colorScheme,
        typography = TraloTypography,
        shapes = TraloShapes,
        content = content
    )
}
