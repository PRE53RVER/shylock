package com.example

import com.example.data.model.Category
import com.example.data.model.Transaction
import com.example.ui.screens.dailyTotals
import com.example.ui.screens.dayCategoryBreakdown
import com.example.ui.screens.formatCompactAmount
import com.example.ui.screens.startOfWeek
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test
import java.time.DayOfWeek
import java.time.LocalDate
import java.time.ZoneId

class SpendingCalendarTest {

    private val zone = ZoneId.systemDefault()

    private fun at(date: LocalDate, hour: Int, minute: Int = 0): Long =
        date.atTime(hour, minute).atZone(zone).toInstant().toEpochMilli()

    @Test
    fun dailyTotalsSplitIncomeAndSpendingPerDay() {
        val day = LocalDate.of(2026, 9, 15)
        val next = day.plusDays(1)
        val txs = listOf(
            Transaction(id = 1, categoryId = 1, amount = 649.0, description = "Netflix", timestamp = at(day, 9, 23)),
            Transaction(id = 2, categoryId = 1, amount = 850.0, description = "KSEB", timestamp = at(day, 13, 15)),
            Transaction(id = 3, categoryId = 9, amount = 8500.0, description = "Salary", timestamp = at(day, 10), type = "INCOME"),
            // 23:59 stays on the 15th, 00:00 belongs to the 16th
            Transaction(id = 4, categoryId = 2, amount = 100.0, description = "Late", timestamp = at(day, 23, 59)),
            Transaction(id = 5, categoryId = 2, amount = 40.0, description = "Early", timestamp = at(next, 0))
        )

        val totals = dailyTotals(txs, zone)

        assertEquals(1599.0, totals.getValue(day).expense, 0.001)
        assertEquals(8500.0, totals.getValue(day).income, 0.001)
        assertEquals(4, totals.getValue(day).count)
        assertEquals(40.0, totals.getValue(next).expense, 0.001)
        assertNull(totals[day.minusDays(1)])
    }

    @Test
    fun breakdownGroupsByCategoryLargestFirstWithShares() {
        val day = LocalDate.of(2026, 9, 15)
        val bills = Category(id = 1, name = "Bills", colorHex = "#10B981", iconName = "home")
        val food = Category(id = 2, name = "Food", colorHex = "#EF4444", iconName = "restaurant")
        val txs = listOf(
            Transaction(categoryId = 2, amount = 300.0, description = "Lunch", timestamp = at(day, 13)),
            Transaction(categoryId = 1, amount = 500.0, description = "Power", timestamp = at(day, 9)),
            Transaction(categoryId = 1, amount = 200.0, description = "Water", timestamp = at(day, 10)),
            Transaction(categoryId = 42, amount = 0.0, description = "Deleted cat", timestamp = at(day, 11)),
            Transaction(categoryId = 9, amount = 8500.0, description = "Salary", timestamp = at(day, 10), type = "INCOME")
        )

        val slices = dayCategoryBreakdown(txs, listOf(bills, food), "EXPENSE")

        assertEquals(listOf(1, 2, 42), slices.map { it.categoryId })
        assertEquals(700.0, slices[0].amount, 0.001)
        assertEquals(70.0, slices[0].share, 0.001)
        assertEquals(2, slices[0].count)
        assertEquals(30.0, slices[1].share, 0.001)
        // A transaction whose category no longer exists is kept, just without a category
        assertNull(slices[2].category)
    }

    @Test
    fun compactAmountsFitCalendarCells() {
        assertEquals("₹2,430", formatCompactAmount(2430.0, "₹"))
        assertEquals("₹12.4k", formatCompactAmount(12_430.0, "₹"))
        assertEquals("₹1.2L", formatCompactAmount(1_20_000.0, "₹"))
        assertEquals("₹2Cr", formatCompactAmount(2_00_00_000.0, "₹"))
        assertEquals("$1.5M", formatCompactAmount(1_500_000.0, "$"))
        assertEquals("$250k", formatCompactAmount(250_000.0, "$"))
    }

    @Test
    fun weekStartsOnTheLocaleFirstDay() {
        val tuesday = LocalDate.of(2026, 9, 15)
        assertEquals(LocalDate.of(2026, 9, 13), startOfWeek(tuesday, DayOfWeek.SUNDAY))
        assertEquals(LocalDate.of(2026, 9, 14), startOfWeek(tuesday, DayOfWeek.MONDAY))
        assertEquals(LocalDate.of(2026, 9, 13), startOfWeek(LocalDate.of(2026, 9, 13), DayOfWeek.SUNDAY))
    }
}
