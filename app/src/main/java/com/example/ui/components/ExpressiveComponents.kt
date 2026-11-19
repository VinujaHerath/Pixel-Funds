package com.example.ui.components

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.animation.expandHorizontally
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.shrinkHorizontally
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Add
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.example.R
import com.example.data.CadenceTab
import com.example.ui.theme.ExpressiveMotion
import java.util.Locale
import kotlin.math.PI
import kotlin.math.abs
import kotlin.math.sin

import androidx.compose.runtime.compositionLocalOf
import com.example.data.CurrencyOption
import com.example.data.WorldCurrencyCatalog

val LocalAppCurrency = compositionLocalOf { WorldCurrencyCatalog.defaultCurrency() }

fun formatCurrencyValue(
    amount: Double,
    currency: CurrencyOption,
    showCents: Boolean = true,
    compact: Boolean = false,
): String {
    val sign = if (amount < 0) "-" else ""
    val value = abs(amount)
    val prefix = if (currency.usesTightPrefix) currency.symbol else "${currency.symbol} "

    if (compact) {
        val compactNum = when {
            value >= 1_000_000.0 -> {
                val m = value / 1_000_000.0
                if (m >= 10.0 || m % 1.0 == 0.0) String.format(Locale.US, "%.0fM", m)
                else String.format(Locale.US, "%.1fM", m)
            }
            value >= 1_000.0 -> {
                val k = value / 1_000.0
                if (k >= 10.0 || k % 1.0 == 0.0) String.format(Locale.US, "%.0fK", k)
                else String.format(Locale.US, "%.1fK", k)
            }
            else -> String.format(Locale.US, "%,.0f", value)
        }
        return "$sign$prefix$compactNum"
    }

    val decimals = if (showCents) currency.fractionDigits else 0
    val formatted = when (decimals) {
        0 -> String.format(Locale.US, "%,.0f", value)
        3 -> String.format(Locale.US, "%,.3f", value)
        else -> String.format(Locale.US, "%,.2f", value)
    }
    return "$sign$prefix$formatted"
}

@Composable
fun formatCurrency(amount: Double, showCents: Boolean = true): String {
    val currency = LocalAppCurrency.current
    return formatCurrencyValue(amount = amount, currency = currency, showCents = showCents)
}

/**
 * Material 3 Expressive organic wavy progress indicator with optional vertical elapsed-day marker.
 */
@Composable
fun WavyProgressIndicator(
    progress: Float,
    color: Color,
    trackColor: Color,
    modifier: Modifier = Modifier,
    strokeWidth: Dp = 5.dp,
    waveAmplitude: Dp = 2.6.dp,
    wavelength: Dp = 26.dp,
    markerFraction: Float? = null,
    markerColor: Color = MaterialTheme.colorScheme.onSurface,
) {
    val clamped = progress.coerceIn(0f, 1f)
    val infiniteTransition = rememberInfiniteTransition(label = "WavyPhase")
    val phase by infiniteTransition.animateFloat(
        initialValue = 0f,
        targetValue = (2.0 * PI).toFloat(),
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = 2800, easing = LinearEasing),
            repeatMode = RepeatMode.Restart,
        ),
        label = "WavyPhaseAngle",
    )

    Canvas(modifier = modifier) {
        val strokePx = strokeWidth.toPx()
        val ampPx = waveAmplitude.toPx()
        val waveLenPx = wavelength.toPx().coerceAtLeast(12f)
        val centerY = size.height / 2f
        val startX = strokePx / 2f
        val endX = (size.width - strokePx / 2f).coerceAtLeast(startX)
        val usableWidth = (endX - startX).coerceAtLeast(1f)
        val fillEndX = startX + usableWidth * clamped
        val gapPx = if (clamped in 0.02f..0.98f) strokePx * 1.4f else 0f

        // Track portion (flat rounded bar from fillEnd + gap to end)
        val trackStartX = (fillEndX + gapPx).coerceAtMost(endX)
        if (trackStartX < endX) {
            drawLine(
                color = trackColor,
                start = Offset(trackStartX, centerY),
                end = Offset(endX, centerY),
                strokeWidth = strokePx,
                cap = StrokeCap.Round,
            )
        }

        // Active organic sine wave portion
        if (clamped > 0.005f) {
            val path = Path()
            val stepPx = 3f
            var x = startX
            var first = true
            val effectiveAmp = if (clamped > 0.04f) ampPx else ampPx * (clamped / 0.04f)

            while (x <= fillEndX) {
                val angle = ((x - startX) / waveLenPx) * (2.0 * PI).toFloat() - phase
                // Smoothly taper amplitude near ends so caps join cleanly
                val distFromStart = (x - startX).coerceAtLeast(0f)
                val distFromEnd = (fillEndX - x).coerceAtLeast(0f)
                val taper = (minOf(distFromStart, distFromEnd) / (waveLenPx * 0.35f)).coerceIn(0f, 1f)
                val y = centerY + sin(angle) * effectiveAmp * taper
                if (first) {
                    path.moveTo(x, y)
                    first = false
                } else {
                    path.lineTo(x, y)
                }
                x += stepPx
            }
            if (fillEndX > startX) {
                path.lineTo(fillEndX, centerY)
            }

            drawPath(
                path = path,
                color = color,
                style = Stroke(
                    width = strokePx,
                    cap = StrokeCap.Round,
                    join = StrokeJoin.Round,
                ),
            )
        }

        // Optional elapsed-month vertical marker
        if (markerFraction != null) {
            val mx = startX + usableWidth * markerFraction.coerceIn(0.04f, 0.96f)
            drawLine(
                color = markerColor,
                start = Offset(mx, 2.dp.toPx()),
                end = Offset(mx, size.height - 2.dp.toPx()),
                strokeWidth = 3.dp.toPx(),
                cap = StrokeCap.Round,
            )
        }
    }
}

