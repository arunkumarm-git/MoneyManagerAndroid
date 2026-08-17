package com.ravi.moneymanagement

import android.app.Application
import com.ravi.moneymanagement.data.AppDatabase
import com.ravi.moneymanagement.data.MoneyRepository
import com.ravi.moneymanagement.notifications.NotificationHelper
import com.ravi.moneymanagement.notifications.ReminderScheduler
import com.ravi.moneymanagement.settings.AppSettings
import com.ravi.moneymanagement.ui.common.ClickSound
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
