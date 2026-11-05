package com.moneymanagement.app.data

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Test

class CalculationsAnalyticsTest {

    private val sampleCategories = listOf(
        Category(id = 1L, name = "Food", type = "expense", monthlyBudget = 5000.0, createdAt = "2026-08-01T00:00:00Z"),
        Category(id = 2L, name = "Shopping", type = "expense", monthlyBudget = 3000.0, createdAt = "2026-08-01T00:00:00Z"),
        Category(id = 3L, name = "Salary", type = "income", monthlyBudget = null, createdAt = "2026-08-01T00:00:00Z"),
    )

    private val sampleAccounts = listOf(
        Account(id = 1L, name = "HDFC Bank", type = "bank", initialBalance = 50000.0, createdAt = "2026-08-01T00:00:00Z"),
        Account(id = 2L, name = "Cash Wallet", type = "cash", initialBalance = 5000.0, createdAt = "2026-08-01T00:00:00Z"),
    )

    private val sampleTransactions = listOf(
        TransactionRow(
            txn = Transaction(id = 1, accountId = 1, toAccountId = null, categoryId = 3, type = "income", amount = 60000.0, note = "Monthly Salary", txnDate = "2026-08-01", createdAt = "2026-08-01T10:00:00Z"),
            categoryName = "Salary",
            accountName = "HDFC Bank",
            toAccountName = null,
        ),
        TransactionRow(
            txn = Transaction(id = 2, accountId = 1, toAccountId = null, categoryId = 1, type = "expense", amount = 1500.0, note = "Zomato Dinner", txnDate = "2026-08-05", createdAt = "2026-08-05T20:00:00Z"),
            categoryName = "Food",
            accountName = "HDFC Bank",
            toAccountName = null,
        ),
        TransactionRow(
            txn = Transaction(id = 3, accountId = 1, toAccountId = null, categoryId = 1, type = "expense", amount = 800.0, note = "Swiggy Lunch", txnDate = "2026-08-08", createdAt = "2026-08-08T13:00:00Z"),
            categoryName = "Food",
            accountName = "HDFC Bank",
            toAccountName = null,
        ),
        TransactionRow(
            txn = Transaction(id = 4, accountId = 1, toAccountId = null, categoryId = 2, type = "expense", amount = 3500.0, note = "Amazon Shoes", txnDate = "2026-08-15", createdAt = "2026-08-15T15:00:00Z"),
            categoryName = "Shopping",
            accountName = "HDFC Bank",
            toAccountName = null,
        ),
        TransactionRow(
            txn = Transaction(id = 5, accountId = 2, toAccountId = null, categoryId = 1, type = "expense", amount = 200.0, note = "Street Snack", txnDate = "2026-08-20", createdAt = "2026-08-20T17:00:00Z"),
            categoryName = "Food",
            accountName = "Cash Wallet",
            toAccountName = null,
        ),
        TransactionRow(
            txn = Transaction(id = 6, accountId = 1, toAccountId = 2, categoryId = null, type = "transfer", amount = 2000.0, note = "ATM Cash Withdrawal", txnDate = "2026-08-10", createdAt = "2026-08-10T11:00:00Z"),
            categoryName = null,
            accountName = "HDFC Bank",
            toAccountName = "Cash Wallet",
        ),
    )

    @Test
    fun testPeriodMetricsCalculation() {
        val metrics = Calculations.calculatePeriodMetrics(
            txns = sampleTransactions,
            startDate = "2026-08-01",
            endDate = "2026-08-31",
        )

        assertEquals(60000.0, metrics.income, 0.001)
        assertEquals(6000.0, metrics.expense, 0.001) // 1500 + 800 + 3500 + 200
        assertEquals(2000.0, metrics.transfers, 0.001)
        assertEquals(54000.0, metrics.netSavings, 0.001)
        assertEquals(90.0, metrics.savingsRatePct, 0.001)
        assertEquals(3500.0, metrics.largestExpense, 0.001)
        assertEquals(6, metrics.transactionCount) // 1 income + 4 expenses + 1 transfer
        assertEquals(31, metrics.daysCount)
    }

