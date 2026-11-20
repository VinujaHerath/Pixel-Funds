package com.example.ui.screens

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.slideOutHorizontally
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Autorenew
import androidx.compose.material.icons.rounded.Close
import androidx.compose.material.icons.rounded.DeleteOutline
import androidx.compose.material.icons.rounded.Edit
import androidx.compose.material.icons.rounded.ReceiptLong
import androidx.compose.material.icons.rounded.Search
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.SwipeToDismissBox
import androidx.compose.material3.SwipeToDismissBoxValue
import androidx.compose.material3.Text
import androidx.compose.material3.rememberSwipeToDismissBoxState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.Immutable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.example.R
import com.example.data.BudgetCategory
import com.example.data.CadenceUiState
import com.example.data.LocalDay
import com.example.data.RecurringItem
import com.example.data.RecurringKind
import com.example.data.Transaction
import com.example.ui.components.LocalReduceMotion
import com.example.ui.components.formatCurrency
import com.example.ui.components.rememberPixelHaptics
import com.example.ui.theme.ExpressiveMotion

enum class ActivitySection {
    TRANSACTIONS,
    BILLS_AND_SUBS,
}

@Immutable
private data class TransactionDayGroup(
    val date: LocalDay,
    val total: Double,
    val items: List<Transaction>,
)

