package com.moneymanagement.app.data

import java.time.DayOfWeek
import java.time.LocalDate
import java.time.Month
import java.time.YearMonth
import java.time.format.TextStyle
import java.time.temporal.ChronoUnit
import java.util.Locale

enum class TimeRangePreset(val label: String) {
    THIS_MONTH("This Month"),
    LAST_30_DAYS("Last 30 Days"),
    LAST_3_MONTHS("Last 3 Months"),
    LAST_6_MONTHS("Last 6 Months"),
    THIS_YEAR("This Year (YTD)"),
    ALL_TIME("All Time"),
    CUSTOM("Custom Range");

    val displayName: String get() = label
}

data class MonthPoint(val year: Int, val month: Int, val income: Double, val expense: Double)
data class TrendPoint(val label: String, val income: Double, val expense: Double, val net: Double)
data class CategoryTotal(
    val categoryId: Long,
    val name: String,
    val total: Double,
    val percentage: Double = 0.0,
    val count: Int = 0,
)
data class BudgetStatusRow(val id: Long, val name: String, val budget: Double?, val spent: Double, val remaining: Double?)

val Category.budget: Double? get() = monthlyBudget

data class PeriodMetrics(
    val totalIncome: Double = 0.0,
    val totalExpense: Double = 0.0,
    val totalTransfers: Double = 0.0,
    val netSavings: Double = 0.0,
    val savingsRate: Double = 0.0,
    val dailyAverageSpend: Double = 0.0,
    val transactionCount: Int = 0,
    val averageTransactionSize: Double = 0.0,
    val largestExpense: Double = 0.0,
    val daysCount: Int = 0,
) {
    val income: Double get() = totalIncome
    val expense: Double get() = totalExpense
    val transfers: Double get() = totalTransfers
    val savingsRatePct: Double get() = savingsRate
    val averageTransactionAmount: Double get() = averageTransactionSize
}

data class SmartInsight(
    val id: String,
    val iconType: String,
    val title: String,
    val description: String,
    val highlightText: String,
    val isPositive: Boolean? = null,
) {
    enum class InsightType { POSITIVE, WARNING, NEUTRAL }

    val type: InsightType
        get() = when (isPositive) {
            true -> InsightType.POSITIVE
            false -> InsightType.WARNING
            null -> InsightType.NEUTRAL
        }

    val icon: String
        get() = when (iconType) {
            "trend_down" -> "📉"
            "trend_up" -> "📈"
            "pie" -> "📊"
            "store" -> "🏪"
            "calendar" -> "📅"
            "savings" -> "💰"
            else -> "💡"
        }
}

data class MerchantSpend(
    val merchant: String,
    val totalAmount: Double,
    val count: Int,
    val percentage: Double,
) {
    val merchantName: String get() = merchant
}

data class DayOfWeekSpend(
    val dayOfWeek: DayOfWeek,
    val dayName: String,
    val totalAmount: Double,
    val averageAmount: Double,
    val count: Int,
)

data class AccountSpendShare(
    val accountId: Long,
    val accountName: String,
    val accountType: String,
    val expenseAmount: Double,
    val incomeAmount: Double,
    val expensePercentage: Double,
)

