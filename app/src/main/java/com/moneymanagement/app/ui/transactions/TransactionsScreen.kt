package com.moneymanagement.app.ui.transactions

import androidx.compose.animation.animateColorAsState
import androidx.compose.foundation.background
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material.icons.filled.ReceiptLong
import androidx.compose.material3.Card
import androidx.compose.material3.CenterAlignedTopAppBar
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarDuration
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.SnackbarResult
import androidx.compose.material3.SwipeToDismissBox
import androidx.compose.material3.SwipeToDismissBoxValue
import androidx.compose.material3.Text
import androidx.compose.material3.rememberSwipeToDismissBoxState
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
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.moneymanagement.app.data.MoneyRepository
import com.moneymanagement.app.data.TransactionRow
import com.moneymanagement.app.ui.common.ConfirmDeleteDialog
import com.moneymanagement.app.ui.common.EmptyState
import com.moneymanagement.app.ui.common.GenericViewModelFactory
import com.moneymanagement.app.ui.common.TransactionTypeAvatar
import com.moneymanagement.app.ui.common.amountColor
import com.moneymanagement.app.ui.common.relativeDateLabel
import com.moneymanagement.app.ui.common.signedAmountText
import com.moneymanagement.app.ui.common.soundClick
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TransactionsScreen(repository: MoneyRepository) {
    val viewModel: TransactionsViewModel = viewModel(factory = GenericViewModelFactory { TransactionsViewModel(repository) })
    val state by viewModel.uiState.collectAsState()
    val scope = rememberCoroutineScope()
    val snackbarHostState = remember { SnackbarHostState() }

    var showAddDialog by remember { mutableStateOf(false) }
    var editingTxn by remember { mutableStateOf<TransactionRow?>(null) }
    var deletingTxn by remember { mutableStateOf<TransactionRow?>(null) }

    Scaffold(
        topBar = { CenterAlignedTopAppBar(title = { Text("Transactions") }) },
        snackbarHost = { SnackbarHost(snackbarHostState) },
        floatingActionButton = {
            FloatingActionButton(onClick = soundClick { showAddDialog = true }) {
                Icon(Icons.Filled.Add, contentDescription = "Add transaction")
            }
        },
    ) { innerPadding ->
        Column(modifier = Modifier.fillMaxSize().padding(innerPadding)) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .horizontalScroll(rememberScrollState())
                    .padding(horizontal = 16.dp, vertical = 8.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                TRANSACTION_FILTERS.forEach { name ->
                    FilterChip(
                        selected = state.filter == name,
                        onClick = soundClick { viewModel.setFilter(name) },
                        label = { Text(name) },
                    )
                }
            }
            if (state.transactions.isEmpty()) {
                EmptyState(
                    icon = Icons.Filled.ReceiptLong,
                    title = "No transactions yet",
                    subtitle = "Tap the + button to record your first one.",
                    modifier = Modifier.padding(16.dp),
                )
            } else {
                val grouped = state.transactions.groupBy { relativeDateLabel(it.txn.txnDate) }
                LazyColumn(
                    modifier = Modifier.fillMaxSize(),
                    contentPadding = PaddingValues(horizontal = 16.dp, vertical = 8.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp),
                ) {
                    grouped.forEach { (label, rows) ->
                        item(key = "header-$label") {
                            Text(
                                label,
                                style = MaterialTheme.typography.titleSmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                modifier = Modifier.padding(top = 4.dp, bottom = 2.dp),
                            )
                        }
                        items(rows, key = { "txn-${it.txn.id}" }) { row ->
                            val dismissState = rememberSwipeToDismissBoxState(
                                confirmValueChange = { dismissValue ->
                                    if (dismissValue == SwipeToDismissBoxValue.EndToStart) {
                                        val itemToDelete = row
                                        viewModel.deleteTransaction(itemToDelete.txn.id)
                                        scope.launch {
                                            val result = snackbarHostState.showSnackbar(
                                                message = "Transaction deleted",
                                                actionLabel = "Undo",
                                                duration = SnackbarDuration.Short,
                                            )
                                            if (result == SnackbarResult.ActionPerformed) {
                                                viewModel.addTransaction(
                                                    accountId = itemToDelete.txn.accountId,
                                                    type = itemToDelete.txn.type,
                                                    amount = itemToDelete.txn.amount,
                                                    txnDate = itemToDelete.txn.txnDate,
                                                    categoryId = itemToDelete.txn.categoryId,
                                                    toAccountId = itemToDelete.txn.toAccountId,
                                                    note = itemToDelete.txn.note ?: "",
                                                )
                                            }
                                        }
                                        true
                                    } else {
                                        false
                                    }
                                }
                            )

                            SwipeToDismissBox(
                                state = dismissState,
                                enableDismissFromStartToEnd = false,
                                enableDismissFromEndToStart = true,
                                backgroundContent = {
                                    val color by animateColorAsState(
                                        when (dismissState.targetValue) {
                                            SwipeToDismissBoxValue.EndToStart -> MaterialTheme.colorScheme.errorContainer
                                            else -> Color.Transparent
                                        },
                                        label = "swipe_color"
                                    )
                                    Box(
                                        modifier = Modifier
                                            .fillMaxSize()
                                            .background(color, shape = MaterialTheme.shapes.medium)
                                            .padding(horizontal = 20.dp),
                                        contentAlignment = Alignment.CenterEnd,
                                    ) {
                                        Icon(
                                            Icons.Filled.Delete,
                                            contentDescription = "Delete",
                                            tint = MaterialTheme.colorScheme.onErrorContainer,
                                        )
                                    }
                                },
                            ) {
                                TransactionCard(
                                    row = row,
                                    onEdit = { editingTxn = row },
                                    onDelete = { deletingTxn = row },
                                )
                            }
                        }
                    }
                }
            }
        }
    }

    if (showAddDialog) {
        TransactionDialog(
            accounts = state.accounts,
            categories = state.categories,
            initial = null,
            onDismiss = { showAddDialog = false },
            onSave = { accountId, type, amount, txnDate, categoryId, toAccountId, note ->
                viewModel.addTransaction(accountId, type, amount, txnDate, categoryId, toAccountId, note)
            },
        )
    }
    editingTxn?.let { row ->
        TransactionDialog(
            accounts = state.accounts,
            categories = state.categories,
            initial = row,
            onDismiss = { editingTxn = null },
            onSave = { accountId, type, amount, txnDate, categoryId, toAccountId, note ->
                viewModel.updateTransaction(row.txn.id, accountId, type, amount, txnDate, categoryId, toAccountId, note)
            },
        )
    }
    deletingTxn?.let { row ->
        ConfirmDeleteDialog(
            message = "Delete this transaction?",
            onConfirm = { viewModel.deleteTransaction(row.txn.id) },
            onDismiss = { deletingTxn = null },
        )
    }
}

@Composable
private fun TransactionCard(row: TransactionRow, onEdit: () -> Unit, onDelete: () -> Unit) {
    val txn = row.txn
    var menuOpen by remember { mutableStateOf(false) }
    val subtitle = if (txn.type == "transfer") {
        "${row.accountName} → ${row.toAccountName}"
    } else {
        "${row.accountName} · ${row.categoryName ?: "Uncategorized"}"
    }

    Card(modifier = Modifier.fillMaxWidth()) {
        Row(
            modifier = Modifier.fillMaxWidth().padding(12.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            TransactionTypeAvatar(txn.type)
            Column(modifier = Modifier.padding(start = 12.dp).weight(1f)) {
                Text(subtitle, style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.Medium)
                val note = txn.note ?: ""
                if (note.isNotBlank()) {
                    Text(
                        note,
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
            }
            Text(
                signedAmountText(txn.type, txn.amount),
                color = amountColor(txn.type),
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
