package com.example.ui.screens

import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Add
import androidx.compose.material.icons.rounded.Autorenew
import androidx.compose.material.icons.rounded.CalendarToday
import androidx.compose.material.icons.rounded.Check
import androidx.compose.material.icons.rounded.CheckCircleOutline
import androidx.compose.material.icons.rounded.DeleteOutline
import androidx.compose.material.icons.rounded.Repeat
import androidx.compose.material.icons.rounded.Subscriptions
import androidx.compose.material3.BottomSheetDefaults
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.DatePicker
import androidx.compose.material3.DatePickerDialog
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.rememberDatePickerState
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.example.R
import com.example.data.BillingCadence
import com.example.data.BudgetCategory
import com.example.data.CadenceUiState
import com.example.data.LocalDay
import com.example.data.RecurringItem
import com.example.data.RecurringKind
import com.example.ui.components.LocalAppCurrency
import com.example.ui.components.formatCurrency
import com.example.ui.components.rememberPixelHaptics
import com.example.ui.theme.ExpressiveMotion
import com.example.ui.theme.LocalFinancialColors
import java.util.Locale

private enum class RecurringFilter { BOTH, RECURRING, SUBSCRIPTIONS }

@Composable
fun RecurringAndSubscriptionsContent(
    state: CadenceUiState,
    contentPadding: PaddingValues,
    listState: LazyListState = rememberLazyListState(),
    headerContent: @Composable () -> Unit,
    onAddRecurring: (RecurringKind) -> Unit,
    onEditRecurring: (RecurringItem) -> Unit,
    onToggleActive: (RecurringItem, Boolean) -> Unit,
    onLogPaymentNow: (RecurringItem) -> Unit,
) {
    val performHaptic = rememberPixelHaptics()
    var filter by remember { mutableStateOf(RecurringFilter.BOTH) }

    val recurringBills = state.recurringBills
    val subscriptions = state.subscriptions
    val categoryMap = remember(state.categories) { state.categories.associateBy { it.id } }

    LazyColumn(
        state = listState,
        modifier = Modifier
            .fillMaxWidth()
            .testTag("recurring_and_subs_list"),
        contentPadding = contentPadding,
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        item(key = "header_switcher", contentType = "recurring_header_switcher") {
            headerContent()
        }

        // Side-by-side Recurring vs Subscriptions Bento Comparison
        item(key = "side_by_side_summary", contentType = "recurring_summary") {
            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(10.dp),
                ) {
                    RecurringKindSummaryCard(
                        title = stringResource(R.string.recurring_bills_title),
                        subtitle = stringResource(R.string.recurring_bills_hint),
                        icon = Icons.Rounded.Repeat,
                        monthlyTotal = state.monthlyRecurringBillsTotal,
                        activeCount = recurringBills.count { it.active },
                        selected = filter == RecurringFilter.RECURRING,
                        containerColor = MaterialTheme.colorScheme.primaryContainer,
                        contentColor = MaterialTheme.colorScheme.onPrimaryContainer,
                        onClick = {
                            performHaptic(HapticFeedbackType.TextHandleMove)
                            filter = if (filter == RecurringFilter.RECURRING) RecurringFilter.BOTH else RecurringFilter.RECURRING
                        },
                        onQuickAdd = {
                            performHaptic(HapticFeedbackType.TextHandleMove)
                            onAddRecurring(RecurringKind.RECURRING)
                        },
                        modifier = Modifier
                            .weight(1f)
                            .testTag("summary_card_recurring"),
                    )
                    RecurringKindSummaryCard(
                        title = stringResource(R.string.subscriptions_title),
                        subtitle = stringResource(R.string.subscriptions_hint),
                        icon = Icons.Rounded.Subscriptions,
                        monthlyTotal = state.monthlySubscriptionsTotal,
                        activeCount = subscriptions.count { it.active },
                        selected = filter == RecurringFilter.SUBSCRIPTIONS,
                        containerColor = MaterialTheme.colorScheme.tertiaryContainer,
                        contentColor = MaterialTheme.colorScheme.onTertiaryContainer,
                        onClick = {
                            performHaptic(HapticFeedbackType.TextHandleMove)
                            filter = if (filter == RecurringFilter.SUBSCRIPTIONS) RecurringFilter.BOTH else RecurringFilter.SUBSCRIPTIONS
                        },
                        onQuickAdd = {
                            performHaptic(HapticFeedbackType.TextHandleMove)
                            onAddRecurring(RecurringKind.SUBSCRIPTION)
                        },
                        modifier = Modifier
                            .weight(1f)
                            .testTag("summary_card_subscriptions"),
                    )
                }

                // Combined Monthly & Yearly Commitment Strip
                Surface(
                    shape = RoundedCornerShape(24.dp),
                    color = MaterialTheme.colorScheme.surfaceContainerHigh,
                    modifier = Modifier.fillMaxWidth(),
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 16.dp, vertical = 14.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = stringResource(R.string.recurring_total_fixed_label),
                                style = MaterialTheme.typography.labelMedium,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                            )
                            Row(
                                verticalAlignment = Alignment.Bottom,
                                horizontalArrangement = Arrangement.spacedBy(8.dp),
                            ) {
                                Text(
                                    text = "${formatCurrency(state.totalMonthlyFixedCommitments, showCents = false)}/mo",
                                    style = MaterialTheme.typography.titleLarge,
                                    fontWeight = FontWeight.Bold,
                                    color = MaterialTheme.colorScheme.onSurface,
                                )
                                Text(
                                    text = "• ${formatCurrency(state.totalMonthlyFixedCommitments * 12.0, showCents = false)}/yr",
                                    style = MaterialTheme.typography.labelLarge,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                                )
                            }
                        }
                        FilledTonalButton(
                            onClick = {
                                performHaptic(HapticFeedbackType.LongPress)
                                val defaultKind = if (filter == RecurringFilter.SUBSCRIPTIONS) {
                                    RecurringKind.SUBSCRIPTION
                                } else {
                                    RecurringKind.RECURRING
                                }
                                onAddRecurring(defaultKind)
                            },
                            shape = CircleShape,
                            modifier = Modifier.testTag("add_recurring_primary_button"),
                        ) {
                            Icon(Icons.Rounded.Add, contentDescription = null, modifier = Modifier.size(18.dp))
                            Spacer(Modifier.width(4.dp))
                            Text(stringResource(R.string.recurring_add_button))
                        }
                    }
                }

                // Filter Pills: Both | Recurring | Subscriptions
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                ) {
                    listOf(
                        RecurringFilter.BOTH to stringResource(R.string.recurring_filter_both),
                        RecurringFilter.RECURRING to "${stringResource(R.string.recurring_bills_short)} (${recurringBills.size})",
                        RecurringFilter.SUBSCRIPTIONS to "${stringResource(R.string.subscriptions_title)} (${subscriptions.size})",
                    ).forEach { (option, label) ->
                        val active = filter == option
                        Surface(
                            onClick = {
                                performHaptic(HapticFeedbackType.TextHandleMove)
                                filter = option
                            },
                            shape = CircleShape,
                            color = if (active) MaterialTheme.colorScheme.primary
                            else MaterialTheme.colorScheme.surfaceContainerHigh,
                            modifier = Modifier.testTag("recurring_filter_${option.name.lowercase()}"),
                        ) {
                            Text(
                                text = label,
                                style = MaterialTheme.typography.labelLarge,
                                color = if (active) MaterialTheme.colorScheme.onPrimary
                                else MaterialTheme.colorScheme.onSurfaceVariant,
                                modifier = Modifier.padding(horizontal = 14.dp, vertical = 9.dp),
                            )
                        }
                    }
                }
            }
        }

        // Section 1: Recurring Transactions / Fixed Bills
        if (filter == RecurringFilter.BOTH || filter == RecurringFilter.RECURRING) {
            item(key = "section_recurring_header", contentType = "section_header") {
                SectionKindHeader(
                    icon = Icons.Rounded.Repeat,
                    title = stringResource(R.string.recurring_bills_title),
                    subtitle = stringResource(R.string.recurring_bills_hint),
                    accentColor = MaterialTheme.colorScheme.primary,
                    onAdd = { onAddRecurring(RecurringKind.RECURRING) },
                    addTag = "add_recurring_bill_button",
                )
            }
            if (recurringBills.isEmpty()) {
                item(key = "empty_recurring_bills", contentType = "empty_card") {
                    EmptyKindCard(
                        message = stringResource(R.string.recurring_empty_bills),
                        actionLabel = stringResource(R.string.recurring_add_bill),
                        onClick = { onAddRecurring(RecurringKind.RECURRING) },
                    )
                }
            } else {
                items(
                    items = recurringBills,
                    key = { "rec_${it.id}" },
                    contentType = { "recurring_card" },
                ) { item ->
                    RecurringItemCard(
                        item = item,
                        category = categoryMap[item.categoryId],
                        today = state.today,
                        compact = state.compactCards,
                        onEdit = { onEditRecurring(item) },
                        onToggleActive = { onToggleActive(item, it) },
                        onLogNow = { onLogPaymentNow(item) },
                        modifier = Modifier.animateItem(),
                    )
                }
            }
        }

        // Section 2: Subscriptions (right below so both are visible together!)
        if (filter == RecurringFilter.BOTH || filter == RecurringFilter.SUBSCRIPTIONS) {
            item(key = "section_subscriptions_header", contentType = "section_header") {
                SectionKindHeader(
                    icon = Icons.Rounded.Subscriptions,
                    title = stringResource(R.string.subscriptions_title),
                    subtitle = stringResource(R.string.subscriptions_hint),
                    accentColor = MaterialTheme.colorScheme.tertiary,
                    onAdd = { onAddRecurring(RecurringKind.SUBSCRIPTION) },
                    addTag = "add_subscription_button",
                )
            }
            if (subscriptions.isEmpty()) {
                item(key = "empty_subscriptions", contentType = "empty_card") {
                    EmptyKindCard(
                        message = stringResource(R.string.recurring_empty_subs),
                        actionLabel = stringResource(R.string.recurring_add_sub),
                        onClick = { onAddRecurring(RecurringKind.SUBSCRIPTION) },
                    )
                }
            } else {
                items(
                    items = subscriptions,
                    key = { "sub_${it.id}" },
                    contentType = { "recurring_card" },
                ) { item ->
                    RecurringItemCard(
                        item = item,
                        category = categoryMap[item.categoryId],
                        today = state.today,
                        compact = state.compactCards,
                        onEdit = { onEditRecurring(item) },
                        onToggleActive = { onToggleActive(item, it) },
                        onLogNow = { onLogPaymentNow(item) },
                        modifier = Modifier.animateItem(),
                    )
                }
            }
        }
    }
}

