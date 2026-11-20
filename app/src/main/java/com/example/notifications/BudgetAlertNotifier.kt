package com.example.notifications

import android.Manifest
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import androidx.core.content.ContextCompat
import com.example.MainActivity
import com.example.R
import com.example.data.BudgetCategory
import com.example.data.CurrencyOption
import com.example.ui.components.formatCurrencyValue
import kotlin.math.roundToInt

/**
 * Threshold tiers for monthly category budget notifications.
 */
enum class BudgetAlertLevel(val thresholdPercent: Int, val ratio: Double) {
    WARNING_80(thresholdPercent = 80, ratio = 0.80),
    LIMIT_100(thresholdPercent = 100, ratio = 1.00),
}

data class CategoryBudgetAlert(
    val categoryId: String,
    val categoryName: String,
    val spent: Double,
    val limit: Double,
    val percentUsed: Int,
    val level: BudgetAlertLevel,
    val cycleKey: String,
)

data class BudgetAlertEvaluation(
    val alertsToSend: List<CategoryBudgetAlert>,
    val updatedSentKeys: Set<String>,
)

/**
 * Pure, deterministic evaluator for 80% and 100% category budget alerts.
 *
 * - Alerts once per (budgetCycle, category, threshold) so users aren't spammed by subsequent edits.
 * - If a single expense jumps directly from <80% to >=100%, emits only the 100% alert and marks
 *   both 80% and 100% as acknowledged for that cycle.
 * - Automatically clears a threshold key when spending drops back below that threshold (e.g.,
 *   after deleting an expense or increasing a category limit) and prunes keys from prior cycles.
 */
object BudgetThresholdEvaluator {

    fun alertKey(cycleKey: String, categoryId: String, level: BudgetAlertLevel): String =
        "$cycleKey|$categoryId|${level.thresholdPercent}"

    fun evaluate(
        categories: List<BudgetCategory>,
        spentByCategory: Map<String, Double>,
        cycleKey: String,
        alreadySentKeys: Set<String>,
        onlyCategoryIds: Set<String>? = null,
        emitAlerts: Boolean = true,
    ): BudgetAlertEvaluation {
        val cyclePrefix = "$cycleKey|"
        // Retain only keys belonging to the active budget cycle
        val updatedKeys = alreadySentKeys
            .filterTo(LinkedHashSet()) { it.startsWith(cyclePrefix) }

        val alerts = mutableListOf<CategoryBudgetAlert>()

        for (category in categories) {
            if (onlyCategoryIds != null && category.id !in onlyCategoryIds) continue
            val limit = category.monthlyLimit
            if (!limit.isFinite() || limit <= 0.0) continue

            val spent = (spentByCategory[category.id] ?: 0.0).coerceAtLeast(0.0)
            val ratio = spent / limit
            val percentUsed = (ratio * 100.0).roundToInt()

            val key80 = alertKey(cycleKey, category.id, BudgetAlertLevel.WARNING_80)
            val key100 = alertKey(cycleKey, category.id, BudgetAlertLevel.LIMIT_100)

            when {
                ratio >= BudgetAlertLevel.LIMIT_100.ratio -> {
                    val alreadyNotified100 = key100 in updatedKeys
                    updatedKeys.add(key80)
                    updatedKeys.add(key100)
                    if (emitAlerts && !alreadyNotified100) {
                        alerts.add(
                            CategoryBudgetAlert(
                                categoryId = category.id,
                                categoryName = category.name,
                                spent = spent,
                                limit = limit,
                                percentUsed = percentUsed.coerceAtLeast(100),
                                level = BudgetAlertLevel.LIMIT_100,
                                cycleKey = cycleKey,
                            )
                        )
                    }
                }

                ratio >= BudgetAlertLevel.WARNING_80.ratio -> {
                    // Spending is between 80% and <100%: clear 100% key in case it dropped back
                    updatedKeys.remove(key100)
                    val alreadyNotified80 = key80 in updatedKeys
                    updatedKeys.add(key80)
                    if (emitAlerts && !alreadyNotified80) {
                        alerts.add(
                            CategoryBudgetAlert(
                                categoryId = category.id,
                                categoryName = category.name,
                                spent = spent,
                                limit = limit,
                                percentUsed = percentUsed.coerceIn(80, 99),
                                level = BudgetAlertLevel.WARNING_80,
                                cycleKey = cycleKey,
                            )
                        )
                    }
                }

                else -> {
                    // Below 80%: clear both keys so crossing them again later triggers a fresh alert
                    updatedKeys.remove(key80)
                    updatedKeys.remove(key100)
                }
            }
        }

        return BudgetAlertEvaluation(
            alertsToSend = alerts,
            updatedSentKeys = updatedKeys,
        )
    }
}

/**
 * Abstraction for posting budget threshold notifications (allows clean faking in unit tests).
 */
