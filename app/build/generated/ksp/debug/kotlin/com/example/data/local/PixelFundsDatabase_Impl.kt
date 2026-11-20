package com.example.`data`.local

import androidx.room.InvalidationTracker
import androidx.room.RoomOpenDelegate
import androidx.room.migration.AutoMigrationSpec
import androidx.room.migration.Migration
import androidx.room.util.TableInfo
import androidx.room.util.TableInfo.Companion.read
import androidx.room.util.dropFtsSyncTriggers
import androidx.sqlite.SQLiteConnection
import androidx.sqlite.execSQL
import javax.`annotation`.processing.Generated
import kotlin.Lazy
import kotlin.String
import kotlin.Suppress
import kotlin.collections.List
import kotlin.collections.Map
import kotlin.collections.MutableList
import kotlin.collections.MutableMap
import kotlin.collections.MutableSet
import kotlin.collections.Set
import kotlin.collections.mutableListOf
import kotlin.collections.mutableMapOf
import kotlin.collections.mutableSetOf
import kotlin.reflect.KClass

@Generated(value = ["androidx.room.RoomProcessor"])
@Suppress(names = ["UNCHECKED_CAST", "DEPRECATION", "REDUNDANT_PROJECTION", "REMOVAL"])
public class PixelFundsDatabase_Impl : PixelFundsDatabase() {
  private val _pixelFundsDao: Lazy<PixelFundsDao> = lazy {
    PixelFundsDao_Impl(this)
  }

  private val _monthlyBudgetOverviewDao: Lazy<MonthlyBudgetOverviewDao> = lazy {
    MonthlyBudgetOverviewDao_Impl(this)
  }

