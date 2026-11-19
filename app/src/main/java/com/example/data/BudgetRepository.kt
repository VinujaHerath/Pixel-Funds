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
    fun observeRecurringItems(): Flow<List<RecurringItem>>

    suspend fun ensureInitialSeed(): Result<Unit>
    suspend fun clearSampleData(): Result<Unit>
    suspend fun restoreSampleData(): Result<Unit>
    suspend fun clearAllTransactions(): Result<Unit>

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

    suspend fun addRecurringItem(
        categoryId: String,
        name: String,
        amount: Double,
        kind: RecurringKind,
        cadence: BillingCadence,
        nextDueDate: LocalDay,
        active: Boolean = true,
        sample: Boolean = false,
    ): Result<Unit>

    suspend fun updateRecurringItem(
        recurringId: String,
        categoryId: String,
        name: String,
        amount: Double,
        kind: RecurringKind,
        cadence: BillingCadence,
        nextDueDate: LocalDay,
        active: Boolean,
    ): Result<Unit>

    suspend fun setRecurringActive(
        recurringId: String,
        active: Boolean,
    ): Result<Unit>

    suspend fun logRecurringPayment(
        item: RecurringItem,
        paidOn: LocalDay,
    ): Result<Unit>

    suspend fun deleteRecurringItem(recurringId: String): Result<Unit>

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

data class SeedRecurring(
    val id: String,
    val categoryId: String,
    val name: String,
    val amount: Double,
    val kind: RecurringKind,
    val cadence: BillingCadence,
    val nextDueDate: LocalDay,
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

    fun defaultRecurringItems(today: LocalDay): List<SeedRecurring> = listOf(
        SeedRecurring(
            id = "rec_metro_pass",
            categoryId = "transit",
            name = "Metro Monthly Pass",
            amount = 86.00,
            kind = RecurringKind.RECURRING,
            cadence = BillingCadence.MONTHLY,
            nextDueDate = today.plusDays(4),
        ),
        SeedRecurring(
            id = "rec_climbing_gym",
            categoryId = "wellness",
            name = "Climbing Gym Membership",
            amount = 65.00,
            kind = RecurringKind.RECURRING,
            cadence = BillingCadence.MONTHLY,
            nextDueDate = today.plusDays(9),
        ),
        SeedRecurring(
            id = "sub_cloud_storage",
            categoryId = "shopping",
            name = "Google One 2TB Cloud",
            amount = 9.99,
            kind = RecurringKind.SUBSCRIPTION,
            cadence = BillingCadence.MONTHLY,
            nextDueDate = today.plusDays(3),
        ),
        SeedRecurring(
            id = "sub_music_stream",
            categoryId = "dining",
            name = "YouTube Music Premium",
            amount = 10.99,
            kind = RecurringKind.SUBSCRIPTION,
            cadence = BillingCadence.MONTHLY,
            nextDueDate = today.plusDays(7),
        ),
    )

    fun defaultTransactions(today: LocalDay): List<SeedTransaction> {
        fun dayOffset(daysAgo: Long): LocalDay {
            val candidate = today.minusDays(daysAgo)
            return if (candidate.isSameMonth(today)) candidate else LocalDay(today.year, today.month, 1)
        }
        fun prevMonthDay(monthsBack: Int, day: Int): LocalDay =
            today.plusMonthsClamped(-monthsBack, day)

        return listOf(
            // Current month
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

            // 1 month ago (powers Month-vs-Month comparison out of the box)
            SeedTransaction("groceries", 245.00, "Whole Foods Groceries", prevMonthDay(1, 4)),
            SeedTransaction("dining", 168.00, "Neighborhood Bistro", prevMonthDay(1, 9)),
            SeedTransaction("shopping", 210.00, "Autumn Jacket", prevMonthDay(1, 14)),
            SeedTransaction("transit", 86.00, "Metro Monthly Pass", prevMonthDay(1, 16)),
            SeedTransaction("wellness", 65.00, "Climbing Gym Monthly", prevMonthDay(1, 20)),
            SeedTransaction("coffee", 58.00, "Cafe & Espresso Beans", prevMonthDay(1, 25)),

            // 2 months ago
            SeedTransaction("groceries", 290.00, "Market & Produce", prevMonthDay(2, 6)),
            SeedTransaction("dining", 225.00, "Sushi & Weekend Brunch", prevMonthDay(2, 13)),
            SeedTransaction("shopping", 125.00, "Desk Accessories", prevMonthDay(2, 19)),
            SeedTransaction("transit", 86.00, "Metro Monthly Pass", prevMonthDay(2, 22)),

            // 3 months ago
            SeedTransaction("groceries", 260.00, "Monthly Groceries", prevMonthDay(3, 8)),
            SeedTransaction("dining", 140.00, "Taco Night & Dinner", prevMonthDay(3, 15)),
            SeedTransaction("wellness", 115.00, "Gym & Physiotherapy", prevMonthDay(3, 21)),

            // Previous year same & adjacent months (powers Year-vs-Year comparison out of the box)
            SeedTransaction("groceries", 275.00, "Groceries (Last Year)", prevMonthDay(12, 5)),
            SeedTransaction("dining", 185.00, "Dinner Out (Last Year)", prevMonthDay(12, 12)),
            SeedTransaction("shopping", 240.00, "Holiday Gifts (Last Year)", prevMonthDay(12, 18)),
            SeedTransaction("groceries", 230.00, "Groceries (Last Year)", prevMonthDay(13, 7)),
            SeedTransaction("dining", 160.00, "Cafe & Dining (Last Year)", prevMonthDay(13, 17)),
            SeedTransaction("transit", 92.00, "Transit Pass (Last Year)", prevMonthDay(14, 10)),
            SeedTransaction("shopping", 175.00, "Seasonal Apparel (Last Year)", prevMonthDay(14, 21)),
        )
    }
}
