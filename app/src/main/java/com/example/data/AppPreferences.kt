package com.example.data

import android.content.Context
import androidx.compose.runtime.Immutable
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.intPreferencesKey
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.map
import java.io.IOException
import java.util.Currency
import java.util.Locale

private val Context.pixelFundsPrefsDataStore: DataStore<Preferences> by preferencesDataStore(
    name = "pixel_funds_preferences",
)

enum class AppThemeMode(val storageKey: String) {
    LIGHT("light"),
    DARK("dark"),
    ADAPTIVE_DEVICE("adaptive_device");

    companion object {
        fun fromStorageKey(key: String?): AppThemeMode =
            entries.find { it.storageKey == key } ?: ADAPTIVE_DEVICE
    }
}

@Immutable
data class CurrencyOption(
    val code: String,
    val displayName: String,
    val symbol: String,
    val fractionDigits: Int,
    val regions: String,
) {
    /** True when the symbol is a compact prefix like $, €, £, ¥, ₹ rather than a 3-letter code. */
    val usesTightPrefix: Boolean
        get() = symbol != code && symbol.length <= 3 && !symbol.all { it.isLetter() }

    val searchIndex: String
        get() = "$code $displayName $symbol $regions".lowercase(Locale.ROOT)
}

@Immutable
data class UserPreferences(
    val budgetResetDay: Int = 1,
    val currencyCode: String = WorldCurrencyCatalog.defaultCurrencyCode(),
    val themeMode: AppThemeMode = AppThemeMode.ADAPTIVE_DEVICE,
    val dynamicColorInManualTheme: Boolean = false,
    val showDecimals: Boolean = true,
    val hapticsEnabled: Boolean = true,
    val reduceMotion: Boolean = false,
    val warningThresholdPercent: Int = 80,
    val includeFixedBillsInSafeDaily: Boolean = false,
    val compactCards: Boolean = false,
) {
    val currency: CurrencyOption
        get() = WorldCurrencyCatalog.findByCode(currencyCode)

    val warningThresholdRatio: Float
        get() = (warningThresholdPercent.coerceIn(55, 95) / 100f)
}

/**
 * Provides every circulating ISO 4217 world currency from the Android/JVM ICU dataset,
 * enriched with country/region names so users worldwide can search by country, name, symbol, or code.
 */
object WorldCurrencyCatalog {
    private val nonCirculatingCodes = setOf(
        "XAU", "XAG", "XPT", "XPD", "XTS", "XXX", "XDR", "XBA", "XBB", "XBC", "XBD",
        "XSU", "XUA", "XFU", "XFO", "USN", "USS", "BOV", "CLF", "COU", "MXV", "UYI",
        "UYW", "CHE", "CHW", "ZWR", "ZWD", "ZWL", "AYM", "BYB", "GHC", "MZM", "ROL",
        "SDD", "SRG", "TRL", "VEB", "VEF", "CYP", "EEK", "LTL", "LVL", "MTL", "SIT",
        "SKK", "HRK", "MRO", "STD", "CUC", "SVC", "BYR", "TMM", "AZM", "RUR", "YUM",
        "ATS", "BEF", "DEM", "ESP", "FIM", "FRF", "GRD", "IEP", "ITL", "LUF", "NLG",
        "PTE", "ADP", "BGL", "CSD", "GWP", "TPE",
    )

