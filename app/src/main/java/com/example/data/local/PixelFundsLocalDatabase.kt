package com.example.data.local

import android.content.Context
import androidx.compose.ui.graphics.Color
import androidx.room.Dao
import androidx.room.Database
import androidx.room.Entity
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.PrimaryKey
import androidx.room.Query
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.room.Transaction as RoomTransaction
import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase
import com.example.data.BillingCadence
import com.example.data.BudgetCategory
import com.example.data.BudgetRepository
import com.example.data.CategoryIconCatalog
import com.example.data.LocalDay
import com.example.data.RecurringItem
import com.example.data.RecurringKind
import com.example.data.StarterBudgetData
import com.example.data.Transaction
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import java.util.UUID

@Entity(tableName = "local_workspace")
data class LocalWorkspaceEntity(
    @PrimaryKey val id: Int = 1,
    val seeded: Boolean = false,
    val hasSampleData: Boolean = false,
)

@Entity(tableName = "local_categories")
data class LocalCategoryEntity(
    @PrimaryKey val id: String,
    val name: String,
    val iconKey: String,
    val colorHex: Long,
    val monthlyLimit: Double,
    val sortOrder: Long,
    val sample: Boolean,
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

@Entity(tableName = "local_transactions")
data class LocalTransactionEntity(
    @PrimaryKey val id: String,
    val categoryId: String,
    val amount: Double,
    val merchantOrNote: String,
    val dateEpochDay: Long,
    val sample: Boolean,
    val createdAtSeconds: Long,
) {
    fun toDomain(): Transaction = Transaction(
        id = id,
        categoryId = categoryId,
        amount = amount,
        merchantOrNote = merchantOrNote,
        date = LocalDay.fromEpochDay(dateEpochDay),
        sample = sample,
        createdAtSeconds = createdAtSeconds,
    )
}

@Entity(tableName = "local_recurring_items")
data class LocalRecurringEntity(
    @PrimaryKey val id: String,
    val categoryId: String,
    val name: String,
    val amount: Double,
    val kind: String,
    val cadence: String,
    val nextDueEpochDay: Long,
    val active: Boolean,
    val sample: Boolean,
) {
    fun toDomain(): RecurringItem = RecurringItem(
        id = id,
        categoryId = categoryId,
        name = name,
        amount = amount,
        kind = RecurringKind.fromKey(kind),
        cadence = BillingCadence.fromKey(cadence),
        nextDueDate = LocalDay.fromEpochDay(nextDueEpochDay),
        active = active,
        sample = sample,
    )
}

@Dao
interface PixelFundsDao {
    @Query("SELECT * FROM local_workspace WHERE id = 1")
    fun observeWorkspace(): Flow<LocalWorkspaceEntity?>

    @Query("SELECT * FROM local_workspace WHERE id = 1")
    suspend fun getWorkspace(): LocalWorkspaceEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsertWorkspace(entity: LocalWorkspaceEntity)

    @Query("SELECT * FROM local_categories ORDER BY sortOrder ASC, name ASC")
    fun observeCategories(): Flow<List<LocalCategoryEntity>>

    @Query("SELECT * FROM local_categories ORDER BY sortOrder ASC, name ASC")
    suspend fun getCategories(): List<LocalCategoryEntity>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertCategory(category: LocalCategoryEntity)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertCategories(categories: List<LocalCategoryEntity>)

    @Query(
        "UPDATE local_categories SET name = :name, iconKey = :iconKey, colorHex = :colorHex, " +
            "monthlyLimit = :monthlyLimit, sample = 0 WHERE id = :id"
    )
    suspend fun updateCategory(
        id: String,
        name: String,
        iconKey: String,
        colorHex: Long,
        monthlyLimit: Double,
    )

    @Query("UPDATE local_categories SET monthlyLimit = :newLimit WHERE id = :id")
    suspend fun updateCategoryLimit(id: String, newLimit: Double)

    @Query("DELETE FROM local_categories WHERE id = :id")
    suspend fun deleteCategory(id: String)

    @Query("SELECT * FROM local_transactions ORDER BY dateEpochDay DESC, createdAtSeconds DESC")
    fun observeTransactions(): Flow<List<LocalTransactionEntity>>

    @Query("SELECT * FROM local_transactions ORDER BY dateEpochDay DESC, createdAtSeconds DESC")
    suspend fun getTransactions(): List<LocalTransactionEntity>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertTransaction(transaction: LocalTransactionEntity)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertTransactions(transactions: List<LocalTransactionEntity>)

    @Query(
        "UPDATE local_transactions SET categoryId = :categoryId, amount = :amount, " +
            "merchantOrNote = :merchantOrNote, dateEpochDay = :dateEpochDay, sample = 0 WHERE id = :id"
    )
    suspend fun updateTransaction(
        id: String,
        categoryId: String,
        amount: Double,
        merchantOrNote: String,
        dateEpochDay: Long,
    )

    @Query("UPDATE local_transactions SET categoryId = :fallbackCategoryId WHERE categoryId = :oldCategoryId")
    suspend fun reassignTransactions(oldCategoryId: String, fallbackCategoryId: String)

    @Query("DELETE FROM local_transactions WHERE id = :id")
    suspend fun deleteTransaction(id: String)

    @Query("DELETE FROM local_transactions WHERE sample = 1")
    suspend fun deleteSampleTransactions()

    @Query("DELETE FROM local_transactions")
    suspend fun deleteAllTransactions()

    // Recurring & Subscriptions
    @Query("SELECT * FROM local_recurring_items ORDER BY active DESC, nextDueEpochDay ASC, name ASC")
    fun observeRecurringItems(): Flow<List<LocalRecurringEntity>>

    @Query("SELECT * FROM local_recurring_items ORDER BY active DESC, nextDueEpochDay ASC, name ASC")
    suspend fun getRecurringItems(): List<LocalRecurringEntity>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertRecurringItem(item: LocalRecurringEntity)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertRecurringItems(items: List<LocalRecurringEntity>)

    @Query(
        "UPDATE local_recurring_items SET categoryId = :categoryId, name = :name, amount = :amount, " +
            "kind = :kind, cadence = :cadence, nextDueEpochDay = :nextDueEpochDay, active = :active, " +
            "sample = 0 WHERE id = :id"
    )
    suspend fun updateRecurringItem(
        id: String,
        categoryId: String,
        name: String,
        amount: Double,
        kind: String,
        cadence: String,
        nextDueEpochDay: Long,
        active: Boolean,
    )

    @Query("UPDATE local_recurring_items SET active = :active WHERE id = :id")
    suspend fun setRecurringActive(id: String, active: Boolean)

    @Query("UPDATE local_recurring_items SET nextDueEpochDay = :nextDueEpochDay WHERE id = :id")
    suspend fun advanceRecurringDueDate(id: String, nextDueEpochDay: Long)

    @Query("UPDATE local_recurring_items SET categoryId = :fallbackCategoryId WHERE categoryId = :oldCategoryId")
    suspend fun reassignRecurringItems(oldCategoryId: String, fallbackCategoryId: String)

    @Query("DELETE FROM local_recurring_items WHERE id = :id")
    suspend fun deleteRecurringItem(id: String)

    @Query("DELETE FROM local_recurring_items WHERE sample = 1")
    suspend fun deleteSampleRecurringItems()

    @RoomTransaction
    suspend fun deleteCategoryAndReassign(categoryId: String, fallbackCategoryId: String) {
        reassignTransactions(categoryId, fallbackCategoryId)
        reassignRecurringItems(categoryId, fallbackCategoryId)
        deleteCategory(categoryId)
    }

    @RoomTransaction
    suspend fun logRecurringAndAdvance(
        transaction: LocalTransactionEntity,
        recurringId: String,
        nextDueEpochDay: Long,
    ) {
        insertTransaction(transaction)
        advanceRecurringDueDate(recurringId, nextDueEpochDay)
    }
}

@Database(
    entities = [
        LocalWorkspaceEntity::class,
        LocalCategoryEntity::class,
        LocalTransactionEntity::class,
        LocalRecurringEntity::class,
    ],
    version = 2,
    exportSchema = false,
)
abstract class PixelFundsDatabase : RoomDatabase() {
    abstract fun dao(): PixelFundsDao

    companion object {
        @Volatile
        private var INSTANCE: PixelFundsDatabase? = null

        private val MIGRATION_1_2 = object : Migration(1, 2) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL(
                    """
                    CREATE TABLE IF NOT EXISTS `local_recurring_items` (
                        `id` TEXT NOT NULL,
                        `categoryId` TEXT NOT NULL,
                        `name` TEXT NOT NULL,
                        `amount` REAL NOT NULL,
                        `kind` TEXT NOT NULL,
                        `cadence` TEXT NOT NULL,
                        `nextDueEpochDay` INTEGER NOT NULL,
                        `active` INTEGER NOT NULL,
                        `sample` INTEGER NOT NULL,
                        PRIMARY KEY(`id`)
                    )
                    """.trimIndent()
                )
            }
        }

        fun getInstance(context: Context): PixelFundsDatabase =
            INSTANCE ?: synchronized(this) {
                INSTANCE ?: Room.databaseBuilder(
                    context.applicationContext,
                    PixelFundsDatabase::class.java,
                    "pixel_funds_local.db",
                )
                    .addMigrations(MIGRATION_1_2)
                    .build()
                    .also { INSTANCE = it }
            }
    }
}

