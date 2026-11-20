package com.example.ui

import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.data.AppPreferencesRepository
import com.example.data.AppThemeMode
import com.example.data.BillingCadence
import com.example.data.BudgetCategory
import com.example.data.BudgetRepository
import com.example.data.CadenceUiState
import com.example.data.LocalDay
import com.example.data.RecurringItem
import com.example.data.RecurringKind
import com.example.data.Transaction
import com.example.data.UserPreferences
import com.example.data.WorkspaceLoadState
import com.example.data.local.ImportedCsvRow
import com.example.data.local.LocalBackupCodec
import com.example.data.local.ParsedImportPayload
import com.example.data.local.PixelFundsBackupData
import com.example.notifications.BudgetAlertDispatcher
import com.example.notifications.BudgetAlertLevel
import com.example.notifications.BudgetThresholdEvaluator
import com.example.ui.components.formatCurrencyValue
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.withContext
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import java.util.Locale

sealed interface CadenceEvent {
    data class ShowMessage(val message: String, val showUndo: Boolean = false) : CadenceEvent
}

private data class LocalUiControls(
    val isMutating: Boolean = false,
    val searchQuery: String = "",
    val selectedFilterCategoryId: String? = null,
    val expandedCategoryId: String? = null,
    val editingTransaction: Transaction? = null,
    val isEntrySheetOpen: Boolean = false,
    val editingCategory: BudgetCategory? = null,
    val isCategorySheetOpen: Boolean = false,
    val editingRecurring: RecurringItem? = null,
    val isRecurringSheetOpen: Boolean = false,
    val defaultRecurringKind: RecurringKind = RecurringKind.RECURRING,
    val isAccountSheetOpen: Boolean = false,
)

private sealed interface DataWorkspaceState {
    data object Loading : DataWorkspaceState
    data class Success(
        val hasSampleData: Boolean,
        val categories: List<BudgetCategory>,
        val transactions: List<Transaction>,
        val recurringItems: List<RecurringItem>,
    ) : DataWorkspaceState

    data class Error(val message: String) : DataWorkspaceState
}

