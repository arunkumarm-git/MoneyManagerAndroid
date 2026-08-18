package com.moneymanagement.app.sms

import java.security.MessageDigest
import java.time.Instant
import java.time.LocalDateTime
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import java.util.Locale
import java.util.regex.Pattern

data class ParsedSmsResult(
    val isFinancial: Boolean,
    val amount: Double = 0.0,
    val type: String = "expense", // "expense" or "income"
    val merchantOrNote: String? = null,
    val accountReference: String? = null,
    val suggestedCategory: String? = null,
    val txnDate: String = "",
    val smsHash: String = "",
)

data class ParsedCreditCardBill(
    val isBillStatement: Boolean,
    val cardLast4: String? = null,
    val bankName: String = "",
    val totalDue: Double = 0.0,
    val minDue: Double = 0.0,
    val dueDate: String? = null, // "YYYY-MM-DD"
    val rawBody: String = "",
    val smsHash: String = "",
)


object SmsParser {

    // Non-financial / OTP patterns to strictly discard
    private val OTP_OR_SPAM_PATTERNS = listOf(
        Pattern.compile("(?i)\\b(otp|one\\s*time\\s*password|verification\\s*code|secret\\s*code|security\\s*code)\\b"),
        Pattern.compile("(?i)\\b(do\\s*not\\s*share|never\\s*share|valid\\s*for\\s*\\d+\\s*(mins|min|seconds|sec))\\b"),
        Pattern.compile("(?i)\\b(congratulations|congrats|win\\b|winner|claim\\s*your|loan\\s*approved|pre-approved|apply\\s*now|flat\\s*\\d+%\\s*off)\\b"),
        Pattern.compile("(?i)\\b(due\\s*date|bill\\s*generated|statement\\s*is\\s*ready|total\\s*due|minimum\\s*due)\\b"), // Bill notices (not actual charge yet)
    )

    // Expense indicator words
    private val EXPENSE_PATTERNS = listOf(
        Pattern.compile("(?i)\\b(debited\\s*(?:by|with|for)?|spent|paid\\s*(?:to)?|sent\\s*(?:to)?|transferred\\s*(?:to)?|withdrawn|purchase(?:d)?\\s*at|charged|pos\\s*txn|txn\\s*of)\\b"),
    )

    // Income indicator words
    private val INCOME_PATTERNS = listOf(
        Pattern.compile("(?i)\\b(credited\\s*(?:by|with|to)?|received\\s*(?:from)?|deposited\\s*(?:in|to)?|refund(?:ed)?|cashback\\s*(?:of|received)?|salary\\s*(?:credited)?)\\b"),
    )

    // Amount extraction regexes
    private val AMOUNT_PATTERNS = listOf(
        // e.g., "Rs. 1,499.00", "INR 250.50", "₹ 500", "$ 45.00", "USD 120", "EUR 30"
        Pattern.compile("(?i)(?:rs\\.?|inr|₹|\\$|usd|eur|€)\\s*([0-9]{1,3}(?:,[0-9]{2,3})*(?:\\.[0-9]{1,2})?|[0-9]+(?:\\.[0-9]{1,2})?)"),
        // e.g., "1,499.00 INR", "500.00 Rs"
        Pattern.compile("(?i)([0-9]{1,3}(?:,[0-9]{2,3})*(?:\\.[0-9]{1,2})?|[0-9]+(?:\\.[0-9]{1,2})?)\\s*(?:rs\\.?|inr|₹|\\$|usd|eur|€)"),
        // e.g. "debited with 500.00"
        Pattern.compile("(?i)(?:debited\\s*(?:by|with)?|spent|credited\\s*(?:by|with)?)\\s*([0-9]{1,3}(?:,[0-9]{2,3})*(?:\\.[0-9]{1,2})?|[0-9]+(?:\\.[0-9]{1,2})?)"),
    )

    // Account/Card reference extraction
    private val ACCOUNT_PATTERNS = listOf(
        Pattern.compile("(?i)(?:a\\/c|acct|account|card|acc)\\s*(?:no\\.?)?\\s*[*xX]*([0-9]{3,4})\\b"),
        Pattern.compile("(?i)(?:ending\\s*(?:with)?|ending\\s*in)\\s*[*xX]*([0-9]{3,4})\\b"),
        Pattern.compile("(?i)[*xX]+([0-9]{4})\\b"),
    )

