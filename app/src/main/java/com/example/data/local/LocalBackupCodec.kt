package com.example.data.local

import androidx.compose.runtime.Immutable
import com.example.data.AppThemeMode
import com.example.data.BillingCadence
import com.example.data.LocalDay
import com.example.data.RecurringKind
import com.example.data.UserPreferences
import com.example.data.WorldCurrencyCatalog
import org.json.JSONArray
import org.json.JSONObject
import java.util.Locale
import java.util.UUID

@Immutable
data class PixelFundsBackupData(
    val schemaVersion: Int = CURRENT_SCHEMA_VERSION,
    val exportedAtMillis: Long = System.currentTimeMillis(),
    val preferences: UserPreferences? = null,
    val categories: List<LocalCategoryEntity> = emptyList(),
    val transactions: List<LocalTransactionEntity> = emptyList(),
    val recurringItems: List<LocalRecurringEntity> = emptyList(),
    val monthlyBudgetLimits: List<MonthlyBudgetLimitEntity> = emptyList(),
) {
    companion object {
        const val CURRENT_SCHEMA_VERSION = 1
    }
}

@Immutable
data class ImportedCsvRow(
    val date: LocalDay,
    val categoryName: String,
    val merchantOrNote: String,
    val amount: Double,
)

sealed interface ParsedImportPayload {
    data class FullBackup(val backup: PixelFundsBackupData) : ParsedImportPayload
    data class CsvTransactions(
        val rows: List<ImportedCsvRow>,
        val skippedRows: Int,
    ) : ParsedImportPayload
}

/**
 * Portable local JSON backup + CSV transaction codec (zero external dependencies).
 */
object LocalBackupCodec {

    fun encodeBackupJson(data: PixelFundsBackupData): String {
        val root = JSONObject().apply {
            put("app", "Pixel Funds")
            put("schemaVersion", data.schemaVersion)
            put("exportedAtMillis", data.exportedAtMillis)

            data.preferences?.let { prefs ->
                put(
                    "preferences",
                    JSONObject().apply {
                        put("budgetResetDay", prefs.budgetResetDay)
                        put("currencyCode", prefs.currencyCode)
                        put("themeMode", prefs.themeMode.storageKey)
                        put("dynamicColorInManualTheme", prefs.dynamicColorInManualTheme)
                        put("showDecimals", prefs.showDecimals)
                        put("hapticsEnabled", prefs.hapticsEnabled)
                        put("reduceMotion", prefs.reduceMotion)
                        put("warningThresholdPercent", prefs.warningThresholdPercent)
                        put("budgetAlertsEnabled", prefs.budgetAlertsEnabled)
                        put("includeFixedBillsInSafeDaily", prefs.includeFixedBillsInSafeDaily)
                        put("compactCards", prefs.compactCards)
                    }
                )
            }

            put(
                "categories",
                JSONArray().apply {
                    data.categories.forEach { cat ->
                        put(
                            JSONObject().apply {
                                put("id", cat.id)
                                put("name", cat.name)
                                put("iconKey", cat.iconKey)
                                put("colorHex", cat.colorHex)
                                put("monthlyLimit", cat.monthlyLimit)
                                put("sortOrder", cat.sortOrder)
                                put("sample", cat.sample)
                            }
                        )
                    }
                }
            )

            put(
                "transactions",
                JSONArray().apply {
                    data.transactions.forEach { tx ->
                        put(
                            JSONObject().apply {
                                put("id", tx.id)
                                put("categoryId", tx.categoryId)
                                put("amount", tx.amount)
                                put("merchantOrNote", tx.merchantOrNote)
                                put("dateEpochDay", tx.dateEpochDay)
                                put("dateIso", LocalDay.fromEpochDay(tx.dateEpochDay).formatIsoDate())
                                put("sample", tx.sample)
                                put("createdAtSeconds", tx.createdAtSeconds)
                            }
                        )
                    }
                }
            )

            put(
                "recurringItems",
                JSONArray().apply {
                    data.recurringItems.forEach { rec ->
                        put(
                            JSONObject().apply {
                                put("id", rec.id)
                                put("categoryId", rec.categoryId)
                                put("name", rec.name)
                                put("amount", rec.amount)
                                put("kind", rec.kind)
                                put("cadence", rec.cadence)
                                put("nextDueEpochDay", rec.nextDueEpochDay)
                                put("active", rec.active)
                                put("sample", rec.sample)
                            }
                        )
                    }
                }
            )

            put(
                "monthlyBudgetLimits",
                JSONArray().apply {
                    data.monthlyBudgetLimits.forEach { limit ->
                        put(
                            JSONObject().apply {
                                put("periodKey", limit.periodKey)
                                put("limitAmount", limit.limitAmount)
                                put("updatedAtMillis", limit.updatedAtMillis)
                            }
                        )
                    }
                }
            )
        }
        return root.toString(2)
    }

