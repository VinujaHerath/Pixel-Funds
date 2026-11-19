package com.example.ui.screens

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.expandVertically
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.shrinkVertically
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
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
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Add
import androidx.compose.material.icons.rounded.Check
import androidx.compose.material.icons.rounded.DeleteOutline
import androidx.compose.material.icons.rounded.Edit
import androidx.compose.material.icons.rounded.Tune
import androidx.compose.material3.BottomSheetDefaults
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Slider
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableDoubleStateOf
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.example.R
import com.example.data.BudgetCategory
import com.example.data.CadenceUiState
import com.example.data.CategoryColorPalette
import com.example.data.CategoryIconCatalog
import com.example.ui.components.WavyProgressIndicator
import com.example.ui.components.formatCurrency
import com.example.ui.theme.ExpressiveMotion
import com.example.ui.theme.budgetStatusPalette
import kotlin.math.roundToInt

@Composable
fun BudgetsScreen(
    state: CadenceUiState,
    contentPadding: PaddingValues,
    onLimitChange: (categoryId: String, newLimit: Double) -> Unit,
    onNewCategory: () -> Unit,
    onEditCategory: (BudgetCategory) -> Unit,
) {
    val haptics = LocalHapticFeedback.current
    var expandedCategoryId by remember { mutableStateOf<String?>(null) }

    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .testTag("budgets_list"),
        contentPadding = contentPadding,
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        item(key = "budgets_header") {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 4.dp, vertical = 4.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = stringResource(R.string.budgets_title),
                        style = MaterialTheme.typography.headlineMedium,
                    )
                    Text(
                        text = stringResource(R.string.budgets_subtitle),
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
                Spacer(Modifier.width(12.dp))
                FilledTonalButton(
                    onClick = {
                        haptics.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                        onNewCategory()
                    },
                    shape = CircleShape,
                    contentPadding = PaddingValues(horizontal = 16.dp, vertical = 12.dp),
                    modifier = Modifier.testTag("new_category_button"),
                ) {
                    Icon(Icons.Rounded.Add, contentDescription = null, modifier = Modifier.size(18.dp))
                    Spacer(Modifier.width(6.dp))
                    Text(
                        text = stringResource(R.string.budgets_new_category),
                        style = MaterialTheme.typography.labelLarge,
                    )
                }
            }
        }

        items(state.categories, key = { it.id }) { category ->
            val spent = state.spentForCategoryThisMonth(category.id)
            BudgetLimitCard(
                category = category,
                spent = spent,
                isExpanded = expandedCategoryId == category.id,
                onToggleExpand = {
                    haptics.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                    expandedCategoryId = if (expandedCategoryId == category.id) null else category.id
                },
                onCommitLimit = { newLimit -> onLimitChange(category.id, newLimit) },
                onEditDetails = { onEditCategory(category) },
            )
        }
    }
}

