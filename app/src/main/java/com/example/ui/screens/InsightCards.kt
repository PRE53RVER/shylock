package com.example.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ChevronRight
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.Speed
import androidx.compose.material.icons.filled.TrendingUp
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.AnalysisPeriod
import com.example.analysisWindow
import com.example.data.model.Category
import com.example.data.model.FlowKind
import com.example.data.model.FlowSummary
import com.example.data.model.Transaction
import com.example.data.model.countsInTotals
import com.example.data.model.displayName
import com.example.data.model.flowKind
import com.example.data.model.realSpendByCategory
import com.example.formatInRupee
import com.example.formatTransactionDate
import com.example.getIconVector
import com.example.parseHexColor
import com.example.ui.theme.ExpenseRed
import com.example.ui.theme.GlassCard
import com.example.ui.theme.IncomeGreen
import java.time.Instant
import java.time.LocalDate
import java.time.YearMonth
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import java.time.temporal.ChronoUnit
import java.util.Locale
import kotlin.math.roundToLong

// ─── Calculations ────────────────────────────────────────────────────────────────────────────
// Pure helpers behind the Insights cards, kept free of Compose so they can be unit tested.

/** How fast money is going out over the analysis period, and what that means for the rest of it. */
data class SpendPace(
    val dailyAverage: Double,
    /** Days of the period covered so far (the whole period once it is over). */
    val daysCounted: Int,
    /** Month-end total at the current pace; only for the month in progress. */
    val projectedTotal: Double?,
    /** Income not yet spent this month, and how much that allows per remaining day (today included). */
    val incomeLeft: Double?,
    val perDayLeft: Double?,
    val daysLeft: Int,
    /** The period's most expensive day and its real spend. */
    val peakDay: LocalDate?,
    val peakAmount: Double
)

fun computeSpendPace(
    periodTransactions: List<Transaction>,
    categoriesById: Map<Int, Category>,
    flow: FlowSummary,
    period: AnalysisPeriod,
    selectedMonth: YearMonth,
    today: LocalDate = LocalDate.now(),
    zone: ZoneId = ZoneId.systemDefault()
): SpendPace {
    val isCurrentMonth = selectedMonth == YearMonth.from(today)
    val periodEnd = if (isCurrentMonth) today else selectedMonth.atEndOfMonth()
    val periodStart = when (period) {
        AnalysisPeriod.WEEKLY -> periodEnd.minusDays(6)
        AnalysisPeriod.MONTHLY -> selectedMonth.atDay(1)
        AnalysisPeriod.YEARLY -> selectedMonth.minusMonths(11).atDay(1)
    }
    val daysCounted = (ChronoUnit.DAYS.between(periodStart, periodEnd) + 1).toInt().coerceAtLeast(1)
    val average = flow.realSpend / daysCounted

    val inProgressMonth = period == AnalysisPeriod.MONTHLY && isCurrentMonth
    val daysInMonth = selectedMonth.lengthOfMonth()
    val daysLeft = if (inProgressMonth) daysInMonth - today.dayOfMonth + 1 else 0
    val incomeLeft = if (inProgressMonth && flow.income > 0.0) flow.income - flow.realSpend else null

    val byDay = HashMap<LocalDate, Double>()
    for (tx in periodTransactions) {
        val kind = tx.flowKind(categoriesById[tx.categoryId])
        val delta = when (kind) {
            FlowKind.SPEND -> tx.amount
            FlowKind.REFUND -> -tx.amount
            else -> continue
        }
        val day = Instant.ofEpochMilli(tx.timestamp).atZone(zone).toLocalDate()
        byDay[day] = (byDay[day] ?: 0.0) + delta
    }
    val peak = byDay.maxByOrNull { it.value }?.takeIf { it.value > 0.0 }

    return SpendPace(
        dailyAverage = average,
        daysCounted = daysCounted,
        projectedTotal = if (inProgressMonth) average * daysInMonth else null,
        incomeLeft = incomeLeft,
        perDayLeft = if (incomeLeft != null && incomeLeft > 0.0 && daysLeft > 0) incomeLeft / daysLeft else null,
        daysLeft = daysLeft,
        peakDay = peak?.key,
        peakAmount = peak?.value ?: 0.0
    )
}

/** Where the money went: purchases grouped by what they were called (merchant or person). */
data class PayeeTotal(val name: String, val total: Double, val count: Int)