    private val preferredSymbols = mapOf(
        "USD" to "$",
        "EUR" to "€",
        "GBP" to "£",
        "JPY" to "¥",
        "CNY" to "¥",
        "INR" to "₹",
        "KRW" to "₩",
        "LKR" to "Rs",
        "PKR" to "Rs",
        "NPR" to "Rs",
        "AUD" to "A$",
        "CAD" to "CA$",
        "NZD" to "NZ$",
        "HKD" to "HK$",
        "SGD" to "S$",
        "TWD" to "NT$",
        "BRL" to "R$",
        "MXN" to "MX$",
        "ZAR" to "R",
        "PHP" to "₱",
        "THB" to "฿",
        "VND" to "₫",
        "NGN" to "₦",
        "TRY" to "₺",
        "UAH" to "₴",
        "ILS" to "₪",
        "PLN" to "zł",
        "MYR" to "RM",
        "IDR" to "Rp",
        "CHF" to "CHF",
        "SEK" to "kr",
        "NOK" to "kr",
        "DKK" to "kr",
        "CZK" to "Kč",
        "HUF" to "Ft",
        "RON" to "lei",
        "BDT" to "৳",
        "KES" to "KSh",
        "GHS" to "₵",
        "CRC" to "₡",
        "PYG" to "₲",
        "KZT" to "₸",
        "MNT" to "₮",
        "RUB" to "₽",
        "GEL" to "₾",
        "AZN" to "₼",
    )

    val allCurrencies: List<CurrencyOption> by lazy {
        val displayLocale = Locale.getDefault()
        val regionsByCode = HashMap<String, MutableSet<String>>()

        Locale.getAvailableLocales().forEach { locale ->
            if (locale.country.length == 2) {
                val currency = runCatching { Currency.getInstance(locale) }.getOrNull()
                if (currency != null) {
                    val countryName = locale.getDisplayCountry(displayLocale)
                    if (countryName.isNotBlank()) {
                        regionsByCode.getOrPut(currency.currencyCode) { LinkedHashSet() }
                            .add(countryName)
                    }
                }
            }
        }

        Currency.getAvailableCurrencies()
            .asSequence()
            .filter { currency ->
                val code = currency.currencyCode
                code.length == 3 &&
                    code !in nonCirculatingCodes &&
                    currency.defaultFractionDigits >= 0
            }
            .map { currency ->
                val code = currency.currencyCode
                val rawName = runCatching { currency.getDisplayName(displayLocale) }
                    .getOrNull()
                    ?.takeIf { it.isNotBlank() }
                    ?: code
                val symbol = preferredSymbols[code]
                    ?: runCatching { currency.getSymbol(Locale.US) }.getOrNull()
                        ?.takeIf { it.isNotBlank() }
                    ?: code
                val regions = regionsByCode[code]
                    ?.sorted()
                    ?.take(4)
                    ?.joinToString(", ")
                    .orEmpty()

                CurrencyOption(
                    code = code,
                    displayName = rawName,
                    symbol = symbol,
                    fractionDigits = currency.defaultFractionDigits.coerceIn(0, 3),
                    regions = regions,
                )
            }
            .sortedWith(compareBy<CurrencyOption> { it.displayName.lowercase(Locale.ROOT) }.thenBy { it.code })
            .toList()
    }

    private val byCode: Map<String, CurrencyOption> by lazy {
        allCurrencies.associateBy { it.code }
    }

    val USD: CurrencyOption by lazy {
        byCode["USD"] ?: CurrencyOption(
            code = "USD",
            displayName = "US Dollar",
            symbol = "$",
            fractionDigits = 2,
            regions = "United States",
        )
    }

    fun defaultCurrencyCode(): String {
        val localeCode = runCatching { Currency.getInstance(Locale.getDefault()).currencyCode }
            .getOrNull()
        return if (localeCode != null && byCode.containsKey(localeCode)) localeCode else "USD"
    }

    fun defaultCurrency(): CurrencyOption = findByCode(defaultCurrencyCode())

    fun findByCode(code: String?): CurrencyOption =
        if (code != null) byCode[code.uppercase(Locale.ROOT)] ?: USD else USD
}

class AppPreferencesRepository(private val context: Context) {

