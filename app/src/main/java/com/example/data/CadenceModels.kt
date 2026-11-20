package com.example.data

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.ReceiptLong
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
import java.text.DateFormatSymbols
import java.util.Calendar
import java.util.Locale

enum class CadenceTab(val label: String, val icon: ImageVector) {
    Home("Home", Icons.Rounded.SpaceDashboard),
    Budgets("Budgets", Icons.Rounded.DonutLarge),
    Activity("Activity", Icons.AutoMirrored.Rounded.ReceiptLong),
    Insights("Insights", Icons.Rounded.AutoGraph),
}

enum class RecurringKind(val storageKey: String, val label: String) {
    RECURRING("recurring", "Recurring"),
    SUBSCRIPTION("subscription", "Subscription");

    companion object {
        fun fromKey(key: String?): RecurringKind =
            entries.find { it.storageKey == key?.lowercase(Locale.ROOT) } ?: RECURRING
    }
}

enum class BillingCadence(
    val storageKey: String,
    val label: String,
    val shortLabel: String,
) {
    WEEKLY("weekly", "Weekly", "/wk"),
    BIWEEKLY("biweekly", "Every 2 wks", "/2wk"),
    MONTHLY("monthly", "Monthly", "/mo"),
    YEARLY("yearly", "Yearly", "/yr");

    fun monthlyEquivalent(amount: Double): Double = when (this) {
        WEEKLY -> amount * (52.0 / 12.0)
        BIWEEKLY -> amount * (26.0 / 12.0)
        MONTHLY -> amount
        YEARLY -> amount / 12.0
    }

    fun nextOccurrenceAfter(date: LocalDay): LocalDay = when (this) {
        WEEKLY -> date.plusDays(7)
        BIWEEKLY -> date.plusDays(14)
        MONTHLY -> date.plusMonthsClamped(1, date.dayOfMonth)
        YEARLY -> date.plusMonthsClamped(12, date.dayOfMonth)
    }

    companion object {
        fun fromKey(key: String?): BillingCadence =
            entries.find { it.storageKey == key?.lowercase(Locale.ROOT) } ?: MONTHLY
    }
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

    fun plusDays(days: Long): LocalDay = fromEpochDay(toEpochDay() + days)

    fun lengthOfMonth(): Int = lengthOfMonthFor(year, month)

    fun isSameMonth(other: LocalDay): Boolean = year == other.year && month == other.month

    fun monthFullName(): String = cachedSymbols().months.getOrNull(month - 1).orEmpty()

    fun monthShortName(): String = cachedSymbols().shortMonths.getOrNull(month - 1).orEmpty()

    fun monthYearShort(): String = "${monthShortName()} $year"

    fun monthYearFullName(): String = "${monthFullName()} $year"

    fun firstDayOfNextMonth(): LocalDay = if (month == 12) {
        LocalDay(year = year + 1, month = 1, dayOfMonth = 1)
    } else {
        LocalDay(year = year, month = month + 1, dayOfMonth = 1)
    }

    fun plusMonthsClamped(monthsOffset: Int, desiredDay: Int): LocalDay {
        val totalMonths = (year * 12 + (month - 1)) + monthsOffset
        val targetYear = Math.floorDiv(totalMonths, 12)
        val targetMonth = Math.floorMod(totalMonths, 12) + 1
        val maxDay = lengthOfMonthFor(targetYear, targetMonth)
        return LocalDay(
            year = targetYear,
            month = targetMonth,
            dayOfMonth = desiredDay.coerceIn(1, maxDay),
        )
    }

    /** 0 = Mon .. 6 = Sun */
    fun dayOfWeekIndexMonFirst(): Int {
        // 1970-01-01 was Thursday (index 3 in Mon=0..Sun=6)
        return Math.floorMod(toEpochDay() + 3L, 7L).toInt()
    }

    private fun calendarDayOfWeek(): Int {
        val monFirst = dayOfWeekIndexMonFirst()
        return if (monFirst == 6) Calendar.SUNDAY else monFirst + 2
    }

    fun dayOfWeekShort(): String = cachedSymbols().shortWeekdays.getOrNull(calendarDayOfWeek()).orEmpty()

    private fun dayOfWeekFull(): String = cachedSymbols().weekdays.getOrNull(calendarDayOfWeek()).orEmpty()

    fun formatShort(): String = "${monthShortName()} $dayOfMonth"

    fun formatDayOfWeekShort(): String = "${dayOfWeekShort()}, ${monthShortName()} $dayOfMonth"

    fun formatFullGroupHeader(): String = "${dayOfWeekFull()}, ${monthShortName()} $dayOfMonth"

    fun formatIsoDate(): String = String.format(Locale.US, "%04d-%02d-%02d", year, month, dayOfMonth)

    fun toUtcMillis(): Long = toEpochDay() * MILLIS_PER_DAY + HALF_DAY_MILLIS

    override fun compareTo(other: LocalDay): Int = toEpochDay().compareTo(other.toEpochDay())

    companion object {
        private const val MILLIS_PER_DAY = 86_400_000L
        private const val HALF_DAY_MILLIS = 43_200_000L

        private class CachedDateSymbols(
            val locale: Locale,
            val months: Array<String>,
            val shortMonths: Array<String>,
            val weekdays: Array<String>,
            val shortWeekdays: Array<String>,
        )

        @Volatile
        private var dateSymbolsCache: CachedDateSymbols? = null

        private fun cachedSymbols(): CachedDateSymbols {
            val currentLocale = Locale.getDefault()
            val existing = dateSymbolsCache
            if (existing != null && existing.locale == currentLocale) {
                return existing
            }
            val raw = DateFormatSymbols.getInstance(currentLocale)
            val created = CachedDateSymbols(
                locale = currentLocale,
                months = raw.months,
                shortMonths = raw.shortMonths,
                weekdays = raw.weekdays,
                shortWeekdays = raw.shortWeekdays,
            )
            dateSymbolsCache = created
            return created
        }

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

        fun fromUtcMillis(millis: Long): LocalDay = fromEpochDay(Math.floorDiv(millis, MILLIS_PER_DAY))

        fun lengthOfMonthFor(year: Int, month: Int): Int = when (month) {
            1, 3, 5, 7, 8, 10, 12 -> 31
            4, 6, 9, 11 -> 30
            2 -> if (isLeapYear(year)) 29 else 28
            else -> 30
        }

        fun ordinalDay(day: Int): String {
            val mod100 = day % 100
            val suffix = if (mod100 in 11..13) {
                "th"
            } else {
                when (day % 10) {
                    1 -> "st"
                    2 -> "nd"
                    3 -> "rd"
                    else -> "th"
                }
            }
            return "$day$suffix"
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

@Immutable
data class RecurringItem(
    val id: String,
    val categoryId: String,
    val name: String,
    val amount: Double,
    val kind: RecurringKind,
    val cadence: BillingCadence,
    val nextDueDate: LocalDay,
    val active: Boolean = true,
    val sample: Boolean = false,
) {
    val monthlyEquivalent: Double
        get() = cadence.monthlyEquivalent(amount)
}

@Immutable
data class MonthlyTrendPoint(
    val year: Int,
    val month: Int,
    val shortLabel: String,
    val totalSpent: Double,
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
    val recurringItems: List<RecurringItem> = emptyList(),
    val hasSampleData: Boolean = false,
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
    val today: LocalDay = LocalDay.today(),
    val budgetResetDay: Int = 1,
    val currency: CurrencyOption = WorldCurrencyCatalog.defaultCurrency(),
    val includeFixedBillsInSafeDaily: Boolean = false,
    val warningThresholdRatio: Float = 0.80f,
    val compactCards: Boolean = false,
) {
    val cycleStart: LocalDay by lazy(LazyThreadSafetyMode.NONE) {
        val resetThisMonth = budgetResetDay.coerceIn(1, today.lengthOfMonth())
        if (today.dayOfMonth >= resetThisMonth) {
            LocalDay(today.year, today.month, resetThisMonth)
        } else {
            today.plusMonthsClamped(-1, budgetResetDay)
        }
    }

    val nextResetDate: LocalDay by lazy(LazyThreadSafetyMode.NONE) {
        val resetThisMonth = budgetResetDay.coerceIn(1, today.lengthOfMonth())
        if (today.dayOfMonth >= resetThisMonth) {
            today.plusMonthsClamped(1, budgetResetDay)
        } else {
            LocalDay(today.year, today.month, resetThisMonth)
        }
    }

    val cycleEnd: LocalDay by lazy(LazyThreadSafetyMode.NONE) {
        nextResetDate.minusDays(1)
    }

    val cycleTotalDays: Int
        get() = (nextResetDate.toEpochDay() - cycleStart.toEpochDay()).toInt().coerceAtLeast(1)

    val cycleDayElapsed: Int
        get() = ((today.toEpochDay() - cycleStart.toEpochDay()).toInt() + 1).coerceIn(1, cycleTotalDays)

    val budgetPeriodTitle: String by lazy(LazyThreadSafetyMode.NONE) {
        if (budgetResetDay == 1) {
            "${today.monthFullName()} Budget"
        } else {
            "${cycleStart.formatShort()} – ${cycleEnd.formatShort()} Budget"
        }
    }

    val currentMonthTransactions: List<Transaction> by lazy(LazyThreadSafetyMode.NONE) {
        val start = cycleStart
        val next = nextResetDate
        transactions.filter { it.date >= start && it.date < next }
    }

    private val spentByCategoryThisMonth: Map<String, Double> by lazy(LazyThreadSafetyMode.NONE) {
        val totals = HashMap<String, Double>(categories.size + 4)
        for (tx in currentMonthTransactions) {
            totals[tx.categoryId] = (totals[tx.categoryId] ?: 0.0) + tx.amount
        }
        totals
    }

    private val categoryById: Map<String, BudgetCategory> by lazy(LazyThreadSafetyMode.NONE) {
        categories.associateBy { it.id }
    }

    val totalMonthlyBudget: Double by lazy(LazyThreadSafetyMode.NONE) {
        categories.sumOf { it.monthlyLimit }
    }

    val totalSpentThisMonth: Double by lazy(LazyThreadSafetyMode.NONE) {
        currentMonthTransactions.sumOf { it.amount }
    }

    val remainingBudget: Double
        get() = totalMonthlyBudget - totalSpentThisMonth

    val overallSpendRatio: Float
        get() = if (totalMonthlyBudget > 0.0) (totalSpentThisMonth / totalMonthlyBudget).toFloat() else 0f

    val monthElapsedRatio: Float
        get() = (cycleDayElapsed.toFloat() / cycleTotalDays.toFloat()).coerceIn(0.03f, 1f)

    val daysRemaining: Int
        get() = (nextResetDate.toEpochDay() - today.toEpochDay()).toInt().coerceAtLeast(1)

    val activeRecurringItems: List<RecurringItem> by lazy(LazyThreadSafetyMode.NONE) {
        recurringItems.filter { it.active }
    }

    val recurringBills: List<RecurringItem> by lazy(LazyThreadSafetyMode.NONE) {
        recurringItems.filter { it.kind == RecurringKind.RECURRING }
            .sortedWith(compareByDescending<RecurringItem> { it.active }.thenBy { it.nextDueDate })
    }

    val subscriptions: List<RecurringItem> by lazy(LazyThreadSafetyMode.NONE) {
        recurringItems.filter { it.kind == RecurringKind.SUBSCRIPTION }
            .sortedWith(compareByDescending<RecurringItem> { it.active }.thenBy { it.nextDueDate })
    }

    val monthlyRecurringBillsTotal: Double by lazy(LazyThreadSafetyMode.NONE) {
        activeRecurringItems.filter { it.kind == RecurringKind.RECURRING }.sumOf { it.monthlyEquivalent }
    }

    val monthlySubscriptionsTotal: Double by lazy(LazyThreadSafetyMode.NONE) {
        activeRecurringItems.filter { it.kind == RecurringKind.SUBSCRIPTION }.sumOf { it.monthlyEquivalent }
    }

    val totalMonthlyFixedCommitments: Double
        get() = monthlyRecurringBillsTotal + monthlySubscriptionsTotal

    /** Active recurring/subscription charges still due before the current cycle ends. */
    val upcomingDueThisCycleTotal: Double by lazy(LazyThreadSafetyMode.NONE) {
        activeRecurringItems
            .filter { it.nextDueDate >= today && it.nextDueDate < nextResetDate }
            .sumOf { it.amount }
    }

    val nextUpcomingBill: RecurringItem? by lazy(LazyThreadSafetyMode.NONE) {
        activeRecurringItems.minByOrNull { it.nextDueDate }
    }

    val safeDailySpend: Double
        get() {
            val reserved = if (includeFixedBillsInSafeDaily) upcomingDueThisCycleTotal else 0.0
            return ((remainingBudget - reserved) / daysRemaining).coerceAtLeast(0.0)
        }

    fun spentForCategoryThisMonth(categoryId: String): Double =
        spentByCategoryThisMonth[categoryId] ?: 0.0

    fun transactionsForCategory(categoryId: String): List<Transaction> =
        transactions
            .filter { it.categoryId == categoryId }
            .sortedWith(compareByDescending<Transaction> { it.date }.thenByDescending { it.createdAtSeconds })

    fun filteredTransactions(): List<Transaction> = transactions
        .filter { tx ->
            (selectedFilterCategoryId == null || tx.categoryId == selectedFilterCategoryId) &&
                (searchQuery.isBlank() ||
                    tx.merchantOrNote.contains(searchQuery, ignoreCase = true) ||
                    categoryById[tx.categoryId]?.name?.contains(searchQuery, ignoreCase = true) == true)
        }
        .sortedWith(compareByDescending<Transaction> { it.date }.thenByDescending { it.createdAtSeconds })

    private val cachedSixMonthsTrend: List<MonthlyTrendPoint> by lazy(LazyThreadSafetyMode.NONE) {
        val currentStart = cycleStart
        val bucketStarts = Array(6) { idx -> currentStart.plusMonthsClamped(idx - 5, budgetResetDay) }
        val bucketEnds = Array(6) { idx -> currentStart.plusMonthsClamped(idx - 4, budgetResetDay) }
        val totals = DoubleArray(6)
        val firstStart = bucketStarts[0]
        val lastEnd = bucketEnds[5]
        for (tx in transactions) {
            if (tx.date < firstStart || tx.date >= lastEnd) continue
            for (i in 0 until 6) {
                if (tx.date >= bucketStarts[i] && tx.date < bucketEnds[i]) {
                    totals[i] += tx.amount
                    break
                }
            }
        }
        List(6) { idx ->
            val start = bucketStarts[idx]
            MonthlyTrendPoint(
                year = start.year,
                month = start.month,
                shortLabel = start.monthShortName(),
                totalSpent = totals[idx],
            )
        }
    }

    /**
     * Computes monthly spending totals for the last 6 budget cycles ending with the active cycle,
     * aligned with the user's configured [budgetResetDay].
     */
    fun lastSixMonthsTrend(): List<MonthlyTrendPoint> = cachedSixMonthsTrend
}
