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
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
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
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.example.R
import com.example.data.CadenceUiState
import com.example.ui.components.formatCurrency
import com.example.ui.theme.ExpressiveMotion

@Composable
fun InsightsScreen(
    state: CadenceUiState,
    contentPadding: PaddingValues,
) {
    val haptics = LocalHapticFeedback.current
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
            val total = state.transactions.filter { it.date == date }.sumOf { it.amount }
            date to total
        }
    }

    var scrubbedIndex by remember(daysCount) { mutableIntStateOf(dailyPoints.lastIndex.coerceAtLeast(0)) }
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

        // 1. Interactive Scrubbable Capsule Spending Trend Chart
        item(key = "trend_chart") {
            val activePoint = dailyPoints.getOrNull(scrubbedIndex)
            val maxVal = (dailyPoints.maxOfOrNull { it.second } ?: 1.0).coerceAtLeast(10.0)
            val activeBarColor = MaterialTheme.colorScheme.primary
            val idleBarColor = MaterialTheme.colorScheme.primary.copy(alpha = 0.26f)
            val periodTotal = remember(dailyPoints) { dailyPoints.sumOf { it.second } }

            Surface(
                modifier = Modifier
                    .fillMaxWidth()
                    .testTag("insights_trend_card"),
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
                        verticalAlignment = Alignment.Bottom,
                    ) {
                        Column {
                            Text(
                                text = activePoint?.first?.formatDayOfWeekShort() ?: "",
                                style = MaterialTheme.typography.labelLarge,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                            )
                            Text(
                                text = formatCurrency(activePoint?.second ?: 0.0, showCents = true),
                                style = MaterialTheme.typography.displaySmall,
                                color = MaterialTheme.colorScheme.onSurface,
                            )
                        }
                        Column(horizontalAlignment = Alignment.End) {
                            Text(
                                text = "$selectedRange total: ${formatCurrency(periodTotal, false)}",
                                style = MaterialTheme.typography.labelLarge,
                                color = MaterialTheme.colorScheme.primary,
                            )
                            Text(
                                text = stringResource(R.string.insights_drag_to_scrub),
                                style = MaterialTheme.typography.labelMedium,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                            )
                        }
                    }

                    Canvas(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(176.dp)
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
                        val slotWidth = size.width / dailyPoints.size
                        val barWidth = (slotWidth * 0.62f).coerceIn(4.dp.toPx(), 26.dp.toPx())
                        val radius = CornerRadius(barWidth / 2f, barWidth / 2f)

                        dailyPoints.forEachIndexed { idx, (_, amount) ->
                            val normalized = (amount / maxVal).toFloat().coerceIn(0.06f, 1f)
                            val barHeight = (size.height * normalized * entranceProgress.value)
                                .coerceAtLeast(barWidth)
                            val x = idx * slotWidth + (slotWidth - barWidth) / 2f
                            val y = size.height - barHeight

                            drawRoundRect(
                                color = if (idx == scrubbedIndex) activeBarColor else idleBarColor,
                                topLeft = Offset(x, y),
                                size = Size(barWidth, barHeight),
                                cornerRadius = radius,
                            )
                        }
                    }
                }
            }
        }

        // 2. Category Distribution Proportional Pill Bar + Breakdown
        item(key = "category_mix") {
            val breakdown = remember(state.categories, state.transactions) {
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
                    Text(
                        text = stringResource(R.string.insights_category_mix),
                        style = MaterialTheme.typography.titleLarge,
                    )

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
                                    ) {
                                        Box(
                                            modifier = Modifier
                                                .size(12.dp)
                                                .clip(CircleShape)
                                                .background(cat.accentColor),
                                        )
                                        Text(cat.name, style = MaterialTheme.typography.titleMedium)
                                    }
                                    Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
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
