package com.moneymanagement.app.ui.reports

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.moneymanagement.app.data.Account
import com.moneymanagement.app.data.AccountSpendShare
import com.moneymanagement.app.data.BudgetStatusRow
import com.moneymanagement.app.data.Calculations
import com.moneymanagement.app.data.Category
import com.moneymanagement.app.data.CategoryTotal
import com.moneymanagement.app.data.DayOfWeekSpend
import com.moneymanagement.app.data.MerchantSpend
import com.moneymanagement.app.data.MoneyRepository
import com.moneymanagement.app.data.MonthPoint
import com.moneymanagement.app.data.PeriodMetrics
import com.moneymanagement.app.data.SmartInsight
import com.moneymanagement.app.data.TimeRangePreset
import com.moneymanagement.app.data.TransactionRow
import com.moneymanagement.app.data.TrendPoint
import java.time.LocalDate
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn

data class ReportsUiState(
    val viewYear: Int = LocalDate.now().year,
    val viewMonth: Int = LocalDate.now().monthValue,
    val timeRangePreset: TimeRangePreset = TimeRangePreset.THIS_MONTH,
    val startDate: String = "",
    val endDate: String = "",
    val selectedType: String? = null,
    val selectedAccountId: Long? = null,
    val selectedCategoryId: Long? = null,
    val searchQuery: String = "",
    val sortBy: String = "date_desc",
    val metrics: PeriodMetrics = PeriodMetrics(),
    val previousMetrics: PeriodMetrics? = null,
    val smartInsights: List<SmartInsight> = emptyList(),
    val trend: List<MonthPoint> = emptyList(),
    val trendData: List<TrendPoint> = emptyList(),
    val breakdown: List<CategoryTotal> = emptyList(),
    val incomeBreakdown: List<CategoryTotal> = emptyList(),
    val topMerchants: List<MerchantSpend> = emptyList(),
    val dayOfWeekBreakdown: List<DayOfWeekSpend> = emptyList(),
    val accountBreakdown: List<AccountSpendShare> = emptyList(),
    val budgetStatus: List<BudgetStatusRow> = emptyList(),
    val filteredTransactions: List<TransactionRow> = emptyList(),
    val categories: List<Category> = emptyList(),
    val accounts: List<Account> = emptyList(),
)

class ReportsViewModel(private val repository: MoneyRepository) : ViewModel() {
    private val today = LocalDate.now()
    private val _viewYear = MutableStateFlow(today.year)
    private val _viewMonth = MutableStateFlow(today.monthValue)
    private val _timeRangePreset = MutableStateFlow(TimeRangePreset.THIS_MONTH)
    private val _customStartDate = MutableStateFlow<String?>(null)
    private val _customEndDate = MutableStateFlow<String?>(null)
    private val _selectedType = MutableStateFlow<String?>(null)
    private val _selectedAccountId = MutableStateFlow<Long?>(null)
    private val _selectedCategoryId = MutableStateFlow<Long?>(null)
    private val _searchQuery = MutableStateFlow("")
    private val _sortBy = MutableStateFlow("date_desc")

    private val filterCriteria = combine(
        _timeRangePreset,
        _customStartDate,
        _customEndDate,
        _viewYear,
        _viewMonth,
    ) { preset, customStart, customEnd, y, m ->
        FilterDates(preset, customStart, customEnd, y, m)
    }

    private val userFilters = combine(
        _selectedType,
        _selectedAccountId,
        _selectedCategoryId,
        _searchQuery,
        _sortBy,
    ) { type, accId, catId, q, sort ->
        UserFilters(type, accId, catId, q, sort)
    }

