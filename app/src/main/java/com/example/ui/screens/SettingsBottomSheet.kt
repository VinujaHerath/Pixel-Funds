package com.example.ui.screens

import android.os.Build
import androidx.activity.compose.BackHandler
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.animateColorAsState
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.ArrowBack
import androidx.compose.material.icons.automirrored.rounded.KeyboardArrowRight
import androidx.compose.material.icons.automirrored.rounded.Logout
import androidx.compose.material.icons.rounded.Animation
import androidx.compose.material.icons.rounded.AutoAwesome
import androidx.compose.material.icons.rounded.CalendarMonth
import androidx.compose.material.icons.rounded.Check
import androidx.compose.material.icons.rounded.CleaningServices
import androidx.compose.material.icons.rounded.Close
import androidx.compose.material.icons.rounded.CloudDone
import androidx.compose.material.icons.rounded.CloudUpload
import androidx.compose.material.icons.rounded.DarkMode
import androidx.compose.material.icons.rounded.DeleteForever
import androidx.compose.material.icons.rounded.Download
import androidx.compose.material.icons.rounded.Info
import androidx.compose.material.icons.rounded.LightMode
import androidx.compose.material.icons.rounded.NotificationsActive
import androidx.compose.material.icons.rounded.Palette
import androidx.compose.material.icons.rounded.Pin
import androidx.compose.material.icons.rounded.Public
import androidx.compose.material.icons.rounded.Repeat
import androidx.compose.material.icons.rounded.Search
import androidx.compose.material.icons.rounded.Smartphone
import androidx.compose.material.icons.rounded.Vibration
import androidx.compose.material.icons.rounded.ViewAgenda
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.BottomSheetDefaults
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilledTonalIconButton
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Slider
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.produceState
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.credentials.CredentialManager
import com.example.R
import com.example.data.AppThemeMode
import com.example.data.CadenceUiState
import com.example.data.CurrencyOption
import com.example.data.LocalDay
import com.example.data.UserPreferences
import com.example.data.WorldCurrencyCatalog
import com.example.ui.auth.onGoogleSignInClicked
import com.example.ui.components.rememberPixelHaptics
import com.example.ui.theme.ExpressiveMotion
import com.example.ui.theme.LocalFinancialColors
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.util.Locale
import kotlin.math.roundToInt

