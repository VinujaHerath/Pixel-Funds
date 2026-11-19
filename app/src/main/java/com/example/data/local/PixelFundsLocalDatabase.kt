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
import com.example.data.BudgetCategory
import com.example.data.BudgetRepository
import com.example.data.CategoryIconCatalog
import com.example.data.LocalDay
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

    @RoomTransaction
    suspend fun deleteCategoryAndReassign(categoryId: String, fallbackCategoryId: String) {
        reassignTransactions(categoryId, fallbackCategoryId)
        deleteCategory(categoryId)
    }
}

@Database(
    entities = [
        LocalWorkspaceEntity::class,
        LocalCategoryEntity::class,
        LocalTransactionEntity::class,
    ],
    version = 1,
    exportSchema = false,
)
abstract class PixelFundsDatabase : RoomDatabase() {
    abstract fun dao(): PixelFundsDao

    companion object {
        @Volatile
        private var INSTANCE: PixelFundsDatabase? = null

        fun getInstance(context: Context): PixelFundsDatabase =
            INSTANCE ?: synchronized(this) {
                INSTANCE ?: Room.databaseBuilder(
                    context.applicationContext,
                    PixelFundsDatabase::class.java,
                    "pixel_funds_local.db",
                ).build().also { INSTANCE = it }
            }
    }
}

data class LocalMigrationSnapshot(
    val categories: List<BudgetCategory>,
    val transactions: List<Transaction>,
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
        return LocalMigrationSnapshot(
            categories = categories,
            transactions = transactions,
            hasSampleData = workspace.hasSampleData || transactions.any { it.sample },
        )
    }

    override fun observeHasSampleData(): Flow<Boolean> =
        dao.observeWorkspace().map { it?.hasSampleData == true }

    override fun observeCategories(): Flow<List<BudgetCategory>> =
        dao.observeCategories().map { list -> list.map { it.toDomain() } }

    override fun observeTransactions(): Flow<List<Transaction>> =
        dao.observeTransactions().map { list -> list.map { it.toDomain() } }

    override suspend fun ensureInitialSeed(): Result<Unit> = runSafe {
        val existing = dao.getWorkspace()
        if (existing != null && existing.seeded) return@runSafe

        val today = LocalDay.today()
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

        dao.insertCategories(categories)
        dao.insertTransactions(transactions)
        dao.upsertWorkspace(LocalWorkspaceEntity(id = 1, seeded = true, hasSampleData = true))
    }

    override suspend fun clearSampleData(): Result<Unit> = runSafe {
        dao.deleteSampleTransactions()
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

        if (missingCategories.isNotEmpty()) dao.insertCategories(missingCategories)
        dao.insertTransactions(sampleTxs)
        dao.upsertWorkspace(LocalWorkspaceEntity(id = 1, seeded = true, hasSampleData = true))
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
        require(amount > 0.0 && amount <= 1_000_000.0) { "Amount must be between $0.01 and $1,000,000." }
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
        require(amount > 0.0 && amount <= 1_000_000.0) { "Amount must be between $0.01 and $1,000,000." }
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
