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
import androidx.compose.material.icons.rounded.AutoAwesome
import androidx.compose.material.icons.rounded.CalendarToday
import androidx.compose.material.icons.rounded.Settings
import androidx.compose.material.icons.rounded.TrendingDown
import androidx.compose.material.icons.rounded.TrendingUp
import androidx.compose.material3.AlertDialog
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
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.example.R
import com.example.data.BudgetCategory
import com.example.data.CadenceUiState
import com.example.ui.components.WavyProgressIndicator
import com.example.ui.components.formatCurrency
import com.example.ui.theme.ExpressiveMotion
import com.example.ui.theme.budgetStatusPalette

@OptIn(ExperimentalSharedTransitionApi::class)
@Composable
fun DashboardScreen(
    state: CadenceUiState,
    isCloudSignedIn: Boolean,
    userInitial: String?,
    contentPadding: PaddingValues,
    sharedTransitionScope: SharedTransitionScope,
    animatedVisibilityScope: AnimatedVisibilityScope,
    onCategoryClick: (BudgetCategory) -> Unit,
    onViewAllActivity: () -> Unit,
    onOpenBillsAndSubs: () -> Unit,
    onOpenAccount: () -> Unit,
    onClearSampleData: () -> Unit,
    onChangeResetDay: (Int) -> Unit,
) {
    val performHaptic = com.example.ui.components.rememberPixelHaptics()
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
        verticalArrangement = Arrangement.spacedBy(if (state.compactCards) 10.dp else 14.dp),
    ) {
        // Editorial Greeting Header
        item(key = "header") {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 4.dp, vertical = 4.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Column(modifier = Modifier.weight(1f).padding(end = 8.dp)) {
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
                    horizontalArrangement = Arrangement.spacedBy(10.dp),
                ) {
                    Surface(
                        onClick = {
                            performHaptic(HapticFeedbackType.TextHandleMove)
                            showCycleInfoDialog = true
                        },
                        shape = CircleShape,
                        color = MaterialTheme.colorScheme.surfaceContainerHigh,
                        modifier = Modifier.testTag("month_cycle_pill"),
                    ) {
                        Row(
                            modifier = Modifier.padding(horizontal = 12.dp, vertical = 12.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(6.dp),
                        ) {
                            Icon(
                                imageVector = Icons.Rounded.CalendarToday,
                                contentDescription = stringResource(R.string.cd_month_cycle_info),
                                tint = MaterialTheme.colorScheme.primary,
                                modifier = Modifier.size(14.dp),
                            )
                            Text(
                                text = "${state.daysRemaining}d left",
                                style = MaterialTheme.typography.labelLarge,
                                color = MaterialTheme.colorScheme.onSurface,
                            )
                        }
                    }
                    Surface(
                        onClick = {
                            performHaptic(HapticFeedbackType.TextHandleMove)
                            onOpenAccount()
                        },
                        shape = CircleShape,
                        color = if (isCloudSignedIn) MaterialTheme.colorScheme.primaryContainer
                        else MaterialTheme.colorScheme.secondaryContainer,
                        modifier = Modifier
                            .size(48.dp)
                            .testTag("account_settings_button"),
                    ) {
                        Box(contentAlignment = Alignment.Center, modifier = Modifier.fillMaxSize()) {
                            if (isCloudSignedIn && !userInitial.isNullOrBlank()) {
                                Text(
                                    text = userInitial,
                                    style = MaterialTheme.typography.titleMedium,
                                    color = MaterialTheme.colorScheme.onPrimaryContainer,
                                    fontWeight = FontWeight.Bold,
                                )
                            } else {
                                Icon(
                                    imageVector = Icons.Rounded.Settings,
                                    contentDescription = stringResource(R.string.cd_account),
                                    tint = MaterialTheme.colorScheme.onSecondaryContainer,
                                )
                            }
                        }
                    }
                }
            }
        }

        // Optional Sample Data Banner with One-Tap Clear
        if (state.hasSampleData) {
            item(key = "sample_banner") {
                Surface(
                    shape = RoundedCornerShape(22.dp),
                    color = MaterialTheme.colorScheme.surfaceContainerHigh,
                    modifier = Modifier.fillMaxWidth(),
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(start = 16.dp, end = 8.dp, top = 8.dp, bottom = 8.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(10.dp),
                    ) {
                        Icon(
                            imageVector = Icons.Rounded.AutoAwesome,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.size(18.dp),
                        )
                        Text(
                            text = stringResource(R.string.dashboard_sample_banner),
                            style = MaterialTheme.typography.labelMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
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

        // 1. Hero Remaining-Budget Squircle
        item(key = "hero_balance") {
            HeroBalanceSquircle(state = state)
        }

        // 2. Asymmetrical Bento Grid: Spend Pace (1.3f) + Stacked Micro-Stats (0.95f)
        item(key = "bento_pace_row") {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(IntrinsicSize.Max),
                horizontalArrangement = Arrangement.spacedBy(12.dp),
            ) {
                SpendPaceBentoCard(
                    state = state,
                    modifier = Modifier
                        .weight(1.32f)
                        .fillMaxHeight(),
                )
                Column(
                    modifier = Modifier
                        .weight(0.93f)
                        .fillMaxHeight(),
                    verticalArrangement = Arrangement.spacedBy(12.dp),
                ) {
                    QuickStatPillTile(
                        label = stringResource(R.string.dashboard_spent_label),
                        value = formatCurrency(state.totalSpentThisMonth, showCents = false),
                        subtext = null,
                        containerColor = MaterialTheme.colorScheme.surfaceContainerHigh,
                        contentColor = MaterialTheme.colorScheme.onSurface,
                        modifier = Modifier
                            .fillMaxWidth()
                            .weight(1f),
                    )
                    QuickStatPillTile(
                        label = stringResource(R.string.dashboard_safe_per_day),
                        value = formatCurrency(state.safeDailySpend, showCents = false),
                        subtext = if (state.includeFixedBillsInSafeDaily && state.upcomingDueThisCycleTotal > 0.0) {
                            stringResource(R.string.dashboard_bills_reserved)
                        } else null,
                        containerColor = MaterialTheme.colorScheme.tertiaryContainer,
                        contentColor = MaterialTheme.colorScheme.onTertiaryContainer,
                        modifier = Modifier
                            .fillMaxWidth()
                            .weight(1f),
                    )
                }
            }
        }

        // 2.5 Side-by-side Recurring Bills & Subscriptions Snapshot Card
        item(key = "recurring_subs_snapshot") {
            DashboardBillsAndSubsBentoCard(
                state = state,
                onClick = {
                    performHaptic(HapticFeedbackType.TextHandleMove)
                    onOpenBillsAndSubs()
                },
            )
        }

        // 3. Top Categories Section Header
        item(key = "top_categories_header") {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = 6.dp, start = 4.dp, end = 4.dp),
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
                    Text(stringResource(R.string.dashboard_view_activity))
                    Spacer(Modifier.width(4.dp))
                    Icon(
                        imageVector = Icons.AutoMirrored.Rounded.ArrowForward,
                        contentDescription = null,
                        modifier = Modifier.size(16.dp),
                    )
                }
            }
        }

        // 4. Top Categories 2x2 Bento Grid
        item(key = "top_categories_grid") {
            val topCategories = remember(state.categories, state.transactions, state.today, state.budgetResetDay) {
                state.categories
                    .sortedByDescending { state.spentForCategoryThisMonth(it.id) }
                    .take(4)
            }
            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                topCategories.chunked(2).forEach { pair ->
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(12.dp),
                    ) {
                        pair.forEach { cat ->
                            CategoryBentoTile(
                                category = cat,
                                spent = state.spentForCategoryThisMonth(cat.id),
                                sharedTransitionScope = sharedTransitionScope,
                                animatedVisibilityScope = animatedVisibilityScope,
                                onClick = {
                                    performHaptic(HapticFeedbackType.TextHandleMove)
                                    onCategoryClick(cat)
                                },
                                modifier = Modifier.weight(1f),
                            )
                        }
                        if (pair.size == 1) {
                            Spacer(Modifier.weight(1f))
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun DashboardBillsAndSubsBentoCard(
    state: CadenceUiState,
    onClick: () -> Unit,
) {
    val activeBills = remember(state.recurringItems) { state.recurringBills.count { it.active } }
    val activeSubs = remember(state.recurringItems) { state.subscriptions.count { it.active } }
    val nextBill = state.nextUpcomingBill

    Surface(
        onClick = onClick,
        shape = RoundedCornerShape(28.dp),
        color = MaterialTheme.colorScheme.surfaceContainerLow,
        modifier = Modifier
            .fillMaxWidth()
            .testTag("dashboard_bills_subs_card"),
    ) {
        Column(
            modifier = Modifier.padding(18.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Text(
                    text = stringResource(R.string.dashboard_fixed_commitments),
                    style = MaterialTheme.typography.titleMedium,
                    color = MaterialTheme.colorScheme.onSurface,
                )
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(4.dp),
                ) {
                    Text(
                        text = stringResource(R.string.dashboard_manage),
                        style = MaterialTheme.typography.labelLarge,
                        color = MaterialTheme.colorScheme.primary,
                    )
                    Icon(
                        imageVector = Icons.AutoMirrored.Rounded.ArrowForward,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.size(16.dp),
                    )
                }
            }

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(10.dp),
            ) {
                Surface(
                    shape = RoundedCornerShape(20.dp),
                    color = MaterialTheme.colorScheme.surfaceContainerHigh,
                    modifier = Modifier.weight(1f),
                ) {
                    Column(
                        modifier = Modifier.padding(12.dp),
                        verticalArrangement = Arrangement.spacedBy(2.dp),
                    ) {
                        Text(
                            text = "${stringResource(R.string.recurring_bills_short)} ($activeBills)",
                            style = MaterialTheme.typography.labelMedium,
                            color = MaterialTheme.colorScheme.primary,
                        )
                        Text(
                            text = "${formatCurrency(state.monthlyRecurringBillsTotal, showCents = false)}/mo",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis,
                        )
                    }
                }
                Surface(
                    shape = RoundedCornerShape(20.dp),
                    color = MaterialTheme.colorScheme.surfaceContainerHigh,
                    modifier = Modifier.weight(1f),
                ) {
                    Column(
                        modifier = Modifier.padding(12.dp),
                        verticalArrangement = Arrangement.spacedBy(2.dp),
                    ) {
                        Text(
                            text = "${stringResource(R.string.subscriptions_title)} ($activeSubs)",
                            style = MaterialTheme.typography.labelMedium,
                            color = MaterialTheme.colorScheme.tertiary,
                        )
                        Text(
                            text = "${formatCurrency(state.monthlySubscriptionsTotal, showCents = false)}/mo",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis,
                        )
                    }
                }
            }

            if (nextBill != null) {
                val daysUntil = (nextBill.nextDueDate.toEpochDay() - state.today.toEpochDay()).toInt()
                val dueText = when {
                    daysUntil < 0 -> stringResource(R.string.recurring_overdue, -daysUntil)
                    daysUntil == 0 -> stringResource(R.string.recurring_due_today)
                    daysUntil == 1 -> stringResource(R.string.recurring_due_tomorrow)
                    else -> "in ${daysUntil}d (${nextBill.nextDueDate.formatShort()})"
                }
                Text(
                    text = "Next due: ${nextBill.name} • ${formatCurrency(nextBill.amount, showCents = true)} • $dueText",
                    style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
            }
        }
    }
}

@Composable
private fun HeroBalanceSquircle(state: CadenceUiState) {
    val isNegative = state.remainingBudget < 0.0
    val animatedRemaining by animateFloatAsState(
        targetValue = state.remainingBudget.toFloat(),
        animationSpec = ExpressiveMotion.slowSpatial(),
        label = "HeroBalanceSpring",
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
    else MaterialTheme.colorScheme.primary.copy(alpha = 0.16f)

    Surface(
        modifier = Modifier
            .fillMaxWidth()
            .testTag("hero_balance_card"),
        shape = RoundedCornerShape(36.dp),
        color = containerColor,
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .drawBehind {
                    drawCircle(
                        brush = Brush.radialGradient(
                            colors = listOf(glowColor, Color.Transparent),
                            center = Offset(size.width * 0.88f, size.height * 0.15f),
                            radius = size.maxDimension * 0.65f,
                        ),
                        radius = size.maxDimension * 0.65f,
                        center = Offset(size.width * 0.88f, size.height * 0.15f),
                    )
                }
                .padding(24.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            Text(
                text = if (isNegative) stringResource(R.string.dashboard_over_budget)
                else stringResource(R.string.dashboard_remaining_to_spend),
                style = MaterialTheme.typography.titleMedium,
                color = contentColor.copy(alpha = 0.80f),
            )
            Text(
                text = formatCurrency(animatedRemaining.toDouble(), showCents = true),
                style = MaterialTheme.typography.displayLarge,
                color = contentColor,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                Surface(
                    shape = CircleShape,
                    color = MaterialTheme.colorScheme.surface.copy(alpha = 0.34f),
                ) {
                    Text(
                        text = "${(state.overallSpendRatio * 100).toInt()}% of ${formatCurrency(state.totalMonthlyBudget, showCents = false)} used",
                        style = MaterialTheme.typography.labelMedium,
                        color = contentColor,
                        modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp),
                    )
                }
            }
        }
    }
}

@Composable
private fun SpendPaceBentoCard(
    state: CadenceUiState,
    modifier: Modifier = Modifier,
) {
    val paceDelta = state.overallSpendRatio - state.monthElapsedRatio
    val isAhead = paceDelta > 0.05f
    val statusPalette = budgetStatusPalette(
        if (isAhead) (0.76f + paceDelta).coerceAtMost(1f) else 0.40f
    )
    val animatedAccent by animateColorAsState(
        targetValue = statusPalette.accent,
        animationSpec = ExpressiveMotion.fastEffects(),
        label = "PaceAccentColor",
    )
    val animatedSpend by animateFloatAsState(
        targetValue = state.overallSpendRatio.coerceIn(0f, 1f),
        animationSpec = ExpressiveMotion.slowSpatial(),
        label = "PaceProgressSpring",
    )

    Surface(
        modifier = modifier.testTag("spend_pace_card"),
        shape = RoundedCornerShape(28.dp),
        color = MaterialTheme.colorScheme.surfaceContainerLow,
    ) {
        Column(
            modifier = Modifier.padding(18.dp),
            verticalArrangement = Arrangement.SpaceBetween,
        ) {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Surface(shape = CircleShape, color = statusPalette.container) {
                    Row(
                        modifier = Modifier.padding(horizontal = 10.dp, vertical = 5.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(4.dp),
                    ) {
                        Icon(
                            imageVector = if (isAhead) Icons.Rounded.TrendingUp else Icons.Rounded.TrendingDown,
                            contentDescription = null,
                            tint = statusPalette.onContainer,
                            modifier = Modifier.size(14.dp),
                        )
                        Text(
                            text = if (isAhead) stringResource(R.string.dashboard_spending_fast)
                            else stringResource(R.string.dashboard_on_pace),
                            style = MaterialTheme.typography.labelMedium,
                            color = statusPalette.onContainer,
                        )
                    }
                }
                Text(
                    text = stringResource(R.string.dashboard_monthly_pace),
                    style = MaterialTheme.typography.titleMedium,
                    color = MaterialTheme.colorScheme.onSurface,
                )
                Text(
                    text = "Day ${state.cycleDayElapsed} of ${state.cycleTotalDays} (${(state.monthElapsedRatio * 100).toInt()}%)",
                    style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }

            Spacer(Modifier.height(16.dp))

            WavyProgressIndicator(
                progress = animatedSpend,
                color = animatedAccent,
                trackColor = MaterialTheme.colorScheme.surfaceContainerHighest,
                markerFraction = state.monthElapsedRatio,
                markerColor = MaterialTheme.colorScheme.onSurface,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(22.dp),
            )
        }
    }
}

@Composable
private fun QuickStatPillTile(
    label: String,
    value: String,
    subtext: String?,
    containerColor: Color,
    contentColor: Color,
    modifier: Modifier = Modifier,
) {
    Surface(
        modifier = modifier,
        shape = RoundedCornerShape(24.dp),
        color = containerColor,
    ) {
        Column(
            modifier = Modifier.padding(horizontal = 16.dp, vertical = 14.dp),
            verticalArrangement = Arrangement.Center,
        ) {
            Text(
                text = label,
                style = MaterialTheme.typography.labelMedium,
                color = contentColor.copy(alpha = 0.74f),
            )
            Spacer(Modifier.height(2.dp))
            Text(
                text = value,
                style = MaterialTheme.typography.titleLarge,
                color = contentColor,
                fontWeight = FontWeight.Bold,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
            if (!subtext.isNullOrBlank()) {
                Text(
                    text = subtext,
                    style = MaterialTheme.typography.labelSmall,
                    color = contentColor.copy(alpha = 0.72f),
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
            }
        }
    }
}

@OptIn(ExperimentalSharedTransitionApi::class)
@Composable
private fun CategoryBentoTile(
    category: BudgetCategory,
    spent: Double,
    sharedTransitionScope: SharedTransitionScope,
    animatedVisibilityScope: AnimatedVisibilityScope,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val ratio = if (category.monthlyLimit > 0.0) (spent / category.monthlyLimit).toFloat().coerceAtLeast(0f) else 0f
    val statusPalette = budgetStatusPalette(ratio)
    val animatedAccent by animateColorAsState(
        targetValue = statusPalette.accent,
        animationSpec = ExpressiveMotion.fastEffects(),
        label = "TileAccent_${category.id}",
    )
    val animatedRatio by animateFloatAsState(
        targetValue = ratio.coerceIn(0f, 1f),
        animationSpec = ExpressiveMotion.defaultSpatial(),
        label = "TileProgress_${category.id}",
    )

    with(sharedTransitionScope) {
        Surface(
            modifier = modifier
                .sharedBounds(
                    sharedContentState = rememberSharedContentState(key = "cat-card-${category.id}"),
                    animatedVisibilityScope = animatedVisibilityScope,
                    boundsTransform = { _, _ -> ExpressiveMotion.defaultSpatial() },
                )
                .clip(RoundedCornerShape(28.dp))
                .clickable(onClick = onClick)
                .testTag("category_bento_tile_${category.id}"),
            shape = RoundedCornerShape(28.dp),
            color = MaterialTheme.colorScheme.surfaceContainer,
        ) {
            Column(
                modifier = Modifier.padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(10.dp),
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Box(
                        modifier = Modifier
                            .size(40.dp)
                            .clip(CircleShape)
                            .background(category.accentColor.copy(alpha = 0.16f)),
                        contentAlignment = Alignment.Center,
                    ) {
                        Icon(
                            imageVector = category.icon,
                            contentDescription = null,
                            tint = category.accentColor,
                            modifier = Modifier.size(20.dp),
                        )
                    }
                    Text(
                        text = "${(ratio * 100).toInt()}%",
                        style = MaterialTheme.typography.labelMedium,
                        color = animatedAccent,
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
                        text = "${formatCurrency(spent, false)} / ${formatCurrency(category.monthlyLimit, false)}",
                        style = MaterialTheme.typography.labelMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
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
}
