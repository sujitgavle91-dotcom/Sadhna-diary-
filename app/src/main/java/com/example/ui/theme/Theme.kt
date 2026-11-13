package com.example.ui.theme

import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Shapes
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.unit.dp

private val MahakaliDarkColorScheme = darkColorScheme(
    primary = RaktaCrimsonDark,
    onPrimary = OnRaktaDark,
    primaryContainer = RaktaContainerDark,
    onPrimaryContainer = OnRaktaContainerDark,
    secondary = DivineGoldDark,
    onSecondary = OnDivineGoldDark,
    secondaryContainer = DivineGoldContainerDark,
    onSecondaryContainer = OnDivineGoldContainerDark,
    tertiary = ShaktiEmeraldDark,
    onTertiary = OnShaktiEmeraldDark,
    tertiaryContainer = ShaktiEmeraldContainerDark,
    onTertiaryContainer = OnShaktiEmeraldContainerDark,
    background = KaliObsidianBackground,
    onBackground = KaliIvoryOnBackground,
    surface = KaliSurface,
    onSurface = KaliIvoryOnBackground,
    surfaceVariant = KaliSurfaceContainer,
    onSurfaceVariant = KaliRoseGrayVariant,
    surfaceContainerLow = KaliSurfaceLow,
    surfaceContainer = KaliSurfaceContainer,
    surfaceContainerHigh = KaliSurfaceHigh,
    outline = KaliOutlineDark,
    outlineVariant = KaliOutlineVariantDark,
    error = ErrorCrimson,
    errorContainer = ErrorContainerDark,
    onErrorContainer = OnErrorContainerDark
)

private val MahakaliLightColorScheme = lightColorScheme(
    primary = KumkumPrimaryLight,
    onPrimary = OnKumkumLight,
    primaryContainer = KumkumContainerLight,
    onPrimaryContainer = OnKumkumContainerLight,
    secondary = AntiqueGoldLight,
    onSecondary = OnAntiqueGoldLight,
    secondaryContainer = AntiqueGoldContainerLight,
    onSecondaryContainer = OnAntiqueGoldContainerLight,
    tertiary = BilvaGreenLight,
    onTertiary = OnBilvaLight,
    tertiaryContainer = BilvaContainerLight,
    onTertiaryContainer = OnBilvaContainerLight,
    background = AlabasterBackground,
    onBackground = CharcoalOnBackground,
    surface = AlabasterSurface,
    onSurface = CharcoalOnBackground,
    surfaceVariant = AlabasterSurfaceContainer,
    onSurfaceVariant = MaroonUmberVariant,
    surfaceContainerLow = AlabasterSurfaceLow,
    surfaceContainer = AlabasterSurfaceContainer,
    surfaceContainerHigh = AlabasterSurfaceHigh,
    outline = WarmRoseOutline,
    outlineVariant = WarmRoseOutline,
    error = ErrorRedLight,
    errorContainer = ErrorContainerLight,
    onErrorContainer = OnErrorContainerLight
)

val SadhanaShapes = Shapes(
    small = RoundedCornerShape(12.dp),
    medium = RoundedCornerShape(16.dp),
    large = RoundedCornerShape(24.dp),
    extraLarge = RoundedCornerShape(28.dp)
)

@Composable
fun SadhanaSankalpTheme(
    darkTheme: Boolean = true,
    content: @Composable () -> Unit
) {
    val colorScheme = if (darkTheme) MahakaliDarkColorScheme else MahakaliLightColorScheme

    MaterialTheme(
        colorScheme = colorScheme,
        typography = Typography,
        shapes = SadhanaShapes,
        content = content
    )
}
