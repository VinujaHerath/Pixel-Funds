package com.example.ui

import android.Manifest
import android.os.Build
import androidx.activity.compose.BackHandler
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.AnimatedVisibilityScope
import androidx.compose.animation.ExperimentalSharedTransitionApi
import androidx.compose.animation.SharedTransitionLayout
import androidx.compose.animation.SharedTransitionScope
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.scaleIn
import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutHorizontally
import androidx.compose.animation.slideOutVertically
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
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
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.R
import com.example.data.BudgetCategory
import com.example.data.CadenceTab
import com.example.data.Transaction
import com.example.data.WorkspaceLoadState
import com.example.notifications.AndroidBudgetAlertNotifier
import com.example.ui.components.AnimatedNavTabIcon
import com.example.ui.components.FloatingPillToolbar
import com.example.ui.components.LocalReduceMotion
import com.example.ui.components.WavyProgressIndicator
import com.example.ui.components.formatCurrency
import com.example.ui.components.rememberPixelHaptics
import com.example.ui.screens.ActivitySection
import com.example.ui.screens.AppTutorialOverlay
import com.example.ui.screens.AppTutorialSteps
import com.example.ui.screens.BudgetsScreen
import com.example.ui.screens.BudgetsSection
import com.example.ui.screens.CategoryEditorBottomSheet
import com.example.ui.screens.DashboardScreen
import com.example.ui.screens.ExpenseEntryBottomSheet
import com.example.ui.screens.InsightsScreen
import com.example.ui.screens.OnboardingScreen
import com.example.ui.screens.RecurringEditorBottomSheet
import com.example.ui.screens.SettingsOverlayScreen
import com.example.ui.screens.TransactionsScreen
import com.example.ui.theme.ExpressiveMotion
import com.example.ui.theme.budgetStatusPalette

