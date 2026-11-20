package com.example

import android.content.Context
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.assertTextContains
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.compose.ui.test.onAllNodesWithTag
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performTextClearance
import androidx.compose.ui.test.performTextInput
import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import com.example.data.AppThemeMode
import com.example.data.BillingCadence
import com.example.data.LocalDay
import com.example.data.RecurringKind
import com.example.data.UserPreferences
import com.example.data.WorldCurrencyCatalog
import com.example.data.local.LocalBackupCodec
import com.example.data.local.LocalBudgetRepository
import com.example.data.local.LocalCategoryEntity
import com.example.data.local.LocalRecurringEntity
import com.example.data.local.LocalTransactionEntity
import com.example.data.local.MonthlyBudgetLimitEntity
import com.example.data.local.MonthlyBudgetOverviewRepository
import com.example.data.local.ParsedImportPayload
import com.example.data.local.PixelFundsDatabase
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [35])
class ExampleRobolectricTest {

    @get:Rule
    val composeRule = createAndroidComposeRule<MainActivity>()

    @Test
    fun appNameMatchesPixelFunds() {
        val context = ApplicationProvider.getApplicationContext<Context>()
        assertEquals("Pixel Funds", context.getString(R.string.app_name))
    }

    @Test
    fun mainActivityStartsWithOnboardingTutorialAndRendersPrimaryFlows() {
        // First launch presents Onboarding flow
        composeRule.waitUntil(timeoutMillis = 10_000) {
            composeRule.onAllNodesWithTag("onboarding_screen").fetchSemanticsNodes().isNotEmpty()
        }
        composeRule.onNodeWithTag("onboarding_screen").assertIsDisplayed()

        // Step through the 3 intro slides to Quick Setup, then launch the guided tour
        repeat(3) {
            composeRule.onNodeWithTag("onboarding_next_button").performClick()
            composeRule.waitForIdle()
        }
        composeRule.waitUntil(timeoutMillis = 5_000) {
            composeRule.onAllNodesWithTag("onboarding_start_tour_button").fetchSemanticsNodes().isNotEmpty()
        }
        composeRule.onNodeWithTag("onboarding_start_tour_button").performClick()
        composeRule.waitForIdle()

        // Step through the 6-step interactive tutorial
        composeRule.waitUntil(timeoutMillis = 5_000) {
            composeRule.onAllNodesWithTag("tutorial_overlay").fetchSemanticsNodes().isNotEmpty()
        }
        composeRule.onNodeWithTag("tutorial_overlay").assertIsDisplayed()
        repeat(5) {
            composeRule.onNodeWithTag("tutorial_next_button").performClick()
            composeRule.waitForIdle()
        }
        composeRule.onNodeWithTag("tutorial_done_button").performClick()
        composeRule.waitForIdle()

        // Return to Home and verify decluttered Dashboard + Help button
        composeRule.onNodeWithTag("nav_tab_home").performClick()
        composeRule.waitUntil(timeoutMillis = 5_000) {
            composeRule.onAllNodesWithTag("hero_balance_card").fetchSemanticsNodes().isNotEmpty()
        }
        composeRule.onNodeWithTag("hero_balance_card").assertIsDisplayed()
        composeRule.onNodeWithTag("home_tutorial_button").assertIsDisplayed()

        // Verify replaying tutorial from Home ? button works and can be dismissed
        composeRule.onNodeWithTag("home_tutorial_button").performClick()
        composeRule.waitForIdle()
        composeRule.onNodeWithTag("tutorial_overlay").assertIsDisplayed()
        composeRule.onNodeWithTag("tutorial_skip_button").performClick()
        composeRule.waitForIdle()

        // Navigate to Budgets tab -> Decluttered Room-backed Monthly Budget Overview screen
        composeRule.onNodeWithTag("nav_tab_budgets").performClick()
        composeRule.waitUntil(timeoutMillis = 5_000) {
            composeRule.onAllNodesWithTag("overview_hero_card").fetchSemanticsNodes().isNotEmpty()
        }
        composeRule.onNodeWithTag("monthly_budget_overview_screen").assertIsDisplayed()
        composeRule.onNodeWithTag("overview_hero_card").assertIsDisplayed()

        // Set a custom monthly budget limit in Room via the bottom sheet
        composeRule.onNodeWithTag("overview_set_limit_button").performClick()
        composeRule.waitForIdle()
        composeRule.onNodeWithTag("overview_limit_amount_input").performTextClearance()
        composeRule.onNodeWithTag("overview_limit_amount_input").performTextInput("2500")
        composeRule.onNodeWithTag("overview_save_limit_button").performClick()
        composeRule.waitForIdle()
        composeRule.onNodeWithTag("overview_budget_limit_value").assertTextContains("2,500", substring = true)

        // Navigate to previous month and jump back to current period
        composeRule.onNodeWithTag("overview_prev_month_button").performClick()
        composeRule.waitUntil(timeoutMillis = 5_000) {
            composeRule.onAllNodesWithTag("overview_jump_current_button").fetchSemanticsNodes().isNotEmpty()
        }
        composeRule.onNodeWithTag("overview_jump_current_button").performClick()
        composeRule.waitForIdle()

        // Switch to Category limits view inside Budgets tab
        composeRule.onNodeWithTag("budgets_section_categories").performClick()
        composeRule.waitForIdle()
        composeRule.onNodeWithTag("budgets_list").assertIsDisplayed()

        // Navigate across Activity tab and switch to Bills & Subs
        composeRule.onNodeWithTag("nav_tab_activity").performClick()
        composeRule.waitForIdle()
        composeRule.onNodeWithTag("activity_section_bills_subs").performClick()
        composeRule.waitForIdle()

        // Navigate across Insights tab and switch comparison modes
        composeRule.onNodeWithTag("nav_tab_insights").performClick()
        composeRule.waitForIdle()
        composeRule.onNodeWithTag("insights_mode_monthly").performClick()
        composeRule.waitForIdle()
        composeRule.onNodeWithTag("insights_mode_yearly").performClick()
        composeRule.waitForIdle()
        composeRule.onNodeWithTag("insights_mode_breakdown").performClick()
        composeRule.waitForIdle()

        // Return Home and open Add Expense bottom sheet
        composeRule.onNodeWithTag("nav_tab_home").performClick()
        composeRule.waitForIdle()
        composeRule.onNodeWithTag("add_expense_fab").performClick()
        composeRule.waitForIdle()
        composeRule.onNodeWithTag("entry_amount_display").assertIsDisplayed()

        composeRule.activityRule.scenario.onActivity { activity ->
            activity.onBackPressedDispatcher.onBackPressed()
        }
        composeRule.waitForIdle()

        // Open Settings and verify local-only backup export & import controls (no Google Sign-In)
        composeRule.onNodeWithTag("account_settings_button").performClick()
        composeRule.waitForIdle()
        composeRule.onNodeWithTag("settings_screen").assertIsDisplayed()
        composeRule.onNodeWithTag("hero_export_backup_button").assertIsDisplayed()
        composeRule.onNodeWithTag("hero_import_backup_button").assertIsDisplayed()
        assertTrue(
            "Google Sign-In button should be completely removed",
            composeRule.onAllNodesWithTag("google_sign_in_button").fetchSemanticsNodes().isEmpty(),
        )
    }

