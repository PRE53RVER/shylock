package com.example.inbox

import android.content.ComponentName
import android.content.Context
import android.content.SharedPreferences
import android.provider.Settings

/**
 * Money Inbox preferences. Lives outside the ViewModel because the notification listener and
 * the reminder receiver both need to read these without an Activity around.
 */
object MoneyInboxSettings {
    const val PREFS_NAME = "shylock_settings"
    const val KEY_DETECTION_ENABLED = "payment_detection_enabled"
    const val KEY_ALERTS_ENABLED = "payment_alerts_enabled"
    const val KEY_DAILY_REMINDER_ENABLED = "daily_review_reminder_enabled"

    fun prefs(context: Context): SharedPreferences =
        context.applicationContext.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)

    fun isDetectionEnabled(context: Context): Boolean =
        prefs(context).getBoolean(KEY_DETECTION_ENABLED, false)

    fun isAlertsEnabled(context: Context): Boolean =
        prefs(context).getBoolean(KEY_ALERTS_ENABLED, true)

    fun isDailyReminderEnabled(context: Context): Boolean =
        prefs(context).getBoolean(KEY_DAILY_REMINDER_ENABLED, false)

    /** True when the user has granted SHYLOCK notification access in system settings. */
    fun hasNotificationAccess(context: Context): Boolean {
        val enabled = Settings.Secure.getString(
            context.contentResolver,
            "enabled_notification_listeners"
        ) ?: return false
        val component = ComponentName(context, PaymentNotificationListener::class.java)
        return enabled.split(':').any { entry ->
            entry == component.flattenToString() || entry == component.flattenToShortString()
        }
    }
}