@Composable
fun SettingsOverlayScreen(
    state: CadenceUiState,
    preferences: UserPreferences,
    isCloudSignedIn: Boolean,
    displayName: String,
    email: String,
    credentialManager: CredentialManager,
    onDismiss: () -> Unit,
    onSelectResetDay: (Int) -> Unit,
    onSelectCurrency: (String) -> Unit,
    onSelectThemeMode: (AppThemeMode) -> Unit,
    onToggleDynamicColorInManualTheme: (Boolean) -> Unit,
    onToggleShowDecimals: (Boolean) -> Unit,
    onToggleHaptics: (Boolean) -> Unit,
    onToggleReduceMotion: (Boolean) -> Unit,
    onUpdateWarningThreshold: (Int) -> Unit,
    onToggleIncludeFixedBills: (Boolean) -> Unit,
    onToggleCompactCards: (Boolean) -> Unit,
    onBuildCsv: () -> String,
    onExportStatusMessage: (String) -> Unit,
    onClearSampleData: () -> Unit,
    onRestoreSampleData: () -> Unit,
    onClearAllTransactions: () -> Unit,
    onSignInSuccess: () -> Unit,
    onSignOut: () -> Unit,
) {
    BackHandler(onBack = onDismiss)

    val finColors = LocalFinancialColors.current
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val performHaptic = rememberPixelHaptics()

    var isSigningIn by remember { mutableStateOf(false) }
    var authError by remember { mutableStateOf<String?>(null) }
    var showResetDayDialog by remember { mutableStateOf(false) }
    var showCurrencyPicker by remember { mutableStateOf(false) }
    var showClearAllDialog by remember { mutableStateOf(false) }
    var sliderDraft by remember(preferences.warningThresholdPercent) {
        mutableFloatStateOf(preferences.warningThresholdPercent.toFloat())
    }

    val exportCsvLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.CreateDocument("text/csv"),
    ) { uri ->
        if (uri != null) {
            val result = runCatching {
                val csv = onBuildCsv()
                context.contentResolver.openOutputStream(uri)?.use { stream ->
                    stream.write(csv.toByteArray(Charsets.UTF_8))
                } ?: error("Unable to open output stream")
            }
            if (result.isSuccess) {
                onExportStatusMessage(context.getString(R.string.settings_export_success))
            } else {
                onExportStatusMessage(context.getString(R.string.settings_export_error))
            }
        }
    }

    Surface(
        modifier = Modifier
            .fillMaxSize()
            .testTag("settings_screen"),
        color = MaterialTheme.colorScheme.surface,
    ) {
        Box(
            modifier = Modifier.fillMaxSize(),
            contentAlignment = Alignment.TopCenter,
        ) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .widthIn(max = 720.dp)
                    .statusBarsPadding(),
            ) {
                // Expressive Full-Screen Top App Bar
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp, vertical = 12.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween,
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(12.dp),
                    ) {
                        FilledTonalIconButton(
                            onClick = {
                                performHaptic(HapticFeedbackType.TextHandleMove)
                                onDismiss()
                            },
                            modifier = Modifier.testTag("settings_back_button"),
                        ) {
                            Icon(
                                imageVector = Icons.AutoMirrored.Rounded.ArrowBack,
                                contentDescription = stringResource(R.string.cd_close),
                            )
                        }
                        Text(
                            text = stringResource(R.string.settings_title),
                            style = MaterialTheme.typography.headlineMedium,
                        )
                    }

                    Surface(
                        shape = CircleShape,
                        color = if (isCloudSignedIn) finColors.healthyContainer
                        else MaterialTheme.colorScheme.secondaryContainer,
                    ) {
                        Row(
                            modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(6.dp),
                        ) {
                            Icon(
                                imageVector = if (isCloudSignedIn) Icons.Rounded.CloudDone else Icons.Rounded.Smartphone,
                                contentDescription = null,
                                tint = if (isCloudSignedIn) finColors.onHealthyContainer
                                else MaterialTheme.colorScheme.onSecondaryContainer,
                                modifier = Modifier.size(16.dp),
                            )
                            Text(
                                text = if (isCloudSignedIn) stringResource(R.string.account_synced_badge)
                                else stringResource(R.string.account_local_badge),
                                style = MaterialTheme.typography.labelMedium,
                                color = if (isCloudSignedIn) finColors.onHealthyContainer
                                else MaterialTheme.colorScheme.onSecondaryContainer,
                            )
                        }
                    }
                }

                LazyColumn(
                    modifier = Modifier.weight(1f),
                    contentPadding = PaddingValues(start = 16.dp, end = 16.dp, top = 4.dp, bottom = 36.dp),
                    verticalArrangement = Arrangement.spacedBy(16.dp),
                ) {
                    // 1. Account & Cloud Sync Hero Card
                    item(key = "settings_account") {
                        SettingsSectionTitle(stringResource(R.string.settings_section_account))
                        Spacer(Modifier.height(8.dp))
                        if (isCloudSignedIn) {
                            Surface(
                                shape = RoundedCornerShape(28.dp),
                                color = MaterialTheme.colorScheme.surfaceContainerHigh,
                                modifier = Modifier.fillMaxWidth(),
                            ) {
                                Column(
                                    modifier = Modifier.padding(18.dp),
                                    verticalArrangement = Arrangement.spacedBy(12.dp),
                                ) {
                                    Row(
                                        verticalAlignment = Alignment.CenterVertically,
                                        horizontalArrangement = Arrangement.spacedBy(14.dp),
                                    ) {
                                        Box(
                                            modifier = Modifier
                                                .size(48.dp)
                                                .clip(CircleShape)
                                                .background(MaterialTheme.colorScheme.primaryContainer),
                                            contentAlignment = Alignment.Center,
                                        ) {
                                            Text(
                                                text = displayName.firstOrNull()?.uppercaseChar()?.toString() ?: "P",
                                                style = MaterialTheme.typography.titleLarge,
                                                fontWeight = FontWeight.Bold,
                                                color = MaterialTheme.colorScheme.onPrimaryContainer,
                                            )
                                        }
                                        Column(modifier = Modifier.weight(1f)) {
                                            Text(displayName, style = MaterialTheme.typography.titleMedium)
                                            if (email.isNotBlank()) {
                                                Text(
                                                    text = email,
                                                    style = MaterialTheme.typography.bodyMedium,
                                                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                                                    maxLines = 1,
                                                    overflow = TextOverflow.Ellipsis,
                                                )
                                            }
                                        }
                                    }
                                    OutlinedButton(
                                        onClick = onSignOut,
                                        enabled = !state.isMutating,
                                        shape = CircleShape,
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .height(48.dp)
                                            .testTag("sign_out_button"),
                                    ) {
                                        Icon(Icons.AutoMirrored.Rounded.Logout, contentDescription = null)
                                        Spacer(Modifier.width(8.dp))
                                        Text(
                                            text = stringResource(R.string.account_sign_out),
                                            style = MaterialTheme.typography.titleSmall,
                                        )
                                    }
                                }
                            }
                        } else {
                            Surface(
                                shape = RoundedCornerShape(28.dp),
                                color = MaterialTheme.colorScheme.surfaceContainerHigh,
                                modifier = Modifier.fillMaxWidth(),
                            ) {
                                Column(
                                    modifier = Modifier.padding(18.dp),
                                    verticalArrangement = Arrangement.spacedBy(12.dp),
                                ) {
                                    Text(
                                        text = stringResource(R.string.account_local_title),
                                        style = MaterialTheme.typography.titleMedium,
                                    )
                                    Text(
                                        text = stringResource(R.string.account_local_subtitle),
                                        style = MaterialTheme.typography.bodyMedium,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                                    )
                                    authError?.let { errorText ->
                                        Surface(
                                            shape = RoundedCornerShape(16.dp),
                                            color = MaterialTheme.colorScheme.errorContainer,
                                            modifier = Modifier.fillMaxWidth(),
                                        ) {
                                            Text(
                                                text = errorText,
                                                style = MaterialTheme.typography.labelMedium,
                                                color = MaterialTheme.colorScheme.onErrorContainer,
                                                modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp),
                                            )
                                        }
                                    }
                                    Button(
                                        onClick = {
                                            performHaptic(HapticFeedbackType.LongPress)
                                            authError = null
                                            isSigningIn = true
                                            onGoogleSignInClicked(
                                                context = context,
                                                credentialManager = credentialManager,
                                                onAuthSuccess = {
                                                    isSigningIn = false
                                                    onSignInSuccess()
                                                },
                                                onAuthError = { msg ->
                                                    isSigningIn = false
                                                    authError = msg
                                                },
                                                scope = scope,
                                                onAuthCancelled = {
                                                    isSigningIn = false
                                                },
                                            )
                                        },
                                        enabled = !isSigningIn && !state.isMutating,
                                        shape = CircleShape,
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .height(52.dp)
                                            .testTag("google_sign_in_button"),
                                    ) {
                                        if (isSigningIn) {
                                            CircularProgressIndicator(
                                                modifier = Modifier.size(20.dp),
                                                color = MaterialTheme.colorScheme.onPrimary,
                                                strokeWidth = 2.dp,
                                            )
                                            Spacer(Modifier.width(10.dp))
                                            Text(
                                                text = stringResource(R.string.auth_signing_in),
                                                style = MaterialTheme.typography.titleMedium,
                                            )
                                        } else {
                                            Icon(Icons.Rounded.CloudUpload, contentDescription = null)
                                            Spacer(Modifier.width(8.dp))
                                            Text(
                                                text = stringResource(R.string.auth_sign_in_google),
                                                style = MaterialTheme.typography.titleMedium,
                                            )
                                        }
                                    }
                                }
                            }
                        }
                    }

                    // 2. Appearance, Display & Pixel Experience
                    item(key = "settings_appearance") {
                        SettingsSectionTitle(stringResource(R.string.settings_appearance_title))
                        Spacer(Modifier.height(8.dp))
                        Surface(
                            shape = RoundedCornerShape(28.dp),
                            color = MaterialTheme.colorScheme.surfaceContainer,
                            modifier = Modifier.fillMaxWidth(),
                        ) {
                            Column(modifier = Modifier.fillMaxWidth()) {
                                Column(
                                    modifier = Modifier.padding(16.dp),
                                    verticalArrangement = Arrangement.spacedBy(12.dp),
                                ) {
                                    Row(
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .clip(CircleShape)
                                            .background(MaterialTheme.colorScheme.surfaceContainerHighest)
                                            .padding(4.dp),
                                        horizontalArrangement = Arrangement.spacedBy(4.dp),
                                    ) {
                                        ThemeModeSegment(
                                            label = stringResource(R.string.settings_theme_light),
                                            icon = Icons.Rounded.LightMode,
                                            selected = preferences.themeMode == AppThemeMode.LIGHT,
                                            onClick = {
                                                performHaptic(HapticFeedbackType.TextHandleMove)
                                                onSelectThemeMode(AppThemeMode.LIGHT)
                                            },
                                            modifier = Modifier
                                                .weight(1f)
                                                .testTag("theme_mode_light"),
                                        )
                                        ThemeModeSegment(
                                            label = stringResource(R.string.settings_theme_dark),
                                            icon = Icons.Rounded.DarkMode,
                                            selected = preferences.themeMode == AppThemeMode.DARK,
                                            onClick = {
                                                performHaptic(HapticFeedbackType.TextHandleMove)
                                                onSelectThemeMode(AppThemeMode.DARK)
                                            },
                                            modifier = Modifier
                                                .weight(1f)
                                                .testTag("theme_mode_dark"),
                                        )
                                        ThemeModeSegment(
                                            label = stringResource(R.string.settings_theme_device),
                                            icon = Icons.Rounded.Palette,
                                            selected = preferences.themeMode == AppThemeMode.ADAPTIVE_DEVICE,
                                            onClick = {
                                                performHaptic(HapticFeedbackType.TextHandleMove)
                                                onSelectThemeMode(AppThemeMode.ADAPTIVE_DEVICE)
                                            },
                                            modifier = Modifier
                                                .weight(1f)
                                                .testTag("theme_mode_device"),
                                        )
                                    }

                                    Text(
                                        text = when (preferences.themeMode) {
                                            AppThemeMode.LIGHT -> stringResource(R.string.settings_theme_light_desc)
                                            AppThemeMode.DARK -> stringResource(R.string.settings_theme_dark_desc)
                                            AppThemeMode.ADAPTIVE_DEVICE -> stringResource(R.string.settings_theme_device_desc)
                                        },
                                        style = MaterialTheme.typography.labelMedium,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                                    )
                                }

                                if (preferences.themeMode != AppThemeMode.ADAPTIVE_DEVICE &&
                                    Build.VERSION.SDK_INT >= Build.VERSION_CODES.S
                                ) {
                                    SettingsDivider()
                                    SettingsToggleRow(
                                        icon = Icons.Rounded.Palette,
                                        title = stringResource(R.string.settings_dynamic_color_title),
                                        subtitle = stringResource(R.string.settings_dynamic_color_subtitle),
                                        checked = preferences.dynamicColorInManualTheme,
                                        onCheckedChange = {
                                            performHaptic(HapticFeedbackType.TextHandleMove)
                                            onToggleDynamicColorInManualTheme(it)
                                        },
                                        testTag = "dynamic_color_switch",
                                    )
                                }

                                SettingsDivider()
                                SettingsToggleRow(
                                    icon = Icons.Rounded.Pin,
                                    title = stringResource(R.string.settings_show_decimals_title),
                                    subtitle = stringResource(R.string.settings_show_decimals_subtitle),
                                    checked = preferences.showDecimals,
                                    onCheckedChange = {
                                        performHaptic(HapticFeedbackType.TextHandleMove)
                                        onToggleShowDecimals(it)
                                    },
                                    testTag = "show_decimals_switch",
                                )

                                SettingsDivider()
                                SettingsToggleRow(
                                    icon = Icons.Rounded.ViewAgenda,
                                    title = stringResource(R.string.settings_compact_cards_title),
                                    subtitle = stringResource(R.string.settings_compact_cards_subtitle),
                                    checked = preferences.compactCards,
                                    onCheckedChange = {
                                        performHaptic(HapticFeedbackType.TextHandleMove)
                                        onToggleCompactCards(it)
                                    },
                                    testTag = "compact_cards_switch",
                                )

                                SettingsDivider()
                                SettingsToggleRow(
                                    icon = Icons.Rounded.Vibration,
                                    title = stringResource(R.string.settings_haptics_title),
                                    subtitle = stringResource(R.string.settings_haptics_subtitle),
                                    checked = preferences.hapticsEnabled,
                                    onCheckedChange = {
                                        onToggleHaptics(it)
                                    },
                                    testTag = "haptics_switch",
                                )

                                SettingsDivider()
                                SettingsToggleRow(
                                    icon = Icons.Rounded.Animation,
                                    title = stringResource(R.string.settings_reduce_motion_title),
                                    subtitle = stringResource(R.string.settings_reduce_motion_subtitle),
                                    checked = preferences.reduceMotion,
                                    onCheckedChange = {
                                        performHaptic(HapticFeedbackType.TextHandleMove)
                                        onToggleReduceMotion(it)
                                    },
                                    testTag = "reduce_motion_switch",
                                )
                            }
                        }
                    }

                    // 3. Budgeting, Alerts & Localization
                    item(key = "settings_budgeting") {
                        SettingsSectionTitle(stringResource(R.string.settings_section_budgeting))
                        Spacer(Modifier.height(8.dp))
                        Surface(
                            shape = RoundedCornerShape(28.dp),
                            color = MaterialTheme.colorScheme.surfaceContainer,
                            modifier = Modifier.fillMaxWidth(),
                        ) {
                            Column(modifier = Modifier.fillMaxWidth()) {
                                SettingsActionRow(
                                    icon = Icons.Rounded.CalendarMonth,
                                    title = stringResource(R.string.settings_reset_day_title),
                                    subtitle = if (preferences.budgetResetDay == 1) {
                                        stringResource(R.string.settings_reset_day_default)
                                    } else {
                                        stringResource(
                                            R.string.settings_reset_day_custom,
                                            LocalDay.ordinalDay(preferences.budgetResetDay),
                                        )
                                    },
                                    onClick = {
                                        performHaptic(HapticFeedbackType.TextHandleMove)
                                        showResetDayDialog = true
                                    },
                                    modifier = Modifier.testTag("settings_reset_day_row"),
                                )

                                SettingsDivider()

                                val activeCurrency = preferences.currency
                                SettingsActionRow(
                                    icon = Icons.Rounded.Public,
                                    title = stringResource(R.string.settings_currency_title),
                                    subtitle = "${activeCurrency.code} (${activeCurrency.symbol}) • ${activeCurrency.displayName}",
                                    onClick = {
                                        performHaptic(HapticFeedbackType.TextHandleMove)
                                        showCurrencyPicker = true
                                    },
                                    modifier = Modifier.testTag("settings_currency_row"),
                                )

                                SettingsDivider()

                                SettingsToggleRow(
                                    icon = Icons.Rounded.Repeat,
                                    title = stringResource(R.string.settings_reserve_bills_title),
                                    subtitle = stringResource(R.string.settings_reserve_bills_subtitle),
                                    checked = preferences.includeFixedBillsInSafeDaily,
                                    onCheckedChange = {
                                        performHaptic(HapticFeedbackType.TextHandleMove)
                                        onToggleIncludeFixedBills(it)
                                    },
                                    testTag = "reserve_bills_switch",
                                )

                                SettingsDivider()

                                // Warning Alert Threshold Slider
                                Column(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(horizontal = 16.dp, vertical = 14.dp),
                                    verticalArrangement = Arrangement.spacedBy(8.dp),
                                ) {
                                    Row(
                                        modifier = Modifier.fillMaxWidth(),
                                        verticalAlignment = Alignment.CenterVertically,
                                        horizontalArrangement = Arrangement.spacedBy(14.dp),
                                    ) {
                                        Box(
                                            modifier = Modifier
                                                .size(44.dp)
                                                .clip(CircleShape)
                                                .background(finColors.warningContainer),
                                            contentAlignment = Alignment.Center,
                                        ) {
                                            Icon(
                                                imageVector = Icons.Rounded.NotificationsActive,
                                                contentDescription = null,
                                                tint = finColors.onWarningContainer,
                                                modifier = Modifier.size(22.dp),
                                            )
                                        }
                                        Column(modifier = Modifier.weight(1f)) {
                                            Text(
                                                text = stringResource(R.string.settings_warning_threshold_title),
                                                style = MaterialTheme.typography.titleMedium,
                                            )
                                            Text(
                                                text = stringResource(
                                                    R.string.settings_warning_threshold_subtitle,
                                                    sliderDraft.roundToInt(),
                                                ),
                                                style = MaterialTheme.typography.labelMedium,
                                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                            )
                                        }
                                        Surface(
                                            shape = CircleShape,
                                            color = finColors.warningContainer,
                                        ) {
                                            Text(
                                                text = "${sliderDraft.roundToInt()}%",
                                                style = MaterialTheme.typography.labelLarge,
                                                fontWeight = FontWeight.Bold,
                                                color = finColors.onWarningContainer,
                                                modifier = Modifier.padding(horizontal = 10.dp, vertical = 5.dp),
                                            )
                                        }
                                    }
                                    Slider(
                                        value = sliderDraft,
                                        onValueChange = { sliderDraft = it },
                                        onValueChangeFinished = {
                                            performHaptic(HapticFeedbackType.TextHandleMove)
                                            onUpdateWarningThreshold(sliderDraft.roundToInt())
                                        },
                                        valueRange = 55f..95f,
                                        steps = 7,
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .testTag("warning_threshold_slider"),
                                    )
                                }
                            }
                        }
                    }

                    // 4. Data & Export Tools
                    item(key = "settings_data") {
                        SettingsSectionTitle(stringResource(R.string.settings_section_data))
                        Spacer(Modifier.height(8.dp))
                        Surface(
                            shape = RoundedCornerShape(28.dp),
                            color = MaterialTheme.colorScheme.surfaceContainer,
                            modifier = Modifier.fillMaxWidth(),
                        ) {
                            Column(modifier = Modifier.fillMaxWidth()) {
                                SettingsActionRow(
                                    icon = Icons.Rounded.Download,
                                    title = stringResource(R.string.settings_export_csv_title),
                                    subtitle = stringResource(
                                        R.string.settings_export_csv_subtitle,
                                        state.transactions.size,
                                    ),
                                    onClick = {
                                        performHaptic(HapticFeedbackType.TextHandleMove)
                                        exportCsvLauncher.launch("pixel_funds_${state.today.formatIsoDate()}.csv")
                                    },
                                    modifier = Modifier.testTag("settings_export_csv_row"),
                                )

                                SettingsDivider()

                                SettingsActionRow(
                                    icon = if (state.hasSampleData) Icons.Rounded.CleaningServices else Icons.Rounded.AutoAwesome,
                                    title = if (state.hasSampleData) stringResource(R.string.account_clear_sample_title)
                                    else stringResource(R.string.account_load_sample_title),
                                    subtitle = if (state.hasSampleData) stringResource(R.string.account_clear_sample_subtitle)
                                    else stringResource(R.string.account_load_sample_subtitle),
                                    onClick = {
                                        if (!state.isMutating && !isSigningIn) {
                                            performHaptic(HapticFeedbackType.LongPress)
                                            if (state.hasSampleData) onClearSampleData() else onRestoreSampleData()
                                        }
                                    },
                                    modifier = Modifier.testTag("sample_data_action_button"),
                                )

                                SettingsDivider()

                                SettingsActionRow(
                                    icon = Icons.Rounded.DeleteForever,
                                    title = stringResource(R.string.settings_clear_all_tx_title),
                                    subtitle = stringResource(R.string.settings_clear_all_tx_subtitle),
                                    iconBg = MaterialTheme.colorScheme.errorContainer,
                                    iconTint = MaterialTheme.colorScheme.onErrorContainer,
                                    onClick = {
                                        performHaptic(HapticFeedbackType.LongPress)
                                        showClearAllDialog = true
                                    },
                                    modifier = Modifier.testTag("settings_clear_all_tx_row"),
                                )
                            }
                        }
                    }

                    // 5. About Pixel Funds Summary Card
                    item(key = "settings_about") {
                        SettingsSectionTitle(stringResource(R.string.settings_section_about))
                        Spacer(Modifier.height(8.dp))
                        Surface(
                            shape = RoundedCornerShape(28.dp),
                            color = MaterialTheme.colorScheme.surfaceContainerLow,
                            modifier = Modifier.fillMaxWidth(),
                        ) {
                            Column(
                                modifier = Modifier.padding(18.dp),
                                verticalArrangement = Arrangement.spacedBy(8.dp),
                            ) {
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(10.dp),
                                ) {
                                    Icon(
                                        imageVector = Icons.Rounded.Info,
                                        contentDescription = null,
                                        tint = MaterialTheme.colorScheme.primary,
                                        modifier = Modifier.size(20.dp),
                                    )
                                    Text(
                                        text = "${stringResource(R.string.app_name)} • v1.0",
                                        style = MaterialTheme.typography.titleMedium,
                                    )
                                }
                                Text(
                                    text = stringResource(
                                        R.string.settings_about_summary,
                                        state.categories.size,
                                        state.transactions.size,
                                        state.activeRecurringItems.size,
                                    ),
                                    style = MaterialTheme.typography.bodyMedium,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                                )
                                Text(
                                    text = "Active cycle: ${state.cycleStart.formatShort()} – ${state.cycleEnd.formatShort()} • Currency: ${preferences.currency.code}",
                                    style = MaterialTheme.typography.labelMedium,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                                )
                            }
                        }
                    }
                }
            }
        }
    }

    if (showResetDayDialog) {
        ResetDayPickerDialog(
            currentDay = preferences.budgetResetDay,
            onDismiss = { showResetDayDialog = false },
            onSelectDay = { day ->
                onSelectResetDay(day)
                showResetDayDialog = false
            },
        )
    }

    if (showCurrencyPicker) {
        CurrencyPickerBottomSheet(
            selectedCode = preferences.currencyCode,
            onDismiss = { showCurrencyPicker = false },
            onSelectCurrency = { code ->
                onSelectCurrency(code)
                showCurrencyPicker = false
            },
        )
    }

    if (showClearAllDialog) {
        AlertDialog(
            onDismissRequest = { showClearAllDialog = false },
            shape = RoundedCornerShape(28.dp),
            icon = {
                Icon(
                    imageVector = Icons.Rounded.DeleteForever,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.error,
                )
            },
            title = { Text(stringResource(R.string.settings_clear_all_confirm_title)) },
            text = { Text(stringResource(R.string.settings_clear_all_confirm_body)) },
            dismissButton = {
                TextButton(onClick = { showClearAllDialog = false }) {
                    Text(stringResource(R.string.entry_cancel))
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        showClearAllDialog = false
                        onClearAllTransactions()
                    },
                    colors = ButtonDefaults.buttonColors(
                        containerColor = MaterialTheme.colorScheme.error,
                        contentColor = MaterialTheme.colorScheme.onError,
                    ),
                    shape = CircleShape,
                    modifier = Modifier.testTag("confirm_clear_all_tx_button"),
                ) {
                    Text(stringResource(R.string.settings_clear_all_confirm_action))
                }
            },
        )
    }
}

