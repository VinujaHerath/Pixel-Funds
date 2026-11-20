package com.example.ui.screens

import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.KeyboardArrowLeft
import androidx.compose.material.icons.automirrored.rounded.KeyboardArrowRight
import androidx.compose.material.icons.automirrored.rounded.ReceiptLong
import androidx.compose.material.icons.automirrored.rounded.TrendingUp
import androidx.compose.material.icons.rounded.Add
import androidx.compose.material.icons.rounded.CheckCircle
import androidx.compose.material.icons.rounded.Edit
import androidx.compose.material.icons.rounded.ErrorOutline
import androidx.compose.material.icons.rounded.RestartAlt
import androidx.compose.material.icons.rounded.WarningAmber
import androidx.compose.material3.BottomSheetDefaults
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.FilterChip
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawWithCache
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.example.R
import com.example.data.local.CategoryMonthlySpend
import com.example.ui.MonthlyBudgetHealth
import com.example.ui.MonthlyBudgetOverviewLoadState
import com.example.ui.MonthlyBudgetOverviewUiState
import com.example.ui.components.LocalAppCurrency
import com.example.ui.components.LocalReduceMotion
import com.example.ui.components.formatCurrency
import com.example.ui.components.rememberPixelHaptics
import com.example.ui.theme.ExpressiveMotion
import com.example.ui.theme.LocalFinancialColors
import com.example.ui.theme.budgetStatusPalette
import kotlin.math.roundToInt