/**
 * Material 3 Expressive Floating Pill Navigation Toolbar paired with a morphing Vibrant FAB.
 */
@Composable
fun FloatingPillToolbar(
    activeTab: CadenceTab,
    onSelectTab: (CadenceTab) -> Unit,
    onAddExpense: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val haptics = LocalHapticFeedback.current
    val fabInteraction = remember { MutableInteractionSource() }
    val isFabPressed by fabInteraction.collectIsPressedAsState()
    val fabCorner by animateDpAsState(
        targetValue = if (isFabPressed) 14.dp else 28.dp,
        animationSpec = ExpressiveMotion.fastSpatial(),
        label = "FabShapeMorph",
    )

    Row(
        modifier = modifier
            .navigationBarsPadding()
            .padding(horizontal = 16.dp, vertical = 14.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(10.dp),
    ) {
        Surface(
            shape = CircleShape,
            color = MaterialTheme.colorScheme.surfaceContainerHighest,
            tonalElevation = 4.dp,
            shadowElevation = 6.dp,
        ) {
            Row(
                modifier = Modifier.padding(horizontal = 6.dp, vertical = 6.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(4.dp),
            ) {
                CadenceTab.entries.forEach { tab ->
                    val selected = tab == activeTab
                    val pillCorner by animateDpAsState(
                        targetValue = if (selected) 24.dp else 14.dp,
                        animationSpec = ExpressiveMotion.fastSpatial(),
                        label = "NavPillMorph_${tab.name}",
                    )
                    Box(
                        modifier = Modifier
                            .heightIn(min = 48.dp)
                            .clip(RoundedCornerShape(pillCorner))
                            .background(
                                if (selected) MaterialTheme.colorScheme.secondaryContainer
                                else Color.Transparent
                            )
                            .clickable {
                                haptics.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                                onSelectTab(tab)
                            }
                            .padding(horizontal = if (selected) 16.dp else 13.dp, vertical = 10.dp)
                            .testTag("nav_tab_${tab.name.lowercase()}"),
                        contentAlignment = Alignment.Center,
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(6.dp),
                        ) {
                            Icon(
                                imageVector = tab.icon,
                                contentDescription = tab.label,
                                tint = if (selected) MaterialTheme.colorScheme.onSecondaryContainer
                                else MaterialTheme.colorScheme.onSurfaceVariant,
                                modifier = Modifier.size(22.dp),
                            )
                            AnimatedVisibility(
                                visible = selected,
                                enter = expandHorizontally(ExpressiveMotion.fastSpatial()) +
                                    fadeIn(ExpressiveMotion.fastEffects()),
                                exit = shrinkHorizontally(ExpressiveMotion.fastSpatial()) +
                                    fadeOut(ExpressiveMotion.fastEffects()),
                            ) {
                                Text(
                                    text = tab.label,
                                    style = MaterialTheme.typography.labelLarge,
                                    color = MaterialTheme.colorScheme.onSecondaryContainer,
                                )
                            }
                        }
                    }
                }
            }
        }

        // Morphing Vibrant Add Expense FAB
        Surface(
            onClick = {
                haptics.performHapticFeedback(HapticFeedbackType.LongPress)
                onAddExpense()
            },
            interactionSource = fabInteraction,
            shape = RoundedCornerShape(fabCorner),
            color = MaterialTheme.colorScheme.primary,
            contentColor = MaterialTheme.colorScheme.onPrimary,
            shadowElevation = 6.dp,
            modifier = Modifier
                .size(58.dp)
                .testTag("add_expense_fab"),
        ) {
            Box(contentAlignment = Alignment.Center, modifier = Modifier.fillMaxSize()) {
                Icon(
                    imageVector = Icons.Rounded.Add,
                    contentDescription = stringResource(R.string.cd_add_expense),
                    modifier = Modifier.size(26.dp),
                )
            }
        }
    }
}
