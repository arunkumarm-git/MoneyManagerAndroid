package com.moneymanagement.app.ui.accounts

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.moneymanagement.app.data.Account
import com.moneymanagement.app.data.Calculations
import com.moneymanagement.app.data.MoneyRepository
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

data class AccountsUiState(
    val accounts: List<Account> = emptyList(),
    val balances: Map<Long, Double> = emptyMap(),
)

class AccountsViewModel(private val repository: MoneyRepository) : ViewModel() {
    val uiState: StateFlow<AccountsUiState> = combine(
        repository.accounts,
        repository.transactions,
    ) { accounts, txns ->
        AccountsUiState(
            accounts = accounts,
            balances = accounts.associate { it.id to Calculations.accountBalance(it, txns) },
        )
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), AccountsUiState())

    fun addAccount(name: String, type: String, initialBalance: Double) {
        viewModelScope.launch { repository.addAccount(name, type, initialBalance) }
    }

    fun updateAccount(id: Long, name: String, type: String, initialBalance: Double) {
        viewModelScope.launch { repository.updateAccount(id, name, type, initialBalance) }
    }

    fun deleteAccount(id: Long) {
        viewModelScope.launch { repository.deleteAccount(id) }
    }
}
