package com.ravi.moneymanagement.ui.reports

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.ravi.moneymanagement.data.BudgetStatusRow
import com.ravi.moneymanagement.data.Calculations
import com.ravi.moneymanagement.data.CategoryTotal
import com.ravi.moneymanagement.data.MonthPoint
import com.ravi.moneymanagement.data.MoneyRepository
import java.time.LocalDate
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn

data class ReportsUiState(
    val viewYear: Int,
    val viewMonth: Int,
    val trend: List<MonthPoint> = emptyList(),
    val breakdown: List<CategoryTotal> = emptyList(),
    val budgetStatus: List<BudgetStatusRow> = emptyList(),
    val allTransactions: List<com.ravi.moneymanagement.data.TransactionRow> = emptyList(),
)

class ReportsViewModel(private val repository: MoneyRepository) : ViewModel() {
    private val today = LocalDate.now()
    private val viewYear = MutableStateFlow(today.year)
    private val viewMonth = MutableStateFlow(today.monthValue)

    val uiState: StateFlow<ReportsUiState> = combine(
        viewYear,
        viewMonth,
        repository.categories,
        repository.transactions,
    ) { y, m, categories, txns ->
        ReportsUiState(
            viewYear = y,
            viewMonth = m,
            trend = Calculations.monthlyTrend(txns, 6, y, m),
            breakdown = Calculations.categoryBreakdown(txns, categories, y, m, "expense"),
            budgetStatus = Calculations.budgetStatus(categories, txns, y, m),
            allTransactions = txns,
        )
    }.stateIn(
        viewModelScope,
        SharingStarted.WhileSubscribed(5000),
        ReportsUiState(today.year, today.monthValue),
    )

    fun changeMonth(delta: Int) {
        var m = viewMonth.value + delta
        var y = viewYear.value
        when (m) {
            0 -> { m = 12; y -= 1 }
            13 -> { m = 1; y += 1 }
        }
        viewYear.value = y
        viewMonth.value = m
    }
}
