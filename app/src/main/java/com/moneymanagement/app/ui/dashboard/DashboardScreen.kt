package com.moneymanagement.app.ui.dashboard

import android.Manifest
import android.content.pm.PackageManager
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts

import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ReceiptLong
import androidx.compose.material.icons.filled.ArrowDownward
import androidx.compose.material.icons.filled.ArrowUpward
import androidx.compose.material.icons.filled.CreditCard
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Savings
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.Sms
import androidx.compose.material.icons.filled.SwapHoriz
import androidx.compose.material.icons.filled.Visibility
import androidx.compose.material.icons.filled.VisibilityOff
import androidx.compose.material3.Badge
import androidx.compose.material3.BadgedBox
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.material3.TopAppBarScrollBehavior
import androidx.compose.material3.LargeTopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.content.ContextCompat
import androidx.lifecycle.viewmodel.compose.viewModel
import com.moneymanagement.app.data.BudgetStatusRow
import com.moneymanagement.app.data.CreditCard
import com.moneymanagement.app.data.MoneyRepository
import com.moneymanagement.app.data.TransactionRow
import com.moneymanagement.app.settings.AppSettings
import com.moneymanagement.app.sms.SmsSyncManager
import com.moneymanagement.app.ui.common.CategoryAvatar
import com.moneymanagement.app.ui.common.EmptyState
import com.moneymanagement.app.ui.common.GenericViewModelFactory
import com.moneymanagement.app.ui.common.SectionHeader
import com.moneymanagement.app.ui.common.TransactionTypeAvatar
import com.moneymanagement.app.ui.common.amountColor
import com.moneymanagement.app.ui.common.formatMoney
import com.moneymanagement.app.ui.common.formatMoneyWithSymbol
import com.moneymanagement.app.ui.common.relativeDateLabel
import com.moneymanagement.app.ui.common.signedAmountText
import com.moneymanagement.app.ui.common.soundClick
import com.moneymanagement.app.ui.settings.SettingsSheet
import com.moneymanagement.app.ui.theme.AmberWarning
import com.moneymanagement.app.ui.theme.ExpenseRed
import com.moneymanagement.app.ui.theme.IncomeGreen
import com.moneymanagement.app.ui.theme.LocalAppColors
import com.moneymanagement.app.ui.transactions.TransactionDialog
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DashboardScreen(
    repository: MoneyRepository,
    onSeeAllTransactions: () -> Unit = {},
    onNavigateToSms: () -> Unit = {},
    onNavigateToCards: () -> Unit = {},
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val viewModel: DashboardViewModel = viewModel(factory = GenericViewModelFactory { DashboardViewModel(repository) })
    val state by viewModel.uiState.collectAsState()
    val pendingSmsCount by repository.pendingSmsCount.collectAsState(initial = 0)
    val upcomingBills by repository.upcomingCreditCardBills.collectAsState(initial = emptyList())
    var pendingType by remember { mutableStateOf<String?>(null) }
    var showSettings by remember { mutableStateOf(false) }
    var isQuickScanning by remember { mutableStateOf(false) }

    val scrollBehavior = TopAppBarDefaults.exitUntilCollapsedScrollBehavior()

    val smsPermissionLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestMultiplePermissions()
    ) { permissions ->
        val granted = permissions[Manifest.permission.READ_SMS] == true ||
                      permissions[Manifest.permission.RECEIVE_SMS] == true
        if (granted) {
            AppSettings.setSmsTrackingEnabled(true)
            scope.launch {
                isQuickScanning = true
                try {
                    val stats = SmsSyncManager.scanHistoricalSmsDetailed(context, repository, 30)
                    val total = stats.transactionsFound + stats.billsFound
                    val msg = if (total > 0) "Found $total new items from SMS!" else "No new transactions found in SMS."
                    Toast.makeText(context, msg, Toast.LENGTH_LONG).show()
                } catch (e: Exception) {
                    Toast.makeText(context, "Scan failed: ${e.localizedMessage}", Toast.LENGTH_SHORT).show()
                } finally {
                    isQuickScanning = false
                }
            }
        } else {
            Toast.makeText(context, "SMS permission required to scan bank transactions", Toast.LENGTH_SHORT).show()
        }
    }

    val triggerSmsScan = {
        val hasReadSms = ContextCompat.checkSelfPermission(
            context, Manifest.permission.READ_SMS
        ) == PackageManager.PERMISSION_GRANTED

        if (hasReadSms) {
            scope.launch {
                isQuickScanning = true
                try {
                    val stats = SmsSyncManager.scanHistoricalSmsDetailed(context, repository, 30)
                    val total = stats.transactionsFound + stats.billsFound
                    val msg = if (total > 0) "Found $total new items from SMS!" else "No new transactions found in SMS."
                    Toast.makeText(context, msg, Toast.LENGTH_LONG).show()
                } catch (e: Exception) {
                    Toast.makeText(context, "Scan failed: ${e.localizedMessage}", Toast.LENGTH_SHORT).show()
                } finally {
                    isQuickScanning = false
                }
            }
        } else {
            smsPermissionLauncher.launch(
                arrayOf(
                    Manifest.permission.READ_SMS,
                    Manifest.permission.RECEIVE_SMS,
                )
            )
        }
    }

    Scaffold(
        modifier = Modifier.nestedScroll(scrollBehavior.nestedScrollConnection),
        topBar = {
            LargeTopAppBar(
                title = {
                    Column {
                        Text(
                            "Overview",
                            style = MaterialTheme.typography.headlineSmall,
                            fontWeight = FontWeight.Bold,
                        )
                    }
                },
                actions = {
                    IconButton(onClick = soundClick(onNavigateToSms)) {
                        BadgedBox(
                            badge = {
                                if (pendingSmsCount > 0) {
                                    Badge(
                                        containerColor = MaterialTheme.colorScheme.primary,
                                        contentColor = MaterialTheme.colorScheme.onPrimary,
                                    ) {
                                        Text("$pendingSmsCount")
                                    }
                                }
                            }
                        ) {
                            Icon(Icons.Filled.Sms, contentDescription = "SMS Transactions")
                        }
                    }
                    IconButton(onClick = soundClick(onNavigateToCards)) {
                        Icon(Icons.Filled.CreditCard, contentDescription = "Credit Cards")
                    }
                    IconButton(onClick = soundClick { showSettings = true }) {
                        Icon(Icons.Filled.Settings, contentDescription = "Settings")
                    }
                },
                scrollBehavior = scrollBehavior,
                colors = TopAppBarDefaults.largeTopAppBarColors(
                    containerColor = MaterialTheme.colorScheme.background,
                    scrolledContainerColor = MaterialTheme.colorScheme.surfaceContainer,
                ),
            )
        },
    ) { innerPadding ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding),
            verticalArrangement = Arrangement.spacedBy(16.dp),
            contentPadding = PaddingValues(bottom = 20.dp),
        ) {
            item {
                SummaryCard(
                    totalBalance = state.totalBalance,
                    income = state.monthIncome,
                    expense = state.monthExpense,
                    modifier = Modifier.padding(horizontal = 16.dp),
                )
            }

            if (upcomingBills.isNotEmpty()) {
                item {
                    CreditCardDueBanner(
                        bills = upcomingBills,
                        onClick = soundClick(onNavigateToCards),
                        modifier = Modifier.padding(horizontal = 16.dp),
                    )
                }
            }

            if (pendingSmsCount > 0) {
                item {
                    PendingSmsBanner(
                        count = pendingSmsCount,
                        onClick = soundClick(onNavigateToSms),
                        modifier = Modifier.padding(horizontal = 16.dp),
                    )
                }
            } else {
                item {
                    ScanSmsQuickCard(
                        isScanning = isQuickScanning,
                        onScanClick = soundClick { triggerSmsScan() },
                        onOpenInbox = soundClick(onNavigateToSms),
                        modifier = Modifier.padding(horizontal = 16.dp),
                    )
                }
            }

            item { QuickActionsRow(onSelect = { pendingType = it }, modifier = Modifier.padding(horizontal = 16.dp)) }

            item {
                SectionHeader(
                    "Budget Watch",
                    modifier = Modifier.padding(horizontal = 16.dp),
                )
            }
            if (state.budgetAlerts.isEmpty()) {
                item {
                    EmptyState(
                        icon = Icons.Filled.Savings,
                        title = "All budgets on track",
                        subtitle = "You'll see a heads-up here once a category nears its monthly limit.",
                        modifier = Modifier.padding(horizontal = 16.dp),
                    )
                }
            } else {
                items(state.budgetAlerts, key = { "budget-${it.id}" }) {
                    BudgetAlertRow(it, modifier = Modifier.padding(horizontal = 16.dp))
                }
            }

            item {
                SectionHeader(
                    "Recent Transactions",
                    trailing = { TextButton(onClick = soundClick(onSeeAllTransactions)) { Text("See all") } },
                    modifier = Modifier.padding(horizontal = 16.dp),
                )
            }
            if (state.recentTransactions.isEmpty()) {
                item {
                    EmptyState(
                        icon = Icons.AutoMirrored.Filled.ReceiptLong,
                        title = "No transactions yet",
                        subtitle = "Add your first income, expense, or transfer above to get started.",
                        modifier = Modifier.padding(horizontal = 16.dp),
                    )
                }
            } else {
                items(state.recentTransactions, key = { "txn-${it.txn.id}" }) { row ->
                    RecentTransactionRow(row, modifier = Modifier.padding(horizontal = 16.dp))
                }
            }
        }
    }

    pendingType?.let { presetType ->
        TransactionDialog(
            accounts = state.accounts,
            categories = state.categories,
            initial = null,
            initialType = presetType,
            onDismiss = { pendingType = null },
            onSave = { accountId, type, amount, txnDate, categoryId, toAccountId, note ->
                viewModel.addTransaction(accountId, type, amount, txnDate, categoryId, toAccountId, note)
            },
        )
    }

    if (showSettings) {
        SettingsSheet(
            repository = repository,
            onDismiss = { showSettings = false },
        )
    }
}