@Composable
private fun SettingsSectionTitle(title: String) {
    Text(
        text = title,
        style = MaterialTheme.typography.labelLarge,
        color = MaterialTheme.colorScheme.primary,
        modifier = Modifier.padding(horizontal = 8.dp),
    )
}

@Composable
private fun SettingsDivider() {
    HorizontalDivider(
        modifier = Modifier.padding(horizontal = 18.dp),
        color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.4f),
    )
}

@Composable
private fun SettingsToggleRow(
    icon: ImageVector,
    title: String,
    subtitle: String,
    checked: Boolean,
    onCheckedChange: (Boolean) -> Unit,
    testTag: String,
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onCheckedChange(!checked) }
            .padding(horizontal = 16.dp, vertical = 14.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(14.dp),
    ) {
        Box(
            modifier = Modifier
                .size(44.dp)
                .clip(CircleShape)
                .background(MaterialTheme.colorScheme.secondaryContainer),
            contentAlignment = Alignment.Center,
        ) {
            Icon(
                imageVector = icon,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.onSecondaryContainer,
                modifier = Modifier.size(21.dp),
            )
        }
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = title,
                style = MaterialTheme.typography.titleMedium,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
            Text(
                text = subtitle,
                style = MaterialTheme.typography.labelMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                maxLines = 2,
                overflow = TextOverflow.Ellipsis,
            )
        }
        Switch(
            checked = checked,
            onCheckedChange = onCheckedChange,
            modifier = Modifier.testTag(testTag),
        )
    }
}

