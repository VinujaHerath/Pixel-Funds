package com.example.data.local

import androidx.compose.runtime.Immutable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import com.example.data.CategoryIconCatalog
import com.example.data.LocalDay
import com.example.data.Transaction
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.combine

@Immutable
data class CategoryMonthlySpend(
    val categoryId: String,
    val categoryName: String,
    val icon: ImageVector,
    val accentColor: Color,
    val categoryLimit: Double,
    val spentAmount: Double,
    val transactionCount: Int,
) {
    val usageRatio: Float
        get() = if (categoryLimit > 0.0) (spentAmount / categoryLimit).toFloat().coerceAtLeast(0f) else 0f

    val remainingAmount: Double
        get() = categoryLimit - spentAmount
}

@Immutable
data class MonthlyBudgetRoomSnapshot(
    val totalSpent: Double,
    val transactionCount: Int,
    val largestExpense: Double,
    val customMonthlyLimit: Double?,
    val categoryLimitsSum: Double,
    val categoryBreakdowns: List<CategoryMonthlySpend>,
    val recentTransactions: List<Transaction>,
) {
    val effectiveBudgetLimit: Double
        get() = customMonthlyLimit ?: categoryLimitsSum

    val hasCustomLimit: Boolean
        get() = customMonthlyLimit != null
}

/**
 * Repository mediating reactive Room SQLite queries for the Monthly Budget Overview screen.
 */
class MonthlyBudgetOverviewRepository(
    private val overviewDao: MonthlyBudgetOverviewDao,
) {
    fun observePeriodOverview(
        startDay: LocalDay,
        endDay: LocalDay,
        periodKey: String,
    ): Flow<MonthlyBudgetRoomSnapshot> {
        val startEpoch = startDay.toEpochDay()
        val endEpoch = endDay.toEpochDay()
        return combine(
            overviewDao.observeMonthlySpendingSummary(startEpoch, endEpoch),
            overviewDao.observeMonthlyBudgetLimit(periodKey),
            overviewDao.observeTotalCategoryLimits(),
            overviewDao.observeCategoryMonthlySpending(startEpoch, endEpoch),
            overviewDao.observeRecentTransactionsForPeriod(startEpoch, endEpoch, limit = 5),
        ) { summary, customLimitEntity, categoryLimitsSum, categoryRows, recentRows ->
            MonthlyBudgetRoomSnapshot(
                totalSpent = summary.totalSpent,
                transactionCount = summary.transactionCount,
                largestExpense = summary.largestExpense,
                customMonthlyLimit = customLimitEntity?.limitAmount,
                categoryLimitsSum = categoryLimitsSum,
                categoryBreakdowns = categoryRows.map { row ->
                    CategoryMonthlySpend(
                        categoryId = row.categoryId,
                        categoryName = row.categoryName,
                        icon = CategoryIconCatalog.iconFor(row.iconKey),
                        accentColor = Color(row.colorHex),
                        categoryLimit = row.categoryLimit,
                        spentAmount = row.spentAmount,
                        transactionCount = row.transactionCount,
                    )
                },
                recentTransactions = recentRows.map { it.toDomain() },
            )
        }
    }

    suspend fun saveMonthlyBudgetLimit(periodKey: String, limitAmount: Double) {
        require(periodKey.isNotBlank()) { "Period key cannot be blank." }
        require(limitAmount > 0.0 && limitAmount <= 10_000_000.0) {
            "Budget limit must be greater than zero."
        }
        overviewDao.upsertMonthlyBudgetLimit(
            MonthlyBudgetLimitEntity(
                periodKey = periodKey,
                limitAmount = limitAmount,
                updatedAtMillis = System.currentTimeMillis(),
            )
        )
    }

    suspend fun clearMonthlyBudgetLimit(periodKey: String) {
        require(periodKey.isNotBlank()) { "Period key cannot be blank." }
        overviewDao.clearMonthlyBudgetLimit(periodKey)
    }
}
