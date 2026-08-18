package com.moneymanagement.app.data.backup

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Test

class CsvHelperTest {

    @Test
    fun testSampleCsvContentParsesSuccessfully() {
        val preview = CsvHelper.generateImportPreview(CsvHelper.SAMPLE_CSV_CONTENT)

        assertEquals(4, preview.totalRows)
        assertEquals(4, preview.validRows.size)
        assertEquals(2, preview.expenseCount)
        assertEquals(1, preview.incomeCount)
        assertEquals(1, preview.transferCount)
        assertEquals(370.50, preview.totalExpenseAmount, 0.001)
        assertEquals(50000.00, preview.totalIncomeAmount, 0.001)
        assertEquals(0, preview.skippedRowsCount)
    }

    @Test
    fun testFlexibleHeaderMapping() {
        val customCsv = """
            Amount,Date,Txn_Type,Category,Account,Description
            1500.00,2026-08-18,Expense,Groceries,HDFC Bank,Supermarket bill
            3000.00,2026-08-19,Credit,Bonus,SBI,Performance bonus
        """.trimIndent()

        val preview = CsvHelper.generateImportPreview(customCsv)

        assertEquals(2, preview.totalRows)
        assertEquals(2, preview.validRows.size)
        assertEquals(1, preview.expenseCount)
        assertEquals(1, preview.incomeCount)

        val first = preview.validRows[0]
        assertEquals("2026-08-18", first.txnDate)
        assertEquals(1500.00, first.amount, 0.001)
        assertEquals("expense", first.type)
        assertEquals("Groceries", first.categoryName)
        assertEquals("HDFC Bank", first.accountName)
        assertEquals("Supermarket bill", first.note)

        val second = preview.validRows[1]
        assertEquals("2026-08-19", second.txnDate)
        assertEquals(3000.00, second.amount, 0.001)
        assertEquals("income", second.type)
    }

    @Test
    fun testSkippedRowsDetected() {
        val faultyCsv = """
            Date,Type,Amount,Category,Account,ToAccount,Note
            2026-08-18,expense,250.00,Food,Cash,,Valid row
            ,expense,100.00,Food,Cash,,Missing date
            2026-08-19,expense,0.0,Food,Cash,,Zero amount
            2026-08-20,expense,invalid_num,Food,Cash,,Invalid amount text
        """.trimIndent()

        val preview = CsvHelper.generateImportPreview(faultyCsv)

        assertEquals(4, preview.totalRows)
        assertEquals(1, preview.validRows.size)
        assertEquals(3, preview.skippedRowsCount)
        assertEquals(250.00, preview.totalExpenseAmount, 0.001)
    }

    @Test
    fun testMaxFileSizeConstantIsFiveMb() {
        assertEquals(5 * 1024 * 1024L, CsvHelper.MAX_FILE_SIZE_BYTES)
        assertEquals("5 MB", CsvHelper.MAX_FILE_SIZE_MB_DISPLAY)
    }
}