@Composable
private fun ThemeModeSegment(
    label: String,
    icon: ImageVector,
    selected: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val bg by animateColorAsState(
        targetValue = if (selected) MaterialTheme.colorScheme.primary else Color.Transparent,
        animationSpec = ExpressiveMotion.fastEffects(),
        label = "ThemeSegBg_$label",
    )
    val fg by animateColorAsState(
        targetValue = if (selected) MaterialTheme.colorScheme.onPrimary
        else MaterialTheme.colorScheme.onSurfaceVariant,
        animationSpec = ExpressiveMotion.fastEffects(),
        label = "ThemeSegFg_$label",
    )
    Row(
        modifier = modifier
            .heightIn(min = 44.dp)
            .clip(CircleShape)
            .background(bg)
            .clickable(onClick = onClick)
            .padding(horizontal = 8.dp, vertical = 10.dp),
        horizontalArrangement = Arrangement.Center,
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Icon(
            imageVector = icon,
            contentDescription = null,
            tint = fg,
            modifier = Modifier.size(16.dp),
        )
        Spacer(Modifier.width(5.dp))
        Text(
            text = label,
            style = MaterialTheme.typography.labelLarge,
            color = fg,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
        )
    }
}

@Composable
private fun SettingsActionRow(
    icon: ImageVector,
    title: String,
    subtitle: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    iconBg: Color = MaterialTheme.colorScheme.primaryContainer,
    iconTint: Color = MaterialTheme.colorScheme.onPrimaryContainer,
) {
    Row(
        modifier = modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
            .padding(horizontal = 16.dp, vertical = 15.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(14.dp),
    ) {
        Box(
            modifier = Modifier
                .size(44.dp)
                .clip(CircleShape)
                .background(iconBg),
            contentAlignment = Alignment.Center,
        ) {
            Icon(
                imageVector = icon,
                contentDescription = null,
                tint = iconTint,
                modifier = Modifier.size(22.dp),
            )
        }
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = title,
                style = MaterialTheme.typography.titleMedium,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
            Text(
                text = subtitle,
                style = MaterialTheme.typography.labelMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                maxLines = 2,
                overflow = TextOverflow.Ellipsis,
            )
        }
        Icon(
            imageVector = Icons.AutoMirrored.Rounded.KeyboardArrowRight,
            contentDescription = null,
            tint = MaterialTheme.colorScheme.onSurfaceVariant,
        )
    }
}