    // Merchant / Payee extraction
    private val MERCHANT_PATTERNS = listOf(
        // "at STARBUCKS on", "to JOHN DOE on", "paid to SWIGGY ref"
        Pattern.compile("(?i)(?:paid\\s*to|sent\\s*to|transferred\\s*to|spent\\s*at|at|to)\\s+([A-Za-z0-9\\s\\.\\-_&@]{2,30}?)(?=\\s+on|\\s+ref|\\s+via|\\s+using|\\s+upi|\\s+avail|\\s+bal|\\s+a\\/c|\\.|\\,|$|\\n)"),
        // UPI payee extraction like "UPI/123456/MERCHANT NAME" or "VPA user@upi"
        Pattern.compile("(?i)(?:upi\\/[0-9]+\\/|vpa\\s+)([A-Za-z0-9\\.\\-_@]+)"),
        Pattern.compile("(?i)(?:info\\/|towards\\s+)([A-Za-z0-9\\s\\.\\-_]{2,25})"),
    )

    // Category auto-inference mapping
    private val CATEGORY_RULES = listOf(
        "Food" to listOf(
            "swiggy", "zomato", "starbucks", "mcdonald", "kfc", "burger", "pizza", "domino",
            "cafe", "restaurant", "dining", "dine", "eats", "blinkit", "zepto", "instamart",
            "bigbasket", "grocery", "supermarket", "dmart", "spencer", "bakery", "food",
        ),
        "Transport" to listOf(
            "uber", "ola", "rapido", "metro", "fuel", "petrol", "diesel", "shell", "hpcl",
            "bpcl", "iocl", "irctc", "redbus", "makemytrip", "flight", "indigo", "airindia",
            "toll", "fastag", "parking", "auto", "cab", "travel",
        ),
        "Shopping" to listOf(
            "amazon", "flipkart", "myntra", "zara", "h&m", "ajio", "nykaa", "reliance", "croma",
            "apple", "decathlon", "meesho", "retail", "clothing", "apparel", "mall", "store",
        ),
        "Bills" to listOf(
            "electricity", "water", "broadband", "wifi", "airtel", "jio", "vi", "vodafone",
            "bescom", "tata play", "gas", "recharge", "billdesk", "utility", "postpaid",
        ),
        "Entertainment" to listOf(
            "netflix", "spotify", "prime video", "hotstar", "bookmyshow", "cinema", "pvr",
            "inox", "youtube", "steam", "sonyliv", "movie", "playstation",
        ),
        "Health" to listOf(
            "apollo", "netmeds", "pharmeasy", "1mg", "hospital", "pharmacy", "clinic",
            "doctor", "medplus", "dental", "healthcare", "diagnostics", "lab",
        ),
        "Salary" to listOf(
            "salary", "payroll", "stipend", "bonus", "wages",
        ),
    )

    /**
     * Parses an incoming or historical SMS message.
     */
    fun parse(sender: String, body: String, timestampMillis: Long = System.currentTimeMillis()): ParsedSmsResult {
        val trimmedBody = body.trim()
        val normalizedBody = trimmedBody.replace("\\s+".toRegex(), " ")

        // Generate consistent unique hash
        val smsHash = generateHash("$sender:$normalizedBody:${timestampMillis / 60000}") // minute-bucketed to avoid sub-second drift

        // 1. Filter out OTP / Spam / Promotional SMS
        for (pattern in OTP_OR_SPAM_PATTERNS) {
            if (pattern.matcher(normalizedBody).find()) {
                return ParsedSmsResult(isFinancial = false, smsHash = smsHash)
            }
        }

        // 2. Determine Transaction Type (Debit/Expense vs Credit/Income)
        var isExpense = false
        var isIncome = false

        for (pattern in EXPENSE_PATTERNS) {
            if (pattern.matcher(normalizedBody).find()) {
                isExpense = true
                break
            }
        }

        for (pattern in INCOME_PATTERNS) {
            if (pattern.matcher(normalizedBody).find()) {
                isIncome = true
                break
            }
        }

        // If neither expense nor income keyword matched, it is not a direct financial transaction
        if (!isExpense && !isIncome) {
            return ParsedSmsResult(isFinancial = false, smsHash = smsHash)
        }

        // 3. Extract Amount
        var amount: Double? = null
        for (pattern in AMOUNT_PATTERNS) {
            val matcher = pattern.matcher(normalizedBody)
            if (matcher.find()) {
                val rawAmt = matcher.group(1)?.replace(",", "")
                amount = rawAmt?.toDoubleOrNull()
                if (amount != null && amount > 0.0) break
            }
        }

        // If no valid positive amount found, not financial
        if (amount == null || amount <= 0.0) {
            return ParsedSmsResult(isFinancial = false, smsHash = smsHash)
        }

        val type = if (isIncome && !isExpense) "income" else "expense"

        // 4. Extract Account / Card reference
        var accountRef: String? = null
        for (pattern in ACCOUNT_PATTERNS) {
            val matcher = pattern.matcher(normalizedBody)
            if (matcher.find()) {
                accountRef = "XX" + (matcher.group(1) ?: "")
                break
            }
        }

        // 5. Extract Merchant / Note
        var merchant: String? = null
        for (pattern in MERCHANT_PATTERNS) {
            val matcher = pattern.matcher(normalizedBody)
            if (matcher.find()) {
                val candidate = matcher.group(1)?.trim()?.replace("[.,;]$".toRegex(), "")
                if (!candidate.isNullOrBlank() && !candidate.equals("your", ignoreCase = true) && !candidate.equals("a/c", ignoreCase = true)) {
                    merchant = candidate
                    break
                }
            }
        }

        // If no merchant extracted, fallback to sender header
        val cleanSender = cleanSenderHeader(sender)
        val note = if (!merchant.isNullOrBlank()) merchant else "SMS Txn ($cleanSender)"

        // 6. Infer Category from merchant/body
        val suggestedCategory = inferCategory(merchant ?: normalizedBody, type)

        // 7. Format Date
        val txnDate = try {
            val ldt = LocalDateTime.ofInstant(Instant.ofEpochMilli(timestampMillis), ZoneId.systemDefault())
            ldt.withNano(0).format(DateTimeFormatter.ISO_LOCAL_DATE_TIME)
        } catch (_: Exception) {
            LocalDateTime.now().withNano(0).toString()
        }

        return ParsedSmsResult(
            isFinancial = true,
            amount = amount,
            type = type,
            merchantOrNote = note,
            accountReference = accountRef,
            suggestedCategory = suggestedCategory,
            txnDate = txnDate,
            smsHash = smsHash,
        )
    }

