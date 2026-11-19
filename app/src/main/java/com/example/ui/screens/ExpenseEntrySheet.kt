package com.example.ui.screens

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.expandVertically
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.shrinkVertically
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
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
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.Backspace
import androidx.compose.material.icons.rounded.CalendarToday
import androidx.compose.material.icons.rounded.Check
import androidx.compose.material.icons.rounded.ReceiptLong
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
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Surface
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
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.example.R
import com.example.data.BillingCadence
import com.example.data.BudgetCategory
import com.example.data.LocalDay
import com.example.data.RecurringKind
import com.example.data.Transaction
import com.example.ui.components.LocalAppCurrency
import com.example.ui.components.LocalReduceMotion
import com.example.ui.components.formatCurrency
import com.example.ui.components.rememberPixelHaptics
import com.example.ui.theme.ExpressiveMotion
import java.util.Locale

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ExpenseEntryBottomSheet(
    categories: List<BudgetCategory>,
    editingTransaction: Transaction?,
    isSaving: Boolean,
    onDismiss: () -> Unit,
    onSave: (
        amount: Double,
        categoryId: String,
        note: String,
        date: LocalDay,
        scheduleKind: RecurringKind?,
        scheduleCadence: BillingCadence,
    ) -> Unit,
) {
    if (categories.isEmpty()) return

    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    val performHaptic = rememberPixelHaptics()
    val reduceMotion = LocalReduceMotion.current
    val today = remember { LocalDay.today() }

    var rawAmount by remember(editingTransaction) {
        mutableStateOf(
            editingTransaction?.amount?.let { amt ->
                String.format(Locale.US, "%.2f", amt).trimEnd('0').trimEnd('.')
            } ?: ""
        )
    }
    var selectedCategoryId by remember(editingTransaction, categories) {
        mutableStateOf(
            editingTransaction?.categoryId?.takeIf { id -> categories.any { it.id == id } }
                ?: categories.first().id
        )
    }
    var note by remember(editingTransaction) {
        mutableStateOf(editingTransaction?.merchantOrNote ?: "")
    }
    var selectedDate by remember(editingTransaction) {
        mutableStateOf(editingTransaction?.date ?: today)
    }
    var scheduleKind by remember(editingTransaction) {
        mutableStateOf<RecurringKind?>(null)
    }
    var scheduleCadence by remember(editingTransaction) {
        mutableStateOf(BillingCadence.MONTHLY)
    }
    var showDatePicker by remember { mutableStateOf(false) }

    val parsedAmount = rawAmount.toDoubleOrNull() ?: 0.0
    val displayAmount = if (rawAmount.isEmpty()) "0" else rawAmount

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
                .padding(bottom = 20.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            // 1. Expressive Amount-First Hero Readout
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 2.dp),
                contentAlignment = Alignment.Center,
            ) {
                val currency = LocalAppCurrency.current
                Row(verticalAlignment = Alignment.Top) {
                    Text(
                        text = currency.symbol,
                        style = MaterialTheme.typography.headlineMedium,
                        color = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.padding(top = 8.dp, end = if (currency.usesTightPrefix) 2.dp else 6.dp),
                    )
                    AnimatedContent(
                        targetState = displayAmount,
                        transitionSpec = {
                            if (reduceMotion) {
                                fadeIn(ExpressiveMotion.fastEffects()) togetherWith fadeOut(ExpressiveMotion.fastEffects())
                            } else {
                                (slideInVertically(ExpressiveMotion.fastSpatial()) { -it / 4 } +
                                    fadeIn(ExpressiveMotion.fastEffects())) togetherWith
                                    (slideOutVertically(ExpressiveMotion.fastSpatial()) { it / 4 } +
                                        fadeOut(ExpressiveMotion.fastEffects()))
                            }
                        },
                        label = "AmountDigits",
                    ) { value ->
                        Text(
                            text = value,
                            style = MaterialTheme.typography.displayLarge,
                            color = if (rawAmount.isEmpty()) MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.4f)
                            else MaterialTheme.colorScheme.onSurface,
                            modifier = Modifier.testTag("entry_amount_display"),
                        )
                    }
                }
            }

            // 2. Shape-Morphing One-Tap Category Pills
            LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                items(categories, key = { it.id }) { category ->
                    val selected = category.id == selectedCategoryId
                    val corner by animateDpAsState(
                        targetValue = if (selected) 12.dp else 24.dp,
                        animationSpec = ExpressiveMotion.fastSpatial(),
                        label = "CategoryChipMorph_${category.id}",
                    )
                    Surface(
                        shape = RoundedCornerShape(corner),
                        color = if (selected) MaterialTheme.colorScheme.primaryContainer
                        else MaterialTheme.colorScheme.surfaceContainerHigh,
                        modifier = Modifier
                            .heightIn(min = 48.dp)
                            .clip(RoundedCornerShape(corner))
                            .clickable {
                                performHaptic(HapticFeedbackType.TextHandleMove)
                                selectedCategoryId = category.id
                            }
                            .testTag("entry_category_chip_${category.id}"),
                    ) {
                        Row(
                            modifier = Modifier.padding(horizontal = 14.dp, vertical = 11.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(6.dp),
                        ) {
                            Icon(
                                imageVector = category.icon,
                                contentDescription = null,
                                tint = if (selected) MaterialTheme.colorScheme.onPrimaryContainer else category.accentColor,
                                modifier = Modifier.size(18.dp),
                            )
                            Text(
                                text = category.name,
                                style = MaterialTheme.typography.labelLarge,
                                color = if (selected) MaterialTheme.colorScheme.onPrimaryContainer
                                else MaterialTheme.colorScheme.onSurface,
                            )
                        }
                    }
                }
            }

            // 3. Always-visible side-by-side One-time | Recurring | Subscription selector (for new expenses)
            if (editingTransaction == null) {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(CircleShape)
                            .background(MaterialTheme.colorScheme.surfaceContainerHigh)
                            .padding(4.dp),
                        horizontalArrangement = Arrangement.spacedBy(4.dp),
                    ) {
                        ScheduleTypeSegment(
                            label = stringResource(R.string.entry_one_time),
                            icon = Icons.Rounded.ReceiptLong,
                            selected = scheduleKind == null,
                            onClick = {
                                performHaptic(HapticFeedbackType.TextHandleMove)
                                scheduleKind = null
                            },
                            modifier = Modifier
                                .weight(1f)
                                .testTag("entry_type_onetime"),
                        )
                        ScheduleTypeSegment(
                            label = stringResource(R.string.recurring_bills_short),
                            icon = Icons.Rounded.Repeat,
                            selected = scheduleKind == RecurringKind.RECURRING,
                            onClick = {
                                performHaptic(HapticFeedbackType.TextHandleMove)
                                scheduleKind = RecurringKind.RECURRING
                            },
                            modifier = Modifier
                                .weight(1f)
                                .testTag("entry_type_recurring"),
                        )
                        ScheduleTypeSegment(
                            label = stringResource(R.string.subscription_singular),
                            icon = Icons.Rounded.Subscriptions,
                            selected = scheduleKind == RecurringKind.SUBSCRIPTION,
                            onClick = {
                                performHaptic(HapticFeedbackType.TextHandleMove)
                                scheduleKind = RecurringKind.SUBSCRIPTION
                            },
                            modifier = Modifier
                                .weight(1f)
                                .testTag("entry_type_subscription"),
                        )
                    }

                    AnimatedVisibility(
                        visible = scheduleKind != null,
                        enter = expandVertically(ExpressiveMotion.fastSpatial()) + fadeIn(ExpressiveMotion.fastEffects()),
                        exit = shrinkVertically(ExpressiveMotion.fastSpatial()) + fadeOut(ExpressiveMotion.fastEffects()),
                    ) {
                        Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.spacedBy(6.dp),
                            ) {
                                BillingCadence.entries.forEach { cadence ->
                                    val selected = cadence == scheduleCadence
                                    Surface(
                                        onClick = {
                                            performHaptic(HapticFeedbackType.TextHandleMove)
                                            scheduleCadence = cadence
                                        },
                                        shape = CircleShape,
                                        color = if (selected) MaterialTheme.colorScheme.secondaryContainer
                                        else MaterialTheme.colorScheme.surfaceContainer,
                                        modifier = Modifier.weight(1f),
                                    ) {
                                        Box(
                                            contentAlignment = Alignment.Center,
                                            modifier = Modifier.padding(vertical = 7.dp, horizontal = 4.dp),
                                        ) {
                                            Text(
                                                text = cadence.label,
                                                style = MaterialTheme.typography.labelSmall,
                                                color = if (selected) MaterialTheme.colorScheme.onSecondaryContainer
                                                else MaterialTheme.colorScheme.onSurfaceVariant,
                                                maxLines = 1,
                                                overflow = TextOverflow.Ellipsis,
                                            )
                                        }
                                    }
                                }
                            }
                            Text(
                                text = if (scheduleKind == RecurringKind.SUBSCRIPTION) {
                                    stringResource(R.string.entry_sub_helper)
                                } else {
                                    stringResource(R.string.entry_recurring_helper)
                                },
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                modifier = Modifier.padding(horizontal = 4.dp),
                            )
                        }
                    }
                }
            }

            // 4. Inline Optional Note + Date Selector Row
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                OutlinedTextField(
                    value = note,
                    onValueChange = { if (it.length <= 120) note = it },
                    placeholder = { Text(stringResource(R.string.entry_note_placeholder)) },
                    singleLine = true,
                    shape = RoundedCornerShape(20.dp),
                    modifier = Modifier
                        .weight(1f)
                        .testTag("entry_note_input"),
                    colors = OutlinedTextFieldDefaults.colors(
                        unfocusedContainerColor = MaterialTheme.colorScheme.surfaceContainerHighest.copy(alpha = 0.55f),
                        focusedContainerColor = MaterialTheme.colorScheme.surfaceContainerHighest,
                        unfocusedBorderColor = Color.Transparent,
                    ),
                )
                FilledTonalButton(
                    onClick = {
                        performHaptic(HapticFeedbackType.TextHandleMove)
                        showDatePicker = true
                    },
                    shape = RoundedCornerShape(20.dp),
                    contentPadding = PaddingValues(horizontal = 14.dp, vertical = 16.dp),
                    modifier = Modifier.testTag("entry_date_button"),
                ) {
                    Icon(Icons.Rounded.CalendarToday, contentDescription = null, modifier = Modifier.size(16.dp))
                    Spacer(Modifier.width(6.dp))
                    Text(
                        text = when (selectedDate) {
                            today -> stringResource(R.string.entry_today)
                            today.minusDays(1) -> stringResource(R.string.entry_yesterday)
                            else -> selectedDate.formatShort()
                        },
                        style = MaterialTheme.typography.labelLarge,
                    )
                }
            }

            // 5. Tactile Spring Numpad
            val keys = remember {
                listOf(
                    listOf("1", "2", "3"),
                    listOf("4", "5", "6"),
                    listOf("7", "8", "9"),
                    listOf(".", "0", "DEL"),
                )
            }
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                keys.forEach { row ->
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                    ) {
                        row.forEach { key ->
                            NumpadKeyButton(
                                label = key,
                                modifier = Modifier.weight(1f),
                                onClick = {
                                    performHaptic(HapticFeedbackType.TextHandleMove)
                                    rawAmount = when (key) {
                                        "DEL" -> rawAmount.dropLast(1)
                                        "." -> if (rawAmount.contains(".")) rawAmount
                                        else if (rawAmount.isEmpty()) "0." else "$rawAmount."
                                        else -> {
                                            val dotIdx = rawAmount.indexOf('.')
                                            if (dotIdx != -1 && rawAmount.length - dotIdx > 2) rawAmount
                                            else if (rawAmount.length >= 8) rawAmount
                                            else if (rawAmount == "0") key
                                            else rawAmount + key
                                        }
                                    }
                                },
                            )
                        }
                    }
                }
            }

            // 6. Primary Expressive Save Pill
            Button(
                onClick = {
                    if (parsedAmount > 0.0 && !isSaving) {
                        performHaptic(HapticFeedbackType.LongPress)
                        onSave(
                            parsedAmount,
                            selectedCategoryId,
                            note,
                            selectedDate,
                            if (editingTransaction == null) scheduleKind else null,
                            scheduleCadence,
                        )
                    }
                },
                enabled = parsedAmount > 0.0 && !isSaving,
                shape = CircleShape,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(54.dp)
                    .testTag("entry_save_button"),
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
                    val actionText = when {
                        editingTransaction != null -> stringResource(R.string.entry_update_expense)
                        scheduleKind == RecurringKind.RECURRING -> "Log & track bill ${formatCurrency(parsedAmount, showCents = true)}"
                        scheduleKind == RecurringKind.SUBSCRIPTION -> "Log & track sub ${formatCurrency(parsedAmount, showCents = true)}"
                        else -> "Log ${formatCurrency(parsedAmount, showCents = true)}"
                    }
                    Text(
                        text = actionText,
                        style = MaterialTheme.typography.titleMedium,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                    )
                }
            }
        }
    }

    if (showDatePicker) {
        val datePickerState = rememberDatePickerState(
            initialSelectedDateMillis = selectedDate.toUtcMillis()
        )
        DatePickerDialog(
            onDismissRequest = { showDatePicker = false },
            confirmButton = {
                TextButton(
                    onClick = {
                        datePickerState.selectedDateMillis?.let { millis ->
                            selectedDate = LocalDay.fromUtcMillis(millis)
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
private fun ScheduleTypeSegment(
    label: String,
    icon: ImageVector,
    selected: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val bg by animateColorAsState(
        targetValue = if (selected) MaterialTheme.colorScheme.primary else Color.Transparent,
        animationSpec = ExpressiveMotion.fastEffects(),
        label = "EntrySchedBg_$label",
    )
    val fg by animateColorAsState(
        targetValue = if (selected) MaterialTheme.colorScheme.onPrimary
        else MaterialTheme.colorScheme.onSurfaceVariant,
        animationSpec = ExpressiveMotion.fastEffects(),
        label = "EntrySchedFg_$label",
    )
    Row(
        modifier = modifier
            .heightIn(min = 40.dp)
            .clip(CircleShape)
            .background(bg)
            .clickable(onClick = onClick)
            .padding(horizontal = 6.dp, vertical = 8.dp),
        horizontalArrangement = Arrangement.Center,
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Icon(
            imageVector = icon,
            contentDescription = null,
            tint = fg,
            modifier = Modifier.size(15.dp),
        )
        Spacer(Modifier.width(4.dp))
        Text(
            text = label,
            style = MaterialTheme.typography.labelMedium,
            color = fg,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
        )
    }
}

@Composable
private fun NumpadKeyButton(
    label: String,
    modifier: Modifier,
    onClick: () -> Unit,
) {
    val interactionSource = remember { MutableInteractionSource() }
    val pressed by interactionSource.collectIsPressedAsState()
    val reduceMotion = LocalReduceMotion.current
    val scale by animateFloatAsState(
        targetValue = if (pressed && !reduceMotion) 0.92f else 1f,
        animationSpec = ExpressiveMotion.fastSpatial(),
        label = "KeyPressScale",
    )
    val corner by animateDpAsState(
        targetValue = if (pressed) 14.dp else 24.dp,
        animationSpec = ExpressiveMotion.fastSpatial(),
        label = "KeyPressCorner",
    )

    Box(
        modifier = modifier
            .height(52.dp)
            .scale(scale)
            .clip(RoundedCornerShape(corner))
            .background(
                if (label == "DEL") MaterialTheme.colorScheme.surfaceContainerHighest
                else MaterialTheme.colorScheme.surfaceContainer
            )
            .clickable(
                interactionSource = interactionSource,
                indication = null,
                onClick = onClick,
            )
            .testTag("numpad_key_${label.lowercase()}"),
        contentAlignment = Alignment.Center,
    ) {
        if (label == "DEL") {
            Icon(
                imageVector = Icons.AutoMirrored.Rounded.Backspace,
                contentDescription = stringResource(R.string.cd_delete_digit),
                modifier = Modifier.size(22.dp),
            )
        } else {
            Text(
                text = label,
                style = MaterialTheme.typography.titleLarge,
                color = MaterialTheme.colorScheme.onSurface,
            )
        }
    }
}
