package com.moneymanagement.app.ui.accounts

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.AccountBalanceWallet
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material3.Card
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
import androidx.compose.ui.Modifier
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
import com.moneymanagement.app.ui.common.soundClick
import com.moneymanagement.app.ui.theme.ExpenseRed
import com.moneymanagement.app.ui.theme.IncomeGreen

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AccountsScreen(repository: MoneyRepository) {
    val viewModel: AccountsViewModel = viewModel(factory = GenericViewModelFactory { AccountsViewModel(repository) })
    val state by viewModel.uiState.collectAsState()

    var showAddDialog by remember { mutableStateOf(false) }
    var editingAccount by remember { mutableStateOf<Account?>(null) }
    var deletingAccount by remember { mutableStateOf<Account?>(null) }

    Scaffold(
        topBar = { CenterAlignedTopAppBar(title = { Text("Accounts") }) },
        floatingActionButton = {
            ExtendedFloatingActionButton(
                onClick = soundClick { showAddDialog = true },
                icon = { Icon(Icons.Filled.Add, contentDescription = null) },
                text = { Text("Add account") },
            )
        },
    ) { innerPadding ->
        if (state.accounts.isEmpty()) {
            Column(modifier = Modifier.fillMaxSize().padding(innerPadding).padding(16.dp)) {
                EmptyState(
                    icon = Icons.Filled.AccountBalanceWallet,
                    title = "No accounts yet",
                    subtitle = "Add a cash, bank, or card account to start tracking balances.",
                )
            }
        } else {
            val total = state.balances.values.sum()
            LazyColumn(
                modifier = Modifier.fillMaxSize().padding(innerPadding),
                contentPadding = PaddingValues(16.dp),
                verticalArrangement = Arrangement.spacedBy(10.dp),
            ) {
                item { TotalBalanceCard(total) }
                items(state.accounts, key = { it.id }) { acc ->
                    val balance = state.balances[acc.id] ?: 0.0
                    AccountRow(
                        account = acc,
                        balance = balance,
                        onEdit = { editingAccount = acc },
                        onDelete = { deletingAccount = acc },
                    )
                }
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

@Composable
private fun TotalBalanceCard(total: Double) {
    Card(modifier = Modifier.fillMaxWidth()) {
        Column(modifier = Modifier.padding(16.dp)) {
            Text("Total across accounts", style = MaterialTheme.typography.labelLarge, color = MaterialTheme.colorScheme.onSurfaceVariant)
            Text(
                "₹ ${formatMoney(total)}",
                style = MaterialTheme.typography.headlineSmall,
                fontWeight = FontWeight.Bold,
                color = if (total >= 0) IncomeGreen else ExpenseRed,
                modifier = Modifier.padding(top = 2.dp),
            )
        }
    }
}

@Composable
private fun AccountRow(account: Account, balance: Double, onEdit: () -> Unit, onDelete: () -> Unit) {
    var menuOpen by remember { mutableStateOf(false) }
    Card(modifier = Modifier.fillMaxWidth()) {
        Row(
            modifier = Modifier.fillMaxWidth().padding(12.dp),
            verticalAlignment = androidx.compose.ui.Alignment.CenterVertically,
        ) {
            AccountTypeAvatar(account.type)
            Column(modifier = Modifier.padding(start = 12.dp).weight(1f)) {
                Text(account.name, style = MaterialTheme.typography.titleMedium)
                Text(
                    account.type.replaceFirstChar { it.uppercase() },
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
            Text(
                "₹ ${formatMoney(balance)}",
                color = amountColor(if (balance >= 0) "income" else "expense"),
                fontWeight = FontWeight.SemiBold,
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
