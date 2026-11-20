package com.example.ui.screens

import androidx.compose.animation.core.Animatable
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.TrendingDown
import androidx.compose.material.icons.rounded.TrendingFlat
import androidx.compose.material.icons.rounded.TrendingUp
import androidx.compose.material3.Icon
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.Immutable
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
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.PathEffect
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.example.R
import com.example.data.BudgetCategory
import com.example.data.CadenceUiState
import com.example.data.LocalDay
import com.example.data.Transaction
import com.example.ui.components.LocalAppCurrency
import com.example.ui.components.LocalReduceMotion
import com.example.ui.components.formatCurrency
import com.example.ui.components.formatCurrencyValue
import com.example.ui.components.rememberPixelHaptics
import com.example.ui.theme.ExpressiveMotion
import com.example.ui.theme.LocalFinancialColors
import kotlin.math.abs
import kotlin.math.roundToInt

private val ComparisonGridDashEffect = PathEffect.dashPathEffect(floatArrayOf(8f, 8f), 0f)
private val ComparisonCurveDashEffect = PathEffect.dashPathEffect(floatArrayOf(12f, 8f), 0f)

@Immutable
internal data class MonthKey(val year: Int, val month: Int) : Comparable<MonthKey> {
    fun toFirstDay(): LocalDay = LocalDay(year, month, 1)
    fun shortLabel(): String = toFirstDay().monthYearShort()
    fun shortMonthOnly(): String = toFirstDay().monthShortName()
    fun fullLabel(): String = toFirstDay().monthYearFullName()
    fun daysInMonth(): Int = LocalDay.lengthOfMonthFor(year, month)

    fun minusMonths(n: Int): MonthKey {
        val shifted = toFirstDay().plusMonthsClamped(-n, 1)
        return MonthKey(shifted.year, shifted.month)
    }

    override fun compareTo(other: MonthKey): Int =
        (year * 12 + month).compareTo(other.year * 12 + other.month)
}

internal fun Transaction.monthKey(): MonthKey = MonthKey(date.year, date.month)

