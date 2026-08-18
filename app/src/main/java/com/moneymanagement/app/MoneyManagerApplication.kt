package com.moneymanagement.app

import android.app.Application
import com.moneymanagement.app.data.AppDatabase
import com.moneymanagement.app.data.MoneyRepository
import com.moneymanagement.app.notifications.NotificationHelper
import com.moneymanagement.app.notifications.ReminderScheduler
import com.moneymanagement.app.settings.AppSettings
import com.moneymanagement.app.ui.common.ClickSound
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch

class MoneyManagerApplication : Application() {
    lateinit var repository: MoneyRepository
        private set

    private val applicationScope = CoroutineScope(SupervisorJob() + Dispatchers.IO)

    override fun onCreate() {
        super.onCreate()
        val db = AppDatabase.getInstance(this)
        repository = MoneyRepository(db)
        applicationScope.launch {
            repository.seedDefaultsIfNeeded()
            repository.processDueRecurringTransactions()
        }

        NotificationHelper.createChannel(this)
        ReminderScheduler.scheduleDailyReminders(this)

        AppSettings.init(this)
        ClickSound.init(this)
    }
}