    @Test
    fun roomMonthlyBudgetRepositoryAggregatesSpendingAndComparesAgainstLimit() = runBlocking {
        val context = ApplicationProvider.getApplicationContext<Context>()
        val db = Room.inMemoryDatabaseBuilder(context, PixelFundsDatabase::class.java)
            .allowMainThreadQueries()
            .build()
        try {
            val mainDao = db.dao()
            val repo = MonthlyBudgetOverviewRepository(db.monthlyBudgetOverviewDao())

            mainDao.insertCategories(
                listOf(
                    LocalCategoryEntity("food", "Dining", "dining", 0xFFE07A5FL, 400.0, 0, false),
                    LocalCategoryEntity("home", "Home", "home", 0xFF3D8B68L, 600.0, 1, false),
                )
            )

            val novStart = LocalDay(2026, 11, 1)
            val novEnd = LocalDay(2026, 11, 30)
            val octDay = LocalDay(2026, 10, 15)

            mainDao.insertTransactions(
                listOf(
                    LocalTransactionEntity("t1", "food", 125.50, "Bistro", LocalDay(2026, 11, 5).toEpochDay(), false, 10L),
                    LocalTransactionEntity("t2", "home", 300.00, "Utilities", LocalDay(2026, 11, 12).toEpochDay(), false, 20L),
                    LocalTransactionEntity("t3", "food", 999.00, "Prior Month", octDay.toEpochDay(), false, 5L),
                )
            )

            val initial = repo.observePeriodOverview(novStart, novEnd, "2026-11").first()
            assertEquals(425.50, initial.totalSpent, 0.01)
            assertEquals(2, initial.transactionCount)
            assertEquals(300.00, initial.largestExpense, 0.01)
            assertFalse(initial.hasCustomLimit)
            assertEquals(1000.0, initial.effectiveBudgetLimit, 0.01)

            repo.saveMonthlyBudgetLimit("2026-11", 1500.0)
            val withCustomLimit = repo.observePeriodOverview(novStart, novEnd, "2026-11").first()
            assertTrue(withCustomLimit.hasCustomLimit)
            assertEquals(1500.0, withCustomLimit.effectiveBudgetLimit, 0.01)

            repo.clearMonthlyBudgetLimit("2026-11")
            val reverted = repo.observePeriodOverview(novStart, novEnd, "2026-11").first()
            assertFalse(reverted.hasCustomLimit)
            assertEquals(1000.0, reverted.effectiveBudgetLimit, 0.01)
        } finally {
            db.close()
        }
    }

