package com.example.ui

import androidx.activity.compose.BackHandler
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.AnimatedVisibilityScope
import androidx.compose.animation.ExperimentalSharedTransitionApi
import androidx.compose.animation.SharedTransitionLayout
import androidx.compose.animation.SharedTransitionScope
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.scaleIn
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Add
import androidx.compose.material.icons.rounded.Close
import androidx.compose.material.icons.rounded.ErrorOutline
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.FilledTonalIconButton
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.NavigationRail
import androidx.compose.material3.NavigationRailItem
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarDuration
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.SnackbarResult
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.credentials.CredentialManager
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.R
import com.example.data.BudgetCategory
import com.example.data.CadenceTab
import com.example.data.Transaction
import com.example.data.WorkspaceLoadState
import com.example.ui.auth.signOut
import com.example.ui.components.FloatingPillToolbar
import com.example.ui.components.WavyProgressIndicator
import com.example.ui.components.formatCurrency
import com.example.ui.screens.BudgetsScreen
import com.example.ui.screens.CategoryEditorBottomSheet
import com.example.ui.screens.DashboardScreen
import com.example.ui.screens.ExpenseEntryBottomSheet
import com.example.ui.screens.InsightsScreen
import com.example.ui.screens.SettingsBottomSheet
import com.example.ui.screens.TransactionsScreen
import com.example.ui.theme.ExpressiveMotion
import com.example.ui.theme.budgetStatusPalette
import com.google.firebase.auth.FirebaseUser