/**
 * Pure, in-memory ports of financial queries and analytical calculations.
 * Transaction dates are stored as ISO "yyyy-MM-dd" strings, which sort/compare correctly
 * as plain strings for fast range checks.
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

    fun monthRange(year: Int, month: Int): Pair<String, String> {
        val start = "%04d-%02d-01".format(year, month)
        val lastDay = YearMonth.of(year, month).lengthOfMonth()
        val end = "%04d-%02d-%02d".format(year, month, lastDay)
        return start to end
    }

    fun getDateRangeForPreset(
        preset: TimeRangePreset,
        viewYear: Int = LocalDate.now().year,
        viewMonth: Int = LocalDate.now().monthValue,
        customStart: String? = null,
        customEnd: String? = null,
    ): Pair<String, String> {
        val today = LocalDate.now()
        return when (preset) {
            TimeRangePreset.THIS_MONTH -> monthRange(viewYear, viewMonth)
            TimeRangePreset.LAST_30_DAYS -> {
                val start = today.minusDays(29).toString()
                val end = today.toString()
                start to end
            }
            TimeRangePreset.LAST_3_MONTHS -> {
                val start = today.minusMonths(3).plusDays(1).toString()
                val end = today.toString()
                start to end
            }
            TimeRangePreset.LAST_6_MONTHS -> {
                val start = today.minusMonths(6).plusDays(1).toString()
                val end = today.toString()
                start to end
            }
            TimeRangePreset.THIS_YEAR -> {
                val start = "${today.year}-01-01"
                val end = today.toString()
                start to end
            }
            TimeRangePreset.ALL_TIME -> "1970-01-01" to "2099-12-31"
            TimeRangePreset.CUSTOM -> {
                val s = customStart ?: today.minusDays(30).toString()
                val e = customEnd ?: today.toString()
                if (s <= e) s to e else e to s
            }
        }
    }

    fun getPrecedingDateRange(startDate: String, endDate: String): Pair<String, String> {
        val s = runCatching { LocalDate.parse(startDate) }.getOrNull() ?: LocalDate.now()
        val e = runCatching { LocalDate.parse(endDate) }.getOrNull() ?: LocalDate.now()
        val days = (ChronoUnit.DAYS.between(s, e) + 1).coerceAtLeast(1)
        val prevEnd = s.minusDays(1)
        val prevStart = prevEnd.minusDays(days - 1)
        return prevStart.toString() to prevEnd.toString()
    }

    fun calculatePeriodMetrics(
        txns: List<TransactionRow>,
        startDate: String,
        endDate: String,
    ): PeriodMetrics {
        var income = 0.0
        var expense = 0.0
        var transfers = 0.0
        var count = 0
        var maxExpense = 0.0

        for (row in txns) {
            val t = row.txn
            val date = if (t.txnDate.length >= 10) t.txnDate.substring(0, 10) else t.txnDate
            if (date < startDate || date > endDate) continue

            count++
            when (t.type) {
                "income" -> income += t.amount
                "expense" -> {
                    expense += t.amount
                    if (t.amount > maxExpense) maxExpense = t.amount
                }
                "transfer" -> transfers += t.amount
            }
        }

        val netSavings = income - expense
        val savingsRate = if (income > 0.0) ((netSavings / income) * 100.0).coerceIn(-100.0, 100.0) else if (expense > 0.0) -100.0 else 0.0

        val sDate = runCatching { LocalDate.parse(startDate) }.getOrNull() ?: LocalDate.now()
        val eDate = runCatching { LocalDate.parse(endDate) }.getOrNull() ?: LocalDate.now()
        val days = (ChronoUnit.DAYS.between(sDate, eDate) + 1).coerceAtLeast(1).toInt()
        val dailyAvg = expense / days
        val avgTxn = if (count > 0) (income + expense + transfers) / count else 0.0

        return PeriodMetrics(
            totalIncome = income,
            totalExpense = expense,
            totalTransfers = transfers,
            netSavings = netSavings,
            savingsRate = savingsRate,
            dailyAverageSpend = dailyAvg,
            transactionCount = count,
            averageTransactionSize = avgTxn,
            largestExpense = maxExpense,
            daysCount = days,
        )
    }

    fun monthSummary(txns: List<TransactionRow>, year: Int, month: Int): Pair<Double, Double> {
        val (start, end) = monthRange(year, month)
        var income = 0.0
        var expense = 0.0
        for (row in txns) {
            val t = row.txn
            val date = if (t.txnDate.length >= 10) t.txnDate.substring(0, 10) else t.txnDate
            if (date < start || date > end) continue
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

    fun calculateTrend(
        txns: List<TransactionRow>,
        preset: TimeRangePreset,
        startDate: String,
        endDate: String,
        viewYear: Int,
        viewMonth: Int,
    ): List<TrendPoint> {
        return when (preset) {
            TimeRangePreset.THIS_MONTH -> {
                // Divide month into 4 weeks/intervals
                val daysInMonth = YearMonth.of(viewYear, viewMonth).lengthOfMonth()
                val intervals = listOf(
                    1 to 7,
                    8 to 14,
                    15 to 21,
                    22 to daysInMonth,
                )
                intervals.mapIndexed { idx, (startD, endD) ->
                    val sStr = "%04d-%02d-%02d".format(viewYear, viewMonth, startD)
                    val eStr = "%04d-%02d-%02d".format(viewYear, viewMonth, endD)
                    val m = calculatePeriodMetrics(txns, sStr, eStr)
                    TrendPoint(
                        label = "W${idx + 1} (${startD}-${endD})",
                        income = m.totalIncome,
                        expense = m.totalExpense,
                        net = m.netSavings,
                    )
                }
            }
            TimeRangePreset.LAST_30_DAYS -> {
                // 5 intervals of 6 days each
                val endL = runCatching { LocalDate.parse(endDate) }.getOrNull() ?: LocalDate.now()
                val list = mutableListOf<TrendPoint>()
                for (i in 4 downTo 0) {
                    val s = endL.minusDays((i * 6 + 5).toLong())
                    val e = endL.minusDays((i * 6).toLong())
                    val m = calculatePeriodMetrics(txns, s.toString(), e.toString())
                    val label = "${s.dayOfMonth}/${s.monthValue}-${e.dayOfMonth}/${e.monthValue}"
                    list.add(TrendPoint(label = label, income = m.totalIncome, expense = m.totalExpense, net = m.netSavings))
                }
                list
            }
            TimeRangePreset.LAST_3_MONTHS -> {
                // 3 months
                val endL = runCatching { LocalDate.parse(endDate) }.getOrNull() ?: LocalDate.now()
                monthlyTrend(txns, 3, endL.year, endL.monthValue).map {
                    val label = Month.of(it.month).getDisplayName(TextStyle.SHORT, Locale.getDefault())
                    TrendPoint(label = label, income = it.income, expense = it.expense, net = it.income - it.expense)
                }
            }
            TimeRangePreset.LAST_6_MONTHS -> {
                val endL = runCatching { LocalDate.parse(endDate) }.getOrNull() ?: LocalDate.now()
                monthlyTrend(txns, 6, endL.year, endL.monthValue).map {
                    val label = Month.of(it.month).getDisplayName(TextStyle.SHORT, Locale.getDefault())
                    TrendPoint(label = label, income = it.income, expense = it.expense, net = it.income - it.expense)
                }
            }
            TimeRangePreset.THIS_YEAR -> {
                val currentMonth = LocalDate.now().monthValue
                monthlyTrend(txns, currentMonth, LocalDate.now().year, currentMonth).map {
                    val label = Month.of(it.month).getDisplayName(TextStyle.SHORT, Locale.getDefault())
                    TrendPoint(label = label, income = it.income, expense = it.expense, net = it.income - it.expense)
                }
            }
            else -> {
                // Default 6 months
                val endL = runCatching { LocalDate.parse(endDate) }.getOrNull() ?: LocalDate.now()
                monthlyTrend(txns, 6, endL.year, endL.monthValue).map {
                    val label = Month.of(it.month).getDisplayName(TextStyle.SHORT, Locale.getDefault())
                    TrendPoint(label = label, income = it.income, expense = it.expense, net = it.income - it.expense)
                }
            }
        }
    }

    fun categoryBreakdown(
        txns: List<TransactionRow>,
        categories: List<Category>,
        year: Int,
        month: Int,
        type: String,
    ): List<CategoryTotal> {
        val (start, end) = monthRange(year, month)
        return calculateCategoryBreakdown(txns, categories, start, end, type)
    }

    fun calculateCategoryBreakdown(
        txns: List<TransactionRow>,
        categories: List<Category>,
        startDate: String,
        endDate: String,
        type: String,
    ): List<CategoryTotal> {
        val totals = linkedMapOf<Long, Double>()
        val counts = linkedMapOf<Long, Int>()

        for (row in txns) {
            val t = row.txn
            if (t.type != type) continue
            val date = if (t.txnDate.length >= 10) t.txnDate.substring(0, 10) else t.txnDate
            if (date < startDate || date > endDate) continue
            val catId = t.categoryId ?: continue
            totals[catId] = (totals[catId] ?: 0.0) + t.amount
            counts[catId] = (counts[catId] ?: 0) + 1
        }

        val totalAmount = totals.values.sum().let { if (it <= 0.0) 1.0 else it }
        val catById = categories.associateBy { it.id }

        return totals.entries
            .mapNotNull { (id, total) ->
                catById[id]?.let { cat ->
                    val pct = (total / totalAmount) * 100.0
                    CategoryTotal(
                        categoryId = id,
                        name = cat.name,
                        total = total,
                        percentage = pct,
                        count = counts[id] ?: 0,
                    )
                }
            }
            .sortedByDescending { it.total }
    }

    fun calculateTopMerchants(
        txns: List<TransactionRow>,
        startDate: String,
        endDate: String,
        limit: Int = 5,
    ): List<MerchantSpend> {
        val merchantTotals = mutableMapOf<String, Double>()
        val merchantCounts = mutableMapOf<String, Int>()

        var totalSpend = 0.0
        for (row in txns) {
            val t = row.txn
            if (t.type != "expense") continue
            val date = if (t.txnDate.length >= 10) t.txnDate.substring(0, 10) else t.txnDate
            if (date < startDate || date > endDate) continue

            val rawName = t.note?.trim()
            val merchantName = if (!rawName.isNullOrBlank()) {
                cleanMerchantName(rawName)
            } else {
                row.categoryName ?: "Other"
            }

            merchantTotals[merchantName] = (merchantTotals[merchantName] ?: 0.0) + t.amount
            merchantCounts[merchantName] = (merchantCounts[merchantName] ?: 0) + 1
            totalSpend += t.amount
        }

        if (totalSpend <= 0.0) totalSpend = 1.0

        return merchantTotals.entries
            .sortedByDescending { it.value }
            .take(limit)
            .map { (name, total) ->
                MerchantSpend(
                    merchant = name,
                    totalAmount = total,
                    count = merchantCounts[name] ?: 1,
                    percentage = (total / totalSpend) * 100.0,
                )
            }
    }

    private fun cleanMerchantName(note: String): String {
        var clean = note
        // Strip common prefixes like "UPI/", "POS/", "Txn:", etc.
        clean = clean.replace("(?i)^upi\\/[0-9]+\\/".toRegex(), "")
        clean = clean.replace("(?i)^vpa\\s+".toRegex(), "")
        clean = clean.replace("(?i)^bill payment:\\s*".toRegex(), "")
        clean = clean.replace("(?i)^sms txn\\s*\\(?".toRegex(), "").replace(")", "")
        return if (clean.length > 25) clean.take(25) + "…" else clean
    }

    fun calculateDayOfWeekSpending(
        txns: List<TransactionRow>,
        startDate: String,
        endDate: String,
    ): List<DayOfWeekSpend> {
        val dayTotals = mutableMapOf<DayOfWeek, Double>()
        val dayCounts = mutableMapOf<DayOfWeek, Int>()

        DayOfWeek.entries.forEach {
            dayTotals[it] = 0.0
            dayCounts[it] = 0
        }

        for (row in txns) {
            val t = row.txn
            if (t.type != "expense") continue
            val dateStr = if (t.txnDate.length >= 10) t.txnDate.substring(0, 10) else t.txnDate
            if (dateStr < startDate || dateStr > endDate) continue

            val date = runCatching { LocalDate.parse(dateStr) }.getOrNull() ?: continue
            val dow = date.dayOfWeek
            dayTotals[dow] = (dayTotals[dow] ?: 0.0) + t.amount
            dayCounts[dow] = (dayCounts[dow] ?: 0) + 1
        }

        return DayOfWeek.entries.map { dow ->
            val total = dayTotals[dow] ?: 0.0
            val count = dayCounts[dow] ?: 0
            val avg = if (count > 0) total / count else 0.0
            val name = dow.getDisplayName(TextStyle.SHORT, Locale.getDefault())
            DayOfWeekSpend(
                dayOfWeek = dow,
                dayName = name,
                totalAmount = total,
                averageAmount = avg,
                count = count,
            )
        }
    }

    fun calculateAccountSpendShare(
        txns: List<TransactionRow>,
        accounts: List<Account>,
        startDate: String,
        endDate: String,
    ): List<AccountSpendShare> {
        val expTotals = mutableMapOf<Long, Double>()
        val incTotals = mutableMapOf<Long, Double>()

        var totalExp = 0.0
        for (row in txns) {
            val t = row.txn
            val dateStr = if (t.txnDate.length >= 10) t.txnDate.substring(0, 10) else t.txnDate
            if (dateStr < startDate || dateStr > endDate) continue

            when (t.type) {
                "expense" -> {
                    expTotals[t.accountId] = (expTotals[t.accountId] ?: 0.0) + t.amount
                    totalExp += t.amount
                }
                "income" -> {
                    incTotals[t.accountId] = (incTotals[t.accountId] ?: 0.0) + t.amount
                }
            }
        }

        if (totalExp <= 0.0) totalExp = 1.0
        val accMap = accounts.associateBy { it.id }

        return accounts.map { acc ->
            val exp = expTotals[acc.id] ?: 0.0
            val inc = incTotals[acc.id] ?: 0.0
            val pct = (exp / totalExp) * 100.0
            AccountSpendShare(
                accountId = acc.id,
                accountName = acc.name,
                accountType = acc.type,
                expenseAmount = exp,
                incomeAmount = inc,
                expensePercentage = pct,
            )
        }.filter { it.expenseAmount > 0.0 || it.incomeAmount > 0.0 }
            .sortedByDescending { it.expenseAmount }
    }

    fun budgetStatus(
        categories: List<Category>,
        txns: List<TransactionRow>,
        year: Int,
        month: Int,
    ): List<BudgetStatusRow> {
        val (start, end) = monthRange(year, month)
        return calculateBudgetStatus(categories, txns, start, end)
    }

    fun calculateBudgetStatus(
        categories: List<Category>,
        txns: List<TransactionRow>,
        startDate: String,
        endDate: String,
    ): List<BudgetStatusRow> {
        val breakdown = calculateCategoryBreakdown(txns, categories, startDate, endDate, "expense").associateBy { it.categoryId }
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

    fun generateSmartInsights(
        currentMetrics: PeriodMetrics,
        previousMetrics: PeriodMetrics,
        categoryBreakdown: List<CategoryTotal>,
        topMerchants: List<MerchantSpend>,
        dayOfWeekSpend: List<DayOfWeekSpend>,
        budgetStatuses: List<BudgetStatusRow>,
        currencySymbol: String = "₹",
    ): List<SmartInsight> {
        val list = mutableListOf<SmartInsight>()

        // 1. Period Comparison Insight
        if (previousMetrics.totalExpense > 0.0 && currentMetrics.totalExpense > 0.0) {
            val diff = currentMetrics.totalExpense - previousMetrics.totalExpense
            val pctDiff = (diff / previousMetrics.totalExpense) * 100.0
            val isLower = diff < 0
            val diffStr = "${kotlin.math.abs(pctDiff).toInt()}%"
            val title = if (isLower) "Spending is Down $diffStr 🎉" else "Spending is Up $diffStr"
            val desc = if (isLower) {
                "You spent $currencySymbol${kotlin.math.abs(diff).toInt()} less compared to the previous period."
            } else {
                "You spent $currencySymbol${diff.toInt()} more compared to the previous period."
            }
            list.add(
                SmartInsight(
                    id = "period_compare",
                    iconType = if (isLower) "trend_down" else "trend_up",
                    title = title,
                    description = desc,
                    highlightText = if (isLower) "$diffStr saved" else "+$diffStr",
                    isPositive = isLower,
                )
            )
        }

        // 2. Top Category Dominance
        val topCat = categoryBreakdown.firstOrNull()
        if (topCat != null && topCat.percentage > 0) {
            val pctStr = "${topCat.percentage.toInt()}%"
            list.add(
                SmartInsight(
                    id = "top_category",
                    iconType = "pie",
                    title = "${topCat.name} is Your #1 Expense",
                    description = "${topCat.name} accounts for $pctStr of your total outflow ($currencySymbol${topCat.total.toInt()} across ${topCat.count} transactions).",
                    highlightText = "$pctStr of total",
                    isPositive = null,
                )
            )
        }

        // 3. Top Payee / Merchant
        val topMerchant = topMerchants.firstOrNull()
        if (topMerchant != null && topMerchant.totalAmount > 0) {
            list.add(
                SmartInsight(
                    id = "top_merchant",
                    iconType = "store",
                    title = "Most Frequent Payee: ${topMerchant.merchant}",
                    description = "Spent $currencySymbol${topMerchant.totalAmount.toInt()} across ${topMerchant.count} payments (${topMerchant.percentage.toInt()}% of total expenses).",
                    highlightText = "${topMerchant.count} payments",
                    isPositive = null,
                )
            )
        }

        // 4. Weekend vs Weekday Spend
        val weekendSpend = dayOfWeekSpend.filter { it.dayOfWeek == DayOfWeek.SATURDAY || it.dayOfWeek == DayOfWeek.SUNDAY }.sumOf { it.totalAmount }
        val weekdaySpend = dayOfWeekSpend.filter { it.dayOfWeek != DayOfWeek.SATURDAY && it.dayOfWeek != DayOfWeek.SUNDAY }.sumOf { it.totalAmount }
        val totalDaysSpend = weekendSpend + weekdaySpend
        if (totalDaysSpend > 0) {
            val weekendPct = (weekendSpend / totalDaysSpend) * 100.0
            val peakDay = dayOfWeekSpend.maxByOrNull { it.totalAmount }
            if (peakDay != null && peakDay.totalAmount > 0) {
                list.add(
                    SmartInsight(
                        id = "day_pattern",
                        iconType = "calendar",
                        title = "Peak Spend on ${peakDay.dayOfWeek.getDisplayName(TextStyle.FULL, Locale.getDefault())}s",
                        description = "Highest outflow occurs on ${peakDay.dayName} ($currencySymbol${peakDay.totalAmount.toInt()}). Weekend spending represents ${weekendPct.toInt()}% of total.",
                        highlightText = "Peak: ${peakDay.dayName}",
                        isPositive = null,
                    )
                )
            }
        }

        // 5. Budget Health
        val budgeted = budgetStatuses.filter { it.budget != null }
        if (budgeted.isNotEmpty()) {
            val exceeded = budgeted.count { it.spent > (it.budget ?: 0.0) }
            val onTrack = budgeted.size - exceeded
            val title = if (exceeded == 0) "All Budgets On Track ✨" else "$exceeded Budget${if (exceeded > 1) "s" else ""} Exceeded ⚠️"
            val desc = if (exceeded == 0) {
                "All ${budgeted.size} budgeted categories are currently within limits."
            } else {
                "$onTrack of ${budgeted.size} categories are on track, while $exceeded need attention."
            }
            list.add(
                SmartInsight(
                    id = "budget_health",
                    iconType = "savings",
                    title = title,
                    description = desc,
                    highlightText = "$onTrack/${budgeted.size} OK",
                    isPositive = exceeded == 0,
                )
            )
        }

        return list
    }

    fun filterTransactions(
        txns: List<TransactionRow>,
        startDate: String? = null,
        endDate: String? = null,
        type: String? = null, // "expense", "income", "transfer", or null for all
        accountId: Long? = null,
        categoryId: Long? = null,
        searchQuery: String? = null,
        sortBy: String = "DATE_DESC", // "DATE_DESC", "DATE_ASC", "AMOUNT_DESC", "AMOUNT_ASC"
    ): List<TransactionRow> {
        val q = searchQuery?.trim()?.lowercase(Locale.getDefault())

        val filtered = txns.filter { row ->
            val t = row.txn
            val dateStr = if (t.txnDate.length >= 10) t.txnDate.substring(0, 10) else t.txnDate

            // Date Range
            if (startDate != null && dateStr < startDate) return@filter false
            if (endDate != null && dateStr > endDate) return@filter false

            // Type
            if (!type.isNullOrBlank() && type != "all" && !t.type.equals(type, ignoreCase = true)) return@filter false

            // Account
            if (accountId != null && t.accountId != accountId && t.toAccountId != accountId) return@filter false

            // Category
            if (categoryId != null && t.categoryId != categoryId) return@filter false

            // Search Query
            if (!q.isNullOrBlank()) {
                val matchesNote = t.note?.lowercase(Locale.getDefault())?.contains(q) == true
                val matchesCat = row.categoryName?.lowercase(Locale.getDefault())?.contains(q) == true
                val matchesAcc = row.accountName?.lowercase(Locale.getDefault())?.contains(q) == true
                val matchesToAcc = row.toAccountName?.lowercase(Locale.getDefault())?.contains(q) == true
                val matchesAmt = t.amount.toString().contains(q)
                if (!matchesNote && !matchesCat && !matchesAcc && !matchesToAcc && !matchesAmt) {
                    return@filter false
                }
            }

            true
        }

        return when (sortBy) {
            "DATE_ASC" -> filtered.sortedWith(compareBy({ it.txn.txnDate }, { it.txn.id }))
            "AMOUNT_DESC" -> filtered.sortedByDescending { it.txn.amount }
            "AMOUNT_ASC" -> filtered.sortedBy { it.txn.amount }
            else -> filtered.sortedWith(compareByDescending<TransactionRow> { it.txn.txnDate }.thenByDescending { it.txn.id })
        }
    }
}

