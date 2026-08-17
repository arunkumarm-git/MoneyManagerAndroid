package com.ravi.moneymanagement.ui.reports

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ChevronLeft
import androidx.compose.material.icons.filled.ChevronRight
import androidx.compose.material.icons.filled.PieChart
import androidx.compose.material.icons.filled.ReceiptLong
import androidx.compose.material3.Card
import androidx.compose.material3.CenterAlignedTopAppBar
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.ravi.moneymanagement.data.MoneyRepository
import com.ravi.moneymanagement.settings.AppSettings
import com.ravi.moneymanagement.ui.common.EditSheet
import com.ravi.moneymanagement.ui.common.EmptyState
import com.ravi.moneymanagement.ui.common.GenericViewModelFactory
import com.ravi.moneymanagement.ui.common.TransactionTypeAvatar
import com.ravi.moneymanagement.ui.common.amountColor
import com.ravi.moneymanagement.ui.common.formatMoney
import com.ravi.moneymanagement.ui.common.relativeDateLabel
import com.ravi.moneymanagement.ui.common.signedAmountText
import com.ravi.moneymanagement.ui.common.soundClick
import com.ravi.moneymanagement.ui.theme.ExpenseRed
import com.ravi.moneymanagement.ui.theme.IncomeGreen
import java.time.LocalDate
import java.time.Month
import java.time.format.TextStyle
import java.util.Locale

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ReportsScreen(repository: MoneyRepository) {
    val viewModel: ReportsViewModel = viewModel(factory = GenericViewModelFactory { ReportsViewModel(repository) })
    val state by viewModel.uiState.collectAsState()
    val currencySymbol by AppSettings.currencySymbol.collectAsState()

    var drilldownCategory by remember { mutableStateOf<String?>(null) }

    Scaffold(
        topBar = { CenterAlignedTopAppBar(title = { Text("Reports & Analytics") }) },
    ) { innerPadding ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp),
        ) {
            item {
                Card(modifier = Modifier.fillMaxWidth()) {
                    Row(
                        modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp),
                        horizontalArrangement = Arrangement.Center,
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        IconButton(onClick = soundClick { viewModel.changeMonth(-1) }) {
                            Icon(Icons.Filled.ChevronLeft, contentDescription = "Previous month")
                        }
                        Text(
                            "${monthName(state.viewMonth)} ${state.viewYear}",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.SemiBold,
                        )
                        IconButton(onClick = soundClick { viewModel.changeMonth(1) }) {
                            Icon(Icons.Filled.ChevronRight, contentDescription = "Next month")
                        }
                    }
                }
            }

            item {
                Card(modifier = Modifier.fillMaxWidth()) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        Text("Income vs Expense (6 months)", style = MaterialTheme.typography.titleMedium)
                        BarChart(state.trend, modifier = Modifier.fillMaxWidth().padding(top = 8.dp))
                        Row(
                            horizontalArrangement = Arrangement.spacedBy(16.dp),
                            modifier = Modifier.padding(top = 8.dp),
                        ) {
                            Text("■ Income", color = IncomeGreen, style = MaterialTheme.typography.labelMedium)
                            Text("■ Expense", color = ExpenseRed, style = MaterialTheme.typography.labelMedium)
                        }
                    }
                }
            }

            item {
                Card(modifier = Modifier.fillMaxWidth()) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically,
                        ) {
                            Text("Expense Breakdown", style = MaterialTheme.typography.titleMedium)
                            Text(
                                "Tap category for details",
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                            )
                        }

                        if (state.breakdown.isEmpty()) {
                            EmptyState(
                                icon = Icons.Filled.PieChart,
                                title = "No expenses recorded",
                                subtitle = "Expenses for ${monthName(state.viewMonth)} will show up here.",
                            )
                        } else {
                            val total = state.breakdown.sumOf { it.total }
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .height(180.dp)
                                    .padding(top = 8.dp),
                                horizontalArrangement = Arrangement.spacedBy(12.dp),
                            ) {
                                DonutChart(
                                    data = state.breakdown,
                                    modifier = Modifier.weight(0.55f).fillMaxSize(),
                                    centerLabel = "$currencySymbol${formatMoney(total, 0)}",
                                    centerSubLabel = "Total",
                                )
                                ChartLegend(
                                    entries = state.breakdown.map {
                                        it.name to "$currencySymbol${formatMoney(it.total, 0)} (${(it.total / total * 100).let { p -> "%.0f".format(p) }}%)"
                                    },
                                    modifier = Modifier.weight(0.45f),
                                    onItemClick = { categoryName ->
                                        drilldownCategory = categoryName
                                    },
                                )
                            }
                        }
                    }
                }
            }

            item {
                Card(modifier = Modifier.fillMaxWidth()) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        Text("Budget vs Actual", style = MaterialTheme.typography.titleMedium)
                        val budgeted = state.budgetStatus.filter { it.budget != null }
                        if (budgeted.isEmpty()) {
                            EmptyState(
                                icon = Icons.Filled.PieChart,
                                title = "No budgets set",
                                subtitle = "Add a monthly budget from the Categories tab.",
                            )
                        } else {
                            Column(modifier = Modifier.padding(top = 8.dp)) {
                                budgeted.forEach { s ->
                                    val budget = s.budget!!
                                    val over = s.spent > budget
                                    Row(
                                        modifier = Modifier.fillMaxWidth().padding(vertical = 6.dp),
                                        horizontalArrangement = Arrangement.SpaceBetween,
                                    ) {
                                        Text(s.name, style = MaterialTheme.typography.bodyMedium)
                                        Text(
                                            "$currencySymbol${formatMoney(s.spent, 0)} / $currencySymbol${formatMoney(budget, 0)}",
                                            color = if (over) ExpenseRed else IncomeGreen,
                                            fontWeight = FontWeight.Medium,
                                        )
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }
    }

    // Drilldown Bottom Sheet
    drilldownCategory?.let { categoryName ->
        val filteredTxns = state.allTransactions.filter { row ->
            val date = runCatching { LocalDate.parse(row.txn.txnDate) }.getOrNull()
            date != null &&
                date.year == state.viewYear &&
                date.monthValue == state.viewMonth &&
                row.categoryName.equals(categoryName, ignoreCase = true)
        }

        EditSheet(onDismiss = { drilldownCategory = null }) {
            Text(
                text = "$categoryName Expenses (${monthName(state.viewMonth)} ${state.viewYear})",
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.Bold,
            )
            Spacer(modifier = Modifier.height(8.dp))

            if (filteredTxns.isEmpty()) {
                EmptyState(
                    icon = Icons.Filled.ReceiptLong,
                    title = "No transactions found",
                    subtitle = "No expense entries found for $categoryName this month.",
                )
            } else {
                LazyColumn(
                    modifier = Modifier.fillMaxWidth().height(320.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp),
                ) {
                    items(filteredTxns, key = { it.txn.id }) { row ->
                        Card(modifier = Modifier.fillMaxWidth()) {
                            Row(
                                modifier = Modifier.fillMaxWidth().padding(12.dp),
                                verticalAlignment = Alignment.CenterVertically,
                            ) {
                                TransactionTypeAvatar(row.txn.type)
                                Column(modifier = Modifier.padding(start = 12.dp).weight(1f)) {
                                    Text(
                                        relativeDateLabel(row.txn.txnDate) + " · " + row.accountName,
                                        style = MaterialTheme.typography.bodyMedium,
                                        fontWeight = FontWeight.Medium,
                                    )
                                    val note = row.txn.note ?: ""
                                    if (note.isNotBlank()) {
                                        Text(
                                            note,
                                            style = MaterialTheme.typography.bodySmall,
                                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                                        )
                                    }
                                }
                                Text(
                                    signedAmountText(row.txn.type, row.txn.amount),
                                    color = amountColor(row.txn.type),
                                    fontWeight = FontWeight.Bold,
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}

private fun monthName(month: Int): String =
    Month.of(month).getDisplayName(TextStyle.FULL, Locale.getDefault())
