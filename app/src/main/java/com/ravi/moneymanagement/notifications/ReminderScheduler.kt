package com.ravi.moneymanagement.notifications

import android.content.Context
import androidx.work.Data
import androidx.work.ExistingPeriodicWorkPolicy
import androidx.work.PeriodicWorkRequestBuilder
import androidx.work.WorkManager
import java.util.Calendar
import java.util.concurrent.TimeUnit

object ReminderScheduler {

    fun scheduleDailyReminders(context: Context) {
        schedule(
            context = context,
            uniqueName = "reminder_morning",
            hour = 9,
            minute = 0,
            message = "Good morning! Don't forget to log yesterday's transactions.",
        )
        schedule(
            context = context,
            uniqueName = "reminder_evening",
            hour = 20,
            minute = 0,
            message = "Quick check — did you log all of today's spending?",
        )
    }

    private fun schedule(context: Context, uniqueName: String, hour: Int, minute: Int, message: String) {
        val now = Calendar.getInstance()
        val target = Calendar.getInstance().apply {
            set(Calendar.HOUR_OF_DAY, hour)
            set(Calendar.MINUTE, minute)
            set(Calendar.SECOND, 0)
            set(Calendar.MILLISECOND, 0)
            if (before(now)) add(Calendar.DAY_OF_YEAR, 1)
        }
        val initialDelayMillis = target.timeInMillis - now.timeInMillis

        val inputData = Data.Builder().putString(ReminderWorker.KEY_MESSAGE, message).build()
        val request = PeriodicWorkRequestBuilder<ReminderWorker>(1, TimeUnit.DAYS)
            .setInitialDelay(initialDelayMillis, TimeUnit.MILLISECONDS)
            .setInputData(inputData)
            .build()

        WorkManager.getInstance(context).enqueueUniquePeriodicWork(
            uniqueName,
            ExistingPeriodicWorkPolicy.KEEP,
            request,
        )
    }
}
