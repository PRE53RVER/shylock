package com.example.data.model

/**
 * What a category is for, beyond its EXPENSE / INCOME side. Only STANDARD categories count towards
 * spending and income; the others are money that moves around without being earned or used up.
 */
object CategoryRole {
    const val STANDARD = "STANDARD"
    /** The ledger's "Lending" / "Loan Repayment" pair: money lent out and coming back. */
    const val LENDING = "LENDING"
    /** Own-account transfers, investments... anything the user keeps out of totals. */
    const val TRANSFER = "TRANSFER"
}

/** Transaction.type values. A REFUND points at the EXPENSE category it gives money back to. */
object TxType {
    const val EXPENSE = "EXPENSE"
    const val INCOME = "INCOME"
    const val REFUND = "REFUND"
}

val Category.isLending: Boolean get() = role == CategoryRole.LENDING
val Category.isTransfer: Boolean get() = role == CategoryRole.TRANSFER
/** Lending and transfer categories are bookkeeping, not spending or income. */
val Category.countsInTotals: Boolean get() = role == CategoryRole.STANDARD

val Transaction.isRefund: Boolean get() = type == TxType.REFUND
/** Money coming in: income and refunds show with a plus sign. */
val Transaction.isInflow: Boolean get() = type == TxType.INCOME || type == TxType.REFUND

enum class FlowKind { SPEND, INCOME, REFUND, LENT, LEND_REPAID, TRANSFER }

/** Classifies a transaction by what it did to the user's money, not just by its type. */
fun Transaction.flowKind(category: Category?): FlowKind = when {
    lendingEntryId != null || category?.isLending == true ->
        if (type == TxType.EXPENSE) FlowKind.LENT else FlowKind.LEND_REPAID
    category?.isTransfer == true -> FlowKind.TRANSFER
    type == TxType.REFUND -> FlowKind.REFUND
    type == TxType.INCOME -> FlowKind.INCOME
    else -> FlowKind.SPEND
}

/** This transaction's effect on real spending: plus for a purchase, minus for a refund, zero otherwise. */
fun Transaction.spendDelta(category: Category?): Double = when (flowKind(category)) {
    FlowKind.SPEND -> amount
    FlowKind.REFUND -> -amount
    else -> 0.0
}

data class FlowSummary(
    val income: Double = 0.0,
    /** Every purchase, before refunds. */
    val grossSpend: Double = 0.0,
    val refunds: Double = 0.0,
    val lentOut: Double = 0.0,
    val gotBack: Double = 0.0,
    val transfers: Double = 0.0
) {
    /** What was actually used up: purchases minus refunds, never counting lending or transfers. */
    val realSpend: Double get() = (grossSpend - refunds).coerceAtLeast(0.0)
    val saved: Double get() = income - realSpend
    /** Share of income kept, or null when there was no income to measure against. */
    val savingsRate: Double? get() = if (income > 0.0) saved / income * 100.0 else null
    /** Anything that moved but was left out of the totals above. */
    val hasUncounted: Boolean get() = refunds > 0.0 || lentOut > 0.0 || gotBack > 0.0 || transfers > 0.0
}

fun summarizeFlow(transactions: Iterable<Transaction>, categoriesById: Map<Int, Category>): FlowSummary {
    var income = 0.0
    var spend = 0.0
    var refunds = 0.0
    var lent = 0.0
    var back = 0.0
    var transfers = 0.0
    for (tx in transactions) {
        when (tx.flowKind(categoriesById[tx.categoryId])) {
            FlowKind.SPEND -> spend += tx.amount
            FlowKind.INCOME -> income += tx.amount
            FlowKind.REFUND -> refunds += tx.amount
            FlowKind.LENT -> lent += tx.amount
            FlowKind.LEND_REPAID -> back += tx.amount
            FlowKind.TRANSFER -> transfers += tx.amount
        }
    }
    return FlowSummary(income, spend, refunds, lent, back, transfers)
}

