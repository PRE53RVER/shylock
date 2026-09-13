package com.example.data.repository

import com.example.data.db.DetectedPaymentDao
import com.example.data.model.DetectedPayment
import kotlinx.coroutines.flow.Flow

class MoneyInboxRepository(private val dao: DetectedPaymentDao) {
    val pendingPayments: Flow<List<DetectedPayment>> = dao.getPendingPayments()
    val pendingCount: Flow<Int> = dao.getPendingCount()

    /**
     * Stores a freshly detected payment unless it is a duplicate. Returns the new row id, or
     * null when the payment was already known (same fingerprint, or an equivalent payment
     * within [SIMILAR_WINDOW_MS]).
     */
    suspend fun addDetectedPayment(payment: DetectedPayment): Long? {
        if (dao.getByFingerprint(payment.fingerprint) != null) return null
        val similar = dao.countSimilar(
            amount = payment.amount,
            direction = payment.direction,
            counterparty = payment.counterparty,
            from = payment.timestamp - SIMILAR_WINDOW_MS,
            to = payment.timestamp + SIMILAR_WINDOW_MS
        )
        if (similar > 0) return null
        val id = dao.insert(payment)
        return if (id > 0) id else null
    }

    suspend fun getById(id: Int): DetectedPayment? = dao.getById(id)

    suspend fun countPendingSince(since: Long): Int = dao.countPendingSince(since)

    suspend fun markRecorded(id: Int) = dao.updateStatus(id, DetectedPayment.STATUS_RECORDED)

    suspend fun markDismissed(id: Int) = dao.updateStatus(id, DetectedPayment.STATUS_DISMISSED)

    suspend fun restorePending(id: Int) = dao.updateStatus(id, DetectedPayment.STATUS_PENDING)

    /** Resolved drafts only exist to block duplicates; a month of history is plenty for that. */
    suspend fun pruneResolved(now: Long = System.currentTimeMillis()) =
        dao.pruneResolvedBefore(now - RESOLVED_RETENTION_MS)

    suspend fun clearAll() = dao.deleteAll()

    companion object {
        const val SIMILAR_WINDOW_MS = 3 * 60 * 1000L
        const val RESOLVED_RETENTION_MS = 30L * 24 * 60 * 60 * 1000L
    }
}