data class LocalMigrationSnapshot(
    val categories: List<BudgetCategory>,
    val transactions: List<Transaction>,
    val recurringItems: List<RecurringItem>,
    val hasSampleData: Boolean,
)

class LocalBudgetRepository(
    private val dao: PixelFundsDao,
) : BudgetRepository {

    constructor(context: Context) : this(PixelFundsDatabase.getInstance(context).dao())

    suspend fun readSnapshotForCloudMigration(): LocalMigrationSnapshot? {
        val workspace = dao.getWorkspace() ?: return null
        val categories = dao.getCategories().map { it.toDomain() }
        if (categories.isEmpty()) return null
        val transactions = dao.getTransactions().map { it.toDomain() }
        val recurring = dao.getRecurringItems().map { it.toDomain() }
        return LocalMigrationSnapshot(
            categories = categories,
            transactions = transactions,
            recurringItems = recurring,
            hasSampleData = workspace.hasSampleData || transactions.any { it.sample } || recurring.any { it.sample },
        )
    }

    override fun observeHasSampleData(): Flow<Boolean> =
        dao.observeWorkspace().map { it?.hasSampleData == true }

    override fun observeCategories(): Flow<List<BudgetCategory>> =
        dao.observeCategories().map { list -> list.map { it.toDomain() } }

    override fun observeTransactions(): Flow<List<Transaction>> =
        dao.observeTransactions().map { list -> list.map { it.toDomain() } }

    override fun observeRecurringItems(): Flow<List<RecurringItem>> =
        dao.observeRecurringItems().map { list -> list.map { it.toDomain() } }

    override suspend fun ensureInitialSeed(): Result<Unit> = runSafe {
        val existing = dao.getWorkspace()
        val today = LocalDay.today()

        if (existing != null && existing.seeded) {
            // Upgrade convenience: if workspace still has sample data enabled and zero recurring rows yet,
            // seed the starter recurring & subscription showcase once.
            if (existing.hasSampleData && dao.getRecurringItems().isEmpty()) {
                val validCatIds = dao.getCategories().map { it.id }.toSet()
                val fallbackCat = validCatIds.firstOrNull()
                if (fallbackCat != null) {
                    val starterRecurring = StarterBudgetData.defaultRecurringItems(today).map { seed ->
                        LocalRecurringEntity(
                            id = seed.id,
                            categoryId = if (seed.categoryId in validCatIds) seed.categoryId else fallbackCat,
                            name = seed.name,
                            amount = seed.amount,
                            kind = seed.kind.storageKey,
                            cadence = seed.cadence.storageKey,
                            nextDueEpochDay = seed.nextDueDate.toEpochDay(),
                            active = true,
                            sample = true,
                        )
                    }
                    dao.insertRecurringItems(starterRecurring)
                }
            }
            return@runSafe
        }

        val nowSec = System.currentTimeMillis() / 1000L
        val categories = StarterBudgetData.defaultCategories().mapIndexed { index, seed ->
            LocalCategoryEntity(
                id = seed.id,
                name = seed.name,
                iconKey = seed.iconKey,
                colorHex = seed.colorHex,
                monthlyLimit = seed.monthlyLimit,
                sortOrder = index.toLong(),
                sample = true,
            )
        }
        val transactions = StarterBudgetData.defaultTransactions(today).mapIndexed { index, seed ->
            LocalTransactionEntity(
                id = "tx_${UUID.randomUUID().toString().replace("-", "").take(12)}",
                categoryId = seed.categoryId,
                amount = seed.amount,
                merchantOrNote = seed.merchantOrNote,
                dateEpochDay = seed.date.toEpochDay(),
                sample = true,
                createdAtSeconds = nowSec - index,
            )
        }
        val recurring = StarterBudgetData.defaultRecurringItems(today).map { seed ->
            LocalRecurringEntity(
                id = seed.id,
                categoryId = seed.categoryId,
                name = seed.name,
                amount = seed.amount,
                kind = seed.kind.storageKey,
                cadence = seed.cadence.storageKey,
                nextDueEpochDay = seed.nextDueDate.toEpochDay(),
                active = true,
                sample = true,
            )
        }

        dao.insertCategories(categories)
        dao.insertTransactions(transactions)
        dao.insertRecurringItems(recurring)
        dao.upsertWorkspace(LocalWorkspaceEntity(id = 1, seeded = true, hasSampleData = true))
    }

    override suspend fun clearSampleData(): Result<Unit> = runSafe {
        dao.deleteSampleTransactions()
        dao.deleteSampleRecurringItems()
        dao.upsertWorkspace(LocalWorkspaceEntity(id = 1, seeded = true, hasSampleData = false))
    }

    override suspend fun restoreSampleData(): Result<Unit> = runSafe {
        val existingIds = dao.getCategories().map { it.id }.toSet()
        val missingCategories = StarterBudgetData.defaultCategories().mapIndexedNotNull { index, seed ->
            if (seed.id in existingIds) null
            else LocalCategoryEntity(
                id = seed.id,
                name = seed.name,
                iconKey = seed.iconKey,
                colorHex = seed.colorHex,
                monthlyLimit = seed.monthlyLimit,
                sortOrder = index.toLong(),
                sample = true,
            )
        }
        val today = LocalDay.today()
        val nowSec = System.currentTimeMillis() / 1000L
        val sampleTxs = StarterBudgetData.defaultTransactions(today).mapIndexed { index, seed ->
            LocalTransactionEntity(
                id = "tx_${UUID.randomUUID().toString().replace("-", "").take(12)}",
                categoryId = seed.categoryId,
                amount = seed.amount,
                merchantOrNote = seed.merchantOrNote,
                dateEpochDay = seed.date.toEpochDay(),
                sample = true,
                createdAtSeconds = nowSec - index,
            )
        }
        val sampleRecurring = StarterBudgetData.defaultRecurringItems(today).map { seed ->
            LocalRecurringEntity(
                id = seed.id,
                categoryId = seed.categoryId,
                name = seed.name,
                amount = seed.amount,
                kind = seed.kind.storageKey,
                cadence = seed.cadence.storageKey,
                nextDueEpochDay = seed.nextDueDate.toEpochDay(),
                active = true,
                sample = true,
            )
        }

        if (missingCategories.isNotEmpty()) dao.insertCategories(missingCategories)
        dao.insertTransactions(sampleTxs)
        dao.insertRecurringItems(sampleRecurring)
        dao.upsertWorkspace(LocalWorkspaceEntity(id = 1, seeded = true, hasSampleData = true))
    }

    override suspend fun clearAllTransactions(): Result<Unit> = runSafe {
        dao.deleteAllTransactions()
        val hasSampleRecurring = dao.getRecurringItems().any { it.sample }
        dao.upsertWorkspace(LocalWorkspaceEntity(id = 1, seeded = true, hasSampleData = hasSampleRecurring))
    }

    override suspend fun addTransaction(
        categoryId: String,
        amount: Double,
        merchantOrNote: String,
        date: LocalDay,
        sample: Boolean,
    ): Result<Unit> = runSafe {
        val cleanNote = merchantOrNote.trim().take(120)
        require(categoryId.isNotBlank()) { "Category must be selected." }
        require(amount > 0.0 && amount <= 1_000_000.0) { "Amount must be positive." }
        require(cleanNote.isNotEmpty()) { "Merchant or note cannot be empty." }

        dao.insertTransaction(
            LocalTransactionEntity(
                id = "tx_${UUID.randomUUID().toString().replace("-", "").take(12)}",
                categoryId = categoryId,
                amount = amount,
                merchantOrNote = cleanNote,
                dateEpochDay = date.toEpochDay(),
                sample = sample,
                createdAtSeconds = System.currentTimeMillis() / 1000L,
            )
        )
    }

    override suspend fun updateTransaction(
        transactionId: String,
        categoryId: String,
        amount: Double,
        merchantOrNote: String,
        date: LocalDay,
    ): Result<Unit> = runSafe {
        val cleanNote = merchantOrNote.trim().take(120)
        require(transactionId.isNotBlank()) { "Transaction ID is required." }
        require(categoryId.isNotBlank()) { "Category must be selected." }
        require(amount > 0.0 && amount <= 1_000_000.0) { "Amount must be positive." }
        require(cleanNote.isNotEmpty()) { "Merchant or note cannot be empty." }

        dao.updateTransaction(
            id = transactionId,
            categoryId = categoryId,
            amount = amount,
            merchantOrNote = cleanNote,
            dateEpochDay = date.toEpochDay(),
        )
    }

    override suspend fun deleteTransaction(transactionId: String): Result<Unit> = runSafe {
        require(transactionId.isNotBlank()) { "Transaction ID is required." }
        dao.deleteTransaction(transactionId)
    }

    override suspend fun addRecurringItem(
        categoryId: String,
        name: String,
        amount: Double,
        kind: RecurringKind,
        cadence: BillingCadence,
        nextDueDate: LocalDay,
        active: Boolean,
        sample: Boolean,
    ): Result<Unit> = runSafe {
        val cleanName = name.trim().take(80)
        require(categoryId.isNotBlank()) { "Category must be selected." }
        require(cleanName.isNotEmpty()) { "Name cannot be empty." }
        require(amount > 0.0 && amount <= 1_000_000.0) { "Amount must be positive." }

        dao.insertRecurringItem(
            LocalRecurringEntity(
                id = "rec_${UUID.randomUUID().toString().replace("-", "").take(12)}",
                categoryId = categoryId,
                name = cleanName,
                amount = amount,
                kind = kind.storageKey,
                cadence = cadence.storageKey,
                nextDueEpochDay = nextDueDate.toEpochDay(),
                active = active,
                sample = sample,
            )
        )
    }

    override suspend fun updateRecurringItem(
        recurringId: String,
        categoryId: String,
        name: String,
        amount: Double,
        kind: RecurringKind,
        cadence: BillingCadence,
        nextDueDate: LocalDay,
        active: Boolean,
    ): Result<Unit> = runSafe {
        val cleanName = name.trim().take(80)
        require(recurringId.isNotBlank()) { "Recurring item ID required." }
        require(categoryId.isNotBlank()) { "Category must be selected." }
        require(cleanName.isNotEmpty()) { "Name cannot be empty." }
        require(amount > 0.0 && amount <= 1_000_000.0) { "Amount must be positive." }

        dao.updateRecurringItem(
            id = recurringId,
            categoryId = categoryId,
            name = cleanName,
            amount = amount,
            kind = kind.storageKey,
            cadence = cadence.storageKey,
            nextDueEpochDay = nextDueDate.toEpochDay(),
            active = active,
        )
    }

    override suspend fun setRecurringActive(recurringId: String, active: Boolean): Result<Unit> = runSafe {
        require(recurringId.isNotBlank()) { "Recurring item ID required." }
        dao.setRecurringActive(recurringId, active)
    }

    override suspend fun logRecurringPayment(item: RecurringItem, paidOn: LocalDay): Result<Unit> = runSafe {
        val nextDue = item.cadence.nextOccurrenceAfter(maxOf(item.nextDueDate, paidOn))
        val tx = LocalTransactionEntity(
            id = "tx_${UUID.randomUUID().toString().replace("-", "").take(12)}",
            categoryId = item.categoryId,
            amount = item.amount,
            merchantOrNote = item.name.take(120),
            dateEpochDay = paidOn.toEpochDay(),
            sample = false,
            createdAtSeconds = System.currentTimeMillis() / 1000L,
        )
        dao.logRecurringAndAdvance(
            transaction = tx,
            recurringId = item.id,
            nextDueEpochDay = nextDue.toEpochDay(),
        )
    }

    override suspend fun deleteRecurringItem(recurringId: String): Result<Unit> = runSafe {
        require(recurringId.isNotBlank()) { "Recurring item ID required." }
        dao.deleteRecurringItem(recurringId)
    }

    override suspend fun createCategory(
        name: String,
        iconKey: String,
        colorHex: Long,
        monthlyLimit: Double,
        sortOrder: Long,
    ): Result<Unit> = runSafe {
        val cleanName = name.trim().take(40)
        val cleanIcon = iconKey.lowercase().replace(Regex("[^a-z0-9_]"), "").ifBlank { "shopping" }
        require(cleanName.isNotEmpty()) { "Category name cannot be empty." }
        require(monthlyLimit > 0.0 && monthlyLimit <= 1_000_000.0) { "Monthly limit must be positive." }

        dao.insertCategory(
            LocalCategoryEntity(
                id = "cat_${UUID.randomUUID().toString().replace("-", "").take(12)}",
                name = cleanName,
                iconKey = cleanIcon,
                colorHex = colorHex,
                monthlyLimit = monthlyLimit,
                sortOrder = sortOrder.coerceIn(0L, 10_000L),
                sample = false,
            )
        )
    }

    override suspend fun updateCategory(
        categoryId: String,
        name: String,
        iconKey: String,
        colorHex: Long,
        monthlyLimit: Double,
    ): Result<Unit> = runSafe {
        val cleanName = name.trim().take(40)
        val cleanIcon = iconKey.lowercase().replace(Regex("[^a-z0-9_]"), "").ifBlank { "shopping" }
        require(categoryId.isNotBlank()) { "Category ID is required." }
        require(cleanName.isNotEmpty()) { "Category name cannot be empty." }
        require(monthlyLimit > 0.0 && monthlyLimit <= 1_000_000.0) { "Monthly limit must be positive." }

        dao.updateCategory(
            id = categoryId,
            name = cleanName,
            iconKey = cleanIcon,
            colorHex = colorHex,
            monthlyLimit = monthlyLimit,
        )
    }

    override suspend fun updateCategoryLimit(categoryId: String, newLimit: Double): Result<Unit> = runSafe {
        require(categoryId.isNotBlank()) { "Category ID is required." }
        require(newLimit > 0.0 && newLimit <= 1_000_000.0) { "Limit must be positive." }
        dao.updateCategoryLimit(categoryId, newLimit)
    }

    override suspend fun deleteCategoryAndReassign(
        categoryId: String,
        fallbackCategoryId: String,
    ): Result<Unit> = runSafe {
        require(categoryId.isNotBlank() && fallbackCategoryId.isNotBlank()) { "Category IDs required." }
        require(categoryId != fallbackCategoryId) { "Cannot reassign category to itself." }
        dao.deleteCategoryAndReassign(categoryId, fallbackCategoryId)
    }

    private suspend inline fun runSafe(block: suspend () -> Unit): Result<Unit> = try {
        block()
        Result.success(Unit)
    } catch (e: CancellationException) {
        throw e
    } catch (e: Exception) {
        Result.failure(e)
    }
}
