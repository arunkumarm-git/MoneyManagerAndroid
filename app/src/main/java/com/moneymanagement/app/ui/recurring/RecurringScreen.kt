package com.moneymanagement.app.ui.recurring

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
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Autorenew
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CenterAlignedTopAppBar
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExtendedFloatingActionButton
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
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
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import com.moneymanagement.app.data.Account
import com.moneymanagement.app.data.Category
import com.moneymanagement.app.data.MoneyRepository
import com.moneymanagement.app.data.RecurringTransaction
import com.moneymanagement.app.settings.AppSettings
import com.moneymanagement.app.ui.common.AppDatePickerField
import com.moneymanagement.app.ui.common.ConfirmDeleteDialog
import com.moneymanagement.app.ui.common.EditSheet
import com.moneymanagement.app.ui.common.EmptyState
import com.moneymanagement.app.ui.common.SectionHeader
import com.moneymanagement.app.ui.common.formatMoney
import com.moneymanagement.app.ui.common.soundClick
import com.moneymanagement.app.ui.theme.ExpenseRed
import com.moneymanagement.app.ui.theme.IncomeGreen
import kotlinx.coroutines.launch
import java.time.LocalDate
import java.time.temporal.ChronoUnit

private fun daysUntilLabel(nextDueDateStr: String): String {
    return try {
        val today = LocalDate.now()
        val target = LocalDate.parse(nextDueDateStr)
        val days = ChronoUnit.DAYS.between(today, target)
        when {
            days < 0 -> "⚠️ Overdue by ${-days}d"
            days == 0L -> "🔴 Due today"
            days == 1L -> "🟡 Due tomorrow"
            days <= 7 -> "🟡 In $days days"
            else -> "🟢 In $days days"
        }
    } catch (e: Exception) {
        nextDueDateStr
    }
}