/** Real spend per expense category (purchases minus refunds), leaving out lending and transfers. */
fun realSpendByCategory(transactions: Iterable<Transaction>, categoriesById: Map<Int, Category>): Map<Int, Double> {
    val totals = HashMap<Int, Double>()
    for (tx in transactions) {
        val delta = tx.spendDelta(categoriesById[tx.categoryId])
        if (delta != 0.0) totals[tx.categoryId] = (totals[tx.categoryId] ?: 0.0) + delta
    }
    return totals.mapValues { it.value.coerceAtLeast(0.0) }
}

/**
 * A category's own total over [transactionsInCategory]: real spend for expense categories, income
 * for income categories, and the plain amount moved for lending / transfer categories (those never
 * feed the overall totals, but their own cards still show what went through them).
 */
fun categoryTotal(category: Category, transactionsInCategory: Iterable<Transaction>): Double = when {
    !category.countsInTotals -> transactionsInCategory.sumOf { it.amount }
    category.type == TxType.INCOME -> transactionsInCategory.filter { it.type == TxType.INCOME }.sumOf { it.amount }
    else -> transactionsInCategory.sumOf { it.spendDelta(category) }.coerceAtLeast(0.0)
}

/**
 * What the user says a record is when adding or reclassifying it. Each kind maps to a transaction
 * type plus the categories it may use; LENT / REPAID skip categories and go through the lending
 * ledger instead.
 */
enum class RecordKind(val label: String) {
    EXPENSE("Expense"),
    INCOME("Income"),
    REFUND("Refund"),
    LENT("Lent"),
    REPAID("Got back"),
    TRANSFER("Transfer");

    val isLending: Boolean get() = this == LENT || this == REPAID
    /** Money coming in (shown in green). */
    val isInflow: Boolean get() = this == INCOME || this == REFUND || this == REPAID

    /** Categories a record of this kind may be filed under. */
    fun accepts(category: Category): Boolean = when (this) {
        EXPENSE, REFUND -> category.countsInTotals && category.type == TxType.EXPENSE
        INCOME -> category.countsInTotals && category.type == TxType.INCOME
        TRANSFER -> category.isTransfer
        LENT, REPAID -> false
    }

    /** The Transaction.type to store; a transfer takes its side from the chosen category. */
    fun transactionType(category: Category?): String = when (this) {
        INCOME, REPAID -> TxType.INCOME
        REFUND -> TxType.REFUND
        TRANSFER -> category?.type ?: TxType.EXPENSE
        EXPENSE, LENT -> TxType.EXPENSE
    }

    companion object {
        /** The kinds that make sense for money moving in one direction (e.g. a Money Inbox draft). */
        fun forDirection(sent: Boolean): List<RecordKind> =
            if (sent) listOf(EXPENSE, LENT, TRANSFER) else listOf(INCOME, REFUND, REPAID, TRANSFER)
    }
}

/** The kind an existing transaction was recorded as, for opening it in the editor. */
fun Transaction.recordKind(category: Category?): RecordKind = when (flowKind(category)) {
    FlowKind.SPEND -> RecordKind.EXPENSE
    FlowKind.INCOME -> RecordKind.INCOME
    FlowKind.REFUND -> RecordKind.REFUND
    FlowKind.LENT -> RecordKind.LENT
    FlowKind.LEND_REPAID -> RecordKind.REPAID
    FlowKind.TRANSFER -> RecordKind.TRANSFER
}

/**
 * Best guess at what a captured payment is: a refund when the alert says so, a loan or repayment
 * when the other party is someone in the lending ledger, otherwise plain spending or income.
 * Returns the kind plus the matching lending contact, if any.
 */
fun suggestRecordKind(
    sent: Boolean,
    counterparty: String?,
    looksLikeRefund: Boolean,
    contacts: List<LendingContact>
): Pair<RecordKind, LendingContact?> {
    if (!sent && looksLikeRefund) return RecordKind.REFUND to null
    val name = counterparty?.trim()?.lowercase()
    val contact = if (name.isNullOrEmpty()) null else contacts.firstOrNull { c ->
        val contactName = c.name.trim().lowercase()
        contactName.isNotEmpty() && (contactName == name || name.startsWith("$contactName ") || contactName.startsWith("$name "))
    }
    return when {
        contact != null -> (if (sent) RecordKind.LENT else RecordKind.REPAID) to contact
        sent -> RecordKind.EXPENSE to null
        else -> RecordKind.INCOME to null
    }
}
