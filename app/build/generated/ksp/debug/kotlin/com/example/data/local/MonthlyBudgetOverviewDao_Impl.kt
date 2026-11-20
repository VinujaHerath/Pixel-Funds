package com.example.`data`.local

import androidx.room.EntityInsertAdapter
import androidx.room.RoomDatabase
import androidx.room.coroutines.createFlow
import androidx.room.util.getColumnIndexOrThrow
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
public class MonthlyBudgetOverviewDao_Impl(
  __db: RoomDatabase,
) : MonthlyBudgetOverviewDao {
  private val __db: RoomDatabase

  private val __insertAdapterOfMonthlyBudgetLimitEntity:
      EntityInsertAdapter<MonthlyBudgetLimitEntity>
  init {
    this.__db = __db
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

  public override suspend fun upsertMonthlyBudgetLimit(entity: MonthlyBudgetLimitEntity): Unit =
      performSuspending(__db, false, true) { _connection ->
    __insertAdapterOfMonthlyBudgetLimitEntity.insert(_connection, entity)
  }

  public override fun observeMonthlySpendingSummary(startEpochDay: Long, endEpochDay: Long):
      Flow<MonthlySpendingSummaryRow> {
    val _sql: String = """
        |
        |        SELECT
        |            COALESCE(SUM(amount), 0.0) AS totalSpent,
        |            COUNT(*) AS transactionCount,
        |            COALESCE(MAX(amount), 0.0) AS largestExpense
        |        FROM local_transactions
        |        WHERE dateEpochDay >= ? AND dateEpochDay <= ?
        |        
        """.trimMargin()
    return createFlow(__db, false, arrayOf("local_transactions")) { _connection ->
      val _stmt: SQLiteStatement = _connection.prepare(_sql)
      try {
        var _argIndex: Int = 1
        _stmt.bindLong(_argIndex, startEpochDay)
        _argIndex = 2
        _stmt.bindLong(_argIndex, endEpochDay)
        val _columnIndexOfTotalSpent: Int = 0
        val _columnIndexOfTransactionCount: Int = 1
        val _columnIndexOfLargestExpense: Int = 2
        val _result: MonthlySpendingSummaryRow
        if (_stmt.step()) {
          val _tmpTotalSpent: Double
          _tmpTotalSpent = _stmt.getDouble(_columnIndexOfTotalSpent)
          val _tmpTransactionCount: Int
          _tmpTransactionCount = _stmt.getLong(_columnIndexOfTransactionCount).toInt()
          val _tmpLargestExpense: Double
          _tmpLargestExpense = _stmt.getDouble(_columnIndexOfLargestExpense)
          _result =
              MonthlySpendingSummaryRow(_tmpTotalSpent,_tmpTransactionCount,_tmpLargestExpense)
        } else {
          error("The query result was empty, but expected a single row to return a NON-NULL object of type <com.example.`data`.local.MonthlySpendingSummaryRow>.")
        }
        _result
      } finally {
        _stmt.close()
      }
    }
  }

  public override fun observeCategoryMonthlySpending(startEpochDay: Long, endEpochDay: Long):
      Flow<List<CategoryMonthlySpendRow>> {
    val _sql: String = """
        |
        |        SELECT
        |            c.id AS categoryId,
        |            c.name AS categoryName,
        |            c.iconKey AS iconKey,
        |            c.colorHex AS colorHex,
        |            c.monthlyLimit AS categoryLimit,
        |            COALESCE(SUM(t.amount), 0.0) AS spentAmount,
        |            COUNT(t.id) AS transactionCount
        |        FROM local_categories c
        |        LEFT JOIN local_transactions t
        |            ON c.id = t.categoryId
        |            AND t.dateEpochDay >= ?
        |            AND t.dateEpochDay <= ?
        |        GROUP BY c.id
        |        ORDER BY spentAmount DESC, c.sortOrder ASC, c.name ASC
        |        
        """.trimMargin()
    return createFlow(__db, false, arrayOf("local_categories", "local_transactions")) {
        _connection ->
      val _stmt: SQLiteStatement = _connection.prepare(_sql)
      try {
        var _argIndex: Int = 1
        _stmt.bindLong(_argIndex, startEpochDay)
        _argIndex = 2
        _stmt.bindLong(_argIndex, endEpochDay)
        val _columnIndexOfCategoryId: Int = 0
        val _columnIndexOfCategoryName: Int = 1
        val _columnIndexOfIconKey: Int = 2
        val _columnIndexOfColorHex: Int = 3
        val _columnIndexOfCategoryLimit: Int = 4
        val _columnIndexOfSpentAmount: Int = 5
        val _columnIndexOfTransactionCount: Int = 6
        val _result: MutableList<CategoryMonthlySpendRow> = mutableListOf()
        while (_stmt.step()) {
          val _item: CategoryMonthlySpendRow
          val _tmpCategoryId: String
          _tmpCategoryId = _stmt.getText(_columnIndexOfCategoryId)
          val _tmpCategoryName: String
          _tmpCategoryName = _stmt.getText(_columnIndexOfCategoryName)
          val _tmpIconKey: String
          _tmpIconKey = _stmt.getText(_columnIndexOfIconKey)
          val _tmpColorHex: Long
          _tmpColorHex = _stmt.getLong(_columnIndexOfColorHex)
          val _tmpCategoryLimit: Double
          _tmpCategoryLimit = _stmt.getDouble(_columnIndexOfCategoryLimit)
          val _tmpSpentAmount: Double
          _tmpSpentAmount = _stmt.getDouble(_columnIndexOfSpentAmount)
          val _tmpTransactionCount: Int
          _tmpTransactionCount = _stmt.getLong(_columnIndexOfTransactionCount).toInt()
          _item =
              CategoryMonthlySpendRow(_tmpCategoryId,_tmpCategoryName,_tmpIconKey,_tmpColorHex,_tmpCategoryLimit,_tmpSpentAmount,_tmpTransactionCount)
          _result.add(_item)
        }
        _result
      } finally {
        _stmt.close()
      }
    }
  }

  public override fun observeTotalCategoryLimits(): Flow<Double> {
    val _sql: String = "SELECT COALESCE(SUM(monthlyLimit), 0.0) FROM local_categories"
    return createFlow(__db, false, arrayOf("local_categories")) { _connection ->
      val _stmt: SQLiteStatement = _connection.prepare(_sql)
      try {
        val _result: Double
        if (_stmt.step()) {
          val _tmp: Double
          _tmp = _stmt.getDouble(0)
          _result = _tmp
        } else {
          _result = 0.0
        }
        _result
      } finally {
        _stmt.close()
      }
    }
  }

  public override fun observeMonthlyBudgetLimit(periodKey: String):
      Flow<MonthlyBudgetLimitEntity?> {
    val _sql: String = "SELECT * FROM local_monthly_budget_limits WHERE periodKey = ? LIMIT 1"
    return createFlow(__db, false, arrayOf("local_monthly_budget_limits")) { _connection ->
      val _stmt: SQLiteStatement = _connection.prepare(_sql)
      try {
        var _argIndex: Int = 1
        _stmt.bindText(_argIndex, periodKey)
        val _columnIndexOfPeriodKey: Int = getColumnIndexOrThrow(_stmt, "periodKey")
        val _columnIndexOfLimitAmount: Int = getColumnIndexOrThrow(_stmt, "limitAmount")
        val _columnIndexOfUpdatedAtMillis: Int = getColumnIndexOrThrow(_stmt, "updatedAtMillis")
        val _result: MonthlyBudgetLimitEntity?
        if (_stmt.step()) {
          val _tmpPeriodKey: String
          _tmpPeriodKey = _stmt.getText(_columnIndexOfPeriodKey)
          val _tmpLimitAmount: Double
          _tmpLimitAmount = _stmt.getDouble(_columnIndexOfLimitAmount)
          val _tmpUpdatedAtMillis: Long
          _tmpUpdatedAtMillis = _stmt.getLong(_columnIndexOfUpdatedAtMillis)
          _result = MonthlyBudgetLimitEntity(_tmpPeriodKey,_tmpLimitAmount,_tmpUpdatedAtMillis)
        } else {
          _result = null
        }
        _result
      } finally {
        _stmt.close()
      }
    }
  }

  public override fun observeRecentTransactionsForPeriod(
    startEpochDay: Long,
    endEpochDay: Long,
    limit: Int,
  ): Flow<List<LocalTransactionEntity>> {
    val _sql: String = """
        |
        |        SELECT * FROM local_transactions
        |        WHERE dateEpochDay >= ? AND dateEpochDay <= ?
        |        ORDER BY dateEpochDay DESC, createdAtSeconds DESC
        |        LIMIT ?
        |        
        """.trimMargin()
    return createFlow(__db, false, arrayOf("local_transactions")) { _connection ->
      val _stmt: SQLiteStatement = _connection.prepare(_sql)
      try {
        var _argIndex: Int = 1
        _stmt.bindLong(_argIndex, startEpochDay)
        _argIndex = 2
        _stmt.bindLong(_argIndex, endEpochDay)
        _argIndex = 3
        _stmt.bindLong(_argIndex, limit.toLong())
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

  public override suspend fun clearMonthlyBudgetLimit(periodKey: String) {
    val _sql: String = "DELETE FROM local_monthly_budget_limits WHERE periodKey = ?"
    return performSuspending(__db, false, true) { _connection ->
      val _stmt: SQLiteStatement = _connection.prepare(_sql)
      try {
        var _argIndex: Int = 1
        _stmt.bindText(_argIndex, periodKey)
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
