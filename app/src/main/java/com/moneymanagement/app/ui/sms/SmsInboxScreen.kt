package com.moneymanagement.app.ui.sms

import android.Manifest
import android.content.pm.PackageManager
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AccountBalance
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.Category
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.DoneAll
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.ExpandLess
import androidx.compose.material.icons.filled.ExpandMore
import androidx.compose.material.icons.filled.Message
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Sms
import androidx.compose.material.icons.filled.Sync
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SuggestionChip
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.core.content.ContextCompat
import androidx.lifecycle.viewmodel.compose.viewModel
import com.moneymanagement.app.data.Account
import com.moneymanagement.app.data.Category
import com.moneymanagement.app.data.MoneyRepository
import com.moneymanagement.app.data.SmsInbox
import com.moneymanagement.app.settings.AppSettings
import com.moneymanagement.app.ui.common.EmptyState
import com.moneymanagement.app.ui.common.GenericViewModelFactory
import com.moneymanagement.app.ui.common.amountColor
import com.moneymanagement.app.ui.common.formatMoney
import com.moneymanagement.app.ui.common.relativeDateLabel
import com.moneymanagement.app.ui.common.signedAmountText
import com.moneymanagement.app.ui.common.soundClick
import com.moneymanagement.app.ui.theme.ExpenseRed
import com.moneymanagement.app.ui.theme.IncomeGreen
import com.moneymanagement.app.ui.transactions.TransactionDialog

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SmsInboxScreen(
    repository: MoneyRepository,
    onBack: () -> Unit,
) {
    val context = LocalContext.current
    val viewModel: SmsInboxViewModel = viewModel(
        factory = GenericViewModelFactory { SmsInboxViewModel(repository) }
    )

    val pendingList by viewModel.pendingSms.collectAsState()
    val accounts by viewModel.accounts.collectAsState()
    val categories by viewModel.categories.collectAsState()
    val isScanning by viewModel.isScanning.collectAsState()
    val scanMessage by viewModel.scanMessage.collectAsState()

    var editingSms by remember { mutableStateOf<SmsInbox?>(null) }

    val smsPermissionLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestMultiplePermissions()
    ) { permissions ->
        val granted = permissions[Manifest.permission.READ_SMS] == true ||
                      permissions[Manifest.permission.RECEIVE_SMS] == true
        if (granted) {
            AppSettings.setSmsTrackingEnabled(true)
            viewModel.scanPastSms(context, days = 30)
        } else {
            Toast.makeText(context, "SMS permission is required to read bank SMS", Toast.LENGTH_SHORT).show()
        }
    }

    LaunchedEffect(scanMessage) {
        scanMessage?.let { msg ->
            Toast.makeText(context, msg, Toast.LENGTH_LONG).show()
            viewModel.clearScanMessage()
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text("SMS Transactions")
                        if (pendingList.isNotEmpty()) {
                            Surface(
                                shape = CircleShape,
                                color = MaterialTheme.colorScheme.primaryContainer,
                                modifier = Modifier.padding(start = 8.dp),
                            ) {
                                Text(
                                    text = pendingList.size.toString(),
                                    style = MaterialTheme.typography.labelMedium,
                                    color = MaterialTheme.colorScheme.onPrimaryContainer,
                                    fontWeight = FontWeight.Bold,
                                    modifier = Modifier.padding(horizontal = 8.dp, vertical = 2.dp),
                                )
                            }
                        }
                    }
                },
                navigationIcon = {
                    IconButton(onClick = soundClick(onBack)) {
                        Icon(Icons.Filled.ArrowBack, contentDescription = "Back")
                    }
                },
                actions = {
                    if (isScanning) {
                        CircularProgressIndicator(
                            modifier = Modifier.size(24.dp).padding(end = 16.dp),
                            strokeWidth = 2.5.dp,
                        )
                    } else {
                        IconButton(
                            onClick = soundClick {
                                val hasReadSms = ContextCompat.checkSelfPermission(
                                    context, Manifest.permission.READ_SMS
                                ) == PackageManager.PERMISSION_GRANTED

                                if (hasReadSms) {
                                    viewModel.scanPastSms(context, days = 30)
                                } else {
                                    smsPermissionLauncher.launch(
                                        arrayOf(
                                            Manifest.permission.READ_SMS,
                                            Manifest.permission.RECEIVE_SMS,
                                        )
                                    )
                                }
                            }
                        ) {
                            Icon(Icons.Filled.Sync, contentDescription = "Scan SMS")
                        }
                    }

                    if (pendingList.size > 1 && accounts.isNotEmpty()) {
                        IconButton(
                            onClick = soundClick {
                                viewModel.approveAll(accounts.first().id)
                                Toast.makeText(context, "Approved all pending transactions!", Toast.LENGTH_SHORT).show()
                            }
                        ) {
                            Icon(Icons.Filled.DoneAll, contentDescription = "Approve All")
                        }
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.surface,
                ),
            )
        },
    ) { innerPadding ->
        if (pendingList.isEmpty()) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(innerPadding)
                    .padding(24.dp),
                contentAlignment = Alignment.Center,
            ) {
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(16.dp),
                ) {
                    Box(
                        modifier = Modifier
                            .size(72.dp)
                            .clip(CircleShape)
                            .background(MaterialTheme.colorScheme.primaryContainer),
                        contentAlignment = Alignment.Center,
                    ) {
                        Icon(
                            Icons.Filled.Sms,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.size(36.dp),
                        )
                    }
                    Text(
                        "No Pending SMS Transactions",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                    )
                    Text(
                        "Incoming debit and credit SMS messages from your bank will be detected and listed here for quick review.",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        textAlign = androidx.compose.ui.text.style.TextAlign.Center,
                    )

                    Button(
                        onClick = soundClick {
                            val hasReadSms = ContextCompat.checkSelfPermission(
                                context, Manifest.permission.READ_SMS
                            ) == PackageManager.PERMISSION_GRANTED

                            if (hasReadSms) {
                                viewModel.scanPastSms(context, days = 30)
                            } else {
                                smsPermissionLauncher.launch(
                                    arrayOf(
                                        Manifest.permission.READ_SMS,
                                        Manifest.permission.RECEIVE_SMS,
                                    )
                                )
                            }
                        },
                        enabled = !isScanning,
                        modifier = Modifier.padding(top = 8.dp),
                    ) {
                        Icon(Icons.Filled.Refresh, contentDescription = null, modifier = Modifier.size(18.dp))
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(if (isScanning) "Scanning SMS Inbox..." else "Scan Past 30 Days")
                    }
                }
            }
        } else {
            LazyColumn(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(innerPadding)
                    .padding(horizontal = 16.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp),
                contentPadding = PaddingValues(vertical = 12.dp),
            ) {
                items(pendingList, key = { it.id }) { sms ->
                    SmsDraftCard(
                        sms = sms,
                        accounts = accounts,
                        categories = categories,
                        onApprove = { accId, catId, note ->
                            viewModel.approveSms(sms, accId, catId, null, note)
                            Toast.makeText(context, "Transaction logged!", Toast.LENGTH_SHORT).show()
                        },
                        onDismiss = {
                            viewModel.dismissSms(sms.id)
                        },
                        onEdit = {
                            editingSms = sms
                        },
                    )
                }
            }
        }
    }

    editingSms?.let { sms ->
        val matchedAccount = accounts.find { it.id == sms.suggestedAccountId } ?: accounts.firstOrNull()
        val matchedCategory = categories.find {
            it.name.equals(sms.suggestedCategory, ignoreCase = true) &&
            it.type.equals(sms.type, ignoreCase = true)
        }

        val initialRow = com.moneymanagement.app.data.TransactionRow(
            txn = com.moneymanagement.app.data.Transaction(
                id = 0,
                accountId = matchedAccount?.id ?: accounts.firstOrNull()?.id ?: 1L,
                toAccountId = null,
                categoryId = matchedCategory?.id,
                type = sms.type,
                amount = sms.amount,
                note = sms.merchantOrNote,
                txnDate = if (sms.txnDate.length >= 10) sms.txnDate.substring(0, 10) else java.time.LocalDate.now().toString(),
                createdAt = com.moneymanagement.app.data.nowIso(),
            ),
            categoryName = matchedCategory?.name,
            accountName = matchedAccount?.name,
            toAccountName = null,
        )

        TransactionDialog(
            accounts = accounts,
            categories = categories,
            initial = initialRow,
            initialType = sms.type,
            onDismiss = { editingSms = null },
            onSave = { accountId, type, amount, txnDate, categoryId, toAccountId, note ->
                viewModel.approveSms(
                    sms = sms,
                    accountId = accountId,
                    categoryId = categoryId,
                    toAccountId = toAccountId,
                    note = note,
                )
                editingSms = null
                Toast.makeText(context, "Transaction logged!", Toast.LENGTH_SHORT).show()
            },
        )
    }
}