@Composable
fun MonthlyBudgetOverviewScreen(
    state: MonthlyBudgetOverviewUiState,
    contentPadding: PaddingValues,
    listState: LazyListState = rememberLazyListState(),
    onPreviousMonth: () -> Unit,
    onNextMonth: () -> Unit,
    onJumpToCurrentMonth: () -> Unit,
    onSetMonthlyLimit: (Double) -> Unit,
    onResetToCategorySum: () -> Unit,
    onLogExpense: () -> Unit,
    onOpenCategoryLimits: () -> Unit,
    headerContent: (@Composable () -> Unit)? = null,
) {
    val performHaptic = rememberPixelHaptics()
    var showLimitSheet by remember { mutableStateOf(false) }
    var showAllCategories by remember { mutableStateOf(false) }

    if (showLimitSheet) {
        SetMonthlyBudgetLimitSheet(
            periodTitle = state.periodTitle,
            currentLimit = state.budgetLimit,
            categoryLimitsSum = state.categoryLimitsSum,
            hasCustomLimit = state.hasCustomLimit,
            onDismiss = { showLimitSheet = false },
            onSaveLimit = { newLimit ->
                onSetMonthlyLimit(newLimit)
                showLimitSheet = false
            },
            onResetToCategorySum = {
                onResetToCategorySum()
                showLimitSheet = false
            },
        )
    }

    LazyColumn(
        state = listState,
        modifier = Modifier
            .fillMaxSize()
            .testTag("monthly_budget_overview_screen"),
        contentPadding = contentPadding,
        verticalArrangement = Arrangement.spacedBy(20.dp),
    ) {
        if (headerContent != null) {
            item(key = "overview_external_header", contentType = "overview_header") {
                headerContent()
            }
        }

        // Clean Inline Month Switcher
        item(key = "overview_period_bar", contentType = "overview_period_bar") {
            InlinePeriodSelectorRow(
                periodTitle = state.periodTitle,
                isCurrentPeriod = state.isCurrentPeriod,
                onPreviousMonth = {
                    performHaptic(HapticFeedbackType.TextHandleMove)
                    onPreviousMonth()
                },
                onNextMonth = {
                    performHaptic(HapticFeedbackType.TextHandleMove)
                    onNextMonth()
                },
                onJumpToCurrentMonth = {
                    performHaptic(HapticFeedbackType.TextHandleMove)
                    onJumpToCurrentMonth()
                },
            )
        }

        when (val load = state.loadState) {
            MonthlyBudgetOverviewLoadState.Loading -> {
                item(key = "overview_loading") {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(240.dp),
                        contentAlignment = Alignment.Center,
                    ) {
                        CircularProgressIndicator(color = MaterialTheme.colorScheme.primary)
                    }
                }
            }

            is MonthlyBudgetOverviewLoadState.Error -> {
                item(key = "overview_error") {
                    Surface(
                        shape = RoundedCornerShape(28.dp),
                        color = MaterialTheme.colorScheme.errorContainer,
                        modifier = Modifier.fillMaxWidth(),
                    ) {
                        Row(
                            modifier = Modifier.padding(20.dp),
                            horizontalArrangement = Arrangement.spacedBy(12.dp),
                            verticalAlignment = Alignment.CenterVertically,
                        ) {
                            Icon(
                                imageVector = Icons.Rounded.ErrorOutline,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.onErrorContainer,
                            )
                            Text(
                                text = load.message,
                                style = MaterialTheme.typography.bodyLarge,
                                color = MaterialTheme.colorScheme.onErrorContainer,
                            )
                        }
                    }
                }
            }

            MonthlyBudgetOverviewLoadState.Ready -> {
                // 1. Clean Hero Gauge Card (Remaining + Spent vs Limit)
                item(key = "overview_hero_gauge", contentType = "overview_hero_gauge") {
                    MonthlyLimitComparisonHeroCard(
                        state = state,
                        onEditLimitClick = {
                            performHaptic(HapticFeedbackType.TextHandleMove)
                            showLimitSheet = true
                        },
                    )
                }

                // 2. Simple 2-Tile Pace Strip (Safe / day + Projected end)
                item(key = "overview_pace_strip", contentType = "overview_pace_strip") {
                    MonthlySimplePaceRow(state = state)
                }

                // 3. Grouped Category Breakdown Card
                item(key = "overview_categories_section", contentType = "overview_categories") {
                    Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(horizontal = 4.dp),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically,
                        ) {
                            Text(
                                text = stringResource(R.string.overview_category_breakdown_title),
                                style = MaterialTheme.typography.titleLarge,
                            )
                            TextButton(
                                onClick = {
                                    performHaptic(HapticFeedbackType.TextHandleMove)
                                    onOpenCategoryLimits()
                                },
                                modifier = Modifier.testTag("overview_manage_categories_button"),
                            ) {
                                Text(stringResource(R.string.overview_tune_limits))
                            }
                        }

                        if (state.categoryBreakdowns.isEmpty()) {
                            OverviewEmptyStateCard(
                                title = stringResource(R.string.overview_empty_title),
                                subtitle = stringResource(R.string.overview_empty_subtitle),
                                onLogExpense = onLogExpense,
                            )
                        } else {
                            val displayed = if (showAllCategories) {
                                state.categoryBreakdowns
                            } else {
                                state.categoryBreakdowns.take(4)
                            }
                            Surface(
                                shape = RoundedCornerShape(28.dp),
                                color = MaterialTheme.colorScheme.surfaceContainerLow,
                                modifier = Modifier.fillMaxWidth(),
                            ) {
                                Column(modifier = Modifier.padding(vertical = 4.dp)) {
                                    displayed.forEachIndexed { index, row ->
                                        OverviewCategoryRow(
                                            item = row,
                                            warningThreshold = state.warningThresholdRatio,
                                        )
                                        if (index < displayed.lastIndex) {
                                            HorizontalDivider(
                                                modifier = Modifier.padding(horizontal = 20.dp),
                                                color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.4f),
                                            )
                                        }
                                    }

                                    if (state.categoryBreakdowns.size > 4) {
                                        HorizontalDivider(
                                            modifier = Modifier.padding(horizontal = 20.dp),
                                            color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.4f),
                                        )
                                        Box(
                                            modifier = Modifier
                                                .fillMaxWidth()
                                                .padding(vertical = 4.dp),
                                            contentAlignment = Alignment.Center,
                                        ) {
                                            TextButton(
                                                onClick = {
                                                    performHaptic(HapticFeedbackType.TextHandleMove)
                                                    showAllCategories = !showAllCategories
                                                },
                                                modifier = Modifier.testTag("overview_toggle_all_categories_button"),
                                            ) {
                                                Text(
                                                    text = if (showAllCategories) {
                                                        stringResource(R.string.overview_show_fewer_categories)
                                                    } else {
                                                        stringResource(
                                                            R.string.overview_show_all_categories,
                                                            state.categoryBreakdowns.size,
                                                        )
                                                    },
                                                )
                                            }
                                        }
                                    }
                                }
                            }
                        }
                    }
                }

                // 4. Compact Recent Expenses Card (Latest 3)
                item(key = "overview_recent_section", contentType = "overview_recent") {
                    Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(horizontal = 4.dp),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically,
                        ) {
                            Text(
                                text = stringResource(R.string.overview_recent_expenses_title),
                                style = MaterialTheme.typography.titleLarge,
                            )
                            Text(
                                text = stringResource(
                                    R.string.overview_expense_count_badge,
                                    state.transactionCount,
                                ),
                                style = MaterialTheme.typography.labelMedium,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                            )
                        }

                        if (state.recentTransactions.isEmpty()) {
                            OverviewEmptyStateCard(
                                title = stringResource(R.string.overview_no_month_spend_title),
                                subtitle = stringResource(R.string.overview_no_month_spend_subtitle),
                                onLogExpense = onLogExpense,
                            )
                        } else {
                            val categoryById = remember(state.categoryBreakdowns) {
                                state.categoryBreakdowns.associateBy { it.categoryId }
                            }
                            val latestThree = remember(state.recentTransactions) {
                                state.recentTransactions.take(3)
                            }
                            Surface(
                                shape = RoundedCornerShape(28.dp),
                                color = MaterialTheme.colorScheme.surfaceContainerLow,
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .testTag("overview_recent_transactions_card"),
                            ) {
                                Column(modifier = Modifier.padding(vertical = 4.dp)) {
                                    latestThree.forEachIndexed { index, tx ->
                                        val cat = categoryById[tx.categoryId]
                                        Row(
                                            modifier = Modifier
                                                .fillMaxWidth()
                                                .padding(horizontal = 18.dp, vertical = 13.dp),
                                            horizontalArrangement = Arrangement.SpaceBetween,
                                            verticalAlignment = Alignment.CenterVertically,
                                        ) {
                                            Row(
                                                verticalAlignment = Alignment.CenterVertically,
                                                horizontalArrangement = Arrangement.spacedBy(12.dp),
                                                modifier = Modifier.weight(1f),
                                            ) {
                                                Box(
                                                    modifier = Modifier
                                                        .size(38.dp)
                                                        .clip(CircleShape)
                                                        .background(
                                                            (cat?.accentColor ?: MaterialTheme.colorScheme.primary)
                                                                .copy(alpha = 0.14f)
                                                        ),
                                                    contentAlignment = Alignment.Center,
                                                ) {
                                                    Icon(
                                                        imageVector = cat?.icon ?: Icons.AutoMirrored.Rounded.ReceiptLong,
                                                        contentDescription = null,
                                                        tint = cat?.accentColor ?: MaterialTheme.colorScheme.primary,
                                                        modifier = Modifier.size(19.dp),
                                                    )
                                                }
                                                Column {
                                                    Text(
                                                        text = tx.merchantOrNote,
                                                        style = MaterialTheme.typography.titleMedium,
                                                        maxLines = 1,
                                                        overflow = TextOverflow.Ellipsis,
                                                    )
                                                    Text(
                                                        text = "${cat?.categoryName ?: "Expense"} • ${tx.date.formatShort()}",
                                                        style = MaterialTheme.typography.labelMedium,
                                                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                                                    )
                                                }
                                            }
                                            Spacer(Modifier.width(8.dp))
                                            Text(
                                                text = "-${formatCurrency(tx.amount, showCents = true)}",
                                                style = MaterialTheme.typography.titleMedium,
                                                fontWeight = FontWeight.SemiBold,
                                            )
                                        }
                                        if (index < latestThree.lastIndex) {
                                            HorizontalDivider(
                                                modifier = Modifier.padding(horizontal = 18.dp),
                                                color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.4f),
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
}

@Composable
private fun InlinePeriodSelectorRow(
    periodTitle: String,
    isCurrentPeriod: Boolean,
    onPreviousMonth: () -> Unit,
    onNextMonth: () -> Unit,
    onJumpToCurrentMonth: () -> Unit,
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 2.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween,
    ) {
        IconButton(
            onClick = onPreviousMonth,
            modifier = Modifier
                .size(48.dp)
                .testTag("overview_prev_month_button"),
        ) {
            Icon(
                imageVector = Icons.AutoMirrored.Rounded.KeyboardArrowLeft,
                contentDescription = stringResource(R.string.cd_previous_month),
            )
        }

        Row(
            modifier = Modifier.weight(1f),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(8.dp, Alignment.CenterHorizontally),
        ) {
            Text(
                text = periodTitle.ifBlank { stringResource(R.string.overview_title) },
                style = MaterialTheme.typography.titleLarge,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
                modifier = Modifier
                    .weight(1f, fill = false)
                    .testTag("overview_period_title"),
            )
            if (!isCurrentPeriod) {
                Surface(
                    onClick = onJumpToCurrentMonth,
                    shape = CircleShape,
                    color = MaterialTheme.colorScheme.primaryContainer,
                    modifier = Modifier.testTag("overview_jump_current_button"),
                ) {
                    Text(
                        text = stringResource(R.string.overview_today_badge),
                        style = MaterialTheme.typography.labelMedium,
                        color = MaterialTheme.colorScheme.onPrimaryContainer,
                        modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp),
                    )
                }
            }
        }

        IconButton(
            onClick = onNextMonth,
            enabled = !isCurrentPeriod,
            modifier = Modifier
                .size(48.dp)
                .testTag("overview_next_month_button"),
        ) {
            Icon(
                imageVector = Icons.AutoMirrored.Rounded.KeyboardArrowRight,
                contentDescription = stringResource(R.string.cd_next_month),
            )
        }
    }
}

@Composable
private fun MonthlyLimitComparisonHeroCard(
    state: MonthlyBudgetOverviewUiState,
    onEditLimitClick: () -> Unit,
) {
    val financialColors = LocalFinancialColors.current
    val reduceMotion = LocalReduceMotion.current
    val statusPalette = budgetStatusPalette(
        ratio = state.usageRatio,
        warningThreshold = state.warningThresholdRatio,
    )

    val animatedRatioState = animateFloatAsState(
        targetValue = state.usageRatio.coerceIn(0f, 1f),
        animationSpec = if (reduceMotion) ExpressiveMotion.fastEffects() else ExpressiveMotion.slowSpatial(),
        label = "MonthlyGaugeRatio",
    )
    val gaugeAccentState = animateColorAsState(
        targetValue = statusPalette.accent,
        animationSpec = ExpressiveMotion.fastEffects(),
        label = "MonthlyGaugeAccent",
    )

    val statusText = when (state.health) {
        MonthlyBudgetHealth.ON_TRACK -> stringResource(R.string.overview_status_on_track)
        MonthlyBudgetHealth.APPROACHING -> stringResource(R.string.overview_status_approaching)
        MonthlyBudgetHealth.OVER_LIMIT -> stringResource(R.string.overview_status_over_limit)
        MonthlyBudgetHealth.NO_LIMIT_SET -> stringResource(R.string.overview_status_no_limit)
    }
    val statusIcon = when (state.health) {
        MonthlyBudgetHealth.ON_TRACK -> Icons.Rounded.CheckCircle
        MonthlyBudgetHealth.APPROACHING, MonthlyBudgetHealth.OVER_LIMIT -> Icons.Rounded.WarningAmber
        MonthlyBudgetHealth.NO_LIMIT_SET -> Icons.AutoMirrored.Rounded.TrendingUp
    }

    Surface(
        shape = RoundedCornerShape(32.dp),
        color = MaterialTheme.colorScheme.surfaceContainerLow,
        modifier = Modifier
            .fillMaxWidth()
            .testTag("overview_hero_card"),
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(22.dp),
            verticalArrangement = Arrangement.spacedBy(18.dp),
        ) {
            // Top bar: Status pill + Set Limit button
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Surface(
                    shape = CircleShape,
                    color = statusPalette.container,
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(6.dp),
                    ) {
                        Icon(
                            imageVector = statusIcon,
                            contentDescription = null,
                            tint = statusPalette.onContainer,
                            modifier = Modifier.size(16.dp),
                        )
                        Text(
                            text = statusText,
                            style = MaterialTheme.typography.labelLarge,
                            color = statusPalette.onContainer,
                            modifier = Modifier.testTag("overview_health_badge"),
                        )
                    }
                }

                FilledTonalButton(
                    onClick = onEditLimitClick,
                    shape = CircleShape,
                    contentPadding = PaddingValues(horizontal = 14.dp, vertical = 8.dp),
                    modifier = Modifier.testTag("overview_set_limit_button"),
                ) {
                    Icon(
                        imageVector = Icons.Rounded.Edit,
                        contentDescription = null,
                        modifier = Modifier.size(15.dp),
                    )
                    Spacer(Modifier.width(6.dp))
                    Text(
                        text = stringResource(R.string.overview_set_limit),
                        style = MaterialTheme.typography.labelLarge,
                    )
                }
            }

            // Clean, airy arc gauge with just 2 focal readouts
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(176.dp),
                contentAlignment = Alignment.Center,
            ) {
                val trackColor = MaterialTheme.colorScheme.surfaceContainerHighest
                val hasSpend = state.totalSpent > 0.0
                Box(
                    modifier = Modifier
                        .size(196.dp)
                        .drawWithCache {
                            val strokePx = 15.dp.toPx()
                            val inset = strokePx / 2f + 4.dp.toPx()
                            val arcSize = Size(size.width - inset * 2f, size.height - inset * 2f)
                            val topLeft = Offset(inset, inset)
                            val arcStroke = Stroke(width = strokePx, cap = StrokeCap.Round)
                            val startAngle = 150f
                            val maxSweep = 240f
                            onDrawBehind {
                                drawArc(
                                    color = trackColor,
                                    startAngle = startAngle,
                                    sweepAngle = maxSweep,
                                    useCenter = false,
                                    topLeft = topLeft,
                                    size = arcSize,
                                    style = arcStroke,
                                )
                                val activeSweep = (maxSweep * animatedRatioState.value)
                                    .coerceAtLeast(if (hasSpend) 4f else 0f)
                                if (activeSweep > 0f) {
                                    drawArc(
                                        color = gaugeAccentState.value,
                                        startAngle = startAngle,
                                        sweepAngle = activeSweep,
                                        useCenter = false,
                                        topLeft = topLeft,
                                        size = arcSize,
                                        style = arcStroke,
                                    )
                                }
                            }
                        }
                        .testTag("overview_gauge_canvas"),
                )

                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(4.dp),
                    modifier = Modifier.padding(top = 14.dp),
                ) {
                    Text(
                        text = if (state.isOverBudget) {
                            formatCurrency(state.overBudgetAmount, showCents = false)
                        } else {
                            formatCurrency(state.remainingAmount, showCents = false)
                        },
                        style = MaterialTheme.typography.headlineLarge,
                        color = if (state.isOverBudget) financialColors.danger else MaterialTheme.colorScheme.onSurface,
                        modifier = Modifier.testTag("overview_remaining_text"),
                    )
                    Text(
                        text = if (state.budgetLimit > 0.0) {
                            if (state.isOverBudget) "${state.usagePercent}% • over limit"
                            else "${state.usagePercent}% used"
                        } else "—",
                        style = MaterialTheme.typography.labelLarge,
                        color = statusPalette.accent,
                        modifier = Modifier.testTag("overview_usage_percent"),
                    )
                }
            }

            HorizontalDivider(
                color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.45f),
            )

            // Clean 2-column Spent vs Monthly Limit footer (no extra nested card)
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = stringResource(R.string.overview_spent_this_month),
                        style = MaterialTheme.typography.labelMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                    Text(
                        text = formatCurrency(state.totalSpent, showCents = true),
                        style = MaterialTheme.typography.headlineSmall,
                        color = MaterialTheme.colorScheme.onSurface,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                        modifier = Modifier.testTag("overview_total_spent_value"),
                    )
                }

                Box(
                    modifier = Modifier
                        .width(1.dp)
                        .height(34.dp)
                        .background(MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.45f)),
                )

                Column(
                    modifier = Modifier
                        .weight(1f)
                        .padding(start = 16.dp),
                ) {
                    Text(
                        text = stringResource(R.string.overview_budget_limit_label),
                        style = MaterialTheme.typography.labelMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                    Text(
                        text = formatCurrency(state.budgetLimit, showCents = true),
                        style = MaterialTheme.typography.headlineSmall,
                        color = MaterialTheme.colorScheme.primary,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                        modifier = Modifier.testTag("overview_budget_limit_value"),
                    )
                }
            }
        }
    }
}

