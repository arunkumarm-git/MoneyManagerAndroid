package com.moneymanagement.app.ui.transactions

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.moneymanagement.app.data.Account
import com.moneymanagement.app.data.Category
import com.moneymanagement.app.data.MoneyRepository
import com.moneymanagement.app.data.TransactionRow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

val TRANSACTION_FILTERS = listOf("All", "Income", "Expense", "Transfer")

data class TransactionsUiState(
    val filter: String = "All",
    val transactions: List<TransactionRow> = emptyList(),
    val accounts: List<Account> = emptyList(),
    val categories: List<Category> = emptyList(),
)

class TransactionsViewModel(private val repository: MoneyRepository) : ViewModel() {
    private val filter = MutableStateFlow("All")

    val uiState: StateFlow<TransactionsUiState> = combine(
        filter,
        repository.transactions,
        repository.accounts,
        repository.categories,
    ) { f, txns, accounts, categories ->
        val typeFilter = if (f == "All") null else f.lowercase()
        val filtered = if (typeFilter == null) txns else txns.filter { it.txn.type == typeFilter }
        TransactionsUiState(f, filtered, accounts, categories)
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), TransactionsUiState())

    fun setFilter(name: String) {
        filter.value = name
    }

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

    fun updateTransaction(
        id: Long,
        accountId: Long,
        type: String,
        amount: Double,
        txnDate: String,
        categoryId: Long?,
        toAccountId: Long?,
        note: String,
    ) {
        viewModelScope.launch {
            repository.updateTransaction(id, accountId, type, amount, txnDate, categoryId, toAccountId, note)
        }
    }

    fun deleteTransaction(id: Long) {
        viewModelScope.launch { repository.deleteTransaction(id) }
    }
}
