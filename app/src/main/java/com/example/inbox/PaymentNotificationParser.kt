package com.example.inbox

import com.example.data.model.DetectedPayment
import java.security.MessageDigest
import java.util.Locale

/**
 * What the parser could pull out of one notification. [counterparty] is only set when the text
 * clearly named a person or merchant; the rest is always present.
 */
data class ParsedPayment(
    val amount: Double,
    val direction: String,
    val counterparty: String?,
    val fingerprint: String
)

/**
 * Pure text parser for bank / UPI notifications. No Android dependencies so it can be unit
 * tested against real-world notification strings.
 *
 * Supported packages map to a friendly source label. Messaging apps are included because most
 * Indian banks alert by SMS, but those notifications carry a lot of unrelated chatter so they
 * must also look like a bank alert (see [looksLikeBankAlert]) before we trust them.
 */
object PaymentNotificationParser {

    val SUPPORTED_SOURCES: Map<String, String> = mapOf(
        // UPI & wallets
        "com.google.android.apps.nbu.paisa.user" to "GPay",
        "com.phonepe.app" to "PhonePe",
        "net.one97.paytm" to "Paytm",
        "in.org.npci.upiapp" to "BHIM",
        "in.amazon.mShop.android.shopping" to "Amazon Pay",
        "com.dreamplug.androidapp" to "CRED",
        "com.mobikwik_new" to "MobiKwik",
        "com.freecharge.android" to "Freecharge",
        "com.whatsapp" to "WhatsApp Pay",
        // Banks
        "com.snapwork.hdfc" to "HDFC Bank",
        "com.hdfcbank.payzapp" to "HDFC Bank",
        "com.csam.icici.bank.imobile" to "ICICI Bank",
        "com.sbi.lotusintouch" to "SBI",
        "com.sbi.SBIFreedomPlus" to "SBI",
        "com.axis.mobile" to "Axis Bank",
        "com.msf.kbank.mobile" to "Kotak Bank",
        "com.infrasofttech.indianBank" to "Indian Bank",
        "com.fss.pnbpsp" to "PNB",
        "com.bankofbaroda.mconnect" to "Bank of Baroda",
        "com.canarabank.mobility" to "Canara Bank",
        "com.idfcfirstbank.optimus" to "IDFC First Bank",
        "com.indusind.indusmobile" to "IndusInd Bank",
        "com.fedmobile" to "Federal Bank",
        "com.yesbank" to "YES Bank",
        "com.rblbank.mobank" to "RBL Bank",
        "com.aufin.aubank" to "AU Bank",
        "in.jupiter.app" to "Jupiter",
        "money.jupiter" to "Jupiter",
        "com.fi.money" to "Fi",
        // SMS apps (bank alerts arrive here)
        "com.google.android.apps.messaging" to "SMS",
        "com.samsung.android.messaging" to "SMS",
        "com.android.mms" to "SMS",
        "com.oneplus.mms" to "SMS",
        "com.miui.mms" to "SMS",
        "com.truecaller" to "SMS"
    )

    private val SMS_PACKAGES = SUPPORTED_SOURCES.filterValues { it == "SMS" }.keys

    fun isSupportedPackage(packageName: String): Boolean = packageName in SUPPORTED_SOURCES

    fun sourceLabel(packageName: String): String = SUPPORTED_SOURCES[packageName] ?: packageName

    // Amount: ₹500 / Rs. 1,250.50 / INR 2000 / Rs500
    private val AMOUNT_REGEX = Regex(
        """(?:₹|rs\.?|inr)\s*([0-9]{1,3}(?:,[0-9]{2,3})*(?:\.[0-9]{1,2})?|[0-9]+(?:\.[0-9]{1,2})?)""",
        RegexOption.IGNORE_CASE
    )

    private val SENT_KEYWORDS = listOf(
        "debited", "sent", "paid", "payment of", "spent", "withdrawn", "purchase", "transferred to",
        "you paid", "paid to", "sent to", "debit"
    )
    private val RECEIVED_KEYWORDS = listOf(
        "credited", "received", "deposited", "added to", "refund", "cashback", "credit", "transferred from"
    )

    // "OTP", "will be debited", "due" etc. describe money that has not moved
    private val NOT_A_PAYMENT = listOf(
        "otp", "one time password", "will be debited", "will be credited", "is due", "due on",
        "reminder", "request", "requested", "declined", "failed", "unsuccessful", "could not",
        "limit", "offer", "win ", "cashback of up to", "upto", "up to", "apply now", "loan of",
        "pre-approved", "emi of", "statement"
    )

    private val BANK_HINTS = listOf(
        "a/c", "ac ", "acct", "account", "bank", "upi", "vpa", "card", "ref no", "ref:", "txn", "imps", "neft", "rtgs"
    )

    // Tokens that end a name capture: connective words, brackets, punctuation, end of text
    private const val NAME_END = """(?=\s+(?:on|via|using|for|from|to|ref|upi|is|has|was|-)\b|\s*[(.,;!]|\s*$)"""
    private const val NAME_CHARS = """([A-Za-z0-9][A-Za-z0-9 .&'@_-]{1,40}?)"""

