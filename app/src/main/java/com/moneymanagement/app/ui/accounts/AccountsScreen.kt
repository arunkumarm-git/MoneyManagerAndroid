package com.moneymanagement.app.ui.accounts

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
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
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.AccountBalanceWallet
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CenterAlignedTopAppBar
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExtendedFloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.moneymanagement.app.data.Account
import com.moneymanagement.app.data.MoneyRepository
import com.moneymanagement.app.ui.common.AccountTypeAvatar
import com.moneymanagement.app.ui.common.ConfirmDeleteDialog
import com.moneymanagement.app.ui.common.EmptyState
import com.moneymanagement.app.ui.common.GenericViewModelFactory
import com.moneymanagement.app.ui.common.amountColor
import com.moneymanagement.app.ui.common.formatMoney
import com.moneymanagement.app.ui.common.formatMoneyWithSymbol
import com.moneymanagement.app.ui.common.soundClick
import com.moneymanagement.app.ui.theme.AccountBankEnd
import com.moneymanagement.app.ui.theme.AccountBankStart
import com.moneymanagement.app.ui.theme.AccountCashEnd
import com.moneymanagement.app.ui.theme.AccountCashStart
import com.moneymanagement.app.ui.theme.AccountCreditEnd
import com.moneymanagement.app.ui.theme.AccountCreditStart
import com.moneymanagement.app.ui.theme.AccountWalletEnd
import com.moneymanagement.app.ui.theme.AccountWalletStart
import com.moneymanagement.app.ui.theme.ExpenseRed
import com.moneymanagement.app.ui.theme.IncomeGreen
import com.moneymanagement.app.ui.theme.LocalAppColors

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AccountsScreen(repository: MoneyRepository) {
    val viewModel: AccountsViewModel = viewModel(factory = GenericViewModelFactory { AccountsViewModel(repository) })
    val state by viewModel.uiState.collectAsState()

    var showAddDialog by remember { mutableStateOf(false) }
    var editingAccount by remember { mutableStateOf<Account?>(null) }
    var deletingAccount by remember { mutableStateOf<Account?>(null) }

    Scaffold(
        topBar = {
            CenterAlignedTopAppBar(
                title = {
                    Text(
                        "Accounts",
                        style = MaterialTheme.typography.titleLarge,
                        fontWeight = FontWeight.Bold,
                    )
                },
            )
        },
        floatingActionButton = {
            ExtendedFloatingActionButton(
                onClick = soundClick { showAddDialog = true },
                icon = { Icon(Icons.Filled.Add, contentDescription = null) },
                text = { Text("Add Account", fontWeight = FontWeight.SemiBold) },
                containerColor = MaterialTheme.colorScheme.primary,
                contentColor = MaterialTheme.colorScheme.onPrimary,
            )
        },
    ) { innerPadding ->
        if (state.accounts.isEmpty()) {
            Box(modifier = Modifier.fillMaxSize().padding(innerPadding), contentAlignment = Alignment.Center) {
                EmptyState(
                    icon = Icons.Filled.AccountBalanceWallet,
                    title = "No accounts yet",
                    subtitle = "Add a cash, bank, or card account to start tracking balances.",
                    modifier = Modifier.padding(16.dp),
                )
            }
        } else {
            val total = state.balances.values.sum()
            LazyColumn(
                modifier = Modifier.fillMaxSize().padding(innerPadding),
                contentPadding = PaddingValues(horizontal = 16.dp, vertical = 16.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp),
            ) {
                item { TotalBalanceHeroCard(total) }
                items(state.accounts, key = { it.id }) { acc ->
                    val balance = state.balances[acc.id] ?: 0.0
                    AccountRow(
                        account = acc,
                        balance = balance,
                        onEdit = { editingAccount = acc },
                        onDelete = { deletingAccount = acc },
                    )
                }
                item { Spacer(modifier = Modifier.height(72.dp)) }
            }
        }
    }

    if (showAddDialog) {
        AccountDialog(
            account = null,
            onDismiss = { showAddDialog = false },
            onSave = { name, type, balance -> viewModel.addAccount(name, type, balance) },
        )
    }
    editingAccount?.let { acc ->
        AccountDialog(
            account = acc,
            onDismiss = { editingAccount = null },
            onSave = { name, type, balance -> viewModel.updateAccount(acc.id, name, type, balance) },
        )
    }
    deletingAccount?.let { acc ->
        ConfirmDeleteDialog(
            message = "Delete '${acc.name}'? Transactions linked to it will also be removed.",
            onConfirm = { viewModel.deleteAccount(acc.id) },
            onDismiss = { deletingAccount = null },
        )
    }
}

