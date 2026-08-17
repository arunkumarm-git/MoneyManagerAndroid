package com.ravi.moneymanagement.ui.dashboard

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowDownward
import androidx.compose.material.icons.filled.ArrowUpward
import androidx.compose.material.icons.filled.ReceiptLong
import androidx.compose.material.icons.filled.Savings
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.SwapHoriz
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CenterAlignedTopAppBar
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.ravi.moneymanagement.data.BudgetStatusRow
import com.ravi.moneymanagement.data.MoneyRepository
import com.ravi.moneymanagement.data.TransactionRow
import com.ravi.moneymanagement.ui.common.CategoryAvatar
import com.ravi.moneymanagement.ui.common.EmptyState
import com.ravi.moneymanagement.ui.common.GenericViewModelFactory
import com.ravi.moneymanagement.ui.common.SectionHeader
import com.ravi.moneymanagement.ui.common.TransactionTypeAvatar
import com.ravi.moneymanagement.ui.common.soundClick
import com.ravi.moneymanagement.ui.common.amountColor
import com.ravi.moneymanagement.ui.common.formatMoney
import com.ravi.moneymanagement.ui.common.relativeDateLabel
import com.ravi.moneymanagement.ui.common.signedAmountText
import com.ravi.moneymanagement.ui.settings.SettingsSheet
import com.ravi.moneymanagement.ui.theme.ExpenseRed
import com.ravi.moneymanagement.ui.theme.IncomeGreen
import androidx.compose.material.icons.filled.Visibility
import androidx.compose.material.icons.filled.VisibilityOff
import com.ravi.moneymanagement.settings.AppSettings
import com.ravi.moneymanagement.ui.common.formatMoneyWithSymbol
import com.ravi.moneymanagement.ui.transactions.TransactionDialog

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DashboardScreen(repository: MoneyRepository, onSeeAllTransactions: () -> Unit = {}) {
    val viewModel: DashboardViewModel = viewModel(factory = GenericViewModelFactory { DashboardViewModel(repository) })
    val state by viewModel.uiState.collectAsState()
    var pendingType by remember { mutableStateOf<String?>(null) }
    var showSettings by remember { mutableStateOf(false) }

    Scaffold(
        topBar = {
            CenterAlignedTopAppBar(
                title = { Text("Overview") },
                actions = {
                    IconButton(onClick = soundClick { showSettings = true }) {
                        Icon(Icons.Filled.Settings, contentDescription = "Settings")
                    }
                },
            )
        },
    ) { innerPadding ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .padding(horizontal = 16.dp),
            verticalArrangement = Arrangement.spacedBy(20.dp),
            contentPadding = androidx.compose.foundation.layout.PaddingValues(vertical = 12.dp),
        ) {
            item { SummaryCard(state.totalBalance, state.monthIncome, state.monthExpense) }
            item { QuickActionsRow(onSelect = { pendingType = it }) }

            item { SectionHeader("Budget Watch") }
            if (state.budgetAlerts.isEmpty()) {
                item {
                    EmptyState(
                        icon = Icons.Filled.Savings,
                        title = "All budgets on track",
                        subtitle = "You'll see a heads-up here once a category nears its monthly limit.",
                    )
                }
            } else {
                items(state.budgetAlerts, key = { "budget-${it.id}" }) { BudgetAlertRow(it) }
            }

            item {
                SectionHeader(
                    "Recent Transactions",
                    trailing = { TextButton(onClick = soundClick(onSeeAllTransactions)) { Text("See all") } },
                )
            }
            if (state.recentTransactions.isEmpty()) {
                item {
                    EmptyState(
                        icon = Icons.Filled.ReceiptLong,
                        title = "No transactions yet",
                        subtitle = "Add your first income, expense, or transfer above to get started.",
                    )
                }
            } else {
                items(state.recentTransactions, key = { "txn-${it.txn.id}" }) { RecentTransactionRow(it) }
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

@Composable
private fun SummaryCard(totalBalance: Double, income: Double, expense: Double) {
    val privacyMode by AppSettings.privacyModeEnabled.collectAsState()
    val currencySymbol by AppSettings.currencySymbol.collectAsState()

    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = MaterialTheme.shapes.large,
        colors = CardDefaults.cardColors(containerColor = Color.Transparent),
    ) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .background(
                    Brush.linearGradient(
                        listOf(MaterialTheme.colorScheme.primary, MaterialTheme.colorScheme.primaryContainer),
                    ),
                ),
        ) {
            Column(modifier = Modifier.padding(20.dp)) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Text(
                        "Total Balance",
                        style = MaterialTheme.typography.titleSmall,
                        color = MaterialTheme.colorScheme.onPrimary.copy(alpha = 0.85f),
                    )
                    IconButton(
                        onClick = soundClick {
                            AppSettings.setPrivacyModeEnabled(!privacyMode)
                        },
                        modifier = Modifier.size(24.dp),
                    ) {
                        Icon(
                            if (privacyMode) Icons.Filled.VisibilityOff else Icons.Filled.Visibility,
                            contentDescription = "Toggle Privacy",
                            tint = MaterialTheme.colorScheme.onPrimary.copy(alpha = 0.85f),
                            modifier = Modifier.size(18.dp),
                        )
                    }
                }
                Text(
                    formatMoneyWithSymbol(totalBalance, 2, privacyMode),
                    style = MaterialTheme.typography.headlineLarge,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onPrimary,
                    modifier = Modifier.padding(top = 4.dp),
                )
                Row(
                    modifier = Modifier.fillMaxWidth().padding(top = 16.dp),
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
            .background(Color.White.copy(alpha = 0.16f), MaterialTheme.shapes.medium)
            .padding(horizontal = 12.dp, vertical = 10.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Icon(icon, contentDescription = null, tint = Color.White, modifier = Modifier.size(16.dp))
        Column(modifier = Modifier.padding(start = 8.dp)) {
            Text(label, style = MaterialTheme.typography.labelSmall, color = Color.White.copy(alpha = 0.85f))
            Text(
                if (masked) "••••" else "$currency ${formatMoney(amount, 0)}",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.SemiBold,
                color = Color.White,
            )
        }
    }
}

private data class QuickAction(val type: String, val label: String, val icon: androidx.compose.ui.graphics.vector.ImageVector, val color: Color)

@Composable
private fun QuickActionsRow(onSelect: (String) -> Unit) {
    val actions = listOf(
        QuickAction("expense", "Expense", Icons.Filled.ArrowUpward, ExpenseRed),
        QuickAction("income", "Income", Icons.Filled.ArrowDownward, IncomeGreen),
        QuickAction("transfer", "Transfer", Icons.Filled.SwapHoriz, MaterialTheme.colorScheme.tertiary),
    )
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        actions.forEach { action ->
            Column(
                modifier = Modifier
                    .weight(1f)
                    .clickable(onClick = soundClick { onSelect(action.type) }),
                horizontalAlignment = Alignment.CenterHorizontally,
            ) {
                Box(
                    modifier = Modifier
                        .size(48.dp)
                        .background(action.color.copy(alpha = 0.14f), CircleShape),
                    contentAlignment = Alignment.Center,
                ) {
                    Icon(action.icon, contentDescription = action.label, tint = action.color)
                }
                Text(
                    action.label,
                    style = MaterialTheme.typography.labelMedium,
                    modifier = Modifier.padding(top = 6.dp),
                )
            }
        }
    }
}

@Composable
private fun BudgetAlertRow(status: BudgetStatusRow) {
    val budget = status.budget ?: return
    val pct = (status.spent / budget).coerceIn(0.0, 1.0)
    Card(modifier = Modifier.fillMaxWidth()) {
        Row(modifier = Modifier.fillMaxWidth().padding(12.dp), verticalAlignment = Alignment.CenterVertically) {
            CategoryAvatar(status.id, status.name)
            Column(modifier = Modifier.padding(start = 12.dp).weight(1f)) {
                Text(status.name, style = MaterialTheme.typography.titleSmall)
                Text(
                    "₹${formatMoney(status.spent, 0)} of ₹${formatMoney(budget, 0)}",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
                LinearProgressIndicator(
                    progress = { pct.toFloat() },
                    modifier = Modifier.fillMaxWidth().padding(top = 6.dp),
                    color = if (pct >= 1.0) ExpenseRed else MaterialTheme.colorScheme.primary,
                )
            }
        }
    }
}

@Composable
private fun RecentTransactionRow(row: TransactionRow) {
    val txn = row.txn
    val label = row.categoryName ?: if (txn.type == "transfer") "Transfer" else "Uncategorized"
    Card(modifier = Modifier.fillMaxWidth()) {
        Row(modifier = Modifier.fillMaxWidth().padding(12.dp), verticalAlignment = Alignment.CenterVertically) {
            TransactionTypeAvatar(txn.type)
            Column(modifier = Modifier.padding(start = 12.dp).weight(1f)) {
                Text(label, style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.Medium)
                Text(
                    relativeDateLabel(txn.txnDate),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
            Text(
                signedAmountText(txn.type, txn.amount),
                color = amountColor(txn.type),
                fontWeight = FontWeight.SemiBold,
            )
        }
    }
}
