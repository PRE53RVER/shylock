package com.example

import com.example.data.model.Category
import com.example.data.model.Transaction
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import java.time.Instant
import java.time.YearMonth
import java.time.ZoneId

class MonthNavigationTest {

    @Test
    fun testYearMonthBoundaryMath() {
        // Test transactions on month edges: March 31 23:59:59 vs April 1 00:00:00
        val zone = ZoneId.systemDefault()

        val marchMonth = YearMonth.of(2026, 3)
        val march31End = marchMonth.atEndOfMonth().atTime(23, 59, 59).atZone(zone).toInstant().toEpochMilli()
        val marchMonthComputed = YearMonth.from(Instant.ofEpochMilli(march31End).atZone(zone))
        assertEquals(marchMonth, marchMonthComputed)

        val aprilMonth = YearMonth.of(2026, 4)
        val april1Start = aprilMonth.atDay(1).atStartOfDay(zone).toInstant().toEpochMilli()
        val aprilMonthComputed = YearMonth.from(Instant.ofEpochMilli(april1Start).atZone(zone))
        assertEquals(aprilMonth, aprilMonthComputed)

        // Ensure consecutive timestamps across midnight boundary resolve to different months
        assertTrue(april1Start > march31End)
        assertEquals(1, aprilMonthComputed.monthValue - marchMonthComputed.monthValue)
    }

    @Test
    fun testMonthFiltering() {
        val zone = ZoneId.systemDefault()
        val marchMonth = YearMonth.of(2026, 3)
        val febMonth = YearMonth.of(2026, 2)

        val febTs = febMonth.atDay(15).atStartOfDay(zone).toInstant().toEpochMilli()
        val marchTs1 = marchMonth.atDay(1).atStartOfDay(zone).toInstant().toEpochMilli()
        val marchTs2 = marchMonth.atDay(25).atStartOfDay(zone).toInstant().toEpochMilli()

        val transactions = listOf(
            Transaction(id = 1, categoryId = 1, subcategoryId = null, amount = 100.0, description = "Feb Grocery", timestamp = febTs, type = "EXPENSE"),
            Transaction(id = 2, categoryId = 1, subcategoryId = null, amount = 250.0, description = "March Rent", timestamp = marchTs1, type = "EXPENSE"),
            Transaction(id = 3, categoryId = 1, subcategoryId = null, amount = 50.0, description = "March Coffee", timestamp = marchTs2, type = "EXPENSE")
        )

        // Filter for March
        val marchTransactions = transactions.filter {
            YearMonth.from(Instant.ofEpochMilli(it.timestamp).atZone(zone)) == marchMonth
        }
        assertEquals(2, marchTransactions.size)
        assertEquals(300.0, marchTransactions.sumOf { it.amount }, 0.001)

        // Filter for Feb
        val febTransactions = transactions.filter {
            YearMonth.from(Instant.ofEpochMilli(it.timestamp).atZone(zone)) == febMonth
        }
        assertEquals(1, febTransactions.size)
        assertEquals(100.0, febTransactions.sumOf { it.amount }, 0.001)

        // Filter for April (empty)
        val aprilTransactions = transactions.filter {
            YearMonth.from(Instant.ofEpochMilli(it.timestamp).atZone(zone)) == YearMonth.of(2026, 4)
        }
        assertTrue(aprilTransactions.isEmpty())
    }

    @Test
    fun testPastMonthEditRestrictions() {
        val currentMonth = YearMonth.now()
        val pastMonth = currentMonth.minusMonths(1)

        // For current month: always editable
        val currentCanEdit = (currentMonth == currentMonth)
        assertTrue(currentCanEdit)

        // For past month locked: not editable
        var pastMonthUnlocked = false
        var pastCanEdit = (pastMonth == currentMonth) || pastMonthUnlocked
        assertFalse(pastCanEdit)

        // For past month unlocked: editable
        pastMonthUnlocked = true
        pastCanEdit = (pastMonth == currentMonth) || pastMonthUnlocked
        assertTrue(pastCanEdit)
    }

