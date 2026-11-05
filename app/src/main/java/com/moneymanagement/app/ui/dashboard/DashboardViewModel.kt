package com.moneymanagement.app.ui.dashboard

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.moneymanagement.app.data.Account
import com.moneymanagement.app.data.BudgetStatusRow
import com.moneymanagement.app.data.Calculations
import com.moneymanagement.app.data.Category
import com.moneymanagement.app.data.CreditCard
import com.moneymanagement.app.data.EmergencyFundAnalysis
import com.moneymanagement.app.data.MoneyRepository
import com.moneymanagement.app.data.TransactionRow
import com.moneymanagement.app.settings.AppSettings
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
    val monthNetSavings: Double = 0.0,
    val isMonthDeficit: Boolean = false,
    val budgetAlerts: List<BudgetStatusRow> = emptyList(),
    val recentTransactions: List<TransactionRow> = emptyList(),
    val accounts: List<Account> = emptyList(),
    val categories: List<Category> = emptyList(),
    val creditCards: List<CreditCard> = emptyList(),
    val emergencyAnalysis: EmergencyFundAnalysis? = null,
    val emergencyFundsCount: Int = 0,
)

class DashboardViewModel(private val repository: MoneyRepository) : ViewModel() {
    val uiState: StateFlow<DashboardUiState> = combine(
        repository.accounts,
        repository.categories,
        repository.transactions,
        repository.creditCards,
        repository.emergencyFunds,
        AppSettings.emergencyTargetMonths,
        AppSettings.emergencyCustomTarget,
        AppSettings.emergencyExpenseOverride,
    ) { args: Array<Any> ->
        @Suppress("UNCHECKED_CAST")
        val accounts = args[0] as List<Account>
        @Suppress("UNCHECKED_CAST")
        val categories = args[1] as List<Category>
        @Suppress("UNCHECKED_CAST")
        val txns = args[2] as List<TransactionRow>
        @Suppress("UNCHECKED_CAST")
        val cards = args[3] as List<CreditCard>
        @Suppress("UNCHECKED_CAST")
        val emergencyFunds = args[4] as List<com.moneymanagement.app.data.EmergencyFundAllocation>
        val targetMonths = args[5] as Int
        val customTarget = args[6] as Double
        val expenseOverride = args[7] as Double

        val today = LocalDate.now()
        val total = Calculations.totalBalance(accounts, txns)
        val (income, expense) = Calculations.monthSummary(txns, today.year, today.monthValue)
        val netSavings = income - expense
        val budgetStatus = Calculations.budgetStatus(categories, txns, today.year, today.monthValue)
        val alerts = budgetStatus.filter { it.budget != null && it.budget > 0 && it.spent / it.budget >= 0.8 }

        val efAnalysis = Calculations.calculateEmergencyFundMetrics(
            allocations = emergencyFunds,
            txns = txns,
            targetMonths = targetMonths,
            customTarget = customTarget,
            expenseOverride = expenseOverride,
        )

        DashboardUiState(
            totalBalance = total,
            monthIncome = income,
            monthExpense = expense,
            monthNetSavings = netSavings,
            isMonthDeficit = expense > income,
            budgetAlerts = alerts,
            recentTransactions = txns.filter { it.txn.type != "transfer" }.take(8),
            accounts = accounts,
            categories = categories,
            creditCards = cards,
            emergencyAnalysis = efAnalysis,
            emergencyFundsCount = emergencyFunds.size,
        )
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), DashboardUiState())

    fun addTransaction(
        accountId: Long?,
        type: String,
        amount: Double,
        txnDate: String,
        categoryId: Long?,
        toAccountId: Long?,
        creditCardId: Long? = null,
        note: String,
    ) {
        viewModelScope.launch {
            repository.addTransaction(accountId, type, amount, txnDate, categoryId, toAccountId, creditCardId, note)
        }
    }
}