@OptIn(ExperimentalSharedTransitionApi::class)
@Composable
fun CadenceApp(
    viewModel: CadenceViewModel,
    signedInUser: FirebaseUser?,
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val userPreferences by viewModel.userPreferences.collectAsStateWithLifecycle()
    var activeTab by remember { mutableStateOf(CadenceTab.Home) }
    val snackbarHostState = remember { SnackbarHostState() }
    val scope = rememberCoroutineScope()
    val context = LocalContext.current
    val haptics = LocalHapticFeedback.current
    val credentialManager = remember(context) { CredentialManager.create(context) }

    val isCloudSignedIn = signedInUser != null
    val userDisplayName = signedInUser?.displayName?.takeIf { it.isNotBlank() }
        ?: stringResource(R.string.account_default_name)
    val userEmail = signedInUser?.email ?: ""
    val userInitial = signedInUser?.displayName?.firstOrNull()?.uppercaseChar()?.toString()
        ?: signedInUser?.email?.firstOrNull()?.uppercaseChar()?.toString()

    LaunchedEffect(viewModel) {
        viewModel.events.collect { event ->
            when (event) {
                is CadenceEvent.ShowMessage -> {
                    val result = snackbarHostState.showSnackbar(
                        message = event.message,
                        actionLabel = if (event.showUndo) context.getString(R.string.activity_undo) else null,
                        duration = SnackbarDuration.Short,
                    )
                    if (result == SnackbarResult.ActionPerformed && event.showUndo) {
                        viewModel.undoDelete()
                    }
                }
            }
        }
    }

    BackHandler(enabled = uiState.expandedCategoryId != null) {
        viewModel.expandCategory(null)
    }
    BackHandler(enabled = uiState.expandedCategoryId == null && activeTab != CadenceTab.Home) {
        activeTab = CadenceTab.Home
    }

    BoxWithConstraints(modifier = Modifier.fillMaxSize()) {
        val isExpandedWidth = maxWidth >= 720.dp

        SharedTransitionLayout {
            Scaffold(
                containerColor = MaterialTheme.colorScheme.surface,
                snackbarHost = {
                    SnackbarHost(
                        hostState = snackbarHostState,
                        modifier = Modifier.padding(bottom = if (isExpandedWidth) 16.dp else 96.dp),
                    )
                },
            ) { innerPadding ->
                Row(modifier = Modifier.fillMaxSize()) {
                    if (isExpandedWidth) {
                        NavigationRail(
                            containerColor = MaterialTheme.colorScheme.surfaceContainerLow,
                            modifier = Modifier
                                .fillMaxHeight()
                                .statusBarsPadding()
                                .navigationBarsPadding(),
                            header = {
                                FloatingActionButton(
                                    onClick = {
                                        haptics.performHapticFeedback(HapticFeedbackType.LongPress)
                                        viewModel.openNewExpenseSheet()
                                    },
                                    containerColor = MaterialTheme.colorScheme.primary,
                                    contentColor = MaterialTheme.colorScheme.onPrimary,
                                    shape = RoundedCornerShape(20.dp),
                                    modifier = Modifier
                                        .padding(top = 12.dp, bottom = 12.dp)
                                        .testTag("add_expense_fab"),
                                ) {
                                    Icon(
                                        imageVector = Icons.Rounded.Add,
                                        contentDescription = stringResource(R.string.cd_add_expense),
                                    )
                                }
                            },
                        ) {
                            CadenceTab.entries.forEach { tab ->
                                NavigationRailItem(
                                    selected = tab == activeTab,
                                    onClick = {
                                        haptics.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                                        activeTab = tab
                                    },
                                    icon = { Icon(tab.icon, contentDescription = tab.label) },
                                    label = { Text(tab.label) },
                                    modifier = Modifier.testTag("nav_tab_${tab.name.lowercase()}"),
                                )
                            }
                        }
                    }

                    Box(
                        modifier = Modifier
                            .weight(1f)
                            .fillMaxHeight(),
                        contentAlignment = Alignment.TopCenter,
                    ) {
                        when (val load = uiState.loadState) {
                            WorkspaceLoadState.Loading -> {
                                Box(
                                    modifier = Modifier.fillMaxSize(),
                                    contentAlignment = Alignment.Center,
                                ) {
                                    CircularProgressIndicator(color = MaterialTheme.colorScheme.primary)
                                }
                            }

                            is WorkspaceLoadState.Error -> {
                                Box(
                                    modifier = Modifier
                                        .fillMaxSize()
                                        .padding(24.dp),
                                    contentAlignment = Alignment.Center,
                                ) {
                                    Surface(
                                        shape = RoundedCornerShape(32.dp),
                                        color = MaterialTheme.colorScheme.errorContainer,
                                        modifier = Modifier.widthIn(max = 420.dp),
                                    ) {
                                        Column(
                                            modifier = Modifier.padding(24.dp),
                                            horizontalAlignment = Alignment.CenterHorizontally,
                                            verticalArrangement = Arrangement.spacedBy(12.dp),
                                        ) {
                                            Icon(
                                                imageVector = Icons.Rounded.ErrorOutline,
                                                contentDescription = null,
                                                tint = MaterialTheme.colorScheme.onErrorContainer,
                                                modifier = Modifier.size(32.dp),
                                            )
                                            Text(
                                                text = load.message,
                                                style = MaterialTheme.typography.bodyLarge,
                                                color = MaterialTheme.colorScheme.onErrorContainer,
                                                textAlign = TextAlign.Center,
                                            )
                                            Button(
                                                onClick = viewModel::retrySync,
                                                shape = CircleShape,
                                                modifier = Modifier.testTag("retry_sync_button"),
                                            ) {
                                                Text(stringResource(R.string.retry))
                                            }
                                        }
                                    }
                                }
                            }

                            WorkspaceLoadState.Ready -> {
                                AnimatedContent(
                                    targetState = activeTab,
                                    transitionSpec = {
                                        fadeIn(ExpressiveMotion.fastEffects()) +
                                            scaleIn(
                                                initialScale = 0.96f,
                                                animationSpec = ExpressiveMotion.defaultSpatial(),
                                            ) togetherWith fadeOut(ExpressiveMotion.fastEffects())
                                    },
                                    label = "TabCrossfade",
                                    modifier = Modifier
                                        .fillMaxHeight()
                                        .fillMaxWidth()
                                        .widthIn(max = 720.dp),
                                ) { tab ->
                                    val contentPadding = PaddingValues(
                                        top = innerPadding.calculateTopPadding() + 12.dp,
                                        bottom = innerPadding.calculateBottomPadding() +
                                            if (isExpandedWidth) 28.dp else 112.dp,
                                        start = 16.dp,
                                        end = 16.dp,
                                    )
                                    when (tab) {
                                        CadenceTab.Home -> DashboardScreen(
                                            state = uiState,
                                            isCloudSignedIn = isCloudSignedIn,
                                            userInitial = userInitial,
                                            contentPadding = contentPadding,
                                            sharedTransitionScope = this@SharedTransitionLayout,
                                            animatedVisibilityScope = this@AnimatedContent,
                                            onCategoryClick = { viewModel.expandCategory(it.id) },
                                            onViewAllActivity = { activeTab = CadenceTab.Activity },
                                            onOpenAccount = viewModel::openAccountSheet,
                                            onClearSampleData = viewModel::clearSampleData,
                                            onChangeResetDay = viewModel::updateBudgetResetDay,
                                        )

                                        CadenceTab.Budgets -> BudgetsScreen(
                                            state = uiState,
                                            contentPadding = contentPadding,
                                            onLimitChange = viewModel::updateCategoryLimit,
                                            onNewCategory = viewModel::openNewCategorySheet,
                                            onEditCategory = viewModel::openEditCategorySheet,
                                        )

                                        CadenceTab.Activity -> TransactionsScreen(
                                            state = uiState,
                                            contentPadding = contentPadding,
                                            onSearchChange = viewModel::updateSearchQuery,
                                            onFilterCategory = viewModel::selectFilterCategory,
                                            onEdit = viewModel::openEditExpenseSheet,
                                            onDelete = viewModel::deleteTransaction,
                                        )

                                        CadenceTab.Insights -> InsightsScreen(
                                            state = uiState,
                                            contentPadding = contentPadding,
                                        )
                                    }
                                }

                                if (!isExpandedWidth) {
                                    androidx.compose.animation.AnimatedVisibility(
                                        visible = uiState.expandedCategoryId == null,
                                        enter = fadeIn(ExpressiveMotion.fastEffects()),
                                        exit = fadeOut(ExpressiveMotion.fastEffects()),
                                        modifier = Modifier.align(Alignment.BottomCenter),
                                    ) {
                                        FloatingPillToolbar(
                                            activeTab = activeTab,
                                            onSelectTab = { activeTab = it },
                                            onAddExpense = viewModel::openNewExpenseSheet,
                                        )
                                    }
                                }

                                androidx.compose.animation.AnimatedVisibility(
                                    visible = uiState.expandedCategoryId != null,
                                    enter = fadeIn(ExpressiveMotion.fastEffects()),
                                    exit = fadeOut(ExpressiveMotion.fastEffects()),
                                ) {
                                    val category = uiState.categories.find { it.id == uiState.expandedCategoryId }
                                    if (category != null) {
                                        CategoryExpandedDetailOverlay(
                                            category = category,
                                            transactions = uiState.transactionsForCategory(category.id),
                                            spent = uiState.spentForCategoryThisMonth(category.id),
                                            sharedTransitionScope = this@SharedTransitionLayout,
                                            animatedVisibilityScope = this@AnimatedVisibility,
                                            onDismiss = { viewModel.expandCategory(null) },
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

    if (uiState.isEntrySheetOpen) {
        ExpenseEntryBottomSheet(
            categories = uiState.categories,
            editingTransaction = uiState.editingTransaction,
            isSaving = uiState.isMutating,
            onDismiss = viewModel::dismissEntrySheet,
            onSave = viewModel::saveTransaction,
        )
    }

    if (uiState.isCategorySheetOpen) {
        CategoryEditorBottomSheet(
            editingCategory = uiState.editingCategory,
            canDelete = uiState.categories.size > 1,
            isSaving = uiState.isMutating,
            onDismiss = viewModel::dismissCategorySheet,
            onSave = viewModel::saveCategory,
            onDelete = viewModel::deleteCategory,
        )
    }

    if (uiState.isAccountSheetOpen) {
        SettingsBottomSheet(
            preferences = userPreferences,
            isCloudSignedIn = isCloudSignedIn,
            displayName = userDisplayName,
            email = userEmail,
            hasSampleData = uiState.hasSampleData,
            isBusy = uiState.isMutating,
            credentialManager = credentialManager,
            onDismiss = viewModel::dismissAccountSheet,
            onSelectResetDay = viewModel::updateBudgetResetDay,
            onSelectCurrency = viewModel::updateCurrencyCode,
            onSelectThemeMode = viewModel::updateThemeMode,
            onToggleDynamicColorInManualTheme = viewModel::updateDynamicColorInManualTheme,
            onClearSampleData = viewModel::clearSampleData,
            onRestoreSampleData = viewModel::restoreSampleData,
            onSignInSuccess = {
                viewModel.dismissAccountSheet()
            },
            onSignOut = {
                viewModel.dismissAccountSheet()
                signOut(credentialManager = credentialManager, scope = scope)
            },
        )
    }
}

@OptIn(ExperimentalSharedTransitionApi::class)
@Composable
private fun CategoryExpandedDetailOverlay(
    category: BudgetCategory,
    transactions: List<Transaction>,
    spent: Double,
    sharedTransitionScope: SharedTransitionScope,
    animatedVisibilityScope: AnimatedVisibilityScope,
    onDismiss: () -> Unit,
) {
    val ratio = if (category.monthlyLimit > 0.0) (spent / category.monthlyLimit).toFloat().coerceAtLeast(0f) else 0f
    val statusPalette = budgetStatusPalette(ratio)

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.scrim.copy(alpha = 0.48f))
            .clickable(onClick = onDismiss),
        contentAlignment = Alignment.Center,
    ) {
        with(sharedTransitionScope) {
            Surface(
                modifier = Modifier
                    .padding(20.dp)
                    .fillMaxWidth()
                    .widthIn(max = 520.dp)
                    .sharedBounds(
                        sharedContentState = rememberSharedContentState(key = "cat-card-${category.id}"),
                        animatedVisibilityScope = animatedVisibilityScope,
                        boundsTransform = { _, _ -> ExpressiveMotion.defaultSpatial() },
                    )
                    .clickable(enabled = false) {},
                shape = RoundedCornerShape(36.dp),
                color = MaterialTheme.colorScheme.surfaceContainerHigh,
            ) {
                Column(
                    modifier = Modifier.padding(24.dp),
                    verticalArrangement = Arrangement.spacedBy(16.dp),
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(12.dp),
                        ) {
                            Box(
                                modifier = Modifier
                                    .size(48.dp)
                                    .clip(CircleShape)
                                    .background(category.accentColor.copy(alpha = 0.18f)),
                                contentAlignment = Alignment.Center,
                            ) {
                                Icon(category.icon, contentDescription = null, tint = category.accentColor)
                            }
                            Column {
                                Text(category.name, style = MaterialTheme.typography.headlineMedium)
                                Text(
                                    text = "${formatCurrency(spent, false)} of ${formatCurrency(category.monthlyLimit, false)}",
                                    style = MaterialTheme.typography.labelLarge,
                                    color = statusPalette.accent,
                                )
                            }
                        }
                        FilledTonalIconButton(
                            onClick = onDismiss,
                            modifier = Modifier.testTag("close_category_overlay_button"),
                        ) {
                            Icon(
                                imageVector = Icons.Rounded.Close,
                                contentDescription = stringResource(R.string.cd_close),
                            )
                        }
                    }

                    WavyProgressIndicator(
                        progress = ratio.coerceIn(0f, 1f),
                        color = statusPalette.accent,
                        trackColor = MaterialTheme.colorScheme.surfaceContainerLowest,
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(14.dp),
                    )

                    HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.45f))

                    if (transactions.isEmpty()) {
                        Text(
                            text = "No transactions yet this month.",
                            style = MaterialTheme.typography.bodyLarge,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                    } else {
                        transactions.take(5).forEach { tx ->
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically,
                            ) {
                                Column(modifier = Modifier.weight(1f)) {
                                    Text(tx.merchantOrNote, style = MaterialTheme.typography.titleMedium)
                                    Text(
                                        text = tx.date.formatDayOfWeekShort(),
                                        style = MaterialTheme.typography.labelMedium,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                                    )
                                }
                                Text(
                                    text = "-${formatCurrency(tx.amount, showCents = true)}",
                                    style = MaterialTheme.typography.titleMedium,
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}
