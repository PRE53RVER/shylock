package com.example.data.model

import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey

/**
 * A money movement spotted in a bank / payment-app notification, parked in Money Inbox until the
 * user records or dismisses it. Nothing here touches the ledger: a draft only becomes a
 * [Transaction] when the user confirms it through the normal add-record flow.
 *
 * Rows are kept after the user acts on them (status flips to RECORDED / DISMISSED) so the
 * fingerprint keeps blocking the same notification from resurfacing as a new draft.
 */
@Entity(
    tableName = "detected_payments",
    indices = [Index(value = ["fingerprint"], unique = true)]
)
data class DetectedPayment(
    @PrimaryKey(autoGenerate = true) val id: Int = 0,
    val amount: Double,
    val direction: String,          // DIRECTION_SENT or DIRECTION_RECEIVED
    val counterparty: String? = null, // person / merchant when the notification named one
    val source: String,             // friendly app or bank name, e.g. "HDFC Bank"
    val sourcePackage: String,      // package that posted the notification
    val rawText: String,            // original notification text, for the review sheet
    val fingerprint: String,        // dedupe key derived from the normalised text
    val timestamp: Long = System.currentTimeMillis(),
    val status: String = STATUS_PENDING
) {
    val isSent: Boolean get() = direction == DIRECTION_SENT

    /** The ledger type this draft maps to when recorded. */
    val transactionType: String get() = if (isSent) "EXPENSE" else "INCOME"

    companion object {
        const val DIRECTION_SENT = "SENT"
        const val DIRECTION_RECEIVED = "RECEIVED"

        const val STATUS_PENDING = "PENDING"
        const val STATUS_RECORDED = "RECORDED"
        const val STATUS_DISMISSED = "DISMISSED"
    }
}