  protected override fun createOpenDelegate(): RoomOpenDelegate {
    val _openDelegate: RoomOpenDelegate = object : RoomOpenDelegate(3,
        "4d182fff1427873af74eabda8967d98b", "4ea3ee1cef58712103d2fe3e60af19d7") {
      public override fun createAllTables(connection: SQLiteConnection) {
        connection.execSQL("CREATE TABLE IF NOT EXISTS `local_workspace` (`id` INTEGER NOT NULL, `seeded` INTEGER NOT NULL, `hasSampleData` INTEGER NOT NULL, PRIMARY KEY(`id`))")
        connection.execSQL("CREATE TABLE IF NOT EXISTS `local_categories` (`id` TEXT NOT NULL, `name` TEXT NOT NULL, `iconKey` TEXT NOT NULL, `colorHex` INTEGER NOT NULL, `monthlyLimit` REAL NOT NULL, `sortOrder` INTEGER NOT NULL, `sample` INTEGER NOT NULL, PRIMARY KEY(`id`))")
        connection.execSQL("CREATE TABLE IF NOT EXISTS `local_transactions` (`id` TEXT NOT NULL, `categoryId` TEXT NOT NULL, `amount` REAL NOT NULL, `merchantOrNote` TEXT NOT NULL, `dateEpochDay` INTEGER NOT NULL, `sample` INTEGER NOT NULL, `createdAtSeconds` INTEGER NOT NULL, PRIMARY KEY(`id`))")
        connection.execSQL("CREATE TABLE IF NOT EXISTS `local_recurring_items` (`id` TEXT NOT NULL, `categoryId` TEXT NOT NULL, `name` TEXT NOT NULL, `amount` REAL NOT NULL, `kind` TEXT NOT NULL, `cadence` TEXT NOT NULL, `nextDueEpochDay` INTEGER NOT NULL, `active` INTEGER NOT NULL, `sample` INTEGER NOT NULL, PRIMARY KEY(`id`))")
        connection.execSQL("CREATE TABLE IF NOT EXISTS `local_monthly_budget_limits` (`periodKey` TEXT NOT NULL, `limitAmount` REAL NOT NULL, `updatedAtMillis` INTEGER NOT NULL, PRIMARY KEY(`periodKey`))")
        connection.execSQL("CREATE TABLE IF NOT EXISTS room_master_table (id INTEGER PRIMARY KEY,identity_hash TEXT)")
        connection.execSQL("INSERT OR REPLACE INTO room_master_table (id,identity_hash) VALUES(42, '4d182fff1427873af74eabda8967d98b')")
      }

      public override fun dropAllTables(connection: SQLiteConnection) {
        connection.execSQL("DROP TABLE IF EXISTS `local_workspace`")
        connection.execSQL("DROP TABLE IF EXISTS `local_categories`")
        connection.execSQL("DROP TABLE IF EXISTS `local_transactions`")
        connection.execSQL("DROP TABLE IF EXISTS `local_recurring_items`")
        connection.execSQL("DROP TABLE IF EXISTS `local_monthly_budget_limits`")
      }

      public override fun onCreate(connection: SQLiteConnection) {
      }

      public override fun onOpen(connection: SQLiteConnection) {
        internalInitInvalidationTracker(connection)
      }

      public override fun onPreMigrate(connection: SQLiteConnection) {
        dropFtsSyncTriggers(connection)
      }

      public override fun onPostMigrate(connection: SQLiteConnection) {
      }

      public override fun onValidateSchema(connection: SQLiteConnection):
          RoomOpenDelegate.ValidationResult {
        val _columnsLocalWorkspace: MutableMap<String, TableInfo.Column> = mutableMapOf()
        _columnsLocalWorkspace.put("id", TableInfo.Column("id", "INTEGER", true, 1, null,
            TableInfo.CREATED_FROM_ENTITY))
        _columnsLocalWorkspace.put("seeded", TableInfo.Column("seeded", "INTEGER", true, 0, null,
            TableInfo.CREATED_FROM_ENTITY))
        _columnsLocalWorkspace.put("hasSampleData", TableInfo.Column("hasSampleData", "INTEGER",
            true, 0, null, TableInfo.CREATED_FROM_ENTITY))
        val _foreignKeysLocalWorkspace: MutableSet<TableInfo.ForeignKey> = mutableSetOf()
        val _indicesLocalWorkspace: MutableSet<TableInfo.Index> = mutableSetOf()
        val _infoLocalWorkspace: TableInfo = TableInfo("local_workspace", _columnsLocalWorkspace,
            _foreignKeysLocalWorkspace, _indicesLocalWorkspace)
        val _existingLocalWorkspace: TableInfo = read(connection, "local_workspace")
        if (!_infoLocalWorkspace.equals(_existingLocalWorkspace)) {
          return RoomOpenDelegate.ValidationResult(false, """
              |local_workspace(com.example.data.local.LocalWorkspaceEntity).
              | Expected:
              |""".trimMargin() + _infoLocalWorkspace + """
              |
              | Found:
              |""".trimMargin() + _existingLocalWorkspace)
        }
        val _columnsLocalCategories: MutableMap<String, TableInfo.Column> = mutableMapOf()
        _columnsLocalCategories.put("id", TableInfo.Column("id", "TEXT", true, 1, null,
            TableInfo.CREATED_FROM_ENTITY))
        _columnsLocalCategories.put("name", TableInfo.Column("name", "TEXT", true, 0, null,
            TableInfo.CREATED_FROM_ENTITY))
        _columnsLocalCategories.put("iconKey", TableInfo.Column("iconKey", "TEXT", true, 0, null,
            TableInfo.CREATED_FROM_ENTITY))
        _columnsLocalCategories.put("colorHex", TableInfo.Column("colorHex", "INTEGER", true, 0,
            null, TableInfo.CREATED_FROM_ENTITY))
        _columnsLocalCategories.put("monthlyLimit", TableInfo.Column("monthlyLimit", "REAL", true,
            0, null, TableInfo.CREATED_FROM_ENTITY))
        _columnsLocalCategories.put("sortOrder", TableInfo.Column("sortOrder", "INTEGER", true, 0,
            null, TableInfo.CREATED_FROM_ENTITY))
        _columnsLocalCategories.put("sample", TableInfo.Column("sample", "INTEGER", true, 0, null,
            TableInfo.CREATED_FROM_ENTITY))
        val _foreignKeysLocalCategories: MutableSet<TableInfo.ForeignKey> = mutableSetOf()
        val _indicesLocalCategories: MutableSet<TableInfo.Index> = mutableSetOf()
        val _infoLocalCategories: TableInfo = TableInfo("local_categories", _columnsLocalCategories,
            _foreignKeysLocalCategories, _indicesLocalCategories)
        val _existingLocalCategories: TableInfo = read(connection, "local_categories")
        if (!_infoLocalCategories.equals(_existingLocalCategories)) {
          return RoomOpenDelegate.ValidationResult(false, """
              |local_categories(com.example.data.local.LocalCategoryEntity).
              | Expected:
              |""".trimMargin() + _infoLocalCategories + """
              |
              | Found:
              |""".trimMargin() + _existingLocalCategories)
        }
        val _columnsLocalTransactions: MutableMap<String, TableInfo.Column> = mutableMapOf()
        _columnsLocalTransactions.put("id", TableInfo.Column("id", "TEXT", true, 1, null,
            TableInfo.CREATED_FROM_ENTITY))
        _columnsLocalTransactions.put("categoryId", TableInfo.Column("categoryId", "TEXT", true, 0,
            null, TableInfo.CREATED_FROM_ENTITY))
        _columnsLocalTransactions.put("amount", TableInfo.Column("amount", "REAL", true, 0, null,
            TableInfo.CREATED_FROM_ENTITY))
        _columnsLocalTransactions.put("merchantOrNote", TableInfo.Column("merchantOrNote", "TEXT",
            true, 0, null, TableInfo.CREATED_FROM_ENTITY))
        _columnsLocalTransactions.put("dateEpochDay", TableInfo.Column("dateEpochDay", "INTEGER",
            true, 0, null, TableInfo.CREATED_FROM_ENTITY))
        _columnsLocalTransactions.put("sample", TableInfo.Column("sample", "INTEGER", true, 0, null,
            TableInfo.CREATED_FROM_ENTITY))
        _columnsLocalTransactions.put("createdAtSeconds", TableInfo.Column("createdAtSeconds",
            "INTEGER", true, 0, null, TableInfo.CREATED_FROM_ENTITY))
        val _foreignKeysLocalTransactions: MutableSet<TableInfo.ForeignKey> = mutableSetOf()
        val _indicesLocalTransactions: MutableSet<TableInfo.Index> = mutableSetOf()
        val _infoLocalTransactions: TableInfo = TableInfo("local_transactions",
            _columnsLocalTransactions, _foreignKeysLocalTransactions, _indicesLocalTransactions)
        val _existingLocalTransactions: TableInfo = read(connection, "local_transactions")
        if (!_infoLocalTransactions.equals(_existingLocalTransactions)) {
          return RoomOpenDelegate.ValidationResult(false, """
              |local_transactions(com.example.data.local.LocalTransactionEntity).
              | Expected:
              |""".trimMargin() + _infoLocalTransactions + """
              |
              | Found:
              |""".trimMargin() + _existingLocalTransactions)
        }
        val _columnsLocalRecurringItems: MutableMap<String, TableInfo.Column> = mutableMapOf()
        _columnsLocalRecurringItems.put("id", TableInfo.Column("id", "TEXT", true, 1, null,
            TableInfo.CREATED_FROM_ENTITY))
        _columnsLocalRecurringItems.put("categoryId", TableInfo.Column("categoryId", "TEXT", true,
            0, null, TableInfo.CREATED_FROM_ENTITY))
        _columnsLocalRecurringItems.put("name", TableInfo.Column("name", "TEXT", true, 0, null,
            TableInfo.CREATED_FROM_ENTITY))
        _columnsLocalRecurringItems.put("amount", TableInfo.Column("amount", "REAL", true, 0, null,
            TableInfo.CREATED_FROM_ENTITY))
        _columnsLocalRecurringItems.put("kind", TableInfo.Column("kind", "TEXT", true, 0, null,
            TableInfo.CREATED_FROM_ENTITY))
        _columnsLocalRecurringItems.put("cadence", TableInfo.Column("cadence", "TEXT", true, 0,
            null, TableInfo.CREATED_FROM_ENTITY))
        _columnsLocalRecurringItems.put("nextDueEpochDay", TableInfo.Column("nextDueEpochDay",
            "INTEGER", true, 0, null, TableInfo.CREATED_FROM_ENTITY))
        _columnsLocalRecurringItems.put("active", TableInfo.Column("active", "INTEGER", true, 0,
            null, TableInfo.CREATED_FROM_ENTITY))
        _columnsLocalRecurringItems.put("sample", TableInfo.Column("sample", "INTEGER", true, 0,
            null, TableInfo.CREATED_FROM_ENTITY))
        val _foreignKeysLocalRecurringItems: MutableSet<TableInfo.ForeignKey> = mutableSetOf()
        val _indicesLocalRecurringItems: MutableSet<TableInfo.Index> = mutableSetOf()
        val _infoLocalRecurringItems: TableInfo = TableInfo("local_recurring_items",
            _columnsLocalRecurringItems, _foreignKeysLocalRecurringItems,
            _indicesLocalRecurringItems)
        val _existingLocalRecurringItems: TableInfo = read(connection, "local_recurring_items")
        if (!_infoLocalRecurringItems.equals(_existingLocalRecurringItems)) {
          return RoomOpenDelegate.ValidationResult(false, """
              |local_recurring_items(com.example.data.local.LocalRecurringEntity).
              | Expected:
              |""".trimMargin() + _infoLocalRecurringItems + """
              |
              | Found:
              |""".trimMargin() + _existingLocalRecurringItems)
        }
        val _columnsLocalMonthlyBudgetLimits: MutableMap<String, TableInfo.Column> = mutableMapOf()
        _columnsLocalMonthlyBudgetLimits.put("periodKey", TableInfo.Column("periodKey", "TEXT",
            true, 1, null, TableInfo.CREATED_FROM_ENTITY))
        _columnsLocalMonthlyBudgetLimits.put("limitAmount", TableInfo.Column("limitAmount", "REAL",
            true, 0, null, TableInfo.CREATED_FROM_ENTITY))
        _columnsLocalMonthlyBudgetLimits.put("updatedAtMillis", TableInfo.Column("updatedAtMillis",
            "INTEGER", true, 0, null, TableInfo.CREATED_FROM_ENTITY))
        val _foreignKeysLocalMonthlyBudgetLimits: MutableSet<TableInfo.ForeignKey> = mutableSetOf()
        val _indicesLocalMonthlyBudgetLimits: MutableSet<TableInfo.Index> = mutableSetOf()
        val _infoLocalMonthlyBudgetLimits: TableInfo = TableInfo("local_monthly_budget_limits",
            _columnsLocalMonthlyBudgetLimits, _foreignKeysLocalMonthlyBudgetLimits,
            _indicesLocalMonthlyBudgetLimits)
        val _existingLocalMonthlyBudgetLimits: TableInfo = read(connection,
            "local_monthly_budget_limits")
        if (!_infoLocalMonthlyBudgetLimits.equals(_existingLocalMonthlyBudgetLimits)) {
          return RoomOpenDelegate.ValidationResult(false, """
              |local_monthly_budget_limits(com.example.data.local.MonthlyBudgetLimitEntity).
              | Expected:
              |""".trimMargin() + _infoLocalMonthlyBudgetLimits + """
              |
              | Found:
              |""".trimMargin() + _existingLocalMonthlyBudgetLimits)
        }
        return RoomOpenDelegate.ValidationResult(true, null)
      }
    }
    return _openDelegate
  }

