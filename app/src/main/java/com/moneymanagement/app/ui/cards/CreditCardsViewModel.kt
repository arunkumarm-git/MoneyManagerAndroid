package com.moneymanagement.app.ui.cards

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.moneymanagement.app.data.Account
import com.moneymanagement.app.data.CreditCard
import com.moneymanagement.app.data.MoneyRepository
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

class CreditCardsViewModel(private val repository: MoneyRepository) : ViewModel() {

    val cards: StateFlow<List<CreditCard>> = repository.creditCards
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val accounts: StateFlow<List<Account>> = repository.accounts
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val upcomingBills: StateFlow<List<CreditCard>> = repository.upcomingCreditCardBills
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    fun addCreditCard(
        cardName: String,
        bankName: String,
        lastFourDigits: String?,
        creditLimit: Double,
        currentBalance: Double,
        totalDue: Double,
        minDue: Double,
        dueDate: String?,
        billingCycleDay: Int?,
        colorHex: String,
        linkedAccountId: Long?,
    ) {
        viewModelScope.launch {
            repository.addCreditCard(
                cardName = cardName,
                bankName = bankName,
                lastFourDigits = lastFourDigits,
                creditLimit = creditLimit,
                currentBalance = currentBalance,
                totalDue = totalDue,
                minDue = minDue,
                dueDate = dueDate,
                billingCycleDay = billingCycleDay,
                colorHex = colorHex,
                linkedAccountId = linkedAccountId,
            )
        }
    }

    fun updateCreditCard(
        id: Long,
        cardName: String,
        bankName: String,
        lastFourDigits: String?,
        creditLimit: Double,
        currentBalance: Double,
        totalDue: Double,
        minDue: Double,
        dueDate: String?,
        isPaid: Boolean,
        billingCycleDay: Int?,
        colorHex: String,
        linkedAccountId: Long?,
    ) {
        viewModelScope.launch {
            repository.updateCreditCard(
                id = id,
                cardName = cardName,
                bankName = bankName,
                lastFourDigits = lastFourDigits,
                creditLimit = creditLimit,
                currentBalance = currentBalance,
                totalDue = totalDue,
                minDue = minDue,
                dueDate = dueDate,
                isPaid = isPaid,
                billingCycleDay = billingCycleDay,
                colorHex = colorHex,
                linkedAccountId = linkedAccountId,
            )
        }
    }

    fun markBillPaid(
        cardId: Long,
        isPaid: Boolean,
        payFromAccountId: Long? = null,
        amount: Double? = null,
    ) {
        viewModelScope.launch {
            repository.markCreditCardBillPaid(cardId, isPaid, payFromAccountId, amount)
        }
    }

    fun deleteCard(cardId: Long) {
        viewModelScope.launch {
            repository.deleteCreditCard(cardId)
        }
    }
}
