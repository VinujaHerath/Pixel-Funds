package com.example.ui.theme

import android.os.Build
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.SpringSpec
import androidx.compose.animation.core.spring
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Shapes
import androidx.compose.material3.dynamicDarkColorScheme
import androidx.compose.material3.dynamicLightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.remember
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp

/**
 * Centralized Material 3 Expressive spring physics tokens tuned for crisp 120Hz Pixel displays.
 * All specs are cached as singletons to avoid per-recomposition allocations.
 */
object ExpressiveMotion {
    private val fastSpatialSpec = spring<Any>(
        dampingRatio = 0.78f,
        stiffness = 850f,
    )

    private val defaultSpatialSpec = spring<Any>(
        dampingRatio = 0.80f,
        stiffness = 520f,
    )

    private val slowSpatialSpec = spring<Any>(
        dampingRatio = 0.84f,
        stiffness = 320f,
    )

    private val fastEffectsSpec = spring<Any>(
        dampingRatio = Spring.DampingRatioNoBouncy,
        stiffness = 900f,
    )

    private val defaultEffectsSpec = spring<Any>(
        dampingRatio = Spring.DampingRatioNoBouncy,
        stiffness = 500f,
    )

    @Suppress("UNCHECKED_CAST")
    fun <T> fastSpatial(): SpringSpec<T> = fastSpatialSpec as SpringSpec<T>

    @Suppress("UNCHECKED_CAST")
    fun <T> defaultSpatial(): SpringSpec<T> = defaultSpatialSpec as SpringSpec<T>

    @Suppress("UNCHECKED_CAST")
    fun <T> slowSpatial(): SpringSpec<T> = slowSpatialSpec as SpringSpec<T>

    @Suppress("UNCHECKED_CAST")
    fun <T> fastEffects(): SpringSpec<T> = fastEffectsSpec as SpringSpec<T>

    @Suppress("UNCHECKED_CAST")
    fun <T> defaultEffects(): SpringSpec<T> = defaultEffectsSpec as SpringSpec<T>
}

val CadenceShapes = Shapes(
    small = RoundedCornerShape(12.dp),
    medium = RoundedCornerShape(20.dp),
    large = RoundedCornerShape(28.dp),
    extraLarge = RoundedCornerShape(36.dp),
)

@Composable
fun CadenceTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    dynamicColor: Boolean = true,
    preferences: com.example.data.UserPreferences = com.example.data.UserPreferences(),
    content: @Composable () -> Unit,
) {
    val context = LocalContext.current
    val colorScheme = remember(context, darkTheme, dynamicColor) {
        when {
            dynamicColor && Build.VERSION.SDK_INT >= Build.VERSION_CODES.S -> {
                if (darkTheme) dynamicDarkColorScheme(context) else dynamicLightColorScheme(context)
            }
            darkTheme -> CadenceDarkColorScheme
            else -> CadenceLightColorScheme
        }
    }

    val financialColors = if (darkTheme) DarkFinancialColors else LightFinancialColors

    CompositionLocalProvider(
        LocalFinancialColors provides financialColors,
        LocalWarningThreshold provides preferences.warningThresholdRatio,
        com.example.ui.components.LocalAppCurrency provides preferences.currency,
        com.example.ui.components.LocalShowDecimals provides preferences.showDecimals,
        com.example.ui.components.LocalHapticsEnabled provides preferences.hapticsEnabled,
        com.example.ui.components.LocalReduceMotion provides preferences.reduceMotion,
    ) {
        MaterialTheme(
            colorScheme = colorScheme,
            shapes = CadenceShapes,
            typography = CadenceTypography,
            content = content,
        )
    }
}
