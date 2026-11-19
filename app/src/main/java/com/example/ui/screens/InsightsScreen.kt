package com.example.ui.screens

import androidx.compose.animation.core.Animatable
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.detectHorizontalDragGestures
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.PathEffect
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.example.R
import com.example.data.CadenceUiState
import com.example.data.LocalDay
import com.example.ui.components.LocalAppCurrency
import com.example.ui.components.formatCurrency
import com.example.ui.components.formatCurrencyValue
import com.example.ui.theme.ExpressiveMotion
import kotlin.math.ceil
import kotlin.math.floor
import kotlin.math.log10
import kotlin.math.pow

private data class DailySpendPoint(
    val date: LocalDay,
    val total: Double,
    val txCount: Int,
)

/**
 * Rounds an arbitrary chart maximum up to a clean, human-readable axis ceiling
 * (e.g., 10, 20, 50, 100, 250, 500, 1000, 2500, 5000, ...).
 */
private fun niceAxisCeiling(rawMax: Double): Double {
    if (rawMax <= 0.0) return 100.0
    val exponent = floor(log10(rawMax))
    val magnitude = 10.0.pow(exponent)
    val fraction = rawMax / magnitude
    val niceFraction = when {
        fraction <= 1.0 -> 1.0
        fraction <= 1.5 -> 1.5
        fraction <= 2.0 -> 2.0
        fraction <= 2.5 -> 2.5
        fraction <= 3.0 -> 3.0
        fraction <= 4.0 -> 4.0
        fraction <= 5.0 -> 5.0
        fraction <= 6.0 -> 6.0
        fraction <= 8.0 -> 8.0
        else -> 10.0
    }
    return ceil(niceFraction * magnitude).coerceAtLeast(10.0)
}