// ─────────────────────────────────────────────────────────
// Summary / Hero Card
// ─────────────────────────────────────────────────────────

@Composable
private fun SummaryCard(
    totalBalance: Double,
    income: Double,
    expense: Double,
    modifier: Modifier = Modifier,
) {
    val privacyMode by AppSettings.privacyModeEnabled.collectAsState()
    val currencySymbol by AppSettings.currencySymbol.collectAsState()
    val appColors = LocalAppColors.current

    Box(
        modifier = modifier
            .fillMaxWidth()
            .clip(MaterialTheme.shapes.extraLarge)
            .shadow(
                elevation = 8.dp,
                shape = MaterialTheme.shapes.extraLarge,
                ambientColor = appColors.gradientEnd.copy(alpha = 0.3f),
                spotColor = appColors.gradientEnd.copy(alpha = 0.3f),
            )
            .background(
                Brush.linearGradient(
                    listOf(appColors.gradientStart, appColors.gradientEnd, appColors.gradientAccent),
                ),
            ),
    ) {
        Column(modifier = Modifier.padding(24.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Text(
                    "Total Balance",
                    style = MaterialTheme.typography.titleSmall,
                    color = Color.White.copy(alpha = 0.75f),
                    letterSpacing = 1.sp,
                )
                IconButton(
                    onClick = soundClick {
                        AppSettings.setPrivacyModeEnabled(!privacyMode)
                    },
                    modifier = Modifier.size(28.dp),
                ) {
                    Icon(
                        if (privacyMode) Icons.Filled.VisibilityOff else Icons.Filled.Visibility,
                        contentDescription = "Toggle Privacy",
                        tint = Color.White.copy(alpha = 0.75f),
                        modifier = Modifier.size(20.dp),
                    )
                }
            }
            Text(
                formatMoneyWithSymbol(totalBalance, 2, privacyMode),
                style = MaterialTheme.typography.displaySmall,
                fontWeight = FontWeight.ExtraBold,
                color = Color.White,
                modifier = Modifier.padding(top = 4.dp, bottom = 20.dp),
            )
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(12.dp),
            ) {
                MoneyPill(
                    icon = Icons.Filled.ArrowDownward,
                    label = "Income",
                    amount = income,
                    currency = currencySymbol,
                    masked = privacyMode,
                    modifier = Modifier.weight(1f),
                )
                MoneyPill(
                    icon = Icons.Filled.ArrowUpward,
                    label = "Expense",
                    amount = expense,
                    currency = currencySymbol,
                    masked = privacyMode,
                    modifier = Modifier.weight(1f),
                )
            }
        }
    }
}

