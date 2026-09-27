package com.example

import com.example.data.model.DetectedPayment
import com.example.inbox.PaymentNotificationParser
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Test

class PaymentNotificationParserTest {

    private val gpay = "com.google.android.apps.nbu.paisa.user"
    private val hdfc = "com.snapwork.hdfc"
    private val sms = "com.google.android.apps.messaging"

    @Test
    fun upiSentPaymentIsDraftedAsExpense() {
        val parsed = PaymentNotificationParser.parse(gpay, "Payment sent", "You paid ₹500 to Rahul via UPI")
        assertNotNull(parsed)
        assertEquals(500.0, parsed!!.amount, 0.001)
        assertEquals(DetectedPayment.DIRECTION_SENT, parsed.direction)
        assertEquals("Rahul", parsed.counterparty)
    }

    @Test
    fun upiReceivedPaymentIsDraftedAsIncome() {
        val parsed = PaymentNotificationParser.parse(gpay, "Money received", "₹2,000 received from Anu")
        assertNotNull(parsed)
        assertEquals(2000.0, parsed!!.amount, 0.001)
        assertEquals(DetectedPayment.DIRECTION_RECEIVED, parsed.direction)
        assertEquals("Anu", parsed.counterparty)
    }

    @Test
    fun bankDebitAlertParsesAmountWithGroupingAndMerchant() {
        val parsed = PaymentNotificationParser.parse(
            hdfc, null,
            "Rs.1,250.50 debited from a/c XX1234 on 12-09-26 to SWIGGY. Avl bal Rs.10,000. Ref 123456"
        )
        assertNotNull(parsed)
        assertEquals(1250.50, parsed!!.amount, 0.001)
        assertEquals(DetectedPayment.DIRECTION_SENT, parsed.direction)
        assertEquals("Swiggy", parsed.counterparty)
    }

    @Test
    fun bankCreditAlertOverSmsRequiresBankHint() {
        val bankStyle = PaymentNotificationParser.parse(
            sms, "AX-HDFCBK", "INR 50,000 credited to A/c XX9876 on 01-09-26 by NEFT from TCS LTD. Ref UTR123"
        )
        assertNotNull(bankStyle)
        assertEquals(50000.0, bankStyle!!.amount, 0.001)
        assertEquals(DetectedPayment.DIRECTION_RECEIVED, bankStyle.direction)

        // A chat message that happens to mention money is not a bank alert
        val chat = PaymentNotificationParser.parse(sms, "Mom", "I sent you Rs 500 for lunch")
        assertNull(chat)
    }

    @Test
    fun otpAndPromoNotificationsAreIgnored() {
        assertNull(PaymentNotificationParser.parse(hdfc, null, "123456 is your OTP for payment of Rs.500 to Amazon"))
        assertNull(PaymentNotificationParser.parse(gpay, "Offer", "Get cashback of up to ₹100 on your next payment"))
        assertNull(PaymentNotificationParser.parse(hdfc, null, "Your EMI of Rs.5,000 will be debited on 05-10-26"))
        assertNull(PaymentNotificationParser.parse(gpay, "Payment failed", "₹300 to Rahul could not be completed"))
    }

    @Test
    fun unsupportedPackageAndMissingAmountAreRejected() {
        assertEquals(false, PaymentNotificationParser.isSupportedPackage("com.example.random"))
        assertNull(PaymentNotificationParser.parse(gpay, "Payment sent", "You paid Rahul"))
        assertNull(PaymentNotificationParser.parse(gpay, "Reminder", "Your bill is due"))
    }

    @Test
    fun upiHandleCounterpartyIsTrimmedToOwner() {
        val parsed = PaymentNotificationParser.parse(gpay, null, "₹150 sent to swiggy@okhdfcbank via UPI")
        assertEquals("swiggy", parsed?.counterparty)
    }

    @Test
    fun fingerprintIgnoresClockTimeAndReferenceButNotAmount() {
        val a = PaymentNotificationParser.fingerprint(gpay, "You paid ₹500 to Rahul at 7:42 pm Ref 123")
        val b = PaymentNotificationParser.fingerprint(gpay, "You paid ₹500 to Rahul at 7:43 pm Ref 456")
        val c = PaymentNotificationParser.fingerprint(gpay, "You paid ₹600 to Rahul at 7:42 pm Ref 123")
        val d = PaymentNotificationParser.fingerprint(hdfc, "You paid ₹500 to Rahul at 7:42 pm Ref 123")
        assertEquals(a, b)
        assertNotEquals(a, c)
        assertNotEquals(a, d)
    }

    @Test
    fun directionPrefersWhicheverKeywordAppearsFirst() {
        assertEquals(
            DetectedPayment.DIRECTION_SENT,
            PaymentNotificationParser.detectDirection("rs.500 debited from a/c and credited to beneficiary")
        )
        assertEquals(
            DetectedPayment.DIRECTION_RECEIVED,
            PaymentNotificationParser.detectDirection("rs.500 credited to a/c by transfer from rahul")
        )
    }

    @Test
    fun detectedPaymentMapsDirectionToLedgerType() {
        val sent = DetectedPayment(amount = 1.0, direction = DetectedPayment.DIRECTION_SENT, source = "GPay", sourcePackage = gpay, rawText = "", fingerprint = "a")
        val received = sent.copy(direction = DetectedPayment.DIRECTION_RECEIVED, fingerprint = "b")
        assertEquals("EXPENSE", sent.transactionType)
        assertEquals("INCOME", received.transactionType)
    }
}
