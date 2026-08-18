package com.moneymanagement.app.ui.settings

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
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Bolt
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.DarkMode
import androidx.compose.material.icons.filled.Description
import androidx.compose.material.icons.filled.FileDownload
import androidx.compose.material.icons.filled.FileUpload
import androidx.compose.material.icons.filled.Fingerprint
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.Paid
import androidx.compose.material.icons.filled.Restore
import androidx.compose.material.icons.filled.Sms
import androidx.compose.material.icons.filled.TableChart
import androidx.compose.material.icons.filled.Visibility
import androidx.compose.material.icons.filled.VolumeUp
import androidx.compose.material.icons.filled.Warning

import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Divider
import androidx.compose.material3.ElevatedCard
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
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
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.moneymanagement.app.data.MoneyRepository
import com.moneymanagement.app.data.backup.CsvHelper
import com.moneymanagement.app.security.BiometricAuthManager
import com.moneymanagement.app.settings.AppSettings
import com.moneymanagement.app.ui.common.ClickSound
import com.moneymanagement.app.ui.common.EditSheet
import com.moneymanagement.app.ui.common.formatMoney
import com.moneymanagement.app.ui.common.soundClick
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

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
    var showCsvGuideDialog by remember { mutableStateOf(false) }
    var pendingCsvPreview by remember { mutableStateOf<CsvHelper.CsvImportPreview?>(null) }
    var fileSizeError by remember { mutableStateOf<String?>(null) }


    // Export CSV Launcher
    val exportCsvLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.CreateDocument("text/csv")
    ) { uri ->
        if (uri != null) {
            scope.launch {
                try {
                    val transactions = repository.transactions.first()
                    val csv = com.moneymanagement.app.data.backup.CsvHelper.exportToCsv(transactions)
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

    // Download Sample CSV Template Launcher
    val downloadSampleCsvLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.CreateDocument("text/csv")
    ) { uri ->
        if (uri != null) {
            scope.launch {
                try {
                    withContext(Dispatchers.IO) {
                        context.contentResolver.openOutputStream(uri)?.use { os ->
                            os.write(CsvHelper.SAMPLE_CSV_CONTENT.toByteArray(Charsets.UTF_8))
                        }
                    }
                    Toast.makeText(context, "Sample CSV template saved!", Toast.LENGTH_SHORT).show()
                } catch (e: Exception) {
                    Toast.makeText(context, "Failed to save sample: ${e.localizedMessage}", Toast.LENGTH_LONG).show()
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

    // Import CSV Launcher with 5MB Size Guard & Preview
    val importCsvLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.OpenDocument()
    ) { uri ->
        if (uri != null) {
            scope.launch {
                try {
                    val fileSize = context.contentResolver.query(uri, arrayOf(android.provider.OpenableColumns.SIZE), null, null, null)?.use { cursor ->
                        val sizeIdx = cursor.getColumnIndex(android.provider.OpenableColumns.SIZE)
                        if (sizeIdx >= 0 && cursor.moveToFirst()) cursor.getLong(sizeIdx) else null
                    } ?: context.contentResolver.openFileDescriptor(uri, "r")?.use { it.statSize } ?: 0L

                    if (fileSize > CsvHelper.MAX_FILE_SIZE_BYTES) {
                        val sizeMb = "%.2f MB".format(fileSize / (1024.0 * 1024.0))
                        fileSizeError = "Selected file size ($sizeMb) exceeds the maximum allowed limit of ${CsvHelper.MAX_FILE_SIZE_MB_DISPLAY}. Please choose a smaller file."
                        return@launch
                    }

                    val content = withContext(Dispatchers.IO) {
                        context.contentResolver.openInputStream(uri)?.bufferedReader()?.use { it.readText() } ?: ""
                    }

                    val preview = CsvHelper.generateImportPreview(content)
                    if (preview.validRows.isEmpty()) {
                        Toast.makeText(context, "No valid transactions found in CSV. Please ensure headers match the required format.", Toast.LENGTH_LONG).show()
                    } else {
                        pendingCsvPreview = preview
                    }
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

            // --- Section: SMS Auto-Tracking ---
            Text("SMS Auto-Tracking", style = MaterialTheme.typography.titleSmall, color = MaterialTheme.colorScheme.primary)

            val smsTrackingEnabled by AppSettings.smsTrackingEnabled.collectAsState()
            val autoApproveSms by AppSettings.autoApproveSms.collectAsState()

            val smsPermissionLauncher = rememberLauncherForActivityResult(
                ActivityResultContracts.RequestMultiplePermissions()
            ) { permissions ->
                val granted = permissions[android.Manifest.permission.READ_SMS] == true ||
                              permissions[android.Manifest.permission.RECEIVE_SMS] == true
                if (granted) {
                    AppSettings.setSmsTrackingEnabled(true)
                    Toast.makeText(context, "SMS Auto-Tracking enabled!", Toast.LENGTH_SHORT).show()
                } else {
                    AppSettings.setSmsTrackingEnabled(false)
                    Toast.makeText(context, "SMS permission denied", Toast.LENGTH_SHORT).show()
                }
            }

            // SMS Detection Toggle
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.weight(1f)) {
                    Icon(Icons.Filled.Sms, contentDescription = null, tint = MaterialTheme.colorScheme.primary, modifier = Modifier.padding(end = 12.dp))
                    Column {
                        Text("Read Bank SMS", style = MaterialTheme.typography.bodyLarge)
                        Text("Automatically detect debit/credit SMS messages locally", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                }
                Switch(
                    checked = smsTrackingEnabled,
                    onCheckedChange = { checked ->
                        if (checked) {
                            val hasPermission = androidx.core.content.ContextCompat.checkSelfPermission(
                                context, android.Manifest.permission.RECEIVE_SMS
                            ) == android.content.pm.PackageManager.PERMISSION_GRANTED

                            if (hasPermission) {
                                AppSettings.setSmsTrackingEnabled(true)
                            } else {
                                smsPermissionLauncher.launch(
                                    arrayOf(
                                        android.Manifest.permission.RECEIVE_SMS,
                                        android.Manifest.permission.READ_SMS,
                                    )
                                )
                            }
                        } else {
                            AppSettings.setSmsTrackingEnabled(false)
                        }
                    },
                )
            }

            // Instant Auto-Approve Toggle (Only visible if SMS Tracking is ON)
            if (smsTrackingEnabled) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.weight(1f)) {
                        Icon(Icons.Filled.Bolt, contentDescription = null, tint = MaterialTheme.colorScheme.primary, modifier = Modifier.padding(end = 12.dp))
                        Column {
                            Text("Instant Auto-Add", style = MaterialTheme.typography.bodyLarge)
                            Text("Log transactions instantly without waiting for review", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                        }
                    }
                    Switch(
                        checked = autoApproveSms,
                        onCheckedChange = { checked -> AppSettings.setAutoApproveSms(checked) },
                    )
                }
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
                        showCsvGuideDialog = true
                    },
                    modifier = Modifier.weight(1f),
                ) {
                    Icon(Icons.Filled.FileUpload, contentDescription = null, modifier = Modifier.padding(end = 6.dp))
                    Text("Import CSV")
                }
            }

            Spacer(modifier = Modifier.height(16.dp))
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

    // CSV Format & Guidance Dialog
    if (showCsvGuideDialog) {
        AlertDialog(
            onDismissRequest = { showCsvGuideDialog = false },
            icon = { Icon(Icons.Filled.Info, contentDescription = null, tint = MaterialTheme.colorScheme.primary) },
            title = { Text("Import CSV Format & Limits") },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    Surface(
                        shape = RoundedCornerShape(8.dp),
                        color = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.5f),
                        modifier = Modifier.fillMaxWidth(),
                    ) {
                        Row(
                            modifier = Modifier.padding(10.dp),
                            verticalAlignment = Alignment.CenterVertically,
                        ) {
                            Icon(Icons.Filled.Description, contentDescription = null, tint = MaterialTheme.colorScheme.primary, modifier = Modifier.padding(end = 8.dp))
                            Text(
                                text = "Max File Size: ${CsvHelper.MAX_FILE_SIZE_MB_DISPLAY} (~50,000 rows)",
                                style = MaterialTheme.typography.labelLarge,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.onPrimaryContainer,
                            )
                        }
                    }

                    Text(
                        text = "Your CSV file should have the following headers:",
                        style = MaterialTheme.typography.bodyMedium,
                        fontWeight = FontWeight.SemiBold,
                    )

                    Surface(
                        shape = RoundedCornerShape(8.dp),
                        color = MaterialTheme.colorScheme.surfaceVariant,
                        modifier = Modifier.fillMaxWidth(),
                    ) {
                        Text(
                            text = "Date,Type,Amount,Category,Account,ToAccount,Note",
                            style = MaterialTheme.typography.bodySmall,
                            fontFamily = FontFamily.Monospace,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier.padding(8.dp),
                        )
                    }

                    Column(verticalArrangement = Arrangement.spacedBy(3.dp)) {
                        Text("• Date: YYYY-MM-DD (e.g. 2026-08-18)", style = MaterialTheme.typography.bodySmall)
                        Text("• Type: expense, income, or transfer", style = MaterialTheme.typography.bodySmall)
                        Text("• Amount: Numbers only (e.g. 250.00)", style = MaterialTheme.typography.bodySmall)
                        Text("• Encoding: UTF-8 standard CSV", style = MaterialTheme.typography.bodySmall)
                    }

                    OutlinedButton(
                        onClick = soundClick {
                            downloadSampleCsvLauncher.launch("money_manager_sample.csv")
                        },
                        modifier = Modifier.fillMaxWidth().padding(top = 4.dp),
                    ) {
                        Icon(Icons.Filled.FileDownload, contentDescription = null, modifier = Modifier.padding(end = 6.dp))
                        Text("Download Sample CSV Template")
                    }
                }
            },
            confirmButton = {
                Button(
                    onClick = soundClick {
                        showCsvGuideDialog = false
                        importCsvLauncher.launch(arrayOf("text/csv", "text/comma-separated-values", "*/*"))
                    }
                ) {
                    Text("Select CSV File")
                }
            },
            dismissButton = {
                TextButton(onClick = { showCsvGuideDialog = false }) {
                    Text("Cancel")
                }
            }
        )
    }

    // File Size Limit Exceeded Error Dialog
    fileSizeError?.let { errorMsg ->
        AlertDialog(
            onDismissRequest = { fileSizeError = null },
            icon = { Icon(Icons.Filled.Warning, contentDescription = null, tint = MaterialTheme.colorScheme.error) },
            title = { Text("File Size Limit Exceeded") },
            text = { Text(errorMsg) },
            confirmButton = {
                Button(onClick = { fileSizeError = null }) {
                    Text("OK")
                }
            }
        )
    }

    // Pre-Import Summary & Confirmation Dialog
    pendingCsvPreview?.let { preview ->
        AlertDialog(
            onDismissRequest = { pendingCsvPreview = null },
            icon = { Icon(Icons.Filled.CheckCircle, contentDescription = null, tint = MaterialTheme.colorScheme.primary) },
            title = { Text("Confirm CSV Import") },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    Text(
                        text = "Found ${preview.validRows.size} valid transactions to import:",
                        style = MaterialTheme.typography.bodyMedium,
                        fontWeight = FontWeight.SemiBold,
                    )

                    Surface(
                        shape = RoundedCornerShape(8.dp),
                        color = MaterialTheme.colorScheme.surfaceVariant,
                        modifier = Modifier.fillMaxWidth(),
                    ) {
                        Column(modifier = Modifier.padding(10.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                            Text(
                                "• Expense: ${preview.expenseCount} transactions ($currencySymbol ${formatMoney(preview.totalExpenseAmount, 2)})",
                                style = MaterialTheme.typography.bodySmall,
                                fontWeight = FontWeight.Medium,
                            )
                            Text(
                                "• Income: ${preview.incomeCount} transactions ($currencySymbol ${formatMoney(preview.totalIncomeAmount, 2)})",
                                style = MaterialTheme.typography.bodySmall,
                                fontWeight = FontWeight.Medium,
                            )
                            if (preview.transferCount > 0) {
                                Text(
                                    "• Transfers: ${preview.transferCount} transactions",
                                    style = MaterialTheme.typography.bodySmall,
                                    fontWeight = FontWeight.Medium,
                                )
                            }
                        }
                    }

                    if (preview.skippedRowsCount > 0) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier.padding(top = 2.dp),
                        ) {
                            Icon(Icons.Filled.Warning, contentDescription = null, tint = MaterialTheme.colorScheme.error, modifier = Modifier.padding(end = 6.dp))
                            Text(
                                text = "${preview.skippedRowsCount} rows skipped (missing date or amount)",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.error,
                            )
                        }
                    }
                }
            },
            confirmButton = {
                Button(
                    onClick = soundClick {
                        val rowsToImport = preview.validRows
                        pendingCsvPreview = null
                        scope.launch {
                            try {
                                val count = repository.importParsedTransactions(rowsToImport)
                                Toast.makeText(context, "Successfully imported $count transactions!", Toast.LENGTH_SHORT).show()
                            } catch (e: Exception) {
                                Toast.makeText(context, "Import failed: ${e.localizedMessage}", Toast.LENGTH_LONG).show()
                            }
                        }
                    }
                ) {
                    Text("Confirm & Import")
                }
            },
            dismissButton = {
                TextButton(onClick = { pendingCsvPreview = null }) {
                    Text("Cancel")
                }
            }
        )
    }
}

