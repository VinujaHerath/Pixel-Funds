package com.example.data

import androidx.annotation.Keep
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.AutoGraph
import androidx.compose.material.icons.rounded.CardGiftcard
import androidx.compose.material.icons.rounded.Checkroom
import androidx.compose.material.icons.rounded.Coffee
import androidx.compose.material.icons.rounded.Devices
import androidx.compose.material.icons.rounded.DirectionsTransit
import androidx.compose.material.icons.rounded.DonutLarge
import androidx.compose.material.icons.rounded.Flight
import androidx.compose.material.icons.rounded.Home
import androidx.compose.material.icons.rounded.LocalGroceryStore
import androidx.compose.material.icons.rounded.MedicalServices
import androidx.compose.material.icons.rounded.Movie
import androidx.compose.material.icons.rounded.Pets
import androidx.compose.material.icons.rounded.ReceiptLong
import androidx.compose.material.icons.rounded.Restaurant
import androidx.compose.material.icons.rounded.Savings
import androidx.compose.material.icons.rounded.School
import androidx.compose.material.icons.rounded.SelfImprovement
import androidx.compose.material.icons.rounded.ShoppingBag
import androidx.compose.material.icons.rounded.SpaceDashboard
import androidx.compose.material.icons.rounded.WaterDrop
import androidx.compose.runtime.Immutable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import com.google.firebase.Timestamp
import com.google.firebase.firestore.DocumentId
import com.google.firebase.firestore.IgnoreExtraProperties
import com.google.firebase.firestore.ServerTimestamp
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date
import java.util.Locale
import java.util.TimeZone

enum class CadenceTab(val label: String, val icon: ImageVector) {
    Home("Home", Icons.Rounded.SpaceDashboard),
    Budgets("Budgets", Icons.Rounded.DonutLarge),
    Activity("Activity", Icons.Rounded.ReceiptLong),
    Insights("Insights", Icons.Rounded.AutoGraph),
}

/**
 * API 24+ compatible timezone-safe calendar date using Howard Hinnant civil-date epoch math.
 */
@Immutable
data class LocalDay(
    val year: Int,
    val month: Int,
    val dayOfMonth: Int,
) : Comparable<LocalDay> {

    fun toEpochDay(): Long {
        val y = (if (month <= 2) year - 1 else year).toLong()
        val era = (if (y >= 0) y else y - 399) / 400
        val yoe = y - era * 400
        val doy = (153 * (month + (if (month > 2) -3 else 9)) + 2) / 5 + dayOfMonth - 1
        val doe = yoe * 365 + yoe / 4 - yoe / 100 + doy
        return era * 146097 + doe - 719468
    }

    fun minusDays(days: Long): LocalDay = fromEpochDay(toEpochDay() - days)

    fun lengthOfMonth(): Int = when (month) {
        1, 3, 5, 7, 8, 10, 12 -> 31
        4, 6, 9, 11 -> 30
        2 -> if (isLeapYear(year)) 29 else 28
        else -> 30
    }

    fun isSameMonth(other: LocalDay): Boolean = year == other.year && month == other.month

    fun monthFullName(): String {
        val cal = toUtcCalendar()
        return SimpleDateFormat("MMMM", Locale.getDefault()).apply {
            timeZone = TimeZone.getTimeZone("UTC")
        }.format(cal.time)
    }

    fun monthYearFullName(): String {
        val cal = toUtcCalendar()
        return SimpleDateFormat("MMMM yyyy", Locale.getDefault()).apply {
            timeZone = TimeZone.getTimeZone("UTC")
        }.format(cal.time)
    }

    fun firstDayOfNextMonth(): LocalDay = if (month == 12) {
        LocalDay(year = year + 1, month = 1, dayOfMonth = 1)
    } else {
        LocalDay(year = year, month = month + 1, dayOfMonth = 1)
    }

    fun formatShort(): String {
        val cal = toUtcCalendar()
        return SimpleDateFormat("MMM d", Locale.getDefault()).apply {
            timeZone = TimeZone.getTimeZone("UTC")
        }.format(cal.time)
    }

    fun formatDayOfWeekShort(): String {
        val cal = toUtcCalendar()
        return SimpleDateFormat("EEE, MMM d", Locale.getDefault()).apply {
            timeZone = TimeZone.getTimeZone("UTC")
        }.format(cal.time)
    }

    fun formatFullGroupHeader(): String {
        val cal = toUtcCalendar()
        return SimpleDateFormat("EEEE, MMM d", Locale.getDefault()).apply {
            timeZone = TimeZone.getTimeZone("UTC")
        }.format(cal.time)
    }

    fun toUtcMillis(): Long = toUtcCalendar().timeInMillis

    private fun toUtcCalendar(): Calendar = Calendar.getInstance(TimeZone.getTimeZone("UTC")).apply {
        clear()
        set(year, month - 1, dayOfMonth, 12, 0, 0)
    }

    override fun compareTo(other: LocalDay): Int = toEpochDay().compareTo(other.toEpochDay())

    companion object {
        fun today(): LocalDay {
            val cal = Calendar.getInstance()
            return LocalDay(
                year = cal.get(Calendar.YEAR),
                month = cal.get(Calendar.MONTH) + 1,
                dayOfMonth = cal.get(Calendar.DAY_OF_MONTH),
            )
        }

        fun fromEpochDay(epochDay: Long): LocalDay {
            val z = epochDay + 719468
            val era = (if (z >= 0) z else z - 146096) / 146097
            val doe = z - era * 146097
            val yoe = (doe - doe / 1460 + doe / 36524 - doe / 146096) / 365
            val y = yoe + era * 400
            val doy = doe - (365 * yoe + yoe / 4 - yoe / 100)
            val mp = (5 * doy + 2) / 153
            val d = doy - (153 * mp + 2) / 5 + 1
            val m = mp + (if (mp < 10) 3 else -9)
            val year = (y + (if (m <= 2) 1 else 0)).toInt()
            return LocalDay(year = year, month = m.toInt(), dayOfMonth = d.toInt())
        }

        fun fromUtcMillis(millis: Long): LocalDay {
            val cal = Calendar.getInstance(TimeZone.getTimeZone("UTC")).apply {
                time = Date(millis)
            }
            return LocalDay(
                year = cal.get(Calendar.YEAR),
                month = cal.get(Calendar.MONTH) + 1,
                dayOfMonth = cal.get(Calendar.DAY_OF_MONTH),
            )
        }

        private fun isLeapYear(year: Int): Boolean =
            (year % 4 == 0 && year % 100 != 0) || (year % 400 == 0)
    }
}

