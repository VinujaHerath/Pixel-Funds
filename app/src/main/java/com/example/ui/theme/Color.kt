package com.example.ui.theme

import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.Immutable
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.graphics.Color

// Editorial Forest Sage & Warm Clay Material 3 Expressive Fallback Palette
val SagePrimaryLight = Color(0xFF1E6444)
val SageOnPrimaryLight = Color(0xFFFFFFFF)
val SagePrimaryContainerLight = Color(0xFFA9F2C9)
val SageOnPrimaryContainerLight = Color(0xFF002113)

val ClaySecondaryLight = Color(0xFF4E6356)
val ClayOnSecondaryLight = Color(0xFFFFFFFF)
val ClaySecondaryContainerLight = Color(0xFFD0E8D7)
val ClayOnSecondaryContainerLight = Color(0xFF0C1F15)

val SandTertiaryLight = Color(0xFF835423)
val SandOnTertiaryLight = Color(0xFFFFFFFF)
val SandTertiaryContainerLight = Color(0xFFFFDCBE)
val SandOnTertiaryContainerLight = Color(0xFF2C1600)

val ErrorLight = Color(0xFFBA1A1A)
val OnErrorLight = Color(0xFFFFFFFF)
val ErrorContainerLight = Color(0xFFFFDAD6)
val OnErrorContainerLight = Color(0xFF410002)

val SurfaceLight = Color(0xFFF6FBF6)
val OnSurfaceLight = Color(0xFF171D19)
val OnSurfaceVariantLight = Color(0xFF404943)
val OutlineLight = Color(0xFF707972)
val OutlineVariantLight = Color(0xFFC0C9C1)
val SurfaceContainerLowestLight = Color(0xFFFFFFFF)
val SurfaceContainerLowLight = Color(0xFFF0F5F0)
val SurfaceContainerLight = Color(0xFFEAEFEA)
val SurfaceContainerHighLight = Color(0xFFE4EAE4)
val SurfaceContainerHighestLight = Color(0xFFDFE4DF)

val SagePrimaryDark = Color(0xFF8DD5AE)
val SageOnPrimaryDark = Color(0xFF003823)
val SagePrimaryContainerDark = Color(0xFF005234)
val SageOnPrimaryContainerDark = Color(0xFFA9F2C9)

val ClaySecondaryDark = Color(0xFFB5CCBC)
val ClayOnSecondaryDark = Color(0xFF203529)
val ClaySecondaryContainerDark = Color(0xFF374B3F)
val ClayOnSecondaryContainerDark = Color(0xFFD0E8D7)

val SandTertiaryDark = Color(0xFFF8BA7F)
val SandOnTertiaryDark = Color(0xFF4A2800)
val SandTertiaryContainerDark = Color(0xFF673D0E)
val SandOnTertiaryContainerDark = Color(0xFFFFDCBE)

val ErrorDark = Color(0xFFFFB4AB)
val OnErrorDark = Color(0xFF690005)
val ErrorContainerDark = Color(0xFF93000A)
val OnErrorContainerDark = Color(0xFFFFDAD6)

val SurfaceDark = Color(0xFF0F1511)
val OnSurfaceDark = Color(0xFFDFE4DF)
val OnSurfaceVariantDark = Color(0xFFC0C9C1)
val OutlineDark = Color(0xFF8A938C)
val OutlineVariantDark = Color(0xFF404943)
val SurfaceContainerLowestDark = Color(0xFF0A0F0C)
val SurfaceContainerLowDark = Color(0xFF171D19)
val SurfaceContainerDark = Color(0xFF1B211D)
val SurfaceContainerHighDark = Color(0xFF262B27)
val SurfaceContainerHighestDark = Color(0xFF303632)