// ──────────────────────────────────────────────────────────────
// Total Balance Hero Card — matches dashboard gradient style
// ──────────────────────────────────────────────────────────────

@Composable
private fun TotalBalanceHeroCard(total: Double) {
    val appColors = LocalAppColors.current

    Box(
        modifier = Modifier
            .fillMaxWidth()
            .clip(MaterialTheme.shapes.extraLarge)
            .shadow(
                elevation = 6.dp,
                shape = MaterialTheme.shapes.extraLarge,
                ambientColor = appColors.gradientEnd.copy(alpha = 0.3f),
                spotColor = appColors.gradientEnd.copy(alpha = 0.3f),
            )
            .background(
                Brush.linearGradient(
                    listOf(appColors.gradientStart, appColors.gradientEnd, appColors.gradientAccent)
                )
            )
            .padding(24.dp),
    ) {
        Column {
            Text(
                "Total Balance",
                style = MaterialTheme.typography.titleSmall,
                color = Color.White.copy(alpha = 0.75f),
            )
            Text(
                formatMoneyWithSymbol(total, 2),
                style = MaterialTheme.typography.headlineLarge,
                fontWeight = FontWeight.ExtraBold,
                color = Color.White,
                modifier = Modifier.padding(top = 4.dp),
            )
        }
    }
}

// ──────────────────────────────────────────────────────────────
// Account Row — account-type-specific gradient accent
// ──────────────────────────────────────────────────────────────

private fun accountTypeGradient(type: String): Pair<Color, Color> = when (type) {
    "bank" -> AccountBankStart to AccountBankEnd
    "cash" -> AccountCashStart to AccountCashEnd
    "wallet" -> AccountWalletStart to AccountWalletEnd
    "card" -> AccountCreditStart to AccountCreditEnd
    else -> AccountBankStart to AccountBankEnd
}

@Composable
private fun AccountRow(account: Account, balance: Double, onEdit: () -> Unit, onDelete: () -> Unit) {
    var menuOpen by remember { mutableStateOf(false) }
    val (gradStart, gradEnd) = accountTypeGradient(account.type)

    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = MaterialTheme.shapes.large,
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainer),
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            // Gradient left accent bar
            Box(
                modifier = Modifier
                    .width(4.dp)
                    .height(72.dp)
                    .background(
                        Brush.verticalGradient(listOf(gradStart, gradEnd)),
                        androidx.compose.foundation.shape.RoundedCornerShape(topStart = 16.dp, bottomStart = 16.dp),
                    ),
            )
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 14.dp, vertical = 12.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                AccountTypeAvatar(account.type)
                Column(modifier = Modifier.padding(start = 12.dp).weight(1f)) {
                    Text(
                        account.name,
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.SemiBold,
                    )
                    Text(
                        account.type.replaceFirstChar { it.uppercase() },
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
                Text(
                    formatMoneyWithSymbol(balance, 2),
                    color = amountColor(if (balance >= 0) "income" else "expense"),
                    fontWeight = FontWeight.Bold,
                    style = MaterialTheme.typography.titleSmall,
                )
                Box {
                    IconButton(onClick = soundClick { menuOpen = true }) {
                        Icon(Icons.Filled.MoreVert, contentDescription = "More options")
                    }
                    DropdownMenu(expanded = menuOpen, onDismissRequest = { menuOpen = false }) {
                        DropdownMenuItem(
                            text = { Text("Edit") },
                            leadingIcon = { Icon(Icons.Filled.Edit, contentDescription = null) },
                            onClick = soundClick { menuOpen = false; onEdit() },
                        )
                        DropdownMenuItem(
                            text = { Text("Delete") },
                            leadingIcon = { Icon(Icons.Filled.Delete, contentDescription = null) },
                            onClick = soundClick { menuOpen = false; onDelete() },
                        )
                    }
                }
            }
        }
    }
}