@Immutable
data class CategoryIconOption(
    val key: String,
    val label: String,
    val icon: ImageVector,
)

object CategoryIconCatalog {
    val options: List<CategoryIconOption> = listOf(
        CategoryIconOption("dining", "Dining", Icons.Rounded.Restaurant),
        CategoryIconOption("groceries", "Groceries", Icons.Rounded.LocalGroceryStore),
        CategoryIconOption("transit", "Transit", Icons.Rounded.DirectionsTransit),
        CategoryIconOption("shopping", "Shopping", Icons.Rounded.ShoppingBag),
        CategoryIconOption("coffee", "Coffee", Icons.Rounded.Coffee),
        CategoryIconOption("wellness", "Wellness", Icons.Rounded.SelfImprovement),
        CategoryIconOption("home", "Home", Icons.Rounded.Home),
        CategoryIconOption("bills", "Bills", Icons.Rounded.WaterDrop),
        CategoryIconOption("entertainment", "Fun", Icons.Rounded.Movie),
        CategoryIconOption("health", "Health", Icons.Rounded.MedicalServices),
        CategoryIconOption("travel", "Travel", Icons.Rounded.Flight),
        CategoryIconOption("pets", "Pets", Icons.Rounded.Pets),
        CategoryIconOption("education", "Learning", Icons.Rounded.School),
        CategoryIconOption("gifts", "Gifts", Icons.Rounded.CardGiftcard),
        CategoryIconOption("tech", "Tech", Icons.Rounded.Devices),
        CategoryIconOption("wardrobe", "Style", Icons.Rounded.Checkroom),
        CategoryIconOption("savings", "Savings", Icons.Rounded.Savings),
    )

    private val byKey: Map<String, ImageVector> = options.associate { it.key to it.icon }

    fun iconFor(key: String): ImageVector = byKey[key] ?: Icons.Rounded.ShoppingBag
}

object CategoryColorPalette {
    val swatches: List<Long> = listOf(
        0xFFE07A5FL, // Terracotta Coral
        0xFF3D8B68L, // Forest Sage
        0xFF4A78B8L, // Pixel Cobalt
        0xFF8E63B5L, // Soft Plum
        0xFFB57B4CL, // Roasted Coffee
        0xFF2D9596L, // Deep Teal
        0xFFD97706L, // Warm Amber
        0xFFC94F6DL, // Berry Rose
        0xFF5C7AEAL, // Periwinkle
        0xFF4F772DL, // Olive Moss
    )
}

// --- Firestore Serialized Models ---

@Keep
@IgnoreExtraProperties
data class UserProfileDoc(
    @DocumentId val id: String = "",
    val userId: String = "",
    val seeded: Boolean = false,
    val hasSampleData: Boolean = false,
    @ServerTimestamp val createdAt: Timestamp? = null,
    @ServerTimestamp val updatedAt: Timestamp? = null,
)

