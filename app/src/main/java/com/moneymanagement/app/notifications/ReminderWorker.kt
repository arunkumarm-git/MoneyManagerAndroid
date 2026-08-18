package com.moneymanagement.app.notifications

import android.content.Context
import androidx.work.Worker
import androidx.work.WorkerParameters

class ReminderWorker(context: Context, params: WorkerParameters) : Worker(context, params) {
    override fun doWork(): Result {
        val message = inputData.getString(KEY_MESSAGE) ?: DEFAULT_MESSAGE
        NotificationHelper.showReminder(applicationContext, message)
        return Result.success()
    }

    companion object {
        const val KEY_MESSAGE = "message"
        const val DEFAULT_MESSAGE = "Don't forget to log today's transactions."
    }
}