    @Test
    fun testCategoryBreakdownCalculation() {
        val breakdown = Calculations.calculateCategoryBreakdown(
            txns = sampleTransactions,
            categories = sampleCategories,
            startDate = "2026-08-01",
            endDate = "2026-08-31",
            type = "expense",
        )

        assertEquals(2, breakdown.size)
        val shopping = breakdown.find { it.name == "Shopping" }
        val food = breakdown.find { it.name == "Food" }

        assertNotNull(shopping)
        assertEquals(3500.0, shopping!!.total, 0.001)
        assertEquals(1, shopping.count)

        assertNotNull(food)
        assertEquals(2500.0, food!!.total, 0.001) // 1500 + 800 + 200
        assertEquals(3, food.count)
    }

    @Test
    fun testTopMerchantsCalculation() {
        val merchants = Calculations.calculateTopMerchants(
            txns = sampleTransactions,
            startDate = "2026-08-01",
            endDate = "2026-08-31",
        )

        assertTrue(merchants.isNotEmpty())
        assertEquals("Amazon Shoes", merchants.first().merchantName)
        assertEquals(3500.0, merchants.first().totalAmount, 0.001)
    }

    @Test
    fun testDayOfWeekSpending() {
        val dayOfWeek = Calculations.calculateDayOfWeekSpending(
            txns = sampleTransactions,
            startDate = "2026-08-01",
            endDate = "2026-08-31",
        )

        assertEquals(7, dayOfWeek.size)
        val totalSum = dayOfWeek.sumOf { it.totalAmount }
        assertEquals(6000.0, totalSum, 0.001)
    }

    @Test
    fun testAccountSpendShare() {
        val shares = Calculations.calculateAccountSpendShare(
            txns = sampleTransactions,
            accounts = sampleAccounts,
            startDate = "2026-08-01",
            endDate = "2026-08-31",
        )

        assertEquals(2, shares.size)
        val hdfc = shares.find { it.accountName == "HDFC Bank" }
        val cash = shares.find { it.accountName == "Cash Wallet" }

        assertNotNull(hdfc)
        assertEquals(5800.0, hdfc!!.expenseAmount, 0.001)

        assertNotNull(cash)
        assertEquals(200.0, cash!!.expenseAmount, 0.001)
    }

    @Test
    fun testSmartInsightsGeneration() {
        val currentMetrics = Calculations.calculatePeriodMetrics(sampleTransactions, "2026-08-01", "2026-08-31")
        val prevMetrics = PeriodMetrics(totalIncome = 50000.0, totalExpense = 8000.0)
        val categoryTotals = Calculations.calculateCategoryBreakdown(sampleTransactions, sampleCategories, "2026-08-01", "2026-08-31", "expense")
        val merchants = Calculations.calculateTopMerchants(sampleTransactions, "2026-08-01", "2026-08-31")
        val dayOfWeek = Calculations.calculateDayOfWeekSpending(sampleTransactions, "2026-08-01", "2026-08-31")
        val budgetStatus = Calculations.calculateBudgetStatus(sampleCategories, sampleTransactions, "2026-08-01", "2026-08-31")

        val insights = Calculations.generateSmartInsights(
            currentMetrics = currentMetrics,
            previousMetrics = prevMetrics,
            categoryBreakdown = categoryTotals,
            topMerchants = merchants,
            dayOfWeekSpend = dayOfWeek,
            budgetStatuses = budgetStatus,
            currencySymbol = "₹",
        )

        assertTrue("Should generate smart insights", insights.isNotEmpty())
        assertTrue("Insights should include spending reduction or dominance", insights.any { it.title.contains("Spending", ignoreCase = true) || it.title.contains("Expense", ignoreCase = true) || it.title.contains("Payee", ignoreCase = true) })
    }

    @Test
    fun testTransactionMultiFiltering() {
        // Test query search
        val searchResult = Calculations.filterTransactions(
            txns = sampleTransactions,
            startDate = "2026-08-01",
            endDate = "2026-08-31",
            searchQuery = "Zomato",
        )
        assertEquals(1, searchResult.size)
        assertEquals("Zomato Dinner", searchResult.first().txn.note)

        // Test category filter
        val catResult = Calculations.filterTransactions(
            txns = sampleTransactions,
            startDate = "2026-08-01",
            endDate = "2026-08-31",
            categoryId = 1L, // Food
        )
        assertEquals(3, catResult.size)

        // Test sorting by amount desc
        val sortResult = Calculations.filterTransactions(
            txns = sampleTransactions,
            startDate = "2026-08-01",
            endDate = "2026-08-31",
            sortBy = "AMOUNT_DESC",
        )
        assertEquals(60000.0, sortResult.first().txn.amount, 0.001)
    }

