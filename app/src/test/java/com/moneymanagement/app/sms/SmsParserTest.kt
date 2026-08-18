package com.moneymanagement.app.sms

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Test

class SmsParserTest {

    @Test
    fun testHdfcDebitSms() {
        val body = "Rs. 1,499.00 debited from HDFC Bank A/c xx1234 on 18-AUG-26 to ZOMATO. Avail Bal: Rs 45,200.00"
        val result = SmsParser.parse("AD-HDFCBK", body)

        assertTrue(result.isFinancial)
        assertEquals(1499.0, result.amount, 0.001)
        assertEquals("expense", result.type)
        assertEquals("Food", result.suggestedCategory)
        assertTrue(result.merchantOrNote?.contains("ZOMATO", ignoreCase = true) == true)
        assertEquals("XX1234", result.accountReference)
    }

    @Test
    fun testSbiCreditSms() {
        val body = "Your A/c *9876 is credited with Rs 50,000.00 on 01-AUG-26 by Salary transfer. Avail Bal Rs 62,500."
        val result = SmsParser.parse("VK-SBIINB", body)

        assertTrue(result.isFinancial)
        assertEquals(50000.0, result.amount, 0.001)
        assertEquals("income", result.type)
        assertEquals("Salary", result.suggestedCategory)
        assertEquals("XX9876", result.accountReference)
    }

    @Test
    fun testIciciUpiDebit() {
        val body = "Dear Customer, your Acct ending 4321 has been debited for INR 350.00 on 18-Aug-26 towards SWIGGY. UPI Ref: 623456789."
        val result = SmsParser.parse("BZ-ICICIB", body)

        assertTrue(result.isFinancial)
        assertEquals(350.0, result.amount, 0.001)
        assertEquals("expense", result.type)
        assertEquals("Food", result.suggestedCategory)
        assertEquals("XX4321", result.accountReference)
    }

    @Test
    fun testAxisUberTransport() {
        val body = "Alert: Rs.420.50 spent on your Axis Bank Card ending in 5566 at UBER TRIP on 15-Aug-26."
        val result = SmsParser.parse("AX-AXISBK", body)

        assertTrue(result.isFinancial)
        assertEquals(420.50, result.amount, 0.001)
        assertEquals("expense", result.type)
        assertEquals("Transport", result.suggestedCategory)
        assertEquals("XX5566", result.accountReference)
    }

    @Test
    fun testAmazonShopping() {
        val body = "Rs 2,999.00 paid to AMAZON PAY INDIA on 10-Aug-26 from a/c *8888. Ref no 123456"
        val result = SmsParser.parse("AD-HDFCBK", body)

        assertTrue(result.isFinancial)
        assertEquals(2999.0, result.amount, 0.001)
        assertEquals("expense", result.type)
        assertEquals("Shopping", result.suggestedCategory)
        assertEquals("XX8888", result.accountReference)
    }

    @Test
    fun testOtpMessageIsIgnored() {
        val body = "123456 is your secret OTP for transaction of Rs 500.00 on HDFC card. Do not share with anyone."
        val result = SmsParser.parse("AD-HDFCBK", body)

        assertFalse("OTP messages must not be classified as financial transactions", result.isFinancial)
    }

    @Test
    fun testSpamPromoIsIgnored() {
        val body = "Congratulations! You have won Rs 50,000 cashback! Claim your loan offer now: http://spam.link"
        val result = SmsParser.parse("VK-PROMO", body)

        assertFalse("Promotional messages must not be classified as financial transactions", result.isFinancial)
    }

    @Test
    fun testBillDueNoticeIsIgnored() {
        val body = "Your Credit Card Statement is ready. Total due Rs 12,450.00, Minimum due Rs 1,000. Due date 25-Aug."
        val result = SmsParser.parse("AD-HDFCBK", body)

        assertFalse("Bill statement reminders without debit should be ignored for expenses", result.isFinancial)
    }

    @Test
    fun testHdfcCreditCardStatementBill() {
        val body = "Your HDFC Bank Credit Card statement for Card ending 1234 is generated. Total Amt Due: Rs 14,500.00, Min Amt Due: Rs 1,500.00, Payment Due Date: 25-AUG-2026."
        val bill = SmsParser.parseCreditCardBill("AD-HDFCBK", body)

        assertTrue(bill.isBillStatement)
        assertEquals(14500.0, bill.totalDue, 0.001)
        assertEquals(1500.0, bill.minDue, 0.001)
        assertEquals("1234", bill.cardLast4)
        assertEquals("2026-08-25", bill.dueDate)
        assertEquals("HDFC Bank", bill.bankName)
    }

    @Test
    fun testSbiCreditCardStatementBill() {
        val body = "Dear Customer, SBI Card ending in 9876 Total Due: Rs 8,250.50, Min Due: Rs 500, Due Date: 20/09/2026. Pay now."
        val bill = SmsParser.parseCreditCardBill("VK-SBIINB", body)

        assertTrue(bill.isBillStatement)
        assertEquals(8250.50, bill.totalDue, 0.001)
        assertEquals(500.0, bill.minDue, 0.001)
        assertEquals("9876", bill.cardLast4)
        assertEquals("2026-09-20", bill.dueDate)
    }

    @Test
    fun testIciciCreditCardStatementBill() {
        val body = "ICICI Bank Credit Card XX4321 Statement: Total Due Rs. 22,100, Due Date: 02-Oct-2026. Pay now to avoid charges."
        val bill = SmsParser.parseCreditCardBill("BZ-ICICIB", body)

        assertTrue(bill.isBillStatement)
        assertEquals(22100.0, bill.totalDue, 0.001)
        assertEquals("4321", bill.cardLast4)
        assertEquals("2026-10-02", bill.dueDate)
    }
}

