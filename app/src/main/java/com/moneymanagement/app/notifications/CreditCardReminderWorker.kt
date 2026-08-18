package com.moneymanagement.app.notifications

import android.content.Context
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import com.moneymanagement.app.MoneyManagerApplication
import com.moneymanagement.app.settings.AppSettings
import java.time.LocalDate
import java.time.temporal.ChronoUnit

class CreditCardReminderWorker(
    private val context: Context,
    params: WorkerParameters,
) : CoroutineWorker(context, params) {

    override suspend fun doWork(): Result {
        val app = context.applicationContext as? MoneyManagerApplication ?: return Result.success()
        val repository = app.repository
        AppSettings.init(context)

        try {
            val pendingCards = repository.getPendingCreditCardBills()
            val today = LocalDate.now()
            val currency = AppSettings.currencySymbol.value

            for (card in pendingCards) {
                val dueStr = card.dueDate ?: continue
                val dueDate = runCatching { LocalDate.parse(dueStr) }.getOrNull() ?: continue
                val daysUntil = ChronoUnit.DAYS.between(today, dueDate)

                when {
                    daysUntil == 3L -> {
                        NotificationHelper.showSmsNotification(
                            context,
                            "Credit Card Due in 3 Days",
                            "${card.cardName}: $currency ${card.totalDue} is due on $dueStr. Pay early to avoid late fees.",
                        )
                    }
                    daysUntil == 1L -> {
                        NotificationHelper.showSmsNotification(
                            context,
                            "⚠️ Credit Card Due Tomorrow",
                            "Action Required: ${card.cardName} bill of $currency ${card.totalDue} is due tomorrow ($dueStr)!",
                        )
                    }
                    daysUntil == 0L -> {
                        NotificationHelper.showSmsNotification(
                            context,
                            "🚨 Credit Card Bill Due Today!",
                            "Urgent: Pay ${card.cardName} bill of $currency ${card.totalDue} today to avoid interest charges.",
                        )
                    }
                    daysUntil < 0L -> {
                        NotificationHelper.showSmsNotification(
                            context,
                            "⛔ Overdue Credit Card Bill",
                            "${card.cardName} bill of $currency ${card.totalDue} is overdue since $dueStr! Please pay immediately.",
                        )
                    }
                }

                // Check high credit utilization (> 70%)
                if (card.creditLimit > 0 && (card.currentBalance / card.creditLimit) >= 0.70) {
                    val pct = ((card.currentBalance / card.creditLimit) * 100).toInt()
                    NotificationHelper.showSmsNotification(
                        context,
                        "High Credit Card Utilization ($pct%)",
                        "${card.cardName} usage is at $pct% of limit ($currency ${card.creditLimit}). Keeping it under 30% helps your credit score.",
                    )
                }
            }
        } catch (_: Exception) {
            // Fail safely
        }

        return Result.success()
    }
}