@Composable
private fun BudgetLimitCard(
    category: BudgetCategory,
    spent: Double,
    isExpanded: Boolean,
    onToggleExpand: () -> Unit,
    onCommitLimit: (Double) -> Unit,
    onEditDetails: () -> Unit,
) {
    val haptics = LocalHapticFeedback.current
    var draftLimit by remember(category.id, category.monthlyLimit) {
        mutableDoubleStateOf(category.monthlyLimit)
    }

    val ratio = if (draftLimit > 0.0) (spent / draftLimit).toFloat().coerceAtLeast(0f) else 0f
    val statusPalette = budgetStatusPalette(ratio)

    val accent by animateColorAsState(
        targetValue = statusPalette.accent,
        animationSpec = ExpressiveMotion.fastEffects(),
        label = "BudgetColorShift_${category.id}",
    )
    val animatedProgress by animateFloatAsState(
        targetValue = ratio.coerceIn(0f, 1f),
        animationSpec = ExpressiveMotion.defaultSpatial(),
        label = "BudgetBarSpring_${category.id}",
    )

    Surface(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(28.dp))
            .clickable(onClick = onToggleExpand)
            .testTag("budget_card_${category.id}"),
        shape = RoundedCornerShape(28.dp),
        color = MaterialTheme.colorScheme.surfaceContainerLow,
    ) {
        Column(
            modifier = Modifier.padding(20.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
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
                            .size(44.dp)
                            .clip(CircleShape)
                            .background(category.accentColor.copy(alpha = 0.16f)),
                        contentAlignment = Alignment.Center,
                    ) {
                        Icon(
                            imageVector = category.icon,
                            contentDescription = null,
                            tint = category.accentColor,
                        )
                    }
                    Column {
                        Text(
                            text = category.name,
                            style = MaterialTheme.typography.titleMedium,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis,
                        )
                        val remaining = draftLimit - spent
                        Text(
                            text = if (remaining >= 0.0) "${formatCurrency(remaining, false)} left"
                            else "${formatCurrency(-remaining, false)} over limit",
                            style = MaterialTheme.typography.labelMedium,
                            color = accent,
                        )
                    }
                }

                Spacer(Modifier.width(8.dp))

                Surface(shape = CircleShape, color = statusPalette.container) {
                    Row(
                        modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(4.dp),
                    ) {
                        Text(
                            text = "${formatCurrency(spent, false)} / ${formatCurrency(draftLimit, false)}",
                            style = MaterialTheme.typography.labelLarge,
                            color = statusPalette.onContainer,
                        )
                        Icon(
                            imageVector = Icons.Rounded.Tune,
                            contentDescription = null,
                            tint = statusPalette.onContainer,
                            modifier = Modifier.size(14.dp),
                        )
                    }
                }
            }

            WavyProgressIndicator(
                progress = animatedProgress,
                color = accent,
                trackColor = MaterialTheme.colorScheme.surfaceContainerHighest,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(14.dp),
            )

            AnimatedVisibility(
                visible = isExpanded,
                enter = expandVertically(ExpressiveMotion.defaultSpatial()) + fadeIn(ExpressiveMotion.fastEffects()),
                exit = shrinkVertically(ExpressiveMotion.fastSpatial()) + fadeOut(ExpressiveMotion.fastEffects()),
            ) {
                Column(
                    modifier = Modifier.padding(top = 6.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp),
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        Text(
                            text = stringResource(R.string.budgets_monthly_limit_label),
                            style = MaterialTheme.typography.labelMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                        Text(
                            text = formatCurrency(draftLimit, false),
                            style = MaterialTheme.typography.labelLarge,
                            color = MaterialTheme.colorScheme.primary,
                        )
                    }
                    val sliderMax = maxOf(1500f, (draftLimit.toFloat() * 1.4f).coerceAtMost(10_000f))
                    Slider(
                        value = draftLimit.toFloat().coerceIn(50f, sliderMax),
                        onValueChange = { raw ->
                            val snapped = (raw / 10f).roundToInt() * 10.0
                            if (snapped != draftLimit) {
                                haptics.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                                draftLimit = snapped
                            }
                        },
                        onValueChangeFinished = {
                            if (draftLimit != category.monthlyLimit) {
                                onCommitLimit(draftLimit)
                            }
                        },
                        valueRange = 50f..sliderMax,
                        modifier = Modifier.testTag("budget_slider_${category.id}"),
                    )
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.End,
                    ) {
                        FilledTonalButton(
                            onClick = {
                                haptics.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                                onEditDetails()
                            },
                            shape = CircleShape,
                            modifier = Modifier.testTag("edit_category_button_${category.id}"),
                        ) {
                            Icon(Icons.Rounded.Edit, contentDescription = null, modifier = Modifier.size(16.dp))
                            Spacer(Modifier.width(6.dp))
                            Text(stringResource(R.string.budgets_edit_category))
                        }
                    }
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class, ExperimentalLayoutApi::class)
@Composable
fun CategoryEditorBottomSheet(
    editingCategory: BudgetCategory?,
    canDelete: Boolean,
    isSaving: Boolean,
    onDismiss: () -> Unit,
    onSave: (name: String, iconKey: String, colorHex: Long, monthlyLimit: Double) -> Unit,
    onDelete: (BudgetCategory) -> Unit,
) {
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    val haptics = LocalHapticFeedback.current

    var name by remember(editingCategory) { mutableStateOf(editingCategory?.name ?: "") }
    var limitText by remember(editingCategory) {
        mutableStateOf(
            editingCategory?.monthlyLimit?.roundToInt()?.toString() ?: "250"
        )
    }
    var selectedIconKey by remember(editingCategory) {
        mutableStateOf(editingCategory?.iconKey ?: CategoryIconCatalog.options.first().key)
    }
    var selectedColorHex by remember(editingCategory) {
        mutableLongStateOf(editingCategory?.colorHex ?: CategoryColorPalette.swatches.first())
    }

    val parsedLimit = limitText.toDoubleOrNull() ?: 0.0
    val isValid = name.trim().isNotEmpty() && parsedLimit > 0.0 && parsedLimit <= 1_000_000.0

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
                text = if (editingCategory != null) stringResource(R.string.category_sheet_edit_title)
                else stringResource(R.string.category_sheet_create_title),
                style = MaterialTheme.typography.headlineMedium,
            )

            OutlinedTextField(
                value = name,
                onValueChange = { if (it.length <= 40) name = it },
                placeholder = { Text(stringResource(R.string.category_name_placeholder)) },
                singleLine = true,
                shape = RoundedCornerShape(20.dp),
                modifier = Modifier
                    .fillMaxWidth()
                    .testTag("category_name_input"),
                colors = OutlinedTextFieldDefaults.colors(
                    unfocusedContainerColor = MaterialTheme.colorScheme.surfaceContainerHighest.copy(alpha = 0.55f),
                    focusedContainerColor = MaterialTheme.colorScheme.surfaceContainerHighest,
                    unfocusedBorderColor = Color.Transparent,
                ),
            )

            OutlinedTextField(
                value = limitText,
                onValueChange = { input ->
                    val filtered = input.filter { it.isDigit() || it == '.' }
                    if (filtered.count { it == '.' } <= 1 && filtered.length <= 9) {
                        limitText = filtered
                    }
                },
                placeholder = { Text(stringResource(R.string.category_limit_placeholder)) },
                prefix = { Text("$") },
                singleLine = true,
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                shape = RoundedCornerShape(20.dp),
                modifier = Modifier
                    .fillMaxWidth()
                    .testTag("category_limit_input"),
                colors = OutlinedTextFieldDefaults.colors(
                    unfocusedContainerColor = MaterialTheme.colorScheme.surfaceContainerHighest.copy(alpha = 0.55f),
                    focusedContainerColor = MaterialTheme.colorScheme.surfaceContainerHighest,
                    unfocusedBorderColor = Color.Transparent,
                ),
            )

            // Accent Color Swatches
            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                Text(
                    text = stringResource(R.string.category_pick_color),
                    style = MaterialTheme.typography.labelLarge,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
                FlowRow(
                    horizontalArrangement = Arrangement.spacedBy(10.dp),
                    verticalArrangement = Arrangement.spacedBy(10.dp),
                ) {
                    CategoryColorPalette.swatches.forEach { hex ->
                        val selected = hex == selectedColorHex
                        val swatchColor = Color(hex)
                        Box(
                            modifier = Modifier
                                .size(48.dp)
                                .clip(CircleShape)
                                .background(swatchColor)
                                .then(
                                    if (selected) Modifier.border(
                                        width = 3.dp,
                                        color = MaterialTheme.colorScheme.onSurface,
                                        shape = CircleShape,
                                    ) else Modifier
                                )
                                .clickable {
                                    haptics.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                                    selectedColorHex = hex
                                }
                                .testTag("category_color_$hex"),
                            contentAlignment = Alignment.Center,
                        ) {
                            if (selected) {
                                Icon(
                                    imageVector = Icons.Rounded.Check,
                                    contentDescription = null,
                                    tint = Color.White,
                                    modifier = Modifier.size(20.dp),
                                )
                            }
                        }
                    }
                }
            }

            // Material Rounded Icon Picker
            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                Text(
                    text = stringResource(R.string.category_pick_icon),
                    style = MaterialTheme.typography.labelLarge,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
                FlowRow(
                    horizontalArrangement = Arrangement.spacedBy(10.dp),
                    verticalArrangement = Arrangement.spacedBy(10.dp),
                ) {
                    val activeAccent = Color(selectedColorHex)
                    CategoryIconCatalog.options.forEach { option ->
                        val selected = option.key == selectedIconKey
                        val corner by animateDpAsState(
                            targetValue = if (selected) 14.dp else 24.dp,
                            animationSpec = ExpressiveMotion.fastSpatial(),
                            label = "IconPick_${option.key}",
                        )
                        Box(
                            modifier = Modifier
                                .size(48.dp)
                                .clip(RoundedCornerShape(corner))
                                .background(
                                    if (selected) activeAccent.copy(alpha = 0.22f)
                                    else MaterialTheme.colorScheme.surfaceContainerHigh
                                )
                                .then(
                                    if (selected) Modifier.border(
                                        width = 2.dp,
                                        color = activeAccent,
                                        shape = RoundedCornerShape(corner),
                                    ) else Modifier
                                )
                                .clickable {
                                    haptics.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                                    selectedIconKey = option.key
                                }
                                .testTag("category_icon_${option.key}"),
                            contentAlignment = Alignment.Center,
                        ) {
                            Icon(
                                imageVector = option.icon,
                                contentDescription = option.label,
                                tint = if (selected) activeAccent else MaterialTheme.colorScheme.onSurfaceVariant,
                                modifier = Modifier.size(22.dp),
                            )
                        }
                    }
                }
            }

            // Primary Save Button
            Button(
                onClick = {
                    if (isValid && !isSaving) {
                        haptics.performHapticFeedback(HapticFeedbackType.LongPress)
                        onSave(name.trim(), selectedIconKey, selectedColorHex, parsedLimit)
                    }
                },
                enabled = isValid && !isSaving,
                shape = CircleShape,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(56.dp)
                    .testTag("category_save_button"),
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
                        text = stringResource(R.string.category_save),
                        style = MaterialTheme.typography.titleMedium,
                    )
                }
            }

            if (editingCategory != null && canDelete) {
                OutlinedButton(
                    onClick = {
                        haptics.performHapticFeedback(HapticFeedbackType.LongPress)
                        onDelete(editingCategory)
                    },
                    enabled = !isSaving,
                    shape = CircleShape,
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(52.dp)
                        .testTag("category_delete_button"),
                ) {
                    Icon(
                        imageVector = Icons.Rounded.DeleteOutline,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.error,
                    )
                    Spacer(Modifier.width(8.dp))
                    Text(
                        text = stringResource(R.string.budgets_delete_category),
                        color = MaterialTheme.colorScheme.error,
                        style = MaterialTheme.typography.titleSmall,
                    )
                }
            }
        }
    }
}
