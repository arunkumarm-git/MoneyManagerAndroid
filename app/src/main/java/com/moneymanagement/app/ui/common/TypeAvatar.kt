package com.moneymanagement.app.ui.common

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AccountBalance
import androidx.compose.material.icons.filled.AccountBalanceWallet
import androidx.compose.material.icons.filled.ArrowDownward
import androidx.compose.material.icons.filled.ArrowUpward
import androidx.compose.material.icons.filled.CreditCard
import androidx.compose.material.icons.filled.Payments
import androidx.compose.material.icons.filled.Savings
import androidx.compose.material.icons.filled.SwapHoriz
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.unit.dp
import com.moneymanagement.app.ui.theme.CHART_PALETTE
import com.moneymanagement.app.ui.theme.ExpenseRed
import com.moneymanagement.app.ui.theme.IncomeGreen
import com.moneymanagement.app.ui.theme.TransferBlue

@Composable
private fun AvatarShell(color: Color, modifier: Modifier = Modifier, content: @Composable () -> Unit) {
    Box(
        modifier = modifier
            .size(44.dp)
            .background(color.copy(alpha = 0.16f), CircleShape),
        contentAlignment = Alignment.Center,
    ) {
        content()
    }
}

@Composable
fun TransactionTypeAvatar(type: String, modifier: Modifier = Modifier) {
    val (color, icon) = when (type) {
        "income" -> IncomeGreen to Icons.Filled.ArrowDownward
        "expense" -> ExpenseRed to Icons.Filled.ArrowUpward
        else -> TransferBlue to Icons.Filled.SwapHoriz
    }
    AvatarShell(color, modifier) {
        Icon(icon, contentDescription = type, tint = color)
    }
}

private fun accountTypeIcon(type: String): ImageVector = when (type) {
    "bank" -> Icons.Filled.AccountBalance
    "card" -> Icons.Filled.CreditCard
    "wallet" -> Icons.Filled.AccountBalanceWallet
    "cash" -> Icons.Filled.Payments
    else -> Icons.Filled.Savings
}

@Composable
fun AccountTypeAvatar(type: String, modifier: Modifier = Modifier) {
    AvatarShell(MaterialTheme.colorScheme.primary, modifier) {
        Icon(accountTypeIcon(type), contentDescription = type, tint = MaterialTheme.colorScheme.primary)
    }
}

@Composable
fun CategoryAvatar(id: Long, name: String, modifier: Modifier = Modifier) {
    val color = CHART_PALETTE[(id % CHART_PALETTE.size).toInt().let { if (it < 0) it + CHART_PALETTE.size else it }]
    AvatarShell(color, modifier) {
        Text(
            name.firstOrNull()?.uppercaseChar()?.toString() ?: "?",
            style = MaterialTheme.typography.titleMedium,
            color = color,
        )
    }
}