    @Test
    fun fullJsonBackupExportAndImportRoundTripsAllLocalData() = runBlocking {
        val context = ApplicationProvider.getApplicationContext<Context>()
        val sourceDb = Room.inMemoryDatabaseBuilder(context, PixelFundsDatabase::class.java)
            .allowMainThreadQueries()
            .build()
        val targetDb = Room.inMemoryDatabaseBuilder(context, PixelFundsDatabase::class.java)
            .allowMainThreadQueries()
            .build()

        try {
            val sourceDao = sourceDb.dao()
            val sourceRepo = LocalBudgetRepository(sourceDao)
            val targetRepo = LocalBudgetRepository(targetDb.dao())

            sourceDao.insertCategories(
                listOf(
                    LocalCategoryEntity("dining", "Dining", "dining", 0xFFE07A5FL, 450.0, 0, false),
                    LocalCategoryEntity("travel", "Travel", "travel", 0xFF4A78B8L, 800.0, 1, false),
                )
            )
            sourceDao.insertTransactions(
                listOf(
                    LocalTransactionEntity("tx1", "dining", 48.75, "Ramen Shop", LocalDay(2026, 11, 10).toEpochDay(), false, 100L),
                    LocalTransactionEntity("tx2", "travel", 220.00, "Train Pass", LocalDay(2026, 11, 12).toEpochDay(), false, 200L),
                )
            )
            sourceDao.insertRecurringItems(
                listOf(
                    LocalRecurringEntity(
                        id = "sub1",
                        categoryId = "dining",
                        name = "Coffee Club",
                        amount = 19.99,
                        kind = RecurringKind.SUBSCRIPTION.storageKey,
                        cadence = BillingCadence.MONTHLY.storageKey,
                        nextDueEpochDay = LocalDay(2026, 12, 1).toEpochDay(),
                        active = true,
                        sample = false,
                    )
                )
            )
            sourceDao.insertMonthlyBudgetLimits(
                listOf(MonthlyBudgetLimitEntity("2026-11", 1800.0, 123456789L))
            )

            val customPrefs = UserPreferences(
                budgetResetDay = 15,
                currencyCode = "EUR",
                themeMode = AppThemeMode.DARK,
                showDecimals = false,
                warningThresholdPercent = 75,
            )

            val exported = sourceRepo.exportFullBackup(customPrefs).getOrThrow()
            val encodedJson = LocalBackupCodec.encodeBackupJson(exported)

            val parsed = LocalBackupCodec.parseImportPayload(encodedJson).getOrThrow()
            assertTrue(parsed is ParsedImportPayload.FullBackup)
            val restoredBackup = (parsed as ParsedImportPayload.FullBackup).backup

            assertEquals(15, restoredBackup.preferences?.budgetResetDay)
            assertEquals("EUR", restoredBackup.preferences?.currencyCode)
            assertEquals(AppThemeMode.DARK, restoredBackup.preferences?.themeMode)

            targetRepo.restoreFullBackup(restoredBackup).getOrThrow()

            val targetDao = targetDb.dao()
            assertEquals(2, targetDao.getCategories().size)
            assertEquals(2, targetDao.getTransactions().size)
            assertEquals(1, targetDao.getRecurringItems().size)
            assertEquals(1, targetDao.getAllMonthlyBudgetLimits().size)
            assertEquals(1800.0, targetDao.getAllMonthlyBudgetLimits().first().limitAmount, 0.01)
        } finally {
            sourceDb.close()
            targetDb.close()
        }
    }