private val SingleSegmentedShape = RoundedCornerShape(24.dp)
private val TopSegmentedShape = RoundedCornerShape(topStart = 24.dp, topEnd = 24.dp, bottomStart = 6.dp, bottomEnd = 6.dp)
private val BottomSegmentedShape = RoundedCornerShape(topStart = 6.dp, topEnd = 6.dp, bottomStart = 24.dp, bottomEnd = 24.dp)
private val MiddleSegmentedShape = RoundedCornerShape(6.dp)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TransactionsScreen(
    state: CadenceUiState,
    activeSection: ActivitySection,
    onSelectSection: (ActivitySection) -> Unit,
    contentPadding: PaddingValues,
    transactionsListState: LazyListState = rememberLazyListState(),
    recurringListState: LazyListState = rememberLazyListState(),
    onSearchChange: (String) -> Unit,
    onFilterCategory: (String?) -> Unit,
    onEdit: (Transaction) -> Unit,
    onDelete: (Transaction) -> Unit,
    onAddRecurring: (RecurringKind) -> Unit,
    onEditRecurring: (RecurringItem) -> Unit,
    onToggleRecurringActive: (RecurringItem, Boolean) -> Unit,
    onLogRecurringNow: (RecurringItem) -> Unit,
) {
    val performHaptic = rememberPixelHaptics()
    val reduceMotion = LocalReduceMotion.current

    val sectionHeader: @Composable () -> Unit = {
        Column(
            modifier = Modifier.padding(bottom = 8.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            Text(
                text = stringResource(R.string.activity_title),
                style = MaterialTheme.typography.headlineMedium,
                modifier = Modifier.padding(horizontal = 4.dp),
            )
            // Expressive Pill Switcher: Transactions | Bills & Subs
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(CircleShape)
                    .background(MaterialTheme.colorScheme.surfaceContainerHigh)
                    .padding(4.dp),
                horizontalArrangement = Arrangement.spacedBy(4.dp),
            ) {
                ActivityModePill(
                    label = stringResource(R.string.activity_tab_transactions),
                    icon = Icons.Rounded.ReceiptLong,
                    selected = activeSection == ActivitySection.TRANSACTIONS,
                    onClick = {
                        performHaptic(HapticFeedbackType.TextHandleMove)
                        onSelectSection(ActivitySection.TRANSACTIONS)
                    },
                    modifier = Modifier
                        .weight(1f)
                        .testTag("activity_section_transactions"),
                )
                ActivityModePill(
                    label = "${stringResource(R.string.activity_tab_bills_subs)} (${state.activeRecurringItems.size})",
                    icon = Icons.Rounded.Autorenew,
                    selected = activeSection == ActivitySection.BILLS_AND_SUBS,
                    onClick = {
                        performHaptic(HapticFeedbackType.TextHandleMove)
                        onSelectSection(ActivitySection.BILLS_AND_SUBS)
                    },
                    modifier = Modifier
                        .weight(1f)
                        .testTag("activity_section_bills_subs"),
                )
            }
        }
    }

    AnimatedContent(
        targetState = activeSection,
        transitionSpec = {
            if (reduceMotion) {
                fadeIn(ExpressiveMotion.fastEffects()) togetherWith fadeOut(ExpressiveMotion.fastEffects())
            } else {
                val forward = targetState.ordinal > initialState.ordinal
                val slideIn = slideInHorizontally(ExpressiveMotion.defaultSpatial()) { w ->
                    if (forward) w / 5 else -w / 5
                } + fadeIn(ExpressiveMotion.fastEffects())
                val slideOut = slideOutHorizontally(ExpressiveMotion.fastSpatial()) { w ->
                    if (forward) -w / 6 else w / 6
                } + fadeOut(ExpressiveMotion.fastEffects())
                slideIn togetherWith slideOut
            }
        },
        label = "ActivitySectionTransition",
        modifier = Modifier.fillMaxSize(),
    ) { section ->
        when (section) {
            ActivitySection.BILLS_AND_SUBS -> {
                RecurringAndSubscriptionsContent(
                    state = state,
                    contentPadding = contentPadding,
                    listState = recurringListState,
                    headerContent = sectionHeader,
                    onAddRecurring = onAddRecurring,
                    onEditRecurring = onEditRecurring,
                    onToggleActive = onToggleRecurringActive,
                    onLogPaymentNow = onLogRecurringNow,
                )
            }

            ActivitySection.TRANSACTIONS -> {
                TransactionsFeedContent(
                    state = state,
                    contentPadding = contentPadding,
                    listState = transactionsListState,
                    headerContent = sectionHeader,
                    onSearchChange = onSearchChange,
                    onFilterCategory = onFilterCategory,
                    onEdit = onEdit,
                    onDelete = onDelete,
                )
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun TransactionsFeedContent(
    state: CadenceUiState,
    contentPadding: PaddingValues,
    listState: LazyListState,
    headerContent: @Composable () -> Unit,
    onSearchChange: (String) -> Unit,
    onFilterCategory: (String?) -> Unit,
    onEdit: (Transaction) -> Unit,
    onDelete: (Transaction) -> Unit,
) {
    val performHaptic = rememberPixelHaptics()
    val today = state.today
    val yesterday = remember(today) { today.minusDays(1) }
    val filtered = remember(state.transactions, state.searchQuery, state.selectedFilterCategoryId, state.categories) {
        state.filteredTransactions()
    }
    val dayGroups = remember(filtered) {
        filtered.groupBy { it.date }.map { (date, list) ->
            TransactionDayGroup(
                date = date,
                total = list.sumOf { it.amount },
                items = list,
            )
        }
    }
    val categoryMap = remember(state.categories) { state.categories.associateBy { it.id } }

    LazyColumn(
        state = listState,
        modifier = Modifier
            .fillMaxSize()
            .testTag("transactions_list"),
        contentPadding = contentPadding,
        verticalArrangement = Arrangement.spacedBy(2.dp),
    ) {
        item(key = "activity_controls", contentType = "activity_controls") {
            Column(
                modifier = Modifier.padding(bottom = 10.dp),
                verticalArrangement = Arrangement.spacedBy(10.dp),
            ) {
                headerContent()
                OutlinedTextField(
                    value = state.searchQuery,
                    onValueChange = onSearchChange,
                    placeholder = { Text(stringResource(R.string.activity_search_placeholder)) },
                    leadingIcon = { Icon(Icons.Rounded.Search, contentDescription = null) },
                    trailingIcon = {
                        if (state.searchQuery.isNotEmpty()) {
                            IconButton(
                                onClick = { onSearchChange("") },
                                modifier = Modifier.testTag("clear_search_button"),
                            ) {
                                Icon(
                                    imageVector = Icons.Rounded.Close,
                                    contentDescription = stringResource(R.string.cd_clear_search),
                                )
                            }
                        }
                    },
                    singleLine = true,
                    shape = CircleShape,
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("activity_search_input"),
                    colors = OutlinedTextFieldDefaults.colors(
                        unfocusedContainerColor = MaterialTheme.colorScheme.surfaceContainerHigh,
                        focusedContainerColor = MaterialTheme.colorScheme.surfaceContainerHighest,
                        unfocusedBorderColor = Color.Transparent,
                        focusedBorderColor = Color.Transparent,
                    ),
                )
                LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    items(
                        items = state.categories,
                        key = { it.id },
                        contentType = { "filter_chip" },
                    ) { cat ->
                        FilterChip(
                            selected = state.selectedFilterCategoryId == cat.id,
                            onClick = {
                                performHaptic(HapticFeedbackType.TextHandleMove)
                                onFilterCategory(cat.id)
                            },
                            label = { Text(cat.name) },
                            leadingIcon = {
                                Icon(
                                    imageVector = cat.icon,
                                    contentDescription = null,
                                    modifier = Modifier.size(16.dp),
                                )
                            },
                            shape = CircleShape,
                            modifier = Modifier.testTag("filter_chip_${cat.id}"),
                        )
                    }
                }
            }
        }

        if (dayGroups.isEmpty()) {
            item(key = "empty_activity", contentType = "empty_activity") {
                val hasActiveFilters = state.searchQuery.isNotBlank() || state.selectedFilterCategoryId != null
                Surface(
                    shape = RoundedCornerShape(32.dp),
                    color = MaterialTheme.colorScheme.surfaceContainerLow,
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(top = 8.dp),
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 24.dp, vertical = 40.dp),
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.spacedBy(10.dp),
                    ) {
                        Box(
                            modifier = Modifier
                                .size(56.dp)
                                .clip(CircleShape)
                                .background(MaterialTheme.colorScheme.secondaryContainer),
                            contentAlignment = Alignment.Center,
                        ) {
                            Icon(
                                imageVector = Icons.Rounded.ReceiptLong,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.onSecondaryContainer,
                                modifier = Modifier.size(28.dp),
                            )
                        }
                        Text(
                            text = if (hasActiveFilters) stringResource(R.string.activity_no_matches_title)
                            else stringResource(R.string.activity_empty_title),
                            style = MaterialTheme.typography.titleLarge,
                            textAlign = TextAlign.Center,
                        )
                        Text(
                            text = if (hasActiveFilters) stringResource(R.string.activity_no_matches_subtitle)
                            else stringResource(R.string.activity_empty_subtitle),
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            textAlign = TextAlign.Center,
                        )
                    }
                }
            }
        } else {
            dayGroups.forEach { group ->
                val date = group.date
                val itemsForDate = group.items
                item(
                    key = "header-${date.toEpochDay()}",
                    contentType = "date_header",
                ) {
                    val dateTitle = when (date) {
                        today -> stringResource(R.string.entry_today)
                        yesterday -> "Yesterday"
                        else -> date.formatFullGroupHeader()
                    }
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(top = 14.dp, bottom = 8.dp, start = 8.dp, end = 8.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                    ) {
                        Text(
                            text = dateTitle,
                            style = MaterialTheme.typography.labelLarge,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                        Text(
                            text = "-${formatCurrency(group.total, showCents = true)}",
                            style = MaterialTheme.typography.labelLarge,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                    }
                }

                itemsIndexed(
                    items = itemsForDate,
                    key = { _, tx -> tx.id },
                    contentType = { _, _ -> "transaction_row" },
                ) { index, tx ->
                    val shape = segmentedItemShape(index, itemsForDate.size)
                    val category = categoryMap[tx.categoryId]

                    val dismissState = rememberSwipeToDismissBoxState(
                        confirmValueChange = { value ->
                            when (value) {
                                SwipeToDismissBoxValue.EndToStart -> {
                                    performHaptic(HapticFeedbackType.LongPress)
                                    onDelete(tx)
                                    true
                                }

                                SwipeToDismissBoxValue.StartToEnd -> {
                                    performHaptic(HapticFeedbackType.TextHandleMove)
                                    onEdit(tx)
                                    false
                                }

                                SwipeToDismissBoxValue.Settled -> false
                            }
                        }
                    )

                    SwipeToDismissBox(
                        state = dismissState,
                        modifier = Modifier
                            .animateItem()
                            .clip(shape)
                            .testTag("transaction_row_${tx.id}"),
                        backgroundContent = {
                            val isDelete = dismissState.targetValue == SwipeToDismissBoxValue.EndToStart
                            val bg = if (isDelete) MaterialTheme.colorScheme.errorContainer
                            else MaterialTheme.colorScheme.secondaryContainer
                            val fg = if (isDelete) MaterialTheme.colorScheme.onErrorContainer
                            else MaterialTheme.colorScheme.onSecondaryContainer

                            Box(
                                modifier = Modifier
                                    .fillMaxSize()
                                    .background(bg)
                                    .padding(horizontal = 24.dp),
                                contentAlignment = if (isDelete) Alignment.CenterEnd else Alignment.CenterStart,
                            ) {
                                Icon(
                                    imageVector = if (isDelete) Icons.Rounded.DeleteOutline else Icons.Rounded.Edit,
                                    contentDescription = if (isDelete) stringResource(R.string.cd_delete_transaction)
                                    else stringResource(R.string.cd_edit_transaction),
                                    tint = fg,
                                )
                            }
                        },
                    ) {
                        TransactionSegmentedRow(
                            tx = tx,
                            category = category,
                            shape = shape,
                            compact = state.compactCards,
                            onClick = { onEdit(tx) },
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun ActivityModePill(
    label: String,
    icon: ImageVector,
    selected: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val bg by animateColorAsState(
        targetValue = if (selected) MaterialTheme.colorScheme.primary else Color.Transparent,
        animationSpec = ExpressiveMotion.fastEffects(),
        label = "ActModeBg_$label",
    )
    val fg by animateColorAsState(
        targetValue = if (selected) MaterialTheme.colorScheme.onPrimary
        else MaterialTheme.colorScheme.onSurfaceVariant,
        animationSpec = ExpressiveMotion.fastEffects(),
        label = "ActModeFg_$label",
    )
    Row(
        modifier = modifier
            .heightIn(min = 44.dp)
            .clip(CircleShape)
            .background(bg)
            .clickable(onClick = onClick)
            .padding(horizontal = 10.dp, vertical = 10.dp),
        horizontalArrangement = Arrangement.Center,
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Icon(
            imageVector = icon,
            contentDescription = null,
            tint = fg,
            modifier = Modifier.size(17.dp),
        )
        Spacer(Modifier.width(6.dp))
        Text(
            text = label,
            style = MaterialTheme.typography.labelLarge,
            color = fg,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
        )
    }
}

private fun segmentedItemShape(index: Int, count: Int): RoundedCornerShape = when {
    count == 1 -> SingleSegmentedShape
    index == 0 -> TopSegmentedShape
    index == count - 1 -> BottomSegmentedShape
    else -> MiddleSegmentedShape
}

@Composable
private fun TransactionSegmentedRow(
    tx: Transaction,
    category: BudgetCategory?,
    shape: RoundedCornerShape,
    compact: Boolean,
    onClick: () -> Unit,
) {
    Surface(
        onClick = onClick,
        modifier = Modifier.fillMaxWidth(),
        shape = shape,
        color = MaterialTheme.colorScheme.surfaceContainerLow,
    ) {
        Row(
            modifier = Modifier.padding(
                horizontal = 16.dp,
                vertical = if (compact) 10.dp else 14.dp,
            ),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(14.dp),
        ) {
            Box(
                modifier = Modifier
                    .size(42.dp)
                    .clip(CircleShape)
                    .background((category?.accentColor ?: MaterialTheme.colorScheme.primary).copy(alpha = 0.16f)),
                contentAlignment = Alignment.Center,
            ) {
                if (category != null) {
                    Icon(
                        imageVector = category.icon,
                        contentDescription = null,
                        tint = category.accentColor,
                        modifier = Modifier.size(20.dp),
                    )
                }
            }
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = tx.merchantOrNote,
                    style = MaterialTheme.typography.titleMedium,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
                Text(
                    text = category?.name ?: "General",
                    style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
            Text(
                text = "-${formatCurrency(tx.amount, showCents = true)}",
                style = MaterialTheme.typography.titleMedium,
                color = MaterialTheme.colorScheme.onSurface,
            )
        }
    }
}
