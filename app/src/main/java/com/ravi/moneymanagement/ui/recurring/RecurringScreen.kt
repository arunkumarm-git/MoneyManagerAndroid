package com.ravi.moneymanagement.ui.recurring

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
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
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Autorenew
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material.icons.filled.Schedule
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Card
import androidx.compose.material3.CenterAlignedTopAppBar
import androidx.compose.material3.Checkbox
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExposedDropdownMenuBox
import androidx.compose.material3.ExposedDropdownMenuDefaults
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SuggestionChip
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
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import com.ravi.moneymanagement.data.Account
import com.ravi.moneymanagement.data.Category
import com.ravi.moneymanagement.data.MoneyRepository
import com.ravi.moneymanagement.data.RecurringTransaction
import com.ravi.moneymanagement.settings.AppSettings
import com.ravi.moneymanagement.ui.common.ConfirmDeleteDialog
import com.ravi.moneymanagement.ui.common.EmptyState
import com.ravi.moneymanagement.ui.common.formatMoney
import com.ravi.moneymanagement.ui.common.soundClick
import kotlinx.coroutines.launch
import java.time.LocalDate

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun RecurringScreen(
    repository: MoneyRepository,
    onBack: (() -> Unit)? = null,
) {
    val items by repository.recurringTransactions.collectAsState(initial = emptyList())
    val accounts by repository.accounts.collectAsState(initial = emptyList())
    val categories by repository.categories.collectAsState(initial = emptyList())
    val currencySymbol by AppSettings.currencySymbol.collectAsState()
    val scope = rememberCoroutineScope()

    var showAddDialog by remember { mutableStateOf(false) }
    var editingItem by remember { mutableStateOf<RecurringTransaction?>(null) }
    var deletingItem by remember { mutableStateOf<RecurringTransaction?>(null) }

    Scaffold(
        topBar = {
            CenterAlignedTopAppBar(
                title = { Text("Subscriptions & Recurring") },
            )
        },
        floatingActionButton = {
            FloatingActionButton(onClick = soundClick { showAddDialog = true }) {
                Icon(Icons.Filled.Add, contentDescription = "Add Recurring")
            }
        },
    ) { innerPadding ->
        if (items.isEmpty()) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(innerPadding),
                contentAlignment = Alignment.Center,
            ) {
                EmptyState(
                    icon = Icons.Filled.Autorenew,
                    title = "No recurring transactions",
                    subtitle = "Set up recurring rent, salary, Netflix, utility bills, or subscriptions!",
                )
            }
        } else {
            LazyColumn(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(innerPadding)
                    .padding(horizontal = 16.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp),
                contentPadding = androidx.compose.foundation.layout.PaddingValues(vertical = 16.dp),
            ) {
                items(items, key = { it.id }) { item ->
                    val acc = accounts.find { it.id == item.accountId }
                    val cat = categories.find { it.id == item.categoryId }
                    RecurringCard(
                        item = item,
                        accountName = acc?.name ?: "Account",
                        categoryName = cat?.name ?: item.type.replaceFirstChar { it.uppercase() },
                        currencySymbol = currencySymbol,
                        onToggleActive = { active ->
                            scope.launch { repository.toggleRecurringActive(item.id, active) }
                        },
                        onEdit = { editingItem = item },
                        onDelete = { deletingItem = item },
                    )
                }
            }
        }
    }

    if (showAddDialog || editingItem != null) {
        RecurringDialog(
            item = editingItem,
            accounts = accounts,
            categories = categories,
            currencySymbol = currencySymbol,
            onDismiss = {
                showAddDialog = false
                editingItem = null
            },
            onSave = { accId, catId, type, amount, freq, startDate, nextDue, note, autoApply, isActive ->
                scope.launch {
                    if (editingItem != null) {
                        repository.updateRecurringTransaction(
                            id = editingItem!!.id,
                            accountId = accId,
                            toAccountId = null,
                            categoryId = catId,
                            type = type,
                            amount = amount,
                            frequency = freq,
                            startDate = startDate,
                            nextDueDate = nextDue,
                            note = note,
                            autoApply = autoApply,
                            isActive = isActive,
                        )
                    } else {
                        repository.addRecurringTransaction(
                            accountId = accId,
                            toAccountId = null,
                            categoryId = catId,
                            type = type,
                            amount = amount,
                            frequency = freq,
                            startDate = startDate,
                            nextDueDate = nextDue,
                            note = note,
                            autoApply = autoApply,
                            isActive = isActive,
                        )
                    }
                    showAddDialog = false
                    editingItem = null
                }
            },
        )
    }

    deletingItem?.let { item ->
        ConfirmDeleteDialog(
            title = "Delete Recurring Transaction?",
            message = "Are you sure you want to remove this recurring rule? Past transactions will not be deleted.",
            onDismiss = { deletingItem = null },
            onConfirm = {
                scope.launch {
                    repository.deleteRecurringTransaction(item.id)
                    deletingItem = null
                }
            },
        )
    }
}