private val FREQUENCY_ORDER = listOf("daily", "weekly", "monthly", "yearly")

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
                title = {
                    Text(
                        "Subscriptions & Recurring",
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
                text = { Text("Add Rule", fontWeight = FontWeight.SemiBold) },
                containerColor = MaterialTheme.colorScheme.primary,
                contentColor = MaterialTheme.colorScheme.onPrimary,
            )
        },
    ) { innerPadding ->
        if (items.isEmpty()) {
            Box(
                modifier = Modifier.fillMaxSize().padding(innerPadding),
                contentAlignment = Alignment.Center,
            ) {
                EmptyState(
                    icon = Icons.Filled.Autorenew,
                    title = "No recurring transactions",
                    subtitle = "Set up recurring rent, salary, Netflix, utility bills, or subscriptions!",
                    modifier = Modifier.padding(16.dp),
                )
            }
        } else {
            // Group by frequency in canonical order
            val grouped = FREQUENCY_ORDER
                .mapNotNull { freq -> items.filter { it.frequency == freq }.takeIf { it.isNotEmpty() }?.let { freq to it } }
                .toMap()

            LazyColumn(
                modifier = Modifier.fillMaxSize().padding(innerPadding),
                contentPadding = PaddingValues(horizontal = 16.dp, vertical = 16.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                grouped.forEach { (freq, freqItems) ->
                    item(key = "header-$freq") {
                        SectionHeader(
                            title = freq.replaceFirstChar { it.uppercase() },
                            modifier = Modifier.padding(vertical = 6.dp),
                        )
                    }
                    items(freqItems, key = { it.id }) { item ->
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
                item { Spacer(modifier = Modifier.height(72.dp)) }
            }
        }
    }

    if (showAddDialog || editingItem != null) {
        EditSheet(
            onDismiss = {
                showAddDialog = false
                editingItem = null
            },
            title = if (editingItem == null) "New Recurring Rule" else "Edit Recurring Rule",
        ) { dismiss ->
            RecurringSheetContent(
                item = editingItem,
                accounts = accounts,
                categories = categories,
                currencySymbol = currencySymbol,
                onDismiss = dismiss,
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
                        dismiss()
                    }
                },
            )
        }
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

    val (accentStart, accentEnd) = if (item.type == "income")
        IncomeGreen.copy(alpha = 0.7f) to IncomeGreen
    else
        ExpenseRed.copy(alpha = 0.7f) to ExpenseRed

    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = MaterialTheme.shapes.large,
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainer),
    ) {
        Row(modifier = Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
            // Left accent border with gradient
            Box(
                modifier = Modifier
                    .width(4.dp)
                    .height(88.dp)
                    .background(
                        Brush.verticalGradient(listOf(accentStart, accentEnd)),
                        RoundedCornerShape(topStart = 16.dp, bottomStart = 16.dp),
                    ),
            )
            Column(
                modifier = Modifier.padding(horizontal = 14.dp, vertical = 12.dp).fillMaxWidth(),
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.weight(1f)) {
                        Box(
                            modifier = Modifier
                                .size(36.dp)
                                .clip(CircleShape)
                                .background(
                                    Brush.linearGradient(listOf(accentStart, accentEnd))
                                ),
                            contentAlignment = Alignment.Center,
                        ) {
                            Icon(
                                Icons.Filled.Autorenew,
                                contentDescription = null,
                                tint = Color.White,
                                modifier = Modifier.size(18.dp),
                            )
                        }
                        Column(modifier = Modifier.padding(start = 10.dp)) {
                            Text(
                                item.note?.ifBlank { null } ?: categoryName,
                                style = MaterialTheme.typography.titleSmall,
                                fontWeight = FontWeight.Bold,
                            )
                            Text(
                                accountName,
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                            )
                        }
                    }
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Switch(
                            checked = item.isActive,
                            onCheckedChange = { onToggleActive(it) },
                            modifier = Modifier.size(width = 44.dp, height = 28.dp),
                        )
                        Box {
                            IconButton(onClick = soundClick { showMenu = true }, modifier = Modifier.size(32.dp)) {
                                Icon(Icons.Filled.MoreVert, contentDescription = "Options", modifier = Modifier.size(18.dp))
                            }
                            DropdownMenu(expanded = showMenu, onDismissRequest = { showMenu = false }) {
                                DropdownMenuItem(
                                    text = { Text("Edit") },
                                    leadingIcon = { Icon(Icons.Filled.Edit, contentDescription = null) },
                                    onClick = { showMenu = false; onEdit() },
                                )
                                DropdownMenuItem(
                                    text = { Text("Delete") },
                                    leadingIcon = {
                                        Icon(
                                            Icons.Filled.Delete,
                                            contentDescription = null,
                                            tint = MaterialTheme.colorScheme.error,
                                        )
                                    },
                                    onClick = { showMenu = false; onDelete() },
                                )
                            }
                        }
                    }
                }

                Spacer(modifier = Modifier.height(10.dp))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    // Countdown label
                    Surface(
                        shape = RoundedCornerShape(6.dp),
                        color = MaterialTheme.colorScheme.surfaceContainerHigh,
                    ) {
                        Text(
                            daysUntilLabel(item.nextDueDate),
                            style = MaterialTheme.typography.labelSmall,
                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
                        )
                    }
                    if (item.autoApply) {
                        Surface(
                            shape = RoundedCornerShape(6.dp),
                            color = MaterialTheme.colorScheme.primaryContainer,
                        ) {
                            Text(
                                "Auto",
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.primary,
                                fontWeight = FontWeight.Bold,
                                modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
                            )
                        }
                    }
                    Text(
                        "$currencySymbol ${formatMoney(item.amount)}",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.ExtraBold,
                        color = if (item.type == "income") IncomeGreen else ExpenseRed,
                    )
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun RecurringSheetContent(
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
    var nextDueDate by remember {
        mutableStateOf(
            item?.nextDueDate?.let { runCatching { LocalDate.parse(it) }.getOrNull() } ?: LocalDate.now()
        )
    }
    var note by remember { mutableStateOf(item?.note ?: "") }
    var autoApply by remember { mutableStateOf(item?.autoApply ?: true) }

    val frequencies = listOf("daily" to "Daily", "weekly" to "Weekly", "monthly" to "Monthly", "yearly" to "Yearly")

    // Type selector
    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
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

    OutlinedTextField(
        value = amountText,
        onValueChange = { amountText = it },
        label = { Text("Amount ($currencySymbol)") },
        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
        modifier = Modifier.fillMaxWidth(),
        singleLine = true,
    )

    OutlinedTextField(
        value = note,
        onValueChange = { note = it },
        label = { Text("Title / Note") },
        placeholder = { Text("e.g. Netflix, Apartment Rent, Salary") },
        modifier = Modifier.fillMaxWidth(),
        singleLine = true,
    )

    Text("Frequency", style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(6.dp)) {
        frequencies.forEach { (freqKey, label) ->
            FilterChip(
                selected = frequency == freqKey,
                onClick = { frequency = freqKey },
                label = { Text(label) },
            )
        }
    }

    AppDatePickerField(
        label = "Next Due Date",
        value = nextDueDate,
        onChange = { nextDueDate = it },
        modifier = Modifier.fillMaxWidth(),
    )

    Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.fillMaxWidth()) {
        Text(
            "Auto-log on due date",
            style = MaterialTheme.typography.bodyMedium,
            modifier = Modifier.weight(1f),
        )
        Switch(checked = autoApply, onCheckedChange = { autoApply = it })
    }

    Spacer(modifier = Modifier.height(4.dp))

    Button(
        onClick = soundClick {
            val amt = amountText.toDoubleOrNull() ?: 0.0
            if (amt > 0 && selectedAccountId != 0L) {
                onSave(
                    selectedAccountId,
                    selectedCategoryId,
                    type,
                    amt,
                    frequency,
                    LocalDate.now().toString(),
                    nextDueDate.toString(),
                    note.ifBlank { null },
                    autoApply,
                    true,
                )
            }
        },
        enabled = (amountText.toDoubleOrNull() ?: 0.0) > 0,
        modifier = Modifier.fillMaxWidth(),
        shape = MaterialTheme.shapes.medium,
    ) {
        Text("Save Rule", fontWeight = FontWeight.Bold)
    }
    TextButton(onClick = soundClick(onDismiss), modifier = Modifier.fillMaxWidth()) {
        Text("Cancel")
    }
}
