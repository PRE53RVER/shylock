package com.example

import com.example.data.model.Category
import com.example.data.model.CategoryRole
import com.example.data.model.FlowKind
import com.example.data.model.LendingContact
import com.example.data.model.RecordKind
import com.example.data.model.Transaction
import com.example.data.model.TxType
import com.example.data.model.categoryTotal
import com.example.data.model.flowKind
import com.example.data.model.realSpendByCategory
import com.example.data.model.suggestRecordKind
import com.example.data.model.summarizeFlow
import com.example.ui.screens.computeSpendPace
import com.example.ui.screens.dailyTotals
import com.example.ui.screens.topPayees
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test
import java.time.LocalDate
import java.time.YearMonth
import java.time.ZoneId

class MoneyFlowTest {

    private val food = Category(id = 1, name = "Food", colorHex = "#EF4444", iconName = "restaurant")
    private val shopping = Category(id = 2, name = "Shopping", colorHex = "#FFD93D", iconName = "shopping_bag")
    private val salary = Category(id = 3, name = "Salary", colorHex = "#2E7D32", iconName = "payments", type = "INCOME")
    private val lending = Category(id = 4, name = "Lending", colorHex = "#00B4D8", iconName = "payments", role = CategoryRole.LENDING)
    private val repayment = Category(id = 5, name = "Loan Repayment", colorHex = "#00C9A7", iconName = "trending_up", type = "INCOME", role = CategoryRole.LENDING)
    private val transferOut = Category(id = 6, name = "Transfer Out", colorHex = "#78909C", iconName = "swap_horiz", role = CategoryRole.TRANSFER)
    private val transferIn = Category(id = 7, name = "Transfer In", colorHex = "#90A4AE", iconName = "swap_horiz", type = "INCOME", role = CategoryRole.TRANSFER)
    private val byId = listOf(food, shopping, salary, lending, repayment, transferOut, transferIn).associateBy { it.id }

    private fun tx(category: Category, amount: Double, type: String = category.type, lendingEntryId: Int? = null, desc: String = "") =
        Transaction(categoryId = category.id, amount = amount, description = desc, type = type, lendingEntryId = lendingEntryId)

    @Test
    fun lendingThenSpendingTheRepaymentCountsOnlyOnce() {
        // Lend 10k, get it back, spend that 10k: only 10k was actually spent
        val txs = listOf(
            tx(salary, 50_000.0),
            tx(lending, 10_000.0, lendingEntryId = 1),
            tx(repayment, 10_000.0, lendingEntryId = 2),
            tx(shopping, 10_000.0)
        )

        val flow = summarizeFlow(txs, byId)

        assertEquals(10_000.0, flow.realSpend, 0.001)
        assertEquals(50_000.0, flow.income, 0.001)
        assertEquals(10_000.0, flow.lentOut, 0.001)
        assertEquals(10_000.0, flow.gotBack, 0.001)
        assertEquals(40_000.0, flow.saved, 0.001)
        assertEquals(80.0, flow.savingsRate!!, 0.001)
        assertTrue(flow.hasUncounted)
    }

    @Test
    fun lendingCategoryCountsAsLendingEvenWithoutLedgerLink() {
        // Filed straight under "Lending" from the inbox, never linked to a ledger entry
        assertEquals(FlowKind.LENT, tx(lending, 500.0).flowKind(lending))
        assertEquals(FlowKind.LEND_REPAID, tx(repayment, 500.0).flowKind(repayment))
        // A ledger link wins even if the category were ordinary
        assertEquals(FlowKind.LENT, tx(food, 500.0, lendingEntryId = 9).flowKind(food))
    }

    @Test
    fun refundLowersItsCategoryAndTheTotal() {
        val txs = listOf(
            tx(shopping, 3_000.0),
            tx(shopping, 500.0, type = TxType.REFUND),
            tx(food, 1_000.0)
        )

        val perCategory = realSpendByCategory(txs, byId)
        val flow = summarizeFlow(txs, byId)

        assertEquals(2_500.0, perCategory.getValue(shopping.id), 0.001)
        assertEquals(1_000.0, perCategory.getValue(food.id), 0.001)
        assertEquals(3_500.0, flow.realSpend, 0.001)
        assertEquals(0.0, flow.income, 0.001)
        assertEquals(500.0, flow.refunds, 0.001)
        assertEquals(2_500.0, categoryTotal(shopping, txs.filter { it.categoryId == shopping.id }), 0.001)
    }

    @Test
    fun refundLargerThanSpendNeverGoesNegative() {
        val txs = listOf(tx(shopping, 200.0), tx(shopping, 900.0, type = TxType.REFUND))
        assertEquals(0.0, realSpendByCategory(txs, byId).getValue(shopping.id), 0.001)
        assertEquals(0.0, summarizeFlow(txs, byId).realSpend, 0.001)
    }