@Composable
private fun RecurringKindSummaryCard(
    title: String,
    subtitle: String,
    icon: ImageVector,
    monthlyTotal: Double,
    activeCount: Int,
    selected: Boolean,
    containerColor: Color,
    contentColor: Color,
    onClick: () -> Unit,
    onQuickAdd: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Surface(
        onClick = onClick,
        shape = RoundedCornerShape(28.dp),
        color = containerColor,
        modifier = modifier.then(
            if (selected) Modifier.border(
                width = 2.dp,
                color = contentColor,
                shape = RoundedCornerShape(28.dp),
            ) else Modifier
        ),
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Box(
                    modifier = Modifier
                        .size(36.dp)
                        .clip(CircleShape)
                        .background(contentColor.copy(alpha = 0.14f)),
                    contentAlignment = Alignment.Center,
                ) {
                    Icon(
                        imageVector = icon,
                        contentDescription = null,
                        tint = contentColor,
                        modifier = Modifier.size(18.dp),
                    )
                }
                Box(
                    modifier = Modifier
                        .size(32.dp)
                        .clip(CircleShape)
                        .background(contentColor.copy(alpha = 0.14f))
                        .clickable(onClick = onQuickAdd),
                    contentAlignment = Alignment.Center,
                ) {
                    Icon(
                        imageVector = Icons.Rounded.Add,
                        contentDescription = title,
                        tint = contentColor,
                        modifier = Modifier.size(18.dp),
                    )
                }
            }
            Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
                Text(
                    text = title,
                    style = MaterialTheme.typography.titleMedium,
                    color = contentColor,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
                Text(
                    text = "${formatCurrency(monthlyTotal, showCents = false)}/mo",
                    style = MaterialTheme.typography.headlineSmall,
                    fontWeight = FontWeight.Bold,
                    color = contentColor,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
                Text(
                    text = "$activeCount active • $subtitle",
                    style = MaterialTheme.typography.labelSmall,
                    color = contentColor.copy(alpha = 0.78f),
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
            }
        }
    }
}

