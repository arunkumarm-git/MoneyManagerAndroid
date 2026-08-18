package com.moneymanagement.app.ui.goals

import androidx.compose.foundation.background
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
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
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.Flag
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material.icons.filled.Savings
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Card
import androidx.compose.material3.CenterAlignedTopAppBar
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
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
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import com.moneymanagement.app.data.MoneyRepository
import com.moneymanagement.app.data.SavingsGoal
import com.moneymanagement.app.settings.AppSettings
import com.moneymanagement.app.ui.common.ConfirmDeleteDialog
import com.moneymanagement.app.ui.common.EmptyState
import com.moneymanagement.app.ui.common.formatMoney
import com.moneymanagement.app.ui.common.soundClick
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SavingsGoalsScreen(
    repository: MoneyRepository,
    onBack: (() -> Unit)? = null,
) {
    val goals by repository.savingsGoals.collectAsState(initial = emptyList())
    val currencySymbol by AppSettings.currencySymbol.collectAsState()
    val scope = rememberCoroutineScope()

    var showAddDialog by remember { mutableStateOf(false) }
    var editingGoal by remember { mutableStateOf<SavingsGoal?>(null) }
    var deletingGoal by remember { mutableStateOf<SavingsGoal?>(null) }
    var depositingGoal by remember { mutableStateOf<SavingsGoal?>(null) }

    Scaffold(
        topBar = {
            CenterAlignedTopAppBar(
                title = { Text("Savings Goals") },
            )
        },
        floatingActionButton = {
            FloatingActionButton(onClick = soundClick { showAddDialog = true }) {
                Icon(Icons.Filled.Add, contentDescription = "Add Goal")
            }
        },
    ) { innerPadding ->
        if (goals.isEmpty()) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(innerPadding),
                contentAlignment = Alignment.Center,
            ) {
                EmptyState(
                    icon = Icons.Filled.Savings,
                    title = "No savings goals yet",
                    subtitle = "Set target goals for vacations, emergency funds, or dreams!",
                )
            }
        } else {
            LazyColumn(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(innerPadding)
                    .padding(horizontal = 16.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp),
                contentPadding = androidx.compose.foundation.layout.PaddingValues(vertical = 16.dp),
            ) {
                items(goals, key = { it.id }) { goal ->
                    SavingsGoalCard(
                        goal = goal,
                        currencySymbol = currencySymbol,
                        onDepositClick = { depositingGoal = goal },
                        onEdit = { editingGoal = goal },
                        onDelete = { deletingGoal = goal },
                    )
                }
            }
        }
    }

    if (showAddDialog || editingGoal != null) {
        GoalDialog(
            goal = editingGoal,
            currencySymbol = currencySymbol,
            onDismiss = {
                showAddDialog = false
                editingGoal = null
            },
            onSave = { name, target, current, targetDate, colorHex ->
                scope.launch {
                    if (editingGoal != null) {
                        repository.updateSavingsGoal(editingGoal!!.id, name, target, current, targetDate, colorHex)
                    } else {
                        repository.addSavingsGoal(name, target, current, targetDate, colorHex)
                    }
                    showAddDialog = false
                    editingGoal = null
                }
            },
        )
    }

    depositingGoal?.let { goal ->
        DepositFundsDialog(
            goal = goal,
            currencySymbol = currencySymbol,
            onDismiss = { depositingGoal = null },
            onConfirm = { amount ->
                scope.launch {
                    repository.depositToSavingsGoal(goal.id, amount)
                    depositingGoal = null
                }
            },
        )
    }

    deletingGoal?.let { goal ->
        ConfirmDeleteDialog(
            title = "Delete ${goal.name}?",
            message = "Are you sure you want to remove this savings goal?",
            onDismiss = { deletingGoal = null },
            onConfirm = {
                scope.launch {
                    repository.deleteSavingsGoal(goal.id)
                    deletingGoal = null
                }
            },
        )
    }
}

