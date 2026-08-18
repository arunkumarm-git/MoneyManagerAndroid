package com.moneymanagement.app.ui.sms

import android.content.Context
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.moneymanagement.app.data.Account
import com.moneymanagement.app.data.Category
import com.moneymanagement.app.data.MoneyRepository
import com.moneymanagement.app.data.SmsInbox
import com.moneymanagement.app.sms.SmsSyncManager
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

data class SmsInboxUiState(
    val pendingList: List<SmsInbox> = emptyList(),
    val accounts: List<Account> = emptyList(),
    val categories: List<Category> = emptyList(),
    val isScanning: Boolean = false,
    val scanResultCount: Int? = null,
)

class SmsInboxViewModel(private val repository: MoneyRepository) : ViewModel() {

    private val _isScanning = MutableStateFlow(false)
    val isScanning: StateFlow<Boolean> = _isScanning.asStateFlow()

    private val _scanMessage = MutableStateFlow<String?>(null)
    val scanMessage: StateFlow<String?> = _scanMessage.asStateFlow()

    val pendingSms: StateFlow<List<SmsInbox>> = repository.pendingSms
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val accounts: StateFlow<List<Account>> = repository.accounts
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val categories: StateFlow<List<Category>> = repository.categories
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    fun approveSms(
        sms: SmsInbox,
        accountId: Long,
        categoryId: Long?,
        toAccountId: Long? = null,
        note: String? = null,
    ) {
        viewModelScope.launch {
            repository.approveSms(
                smsId = sms.id,
                accountId = accountId,
                categoryId = categoryId,
                toAccountId = toAccountId,
                type = sms.type,
                amount = sms.amount,
                txnDate = sms.txnDate,
                note = note ?: sms.merchantOrNote ?: "SMS Transaction",
            )
        }
    }

    fun approveAll(defaultAccountId: Long) {
        viewModelScope.launch {
            val list = pendingSms.value
            for (sms in list) {
                val matchedCat = categories.value.find {

                    it.name.equals(sms.suggestedCategory, ignoreCase = true) &&
                    it.type.equals(sms.type, ignoreCase = true)
                }
                repository.approveSms(
                    smsId = sms.id,
                    accountId = sms.suggestedAccountId ?: defaultAccountId,
                    categoryId = matchedCat?.id,
                    toAccountId = null,
                    type = sms.type,
                    amount = sms.amount,
                    txnDate = sms.txnDate,
                    note = sms.merchantOrNote ?: "SMS Transaction",
                )
            }
        }
    }

    fun dismissSms(smsId: Long) {
        viewModelScope.launch {
            repository.dismissSms(smsId)
        }
    }

    fun scanPastSms(context: Context, days: Int = 30) {
        viewModelScope.launch {
            _isScanning.value = true
            _scanMessage.value = null
            try {
                val count = SmsSyncManager.scanHistoricalSms(context, repository, days)
                _scanMessage.value = if (count > 0) "Found $count financial transactions!" else "No new transactions found in SMS."
            } catch (e: Exception) {
                _scanMessage.value = "Scan error: ${e.localizedMessage}"
            } finally {
                _isScanning.value = false
            }
        }
    }

    fun clearScanMessage() {
        _scanMessage.value = null
    }
}