interface BudgetAlertDispatcher {
    fun notifyBudgetAlert(
        alert: CategoryBudgetAlert,
        currency: CurrencyOption,
        showDecimals: Boolean,
    ): Boolean
}

/**
 * Android notification manager for Pixel Funds category budget alerts (80% & 100%).
 */
class AndroidBudgetAlertNotifier(
    private val context: Context,
) : BudgetAlertDispatcher {

    fun ensureChannelCreated() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val manager = context.getSystemService(Context.NOTIFICATION_SERVICE) as? NotificationManager
                ?: return
            val existing = manager.getNotificationChannel(CHANNEL_ID)
            if (existing == null) {
                val channel = NotificationChannel(
                    CHANNEL_ID,
                    context.getString(R.string.notification_channel_budget_alerts_name),
                    NotificationManager.IMPORTANCE_HIGH,
                ).apply {
                    description = context.getString(R.string.notification_channel_budget_alerts_desc)
                    enableVibration(true)
                }
                manager.createNotificationChannel(channel)
            }
        }
    }

    override fun notifyBudgetAlert(
        alert: CategoryBudgetAlert,
        currency: CurrencyOption,
        showDecimals: Boolean,
    ): Boolean {
        ensureChannelCreated()
        if (!canPostNotifications(context)) return false

        val spentFormatted = formatCurrencyValue(
            amount = alert.spent,
            currency = currency,
            showCents = showDecimals,
        )
        val limitFormatted = formatCurrencyValue(
            amount = alert.limit,
            currency = currency,
            showCents = showDecimals,
        )

        val (title, body) = when (alert.level) {
            BudgetAlertLevel.WARNING_80 -> {
                val leftFormatted = formatCurrencyValue(
                    amount = (alert.limit - alert.spent).coerceAtLeast(0.0),
                    currency = currency,
                    showCents = showDecimals,
                )
                context.getString(
                    R.string.notification_budget_80_title,
                    alert.categoryName,
                    alert.percentUsed,
                ) to context.getString(
                    R.string.notification_budget_80_body,
                    spentFormatted,
                    limitFormatted,
                    alert.categoryName,
                    leftFormatted,
                )
            }

            BudgetAlertLevel.LIMIT_100 -> {
                val overAmount = (alert.spent - alert.limit).coerceAtLeast(0.0)
                val bodyText = if (overAmount >= 0.01) {
                    val overFormatted = formatCurrencyValue(
                        amount = overAmount,
                        currency = currency,
                        showCents = showDecimals,
                    )
                    context.getString(
                        R.string.notification_budget_100_body,
                        spentFormatted,
                        limitFormatted,
                        alert.categoryName,
                        overFormatted,
                    )
                } else {
                    context.getString(
                        R.string.notification_budget_100_exact_body,
                        spentFormatted,
                        limitFormatted,
                        alert.categoryName,
                    )
                }
                context.getString(
                    R.string.notification_budget_100_title,
                    alert.categoryName,
                    alert.percentUsed,
                ) to bodyText
            }
        }

        val launchIntent = Intent(context, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_SINGLE_TOP or Intent.FLAG_ACTIVITY_CLEAR_TOP
            putExtra(EXTRA_ALERT_CATEGORY_ID, alert.categoryId)
        }
        val pendingIntent = PendingIntent.getActivity(
            context,
            notificationIdFor(alert.categoryId),
            launchIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE,
        )

        val notification = NotificationCompat.Builder(context, CHANNEL_ID)
            .setSmallIcon(R.drawable.ic_budget_alert)
            .setContentTitle(title)
            .setContentText(body)
            .setStyle(NotificationCompat.BigTextStyle().bigText(body))
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .setCategory(NotificationCompat.CATEGORY_REMINDER)
            .setAutoCancel(true)
            .setContentIntent(pendingIntent)
            .build()

        return runCatching {
            NotificationManagerCompat.from(context)
                .notify(notificationIdFor(alert.categoryId), notification)
            true
        }.getOrDefault(false)
    }

    companion object {
        const val CHANNEL_ID = "pixel_funds_budget_alerts"
        const val EXTRA_ALERT_CATEGORY_ID = "extra_alert_category_id"
        private const val NOTIFICATION_ID_BASE = 4100

        fun notificationIdFor(categoryId: String): Int =
            NOTIFICATION_ID_BASE + (categoryId.hashCode() and 0x0FFF)

        fun hasPostNotificationsPermission(context: Context): Boolean {
            return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                ContextCompat.checkSelfPermission(
                    context,
                    Manifest.permission.POST_NOTIFICATIONS,
                ) == PackageManager.PERMISSION_GRANTED
            } else {
                true
            }
        }

        fun canPostNotifications(context: Context): Boolean {
            if (!hasPostNotificationsPermission(context)) return false
            return NotificationManagerCompat.from(context).areNotificationsEnabled()
        }
    }
}
