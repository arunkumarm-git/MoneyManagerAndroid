package com.moneymanagement.app.data.backup

import com.moneymanagement.app.data.TransactionRow

object CsvHelper {
    const val MAX_FILE_SIZE_BYTES = 5 * 1024 * 1024L // 5 MB
    const val MAX_FILE_SIZE_MB_DISPLAY = "5 MB"

    const val HEADER = "Date,Type,Amount,Category,Account,ToAccount,Note"

    val SAMPLE_CSV_CONTENT = """
        Date,Type,Amount,Category,Account,ToAccount,Note
        2026-08-18,expense,250.00,Food,Bank Account,,Lunch with colleagues
        2026-08-18,income,50000.00,Salary,Bank Account,,Monthly salary
        2026-08-18,transfer,5000.00,,Bank Account,Cash,ATM cash withdrawal
        2026-08-19,expense,120.50,Transport,Credit Card,,Metro pass recharge
    """.trimIndent()

    data class ParsedCsvRow(
        val txnDate: String,
        val type: String,
        val amount: Double,
        val categoryName: String,
        val accountName: String,
        val toAccountName: String,
        val note: String,
    )

    data class CsvImportPreview(
        val totalRows: Int,
        val validRows: List<ParsedCsvRow>,
        val expenseCount: Int,
        val incomeCount: Int,
        val transferCount: Int,
        val totalExpenseAmount: Double,
        val totalIncomeAmount: Double,
        val skippedRowsCount: Int,
        val sampleRows: List<ParsedCsvRow>,
    )

    fun exportToCsv(transactions: List<TransactionRow>): String {
        val sb = StringBuilder()
        sb.append(HEADER).append("\n")
        for (row in transactions) {
            val date = escapeCsv(row.txn.txnDate)
            val type = escapeCsv(row.txn.type)
            val amount = row.txn.amount.toString()
            val category = escapeCsv(row.categoryName ?: "")
            val account = escapeCsv(row.accountName ?: "")
            val toAccount = escapeCsv(row.toAccountName ?: "")
            val note = escapeCsv(row.txn.note ?: "")
            sb.append("$date,$type,$amount,$category,$account,$toAccount,$note\n")
        }
        return sb.toString()
    }

    fun parseCsv(csvContent: String): List<ParsedCsvRow> {
        return generateImportPreview(csvContent).validRows
    }

    fun generateImportPreview(csvContent: String): CsvImportPreview {
        val lines = csvContent.lines().filter { it.isNotBlank() }
        if (lines.isEmpty()) {
            return CsvImportPreview(
                totalRows = 0,
                validRows = emptyList(),
                expenseCount = 0,
                incomeCount = 0,
                transferCount = 0,
                totalExpenseAmount = 0.0,
                totalIncomeAmount = 0.0,
                skippedRowsCount = 0,
                sampleRows = emptyList(),
            )
        }

        var startIndex = 0

        val firstLineTokens = parseCsvLine(lines[0])
        val isFirstLineHeader = firstLineTokens.any { token ->

            val t = token.trim().lowercase()
            t == "date" || t == "amount" || t == "type" || t == "category" || t == "account"
        }

        var dateIdx = 0
        var typeIdx = 1
        var amountIdx = 2
        var categoryIdx = 3
        var accountIdx = 4
        var toAccountIdx = 5
        var noteIdx = 6

        if (isFirstLineHeader) {
            startIndex = 1
            val headerTokens = firstLineTokens.map { it.trim().lowercase() }
            for (idx in headerTokens.indices) {
                val h = headerTokens[idx]

                when {
                    h in listOf("date", "time", "timestamp", "txn_date", "txndate", "transaction_date") -> dateIdx = idx
                    h in listOf("type", "txn_type", "txntype", "transaction_type", "dr_cr") -> typeIdx = idx
                    h in listOf("amount", "amt", "cost", "value", "total") -> amountIdx = idx
                    h in listOf("category", "cat", "category_name") -> categoryIdx = idx
                    h in listOf("account", "acc", "account_name", "from_account", "source_account") -> accountIdx = idx
                    h in listOf("to_account", "toaccount", "destination_account", "transfer_to") -> toAccountIdx = idx
                    h in listOf("note", "notes", "description", "desc", "remarks", "merchant", "payee") -> noteIdx = idx
                }
            }
        }

        val validRows = mutableListOf<ParsedCsvRow>()
        var skippedCount = 0
        var expenseSum = 0.0
        var incomeSum = 0.0
        var expenseCount = 0
        var incomeCount = 0
        var transferCount = 0

        for (i in startIndex until lines.size) {
            val tokens = parseCsvLine(lines[i])
            if (tokens.isEmpty()) {
                skippedCount++
                continue
            }

            val rawDate = tokens.getOrNull(dateIdx)?.trim() ?: ""
            val rawType = tokens.getOrNull(typeIdx)?.trim()?.lowercase() ?: "expense"
            val rawAmount = tokens.getOrNull(amountIdx)?.trim()?.replace(",", "")?.toDoubleOrNull()
            val categoryName = tokens.getOrNull(categoryIdx)?.trim() ?: ""
            val accountName = tokens.getOrNull(accountIdx)?.trim() ?: ""
            val toAccountName = tokens.getOrNull(toAccountIdx)?.trim() ?: ""
            val note = tokens.getOrNull(noteIdx)?.trim() ?: ""

            val normalizedType = when {
                rawType.contains("income") || rawType.contains("credit") || rawType == "cr" -> "income"
                rawType.contains("transfer") -> "transfer"
                else -> "expense"
            }

            if (rawAmount != null && rawAmount > 0.0 && rawDate.isNotBlank()) {
                val row = ParsedCsvRow(
                    txnDate = rawDate,
                    type = normalizedType,
                    amount = rawAmount,
                    categoryName = categoryName,
                    accountName = accountName,
                    toAccountName = toAccountName,
                    note = note,
                )
                validRows.add(row)

                when (normalizedType) {
                    "income" -> {
                        incomeCount++
                        incomeSum += rawAmount
                    }
                    "transfer" -> {
                        transferCount++
                    }
                    else -> {
                        expenseCount++
                        expenseSum += rawAmount
                    }
                }
            } else {
                skippedCount++
            }
        }

        return CsvImportPreview(
            totalRows = (lines.size - startIndex),
            validRows = validRows,
            expenseCount = expenseCount,
            incomeCount = incomeCount,
            transferCount = transferCount,
            totalExpenseAmount = expenseSum,
            totalIncomeAmount = incomeSum,
            skippedRowsCount = skippedCount,
            sampleRows = validRows.take(3),
        )
    }

    private fun escapeCsv(value: String): String {
        return if (value.contains(",") || value.contains("\"") || value.contains("\n")) {
            "\"" + value.replace("\"", "\"\"") + "\""
        } else {
            value
        }
    }

    private fun parseCsvLine(line: String): List<String> {
        val tokens = mutableListOf<String>()
        val sb = StringBuilder()
        var insideQuote = false
        var i = 0
        while (i < line.length) {
            val c = line[i]
            if (c == '"') {
                if (insideQuote && i + 1 < line.length && line[i + 1] == '"') {
                    sb.append('"')
                    i++
                } else {
                    insideQuote = !insideQuote
                }
            } else if (c == ',' && !insideQuote) {
                tokens.add(sb.toString())
                sb.clear()
            } else {
                sb.append(c)
            }
            i++
        }
        tokens.add(sb.toString())
        return tokens
    }
}