fun topPayees(spendTransactions: List<Transaction>, limit: Int = 5): List<PayeeTotal> =
    spendTransactions
        .filter { it.description.isNotBlank() }
        .groupBy { it.description.trim().lowercase(Locale.ROOT) }
        .map { (_, txs) ->
            // Show the spelling used most often for the group
            val name = txs.groupingBy { it.description.trim() }.eachCount().maxByOrNull { it.value }!!.key
            PayeeTotal(name, txs.sumOf { it.amount }, txs.size)
        }
        .sortedByDescending { it.total }
        .take(limit)

/** A category spending clearly more than it usually does. */
data class UnusualSpend(val category: Category, val current: Double, val usual: Double) {
    val extra: Double get() = current - usual
}

/**
 * Categories where this period's real spend is at least [ratio] times the average of the previous
 * [lookback] periods and at least [minExtra] above it. Weekly and monthly only; a year has no
 * meaningful "usual" to compare with.
 */
fun unusualSpends(
    transactions: List<Transaction>,
    categories: List<Category>,
    currentByCategory: Map<Int, Double>,
    period: AnalysisPeriod,
    selectedMonth: YearMonth,
    lookback: Int = 3,
    ratio: Double = 1.5,
    minExtra: Double = 500.0
): List<UnusualSpend> {
    if (period == AnalysisPeriod.YEARLY) return emptyList()
    val categoriesById = categories.associateBy { it.id }
    val zone = ZoneId.systemDefault()
    val current = analysisWindow(period, selectedMonth)
    val pastWindows: List<LongRange> = (1..lookback).map { i ->
        when (period) {
            AnalysisPeriod.MONTHLY -> {
                val month = selectedMonth.minusMonths(i.toLong())
                month.atDay(1).atStartOfDay(zone).toInstant().toEpochMilli() until
                    month.plusMonths(1).atDay(1).atStartOfDay(zone).toInstant().toEpochMilli()
            }
            else -> {
                val span = 7L * 24 * 60 * 60 * 1000
                (current.start - i * span) until (current.start - (i - 1) * span)
            }
        }
    }
    // Only compare once records reach back into the oldest window; otherwise "usual" reads too low
    val earliest = transactions.minOfOrNull { it.timestamp } ?: return emptyList()
    if (earliest > pastWindows.last().last) return emptyList()

    val pastTotals = pastWindows.map { window ->
        realSpendByCategory(transactions.filter { it.timestamp in window }, categoriesById)
    }
    return categories
        .filter { it.countsInTotals && it.type == "EXPENSE" }
        .mapNotNull { category ->
            val now = currentByCategory[category.id] ?: 0.0
            val usual = pastTotals.sumOf { it[category.id] ?: 0.0 } / lookback
            if (usual > 0.0 && now >= usual * ratio && now - usual >= minExtra) UnusualSpend(category, now, usual) else null
        }
        .sortedByDescending { it.extra }
}

// ─── Cards ───────────────────────────────────────────────────────────────────────────────────

private fun money(amount: Double, currency: String) = formatInRupee(amount.roundToLong().toDouble(), currency)