    fun parseImportPayload(rawText: String): Result<ParsedImportPayload> = runCatching {
        val cleaned = rawText.removePrefix("\uFEFF").trim()
        require(cleaned.isNotEmpty()) { "The selected file is empty." }

        if (cleaned.startsWith("{")) {
            ParsedImportPayload.FullBackup(parseBackupJson(cleaned))
        } else {
            parseCsvTransactions(cleaned)
        }
    }

    private fun parseBackupJson(jsonText: String): PixelFundsBackupData {
        val root = JSONObject(jsonText)
        val schemaVersion = root.optInt("schemaVersion", 1)
        val exportedAtMillis = root.optLong("exportedAtMillis", System.currentTimeMillis())

        val prefsObj = root.optJSONObject("preferences")
        val preferences = prefsObj?.let { p ->
            val rawCurrency = p.optString("currencyCode", WorldCurrencyCatalog.defaultCurrencyCode())
            UserPreferences(
                budgetResetDay = p.optInt("budgetResetDay", 1).coerceIn(1, 31),
                currencyCode = WorldCurrencyCatalog.findByCode(rawCurrency).code,
                themeMode = AppThemeMode.fromStorageKey(p.optString("themeMode", "adaptive_device")),
                dynamicColorInManualTheme = p.optBoolean("dynamicColorInManualTheme", false),
                showDecimals = p.optBoolean("showDecimals", true),
                hapticsEnabled = p.optBoolean("hapticsEnabled", true),
                reduceMotion = p.optBoolean("reduceMotion", false),
                warningThresholdPercent = p.optInt("warningThresholdPercent", 80).coerceIn(55, 95),
                budgetAlertsEnabled = p.optBoolean("budgetAlertsEnabled", true),
                includeFixedBillsInSafeDaily = p.optBoolean("includeFixedBillsInSafeDaily", false),
                compactCards = p.optBoolean("compactCards", false),
            )
        }

        val categories = mutableListOf<LocalCategoryEntity>()
        val catArray = root.optJSONArray("categories")
        if (catArray != null) {
            for (i in 0 until catArray.length()) {
                val obj = catArray.optJSONObject(i) ?: continue
                val id = obj.optString("id").trim().ifBlank {
                    "cat_${UUID.randomUUID().toString().replace("-", "").take(10)}"
                }
                val name = obj.optString("name").trim().take(40)
                if (name.isEmpty()) continue
                val iconKey = obj.optString("iconKey", "shopping")
                    .lowercase(Locale.ROOT)
                    .replace(Regex("[^a-z0-9_]"), "")
                    .ifBlank { "shopping" }
                val colorHex = obj.optLong("colorHex", 0xFF3D8B68L)
                val monthlyLimit = obj.optDouble("monthlyLimit", 100.0).takeIf { it.isFinite() && it > 0.0 } ?: 100.0
                val sortOrder = obj.optLong("sortOrder", i.toLong()).coerceIn(0L, 10_000L)
                val sample = obj.optBoolean("sample", false)
                categories.add(
                    LocalCategoryEntity(
                        id = id,
                        name = name,
                        iconKey = iconKey,
                        colorHex = colorHex,
                        monthlyLimit = monthlyLimit.coerceAtMost(10_000_000.0),
                        sortOrder = sortOrder,
                        sample = sample,
                    )
                )
            }
        }

        val validCategoryIds = categories.map { it.id }.toSet()
        val fallbackCategoryId = categories.firstOrNull()?.id

        val transactions = mutableListOf<LocalTransactionEntity>()
        val txArray = root.optJSONArray("transactions")
        if (txArray != null) {
            for (i in 0 until txArray.length()) {
                val obj = txArray.optJSONObject(i) ?: continue
                val amount = obj.optDouble("amount", Double.NaN)
                if (!amount.isFinite() || amount <= 0.0 || amount > 10_000_000.0) continue
                val note = obj.optString("merchantOrNote").trim().take(120).ifBlank { "Expense" }
                val rawCatId = obj.optString("categoryId").trim()
                val resolvedCatId = when {
                    rawCatId.isNotEmpty() && (validCategoryIds.isEmpty() || rawCatId in validCategoryIds) -> rawCatId
                    fallbackCategoryId != null -> fallbackCategoryId
                    else -> rawCatId.ifBlank { "general" }
                }
                val epochDay = when {
                    obj.has("dateEpochDay") && !obj.isNull("dateEpochDay") -> obj.optLong("dateEpochDay")
                    obj.has("dateIso") -> parseIsoDate(obj.optString("dateIso"))?.toEpochDay() ?: LocalDay.today().toEpochDay()
                    else -> LocalDay.today().toEpochDay()
                }
                val id = obj.optString("id").trim().ifBlank {
                    "tx_${UUID.randomUUID().toString().replace("-", "").take(12)}"
                }
                val sample = obj.optBoolean("sample", false)
                val createdAtSeconds = obj.optLong("createdAtSeconds", System.currentTimeMillis() / 1000L - i)
                transactions.add(
                    LocalTransactionEntity(
                        id = id,
                        categoryId = resolvedCatId,
                        amount = amount,
                        merchantOrNote = note,
                        dateEpochDay = epochDay,
                        sample = sample,
                        createdAtSeconds = createdAtSeconds,
                    )
                )
            }
        }

        val recurringItems = mutableListOf<LocalRecurringEntity>()
        val recArray = root.optJSONArray("recurringItems")
        if (recArray != null) {
            for (i in 0 until recArray.length()) {
                val obj = recArray.optJSONObject(i) ?: continue
                val name = obj.optString("name").trim().take(80)
                val amount = obj.optDouble("amount", Double.NaN)
                if (name.isEmpty() || !amount.isFinite() || amount <= 0.0 || amount > 10_000_000.0) continue
                val rawCatId = obj.optString("categoryId").trim()
                val resolvedCatId = when {
                    rawCatId.isNotEmpty() && (validCategoryIds.isEmpty() || rawCatId in validCategoryIds) -> rawCatId
                    fallbackCategoryId != null -> fallbackCategoryId
                    else -> rawCatId.ifBlank { "general" }
                }
                val id = obj.optString("id").trim().ifBlank {
                    "rec_${UUID.randomUUID().toString().replace("-", "").take(12)}"
                }
                val kind = RecurringKind.fromKey(obj.optString("kind")).storageKey
                val cadence = BillingCadence.fromKey(obj.optString("cadence")).storageKey
                val nextDueEpochDay = obj.optLong("nextDueEpochDay", LocalDay.today().toEpochDay())
                val active = obj.optBoolean("active", true)
                val sample = obj.optBoolean("sample", false)
                recurringItems.add(
                    LocalRecurringEntity(
                        id = id,
                        categoryId = resolvedCatId,
                        name = name,
                        amount = amount,
                        kind = kind,
                        cadence = cadence,
                        nextDueEpochDay = nextDueEpochDay,
                        active = active,
                        sample = sample,
                    )
                )
            }
        }

        val monthlyLimits = mutableListOf<MonthlyBudgetLimitEntity>()
        val limitsArray = root.optJSONArray("monthlyBudgetLimits")
        if (limitsArray != null) {
            for (i in 0 until limitsArray.length()) {
                val obj = limitsArray.optJSONObject(i) ?: continue
                val periodKey = obj.optString("periodKey").trim()
                val limitAmount = obj.optDouble("limitAmount", Double.NaN)
                if (periodKey.isBlank() || !limitAmount.isFinite() || limitAmount <= 0.0 || limitAmount > 10_000_000.0) {
                    continue
                }
                val updatedAt = obj.optLong("updatedAtMillis", System.currentTimeMillis())
                monthlyLimits.add(
                    MonthlyBudgetLimitEntity(
                        periodKey = periodKey,
                        limitAmount = limitAmount,
                        updatedAtMillis = updatedAt,
                    )
                )
            }
        }

        val hasAnyRecognizedSection = root.has("categories") ||
            root.has("transactions") ||
            root.has("recurringItems") ||
            root.has("monthlyBudgetLimits") ||
            root.has("preferences")
        require(hasAnyRecognizedSection) {
            "This JSON file is not a valid Pixel Funds backup."
        }

        return PixelFundsBackupData(
            schemaVersion = schemaVersion,
            exportedAtMillis = exportedAtMillis,
            preferences = preferences,
            categories = categories,
            transactions = transactions,
            recurringItems = recurringItems,
            monthlyBudgetLimits = monthlyLimits,
        )
    }