    private fun inferCategory(text: String, type: String): String? {
        val lower = text.lowercase(Locale.getDefault())
        if (type == "income") {
            if (lower.contains("salary") || lower.contains("payroll") || lower.contains("stipend")) {
                return "Salary"
            }
            return "Other Income"
        }

        for ((category, keywords) in CATEGORY_RULES) {
            for (kw in keywords) {
                if (lower.contains(kw)) {
                    return category
                }
            }
        }
        return null
    }

    private fun cleanSenderHeader(sender: String): String {
        // e.g. "AD-HDFCBK" -> "HDFC Bank", "VK-SBIINB" -> "SBI"
        val raw = sender.substringAfter("-").uppercase(Locale.getDefault())
        return when {
            raw.contains("HDFC") -> "HDFC Bank"
            raw.contains("SBI") -> "State Bank of India"
            raw.contains("ICICI") -> "ICICI Bank"
            raw.contains("AXIS") -> "Axis Bank"
            raw.contains("KOTAK") -> "Kotak Bank"
            raw.contains("CITI") -> "Citi Bank"
            raw.contains("CHASE") -> "Chase Bank"
            raw.contains("PAYTM") -> "Paytm Bank"
            raw.contains("AMEX") -> "American Express"
            raw.isNotBlank() -> raw
            else -> sender
        }
    }

    private fun generateHash(input: String): String {
        val bytes = MessageDigest.getInstance("SHA-256").digest(input.toByteArray(Charsets.UTF_8))
        return bytes.joinToString("") { "%02x".format(it) }
    }

    // Bill Statement Detection Regex Patterns
    private val BILL_TOTAL_DUE_PATTERNS = listOf(
        Pattern.compile("(?i)(?:total\\s*(?:amount|amt)?\\s*due|tot\\s*due|t\\.?\\s*due|amt\\s*due|balance\\s*due)\\s*(?:is|:|=|of)?\\s*(?:rs\\.?|inr|₹|\\$|usd)?\\s*([0-9]{1,3}(?:,[0-9]{2,3})*(?:\\.[0-9]{1,2})?|[0-9]+(?:\\.[0-9]{1,2})?)"),
        Pattern.compile("(?i)(?:rs\\.?|inr|₹|\\$)\\s*([0-9]{1,3}(?:,[0-9]{2,3})*(?:\\.[0-9]{1,2})?|[0-9]+(?:\\.[0-9]{1,2})?)\\s*(?:is\\s*the\\s*)?(?:total\\s*(?:amount|amt)?\\s*due)"),
    )

    private val BILL_MIN_DUE_PATTERNS = listOf(
        Pattern.compile("(?i)(?:min(?:imum)?\\s*(?:amount|amt)?\\s*due|min\\s*due|m\\.?\\s*due)\\s*(?:is|:|=|of)?\\s*(?:rs\\.?|inr|₹|\\$|usd)?\\s*([0-9]{1,3}(?:,[0-9]{2,3})*(?:\\.[0-9]{1,2})?|[0-9]+(?:\\.[0-9]{1,2})?)"),
    )

    private val BILL_DUE_DATE_PATTERNS = listOf(
        Pattern.compile("(?i)(?:payment\\s*due\\s*date|pay\\s*(?:by|before)|due\\s*(?:date|on|by)|due\\s*before)\\s*(?:is|:|=|on)?\\s*([0-9]{1,2}[-\\/\\s][A-Za-z0-9]{2,4}[-\\/\\s][0-9]{2,4}|[0-9]{1,2}\\s+[A-Za-z]{3,9}(?:\\s+[0-9]{2,4})?)"),
    )