    @Test
    fun testDateRangePresets() {
        val (startMonth, endMonth) = Calculations.getDateRangeForPreset(TimeRangePreset.THIS_MONTH, 2026, 8)
        assertEquals("2026-08-01", startMonth)
        assertEquals("2026-08-31", endMonth)

        val (prevStart, prevEnd) = Calculations.getPrecedingDateRange(startMonth, endMonth)
        assertEquals("2026-07-01", prevStart)
        assertEquals("2026-07-31", prevEnd)
    }

    @Test
    fun testCreditCardExpenseDoesNotDeductBankBalance() {
        val bankAccount = Account(id = 1L, name = "HDFC Bank", type = "bank", initialBalance = 10000.0, createdAt = "2026-08-01T00:00:00Z")
        val ccExpense = TransactionRow(
            txn = Transaction(
                id = 100,
                accountId = null, // paid by credit card
                toAccountId = null,
                categoryId = 1, // Food
                creditCardId = 5L,
                type = "expense",
                amount = 2500.0,
                note = "Flight booking via CC",
                txnDate = "2026-08-12",
                createdAt = "2026-08-12T10:00:00Z",
            ),
            categoryName = "Food",
            accountName = null,
            toAccountName = null,
            creditCardName = "HDFC Regalia",
        )

        // Bank balance should remain untouched
        val bankBalance = Calculations.accountBalance(bankAccount, listOf(ccExpense))
        assertEquals(10000.0, bankBalance, 0.001)

        // But month expense summary should include it
        val (income, expense) = Calculations.monthSummary(listOf(ccExpense), 2026, 8)
        assertEquals(0.0, income, 0.001)
        assertEquals(2500.0, expense, 0.001)

        // Category breakdown should also reflect it
        val breakdown = Calculations.categoryBreakdown(listOf(ccExpense), sampleCategories, 2026, 8, "expense")
        val foodTotal = breakdown.find { it.categoryId == 1L }?.total ?: 0.0
        assertEquals(2500.0, foodTotal, 0.001)
    }

    @Test
    fun testEmergencyFundMetricsAndStorageDistribution() {
        val sampleAllocations = listOf(
            EmergencyFundAllocation(
                id = 1L,
                name = "HDFC High Interest Savings",
                institution = "HDFC Bank",
                storageType = "bank_savings",
                amount = 60000.0,
                returnRate = 4.5,
                createdAt = "2026-08-01T00:00:00Z",
                updatedAt = "2026-08-01T00:00:00Z",
            ),
            EmergencyFundAllocation(
                id = 2L,
                name = "Nippon Liquid Mutual Fund",
                institution = "Nippon AMC",
                storageType = "liquid_fund",
                amount = 90000.0,
                returnRate = 6.9,
                createdAt = "2026-08-01T00:00:00Z",
                updatedAt = "2026-08-01T00:00:00Z",
            ),
            EmergencyFundAllocation(
                id = 3L,
                name = "Emergency Cash in Home Safe",
                institution = "Physical Cash",
                storageType = "cash",
                amount = 30000.0,
                returnRate = null,
                createdAt = "2026-08-01T00:00:00Z",
                updatedAt = "2026-08-01T00:00:00Z",
            ),
        )

        val analysis = Calculations.calculateEmergencyFundMetrics(
            allocations = sampleAllocations,
            txns = sampleTransactions,
            targetMonths = 6,
            customTarget = 0.0,
            expenseOverride = 30000.0, // 30,000 per month
        )

        // Total saved: 60k + 90k + 30k = 180,000
        assertEquals(180000.0, analysis.totalSaved, 0.001)

        // Target: 6 months * 30,000 = 180,000
        assertEquals(180000.0, analysis.targetAmount, 0.001)

        // Coverage months: 180,000 / 30,000 = 6.0 months
        assertEquals(6.0, analysis.coverageMonths, 0.001)

        // 100% funded
        assertEquals(100f, analysis.progressPercent, 0.01f)

        // Health status should be SECURE
        assertEquals(EmergencyHealthStatus.SECURE, analysis.healthStatus)

        // Check storage breakdown
        assertEquals(3, analysis.storageBreakdown.size)
        val liquidFundGroup = analysis.storageBreakdown.find { it.type == "liquid_fund" }
        assertNotNull(liquidFundGroup)
        assertEquals(90000.0, liquidFundGroup!!.amount, 0.001)
        assertEquals(50.0, liquidFundGroup.percentage, 0.001) // 90k / 180k = 50%
    }
}