    private fun parseCsvTransactions(csvText: String): ParsedImportPayload.CsvTransactions {
        val lines = splitCsvLines(csvText)
        require(lines.isNotEmpty()) { "No rows found in CSV file." }

        val firstRow = parseCsvLine(lines.first())
        val loweredHeader = firstRow.map { it.trim().lowercase(Locale.ROOT) }
        val hasHeader = loweredHeader.any {
            it.contains("date") || it.contains("amount") || it.contains("category") || it.contains("merchant")
        }

        var dateCol = 0
        var categoryCol = 1
        var noteCol = 2
        var amountCol = 3

        if (hasHeader) {
            dateCol = loweredHeader.indexOfFirst { it.contains("date") }.takeIf { it >= 0 } ?: 0
            categoryCol = loweredHeader.indexOfFirst { it.contains("cat") }.takeIf { it >= 0 } ?: 1
            noteCol = loweredHeader.indexOfFirst {
                it.contains("merchant") || it.contains("note") || it.contains("desc") || it.contains("title")
            }.takeIf { it >= 0 } ?: 2
            amountCol = loweredHeader.indexOfFirst { it.contains("amount") || it.contains("price") || it.contains("cost") }
                .takeIf { it >= 0 } ?: 3
        }

        val startIndex = if (hasHeader) 1 else 0
        val parsedRows = mutableListOf<ImportedCsvRow>()
        var skipped = 0

        for (i in startIndex until lines.size) {
            val rawLine = lines[i]
            if (rawLine.isBlank()) continue
            val cols = parseCsvLine(rawLine)
            val dateRaw = cols.getOrNull(dateCol)?.trim().orEmpty()
            val catRaw = cols.getOrNull(categoryCol)?.trim().orEmpty().ifBlank { "General" }
            val noteRaw = cols.getOrNull(noteCol)?.trim().orEmpty().ifBlank { catRaw }
            val amountRaw = cols.getOrNull(amountCol)?.trim().orEmpty()

            val parsedDate = parseIsoDate(dateRaw)
            val parsedAmount = parseCurrencyNumber(amountRaw)

            if (parsedDate == null || parsedAmount == null || parsedAmount <= 0.0 || parsedAmount > 10_000_000.0) {
                skipped++
                continue
            }

            parsedRows.add(
                ImportedCsvRow(
                    date = parsedDate,
                    categoryName = catRaw.take(40),
                    merchantOrNote = noteRaw.take(120),
                    amount = parsedAmount,
                )
            )
        }

        require(parsedRows.isNotEmpty()) {
            "No valid transactions found. Expected columns: Date (YYYY-MM-DD), Category, Merchant/Note, Amount."
        }

        return ParsedImportPayload.CsvTransactions(
            rows = parsedRows,
            skippedRows = skipped,
        )
    }