    private object Keys {
        val BUDGET_RESET_DAY = intPreferencesKey("budget_reset_day")
        val CURRENCY_CODE = stringPreferencesKey("currency_code")
        val THEME_MODE = stringPreferencesKey("theme_mode")
        val DYNAMIC_COLOR_MANUAL = booleanPreferencesKey("dynamic_color_manual")
        val SHOW_DECIMALS = booleanPreferencesKey("show_decimals")
        val HAPTICS_ENABLED = booleanPreferencesKey("haptics_enabled")
        val REDUCE_MOTION = booleanPreferencesKey("reduce_motion")
        val WARNING_THRESHOLD = intPreferencesKey("warning_threshold_percent")
        val INCLUDE_FIXED_BILLS_SAFE = booleanPreferencesKey("include_fixed_bills_safe")
        val COMPACT_CARDS = booleanPreferencesKey("compact_cards")
    }

    val preferencesFlow: Flow<UserPreferences> = context.pixelFundsPrefsDataStore.data
        .catch { exception ->
            if (exception is IOException) {
                emit(androidx.datastore.preferences.core.emptyPreferences())
            } else {
                throw exception
            }
        }
        .map { prefs ->
            UserPreferences(
                budgetResetDay = (prefs[Keys.BUDGET_RESET_DAY] ?: 1).coerceIn(1, 31),
                currencyCode = prefs[Keys.CURRENCY_CODE] ?: WorldCurrencyCatalog.defaultCurrencyCode(),
                themeMode = AppThemeMode.fromStorageKey(prefs[Keys.THEME_MODE]),
                dynamicColorInManualTheme = prefs[Keys.DYNAMIC_COLOR_MANUAL] ?: false,
                showDecimals = prefs[Keys.SHOW_DECIMALS] ?: true,
                hapticsEnabled = prefs[Keys.HAPTICS_ENABLED] ?: true,
                reduceMotion = prefs[Keys.REDUCE_MOTION] ?: false,
                warningThresholdPercent = (prefs[Keys.WARNING_THRESHOLD] ?: 80).coerceIn(55, 95),
                includeFixedBillsInSafeDaily = prefs[Keys.INCLUDE_FIXED_BILLS_SAFE] ?: false,
                compactCards = prefs[Keys.COMPACT_CARDS] ?: false,
            )
        }

    suspend fun setBudgetResetDay(day: Int) {
        val clamped = day.coerceIn(1, 31)
        context.pixelFundsPrefsDataStore.edit { it[Keys.BUDGET_RESET_DAY] = clamped }
    }

    suspend fun setCurrencyCode(code: String) {
        val normalized = WorldCurrencyCatalog.findByCode(code).code
        context.pixelFundsPrefsDataStore.edit { it[Keys.CURRENCY_CODE] = normalized }
    }

    suspend fun setThemeMode(mode: AppThemeMode) {
        context.pixelFundsPrefsDataStore.edit { it[Keys.THEME_MODE] = mode.storageKey }
    }

    suspend fun setDynamicColorInManualTheme(enabled: Boolean) {
        context.pixelFundsPrefsDataStore.edit { it[Keys.DYNAMIC_COLOR_MANUAL] = enabled }
    }

    suspend fun setShowDecimals(enabled: Boolean) {
        context.pixelFundsPrefsDataStore.edit { it[Keys.SHOW_DECIMALS] = enabled }
    }

    suspend fun setHapticsEnabled(enabled: Boolean) {
        context.pixelFundsPrefsDataStore.edit { it[Keys.HAPTICS_ENABLED] = enabled }
    }

    suspend fun setReduceMotion(enabled: Boolean) {
        context.pixelFundsPrefsDataStore.edit { it[Keys.REDUCE_MOTION] = enabled }
    }

    suspend fun setWarningThresholdPercent(percent: Int) {
        context.pixelFundsPrefsDataStore.edit { it[Keys.WARNING_THRESHOLD] = percent.coerceIn(55, 95) }
    }

    suspend fun setIncludeFixedBillsInSafeDaily(enabled: Boolean) {
        context.pixelFundsPrefsDataStore.edit { it[Keys.INCLUDE_FIXED_BILLS_SAFE] = enabled }
    }

    suspend fun setCompactCards(enabled: Boolean) {
        context.pixelFundsPrefsDataStore.edit { it[Keys.COMPACT_CARDS] = enabled }
    }
}
