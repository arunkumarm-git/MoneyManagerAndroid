package com.ravi.moneymanagement.data.backup

import com.ravi.moneymanagement.data.TransactionRow

object CsvHelper {
    private const val HEADER = "Date,Type,Amount,Category,Account,ToAccount,Note"

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

    data class ParsedCsvRow(
        val txnDate: String,
        val type: String,
        val amount: Double,
        val categoryName: String,
        val accountName: String,
        val toAccountName: String,
        val note: String,
    )

    fun parseCsv(csvContent: String): List<ParsedCsvRow> {
        val lines = csvContent.lines().filter { it.isNotBlank() }
        if (lines.isEmpty()) return emptyList()

        val results = mutableListOf<ParsedCsvRow>()
        val startIndex = if (lines[0].startsWith("Date", ignoreCase = true)) 1 else 0

        for (i in startIndex until lines.size) {
            val tokens = parseCsvLine(lines[i])
            if (tokens.size >= 5) {
                val date = tokens.getOrElse(0) { "" }.trim()
                val type = tokens.getOrElse(1) { "expense" }.trim().lowercase()
                val amount = tokens.getOrElse(2) { "0" }.trim().toDoubleOrNull() ?: 0.0
                val categoryName = tokens.getOrElse(3) { "" }.trim()
                val accountName = tokens.getOrElse(4) { "" }.trim()
                val toAccountName = tokens.getOrElse(5) { "" }.trim()
                val note = tokens.getOrElse(6) { "" }.trim()

                if (amount > 0.0 && date.isNotBlank()) {
                    results.add(
                        ParsedCsvRow(
                            txnDate = date,
                            type = type,
                            amount = amount,
                            categoryName = categoryName,
                            accountName = accountName,
                            toAccountName = toAccountName,
                            note = note,
                        )
                    )
                }
            }
        }
        return results
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