    // Counterparty patterns, tried in order. Group 1 is the name.
    private val COUNTERPARTY_REGEXES = listOf(
        Regex("""(?:sent|paid|transferred|payment)(?:[^.]{0,30}?)\s+to\s+$NAME_CHARS$NAME_END""", RegexOption.IGNORE_CASE),
        Regex("""(?:received|credited|deposited)(?:[^.]{0,30}?)\s+from\s+$NAME_CHARS$NAME_END""", RegexOption.IGNORE_CASE),
        Regex("""\bat\s+$NAME_CHARS$NAME_END""", RegexOption.IGNORE_CASE),
        Regex("""\bto\s+$NAME_CHARS$NAME_END""", RegexOption.IGNORE_CASE),
        Regex("""\bfrom\s+$NAME_CHARS$NAME_END""", RegexOption.IGNORE_CASE)
    )

    // Words that follow "to"/"from" but are not names
    private val COUNTERPARTY_STOPWORDS = setOf(
        "your", "you", "the", "a", "an", "account", "a/c", "acct", "bank", "upi", "card", "wallet", "review", "avl", "bal"
    )

    /**
     * Parses a notification. Returns null when the text does not describe a completed money
     * movement with a recognisable amount.
     */
    fun parse(packageName: String, title: String?, text: String?): ParsedPayment? {
        val combined = listOfNotNull(title, text).joinToString(" ").replace('\n', ' ').trim()
        if (combined.isBlank()) return null
        val lower = combined.lowercase(Locale.ROOT)

        if (NOT_A_PAYMENT.any { lower.contains(it) }) return null
        if (packageName in SMS_PACKAGES && !looksLikeBankAlert(lower)) return null

        val direction = detectDirection(lower) ?: return null
        val amount = extractAmount(combined) ?: return null
        if (amount <= 0.0) return null

        val counterparty = extractCounterparty(combined)
        return ParsedPayment(
            amount = amount,
            direction = direction,
            counterparty = counterparty,
            fingerprint = fingerprint(packageName, combined)
        )
    }

    fun looksLikeBankAlert(lowerText: String): Boolean = BANK_HINTS.any { lowerText.contains(it) }

    fun detectDirection(lowerText: String): String? {
        val sentIdx = SENT_KEYWORDS.mapNotNull { k -> lowerText.indexOf(k).takeIf { it >= 0 } }.minOrNull()
        val recvIdx = RECEIVED_KEYWORDS.mapNotNull { k -> lowerText.indexOf(k).takeIf { it >= 0 } }.minOrNull()
        return when {
            sentIdx == null && recvIdx == null -> null
            recvIdx == null -> DetectedPayment.DIRECTION_SENT
            sentIdx == null -> DetectedPayment.DIRECTION_RECEIVED
            // Both present (e.g. "debited ... credited to beneficiary"): whichever comes first wins
            sentIdx <= recvIdx -> DetectedPayment.DIRECTION_SENT
            else -> DetectedPayment.DIRECTION_RECEIVED
        }
    }

    fun extractAmount(text: String): Double? {
        val match = AMOUNT_REGEX.find(text) ?: return null
        return match.groupValues[1].replace(",", "").toDoubleOrNull()
    }

    fun extractCounterparty(text: String): String? {
        for (regex in COUNTERPARTY_REGEXES) {
            val raw = regex.find(text)?.groupValues?.get(1)?.trim() ?: continue
            val cleaned = cleanCounterparty(raw) ?: continue
            return cleaned
        }
        return null
    }

    private fun cleanCounterparty(raw: String): String? {
        var name = raw.trim().trimEnd('.', ',', ';', '-')
        // UPI ids read better as the handle owner: "rahul@okhdfc" -> "rahul"
        if (name.contains('@')) name = name.substringBefore('@')
        // Drop masked account fragments like "XX1234"
        name = name.replace(Regex("""\b[Xx*]{2,}\d+\b"""), "").trim()
        val words = name.split(Regex("\\s+")).filter { it.isNotBlank() }
        if (words.isEmpty()) return null
        if (words.first().lowercase(Locale.ROOT) in COUNTERPARTY_STOPWORDS) return null
        if (name.length < 2 || name.all { it.isDigit() }) return null
        // Title-case shouty bank text ("SWIGGY" -> "Swiggy") but leave mixed case alone
        return if (name == name.uppercase(Locale.ROOT) && name.length > 3) {
            words.joinToString(" ") { w -> w.lowercase(Locale.ROOT).replaceFirstChar { it.titlecase(Locale.ROOT) } }
        } else name
    }

    /**
     * Stable key for "the same notification". Times and reference numbers are stripped so a
     * re-post with an updated clock or the bank's own retry does not look new, while amount,
     * direction and counterparty stay in so genuinely different payments never collide.
     */
    fun fingerprint(packageName: String, text: String): String {
        val normalised = text.lowercase(Locale.ROOT)
            .replace(Regex("""\b\d{1,2}[:.]\d{2}(?:\s*[ap]m)?\b"""), "")        // clock times
            .replace(Regex("""\b(?:ref|txn|utr|upi ref)[\s:#.-]*\w+"""), "")   // reference ids
            .replace(Regex("""\s+"""), " ")
            .trim()
        return sha1("$packageName|$normalised")
    }

    private fun sha1(input: String): String {
        val digest = MessageDigest.getInstance("SHA-1").digest(input.toByteArray())
        return digest.joinToString("") { "%02x".format(it) }
    }
}
