package com.example

import android.content.Context
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.compose.ui.test.onAllNodesWithTag
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.performClick
import androidx.test.core.app.ApplicationProvider
import com.example.data.WorldCurrencyCatalog
import com.google.firebase.FirebaseApp
import com.google.firebase.FirebaseOptions
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Before
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

    @Before
    fun ensureFirebaseInitialized() {
        val context = ApplicationProvider.getApplicationContext<Context>()
        if (FirebaseApp.getApps(context).isEmpty()) {
            FirebaseApp.initializeApp(
                context,
                FirebaseOptions.Builder()
                    .setApplicationId("1:885325190096:android:bdc4a02374c8775062dfd5")
                    .setApiKey("AIzaSyTestKeyForRobolectric00000000000")
                    .setProjectId("gen-lang-client-0952622125")
                    .build(),
            )
        }
    }

    @Test
    fun appNameMatchesPixelFunds() {
        val context = ApplicationProvider.getApplicationContext<Context>()
        assertEquals("Pixel Funds", context.getString(R.string.app_name))
    }

    @Test
    fun mainActivityStartsAndRendersAllPrimaryFlows() {
        composeRule.waitUntil(timeoutMillis = 10_000) {
            composeRule.onAllNodesWithTag("hero_balance_card").fetchSemanticsNodes().isNotEmpty()
        }
        composeRule.onNodeWithTag("hero_balance_card").assertIsDisplayed()

        // Navigate across Budgets tab
        composeRule.onNodeWithTag("nav_tab_budgets").performClick()
        composeRule.waitForIdle()

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

        // Close sheet via save disabled / back press and open Settings
        composeRule.activityRule.scenario.onActivity { activity ->
            activity.onBackPressedDispatcher.onBackPressed()
        }
        composeRule.waitForIdle()

        composeRule.onNodeWithTag("account_settings_button").performClick()
        composeRule.waitForIdle()
        composeRule.onNodeWithTag("settings_screen").assertIsDisplayed()
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
