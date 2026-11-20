package com.example.ui

import androidx.compose.runtime.Immutable
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.data.AppPreferencesRepository
import com.example.data.LocalDay
import com.example.data.Transaction
import com.example.data.UserPreferences
import com.example.data.local.CategoryMonthlySpend
import com.example.data.local.MonthlyBudgetOverviewRepository
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import java.util.Locale

enum class MonthlyBudgetHealth {
    ON_TRACK,
    APPROACHING,
    OVER_LIMIT,
    NO_LIMIT_SET,
}

sealed interface MonthlyBudgetOverviewLoadState {
    data object Loading : MonthlyBudgetOverviewLoadState
    data object Ready : MonthlyBudgetOverviewLoadState
    data class Error(val message: String) : MonthlyBudgetOverviewLoadState
}

@Immutable
data class MonthlyBudgetOverviewUiState(
    val loadState: MonthlyBudgetOverviewLoadState = MonthlyBudgetOverviewLoadState.Loading,
    val monthOffset: Int = 0,
    val periodKey: String = "",
    val periodTitle: String = "",
    val periodRangeLabel: String = "",
    val isCurrentPeriod: Boolean = true,
    val totalDaysInPeriod: Int = 30,
    val daysElapsed: Int = 1,
    val daysRemaining: Int = 29,
    val totalSpent: Double = 0.0,
    val budgetLimit: Double = 0.0,
    val hasCustomLimit: Boolean = false,
    val categoryLimitsSum: Double = 0.0,
    val transactionCount: Int = 0,
    val largestExpense: Double = 0.0,
    val warningThresholdRatio: Float = 0.80f,
    val categoryBreakdowns: List<CategoryMonthlySpend> = emptyList(),
    val recentTransactions: List<Transaction> = emptyList(),
) {
    val remainingAmount: Double
        get() = (budgetLimit - totalSpent).coerceAtLeast(0.0)

    val overBudgetAmount: Double
        get() = (totalSpent - budgetLimit).coerceAtLeast(0.0)

    val isOverBudget: Boolean
        get() = budgetLimit > 0.0 && totalSpent > budgetLimit

    val usageRatio: Float
        get() = if (budgetLimit > 0.0) (totalSpent / budgetLimit).toFloat().coerceAtLeast(0f) else 0f

    val usagePercent: Int
        get() = if (budgetLimit > 0.0) ((totalSpent / budgetLimit) * 100.0).toInt().coerceAtLeast(0) else 0

    val expectedPaceRatio: Float
        get() = (daysElapsed.toFloat() / totalDaysInPeriod.coerceAtLeast(1).toFloat()).coerceIn(0.02f, 1f)

    val dailyAverageSpend: Double
        get() = totalSpent / daysElapsed.coerceAtLeast(1)

    val safeDailyAllowance: Double
        get() = if (isCurrentPeriod && remainingAmount > 0.0) {
            remainingAmount / daysRemaining.coerceAtLeast(1)
        } else {
            0.0
        }

    val projectedMonthEndSpend: Double
        get() = if (isCurrentPeriod) {
            dailyAverageSpend * totalDaysInPeriod
        } else {
            totalSpent
        }

    val health: MonthlyBudgetHealth
        get() = when {
            budgetLimit <= 0.0 -> MonthlyBudgetHealth.NO_LIMIT_SET
            totalSpent > budgetLimit -> MonthlyBudgetHealth.OVER_LIMIT
            usageRatio >= warningThresholdRatio -> MonthlyBudgetHealth.APPROACHING
            else -> MonthlyBudgetHealth.ON_TRACK
        }
}

private data class PeriodWindow(
    val monthOffset: Int,
    val startDay: LocalDay,
    val endDay: LocalDay,
    val periodKey: String,
    val periodTitle: String,
    val periodRangeLabel: String,
    val isCurrentPeriod: Boolean,
    val totalDays: Int,
    val daysElapsed: Int,
    val daysRemaining: Int,
    val warningThresholdRatio: Float,
)