@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun SmsDraftCard(
    sms: SmsInbox,
    accounts: List<Account>,
    categories: List<Category>,
    onApprove: (accountId: Long, categoryId: Long?, note: String?) -> Unit,
    onDismiss: () -> Unit,
    onEdit: () -> Unit,
) {
    val currencySymbol by AppSettings.currencySymbol.collectAsState()

    var selectedAccountId by remember(sms.id, accounts) {
        val matched = accounts.find { acc ->
            sms.accountReference != null && acc.name.contains(sms.accountReference.replace("XX", ""), ignoreCase = true)
        } ?: accounts.firstOrNull()
        mutableStateOf(matched?.id)
    }

    var selectedCategoryId by remember(sms.id, categories) {
        val matched = categories.find { cat ->
            cat.name.equals(sms.suggestedCategory, ignoreCase = true) &&
            cat.type.equals(sms.type, ignoreCase = true)
        }
        mutableStateOf(matched?.id)
    }

    var expandedSms by remember { mutableStateOf(false) }

    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.6f)),
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            // Header Row: Title/Merchant, Amount & Type Avatar
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.Top,
            ) {
                Column(modifier = Modifier.weight(1f).padding(end = 8.dp)) {
                    Text(
                        text = sms.merchantOrNote ?: "SMS Transaction",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                    )
                    Text(
                        text = buildString {
                            append(relativeDateLabel(sms.txnDate))
                            if (!sms.accountReference.isNullOrBlank()) {
                                append(" • ")
                                append(sms.accountReference)
                            }
                            append(" (${sms.sender})")
                        },
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }

                Text(
                    text = "${if (sms.type == "expense") "-" else "+"}$currencySymbol ${formatMoney(sms.amount, 2)}",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    color = if (sms.type == "expense") ExpenseRed else IncomeGreen,
                )
            }

            // Raw SMS expandable preview
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = 8.dp)
                    .clickable(onClick = soundClick { expandedSms = !expandedSms }),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Text(
                    text = if (expandedSms) sms.rawBody else "View original SMS",
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.8f),
                    modifier = Modifier.weight(1f),
                )
                Icon(
                    if (expandedSms) Icons.Filled.ExpandLess else Icons.Filled.ExpandMore,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.size(16.dp),
                )
            }

            Spacer(modifier = Modifier.height(12.dp))

            // Category Picker Chips
            val filteredCategories = categories.filter { it.type == sms.type }
            if (filteredCategories.isNotEmpty()) {
                Text("Category:", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                FlowRow(
                    modifier = Modifier.fillMaxWidth().padding(top = 4.dp),
                    horizontalArrangement = Arrangement.spacedBy(6.dp),
                    verticalArrangement = Arrangement.spacedBy(4.dp),
                ) {
                    filteredCategories.forEach { cat ->
                        FilterChip(
                            selected = selectedCategoryId == cat.id,
                            onClick = soundClick { selectedCategoryId = cat.id },
                            label = { Text(cat.name, style = MaterialTheme.typography.labelSmall) },
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(8.dp))

            // Account Picker Chips
            if (accounts.isNotEmpty()) {
                Text("Account:", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                FlowRow(
                    modifier = Modifier.fillMaxWidth().padding(top = 4.dp),
                    horizontalArrangement = Arrangement.spacedBy(6.dp),
                    verticalArrangement = Arrangement.spacedBy(4.dp),
                ) {
                    accounts.forEach { acc ->
                        FilterChip(
                            selected = selectedAccountId == acc.id,
                            onClick = soundClick { selectedAccountId = acc.id },
                            label = { Text(acc.name, style = MaterialTheme.typography.labelSmall) },
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(14.dp))

            // Action Buttons: Approve, Edit, Dismiss
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                OutlinedButton(
                    onClick = soundClick(onDismiss),
                    modifier = Modifier.weight(1f),
                    colors = ButtonDefaults.outlinedButtonColors(
                        contentColor = MaterialTheme.colorScheme.error,
                    ),
                ) {
                    Icon(Icons.Filled.Close, contentDescription = null, modifier = Modifier.size(16.dp))
                    Spacer(modifier = Modifier.width(4.dp))
                    Text("Dismiss")
                }

                IconButton(
                    onClick = soundClick(onEdit),
                    modifier = Modifier.size(40.dp),
                ) {
                    Icon(Icons.Filled.Edit, contentDescription = "Edit", tint = MaterialTheme.colorScheme.primary)
                }

                Button(
                    onClick = soundClick {
                        val accId = selectedAccountId ?: accounts.firstOrNull()?.id ?: 1L
                        onApprove(accId, selectedCategoryId, sms.merchantOrNote)
                    },
                    modifier = Modifier.weight(1.3f),
                ) {
                    Icon(Icons.Filled.Check, contentDescription = null, modifier = Modifier.size(16.dp))
                    Spacer(modifier = Modifier.width(4.dp))
                    Text("Confirm")
                }
            }
        }
    }
}