@Composable
private fun SectionKindHeader(
    icon: ImageVector,
    title: String,
    subtitle: String,
    accentColor: Color,
    onAdd: () -> Unit,
    addTag: String,
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(top = 6.dp, start = 4.dp, end = 4.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            modifier = Modifier.weight(1f),
        ) {
            Icon(
                imageVector = icon,
                contentDescription = null,
                tint = accentColor,
                modifier = Modifier.size(20.dp),
            )
            Column {
                Text(
                    text = title,
                    style = MaterialTheme.typography.titleMedium,
                    color = MaterialTheme.colorScheme.onSurface,
                )
                Text(
                    text = subtitle,
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
            }
        }
        TextButton(
            onClick = onAdd,
            modifier = Modifier.testTag(addTag),
        ) {
            Icon(Icons.Rounded.Add, contentDescription = null, modifier = Modifier.size(16.dp))
            Spacer(Modifier.width(2.dp))
            Text(stringResource(R.string.recurring_add_button))
        }
    }
}

@Composable
private fun EmptyKindCard(
    message: String,
    actionLabel: String,
    onClick: () -> Unit,
) {
    Surface(
        shape = RoundedCornerShape(24.dp),
        color = MaterialTheme.colorScheme.surfaceContainerLow,
        modifier = Modifier.fillMaxWidth(),
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 18.dp, vertical = 16.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween,
        ) {
            Text(
                text = message,
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.weight(1f).padding(end = 12.dp),
            )
            FilledTonalButton(onClick = onClick, shape = CircleShape) {
                Text(actionLabel)
            }
        }
    }
}

