package com.moneymanagement.app.ui.categories

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.material3.Button
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.moneymanagement.app.data.Category
import com.moneymanagement.app.ui.common.EditSheet
import com.moneymanagement.app.ui.common.TypeSegmentedControl
import com.moneymanagement.app.ui.common.soundClick

private val CATEGORY_TYPES = listOf("expense", "income")

@Composable
fun CategoryDialog(
    category: Category?,
    onDismiss: () -> Unit,
    onSave: (name: String, type: String, monthlyBudget: Double?) -> Unit,
) {
    var name by remember { mutableStateOf(category?.name ?: "") }
    var type by remember { mutableStateOf(category?.type ?: CATEGORY_TYPES.first()) }
    var budgetText by remember { mutableStateOf(category?.monthlyBudget?.toString() ?: "") }
    var errorText by remember { mutableStateOf<String?>(null) }

    EditSheet(onDismiss = onDismiss) { dismiss ->
        Text(
            if (category != null) "Edit Category" else "Add Category",
            style = MaterialTheme.typography.titleLarge,
        )

        TypeSegmentedControl(
            options = CATEGORY_TYPES,
            selected = type,
            onSelect = { type = it },
        )

        OutlinedTextField(
            value = name,
            onValueChange = { name = it },
            label = { Text("Category name") },
            isError = errorText != null,
            modifier = Modifier.fillMaxWidth(),
        )
        OutlinedTextField(
            value = budgetText,
            onValueChange = { budgetText = it },
            label = { Text("Monthly budget (optional)") },
            leadingIcon = { Text("₹", style = MaterialTheme.typography.titleMedium) },
            modifier = Modifier.fillMaxWidth(),
        )

        if (errorText != null) {
            Text(errorText!!, color = MaterialTheme.colorScheme.error, style = MaterialTheme.typography.bodySmall)
        }

        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
            TextButton(onClick = soundClick(dismiss)) { Text("Cancel") }
            Button(
                onClick = soundClick {
                    val budget = budgetText.trim().takeIf { it.isNotEmpty() }?.toDoubleOrNull()
                    if (name.isBlank()) {
                        errorText = "Enter a category name"
                    } else {
                        onSave(name.trim(), type, budget)
                        dismiss()
                    }
                },
                modifier = Modifier.weight(1f),
            ) { Text("Save") }
        }
    }
}
