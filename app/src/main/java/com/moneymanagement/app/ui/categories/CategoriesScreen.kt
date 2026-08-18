package com.moneymanagement.app.ui.categories

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
import androidx.compose.material.icons.filled.Category
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
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Tab
import androidx.compose.material3.TabRow
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.moneymanagement.app.data.Category
import com.moneymanagement.app.data.MoneyRepository
import com.moneymanagement.app.ui.common.CategoryAvatar
import com.moneymanagement.app.ui.common.ConfirmDeleteDialog
import com.moneymanagement.app.ui.common.EmptyState
import com.moneymanagement.app.ui.common.GenericViewModelFactory
import com.moneymanagement.app.ui.common.formatMoney
import com.moneymanagement.app.ui.common.soundClick
import com.moneymanagement.app.ui.theme.ExpenseRed

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CategoriesScreen(repository: MoneyRepository) {
    val viewModel: CategoriesViewModel = viewModel(factory = GenericViewModelFactory { CategoriesViewModel(repository) })
    val state by viewModel.uiState.collectAsState()

    var showAddDialog by remember { mutableStateOf(false) }
    var editingCategory by remember { mutableStateOf<Category?>(null) }
    var deletingCategory by remember { mutableStateOf<Category?>(null) }
    var tabIndex by remember { mutableStateOf(0) }

    Scaffold(
        topBar = { CenterAlignedTopAppBar(title = { Text("Categories") }) },
        floatingActionButton = {
            ExtendedFloatingActionButton(
                onClick = soundClick { showAddDialog = true },
                icon = { Icon(Icons.Filled.Add, contentDescription = null) },
                text = { Text("Add category") },
            )
        },
    ) { innerPadding ->
        Column(modifier = Modifier.fillMaxSize().padding(innerPadding)) {
            TabRow(selectedTabIndex = tabIndex) {
                Tab(selected = tabIndex == 0, onClick = soundClick { tabIndex = 0 }, text = { Text("Expense") })
                Tab(selected = tabIndex == 1, onClick = soundClick { tabIndex = 1 }, text = { Text("Income") })
            }
            val list = if (tabIndex == 0) state.expenseCategories else state.incomeCategories
            if (list.isEmpty()) {
                EmptyState(
                    icon = Icons.Filled.Category,
                    title = if (tabIndex == 0) "No expense categories yet" else "No income categories yet",
                    subtitle = "Tap \"Add category\" to create one.",
                    modifier = Modifier.padding(16.dp),
                )
            } else {
                LazyColumn(
                    modifier = Modifier.fillMaxSize(),
                    contentPadding = PaddingValues(16.dp),
                    verticalArrangement = Arrangement.spacedBy(10.dp),
                ) {
                    items(list, key = { it.id }) { cat ->
                        CategoryRow(
                            cat,
                            if (tabIndex == 0) state.spendByCategory[cat.id] ?: 0.0 else 0.0,
                            onEdit = { editingCategory = cat },
                            onDelete = { deletingCategory = cat },
                        )
                    }
                }
            }
        }
    }

    if (showAddDialog) {
        CategoryDialog(
            category = null,
            onDismiss = { showAddDialog = false },
            onSave = { name, type, budget -> viewModel.addCategory(name, type, budget) },
        )
    }
    editingCategory?.let { cat ->
        CategoryDialog(
            category = cat,
            onDismiss = { editingCategory = null },
            onSave = { name, type, budget -> viewModel.updateCategory(cat.id, name, type, budget) },
        )
    }
    deletingCategory?.let { cat ->
        ConfirmDeleteDialog(
            message = "Delete '${cat.name}'? Existing transactions keep their history but lose this category.",
            onConfirm = { viewModel.deleteCategory(cat.id) },
            onDismiss = { deletingCategory = null },
        )
    }
}

@Composable
private fun CategoryRow(category: Category, spent: Double, onEdit: () -> Unit, onDelete: () -> Unit) {
    var menuOpen by remember { mutableStateOf(false) }
    Card(modifier = Modifier.fillMaxWidth()) {
        Column(modifier = Modifier.padding(12.dp)) {
            Row(modifier = Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                CategoryAvatar(category.id, category.name)
                Column(modifier = Modifier.padding(start = 12.dp).weight(1f)) {
                    Text(category.name, style = MaterialTheme.typography.titleMedium)
                    val budgetText = category.monthlyBudget?.let { "₹${formatMoney(it, 0)}/mo" } ?: "No budget"
                    Text(budgetText, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
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
            val budget = category.monthlyBudget
            if (category.type == "expense" && budget != null && budget > 0) {
                val pct = (spent / budget).coerceIn(0.0, 1.0)
                LinearProgressIndicator(
                    progress = { pct.toFloat() },
                    modifier = Modifier.fillMaxWidth().padding(top = 10.dp),
                    color = if (pct >= 1.0) ExpenseRed else MaterialTheme.colorScheme.primary,
                )
                Text(
                    "Spent ₹${formatMoney(spent, 0)} this month",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.padding(top = 4.dp),
                )
            }
        }
    }
}
