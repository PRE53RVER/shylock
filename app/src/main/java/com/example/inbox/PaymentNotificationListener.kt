package com.example.inbox

import android.app.Notification
import android.service.notification.NotificationListenerService
import android.service.notification.StatusBarNotification
import com.example.data.db.AppDatabase
import com.example.data.model.DetectedPayment
import com.example.data.repository.MoneyInboxRepository
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.launch

/**
 * Watches notifications from supported bank / UPI apps and turns the ones that describe a
 * completed payment into Money Inbox drafts. Nothing is recorded automatically; the draft waits
 * for the user to Record, Add to Income or Dismiss it.
 */
class PaymentNotificationListener : NotificationListenerService() {

    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)
    private val repository by lazy { MoneyInboxRepository(AppDatabase.getDatabase(applicationContext).detectedPaymentDao()) }

    // Once access is granted, sweep what is already in the shade so recent payments show up
    // straight away instead of only the next one
    override fun onListenerConnected() {
        super.onListenerConnected()
        if (!MoneyInboxSettings.isDetectionEnabled(this)) return
        val active = try { activeNotifications } catch (_: SecurityException) { null } ?: return
        active.forEach { handle(it, alert = false) }
    }

    override fun onNotificationPosted(sbn: StatusBarNotification?) {
        handle(sbn ?: return, alert = true)
    }

    private fun handle(notification: StatusBarNotification, alert: Boolean) {
        if (!MoneyInboxSettings.isDetectionEnabled(this)) return
        val pkg = notification.packageName ?: return
        if (!PaymentNotificationParser.isSupportedPackage(pkg)) return
        // Group summaries repeat the children and would double-count
        if (notification.notification.flags and Notification.FLAG_GROUP_SUMMARY != 0) return

        val extras = notification.notification.extras ?: return
        val title = extras.getCharSequence(Notification.EXTRA_TITLE)?.toString()
        val text = extras.getCharSequence(Notification.EXTRA_BIG_TEXT)?.toString()
            ?: extras.getCharSequence(Notification.EXTRA_TEXT)?.toString()
        val parsed = PaymentNotificationParser.parse(pkg, title, text) ?: return

        val rawText = listOfNotNull(title, text).joinToString(" — ")
        val payment = DetectedPayment(
            amount = parsed.amount,
            direction = parsed.direction,
            counterparty = parsed.counterparty,
            source = PaymentNotificationParser.sourceLabel(pkg),
            sourcePackage = pkg,
            rawText = rawText,
            fingerprint = parsed.fingerprint,
            timestamp = notification.postTime.takeIf { it > 0 } ?: System.currentTimeMillis()
        )

        scope.launch {
            val id = repository.addDetectedPayment(payment) ?: return@launch
            repository.pruneResolved()
            if (alert && MoneyInboxSettings.isAlertsEnabled(this@PaymentNotificationListener)) {
                val currency = MoneyInboxSettings.prefs(this@PaymentNotificationListener).getString("currency", "₹") ?: "₹"
                MoneyInboxNotifications.showPaymentDetected(
                    this@PaymentNotificationListener,
                    payment.copy(id = id.toInt()),
                    currency
                )
            }
        }
    }

    override fun onDestroy() {
        scope.cancel()
        super.onDestroy()
    }
}