@Composable
private fun InsightCard(
    title: String,
    subtitle: String,
    modifier: Modifier = Modifier,
    content: @Composable ColumnScope.() -> Unit
) {
    GlassCard(
        modifier = modifier.fillMaxWidth(),
        shape = RoundedCornerShape(24.dp),
        elevation = 8.dp,
        contentPadding = PaddingValues(horizontal = 14.dp, vertical = 14.dp)
    ) {
        Text(
            text = title,
            style = MaterialTheme.typography.titleMedium,
            fontSize = 16.sp,
            lineHeight = 20.sp,
            fontWeight = FontWeight.ExtraBold,
            color = MaterialTheme.colorScheme.onSurface
        )
        Text(
            text = subtitle,
            style = MaterialTheme.typography.labelMedium,
            fontWeight = FontWeight.Normal,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
        Spacer(Modifier.height(10.dp))
        content()
    }
}

@Composable
private fun SubHeading(text: String) {
    Text(
        text = text.uppercase(Locale.getDefault()),
        style = MaterialTheme.typography.labelSmall,
        fontWeight = FontWeight.Bold,
        letterSpacing = 1.sp,
        color = MaterialTheme.colorScheme.onSurfaceVariant,
        modifier = Modifier.padding(top = 4.dp, bottom = 6.dp)
    )
}

/**
 * Income − real spending = saved, then a quiet line listing the money that moved but was left
 * out (lending, refunds, transfers). Sits at the bottom of the Insights summary card.
 */
@Composable
fun MoneyFlowStrip(
    flow: FlowSummary,
    currencySymbol: String,
    onExplain: () -> Unit,
    modifier: Modifier = Modifier
) {
    Column(modifier = modifier.fillMaxWidth().testTag("insights_money_flow")) {
        HorizontalDivider(color = MaterialTheme.colorScheme.outline.copy(alpha = 0.15f))
        Spacer(Modifier.height(10.dp))
        Row(modifier = Modifier.fillMaxWidth()) {
            FlowFigure("Income", money(flow.income, currencySymbol), IncomeGreen, Modifier.weight(1f))
            FlowFigure("Spent", money(flow.realSpend, currencySymbol), ExpenseRed, Modifier.weight(1f))
            val savedColor = if (flow.saved >= 0.0) MaterialTheme.colorScheme.primary else ExpenseRed
            val savedLabel = flow.savingsRate?.let { "Saved · ${it.roundToLong()}%" } ?: "Saved"
            FlowFigure(savedLabel, money(flow.saved, currencySymbol), savedColor, Modifier.weight(1f))
        }
        if (flow.hasUncounted) {
            Spacer(Modifier.height(10.dp))
            val parts = buildList {
                if (flow.lentOut > 0.0) add("Lent ${money(flow.lentOut, currencySymbol)}")
                if (flow.gotBack > 0.0) add("Got back ${money(flow.gotBack, currencySymbol)}")
                if (flow.refunds > 0.0) add("Refunds −${money(flow.refunds, currencySymbol)}")
                if (flow.transfers > 0.0) add("Transfers ${money(flow.transfers, currencySymbol)}")
            }
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(12.dp))
                    .background(MaterialTheme.colorScheme.onSurface.copy(alpha = 0.05f))
                    .clickable(onClick = onExplain)
                    .padding(horizontal = 10.dp, vertical = 8.dp)
                    .testTag("insights_not_counted_strip"),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Icon(Icons.Default.Info, contentDescription = null, tint = MaterialTheme.colorScheme.outline, modifier = Modifier.size(14.dp))
                Spacer(Modifier.width(6.dp))
                Text(
                    text = "Not counted: " + parts.joinToString(" · "),
                    style = MaterialTheme.typography.labelSmall,
                    lineHeight = 14.sp,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.weight(1f)
                )
            }
        }
    }
}

@Composable
private fun FlowFigure(label: String, value: String, color: Color, modifier: Modifier = Modifier) {
    Column(modifier = modifier) {
        Text(label, style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant, maxLines = 1)
        Text(
            text = value,
            fontSize = 15.sp,
            fontWeight = FontWeight.ExtraBold,
            color = color,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis
        )
    }
}