    private fun parseIsoDate(raw: String): LocalDay? {
        val cleaned = raw.trim().substringBefore('T').substringBefore(' ')
        val parts = cleaned.split('-', '/')
        if (parts.size != 3) return null
        val y = parts[0].toIntOrNull() ?: return null
        val m = parts[1].toIntOrNull() ?: return null
        val d = parts[2].toIntOrNull() ?: return null
        if (y !in 1970..2100 || m !in 1..12) return null
        val maxDay = LocalDay.lengthOfMonthFor(y, m)
        if (d !in 1..maxDay) return null
        return LocalDay(y, m, d)
    }

    private fun parseCurrencyNumber(raw: String): Double? {
        val cleaned = raw
            .replace(",", "")
            .replace(Regex("[^0-9.\\-]"), "")
        val value = cleaned.toDoubleOrNull() ?: return null
        if (!value.isFinite()) return null
        return kotlin.math.abs(value)
    }

    private fun splitCsvLines(text: String): List<String> {
        val result = mutableListOf<String>()
        val current = StringBuilder()
        var inQuotes = false
        var i = 0
        while (i < text.length) {
            val c = text[i]
            when {
                c == '"' -> {
                    inQuotes = !inQuotes
                    current.append(c)
                }
                (c == '\n' || c == '\r') && !inQuotes -> {
                    if (c == '\r' && i + 1 < text.length && text[i + 1] == '\n') {
                        i++
                    }
                    result.add(current.toString())
                    current.setLength(0)
                }
                else -> current.append(c)
            }
            i++
        }
        if (current.isNotEmpty()) {
            result.add(current.toString())
        }
        return result
    }

    private fun parseCsvLine(line: String): List<String> {
        val fields = mutableListOf<String>()
        val current = StringBuilder()
        var inQuotes = false
        var i = 0
        while (i < line.length) {
            val c = line[i]
            when {
                c == '"' -> {
                    if (inQuotes && i + 1 < line.length && line[i + 1] == '"') {
                        current.append('"')
                        i++
                    } else {
                        inQuotes = !inQuotes
                    }
                }
                c == ',' && !inQuotes -> {
                    fields.add(current.toString())
                    current.setLength(0)
                }
                else -> current.append(c)
            }
            i++
        }
        fields.add(current.toString())
        return fields
    }
}