    @Test
    fun transfersStayOutOfIncomeAndSpending() {
        val txs = listOf(
            tx(transferOut, 20_000.0),
            tx(transferIn, 20_000.0),
            tx(food, 300.0)
        )

        val flow = summarizeFlow(txs, byId)

        assertEquals(300.0, flow.realSpend, 0.001)
        assertEquals(0.0, flow.income, 0.001)
        assertEquals(40_000.0, flow.transfers, 0.001)
        assertTrue(transferOut.id !in realSpendByCategory(txs, byId))
    }

    @Test
    fun recordKindsPickTheRightCategoriesAndTypes() {
        assertTrue(RecordKind.EXPENSE.accepts(food))
        assertTrue(!RecordKind.EXPENSE.accepts(lending))
        assertTrue(!RecordKind.EXPENSE.accepts(transferOut))
        assertTrue(RecordKind.REFUND.accepts(shopping))
        assertTrue(RecordKind.TRANSFER.accepts(transferIn))
        assertTrue(!RecordKind.INCOME.accepts(repayment))
        assertEquals(TxType.REFUND, RecordKind.REFUND.transactionType(shopping))
        assertEquals(TxType.INCOME, RecordKind.TRANSFER.transactionType(transferIn))
        assertEquals(listOf(RecordKind.EXPENSE, RecordKind.LENT, RecordKind.TRANSFER), RecordKind.forDirection(sent = true))
    }

    @Test
    fun inboxSuggestionSpotsRefundsAndLendingContacts() {
        val rahul = LendingContact(id = 3, name = "Rahul")
        val contacts = listOf(rahul)

        assertEquals(RecordKind.REFUND to null, suggestRecordKind(sent = false, counterparty = "Amazon", looksLikeRefund = true, contacts = contacts))
        assertEquals(RecordKind.REPAID to rahul, suggestRecordKind(sent = false, counterparty = "rahul", looksLikeRefund = false, contacts = contacts))
        assertEquals(RecordKind.LENT to rahul, suggestRecordKind(sent = true, counterparty = "Rahul Kumar", looksLikeRefund = false, contacts = contacts))
        assertEquals(RecordKind.EXPENSE to null, suggestRecordKind(sent = true, counterparty = "Swiggy", looksLikeRefund = false, contacts = contacts))
        assertEquals(RecordKind.INCOME to null, suggestRecordKind(sent = false, counterparty = null, looksLikeRefund = false, contacts = contacts))
    }

    @Test
    fun calendarDaysLeaveLendingOut() {
        val zone = ZoneId.systemDefault()
        val day = LocalDate.of(2026, 9, 15)
        val at = day.atTime(12, 0).atZone(zone).toInstant().toEpochMilli()
        val txs = listOf(
            tx(food, 400.0).copy(timestamp = at),
            tx(lending, 10_000.0, lendingEntryId = 1).copy(timestamp = at)
        )

        val totals = dailyTotals(txs, zone, byId).getValue(day)

        assertEquals(400.0, totals.expense, 0.001)
        assertEquals(2, totals.count)
    }

    @Test
    fun paceProjectsTheMonthInProgress() {
        val zone = ZoneId.systemDefault()
        val month = YearMonth.of(2026, 9) // 30 days
        val today = month.atDay(10)
        fun on(day: Int) = month.atDay(day).atTime(12, 0).atZone(zone).toInstant().toEpochMilli()
        val txs = listOf(
            tx(food, 1_000.0).copy(timestamp = on(2)),
            tx(shopping, 4_000.0).copy(timestamp = on(5)),
            tx(salary, 30_000.0).copy(timestamp = on(1)),
            tx(lending, 10_000.0).copy(timestamp = on(6))
        )
        val flow = summarizeFlow(txs, byId)

        val pace = computeSpendPace(txs, byId, flow, AnalysisPeriod.MONTHLY, month, today, zone)

        assertEquals(500.0, pace.dailyAverage, 0.001) // 5,000 over 10 days
        assertEquals(15_000.0, pace.projectedTotal!!, 0.001)
        assertEquals(25_000.0, pace.incomeLeft!!, 0.001)
        assertEquals(21, pace.daysLeft) // 10th through 30th
        assertEquals(25_000.0 / 21, pace.perDayLeft!!, 0.001)
        assertEquals(month.atDay(5), pace.peakDay)
    }

    @Test
    fun paceForAPastMonthHasNoProjection() {
        val month = YearMonth.of(2026, 8)
        val pace = computeSpendPace(emptyList(), byId, summarizeFlow(emptyList(), byId), AnalysisPeriod.MONTHLY, month, LocalDate.of(2026, 9, 27))
        assertNull(pace.projectedTotal)
        assertNull(pace.incomeLeft)
        assertEquals(31, pace.daysCounted)
    }

    @Test
    fun payeesGroupByDescriptionIgnoringCase() {
        val txs = listOf(
            tx(food, 200.0, desc = "Swiggy"),
            tx(food, 300.0, desc = "swiggy "),
            tx(shopping, 900.0, desc = "Amazon"),
            tx(food, 50.0, desc = "")
        )

        val payees = topPayees(txs)

        assertEquals(listOf("Amazon", "Swiggy"), payees.map { it.name })
        assertEquals(500.0, payees[1].total, 0.001)
        assertEquals(2, payees[1].count)
    }
}
