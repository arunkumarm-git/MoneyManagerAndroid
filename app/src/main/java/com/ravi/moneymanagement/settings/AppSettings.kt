package com.ravi.moneymanagement.settings

import android.content.Context
import android.content.SharedPreferences
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow

/**
 * Singleton holding all app-wide user preferences: sound toggle, currency symbol,
 * and theme mode. Backed by SharedPreferences.
 */
object AppSettings {
    private const val PREFS_NAME = "app_settings"
    private const val KEY_SOUND_ENABLED = "sound_enabled"
    private const val KEY_CURRENCY_SYMBOL = "currency_symbol"
    private const val KEY_THEME_MODE = "theme_mode"
    private const val KEY_BIOMETRIC_LOCK = "biometric_lock"
    private const val KEY_PRIVACY_MODE = "privacy_mode"

    private lateinit var prefs: SharedPreferences

    // Sound
    private val _soundEnabled = MutableStateFlow(true)
    val soundEnabled: StateFlow<Boolean> = _soundEnabled

    // Currency
    private val _currencySymbol = MutableStateFlow("₹")
    val currencySymbol: StateFlow<String> = _currencySymbol

    // Theme: "system", "light", or "dark"
    private val _themeMode = MutableStateFlow("system")
    val themeMode: StateFlow<String> = _themeMode

    // Biometric Security Lock
    private val _biometricLockEnabled = MutableStateFlow(false)
    val biometricLockEnabled: StateFlow<Boolean> = _biometricLockEnabled

    // Privacy Mask Mode (masks dashboard balances)
    private val _privacyModeEnabled = MutableStateFlow(false)
    val privacyModeEnabled: StateFlow<Boolean> = _privacyModeEnabled

    val supportedCurrencies = listOf("₹", "$", "€", "£", "¥")

    fun init(context: Context) {
        prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        _soundEnabled.value = prefs.getBoolean(KEY_SOUND_ENABLED, true)
        _currencySymbol.value = prefs.getString(KEY_CURRENCY_SYMBOL, "₹") ?: "₹"
        _themeMode.value = prefs.getString(KEY_THEME_MODE, "system") ?: "system"
        _biometricLockEnabled.value = prefs.getBoolean(KEY_BIOMETRIC_LOCK, false)
        _privacyModeEnabled.value = prefs.getBoolean(KEY_PRIVACY_MODE, false)
    }

    fun setSoundEnabled(value: Boolean) {
        _soundEnabled.value = value
        prefs.edit().putBoolean(KEY_SOUND_ENABLED, value).apply()
    }

    fun setCurrencySymbol(symbol: String) {
        _currencySymbol.value = symbol
        prefs.edit().putString(KEY_CURRENCY_SYMBOL, symbol).apply()
    }

    fun setThemeMode(mode: String) {
        _themeMode.value = mode
        prefs.edit().putString(KEY_THEME_MODE, mode).apply()
    }

    fun setBiometricLockEnabled(enabled: Boolean) {
        _biometricLockEnabled.value = enabled
        prefs.edit().putBoolean(KEY_BIOMETRIC_LOCK, enabled).apply()
    }

    fun setPrivacyModeEnabled(enabled: Boolean) {
        _privacyModeEnabled.value = enabled
        prefs.edit().putBoolean(KEY_PRIVACY_MODE, enabled).apply()
    }
}
