package com.ravi.moneymanagement.ui.categories

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.ravi.moneymanagement.data.Calculations
import com.ravi.moneymanagement.data.Category
import com.ravi.moneymanagement.data.MoneyRepository
import java.time.LocalDate
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

data class CategoriesUiState(
    val incomeCategories: List<Category> = emptyList(),
    val expenseCategories: List<Category> = emptyList(),
    val spendByCategory: Map<Long, Double> = emptyMap(),
)

class CategoriesViewModel(private val repository: MoneyRepository) : ViewModel() {
    val uiState: StateFlow<CategoriesUiState> = combine(
        repository.categories,
        repository.transactions,
    ) { categories, txns ->
        val today = LocalDate.now()
        val breakdown = Calculations.categoryBreakdown(txns, categories, today.year, today.monthValue, "expense")
            .associate { it.categoryId to it.total }
        CategoriesUiState(
            incomeCategories = categories.filter { it.type == "income" },
            expenseCategories = categories.filter { it.type == "expense" },
            spendByCategory = breakdown,
        )
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), CategoriesUiState())

    fun addCategory(name: String, type: String, monthlyBudget: Double?) {
        viewModelScope.launch { repository.addCategory(name, type, monthlyBudget) }
    }

    fun updateCategory(id: Long, name: String, type: String, monthlyBudget: Double?) {
        viewModelScope.launch { repository.updateCategory(id, name, type, monthlyBudget) }
    }

    fun deleteCategory(id: Long) {
        viewModelScope.launch { repository.deleteCategory(id) }
    }
}