@Composable
private fun RecurringCard(
    item: RecurringTransaction,
    accountName: String,
    categoryName: String,
    currencySymbol: String,
    onToggleActive: (Boolean) -> Unit,
    onEdit: () -> Unit,
    onDelete: () -> Unit,
) {
    var showMenu by remember { mutableStateOf(false) }

    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = MaterialTheme.shapes.large,
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.weight(1f)) {
                    Box(
                        modifier = Modifier
                            .size(40.dp)
                            .clip(CircleShape)
                            .background(
                                if (item.type == "income")
                                    MaterialTheme.colorScheme.primaryContainer
                                else
                                    MaterialTheme.colorScheme.errorContainer
                            ),
                        contentAlignment = Alignment.Center,
                    ) {
                        Icon(
                            Icons.Filled.Autorenew,
                            contentDescription = null,
                            tint = if (item.type == "income") MaterialTheme.colorScheme.onPrimaryContainer else MaterialTheme.colorScheme.onErrorContainer,
                        )
                    }
                    Spacer(modifier = Modifier.width(12.dp))
                    Column {
                        Text(
                            item.note?.ifBlank { null } ?: categoryName,
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold,
                        )
                        Text(
                            "$accountName • Next: ${item.nextDueDate}",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                    }
                }

                Row(verticalAlignment = Alignment.CenterVertically) {
                    Switch(
                        checked = item.isActive,
                        onCheckedChange = { onToggleActive(it) },
                    )
                    Box {
                        IconButton(onClick = soundClick { showMenu = true }) {
                            Icon(Icons.Filled.MoreVert, contentDescription = "Options")
                        }
                        DropdownMenu(expanded = showMenu, onDismissRequest = { showMenu = false }) {
                            DropdownMenuItem(
                                text = { Text("Edit") },
                                leadingIcon = { Icon(Icons.Filled.Edit, contentDescription = null) },
                                onClick = {
                                    showMenu = false
                                    onEdit()
                                },
                            )
                            DropdownMenuItem(
                                text = { Text("Delete") },
                                leadingIcon = { Icon(Icons.Filled.Delete, contentDescription = null, tint = MaterialTheme.colorScheme.error) },
                                onClick = {
                                    showMenu = false
                                    onDelete()
                                },
                            )
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(12.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    SuggestionChip(
                        onClick = {},
                        label = { Text(item.frequency.replaceFirstChar { it.uppercase() }) },
                    )
                    if (item.autoApply) {
                        SuggestionChip(
                            onClick = {},
                            label = { Text("Auto-Apply") },
                        )
                    }
                }
                Text(
                    "$currencySymbol ${formatMoney(item.amount)}",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    color = if (item.type == "income") MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.error,
                )
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun RecurringDialog(
    item: RecurringTransaction?,
    accounts: List<Account>,
    categories: List<Category>,
    currencySymbol: String,
    onDismiss: () -> Unit,
    onSave: (
        accId: Long,
        catId: Long?,
        type: String,
        amount: Double,
        frequency: String,
        startDate: String,
        nextDueDate: String,
        note: String?,
        autoApply: Boolean,
        isActive: Boolean,
    ) -> Unit,
) {
    var type by remember { mutableStateOf(item?.type ?: "expense") }
    var amountText by remember { mutableStateOf(item?.amount?.toString() ?: "") }
    var selectedAccountId by remember { mutableStateOf(item?.accountId ?: accounts.firstOrNull()?.id ?: 0L) }
    var selectedCategoryId by remember { mutableStateOf(item?.categoryId ?: categories.firstOrNull { it.type == type }?.id) }
    var frequency by remember { mutableStateOf(item?.frequency ?: "monthly") }
    var startDate by remember { mutableStateOf(item?.startDate ?: LocalDate.now().toString()) }
    var nextDueDate by remember { mutableStateOf(item?.nextDueDate ?: LocalDate.now().toString()) }
    var note by remember { mutableStateOf(item?.note ?: "") }
    var autoApply by remember { mutableStateOf(item?.autoApply ?: true) }

    val frequencies = listOf("daily" to "Daily", "weekly" to "Weekly", "monthly" to "Monthly", "yearly" to "Yearly")

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(if (item == null) "New Recurring Rule" else "Edit Recurring Rule") },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                // Type selector
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                ) {
                    listOf("expense" to "Expense", "income" to "Income").forEach { (t, label) ->
                        FilterChip(
                            selected = type == t,
                            onClick = {
                                type = t
                                selectedCategoryId = categories.firstOrNull { it.type == t }?.id
                            },
                            label = { Text(label) },
                        )
                    }
                }

                // Amount
                OutlinedTextField(
                    value = amountText,
                    onValueChange = { amountText = it },
                    label = { Text("Amount ($currencySymbol)") },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = true,
                )

                // Note/Description
                OutlinedTextField(
                    value = note,
                    onValueChange = { note = it },
                    label = { Text("Title / Note") },
                    placeholder = { Text("e.g. Netflix, Apartment Rent, Salary") },
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = true,
                )

                // Frequency chips
                Text("Frequency", style = MaterialTheme.typography.labelMedium)
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(6.dp),
                ) {
                    frequencies.forEach { (freqKey, label) ->
                        FilterChip(
                            selected = frequency == freqKey,
                            onClick = { frequency = freqKey },
                            label = { Text(label) },
                        )
                    }
                }

                // Next Due Date
                OutlinedTextField(
                    value = nextDueDate,
                    onValueChange = { nextDueDate = it },
                    label = { Text("Next Due Date (YYYY-MM-DD)") },
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = true,
                )

                // Auto apply checkbox
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.fillMaxWidth(),
                ) {
                    Checkbox(
                        checked = autoApply,
                        onCheckedChange = { autoApply = it },
                    )
                    Text("Auto-log transaction on due date", style = MaterialTheme.typography.bodyMedium)
                }
            }
        },
        confirmButton = {
            TextButton(
                onClick = {
                    val amt = amountText.toDoubleOrNull() ?: 0.0
                    if (amt > 0 && selectedAccountId != 0L) {
                        onSave(
                            selectedAccountId,
                            selectedCategoryId,
                            type,
                            amt,
                            frequency,
                            startDate,
                            nextDueDate,
                            note.ifBlank { null },
                            autoApply,
                            true,
                        )
                    }
                },
                enabled = (amountText.toDoubleOrNull() ?: 0.0) > 0,
            ) {
                Text("Save")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Cancel")
            }
        },
    )
}