@Composable
internal fun MonthlyComparisonAnalytics(state: CadenceUiState) {
    val performHaptic = rememberPixelHaptics()
    val currency = LocalAppCurrency.current
    val reduceMotion = LocalReduceMotion.current

    val currentMonth = remember(state.today) { MonthKey(state.today.year, state.today.month) }
    val selectableMonths = remember(currentMonth) {
        (0..17).map { currentMonth.minusMonths(it) }
    }

    var historySpan by remember { mutableIntStateOf(6) } // 6 or 12 months
    var monthA by remember(currentMonth) { mutableStateOf(currentMonth) }
    var monthB by remember(currentMonth) { mutableStateOf(currentMonth.minusMonths(1)) }
    var selectedOverviewIdx by remember(historySpan) { mutableIntStateOf(historySpan - 1) }

    val txByMonth = remember(state.transactions) {
        state.transactions.groupBy { it.monthKey() }
    }

    // 1. 6M / 12M Monthly Overview Bar Chart
    val overviewMonths = remember(currentMonth, historySpan) {
        (historySpan - 1 downTo 0).map { currentMonth.minusMonths(it) }
    }
    val overviewTotals = remember(overviewMonths, txByMonth) {
        overviewMonths.map { m -> m to (txByMonth[m]?.sumOf { it.amount } ?: 0.0) }
    }
    val overviewAnim = remember(historySpan) { Animatable(if (reduceMotion) 1f else 0f) }
    LaunchedEffect(historySpan, reduceMotion) {
        if (reduceMotion) {
            overviewAnim.snapTo(1f)
        } else {
            overviewAnim.snapTo(0f)
            overviewAnim.animateTo(1f, ExpressiveMotion.slowSpatial())
        }
    }

    Column(verticalArrangement = Arrangement.spacedBy(16.dp)) {
        // Card A: 6M / 12M History Overview
        Surface(
            shape = RoundedCornerShape(32.dp),
            color = MaterialTheme.colorScheme.surfaceContainerLow,
            modifier = Modifier
                .fillMaxWidth()
                .testTag("insights_monthly_overview_card"),
        ) {
            Column(
                modifier = Modifier.padding(20.dp),
                verticalArrangement = Arrangement.spacedBy(14.dp),
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = stringResource(R.string.insights_monthly_history),
                            style = MaterialTheme.typography.titleLarge,
                        )
                        val activePair = overviewTotals.getOrNull(selectedOverviewIdx)
                        if (activePair != null) {
                            Text(
                                text = "${activePair.first.fullLabel()} • ${formatCurrency(activePair.second, showCents = false)}",
                                style = MaterialTheme.typography.labelLarge,
                                color = MaterialTheme.colorScheme.primary,
                            )
                        }
                    }
                    Surface(shape = CircleShape, color = MaterialTheme.colorScheme.surfaceContainerHigh) {
                        Row(modifier = Modifier.padding(3.dp)) {
                            listOf(6, 12).forEach { span ->
                                val active = historySpan == span
                                Surface(
                                    onClick = {
                                        performHaptic(HapticFeedbackType.TextHandleMove)
                                        historySpan = span
                                    },
                                    shape = CircleShape,
                                    color = if (active) MaterialTheme.colorScheme.primary else Color.Transparent,
                                ) {
                                    Text(
                                        text = "${span}M",
                                        style = MaterialTheme.typography.labelMedium,
                                        color = if (active) MaterialTheme.colorScheme.onPrimary
                                        else MaterialTheme.colorScheme.onSurfaceVariant,
                                        modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp),
                                    )
                                }
                            }
                        }
                    }
                }

                val axisMax = remember(overviewTotals) {
                    niceAxisCeiling((overviewTotals.maxOfOrNull { it.second } ?: 0.0) * 1.15)
                }
                val primaryColor = MaterialTheme.colorScheme.primary
                val mutedColor = MaterialTheme.colorScheme.primary.copy(alpha = 0.34f)
                val gridColor = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.4f)

                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(168.dp),
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxHeight()
                            .padding(end = 8.dp, top = 4.dp, bottom = 4.dp),
                        verticalArrangement = Arrangement.SpaceBetween,
                        horizontalAlignment = Alignment.End,
                    ) {
                        Text(
                            text = formatCurrencyValue(axisMax, currency, false, compact = true),
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                        Text(
                            text = formatCurrencyValue(axisMax / 2.0, currency, false, compact = true),
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                        Text(
                            text = formatCurrencyValue(0.0, currency, false, compact = true),
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                    }
                    Canvas(
                        modifier = Modifier
                            .weight(1f)
                            .fillMaxHeight()
                            .pointerInput(overviewTotals) {
                                detectTapGestures { offset ->
                                    if (overviewTotals.isNotEmpty() && size.width > 0) {
                                        val idx = ((offset.x / size.width) * overviewTotals.size)
                                            .toInt()
                                            .coerceIn(0, overviewTotals.lastIndex)
                                        selectedOverviewIdx = idx
                                        performHaptic(HapticFeedbackType.TextHandleMove)
                                    }
                                }
                            },
                    ) {
                        val topPad = 10.dp.toPx()
                        val bottomPad = 8.dp.toPx()
                        val chartH = (size.height - topPad - bottomPad).coerceAtLeast(1f)
                        val baseY = topPad + chartH
                        val midY = topPad + chartH / 2f
                        val gridStroke = 1.dp.toPx()

                        drawLine(
                            color = gridColor,
                            start = Offset(0f, topPad),
                            end = Offset(size.width, topPad),
                            strokeWidth = gridStroke,
                            pathEffect = ComparisonGridDashEffect,
                        )
                        drawLine(
                            color = gridColor,
                            start = Offset(0f, midY),
                            end = Offset(size.width, midY),
                            strokeWidth = gridStroke,
                            pathEffect = ComparisonGridDashEffect,
                        )
                        drawLine(
                            color = gridColor,
                            start = Offset(0f, baseY),
                            end = Offset(size.width, baseY),
                            strokeWidth = 1.5.dp.toPx(),
                        )

                        val slot = size.width / overviewTotals.size.coerceAtLeast(1)
                        val barW = (slot * 0.54f).coerceIn(8.dp.toPx(), 28.dp.toPx())
                        val rad = CornerRadius(barW / 2f, barW / 2f)

                        overviewTotals.forEachIndexed { idx, (_, value) ->
                            val cx = idx * slot + slot / 2f
                            if (value <= 0.001) {
                                drawCircle(
                                    color = if (idx == selectedOverviewIdx) primaryColor else gridColor,
                                    radius = 3.dp.toPx(),
                                    center = Offset(cx, baseY - 4.dp.toPx()),
                                )
                            } else {
                                val frac = (value / axisMax).toFloat().coerceIn(0.05f, 1f)
                                val h = (chartH * frac * overviewAnim.value).coerceAtLeast(6.dp.toPx())
                                drawRoundRect(
                                    color = if (idx == selectedOverviewIdx) primaryColor else mutedColor,
                                    topLeft = Offset(cx - barW / 2f, baseY - h),
                                    size = Size(barW, h),
                                    cornerRadius = rad,
                                )
                            }
                        }
                    }
                }

                // Month x-axis labels
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(start = 40.dp),
                ) {
                    overviewMonths.forEachIndexed { idx, m ->
                        val showLabel = historySpan == 6 || idx % 2 == 0 || idx == overviewMonths.lastIndex
                        Text(
                            text = if (showLabel) m.shortMonthOnly().take(3) else "",
                            style = MaterialTheme.typography.labelSmall,
                            color = if (idx == selectedOverviewIdx) MaterialTheme.colorScheme.primary
                            else MaterialTheme.colorScheme.onSurfaceVariant,
                            fontWeight = if (idx == selectedOverviewIdx) FontWeight.Bold else FontWeight.Normal,
                            textAlign = TextAlign.Center,
                            maxLines = 1,
                            modifier = Modifier.weight(1f),
                        )
                    }
                }
            }
        }

        // Card B: Head-to-Head Month A vs Month B Comparison + Cumulative Curve
        val txA = remember(txByMonth, monthA) { txByMonth[monthA].orEmpty() }
        val txB = remember(txByMonth, monthB) { txByMonth[monthB].orEmpty() }
        val totalA = remember(txA) { txA.sumOf { it.amount } }
        val totalB = remember(txB) { txB.sumOf { it.amount } }

        Surface(
            shape = RoundedCornerShape(32.dp),
            color = MaterialTheme.colorScheme.surfaceContainerLow,
            modifier = Modifier
                .fillMaxWidth()
                .testTag("insights_month_vs_month_card"),
        ) {
            Column(
                modifier = Modifier.padding(20.dp),
                verticalArrangement = Arrangement.spacedBy(14.dp),
            ) {
                Text(
                    text = stringResource(R.string.insights_compare_months_title),
                    style = MaterialTheme.typography.titleLarge,
                )

                // Quick Preset Comparisons
                LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    val presets = listOf(
                        "vs Last Mo" to currentMonth.minusMonths(1),
                        "vs 2 Mo Ago" to currentMonth.minusMonths(2),
                        "vs 3 Mo Ago" to currentMonth.minusMonths(3),
                        "vs Last Year" to currentMonth.minusMonths(12),
                    )
                    items(presets) { (label, targetB) ->
                        val active = monthA == currentMonth && monthB == targetB
                        Surface(
                            onClick = {
                                performHaptic(HapticFeedbackType.TextHandleMove)
                                monthA = currentMonth
                                monthB = targetB
                            },
                            shape = CircleShape,
                            color = if (active) MaterialTheme.colorScheme.secondaryContainer
                            else MaterialTheme.colorScheme.surfaceContainerHigh,
                        ) {
                            Text(
                                text = label,
                                style = MaterialTheme.typography.labelMedium,
                                color = if (active) MaterialTheme.colorScheme.onSecondaryContainer
                                else MaterialTheme.colorScheme.onSurfaceVariant,
                                modifier = Modifier.padding(horizontal = 12.dp, vertical = 7.dp),
                            )
                        }
                    }
                }

                // Month A Picker Row
                MonthSelectorStrip(
                    label = stringResource(R.string.insights_primary_month),
                    months = selectableMonths,
                    selected = monthA,
                    dotColor = MaterialTheme.colorScheme.primary,
                    onSelect = {
                        performHaptic(HapticFeedbackType.TextHandleMove)
                        monthA = it
                    },
                )

                // Month B Picker Row
                MonthSelectorStrip(
                    label = stringResource(R.string.insights_compare_against),
                    months = selectableMonths,
                    selected = monthB,
                    dotColor = MaterialTheme.colorScheme.tertiary,
                    onSelect = {
                        performHaptic(HapticFeedbackType.TextHandleMove)
                        monthB = it
                    },
                )

                // Side-by-side Totals + Delta Pill
                ComparisonTotalsHero(
                    labelA = monthA.shortLabel(),
                    totalA = totalA,
                    labelB = monthB.shortLabel(),
                    totalB = totalB,
                )

                // Cumulative Spend Pace Dual-Line Chart (Day 1..31)
                CumulativeMonthPaceChart(
                    monthA = monthA,
                    txA = txA,
                    monthB = monthB,
                    txB = txB,
                    today = state.today,
                )
            }
        }

        // Card C: Category-by-Category Month Comparison
        CategoryDeltaComparisonCard(
            title = stringResource(R.string.insights_category_comparison),
            labelA = monthA.shortLabel(),
            labelB = monthB.shortLabel(),
            categories = state.categories,
            txA = txA,
            txB = txB,
        )
    }
}