@Composable
fun InsightsScreen(
    state: CadenceUiState,
    contentPadding: PaddingValues,
) {
    val haptics = LocalHapticFeedback.current
    val currency = LocalAppCurrency.current
    var selectedRange by remember { mutableStateOf("7D") }
    val ranges = remember { listOf("7D", "14D", "30D") }
    val daysCount = when (selectedRange) {
        "7D" -> 7
        "14D" -> 14
        else -> 30
    }

    val dailyPoints = remember(state.transactions, daysCount, state.today) {
        (daysCount - 1 downTo 0).map { offset ->
            val date = state.today.minusDays(offset.toLong())
            val matching = state.transactions.filter { it.date == date }
            DailySpendPoint(
                date = date,
                total = matching.sumOf { it.amount },
                txCount = matching.size,
            )
        }
    }

    var scrubbedIndex by remember(daysCount) {
        val lastNonZero = dailyPoints.indexOfLast { it.total > 0.0 }
        mutableIntStateOf(if (lastNonZero >= 0) lastNonZero else dailyPoints.lastIndex.coerceAtLeast(0))
    }
    val entranceProgress = remember(daysCount) { Animatable(0f) }

    LaunchedEffect(daysCount) {
        entranceProgress.snapTo(0f)
        entranceProgress.animateTo(1f, animationSpec = ExpressiveMotion.slowSpatial())
    }

    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .testTag("insights_list"),
        contentPadding = contentPadding,
        verticalArrangement = Arrangement.spacedBy(16.dp),
    ) {
        item(key = "insights_header") {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 4.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Text(
                    text = stringResource(R.string.insights_title),
                    style = MaterialTheme.typography.headlineMedium,
                )
                Surface(
                    shape = CircleShape,
                    color = MaterialTheme.colorScheme.surfaceContainerHigh,
                ) {
                    Row(modifier = Modifier.padding(4.dp)) {
                        ranges.forEach { range ->
                            val active = range == selectedRange
                            Surface(
                                onClick = {
                                    haptics.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                                    selectedRange = range
                                },
                                shape = CircleShape,
                                color = if (active) MaterialTheme.colorScheme.primary
                                else MaterialTheme.colorScheme.surfaceContainerHigh,
                                modifier = Modifier.testTag("insights_range_$range"),
                            ) {
                                Text(
                                    text = range,
                                    style = MaterialTheme.typography.labelLarge,
                                    color = if (active) MaterialTheme.colorScheme.onPrimary
                                    else MaterialTheme.colorScheme.onSurfaceVariant,
                                    modifier = Modifier.padding(horizontal = 14.dp, vertical = 8.dp),
                                )
                            }
                        }
                    }
                }
            }
        }

        // 1. Readable Interactive Daily Spending Chart
        item(key = "trend_chart") {
            val activePoint = dailyPoints.getOrNull(scrubbedIndex)
            val periodTotal = remember(dailyPoints) { dailyPoints.sumOf { it.total } }
            val dailyAverage = remember(dailyPoints) {
                if (dailyPoints.isNotEmpty()) periodTotal / dailyPoints.size else 0.0
            }
            val peakPoint = remember(dailyPoints) { dailyPoints.maxByOrNull { it.total } }
            val rawMax = remember(dailyPoints) { dailyPoints.maxOfOrNull { it.total } ?: 0.0 }
            val axisMax = remember(rawMax) { niceAxisCeiling(rawMax * 1.12) }
            val axisMid = axisMax / 2.0

            val activeBarColor = MaterialTheme.colorScheme.primary
            val idleBarColor = MaterialTheme.colorScheme.primary.copy(alpha = 0.34f)
            val zeroDotColor = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.65f)
            val highlightColColor = MaterialTheme.colorScheme.primary.copy(alpha = 0.09f)
            val gridLineColor = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.42f)
            val averageLineColor = MaterialTheme.colorScheme.tertiary

            Surface(
                modifier = Modifier
                    .fillMaxWidth()
                    .testTag("insights_trend_card"),
                shape = RoundedCornerShape(32.dp),
                color = MaterialTheme.colorScheme.surfaceContainerLow,
            ) {
                Column(
                    modifier = Modifier.padding(20.dp),
                    verticalArrangement = Arrangement.spacedBy(16.dp),
                ) {
                    // Selected Day Hero Readout
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.Top,
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(8.dp),
                            ) {
                                Text(
                                    text = when (activePoint?.date) {
                                        state.today -> "${stringResource(R.string.entry_today)} • ${activePoint.date.formatShort()}"
                                        state.today.minusDays(1) -> "Yesterday • ${activePoint.date.formatShort()}"
                                        else -> activePoint?.date?.formatDayOfWeekShort() ?: ""
                                    },
                                    style = MaterialTheme.typography.labelLarge,
                                    color = MaterialTheme.colorScheme.primary,
                                )
                            }
                            Text(
                                text = formatCurrency(activePoint?.total ?: 0.0, showCents = true),
                                style = MaterialTheme.typography.displaySmall,
                                color = MaterialTheme.colorScheme.onSurface,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis,
                            )
                        }
                        Surface(
                            shape = CircleShape,
                            color = MaterialTheme.colorScheme.surfaceContainerHigh,
                        ) {
                            val count = activePoint?.txCount ?: 0
                            Text(
                                text = when (count) {
                                    0 -> stringResource(R.string.insights_no_expenses_day)
                                    1 -> stringResource(R.string.insights_one_expense_day)
                                    else -> stringResource(R.string.insights_many_expenses_day, count)
                                },
                                style = MaterialTheme.typography.labelMedium,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp),
                            )
                        }
                    }

                    // 3-Metric Quick Summary Strip (Total, Daily Avg, Peak)
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                    ) {
                        ChartStatChip(
                            label = "$selectedRange total",
                            value = formatCurrencyValue(periodTotal, currency, showCents = false),
                            modifier = Modifier.weight(1f),
                        )
                        ChartStatChip(
                            label = stringResource(R.string.insights_daily_avg),
                            value = formatCurrencyValue(dailyAverage, currency, showCents = false),
                            accentColor = averageLineColor,
                            modifier = Modifier.weight(1f),
                        )
                        ChartStatChip(
                            label = stringResource(R.string.insights_peak_day),
                            value = formatCurrencyValue(peakPoint?.total ?: 0.0, currency, showCents = false),
                            modifier = Modifier.weight(1f),
                        )
                    }

                    // Plot Area with Y-Axis Scale + Gridlines + Average Line + Interactive Bars
                    val yMaxLabel = remember(axisMax, currency) {
                        formatCurrencyValue(axisMax, currency, showCents = false, compact = true)
                    }
                    val yMidLabel = remember(axisMid, currency) {
                        formatCurrencyValue(axisMid, currency, showCents = false, compact = true)
                    }
                    val yZeroLabel = remember(currency) {
                        formatCurrencyValue(0.0, currency, showCents = false, compact = true)
                    }

                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(196.dp),
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        // Left Y-Axis Scale Column
                        Column(
                            modifier = Modifier
                                .fillMaxHeight()
                                .padding(top = 6.dp, bottom = 6.dp, end = 8.dp),
                            verticalArrangement = Arrangement.SpaceBetween,
                            horizontalAlignment = Alignment.End,
                        ) {
                            Text(
                                text = yMaxLabel,
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                maxLines = 1,
                            )
                            Text(
                                text = yMidLabel,
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                maxLines = 1,
                            )
                            Text(
                                text = yZeroLabel,
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                maxLines = 1,
                            )
                        }

                        // Interactive Canvas Plot
                        Canvas(
                            modifier = Modifier
                                .weight(1f)
                                .fillMaxHeight()
                                .pointerInput(dailyPoints) {
                                    detectTapGestures { offset ->
                                        if (dailyPoints.isNotEmpty() && size.width > 0) {
                                            val idx = ((offset.x / size.width) * dailyPoints.size)
                                                .toInt()
                                                .coerceIn(0, dailyPoints.lastIndex)
                                            if (idx != scrubbedIndex) {
                                                scrubbedIndex = idx
                                                haptics.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                                            }
                                        }
                                    }
                                }
                                .pointerInput(dailyPoints) {
                                    detectHorizontalDragGestures { change, _ ->
                                        if (dailyPoints.isNotEmpty() && size.width > 0) {
                                            val idx = ((change.position.x / size.width) * dailyPoints.size)
                                                .toInt()
                                                .coerceIn(0, dailyPoints.lastIndex)
                                            if (idx != scrubbedIndex) {
                                                scrubbedIndex = idx
                                                haptics.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                                            }
                                        }
                                    }
                                }
                                .testTag("insights_trend_canvas"),
                        ) {
                            if (dailyPoints.isEmpty()) return@Canvas

                            val topPad = 12.dp.toPx()
                            val bottomPad = 10.dp.toPx()
                            val chartHeight = (size.height - topPad - bottomPad).coerceAtLeast(1f)
                            val baselineY = topPad + chartHeight
                            val dashEffect = PathEffect.dashPathEffect(floatArrayOf(8f, 8f), 0f)

                            // Horizontal Reference Gridlines (Max, Mid, Zero)
                            listOf(topPad, topPad + chartHeight / 2f, baselineY).forEachIndexed { lineIdx, y ->
                                drawLine(
                                    color = gridLineColor,
                                    start = Offset(0f, y),
                                    end = Offset(size.width, y),
                                    strokeWidth = if (lineIdx == 2) 1.5.dp.toPx() else 1.dp.toPx(),
                                    pathEffect = if (lineIdx == 2) null else dashEffect,
                                )
                            }

                            val slotWidth = size.width / dailyPoints.size
                            val barWidth = when (daysCount) {
                                7 -> (slotWidth * 0.48f).coerceIn(14.dp.toPx(), 26.dp.toPx())
                                14 -> (slotWidth * 0.56f).coerceIn(8.dp.toPx(), 16.dp.toPx())
                                else -> (slotWidth * 0.64f).coerceIn(4.dp.toPx(), 10.dp.toPx())
                            }
                            val radius = CornerRadius(barWidth / 2f, barWidth / 2f)

                            // Subtle Vertical Highlight Band behind Active Day
                            if (scrubbedIndex in dailyPoints.indices) {
                                val bandWidth = slotWidth.coerceAtLeast(barWidth + 8.dp.toPx())
                                val bandX = (scrubbedIndex * slotWidth + (slotWidth - bandWidth) / 2f)
                                    .coerceIn(0f, (size.width - bandWidth).coerceAtLeast(0f))
                                drawRoundRect(
                                    color = highlightColColor,
                                    topLeft = Offset(bandX, topPad - 4.dp.toPx()),
                                    size = Size(bandWidth, chartHeight + 8.dp.toPx()),
                                    cornerRadius = CornerRadius(10.dp.toPx(), 10.dp.toPx()),
                                )
                            }

                            // Daily Spend Bars (with true zero-day dots so empty days never look misleading)
                            dailyPoints.forEachIndexed { idx, point ->
                                val isSelected = idx == scrubbedIndex
                                val centerX = idx * slotWidth + slotWidth / 2f

                                if (point.total <= 0.001) {
                                    val dotRadius = if (isSelected) 4.dp.toPx() else 2.5.dp.toPx()
                                    drawCircle(
                                        color = if (isSelected) activeBarColor else zeroDotColor,
                                        radius = dotRadius,
                                        center = Offset(centerX, baselineY - dotRadius - 2.dp.toPx()),
                                    )
                                } else {
                                    val normalized = (point.total / axisMax).toFloat().coerceIn(0.04f, 1f)
                                    val barHeight = (chartHeight * normalized * entranceProgress.value)
                                        .coerceAtLeast(6.dp.toPx())
                                    val x = centerX - barWidth / 2f
                                    val y = baselineY - barHeight

                                    drawRoundRect(
                                        color = if (isSelected) activeBarColor else idleBarColor,
                                        topLeft = Offset(x, y),
                                        size = Size(barWidth, barHeight),
                                        cornerRadius = radius,
                                    )

                                    if (isSelected) {
                                        drawCircle(
                                            color = activeBarColor,
                                            radius = 4.5.dp.toPx(),
                                            center = Offset(centerX, (y - 6.dp.toPx()).coerceAtLeast(4.dp.toPx())),
                                        )
                                    }
                                }
                            }

                            // Dashed Daily Average Reference Line
                            if (dailyAverage > 0.0) {
                                val avgFrac = (dailyAverage / axisMax).toFloat().coerceIn(0.02f, 0.98f)
                                val avgY = baselineY - chartHeight * avgFrac
                                drawLine(
                                    color = averageLineColor,
                                    start = Offset(0f, avgY),
                                    end = Offset(size.width, avgY),
                                    strokeWidth = 1.8.dp.toPx(),
                                    pathEffect = PathEffect.dashPathEffect(floatArrayOf(12f, 8f), 0f),
                                )
                            }
                        }
                    }

                    // X-Axis Date Labels Aligned Under the Plot
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(start = 42.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                    ) {
                        if (daysCount == 7) {
                            dailyPoints.forEachIndexed { idx, point ->
                                val isSelected = idx == scrubbedIndex
                                Text(
                                    text = if (point.date == state.today) stringResource(R.string.entry_today)
                                    else point.date.dayOfWeekShort(),
                                    style = MaterialTheme.typography.labelSmall,
                                    fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal,
                                    color = if (isSelected) MaterialTheme.colorScheme.primary
                                    else MaterialTheme.colorScheme.onSurfaceVariant,
                                    textAlign = TextAlign.Center,
                                    maxLines = 1,
                                    modifier = Modifier.weight(1f),
                                )
                            }
                        } else {
                            val tickIndices = remember(daysCount) {
                                listOf(0, daysCount / 3, (daysCount * 2) / 3, daysCount - 1)
                            }
                            tickIndices.forEachIndexed { pos, idx ->
                                val point = dailyPoints.getOrNull(idx)
                                val label = if (point?.date == state.today) {
                                    stringResource(R.string.entry_today)
                                } else {
                                    point?.date?.formatShort().orEmpty()
                                }
                                Text(
                                    text = label,
                                    style = MaterialTheme.typography.labelSmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                                    textAlign = when (pos) {
                                        0 -> TextAlign.Start
                                        tickIndices.lastIndex -> TextAlign.End
                                        else -> TextAlign.Center
                                    },
                                    maxLines = 1,
                                    modifier = Modifier.weight(1f),
                                )
                            }
                        }
                    }

                    // Compact Legend + Interaction Hint
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(12.dp),
                        ) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(5.dp),
                            ) {
                                Box(
                                    modifier = Modifier
                                        .size(8.dp)
                                        .clip(CircleShape)
                                        .background(activeBarColor),
                                )
                                Text(
                                    text = stringResource(R.string.insights_legend_daily),
                                    style = MaterialTheme.typography.labelSmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                                )
                            }
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(5.dp),
                            ) {
                                Box(
                                    modifier = Modifier
                                        .width(12.dp)
                                        .height(2.5.dp)
                                        .clip(CircleShape)
                                        .background(averageLineColor),
                                )
                                Text(
                                    text = stringResource(R.string.insights_legend_avg),
                                    style = MaterialTheme.typography.labelSmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                                )
                            }
                        }
                        Text(
                            text = stringResource(R.string.insights_drag_to_scrub),
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                    }
                }
            }
        }

        // 2. Category Distribution Proportional Pill Bar + Breakdown (Synced to Active Cycle)
        item(key = "category_mix") {
            val breakdown = remember(state.categories, state.transactions, state.today, state.budgetResetDay) {
                state.categories
                    .map { it to state.spentForCategoryThisMonth(it.id) }
                    .filter { it.second > 0.0 }
                    .sortedByDescending { it.second }
            }
            val total = breakdown.sumOf { it.second }.coerceAtLeast(1.0)

            Surface(
                modifier = Modifier
                    .fillMaxWidth()
                    .testTag("insights_category_mix_card"),
                shape = RoundedCornerShape(32.dp),
                color = MaterialTheme.colorScheme.surfaceContainerLow,
            ) {
                Column(
                    modifier = Modifier.padding(22.dp),
                    verticalArrangement = Arrangement.spacedBy(16.dp),
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        Text(
                            text = stringResource(R.string.insights_category_mix),
                            style = MaterialTheme.typography.titleLarge,
                        )
                        Text(
                            text = "${state.cycleStart.formatShort()} – ${state.cycleEnd.formatShort()}",
                            style = MaterialTheme.typography.labelMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                    }

                    if (breakdown.isEmpty()) {
                        Text(
                            text = stringResource(R.string.insights_empty_mix),
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                    } else {
                        // Organic Segmented Pill Strip
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(22.dp),
                            horizontalArrangement = Arrangement.spacedBy(4.dp),
                        ) {
                            breakdown.forEach { (cat, spent) ->
                                val share = (spent / total).toFloat().coerceAtLeast(0.05f)
                                Box(
                                    modifier = Modifier
                                        .weight(share)
                                        .fillMaxHeight()
                                        .clip(CircleShape)
                                        .background(cat.accentColor),
                                )
                            }
                        }

                        Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                            breakdown.forEach { (cat, spent) ->
                                val pct = ((spent / total) * 100).toInt()
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                ) {
                                    Row(
                                        verticalAlignment = Alignment.CenterVertically,
                                        horizontalArrangement = Arrangement.spacedBy(10.dp),
                                        modifier = Modifier.weight(1f),
                                    ) {
                                        Box(
                                            modifier = Modifier
                                                .size(32.dp)
                                                .clip(CircleShape)
                                                .background(cat.accentColor.copy(alpha = 0.16f)),
                                            contentAlignment = Alignment.Center,
                                        ) {
                                            Icon(
                                                imageVector = cat.icon,
                                                contentDescription = null,
                                                tint = cat.accentColor,
                                                modifier = Modifier.size(16.dp),
                                            )
                                        }
                                        Text(
                                            text = cat.name,
                                            style = MaterialTheme.typography.titleMedium,
                                            maxLines = 1,
                                            overflow = TextOverflow.Ellipsis,
                                        )
                                    }
                                    Spacer(Modifier.width(8.dp))
                                    Row(
                                        verticalAlignment = Alignment.CenterVertically,
                                        horizontalArrangement = Arrangement.spacedBy(10.dp),
                                    ) {
                                        Text(
                                            text = "$pct%",
                                            style = MaterialTheme.typography.labelLarge,
                                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                                        )
                                        Text(
                                            text = formatCurrency(spent, showCents = false),
                                            style = MaterialTheme.typography.titleMedium,
                                        )
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun ChartStatChip(
    label: String,
    value: String,
    modifier: Modifier = Modifier,
    accentColor: androidx.compose.ui.graphics.Color? = null,
) {
    Surface(
        modifier = modifier,
        shape = RoundedCornerShape(18.dp),
        color = MaterialTheme.colorScheme.surfaceContainerHigh,
    ) {
        Column(
            modifier = Modifier.padding(horizontal = 12.dp, vertical = 10.dp),
            verticalArrangement = Arrangement.spacedBy(2.dp),
        ) {
            Text(
                text = label,
                style = MaterialTheme.typography.labelSmall,
                color = accentColor ?: MaterialTheme.colorScheme.onSurfaceVariant,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
            Text(
                text = value,
                style = MaterialTheme.typography.titleSmall,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onSurface,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
        }
    }
}
