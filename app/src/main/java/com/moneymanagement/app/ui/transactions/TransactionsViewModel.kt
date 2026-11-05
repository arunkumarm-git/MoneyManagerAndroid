package com.moneymanagement.app.ui.transactions

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.moneymanagement.app.data.Account
import com.moneymanagement.app.data.Category
import com.moneymanagement.app.data.CreditCard
import com.moneymanagement.app.data.MoneyRepository
import com.moneymanagement.app.data.TransactionRow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

// Transfer removed from user-facing filter list
val TRANSACTION_FILTERS = listOf("All", "Income", "Expense")

data class TransactionsUiState(
    val filter: String = "All",
    val transactions: List<TransactionRow> = emptyList(),
    val accounts: List<Account> = emptyList(),
    val categories: List<Category> = emptyList(),
    val creditCards: List<CreditCard> = emptyList(),
)

class TransactionsViewModel(private val repository: MoneyRepository) : ViewModel() {
    private val filter = MutableStateFlow("All")

    val uiState: StateFlow<TransactionsUiState> = combine(
        filter,
        repository.transactions,
        repository.accounts,
        repository.categories,
        repository.creditCards,
    ) { f, txns, accounts, categories, cards ->
        val typeFilter = if (f == "All") null else f.lowercase()
        // Hide legacy transfer transactions from the main list
        val nonTransfer = txns.filter { it.txn.type != "transfer" }
        val filtered = if (typeFilter == null) nonTransfer else nonTransfer.filter { it.txn.type == typeFilter }
        TransactionsUiState(f, filtered, accounts, categories, cards)
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), TransactionsUiState())

    fun setFilter(name: String) {
        filter.value = name
    }

    fun addTransaction(
        accountId: Long?,
        type: String,
        amount: Double,
        txnDate: String,
        categoryId: Long?,
        toAccountId: Long?,
        creditCardId: Long?,
        note: String,
    ) {
        viewModelScope.launch {
            repository.addTransaction(accountId, type, amount, txnDate, categoryId, toAccountId, creditCardId, note)
        }
    }

    fun updateTransaction(
        id: Long,
        accountId: Long?,
        type: String,
        amount: Double,
        txnDate: String,
        categoryId: Long?,
        toAccountId: Long?,
        creditCardId: Long?,
        note: String,
    ) {
        viewModelScope.launch {
            repository.updateTransaction(id, accountId, type, amount, txnDate, categoryId, toAccountId, creditCardId, note)
        }
    }

    fun deleteTransaction(id: Long) {
        viewModelScope.launch { repository.deleteTransaction(id) }
    }
}
