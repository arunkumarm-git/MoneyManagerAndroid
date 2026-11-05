package com.moneymanagement.app.ui.transactions

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.material3.Button
import androidx.compose.material3.ExperimentalMaterial3Api
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
import com.moneymanagement.app.data.Account
import com.moneymanagement.app.data.Category
import com.moneymanagement.app.data.CreditCard
import com.moneymanagement.app.data.TransactionRow
import com.moneymanagement.app.ui.common.AmountField
import com.moneymanagement.app.ui.common.AppDatePickerField
import com.moneymanagement.app.ui.common.DropdownField
import com.moneymanagement.app.ui.common.EditSheet
import com.moneymanagement.app.ui.common.TypeSegmentedControl
import com.moneymanagement.app.ui.common.soundClick
import java.time.LocalDate

// Transfer has been removed – expense and income only
private val TXN_TYPES = listOf("expense", "income")

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TransactionDialog(
    accounts: List<Account>,
    categories: List<Category>,
    creditCards: List<CreditCard> = emptyList(),
    initial: TransactionRow?,
    onDismiss: () -> Unit,
    onSave: (
        accountId: Long?,
        type: String,
        amount: Double,
        txnDate: String,
        categoryId: Long?,
        toAccountId: Long?,
        creditCardId: Long?,
        note: String,
    ) -> Unit,
    initialType: String = "expense",
) {
    var type by remember { mutableStateOf(initial?.txn?.type?.let { if (it == "transfer") "expense" else it } ?: initialType) }
    var amountText by remember { mutableStateOf(initial?.txn?.amount?.toString() ?: "") }
    var note by remember { mutableStateOf(initial?.txn?.note ?: "") }
    var txnDate by remember {
        mutableStateOf(initial?.txn?.txnDate?.let { runCatching { LocalDate.parse(it) }.getOrNull() } ?: LocalDate.now())
    }
    var accountId by remember { mutableStateOf(initial?.txn?.accountId ?: accounts.firstOrNull()?.id) }
    var categoryId by remember {
        mutableStateOf(initial?.txn?.categoryId ?: categories.firstOrNull { it.type == type }?.id)
    }

    // Payment source: "account" (Cash/Bank) or "card" (Credit Card)
    val initialCCId = initial?.txn?.creditCardId
    var paySource by remember { mutableStateOf(if (initialCCId != null) "card" else "account") }
    var creditCardId by remember { mutableStateOf(initialCCId ?: creditCards.firstOrNull()?.id) }

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
                if (categoryOptionFor(categories, newType, categoryId) == null) {
                    categoryId = categories.firstOrNull { it.type == newType }?.id
                }
                // Credit card payment only makes sense for expenses
                if (newType != "expense") paySource = "account"
            },
        )

        AmountField(
            label = "Amount",
            value = amountText,
            onValueChange = { amountText = it },
            modifier = Modifier.fillMaxWidth(),
            isError = errorText != null,
        )

        // Payment source toggle (only for expenses and when credit cards exist)
        if (type == "expense" && creditCards.isNotEmpty()) {
            Text(
                "Pay with",
                style = MaterialTheme.typography.labelMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            TypeSegmentedControl(
                options = listOf("account", "card"),
                selected = paySource,
                onSelect = { paySource = it },
                labels = mapOf("account" to "Cash / Bank", "card" to "Credit Card"),
            )
        }

        // Show account OR credit card picker based on paySource
        if (type != "expense" || paySource == "account") {
            DropdownField(
                label = "Account",
                options = accounts.map { it.name },
                selected = accounts.find { it.id == accountId }?.name ?: "",
                onSelect = { name -> accountId = accounts.find { it.name == name }?.id },
                modifier = Modifier.fillMaxWidth(),
            )
        } else {
            // paySource == "card"
            DropdownField(
                label = "Credit Card",
                options = creditCards.map { "${it.cardName} (${it.bankName})" },
                selected = creditCards.find { it.id == creditCardId }?.let { "${it.cardName} (${it.bankName})" } ?: "",
                onSelect = { label ->
                    creditCardId = creditCards.find { "${it.cardName} (${it.bankName})" == label }?.id
                },
                modifier = Modifier.fillMaxWidth(),
            )
        }

        DropdownField(
            label = "Category",
            options = categoryOptions.map { it.name },
            selected = categoryOptions.find { it.id == categoryId }?.name ?: "",
            onSelect = { name -> categoryId = categoryOptions.find { it.name == name }?.id },
            modifier = Modifier.fillMaxWidth(),
        )

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
                    val usingCard = type == "expense" && paySource == "card" && creditCards.isNotEmpty()
                    val accId = if (usingCard) null else accountId
                    val ccId = if (usingCard) creditCardId else null

                    errorText = when {
                        amount == null || amount <= 0 -> "Enter a valid amount"
                        !usingCard && accId == null -> "Add an account first"
                        usingCard && ccId == null -> "Select a credit card"
                        else -> null
                    }
                    if (errorText == null && amount != null) {
                        onSave(
                            accId,
                            type,
                            amount,
                            txnDate.toString(),
                            categoryId,
                            null,       // toAccountId always null (transfer removed)
                            ccId,
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
