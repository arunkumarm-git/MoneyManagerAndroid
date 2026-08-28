package com.moneymanagement.app.sms

import android.content.Context
import android.database.Cursor
import android.net.Uri
import android.provider.Telephony
import com.moneymanagement.app.data.MoneyRepository
import com.moneymanagement.app.data.SmsInbox
import com.moneymanagement.app.data.nowIso
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

data class SmsScanStats(
    val messagesScanned: Int = 0,
    val transactionsFound: Int = 0,
    val billsFound: Int = 0,
    val daysScanned: Int = 30,
)

object SmsSyncManager {

    /**
     * Scans the device SMS inbox for financial messages from the last [days] days.
     * Inserts newly parsed transactions into [SmsInbox] as PENDING for user review.
     * Returns detailed [SmsScanStats].
     */
    suspend fun scanHistoricalSmsDetailed(
        context: Context,
        repository: MoneyRepository,
        days: Int = 30,
    ): SmsScanStats = withContext(Dispatchers.IO) {
        val uri: Uri = Telephony.Sms.Inbox.CONTENT_URI
        val projection = arrayOf(
            Telephony.Sms.Inbox.ADDRESS,
            Telephony.Sms.Inbox.BODY,
            Telephony.Sms.Inbox.DATE,
        )

        val cutoffMillis = System.currentTimeMillis() - (days.toLong() * 24 * 60 * 60 * 1000)
        val selection = "${Telephony.Sms.Inbox.DATE} >= ?"
        val selectionArgs = arrayOf(cutoffMillis.toString())
        val sortOrder = "${Telephony.Sms.Inbox.DATE} DESC"

        var totalScanned = 0
        var importedTxns = 0
        var importedBills = 0

        try {
            val cursor: Cursor? = context.contentResolver.query(
                uri,
                projection,
                selection,
                selectionArgs,
                sortOrder,
            )

            cursor?.use { c ->
                val addressIdx = c.getColumnIndex(Telephony.Sms.Inbox.ADDRESS)
                val bodyIdx = c.getColumnIndex(Telephony.Sms.Inbox.BODY)
                val dateIdx = c.getColumnIndex(Telephony.Sms.Inbox.DATE)

                val accounts = repository.getAllAccounts()
                val categories = repository.getAllCategories()

                while (c.moveToNext()) {
                    val address = if (addressIdx >= 0) c.getString(addressIdx) ?: "" else ""
                    val body = if (bodyIdx >= 0) c.getString(bodyIdx) ?: "" else ""
                    val date = if (dateIdx >= 0) c.getLong(dateIdx) else System.currentTimeMillis()

                    if (body.isBlank()) continue
                    totalScanned++

                    val parsed = SmsParser.parse(address, body, date)
                    if (parsed.isFinancial) {
                        val isDuplicate = repository.isSmsDuplicate(parsed.smsHash)
                        if (!isDuplicate) {
                            val matchedAccount = accounts.find { acc ->
                                parsed.accountReference != null && acc.name.contains(parsed.accountReference.replace("XX", ""), ignoreCase = true)
                            } ?: accounts.find { acc ->
                                acc.type.equals("bank", ignoreCase = true) || acc.name.contains("bank", ignoreCase = true)
                            } ?: accounts.firstOrNull()

                            val matchedCategory = categories.find { cat ->
                                cat.name.equals(parsed.suggestedCategory, ignoreCase = true) &&
                                cat.type.equals(parsed.type, ignoreCase = true)
                            } ?: categories.find { cat ->
                                (cat.name.equals("Other", ignoreCase = true) || cat.name.equals("Others", ignoreCase = true) || cat.name.equals("Other Income", ignoreCase = true)) &&
                                cat.type.equals(parsed.type, ignoreCase = true)
                            } ?: categories.firstOrNull { it.type == parsed.type }

                            val draft = SmsInbox(
                                sender = address,
                                rawBody = body,
                                amount = parsed.amount,
                                type = parsed.type,
                                suggestedCategory = matchedCategory?.name ?: parsed.suggestedCategory,
                                suggestedAccountId = matchedAccount?.id,
                                merchantOrNote = parsed.merchantOrNote,
                                accountReference = parsed.accountReference,
                                txnDate = parsed.txnDate,
                                status = "PENDING",
                                smsHash = parsed.smsHash,
                                createdAt = nowIso(),
                            )

                            repository.addSmsInbox(draft)
                            importedTxns++
                        }
                    } else {
                        // Check if it's a Credit Card bill statement
                        val bill = SmsParser.parseCreditCardBill(address, body, date)
                        if (bill.isBillStatement) {
                            val existingCards = repository.getAllCreditCards()
                            val matchedCard = if (bill.cardLast4 != null) {
                                existingCards.find { it.lastFourDigits == bill.cardLast4 }
                            } else {
                                existingCards.find { it.bankName.equals(bill.bankName, ignoreCase = true) }
                            }

                            if (matchedCard != null) {
                                repository.updateCreditCardBillStatement(
                                    id = matchedCard.id,
                                    totalDue = bill.totalDue,
                                    minDue = bill.minDue,
                                    dueDate = bill.dueDate,
                                )
                                importedBills++
                            } else {
                                val cardName = if (bill.cardLast4 != null) "${bill.bankName} Card (..${bill.cardLast4})" else "${bill.bankName} Card"
                                repository.addCreditCard(
                                    cardName = cardName,
                                    bankName = bill.bankName,
                                    lastFourDigits = bill.cardLast4,
                                    creditLimit = (bill.totalDue * 3).coerceAtLeast(50000.0),
                                    currentBalance = bill.totalDue,
                                    totalDue = bill.totalDue,
                                    minDue = bill.minDue,
                                    dueDate = bill.dueDate,
                                )
                                importedBills++
                            }
                        }
                    }
                }
            }
        } catch (_: Exception) {
            // Content provider query failed or permissions denied
        }

        return@withContext SmsScanStats(
            messagesScanned = totalScanned,
            transactionsFound = importedTxns,
            billsFound = importedBills,
            daysScanned = days,
        )
    }

    /**
     * Backward-compatible helper returning imported transaction count.
     */
    suspend fun scanHistoricalSms(
        context: Context,
        repository: MoneyRepository,
        days: Int = 30,
    ): Int {
        val stats = scanHistoricalSmsDetailed(context, repository, days)
        return stats.transactionsFound + stats.billsFound
    }
}

