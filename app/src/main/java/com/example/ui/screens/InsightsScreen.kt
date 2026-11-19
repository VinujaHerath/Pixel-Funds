package com.example.ui.screens

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.slideOutHorizontally
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
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
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
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
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.PathEffect
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.input.pointer.pointerInput
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
import com.example.ui.components.LocalReduceMotion
import com.example.ui.components.formatCurrency
import com.example.ui.components.formatCurrencyValue
import com.example.ui.components.rememberPixelHaptics
import com.example.ui.theme.ExpressiveMotion
import com.example.ui.theme.LocalFinancialColors
import kotlin.math.abs
import kotlin.math.ceil
import kotlin.math.floor
import kotlin.math.log10
import kotlin.math.pow
import kotlin.math.roundToInt

private enum class InsightsMode(val label: String) {
    TRENDS("Trends"),
    MONTHLY("Monthly"),
    YEARLY("Yearly"),
    BREAKDOWN("Mix"),
}

private data class DailySpendPoint(
    val date: LocalDay,
    val total: Double,
    val txCount: Int,
)

/**
 * Rounds an arbitrary chart maximum up to a clean, human-readable axis ceiling.
 */
internal fun niceAxisCeiling(rawMax: Double): Double {
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
    val performHaptic = rememberPixelHaptics()
    val reduceMotion = LocalReduceMotion.current
    var activeMode by remember { mutableStateOf(InsightsMode.TRENDS) }

    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .testTag("insights_list"),
        contentPadding = contentPadding,
        verticalArrangement = Arrangement.spacedBy(16.dp),
    ) {
        item(key = "insights_header_and_modes") {
            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                Text(
                    text = stringResource(R.string.insights_title),
                    style = MaterialTheme.typography.headlineMedium,
                    modifier = Modifier.padding(horizontal = 4.dp),
                )

                // Expressive 4-Way Mode Switcher: Trends | Monthly | Yearly | Mix
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(CircleShape)
                        .background(MaterialTheme.colorScheme.surfaceContainerHigh)
                        .padding(4.dp),
                    horizontalArrangement = Arrangement.spacedBy(4.dp),
                ) {
                    InsightsMode.entries.forEach { mode ->
                        val selected = mode == activeMode
                        val bg by animateColorAsState(
                            targetValue = if (selected) MaterialTheme.colorScheme.primary else Color.Transparent,
                            animationSpec = ExpressiveMotion.fastEffects(),
                            label = "InsModeBg_${mode.name}",
                        )
                        val fg by animateColorAsState(
                            targetValue = if (selected) MaterialTheme.colorScheme.onPrimary
                            else MaterialTheme.colorScheme.onSurfaceVariant,
                            animationSpec = ExpressiveMotion.fastEffects(),
                            label = "InsModeFg_${mode.name}",
                        )
                        Box(
                            modifier = Modifier
                                .weight(1f)
                                .heightIn(min = 42.dp)
                                .clip(CircleShape)
                                .background(bg)
                                .clickable {
                                    performHaptic(HapticFeedbackType.TextHandleMove)
                                    activeMode = mode
                                }
                                .testTag("insights_mode_${mode.name.lowercase()}"),
                            contentAlignment = Alignment.Center,
                        ) {
                            Text(
                                text = mode.label,
                                style = MaterialTheme.typography.labelLarge,
                                color = fg,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis,
                            )
                        }
                    }
                }
            }
        }

        item(key = "insights_mode_body") {
            AnimatedContent(
                targetState = activeMode,
                transitionSpec = {
                    if (reduceMotion) {
                        fadeIn(ExpressiveMotion.fastEffects()) togetherWith fadeOut(ExpressiveMotion.fastEffects())
                    } else {
                        val forward = targetState.ordinal > initialState.ordinal
                        val enter = slideInHorizontally(ExpressiveMotion.defaultSpatial()) { w ->
                            if (forward) w / 6 else -w / 6
                        } + fadeIn(ExpressiveMotion.fastEffects())
                        val exit = slideOutHorizontally(ExpressiveMotion.fastSpatial()) { w ->
                            if (forward) -w / 7 else w / 7
                        } + fadeOut(ExpressiveMotion.fastEffects())
                        enter togetherWith exit
                    }
                },
                label = "InsightsModeContent",
            ) { mode ->
                when (mode) {
                    InsightsMode.TRENDS -> DailyTrendsAnalytics(state = state)
                    InsightsMode.MONTHLY -> MonthlyComparisonAnalytics(state = state)
                    InsightsMode.YEARLY -> YearlyComparisonAnalytics(state = state)
                    InsightsMode.BREAKDOWN -> BreakdownAnalytics(state = state)
                }
            }
        }
    }
}