@Composable
private fun RecurringItemCard(
    item: RecurringItem,
    category: BudgetCategory?,
    today: LocalDay,
    compact: Boolean,
    onEdit: () -> Unit,
    onToggleActive: (Boolean) -> Unit,
    onLogNow: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val performHaptic = rememberPixelHaptics()
    val finColors = LocalFinancialColors.current
    val daysUntilDue = (item.nextDueDate.toEpochDay() - today.toEpochDay()).toInt()
    val dueLabel = when {
        !item.active -> stringResource(R.string.recurring_paused)
        daysUntilDue < 0 -> stringResource(R.string.recurring_overdue, -daysUntilDue)
        daysUntilDue == 0 -> stringResource(R.string.recurring_due_today)
        daysUntilDue == 1 -> stringResource(R.string.recurring_due_tomorrow)
        daysUntilDue <= 7 -> stringResource(R.string.recurring_due_in_days, daysUntilDue)
        else -> "Due ${item.nextDueDate.formatShort()}"
    }
    val dueBadgeBg = when {
        !item.active -> MaterialTheme.colorScheme.surfaceContainerHighest
        daysUntilDue < 0 -> finColors.dangerContainer
        daysUntilDue <= 3 -> finColors.warningContainer
        else -> MaterialTheme.colorScheme.secondaryContainer
    }
    val dueBadgeFg = when {
        !item.active -> MaterialTheme.colorScheme.onSurfaceVariant
        daysUntilDue < 0 -> finColors.onDangerContainer
        daysUntilDue <= 3 -> finColors.onWarningContainer
        else -> MaterialTheme.colorScheme.onSecondaryContainer
    }
    val accent = category?.accentColor ?: MaterialTheme.colorScheme.primary

    Surface(
        onClick = {
            performHaptic(HapticFeedbackType.TextHandleMove)
            onEdit()
        },
        shape = RoundedCornerShape(26.dp),
        color = MaterialTheme.colorScheme.surfaceContainerLow,
        modifier = modifier
            .fillMaxWidth()
            .alpha(if (item.active) 1f else 0.65f)
            .testTag("recurring_item_${item.id}"),
    ) {
        Column(
            modifier = Modifier.padding(
                horizontal = 16.dp,
                vertical = if (compact) 11.dp else 14.dp,
            ),
            verticalArrangement = Arrangement.spacedBy(10.dp),
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(12.dp),
            ) {
                Box(
                    modifier = Modifier
                        .size(42.dp)
                        .clip(CircleShape)
                        .background(accent.copy(alpha = 0.16f)),
                    contentAlignment = Alignment.Center,
                ) {
                    Icon(
                        imageVector = category?.icon ?: Icons.Rounded.Autorenew,
                        contentDescription = null,
                        tint = accent,
                        modifier = Modifier.size(20.dp),
                    )
                }
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = item.name,
                        style = MaterialTheme.typography.titleMedium,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                    )
                    Text(
                        text = "${category?.name ?: "General"} • ${item.cadence.label}",
                        style = MaterialTheme.typography.labelMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                    )
                }
                Column(horizontalAlignment = Alignment.End) {
                    Text(
                        text = "${formatCurrency(item.amount, showCents = true)}${item.cadence.shortLabel}",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.SemiBold,
                        color = MaterialTheme.colorScheme.onSurface,
                    )
                    if (item.cadence != BillingCadence.MONTHLY) {
                        Text(
                            text = "≈ ${formatCurrency(item.monthlyEquivalent, showCents = false)}/mo",
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                    }
                }
            }

            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween,
            ) {
                Surface(
                    shape = CircleShape,
                    color = dueBadgeBg,
                ) {
                    Text(
                        text = dueLabel,
                        style = MaterialTheme.typography.labelMedium,
                        color = dueBadgeFg,
                        modifier = Modifier.padding(horizontal = 10.dp, vertical = 5.dp),
                    )
                }

                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                ) {
                    if (item.active) {
                        Surface(
                            onClick = {
                                performHaptic(HapticFeedbackType.LongPress)
                                onLogNow()
                            },
                            shape = CircleShape,
                            color = MaterialTheme.colorScheme.primaryContainer,
                            modifier = Modifier.testTag("log_recurring_now_${item.id}"),
                        ) {
                            Row(
                                modifier = Modifier.padding(horizontal = 12.dp, vertical = 7.dp),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(4.dp),
                            ) {
                                Icon(
                                    imageVector = Icons.Rounded.CheckCircleOutline,
                                    contentDescription = null,
                                    tint = MaterialTheme.colorScheme.onPrimaryContainer,
                                    modifier = Modifier.size(15.dp),
                                )
                                Text(
                                    text = stringResource(R.string.recurring_log_paid),
                                    style = MaterialTheme.typography.labelMedium,
                                    color = MaterialTheme.colorScheme.onPrimaryContainer,
                                )
                            }
                        }
                    }
                    Switch(
                        checked = item.active,
                        onCheckedChange = { enabled ->
                            performHaptic(HapticFeedbackType.TextHandleMove)
                            onToggleActive(enabled)
                        },
                        modifier = Modifier.testTag("toggle_recurring_${item.id}"),
                    )
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun RecurringEditorBottomSheet(
    categories: List<BudgetCategory>,
    editingItem: RecurringItem?,
    defaultKind: RecurringKind,
    isSaving: Boolean,
    onDismiss: () -> Unit,
    onSave: (
        name: String,
        amount: Double,
        categoryId: String,
        kind: RecurringKind,
        cadence: BillingCadence,
        nextDueDate: LocalDay,
        active: Boolean,
    ) -> Unit,
    onDelete: (RecurringItem) -> Unit,
) {
    if (categories.isEmpty()) return

    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    val performHaptic = rememberPixelHaptics()
    val currency = LocalAppCurrency.current
    val today = remember { LocalDay.today() }

    var selectedKind by remember(editingItem, defaultKind) {
        mutableStateOf(editingItem?.kind ?: defaultKind)
    }
    var name by remember(editingItem) { mutableStateOf(editingItem?.name ?: "") }
    var amountInput by remember(editingItem) {
        mutableStateOf(
            editingItem?.amount?.let { amt ->
                String.format(Locale.US, "%.2f", amt).trimEnd('0').trimEnd('.')
            } ?: ""
        )
    }
    var selectedCategoryId by remember(editingItem, categories) {
        mutableStateOf(
            editingItem?.categoryId?.takeIf { id -> categories.any { it.id == id } }
                ?: categories.first().id
        )
    }
    var selectedCadence by remember(editingItem) {
        mutableStateOf(editingItem?.cadence ?: BillingCadence.MONTHLY)
    }
    var nextDueDate by remember(editingItem) {
        mutableStateOf(editingItem?.nextDueDate ?: today.plusDays(7))
    }
    var active by remember(editingItem) { mutableStateOf(editingItem?.active ?: true) }
    var showDatePicker by remember { mutableStateOf(false) }

    val parsedAmount = amountInput.toDoubleOrNull() ?: 0.0
    val isValid = name.trim().isNotEmpty() && parsedAmount > 0.0

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
                .verticalScroll(rememberScrollState())
                .navigationBarsPadding()
                .padding(horizontal = 20.dp)
                .padding(bottom = 24.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp),
        ) {
            Text(
                text = if (editingItem != null) stringResource(R.string.recurring_sheet_edit_title)
                else stringResource(R.string.recurring_sheet_create_title),
                style = MaterialTheme.typography.headlineMedium,
            )

            // Side-by-side Recurring vs Subscription decision cards
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Text(
                    text = stringResource(R.string.recurring_choose_type_label),
                    style = MaterialTheme.typography.labelLarge,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(10.dp),
                ) {
                    RecurringTypeChoiceCard(
                        title = stringResource(R.string.recurring_bills_short),
                        description = stringResource(R.string.recurring_bills_hint),
                        icon = Icons.Rounded.Repeat,
                        selected = selectedKind == RecurringKind.RECURRING,
                        onClick = {
                            performHaptic(HapticFeedbackType.TextHandleMove)
                            selectedKind = RecurringKind.RECURRING
                        },
                        modifier = Modifier
                            .weight(1f)
                            .testTag("choice_kind_recurring"),
                    )
                    RecurringTypeChoiceCard(
                        title = stringResource(R.string.subscription_singular),
                        description = stringResource(R.string.subscriptions_hint),
                        icon = Icons.Rounded.Subscriptions,
                        selected = selectedKind == RecurringKind.SUBSCRIPTION,
                        onClick = {
                            performHaptic(HapticFeedbackType.TextHandleMove)
                            selectedKind = RecurringKind.SUBSCRIPTION
                        },
                        modifier = Modifier
                            .weight(1f)
                            .testTag("choice_kind_subscription"),
                    )
                }
            }

            // Name & Amount inputs
            OutlinedTextField(
                value = name,
                onValueChange = { if (it.length <= 60) name = it },
                label = {
                    Text(
                        if (selectedKind == RecurringKind.SUBSCRIPTION) {
                            stringResource(R.string.recurring_sub_name_placeholder)
                        } else {
                            stringResource(R.string.recurring_bill_name_placeholder)
                        }
                    )
                },
                singleLine = true,
                shape = RoundedCornerShape(20.dp),
                modifier = Modifier
                    .fillMaxWidth()
                    .testTag("recurring_name_input"),
                colors = OutlinedTextFieldDefaults.colors(
                    unfocusedContainerColor = MaterialTheme.colorScheme.surfaceContainerHigh,
                    focusedContainerColor = MaterialTheme.colorScheme.surfaceContainerHighest,
                ),
            )

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(10.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                OutlinedTextField(
                    value = amountInput,
                    onValueChange = { value ->
                        if (value.all { it.isDigit() || it == '.' } && value.count { it == '.' } <= 1 && value.length <= 10) {
                            amountInput = value
                        }
                    },
                    label = { Text(stringResource(R.string.recurring_amount_label, currency.symbol)) },
                    prefix = { Text(if (currency.usesTightPrefix) currency.symbol else "${currency.symbol} ") },
                    singleLine = true,
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                    shape = RoundedCornerShape(20.dp),
                    modifier = Modifier
                        .weight(1f)
                        .testTag("recurring_amount_input"),
                    colors = OutlinedTextFieldDefaults.colors(
                        unfocusedContainerColor = MaterialTheme.colorScheme.surfaceContainerHigh,
                        focusedContainerColor = MaterialTheme.colorScheme.surfaceContainerHighest,
                    ),
                )
                FilledTonalButton(
                    onClick = {
                        performHaptic(HapticFeedbackType.TextHandleMove)
                        showDatePicker = true
                    },
                    shape = RoundedCornerShape(20.dp),
                    contentPadding = PaddingValues(horizontal = 14.dp, vertical = 16.dp),
                    modifier = Modifier.testTag("recurring_due_date_button"),
                ) {
                    Icon(Icons.Rounded.CalendarToday, contentDescription = null, modifier = Modifier.size(16.dp))
                    Spacer(Modifier.width(6.dp))
                    Text(nextDueDate.formatShort(), style = MaterialTheme.typography.labelLarge)
                }
            }

            // Cadence pills
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Text(
                    text = stringResource(R.string.recurring_frequency_label),
                    style = MaterialTheme.typography.labelLarge,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(6.dp),
                ) {
                    BillingCadence.entries.forEach { cadence ->
                        val selected = cadence == selectedCadence
                        Surface(
                            onClick = {
                                performHaptic(HapticFeedbackType.TextHandleMove)
                                selectedCadence = cadence
                            },
                            shape = CircleShape,
                            color = if (selected) MaterialTheme.colorScheme.primary
                            else MaterialTheme.colorScheme.surfaceContainerHigh,
                            modifier = Modifier
                                .weight(1f)
                                .testTag("cadence_${cadence.storageKey}"),
                        ) {
                            Box(
                                contentAlignment = Alignment.Center,
                                modifier = Modifier.padding(vertical = 10.dp, horizontal = 4.dp),
                            ) {
                                Text(
                                    text = cadence.label,
                                    style = MaterialTheme.typography.labelMedium,
                                    color = if (selected) MaterialTheme.colorScheme.onPrimary
                                    else MaterialTheme.colorScheme.onSurfaceVariant,
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis,
                                )
                            }
                        }
                    }
                }
            }

            // Category Chips
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Text(
                    text = stringResource(R.string.recurring_category_label),
                    style = MaterialTheme.typography.labelLarge,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
                LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    items(categories, key = { it.id }) { cat ->
                        val selected = cat.id == selectedCategoryId
                        val corner by animateDpAsState(
                            targetValue = if (selected) 12.dp else 24.dp,
                            animationSpec = ExpressiveMotion.fastSpatial(),
                            label = "RecCatChip_${cat.id}",
                        )
                        Surface(
                            shape = RoundedCornerShape(corner),
                            color = if (selected) MaterialTheme.colorScheme.primaryContainer
                            else MaterialTheme.colorScheme.surfaceContainerHigh,
                            modifier = Modifier
                                .heightIn(min = 44.dp)
                                .clip(RoundedCornerShape(corner))
                                .clickable {
                                    performHaptic(HapticFeedbackType.TextHandleMove)
                                    selectedCategoryId = cat.id
                                },
                        ) {
                            Row(
                                modifier = Modifier.padding(horizontal = 14.dp, vertical = 10.dp),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(6.dp),
                            ) {
                                Icon(
                                    imageVector = cat.icon,
                                    contentDescription = null,
                                    tint = if (selected) MaterialTheme.colorScheme.onPrimaryContainer else cat.accentColor,
                                    modifier = Modifier.size(16.dp),
                                )
                                Text(
                                    text = cat.name,
                                    style = MaterialTheme.typography.labelLarge,
                                    color = if (selected) MaterialTheme.colorScheme.onPrimaryContainer
                                    else MaterialTheme.colorScheme.onSurface,
                                )
                            }
                        }
                    }
                }
            }

            // Active status row
            Surface(
                shape = RoundedCornerShape(22.dp),
                color = MaterialTheme.colorScheme.surfaceContainer,
                modifier = Modifier.fillMaxWidth(),
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp, vertical = 12.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween,
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = stringResource(R.string.recurring_active_label),
                            style = MaterialTheme.typography.titleSmall,
                        )
                        Text(
                            text = stringResource(R.string.recurring_active_hint),
                            style = MaterialTheme.typography.labelMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                    }
                    Switch(
                        checked = active,
                        onCheckedChange = {
                            performHaptic(HapticFeedbackType.TextHandleMove)
                            active = it
                        },
                    )
                }
            }

            // Save & Delete Actions
            Button(
                onClick = {
                    if (isValid && !isSaving) {
                        performHaptic(HapticFeedbackType.LongPress)
                        onSave(
                            name.trim(),
                            parsedAmount,
                            selectedCategoryId,
                            selectedKind,
                            selectedCadence,
                            nextDueDate,
                            active,
                        )
                    }
                },
                enabled = isValid && !isSaving,
                shape = CircleShape,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(54.dp)
                    .testTag("recurring_save_button"),
            ) {
                if (isSaving) {
                    CircularProgressIndicator(
                        modifier = Modifier.size(20.dp),
                        color = MaterialTheme.colorScheme.onPrimary,
                        strokeWidth = 2.dp,
                    )
                } else {
                    Icon(Icons.Rounded.Check, contentDescription = null)
                    Spacer(Modifier.width(8.dp))
                    Text(
                        text = stringResource(R.string.recurring_save_button),
                        style = MaterialTheme.typography.titleMedium,
                    )
                }
            }

            if (editingItem != null) {
                OutlinedButton(
                    onClick = {
                        performHaptic(HapticFeedbackType.LongPress)
                        onDelete(editingItem)
                    },
                    enabled = !isSaving,
                    shape = CircleShape,
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(48.dp)
                        .testTag("recurring_delete_button"),
                ) {
                    Icon(
                        imageVector = Icons.Rounded.DeleteOutline,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.error,
                    )
                    Spacer(Modifier.width(8.dp))
                    Text(
                        text = stringResource(R.string.recurring_delete_button),
                        color = MaterialTheme.colorScheme.error,
                    )
                }
            }
        }
    }

    if (showDatePicker) {
        val datePickerState = rememberDatePickerState(
            initialSelectedDateMillis = nextDueDate.toUtcMillis(),
        )
        DatePickerDialog(
            onDismissRequest = { showDatePicker = false },
            confirmButton = {
                TextButton(
                    onClick = {
                        datePickerState.selectedDateMillis?.let { millis ->
                            nextDueDate = LocalDay.fromUtcMillis(millis)
                        }
                        showDatePicker = false
                    }
                ) {
                    Text(stringResource(R.string.entry_set_date))
                }
            },
            dismissButton = {
                TextButton(onClick = { showDatePicker = false }) {
                    Text(stringResource(R.string.entry_cancel))
                }
            },
        ) {
            DatePicker(state = datePickerState)
        }
    }
}