  protected override fun createInvalidationTracker(): InvalidationTracker {
    val _shadowTablesMap: MutableMap<String, String> = mutableMapOf()
    val _viewTables: MutableMap<String, Set<String>> = mutableMapOf()
    return InvalidationTracker(this, _shadowTablesMap, _viewTables, "local_workspace",
        "local_categories", "local_transactions", "local_recurring_items",
        "local_monthly_budget_limits")
  }

  public override fun clearAllTables() {
    super.performClear(false, "local_workspace", "local_categories", "local_transactions",
        "local_recurring_items", "local_monthly_budget_limits")
  }

  protected override fun getRequiredTypeConverterClasses(): Map<KClass<*>, List<KClass<*>>> {
    val _typeConvertersMap: MutableMap<KClass<*>, List<KClass<*>>> = mutableMapOf()
    _typeConvertersMap.put(PixelFundsDao::class, PixelFundsDao_Impl.getRequiredConverters())
    _typeConvertersMap.put(MonthlyBudgetOverviewDao::class,
        MonthlyBudgetOverviewDao_Impl.getRequiredConverters())
    return _typeConvertersMap
  }

  public override fun getRequiredAutoMigrationSpecClasses(): Set<KClass<out AutoMigrationSpec>> {
    val _autoMigrationSpecsSet: MutableSet<KClass<out AutoMigrationSpec>> = mutableSetOf()
    return _autoMigrationSpecsSet
  }

  public override
      fun createAutoMigrations(autoMigrationSpecs: Map<KClass<out AutoMigrationSpec>, AutoMigrationSpec>):
      List<Migration> {
    val _autoMigrations: MutableList<Migration> = mutableListOf()
    return _autoMigrations
  }

  public override fun dao(): PixelFundsDao = _pixelFundsDao.value

  public override fun monthlyBudgetOverviewDao(): MonthlyBudgetOverviewDao =
      _monthlyBudgetOverviewDao.value
}