    /**
     * Checks if an incoming or historical SMS is a Credit Card Bill Statement.
     */
    fun parseCreditCardBill(sender: String, body: String, timestampMillis: Long = System.currentTimeMillis()): ParsedCreditCardBill {
        val trimmedBody = body.trim()
        val normalizedBody = trimmedBody.replace("\\s+".toRegex(), " ")

        // Discard OTPs or Promo Spam
        for (pattern in OTP_OR_SPAM_PATTERNS.take(3)) {
            if (pattern.matcher(normalizedBody).find()) {
                return ParsedCreditCardBill(isBillStatement = false)
            }
        }

        val isCreditCardStatement = normalizedBody.contains("credit card", ignoreCase = true) ||
                normalizedBody.contains("card statement", ignoreCase = true) ||
                normalizedBody.contains("statement is generated", ignoreCase = true) ||
                normalizedBody.contains("total due", ignoreCase = true) ||
                normalizedBody.contains("payment due date", ignoreCase = true)

        if (!isCreditCardStatement) {
            return ParsedCreditCardBill(isBillStatement = false)
        }

        // 1. Extract Total Due
        var totalDue: Double? = null
        for (pattern in BILL_TOTAL_DUE_PATTERNS) {
            val matcher = pattern.matcher(normalizedBody)
            if (matcher.find()) {
                val rawAmt = matcher.group(1)?.replace(",", "")
                totalDue = rawAmt?.toDoubleOrNull()
                if (totalDue != null && totalDue > 0.0) break
            }
        }

        if (totalDue == null || totalDue <= 0.0) {
            return ParsedCreditCardBill(isBillStatement = false)
        }

        // 2. Extract Min Due (optional)
        var minDue = 0.0
        for (pattern in BILL_MIN_DUE_PATTERNS) {
            val matcher = pattern.matcher(normalizedBody)
            if (matcher.find()) {
                val rawAmt = matcher.group(1)?.replace(",", "")
                minDue = rawAmt?.toDoubleOrNull() ?: 0.0
                if (minDue > 0.0) break
            }
        }

        // 3. Extract Card last 4
        var cardLast4: String? = null
        for (pattern in ACCOUNT_PATTERNS) {
            val matcher = pattern.matcher(normalizedBody)
            if (matcher.find()) {
                cardLast4 = matcher.group(1)
                break
            }
        }

        // 4. Extract Due Date
        var dueDateStr: String? = null
        for (pattern in BILL_DUE_DATE_PATTERNS) {
            val matcher = pattern.matcher(normalizedBody)
            if (matcher.find()) {
                val candidate = matcher.group(1)?.trim()
                dueDateStr = normalizeDueDate(candidate)
                if (dueDateStr != null) break
            }
        }

        val bankName = cleanSenderHeader(sender)
        val smsHash = generateHash("bill:$sender:$normalizedBody:${timestampMillis / 60000}")

        return ParsedCreditCardBill(
            isBillStatement = true,
            cardLast4 = cardLast4,
            bankName = bankName,
            totalDue = totalDue,
            minDue = minDue,
            dueDate = dueDateStr,
            rawBody = trimmedBody,
            smsHash = smsHash,
        )
    }

    private fun normalizeDueDate(rawDate: String?): String? {
        if (rawDate.isNullOrBlank()) return null
        val cleaned = rawDate.trim().replace(",", "").replace(".", "-").replace("/", "-")
        val parts = cleaned.split("-", " ")
        if (parts.size < 2) return null

        val currentYear = java.time.LocalDate.now().year
        val day = parts[0].toIntOrNull() ?: return null

        var month: Int? = parts[1].toIntOrNull()
        if (month == null) {
            val mStr = parts[1].lowercase(Locale.getDefault())
            month = when {
                mStr.startsWith("jan") -> 1
                mStr.startsWith("feb") -> 2
                mStr.startsWith("mar") -> 3
                mStr.startsWith("apr") -> 4
                mStr.startsWith("may") -> 5
                mStr.startsWith("jun") -> 6
                mStr.startsWith("jul") -> 7
                mStr.startsWith("aug") -> 8
                mStr.startsWith("sep") -> 9
                mStr.startsWith("oct") -> 10
                mStr.startsWith("nov") -> 11
                mStr.startsWith("dec") -> 12
                else -> null
            }
        }
        if (month == null || month !in 1..12) return null

        var year = if (parts.size >= 3) parts[2].toIntOrNull() ?: currentYear else currentYear
        if (year < 100) year += 2000

        return try {
            java.time.LocalDate.of(year, month, day.coerceIn(1, 31)).toString()
        } catch (_: Exception) {
            null
        }
    }
}