@OptIn(ExperimentalCoroutinesApi::class)
class CadenceViewModel(
    private val repository: BudgetRepository,
    private val preferencesRepository: AppPreferencesRepository,
    private val budgetAlertDispatcher: BudgetAlertDispatcher? = null,
) : ViewModel() {

    private val refreshTrigger = MutableStateFlow(0)
    private val localControls = MutableStateFlow(LocalUiControls())
    private val currentDay = MutableStateFlow(LocalDay.today())
    private val _events = MutableSharedFlow<CadenceEvent>(extraBufferCapacity = 4)
    val events: SharedFlow<CadenceEvent> = _events.asSharedFlow()

    private val _preferencesLoaded = MutableStateFlow(false)
    val preferencesLoaded: StateFlow<Boolean> = _preferencesLoaded

    private val _isOnboardingForced = MutableStateFlow(false)
    val isOnboardingForced: StateFlow<Boolean> = _isOnboardingForced

    private val _tutorialStepIndex = MutableStateFlow<Int?>(null)
    val tutorialStepIndex: StateFlow<Int?> = _tutorialStepIndex

    val userPreferences: StateFlow<UserPreferences> = preferencesRepository.preferencesFlow
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.Eagerly,
            initialValue = UserPreferences(),
        )

    private var lastDeletedTransaction: Transaction? = null

    init {
        viewModelScope.launch {
            preferencesRepository.preferencesFlow.collect {
                if (!_preferencesLoaded.value) {
                    _preferencesLoaded.value = true
                }
            }
        }
        viewModelScope.launch {
            val result = repository.ensureInitialSeed()
            result.onFailure { err ->
                Log.w(TAG, "Initial seed check encountered an error", err)
            }
        }
    }

    private val remoteState: StateFlow<DataWorkspaceState> = refreshTrigger
        .flatMapLatest {
            combine(
                repository.observeHasSampleData(),
                repository.observeCategories(),
                repository.observeTransactions(),
                repository.observeRecurringItems(),
            ) { hasSampleData, categories, transactions, recurringItems ->
                DataWorkspaceState.Success(
                    hasSampleData = hasSampleData,
                    categories = categories,
                    transactions = transactions,
                    recurringItems = recurringItems,
                ) as DataWorkspaceState
            }.catch { error ->
                if (error is CancellationException) throw error
                Log.w(TAG, "Error observing local budget data", error)
                emit(
                    DataWorkspaceState.Error(
                        error.localizedMessage ?: "Unable to load your local budget data right now."
                    )
                )
            }
        }
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000L),
            initialValue = DataWorkspaceState.Loading,
        )

    val uiState: StateFlow<CadenceUiState> = combine(
        remoteState,
        localControls,
        currentDay,
        userPreferences,
    ) { data, controls, today, prefs ->
        when (data) {
            DataWorkspaceState.Loading -> CadenceUiState(
                loadState = WorkspaceLoadState.Loading,
                isMutating = controls.isMutating,
                searchQuery = controls.searchQuery,
                selectedFilterCategoryId = controls.selectedFilterCategoryId,
                expandedCategoryId = controls.expandedCategoryId,
                editingTransaction = controls.editingTransaction,
                isEntrySheetOpen = controls.isEntrySheetOpen,
                editingCategory = controls.editingCategory,
                isCategorySheetOpen = controls.isCategorySheetOpen,
                editingRecurring = controls.editingRecurring,
                isRecurringSheetOpen = controls.isRecurringSheetOpen,
                defaultRecurringKind = controls.defaultRecurringKind,
                isAccountSheetOpen = controls.isAccountSheetOpen,
                today = today,
                budgetResetDay = prefs.budgetResetDay,
                currency = prefs.currency,
                includeFixedBillsInSafeDaily = prefs.includeFixedBillsInSafeDaily,
                warningThresholdRatio = prefs.warningThresholdRatio,
                compactCards = prefs.compactCards,
            )

            is DataWorkspaceState.Error -> CadenceUiState(
                loadState = WorkspaceLoadState.Error(data.message),
                isMutating = controls.isMutating,
                isAccountSheetOpen = controls.isAccountSheetOpen,
                today = today,
                budgetResetDay = prefs.budgetResetDay,
                currency = prefs.currency,
                includeFixedBillsInSafeDaily = prefs.includeFixedBillsInSafeDaily,
                warningThresholdRatio = prefs.warningThresholdRatio,
                compactCards = prefs.compactCards,
            )

            is DataWorkspaceState.Success -> {
                val hasSampleFlag = data.hasSampleData ||
                    data.transactions.any { it.sample } ||
                    data.recurringItems.any { it.sample }
                CadenceUiState(
                    loadState = WorkspaceLoadState.Ready,
                    categories = data.categories,
                    transactions = data.transactions,
                    recurringItems = data.recurringItems,
                    hasSampleData = hasSampleFlag,
                    isMutating = controls.isMutating,
                    searchQuery = controls.searchQuery,
                    selectedFilterCategoryId = controls.selectedFilterCategoryId?.takeIf { id ->
                        data.categories.any { it.id == id }
                    },
                    expandedCategoryId = controls.expandedCategoryId?.takeIf { id ->
                        data.categories.any { it.id == id }
                    },
                    editingTransaction = controls.editingTransaction,
                    isEntrySheetOpen = controls.isEntrySheetOpen,
                    editingCategory = controls.editingCategory,
                    isCategorySheetOpen = controls.isCategorySheetOpen,
                    editingRecurring = controls.editingRecurring,
                    isRecurringSheetOpen = controls.isRecurringSheetOpen,
                    defaultRecurringKind = controls.defaultRecurringKind,
                    isAccountSheetOpen = controls.isAccountSheetOpen,
                    today = today,
                    budgetResetDay = prefs.budgetResetDay,
                    currency = prefs.currency,
                    includeFixedBillsInSafeDaily = prefs.includeFixedBillsInSafeDaily,
                    warningThresholdRatio = prefs.warningThresholdRatio,
                    compactCards = prefs.compactCards,
                )
            }
        }
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000L),
        initialValue = CadenceUiState(loadState = WorkspaceLoadState.Loading),
    )

    fun syncDeviceDate() {
        val freshToday = LocalDay.today()
        if (currentDay.value != freshToday) {
            currentDay.value = freshToday
        }
    }

    // --- Preferences Updates ---

    fun updateBudgetResetDay(day: Int) {
        val clamped = day.coerceIn(1, 31)
        viewModelScope.launch {
            preferencesRepository.setBudgetResetDay(clamped)
            _events.emit(
                CadenceEvent.ShowMessage("Budget resets on the ${LocalDay.ordinalDay(clamped)} of each month")
            )
        }
    }

    fun updateCurrencyCode(code: String) {
        viewModelScope.launch {
            preferencesRepository.setCurrencyCode(code)
            _events.emit(CadenceEvent.ShowMessage("Currency changed to $code"))
        }
    }

    fun updateThemeMode(mode: AppThemeMode) {
        viewModelScope.launch { preferencesRepository.setThemeMode(mode) }
    }

    fun updateDynamicColorInManualTheme(enabled: Boolean) {
        viewModelScope.launch { preferencesRepository.setDynamicColorInManualTheme(enabled) }
    }

    fun updateShowDecimals(enabled: Boolean) {
        viewModelScope.launch { preferencesRepository.setShowDecimals(enabled) }
    }

    fun updateHapticsEnabled(enabled: Boolean) {
        viewModelScope.launch { preferencesRepository.setHapticsEnabled(enabled) }
    }

    fun updateReduceMotion(enabled: Boolean) {
        viewModelScope.launch { preferencesRepository.setReduceMotion(enabled) }
    }

    fun updateWarningThresholdPercent(percent: Int) {
        viewModelScope.launch { preferencesRepository.setWarningThresholdPercent(percent) }
    }

    fun updateBudgetAlertsEnabled(enabled: Boolean) {
        viewModelScope.launch { preferencesRepository.setBudgetAlertsEnabled(enabled) }
    }

    fun updateIncludeFixedBillsInSafeDaily(enabled: Boolean) {
        viewModelScope.launch { preferencesRepository.setIncludeFixedBillsInSafeDaily(enabled) }
    }

    fun updateCompactCards(enabled: Boolean) {
        viewModelScope.launch { preferencesRepository.setCompactCards(enabled) }
    }

    private suspend fun evaluateBudgetThresholdAlerts(
        affectedCategoryIds: Set<String>?,
        emitAlerts: Boolean,
    ) {
        runCatching {
            val prefs = userPreferences.value
            val today = currentDay.value
            val resetDay = prefs.budgetResetDay.coerceIn(1, 31)
            val resetThisMonth = resetDay.coerceIn(1, today.lengthOfMonth())
            val cycleStart = if (today.dayOfMonth >= resetThisMonth) {
                LocalDay(today.year, today.month, resetThisMonth)
            } else {
                today.plusMonthsClamped(-1, resetDay)
            }
            val nextResetDate = if (today.dayOfMonth >= resetThisMonth) {
                today.plusMonthsClamped(1, resetDay)
            } else {
                LocalDay(today.year, today.month, resetThisMonth)
            }

            val categories = repository.observeCategories().first()
            val transactions = repository.observeTransactions().first()
            val spentByCategory = transactions
                .asSequence()
                .filter { it.date >= cycleStart && it.date < nextResetDate }
                .groupBy { it.categoryId }
                .mapValues { (_, list) -> list.sumOf { it.amount } }

            val alreadySentKeys = preferencesRepository.getSentBudgetAlertKeys()
            val evaluation = BudgetThresholdEvaluator.evaluate(
                categories = categories,
                spentByCategory = spentByCategory,
                cycleKey = cycleStart.formatIsoDate(),
                alreadySentKeys = alreadySentKeys,
                onlyCategoryIds = affectedCategoryIds,
                emitAlerts = emitAlerts && prefs.budgetAlertsEnabled,
            )

            if (evaluation.updatedSentKeys != alreadySentKeys) {
                preferencesRepository.setSentBudgetAlertKeys(evaluation.updatedSentKeys)
            }

            if (emitAlerts && prefs.budgetAlertsEnabled) {
                evaluation.alertsToSend.forEach { alert ->
                    budgetAlertDispatcher?.notifyBudgetAlert(
                        alert = alert,
                        currency = prefs.currency,
                        showDecimals = prefs.showDecimals,
                    )
                    val spentText = formatCurrencyValue(
                        amount = alert.spent,
                        currency = prefs.currency,
                        showCents = prefs.showDecimals,
                    )
                    val limitText = formatCurrencyValue(
                        amount = alert.limit,
                        currency = prefs.currency,
                        showCents = prefs.showDecimals,
                    )
                    val message = when (alert.level) {
                        BudgetAlertLevel.WARNING_80 ->
                            "${alert.categoryName} reached ${alert.percentUsed}% of budget ($spentText of $limitText)"
                        BudgetAlertLevel.LIMIT_100 ->
                            "${alert.categoryName} exceeded 100% of budget ($spentText of $limitText)"
                    }
                    _events.emit(CadenceEvent.ShowMessage(message))
                }
            }
        }.onFailure { err ->
            if (err is CancellationException) throw err
            Log.w(TAG, "Budget threshold alert evaluation failed", err)
        }
    }

    // --- Onboarding & Interactive Tutorial ---

    fun completeOnboarding(startTutorial: Boolean, keepSampleData: Boolean) {
        _isOnboardingForced.value = false
        if (startTutorial) {
            _tutorialStepIndex.value = 0
        }
        viewModelScope.launch {
            preferencesRepository.setOnboardingCompleted(true)
            if (!keepSampleData && uiState.value.hasSampleData) {
                repository.clearSampleData()
            }
        }
    }

    fun replayOnboarding() {
        localControls.update { it.copy(isAccountSheetOpen = false) }
        _tutorialStepIndex.value = null
        _isOnboardingForced.value = true
    }

    fun startTutorial() {
        localControls.update { it.copy(isAccountSheetOpen = false, expandedCategoryId = null) }
        _tutorialStepIndex.value = 0
    }

    fun nextTutorialStep(totalSteps: Int) {
        val current = _tutorialStepIndex.value ?: return
        if (current + 1 < totalSteps) {
            _tutorialStepIndex.value = current + 1
        } else {
            dismissTutorial()
        }
    }

    fun previousTutorialStep() {
        val current = _tutorialStepIndex.value ?: return
        if (current > 0) {
            _tutorialStepIndex.value = current - 1
        }
    }

    fun dismissTutorial() {
        _tutorialStepIndex.value = null
        viewModelScope.launch {
            preferencesRepository.setTutorialCompleted(true)
        }
    }

    fun retrySync() {
        syncDeviceDate()
        refreshTrigger.update { it + 1 }
        viewModelScope.launch { repository.ensureInitialSeed() }
    }

    // --- Sheet & UI Controls ---

    fun openNewExpenseSheet() {
        if (uiState.value.categories.isEmpty()) {
            _events.tryEmit(CadenceEvent.ShowMessage("Create a budget category first"))
            openNewCategorySheet()
            return
        }
        localControls.update { it.copy(editingTransaction = null, isEntrySheetOpen = true) }
    }

    fun openEditExpenseSheet(tx: Transaction) {
        localControls.update { it.copy(editingTransaction = tx, isEntrySheetOpen = true) }
    }

    fun dismissEntrySheet() {
        localControls.update { it.copy(isEntrySheetOpen = false, editingTransaction = null) }
    }

    fun openNewCategorySheet() {
        localControls.update { it.copy(editingCategory = null, isCategorySheetOpen = true) }
    }

    fun openEditCategorySheet(category: BudgetCategory) {
        localControls.update { it.copy(editingCategory = category, isCategorySheetOpen = true) }
    }

    fun dismissCategorySheet() {
        localControls.update { it.copy(isCategorySheetOpen = false, editingCategory = null) }
    }

    fun openNewRecurringSheet(defaultKind: RecurringKind = RecurringKind.RECURRING) {
        if (uiState.value.categories.isEmpty()) {
            _events.tryEmit(CadenceEvent.ShowMessage("Create a budget category first"))
            openNewCategorySheet()
            return
        }
        localControls.update {
            it.copy(
                editingRecurring = null,
                defaultRecurringKind = defaultKind,
                isRecurringSheetOpen = true,
            )
        }
    }

    fun openEditRecurringSheet(item: RecurringItem) {
        localControls.update {
            it.copy(
                editingRecurring = item,
                defaultRecurringKind = item.kind,
                isRecurringSheetOpen = true,
            )
        }
    }

    fun dismissRecurringSheet() {
        localControls.update { it.copy(isRecurringSheetOpen = false, editingRecurring = null) }
    }

    fun openAccountSheet() {
        localControls.update { it.copy(isAccountSheetOpen = true) }
    }

    fun dismissAccountSheet() {
        localControls.update { it.copy(isAccountSheetOpen = false) }
    }

    fun updateSearchQuery(query: String) {
        localControls.update { it.copy(searchQuery = query) }
    }

    fun selectFilterCategory(id: String?) {
        localControls.update {
            it.copy(selectedFilterCategoryId = if (it.selectedFilterCategoryId == id) null else id)
        }
    }

    fun expandCategory(id: String?) {
        localControls.update { it.copy(expandedCategoryId = id) }
    }

    // --- Transactions + Optional Recurring/Subscription Creation ---

    fun saveTransaction(
        amount: Double,
        categoryId: String,
        note: String,
        date: LocalDay,
        scheduleKind: RecurringKind? = null,
        scheduleCadence: BillingCadence = BillingCadence.MONTHLY,
    ) {
        val currentCategories = uiState.value.categories
        val fallbackLabel = currentCategories.find { it.id == categoryId }?.name ?: "Expense"
        val cleanLabel = note.trim().ifBlank { fallbackLabel }
        val editing = localControls.value.editingTransaction

        viewModelScope.launch {
            localControls.update { it.copy(isMutating = true) }
            val result = if (editing != null) {
                repository.updateTransaction(
                    transactionId = editing.id,
                    categoryId = categoryId,
                    amount = amount,
                    merchantOrNote = cleanLabel,
                    date = date,
                )
            } else {
                repository.addTransaction(
                    categoryId = categoryId,
                    amount = amount,
                    merchantOrNote = cleanLabel,
                    date = date,
                    sample = false,
                )
            }

            if (result.isSuccess && editing == null && scheduleKind != null) {
                val nextDue = scheduleCadence.nextOccurrenceAfter(date)
                repository.addRecurringItem(
                    categoryId = categoryId,
                    name = cleanLabel,
                    amount = amount,
                    kind = scheduleKind,
                    cadence = scheduleCadence,
                    nextDueDate = nextDue,
                    active = true,
                    sample = false,
                )
            }

            localControls.update {
                if (result.isSuccess) {
                    it.copy(isMutating = false, isEntrySheetOpen = false, editingTransaction = null)
                } else {
                    it.copy(isMutating = false)
                }
            }
            if (result.isSuccess && editing == null && scheduleKind != null) {
                val label = if (scheduleKind == RecurringKind.SUBSCRIPTION) "subscription" else "recurring bill"
                _events.emit(CadenceEvent.ShowMessage("Logged & added $label"))
            }
            if (result.isSuccess) {
                val touchedCategories = buildSet {
                    add(categoryId)
                    editing?.categoryId?.let { add(it) }
                }
                evaluateBudgetThresholdAlerts(
                    affectedCategoryIds = touchedCategories,
                    emitAlerts = true,
                )
            }
            result.onFailure { err ->
                _events.emit(
                    CadenceEvent.ShowMessage(err.localizedMessage ?: "Couldn't save expense")
                )
            }
        }
    }

    fun deleteTransaction(tx: Transaction) {
        lastDeletedTransaction = tx
        viewModelScope.launch {
            val result = repository.deleteTransaction(tx.id)
            if (result.isSuccess) {
                evaluateBudgetThresholdAlerts(
                    affectedCategoryIds = setOf(tx.categoryId),
                    emitAlerts = false,
                )
                _events.emit(
                    CadenceEvent.ShowMessage("Deleted ${tx.merchantOrNote}", showUndo = true)
                )
            } else {
                lastDeletedTransaction = null
                _events.emit(
                    CadenceEvent.ShowMessage(
                        result.exceptionOrNull()?.localizedMessage ?: "Couldn't delete expense"
                    )
                )
            }
        }
    }

    fun undoDelete() {
        val restored = lastDeletedTransaction ?: return
        lastDeletedTransaction = null
        val validCatId = uiState.value.categories.find { it.id == restored.categoryId }?.id
            ?: uiState.value.categories.firstOrNull()?.id
            ?: return

        viewModelScope.launch {
            val result = repository.addTransaction(
                categoryId = validCatId,
                amount = restored.amount,
                merchantOrNote = restored.merchantOrNote,
                date = restored.date,
                sample = restored.sample,
            )
            result.onFailure { err ->
                _events.emit(
                    CadenceEvent.ShowMessage(err.localizedMessage ?: "Couldn't restore expense")
                )
            }
        }
    }

    // --- Recurring Transactions & Subscriptions CRUD ---

    fun saveRecurringItem(
        name: String,
        amount: Double,
        categoryId: String,
        kind: RecurringKind,
        cadence: BillingCadence,
        nextDueDate: LocalDay,
        active: Boolean,
    ) {
        val editing = localControls.value.editingRecurring
        viewModelScope.launch {
            localControls.update { it.copy(isMutating = true) }
            val result = if (editing != null) {
                repository.updateRecurringItem(
                    recurringId = editing.id,
                    categoryId = categoryId,
                    name = name,
                    amount = amount,
                    kind = kind,
                    cadence = cadence,
                    nextDueDate = nextDueDate,
                    active = active,
                )
            } else {
                repository.addRecurringItem(
                    categoryId = categoryId,
                    name = name,
                    amount = amount,
                    kind = kind,
                    cadence = cadence,
                    nextDueDate = nextDueDate,
                    active = active,
                    sample = false,
                )
            }
            localControls.update {
                if (result.isSuccess) {
                    it.copy(isMutating = false, isRecurringSheetOpen = false, editingRecurring = null)
                } else {
                    it.copy(isMutating = false)
                }
            }
            result.onSuccess {
                val typeLabel = if (kind == RecurringKind.SUBSCRIPTION) "subscription" else "recurring bill"
                _events.emit(
                    CadenceEvent.ShowMessage(
                        if (editing != null) "Updated ${name.trim()}" else "Added $typeLabel"
                    )
                )
            }.onFailure { err ->
                _events.emit(
                    CadenceEvent.ShowMessage(err.localizedMessage ?: "Couldn't save schedule")
                )
            }
        }
    }

    fun toggleRecurringActive(item: RecurringItem, active: Boolean) {
        viewModelScope.launch {
            val result = repository.setRecurringActive(item.id, active)
            result.onFailure { err ->
                _events.emit(
                    CadenceEvent.ShowMessage(err.localizedMessage ?: "Couldn't update status")
                )
            }
        }
    }

    fun logRecurringPaymentNow(item: RecurringItem) {
        val today = currentDay.value
        viewModelScope.launch {
            val result = repository.logRecurringPayment(item, today)
            result.onSuccess {
                _events.emit(CadenceEvent.ShowMessage("Logged ${item.name} & updated next due date"))
                evaluateBudgetThresholdAlerts(
                    affectedCategoryIds = setOf(item.categoryId),
                    emitAlerts = true,
                )
            }.onFailure { err ->
                _events.emit(
                    CadenceEvent.ShowMessage(err.localizedMessage ?: "Couldn't log payment")
                )
            }
        }
    }

    fun deleteRecurringItem(item: RecurringItem) {
        viewModelScope.launch {
            localControls.update { it.copy(isMutating = true) }
            val result = repository.deleteRecurringItem(item.id)
            localControls.update {
                it.copy(isMutating = false, isRecurringSheetOpen = false, editingRecurring = null)
            }
            result.onSuccess {
                _events.emit(CadenceEvent.ShowMessage("Removed ${item.name}"))
            }.onFailure { err ->
                _events.emit(
                    CadenceEvent.ShowMessage(err.localizedMessage ?: "Couldn't delete item")
                )
            }
        }
    }

    // --- Categories CRUD ---

    fun updateCategoryLimit(categoryId: String, newLimit: Double) {
        viewModelScope.launch {
            val result = repository.updateCategoryLimit(categoryId, newLimit)
            result.onSuccess {
                evaluateBudgetThresholdAlerts(
                    affectedCategoryIds = setOf(categoryId),
                    emitAlerts = true,
                )
            }.onFailure { err ->
                _events.emit(
                    CadenceEvent.ShowMessage(err.localizedMessage ?: "Couldn't update limit")
                )
            }
        }
    }

    fun saveCategory(
        name: String,
        iconKey: String,
        colorHex: Long,
        monthlyLimit: Double,
    ) {
        val editing = localControls.value.editingCategory
        val nextSortOrder = (uiState.value.categories.maxOfOrNull { it.sortOrder } ?: -1L) + 1L

        viewModelScope.launch {
            localControls.update { it.copy(isMutating = true) }
            val result = if (editing != null) {
                repository.updateCategory(
                    categoryId = editing.id,
                    name = name,
                    iconKey = iconKey,
                    colorHex = colorHex,
                    monthlyLimit = monthlyLimit,
                )
            } else {
                repository.createCategory(
                    name = name,
                    iconKey = iconKey,
                    colorHex = colorHex,
                    monthlyLimit = monthlyLimit,
                    sortOrder = nextSortOrder,
                )
            }
            localControls.update {
                if (result.isSuccess) {
                    it.copy(isMutating = false, isCategorySheetOpen = false, editingCategory = null)
                } else {
                    it.copy(isMutating = false)
                }
            }
            result.onSuccess {
                _events.emit(
                    CadenceEvent.ShowMessage(
                        if (editing != null) "Updated ${name.trim()}" else "Added ${name.trim()}"
                    )
                )
                if (editing != null) {
                    evaluateBudgetThresholdAlerts(
                        affectedCategoryIds = setOf(editing.id),
                        emitAlerts = true,
                    )
                }
            }.onFailure { err ->
                _events.emit(
                    CadenceEvent.ShowMessage(err.localizedMessage ?: "Couldn't save category")
                )
            }
        }
    }

    fun deleteCategory(category: BudgetCategory) {
        val otherCategories = uiState.value.categories.filterNot { it.id == category.id }
        val fallback = otherCategories.firstOrNull()
        if (fallback == null) {
            _events.tryEmit(CadenceEvent.ShowMessage("Keep at least one budget category"))
            return
        }

        viewModelScope.launch {
            localControls.update { it.copy(isMutating = true) }
            val result = repository.deleteCategoryAndReassign(
                categoryId = category.id,
                fallbackCategoryId = fallback.id,
            )
            localControls.update {
                it.copy(
                    isMutating = false,
                    isCategorySheetOpen = false,
                    editingCategory = null,
                    expandedCategoryId = if (it.expandedCategoryId == category.id) null else it.expandedCategoryId,
                )
            }
            result.onSuccess {
                _events.emit(
                    CadenceEvent.ShowMessage("Removed ${category.name}; entries moved to ${fallback.name}")
                )
            }.onFailure { err ->
                _events.emit(
                    CadenceEvent.ShowMessage(err.localizedMessage ?: "Couldn't delete category")
                )
            }
        }
    }

    // --- Workspace Data Tools & CSV Export ---

    fun clearSampleData() {
        viewModelScope.launch {
            localControls.update { it.copy(isMutating = true) }
            val result = repository.clearSampleData()
            localControls.update { it.copy(isMutating = false) }
            result.onSuccess {
                evaluateBudgetThresholdAlerts(affectedCategoryIds = null, emitAlerts = false)
                _events.emit(CadenceEvent.ShowMessage("Sample data cleared"))
            }.onFailure { err ->
                _events.emit(
                    CadenceEvent.ShowMessage(err.localizedMessage ?: "Couldn't clear sample data")
                )
            }
        }
    }

    fun restoreSampleData() {
        viewModelScope.launch {
            localControls.update { it.copy(isMutating = true) }
            val result = repository.restoreSampleData()
            localControls.update { it.copy(isMutating = false) }
            result.onSuccess {
                _events.emit(CadenceEvent.ShowMessage("Sample data loaded"))
            }.onFailure { err ->
                _events.emit(
                    CadenceEvent.ShowMessage(err.localizedMessage ?: "Couldn't load sample data")
                )
            }
        }
    }

    fun clearAllTransactions() {
        viewModelScope.launch {
            localControls.update { it.copy(isMutating = true) }
            val result = repository.clearAllTransactions()
            localControls.update { it.copy(isMutating = false) }
            result.onSuccess {
                preferencesRepository.setSentBudgetAlertKeys(emptySet())
                _events.emit(CadenceEvent.ShowMessage("All transactions cleared"))
            }.onFailure { err ->
                _events.emit(
                    CadenceEvent.ShowMessage(err.localizedMessage ?: "Couldn't clear transactions")
                )
            }
        }
    }

    fun buildTransactionsCsv(): String {
        val state = uiState.value
        val categoryMap = state.categories.associateBy { it.id }
        val currencyCode = state.currency.code
        val sb = StringBuilder()
        sb.appendLine("Date,Category,Merchant/Note,Amount,Currency")
        state.transactions
            .sortedByDescending { it.date }
            .forEach { tx ->
                val catName = (categoryMap[tx.categoryId]?.name ?: "General").replace("\"", "\"\"")
                val note = tx.merchantOrNote.replace("\"", "\"\"")
                val amt = String.format(Locale.US, "%.2f", tx.amount)
                sb.appendLine("${tx.date.formatIsoDate()},\"$catName\",\"$note\",$amt,$currencyCode")
            }
        return sb.toString()
    }

    suspend fun buildFullBackupJson(): Result<String> = withContext(Dispatchers.IO) {
        repository.exportFullBackup(userPreferences.value).map { snapshot ->
            LocalBackupCodec.encodeBackupJson(snapshot)
        }
    }

    suspend fun parseImportText(rawText: String): Result<ParsedImportPayload> =
        withContext(Dispatchers.Default) {
            LocalBackupCodec.parseImportPayload(rawText)
        }

    fun restoreFullBackup(backup: PixelFundsBackupData) {
        viewModelScope.launch {
            localControls.update { it.copy(isMutating = true) }
            val result = repository.restoreFullBackup(backup)
            if (result.isSuccess) {
                backup.preferences?.let { prefs ->
                    runCatching { preferencesRepository.restorePreferences(prefs) }
                }
            }
            localControls.update { it.copy(isMutating = false) }
            result.onSuccess {
                _events.emit(
                    CadenceEvent.ShowMessage(
                        "Restored ${backup.categories.size} categories, ${backup.transactions.size} transactions & ${backup.recurringItems.size} schedules"
                    )
                )
            }.onFailure { err ->
                _events.emit(
                    CadenceEvent.ShowMessage(err.localizedMessage ?: "Could not restore backup")
                )
            }
        }
    }

    fun importCsvTransactions(rows: List<ImportedCsvRow>, replaceExisting: Boolean) {
        viewModelScope.launch {
            localControls.update { it.copy(isMutating = true) }
            val result = repository.importCsvTransactions(rows, replaceExisting)
            localControls.update { it.copy(isMutating = false) }
            result.onSuccess { count ->
                val mode = if (replaceExisting) "Replaced with" else "Imported"
                _events.emit(CadenceEvent.ShowMessage("$mode $count transactions"))
            }.onFailure { err ->
                _events.emit(
                    CadenceEvent.ShowMessage(err.localizedMessage ?: "Could not import CSV transactions")
                )
            }
        }
    }

    fun showToastMessage(message: String) {
        _events.tryEmit(CadenceEvent.ShowMessage(message))
    }

    private companion object {
        const val TAG = "PixelFundsViewModel"
    }
}
