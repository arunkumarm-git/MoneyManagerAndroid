package com.moneymanagement.app.ui.transactions

import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
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
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ReceiptLong
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material.icons.filled.Sms
import androidx.compose.material3.Badge
import androidx.compose.material3.BadgedBox
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CenterAlignedTopAppBar
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExtendedFloatingActionButton
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarDuration
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.SnackbarResult
import androidx.compose.material3.Surface
import androidx.compose.material3.SwipeToDismissBox
import androidx.compose.material3.SwipeToDismissBoxValue
import androidx.compose.material3.Text
import androidx.compose.material3.rememberSwipeToDismissBoxState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
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
import com.moneymanagement.app.ui.theme.ExpenseRed
import com.moneymanagement.app.ui.theme.IncomeGreen
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TransactionsScreen(
    repository: MoneyRepository,
    onNavigateToSms: (() -> Unit)? = null,
) {
    val viewModel: TransactionsViewModel = viewModel(factory = GenericViewModelFactory { TransactionsViewModel(repository) })
    val state by viewModel.uiState.collectAsState()
    val pendingSmsCount by repository.pendingSmsCount.collectAsState(initial = 0)
    val scope = rememberCoroutineScope()
    val snackbarHostState = remember { SnackbarHostState() }

    var showAddDialog by remember { mutableStateOf(false) }
    var editingTxn by remember { mutableStateOf<TransactionRow?>(null) }
    var deletingTxn by remember { mutableStateOf<TransactionRow?>(null) }

    Scaffold(
        topBar = {
            CenterAlignedTopAppBar(
                title = {
                    Text(
                        "Transactions",
                        style = MaterialTheme.typography.titleLarge,
                        fontWeight = FontWeight.Bold,
                    )
                },
                actions = {
                    if (onNavigateToSms != null) {
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
                    }
                },
            )
        },
        snackbarHost = { SnackbarHost(snackbarHostState) },
        floatingActionButton = {
            ExtendedFloatingActionButton(
                onClick = soundClick { showAddDialog = true },
                icon = { Icon(Icons.Filled.Add, contentDescription = null) },
                text = { Text("Add Transaction", fontWeight = FontWeight.SemiBold) },
                containerColor = MaterialTheme.colorScheme.primary,
                contentColor = MaterialTheme.colorScheme.onPrimary,
            )
        },
    ) { innerPadding ->
        Column(modifier = Modifier.fillMaxSize().padding(innerPadding)) {
            // Filter Chips Bar
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .horizontalScroll(rememberScrollState())
                    .padding(horizontal = 16.dp, vertical = 10.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                TRANSACTION_FILTERS.forEach { name ->
                    FilterChip(
                        selected = state.filter == name,
                        onClick = soundClick { viewModel.setFilter(name) },
                        label = {
                            Text(
                                name,
                                fontWeight = if (state.filter == name) FontWeight.Bold else FontWeight.Normal,
                            )
                        },
                        colors = FilterChipDefaults.filterChipColors(
                            selectedContainerColor = MaterialTheme.colorScheme.primary,
                            selectedLabelColor = MaterialTheme.colorScheme.onPrimary,
                        ),
                    )
                }
            }

            if (state.transactions.isEmpty()) {
                EmptyState(
                    icon = Icons.AutoMirrored.Filled.ReceiptLong,
                    title = "No transactions yet",
                    subtitle = "Tap the button below to record your first one.",
                    modifier = Modifier.padding(16.dp),
                )
            } else {
                val grouped = state.transactions.groupBy { relativeDateLabel(it.txn.txnDate) }
                LazyColumn(
                    modifier = Modifier.fillMaxSize(),
                    contentPadding = PaddingValues(horizontal = 16.dp, vertical = 4.dp, ),
                    verticalArrangement = Arrangement.spacedBy(8.dp),
                ) {
                    grouped.forEach { (label, rows) ->
                        // Sticky-style date header
                        item(key = "header-$label") {
                            Surface(
                                modifier = Modifier.padding(vertical = 6.dp),
                                shape = RoundedCornerShape(8.dp),
                                color = MaterialTheme.colorScheme.surfaceContainerHigh,
                            ) {
                                Text(
                                    label,
                                    style = MaterialTheme.typography.labelMedium,
                                    fontWeight = FontWeight.SemiBold,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                                    modifier = Modifier.padding(horizontal = 10.dp, vertical = 5.dp),
                                )
                            }
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
                                                    creditCardId = itemToDelete.txn.creditCardId,
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
                                        animationSpec = tween(200),
                                        label = "swipe_color"
                                    )
                                    Box(
                                        modifier = Modifier
                                            .fillMaxSize()
                                            .background(color, shape = MaterialTheme.shapes.large)
                                            .padding(horizontal = 20.dp),
                                        contentAlignment = Alignment.CenterEnd,
                                    ) {
                                        Icon(
                                            Icons.Filled.Delete,
                                            contentDescription = "Delete",
                                            tint = MaterialTheme.colorScheme.onErrorContainer,
                                            modifier = Modifier.size(24.dp),
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
                    // Bottom padding for FAB
                    item { Spacer(modifier = Modifier.height(72.dp)) }
                }
            }
        }
    }

    if (showAddDialog) {
        TransactionDialog(
            accounts = state.accounts,
            categories = state.categories,
            creditCards = state.creditCards,
            initial = null,
            onDismiss = { showAddDialog = false },
            onSave = { accountId, type, amount, txnDate, categoryId, toAccountId, creditCardId, note ->
                viewModel.addTransaction(accountId, type, amount, txnDate, categoryId, toAccountId, creditCardId, note)
            },
        )
    }
    editingTxn?.let { row ->
        TransactionDialog(
            accounts = state.accounts,
            categories = state.categories,
            creditCards = state.creditCards,
            initial = row,
            onDismiss = { editingTxn = null },
            onSave = { accountId, type, amount, txnDate, categoryId, toAccountId, creditCardId, note ->
                viewModel.updateTransaction(row.txn.id, accountId, type, amount, txnDate, categoryId, toAccountId, creditCardId, note)
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
    // Build subtitle: credit card name, or account · category
    val subtitle = when {
        row.creditCardName != null -> "${row.creditCardName} · ${row.categoryName ?: "Uncategorized"}"
        else -> "${row.accountName ?: ""} · ${row.categoryName ?: "Uncategorized"}"
    }
    val accentColor = when (txn.type) {
        "income" -> IncomeGreen
        "expense" -> ExpenseRed
        else -> Color(0xFF448AFF)
    }

    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = MaterialTheme.shapes.large,
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainer),
    ) {
        Row(modifier = Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
            // Left accent border
            Box(
                modifier = Modifier
                    .width(4.dp)
                    .height(72.dp)
                    .background(accentColor, RoundedCornerShape(topStart = 16.dp, bottomStart = 16.dp)),
            )
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 12.dp, vertical = 10.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                TransactionTypeAvatar(txn.type)
                Column(modifier = Modifier.padding(start = 12.dp).weight(1f)) {
                    Text(subtitle, style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.SemiBold)
                    val note = txn.note ?: ""
                    if (note.isNotBlank()) {
                        Text(
                            note,
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                    }
                    Text(
                        txn.txnDate,
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f),
                    )
                }
                Text(
                    signedAmountText(txn.type, txn.amount),
                    color = amountColor(txn.type),
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