@Composable
private fun DailyTrendsAnalytics(state: CadenceUiState) {
    val performHaptic = rememberPixelHaptics()
    val currency = LocalAppCurrency.current
    val reduceMotion = LocalReduceMotion.current
    val finColors = LocalFinancialColors.current

    var selectedRange by remember { mutableStateOf("7D") }
    val ranges = remember { listOf("7D", "14D", "30D", "90D", "Cycle") }

    val dailyPoints = remember(state.transactions, selectedRange, state.today, state.budgetResetDay) {
        val txByDay = state.transactions.groupBy { it.date }
        val dates: List<LocalDay> = when (selectedRange) {
            "7D" -> (6 downTo 0).map { state.today.minusDays(it.toLong()) }
            "14D" -> (13 downTo 0).map { state.today.minusDays(it.toLong()) }
            "30D" -> (29 downTo 0).map { state.today.minusDays(it.toLong()) }
            "90D" -> (89 downTo 0).map { state.today.minusDays(it.toLong()) }
            else -> {
                val start = state.cycleStart
                val count = ((state.today.toEpochDay() - start.toEpochDay()).toInt() + 1).coerceIn(1, 31)
                (0 until count).map { start.plusDays(it.toLong()) }
            }
        }
        dates.map { date ->
            val matching = txByDay[date].orEmpty()
            DailySpendPoint(
                date = date,
                total = matching.sumOf { it.amount },
                txCount = matching.size,
            )
        }
    }

    var scrubbedIndex by remember(selectedRange, dailyPoints.size) {
        val lastNonZero = dailyPoints.indexOfLast { it.total > 0.0 }
        mutableIntStateOf(if (lastNonZero >= 0) lastNonZero else dailyPoints.lastIndex.coerceAtLeast(0))
    }
    val entranceProgress = remember(selectedRange) { Animatable(if (reduceMotion) 1f else 0f) }

    LaunchedEffect(selectedRange, reduceMotion) {
        if (reduceMotion) {
            entranceProgress.snapTo(1f)
        } else {
            entranceProgress.snapTo(0f)
            entranceProgress.animateTo(1f, animationSpec = ExpressiveMotion.slowSpatial())
        }
    }

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

    // Week-over-week velocity calculation
    val last7Total = remember(state.transactions, state.today) {
        val cutoff = state.today.minusDays(6)
        state.transactions.filter { it.date >= cutoff && it.date <= state.today }.sumOf { it.amount }
    }
    val prev7Total = remember(state.transactions, state.today) {
        val start = state.today.minusDays(13)
        val end = state.today.minusDays(7)
        state.transactions.filter { it.date >= start && it.date <= end }.sumOf { it.amount }
    }

    Column(verticalArrangement = Arrangement.spacedBy(16.dp)) {
        // Range Selector Strip
        LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            items(ranges) { range ->
                val active = range == selectedRange
                Surface(
                    onClick = {
                        performHaptic(HapticFeedbackType.TextHandleMove)
                        selectedRange = range
                    },
                    shape = CircleShape,
                    color = if (active) MaterialTheme.colorScheme.primaryContainer
                    else MaterialTheme.colorScheme.surfaceContainerHigh,
                    modifier = Modifier.testTag("insights_range_$range"),
                ) {
                    Text(
                        text = range,
                        style = MaterialTheme.typography.labelLarge,
                        color = if (active) MaterialTheme.colorScheme.onPrimaryContainer
                        else MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.padding(horizontal = 16.dp, vertical = 9.dp),
                    )
                }
            }
        }

        // Main Interactive Daily Spending Chart Card
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
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.Top,
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = when (activePoint?.date) {
                                state.today -> "${stringResource(R.string.entry_today)} • ${activePoint.date.formatShort()}"
                                state.today.minusDays(1) -> "Yesterday • ${activePoint.date.formatShort()}"
                                else -> activePoint?.date?.formatDayOfWeekShort() ?: ""
                            },
                            style = MaterialTheme.typography.labelLarge,
                            color = MaterialTheme.colorScheme.primary,
                        )
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

                // 3-Metric Quick Summary Strip
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
                    Column(
                        modifier = Modifier
                            .fillMaxHeight()
                            .padding(top = 6.dp, bottom = 6.dp, end = 8.dp),
                        verticalArrangement = Arrangement.SpaceBetween,
                        horizontalAlignment = Alignment.End,
                    ) {
                        Text(yMaxLabel, style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                        Text(yMidLabel, style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                        Text(yZeroLabel, style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    }

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
                                            performHaptic(HapticFeedbackType.TextHandleMove)
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
                                            performHaptic(HapticFeedbackType.TextHandleMove)
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

                        listOf(topPad, topPad + chartHeight / 2f, baselineY).forEachIndexed { lineIdx, y ->
                            drawLine(
                                color = gridLineColor,
                                start = Offset(0f, y),
                                end = Offset(size.width, y),
                                strokeWidth = if (lineIdx == 2) 1.5.dp.toPx() else 1.dp.toPx(),
                                pathEffect = if (lineIdx == 2) null else dashEffect,
                            )
                        }

                        val count = dailyPoints.size
                        val slotWidth = size.width / count
                        val barWidth = when {
                            count <= 7 -> (slotWidth * 0.48f).coerceIn(14.dp.toPx(), 26.dp.toPx())
                            count <= 14 -> (slotWidth * 0.56f).coerceIn(8.dp.toPx(), 16.dp.toPx())
                            count <= 31 -> (slotWidth * 0.64f).coerceIn(4.dp.toPx(), 10.dp.toPx())
                            else -> (slotWidth * 0.72f).coerceIn(2.dp.toPx(), 5.dp.toPx())
                        }
                        val radius = CornerRadius(barWidth / 2f, barWidth / 2f)

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

                        dailyPoints.forEachIndexed { idx, point ->
                            val isSelected = idx == scrubbedIndex
                            val centerX = idx * slotWidth + slotWidth / 2f

                            if (point.total <= 0.001) {
                                if (count <= 31 || isSelected) {
                                    val dotRadius = if (isSelected) 4.dp.toPx() else 2.2.dp.toPx()
                                    drawCircle(
                                        color = if (isSelected) activeBarColor else zeroDotColor,
                                        radius = dotRadius,
                                        center = Offset(centerX, baselineY - dotRadius - 2.dp.toPx()),
                                    )
                                }
                            } else {
                                val normalized = (point.total / axisMax).toFloat().coerceIn(0.04f, 1f)
                                val barHeight = (chartHeight * normalized * entranceProgress.value)
                                    .coerceAtLeast(5.dp.toPx())
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

                // X-Axis Labels
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(start = 42.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                ) {
                    if (dailyPoints.size <= 7) {
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
                        val size = dailyPoints.size
                        val tickIndices = listOf(0, size / 3, (size * 2) / 3, size - 1).distinct()
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

                // Legend
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

        // Week-over-Week Velocity Card
        Surface(
            shape = RoundedCornerShape(28.dp),
            color = MaterialTheme.colorScheme.surfaceContainerLow,
            modifier = Modifier.fillMaxWidth(),
        ) {
            val delta = last7Total - prev7Total
            val pct = if (prev7Total > 0.01) ((delta / prev7Total) * 100.0).roundToInt() else null
            val down = delta < -0.5
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(18.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = stringResource(R.string.insights_weekly_velocity),
                        style = MaterialTheme.typography.titleMedium,
                    )
                    Text(
                        text = "Last 7d: ${formatCurrency(last7Total, false)} • Prior 7d: ${formatCurrency(prev7Total, false)}",
                        style = MaterialTheme.typography.labelMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
                Surface(
                    shape = CircleShape,
                    color = if (down) finColors.healthyContainer else finColors.warningContainer,
                ) {
                    Text(
                        text = when {
                            pct == null -> "New"
                            down -> "↓ ${abs(pct)}%"
                            else -> "↑ ${abs(pct)}%"
                        },
                        style = MaterialTheme.typography.labelLarge,
                        fontWeight = FontWeight.Bold,
                        color = if (down) finColors.onHealthyContainer else finColors.onWarningContainer,
                        modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp),
                    )
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
    accentColor: Color? = null,
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
