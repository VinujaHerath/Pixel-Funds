package com.example.data

import kotlinx.coroutines.flow.Flow

/**
 * Common abstraction for both on-device Room storage (guest/local mode)
 * and authenticated Cloud Firestore sync.
 */
interface BudgetRepository {
    fun observeHasSampleData(): Flow<Boolean>
    fun observeCategories(): Flow<List<BudgetCategory>>
    fun observeTransactions(): Flow<List<Transaction>>

    suspend fun ensureInitialSeed(): Result<Unit>
    suspend fun clearSampleData(): Result<Unit>
    suspend fun restoreSampleData(): Result<Unit>

    suspend fun addTransaction(
        categoryId: String,
        amount: Double,
        merchantOrNote: String,
        date: LocalDay,
        sample: Boolean = false,
    ): Result<Unit>

    suspend fun updateTransaction(
        transactionId: String,
        categoryId: String,
        amount: Double,
        merchantOrNote: String,
        date: LocalDay,
    ): Result<Unit>

    suspend fun deleteTransaction(transactionId: String): Result<Unit>

    suspend fun createCategory(
        name: String,
        iconKey: String,
        colorHex: Long,
        monthlyLimit: Double,
        sortOrder: Long,
    ): Result<Unit>

    suspend fun updateCategory(
        categoryId: String,
        name: String,
        iconKey: String,
        colorHex: Long,
        monthlyLimit: Double,
    ): Result<Unit>

    suspend fun updateCategoryLimit(
        categoryId: String,
        newLimit: Double,
    ): Result<Unit>

    suspend fun deleteCategoryAndReassign(
        categoryId: String,
        fallbackCategoryId: String,
    ): Result<Unit>
}

data class SeedCategory(
    val id: String,
    val name: String,
    val iconKey: String,
    val colorHex: Long,
    val monthlyLimit: Double,
)

data class SeedTransaction(
    val categoryId: String,
    val amount: Double,
    val merchantOrNote: String,
    val date: LocalDay,
)

object StarterBudgetData {
    fun defaultCategories(): List<SeedCategory> = listOf(
        SeedCategory("dining", "Dining", "dining", 0xFFE07A5FL, 450.0),
        SeedCategory("groceries", "Groceries", "groceries", 0xFF3D8B68L, 600.0),
        SeedCategory("transit", "Transit", "transit", 0xFF4A78B8L, 220.0),
        SeedCategory("shopping", "Shopping", "shopping", 0xFF8E63B5L, 350.0),
        SeedCategory("coffee", "Coffee", "coffee", 0xFFB57B4CL, 120.0),
        SeedCategory("wellness", "Wellness", "wellness", 0xFF2D9596L, 180.0),
    )

    fun defaultTransactions(today: LocalDay): List<SeedTransaction> {
        fun dayOffset(daysAgo: Long): LocalDay {
            val candidate = today.minusDays(daysAgo)
            return if (candidate.isSameMonth(today)) candidate else LocalDay(today.year, today.month, 1)
        }
        return listOf(
            SeedTransaction("coffee", 6.50, "Blue Bottle Oat Latte", dayOffset(0)),
            SeedTransaction("groceries", 84.20, "Trader Joe's Weekly Run", dayOffset(0)),
            SeedTransaction("dining", 54.00, "Ramen Nagi Dinner", dayOffset(1)),
            SeedTransaction("transit", 32.00, "Metro Pass Top-Up", dayOffset(1)),
            SeedTransaction("shopping", 148.50, "Running Shoes", dayOffset(2)),
            SeedTransaction("dining", 112.40, "Friday Izakaya", dayOffset(3)),
            SeedTransaction("groceries", 136.80, "Whole Foods Market", dayOffset(4)),
            SeedTransaction("wellness", 65.00, "Climbing Gym Monthly", dayOffset(5)),
            SeedTransaction("dining", 195.00, "Anniversary Bistro", dayOffset(6)),
            SeedTransaction("coffee", 42.00, "Single Origin Beans x2", dayOffset(7)),
        )
    }
}
