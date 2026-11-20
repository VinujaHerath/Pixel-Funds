package com.example.ui.screens

import androidx.compose.animation.AnimatedVisibilityScope
import androidx.compose.animation.ExperimentalSharedTransitionApi
import androidx.compose.animation.SharedTransitionScope
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.IntrinsicSize
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
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.ArrowForward
import androidx.compose.material.icons.automirrored.rounded.HelpOutline
import androidx.compose.material.icons.automirrored.rounded.KeyboardArrowRight
import androidx.compose.material.icons.automirrored.rounded.TrendingDown
import androidx.compose.material.icons.automirrored.rounded.TrendingUp
import androidx.compose.material.icons.rounded.AutoAwesome
import androidx.compose.material.icons.rounded.CalendarToday
import androidx.compose.material.icons.rounded.Settings
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.example.R
import com.example.data.BudgetCategory
import com.example.data.CadenceUiState
import com.example.ui.components.formatCurrency
import com.example.ui.components.rememberPixelHaptics
import com.example.ui.theme.ExpressiveMotion
import com.example.ui.theme.budgetStatusPalette

@OptIn(ExperimentalSharedTransitionApi::class)
@Composable
fun DashboardScreen(
    state: CadenceUiState,
    contentPadding: PaddingValues,
    sharedTransitionScope: SharedTransitionScope,
    animatedVisibilityScope: AnimatedVisibilityScope,
    onCategoryClick: (BudgetCategory) -> Unit,
    onViewAllActivity: () -> Unit,
    onOpenBillsAndSubs: () -> Unit,
    onOpenAccount: () -> Unit,
    onOpenTutorial: () -> Unit,
    onClearSampleData: () -> Unit,
    onChangeResetDay: (Int) -> Unit,
) {
    val performHaptic = rememberPixelHaptics()
    var showCycleInfoDialog by remember { mutableStateOf(false) }
    var showResetDayPicker by remember { mutableStateOf(false) }

    if (showCycleInfoDialog) {
        val nextResetLabel = remember(state.today, state.budgetResetDay) {
            state.nextResetDate.formatShort()
        }
        val cycleHeading = remember(state.today, state.budgetResetDay) {
            if (state.budgetResetDay == 1) {
                "${state.today.monthYearFullName()} cycle"
            } else {
                "${state.cycleStart.formatShort()} – ${state.cycleEnd.formatShort()} cycle"
            }
        }
        AlertDialog(
            onDismissRequest = { showCycleInfoDialog = false },
            shape = RoundedCornerShape(28.dp),
            icon = {
                Icon(
                    imageVector = Icons.Rounded.CalendarToday,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.primary,
                )
            },
            title = {
                Text(
                    text = cycleHeading,
                    style = MaterialTheme.typography.titleLarge,
                )
            },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    Text(
                        text = "${state.daysRemaining} days left in this cycle (Day ${state.cycleDayElapsed} of ${state.cycleTotalDays}).",
                        style = MaterialTheme.typography.bodyMedium,
                        fontWeight = FontWeight.SemiBold,
                        color = MaterialTheme.colorScheme.onSurface,
                    )
                    Text(
                        text = stringResource(R.string.dashboard_cycle_synced_note, nextResetLabel),
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                    Text(
                        text = stringResource(R.string.dashboard_cycle_safe_note, state.daysRemaining),
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
            },
            dismissButton = {
                TextButton(
                    onClick = {
                        showCycleInfoDialog = false
                        showResetDayPicker = true
                    },
                    modifier = Modifier.testTag("cycle_info_change_reset_button"),
                ) {
                    Text(stringResource(R.string.dashboard_cycle_change_reset_day))
                }
            },
            confirmButton = {
                TextButton(
                    onClick = { showCycleInfoDialog = false },
                    modifier = Modifier.testTag("cycle_info_dismiss_button"),
                ) {
                    Text(stringResource(R.string.dashboard_cycle_dialog_got_it))
                }
            },
        )
    }

    if (showResetDayPicker) {
        ResetDayPickerDialog(
            currentDay = state.budgetResetDay,
            onDismiss = { showResetDayPicker = false },
            onSelectDay = { day ->
                onChangeResetDay(day)
                showResetDayPicker = false
            },
        )
    }

    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .testTag("dashboard_list"),
        contentPadding = contentPadding,
        verticalArrangement = Arrangement.spacedBy(if (state.compactCards) 14.dp else 20.dp),
    ) {
        // 1. Minimal Editorial Header
        item(key = "header") {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 4.dp, vertical = 2.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Column(modifier = Modifier.weight(1f).padding(end = 12.dp)) {
                    Text(
                        text = state.budgetPeriodTitle,
                        style = MaterialTheme.typography.labelLarge,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                    )
                    Text(
                        text = stringResource(R.string.app_name),
                        style = MaterialTheme.typography.headlineMedium,
                        color = MaterialTheme.colorScheme.onSurface,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                    )
                }

                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                ) {
                    Surface(
                        onClick = {
                            performHaptic(HapticFeedbackType.TextHandleMove)
                            onOpenTutorial()
                        },
                        shape = CircleShape,
                        color = MaterialTheme.colorScheme.surfaceContainerHigh,
                        modifier = Modifier
                            .size(48.dp)
                            .testTag("home_tutorial_button"),
                    ) {
                        Box(contentAlignment = Alignment.Center, modifier = Modifier.fillMaxSize()) {
                            Icon(
                                imageVector = Icons.AutoMirrored.Rounded.HelpOutline,
                                contentDescription = stringResource(R.string.cd_help_tutorial),
                                tint = MaterialTheme.colorScheme.onSurfaceVariant,
                                modifier = Modifier.size(22.dp),
                            )
                        }
                    }

                    Surface(
                        onClick = {
                            performHaptic(HapticFeedbackType.TextHandleMove)
                            onOpenAccount()
                        },
                        shape = CircleShape,
                        color = MaterialTheme.colorScheme.secondaryContainer,
                        modifier = Modifier
                            .size(48.dp)
                            .testTag("account_settings_button"),
                    ) {
                        Box(contentAlignment = Alignment.Center, modifier = Modifier.fillMaxSize()) {
                            Icon(
                                imageVector = Icons.Rounded.Settings,
                                contentDescription = stringResource(R.string.cd_account),
                                tint = MaterialTheme.colorScheme.onSecondaryContainer,
                                modifier = Modifier.size(22.dp),
                            )
                        }
                    }
                }
            }
        }

        // Slim Sample Data Notice (only shown if demo data is present)
        if (state.hasSampleData) {
            item(key = "sample_banner") {
                Surface(
                    shape = CircleShape,
                    color = MaterialTheme.colorScheme.surfaceContainerHigh,
                    modifier = Modifier.fillMaxWidth(),
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(start = 16.dp, end = 6.dp, top = 4.dp, bottom = 4.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(10.dp),
                    ) {
                        Icon(
                            imageVector = Icons.Rounded.AutoAwesome,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.size(16.dp),
                        )
                        Text(
                            text = stringResource(R.string.dashboard_sample_banner),
                            style = MaterialTheme.typography.labelMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis,
                            modifier = Modifier.weight(1f),
                        )
                        TextButton(
                            onClick = {
                                performHaptic(HapticFeedbackType.LongPress)
                                onClearSampleData()
                            },
                            modifier = Modifier.testTag("clear_sample_banner_button"),
                        ) {
                            Text(
                                text = stringResource(R.string.dashboard_clear_sample),
                                style = MaterialTheme.typography.labelLarge,
                            )
                        }
                    }
                }
            }
        }

        // 2. Unified Hero Balance Squircle (Remaining + Cycle Pill + Clean Progress)
        item(key = "hero_balance") {
            HeroBalanceSquircle(
                state = state,
                onCyclePillClick = {
                    performHaptic(HapticFeedbackType.TextHandleMove)
                    showCycleInfoDialog = true
                },
            )
        }

        // 3. Calm 2-Tile Glance Strip: Safe / Day + Next Bill
        item(key = "glance_strip") {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(IntrinsicSize.Max),
                horizontalArrangement = Arrangement.spacedBy(12.dp),
            ) {
                SafeDailyPaceTile(
                    state = state,
                    modifier = Modifier
                        .weight(1f)
                        .fillMaxHeight(),
                )
                NextBillTile(
                    state = state,
                    onClick = {
                        performHaptic(HapticFeedbackType.TextHandleMove)
                        onOpenBillsAndSubs()
                    },
                    modifier = Modifier
                        .weight(1f)
                        .fillMaxHeight(),
                )
            }
        }

        // 4. Clean Top Categories List (Single Grouped Card, Top 3)
        item(key = "top_categories_section") {
            val topCategories = remember(state.categories, state.transactions, state.today, state.budgetResetDay) {
                state.categories
                    .sortedByDescending { state.spentForCategoryThisMonth(it.id) }
                    .take(3)
            }

            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 4.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Text(
                        text = stringResource(R.string.dashboard_top_categories),
                        style = MaterialTheme.typography.titleLarge,
                        color = MaterialTheme.colorScheme.onSurface,
                    )
                    TextButton(
                        onClick = {
                            performHaptic(HapticFeedbackType.TextHandleMove)
                            onViewAllActivity()
                        },
                        modifier = Modifier.testTag("view_all_activity_button"),
                    ) {
                        Text(stringResource(R.string.dashboard_see_all))
                        Spacer(Modifier.width(4.dp))
                        Icon(
                            imageVector = Icons.AutoMirrored.Rounded.ArrowForward,
                            contentDescription = null,
                            modifier = Modifier.size(16.dp),
                        )
                    }
                }

                if (topCategories.isNotEmpty()) {
                    Surface(
                        shape = RoundedCornerShape(28.dp),
                        color = MaterialTheme.colorScheme.surfaceContainerLow,
                        modifier = Modifier.fillMaxWidth(),
                    ) {
                        Column(modifier = Modifier.padding(vertical = 4.dp)) {
                            topCategories.forEachIndexed { index, category ->
                                CategorySummaryRow(
                                    category = category,
                                    spent = state.spentForCategoryThisMonth(category.id),
                                    warningThreshold = state.warningThresholdRatio,
                                    sharedTransitionScope = sharedTransitionScope,
                                    animatedVisibilityScope = animatedVisibilityScope,
                                    onClick = {
                                        performHaptic(HapticFeedbackType.TextHandleMove)
                                        onCategoryClick(category)
                                    },
                                )
                                if (index < topCategories.lastIndex) {
                                    HorizontalDivider(
                                        modifier = Modifier.padding(horizontal = 20.dp),
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

@Composable
private fun HeroBalanceSquircle(
    state: CadenceUiState,
    onCyclePillClick: () -> Unit,
) {
    val isNegative = state.remainingBudget < 0.0
    val animatedRemaining by animateFloatAsState(
        targetValue = state.remainingBudget.toFloat(),
        animationSpec = ExpressiveMotion.slowSpatial(),
        label = "HeroBalanceSpring",
    )
    val animatedProgress by animateFloatAsState(
        targetValue = state.overallSpendRatio.coerceIn(0f, 1f),
        animationSpec = ExpressiveMotion.slowSpatial(),
        label = "HeroSpendProgress",
    )
    val containerColor by animateColorAsState(
        targetValue = if (isNegative) MaterialTheme.colorScheme.errorContainer
        else MaterialTheme.colorScheme.primaryContainer,
        animationSpec = ExpressiveMotion.fastEffects(),
        label = "HeroContainerColor",
    )
    val contentColor by animateColorAsState(
        targetValue = if (isNegative) MaterialTheme.colorScheme.onErrorContainer
        else MaterialTheme.colorScheme.onPrimaryContainer,
        animationSpec = ExpressiveMotion.fastEffects(),
        label = "HeroContentColor",
    )
    val glowColor = if (isNegative) MaterialTheme.colorScheme.error.copy(alpha = 0.14f)
    else MaterialTheme.colorScheme.primary.copy(alpha = 0.15f)

    Surface(
        modifier = Modifier
            .fillMaxWidth()
            .testTag("hero_balance_card"),
        shape = RoundedCornerShape(34.dp),
        color = containerColor,
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .drawBehind {
                    drawCircle(
                        brush = Brush.radialGradient(
                            colors = listOf(glowColor, Color.Transparent),
                            center = Offset(size.width * 0.88f, size.height * 0.14f),
                            radius = size.maxDimension * 0.68f,
                        ),
                        radius = size.maxDimension * 0.68f,
                        center = Offset(size.width * 0.88f, size.height * 0.14f),
                    )
                }
                .padding(24.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp),
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Text(
                    text = if (isNegative) stringResource(R.string.dashboard_over_budget)
                    else stringResource(R.string.dashboard_remaining_to_spend),
                    style = MaterialTheme.typography.titleMedium,
                    color = contentColor.copy(alpha = 0.82f),
                )

                Surface(
                    onClick = onCyclePillClick,
                    shape = CircleShape,
                    color = MaterialTheme.colorScheme.surface.copy(alpha = 0.34f),
                    modifier = Modifier.testTag("month_cycle_pill"),
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 12.dp, vertical = 7.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(6.dp),
                    ) {
                        Icon(
                            imageVector = Icons.Rounded.CalendarToday,
                            contentDescription = stringResource(R.string.cd_month_cycle_info),
                            tint = contentColor,
                            modifier = Modifier.size(13.dp),
                        )
                        Text(
                            text = "${state.daysRemaining}d left",
                            style = MaterialTheme.typography.labelMedium,
                            fontWeight = FontWeight.SemiBold,
                            color = contentColor,
                        )
                    }
                }
            }

            Text(
                text = formatCurrency(animatedRemaining.toDouble(), showCents = true),
                style = MaterialTheme.typography.displayLarge,
                color = contentColor,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )

            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                LinearProgressIndicator(
                    progress = { animatedProgress },
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(8.dp)
                        .clip(CircleShape),
                    color = contentColor,
                    trackColor = contentColor.copy(alpha = 0.18f),
                )

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Text(
                        text = stringResource(
                            R.string.dashboard_spent_of_budget,
                            formatCurrency(state.totalSpentThisMonth, showCents = false),
                            formatCurrency(state.totalMonthlyBudget, showCents = false),
                        ),
                        style = MaterialTheme.typography.labelLarge,
                        color = contentColor.copy(alpha = 0.84f),
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                    )
                    Text(
                        text = "${(state.overallSpendRatio * 100).toInt()}%",
                        style = MaterialTheme.typography.labelLarge,
                        fontWeight = FontWeight.SemiBold,
                        color = contentColor,
                    )
                }
            }
        }
    }
}

@Composable
private fun SafeDailyPaceTile(
    state: CadenceUiState,
    modifier: Modifier = Modifier,
) {
    val paceDelta = state.overallSpendRatio - state.monthElapsedRatio
    val isAhead = paceDelta > 0.05f
    val statusPalette = budgetStatusPalette(
        ratio = if (isAhead) (0.82f + paceDelta).coerceAtMost(1f) else 0.35f,
        warningThreshold = state.warningThresholdRatio,
    )

    Surface(
        modifier = modifier.testTag("spend_pace_card"),
        shape = RoundedCornerShape(26.dp),
        color = MaterialTheme.colorScheme.surfaceContainerLow,
    ) {
        Column(
            modifier = Modifier.padding(18.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            Text(
                text = stringResource(R.string.dashboard_safe_per_day),
                style = MaterialTheme.typography.labelLarge,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            Text(
                text = formatCurrency(state.safeDailySpend, showCents = false),
                style = MaterialTheme.typography.headlineMedium,
                color = MaterialTheme.colorScheme.onSurface,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(5.dp),
            ) {
                Icon(
                    imageVector = if (isAhead) Icons.AutoMirrored.Rounded.TrendingUp
                    else Icons.AutoMirrored.Rounded.TrendingDown,
                    contentDescription = null,
                    tint = statusPalette.accent,
                    modifier = Modifier.size(16.dp),
                )
                Text(
                    text = if (isAhead) stringResource(R.string.dashboard_spending_fast)
                    else stringResource(R.string.dashboard_on_pace),
                    style = MaterialTheme.typography.labelMedium,
                    color = statusPalette.accent,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
            }
        }
    }
}

@Composable
private fun NextBillTile(
    state: CadenceUiState,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val nextBill = state.nextUpcomingBill

    Surface(
        onClick = onClick,
        modifier = modifier.testTag("dashboard_bills_subs_card"),
        shape = RoundedCornerShape(26.dp),
        color = MaterialTheme.colorScheme.surfaceContainerLow,
    ) {
        Column(
            modifier = Modifier.padding(18.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Text(
                    text = stringResource(R.string.dashboard_upcoming_bills),
                    style = MaterialTheme.typography.labelLarge,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
                Icon(
                    imageVector = Icons.AutoMirrored.Rounded.KeyboardArrowRight,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.size(18.dp),
                )
            }

            if (nextBill != null) {
                val daysUntil = (nextBill.nextDueDate.toEpochDay() - state.today.toEpochDay()).toInt()
                val dueLabel = when {
                    daysUntil < 0 -> stringResource(R.string.recurring_overdue, -daysUntil)
                    daysUntil == 0 -> stringResource(R.string.recurring_due_today)
                    daysUntil == 1 -> stringResource(R.string.recurring_due_tomorrow)
                    else -> stringResource(R.string.recurring_due_in_days, daysUntil)
                }
                Text(
                    text = formatCurrency(nextBill.amount, showCents = false),
                    style = MaterialTheme.typography.headlineMedium,
                    color = MaterialTheme.colorScheme.onSurface,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
                Text(
                    text = "${nextBill.name} • $dueLabel",
                    style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
            } else {
                Text(
                    text = stringResource(R.string.dashboard_no_upcoming_bills),
                    style = MaterialTheme.typography.titleLarge,
                    color = MaterialTheme.colorScheme.onSurface,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
                Text(
                    text = stringResource(
                        R.string.dashboard_fixed_per_month,
                        formatCurrency(state.totalMonthlyFixedCommitments, showCents = false),
                    ),
                    style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
            }
        }
    }
}

@OptIn(ExperimentalSharedTransitionApi::class)
@Composable
private fun CategorySummaryRow(
    category: BudgetCategory,
    spent: Double,
    warningThreshold: Float,
    sharedTransitionScope: SharedTransitionScope,
    animatedVisibilityScope: AnimatedVisibilityScope,
    onClick: () -> Unit,
) {
    val ratio = if (category.monthlyLimit > 0.0) (spent / category.monthlyLimit).toFloat().coerceAtLeast(0f) else 0f
    val statusPalette = budgetStatusPalette(ratio, warningThreshold)
    val animatedAccent by animateColorAsState(
        targetValue = statusPalette.accent,
        animationSpec = ExpressiveMotion.fastEffects(),
        label = "RowAccent_${category.id}",
    )
    val animatedRatio by animateFloatAsState(
        targetValue = ratio.coerceIn(0f, 1f),
        animationSpec = ExpressiveMotion.defaultSpatial(),
        label = "RowProgress_${category.id}",
    )

    with(sharedTransitionScope) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .sharedBounds(
                    sharedContentState = rememberSharedContentState(key = "cat-card-${category.id}"),
                    animatedVisibilityScope = animatedVisibilityScope,
                    boundsTransform = { _, _ -> ExpressiveMotion.defaultSpatial() },
                )
                .clickable(onClick = onClick)
                .padding(horizontal = 18.dp, vertical = 14.dp)
                .testTag("category_bento_tile_${category.id}"),
            verticalArrangement = Arrangement.spacedBy(10.dp),
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(14.dp),
                    modifier = Modifier.weight(1f),
                ) {
                    Box(
                        modifier = Modifier
                            .size(40.dp)
                            .clip(CircleShape)
                            .background(category.accentColor.copy(alpha = 0.15f)),
                        contentAlignment = Alignment.Center,
                    ) {
                        Icon(
                            imageVector = category.icon,
                            contentDescription = null,
                            tint = category.accentColor,
                            modifier = Modifier.size(20.dp),
                        )
                    }
                    Column {
                        Text(
                            text = category.name,
                            style = MaterialTheme.typography.titleMedium,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis,
                        )
                        Text(
                            text = "${formatCurrency(spent, false)} of ${formatCurrency(category.monthlyLimit, false)}",
                            style = MaterialTheme.typography.labelMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis,
                        )
                    }
                }

                Spacer(Modifier.width(8.dp))

                Text(
                    text = "${(ratio * 100).toInt()}%",
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.SemiBold,
                    color = animatedAccent,
                )
            }

            LinearProgressIndicator(
                progress = { animatedRatio },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(6.dp)
                    .clip(CircleShape),
                color = animatedAccent,
                trackColor = MaterialTheme.colorScheme.surfaceContainerHighest,
            )
        }
    }
}
