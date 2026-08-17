package com.ravi.moneymanagement.ui.transactions

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
import com.ravi.moneymanagement.data.Account
import com.ravi.moneymanagement.data.Category
import com.ravi.moneymanagement.data.TransactionRow
import com.ravi.moneymanagement.ui.common.AmountField
import com.ravi.moneymanagement.ui.common.AppDatePickerField
import com.ravi.moneymanagement.ui.common.DropdownField
import com.ravi.moneymanagement.ui.common.EditSheet
import com.ravi.moneymanagement.ui.common.TypeSegmentedControl
import com.ravi.moneymanagement.ui.common.soundClick
import java.time.LocalDate

private val TXN_TYPES = listOf("expense", "income", "transfer")

@Composable
fun TransactionDialog(
    accounts: List<Account>,
    categories: List<Category>,
    initial: TransactionRow?,
    onDismiss: () -> Unit,
    onSave: (
        accountId: Long,
        type: String,
        amount: Double,
        txnDate: String,
        categoryId: Long?,
        toAccountId: Long?,
        note: String,
    ) -> Unit,
    initialType: String = "expense",
) {
    var type by remember { mutableStateOf(initial?.txn?.type ?: initialType) }
    var amountText by remember { mutableStateOf(initial?.txn?.amount?.toString() ?: "") }
    var note by remember { mutableStateOf(initial?.txn?.note ?: "") }
    var txnDate by remember {
        mutableStateOf(initial?.txn?.txnDate?.let { runCatching { LocalDate.parse(it) }.getOrNull() } ?: LocalDate.now())
    }
    var accountId by remember { mutableStateOf(initial?.txn?.accountId ?: accounts.firstOrNull()?.id) }
    var toAccountId by remember { mutableStateOf(initial?.txn?.toAccountId ?: accounts.getOrNull(1)?.id) }
    var categoryId by remember {
        mutableStateOf(initial?.txn?.categoryId ?: categories.firstOrNull { it.type == type }?.id)
    }
    var errorText by remember { mutableStateOf<String?>(null) }

    val categoryOptions = categories.filter { it.type == type }

    EditSheet(onDismiss = onDismiss) { dismiss ->
        Text(
            if (initial != null) "Edit Transaction" else "Add Transaction",
            style = MaterialTheme.typography.titleLarge,
        )

        TypeSegmentedControl(
            options = TXN_TYPES,
            selected = type,
            onSelect = { newType ->
                type = newType
                if (newType != "transfer" && categoryOptionFor(categories, newType, categoryId) == null) {
                    categoryId = categories.firstOrNull { it.type == newType }?.id
                }
            },
        )

        AmountField(
            label = "Amount",
            value = amountText,
            onValueChange = { amountText = it },
            modifier = Modifier.fillMaxWidth(),
            isError = errorText != null,
        )

        DropdownField(
            label = if (type == "transfer") "From account" else "Account",
            options = accounts.map { it.name },
            selected = accounts.find { it.id == accountId }?.name ?: "",
            onSelect = { name -> accountId = accounts.find { it.name == name }?.id },
            modifier = Modifier.fillMaxWidth(),
        )

        if (type == "transfer") {
            DropdownField(
                label = "To account",
                options = accounts.map { it.name },
                selected = accounts.find { it.id == toAccountId }?.name ?: "",
                onSelect = { name -> toAccountId = accounts.find { it.name == name }?.id },
                modifier = Modifier.fillMaxWidth(),
            )
        } else {
            DropdownField(
                label = "Category",
                options = categoryOptions.map { it.name },
                selected = categoryOptions.find { it.id == categoryId }?.name ?: "",
                onSelect = { name -> categoryId = categoryOptions.find { it.name == name }?.id },
                modifier = Modifier.fillMaxWidth(),
            )
        }

        AppDatePickerField(
            label = "Date",
            value = txnDate,
            onChange = { txnDate = it },
            modifier = Modifier.fillMaxWidth(),
        )

        OutlinedTextField(
            value = note,
            onValueChange = { note = it },
            label = { Text("Note (optional)") },
            modifier = Modifier.fillMaxWidth(),
        )

        if (errorText != null) {
            Text(errorText!!, color = MaterialTheme.colorScheme.error, style = MaterialTheme.typography.bodySmall)
        }

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            TextButton(onClick = soundClick(dismiss)) { Text("Cancel") }
            Button(
                onClick = soundClick {
                    val amount = amountText.toDoubleOrNull()
                    val accId = accountId
                    errorText = when {
                        accId == null -> "Add an account first"
                        amount == null || amount <= 0 -> "Enter a valid amount"
                        type == "transfer" && (toAccountId == null || toAccountId == accId) -> "Choose a different destination account"
                        else -> null
                    }
                    if (errorText == null && amount != null && accId != null) {
                        onSave(
                            accId,
                            type,
                            amount,
                            txnDate.toString(),
                            if (type == "transfer") null else categoryId,
                            if (type == "transfer") toAccountId else null,
                            note.trim(),
                        )
                        dismiss()
                    }
                },
                modifier = Modifier.weight(1f),
            ) { Text("Save") }
        }
    }
}

private fun categoryOptionFor(categories: List<Category>, type: String, categoryId: Long?): Category? =
    categories.firstOrNull { it.type == type && it.id == categoryId }
