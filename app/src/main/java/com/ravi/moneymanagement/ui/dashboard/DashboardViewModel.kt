package com.ravi.moneymanagement.ui.dashboard

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.ravi.moneymanagement.data.Account
import com.ravi.moneymanagement.data.BudgetStatusRow
import com.ravi.moneymanagement.data.Calculations
import com.ravi.moneymanagement.data.Category
import com.ravi.moneymanagement.data.MoneyRepository
import com.ravi.moneymanagement.data.TransactionRow
import java.time.LocalDate
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

data class DashboardUiState(
    val totalBalance: Double = 0.0,
    val monthIncome: Double = 0.0,
    val monthExpense: Double = 0.0,
    val budgetAlerts: List<BudgetStatusRow> = emptyList(),
    val recentTransactions: List<TransactionRow> = emptyList(),
    val accounts: List<Account> = emptyList(),
    val categories: List<Category> = emptyList(),
)

class DashboardViewModel(private val repository: MoneyRepository) : ViewModel() {
    val uiState: StateFlow<DashboardUiState> = combine(
        repository.accounts,
        repository.categories,
        repository.transactions,
    ) { accounts, categories, txns ->
        val today = LocalDate.now()
        val total = Calculations.totalBalance(accounts, txns)
        val (income, expense) = Calculations.monthSummary(txns, today.year, today.monthValue)
        val budgetStatus = Calculations.budgetStatus(categories, txns, today.year, today.monthValue)
        val alerts = budgetStatus.filter { it.budget != null && it.budget > 0 && it.spent / it.budget >= 0.8 }
        DashboardUiState(
            totalBalance = total,
            monthIncome = income,
            monthExpense = expense,
            budgetAlerts = alerts,
            recentTransactions = txns.take(8),
            accounts = accounts,
            categories = categories,
        )
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), DashboardUiState())

    fun addTransaction(
        accountId: Long,
        type: String,
        amount: Double,
        txnDate: String,
        categoryId: Long?,
        toAccountId: Long?,
        note: String,
    ) {
        viewModelScope.launch {
            repository.addTransaction(accountId, type, amount, txnDate, categoryId, toAccountId, note)
        }
    }
}
