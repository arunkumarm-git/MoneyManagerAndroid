package com.moneymanagement.app

import android.Manifest
import android.content.pm.PackageManager
import android.os.Build
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.runtime.SideEffect
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalView
import androidx.core.content.ContextCompat
import androidx.core.view.WindowCompat
import com.moneymanagement.app.ui.nav.AppRoot
import com.moneymanagement.app.ui.theme.MoneyManagerTheme

import androidx.fragment.app.FragmentActivity
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.SystemUpdate
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.ui.Alignment
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.moneymanagement.app.security.BiometricAuthManager
import com.moneymanagement.app.settings.AppSettings
import com.moneymanagement.app.ui.security.AppLockScreen
import com.moneymanagement.app.update.InAppUpdateManager

class MainActivity : FragmentActivity() {
    private val requestNotificationPermission =
        registerForActivityResult(ActivityResultContracts.RequestPermission()) { /* no-op either way */ }

    private lateinit var inAppUpdateManager: InAppUpdateManager

    private val updateResultLauncher =
        registerForActivityResult(ActivityResultContracts.StartIntentSenderForResult()) { result ->
            if (result.resultCode != RESULT_OK) {
                // Update flow cancelled or failed by user
            }
        }

    private var isUnlocked by mutableStateOf(false)
    private var pendingRoute by mutableStateOf<String?>(null)

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        requestNotificationPermissionIfNeeded()
        pendingRoute = intent?.getStringExtra("navigate_to")
        val repository = (application as MoneyManagerApplication).repository

        inAppUpdateManager = InAppUpdateManager(this)
        inAppUpdateManager.registerListener()

        if (!AppSettings.biometricLockEnabled.value) {
            isUnlocked = true
            inAppUpdateManager.checkForUpdate(updateResultLauncher)
        } else {
            promptBiometric()
        }

        setContent {
            val themeMode by AppSettings.themeMode.collectAsState()
            val isSystemDark = isSystemInDarkTheme()
            val darkTheme = when (themeMode) {
                "light" -> false
                "dark" -> true
                else -> isSystemDark
            }
            val view = LocalView.current
            val biometricEnabled by AppSettings.biometricLockEnabled.collectAsState()
            val isUpdateDownloaded by inAppUpdateManager.isUpdateDownloaded.collectAsState()

            SideEffect {
                WindowCompat.getInsetsController(window, view).isAppearanceLightStatusBars = !darkTheme
                WindowCompat.getInsetsController(window, view).isAppearanceLightNavigationBars = !darkTheme
            }
            MoneyManagerTheme(darkTheme = darkTheme) {
                Surface(modifier = Modifier.fillMaxSize(), color = MaterialTheme.colorScheme.background) {
                    Box(modifier = Modifier.fillMaxSize()) {
                        if (biometricEnabled && !isUnlocked) {
                            AppLockScreen(onUnlockClick = { promptBiometric() })
                        } else {
                            AppRoot(
                                repository = repository,
                                initialRoute = pendingRoute,
                                onCheckForUpdates = {
                                    inAppUpdateManager.checkForUpdate(updateResultLauncher, isUserInitiated = true)
                                },
                            )
                        }

                        if (isUpdateDownloaded) {
                            UpdateDownloadedBanner(
                                onRestartClick = { inAppUpdateManager.completeUpdate() },
                                modifier = Modifier
                                    .align(Alignment.BottomCenter)
                                    .padding(bottom = 84.dp, start = 16.dp, end = 16.dp),
                            )
                        }
                    }
                }
            }
        }
    }

    override fun onResume() {
        super.onResume()
        if (::inAppUpdateManager.isInitialized) {
            inAppUpdateManager.checkAndResumeIfDownloaded()
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        if (::inAppUpdateManager.isInitialized) {
            inAppUpdateManager.unregisterListener()
        }
    }

    override fun onNewIntent(intent: android.content.Intent) {
        super.onNewIntent(intent)
        setIntent(intent)
        intent.getStringExtra("navigate_to")?.let { pendingRoute = it }
    }

    private fun promptBiometric() {
        if (!BiometricAuthManager.isBiometricAvailable(this)) {
            isUnlocked = true
            inAppUpdateManager.checkForUpdate(updateResultLauncher)
            return
        }
        BiometricAuthManager.authenticate(
            activity = this,
            title = "Unlock Money Manager",
            subtitle = "Verify your identity to view your finances",
            onSuccess = {
                isUnlocked = true
                inAppUpdateManager.checkForUpdate(updateResultLauncher)
            },
            onError = { /* Keep locked */ },
        )
    }

    private fun requestNotificationPermissionIfNeeded() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU &&
            ContextCompat.checkSelfPermission(this, Manifest.permission.POST_NOTIFICATIONS) !=
            PackageManager.PERMISSION_GRANTED
        ) {
            requestNotificationPermission.launch(Manifest.permission.POST_NOTIFICATIONS)
        }
    }
}

@Composable
private fun UpdateDownloadedBanner(
    onRestartClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Surface(
        shape = RoundedCornerShape(16.dp),
        color = MaterialTheme.colorScheme.primaryContainer,
        shadowElevation = 8.dp,
        modifier = modifier.fillMaxWidth(),
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 16.dp, vertical = 12.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween,
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.weight(1f),
            ) {
                Icon(
                    Icons.Filled.SystemUpdate,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.onPrimaryContainer,
                    modifier = Modifier.padding(end = 12.dp),
                )
                Column {
                    Text(
                        "Update ready to install",
                        style = MaterialTheme.typography.titleSmall,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onPrimaryContainer,
                    )
                    Text(
                        "Restart to complete installation",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.8f),
                    )
                }
            }
            Button(
                onClick = onRestartClick,
                colors = ButtonDefaults.buttonColors(
                    containerColor = MaterialTheme.colorScheme.primary,
                    contentColor = MaterialTheme.colorScheme.onPrimary,
                ),
            ) {
                Text("Restart")
            }
        }
    }
}