@Composable
private fun MonthlySimplePaceRow(state: MonthlyBudgetOverviewUiState) {
    val financialColors = LocalFinancialColors.current
    val projectedOver = state.budgetLimit > 0.0 && state.projectedMonthEndSpend > state.budgetLimit

    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        // Left: Safe / day
        Surface(
            shape = RoundedCornerShape(26.dp),
            color = MaterialTheme.colorScheme.surfaceContainerLow,
            modifier = Modifier
                .weight(1f)
                .heightIn(min = 108.dp),
        ) {
            Column(
                modifier = Modifier.padding(18.dp),
                verticalArrangement = Arrangement.spacedBy(6.dp),
            ) {
                Text(
                    text = stringResource(R.string.dashboard_safe_per_day),
                    style = MaterialTheme.typography.labelLarge,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
                Text(
                    text = formatCurrency(state.safeDailyAllowance, showCents = false),
                    style = MaterialTheme.typography.headlineSmall,
                    color = MaterialTheme.colorScheme.onSurface,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
                Text(
                    text = if (state.isCurrentPeriod) {
                        stringResource(R.string.overview_days_left_label, state.daysRemaining)
                    } else {
                        stringResource(R.string.overview_completed_period)
                    },
                    style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
        }

        // Right: Projected end
        Surface(
            shape = RoundedCornerShape(26.dp),
            color = MaterialTheme.colorScheme.surfaceContainerLow,
            modifier = Modifier
                .weight(1f)
                .heightIn(min = 108.dp),
        ) {
            Column(
                modifier = Modifier.padding(18.dp),
                verticalArrangement = Arrangement.spacedBy(6.dp),
            ) {
                Text(
                    text = if (state.isCurrentPeriod) {
                        stringResource(R.string.overview_projected_end_label)
                    } else {
                        stringResource(R.string.overview_final_month_total)
                    },
                    style = MaterialTheme.typography.labelLarge,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
                Text(
                    text = formatCurrency(state.projectedMonthEndSpend, showCents = false),
                    style = MaterialTheme.typography.headlineSmall,
                    color = if (projectedOver) financialColors.danger else MaterialTheme.colorScheme.onSurface,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
                Text(
                    text = if (projectedOver) {
                        stringResource(R.string.overview_status_over_limit)
                    } else {
                        stringResource(R.string.overview_status_on_track)
                    },
                    style = MaterialTheme.typography.labelMedium,
                    color = if (projectedOver) financialColors.danger else financialColors.healthy,
                )
            }
        }
    }
}

@Composable
private fun OverviewCategoryRow(
    item: CategoryMonthlySpend,
    warningThreshold: Float,
) {
    val statusPalette = budgetStatusPalette(item.usageRatio, warningThreshold)
    val animatedProgressState = animateFloatAsState(
        targetValue = item.usageRatio.coerceIn(0f, 1f),
        animationSpec = ExpressiveMotion.defaultSpatial(),
        label = "OverviewCatProgress_${item.categoryId}",
    )

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 18.dp, vertical = 14.dp)
            .testTag("overview_category_row_${item.categoryId}"),
        verticalArrangement = Arrangement.spacedBy(10.dp),
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(12.dp),
                modifier = Modifier.weight(1f),
            ) {
                Box(
                    modifier = Modifier
                        .size(38.dp)
                        .clip(CircleShape)
                        .background(item.accentColor.copy(alpha = 0.15f)),
                    contentAlignment = Alignment.Center,
                ) {
                    Icon(
                        imageVector = item.icon,
                        contentDescription = null,
                        tint = item.accentColor,
                        modifier = Modifier.size(19.dp),
                    )
                }
                Column {
                    Text(
                        text = item.categoryName,
                        style = MaterialTheme.typography.titleMedium,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                    )
                    Text(
                        text = if (item.remainingAmount >= 0.0) {
                            stringResource(
                                R.string.overview_remaining_short,
                                formatCurrency(item.remainingAmount, false),
                            )
                        } else {
                            stringResource(
                                R.string.overview_over_short,
                                formatCurrency(-item.remainingAmount, false),
                            )
                        },
                        style = MaterialTheme.typography.labelMedium,
                        color = statusPalette.accent,
                    )
                }
            }

            Spacer(Modifier.width(8.dp))

            Text(
                text = "${formatCurrency(item.spentAmount, false)} / ${formatCurrency(item.categoryLimit, false)}",
                style = MaterialTheme.typography.titleSmall,
                fontWeight = FontWeight.SemiBold,
            )
        }

        LinearProgressIndicator(
            progress = { animatedProgressState.value },
            modifier = Modifier
                .fillMaxWidth()
                .height(6.dp)
                .clip(CircleShape),
            color = statusPalette.accent,
            trackColor = MaterialTheme.colorScheme.surfaceContainerHighest,
        )
    }
}

@Composable
private fun OverviewEmptyStateCard(
    title: String,
    subtitle: String,
    onLogExpense: () -> Unit,
) {
    Surface(
        shape = RoundedCornerShape(28.dp),
        color = MaterialTheme.colorScheme.surfaceContainerLow,
        modifier = Modifier.fillMaxWidth(),
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(24.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(10.dp),
        ) {
            Box(
                modifier = Modifier
                    .size(48.dp)
                    .clip(CircleShape)
                    .background(MaterialTheme.colorScheme.primaryContainer),
                contentAlignment = Alignment.Center,
            ) {
                Icon(
                    imageVector = Icons.AutoMirrored.Rounded.ReceiptLong,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.onPrimaryContainer,
                )
            }
            Text(
                text = title,
                style = MaterialTheme.typography.titleMedium,
                textAlign = TextAlign.Center,
            )
            Text(
                text = subtitle,
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                textAlign = TextAlign.Center,
            )
            FilledTonalButton(
                onClick = onLogExpense,
                shape = CircleShape,
                modifier = Modifier.testTag("overview_log_expense_button"),
            ) {
                Icon(Icons.Rounded.Add, contentDescription = null, modifier = Modifier.size(18.dp))
                Spacer(Modifier.width(6.dp))
                Text(stringResource(R.string.overview_log_expense_cta))
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class, ExperimentalLayoutApi::class)
@Composable
private fun SetMonthlyBudgetLimitSheet(
    periodTitle: String,
    currentLimit: Double,
    categoryLimitsSum: Double,
    hasCustomLimit: Boolean,
    onDismiss: () -> Unit,
    onSaveLimit: (Double) -> Unit,
    onResetToCategorySum: () -> Unit,
) {
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    val currency = LocalAppCurrency.current
    val performHaptic = rememberPixelHaptics()

    var amountInput by remember(currentLimit) {
        val initial = if (currentLimit > 0.0) {
            if (currentLimit % 1.0 == 0.0) currentLimit.toLong().toString()
            else String.format(java.util.Locale.US, "%.2f", currentLimit)
        } else {
            ""
        }
        mutableStateOf(initial)
    }

    val parsedAmount = amountInput.replace(",", "").toDoubleOrNull()
    val isValid = parsedAmount != null && parsedAmount > 0.0 && parsedAmount <= 10_000_000.0

    val quickPresets = remember(categoryLimitsSum) {
        val base = if (categoryLimitsSum > 0.0) categoryLimitsSum else 2000.0
        listOf(
            ((base * 0.85) / 50.0).roundToInt().coerceAtLeast(1) * 50.0,
            ((base) / 50.0).roundToInt().coerceAtLeast(1) * 50.0,
            ((base * 1.15) / 50.0).roundToInt().coerceAtLeast(1) * 50.0,
            ((base * 1.35) / 50.0).roundToInt().coerceAtLeast(1) * 50.0,
        ).distinct()
    }

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = sheetState,
        shape = RoundedCornerShape(topStart = 36.dp, topEnd = 36.dp),
        containerColor = MaterialTheme.colorScheme.surfaceContainerLow,
        dragHandle = { BottomSheetDefaults.DragHandle() },
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .navigationBarsPadding()
                .padding(horizontal = 24.dp)
                .padding(bottom = 28.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp),
        ) {
            Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                Text(
                    text = stringResource(R.string.overview_limit_sheet_title),
                    style = MaterialTheme.typography.headlineMedium,
                )
                Text(
                    text = stringResource(R.string.overview_limit_sheet_subtitle, periodTitle),
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }

            OutlinedTextField(
                value = amountInput,
                onValueChange = { raw ->
                    val filtered = raw.filter { it.isDigit() || it == '.' }
                    if (filtered.count { it == '.' } <= 1) {
                        amountInput = filtered.take(12)
                    }
                },
                label = {
                    Text(stringResource(R.string.overview_limit_input_label, currency.code))
                },
                prefix = { Text("${currency.symbol} ") },
                singleLine = true,
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                shape = RoundedCornerShape(22.dp),
                modifier = Modifier
                    .fillMaxWidth()
                    .testTag("overview_limit_amount_input"),
                colors = OutlinedTextFieldDefaults.colors(
                    unfocusedContainerColor = MaterialTheme.colorScheme.surfaceContainerHigh,
                    focusedContainerColor = MaterialTheme.colorScheme.surfaceContainerHighest,
                ),
            )

            FlowRow(
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalArrangement = Arrangement.spacedBy(6.dp),
            ) {
                quickPresets.forEach { preset ->
                    val isSelected = parsedAmount != null && kotlin.math.abs(parsedAmount - preset) < 0.01
                    FilterChip(
                        selected = isSelected,
                        onClick = {
                            performHaptic(HapticFeedbackType.TextHandleMove)
                            amountInput = preset.toLong().toString()
                        },
                        label = { Text(formatCurrency(preset, showCents = false)) },
                        shape = CircleShape,
                    )
                }
            }

            if (hasCustomLimit && categoryLimitsSum > 0.0) {
                OutlinedButton(
                    onClick = {
                        performHaptic(HapticFeedbackType.TextHandleMove)
                        onResetToCategorySum()
                    },
                    shape = CircleShape,
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("overview_reset_limit_to_categories_button"),
                ) {
                    Icon(
                        imageVector = Icons.Rounded.RestartAlt,
                        contentDescription = null,
                        modifier = Modifier.size(18.dp),
                    )
                    Spacer(Modifier.width(8.dp))
                    Text(
                        stringResource(
                            R.string.overview_use_category_sum,
                            formatCurrency(categoryLimitsSum, showCents = false),
                        )
                    )
                }
            }

            Button(
                onClick = {
                    val value = parsedAmount ?: return@Button
                    performHaptic(HapticFeedbackType.LongPress)
                    onSaveLimit(value)
                },
                enabled = isValid,
                shape = CircleShape,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(54.dp)
                    .testTag("overview_save_limit_button"),
            ) {
                Text(
                    text = stringResource(R.string.overview_save_limit_cta),
                    style = MaterialTheme.typography.titleMedium,
                )
            }
        }
    }
}
