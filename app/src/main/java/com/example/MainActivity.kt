package com.example

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.graphics.Color
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.SystemBarStyle
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.getValue
import androidx.compose.ui.platform.LocalContext
import androidx.core.content.ContextCompat
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.ViewModelProvider.AndroidViewModelFactory.Companion.APPLICATION_KEY
import androidx.lifecycle.compose.LocalLifecycleOwner
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import com.example.data.AppPreferencesRepository
import com.example.data.AppThemeMode
import com.example.data.UserPreferences
import com.example.data.local.LocalBudgetRepository
import com.example.data.local.MonthlyBudgetOverviewRepository
import com.example.data.local.PixelFundsDatabase
import com.example.ui.CadenceApp
import com.example.ui.CadenceViewModel
import com.example.ui.MonthlyBudgetOverviewViewModel
import com.example.ui.theme.CadenceTheme

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        val preferencesRepository = AppPreferencesRepository(applicationContext)

        setContent {
            val prefs by preferencesRepository.preferencesFlow.collectAsStateWithLifecycle(
                initialValue = UserPreferences(),
            )
            val systemDark = isSystemInDarkTheme()
            val darkTheme = when (prefs.themeMode) {
                AppThemeMode.LIGHT -> false
                AppThemeMode.DARK -> true
                AppThemeMode.ADAPTIVE_DEVICE -> systemDark
            }
            val dynamicColor = when (prefs.themeMode) {
                AppThemeMode.ADAPTIVE_DEVICE -> true
                AppThemeMode.LIGHT, AppThemeMode.DARK -> prefs.dynamicColorInManualTheme
            }

            DisposableEffect(darkTheme) {
                enableEdgeToEdge(
                    statusBarStyle = if (darkTheme) {
                        SystemBarStyle.dark(Color.TRANSPARENT)
                    } else {
                        SystemBarStyle.light(Color.TRANSPARENT, Color.TRANSPARENT)
                    },
                    navigationBarStyle = if (darkTheme) {
                        SystemBarStyle.dark(Color.TRANSPARENT)
                    } else {
                        SystemBarStyle.light(Color.TRANSPARENT, Color.TRANSPARENT)
                    },
                )
                onDispose {}
            }

            CadenceTheme(
                darkTheme = darkTheme,
                dynamicColor = dynamicColor,
                preferences = prefs,
            ) {
                PixelFundsRootNavigation(
                    preferencesRepository = preferencesRepository,
                )
            }
        }
    }

    override fun onAttachedToWindow() {
        super.onAttachedToWindow()
        enablePixelSmoothDisplay()
    }

    private fun enablePixelSmoothDisplay() {
        if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.R) {
            runCatching {
                val currentDisplay = window?.decorView?.display ?: display ?: return
                val currentMode = currentDisplay.mode ?: return
                val bestMode = currentDisplay.supportedModes
                    .filter {
                        it.physicalWidth == currentMode.physicalWidth &&
                            it.physicalHeight == currentMode.physicalHeight
                    }
                    .maxByOrNull { it.refreshRate }
                if (bestMode != null && bestMode.modeId != currentMode.modeId) {
                    val attrs = window.attributes
                    attrs.preferredDisplayModeId = bestMode.modeId
                    window.attributes = attrs
                }
            }
        }
    }
}

@Composable
private fun PixelFundsRootNavigation(
    preferencesRepository: AppPreferencesRepository,
) {
    val context = LocalContext.current

    val viewModel: CadenceViewModel = viewModel(
        key = "local_device_budget",
        factory = viewModelFactory {
            initializer {
                val app = checkNotNull(this[APPLICATION_KEY]) {
                    "APPLICATION_KEY missing from CreationExtras"
                }
                CadenceViewModel(
                    repository = LocalBudgetRepository(app),
                    preferencesRepository = preferencesRepository,
                )
            }
        },
    )

    val overviewViewModel: MonthlyBudgetOverviewViewModel = viewModel(
        key = "local_monthly_overview",
        factory = viewModelFactory {
            initializer {
                val app = checkNotNull(this[APPLICATION_KEY]) {
                    "APPLICATION_KEY missing from CreationExtras"
                }
                val overviewDao = PixelFundsDatabase.getInstance(app).monthlyBudgetOverviewDao()
                MonthlyBudgetOverviewViewModel(
                    repository = MonthlyBudgetOverviewRepository(overviewDao),
                    preferencesRepository = preferencesRepository,
                )
            }
        },
    )

    val lifecycleOwner = LocalLifecycleOwner.current
    DisposableEffect(viewModel, overviewViewModel, lifecycleOwner, context) {
        viewModel.syncDeviceDate()
        overviewViewModel.refreshDate()
        val lifecycleObserver = LifecycleEventObserver { _, event ->
            if (event == Lifecycle.Event.ON_RESUME) {
                viewModel.syncDeviceDate()
                overviewViewModel.refreshDate()
            }
        }
        lifecycleOwner.lifecycle.addObserver(lifecycleObserver)

        val dateChangeReceiver = object : BroadcastReceiver() {
            override fun onReceive(ctx: Context?, intent: Intent?) {
                viewModel.syncDeviceDate()
                overviewViewModel.refreshDate()
            }
        }
        val filter = IntentFilter().apply {
            addAction(Intent.ACTION_DATE_CHANGED)
            addAction(Intent.ACTION_TIME_CHANGED)
            addAction(Intent.ACTION_TIMEZONE_CHANGED)
        }
        ContextCompat.registerReceiver(
            context,
            dateChangeReceiver,
            filter,
            ContextCompat.RECEIVER_NOT_EXPORTED,
        )

        onDispose {
            lifecycleOwner.lifecycle.removeObserver(lifecycleObserver)
            runCatching { context.unregisterReceiver(dateChangeReceiver) }
        }
    }

    CadenceApp(
        viewModel = viewModel,
        overviewViewModel = overviewViewModel,
    )
}