@Composable
private fun SavingsGoalCard(
    goal: SavingsGoal,
    currencySymbol: String,
    onDepositClick: () -> Unit,
    onEdit: () -> Unit,
    onDelete: () -> Unit,
) {
    val progress = (if (goal.targetAmount > 0) goal.currentAmount / goal.targetAmount else 0.0)
        .coerceIn(0.0, 1.0)
        .toFloat()
    val percentage = (progress * 100).toInt()

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
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Box(
                        modifier = Modifier
                            .size(40.dp)
                            .clip(CircleShape)
                            .background(MaterialTheme.colorScheme.primaryContainer),
                        contentAlignment = Alignment.Center,
                    ) {
                        Icon(
                            Icons.Filled.Flag,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.onPrimaryContainer,
                        )
                    }
                    Spacer(modifier = Modifier.width(12.dp))
                    Column {
                        Text(
                            goal.name,
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold,
                        )
                        if (!goal.targetDate.isNullOrBlank()) {
                            Text(
                                "Target: ${goal.targetDate}",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                            )
                        }
                    }
                }

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

            Spacer(modifier = Modifier.height(16.dp))

            // Progress Bar
            LinearProgressIndicator(
                progress = { progress },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(10.dp)
                    .clip(CircleShape),
            )

            Spacer(modifier = Modifier.height(8.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Text(
                    "$percentage% Reached",
                    style = MaterialTheme.typography.labelMedium,
                    fontWeight = FontWeight.SemiBold,
                    color = MaterialTheme.colorScheme.primary,
                )
                Text(
                    "$currencySymbol ${formatMoney(goal.currentAmount)} / $currencySymbol ${formatMoney(goal.targetAmount)}",
                    style = MaterialTheme.typography.bodyMedium,
                    fontWeight = FontWeight.Medium,
                )
            }

            Spacer(modifier = Modifier.height(12.dp))

            FilledTonalButton(
                onClick = soundClick(onDepositClick),
                modifier = Modifier.fillMaxWidth(),
            ) {
                Icon(Icons.Filled.Add, contentDescription = null, modifier = Modifier.padding(end = 6.dp))
                Text("Add Funds")
            }
        }
    }
}

@Composable
private fun GoalDialog(
    goal: SavingsGoal?,
    currencySymbol: String,
    onDismiss: () -> Unit,
    onSave: (name: String, target: Double, current: Double, targetDate: String?, colorHex: String) -> Unit,
) {
    var name by remember { mutableStateOf(goal?.name ?: "") }
    var targetText by remember { mutableStateOf(goal?.targetAmount?.toString() ?: "") }
    var currentText by remember { mutableStateOf(goal?.currentAmount?.toString() ?: "0") }
    var targetDate by remember { mutableStateOf(goal?.targetDate ?: "") }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(if (goal == null) "New Savings Goal" else "Edit Savings Goal") },
        text = {
            Column(
                modifier = Modifier.verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(12.dp),
            ) {
                OutlinedTextField(
                    value = name,
                    onValueChange = { name = it },
                    label = { Text("Goal Name") },
                    placeholder = { Text("e.g. New Laptop, Vacation") },
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = true,
                )
                OutlinedTextField(
                    value = targetText,
                    onValueChange = { targetText = it },
                    label = { Text("Target Amount ($currencySymbol)") },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = true,
                )
                OutlinedTextField(
                    value = currentText,
                    onValueChange = { currentText = it },
                    label = { Text("Current Saved ($currencySymbol)") },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = true,
                )
                OutlinedTextField(
                    value = targetDate,
                    onValueChange = { targetDate = it },
                    label = { Text("Target Date (Optional)") },
                    placeholder = { Text("YYYY-MM-DD") },
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = true,
                )
            }
        },
        confirmButton = {
            TextButton(
                onClick = {
                    val target = targetText.toDoubleOrNull() ?: 0.0
                    val current = currentText.toDoubleOrNull() ?: 0.0
                    if (name.isNotBlank() && target > 0) {
                        onSave(name.trim(), target, current, targetDate.ifBlank { null }, "#3B82F6")
                    }
                },
                enabled = name.isNotBlank() && (targetText.toDoubleOrNull() ?: 0.0) > 0,
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

@Composable
private fun DepositFundsDialog(
    goal: SavingsGoal,
    currencySymbol: String,
    onDismiss: () -> Unit,
    onConfirm: (Double) -> Unit,
) {
    var amountText by remember { mutableStateOf("") }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Add Funds to ${goal.name}") },
        text = {
            Column {
                Text(
                    "Enter the amount you would like to deposit towards this goal:",
                    style = MaterialTheme.typography.bodyMedium,
                )
                Spacer(modifier = Modifier.height(12.dp))
                OutlinedTextField(
                    value = amountText,
                    onValueChange = { amountText = it },
                    label = { Text("Deposit Amount ($currencySymbol)") },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = true,
                )
            }
        },
        confirmButton = {
            TextButton(
                onClick = {
                    val amt = amountText.toDoubleOrNull() ?: 0.0
                    if (amt > 0) {
                        onConfirm(amt)
                    }
                },
                enabled = (amountText.toDoubleOrNull() ?: 0.0) > 0,
            ) {
                Text("Deposit")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Cancel")
            }
        },
    )
}
