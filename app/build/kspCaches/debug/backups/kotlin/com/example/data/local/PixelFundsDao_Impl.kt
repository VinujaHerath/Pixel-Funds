package com.example.`data`.local

import androidx.room.EntityInsertAdapter
import androidx.room.RoomDatabase
import androidx.room.coroutines.createFlow
import androidx.room.util.getColumnIndexOrThrow
import androidx.room.util.performInTransactionSuspending
import androidx.room.util.performSuspending
import androidx.sqlite.SQLiteStatement
import javax.`annotation`.processing.Generated
import kotlin.Boolean
import kotlin.Double
import kotlin.Int
import kotlin.Long
import kotlin.String
import kotlin.Suppress
import kotlin.Unit
import kotlin.collections.List
import kotlin.collections.MutableList
import kotlin.collections.mutableListOf
import kotlin.reflect.KClass
import kotlinx.coroutines.flow.Flow

@Generated(value = ["androidx.room.RoomProcessor"])
@Suppress(names = ["UNCHECKED_CAST", "DEPRECATION", "REDUNDANT_PROJECTION", "REMOVAL"])
public class PixelFundsDao_Impl(
  __db: RoomDatabase,
) : PixelFundsDao {
  private val __db: RoomDatabase

  private val __insertAdapterOfLocalWorkspaceEntity: EntityInsertAdapter<LocalWorkspaceEntity>

  private val __insertAdapterOfLocalCategoryEntity: EntityInsertAdapter<LocalCategoryEntity>

  private val __insertAdapterOfLocalTransactionEntity: EntityInsertAdapter<LocalTransactionEntity>

  private val __insertAdapterOfLocalRecurringEntity: EntityInsertAdapter<LocalRecurringEntity>

  private val __insertAdapterOfMonthlyBudgetLimitEntity:
      EntityInsertAdapter<MonthlyBudgetLimitEntity>
  init {
    this.__db = __db
    this.__insertAdapterOfLocalWorkspaceEntity = object :
        EntityInsertAdapter<LocalWorkspaceEntity>() {
      protected override fun createQuery(): String =
          "INSERT OR REPLACE INTO `local_workspace` (`id`,`seeded`,`hasSampleData`) VALUES (?,?,?)"

      protected override fun bind(statement: SQLiteStatement, entity: LocalWorkspaceEntity) {
        statement.bindLong(1, entity.id.toLong())
        val _tmp: Int = if (entity.seeded) 1 else 0
        statement.bindLong(2, _tmp.toLong())
        val _tmp_1: Int = if (entity.hasSampleData) 1 else 0
        statement.bindLong(3, _tmp_1.toLong())
      }
    }
    this.__insertAdapterOfLocalCategoryEntity = object : EntityInsertAdapter<LocalCategoryEntity>()
        {
      protected override fun createQuery(): String =
          "INSERT OR REPLACE INTO `local_categories` (`id`,`name`,`iconKey`,`colorHex`,`monthlyLimit`,`sortOrder`,`sample`) VALUES (?,?,?,?,?,?,?)"

      protected override fun bind(statement: SQLiteStatement, entity: LocalCategoryEntity) {
        statement.bindText(1, entity.id)
        statement.bindText(2, entity.name)
        statement.bindText(3, entity.iconKey)
        statement.bindLong(4, entity.colorHex)
        statement.bindDouble(5, entity.monthlyLimit)
        statement.bindLong(6, entity.sortOrder)
        val _tmp: Int = if (entity.sample) 1 else 0
        statement.bindLong(7, _tmp.toLong())
      }
    }
    this.__insertAdapterOfLocalTransactionEntity = object :
        EntityInsertAdapter<LocalTransactionEntity>() {
      protected override fun createQuery(): String =
          "INSERT OR REPLACE INTO `local_transactions` (`id`,`categoryId`,`amount`,`merchantOrNote`,`dateEpochDay`,`sample`,`createdAtSeconds`) VALUES (?,?,?,?,?,?,?)"

      protected override fun bind(statement: SQLiteStatement, entity: LocalTransactionEntity) {
        statement.bindText(1, entity.id)
        statement.bindText(2, entity.categoryId)
        statement.bindDouble(3, entity.amount)
        statement.bindText(4, entity.merchantOrNote)
        statement.bindLong(5, entity.dateEpochDay)
        val _tmp: Int = if (entity.sample) 1 else 0
        statement.bindLong(6, _tmp.toLong())
        statement.bindLong(7, entity.createdAtSeconds)
      }
    }
    this.__insertAdapterOfLocalRecurringEntity = object :
        EntityInsertAdapter<LocalRecurringEntity>() {
      protected override fun createQuery(): String =
          "INSERT OR REPLACE INTO `local_recurring_items` (`id`,`categoryId`,`name`,`amount`,`kind`,`cadence`,`nextDueEpochDay`,`active`,`sample`) VALUES (?,?,?,?,?,?,?,?,?)"

      protected override fun bind(statement: SQLiteStatement, entity: LocalRecurringEntity) {
        statement.bindText(1, entity.id)
        statement.bindText(2, entity.categoryId)
        statement.bindText(3, entity.name)
        statement.bindDouble(4, entity.amount)
        statement.bindText(5, entity.kind)
        statement.bindText(6, entity.cadence)
        statement.bindLong(7, entity.nextDueEpochDay)
        val _tmp: Int = if (entity.active) 1 else 0
        statement.bindLong(8, _tmp.toLong())
        val _tmp_1: Int = if (entity.sample) 1 else 0
        statement.bindLong(9, _tmp_1.toLong())
      }
    }
    this.__insertAdapterOfMonthlyBudgetLimitEntity = object :
        EntityInsertAdapter<MonthlyBudgetLimitEntity>() {
      protected override fun createQuery(): String =
          "INSERT OR REPLACE INTO `local_monthly_budget_limits` (`periodKey`,`limitAmount`,`updatedAtMillis`) VALUES (?,?,?)"

      protected override fun bind(statement: SQLiteStatement, entity: MonthlyBudgetLimitEntity) {
        statement.bindText(1, entity.periodKey)
        statement.bindDouble(2, entity.limitAmount)
        statement.bindLong(3, entity.updatedAtMillis)
      }
    }
  }

  public override suspend fun upsertWorkspace(entity: LocalWorkspaceEntity): Unit =
      performSuspending(__db, false, true) { _connection ->
    __insertAdapterOfLocalWorkspaceEntity.insert(_connection, entity)
  }

  public override suspend fun insertCategory(category: LocalCategoryEntity): Unit =
      performSuspending(__db, false, true) { _connection ->
    __insertAdapterOfLocalCategoryEntity.insert(_connection, category)
  }

  public override suspend fun insertCategories(categories: List<LocalCategoryEntity>): Unit =
      performSuspending(__db, false, true) { _connection ->
    __insertAdapterOfLocalCategoryEntity.insert(_connection, categories)
  }

  public override suspend fun insertTransaction(transaction: LocalTransactionEntity): Unit =
      performSuspending(__db, false, true) { _connection ->
    __insertAdapterOfLocalTransactionEntity.insert(_connection, transaction)
  }

  public override suspend fun insertTransactions(transactions: List<LocalTransactionEntity>): Unit =
      performSuspending(__db, false, true) { _connection ->
    __insertAdapterOfLocalTransactionEntity.insert(_connection, transactions)
  }

  public override suspend fun insertRecurringItem(item: LocalRecurringEntity): Unit =
      performSuspending(__db, false, true) { _connection ->
    __insertAdapterOfLocalRecurringEntity.insert(_connection, item)
  }

  public override suspend fun insertRecurringItems(items: List<LocalRecurringEntity>): Unit =
      performSuspending(__db, false, true) { _connection ->
    __insertAdapterOfLocalRecurringEntity.insert(_connection, items)
  }

  public override suspend fun insertMonthlyBudgetLimits(limits: List<MonthlyBudgetLimitEntity>):
      Unit = performSuspending(__db, false, true) { _connection ->
    __insertAdapterOfMonthlyBudgetLimitEntity.insert(_connection, limits)
  }

  public override suspend fun deleteCategoryAndReassign(categoryId: String,
      fallbackCategoryId: String): Unit = performInTransactionSuspending(__db) {
    super@PixelFundsDao_Impl.deleteCategoryAndReassign(categoryId, fallbackCategoryId)
  }

  public override suspend fun logRecurringAndAdvance(
    transaction: LocalTransactionEntity,
    recurringId: String,
    nextDueEpochDay: Long,
  ): Unit = performInTransactionSuspending(__db) {
    super@PixelFundsDao_Impl.logRecurringAndAdvance(transaction, recurringId, nextDueEpochDay)
  }

  public override suspend fun replaceWorkspaceFromBackup(
    categories: List<LocalCategoryEntity>,
    transactions: List<LocalTransactionEntity>,
    recurringItems: List<LocalRecurringEntity>,
    monthlyBudgetLimits: List<MonthlyBudgetLimitEntity>,
    hasSampleData: Boolean,
  ): Unit = performInTransactionSuspending(__db) {
    super@PixelFundsDao_Impl.replaceWorkspaceFromBackup(categories, transactions, recurringItems,
        monthlyBudgetLimits, hasSampleData)
  }

  public override suspend fun importTransactionsAtomic(
    newCategories: List<LocalCategoryEntity>,
    transactions: List<LocalTransactionEntity>,
    replaceExisting: Boolean,
  ): Unit = performInTransactionSuspending(__db) {
    super@PixelFundsDao_Impl.importTransactionsAtomic(newCategories, transactions, replaceExisting)
  }

  public override fun observeWorkspace(): Flow<LocalWorkspaceEntity?> {
    val _sql: String = "SELECT * FROM local_workspace WHERE id = 1"
    return createFlow(__db, false, arrayOf("local_workspace")) { _connection ->
      val _stmt: SQLiteStatement = _connection.prepare(_sql)
      try {
        val _columnIndexOfId: Int = getColumnIndexOrThrow(_stmt, "id")
        val _columnIndexOfSeeded: Int = getColumnIndexOrThrow(_stmt, "seeded")
        val _columnIndexOfHasSampleData: Int = getColumnIndexOrThrow(_stmt, "hasSampleData")
        val _result: LocalWorkspaceEntity?
        if (_stmt.step()) {
          val _tmpId: Int
          _tmpId = _stmt.getLong(_columnIndexOfId).toInt()
          val _tmpSeeded: Boolean
          val _tmp: Int
          _tmp = _stmt.getLong(_columnIndexOfSeeded).toInt()
          _tmpSeeded = _tmp != 0
          val _tmpHasSampleData: Boolean
          val _tmp_1: Int
          _tmp_1 = _stmt.getLong(_columnIndexOfHasSampleData).toInt()
          _tmpHasSampleData = _tmp_1 != 0
          _result = LocalWorkspaceEntity(_tmpId,_tmpSeeded,_tmpHasSampleData)
        } else {
          _result = null
        }
        _result
      } finally {
        _stmt.close()
      }
    }
  }

  public override suspend fun getWorkspace(): LocalWorkspaceEntity? {
    val _sql: String = "SELECT * FROM local_workspace WHERE id = 1"
    return performSuspending(__db, true, false) { _connection ->
      val _stmt: SQLiteStatement = _connection.prepare(_sql)
      try {
        val _columnIndexOfId: Int = getColumnIndexOrThrow(_stmt, "id")
        val _columnIndexOfSeeded: Int = getColumnIndexOrThrow(_stmt, "seeded")
        val _columnIndexOfHasSampleData: Int = getColumnIndexOrThrow(_stmt, "hasSampleData")
        val _result: LocalWorkspaceEntity?
        if (_stmt.step()) {
          val _tmpId: Int
          _tmpId = _stmt.getLong(_columnIndexOfId).toInt()
          val _tmpSeeded: Boolean
          val _tmp: Int
          _tmp = _stmt.getLong(_columnIndexOfSeeded).toInt()
          _tmpSeeded = _tmp != 0
          val _tmpHasSampleData: Boolean
          val _tmp_1: Int
          _tmp_1 = _stmt.getLong(_columnIndexOfHasSampleData).toInt()
          _tmpHasSampleData = _tmp_1 != 0
          _result = LocalWorkspaceEntity(_tmpId,_tmpSeeded,_tmpHasSampleData)
        } else {
          _result = null
        }
        _result
      } finally {
        _stmt.close()
      }
    }
  }

  public override fun observeCategories(): Flow<List<LocalCategoryEntity>> {
    val _sql: String = "SELECT * FROM local_categories ORDER BY sortOrder ASC, name ASC"
    return createFlow(__db, false, arrayOf("local_categories")) { _connection ->
      val _stmt: SQLiteStatement = _connection.prepare(_sql)
      try {
        val _columnIndexOfId: Int = getColumnIndexOrThrow(_stmt, "id")
        val _columnIndexOfName: Int = getColumnIndexOrThrow(_stmt, "name")
        val _columnIndexOfIconKey: Int = getColumnIndexOrThrow(_stmt, "iconKey")
        val _columnIndexOfColorHex: Int = getColumnIndexOrThrow(_stmt, "colorHex")
        val _columnIndexOfMonthlyLimit: Int = getColumnIndexOrThrow(_stmt, "monthlyLimit")
        val _columnIndexOfSortOrder: Int = getColumnIndexOrThrow(_stmt, "sortOrder")
        val _columnIndexOfSample: Int = getColumnIndexOrThrow(_stmt, "sample")
        val _result: MutableList<LocalCategoryEntity> = mutableListOf()
        while (_stmt.step()) {
          val _item: LocalCategoryEntity
          val _tmpId: String
          _tmpId = _stmt.getText(_columnIndexOfId)
          val _tmpName: String
          _tmpName = _stmt.getText(_columnIndexOfName)
          val _tmpIconKey: String
          _tmpIconKey = _stmt.getText(_columnIndexOfIconKey)
          val _tmpColorHex: Long
          _tmpColorHex = _stmt.getLong(_columnIndexOfColorHex)
          val _tmpMonthlyLimit: Double
          _tmpMonthlyLimit = _stmt.getDouble(_columnIndexOfMonthlyLimit)
          val _tmpSortOrder: Long
          _tmpSortOrder = _stmt.getLong(_columnIndexOfSortOrder)
          val _tmpSample: Boolean
          val _tmp: Int
          _tmp = _stmt.getLong(_columnIndexOfSample).toInt()
          _tmpSample = _tmp != 0
          _item =
              LocalCategoryEntity(_tmpId,_tmpName,_tmpIconKey,_tmpColorHex,_tmpMonthlyLimit,_tmpSortOrder,_tmpSample)
          _result.add(_item)
        }
        _result
      } finally {
        _stmt.close()
      }
    }
  }

  public override suspend fun getCategories(): List<LocalCategoryEntity> {
    val _sql: String = "SELECT * FROM local_categories ORDER BY sortOrder ASC, name ASC"
    return performSuspending(__db, true, false) { _connection ->
      val _stmt: SQLiteStatement = _connection.prepare(_sql)
      try {
        val _columnIndexOfId: Int = getColumnIndexOrThrow(_stmt, "id")
        val _columnIndexOfName: Int = getColumnIndexOrThrow(_stmt, "name")
        val _columnIndexOfIconKey: Int = getColumnIndexOrThrow(_stmt, "iconKey")
        val _columnIndexOfColorHex: Int = getColumnIndexOrThrow(_stmt, "colorHex")
        val _columnIndexOfMonthlyLimit: Int = getColumnIndexOrThrow(_stmt, "monthlyLimit")
        val _columnIndexOfSortOrder: Int = getColumnIndexOrThrow(_stmt, "sortOrder")
        val _columnIndexOfSample: Int = getColumnIndexOrThrow(_stmt, "sample")
        val _result: MutableList<LocalCategoryEntity> = mutableListOf()
        while (_stmt.step()) {
          val _item: LocalCategoryEntity
          val _tmpId: String
          _tmpId = _stmt.getText(_columnIndexOfId)
          val _tmpName: String
          _tmpName = _stmt.getText(_columnIndexOfName)
          val _tmpIconKey: String
          _tmpIconKey = _stmt.getText(_columnIndexOfIconKey)
          val _tmpColorHex: Long
          _tmpColorHex = _stmt.getLong(_columnIndexOfColorHex)
          val _tmpMonthlyLimit: Double
          _tmpMonthlyLimit = _stmt.getDouble(_columnIndexOfMonthlyLimit)
          val _tmpSortOrder: Long
          _tmpSortOrder = _stmt.getLong(_columnIndexOfSortOrder)
          val _tmpSample: Boolean
          val _tmp: Int
          _tmp = _stmt.getLong(_columnIndexOfSample).toInt()
          _tmpSample = _tmp != 0
          _item =
              LocalCategoryEntity(_tmpId,_tmpName,_tmpIconKey,_tmpColorHex,_tmpMonthlyLimit,_tmpSortOrder,_tmpSample)
          _result.add(_item)
        }
        _result
      } finally {
        _stmt.close()
      }
    }
  }

  public override fun observeTransactions(): Flow<List<LocalTransactionEntity>> {
    val _sql: String =
        "SELECT * FROM local_transactions ORDER BY dateEpochDay DESC, createdAtSeconds DESC"
    return createFlow(__db, false, arrayOf("local_transactions")) { _connection ->
      val _stmt: SQLiteStatement = _connection.prepare(_sql)
      try {
        val _columnIndexOfId: Int = getColumnIndexOrThrow(_stmt, "id")
        val _columnIndexOfCategoryId: Int = getColumnIndexOrThrow(_stmt, "categoryId")
        val _columnIndexOfAmount: Int = getColumnIndexOrThrow(_stmt, "amount")
        val _columnIndexOfMerchantOrNote: Int = getColumnIndexOrThrow(_stmt, "merchantOrNote")
        val _columnIndexOfDateEpochDay: Int = getColumnIndexOrThrow(_stmt, "dateEpochDay")
        val _columnIndexOfSample: Int = getColumnIndexOrThrow(_stmt, "sample")
        val _columnIndexOfCreatedAtSeconds: Int = getColumnIndexOrThrow(_stmt, "createdAtSeconds")
        val _result: MutableList<LocalTransactionEntity> = mutableListOf()
        while (_stmt.step()) {
          val _item: LocalTransactionEntity
          val _tmpId: String
          _tmpId = _stmt.getText(_columnIndexOfId)
          val _tmpCategoryId: String
          _tmpCategoryId = _stmt.getText(_columnIndexOfCategoryId)
          val _tmpAmount: Double
          _tmpAmount = _stmt.getDouble(_columnIndexOfAmount)
          val _tmpMerchantOrNote: String
          _tmpMerchantOrNote = _stmt.getText(_columnIndexOfMerchantOrNote)
          val _tmpDateEpochDay: Long
          _tmpDateEpochDay = _stmt.getLong(_columnIndexOfDateEpochDay)
          val _tmpSample: Boolean
          val _tmp: Int
          _tmp = _stmt.getLong(_columnIndexOfSample).toInt()
          _tmpSample = _tmp != 0
          val _tmpCreatedAtSeconds: Long
          _tmpCreatedAtSeconds = _stmt.getLong(_columnIndexOfCreatedAtSeconds)
          _item =
              LocalTransactionEntity(_tmpId,_tmpCategoryId,_tmpAmount,_tmpMerchantOrNote,_tmpDateEpochDay,_tmpSample,_tmpCreatedAtSeconds)
          _result.add(_item)
        }
        _result
      } finally {
        _stmt.close()
      }
    }
  }

  public override suspend fun getTransactions(): List<LocalTransactionEntity> {
    val _sql: String =
        "SELECT * FROM local_transactions ORDER BY dateEpochDay DESC, createdAtSeconds DESC"
    return performSuspending(__db, true, false) { _connection ->
      val _stmt: SQLiteStatement = _connection.prepare(_sql)
      try {
        val _columnIndexOfId: Int = getColumnIndexOrThrow(_stmt, "id")
        val _columnIndexOfCategoryId: Int = getColumnIndexOrThrow(_stmt, "categoryId")
        val _columnIndexOfAmount: Int = getColumnIndexOrThrow(_stmt, "amount")
        val _columnIndexOfMerchantOrNote: Int = getColumnIndexOrThrow(_stmt, "merchantOrNote")
        val _columnIndexOfDateEpochDay: Int = getColumnIndexOrThrow(_stmt, "dateEpochDay")
        val _columnIndexOfSample: Int = getColumnIndexOrThrow(_stmt, "sample")
        val _columnIndexOfCreatedAtSeconds: Int = getColumnIndexOrThrow(_stmt, "createdAtSeconds")
        val _result: MutableList<LocalTransactionEntity> = mutableListOf()
        while (_stmt.step()) {
          val _item: LocalTransactionEntity
          val _tmpId: String
          _tmpId = _stmt.getText(_columnIndexOfId)
          val _tmpCategoryId: String
          _tmpCategoryId = _stmt.getText(_columnIndexOfCategoryId)
          val _tmpAmount: Double
          _tmpAmount = _stmt.getDouble(_columnIndexOfAmount)
          val _tmpMerchantOrNote: String
          _tmpMerchantOrNote = _stmt.getText(_columnIndexOfMerchantOrNote)
          val _tmpDateEpochDay: Long
          _tmpDateEpochDay = _stmt.getLong(_columnIndexOfDateEpochDay)
          val _tmpSample: Boolean
          val _tmp: Int
          _tmp = _stmt.getLong(_columnIndexOfSample).toInt()
          _tmpSample = _tmp != 0
          val _tmpCreatedAtSeconds: Long
          _tmpCreatedAtSeconds = _stmt.getLong(_columnIndexOfCreatedAtSeconds)
          _item =
              LocalTransactionEntity(_tmpId,_tmpCategoryId,_tmpAmount,_tmpMerchantOrNote,_tmpDateEpochDay,_tmpSample,_tmpCreatedAtSeconds)
          _result.add(_item)
        }
        _result
      } finally {
        _stmt.close()
      }
    }
  }

  public override fun observeRecurringItems(): Flow<List<LocalRecurringEntity>> {
    val _sql: String =
        "SELECT * FROM local_recurring_items ORDER BY active DESC, nextDueEpochDay ASC, name ASC"
    return createFlow(__db, false, arrayOf("local_recurring_items")) { _connection ->
      val _stmt: SQLiteStatement = _connection.prepare(_sql)
      try {
        val _columnIndexOfId: Int = getColumnIndexOrThrow(_stmt, "id")
        val _columnIndexOfCategoryId: Int = getColumnIndexOrThrow(_stmt, "categoryId")
        val _columnIndexOfName: Int = getColumnIndexOrThrow(_stmt, "name")
        val _columnIndexOfAmount: Int = getColumnIndexOrThrow(_stmt, "amount")
        val _columnIndexOfKind: Int = getColumnIndexOrThrow(_stmt, "kind")
        val _columnIndexOfCadence: Int = getColumnIndexOrThrow(_stmt, "cadence")
        val _columnIndexOfNextDueEpochDay: Int = getColumnIndexOrThrow(_stmt, "nextDueEpochDay")
        val _columnIndexOfActive: Int = getColumnIndexOrThrow(_stmt, "active")
        val _columnIndexOfSample: Int = getColumnIndexOrThrow(_stmt, "sample")
        val _result: MutableList<LocalRecurringEntity> = mutableListOf()
        while (_stmt.step()) {
          val _item: LocalRecurringEntity
          val _tmpId: String
          _tmpId = _stmt.getText(_columnIndexOfId)
          val _tmpCategoryId: String
          _tmpCategoryId = _stmt.getText(_columnIndexOfCategoryId)
          val _tmpName: String
          _tmpName = _stmt.getText(_columnIndexOfName)
          val _tmpAmount: Double
          _tmpAmount = _stmt.getDouble(_columnIndexOfAmount)
          val _tmpKind: String
          _tmpKind = _stmt.getText(_columnIndexOfKind)
          val _tmpCadence: String
          _tmpCadence = _stmt.getText(_columnIndexOfCadence)
          val _tmpNextDueEpochDay: Long
          _tmpNextDueEpochDay = _stmt.getLong(_columnIndexOfNextDueEpochDay)
          val _tmpActive: Boolean
          val _tmp: Int
          _tmp = _stmt.getLong(_columnIndexOfActive).toInt()
          _tmpActive = _tmp != 0
          val _tmpSample: Boolean
          val _tmp_1: Int
          _tmp_1 = _stmt.getLong(_columnIndexOfSample).toInt()
          _tmpSample = _tmp_1 != 0
          _item =
              LocalRecurringEntity(_tmpId,_tmpCategoryId,_tmpName,_tmpAmount,_tmpKind,_tmpCadence,_tmpNextDueEpochDay,_tmpActive,_tmpSample)
          _result.add(_item)
        }
        _result
      } finally {
        _stmt.close()
      }
    }
  }

  public override suspend fun getRecurringItems(): List<LocalRecurringEntity> {
    val _sql: String =
        "SELECT * FROM local_recurring_items ORDER BY active DESC, nextDueEpochDay ASC, name ASC"
    return performSuspending(__db, true, false) { _connection ->
      val _stmt: SQLiteStatement = _connection.prepare(_sql)
      try {
        val _columnIndexOfId: Int = getColumnIndexOrThrow(_stmt, "id")
        val _columnIndexOfCategoryId: Int = getColumnIndexOrThrow(_stmt, "categoryId")
        val _columnIndexOfName: Int = getColumnIndexOrThrow(_stmt, "name")
        val _columnIndexOfAmount: Int = getColumnIndexOrThrow(_stmt, "amount")
        val _columnIndexOfKind: Int = getColumnIndexOrThrow(_stmt, "kind")
        val _columnIndexOfCadence: Int = getColumnIndexOrThrow(_stmt, "cadence")
        val _columnIndexOfNextDueEpochDay: Int = getColumnIndexOrThrow(_stmt, "nextDueEpochDay")
        val _columnIndexOfActive: Int = getColumnIndexOrThrow(_stmt, "active")
        val _columnIndexOfSample: Int = getColumnIndexOrThrow(_stmt, "sample")
        val _result: MutableList<LocalRecurringEntity> = mutableListOf()
        while (_stmt.step()) {
          val _item: LocalRecurringEntity
          val _tmpId: String
          _tmpId = _stmt.getText(_columnIndexOfId)
          val _tmpCategoryId: String
          _tmpCategoryId = _stmt.getText(_columnIndexOfCategoryId)
          val _tmpName: String
          _tmpName = _stmt.getText(_columnIndexOfName)
          val _tmpAmount: Double
          _tmpAmount = _stmt.getDouble(_columnIndexOfAmount)
          val _tmpKind: String
          _tmpKind = _stmt.getText(_columnIndexOfKind)
          val _tmpCadence: String
          _tmpCadence = _stmt.getText(_columnIndexOfCadence)
          val _tmpNextDueEpochDay: Long
          _tmpNextDueEpochDay = _stmt.getLong(_columnIndexOfNextDueEpochDay)
          val _tmpActive: Boolean
          val _tmp: Int
          _tmp = _stmt.getLong(_columnIndexOfActive).toInt()
          _tmpActive = _tmp != 0
          val _tmpSample: Boolean
          val _tmp_1: Int
          _tmp_1 = _stmt.getLong(_columnIndexOfSample).toInt()
          _tmpSample = _tmp_1 != 0
          _item =
              LocalRecurringEntity(_tmpId,_tmpCategoryId,_tmpName,_tmpAmount,_tmpKind,_tmpCadence,_tmpNextDueEpochDay,_tmpActive,_tmpSample)
          _result.add(_item)
        }
        _result
      } finally {
        _stmt.close()
      }
    }
  }

  public override suspend fun getAllMonthlyBudgetLimits(): List<MonthlyBudgetLimitEntity> {
    val _sql: String = "SELECT * FROM local_monthly_budget_limits ORDER BY periodKey ASC"
    return performSuspending(__db, true, false) { _connection ->
      val _stmt: SQLiteStatement = _connection.prepare(_sql)
      try {
        val _columnIndexOfPeriodKey: Int = getColumnIndexOrThrow(_stmt, "periodKey")
        val _columnIndexOfLimitAmount: Int = getColumnIndexOrThrow(_stmt, "limitAmount")
        val _columnIndexOfUpdatedAtMillis: Int = getColumnIndexOrThrow(_stmt, "updatedAtMillis")
        val _result: MutableList<MonthlyBudgetLimitEntity> = mutableListOf()
        while (_stmt.step()) {
          val _item: MonthlyBudgetLimitEntity
          val _tmpPeriodKey: String
          _tmpPeriodKey = _stmt.getText(_columnIndexOfPeriodKey)
          val _tmpLimitAmount: Double
          _tmpLimitAmount = _stmt.getDouble(_columnIndexOfLimitAmount)
          val _tmpUpdatedAtMillis: Long
          _tmpUpdatedAtMillis = _stmt.getLong(_columnIndexOfUpdatedAtMillis)
          _item = MonthlyBudgetLimitEntity(_tmpPeriodKey,_tmpLimitAmount,_tmpUpdatedAtMillis)
          _result.add(_item)
        }
        _result
      } finally {
        _stmt.close()
      }
    }
  }

  public override suspend fun updateCategory(
    id: String,
    name: String,
    iconKey: String,
    colorHex: Long,
    monthlyLimit: Double,
  ) {
    val _sql: String =
        "UPDATE local_categories SET name = ?, iconKey = ?, colorHex = ?, monthlyLimit = ?, sample = 0 WHERE id = ?"
    return performSuspending(__db, false, true) { _connection ->
      val _stmt: SQLiteStatement = _connection.prepare(_sql)
      try {
        var _argIndex: Int = 1
        _stmt.bindText(_argIndex, name)
        _argIndex = 2
        _stmt.bindText(_argIndex, iconKey)
        _argIndex = 3
        _stmt.bindLong(_argIndex, colorHex)
        _argIndex = 4
        _stmt.bindDouble(_argIndex, monthlyLimit)
        _argIndex = 5
        _stmt.bindText(_argIndex, id)
        _stmt.step()
      } finally {
        _stmt.close()
      }
    }
  }

  public override suspend fun updateCategoryLimit(id: String, newLimit: Double) {
    val _sql: String = "UPDATE local_categories SET monthlyLimit = ? WHERE id = ?"
    return performSuspending(__db, false, true) { _connection ->
      val _stmt: SQLiteStatement = _connection.prepare(_sql)
      try {
        var _argIndex: Int = 1
        _stmt.bindDouble(_argIndex, newLimit)
        _argIndex = 2
        _stmt.bindText(_argIndex, id)
        _stmt.step()
      } finally {
        _stmt.close()
      }
    }
  }

  public override suspend fun deleteCategory(id: String) {
    val _sql: String = "DELETE FROM local_categories WHERE id = ?"
    return performSuspending(__db, false, true) { _connection ->
      val _stmt: SQLiteStatement = _connection.prepare(_sql)
      try {
        var _argIndex: Int = 1
        _stmt.bindText(_argIndex, id)
        _stmt.step()
      } finally {
        _stmt.close()
      }
    }
  }

  public override suspend fun updateTransaction(
    id: String,
    categoryId: String,
    amount: Double,
    merchantOrNote: String,
    dateEpochDay: Long,
  ) {
    val _sql: String =
        "UPDATE local_transactions SET categoryId = ?, amount = ?, merchantOrNote = ?, dateEpochDay = ?, sample = 0 WHERE id = ?"
    return performSuspending(__db, false, true) { _connection ->
      val _stmt: SQLiteStatement = _connection.prepare(_sql)
      try {
        var _argIndex: Int = 1
        _stmt.bindText(_argIndex, categoryId)
        _argIndex = 2
        _stmt.bindDouble(_argIndex, amount)
        _argIndex = 3
        _stmt.bindText(_argIndex, merchantOrNote)
        _argIndex = 4
        _stmt.bindLong(_argIndex, dateEpochDay)
        _argIndex = 5
        _stmt.bindText(_argIndex, id)
        _stmt.step()
      } finally {
        _stmt.close()
      }
    }
  }

  public override suspend fun reassignTransactions(oldCategoryId: String,
      fallbackCategoryId: String) {
    val _sql: String = "UPDATE local_transactions SET categoryId = ? WHERE categoryId = ?"
    return performSuspending(__db, false, true) { _connection ->
      val _stmt: SQLiteStatement = _connection.prepare(_sql)
      try {
        var _argIndex: Int = 1
        _stmt.bindText(_argIndex, fallbackCategoryId)
        _argIndex = 2
        _stmt.bindText(_argIndex, oldCategoryId)
        _stmt.step()
      } finally {
        _stmt.close()
      }
    }
  }

  public override suspend fun deleteTransaction(id: String) {
    val _sql: String = "DELETE FROM local_transactions WHERE id = ?"
    return performSuspending(__db, false, true) { _connection ->
      val _stmt: SQLiteStatement = _connection.prepare(_sql)
      try {
        var _argIndex: Int = 1
        _stmt.bindText(_argIndex, id)
        _stmt.step()
      } finally {
        _stmt.close()
      }
    }
  }

  public override suspend fun deleteSampleTransactions() {
    val _sql: String = "DELETE FROM local_transactions WHERE sample = 1"
    return performSuspending(__db, false, true) { _connection ->
      val _stmt: SQLiteStatement = _connection.prepare(_sql)
      try {
        _stmt.step()
      } finally {
        _stmt.close()
      }
    }
  }

  public override suspend fun deleteAllTransactions() {
    val _sql: String = "DELETE FROM local_transactions"
    return performSuspending(__db, false, true) { _connection ->
      val _stmt: SQLiteStatement = _connection.prepare(_sql)
      try {
        _stmt.step()
      } finally {
        _stmt.close()
      }
    }
  }

  public override suspend fun updateRecurringItem(
    id: String,
    categoryId: String,
    name: String,
    amount: Double,
    kind: String,
    cadence: String,
    nextDueEpochDay: Long,
    active: Boolean,
  ) {
    val _sql: String =
        "UPDATE local_recurring_items SET categoryId = ?, name = ?, amount = ?, kind = ?, cadence = ?, nextDueEpochDay = ?, active = ?, sample = 0 WHERE id = ?"
    return performSuspending(__db, false, true) { _connection ->
      val _stmt: SQLiteStatement = _connection.prepare(_sql)
      try {
        var _argIndex: Int = 1
        _stmt.bindText(_argIndex, categoryId)
        _argIndex = 2
        _stmt.bindText(_argIndex, name)
        _argIndex = 3
        _stmt.bindDouble(_argIndex, amount)
        _argIndex = 4
        _stmt.bindText(_argIndex, kind)
        _argIndex = 5
        _stmt.bindText(_argIndex, cadence)
        _argIndex = 6
        _stmt.bindLong(_argIndex, nextDueEpochDay)
        _argIndex = 7
        val _tmp: Int = if (active) 1 else 0
        _stmt.bindLong(_argIndex, _tmp.toLong())
        _argIndex = 8
        _stmt.bindText(_argIndex, id)
        _stmt.step()
      } finally {
        _stmt.close()
      }
    }
  }

  public override suspend fun setRecurringActive(id: String, active: Boolean) {
    val _sql: String = "UPDATE local_recurring_items SET active = ? WHERE id = ?"
    return performSuspending(__db, false, true) { _connection ->
      val _stmt: SQLiteStatement = _connection.prepare(_sql)
      try {
        var _argIndex: Int = 1
        val _tmp: Int = if (active) 1 else 0
        _stmt.bindLong(_argIndex, _tmp.toLong())
        _argIndex = 2
        _stmt.bindText(_argIndex, id)
        _stmt.step()
      } finally {
        _stmt.close()
      }
    }
  }

  public override suspend fun advanceRecurringDueDate(id: String, nextDueEpochDay: Long) {
    val _sql: String = "UPDATE local_recurring_items SET nextDueEpochDay = ? WHERE id = ?"
    return performSuspending(__db, false, true) { _connection ->
      val _stmt: SQLiteStatement = _connection.prepare(_sql)
      try {
        var _argIndex: Int = 1
        _stmt.bindLong(_argIndex, nextDueEpochDay)
        _argIndex = 2
        _stmt.bindText(_argIndex, id)
        _stmt.step()
      } finally {
        _stmt.close()
      }
    }
  }

  public override suspend fun reassignRecurringItems(oldCategoryId: String,
      fallbackCategoryId: String) {
    val _sql: String = "UPDATE local_recurring_items SET categoryId = ? WHERE categoryId = ?"
    return performSuspending(__db, false, true) { _connection ->
      val _stmt: SQLiteStatement = _connection.prepare(_sql)
      try {
        var _argIndex: Int = 1
        _stmt.bindText(_argIndex, fallbackCategoryId)
        _argIndex = 2
        _stmt.bindText(_argIndex, oldCategoryId)
        _stmt.step()
      } finally {
        _stmt.close()
      }
    }
  }

  public override suspend fun deleteRecurringItem(id: String) {
    val _sql: String = "DELETE FROM local_recurring_items WHERE id = ?"
    return performSuspending(__db, false, true) { _connection ->
      val _stmt: SQLiteStatement = _connection.prepare(_sql)
      try {
        var _argIndex: Int = 1
        _stmt.bindText(_argIndex, id)
        _stmt.step()
      } finally {
        _stmt.close()
      }
    }
  }

  public override suspend fun deleteSampleRecurringItems() {
    val _sql: String = "DELETE FROM local_recurring_items WHERE sample = 1"
    return performSuspending(__db, false, true) { _connection ->
      val _stmt: SQLiteStatement = _connection.prepare(_sql)
      try {
        _stmt.step()
      } finally {
        _stmt.close()
      }
    }
  }

  public override suspend fun deleteAllCategories() {
    val _sql: String = "DELETE FROM local_categories"
    return performSuspending(__db, false, true) { _connection ->
      val _stmt: SQLiteStatement = _connection.prepare(_sql)
      try {
        _stmt.step()
      } finally {
        _stmt.close()
      }
    }
  }

  public override suspend fun deleteAllRecurringItems() {
    val _sql: String = "DELETE FROM local_recurring_items"
    return performSuspending(__db, false, true) { _connection ->
      val _stmt: SQLiteStatement = _connection.prepare(_sql)
      try {
        _stmt.step()
      } finally {
        _stmt.close()
      }
    }
  }

  public override suspend fun deleteAllMonthlyBudgetLimits() {
    val _sql: String = "DELETE FROM local_monthly_budget_limits"
    return performSuspending(__db, false, true) { _connection ->
      val _stmt: SQLiteStatement = _connection.prepare(_sql)
      try {
        _stmt.step()
      } finally {
        _stmt.close()
      }
    }
  }

  public companion object {
    public fun getRequiredConverters(): List<KClass<*>> = emptyList()
  }
}
