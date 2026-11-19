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
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.key
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.platform.LocalContext
import androidx.core.content.ContextCompat
import androidx.credentials.CredentialManager
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
import com.example.data.CadenceRepository
import com.example.data.UserPreferences
import com.example.data.local.LocalBudgetRepository
import com.example.ui.CadenceApp
import com.example.ui.CadenceViewModel
import com.example.ui.auth.attemptAutoSignIn
import com.example.ui.auth.getFirebaseAuthOrNull
import com.example.ui.theme.CadenceTheme
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.FirebaseUser
import com.google.firebase.firestore.FirebaseFirestore
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.flow.flowOf

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        runCatching { enablePixelSmoothDisplay() }
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

    private fun enablePixelSmoothDisplay() {
        if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.R) {
            val currentDisplay = display ?: return
            val currentMode = currentDisplay.mode
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

private fun FirebaseAuth.authStateFlow(): Flow<FirebaseUser?> = callbackFlow {
    val listener = FirebaseAuth.AuthStateListener { firebaseAuth ->
        trySend(firebaseAuth.currentUser)
    }
    addAuthStateListener(listener)
    awaitClose { removeAuthStateListener(listener) }
}

@Composable
private fun PixelFundsRootNavigation(
    preferencesRepository: AppPreferencesRepository,
    auth: FirebaseAuth? = null,
) {
    val context = LocalContext.current
    val firebaseAuth = remember(context, auth) { auth ?: getFirebaseAuthOrNull(context) }
    val scope = rememberCoroutineScope()
    val credentialManager = remember(context) { CredentialManager.create(context) }
    val authFlow = remember(firebaseAuth) { firebaseAuth?.authStateFlow() ?: flowOf(null) }
    val currentUser by authFlow.collectAsStateWithLifecycle(initialValue = firebaseAuth?.currentUser)

    // Non-blocking silent check for previously authorized Google accounts;
    // users remain immediately in local on-device mode if not signed in or if Firebase is unconfigured.
    LaunchedEffect(firebaseAuth) {
        if (firebaseAuth != null) {
            attemptAutoSignIn(
                context = context,
                credentialManager = credentialManager,
                onAuthSuccess = {},
                onUnauthenticated = {},
                scope = scope,
            )
        }
    }

    val user = currentUser
    val sessionKey = if (user != null) "cloud_${user.uid}" else "local_device"

    key(sessionKey) {
        val viewModel: CadenceViewModel = viewModel(
            key = sessionKey,
            factory = viewModelFactory {
                initializer {
                    val app = checkNotNull(this[APPLICATION_KEY]) {
                        "APPLICATION_KEY missing from CreationExtras"
                    }
                    val localRepository = LocalBudgetRepository(app)
                    val repository = if (user != null) {
                        val databaseId = app.getString(R.string.firestore_database_id)
                        val db = FirebaseFirestore.getInstance(databaseId)
                        CadenceRepository(
                            db = db,
                            userId = user.uid,
                            localMigrationSource = localRepository,
                        )
                    } else {
                        localRepository
                    }
                    CadenceViewModel(
                        repository = repository,
                        preferencesRepository = preferencesRepository,
                    )
                }
            },
        )
        val lifecycleOwner = LocalLifecycleOwner.current
        DisposableEffect(viewModel, lifecycleOwner, context) {
            viewModel.syncDeviceDate()
            val lifecycleObserver = LifecycleEventObserver { _, event ->
                if (event == Lifecycle.Event.ON_RESUME) {
                    viewModel.syncDeviceDate()
                }
            }
            lifecycleOwner.lifecycle.addObserver(lifecycleObserver)

            val dateChangeReceiver = object : BroadcastReceiver() {
                override fun onReceive(ctx: Context?, intent: Intent?) {
                    viewModel.syncDeviceDate()
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
            signedInUser = user,
        )
    }
}