@OptIn(ExperimentalSharedTransitionApi::class)
@Composable
fun CadenceApp(
    viewModel: CadenceViewModel,
    overviewViewModel: MonthlyBudgetOverviewViewModel,
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val overviewState by overviewViewModel.uiState.collectAsStateWithLifecycle()
    val userPreferences by viewModel.userPreferences.collectAsStateWithLifecycle()
    val preferencesLoaded by viewModel.preferencesLoaded.collectAsStateWithLifecycle()
    val isOnboardingForced by viewModel.isOnboardingForced.collectAsStateWithLifecycle()
    val tutorialStepIndex by viewModel.tutorialStepIndex.collectAsStateWithLifecycle()

    val showOnboarding = preferencesLoaded && (!userPreferences.hasCompletedOnboarding || isOnboardingForced)

    var activeTab by remember { mutableStateOf(CadenceTab.Home) }
    var budgetsSection by remember { mutableStateOf(BudgetsSection.OVERVIEW) }
    var activitySection by remember { mutableStateOf(ActivitySection.TRANSACTIONS) }
    val snackbarHostState = remember { SnackbarHostState() }
    val context = LocalContext.current
    val performHaptic = rememberPixelHaptics()
    val reduceMotion = LocalReduceMotion.current
    var hasPromptedNotificationPermission by remember { mutableStateOf(false) }

    val expenseNotificationPermissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestPermission(),
    ) { granted ->
        if (!granted) {
            viewModel.showToastMessage(context.getString(R.string.settings_budget_alerts_denied_toast))
        }
    }

    // Keep the visible tab & sub-section synced with the current guided tutorial step
    LaunchedEffect(tutorialStepIndex) {
        val idx = tutorialStepIndex ?: return@LaunchedEffect
        val step = AppTutorialSteps.getOrNull(idx) ?: return@LaunchedEffect
        activeTab = step.targetTab
        if (step.targetTab == CadenceTab.Budgets) {
            budgetsSection = step.targetBudgetsSection
        } else if (step.targetTab == CadenceTab.Activity) {
            activitySection = step.targetActivitySection
        }
    }

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

    BackHandler(enabled = uiState.expandedCategoryId != null && !uiState.isAccountSheetOpen) {
        viewModel.expandCategory(null)
    }
    BackHandler(enabled = uiState.expandedCategoryId == null && !uiState.isAccountSheetOpen && activeTab != CadenceTab.Home) {
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
                        modifier = Modifier.padding(
                            bottom = if (isExpandedWidth || uiState.isAccountSheetOpen) 16.dp else 96.dp
                        ),
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
                                        performHaptic(HapticFeedbackType.LongPress)
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
                                val selected = tab == activeTab
                                var clickNonce by remember(tab) { mutableIntStateOf(0) }
                                NavigationRailItem(
                                    selected = selected,
                                    onClick = {
                                        clickNonce += 1
                                        performHaptic(HapticFeedbackType.TextHandleMove)
                                        activeTab = tab
                                    },
                                    icon = {
                                        AnimatedNavTabIcon(
                                            tab = tab,
                                            selected = selected,
                                            clickNonce = clickNonce,
                                            tint = if (selected) MaterialTheme.colorScheme.onSecondaryContainer
                                            else MaterialTheme.colorScheme.onSurfaceVariant,
                                        )
                                    },
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
                                        if (reduceMotion) {
                                            fadeIn(ExpressiveMotion.fastEffects()) togetherWith
                                                fadeOut(ExpressiveMotion.fastEffects())
                                        } else {
                                            val forward = targetState.ordinal > initialState.ordinal
                                            val enter = slideInHorizontally(ExpressiveMotion.defaultSpatial()) { w ->
                                                if (forward) w / 7 else -w / 7
                                            } + fadeIn(ExpressiveMotion.fastEffects()) +
                                                scaleIn(
                                                    initialScale = 0.97f,
                                                    animationSpec = ExpressiveMotion.defaultSpatial(),
                                                )
                                            val exit = slideOutHorizontally(ExpressiveMotion.fastSpatial()) { w ->
                                                if (forward) -w / 8 else w / 8
                                            } + fadeOut(ExpressiveMotion.fastEffects())
                                            enter togetherWith exit
                                        }
                                    },
                                    label = "TabDirectionalTransition",
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
                                            contentPadding = contentPadding,
                                            sharedTransitionScope = this@SharedTransitionLayout,
                                            animatedVisibilityScope = this@AnimatedContent,
                                            onCategoryClick = { viewModel.expandCategory(it.id) },
                                            onViewAllActivity = {
                                                activitySection = ActivitySection.TRANSACTIONS
                                                activeTab = CadenceTab.Activity
                                            },
                                            onOpenBillsAndSubs = {
                                                activitySection = ActivitySection.BILLS_AND_SUBS
                                                activeTab = CadenceTab.Activity
                                            },
                                            onOpenInsights = {
                                                activeTab = CadenceTab.Insights
                                            },
                                            onOpenAccount = viewModel::openAccountSheet,
                                            onOpenTutorial = viewModel::startTutorial,
                                            onClearSampleData = viewModel::clearSampleData,
                                            onChangeResetDay = viewModel::updateBudgetResetDay,
                                        )

                                        CadenceTab.Budgets -> BudgetsScreen(
                                            state = uiState,
                                            overviewState = overviewState,
                                            activeSection = budgetsSection,
                                            onSelectSection = { budgetsSection = it },
                                            contentPadding = contentPadding,
                                            onLimitChange = viewModel::updateCategoryLimit,
                                            onNewCategory = viewModel::openNewCategorySheet,
                                            onEditCategory = viewModel::openEditCategorySheet,
                                            onPreviousMonth = overviewViewModel::previousMonth,
                                            onNextMonth = overviewViewModel::nextMonth,
                                            onJumpToCurrentMonth = overviewViewModel::jumpToCurrentMonth,
                                            onSetMonthlyLimit = overviewViewModel::setMonthlyBudgetLimit,
                                            onResetMonthlyLimitToCategories = overviewViewModel::resetToCategoryLimitsSum,
                                            onLogExpense = viewModel::openNewExpenseSheet,
                                        )

                                        CadenceTab.Activity -> TransactionsScreen(
                                            state = uiState,
                                            activeSection = activitySection,
                                            onSelectSection = { activitySection = it },
                                            contentPadding = contentPadding,
                                            onSearchChange = viewModel::updateSearchQuery,
                                            onFilterCategory = viewModel::selectFilterCategory,
                                            onEdit = viewModel::openEditExpenseSheet,
                                            onDelete = viewModel::deleteTransaction,
                                            onAddRecurring = viewModel::openNewRecurringSheet,
                                            onEditRecurring = viewModel::openEditRecurringSheet,
                                            onToggleRecurringActive = viewModel::toggleRecurringActive,
                                            onLogRecurringNow = viewModel::logRecurringPaymentNow,
                                        )

                                        CadenceTab.Insights -> InsightsScreen(
                                            state = uiState,
                                            contentPadding = contentPadding,
                                        )
                                    }
                                }

                                if (!isExpandedWidth) {
                                    androidx.compose.animation.AnimatedVisibility(
                                        visible = uiState.expandedCategoryId == null && !uiState.isAccountSheetOpen,
                                        enter = fadeIn(ExpressiveMotion.fastEffects()) +
                                            slideInVertically(ExpressiveMotion.fastSpatial()) { it / 2 },
                                        exit = fadeOut(ExpressiveMotion.fastEffects()) +
                                            slideOutVertically(ExpressiveMotion.fastSpatial()) { it / 2 },
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

            // Full-screen Settings Overlay with smooth Pixel spring slide-up + fade transition
            AnimatedVisibility(
                visible = uiState.isAccountSheetOpen,
                enter = if (reduceMotion) {
                    fadeIn(ExpressiveMotion.fastEffects())
                } else {
                    slideInVertically(ExpressiveMotion.defaultSpatial()) { it / 5 } +
                        fadeIn(ExpressiveMotion.fastEffects())
                },
                exit = if (reduceMotion) {
                    fadeOut(ExpressiveMotion.fastEffects())
                } else {
                    slideOutVertically(ExpressiveMotion.fastSpatial()) { it / 5 } +
                        fadeOut(ExpressiveMotion.fastEffects())
                },
            ) {
                SettingsOverlayScreen(
                    state = uiState,
                    preferences = userPreferences,
                    onDismiss = viewModel::dismissAccountSheet,
                    onSelectResetDay = viewModel::updateBudgetResetDay,
                    onSelectCurrency = viewModel::updateCurrencyCode,
                    onSelectThemeMode = viewModel::updateThemeMode,
                    onToggleDynamicColorInManualTheme = viewModel::updateDynamicColorInManualTheme,
                    onToggleShowDecimals = viewModel::updateShowDecimals,
                    onToggleHaptics = viewModel::updateHapticsEnabled,
                    onToggleReduceMotion = viewModel::updateReduceMotion,
                    onUpdateWarningThreshold = viewModel::updateWarningThresholdPercent,
                    onToggleBudgetAlerts = viewModel::updateBudgetAlertsEnabled,
                    onToggleIncludeFixedBills = viewModel::updateIncludeFixedBillsInSafeDaily,
                    onToggleCompactCards = viewModel::updateCompactCards,
                    onBuildFullBackupJson = viewModel::buildFullBackupJson,
                    onBuildCsv = viewModel::buildTransactionsCsv,
                    onParseImportText = viewModel::parseImportText,
                    onRestoreFullBackup = viewModel::restoreFullBackup,
                    onImportCsvTransactions = viewModel::importCsvTransactions,
                    onExportStatusMessage = viewModel::showToastMessage,
                    onClearSampleData = viewModel::clearSampleData,
                    onRestoreSampleData = viewModel::restoreSampleData,
                    onClearAllTransactions = viewModel::clearAllTransactions,
                    onReplayTutorial = viewModel::startTutorial,
                    onReplayOnboarding = viewModel::replayOnboarding,
                )
            }

            // Interactive 6-step Guided Tutorial Overlay (rendered above live tabs)
            AnimatedVisibility(
                visible = !showOnboarding && tutorialStepIndex != null && !uiState.isAccountSheetOpen,
                enter = fadeIn(ExpressiveMotion.fastEffects()),
                exit = fadeOut(ExpressiveMotion.fastEffects()),
            ) {
                val activeStep = tutorialStepIndex ?: 0
                AppTutorialOverlay(
                    stepIndex = activeStep,
                    onNext = { viewModel.nextTutorialStep(AppTutorialSteps.size) },
                    onBack = viewModel::previousTutorialStep,
                    onDismiss = viewModel::dismissTutorial,
                    onFinish = {
                        activeTab = CadenceTab.Home
                        budgetsSection = BudgetsSection.OVERVIEW
                        activitySection = ActivitySection.TRANSACTIONS
                        viewModel.dismissTutorial()
                    },
                )
            }

            // First-Launch Welcome & Quick Setup Onboarding Flow
            AnimatedVisibility(
                visible = showOnboarding,
                enter = fadeIn(ExpressiveMotion.fastEffects()),
                exit = fadeOut(ExpressiveMotion.fastEffects()),
            ) {
                OnboardingScreen(
                    preferences = userPreferences,
                    hasSampleData = uiState.hasSampleData,
                    onSelectCurrency = viewModel::updateCurrencyCode,
                    onSelectResetDay = viewModel::updateBudgetResetDay,
                    onCompleteOnboarding = { startTour, keepSample ->
                        activeTab = CadenceTab.Home
                        viewModel.completeOnboarding(
                            startTutorial = startTour,
                            keepSampleData = keepSample,
                        )
                    },
                )
            }
        }
    }

    if (uiState.isEntrySheetOpen) {
        ExpenseEntryBottomSheet(
            categories = uiState.categories,
            editingTransaction = uiState.editingTransaction,
            isSaving = uiState.isMutating,
            onDismiss = viewModel::dismissEntrySheet,
            onSave = { amount, categoryId, note, date, scheduleKind, scheduleCadence ->
                if (userPreferences.budgetAlertsEnabled &&
                    Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU &&
                    !hasPromptedNotificationPermission &&
                    !AndroidBudgetAlertNotifier.hasPostNotificationsPermission(context)
                ) {
                    hasPromptedNotificationPermission = true
                    expenseNotificationPermissionLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
                }
                viewModel.saveTransaction(
                    amount = amount,
                    categoryId = categoryId,
                    note = note,
                    date = date,
                    scheduleKind = scheduleKind,
                    scheduleCadence = scheduleCadence,
                )
            },
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

    if (uiState.isRecurringSheetOpen) {
        RecurringEditorBottomSheet(
            categories = uiState.categories,
            editingItem = uiState.editingRecurring,
            defaultKind = uiState.defaultRecurringKind,
            isSaving = uiState.isMutating,
            onDismiss = viewModel::dismissRecurringSheet,
            onSave = viewModel::saveRecurringItem,
            onDelete = viewModel::deleteRecurringItem,
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