@Composable
private fun MoneyPill(
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    label: String,
    amount: Double,
    currency: String,
    masked: Boolean,
    modifier: Modifier = Modifier,
) {
    Row(
        modifier = modifier
            .clip(MaterialTheme.shapes.large)
            .background(Color.White.copy(alpha = 0.15f))
            .padding(horizontal = 14.dp, vertical = 12.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Box(
            modifier = Modifier
                .size(28.dp)
                .background(Color.White.copy(alpha = 0.2f), CircleShape),
            contentAlignment = Alignment.Center,
        ) {
            Icon(icon, contentDescription = null, tint = Color.White, modifier = Modifier.size(16.dp))
        }
        Column(modifier = Modifier.padding(start = 10.dp)) {
            Text(label, style = MaterialTheme.typography.labelSmall, color = Color.White.copy(alpha = 0.75f))
            Text(
                if (masked) "••••" else "$currency ${formatMoney(amount, 0)}",
                style = MaterialTheme.typography.titleSmall,
                fontWeight = FontWeight.Bold,
                color = Color.White,
            )
        }
    }
}

// ─────────────────────────────────────────────────────────
// Quick Actions Row
// ─────────────────────────────────────────────────────────

private data class QuickAction(
    val type: String,
    val label: String,
    val icon: androidx.compose.ui.graphics.vector.ImageVector,
    val gradientStart: Color,
    val gradientEnd: Color,
)

@Composable
private fun QuickActionsRow(onSelect: (String) -> Unit, modifier: Modifier = Modifier) {
    val actions = listOf(
        QuickAction("expense", "Expense", Icons.Filled.ArrowUpward, Color(0xFFFF5252), Color(0xFFFF867C)),
        QuickAction("income", "Income", Icons.Filled.ArrowDownward, Color(0xFF00BFA5), Color(0xFF69F0AE)),
        QuickAction("transfer", "Transfer", Icons.Filled.SwapHoriz, Color(0xFF448AFF), Color(0xFF82B1FF)),
    )
    Row(
        modifier = modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(10.dp),
    ) {
        actions.forEach { action ->
            val interactionSource = remember { MutableInteractionSource() }
            Column(
                modifier = Modifier
                    .weight(1f)
                    .clip(MaterialTheme.shapes.large)
                    .background(
                        Brush.linearGradient(
                            listOf(action.gradientStart.copy(alpha = 0.12f), action.gradientEnd.copy(alpha = 0.2f))
                        )
                    )
                    .clickable(
                        interactionSource = interactionSource,
                        indication = null,
                        onClick = soundClick { onSelect(action.type) },
                    )
                    .padding(vertical = 14.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                Box(
                    modifier = Modifier
                        .size(44.dp)
                        .clip(CircleShape)
                        .background(
                            Brush.linearGradient(listOf(action.gradientStart, action.gradientEnd))
                        ),
                    contentAlignment = Alignment.Center,
                ) {
                    Icon(action.icon, contentDescription = action.label, tint = Color.White, modifier = Modifier.size(20.dp))
                }
                Text(
                    action.label,
                    style = MaterialTheme.typography.labelMedium,
                    fontWeight = FontWeight.SemiBold,
                    color = MaterialTheme.colorScheme.onSurface,
                )
            }
        }
    }
}

// ─────────────────────────────────────────────────────────
// Budget Alert Row — animated gradient progress bar
// ─────────────────────────────────────────────────────────

@Composable
private fun BudgetAlertRow(status: BudgetStatusRow, modifier: Modifier = Modifier) {
    val budget = status.budget ?: return
    val pct = (status.spent / budget).coerceIn(0.0, 1.0)
    val animatedPct by animateFloatAsState(
        targetValue = pct.toFloat(),
        animationSpec = tween(600),
        label = "budget_pct",
    )
    val barColor = when {
        pct >= 1.0 -> ExpenseRed
        pct >= 0.8 -> AmberWarning
        else -> IncomeGreen
    }

    Card(
        modifier = modifier.fillMaxWidth(),
        shape = MaterialTheme.shapes.large,
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainer),
    ) {
        Row(modifier = Modifier.fillMaxWidth().padding(14.dp), verticalAlignment = Alignment.CenterVertically) {
            CategoryAvatar(status.id, status.name)
            Column(modifier = Modifier.padding(start = 12.dp).weight(1f)) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                ) {
                    Text(status.name, style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.SemiBold)
                    Text(
                        "${(pct * 100).toInt()}%",
                        style = MaterialTheme.typography.labelMedium,
                        fontWeight = FontWeight.Bold,
                        color = barColor,
                    )
                }
                Text(
                    "₹${formatMoney(status.spent, 0)} of ₹${formatMoney(budget, 0)}",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
                LinearProgressIndicator(
                    progress = { animatedPct },
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(top = 8.dp)
                        .height(6.dp)
                        .clip(RoundedCornerShape(3.dp)),
                    color = barColor,
                    trackColor = MaterialTheme.colorScheme.surfaceVariant,
                    strokeCap = StrokeCap.Round,
                )
            }
        }
    }
}