@Keep
@IgnoreExtraProperties
data class BudgetCategoryDoc(
    @DocumentId val id: String = "",
    val userId: String = "",
    val name: String = "",
    val iconKey: String = "shopping",
    val colorHex: Long = 0xFF3D8B68L,
    val monthlyLimit: Double = 100.0,
    val sortOrder: Long = 0L,
    val sample: Boolean = false,
    @ServerTimestamp val createdAt: Timestamp? = null,
    @ServerTimestamp val updatedAt: Timestamp? = null,
) {
    fun toDomain(): BudgetCategory = BudgetCategory(
        id = id,
        name = name,
        iconKey = iconKey,
        icon = CategoryIconCatalog.iconFor(iconKey),
        colorHex = colorHex,
        accentColor = Color(colorHex),
        monthlyLimit = monthlyLimit,
        sortOrder = sortOrder,
        sample = sample,
    )
}

@Keep
@IgnoreExtraProperties
data class TransactionDoc(
    @DocumentId val id: String = "",
    val userId: String = "",
    val categoryId: String = "",
    val amount: Double = 0.0,
    val merchantOrNote: String = "",
    val dateEpochDay: Long = 0L,
    val sample: Boolean = false,
    @ServerTimestamp val createdAt: Timestamp? = null,
    @ServerTimestamp val updatedAt: Timestamp? = null,
) {
    fun toDomain(): Transaction = Transaction(
        id = id,
        categoryId = categoryId,
        amount = amount,
        merchantOrNote = merchantOrNote,
        date = LocalDay.fromEpochDay(dateEpochDay),
        sample = sample,
        createdAtSeconds = createdAt?.seconds ?: 0L,
    )
}

// --- UI Domain Models ---

@Immutable
data class BudgetCategory(
    val id: String,
    val name: String,
    val iconKey: String,
    val icon: ImageVector,
    val colorHex: Long,
    val accentColor: Color,
    val monthlyLimit: Double,
    val sortOrder: Long = 0L,
    val sample: Boolean = false,
)

@Immutable
data class Transaction(
    val id: String,
    val categoryId: String,
    val amount: Double,
    val merchantOrNote: String,
    val date: LocalDay,
    val sample: Boolean = false,
    val createdAtSeconds: Long = 0L,
)

sealed interface WorkspaceLoadState {
    data object Loading : WorkspaceLoadState
    data object Ready : WorkspaceLoadState
    data class Error(val message: String) : WorkspaceLoadState
}

@Immutable
data class CadenceUiState(
    val loadState: WorkspaceLoadState = WorkspaceLoadState.Loading,
    val categories: List<BudgetCategory> = emptyList(),
    val transactions: List<Transaction> = emptyList(),
    val hasSampleData: Boolean = false,
    val isMutating: Boolean = false,
    val searchQuery: String = "",
    val selectedFilterCategoryId: String? = null,
    val expandedCategoryId: String? = null,
    val editingTransaction: Transaction? = null,
    val isEntrySheetOpen: Boolean = false,
    val editingCategory: BudgetCategory? = null,
    val isCategorySheetOpen: Boolean = false,
    val isAccountSheetOpen: Boolean = false,
    val today: LocalDay = LocalDay.today(),
) {
    val currentMonthTransactions: List<Transaction>
        get() = transactions.filter { it.date.isSameMonth(today) }

    val totalMonthlyBudget: Double
        get() = categories.sumOf { it.monthlyLimit }

    val totalSpentThisMonth: Double
        get() = currentMonthTransactions.sumOf { it.amount }

    val remainingBudget: Double
        get() = totalMonthlyBudget - totalSpentThisMonth

    val overallSpendRatio: Float
        get() = if (totalMonthlyBudget > 0.0) (totalSpentThisMonth / totalMonthlyBudget).toFloat() else 0f

    val monthElapsedRatio: Float
        get() = (today.dayOfMonth.toFloat() / today.lengthOfMonth().toFloat()).coerceIn(0.03f, 1f)

    val daysRemaining: Int
        get() = (today.lengthOfMonth() - today.dayOfMonth + 1).coerceAtLeast(1)

    val safeDailySpend: Double
        get() = (remainingBudget / daysRemaining).coerceAtLeast(0.0)

    fun spentForCategoryThisMonth(categoryId: String): Double =
        currentMonthTransactions.filter { it.categoryId == categoryId }.sumOf { it.amount }

    fun transactionsForCategory(categoryId: String): List<Transaction> =
        transactions
            .filter { it.categoryId == categoryId }
            .sortedWith(compareByDescending<Transaction> { it.date }.thenByDescending { it.createdAtSeconds })

    fun filteredTransactions(): List<Transaction> = transactions
        .filter { tx ->
            (selectedFilterCategoryId == null || tx.categoryId == selectedFilterCategoryId) &&
                (searchQuery.isBlank() ||
                    tx.merchantOrNote.contains(searchQuery, ignoreCase = true) ||
                    categories.find { it.id == tx.categoryId }?.name?.contains(searchQuery, ignoreCase = true) == true)
        }
        .sortedWith(compareByDescending<Transaction> { it.date }.thenByDescending { it.createdAtSeconds })
}
