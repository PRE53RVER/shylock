package com.example.inbox

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import com.example.data.db.AppDatabase
import com.example.data.repository.MoneyInboxRepository
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

/**
 * Fires at 9 PM (via [MoneyInboxNotifications.syncDailyReminder]) and nudges the user only when
 * payments detected today are still pending. Also re-arms the alarm after a reboot, since
 * AlarmManager schedules do not survive one.
 */
class DailyReviewReceiver : BroadcastReceiver() {

    override fun onReceive(context: Context, intent: Intent) {
        when (intent.action) {
            Intent.ACTION_BOOT_COMPLETED, Intent.ACTION_MY_PACKAGE_REPLACED -> {
                MoneyInboxNotifications.syncDailyReminder(context)
            }
            ACTION_DAILY_REVIEW -> {
                if (!MoneyInboxSettings.isDailyReminderEnabled(context)) return
                val pending = goAsync()
                val appContext = context.applicationContext
                CoroutineScope(Dispatchers.IO).launch {
                    try {
                        val repository = MoneyInboxRepository(AppDatabase.getDatabase(appContext).detectedPaymentDao())
                        val count = repository.countPendingSince(MoneyInboxNotifications.startOfToday())
                        MoneyInboxNotifications.showDailyReminder(appContext, count)
                    } finally {
                        pending.finish()
                    }
                }
            }
        }
    }

    companion object {
        const val ACTION_DAILY_REVIEW = "com.example.action.DAILY_REVIEW"
    }
}