// ─────────────────────────────────────────────────────────
// Recent Transaction Row — with accent border
// ─────────────────────────────────────────────────────────

@Composable
private fun RecentTransactionRow(row: TransactionRow, modifier: Modifier = Modifier) {
    val txn = row.txn
    val label = row.categoryName ?: if (txn.type == "transfer") "Transfer" else "Uncategorized"
    val accentColor = when (txn.type) {
        "income" -> IncomeGreen
        "expense" -> ExpenseRed
        else -> Color(0xFF448AFF)
    }

    Card(
        modifier = modifier.fillMaxWidth(),
        shape = MaterialTheme.shapes.large,
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainer),
    ) {
        Row(modifier = Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
            // Left accent border
            Box(
                modifier = Modifier
                    .width(4.dp)
                    .height(64.dp)
                    .background(accentColor, RoundedCornerShape(topStart = 16.dp, bottomStart = 16.dp)),
            )
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 14.dp, vertical = 12.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                TransactionTypeAvatar(txn.type)
                Column(modifier = Modifier.padding(start = 12.dp).weight(1f)) {
                    Text(label, style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.SemiBold)
                    Text(
                        "${relativeDateLabel(txn.txnDate)} · ${row.accountName ?: ""}",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
                Text(
                    signedAmountText(txn.type, txn.amount),
                    color = amountColor(txn.type),
                    fontWeight = FontWeight.Bold,
                    style = MaterialTheme.typography.titleSmall,
                )
            }
        }
    }
}

