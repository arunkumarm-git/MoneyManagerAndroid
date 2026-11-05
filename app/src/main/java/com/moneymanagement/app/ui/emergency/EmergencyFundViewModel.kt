package com.moneymanagement.app.ui.emergency

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.moneymanagement.app.data.Account
import com.moneymanagement.app.data.Calculations
import com.moneymanagement.app.data.EmergencyFundAllocation
import com.moneymanagement.app.data.EmergencyFundAnalysis
import com.moneymanagement.app.data.EmergencyHealthStatus
import com.moneymanagement.app.data.MoneyRepository
import com.moneymanagement.app.settings.AppSettings
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

data class EmergencyFundUiState(
    val allocations: List<EmergencyFundAllocation> = emptyList(),
    val accounts: List<Account> = emptyList(),
    val analysis: EmergencyFundAnalysis = EmergencyFundAnalysis(
        totalSaved = 0.0,
        targetAmount = 150000.0,
        targetMonths = 6,
        monthlyExpense = 25000.0,
        coverageMonths = 0.0,
        progressPercent = 0f,
        healthStatus = EmergencyHealthStatus.BUILDING,
        storageBreakdown = emptyList(),
        recommendation = "",
    ),
    val targetMonths: Int = 6,
    val customTarget: Double = 0.0,
    val expenseOverride: Double = 0.0,
    val isLoading: Boolean = false,
)

class EmergencyFundViewModel(private val repository: MoneyRepository) : ViewModel() {

    val uiState: StateFlow<EmergencyFundUiState> = combine(
        repository.emergencyFunds,
        repository.transactions,
        repository.accounts,
        AppSettings.emergencyTargetMonths,
        AppSettings.emergencyCustomTarget,
        AppSettings.emergencyExpenseOverride,
    ) { args: Array<Any> ->
        @Suppress("UNCHECKED_CAST")
        val allocations = args[0] as List<EmergencyFundAllocation>
        @Suppress("UNCHECKED_CAST")
        val txns = args[1] as List<com.moneymanagement.app.data.TransactionRow>
        @Suppress("UNCHECKED_CAST")
        val accounts = args[2] as List<Account>
        val targetMonths = args[3] as Int
        val customTarget = args[4] as Double
        val expenseOverride = args[5] as Double

        val analysis = Calculations.calculateEmergencyFundMetrics(
            allocations = allocations,
            txns = txns,
            targetMonths = targetMonths,
            customTarget = customTarget,
            expenseOverride = expenseOverride,
        )
        EmergencyFundUiState(
            allocations = allocations,
            accounts = accounts,
            analysis = analysis,
            targetMonths = targetMonths,
            customTarget = customTarget,
            expenseOverride = expenseOverride,
            isLoading = false,
        )
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5_000),
        initialValue = EmergencyFundUiState(isLoading = true),
    )

    fun addAllocation(
        name: String,
        institution: String,
        storageType: String,
        amount: Double,
        returnRate: Double?,
        notes: String?,
        linkedAccountId: Long?,
    ) {
        viewModelScope.launch {
            repository.addEmergencyFundAllocation(
                name = name,
                institution = institution,
                storageType = storageType,
                amount = amount,
                returnRate = returnRate,
                notes = notes,
                linkedAccountId = linkedAccountId,
            )
        }
    }

    fun updateAllocation(
        id: Long,
        name: String,
        institution: String,
        storageType: String,
        amount: Double,
        returnRate: Double?,
        notes: String?,
        linkedAccountId: Long?,
    ) {
        viewModelScope.launch {
            repository.updateEmergencyFundAllocation(
                id = id,
                name = name,
                institution = institution,
                storageType = storageType,
                amount = amount,
                returnRate = returnRate,
                notes = notes,
                linkedAccountId = linkedAccountId,
            )
        }
    }

    fun adjustAmount(id: Long, delta: Double) {
        viewModelScope.launch {
            repository.adjustEmergencyFundAmount(id, delta)
        }
    }

    fun deleteAllocation(id: Long) {
        viewModelScope.launch {
            repository.deleteEmergencyFundAllocation(id)
        }
    }

    fun saveTargetSettings(months: Int, customTarget: Double, expenseOverride: Double) {
        AppSettings.setEmergencyTargetMonths(months)
        AppSettings.setEmergencyCustomTarget(customTarget)
        AppSettings.setEmergencyExpenseOverride(expenseOverride)
    }

    fun seedStarterData() {
        viewModelScope.launch {
            repository.seedStarterEmergencyFundsIfEmpty()
        }
    }
}
