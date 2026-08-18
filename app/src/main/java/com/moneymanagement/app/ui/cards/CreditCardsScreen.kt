package com.moneymanagement.app.ui.cards

import android.widget.Toast
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
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
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.CreditCard
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.Payment
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CenterAlignedTopAppBar
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExtendedFloatingActionButton
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel

import com.moneymanagement.app.data.Account
import com.moneymanagement.app.data.CreditCard
import com.moneymanagement.app.data.MoneyRepository
import com.moneymanagement.app.settings.AppSettings
import com.moneymanagement.app.ui.common.ConfirmDeleteDialog
import com.moneymanagement.app.ui.common.EditSheet
import com.moneymanagement.app.ui.common.EmptyState
import com.moneymanagement.app.ui.common.GenericViewModelFactory
import com.moneymanagement.app.ui.common.formatMoney
import com.moneymanagement.app.ui.common.soundClick
import com.moneymanagement.app.ui.theme.ExpenseRed
import com.moneymanagement.app.ui.theme.IncomeGreen
import java.time.LocalDate
import java.time.temporal.ChronoUnit

private val CARD_COLORS = listOf(
    "#1E293B" to "Midnight Slate",
    "#1E3A8A" to "Royal Navy",
    "#065F46" to "Emerald Dark",
    "#831843" to "Burgundy Wine",
    "#4C1D95" to "Deep Violet",
    "#78350F" to "Copper Bronze",
)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CreditCardsScreen(
    repository: MoneyRepository,
    onBack: () -> Unit,
) {
    val context = LocalContext.current
    val viewModel: CreditCardsViewModel = viewModel(
        factory = GenericViewModelFactory { CreditCardsViewModel(repository) }
    )

    val cards by viewModel.cards.collectAsState()
    val accounts by viewModel.accounts.collectAsState()

    var showAddDialog by remember { mutableStateOf(false) }
    var editingCard by remember { mutableStateOf<CreditCard?>(null) }
    var deletingCard by remember { mutableStateOf<CreditCard?>(null) }
    var payingCard by remember { mutableStateOf<CreditCard?>(null) }

    Scaffold(
        topBar = {
            CenterAlignedTopAppBar(
                title = { Text("Credit Cards & Alerts") },
                navigationIcon = {
                    IconButton(onClick = soundClick(onBack)) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                    }
                },
            )
        },
        floatingActionButton = {
            ExtendedFloatingActionButton(
                onClick = soundClick { showAddDialog = true },
                icon = { Icon(Icons.Filled.Add, contentDescription = null) },
                text = { Text("Add Card") },
            )
        },
    ) { innerPadding ->
        if (cards.isEmpty()) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(innerPadding)
                    .padding(16.dp),
            ) {
                EmptyState(
                    icon = Icons.Filled.CreditCard,
                    title = "No Credit Cards Tracked",
                    subtitle = "Add your credit cards to receive bill due date alerts, utilization warnings, and automated SMS bill detection.",
                )
            }
        } else {
            LazyColumn(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(innerPadding),
                contentPadding = PaddingValues(16.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp),
            ) {
                items(cards, key = { it.id }) { card ->
                    CreditCardItem(
                        card = card,
                        onEdit = { editingCard = card },
                        onDelete = { deletingCard = card },
                        onPay = { payingCard = card },
                        onTogglePaid = { isPaid ->
                            viewModel.markBillPaid(card.id, isPaid)
                            val msg = if (isPaid) "Marked bill as paid!" else "Marked bill as unpaid."
                            Toast.makeText(context, msg, Toast.LENGTH_SHORT).show()
                        },
                    )
                }
            }
        }
    }

    if (showAddDialog) {
        CreditCardDialog(
            card = null,
            onDismiss = { showAddDialog = false },
            onSave = { cardName, bankName, last4, limit, balance, totalDue, minDue, dueDate, billingDay, colorHex, linkedAccId ->
                viewModel.addCreditCard(cardName, bankName, last4, limit, balance, totalDue, minDue, dueDate, billingDay, colorHex, linkedAccId)
                showAddDialog = false
                Toast.makeText(context, "Credit card added!", Toast.LENGTH_SHORT).show()
            },
        )
    }

    editingCard?.let { card ->
        CreditCardDialog(
            card = card,
            onDismiss = { editingCard = null },
            onSave = { cardName, bankName, last4, limit, balance, totalDue, minDue, dueDate, billingDay, colorHex, linkedAccId ->
                viewModel.updateCreditCard(card.id, cardName, bankName, last4, limit, balance, totalDue, minDue, dueDate, card.isPaid, billingDay, colorHex, linkedAccId)
                editingCard = null
                Toast.makeText(context, "Credit card updated!", Toast.LENGTH_SHORT).show()
            },
        )
    }


    deletingCard?.let { card ->
        ConfirmDeleteDialog(
            message = "Delete '${card.cardName}'? Tracked bill reminders for this card will be removed.",
            onConfirm = {
                viewModel.deleteCard(card.id)
                deletingCard = null
                Toast.makeText(context, "Credit card deleted", Toast.LENGTH_SHORT).show()
            },
            onDismiss = { deletingCard = null },
        )
    }

    payingCard?.let { card ->
        PayBillDialog(
            card = card,
            accounts = accounts,
            onDismiss = { payingCard = null },
            onConfirmPay = { fromAccountId, amount ->
                viewModel.markBillPaid(card.id, true, fromAccountId, amount)
                payingCard = null
                Toast.makeText(context, "Bill payment logged successfully!", Toast.LENGTH_SHORT).show()
            },
        )
    }
}

