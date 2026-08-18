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
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import com.moneymanagement.app.security.BiometricAuthManager
import com.moneymanagement.app.settings.AppSettings
import com.moneymanagement.app.ui.security.AppLockScreen

class MainActivity : FragmentActivity() {
    private val requestNotificationPermission =
        registerForActivityResult(ActivityResultContracts.RequestPermission()) { /* no-op either way */ }

    private var isUnlocked by mutableStateOf(false)

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        requestNotificationPermissionIfNeeded()
        val repository = (application as MoneyManagerApplication).repository

        if (!AppSettings.biometricLockEnabled.value) {
            isUnlocked = true
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

            SideEffect {
                WindowCompat.getInsetsController(window, view).isAppearanceLightStatusBars = !darkTheme
                WindowCompat.getInsetsController(window, view).isAppearanceLightNavigationBars = !darkTheme
            }
            MoneyManagerTheme(darkTheme = darkTheme) {
                Surface(modifier = Modifier.fillMaxSize(), color = MaterialTheme.colorScheme.background) {
                    if (biometricEnabled && !isUnlocked) {
                        AppLockScreen(onUnlockClick = { promptBiometric() })
                    } else {
                        AppRoot(repository)
                    }
                }
            }
        }
    }

    private fun promptBiometric() {
        if (!BiometricAuthManager.isBiometricAvailable(this)) {
            isUnlocked = true
            return
        }
        BiometricAuthManager.authenticate(
            activity = this,
            title = "Unlock Money Manager",
            subtitle = "Verify your identity to view your finances",
            onSuccess = { isUnlocked = true },
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
