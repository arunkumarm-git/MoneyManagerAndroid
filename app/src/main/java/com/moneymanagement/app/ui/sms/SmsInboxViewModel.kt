package com.moneymanagement.app.ui.sms

import android.content.Context
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.moneymanagement.app.data.Account
import com.moneymanagement.app.data.Category
import com.moneymanagement.app.data.CreditCard
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
    val selectedScanDays: Int = 30,
)

class SmsInboxViewModel(private val repository: MoneyRepository) : ViewModel() {

    private val _isScanning = MutableStateFlow(false)
    val isScanning: StateFlow<Boolean> = _isScanning.asStateFlow()

    private val _selectedScanDays = MutableStateFlow(30)
    val selectedScanDays: StateFlow<Int> = _selectedScanDays.asStateFlow()

    private val _scanMessage = MutableStateFlow<String?>(null)
    val scanMessage: StateFlow<String?> = _scanMessage.asStateFlow()

    val pendingSms: StateFlow<List<SmsInbox>> = repository.pendingSms
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val accounts: StateFlow<List<Account>> = repository.accounts
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val categories: StateFlow<List<Category>> = repository.categories
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val creditCards: StateFlow<List<CreditCard>> = repository.creditCards
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    fun setScanDays(days: Int) {
        _selectedScanDays.value = days
    }

    fun approveSms(
        sms: SmsInbox,
        accountId: Long?,
        categoryId: Long?,
        toAccountId: Long? = null,
        note: String? = null,
        creditCardId: Long? = null,
        overrideType: String? = null,
    ) {
        viewModelScope.launch {
            repository.approveSms(
                smsId = sms.id,
                accountId = accountId,
                categoryId = categoryId,
                toAccountId = toAccountId,
                type = overrideType ?: sms.type,
                amount = sms.amount,
                txnDate = sms.txnDate,
                note = note ?: sms.merchantOrNote ?: "SMS Transaction",
                creditCardId = creditCardId,
            )
        }
    }

    fun approveAll(defaultAccountId: Long) {
        viewModelScope.launch {
            val list = pendingSms.value
            val bankAccount = accounts.value.find { acc ->
                acc.type.equals("bank", ignoreCase = true) || acc.name.contains("bank", ignoreCase = true)
            }
            val fallbackAccId = bankAccount?.id ?: defaultAccountId

            for (sms in list) {
                val matchedCat = categories.value.find {
                    it.name.equals(sms.suggestedCategory, ignoreCase = true) &&
                    it.type.equals(sms.type, ignoreCase = true)
                } ?: categories.value.find {
                    (it.name.equals("Other", ignoreCase = true) || it.name.equals("Others", ignoreCase = true) || it.name.equals("Other Income", ignoreCase = true)) &&
                    it.type.equals(sms.type, ignoreCase = true)
                }
                repository.approveSms(
                    smsId = sms.id,
                    accountId = sms.suggestedAccountId ?: fallbackAccId,
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

    fun scanPastSms(context: Context, days: Int = _selectedScanDays.value) {
        viewModelScope.launch {
            _isScanning.value = true
            _scanMessage.value = null
            try {
                val stats = SmsSyncManager.scanHistoricalSmsDetailed(context, repository, days)
                val totalFound = stats.transactionsFound + stats.billsFound
                _scanMessage.value = if (totalFound > 0) {
                    "Scanned ${stats.messagesScanned} SMS (${days}d). Found $totalFound new item${if (totalFound > 1) "s" else ""}!"
                } else if (stats.messagesScanned > 0) {
                    "Scanned ${stats.messagesScanned} SMS (${days}d). No new transactions found."
                } else {
                    "No SMS found for the past $days days."
                }
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