/** Explains why lending, refunds and transfers are left out of spending and income. */
@Composable
fun NotCountedExplainer(flow: FlowSummary, currencySymbol: String, onDismiss: () -> Unit) {
    AlertDialog(
        onDismissRequest = onDismiss,
        confirmButton = { TextButton(onClick = onDismiss) { Text("Got it") } },
        title = { Text("What's left out of the totals", fontWeight = FontWeight.Bold) },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                Text(
                    "Spending only counts money that's actually used up. Money that goes out and comes back would otherwise be counted twice.",
                    style = MaterialTheme.typography.bodyMedium
                )
                ExplainerLine("Lent", flow.lentOut, currencySymbol, "Money you lent. It's still yours, so it isn't spending. It's tracked in Lending.")
                ExplainerLine("Got back", flow.gotBack, currencySymbol, "Repayments. They aren't income, because the money was already yours.")
                ExplainerLine("Refunds", flow.refunds, currencySymbol, "Subtracted from the category they came back to.")
                ExplainerLine("Transfers", flow.transfers, currencySymbol, "Moves between your own accounts, savings or investments.")
                Text(
                    "Recorded a loan or refund as a normal expense or income? Open the record and change \"What is this?\".",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
    )
}

@Composable
private fun ExplainerLine(label: String, amount: Double, currencySymbol: String, meaning: String) {
    Column {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Text(label, fontWeight = FontWeight.Bold, style = MaterialTheme.typography.bodyMedium, modifier = Modifier.weight(1f))
            Text(money(amount, currencySymbol), fontWeight = FontWeight.Bold, style = MaterialTheme.typography.bodyMedium)
        }
        Text(meaning, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
    }
}

/** Daily average, where the month is heading, and how much of this month's income is left per day. */
@Composable
fun SpendPaceCard(
    pace: SpendPace,
    lastPeriodSpend: Double,
    currencySymbol: String,
    modifier: Modifier = Modifier
) {
    val dayFormatter = DateTimeFormatter.ofPattern("EEE, d MMM", Locale.getDefault())
    InsightCard(
        title = "Pace",
        subtitle = if (pace.projectedTotal != null) "Where this month is heading" else "How fast the money went",
        modifier = modifier.testTag("insights_pace_card")
    ) {
        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
            PaceTile(
                label = "Per day",
                value = money(pace.dailyAverage, currencySymbol),
                note = "over ${pace.daysCounted} day${if (pace.daysCounted == 1) "" else "s"}",
                modifier = Modifier.weight(1f)
            )
            if (pace.projectedTotal != null) {
                val overLast = lastPeriodSpend > 0.0 && pace.projectedTotal > lastPeriodSpend
                PaceTile(
                    label = "Month-end",
                    value = money(pace.projectedTotal, currencySymbol),
                    note = when {
                        lastPeriodSpend <= 0.0 -> "at this pace"
                        overLast -> "${money(pace.projectedTotal - lastPeriodSpend, currencySymbol)} over last month"
                        else -> "${money(lastPeriodSpend - pace.projectedTotal, currencySymbol)} under last month"
                    },
                    noteColor = if (overLast) ExpenseRed else null,
                    modifier = Modifier.weight(1f)
                )
            } else if (pace.peakDay != null) {
                PaceTile(
                    label = "Biggest day",
                    value = money(pace.peakAmount, currencySymbol),
                    note = pace.peakDay.format(dayFormatter),
                    modifier = Modifier.weight(1f)
                )
            }
        }
        if (pace.incomeLeft != null) {
            Spacer(Modifier.height(10.dp))
            val message = if (pace.incomeLeft > 0.0 && pace.perDayLeft != null) {
                "${money(pace.incomeLeft, currencySymbol)} of this month's income left: " +
                    "${money(pace.perDayLeft, currencySymbol)} a day for the next ${pace.daysLeft} day${if (pace.daysLeft == 1) "" else "s"}."
            } else {
                "You've spent ${money(-pace.incomeLeft, currencySymbol)} more than you earned this month."
            }
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(12.dp))
                    .background((if (pace.incomeLeft > 0.0) IncomeGreen else ExpenseRed).copy(alpha = 0.10f))
                    .padding(horizontal = 10.dp, vertical = 9.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Icon(
                    Icons.Default.Speed,
                    contentDescription = null,
                    tint = if (pace.incomeLeft > 0.0) IncomeGreen else ExpenseRed,
                    modifier = Modifier.size(16.dp)
                )
                Spacer(Modifier.width(8.dp))
                Text(message, style = MaterialTheme.typography.labelMedium, lineHeight = 16.sp, color = MaterialTheme.colorScheme.onSurface)
            }
        }
    }
}

