package com.moneymanagement.app.data

import java.time.YearMonth

data class MonthPoint(val year: Int, val month: Int, val income: Double, val expense: Double)
data class CategoryTotal(val categoryId: Long, val name: String, val total: Double)
data class BudgetStatusRow(val id: Long, val name: String, val budget: Double?, val spent: Double, val remaining: Double?)

/**
 * Pure, in-memory ports of the report queries from the original Python `Database` class.
 * Transaction dates are stored as ISO "yyyy-MM-dd" strings, which sort/compare correctly
 * as plain strings, so no date parsing is needed for range checks.
 */
object Calculations {

    fun accountBalance(account: Account, txns: List<TransactionRow>): Double {
        var balance = account.initialBalance
        for (row in txns) {
            val t = row.txn
            when {
                t.accountId == account.id && t.type == "income" -> balance += t.amount
                t.accountId == account.id && t.type == "expense" -> balance -= t.amount
                t.accountId == account.id && t.type == "transfer" -> balance -= t.amount
                t.toAccountId == account.id && t.type == "transfer" -> balance += t.amount
            }
        }
        return balance
    }

    fun totalBalance(accounts: List<Account>, txns: List<TransactionRow>): Double =
        accounts.sumOf { accountBalance(it, txns) }

    private fun monthRange(year: Int, month: Int): Pair<String, String> {
        val start = "%04d-%02d-01".format(year, month)
        val lastDay = YearMonth.of(year, month).lengthOfMonth()
        val end = "%04d-%02d-%02d".format(year, month, lastDay)
        return start to end
    }

    fun monthSummary(txns: List<TransactionRow>, year: Int, month: Int): Pair<Double, Double> {
        val (start, end) = monthRange(year, month)
        var income = 0.0
        var expense = 0.0
        for (row in txns) {
            val t = row.txn
            if (t.txnDate < start || t.txnDate > end) continue
            when (t.type) {
                "income" -> income += t.amount
                "expense" -> expense += t.amount
            }
        }
        return income to expense
    }

    fun monthlyTrend(txns: List<TransactionRow>, months: Int, endYear: Int, endMonth: Int): List<MonthPoint> {
        val results = mutableListOf<MonthPoint>()
        var y = endYear
        var m = endMonth
        repeat(months) {
            val (income, expense) = monthSummary(txns, y, m)
            results.add(MonthPoint(y, m, income, expense))
            m -= 1
            if (m == 0) {
                m = 12
                y -= 1
            }
        }
        return results.reversed()
    }

    fun categoryBreakdown(
        txns: List<TransactionRow>,
        categories: List<Category>,
        year: Int,
        month: Int,
        type: String,
    ): List<CategoryTotal> {
        val (start, end) = monthRange(year, month)
        val totals = linkedMapOf<Long, Double>()
        for (row in txns) {
            val t = row.txn
            if (t.type != type) continue
            if (t.txnDate < start || t.txnDate > end) continue
            val catId = t.categoryId ?: continue
            totals[catId] = (totals[catId] ?: 0.0) + t.amount
        }
        val catById = categories.associateBy { it.id }
        return totals.entries
            .mapNotNull { (id, total) -> catById[id]?.let { CategoryTotal(id, it.name, total) } }
            .sortedByDescending { it.total }
    }

    fun budgetStatus(
        categories: List<Category>,
        txns: List<TransactionRow>,
        year: Int,
        month: Int,
    ): List<BudgetStatusRow> {
        val breakdown = categoryBreakdown(txns, categories, year, month, "expense").associateBy { it.categoryId }
        return categories.filter { it.type == "expense" }.map { cat ->
            val spent = breakdown[cat.id]?.total ?: 0.0
            val budget = cat.monthlyBudget
            BudgetStatusRow(
                id = cat.id,
                name = cat.name,
                budget = budget,
                spent = spent,
                remaining = if (budget != null) budget - spent else null,
            )
        }
    }
}
