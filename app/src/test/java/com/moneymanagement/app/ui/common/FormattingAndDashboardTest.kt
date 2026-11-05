package com.moneymanagement.app.ui.common

import com.moneymanagement.app.data.Account
import com.moneymanagement.app.data.Calculations
import com.moneymanagement.app.data.Transaction
import com.moneymanagement.app.data.TransactionRow
import com.moneymanagement.app.sms.SmsParser
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class FormattingAndDashboardTest {

    @Test
    fun testFormatMoneyWithSymbolPositive() {
        val result = formatMoneyWithSymbol(53974.80, 2)
        assertEquals("₹ 53,974.80", result)
    }

    @Test
    fun testFormatMoneyWithSymbolNegative() {
        val result = formatMoneyWithSymbol(-53974.80, 2)
        assertEquals("-₹ 53,974.80", result)
    }

    @Test
    fun testFormatSignedMoneyWithSymbol() {
        assertEquals("+₹ 152,917.00", formatSignedMoneyWithSymbol(152917.0, 2))
        assertEquals("-₹ 207,659.00", formatSignedMoneyWithSymbol(-207659.0, 2))
        assertEquals("₹ 0.00", formatSignedMoneyWithSymbol(0.0, 2))
    }

    @Test
    fun testDeficitBalanceCalculation() {
        val account = Account(id = 1L, name = "Bank Account", type = "bank", initialBalance = 0.0, createdAt = "2026-08-01T00:00:00Z")
        val txns = listOf(
            TransactionRow(
                txn = Transaction(id = 1, accountId = 1, toAccountId = null, categoryId = 1, type = "income", amount = 152917.0, note = "Income", txnDate = "2026-08-01", createdAt = "2026-08-01T10:00:00Z"),
                categoryName = "Salary",
                accountName = "Bank Account",
                toAccountName = null,
            ),
            TransactionRow(
                txn = Transaction(id = 2, accountId = 1, toAccountId = null, categoryId = 2, type = "expense", amount = 207659.0, note = "Bills and Shopping", txnDate = "2026-08-05", createdAt = "2026-08-05T10:00:00Z"),
                categoryName = "Bills",
                accountName = "Bank Account",
                toAccountName = null,
            )
        )

        val balance = Calculations.accountBalance(account, txns)
        assertEquals(-54742.0, balance, 0.001)

        val (income, expense) = Calculations.monthSummary(txns, 2026, 8)
        assertEquals(152917.0, income, 0.001)
        assertEquals(207659.0, expense, 0.001)

        val netSavings = income - expense
        val isDeficit = expense > income
        assertEquals(-54742.0, netSavings, 0.001)
        assertTrue(isDeficit)

        // Verify formatting of negative balance
        assertEquals("-₹ 54,742.00", formatMoneyWithSymbol(balance, 2))
    }

    @Test
    fun testDisambiguateCreditSmsWithTxnWords() {
        val body = "Your A/c ending 1234 is credited with INR 5,000.00 on 10-Aug for UPI txn of refund from Store"
        val parsed = SmsParser.parse("AD-HDFCBK", body)
        assertTrue(parsed.isFinancial)
        assertEquals("income", parsed.type)
        assertEquals(5000.0, parsed.amount, 0.001)
    }

    @Test
    fun testDisambiguateDebitSmsWithCreditWords() {
        val body = "Your A/c ending 1234 is debited by INR 1,500.00 on 10-Aug for credit card bill payment"
        val parsed = SmsParser.parse("AD-HDFCBK", body)
        assertTrue(parsed.isFinancial)
        assertEquals("expense", parsed.type)
        assertEquals(1500.0, parsed.amount, 0.001)
    }

    @Test
    fun testAccountBalanceWithNegativeAmountHandling() {
        val account = Account(id = 1L, name = "Bank Account", type = "bank", initialBalance = 10000.0, createdAt = "2026-08-01T00:00:00Z")
        val txns = listOf(
            TransactionRow(
                txn = Transaction(id = 1, accountId = 1, toAccountId = null, categoryId = 1, type = "expense", amount = -2500.0, note = "Expense with negative amount", txnDate = "2026-08-02", createdAt = "2026-08-02T10:00:00Z"),
                categoryName = "Food",
                accountName = "Bank Account",
                toAccountName = null,
            )
        )

        val balance = Calculations.accountBalance(account, txns)
        // Should subtract 2500, resulting in 7500, not add 2500
        assertEquals(7500.0, balance, 0.001)

        val (income, expense) = Calculations.monthSummary(txns, 2026, 8)
        assertEquals(0.0, income, 0.001)
        assertEquals(2500.0, expense, 0.001)
    }
}