    @Test
    fun csvImportParsesExportedFormatAndCreatesMissingCategories() = runBlocking {
        val context = ApplicationProvider.getApplicationContext<Context>()
        val db = Room.inMemoryDatabaseBuilder(context, PixelFundsDatabase::class.java)
            .allowMainThreadQueries()
            .build()
        try {
            val dao = db.dao()
            val repo = LocalBudgetRepository(dao)
            dao.insertCategory(LocalCategoryEntity("dining", "Dining", "dining", 0xFFE07A5FL, 400.0, 0, false))

            val csv = listOf(
                "Date,Category,Merchant/Note,Amount,Currency",
                "2026-11-14,\"Dining\",\"Bistro, \"\"Downtown\"\"\",64.50,USD",
                "2026-11-15,\"Fitness\",\"Climbing Pass\",95.00,USD",
                "bad-date,\"Dining\",\"Skip Me\",12.00,USD",
            ).joinToString("\n")

            val parsed = LocalBackupCodec.parseImportPayload(csv).getOrThrow()
            assertTrue(parsed is ParsedImportPayload.CsvTransactions)
            val csvPayload = parsed as ParsedImportPayload.CsvTransactions
            assertEquals(2, csvPayload.rows.size)
            assertEquals(1, csvPayload.skippedRows)
            assertEquals("Bistro, \"Downtown\"", csvPayload.rows[0].merchantOrNote)

            val importedCount = repo.importCsvTransactions(csvPayload.rows, replaceExisting = false).getOrThrow()
            assertEquals(2, importedCount)

            val categories = dao.getCategories()
            assertEquals(2, categories.size)
            assertTrue(categories.any { it.name == "Fitness" })

            val txs = dao.getTransactions()
            assertEquals(2, txs.size)
            assertEquals(159.50, txs.sumOf { it.amount }, 0.01)
        } finally {
            db.close()
        }
    }

    @Test
    fun defaultCurrencyResolvesFastWithoutMainThreadFreeze() {
        val startMs = System.currentTimeMillis()
        val currency = WorldCurrencyCatalog.defaultCurrency()
        val elapsedMs = System.currentTimeMillis() - startMs
        assertTrue("Expected default currency code to be 3 letters", currency.code.length == 3)
        assertTrue("Default currency resolution took too long (${elapsedMs}ms)", elapsedMs < 500L)
    }
}
