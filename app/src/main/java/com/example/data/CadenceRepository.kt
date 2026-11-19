package com.example.data

import android.content.Context
import com.example.R
import com.example.data.local.LocalBudgetRepository
import com.google.firebase.Firebase
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.auth
import com.google.firebase.firestore.DocumentSnapshot
import com.google.firebase.firestore.FieldValue
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.FirebaseFirestoreException
import com.google.firebase.firestore.snapshots
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.emitAll
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.tasks.await
import java.util.UUID

class CadenceRepository(
    private val db: FirebaseFirestore,
    private val userId: String,
    private val localMigrationSource: LocalBudgetRepository? = null,
    private val auth: FirebaseAuth = Firebase.auth,
) : BudgetRepository {

    constructor(
        context: Context,
        userId: String,
        localMigrationSource: LocalBudgetRepository? = null,
    ) : this(
        db = FirebaseFirestore.getInstance(
            context.applicationContext.getString(R.string.firestore_database_id)
        ),
        userId = userId,
        localMigrationSource = localMigrationSource,
    )

    private fun requireAuthorizedUserId(): String {
        val uid = auth.currentUser?.uid
            ?: throw IllegalStateException("User must be signed in with Google before accessing Firestore.")
        check(uid == userId) { "Authenticated user mismatch; aborting cross-user operation." }
        return uid
    }

    private fun userDocRef(uid: String) = db.collection("users").document(uid)
    private fun categoriesCol(uid: String) = userDocRef(uid).collection("categories")
    private fun transactionsCol(uid: String) = userDocRef(uid).collection("transactions")

    override fun observeHasSampleData(): Flow<Boolean> = flow {
        val uid = requireAuthorizedUserId()
        val path = "users/$uid"
        emitAll(
            userDocRef(uid)
                .snapshots()
                .map { snapshot ->
                    if (snapshot.exists()) {
                        snapshot.toObject(
                            UserProfileDoc::class.java,
                            DocumentSnapshot.ServerTimestampBehavior.ESTIMATE,
                        )?.hasSampleData == true
                    } else false
                }
                .catch { error ->
                    logStreamErrorIfActive(error, OperationType.GET, path, uid)
                    throw error
                }
        )
    }

    override fun observeCategories(): Flow<List<BudgetCategory>> = flow {
        val uid = requireAuthorizedUserId()
        val path = "users/$uid/categories"
        emitAll(
            categoriesCol(uid)
                .snapshots()
                .map { snapshot ->
                    snapshot.toObjects(
                        BudgetCategoryDoc::class.java,
                        DocumentSnapshot.ServerTimestampBehavior.ESTIMATE,
                    )
                        .map { it.toDomain() }
                        .sortedWith(compareBy<BudgetCategory> { it.sortOrder }.thenBy { it.name })
                }
                .catch { error ->
                    logStreamErrorIfActive(error, OperationType.LIST, path, uid)
                    throw error
                }
        )
    }

    override fun observeTransactions(): Flow<List<Transaction>> = flow {
        val uid = requireAuthorizedUserId()
        val path = "users/$uid/transactions"
        emitAll(
            transactionsCol(uid)
                .snapshots()
                .map { snapshot ->
                    snapshot.toObjects(
                        TransactionDoc::class.java,
                        DocumentSnapshot.ServerTimestampBehavior.ESTIMATE,
                    )
                        .map { it.toDomain() }
                        .sortedWith(
                            compareByDescending<Transaction> { it.date }
                                .thenByDescending { it.createdAtSeconds }
                        )
                }
                .catch { error ->
                    logStreamErrorIfActive(error, OperationType.LIST, path, uid)
                    throw error
                }
        )
    }

    private fun logStreamErrorIfActive(
        error: Throwable,
        operationType: OperationType,
        path: String,
        uid: String,
    ) {
        val firestoreError = generateSequence(error) { it.cause }
            .filterIsInstance<FirebaseFirestoreException>()
            .firstOrNull()
        if (firestoreError == null && error is CancellationException) return
        val exceptionToLog = firestoreError ?: error
        if (auth.currentUser?.uid == uid) {
            handleFirestoreError(exceptionToLog, operationType, path)
        }
    }

    /**
     * Initializes a new cloud account once: migrates existing on-device local categories &
     * transactions if present, otherwise seeds starter sample data.
     */
    override suspend fun ensureInitialSeed(): Result<Unit> {
        val path = "users/$userId"
        return try {
            val uid = requireAuthorizedUserId()
            val profileSnapshot = userDocRef(uid).get().await()
            if (profileSnapshot.exists()) {
                return Result.success(Unit)
            }

            val localSnapshot = localMigrationSource?.readSnapshotForCloudMigration()
            val batch = db.batch()

            if (localSnapshot != null && localSnapshot.categories.isNotEmpty()) {
                val validCategoryIds = mutableSetOf<String>()
                batch.set(
                    userDocRef(uid),
                    mapOf(
                        "userId" to uid,
                        "seeded" to true,
                        "hasSampleData" to localSnapshot.hasSampleData,
                        "createdAt" to FieldValue.serverTimestamp(),
                        "updatedAt" to FieldValue.serverTimestamp(),
                    ),
                )

                localSnapshot.categories.take(100).forEachIndexed { index, cat ->
                    val safeId = sanitizeDocId(cat.id, "cat_$index")
                    validCategoryIds.add(safeId)
                    batch.set(
                        categoriesCol(uid).document(safeId),
                        mapOf(
                            "userId" to uid,
                            "name" to cat.name.trim().ifBlank { "Budget" }.take(40),
                            "iconKey" to sanitizeIconKey(cat.iconKey),
                            "colorHex" to cat.colorHex.coerceAtLeast(0L),
                            "monthlyLimit" to cat.monthlyLimit.coerceIn(1.0, 1_000_000.0),
                            "sortOrder" to cat.sortOrder.coerceIn(0L, 10_000L),
                            "sample" to cat.sample,
                            "createdAt" to FieldValue.serverTimestamp(),
                            "updatedAt" to FieldValue.serverTimestamp(),
                        ),
                    )
                }

                val fallbackCatId = validCategoryIds.first()
                localSnapshot.transactions.take(350).forEach { tx ->
                    val safeCatId = sanitizeDocId(tx.categoryId, fallbackCatId)
                        .takeIf { it in validCategoryIds } ?: fallbackCatId
                    batch.set(
                        transactionsCol(uid).document(),
                        mapOf(
                            "userId" to uid,
                            "categoryId" to safeCatId,
                            "amount" to tx.amount.coerceIn(0.01, 1_000_000.0),
                            "merchantOrNote" to tx.merchantOrNote.trim().ifBlank { "Expense" }.take(120),
                            "dateEpochDay" to tx.date.toEpochDay().coerceIn(0L, 100_000L),
                            "sample" to tx.sample,
                            "createdAt" to FieldValue.serverTimestamp(),
                            "updatedAt" to FieldValue.serverTimestamp(),
                        ),
                    )
                }
            } else {
                val today = LocalDay.today()
                batch.set(
                    userDocRef(uid),
                    mapOf(
                        "userId" to uid,
                        "seeded" to true,
                        "hasSampleData" to true,
                        "createdAt" to FieldValue.serverTimestamp(),
                        "updatedAt" to FieldValue.serverTimestamp(),
                    ),
                )

                StarterBudgetData.defaultCategories().forEachIndexed { index, seed ->
                    batch.set(
                        categoriesCol(uid).document(seed.id),
                        mapOf(
                            "userId" to uid,
                            "name" to seed.name,
                            "iconKey" to seed.iconKey,
                            "colorHex" to seed.colorHex,
                            "monthlyLimit" to seed.monthlyLimit,
                            "sortOrder" to index.toLong(),
                            "sample" to true,
                            "createdAt" to FieldValue.serverTimestamp(),
                            "updatedAt" to FieldValue.serverTimestamp(),
                        ),
                    )
                }

                StarterBudgetData.defaultTransactions(today).forEach { seed ->
                    batch.set(
                        transactionsCol(uid).document(),
                        mapOf(
                            "userId" to uid,
                            "categoryId" to seed.categoryId,
                            "amount" to seed.amount,
                            "merchantOrNote" to seed.merchantOrNote,
                            "dateEpochDay" to seed.date.toEpochDay(),
                            "sample" to true,
                            "createdAt" to FieldValue.serverTimestamp(),
                            "updatedAt" to FieldValue.serverTimestamp(),
                        ),
                    )
                }
            }

            batch.commit().await()
            Result.success(Unit)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.WRITE, path)
            Result.failure(e)
        }
    }

    override suspend fun clearSampleData(): Result<Unit> {
        val path = "users/$userId"
        return try {
            val uid = requireAuthorizedUserId()
            val txSnap = transactionsCol(uid).get().await()
            val batch = db.batch()

            txSnap.documents.forEach { doc ->
                if (doc.getBoolean("sample") == true) {
                    batch.delete(doc.reference)
                }
            }

            val profileSnap = userDocRef(uid).get().await()
            if (profileSnap.exists()) {
                batch.update(
                    userDocRef(uid),
                    mapOf(
                        "hasSampleData" to false,
                        "updatedAt" to FieldValue.serverTimestamp(),
                    ),
                )
            } else {
                batch.set(
                    userDocRef(uid),
                    mapOf(
                        "userId" to uid,
                        "seeded" to true,
                        "hasSampleData" to false,
                        "createdAt" to FieldValue.serverTimestamp(),
                        "updatedAt" to FieldValue.serverTimestamp(),
                    ),
                )
            }

            batch.commit().await()
            Result.success(Unit)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.WRITE, path)
            Result.failure(e)
        }
    }

    override suspend fun restoreSampleData(): Result<Unit> {
        val path = "users/$userId"
        return try {
            val uid = requireAuthorizedUserId()
            val existingCats = categoriesCol(uid).get().await().documents.map { it.id }.toSet()
            val today = LocalDay.today()
            val batch = db.batch()

            StarterBudgetData.defaultCategories().forEachIndexed { index, seed ->
                if (seed.id !in existingCats) {
                    batch.set(
                        categoriesCol(uid).document(seed.id),
                        mapOf(
                            "userId" to uid,
                            "name" to seed.name,
                            "iconKey" to seed.iconKey,
                            "colorHex" to seed.colorHex,
                            "monthlyLimit" to seed.monthlyLimit,
                            "sortOrder" to index.toLong(),
                            "sample" to true,
                            "createdAt" to FieldValue.serverTimestamp(),
                            "updatedAt" to FieldValue.serverTimestamp(),
                        ),
                    )
                }
            }

            StarterBudgetData.defaultTransactions(today).forEach { seed ->
                batch.set(
                    transactionsCol(uid).document(),
                    mapOf(
                        "userId" to uid,
                        "categoryId" to seed.categoryId,
                        "amount" to seed.amount,
                        "merchantOrNote" to seed.merchantOrNote,
                        "dateEpochDay" to seed.date.toEpochDay(),
                        "sample" to true,
                        "createdAt" to FieldValue.serverTimestamp(),
                        "updatedAt" to FieldValue.serverTimestamp(),
                    ),
                )
            }

            val profileSnap = userDocRef(uid).get().await()
            if (profileSnap.exists()) {
                batch.update(
                    userDocRef(uid),
                    mapOf(
                        "seeded" to true,
                        "hasSampleData" to true,
                        "updatedAt" to FieldValue.serverTimestamp(),
                    ),
                )
            } else {
                batch.set(
                    userDocRef(uid),
                    mapOf(
                        "userId" to uid,
                        "seeded" to true,
                        "hasSampleData" to true,
                        "createdAt" to FieldValue.serverTimestamp(),
                        "updatedAt" to FieldValue.serverTimestamp(),
                    ),
                )
            }

            batch.commit().await()
            Result.success(Unit)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.WRITE, path)
            Result.failure(e)
        }
    }

    override suspend fun addTransaction(
        categoryId: String,
        amount: Double,
        merchantOrNote: String,
        date: LocalDay,
        sample: Boolean,
    ): Result<Unit> {
        var docPath = "users/$userId/transactions"
        return try {
            val uid = requireAuthorizedUserId()
            val cleanNote = merchantOrNote.trim().take(120)
            require(categoryId.isNotBlank()) { "Category must be selected." }
            require(amount > 0.0 && amount <= 1_000_000.0) { "Amount must be between $0.01 and $1,000,000." }
            require(cleanNote.isNotEmpty()) { "Merchant or note cannot be empty." }

            val docRef = transactionsCol(uid).document()
            docPath = docRef.path
            val payload = mapOf(
                "userId" to uid,
                "categoryId" to categoryId,
                "amount" to amount,
                "merchantOrNote" to cleanNote,
                "dateEpochDay" to date.toEpochDay(),
                "sample" to sample,
                "createdAt" to FieldValue.serverTimestamp(),
                "updatedAt" to FieldValue.serverTimestamp(),
            )
            docRef.set(payload).await()
            Result.success(Unit)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.CREATE, docPath)
            Result.failure(e)
        }
    }

    override suspend fun updateTransaction(
        transactionId: String,
        categoryId: String,
        amount: Double,
        merchantOrNote: String,
        date: LocalDay,
    ): Result<Unit> {
        val docPath = "users/$userId/transactions/$transactionId"
        return try {
            val uid = requireAuthorizedUserId()
            val cleanNote = merchantOrNote.trim().take(120)
            require(transactionId.isNotBlank()) { "Transaction ID is required." }
            require(categoryId.isNotBlank()) { "Category must be selected." }
            require(amount > 0.0 && amount <= 1_000_000.0) { "Amount must be between $0.01 and $1,000,000." }
            require(cleanNote.isNotEmpty()) { "Merchant or note cannot be empty." }

            transactionsCol(uid).document(transactionId).update(
                mapOf(
                    "categoryId" to categoryId,
                    "amount" to amount,
                    "merchantOrNote" to cleanNote,
                    "dateEpochDay" to date.toEpochDay(),
                    "sample" to false,
                    "updatedAt" to FieldValue.serverTimestamp(),
                )
            ).await()
            Result.success(Unit)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.UPDATE, docPath)
            Result.failure(e)
        }
    }

    override suspend fun deleteTransaction(transactionId: String): Result<Unit> {
        val docPath = "users/$userId/transactions/$transactionId"
        return try {
            val uid = requireAuthorizedUserId()
            require(transactionId.isNotBlank()) { "Transaction ID is required." }
            transactionsCol(uid).document(transactionId).delete().await()
            Result.success(Unit)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.DELETE, docPath)
            Result.failure(e)
        }
    }

    override suspend fun createCategory(
        name: String,
        iconKey: String,
        colorHex: Long,
        monthlyLimit: Double,
        sortOrder: Long,
    ): Result<Unit> {
        var docPath = "users/$userId/categories"
        return try {
            val uid = requireAuthorizedUserId()
            val cleanName = name.trim().take(40)
            val cleanIcon = sanitizeIconKey(iconKey)
            require(cleanName.isNotEmpty()) { "Category name cannot be empty." }
            require(monthlyLimit > 0.0 && monthlyLimit <= 1_000_000.0) { "Monthly limit must be positive." }

            val docId = "cat_${UUID.randomUUID().toString().replace("-", "").take(12)}"
            val docRef = categoriesCol(uid).document(docId)
            docPath = docRef.path
            docRef.set(
                mapOf(
                    "userId" to uid,
                    "name" to cleanName,
                    "iconKey" to cleanIcon,
                    "colorHex" to colorHex,
                    "monthlyLimit" to monthlyLimit,
                    "sortOrder" to sortOrder.coerceIn(0L, 10_000L),
                    "sample" to false,
                    "createdAt" to FieldValue.serverTimestamp(),
                    "updatedAt" to FieldValue.serverTimestamp(),
                )
            ).await()
            Result.success(Unit)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.CREATE, docPath)
            Result.failure(e)
        }
    }

    override suspend fun updateCategory(
        categoryId: String,
        name: String,
        iconKey: String,
        colorHex: Long,
        monthlyLimit: Double,
    ): Result<Unit> {
        val docPath = "users/$userId/categories/$categoryId"
        return try {
            val uid = requireAuthorizedUserId()
            val cleanName = name.trim().take(40)
            val cleanIcon = sanitizeIconKey(iconKey)
            require(categoryId.isNotBlank()) { "Category ID is required." }
            require(cleanName.isNotEmpty()) { "Category name cannot be empty." }
            require(monthlyLimit > 0.0 && monthlyLimit <= 1_000_000.0) { "Monthly limit must be positive." }

            categoriesCol(uid).document(categoryId).update(
                mapOf(
                    "name" to cleanName,
                    "iconKey" to cleanIcon,
                    "colorHex" to colorHex,
                    "monthlyLimit" to monthlyLimit,
                    "sample" to false,
                    "updatedAt" to FieldValue.serverTimestamp(),
                )
            ).await()
            Result.success(Unit)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.UPDATE, docPath)
            Result.failure(e)
        }
    }

    override suspend fun updateCategoryLimit(categoryId: String, newLimit: Double): Result<Unit> {
        val docPath = "users/$userId/categories/$categoryId"
        return try {
            val uid = requireAuthorizedUserId()
            require(categoryId.isNotBlank()) { "Category ID is required." }
            require(newLimit > 0.0 && newLimit <= 1_000_000.0) { "Limit must be positive." }

            categoriesCol(uid).document(categoryId).update(
                mapOf(
                    "monthlyLimit" to newLimit,
                    "updatedAt" to FieldValue.serverTimestamp(),
                )
            ).await()
            Result.success(Unit)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.UPDATE, docPath)
            Result.failure(e)
        }
    }

    override suspend fun deleteCategoryAndReassign(
        categoryId: String,
        fallbackCategoryId: String,
    ): Result<Unit> {
        val docPath = "users/$userId/categories/$categoryId"
        return try {
            val uid = requireAuthorizedUserId()
            require(categoryId.isNotBlank() && fallbackCategoryId.isNotBlank()) { "Category IDs required." }
            require(categoryId != fallbackCategoryId) { "Cannot reassign category to itself." }

            val txSnap = transactionsCol(uid).get().await()
            val batch = db.batch()
            txSnap.documents.forEach { doc ->
                if (doc.getString("categoryId") == categoryId) {
                    batch.update(
                        doc.reference,
                        mapOf(
                            "categoryId" to fallbackCategoryId,
                            "updatedAt" to FieldValue.serverTimestamp(),
                        ),
                    )
                }
            }
            batch.delete(categoriesCol(uid).document(categoryId))
            batch.commit().await()
            Result.success(Unit)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.DELETE, docPath)
            Result.failure(e)
        }
    }

    private fun sanitizeIconKey(iconKey: String): String =
        iconKey.lowercase().replace(Regex("[^a-z0-9_]"), "").take(32).ifBlank { "shopping" }

    private fun sanitizeDocId(rawId: String, fallback: String): String =
        rawId.replace(Regex("[^a-zA-Z0-9_\\-]"), "").take(128).ifBlank { fallback }
}