@Composable
private fun RecurringTypeChoiceCard(
    title: String,
    description: String,
    icon: ImageVector,
    selected: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val bg by animateColorAsState(
        targetValue = if (selected) MaterialTheme.colorScheme.primaryContainer
        else MaterialTheme.colorScheme.surfaceContainerHigh,
        animationSpec = ExpressiveMotion.fastEffects(),
        label = "RecTypeBg_$title",
    )
    val fg by animateColorAsState(
        targetValue = if (selected) MaterialTheme.colorScheme.onPrimaryContainer
        else MaterialTheme.colorScheme.onSurface,
        animationSpec = ExpressiveMotion.fastEffects(),
        label = "RecTypeFg_$title",
    )

    Surface(
        onClick = onClick,
        shape = RoundedCornerShape(24.dp),
        color = bg,
        modifier = modifier.then(
            if (selected) Modifier.border(
                width = 2.dp,
                color = MaterialTheme.colorScheme.primary,
                shape = RoundedCornerShape(24.dp),
            ) else Modifier
        ),
    ) {
        Column(
            modifier = Modifier.padding(14.dp),
            verticalArrangement = Arrangement.spacedBy(6.dp),
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Icon(
                    imageVector = icon,
                    contentDescription = null,
                    tint = if (selected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.size(22.dp),
                )
                if (selected) {
                    Icon(
                        imageVector = Icons.Rounded.Check,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.size(18.dp),
                    )
                }
            }
            Text(
                text = title,
                style = MaterialTheme.typography.titleMedium,
                color = fg,
                fontWeight = FontWeight.SemiBold,
            )
            Text(
                text = description,
                style = MaterialTheme.typography.labelSmall,
                color = fg.copy(alpha = 0.78f),
                maxLines = 2,
                overflow = TextOverflow.Ellipsis,
            )
        }
    }
}
