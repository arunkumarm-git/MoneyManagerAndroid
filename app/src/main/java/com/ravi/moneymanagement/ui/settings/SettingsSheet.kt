package com.ravi.moneymanagement.ui.settings

import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.DarkMode
import androidx.compose.material.icons.filled.FileDownload
import androidx.compose.material.icons.filled.FileUpload
import androidx.compose.material.icons.filled.Fingerprint
import androidx.compose.material.icons.filled.Paid
import androidx.compose.material.icons.filled.Restore
import androidx.compose.material.icons.filled.TableChart
import androidx.compose.material.icons.filled.Visibility
import androidx.compose.material.icons.filled.VolumeUp
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Divider
import androidx.compose.material3.ElevatedCard
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.ravi.moneymanagement.data.MoneyRepository
import com.ravi.moneymanagement.security.BiometricAuthManager
import com.ravi.moneymanagement.settings.AppSettings
import com.ravi.moneymanagement.ui.common.ClickSound
import com.ravi.moneymanagement.ui.common.EditSheet
import com.ravi.moneymanagement.ui.common.soundClick
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

@Composable
fun SettingsSheet(
    repository: MoneyRepository,
    onDismiss: () -> Unit,
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()

    val soundEnabled by AppSettings.soundEnabled.collectAsState()
    val currencySymbol by AppSettings.currencySymbol.collectAsState()
    val themeMode by AppSettings.themeMode.collectAsState()
    val biometricEnabled by AppSettings.biometricLockEnabled.collectAsState()
    val privacyMode by AppSettings.privacyModeEnabled.collectAsState()

    var pendingRestoreJson by remember { mutableStateOf<String?>(null) }
    var isProcessing by remember { mutableStateOf(false) }

    // Export CSV Launcher
    val exportCsvLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.CreateDocument("text/csv")
    ) { uri ->
        if (uri != null) {
            scope.launch {
                try {
                    val transactions = repository.transactions.first()
                    val csv = com.ravi.moneymanagement.data.backup.CsvHelper.exportToCsv(transactions)
                    withContext(Dispatchers.IO) {
                        context.contentResolver.openOutputStream(uri)?.use { os ->
                            os.write(csv.toByteArray(Charsets.UTF_8))
                        }
                    }
                    Toast.makeText(context, "CSV exported successfully!", Toast.LENGTH_SHORT).show()
                } catch (e: Exception) {
                    Toast.makeText(context, "Export failed: ${e.localizedMessage}", Toast.LENGTH_LONG).show()
                }
            }
        }
    }

    // Export JSON Launcher
    val exportJsonLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.CreateDocument("application/json")
    ) { uri ->
        if (uri != null) {
            scope.launch {
                try {
                    val json = repository.exportBackupJson()
                    withContext(Dispatchers.IO) {
                        context.contentResolver.openOutputStream(uri)?.use { os ->
                            os.write(json.toByteArray(Charsets.UTF_8))
                        }
                    }
                    Toast.makeText(context, "Backup exported successfully!", Toast.LENGTH_SHORT).show()
                } catch (e: Exception) {
                    Toast.makeText(context, "Backup failed: ${e.localizedMessage}", Toast.LENGTH_LONG).show()
                }
            }
        }
    }

    // Import CSV Launcher
    val importCsvLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.OpenDocument()
    ) { uri ->
        if (uri != null) {
            scope.launch {
                try {
                    val content = withContext(Dispatchers.IO) {
                        context.contentResolver.openInputStream(uri)?.bufferedReader()?.use { it.readText() } ?: ""
                    }
                    val count = repository.importTransactionsCsv(content)
                    Toast.makeText(context, "Imported $count transactions!", Toast.LENGTH_SHORT).show()
                } catch (e: Exception) {
                    Toast.makeText(context, "CSV Import failed: ${e.localizedMessage}", Toast.LENGTH_LONG).show()
                }
            }
        }
    }

    // Restore Backup JSON Launcher
    val restoreJsonLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.OpenDocument()
    ) { uri ->
        if (uri != null) {
            scope.launch {
                try {
                    val content = withContext(Dispatchers.IO) {
                        context.contentResolver.openInputStream(uri)?.bufferedReader()?.use { it.readText() } ?: ""
                    }
                    pendingRestoreJson = content
                } catch (e: Exception) {
                    Toast.makeText(context, "Could not read backup file: ${e.localizedMessage}", Toast.LENGTH_LONG).show()
                }
            }
        }
    }

    EditSheet(onDismiss = onDismiss) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .verticalScroll(rememberScrollState()),
            verticalArrangement = Arrangement.spacedBy(16.dp),
        ) {
            Text("Settings & Data", style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold)

            // --- Section: Preferences ---
            Text("Preferences", style = MaterialTheme.typography.titleSmall, color = MaterialTheme.colorScheme.primary)

            // Button Click Sound
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.weight(1f)) {
                    Icon(Icons.Filled.VolumeUp, contentDescription = null, tint = MaterialTheme.colorScheme.primary, modifier = Modifier.padding(end = 12.dp))
                    Column {
                        Text("Button click sound", style = MaterialTheme.typography.bodyLarge)
                        Text("Play a subtle sound on interactions", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                }
                Switch(
                    checked = soundEnabled,
                    onCheckedChange = { checked ->
                        if (checked) ClickSound.play()
                        AppSettings.setSoundEnabled(checked)
                    },
                )
            }

            // Currency Symbol
            Column(modifier = Modifier.fillMaxWidth()) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(Icons.Filled.Paid, contentDescription = null, tint = MaterialTheme.colorScheme.primary, modifier = Modifier.padding(end = 12.dp))
                    Text("Currency Symbol", style = MaterialTheme.typography.bodyLarge)
                }
                Row(
                    modifier = Modifier.fillMaxWidth().padding(top = 8.dp),
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                ) {
                    AppSettings.supportedCurrencies.forEach { symbol ->
                        FilterChip(
                            selected = currencySymbol == symbol,
                            onClick = soundClick { AppSettings.setCurrencySymbol(symbol) },
                            label = { Text(symbol, fontWeight = FontWeight.Bold) },
                        )
                    }
                }
            }

            // Theme Mode
            Column(modifier = Modifier.fillMaxWidth()) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(Icons.Filled.DarkMode, contentDescription = null, tint = MaterialTheme.colorScheme.primary, modifier = Modifier.padding(end = 12.dp))
                    Text("Theme Mode", style = MaterialTheme.typography.bodyLarge)
                }
                Row(
                    modifier = Modifier.fillMaxWidth().padding(top = 8.dp),
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                ) {
                    listOf("system" to "System", "light" to "Light", "dark" to "Dark").forEach { (mode, label) ->
                        FilterChip(
                            selected = themeMode == mode,
                            onClick = soundClick { AppSettings.setThemeMode(mode) },
                            label = { Text(label) },
                        )
                    }
                }
            }

            Divider(modifier = Modifier.padding(vertical = 4.dp))

            // --- Section: Security & Privacy ---
            Text("Security & Privacy", style = MaterialTheme.typography.titleSmall, color = MaterialTheme.colorScheme.primary)

            // Biometric Lock
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.weight(1f)) {
                    Icon(Icons.Filled.Fingerprint, contentDescription = null, tint = MaterialTheme.colorScheme.primary, modifier = Modifier.padding(end = 12.dp))
                    Column {
                        Text("App Biometric Lock", style = MaterialTheme.typography.bodyLarge)
                        Text("Require fingerprint, face or PIN to open app", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                }
                Switch(
                    checked = biometricEnabled,
                    onCheckedChange = { checked ->
                        if (checked && !BiometricAuthManager.isBiometricAvailable(context)) {
                            Toast.makeText(context, "No biometrics or screen lock set on device", Toast.LENGTH_LONG).show()
                        } else {
                            AppSettings.setBiometricLockEnabled(checked)
                        }
                    },
                )
            }

            // Privacy Masking
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.weight(1f)) {
                    Icon(Icons.Filled.Visibility, contentDescription = null, tint = MaterialTheme.colorScheme.primary, modifier = Modifier.padding(end = 12.dp))
                    Column {
                        Text("Privacy Mask Mode", style = MaterialTheme.typography.bodyLarge)
                        Text("Mask financial amounts on dashboard", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                }
                Switch(
                    checked = privacyMode,
                    onCheckedChange = { checked -> AppSettings.setPrivacyModeEnabled(checked) },
                )
            }

            Divider(modifier = Modifier.padding(vertical = 4.dp))

            // --- Section: Data Backup & Portability ---
            Text("Data & Backup", style = MaterialTheme.typography.titleSmall, color = MaterialTheme.colorScheme.primary)

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                FilledTonalButton(
                    onClick = soundClick {
                        val fileName = "money_manager_backup_${System.currentTimeMillis()}.json"
                        exportJsonLauncher.launch(fileName)
                    },
                    modifier = Modifier.weight(1f),
                ) {
                    Icon(Icons.Filled.FileDownload, contentDescription = null, modifier = Modifier.padding(end = 6.dp))
                    Text("Backup JSON")
                }

                OutlinedButton(
                    onClick = soundClick {
                        restoreJsonLauncher.launch(arrayOf("application/json", "*/*"))
                    },
                    modifier = Modifier.weight(1f),
                ) {
                    Icon(Icons.Filled.Restore, contentDescription = null, modifier = Modifier.padding(end = 6.dp))
                    Text("Restore JSON")
                }
            }

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                FilledTonalButton(
                    onClick = soundClick {
                        val fileName = "transactions_${System.currentTimeMillis()}.csv"
                        exportCsvLauncher.launch(fileName)
                    },
                    modifier = Modifier.weight(1f),
                ) {
                    Icon(Icons.Filled.TableChart, contentDescription = null, modifier = Modifier.padding(end = 6.dp))
                    Text("Export CSV")
                }

                OutlinedButton(
                    onClick = soundClick {
                        importCsvLauncher.launch(arrayOf("text/csv", "text/comma-separated-values", "*/*"))
                    },
                    modifier = Modifier.weight(1f),
                ) {
                    Icon(Icons.Filled.FileUpload, contentDescription = null, modifier = Modifier.padding(end = 6.dp))
                    Text("Import CSV")
                }
            }

            Spacer(modifier = Modifier.height(16.dp))
        }
    }

    // Confirmation dialog before restoring backup
    pendingRestoreJson?.let { jsonStr ->
        AlertDialog(
            onDismissRequest = { pendingRestoreJson = null },
            title = { Text("Restore Backup?") },
            text = { Text("Restoring a backup will replace your current accounts, categories, and transactions with the backup file data. Do you want to continue?") },
            confirmButton = {
                TextButton(
                    onClick = {
                        val content = pendingRestoreJson
                        pendingRestoreJson = null
                        if (content != null) {
                            scope.launch {
                                try {
                                    repository.restoreBackupJson(content)
                                    Toast.makeText(context, "Database restored successfully!", Toast.LENGTH_SHORT).show()
                                } catch (e: Exception) {
                                    Toast.makeText(context, "Restore failed: ${e.localizedMessage}", Toast.LENGTH_LONG).show()
                                }
                            }
                        }
                    }
                ) {
                    Text("Restore", color = MaterialTheme.colorScheme.error)
                }
            },
            dismissButton = {
                TextButton(onClick = { pendingRestoreJson = null }) {
                    Text("Cancel")
                }
            }
        )
    }
}
