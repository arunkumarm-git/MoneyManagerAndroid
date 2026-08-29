package com.moneymanagement.app.ui.common

import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import com.moneymanagement.app.ui.theme.ExpenseRed
import com.moneymanagement.app.ui.theme.IncomeGreen
import com.moneymanagement.app.ui.theme.TransferBlue
import java.time.LocalDate
import java.time.format.DateTimeFormatter
import java.util.Locale

import com.moneymanagement.app.settings.AppSettings

fun formatMoney(value: Double, decimals: Int = 2): String =
    String.format(Locale.US, "%,.${decimals}f", value)

fun formatMoneyWithSymbol(value: Double, decimals: Int = 2, masked: Boolean = false): String {
    if (masked) return "${AppSettings.currencySymbol.value} ••••••"
    val symbol = AppSettings.currencySymbol.value
    val absValue = kotlin.math.abs(value)
    return if (value < 0.0) "-$symbol ${formatMoney(absValue, decimals)}" else "$symbol ${formatMoney(value, decimals)}"
}

fun formatSignedMoneyWithSymbol(value: Double, decimals: Int = 2, masked: Boolean = false): String {
    if (masked) return "••••••"
    val symbol = AppSettings.currencySymbol.value
    val absValue = kotlin.math.abs(value)
    val sign = if (value > 0.0) "+" else if (value < 0.0) "-" else ""
    return if (sign.isNotEmpty()) "$sign$symbol ${formatMoney(absValue, decimals)}" else "$symbol ${formatMoney(0.0, decimals)}"
}

fun amountColor(type: String): Color = when (type) {
    "income" -> IncomeGreen
    "expense" -> ExpenseRed
    else -> TransferBlue
}

fun signedAmountText(type: String, amount: Double, decimals: Int = 2, masked: Boolean = false): String {
    if (masked) return "••••••"
    val sign = when (type) {
        "income" -> "+"
        "expense" -> "-"
        else -> ""
    }
    val absAmount = kotlin.math.abs(amount)
    return if (sign.isNotEmpty()) "$sign ${AppSettings.currencySymbol.value}${formatMoney(absAmount, decimals)}" else "${AppSettings.currencySymbol.value}${formatMoney(absAmount, decimals)}"
}

private val DISPLAY_DATE_FORMAT: DateTimeFormatter = DateTimeFormatter.ofPattern("d MMM yyyy", Locale.getDefault())

fun relativeDateLabel(isoDate: String): String {
    val date = runCatching { LocalDate.parse(isoDate) }.getOrNull() ?: return isoDate
    val today = LocalDate.now()
    return when (date) {
        today -> "Today"
        today.minusDays(1) -> "Yesterday"
        else -> date.format(DISPLAY_DATE_FORMAT)
    }
}
