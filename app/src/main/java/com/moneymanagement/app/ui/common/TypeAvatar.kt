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
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.moneymanagement.app.ui.theme.AccountBankEnd
import com.moneymanagement.app.ui.theme.AccountBankStart
import com.moneymanagement.app.ui.theme.AccountCashEnd
import com.moneymanagement.app.ui.theme.AccountCashStart
import com.moneymanagement.app.ui.theme.AccountCreditEnd
import com.moneymanagement.app.ui.theme.AccountCreditStart
import com.moneymanagement.app.ui.theme.AccountWalletEnd
import com.moneymanagement.app.ui.theme.AccountWalletStart
import com.moneymanagement.app.ui.theme.CHART_PALETTE
import com.moneymanagement.app.ui.theme.ExpenseRed
import com.moneymanagement.app.ui.theme.IncomeGreen
import com.moneymanagement.app.ui.theme.TransferBlue

/**
 * Base avatar shell with a gradient background for a premium visual feel.
 * Von Restorff Effect — color-coded avatars make each transaction type immediately distinct.
 */
@Composable
private fun GradientAvatarShell(
    gradientStart: Color,
    gradientEnd: Color,
    modifier: Modifier = Modifier,
    size: Dp = 44.dp,
    content: @Composable () -> Unit,
) {
    Box(
        modifier = modifier
            .size(size)
            .clip(CircleShape)
            .background(
                Brush.linearGradient(listOf(gradientStart, gradientEnd)),
            ),
        contentAlignment = Alignment.Center,
    ) {
        content()
    }
}

@Composable
fun TransactionTypeAvatar(type: String, modifier: Modifier = Modifier) {
    val (start, end, icon) = when (type) {
        "income" -> Triple(IncomeGreen.copy(alpha = 0.7f), IncomeGreen, Icons.Filled.ArrowDownward)
        "expense" -> Triple(ExpenseRed.copy(alpha = 0.7f), ExpenseRed, Icons.Filled.ArrowUpward)
        else -> Triple(TransferBlue.copy(alpha = 0.7f), TransferBlue, Icons.Filled.SwapHoriz)
    }
    GradientAvatarShell(start, end, modifier) {
        Icon(icon, contentDescription = type, tint = Color.White, modifier = Modifier.size(20.dp))
    }
}

private fun accountTypeIcon(type: String): ImageVector = when (type) {
    "bank" -> Icons.Filled.AccountBalance
    "card" -> Icons.Filled.CreditCard
    "wallet" -> Icons.Filled.AccountBalanceWallet
    "cash" -> Icons.Filled.Payments
    else -> Icons.Filled.Savings
}

private fun accountTypeGradient(type: String): Pair<Color, Color> = when (type) {
    "bank" -> AccountBankStart to AccountBankEnd
    "cash" -> AccountCashStart to AccountCashEnd
    "wallet" -> AccountWalletStart to AccountWalletEnd
    "card" -> AccountCreditStart to AccountCreditEnd
    else -> AccountBankStart to AccountBankEnd
}

@Composable
fun AccountTypeAvatar(type: String, modifier: Modifier = Modifier) {
    val (start, end) = accountTypeGradient(type)
    GradientAvatarShell(start, end, modifier) {
        Icon(accountTypeIcon(type), contentDescription = type, tint = Color.White, modifier = Modifier.size(20.dp))
    }
}

@Composable
fun CategoryAvatar(id: Long, name: String, modifier: Modifier = Modifier) {
    val color = CHART_PALETTE[(id % CHART_PALETTE.size).toInt().let { if (it < 0) it + CHART_PALETTE.size else it }]
    GradientAvatarShell(
        gradientStart = color.copy(alpha = 0.6f),
        gradientEnd = color,
        modifier = modifier,
    ) {
        Text(
            name.firstOrNull()?.uppercaseChar()?.toString() ?: "?",
            style = MaterialTheme.typography.titleSmall,
            fontWeight = FontWeight.Bold,
            color = Color.White,
        )
    }
}
