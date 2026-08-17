package com.ravi.moneymanagement.ui.accounts

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
import com.ravi.moneymanagement.ui.common.DropdownField
import com.ravi.moneymanagement.ui.common.EditSheet
import com.ravi.moneymanagement.ui.common.soundClick

private val ACCOUNT_TYPES = listOf("cash", "bank", "card", "wallet", "other")

@Composable
fun AccountDialog(
    account: Account?,
    onDismiss: () -> Unit,
    onSave: (name: String, type: String, initialBalance: Double) -> Unit,
) {
    var name by remember { mutableStateOf(account?.name ?: "") }
    var type by remember { mutableStateOf(account?.type ?: ACCOUNT_TYPES.first()) }
    var balanceText by remember { mutableStateOf(account?.initialBalance?.toString() ?: "0") }
    var errorText by remember { mutableStateOf<String?>(null) }

    EditSheet(onDismiss = onDismiss) { dismiss ->
        Text(
            if (account != null) "Edit Account" else "Add Account",
            style = MaterialTheme.typography.titleLarge,
        )

        OutlinedTextField(
            value = name,
            onValueChange = { name = it },
            label = { Text("Account name") },
            isError = errorText != null,
            modifier = Modifier.fillMaxWidth(),
        )
        DropdownField(
            label = "Account type",
            options = ACCOUNT_TYPES,
            selected = type,
            onSelect = { type = it },
            modifier = Modifier.fillMaxWidth(),
        )
        OutlinedTextField(
            value = balanceText,
            onValueChange = { balanceText = it },
            label = { Text("Opening balance") },
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
                    val balance = balanceText.toDoubleOrNull() ?: 0.0
                    if (name.isBlank()) {
                        errorText = "Enter an account name"
                    } else {
                        onSave(name.trim(), type, balance)
                        dismiss()
                    }
                },
                modifier = Modifier.weight(1f),
            ) { Text("Save") }
        }
    }
}