@Composable
private fun PaceTile(label: String, value: String, note: String, modifier: Modifier = Modifier, noteColor: Color? = null) {
    Column(
        modifier = modifier
            .clip(RoundedCornerShape(14.dp))
            .background(MaterialTheme.colorScheme.onSurface.copy(alpha = 0.05f))
            .padding(horizontal = 12.dp, vertical = 10.dp)
    ) {
        Text(label, style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
        Text(value, fontSize = 18.sp, fontWeight = FontWeight.ExtraBold, color = MaterialTheme.colorScheme.onSurface, maxLines = 1)
        Text(
            note,
            style = MaterialTheme.typography.labelSmall,
            color = noteColor ?: MaterialTheme.colorScheme.onSurfaceVariant,
            maxLines = 2
        )
    }
}

/** The period's biggest purchases, the payees that took the most, and categories running hot. */
@Composable
fun TopSpendsCard(
    topTransactions: List<Transaction>,
    payees: List<PayeeTotal>,
    unusual: List<UnusualSpend>,
    categoriesById: Map<Int, Category>,
    period: AnalysisPeriod,
    currencySymbol: String,
    onTransactionClick: (Transaction) -> Unit,
    modifier: Modifier = Modifier
) {
    if (topTransactions.isEmpty() && unusual.isEmpty()) return
    InsightCard(
        title = "Top & Unusual",
        subtitle = "Your biggest spends and anything out of the ordinary",
        modifier = modifier.testTag("insights_top_spends_card")
    ) {
        if (unusual.isNotEmpty()) {
            SubHeading("Above your usual")
            val usualNoun = if (period == AnalysisPeriod.WEEKLY) "week" else "month"
            unusual.take(3).forEach { item ->
                val color = parseHexColor(item.category.colorHex)
                Row(
                    modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(Icons.Default.TrendingUp, contentDescription = null, tint = ExpenseRed, modifier = Modifier.size(16.dp))
                    Spacer(Modifier.width(8.dp))
                    Text(
                        text = "${item.category.displayName} is ${money(item.extra, currencySymbol)} above a usual $usualNoun " +
                            "(${money(item.usual, currencySymbol)})",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurface,
                        modifier = Modifier.weight(1f)
                    )
                    Box(Modifier.size(8.dp).clip(CircleShape).background(color))
                }
            }
            Spacer(Modifier.height(6.dp))
        }

        if (topTransactions.isNotEmpty()) {
            SubHeading("Biggest spends")
            topTransactions.forEach { tx ->
                val category = categoriesById[tx.categoryId]
                val color = parseHexColor(category?.colorHex ?: "#888888")
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(12.dp))
                        .clickable { onTransactionClick(tx) }
                        .padding(vertical = 6.dp, horizontal = 2.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Box(
                        modifier = Modifier.size(30.dp).clip(CircleShape).background(color.copy(alpha = 0.18f)),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(getIconVector(category?.iconName ?: "help"), contentDescription = null, tint = color, modifier = Modifier.size(16.dp))
                    }
                    Spacer(Modifier.width(10.dp))
                    Column(Modifier.weight(1f)) {
                        Text(
                            tx.description.ifBlank { category?.displayName ?: "Record" },
                            style = MaterialTheme.typography.bodyMedium,
                            fontWeight = FontWeight.SemiBold,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                        Text(
                            "${category?.displayName ?: "Unknown"} • ${formatTransactionDate(tx.timestamp)}",
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            maxLines = 1
                        )
                    }
                    Text(money(tx.amount, currencySymbol), fontWeight = FontWeight.Bold, color = ExpenseRed, fontSize = 14.sp)
                    Icon(Icons.Default.ChevronRight, contentDescription = null, tint = MaterialTheme.colorScheme.outline, modifier = Modifier.size(16.dp))
                }
            }
            Text(
                "Tap one to change it, e.g. to mark a payment as Lent.",
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.outline,
                modifier = Modifier.padding(top = 2.dp)
            )
        }

        if (payees.size > 1) {
            Spacer(Modifier.height(8.dp))
            SubHeading("Where it went")
            val max = payees.maxOf { it.total }.coerceAtLeast(1.0)
            payees.forEach { payee ->
                Column(Modifier.fillMaxWidth().padding(vertical = 4.dp)) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text(
                            payee.name,
                            style = MaterialTheme.typography.bodySmall,
                            fontWeight = FontWeight.SemiBold,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis,
                            modifier = Modifier.weight(1f)
                        )
                        Text(
                            "${money(payee.total, currencySymbol)} · ${payee.count}×",
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                    Spacer(Modifier.height(4.dp))
                    Box(
                        Modifier
                            .fillMaxWidth()
                            .height(5.dp)
                            .clip(CircleShape)
                            .background(MaterialTheme.colorScheme.onSurface.copy(alpha = 0.06f))
                    ) {
                        Box(
                            Modifier
                                .fillMaxWidth((payee.total / max).toFloat().coerceIn(0.02f, 1f))
                                .height(5.dp)
                                .clip(CircleShape)
                                .background(MaterialTheme.colorScheme.primary.copy(alpha = 0.7f))
                        )
                    }
                }
            }
        }
    }
}

/** Lending for the period, kept beside (not inside) spending: lent, got back, and still owed overall. */
@Composable
fun LendingInsightStrip(
    lentOut: Double,
    gotBack: Double,
    stillOwed: Double,
    currencySymbol: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    if (lentOut <= 0.0 && gotBack <= 0.0 && stillOwed <= 0.0) return
    InsightCard(
        title = "Lending",
        subtitle = "Kept separate from your spending",
        modifier = modifier.clickable(onClick = onClick).testTag("insights_lending_strip")
    ) {
        Row(modifier = Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
            FlowFigure("Lent", money(lentOut, currencySymbol), MaterialTheme.colorScheme.primary, Modifier.weight(1f))
            FlowFigure("Got back", money(gotBack, currencySymbol), IncomeGreen, Modifier.weight(1f))
            FlowFigure("Still owed", money(stillOwed, currencySymbol), MaterialTheme.colorScheme.onSurface, Modifier.weight(1f))
            Icon(Icons.Default.ChevronRight, contentDescription = "Open lending", tint = MaterialTheme.colorScheme.outline)
        }
    }
}