    @Test
    fun testCalculatePeriodStatsMonthlyScopedToSelectedMonth() {
        val zone = ZoneId.systemDefault()
        val marchMonth = YearMonth.of(2026, 3)
        val febMonth = YearMonth.of(2026, 2)

        val catFood = Category(id = 1, name = "Food", colorHex = "#FF5722", iconName = "Restaurant")
        val catTransport = Category(id = 2, name = "Transport", colorHex = "#2196F3", iconName = "DirectionsCar")
        val categories = listOf(catFood, catTransport)

        val febTs = febMonth.atDay(10).atStartOfDay(zone).toInstant().toEpochMilli()
        val marchTs1 = marchMonth.atDay(5).atStartOfDay(zone).toInstant().toEpochMilli()
        val marchTs2 = marchMonth.atDay(20).atStartOfDay(zone).toInstant().toEpochMilli()
        val aprilTs = YearMonth.of(2026, 4).atDay(2).atStartOfDay(zone).toInstant().toEpochMilli()

        val transactions = listOf(
            Transaction(id = 1, categoryId = 1, subcategoryId = null, amount = 100.0, description = "Feb Food", timestamp = febTs, type = "EXPENSE"),
            Transaction(id = 2, categoryId = 1, subcategoryId = null, amount = 150.0, description = "March Food", timestamp = marchTs1, type = "EXPENSE"),
            Transaction(id = 3, categoryId = 2, subcategoryId = null, amount = 50.0, description = "March Taxi", timestamp = marchTs2, type = "EXPENSE"),
            Transaction(id = 4, categoryId = 1, subcategoryId = null, amount = 300.0, description = "April Food", timestamp = aprilTs, type = "EXPENSE")
        )

        // When viewing March 2026 in Insights
        val marchStats = calculatePeriodStats(
            categories = categories,
            transactions = transactions,
            period = AnalysisPeriod.MONTHLY,
            selectedMonth = marchMonth
        )

        // Food in March: amount 150.0, total spend 200.0 (75%)
        val foodMarch = marchStats[1]!!
        assertEquals(150.0, foodMarch.totalAmount, 0.001)
        assertEquals(1, foodMarch.transactionCount)
        assertEquals(75.0, foodMarch.percentage, 0.001)
        // Previous month (Feb) Food was 100.0. Trend: ((150 - 100) / 100) * 100 = +50%
        assertEquals(50.0, foodMarch.trendPercentage, 0.001)

        // Transport in March: amount 50.0, total spend 200.0 (25%)
        val transportMarch = marchStats[2]!!
        assertEquals(50.0, transportMarch.totalAmount, 0.001)
        assertEquals(1, transportMarch.transactionCount)
        assertEquals(25.0, transportMarch.percentage, 0.001)

        // When user switches to February 2026
        val febStats = calculatePeriodStats(
            categories = categories,
            transactions = transactions,
            period = AnalysisPeriod.MONTHLY,
            selectedMonth = febMonth
        )

        val foodFeb = febStats[1]!!
        assertEquals(100.0, foodFeb.totalAmount, 0.001)
        assertEquals(1, foodFeb.transactionCount)
        assertEquals(100.0, foodFeb.percentage, 0.001)

        val transportFeb = febStats[2]!!
        assertEquals(0.0, transportFeb.totalAmount, 0.001)
        assertEquals(0, transportFeb.transactionCount)
    }

    @Test
    fun testCalculatePeriodStatsWeeklyAndYearlyAnchored() {
        val zone = ZoneId.systemDefault()
        val marchMonth = YearMonth.of(2026, 3)

        val catFood = Category(id = 1, name = "Food", colorHex = "#FF5722", iconName = "Restaurant")
        val categories = listOf(catFood)

        // March 31 end of month anchor: 7-day window is March 25 - March 31
        val march28Ts = marchMonth.atDay(28).atStartOfDay(zone).toInstant().toEpochMilli()
        val march10Ts = marchMonth.atDay(10).atStartOfDay(zone).toInstant().toEpochMilli()

        val transactions = listOf(
            Transaction(id = 1, categoryId = 1, subcategoryId = null, amount = 75.0, description = "Late March Food", timestamp = march28Ts, type = "EXPENSE"),
            Transaction(id = 2, categoryId = 1, subcategoryId = null, amount = 120.0, description = "Early March Food", timestamp = march10Ts, type = "EXPENSE")
        )

        // Weekly anchored to March 2026 should only capture March 28 transaction
        val weeklyStats = calculatePeriodStats(
            categories = categories,
            transactions = transactions,
            period = AnalysisPeriod.WEEKLY,
            selectedMonth = marchMonth
        )
        assertEquals(75.0, weeklyStats[1]!!.totalAmount, 0.001)

        // Yearly anchored to March 2026 should capture both transactions
        val yearlyStats = calculatePeriodStats(
            categories = categories,
            transactions = transactions,
            period = AnalysisPeriod.YEARLY,
            selectedMonth = marchMonth
        )
        assertEquals(195.0, yearlyStats[1]!!.totalAmount, 0.001)
    }
}