@Composable
private fun CreditCardItem(
    card: CreditCard,
    onEdit: () -> Unit,
    onDelete: () -> Unit,
    onPay: () -> Unit,
    onTogglePaid: (Boolean) -> Unit,
) {
    val currencySymbol by AppSettings.currencySymbol.collectAsState()
    val cardColor = parseHexColor(card.colorHex)

    val utilizationPct = if (card.creditLimit > 0) (card.currentBalance / card.creditLimit).coerceIn(0.0, 1.0) else 0.0
    val isHighUtilization = utilizationPct >= 0.70
    val isModerateUtilization = utilizationPct >= 0.30

    val daysUntilDue = calculateDaysUntil(card.dueDate)

    Card(

        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(20.dp),
        colors = CardDefaults.cardColors(containerColor = Color.Transparent),
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .background(
                    Brush.verticalGradient(
                        colors = listOf(cardColor, cardColor.copy(alpha = 0.85f)),
                    ),
                    RoundedCornerShape(20.dp),
                )
                .padding(20.dp),
        ) {
            // Top Row: Bank Name, Last 4 digits & Actions
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Column {
                    Text(
                        text = card.bankName.uppercase(),
                        style = MaterialTheme.typography.labelSmall,
                        color = Color.White.copy(alpha = 0.75f),
                        fontWeight = FontWeight.Bold,
                    )
                    Text(
                        text = card.cardName,
                        style = MaterialTheme.typography.titleLarge,
                        fontWeight = FontWeight.Bold,
                        color = Color.White,
                    )
                }

                Row(verticalAlignment = Alignment.CenterVertically) {
                    IconButton(onClick = soundClick(onEdit), modifier = Modifier.size(32.dp)) {
                        Icon(Icons.Filled.Edit, contentDescription = "Edit", tint = Color.White.copy(alpha = 0.85f), modifier = Modifier.size(18.dp))
                    }
                    IconButton(onClick = soundClick(onDelete), modifier = Modifier.size(32.dp)) {
                        Icon(Icons.Filled.Delete, contentDescription = "Delete", tint = Color.White.copy(alpha = 0.85f), modifier = Modifier.size(18.dp))
                    }
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            // Card Number Visual Representation
            Text(
                text = if (!card.lastFourDigits.isNullOrBlank()) "•••• •••• •••• ${card.lastFourDigits}" else "•••• •••• •••• ••••",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.SemiBold,
                color = Color.White.copy(alpha = 0.9f),
                letterSpacing = 2.sp,
            )


            Spacer(modifier = Modifier.height(16.dp))

            // Limit & Utilization Section
            if (card.creditLimit > 0) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                ) {
                    Text(
                        "Used: $currencySymbol ${formatMoney(card.currentBalance, 0)}",
                        style = MaterialTheme.typography.bodySmall,
                        color = Color.White.copy(alpha = 0.85f),
                    )
                    Text(
                        "Limit: $currencySymbol ${formatMoney(card.creditLimit, 0)}",
                        style = MaterialTheme.typography.bodySmall,
                        color = Color.White.copy(alpha = 0.85f),
                    )
                }

                LinearProgressIndicator(
                    progress = { utilizationPct.toFloat() },
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 6.dp)
                        .height(6.dp)
                        .clip(RoundedCornerShape(3.dp)),
                    color = when {
                        isHighUtilization -> ExpenseRed
                        isModerateUtilization -> Color(0xFFF59E0B)
                        else -> IncomeGreen
                    },
                    trackColor = Color.White.copy(alpha = 0.2f),
                )

                if (isHighUtilization) {
                    Row(
                        modifier = Modifier.padding(top = 2.dp),
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        Icon(Icons.Filled.Warning, contentDescription = null, tint = ExpenseRed, modifier = Modifier.size(14.dp))
                        Text(
                            " High Utilization (${(utilizationPct * 100).toInt()}%). Pay down to boost credit score.",
                            style = MaterialTheme.typography.labelSmall,
                            color = Color(0xFFFF8A80),
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(12.dp))

            // Bill & Due Date Section
            Surface(
                shape = RoundedCornerShape(12.dp),
                color = Color.Black.copy(alpha = 0.25f),
                modifier = Modifier.fillMaxWidth(),
            ) {
                Column(modifier = Modifier.padding(12.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        Column {
                            Text("Total Bill Due", style = MaterialTheme.typography.labelSmall, color = Color.White.copy(alpha = 0.7f))
                            Text(
                                "$currencySymbol ${formatMoney(card.totalDue, 2)}",
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.Bold,
                                color = Color.White,
                            )
                            if (card.minDue > 0) {
                                Text(
                                    "Min Due: $currencySymbol ${formatMoney(card.minDue, 2)}",
                                    style = MaterialTheme.typography.labelSmall,
                                    color = Color.White.copy(alpha = 0.75f),
                                )
                            }
                        }

                        Column(horizontalAlignment = Alignment.End) {
                            Text("Due Date", style = MaterialTheme.typography.labelSmall, color = Color.White.copy(alpha = 0.7f))
                            Text(
                                card.dueDate ?: "Not set",
                                style = MaterialTheme.typography.bodyMedium,
                                fontWeight = FontWeight.SemiBold,
                                color = Color.White,
                            )
                            if (card.isPaid) {
                                Surface(
                                    shape = RoundedCornerShape(6.dp),
                                    color = IncomeGreen.copy(alpha = 0.3f),
                                    modifier = Modifier.padding(top = 4.dp),
                                ) {
                                    Text(
                                        "Paid ✅",
                                        style = MaterialTheme.typography.labelSmall,
                                        color = IncomeGreen,
                                        fontWeight = FontWeight.Bold,
                                        modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp),
                                    )
                                }
                            } else if (daysUntilDue != null) {
                                val dueTag = when {
                                    daysUntilDue < 0 -> "Overdue by ${-daysUntilDue}d"
                                    daysUntilDue == 0L -> "Due Today"
                                    daysUntilDue == 1L -> "Due Tomorrow"
                                    else -> "Due in ${daysUntilDue}d"
                                }
                                val tagColor = if (daysUntilDue <= 3) ExpenseRed else Color(0xFFF59E0B)
                                Surface(
                                    shape = RoundedCornerShape(6.dp),
                                    color = tagColor.copy(alpha = 0.3f),
                                    modifier = Modifier.padding(top = 4.dp),
                                ) {
                                    Text(
                                        dueTag,
                                        style = MaterialTheme.typography.labelSmall,
                                        color = if (daysUntilDue <= 3) Color(0xFFFF8A80) else Color(0xFFFDE68A),
                                        fontWeight = FontWeight.Bold,
                                        modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp),
                                    )
                                }
                            }
                        }
                    }

                    if (card.totalDue > 0) {
                        Spacer(modifier = Modifier.height(10.dp))
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(8.dp),
                        ) {
                            if (!card.isPaid) {
                                Button(
                                    onClick = soundClick(onPay),
                                    modifier = Modifier.weight(1f),
                                    colors = ButtonDefaults.buttonColors(
                                        containerColor = Color.White,
                                        contentColor = cardColor,
                                    ),
                                ) {
                                    Icon(Icons.Filled.Payment, contentDescription = null, modifier = Modifier.size(16.dp))
                                    Spacer(modifier = Modifier.width(6.dp))
                                    Text("Pay Bill", fontWeight = FontWeight.Bold)
                                }

                                OutlinedButton(
                                    onClick = soundClick { onTogglePaid(true) },
                                    colors = ButtonDefaults.outlinedButtonColors(contentColor = Color.White),
                                    border = androidx.compose.foundation.BorderStroke(1.dp, Color.White.copy(alpha = 0.5f)),
                                ) {
                                    Icon(Icons.Filled.CheckCircle, contentDescription = null, modifier = Modifier.size(16.dp))
                                }
                            } else {
                                OutlinedButton(
                                    onClick = soundClick { onTogglePaid(false) },
                                    modifier = Modifier.fillMaxWidth(),
                                    colors = ButtonDefaults.outlinedButtonColors(contentColor = Color.White.copy(alpha = 0.8f)),
                                    border = androidx.compose.foundation.BorderStroke(1.dp, Color.White.copy(alpha = 0.3f)),
                                ) {
                                    Text("Mark as Unpaid")
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun PayBillDialog(
    card: CreditCard,
    accounts: List<Account>,
    onDismiss: () -> Unit,
    onConfirmPay: (fromAccountId: Long?, amount: Double) -> Unit,
) {
    var selectedAccountId by remember { mutableStateOf(accounts.firstOrNull()?.id) }
    var payAmountText by remember { mutableStateOf(card.totalDue.toString()) }

    AlertDialog(

        onDismissRequest = onDismiss,
        title = { Text("Pay ${card.cardName} Bill") },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                Text(
                    "Total Due: ₹${formatMoney(card.totalDue, 2)}",
                    style = MaterialTheme.typography.bodyMedium,
                    fontWeight = FontWeight.SemiBold,
                )

                OutlinedTextField(
                    value = payAmountText,
                    onValueChange = { payAmountText = it },
                    label = { Text("Payment Amount") },
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = true,
                )

                if (accounts.isNotEmpty()) {
                    Text("Pay from Account:", style = MaterialTheme.typography.labelSmall)
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                    ) {
                        accounts.forEach { acc ->
                            FilterChip(
                                selected = selectedAccountId == acc.id,
                                onClick = soundClick { selectedAccountId = acc.id },
                                label = { Text(acc.name, style = MaterialTheme.typography.labelSmall) },
                            )
                        }
                    }
                }
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    val amt = payAmountText.toDoubleOrNull() ?: card.totalDue
                    onConfirmPay(selectedAccountId, amt)
                }
            ) {
                Text("Confirm Payment")
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
private fun CreditCardDialog(
    card: CreditCard?,
    onDismiss: () -> Unit,
    onSave: (

        cardName: String,
        bankName: String,
        last4: String?,
        limit: Double,
        balance: Double,
        totalDue: Double,
        minDue: Double,
        dueDate: String?,
        billingDay: Int?,
        colorHex: String,
        linkedAccountId: Long?,
    ) -> Unit,
) {
    var cardName by remember { mutableStateOf(card?.cardName ?: "") }
    var bankName by remember { mutableStateOf(card?.bankName ?: "") }
    var last4 by remember { mutableStateOf(card?.lastFourDigits ?: "") }
    var limitText by remember { mutableStateOf(card?.creditLimit?.takeIf { it > 0 }?.toString() ?: "") }
    var balanceText by remember { mutableStateOf(card?.currentBalance?.takeIf { it > 0 }?.toString() ?: "") }
    var totalDueText by remember { mutableStateOf(card?.totalDue?.takeIf { it > 0 }?.toString() ?: "") }
    var minDueText by remember { mutableStateOf(card?.minDue?.takeIf { it > 0 }?.toString() ?: "") }
    var dueDate by remember { mutableStateOf(card?.dueDate ?: "") }
    var selectedColor by remember { mutableStateOf(card?.colorHex ?: CARD_COLORS.first().first) }

    EditSheet(onDismiss = onDismiss) {
        Text(if (card == null) "Add Credit Card" else "Edit Credit Card", style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold)

        OutlinedTextField(
            value = cardName,
            onValueChange = { cardName = it },
            label = { Text("Card Name (e.g. HDFC Regalia)") },
            modifier = Modifier.fillMaxWidth(),
            singleLine = true,
        )

        OutlinedTextField(
            value = bankName,
            onValueChange = { bankName = it },
            label = { Text("Bank Name (e.g. HDFC Bank)") },
            modifier = Modifier.fillMaxWidth(),
            singleLine = true,
        )

        OutlinedTextField(
            value = last4,
            onValueChange = { if (it.length <= 4) last4 = it },
            label = { Text("Last 4 Digits (for SMS auto-matching)") },
            modifier = Modifier.fillMaxWidth(),
            singleLine = true,
        )

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            OutlinedTextField(
                value = limitText,
                onValueChange = { limitText = it },
                label = { Text("Credit Limit") },
                modifier = Modifier.weight(1f),
                singleLine = true,
            )
            OutlinedTextField(
                value = balanceText,
                onValueChange = { balanceText = it },
                label = { Text("Current Used") },
                modifier = Modifier.weight(1f),
                singleLine = true,
            )
        }

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            OutlinedTextField(
                value = totalDueText,
                onValueChange = { totalDueText = it },
                label = { Text("Total Due") },
                modifier = Modifier.weight(1f),
                singleLine = true,
            )
            OutlinedTextField(
                value = minDueText,
                onValueChange = { minDueText = it },
                label = { Text("Min Due") },
                modifier = Modifier.weight(1f),
                singleLine = true,
            )
        }

        OutlinedTextField(
            value = dueDate,
            onValueChange = { dueDate = it },
            label = { Text("Due Date (YYYY-MM-DD)") },
            placeholder = { Text("e.g. 2026-08-25") },
            modifier = Modifier.fillMaxWidth(),
            singleLine = true,
        )

        Text("Card Color", style = MaterialTheme.typography.titleSmall, color = MaterialTheme.colorScheme.primary)
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            CARD_COLORS.forEach { (hex, _) ->
                val col = parseHexColor(hex)
                Box(
                    modifier = Modifier
                        .size(36.dp)
                        .clip(CircleShape)
                        .background(col)
                        .clickable(onClick = soundClick { selectedColor = hex })
                        .padding(2.dp),
                    contentAlignment = Alignment.Center,
                ) {
                    if (selectedColor == hex) {
                        Icon(Icons.Filled.CheckCircle, contentDescription = null, tint = Color.White, modifier = Modifier.size(20.dp))
                    }
                }
            }
        }

        Button(
            onClick = soundClick {
                if (cardName.isBlank()) return@soundClick
                onSave(
                    cardName,
                    if (bankName.isBlank()) "Bank" else bankName,
                    last4.takeIf { it.isNotBlank() },
                    limitText.toDoubleOrNull() ?: 0.0,
                    balanceText.toDoubleOrNull() ?: 0.0,
                    totalDueText.toDoubleOrNull() ?: 0.0,
                    minDueText.toDoubleOrNull() ?: 0.0,
                    dueDate.takeIf { it.isNotBlank() },
                    null,
                    selectedColor,
                    null,
                )
            },
            enabled = cardName.isNotBlank(),
            modifier = Modifier.fillMaxWidth().padding(top = 12.dp),
        ) {
            Text(if (card == null) "Add Credit Card" else "Save Changes")
        }
    }
}

private fun calculateDaysUntil(dueDateStr: String?): Long? {
    if (dueDateStr.isNullOrBlank()) return null
    val target = runCatching { LocalDate.parse(dueDateStr) }.getOrNull() ?: return null
    return ChronoUnit.DAYS.between(LocalDate.now(), target)
}

private fun parseHexColor(hex: String): Color {
    return try {
        Color(android.graphics.Color.parseColor(hex))
    } catch (_: Exception) {
        Color(0xFF1E293B)
    }
}