@Composable
fun ResetDayPickerDialog(
    currentDay: Int,
    onDismiss: () -> Unit,
    onSelectDay: (Int) -> Unit,
) {
    val performHaptic = rememberPixelHaptics()
    var draftDay by remember(currentDay) { mutableStateOf(currentDay.coerceIn(1, 31)) }

    AlertDialog(
        onDismissRequest = onDismiss,
        shape = RoundedCornerShape(32.dp),
        title = {
            Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                Text(
                    text = stringResource(R.string.settings_reset_day_picker_title),
                    style = MaterialTheme.typography.titleLarge,
                )
                Text(
                    text = stringResource(
                        R.string.settings_reset_day_picker_subtitle,
                        LocalDay.ordinalDay(draftDay),
                    ),
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.primary,
                    fontWeight = FontWeight.Medium,
                )
            }
        },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                val rows = remember { (1..31).chunked(7) }
                Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                    rows.forEach { week ->
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(4.dp),
                        ) {
                            week.forEach { day ->
                                val selected = day == draftDay
                                Box(
                                    modifier = Modifier
                                        .weight(1f)
                                        .height(40.dp)
                                        .clip(CircleShape)
                                        .background(
                                            if (selected) MaterialTheme.colorScheme.primary
                                            else MaterialTheme.colorScheme.surfaceContainerHigh
                                        )
                                        .clickable {
                                            performHaptic(HapticFeedbackType.TextHandleMove)
                                            draftDay = day
                                        }
                                        .testTag("reset_day_$day"),
                                    contentAlignment = Alignment.Center,
                                ) {
                                    Text(
                                        text = day.toString(),
                                        style = MaterialTheme.typography.labelLarge,
                                        color = if (selected) MaterialTheme.colorScheme.onPrimary
                                        else MaterialTheme.colorScheme.onSurface,
                                    )
                                }
                            }
                            repeat(7 - week.size) {
                                Spacer(Modifier.weight(1f))
                            }
                        }
                    }
                }
                Text(
                    text = stringResource(R.string.settings_reset_day_note),
                    style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
        },
        dismissButton = {
            if (draftDay != 1) {
                TextButton(
                    onClick = {
                        performHaptic(HapticFeedbackType.TextHandleMove)
                        onSelectDay(1)
                    },
                    modifier = Modifier.testTag("reset_day_default_button"),
                ) {
                    Text(stringResource(R.string.settings_reset_to_first))
                }
            } else {
                TextButton(onClick = onDismiss) {
                    Text(stringResource(R.string.entry_cancel))
                }
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    performHaptic(HapticFeedbackType.LongPress)
                    onSelectDay(draftDay)
                },
                shape = CircleShape,
                modifier = Modifier.testTag("reset_day_save_button"),
            ) {
                Text(stringResource(R.string.settings_save_day))
            }
        },
    )
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun CurrencyPickerBottomSheet(
    selectedCode: String,
    onDismiss: () -> Unit,
    onSelectCurrency: (String) -> Unit,
) {
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    val performHaptic = rememberPixelHaptics()
    var query by remember { mutableStateOf("") }

    val allCurrencies by produceState<List<CurrencyOption>?>(initialValue = null) {
        value = withContext(Dispatchers.Default) {
            WorldCurrencyCatalog.allCurrencies
        }
    }
    val filtered = remember(query, allCurrencies, selectedCode) {
        val list = allCurrencies ?: return@remember emptyList()
        val q = query.trim().lowercase(Locale.ROOT)
        val matches = if (q.isEmpty()) {
            list
        } else {
            list.filter { it.searchIndex.contains(q) }
        }
        if (q.isEmpty()) {
            val active = matches.find { it.code == selectedCode }
            if (active != null) listOf(active) + matches.filterNot { it.code == selectedCode }
            else matches
        } else {
            matches
        }
    }

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = sheetState,
        shape = RoundedCornerShape(topStart = 36.dp, topEnd = 36.dp),
        containerColor = MaterialTheme.colorScheme.surfaceContainerLow,
        dragHandle = { BottomSheetDefaults.DragHandle() },
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .fillMaxHeight(0.86f)
                .navigationBarsPadding()
                .padding(horizontal = 20.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            Column {
                Text(
                    text = stringResource(R.string.settings_currency_picker_title),
                    style = MaterialTheme.typography.headlineMedium,
                )
                val loadedCount = allCurrencies?.size ?: 0
                if (loadedCount > 0) {
                    Text(
                        text = stringResource(R.string.settings_currency_count, loadedCount),
                        style = MaterialTheme.typography.labelMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
            }

            OutlinedTextField(
                value = query,
                onValueChange = { query = it },
                placeholder = { Text(stringResource(R.string.settings_currency_search_hint)) },
                leadingIcon = { Icon(Icons.Rounded.Search, contentDescription = null) },
                trailingIcon = {
                    if (query.isNotEmpty()) {
                        IconButton(onClick = { query = "" }) {
                            Icon(
                                imageVector = Icons.Rounded.Close,
                                contentDescription = stringResource(R.string.cd_clear_search),
                            )
                        }
                    }
                },
                singleLine = true,
                shape = CircleShape,
                modifier = Modifier
                    .fillMaxWidth()
                    .testTag("currency_search_input"),
                colors = OutlinedTextFieldDefaults.colors(
                    unfocusedContainerColor = MaterialTheme.colorScheme.surfaceContainerHigh,
                    focusedContainerColor = MaterialTheme.colorScheme.surfaceContainerHighest,
                    unfocusedBorderColor = Color.Transparent,
                ),
            )

            if (allCurrencies == null) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .weight(1f),
                    contentAlignment = Alignment.Center,
                ) {
                    CircularProgressIndicator()
                }
            } else if (filtered.isEmpty()) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 32.dp),
                    contentAlignment = Alignment.Center,
                ) {
                    Text(
                        text = stringResource(R.string.settings_currency_no_results),
                        style = MaterialTheme.typography.bodyLarge,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
            } else {
                LazyColumn(
                    modifier = Modifier.weight(1f),
                    contentPadding = PaddingValues(bottom = 24.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp),
                ) {
                    items(filtered, key = { it.code }) { option ->
                        CurrencyRowItem(
                            option = option,
                            selected = option.code == selectedCode,
                            onClick = {
                                performHaptic(HapticFeedbackType.TextHandleMove)
                                onSelectCurrency(option.code)
                            },
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun CurrencyRowItem(
    option: CurrencyOption,
    selected: Boolean,
    onClick: () -> Unit,
) {
    Surface(
        onClick = onClick,
        shape = RoundedCornerShape(22.dp),
        color = if (selected) MaterialTheme.colorScheme.primaryContainer
        else MaterialTheme.colorScheme.surfaceContainer,
        modifier = Modifier
            .fillMaxWidth()
            .then(
                if (selected) Modifier.border(
                    width = 1.5.dp,
                    color = MaterialTheme.colorScheme.primary,
                    shape = RoundedCornerShape(22.dp),
                ) else Modifier
            )
            .testTag("currency_option_${option.code}"),
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 14.dp, vertical = 12.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            Box(
                modifier = Modifier
                    .size(44.dp)
                    .clip(CircleShape)
                    .background(
                        if (selected) MaterialTheme.colorScheme.primary
                        else MaterialTheme.colorScheme.surfaceContainerHighest
                    ),
                contentAlignment = Alignment.Center,
            ) {
                Text(
                    text = option.symbol,
                    style = if (option.symbol.length <= 2) MaterialTheme.typography.titleMedium
                    else MaterialTheme.typography.labelMedium,
                    color = if (selected) MaterialTheme.colorScheme.onPrimary
                    else MaterialTheme.colorScheme.onSurface,
                    fontWeight = FontWeight.Bold,
                    maxLines = 1,
                )
            }

            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = option.displayName,
                    style = MaterialTheme.typography.titleSmall,
                    color = if (selected) MaterialTheme.colorScheme.onPrimaryContainer
                    else MaterialTheme.colorScheme.onSurface,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
                if (option.regions.isNotBlank()) {
                    Text(
                        text = option.regions,
                        style = MaterialTheme.typography.labelMedium,
                        color = if (selected) MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.78f)
                        else MaterialTheme.colorScheme.onSurfaceVariant,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                    )
                }
            }

            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(6.dp),
            ) {
                Text(
                    text = option.code,
                    style = MaterialTheme.typography.labelLarge,
                    fontWeight = FontWeight.Bold,
                    color = if (selected) MaterialTheme.colorScheme.primary
                    else MaterialTheme.colorScheme.onSurfaceVariant,
                )
                if (selected) {
                    Icon(
                        imageVector = Icons.Rounded.Check,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.size(18.dp),
                    )
                }
            }
        }
    }
}
