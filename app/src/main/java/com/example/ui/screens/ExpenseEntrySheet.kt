package com.example.ui.screens

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
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
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.Backspace
import androidx.compose.material.icons.rounded.CalendarToday
import androidx.compose.material.icons.rounded.Check
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
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.example.R
import com.example.data.BudgetCategory
import com.example.data.LocalDay
import com.example.data.Transaction
import com.example.ui.components.formatCurrency
import com.example.ui.theme.ExpressiveMotion
import java.util.Locale

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ExpenseEntryBottomSheet(
    categories: List<BudgetCategory>,
    editingTransaction: Transaction?,
    isSaving: Boolean,
    onDismiss: () -> Unit,
    onSave: (amount: Double, categoryId: String, note: String, date: LocalDay) -> Unit,
) {
    if (categories.isEmpty()) return

    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    val haptics = LocalHapticFeedback.current
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
                .navigationBarsPadding()
                .padding(horizontal = 20.dp)
                .padding(bottom = 20.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp),
        ) {
            // 1. Expressive Amount-First Hero Readout
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 4.dp),
                contentAlignment = Alignment.Center,
            ) {
                Row(verticalAlignment = Alignment.Top) {
                    Text(
                        text = "$",
                        style = MaterialTheme.typography.headlineMedium,
                        color = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.padding(top = 8.dp, end = 2.dp),
                    )
                    AnimatedContent(
                        targetState = displayAmount,
                        transitionSpec = {
                            (slideInVertically(ExpressiveMotion.fastSpatial()) { -it / 4 } +
                                fadeIn(ExpressiveMotion.fastEffects())) togetherWith
                                (slideOutVertically(ExpressiveMotion.fastSpatial()) { it / 4 } +
                                    fadeOut(ExpressiveMotion.fastEffects()))
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
                                haptics.performHapticFeedback(HapticFeedbackType.TextHandleMove)
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

            // 3. Inline Optional Note + Date Selector Row
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
                        haptics.performHapticFeedback(HapticFeedbackType.TextHandleMove)
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

            // 4. Tactile Spring Numpad
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
                                    haptics.performHapticFeedback(HapticFeedbackType.TextHandleMove)
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

            // 5. Primary Expressive Save Pill
            Button(
                onClick = {
                    if (parsedAmount > 0.0 && !isSaving) {
                        haptics.performHapticFeedback(HapticFeedbackType.LongPress)
                        onSave(parsedAmount, selectedCategoryId, note, selectedDate)
                    }
                },
                enabled = parsedAmount > 0.0 && !isSaving,
                shape = CircleShape,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(56.dp)
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
                    Text(
                        text = if (editingTransaction != null) stringResource(R.string.entry_update_expense)
                        else "Log ${formatCurrency(parsedAmount, showCents = true)}",
                        style = MaterialTheme.typography.titleMedium,
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
private fun NumpadKeyButton(
    label: String,
    modifier: Modifier,
    onClick: () -> Unit,
) {
    val interactionSource = remember { MutableInteractionSource() }
    val pressed by interactionSource.collectIsPressedAsState()
    val scale by animateFloatAsState(
        targetValue = if (pressed) 0.92f else 1f,
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
            .height(54.dp)
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
