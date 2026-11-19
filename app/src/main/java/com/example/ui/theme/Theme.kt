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
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp

/**
 * Centralized Material 3 Expressive spring physics tokens.
 * Spatial springs use lively overshoot for layout expansion, morphing, and charts;
 * Effects springs are critically damped for crisp color and alpha shifts.
 */
object ExpressiveMotion {
    fun <T> fastSpatial(): SpringSpec<T> = spring(
        dampingRatio = 0.72f,
        stiffness = 650f,
    )

    fun <T> defaultSpatial(): SpringSpec<T> = spring(
        dampingRatio = 0.74f,
        stiffness = 380f,
    )

    fun <T> slowSpatial(): SpringSpec<T> = spring(
        dampingRatio = 0.78f,
        stiffness = 220f,
    )

    fun <T> fastEffects(): SpringSpec<T> = spring(
        dampingRatio = Spring.DampingRatioNoBouncy,
        stiffness = 600f,
    )

    fun <T> defaultEffects(): SpringSpec<T> = spring(
        dampingRatio = Spring.DampingRatioNoBouncy,
        stiffness = 320f,
    )
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
    content: @Composable () -> Unit,
) {
    val context = LocalContext.current
    val colorScheme = when {
        dynamicColor && Build.VERSION.SDK_INT >= Build.VERSION_CODES.S -> {
            if (darkTheme) dynamicDarkColorScheme(context) else dynamicLightColorScheme(context)
        }
        darkTheme -> CadenceDarkColorScheme
        else -> CadenceLightColorScheme
    }

    val financialColors = if (darkTheme) DarkFinancialColors else LightFinancialColors

    CompositionLocalProvider(LocalFinancialColors provides financialColors) {
        MaterialTheme(
            colorScheme = colorScheme,
            shapes = CadenceShapes,
            typography = CadenceTypography,
            content = content,
        )
    }
}