@Composable
internal fun YearlyComparisonAnalytics(state: CadenceUiState) {
    val performHaptic = rememberPixelHaptics()
    val currency = LocalAppCurrency.current
    val reduceMotion = LocalReduceMotion.current

    val currentYear = state.today.year
    val availableYears = remember(state.transactions, currentYear) {
        val fromTx = state.transactions.map { it.date.year }
        (fromTx + listOf(currentYear, currentYear - 1, currentYear - 2))
            .distinct()
            .sortedDescending()
    }

    var yearA by remember(availableYears) { mutableIntStateOf(availableYears.firstOrNull() ?: currentYear) }
    var yearB by remember(availableYears) {
        mutableIntStateOf(availableYears.getOrNull(1) ?: (currentYear - 1))
    }
    var ytdMode by remember { mutableStateOf(false) }
    var selectedMonthIndex by remember { mutableIntStateOf((state.today.month - 1).coerceIn(0, 11)) }

    val txByYear = remember(state.transactions) { state.transactions.groupBy { it.date.year } }

    fun filterForYear(year: Int): List<Transaction> {
        val list = txByYear[year].orEmpty()
        return if (!ytdMode) {
            list
        } else {
            list.filter { tx ->
                tx.date.month < state.today.month ||
                    (tx.date.month == state.today.month && tx.date.dayOfMonth <= state.today.dayOfMonth)
            }
        }
    }

    val txA = remember(txByYear, yearA, ytdMode, state.today) { filterForYear(yearA) }
    val txB = remember(txByYear, yearB, ytdMode, state.today) { filterForYear(yearB) }
    val totalA = remember(txA) { txA.sumOf { it.amount } }
    val totalB = remember(txB) { txB.sumOf { it.amount } }

    val monthlyA = remember(txA) {
        DoubleArray(12) { mIdx -> txA.filter { it.date.month == mIdx + 1 }.sumOf { it.amount } }
    }
    val monthlyB = remember(txB) {
        DoubleArray(12) { mIdx -> txB.filter { it.date.month == mIdx + 1 }.sumOf { it.amount } }
    }

    val barAnim = remember(yearA, yearB, ytdMode) { Animatable(if (reduceMotion) 1f else 0f) }
    LaunchedEffect(yearA, yearB, ytdMode, reduceMotion) {
        if (reduceMotion) {
            barAnim.snapTo(1f)
        } else {
            barAnim.snapTo(0f)
            barAnim.animateTo(1f, ExpressiveMotion.slowSpatial())
        }
    }

    Column(verticalArrangement = Arrangement.spacedBy(16.dp)) {
        // Card 1: Multi-Year Totals Snapshot
        Surface(
            shape = RoundedCornerShape(32.dp),
            color = MaterialTheme.colorScheme.surfaceContainerLow,
            modifier = Modifier
                .fillMaxWidth()
                .testTag("insights_yearly_overview_card"),
        ) {
            Column(
                modifier = Modifier.padding(20.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp),
            ) {
                Text(
                    text = stringResource(R.string.insights_multi_year_totals),
                    style = MaterialTheme.typography.titleLarge,
                )
                val yearTotals = remember(availableYears, txByYear) {
                    availableYears.map { y -> y to (txByYear[y]?.sumOf { it.amount } ?: 0.0) }
                }
                val maxYearVal = (yearTotals.maxOfOrNull { it.second } ?: 1.0).coerceAtLeast(1.0)
                yearTotals.forEach { (yr, amt) ->
                    val ratio = (amt / maxYearVal).toFloat().coerceIn(0.02f, 1f)
                    val isSelected = yr == yearA || yr == yearB
                    Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically,
                        ) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(8.dp),
                            ) {
                                Text(
                                    text = yr.toString(),
                                    style = MaterialTheme.typography.titleMedium,
                                    fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium,
                                )
                                if (yr == currentYear) {
                                    Surface(shape = CircleShape, color = MaterialTheme.colorScheme.primaryContainer) {
                                        Text(
                                            text = "Current",
                                            style = MaterialTheme.typography.labelSmall,
                                            color = MaterialTheme.colorScheme.onPrimaryContainer,
                                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 2.dp),
                                        )
                                    }
                                }
                            }
                            Text(
                                text = formatCurrency(amt, showCents = false),
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.SemiBold,
                            )
                        }
                        LinearProgressIndicator(
                            progress = { if (amt > 0.0) ratio else 0f },
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(10.dp)
                                .clip(CircleShape),
                            color = when (yr) {
                                yearA -> MaterialTheme.colorScheme.primary
                                yearB -> MaterialTheme.colorScheme.tertiary
                                else -> MaterialTheme.colorScheme.secondary.copy(alpha = 0.55f)
                            },
                            trackColor = MaterialTheme.colorScheme.surfaceContainerHighest,
                        )
                    }
                }
            }
        }

        // Card 2: Head-to-Head Year A vs Year B + 12-Month Grouped Bar Chart
        Surface(
            shape = RoundedCornerShape(32.dp),
            color = MaterialTheme.colorScheme.surfaceContainerLow,
            modifier = Modifier
                .fillMaxWidth()
                .testTag("insights_year_vs_year_card"),
        ) {
            Column(
                modifier = Modifier.padding(20.dp),
                verticalArrangement = Arrangement.spacedBy(14.dp),
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Text(
                        text = stringResource(R.string.insights_compare_years_title),
                        style = MaterialTheme.typography.titleLarge,
                    )
                    // Full Year vs YTD toggle
                    Surface(shape = CircleShape, color = MaterialTheme.colorScheme.surfaceContainerHigh) {
                        Row(modifier = Modifier.padding(3.dp)) {
                            listOf(false to "Full Yr", true to "YTD").forEach { (isYtd, label) ->
                                val active = ytdMode == isYtd
                                Surface(
                                    onClick = {
                                        performHaptic(HapticFeedbackType.TextHandleMove)
                                        ytdMode = isYtd
                                    },
                                    shape = CircleShape,
                                    color = if (active) MaterialTheme.colorScheme.primary else Color.Transparent,
                                    modifier = Modifier.testTag("yearly_scope_${if (isYtd) "ytd" else "full"}"),
                                ) {
                                    Text(
                                        text = label,
                                        style = MaterialTheme.typography.labelMedium,
                                        color = if (active) MaterialTheme.colorScheme.onPrimary
                                        else MaterialTheme.colorScheme.onSurfaceVariant,
                                        modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp),
                                    )
                                }
                            }
                        }
                    }
                }

                // Year A & Year B Chip Selectors
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(10.dp),
                ) {
                    Column(modifier = Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                        Text(
                            text = stringResource(R.string.insights_primary_year),
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.primary,
                        )
                        LazyRow(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                            items(availableYears) { yr ->
                                val active = yr == yearA
                                Surface(
                                    onClick = {
                                        performHaptic(HapticFeedbackType.TextHandleMove)
                                        yearA = yr
                                    },
                                    shape = CircleShape,
                                    color = if (active) MaterialTheme.colorScheme.primary
                                    else MaterialTheme.colorScheme.surfaceContainerHigh,
                                ) {
                                    Text(
                                        text = yr.toString(),
                                        style = MaterialTheme.typography.labelMedium,
                                        color = if (active) MaterialTheme.colorScheme.onPrimary
                                        else MaterialTheme.colorScheme.onSurfaceVariant,
                                        modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp),
                                    )
                                }
                            }
                        }
                    }
                    Column(modifier = Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                        Text(
                            text = stringResource(R.string.insights_compare_against),
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.tertiary,
                        )
                        LazyRow(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                            items(availableYears) { yr ->
                                val active = yr == yearB
                                Surface(
                                    onClick = {
                                        performHaptic(HapticFeedbackType.TextHandleMove)
                                        yearB = yr
                                    },
                                    shape = CircleShape,
                                    color = if (active) MaterialTheme.colorScheme.tertiary
                                    else MaterialTheme.colorScheme.surfaceContainerHigh,
                                ) {
                                    Text(
                                        text = yr.toString(),
                                        style = MaterialTheme.typography.labelMedium,
                                        color = if (active) MaterialTheme.colorScheme.onTertiary
                                        else MaterialTheme.colorScheme.onSurfaceVariant,
                                        modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp),
                                    )
                                }
                            }
                        }
                    }
                }

                ComparisonTotalsHero(
                    labelA = if (ytdMode) "$yearA YTD" else "$yearA",
                    totalA = totalA,
                    labelB = if (ytdMode) "$yearB YTD" else "$yearB",
                    totalB = totalB,
                )

                // Active Month Inspection Banner for 12-Month Grouped Chart
                val monthNames = remember {
                    listOf("Jan", "Feb", "Mar", "Apr", "May", "Jun", "Jul", "Aug", "Sep", "Oct", "Nov", "Dec")
                }
                Surface(
                    shape = RoundedCornerShape(18.dp),
                    color = MaterialTheme.colorScheme.surfaceContainerHigh,
                    modifier = Modifier.fillMaxWidth(),
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 14.dp, vertical = 10.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        Text(
                            text = monthNames[selectedMonthIndex],
                            style = MaterialTheme.typography.titleSmall,
                            fontWeight = FontWeight.Bold,
                        )
                        Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                            Text(
                                text = "$yearA: ${formatCurrency(monthlyA[selectedMonthIndex], false)}",
                                style = MaterialTheme.typography.labelLarge,
                                color = MaterialTheme.colorScheme.primary,
                            )
                            Text(
                                text = "$yearB: ${formatCurrency(monthlyB[selectedMonthIndex], false)}",
                                style = MaterialTheme.typography.labelLarge,
                                color = MaterialTheme.colorScheme.tertiary,
                            )
                        }
                    }
                }

                // 12-Month Grouped Side-by-Side Bar Chart
                val maxBar = remember(monthlyA, monthlyB) {
                    val mA = monthlyA.maxOrNull() ?: 0.0
                    val mB = monthlyB.maxOrNull() ?: 0.0
                    niceAxisCeiling(maxOf(mA, mB) * 1.15)
                }
                val colorA = MaterialTheme.colorScheme.primary
                val colorB = MaterialTheme.colorScheme.tertiary
                val gridColor = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.4f)
                val highlightBg = MaterialTheme.colorScheme.primary.copy(alpha = 0.09f)

                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(176.dp),
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxHeight()
                            .padding(end = 6.dp, top = 4.dp, bottom = 4.dp),
                        verticalArrangement = Arrangement.SpaceBetween,
                        horizontalAlignment = Alignment.End,
                    ) {
                        Text(
                            text = formatCurrencyValue(maxBar, currency, false, compact = true),
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                        Text(
                            text = formatCurrencyValue(maxBar / 2.0, currency, false, compact = true),
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                        Text(
                            text = formatCurrencyValue(0.0, currency, false, compact = true),
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                    }
                    Canvas(
                        modifier = Modifier
                            .weight(1f)
                            .fillMaxHeight()
                            .pointerInput(yearA, yearB) {
                                detectTapGestures { offset ->
                                    if (size.width > 0) {
                                        val idx = ((offset.x / size.width) * 12).toInt().coerceIn(0, 11)
                                        selectedMonthIndex = idx
                                        performHaptic(HapticFeedbackType.TextHandleMove)
                                    }
                                }
                            },
                    ) {
                        val topPad = 8.dp.toPx()
                        val bottomPad = 6.dp.toPx()
                        val chartH = (size.height - topPad - bottomPad).coerceAtLeast(1f)
                        val baseY = topPad + chartH
                        val midY = topPad + chartH / 2f
                        val gridStroke = 1.dp.toPx()

                        drawLine(
                            color = gridColor,
                            start = Offset(0f, topPad),
                            end = Offset(size.width, topPad),
                            strokeWidth = gridStroke,
                            pathEffect = ComparisonGridDashEffect,
                        )
                        drawLine(
                            color = gridColor,
                            start = Offset(0f, midY),
                            end = Offset(size.width, midY),
                            strokeWidth = gridStroke,
                            pathEffect = ComparisonGridDashEffect,
                        )
                        drawLine(
                            color = gridColor,
                            start = Offset(0f, baseY),
                            end = Offset(size.width, baseY),
                            strokeWidth = 1.5.dp.toPx(),
                        )

                        val slot = size.width / 12f
                        val barW = (slot * 0.34f).coerceIn(4.dp.toPx(), 10.dp.toPx())
                        val gap = 2.dp.toPx()
                        val rad = CornerRadius(barW / 2f, barW / 2f)

                        // Highlight selected month slot
                        drawRoundRect(
                            color = highlightBg,
                            topLeft = Offset(selectedMonthIndex * slot, topPad - 2.dp.toPx()),
                            size = Size(slot, chartH + 4.dp.toPx()),
                            cornerRadius = CornerRadius(8.dp.toPx(), 8.dp.toPx()),
                        )

                        for (m in 0 until 12) {
                            val centerX = m * slot + slot / 2f
                            val isSel = m == selectedMonthIndex
                            val valA = monthlyA[m]
                            val valB = monthlyB[m]

                            if (valB > 0.001) {
                                val hB = (chartH * (valB / maxBar).toFloat().coerceIn(0.04f, 1f) * barAnim.value)
                                    .coerceAtLeast(4.dp.toPx())
                                drawRoundRect(
                                    color = if (isSel) colorB else colorB.copy(alpha = 0.45f),
                                    topLeft = Offset(centerX - barW - gap / 2f, baseY - hB),
                                    size = Size(barW, hB),
                                    cornerRadius = rad,
                                )
                            }
                            if (valA > 0.001) {
                                val hA = (chartH * (valA / maxBar).toFloat().coerceIn(0.04f, 1f) * barAnim.value)
                                    .coerceAtLeast(4.dp.toPx())
                                drawRoundRect(
                                    color = if (isSel) colorA else colorA.copy(alpha = 0.55f),
                                    topLeft = Offset(centerX + gap / 2f, baseY - hA),
                                    size = Size(barW, hA),
                                    cornerRadius = rad,
                                )
                            }
                        }
                    }
                }

                // J..D Month initials
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(start = 38.dp),
                ) {
                    monthNames.forEachIndexed { idx, name ->
                        Text(
                            text = name.first().toString(),
                            style = MaterialTheme.typography.labelSmall,
                            fontWeight = if (idx == selectedMonthIndex) FontWeight.Bold else FontWeight.Normal,
                            color = if (idx == selectedMonthIndex) MaterialTheme.colorScheme.primary
                            else MaterialTheme.colorScheme.onSurfaceVariant,
                            textAlign = TextAlign.Center,
                            modifier = Modifier.weight(1f),
                        )
                    }
                }

                // Yearly KPIs Strip
                val nonZeroMonthsA = monthlyA.count { it > 0.0 }.coerceAtLeast(1)
                val avgMonthA = totalA / nonZeroMonthsA
                val peakIdx = monthlyA.indices.maxByOrNull { monthlyA[it] } ?: 0
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                ) {
                    InsightsMiniStat(
                        label = "$yearA avg/mo",
                        value = formatCurrencyValue(avgMonthA, currency, false),
                        modifier = Modifier.weight(1f),
                    )
                    InsightsMiniStat(
                        label = "Peak (${monthNames[peakIdx]})",
                        value = formatCurrencyValue(monthlyA[peakIdx], currency, false),
                        modifier = Modifier.weight(1f),
                    )
                    InsightsMiniStat(
                        label = "$yearA entries",
                        value = txA.size.toString(),
                        modifier = Modifier.weight(1f),
                    )
                }
            }
        }

        // Card 3: Category Year-over-Year Comparison
        CategoryDeltaComparisonCard(
            title = stringResource(R.string.insights_category_yoy),
            labelA = yearA.toString(),
            labelB = yearB.toString(),
            categories = state.categories,
            txA = txA,
            txB = txB,
        )
    }
}