@OptIn(ExperimentalCoroutinesApi::class)
class MonthlyBudgetOverviewViewModel(
    private val repository: MonthlyBudgetOverviewRepository,
    preferencesRepository: AppPreferencesRepository,
) : ViewModel() {

    private val monthOffsetFlow = MutableStateFlow(0)
    private val todayFlow = MutableStateFlow(LocalDay.today())

    private val periodWindowFlow = combine(
        monthOffsetFlow,
        todayFlow,
        preferencesRepository.preferencesFlow.catch { emit(UserPreferences()) },
    ) { offset, today, prefs ->
        buildPeriodWindow(
            monthOffset = offset,
            today = today,
            budgetResetDay = prefs.budgetResetDay,
            warningThresholdRatio = prefs.warningThresholdRatio,
        )
    }

    val uiState: StateFlow<MonthlyBudgetOverviewUiState> = periodWindowFlow
        .flatMapLatest { window ->
            repository.observePeriodOverview(
                startDay = window.startDay,
                endDay = window.endDay,
                periodKey = window.periodKey,
            ).map { snapshot ->
                MonthlyBudgetOverviewUiState(
                    loadState = MonthlyBudgetOverviewLoadState.Ready,
                    monthOffset = window.monthOffset,
                    periodKey = window.periodKey,
                    periodTitle = window.periodTitle,
                    periodRangeLabel = window.periodRangeLabel,
                    isCurrentPeriod = window.isCurrentPeriod,
                    totalDaysInPeriod = window.totalDays,
                    daysElapsed = window.daysElapsed,
                    daysRemaining = window.daysRemaining,
                    totalSpent = snapshot.totalSpent,
                    budgetLimit = snapshot.effectiveBudgetLimit,
                    hasCustomLimit = snapshot.hasCustomLimit,
                    categoryLimitsSum = snapshot.categoryLimitsSum,
                    transactionCount = snapshot.transactionCount,
                    largestExpense = snapshot.largestExpense,
                    warningThresholdRatio = window.warningThresholdRatio,
                    categoryBreakdowns = snapshot.categoryBreakdowns,
                    recentTransactions = snapshot.recentTransactions,
                )
            }.catch { err ->
                if (err is CancellationException) throw err
                emit(
                    MonthlyBudgetOverviewUiState(
                        loadState = MonthlyBudgetOverviewLoadState.Error(
                            err.localizedMessage ?: "Unable to read monthly budget from database."
                        ),
                        monthOffset = window.monthOffset,
                        periodKey = window.periodKey,
                        periodTitle = window.periodTitle,
                        periodRangeLabel = window.periodRangeLabel,
                        isCurrentPeriod = window.isCurrentPeriod,
                    )
                )
            }
        }
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000L),
            initialValue = MonthlyBudgetOverviewUiState(loadState = MonthlyBudgetOverviewLoadState.Loading),
        )

    fun refreshDate() {
        val fresh = LocalDay.today()
        if (todayFlow.value != fresh) {
            todayFlow.value = fresh
        }
    }

    fun previousMonth() {
        monthOffsetFlow.value = (monthOffsetFlow.value - 1).coerceAtLeast(-24)
    }

    fun nextMonth() {
        monthOffsetFlow.value = (monthOffsetFlow.value + 1).coerceAtMost(0)
    }

    fun jumpToCurrentMonth() {
        monthOffsetFlow.value = 0
    }

    fun setMonthlyBudgetLimit(limitAmount: Double) {
        val key = uiState.value.periodKey
        if (key.isBlank() || limitAmount <= 0.0) return
        viewModelScope.launch {
            runCatching {
                repository.saveMonthlyBudgetLimit(periodKey = key, limitAmount = limitAmount)
            }
        }
    }

    fun resetToCategoryLimitsSum() {
        val key = uiState.value.periodKey
        if (key.isBlank()) return
        viewModelScope.launch {
            runCatching {
                repository.clearMonthlyBudgetLimit(periodKey = key)
            }
        }
    }

    companion object {
        internal fun periodKeyFor(startDay: LocalDay): String =
            String.format(Locale.US, "%04d-%02d", startDay.year, startDay.month)

        private fun buildPeriodWindow(
            monthOffset: Int,
            today: LocalDay,
            budgetResetDay: Int,
            warningThresholdRatio: Float,
        ): PeriodWindow {
            val resetThisMonth = budgetResetDay.coerceIn(1, today.lengthOfMonth())
            val currentCycleStart = if (today.dayOfMonth >= resetThisMonth) {
                LocalDay(today.year, today.month, resetThisMonth)
            } else {
                today.plusMonthsClamped(-1, budgetResetDay)
            }
            val startDay = if (monthOffset == 0) {
                currentCycleStart
            } else {
                currentCycleStart.plusMonthsClamped(monthOffset, budgetResetDay)
            }
            val nextStart = startDay.plusMonthsClamped(1, budgetResetDay)
            val endDay = nextStart.minusDays(1)
            val totalDays = (nextStart.toEpochDay() - startDay.toEpochDay()).toInt().coerceAtLeast(1)

            val isCurrent = monthOffset == 0
            val elapsed = if (isCurrent) {
                ((today.toEpochDay() - startDay.toEpochDay()).toInt() + 1).coerceIn(1, totalDays)
            } else {
                totalDays
            }
            val remaining = if (isCurrent) {
                (nextStart.toEpochDay() - today.toEpochDay()).toInt().coerceAtLeast(1)
            } else {
                0
            }

            val title = if (budgetResetDay == 1) {
                startDay.monthYearFullName()
            } else {
                "${startDay.formatShort()} – ${endDay.formatShort()}"
            }
            val rangeLabel = "${startDay.formatShort()} – ${endDay.formatShort()}, ${endDay.year}"

            return PeriodWindow(
                monthOffset = monthOffset,
                startDay = startDay,
                endDay = endDay,
                periodKey = periodKeyFor(startDay),
                periodTitle = title,
                periodRangeLabel = rangeLabel,
                isCurrentPeriod = isCurrent,
                totalDays = totalDays,
                daysElapsed = elapsed,
                daysRemaining = remaining,
                warningThresholdRatio = warningThresholdRatio,
            )
        }
    }
}
