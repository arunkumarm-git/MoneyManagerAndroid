package com.moneymanagement.app.sms

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.provider.Telephony
import com.moneymanagement.app.MoneyManagerApplication
import com.moneymanagement.app.data.SmsInbox
import com.moneymanagement.app.data.nowIso
import com.moneymanagement.app.notifications.NotificationHelper
import com.moneymanagement.app.settings.AppSettings
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

class SmsBroadcastReceiver : BroadcastReceiver() {

    override fun onReceive(context: Context, intent: Intent?) {
        if (intent?.action != Telephony.Sms.Intents.SMS_RECEIVED_ACTION) return

        // Ensure AppSettings is initialized
        AppSettings.init(context)
        if (!AppSettings.smsTrackingEnabled.value) return

        val messages = Telephony.Sms.Intents.getMessagesFromIntent(intent)
        if (messages.isNullOrEmpty()) return

        val app = context.applicationContext as? MoneyManagerApplication ?: return
        val repository = app.repository

        val pendingResult = goAsync()

        CoroutineScope(Dispatchers.IO).launch {
            try {
                // Combine multipart SMS segments from same sender
                val sender = messages[0].originatingAddress ?: "Unknown"
                val body = messages.joinToString("") { it.messageBody ?: "" }
                val timestamp = messages[0].timestampMillis

                val parsed = SmsParser.parse(sender, body, timestamp)
                if (parsed.isFinancial) {
                    val isDuplicate = repository.isSmsDuplicate(parsed.smsHash)
                    if (!isDuplicate) {
                        val accounts = repository.getAllAccounts()
                        val categories = repository.getAllCategories()

                        // Match suggested account or fallback to bank account (or first account)
                        val matchedAccount = accounts.find { acc ->
                            parsed.accountReference != null && acc.name.contains(parsed.accountReference.replace("XX", ""), ignoreCase = true)
                        } ?: accounts.find { acc ->
                            acc.type.equals("bank", ignoreCase = true) || acc.name.contains("bank", ignoreCase = true)
                        } ?: accounts.firstOrNull()

                        // Match suggested category or fallback to Other
                        val matchedCategory = categories.find { cat ->
                            cat.name.equals(parsed.suggestedCategory, ignoreCase = true) &&
                            cat.type.equals(parsed.type, ignoreCase = true)
                        } ?: categories.find { cat ->
                            (cat.name.equals("Other", ignoreCase = true) || cat.name.equals("Others", ignoreCase = true) || cat.name.equals("Other Income", ignoreCase = true)) &&
                            cat.type.equals(parsed.type, ignoreCase = true)
                        } ?: categories.firstOrNull { it.type == parsed.type }

                        val autoApprove = AppSettings.autoApproveSms.value

                        if (autoApprove && matchedAccount != null) {
                            // Instant auto-insertion
                            val smsInbox = SmsInbox(
                                sender = sender,
                                rawBody = body,
                                amount = parsed.amount,
                                type = parsed.type,
                                suggestedCategory = matchedCategory?.name ?: parsed.suggestedCategory,
                                suggestedAccountId = matchedAccount.id,
                                merchantOrNote = parsed.merchantOrNote,
                                accountReference = parsed.accountReference,
                                txnDate = parsed.txnDate,
                                status = "APPROVED",
                                smsHash = parsed.smsHash,
                                createdAt = nowIso(),
                            )
                            repository.addSmsInbox(smsInbox)

                            repository.addTransaction(
                                accountId = matchedAccount.id,
                                type = parsed.type,
                                amount = parsed.amount,
                                txnDate = parsed.txnDate,
                                categoryId = matchedCategory?.id,
                                toAccountId = null,
                                note = parsed.merchantOrNote ?: "SMS Auto-tracked",
                            )

                            val currency = AppSettings.currencySymbol.value
                            NotificationHelper.showSmsNotification(
                                context,
                                "Transaction Auto-Recorded",
                                "$currency ${parsed.amount} (${parsed.merchantOrNote ?: parsed.type}) added to ${matchedAccount.name}",
                            )
                        } else {
                            // Queue to Pending Review Inbox
                            val smsInbox = SmsInbox(
                                sender = sender,
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
                            repository.addSmsInbox(smsInbox)

                            val currency = AppSettings.currencySymbol.value
                            NotificationHelper.showSmsNotification(
                                context,
                                "New Transaction Detected",
                                "Detected $currency ${parsed.amount} at ${parsed.merchantOrNote ?: "merchant"}. Tap to review.",
                            )
                        }
                    }
                } else {
                    // Check if it's a Credit Card Bill Statement
                    val bill = SmsParser.parseCreditCardBill(sender, body, timestamp)
                    if (bill.isBillStatement) {
                        val existingCards = repository.getAllCreditCards()
                        val matchedCard = if (bill.cardLast4 != null) {
                            existingCards.find { it.lastFourDigits == bill.cardLast4 }
                        } else {
                            existingCards.find { it.bankName.equals(bill.bankName, ignoreCase = true) }
                        }

                        val currency = AppSettings.currencySymbol.value
                        if (matchedCard != null) {
                            repository.updateCreditCardBillStatement(
                                id = matchedCard.id,
                                totalDue = bill.totalDue,
                                minDue = bill.minDue,
                                dueDate = bill.dueDate,
                            )
                            NotificationHelper.showSmsNotification(
                                context,
                                "Credit Card Bill Alert",
                                "${matchedCard.cardName}: $currency ${bill.totalDue} due on ${bill.dueDate ?: "upcoming due date"}.",
                            )
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
                            NotificationHelper.showSmsNotification(
                                context,
                                "Credit Card Bill Detected",
                                "Detected $cardName: $currency ${bill.totalDue} due on ${bill.dueDate ?: "due date"}.",
                            )
                        }
                    }
                }
            } catch (_: Exception) {
                // Fail gracefully
            } finally {
                pendingResult.finish()
            }

        }
    }
}