// ─────────────────────────────────────────────────────────
// Banners
// ─────────────────────────────────────────────────────────

@Composable
private fun PendingSmsBanner(
    count: Int,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Card(
        modifier = modifier
            .fillMaxWidth()
            .clickable(onClick = onClick),
        shape = MaterialTheme.shapes.large,
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.tertiaryContainer,
        ),
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween,
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.weight(1f),
            ) {
                Box(
                    modifier = Modifier
                        .size(40.dp)
                        .background(MaterialTheme.colorScheme.tertiary, CircleShape),
                    contentAlignment = Alignment.Center,
                ) {
                    Text(
                        text = "$count",
                        color = MaterialTheme.colorScheme.onTertiary,
                        fontWeight = FontWeight.Bold,
                        style = MaterialTheme.typography.labelLarge,
                    )
                }
                Column(modifier = Modifier.padding(start = 12.dp)) {
                    Text(
                        text = if (count == 1) "1 SMS transaction detected" else "$count SMS transactions detected",
                        style = MaterialTheme.typography.titleSmall,
                        fontWeight = FontWeight.SemiBold,
                        color = MaterialTheme.colorScheme.onTertiaryContainer,
                    )
                    Text(
                        text = "Tap to review, categorize & log",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onTertiaryContainer.copy(alpha = 0.8f),
                    )
                }
            }
            Text(
                text = "Review →",
                style = MaterialTheme.typography.labelLarge,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.tertiary,
            )
        }
    }
}

