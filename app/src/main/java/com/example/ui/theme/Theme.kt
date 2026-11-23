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

private val RegencyLightColorScheme = lightColorScheme(
    primary = RegencySage,
    onPrimary = Color.White,
    primaryContainer = RegencySageContainer,
    onPrimaryContainer = RegencySageDark,
    secondary = AntiqueWalnut,
    onSecondary = Color.White,
    secondaryContainer = AntiqueWalnutContainer,
    onSecondaryContainer = InkCharcoal,
    tertiary = WarmBronze,
    onTertiary = Color.White,
    tertiaryContainer = WarmBronzeContainer,
    onTertiaryContainer = AntiqueWalnut,
    background = ParchmentBackground,
    onBackground = InkCharcoal,
    surface = LinenSurface,
    onSurface = InkCharcoal,
    surfaceVariant = OatmealSurfaceVariant,
    onSurfaceVariant = MutedTaupe,
    outline = HairlineBorder,
    outlineVariant = SubtleRule,
    error = ForgotCrimson,
    onError = Color.White,
    errorContainer = ForgotCrimsonBg,
    onErrorContainer = ForgotCrimson
)

private val RegencyDarkColorScheme = darkColorScheme(
    primary = SoftSageDark,
    onPrimary = DeepWalnutBg,
    primaryContainer = RegencySageDark,
    onPrimaryContainer = SoftParchmentText,
    secondary = SoftBronzeDark,
    onSecondary = DeepWalnutBg,
    secondaryContainer = DarkParlorVariant,
    onSecondaryContainer = SoftParchmentText,
    tertiary = WarmBronze,
    onTertiary = DeepWalnutBg,
    background = DeepWalnutBg,
    onBackground = SoftParchmentText,
    surface = DarkParlorSurface,
    onSurface = SoftParchmentText,
    surfaceVariant = DarkParlorVariant,
    onSurfaceVariant = MutedSandText,
    outline = DarkHairline,
    outlineVariant = DarkHairline,
    error = Color(0xFFE08B81),
    onError = DeepWalnutBg,
    errorContainer = Color(0xFF4F2420),
    onErrorContainer = Color(0xFFF6D6D2)
)

val RegencyShapes = Shapes(
    extraSmall = RoundedCornerShape(4.dp),
    small = RoundedCornerShape(8.dp),
    medium = RoundedCornerShape(12.dp),
    large = RoundedCornerShape(18.dp),
    extraLarge = RoundedCornerShape(24.dp)
)

@Composable
fun HanziSalonTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    content: @Composable () -> Unit
) {
    val colorScheme = if (darkTheme) RegencyDarkColorScheme else RegencyLightColorScheme

    MaterialTheme(
        colorScheme = colorScheme,
        typography = Typography,
        shapes = RegencyShapes,
        content = content
    )
}