    val uiState: StateFlow<ReportsUiState> = combine(
        filterCriteria,
        userFilters,
        repository.categories,
        repository.accounts,
        repository.transactions,
    ) { dates, filters, categories, accounts, txns ->
        val (start, end) = Calculations.getDateRangeForPreset(
            preset = dates.preset,
            viewYear = dates.year,
            viewMonth = dates.month,
            customStart = dates.customStart,
            customEnd = dates.customEnd,
        )

        val (prevStart, prevEnd) = Calculations.getPrecedingDateRange(start, end)

        val currentMetrics = Calculations.calculatePeriodMetrics(txns, start, end)
        val prevMetrics = Calculations.calculatePeriodMetrics(txns, prevStart, prevEnd)

        val trendPoints = Calculations.calculateTrend(
            txns = txns,
            preset = dates.preset,
            startDate = start,
            endDate = end,
            viewYear = dates.year,
            viewMonth = dates.month,
        )
        val expenseBreakdown = Calculations.calculateCategoryBreakdown(txns, categories, start, end, "expense")
        val incomeBreakdown = Calculations.calculateCategoryBreakdown(txns, categories, start, end, "income")
        val topMerchants = Calculations.calculateTopMerchants(txns, start, end)
        val dayOfWeek = Calculations.calculateDayOfWeekSpending(txns, start, end)
        val accountShare = Calculations.calculateAccountSpendShare(txns, accounts, start, end)
        val budgetStatus = Calculations.calculateBudgetStatus(categories, txns, start, end)

        val insights = Calculations.generateSmartInsights(
            currentMetrics = currentMetrics,
            previousMetrics = prevMetrics,
            categoryBreakdown = expenseBreakdown,
            topMerchants = topMerchants,
            dayOfWeekSpend = dayOfWeek,
            budgetStatuses = budgetStatus,
            currencySymbol = "₹",
        )

        val filteredTxns = Calculations.filterTransactions(
            txns = txns,
            startDate = start,
            endDate = end,
            type = filters.type,
            accountId = filters.accountId,
            categoryId = filters.categoryId,
            searchQuery = filters.searchQuery,
            sortBy = filters.sortBy,
        )

        ReportsUiState(
            viewYear = dates.year,
            viewMonth = dates.month,
            timeRangePreset = dates.preset,
            startDate = start,
            endDate = end,
            selectedType = filters.type,
            selectedAccountId = filters.accountId,
            selectedCategoryId = filters.categoryId,
            searchQuery = filters.searchQuery,
            sortBy = filters.sortBy,
            metrics = currentMetrics,
            previousMetrics = prevMetrics,
            smartInsights = insights,
            trend = Calculations.monthlyTrend(txns, 6, dates.year, dates.month),
            trendData = trendPoints,
            breakdown = expenseBreakdown,
            incomeBreakdown = incomeBreakdown,
            topMerchants = topMerchants,
            dayOfWeekBreakdown = dayOfWeek,
            accountBreakdown = accountShare,
            budgetStatus = budgetStatus,
            filteredTransactions = filteredTxns,
            categories = categories,
            accounts = accounts,
        )
    }.stateIn(
        viewModelScope,
        SharingStarted.WhileSubscribed(5000),
        ReportsUiState(),
    )

    fun setTimeRangePreset(preset: TimeRangePreset) {
        _timeRangePreset.value = preset
    }

    fun setCustomDateRange(start: String, end: String) {
        _customStartDate.value = start
        _customEndDate.value = end
        _timeRangePreset.value = TimeRangePreset.CUSTOM
    }

    fun changeMonth(delta: Int) {
        var m = _viewMonth.value + delta
        var y = _viewYear.value
        when (m) {
            0 -> { m = 12; y -= 1 }
            13 -> { m = 1; y += 1 }
        }
        _viewYear.value = y
        _viewMonth.value = m
    }

    fun setTypeFilter(type: String?) {
        _selectedType.value = type
    }

    fun setAccountFilter(accountId: Long?) {
        _selectedAccountId.value = accountId
    }

    fun setCategoryFilter(categoryId: Long?) {
        _selectedCategoryId.value = categoryId
    }

    fun setSearchQuery(query: String) {
        _searchQuery.value = query
    }

    fun setSortBy(sort: String) {
        _sortBy.value = sort
    }

    fun resetFilters() {
        _selectedType.value = null
        _selectedAccountId.value = null
        _selectedCategoryId.value = null
        _searchQuery.value = ""
        _sortBy.value = "date_desc"
    }

    private data class FilterDates(
        val preset: TimeRangePreset,
        val customStart: String?,
        val customEnd: String?,
        val year: Int,
        val month: Int,
    )

    private data class UserFilters(
        val type: String?,
        val accountId: Long?,
        val categoryId: Long?,
        val searchQuery: String,
        val sortBy: String,
    )
}

