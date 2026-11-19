package com.example.ui

import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.data.BudgetCategory
import com.example.data.BudgetRepository
import com.example.data.CadenceUiState
import com.example.data.LocalDay
import com.example.data.Transaction
import com.example.data.WorkspaceLoadState
import com.google.firebase.firestore.FirebaseFirestoreException
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

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
    val isAccountSheetOpen: Boolean = false,
)

private sealed interface DataWorkspaceState {
    data object Loading : DataWorkspaceState
    data class Success(
        val hasSampleData: Boolean,
        val categories: List<BudgetCategory>,
        val transactions: List<Transaction>,
    ) : DataWorkspaceState

    data class Error(val message: String) : DataWorkspaceState
}

@OptIn(ExperimentalCoroutinesApi::class)
class CadenceViewModel(
    private val repository: BudgetRepository,
) : ViewModel() {

    private val refreshTrigger = MutableStateFlow(0)
    private val localControls = MutableStateFlow(LocalUiControls())
    private val currentDay = MutableStateFlow(LocalDay.today())
    private val _events = MutableSharedFlow<CadenceEvent>(extraBufferCapacity = 4)
    val events: SharedFlow<CadenceEvent> = _events.asSharedFlow()

    private var lastDeletedTransaction: Transaction? = null

    init {
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
            ) { hasSampleData, categories, transactions ->
                DataWorkspaceState.Success(
                    hasSampleData = hasSampleData,
                    categories = categories,
                    transactions = transactions,
                ) as DataWorkspaceState
            }.catch { error ->
                val firestoreError = generateSequence(error) { it.cause }
                    .filterIsInstance<FirebaseFirestoreException>()
                    .firstOrNull()
                if (firestoreError == null && error is CancellationException) throw error
                val rootError = firestoreError ?: error
                Log.w(TAG, "Error observing budget data", rootError)
                emit(
                    DataWorkspaceState.Error(
                        rootError.localizedMessage ?: "Unable to load your budget data right now."
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
    ) { data, controls, today ->
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
                isAccountSheetOpen = controls.isAccountSheetOpen,
                today = today,
            )

            is DataWorkspaceState.Error -> CadenceUiState(
                loadState = WorkspaceLoadState.Error(data.message),
                isMutating = controls.isMutating,
                isAccountSheetOpen = controls.isAccountSheetOpen,
                today = today,
            )

            is DataWorkspaceState.Success -> {
                val hasSampleFlag = data.hasSampleData || data.transactions.any { it.sample }
                CadenceUiState(
                    loadState = WorkspaceLoadState.Ready,
                    categories = data.categories,
                    transactions = data.transactions,
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
                    isAccountSheetOpen = controls.isAccountSheetOpen,
                    today = today,
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

    fun retrySync() {
        syncDeviceDate()
        refreshTrigger.update { it + 1 }
        viewModelScope.launch {
            repository.ensureInitialSeed()
        }
    }

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

    fun saveTransaction(
        amount: Double,
        categoryId: String,
        note: String,
        date: LocalDay,
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
            localControls.update {
                if (result.isSuccess) {
                    it.copy(isMutating = false, isEntrySheetOpen = false, editingTransaction = null)
                } else {
                    it.copy(isMutating = false)
                }
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

    fun updateCategoryLimit(categoryId: String, newLimit: Double) {
        viewModelScope.launch {
            val result = repository.updateCategoryLimit(categoryId, newLimit)
            result.onFailure { err ->
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

    fun clearSampleData() {
        viewModelScope.launch {
            localControls.update { it.copy(isMutating = true) }
            val result = repository.clearSampleData()
            localControls.update { it.copy(isMutating = false, isAccountSheetOpen = false) }
            result.onSuccess {
                _events.emit(CadenceEvent.ShowMessage("Sample transactions cleared"))
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
            localControls.update { it.copy(isMutating = false, isAccountSheetOpen = false) }
            result.onSuccess {
                _events.emit(CadenceEvent.ShowMessage("Sample transactions loaded"))
            }.onFailure { err ->
                _events.emit(
                    CadenceEvent.ShowMessage(err.localizedMessage ?: "Couldn't load sample data")
                )
            }
        }
    }

    private companion object {
        const val TAG = "PixelFundsViewModel"
    }
}