@Composable
private fun CreditCardDueBanner(
    bills: List<CreditCard>,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val firstBill = bills.firstOrNull() ?: return
    val currencySymbol by AppSettings.currencySymbol.collectAsState()
    val daysUntil = firstBill.dueDate?.let {
        val target = runCatching { java.time.LocalDate.parse(it) }.getOrNull()
        if (target != null) java.time.temporal.ChronoUnit.DAYS.between(java.time.LocalDate.now(), target) else null
    }

    val isUrgent = daysUntil != null && daysUntil <= 3
    val containerColor = if (isUrgent) MaterialTheme.colorScheme.errorContainer else MaterialTheme.colorScheme.secondaryContainer
    val onContainerColor = if (isUrgent) MaterialTheme.colorScheme.onErrorContainer else MaterialTheme.colorScheme.onSecondaryContainer

    Card(
        modifier = modifier
            .fillMaxWidth()
            .clickable(onClick = onClick),
        shape = MaterialTheme.shapes.large,
        colors = CardDefaults.cardColors(containerColor = containerColor),
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween,
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.weight(1f),
            ) {
                Box(
                    modifier = Modifier
                        .size(40.dp)
                        .background(if (isUrgent) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.secondary, CircleShape),
                    contentAlignment = Alignment.Center,
                ) {
                    Icon(
                        Icons.Filled.CreditCard,
                        contentDescription = null,
                        tint = if (isUrgent) MaterialTheme.colorScheme.onError else MaterialTheme.colorScheme.onSecondary,
                        modifier = Modifier.size(20.dp),
                    )
                }
                Column(modifier = Modifier.padding(start = 12.dp)) {
                    Text(
                        text = "${firstBill.cardName}: $currencySymbol ${formatMoney(firstBill.totalDue, 0)} due",
                        style = MaterialTheme.typography.titleSmall,
                        fontWeight = FontWeight.Bold,
                        color = onContainerColor,
                    )
                    Text(
                        text = when {
                            daysUntil == null -> "Due: ${firstBill.dueDate}"
                            daysUntil < 0 -> "Overdue by ${-daysUntil} days! Pay now"
                            daysUntil == 0L -> "🚨 Payment due today!"
                            daysUntil == 1L -> "⚠️ Payment due tomorrow!"
                            else -> "Due in $daysUntil days (${firstBill.dueDate})"
                        },
                        style = MaterialTheme.typography.bodySmall,
                        fontWeight = if (isUrgent) FontWeight.SemiBold else FontWeight.Normal,
                        color = onContainerColor.copy(alpha = 0.85f),
                    )
                }
            }
            Text(
                text = "Pay →",
                style = MaterialTheme.typography.labelLarge,
                fontWeight = FontWeight.Bold,
                color = onContainerColor,
            )
        }
    }
}

@Composable
private fun ScanSmsQuickCard(
    isScanning: Boolean,
    onScanClick: () -> Unit,
    onOpenInbox: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Card(
        modifier = modifier.fillMaxWidth(),
        shape = MaterialTheme.shapes.large,
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.35f),
        ),
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(14.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween,
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.weight(1f).clickable(onClick = onOpenInbox),
            ) {
                Box(
                    modifier = Modifier
                        .size(40.dp)
                        .background(MaterialTheme.colorScheme.primary.copy(alpha = 0.15f), CircleShape),
                    contentAlignment = Alignment.Center,
                ) {
                    Icon(
                        Icons.Filled.Sms,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.size(20.dp),
                    )
                }
                Column(modifier = Modifier.padding(start = 12.dp, end = 8.dp)) {
                    Text(
                        text = "Bank SMS Sync",
                        style = MaterialTheme.typography.titleSmall,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onSurface,
                    )
                    Text(
                        text = "Scan last 30 days bank SMS",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
            }

            FilledTonalButton(
                onClick = onScanClick,
                enabled = !isScanning,
                modifier = Modifier.height(36.dp),
                colors = ButtonDefaults.filledTonalButtonColors(
                    containerColor = MaterialTheme.colorScheme.primary,
                    contentColor = MaterialTheme.colorScheme.onPrimary,
                ),
            ) {
                if (isScanning) {
                    CircularProgressIndicator(
                        modifier = Modifier.size(16.dp),
                        strokeWidth = 2.dp,
                        color = MaterialTheme.colorScheme.onPrimary,
                    )
                } else {
                    Icon(Icons.Filled.Refresh, contentDescription = null, modifier = Modifier.size(15.dp))
                    Spacer(modifier = Modifier.width(4.dp))
                    Text("Scan 30d", style = MaterialTheme.typography.labelMedium, fontWeight = FontWeight.Bold)
                }
            }
        }
    }
}