val CadenceLightColorScheme = lightColorScheme(
    primary = SagePrimaryLight,
    onPrimary = SageOnPrimaryLight,
    primaryContainer = SagePrimaryContainerLight,
    onPrimaryContainer = SageOnPrimaryContainerLight,
    secondary = ClaySecondaryLight,
    onSecondary = ClayOnSecondaryLight,
    secondaryContainer = ClaySecondaryContainerLight,
    onSecondaryContainer = ClayOnSecondaryContainerLight,
    tertiary = SandTertiaryLight,
    onTertiary = SandOnTertiaryLight,
    tertiaryContainer = SandTertiaryContainerLight,
    onTertiaryContainer = SandOnTertiaryContainerLight,
    error = ErrorLight,
    onError = OnErrorLight,
    errorContainer = ErrorContainerLight,
    onErrorContainer = OnErrorContainerLight,
    background = SurfaceLight,
    onBackground = OnSurfaceLight,
    surface = SurfaceLight,
    onSurface = OnSurfaceLight,
    surfaceVariant = SurfaceContainerHighLight,
    onSurfaceVariant = OnSurfaceVariantLight,
    outline = OutlineLight,
    outlineVariant = OutlineVariantLight,
    surfaceContainerLowest = SurfaceContainerLowestLight,
    surfaceContainerLow = SurfaceContainerLowLight,
    surfaceContainer = SurfaceContainerLight,
    surfaceContainerHigh = SurfaceContainerHighLight,
    surfaceContainerHighest = SurfaceContainerHighestLight,
)

val CadenceDarkColorScheme = darkColorScheme(
    primary = SagePrimaryDark,
    onPrimary = SageOnPrimaryDark,
    primaryContainer = SagePrimaryContainerDark,
    onPrimaryContainer = SageOnPrimaryContainerDark,
    secondary = ClaySecondaryDark,
    onSecondary = ClayOnSecondaryDark,
    secondaryContainer = ClaySecondaryContainerDark,
    onSecondaryContainer = ClayOnSecondaryContainerDark,
    tertiary = SandTertiaryDark,
    onTertiary = SandOnTertiaryDark,
    tertiaryContainer = SandTertiaryContainerDark,
    onTertiaryContainer = SandOnTertiaryContainerDark,
    error = ErrorDark,
    onError = OnErrorDark,
    errorContainer = ErrorContainerDark,
    onErrorContainer = OnErrorContainerDark,
    background = SurfaceDark,
    onBackground = OnSurfaceDark,
    surface = SurfaceDark,
    onSurface = OnSurfaceDark,
    surfaceVariant = SurfaceContainerHighDark,
    onSurfaceVariant = OnSurfaceVariantDark,
    outline = OutlineDark,
    outlineVariant = OutlineVariantDark,
    surfaceContainerLowest = SurfaceContainerLowestDark,
    surfaceContainerLow = SurfaceContainerLowDark,
    surfaceContainer = SurfaceContainerDark,
    surfaceContainerHigh = SurfaceContainerHighDark,
    surfaceContainerHighest = SurfaceContainerHighestDark,
)

@Immutable
data class FinancialColors(
    val healthy: Color,
    val healthyContainer: Color,
    val onHealthyContainer: Color,
    val warning: Color,
    val warningContainer: Color,
    val onWarningContainer: Color,
    val danger: Color,
    val dangerContainer: Color,
    val onDangerContainer: Color,
)

val LightFinancialColors = FinancialColors(
    healthy = Color(0xFF286A48),
    healthyContainer = Color(0xFFCCF2DA),
    onHealthyContainer = Color(0xFF0A3820),
    warning = Color(0xFF9C5700),
    warningContainer = Color(0xFFFFE1B6),
    onWarningContainer = Color(0xFF3E2000),
    danger = Color(0xFFBA1A1A),
    dangerContainer = Color(0xFFFFDAD6),
    onDangerContainer = Color(0xFF410002),
)

val DarkFinancialColors = FinancialColors(
    healthy = Color(0xFF8FD8AE),
    healthyContainer = Color(0xFF15492F),
    onHealthyContainer = Color(0xFFCCF2DA),
    warning = Color(0xFFFFC878),
    warningContainer = Color(0xFF5A3300),
    onWarningContainer = Color(0xFFFFE1B6),
    danger = Color(0xFFFFB4AB),
    dangerContainer = Color(0xFF690005),
    onDangerContainer = Color(0xFFFFDAD6),
)

val LocalFinancialColors = staticCompositionLocalOf { LightFinancialColors }

@Immutable
data class BudgetStatusPalette(
    val accent: Color,
    val container: Color,
    val onContainer: Color,
)

@Composable
fun budgetStatusPalette(ratio: Float): BudgetStatusPalette {
    val fin = LocalFinancialColors.current
    return when {
        ratio >= 0.95f -> BudgetStatusPalette(fin.danger, fin.dangerContainer, fin.onDangerContainer)
        ratio >= 0.75f -> BudgetStatusPalette(fin.warning, fin.warningContainer, fin.onWarningContainer)
        else -> BudgetStatusPalette(fin.healthy, fin.healthyContainer, fin.onHealthyContainer)
    }
}