private enum class BreakdownScope(val label: String) {
    CYCLE("Cycle"),
    MONTH("Month"),
    YEAR("Year"),
    ALL("All"),
}

@Composable
internal fun BreakdownAnalytics(state: CadenceUiState) {
    val performHaptic = rememberPixelHaptics()
    var scope by remember { mutableStateOf(BreakdownScope.CYCLE) }

    val scopedTx = remember(state.transactions, scope, state.today, state.budgetResetDay) {
        when (scope) {
            BreakdownScope.CYCLE -> state.currentMonthTransactions
            BreakdownScope.MONTH -> state.transactions.filter {
                it.date.year == state.today.year && it.date.month == state.today.month
            }
            BreakdownScope.YEAR -> state.transactions.filter { it.date.year == state.today.year }
            BreakdownScope.ALL -> state.transactions
        }
    }

    val categoryMap = remember(state.categories) { state.categories.associateBy { it.id } }

    Column(verticalArrangement = Arrangement.spacedBy(16.dp)) {
        // Scope Pills
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            BreakdownScope.entries.forEach { item ->
                val active = scope == item
                Surface(
                    onClick = {
                        performHaptic(HapticFeedbackType.TextHandleMove)
                        scope = item
                    },
                    shape = CircleShape,
                    color = if (active) MaterialTheme.colorScheme.primary
                    else MaterialTheme.colorScheme.surfaceContainerHigh,
                    modifier = Modifier.weight(1f),
                ) {
                    Box(
                        contentAlignment = Alignment.Center,
                        modifier = Modifier.padding(vertical = 9.dp),
                    ) {
                        Text(
                            text = item.label,
                            style = MaterialTheme.typography.labelLarge,
                            color = if (active) MaterialTheme.colorScheme.onPrimary
                            else MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                    }
                }
            }
        }

        // 1. Category Mix Card
        val breakdown = remember(state.categories, scopedTx) {
            state.categories
                .map { cat -> cat to scopedTx.filter { it.categoryId == cat.id }.sumOf { it.amount } }
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
                        text = formatCurrency(if (breakdown.isEmpty()) 0.0 else total, showCents = false),
                        style = MaterialTheme.typography.titleMedium,
                        color = MaterialTheme.colorScheme.primary,
                        fontWeight = FontWeight.Bold,
                    )
                }

                if (breakdown.isEmpty()) {
                    Text(
                        text = stringResource(R.string.insights_empty_mix),
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                } else {
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
                            val pct = ((spent / total) * 100).roundToInt()
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

        // 2. Fixed Commitments vs Flexible Budget Split
        Surface(
            shape = RoundedCornerShape(32.dp),
            color = MaterialTheme.colorScheme.surfaceContainerLow,
            modifier = Modifier.fillMaxWidth(),
        ) {
            val billsMo = state.monthlyRecurringBillsTotal
            val subsMo = state.monthlySubscriptionsTotal
            val flexibleMo = (state.totalMonthlyBudget - billsMo - subsMo).coerceAtLeast(0.0)
            val denom = (billsMo + subsMo + flexibleMo).coerceAtLeast(1.0)

            Column(
                modifier = Modifier.padding(20.dp),
                verticalArrangement = Arrangement.spacedBy(14.dp),
            ) {
                Text(
                    text = stringResource(R.string.insights_fixed_vs_flexible),
                    style = MaterialTheme.typography.titleLarge,
                )
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(18.dp),
                    horizontalArrangement = Arrangement.spacedBy(4.dp),
                ) {
                    if (billsMo > 0.0) {
                        Box(
                            modifier = Modifier
                                .weight((billsMo / denom).toFloat().coerceAtLeast(0.06f))
                                .fillMaxHeight()
                                .clip(CircleShape)
                                .background(MaterialTheme.colorScheme.primary),
                        )
                    }
                    if (subsMo > 0.0) {
                        Box(
                            modifier = Modifier
                                .weight((subsMo / denom).toFloat().coerceAtLeast(0.06f))
                                .fillMaxHeight()
                                .clip(CircleShape)
                                .background(MaterialTheme.colorScheme.tertiary),
                        )
                    }
                    if (flexibleMo > 0.0) {
                        Box(
                            modifier = Modifier
                                .weight((flexibleMo / denom).toFloat().coerceAtLeast(0.06f))
                                .fillMaxHeight()
                                .clip(CircleShape)
                                .background(MaterialTheme.colorScheme.secondaryContainer),
                        )
                    }
                }
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                ) {
                    InsightsMiniStat(
                        label = stringResource(R.string.recurring_bills_short),
                        value = "${formatCurrency(billsMo, false)}/mo",
                        accent = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.weight(1f),
                    )
                    InsightsMiniStat(
                        label = stringResource(R.string.subscriptions_title),
                        value = "${formatCurrency(subsMo, false)}/mo",
                        accent = MaterialTheme.colorScheme.tertiary,
                        modifier = Modifier.weight(1f),
                    )
                    InsightsMiniStat(
                        label = stringResource(R.string.insights_flexible_label),
                        value = "${formatCurrency(flexibleMo, false)}/mo",
                        modifier = Modifier.weight(1f),
                    )
                }
            }
        }

        // 3. Day-of-Week Spending Pattern (Mon..Sun)
        Surface(
            shape = RoundedCornerShape(32.dp),
            color = MaterialTheme.colorScheme.surfaceContainerLow,
            modifier = Modifier.fillMaxWidth(),
        ) {
            val dayLabels = remember { listOf("Mon", "Tue", "Wed", "Thu", "Fri", "Sat", "Sun") }
            val totalsByDow = remember(scopedTx) {
                DoubleArray(7) { idx ->
                    scopedTx.filter { it.date.dayOfWeekIndexMonFirst() == idx }.sumOf { it.amount }
                }
            }
            val maxDow = (totalsByDow.maxOrNull() ?: 1.0).coerceAtLeast(1.0)
            val peakDowIdx = totalsByDow.indices.maxByOrNull { totalsByDow[it] } ?: 0

            Column(
                modifier = Modifier.padding(20.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp),
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Text(
                        text = stringResource(R.string.insights_weekday_pattern),
                        style = MaterialTheme.typography.titleLarge,
                    )
                    if (totalsByDow[peakDowIdx] > 0.0) {
                        Text(
                            text = "Busiest: ${dayLabels[peakDowIdx]}",
                            style = MaterialTheme.typography.labelLarge,
                            color = MaterialTheme.colorScheme.primary,
                        )
                    }
                }
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(120.dp),
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalAlignment = Alignment.Bottom,
                ) {
                    totalsByDow.forEachIndexed { idx, amt ->
                        val frac = if (amt > 0.0) (amt / maxDow).toFloat().coerceIn(0.10f, 1f) else 0.06f
                        val isPeak = idx == peakDowIdx && amt > 0.0
                        Column(
                            modifier = Modifier.weight(1f),
                            horizontalAlignment = Alignment.CenterHorizontally,
                            verticalArrangement = Arrangement.Bottom,
                        ) {
                            Box(
                                modifier = Modifier
                                    .fillMaxWidth(0.72f)
                                    .height((88f * frac).dp)
                                    .clip(RoundedCornerShape(10.dp))
                                    .background(
                                        when {
                                            amt <= 0.0 -> MaterialTheme.colorScheme.surfaceContainerHighest
                                            isPeak -> MaterialTheme.colorScheme.primary
                                            else -> MaterialTheme.colorScheme.primary.copy(alpha = 0.36f)
                                        }
                                    ),
                            )
                            Spacer(Modifier.height(6.dp))
                            Text(
                                text = dayLabels[idx],
                                style = MaterialTheme.typography.labelSmall,
                                fontWeight = if (isPeak) FontWeight.Bold else FontWeight.Normal,
                                color = if (isPeak) MaterialTheme.colorScheme.primary
                                else MaterialTheme.colorScheme.onSurfaceVariant,
                            )
                        }
                    }
                }
            }
        }

        // 4. Top 5 Largest Expenses
        val topExpenses = remember(scopedTx) {
            scopedTx.sortedByDescending { it.amount }.take(5)
        }
        if (topExpenses.isNotEmpty()) {
            Surface(
                shape = RoundedCornerShape(32.dp),
                color = MaterialTheme.colorScheme.surfaceContainerLow,
                modifier = Modifier.fillMaxWidth(),
            ) {
                Column(
                    modifier = Modifier.padding(20.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp),
                ) {
                    Text(
                        text = stringResource(R.string.insights_top_expenses),
                        style = MaterialTheme.typography.titleLarge,
                    )
                    topExpenses.forEachIndexed { idx, tx ->
                        val cat = categoryMap[tx.categoryId]
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(12.dp),
                        ) {
                            Box(
                                modifier = Modifier
                                    .size(30.dp)
                                    .clip(CircleShape)
                                    .background(MaterialTheme.colorScheme.surfaceContainerHigh),
                                contentAlignment = Alignment.Center,
                            ) {
                                Text(
                                    text = "${idx + 1}",
                                    style = MaterialTheme.typography.labelMedium,
                                    fontWeight = FontWeight.Bold,
                                )
                            }
                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    text = tx.merchantOrNote,
                                    style = MaterialTheme.typography.titleSmall,
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis,
                                )
                                Text(
                                    text = "${cat?.name ?: "General"} • ${tx.date.formatShort()}",
                                    style = MaterialTheme.typography.labelSmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                                )
                            }
                            Text(
                                text = formatCurrency(tx.amount, showCents = true),
                                style = MaterialTheme.typography.titleSmall,
                                fontWeight = FontWeight.Bold,
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun MonthSelectorStrip(
    label: String,
    months: List<MonthKey>,
    selected: MonthKey,
    dotColor: Color,
    onSelect: (MonthKey) -> Unit,
) {
    Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(6.dp),
        ) {
            Box(
                modifier = Modifier
                    .size(8.dp)
                    .clip(CircleShape)
                    .background(dotColor),
            )
            Text(
                text = label,
                style = MaterialTheme.typography.labelMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
        LazyRow(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
            items(months, key = { "${label}_${it.year}_${it.month}" }) { m ->
                val active = m == selected
                Surface(
                    onClick = { onSelect(m) },
                    shape = CircleShape,
                    color = if (active) dotColor else MaterialTheme.colorScheme.surfaceContainerHigh,
                ) {
                    Text(
                        text = m.shortLabel(),
                        style = MaterialTheme.typography.labelMedium,
                        color = if (active) MaterialTheme.colorScheme.onPrimary
                        else MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.padding(horizontal = 12.dp, vertical = 7.dp),
                    )
                }
            }
        }
    }
}

@Composable
private fun ComparisonTotalsHero(
    labelA: String,
    totalA: Double,
    labelB: String,
    totalB: Double,
) {
    val finColors = LocalFinancialColors.current
    val diff = totalA - totalB
    val pctChange = if (totalB > 0.01) ((diff / totalB) * 100.0).roundToInt() else null
    val isLower = diff < -0.5
    val isHigher = diff > 0.5
    val badgeBg = when {
        isLower -> finColors.healthyContainer
        isHigher -> finColors.warningContainer
        else -> MaterialTheme.colorScheme.surfaceContainerHighest
    }
    val badgeFg = when {
        isLower -> finColors.onHealthyContainer
        isHigher -> finColors.onWarningContainer
        else -> MaterialTheme.colorScheme.onSurfaceVariant
    }

    Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(10.dp),
        ) {
            Surface(
                shape = RoundedCornerShape(22.dp),
                color = MaterialTheme.colorScheme.primaryContainer,
                modifier = Modifier.weight(1f),
            ) {
                Column(modifier = Modifier.padding(14.dp)) {
                    Text(
                        text = labelA,
                        style = MaterialTheme.typography.labelMedium,
                        color = MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.8f),
                    )
                    Text(
                        text = formatCurrency(totalA, showCents = false),
                        style = MaterialTheme.typography.titleLarge,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onPrimaryContainer,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                    )
                }
            }
            Surface(
                shape = RoundedCornerShape(22.dp),
                color = MaterialTheme.colorScheme.tertiaryContainer,
                modifier = Modifier.weight(1f),
            ) {
                Column(modifier = Modifier.padding(14.dp)) {
                    Text(
                        text = labelB,
                        style = MaterialTheme.typography.labelMedium,
                        color = MaterialTheme.colorScheme.onTertiaryContainer.copy(alpha = 0.8f),
                    )
                    Text(
                        text = formatCurrency(totalB, showCents = false),
                        style = MaterialTheme.typography.titleLarge,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onTertiaryContainer,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                    )
                }
            }
        }

        Surface(
            shape = CircleShape,
            color = badgeBg,
        ) {
            Row(
                modifier = Modifier.padding(horizontal = 12.dp, vertical = 7.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(6.dp),
            ) {
                Icon(
                    imageVector = when {
                        isLower -> Icons.Rounded.TrendingDown
                        isHigher -> Icons.Rounded.TrendingUp
                        else -> Icons.Rounded.TrendingFlat
                    },
                    contentDescription = null,
                    tint = badgeFg,
                    modifier = Modifier.size(16.dp),
                )
                val deltaText = when {
                    pctChange != null && isLower -> "${abs(pctChange)}% less (${formatCurrency(abs(diff), false)} saved vs $labelB)"
                    pctChange != null && isHigher -> "${abs(pctChange)}% more (+${formatCurrency(abs(diff), false)} vs $labelB)"
                    totalB <= 0.01 && totalA > 0.0 -> "${formatCurrency(totalA, false)} spent (no data in $labelB)"
                    else -> "Matching pace with $labelB"
                }
                Text(
                    text = deltaText,
                    style = MaterialTheme.typography.labelMedium,
                    color = badgeFg,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
            }
        }
    }
}

@Composable
private fun CumulativeMonthPaceChart(
    monthA: MonthKey,
    txA: List<Transaction>,
    monthB: MonthKey,
    txB: List<Transaction>,
    today: LocalDay,
) {
    val currency = LocalAppCurrency.current
    val maxDayA = if (monthA.year == today.year && monthA.month == today.month) {
        today.dayOfMonth
    } else {
        monthA.daysInMonth()
    }
    val maxDayB = monthB.daysInMonth()

    val curveA = remember(txA, maxDayA) {
        val daily = DoubleArray(32)
        txA.forEach { daily[it.date.dayOfMonth.coerceIn(1, 31)] += it.amount }
        var running = 0.0
        (1..maxDayA).map { d ->
            running += daily[d]
            running
        }
    }
    val curveB = remember(txB, maxDayB) {
        val daily = DoubleArray(32)
        txB.forEach { daily[it.date.dayOfMonth.coerceIn(1, 31)] += it.amount }
        var running = 0.0
        (1..maxDayB).map { d ->
            running += daily[d]
            running
        }
    }

    val axisMax = remember(curveA, curveB) {
        niceAxisCeiling(maxOf(curveA.lastOrNull() ?: 0.0, curveB.lastOrNull() ?: 0.0) * 1.15)
    }

    val colorA = MaterialTheme.colorScheme.primary
    val colorB = MaterialTheme.colorScheme.tertiary
    val gridColor = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.4f)
    val pathA = remember { Path() }
    val pathB = remember { Path() }

    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        Text(
            text = stringResource(R.string.insights_cumulative_pace_title),
            style = MaterialTheme.typography.labelLarge,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .height(152.dp),
        ) {
            Column(
                modifier = Modifier
                    .fillMaxHeight()
                    .padding(end = 8.dp, top = 4.dp, bottom = 4.dp),
                verticalArrangement = Arrangement.SpaceBetween,
                horizontalAlignment = Alignment.End,
            ) {
                Text(
                    text = formatCurrencyValue(axisMax, currency, false, compact = true),
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
                Text(
                    text = formatCurrencyValue(axisMax / 2.0, currency, false, compact = true),
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
                Text(
                    text = formatCurrencyValue(0.0, currency, false, compact = true),
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }

            Canvas(
                modifier = Modifier
                    .weight(1f)
                    .fillMaxHeight(),
            ) {
                val topPad = 8.dp.toPx()
                val bottomPad = 8.dp.toPx()
                val chartH = (size.height - topPad - bottomPad).coerceAtLeast(1f)
                val baseY = topPad + chartH
                val midY = topPad + chartH / 2f
                val gridStroke = 1.dp.toPx()

                drawLine(
                    color = gridColor,
                    start = Offset(0f, topPad),
                    end = Offset(size.width, topPad),
                    strokeWidth = gridStroke,
                    pathEffect = ComparisonGridDashEffect,
                )
                drawLine(
                    color = gridColor,
                    start = Offset(0f, midY),
                    end = Offset(size.width, midY),
                    strokeWidth = gridStroke,
                    pathEffect = ComparisonGridDashEffect,
                )
                drawLine(
                    color = gridColor,
                    start = Offset(0f, baseY),
                    end = Offset(size.width, baseY),
                    strokeWidth = 1.5.dp.toPx(),
                )

                // Draw Month B curve (dashed tertiary)
                if (curveB.isNotEmpty()) {
                    pathB.reset()
                    curveB.forEachIndexed { idx, value ->
                        val x = (idx / 30f).coerceIn(0f, 1f) * size.width
                        val y = baseY - (value / axisMax).toFloat().coerceIn(0f, 1f) * chartH
                        if (idx == 0) pathB.moveTo(x, y) else pathB.lineTo(x, y)
                    }
                    drawPath(
                        path = pathB,
                        color = colorB,
                        style = Stroke(
                            width = 2.4.dp.toPx(),
                            cap = StrokeCap.Round,
                            join = StrokeJoin.Round,
                            pathEffect = ComparisonCurveDashEffect,
                        ),
                    )
                }

                // Draw Month A curve (solid primary)
                if (curveA.isNotEmpty()) {
                    pathA.reset()
                    var lastX = 0f
                    var lastY = baseY
                    curveA.forEachIndexed { idx, value ->
                        val x = (idx / 30f).coerceIn(0f, 1f) * size.width
                        val y = baseY - (value / axisMax).toFloat().coerceIn(0f, 1f) * chartH
                        if (idx == 0) pathA.moveTo(x, y) else pathA.lineTo(x, y)
                        lastX = x
                        lastY = y
                    }
                    drawPath(
                        path = pathA,
                        color = colorA,
                        style = Stroke(
                            width = 3.dp.toPx(),
                            cap = StrokeCap.Round,
                            join = StrokeJoin.Round,
                        ),
                    )
                    drawCircle(color = colorA, radius = 4.5.dp.toPx(), center = Offset(lastX, lastY))
                }
            }
        }
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(start = 40.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
        ) {
            listOf("Day 1", "Day 10", "Day 20", "Day 31").forEach { lbl ->
                Text(
                    text = lbl,
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
        }
    }
}

@Composable
private fun CategoryDeltaComparisonCard(
    title: String,
    labelA: String,
    labelB: String,
    categories: List<BudgetCategory>,
    txA: List<Transaction>,
    txB: List<Transaction>,
) {
    val finColors = LocalFinancialColors.current
    val rows = remember(categories, txA, txB) {
        categories.map { cat ->
            val a = txA.filter { it.categoryId == cat.id }.sumOf { it.amount }
            val b = txB.filter { it.categoryId == cat.id }.sumOf { it.amount }
            Triple(cat, a, b)
        }.filter { it.second > 0.0 || it.third > 0.0 }
            .sortedByDescending { maxOf(it.second, it.third) }
    }
    val maxVal = remember(rows) {
        rows.maxOfOrNull { maxOf(it.second, it.third) }?.coerceAtLeast(1.0) ?: 1.0
    }

    Surface(
        shape = RoundedCornerShape(32.dp),
        color = MaterialTheme.colorScheme.surfaceContainerLow,
        modifier = Modifier.fillMaxWidth(),
    ) {
        Column(
            modifier = Modifier.padding(20.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp),
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Text(text = title, style = MaterialTheme.typography.titleLarge)
                Text(
                    text = "$labelA vs $labelB",
                    style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }

            if (rows.isEmpty()) {
                Text(
                    text = stringResource(R.string.insights_empty_comparison),
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            } else {
                rows.forEach { (cat, valA, valB) ->
                    val diff = valA - valB
                    Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically,
                        ) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(8.dp),
                                modifier = Modifier.weight(1f),
                            ) {
                                Icon(
                                    imageVector = cat.icon,
                                    contentDescription = null,
                                    tint = cat.accentColor,
                                    modifier = Modifier.size(18.dp),
                                )
                                Text(
                                    text = cat.name,
                                    style = MaterialTheme.typography.titleSmall,
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis,
                                )
                            }
                            Text(
                                text = "${formatCurrency(valA, false)} vs ${formatCurrency(valB, false)}",
                                style = MaterialTheme.typography.labelLarge,
                                color = MaterialTheme.colorScheme.onSurface,
                            )
                            Spacer(Modifier.width(8.dp))
                            val deltaSign = if (diff > 0.5) "+" else if (diff < -0.5) "-" else ""
                            Text(
                                text = "$deltaSign${formatCurrency(abs(diff), false)}",
                                style = MaterialTheme.typography.labelMedium,
                                fontWeight = FontWeight.Bold,
                                color = when {
                                    diff < -0.5 -> finColors.healthy
                                    diff > 0.5 -> finColors.warning
                                    else -> MaterialTheme.colorScheme.onSurfaceVariant
                                },
                            )
                        }
                        Column(verticalArrangement = Arrangement.spacedBy(3.dp)) {
                            LinearProgressIndicator(
                                progress = { (valA / maxVal).toFloat().coerceIn(0f, 1f) },
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .height(6.dp)
                                    .clip(CircleShape),
                                color = MaterialTheme.colorScheme.primary,
                                trackColor = MaterialTheme.colorScheme.surfaceContainerHighest,
                            )
                            LinearProgressIndicator(
                                progress = { (valB / maxVal).toFloat().coerceIn(0f, 1f) },
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .height(5.dp)
                                    .clip(CircleShape),
                                color = MaterialTheme.colorScheme.tertiary.copy(alpha = 0.72f),
                                trackColor = MaterialTheme.colorScheme.surfaceContainerHighest,
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun InsightsMiniStat(
    label: String,
    value: String,
    modifier: Modifier = Modifier,
    accent: Color? = null,
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
                color = accent ?: MaterialTheme.colorScheme.onSurfaceVariant,
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
