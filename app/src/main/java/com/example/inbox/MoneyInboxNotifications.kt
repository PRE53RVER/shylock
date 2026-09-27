package com.example.inbox

import android.Manifest
import android.app.AlarmManager
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import androidx.core.content.ContextCompat
import com.example.MainActivity
import com.example.R
import com.example.data.model.DetectedPayment
import java.text.NumberFormat
import java.util.Calendar
import java.util.Locale

/**
 * Posts the two Money Inbox notifications and owns the 9 PM review alarm. Tapping either
 * notification opens the app straight on the Money Inbox tab.
 */
object MoneyInboxNotifications {
    const val CHANNEL_ALERTS = "money_inbox_alerts"
    const val CHANNEL_REMINDERS = "money_inbox_reminders"
    const val EXTRA_OPEN_INBOX = "open_money_inbox"

    private const val ALERT_NOTIFICATION_BASE_ID = 4000
    private const val REMINDER_NOTIFICATION_ID = 4999
    private const val REMINDER_REQUEST_CODE = 9021
    const val REMINDER_HOUR = 21

    fun ensureChannels(context: Context) {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.O) return
        val manager = context.getSystemService(NotificationManager::class.java) ?: return
        manager.createNotificationChannel(
            NotificationChannel(CHANNEL_ALERTS, "Payment alerts", NotificationManager.IMPORTANCE_DEFAULT).apply {
                description = "A new payment was detected and is waiting in Money Inbox"
            }
        )
        manager.createNotificationChannel(
            NotificationChannel(CHANNEL_REMINDERS, "Daily review reminder", NotificationManager.IMPORTANCE_DEFAULT).apply {
                description = "Evening nudge when payments are still waiting to be recorded"
            }
        )
    }

    fun canPostNotifications(context: Context): Boolean {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            val granted = ContextCompat.checkSelfPermission(context, Manifest.permission.POST_NOTIFICATIONS)
            if (granted != PackageManager.PERMISSION_GRANTED) return false
        }
        return NotificationManagerCompat.from(context).areNotificationsEnabled()
    }

    /** "Payment detected — ₹500 was debited from your account. Tap to review." */
    fun showPaymentDetected(context: Context, payment: DetectedPayment, currencySymbol: String) {
        if (!canPostNotifications(context)) return
        ensureChannels(context)
        val amount = formatAmount(payment.amount, currencySymbol)
        val verb = if (payment.isSent) "debited from" else "credited to"
        val body = "$amount was $verb your account. Tap to review."
        val notification = NotificationCompat.Builder(context, CHANNEL_ALERTS)
            .setSmallIcon(R.drawable.ic_stat_money_inbox)
            .setContentTitle("Payment detected")
            .setContentText(body)
            .setStyle(NotificationCompat.BigTextStyle().bigText(body))
            .setContentIntent(openInboxIntent(context, payment.id))
            .setAutoCancel(true)
            .setPriority(NotificationCompat.PRIORITY_DEFAULT)
            .build()
        try {
            NotificationManagerCompat.from(context).notify(ALERT_NOTIFICATION_BASE_ID + (payment.id % 500), notification)
        } catch (_: SecurityException) {
            // Permission revoked between the check and the post; nothing to do
        }
    }

    /** "You have 3 payments waiting to be recorded today." */
    fun showDailyReminder(context: Context, pendingToday: Int) {
        if (pendingToday <= 0 || !canPostNotifications(context)) return
        ensureChannels(context)
        val noun = if (pendingToday == 1) "payment" else "payments"
        val body = "You have $pendingToday $noun waiting to be recorded today."
        val notification = NotificationCompat.Builder(context, CHANNEL_REMINDERS)
            .setSmallIcon(R.drawable.ic_stat_money_inbox)
            .setContentTitle("Money Inbox")
            .setContentText(body)
            .setStyle(NotificationCompat.BigTextStyle().bigText(body))
            .setContentIntent(openInboxIntent(context, 0))
            .setAutoCancel(true)
            .setPriority(NotificationCompat.PRIORITY_DEFAULT)
            .build()
        try {
            NotificationManagerCompat.from(context).notify(REMINDER_NOTIFICATION_ID, notification)
        } catch (_: SecurityException) {
        }
    }

    private fun openInboxIntent(context: Context, requestCode: Int): PendingIntent {
        val intent = Intent(context, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_SINGLE_TOP or Intent.FLAG_ACTIVITY_CLEAR_TOP
            putExtra(EXTRA_OPEN_INBOX, true)
        }
        return PendingIntent.getActivity(
            context,
            requestCode,
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )
    }

    // ---- Daily reminder alarm ----

    /** Schedules (or cancels) the inexact 9 PM daily check. Safe to call repeatedly. */
    fun syncDailyReminder(context: Context) {
        val alarmManager = context.getSystemService(Context.ALARM_SERVICE) as? AlarmManager ?: return
        val pending = reminderIntent(context)
        if (!MoneyInboxSettings.isDailyReminderEnabled(context)) {
            alarmManager.cancel(pending)
            return
        }
        val next = Calendar.getInstance().apply {
            set(Calendar.HOUR_OF_DAY, REMINDER_HOUR)
            set(Calendar.MINUTE, 0)
            set(Calendar.SECOND, 0)
            set(Calendar.MILLISECOND, 0)
            if (timeInMillis <= System.currentTimeMillis()) add(Calendar.DAY_OF_YEAR, 1)
        }
        // Inexact is fine for a nudge and avoids the exact-alarm permission on Android 12+
        alarmManager.setInexactRepeating(
            AlarmManager.RTC_WAKEUP,
            next.timeInMillis,
            AlarmManager.INTERVAL_DAY,
            pending
        )
    }

    private fun reminderIntent(context: Context): PendingIntent {
        val intent = Intent(context, DailyReviewReceiver::class.java).apply {
            action = DailyReviewReceiver.ACTION_DAILY_REVIEW
        }
        return PendingIntent.getBroadcast(
            context,
            REMINDER_REQUEST_CODE,
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )
    }

    /** Midnight of today in the device zone, i.e. the start of "today's" payments. */
    fun startOfToday(): Long = Calendar.getInstance().apply {
        set(Calendar.HOUR_OF_DAY, 0)
        set(Calendar.MINUTE, 0)
        set(Calendar.SECOND, 0)
        set(Calendar.MILLISECOND, 0)
    }.timeInMillis

    private fun formatAmount(amount: Double, symbol: String): String {
        val formatter = NumberFormat.getNumberInstance(Locale("en", "IN")).apply {
            maximumFractionDigits = if (amount % 1.0 == 0.0) 0 else 2
            minimumFractionDigits = 0
        }
        return "$symbol${formatter.format(amount)}"
    }
}
