package com.example.ui.screens

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.core.tween
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowLeft
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowRight
import androidx.compose.material.icons.filled.ArrowDownward
import androidx.compose.material.icons.filled.ArrowUpward
import androidx.compose.material.icons.filled.ChevronRight
import androidx.compose.material.icons.filled.EventBusy
import androidx.compose.material.icons.filled.ExpandMore
import androidx.compose.material.icons.filled.SwapVert
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.lerp
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.AutoShrinkText
import com.example.GlassSegmentedControl
import com.example.data.model.Category
import com.example.data.model.Subcategory
import com.example.data.model.Transaction
import com.example.data.model.displayName
import com.example.formatInRupee
import com.example.getContrastColorFor
import com.example.getIconVector
import com.example.parseHexColor
import com.example.subcategoryIconVector
import com.example.ui.theme.ExpenseRed
import com.example.ui.theme.ExpenseRedContainer
import com.example.ui.theme.GlassCard
import com.example.ui.theme.IncomeGreen
import com.example.ui.theme.IncomeGreenContainer
import com.example.ui.theme.accentGlow
import com.example.ui.theme.liquidGlass
import com.example.ui.theme.selectedPillBrush
import com.example.ui.theme.selectedPillRim
import java.text.NumberFormat
import java.time.DayOfWeek
import java.time.Instant
import java.time.LocalDate
import java.time.YearMonth
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import java.time.format.TextStyle
import java.time.temporal.TemporalAdjusters
import java.time.temporal.WeekFields
import java.util.Locale
import kotlin.math.abs

// ─── Aggregation ─────────────────────────────────────────────────────────────────────────────
// Pure helpers behind the calendar, kept free of Compose so they can be unit tested.

data class DayTotals(
    val income: Double = 0.0,
    val expense: Double = 0.0,
    val count: Int = 0
)

/** One category's share of a day's income or spending. [category] is null when it was deleted. */
data class DayCategorySlice(
    val categoryId: Int,
    val category: Category?,
    val amount: Double,
    val share: Double,
    val count: Int
)

fun Transaction.localDate(zone: ZoneId = ZoneId.systemDefault()): LocalDate =
    Instant.ofEpochMilli(timestamp).atZone(zone).toLocalDate()

/** Income, spending and record count for every day that has at least one transaction. */
fun dailyTotals(
    transactions: List<Transaction>,
    zone: ZoneId = ZoneId.systemDefault()
): Map<LocalDate, DayTotals> =
    transactions
        .groupBy { it.localDate(zone) }
        .mapValues { (_, dayTx) ->
            DayTotals(
                income = dayTx.filter { it.type == "INCOME" }.sumOf { it.amount },
                expense = dayTx.filter { it.type == "EXPENSE" }.sumOf { it.amount },
                count = dayTx.size
            )
        }

/** [type] transactions of a single day grouped by category, largest first. */
fun dayCategoryBreakdown(
    dayTransactions: List<Transaction>,
    categories: List<Category>,
    type: String
): List<DayCategorySlice> {
    val typed = dayTransactions.filter { it.type == type }
    val total = typed.sumOf { it.amount }
    val byId = categories.associateBy { it.id }
    return typed
        .groupBy { it.categoryId }
        .map { (categoryId, txs) ->
            val amount = txs.sumOf { it.amount }
            DayCategorySlice(
                categoryId = categoryId,
                category = byId[categoryId],
                amount = amount,
                share = if (total > 0.0) amount / total * 100.0 else 0.0,
                count = txs.size
            )
        }
        .sortedWith(compareByDescending<DayCategorySlice> { it.amount }.thenBy { it.category?.displayName ?: "" })
}

/**
 * Short amount for the tight calendar cells: full figure below 10k, then k / L / Cr for the rupee
 * (k / M / B elsewhere). "₹2,430", "₹12.4k", "₹1.2L".
 */
fun formatCompactAmount(amount: Double, currency: String): String {
    val value = abs(amount)
    val indian = currency == "₹"
    fun oneDecimal(v: Double): String {
        val text = String.format(Locale.US, "%.1f", v)
        return if (text.endsWith(".0")) text.dropLast(2) else text
    }
    val body = when {
        indian && value >= 1_00_00_000 -> oneDecimal(value / 1_00_00_000) + "Cr"
        indian && value >= 1_00_000 -> oneDecimal(value / 1_00_000) + "L"
        !indian && value >= 1_000_000_000 -> oneDecimal(value / 1_000_000_000) + "B"
        !indian && value >= 1_000_000 -> oneDecimal(value / 1_000_000) + "M"
        value >= 10_000 -> oneDecimal(value / 1_000) + "k"
        else -> NumberFormat.getNumberInstance(if (indian) Locale("en", "IN") else Locale.US)
            .apply { maximumFractionDigits = 0 }
            .format(value)
    }
    return currency + body
}

/** The first date of the week holding [date], honouring the locale's first day of week. */
fun startOfWeek(date: LocalDate, firstDay: DayOfWeek): LocalDate =
    date.with(TemporalAdjusters.previousOrSame(firstDay))

private val calendarDayFormatter: DateTimeFormatter
    get() = DateTimeFormatter.ofPattern("d MMMM yyyy", Locale.getDefault())

private fun uncategorizedColor() = Color(0xFF777777)

// ─── Calendar (month / week / list) ──────────────────────────────────────────────────────────

enum class CalendarView(val label: String) { MONTH("Month"), WEEK("Week"), LIST("List") }

@Composable
fun SpendingCalendarScreen(
    transactions: List<Transaction>,
    categories: List<Category>,
    month: YearMonth,
    currencySymbol: String,
    onMonthChange: (YearMonth) -> Unit,
    onPickMonth: () -> Unit,
    onBack: () -> Unit,
    onOpenDay: (LocalDate) -> Unit,
    modifier: Modifier = Modifier
) {
    val today = remember { LocalDate.now() }
    val firstDayOfWeek = remember { WeekFields.of(Locale.getDefault()).firstDayOfWeek }
    val totals = remember(transactions) { dailyTotals(transactions) }

    var view by rememberSaveable { mutableStateOf(CalendarView.MONTH) }
    // Saved as an epoch day so it survives the trip into a day's breakdown and back
    var selectedEpochDay by rememberSaveable { mutableStateOf(defaultSelection(month, today, totals).toEpochDay()) }
    val selectedDate = LocalDate.ofEpochDay(selectedEpochDay)

    // Month changed from the picker sheet or the header arrows: keep the selection inside it
    LaunchedEffect(month) {
        if (YearMonth.from(selectedDate) != month) {
            selectedEpochDay = defaultSelection(month, today, totals).toEpochDay()
        }
    }

    val selectDate: (LocalDate) -> Unit = { date ->
        selectedEpochDay = date.toEpochDay()
        val target = YearMonth.from(date)
        if (target != month) onMonthChange(target)
    }

    val weekStart = startOfWeek(selectedDate, firstDayOfWeek)
    val weekDays = (0L until 7L).map { weekStart.plusDays(it) }
    val rangeTotals = remember(totals, month, view, weekStart) {
        val days = if (view == CalendarView.WEEK) weekDays else (1..month.lengthOfMonth()).map { month.atDay(it) }
        DayTotals(
            income = days.sumOf { totals[it]?.income ?: 0.0 },
            expense = days.sumOf { totals[it]?.expense ?: 0.0 },
            count = days.sumOf { totals[it]?.count ?: 0 }
        )
    }

    val canGoForward = when (view) {
        CalendarView.WEEK -> weekStart.plusDays(7) <= today
        else -> month < YearMonth.from(today)
    }
    val stepBack: () -> Unit = {
        if (view == CalendarView.WEEK) selectDate(selectedDate.minusDays(7)) else onMonthChange(month.minusMonths(1))
    }
    val stepForward: () -> Unit = {
        if (view == CalendarView.WEEK) selectDate(minOf(selectedDate.plusDays(7), today)) else onMonthChange(month.plusMonths(1))
    }

    Column(
        modifier = modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(start = 16.dp, end = 16.dp, top = 4.dp, bottom = 24.dp)
            .testTag("spending_calendar_screen"),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        CalendarTopBar(
            title = month.format(DateTimeFormatter.ofPattern("MMMM yyyy", Locale.getDefault())),
            onBack = onBack,
            onTitleClick = onPickMonth,
            trailing = {
                RoundGlassIconButton(
                    icon = Icons.AutoMirrored.Filled.KeyboardArrowLeft,
                    description = if (view == CalendarView.WEEK) "Previous week" else "Previous month",
                    onClick = stepBack,
                    modifier = Modifier.testTag("calendar_prev_btn")
                )
                Spacer(Modifier.width(8.dp))
                RoundGlassIconButton(
                    icon = Icons.AutoMirrored.Filled.KeyboardArrowRight,
                    description = if (view == CalendarView.WEEK) "Next week" else "Next month",
                    enabled = canGoForward,
                    onClick = stepForward,
                    modifier = Modifier.testTag("calendar_next_btn")
                )
            }
        )

        GlassSegmentedControl(
            options = CalendarView.entries.map { it.label },
            selectedIndex = view.ordinal,
            onSelect = { view = CalendarView.entries[it] },
            expand = true,
            height = 42.dp,
            fontSize = 13.sp,
            modifier = Modifier
                .fillMaxWidth()
                .testTag("calendar_view_selector")
        )

        FlowTotalsRow(
            income = rangeTotals.income,
            expense = rangeTotals.expense,
            currencySymbol = currencySymbol,
            caption = if (view == CalendarView.WEEK) "This week" else null
        )

        AnimatedContent(
            targetState = view,
            transitionSpec = { fadeIn(tween(200)) togetherWith fadeOut(tween(200)) },
            label = "calendar_view"
        ) { current ->
            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                when (current) {
                    CalendarView.MONTH -> {
                        MonthGrid(
                            month = month,
                            firstDayOfWeek = firstDayOfWeek,
                            today = today,
                            selectedDate = selectedDate,
                            totals = totals,
                            currencySymbol = currencySymbol,
                            onDayClick = selectDate
                        )
                        SelectedDayCard(
                            date = selectedDate,
                            today = today,
                            transactions = transactions,
                            categories = categories,
                            currencySymbol = currencySymbol,
                            onPrevDay = { selectDate(selectedDate.minusDays(1)) },
                            onNextDay = { selectDate(selectedDate.plusDays(1)) },
                            onOpen = { onOpenDay(selectedDate) }
                        )
                    }
                    CalendarView.WEEK -> {
                        WeekList(
                            days = weekDays,
                            today = today,
                            selectedDate = selectedDate,
                            totals = totals,
                            currencySymbol = currencySymbol,
                            onDayClick = selectDate
                        )
                        SelectedDayCard(
                            date = selectedDate,
                            today = today,
                            transactions = transactions,
                            categories = categories,
                            currencySymbol = currencySymbol,
                            onPrevDay = { selectDate(selectedDate.minusDays(1)) },
                            onNextDay = { selectDate(selectedDate.plusDays(1)) },
                            onOpen = { onOpenDay(selectedDate) }
                        )
                    }
                    CalendarView.LIST -> MonthDayList(
                        month = month,
                        today = today,
                        totals = totals,
                        currencySymbol = currencySymbol,
                        onDayClick = { date ->
                            selectDate(date)
                            onOpenDay(date)
                        }
                    )
                }
            }
        }
    }
}

/** Today in the current month, else the latest day with activity, else the 1st. */
private fun defaultSelection(month: YearMonth, today: LocalDate, totals: Map<LocalDate, DayTotals>): LocalDate =
    if (YearMonth.from(today) == month) today
    else totals.keys.filter { YearMonth.from(it) == month }.maxOrNull() ?: month.atDay(1)

@Composable
private fun MonthGrid(
    month: YearMonth,
    firstDayOfWeek: DayOfWeek,
    today: LocalDate,
    selectedDate: LocalDate,
    totals: Map<LocalDate, DayTotals>,
    currencySymbol: String,
    onDayClick: (LocalDate) -> Unit
) {
    val gridStart = startOfWeek(month.atDay(1), firstDayOfWeek)
    val weeks = ((month.atDay(1).toEpochDay() - gridStart.toEpochDay() + month.lengthOfMonth() + 6) / 7).toInt()
    val peakSpend = (1..month.lengthOfMonth()).maxOfOrNull { totals[month.atDay(it)]?.expense ?: 0.0 } ?: 0.0

    GlassCard(
        modifier = Modifier
            .fillMaxWidth()
            .testTag("calendar_month_grid"),
        shape = RoundedCornerShape(24.dp),
        elevation = 6.dp,
        contentPadding = PaddingValues(8.dp)
    ) {
        Row(modifier = Modifier.fillMaxWidth().padding(vertical = 6.dp)) {
            (0L until 7L).forEach { offset ->
                Text(
                    text = firstDayOfWeek.plus(offset).getDisplayName(TextStyle.SHORT, Locale.getDefault()),
                    fontSize = 12.sp,
                    fontWeight = FontWeight.SemiBold,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    textAlign = TextAlign.Center,
                    modifier = Modifier.weight(1f)
                )
            }
        }
        Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
            repeat(weeks) { week ->
                Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                    repeat(7) { dayIndex ->
                        val date = gridStart.plusDays((week * 7 + dayIndex).toLong())
                        CalendarDayCell(
                            date = date,
                            inMonth = YearMonth.from(date) == month,
                            isToday = date == today,
                            isFuture = date.isAfter(today),
                            isSelected = date == selectedDate,
                            totals = totals[date],
                            peakSpend = peakSpend,
                            currencySymbol = currencySymbol,
                            onClick = { onDayClick(date) },
                            modifier = Modifier.weight(1f)
                        )
                    }
                }
            }
        }
        Spacer(Modifier.height(10.dp))
        CalendarLegend()
        Spacer(Modifier.height(4.dp))
    }
}

@Composable
private fun CalendarDayCell(
    date: LocalDate,
    inMonth: Boolean,
    isToday: Boolean,
    isFuture: Boolean,
    isSelected: Boolean,
    totals: DayTotals?,
    peakSpend: Double,
    currencySymbol: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val accent = MaterialTheme.colorScheme.primary
    val shape = RoundedCornerShape(12.dp)
    val spend = totals?.expense ?: 0.0
    val income = totals?.income ?: 0.0

    // Days outside the month keep the grid square but stay quiet and inert
    if (!inMonth) {
        Box(
            modifier = modifier.height(66.dp),
            contentAlignment = Alignment.TopCenter
        ) {
            Text(
                text = date.dayOfMonth.toString(),
                fontSize = 14.sp,
                fontWeight = FontWeight.Medium,
                color = MaterialTheme.colorScheme.outline.copy(alpha = 0.45f),
                modifier = Modifier.padding(top = 8.dp)
            )
        }
        return
    }

    // Heat tint: the busier the spending day, the warmer the cell
    val heat = if (peakSpend > 0.0 && spend > 0.0) (0.06f + 0.16f * (spend / peakSpend).toFloat()) else 0f
    val background = when {
        isSelected -> Modifier
            .accentGlow(accent, shape, 8.dp)
            .clip(shape)
            .background(selectedPillBrush(accent))
            .border(0.8.dp, selectedPillRim(accent), shape)
        heat > 0f -> Modifier
            .clip(shape)
            .background(ExpenseRed.copy(alpha = heat))
        else -> Modifier
            .clip(shape)
            .background(MaterialTheme.colorScheme.surface.copy(alpha = 0.25f))
    }
    val todayRing = if (isToday && !isSelected) Modifier.border(1.4.dp, accent, shape) else Modifier
    val onSelected = Color.White
    val spendColor = if (isSelected) onSelected else lerp(MaterialTheme.colorScheme.onSurface, ExpenseRed, 0.45f)
    val incomeColor = if (isSelected) onSelected.copy(alpha = 0.9f) else IncomeGreen

    Column(
        modifier = modifier
            .height(66.dp)
            .then(background)
            .then(todayRing)
            .clickable(onClick = onClick)
            .alpha(if (isFuture) 0.45f else 1f)
            .padding(horizontal = 1.dp, vertical = 6.dp)
            .testTag("calendar_day_${date}"),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text(
            text = date.dayOfMonth.toString(),
            fontSize = 15.sp,
            lineHeight = 18.sp,
            fontWeight = if (isSelected || isToday) FontWeight.ExtraBold else FontWeight.Bold,
            color = when {
                isSelected -> onSelected
                isToday -> accent
                else -> MaterialTheme.colorScheme.onSurface
            }
        )
        Spacer(Modifier.weight(1f))
        if (spend > 0.0) {
            CellAmount(text = formatCompactAmount(spend, currencySymbol), color = spendColor)
        }
        if (income > 0.0) {
            CellAmount(text = "+" + formatCompactAmount(income, currencySymbol), color = incomeColor)
        }
    }
}

@Composable
private fun CellAmount(text: String, color: Color) {
    Text(
        text = text,
        fontSize = 9.5.sp,
        lineHeight = 12.sp,
        fontWeight = FontWeight.SemiBold,
        letterSpacing = (-0.2).sp,
        color = color,
        maxLines = 1,
        softWrap = false,
        overflow = TextOverflow.Clip
    )
}

@Composable
private fun CalendarLegend() {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .liquidGlass(shape = RoundedCornerShape(50), strength = 0.6f)
            .padding(horizontal = 14.dp, vertical = 8.dp),
        horizontalArrangement = Arrangement.SpaceEvenly,
        verticalAlignment = Alignment.CenterVertically
    ) {
        LegendItem(color = IncomeGreen, label = "Income")
        LegendItem(color = ExpenseRed, label = "Spending")
        Row(verticalAlignment = Alignment.CenterVertically) {
            Box(
                modifier = Modifier
                    .size(10.dp)
                    .border(1.4.dp, MaterialTheme.colorScheme.primary, RoundedCornerShape(3.dp))
            )
            Spacer(Modifier.width(6.dp))
            Text("Today", fontSize = 11.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
    }
}

@Composable
private fun LegendItem(color: Color, label: String) {
    Row(verticalAlignment = Alignment.CenterVertically) {
        Box(
            modifier = Modifier
                .size(8.dp)
                .clip(CircleShape)
                .background(color)
        )
        Spacer(Modifier.width(6.dp))
        Text(label, fontSize = 11.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
    }
}

// Summary for the day last tapped in the grid, with day-stepping and its top categories
@Composable
private fun SelectedDayCard(
    date: LocalDate,
    today: LocalDate,
    transactions: List<Transaction>,
    categories: List<Category>,
    currencySymbol: String,
    onPrevDay: () -> Unit,
    onNextDay: () -> Unit,
    onOpen: () -> Unit
) {
    val dayTx = remember(transactions, date) { transactions.filter { it.localDate() == date } }
    val income = dayTx.filter { it.type == "INCOME" }.sumOf { it.amount }
    val spend = dayTx.filter { it.type == "EXPENSE" }.sumOf { it.amount }
    val top = remember(dayTx, categories) { dayCategoryBreakdown(dayTx, categories, "EXPENSE").take(3) }
    val hasActivity = dayTx.isNotEmpty()

    GlassCard(
        modifier = Modifier
            .fillMaxWidth()
            .testTag("calendar_selected_day_card"),
        shape = RoundedCornerShape(24.dp),
        elevation = 6.dp,
        contentPadding = PaddingValues(14.dp)
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = date.format(calendarDayFormatter),
                    fontSize = 17.sp,
                    fontWeight = FontWeight.ExtraBold,
                    color = MaterialTheme.colorScheme.onSurface
                )
                Text(
                    text = relativeDayLabel(date, today),
                    fontSize = 12.sp,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
            RoundGlassIconButton(
                icon = Icons.AutoMirrored.Filled.KeyboardArrowLeft,
                description = "Previous day",
                size = 36,
                onClick = onPrevDay
            )
            Spacer(Modifier.width(8.dp))
            RoundGlassIconButton(
                icon = Icons.AutoMirrored.Filled.KeyboardArrowRight,
                description = "Next day",
                size = 36,
                enabled = date < today,
                onClick = onNextDay
            )
        }

        Spacer(Modifier.height(12.dp))

        if (!hasActivity) {
            EmptyNote(
                icon = Icons.Default.EventBusy,
                text = if (date.isAfter(today)) "This day hasn't happened yet." else "No transactions on this day."
            )
            return@GlassCard
        }

        FlowTotalsRow(income = income, expense = spend, currencySymbol = currencySymbol, compact = true)

        if (top.isNotEmpty()) {
            Spacer(Modifier.height(14.dp))
            Text(
                text = "Top Categories",
                fontSize = 13.sp,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onSurface
            )
            Spacer(Modifier.height(8.dp))
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                top.forEach { slice -> TopCategoryBar(slice, currencySymbol) }
            }
        }

        Spacer(Modifier.height(12.dp))
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .liquidGlass(shape = RoundedCornerShape(14.dp), strength = 0.7f)
                .clickable(onClick = onOpen)
                .padding(horizontal = 14.dp, vertical = 11.dp)
                .testTag("calendar_open_day_btn"),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = "View category breakdown",
                fontSize = 13.sp,
                fontWeight = FontWeight.SemiBold,
                color = MaterialTheme.colorScheme.primary,
                modifier = Modifier.weight(1f)
            )
            Icon(
                imageVector = Icons.Default.ChevronRight,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.primary,
                modifier = Modifier.size(18.dp)
            )
        }
    }
}

@Composable
private fun TopCategoryBar(slice: DayCategorySlice, currencySymbol: String) {
    val color = slice.category?.let { parseHexColor(it.colorHex) } ?: uncategorizedColor()
    Row(verticalAlignment = Alignment.CenterVertically) {
        Box(
            modifier = Modifier
                .size(10.dp)
                .clip(CircleShape)
                .background(color)
        )
        Spacer(Modifier.width(8.dp))
        Text(
            text = slice.category?.displayName ?: "Uncategorized",
            fontSize = 12.sp,
            color = MaterialTheme.colorScheme.onSurface,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
            modifier = Modifier.width(96.dp)
        )
        Box(
            modifier = Modifier
                .weight(1f)
                .height(7.dp)
                .clip(RoundedCornerShape(50))
                .background(MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.3f))
        ) {
            Box(
                modifier = Modifier
                    .fillMaxHeight()
                    .fillMaxWidth((slice.share / 100.0).toFloat().coerceIn(0.02f, 1f))
                    .clip(RoundedCornerShape(50))
                    .background(color)
            )
        }
        Spacer(Modifier.width(8.dp))
        Text(
            text = "${String.format(Locale.US, "%.0f", slice.share)}%",
            fontSize = 11.sp,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            textAlign = TextAlign.End,
            modifier = Modifier.width(32.dp)
        )
        Text(
            text = formatInRupee(slice.amount, currencySymbol),
            fontSize = 12.sp,
            fontWeight = FontWeight.SemiBold,
            color = MaterialTheme.colorScheme.onSurface,
            textAlign = TextAlign.End,
            maxLines = 1,
            modifier = Modifier.widthIn(min = 64.dp)
        )
    }
}

@Composable
private fun WeekList(
    days: List<LocalDate>,
    today: LocalDate,
    selectedDate: LocalDate,
    totals: Map<LocalDate, DayTotals>,
    currencySymbol: String,
    onDayClick: (LocalDate) -> Unit
) {
    val peak = days.maxOfOrNull { maxOf(totals[it]?.expense ?: 0.0, totals[it]?.income ?: 0.0) } ?: 0.0
    GlassCard(
        modifier = Modifier
            .fillMaxWidth()
            .testTag("calendar_week_list"),
        shape = RoundedCornerShape(24.dp),
        elevation = 6.dp,
        contentPadding = PaddingValues(6.dp)
    ) {
        days.forEachIndexed { index, date ->
            if (index > 0) SoftDivider()
            val dayTotals = totals[date]
            DaySummaryRow(
                date = date,
                today = today,
                isSelected = date == selectedDate,
                totals = dayTotals,
                barPeak = peak,
                currencySymbol = currencySymbol,
                enabled = !date.isAfter(today),
                showChevron = false,
                onClick = { onDayClick(date) }
            )
        }
    }
}

@Composable
private fun MonthDayList(
    month: YearMonth,
    today: LocalDate,
    totals: Map<LocalDate, DayTotals>,
    currencySymbol: String,
    onDayClick: (LocalDate) -> Unit
) {
    val activeDays = (1..month.lengthOfMonth())
        .map { month.atDay(it) }
        .filter { (totals[it]?.count ?: 0) > 0 }
        .sortedDescending()
    val peak = activeDays.maxOfOrNull { maxOf(totals[it]?.expense ?: 0.0, totals[it]?.income ?: 0.0) } ?: 0.0

    GlassCard(
        modifier = Modifier
            .fillMaxWidth()
            .testTag("calendar_day_list"),
        shape = RoundedCornerShape(24.dp),
        elevation = 6.dp,
        contentPadding = PaddingValues(6.dp)
    ) {
        if (activeDays.isEmpty()) {
            Box(Modifier.padding(12.dp)) {
                EmptyNote(icon = Icons.Default.EventBusy, text = "No transactions recorded this month.")
            }
            return@GlassCard
        }
        activeDays.forEachIndexed { index, date ->
            if (index > 0) SoftDivider()
            DaySummaryRow(
                date = date,
                today = today,
                isSelected = false,
                totals = totals[date],
                barPeak = peak,
                currencySymbol = currencySymbol,
                enabled = true,
                onClick = { onDayClick(date) }
            )
        }
    }
}

// One day as a row: date badge, income / spending figures with proportional bars, chevron
@Composable
private fun DaySummaryRow(
    date: LocalDate,
    today: LocalDate,
    isSelected: Boolean,
    totals: DayTotals?,
    barPeak: Double,
    currencySymbol: String,
    enabled: Boolean,
    onClick: () -> Unit,
    showChevron: Boolean = true
) {
    val accent = MaterialTheme.colorScheme.primary
    val badgeShape = RoundedCornerShape(12.dp)
    val income = totals?.income ?: 0.0
    val spend = totals?.expense ?: 0.0
    val count = totals?.count ?: 0

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(16.dp))
            .background(if (isSelected) accent.copy(alpha = 0.10f) else Color.Transparent)
            .clickable(enabled = enabled, onClick = onClick)
            .alpha(if (enabled) 1f else 0.45f)
            .padding(horizontal = 8.dp, vertical = 9.dp)
            .testTag("calendar_row_${date}"),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Column(
            modifier = Modifier
                .size(46.dp)
                .then(
                    if (date == today) Modifier.clip(badgeShape).background(selectedPillBrush(accent))
                    else Modifier.liquidGlass(shape = badgeShape, strength = 0.7f)
                ),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            val onBadge = if (date == today) Color.White else MaterialTheme.colorScheme.onSurface
            Text(
                text = date.dayOfWeek.getDisplayName(TextStyle.SHORT, Locale.getDefault()).uppercase(),
                fontSize = 9.sp,
                lineHeight = 11.sp,
                fontWeight = FontWeight.Bold,
                color = onBadge.copy(alpha = 0.75f)
            )
            Text(
                text = date.dayOfMonth.toString(),
                fontSize = 17.sp,
                lineHeight = 20.sp,
                fontWeight = FontWeight.ExtraBold,
                color = onBadge
            )
        }
        Spacer(Modifier.width(12.dp))
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = when (count) {
                    0 -> "No transactions"
                    1 -> "1 transaction"
                    else -> "$count transactions"
                },
                fontSize = 12.sp,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            if (count > 0) {
                Spacer(Modifier.height(5.dp))
                FlowBar(amount = spend, peak = barPeak, color = ExpenseRed)
                if (income > 0.0) {
                    Spacer(Modifier.height(3.dp))
                    FlowBar(amount = income, peak = barPeak, color = IncomeGreen)
                }
            }
        }
        Spacer(Modifier.width(12.dp))
        Column(horizontalAlignment = Alignment.End) {
            if (spend > 0.0) {
                Text(
                    text = "-" + formatInRupee(spend, currencySymbol),
                    fontSize = 13.sp,
                    fontWeight = FontWeight.Bold,
                    color = ExpenseRed,
                    maxLines = 1
                )
            }
            if (income > 0.0) {
                Text(
                    text = "+" + formatInRupee(income, currencySymbol),
                    fontSize = 13.sp,
                    fontWeight = FontWeight.Bold,
                    color = IncomeGreen,
                    maxLines = 1
                )
            }
            if (count == 0) {
                Text("—", fontSize = 13.sp, color = MaterialTheme.colorScheme.outline)
            }
        }
        Spacer(Modifier.width(2.dp))
        Icon(
            imageVector = Icons.Default.ChevronRight,
            contentDescription = null,
            tint = if (showChevron && count > 0) MaterialTheme.colorScheme.outline else Color.Transparent,
            modifier = Modifier.size(18.dp)
        )
    }
}

@Composable
private fun FlowBar(amount: Double, peak: Double, color: Color) {
    val fraction = if (peak > 0.0) (amount / peak).toFloat().coerceIn(0f, 1f) else 0f
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .height(4.dp)
            .clip(RoundedCornerShape(50))
            .background(MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.25f))
    ) {
        if (fraction > 0f) {
            Box(
                modifier = Modifier
                    .fillMaxHeight()
                    .fillMaxWidth(fraction.coerceAtLeast(0.03f))
                    .clip(RoundedCornerShape(50))
                    .background(color)
            )
        }
    }
}

// ─── Day breakdown ───────────────────────────────────────────────────────────────────────────

@Composable
fun CalendarDayDetailScreen(
    date: LocalDate,
    transactions: List<Transaction>,
    categories: List<Category>,
    currencySymbol: String,
    onBack: () -> Unit,
    onOpenCategory: (type: String, categoryId: Int) -> Unit,
    modifier: Modifier = Modifier
) {
    val today = remember { LocalDate.now() }
    val dayTx = remember(transactions, date) { transactions.filter { it.localDate() == date } }
    val spending = remember(dayTx, categories) { dayCategoryBreakdown(dayTx, categories, "EXPENSE") }
    val income = remember(dayTx, categories) { dayCategoryBreakdown(dayTx, categories, "INCOME") }

    Column(
        modifier = modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(start = 16.dp, end = 16.dp, top = 4.dp, bottom = 24.dp)
            .testTag("calendar_day_detail_screen"),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        CalendarTopBar(
            title = date.format(calendarDayFormatter),
            subtitle = relativeDayLabel(date, today),
            onBack = onBack
        )

        FlowTotalsRow(
            income = income.sumOf { it.amount },
            expense = spending.sumOf { it.amount },
            currencySymbol = currencySymbol
        )

        if (dayTx.isEmpty()) {
            GlassCard(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(24.dp),
                contentPadding = PaddingValues(16.dp)
            ) {
                EmptyNote(icon = Icons.Default.EventBusy, text = "No transactions on this day.")
            }
            return@Column
        }

        if (spending.isNotEmpty()) {
            SectionTitle("Spending Breakdown")
            CategorySliceList(
                slices = spending,
                currencySymbol = currencySymbol,
                onClick = { onOpenCategory("EXPENSE", it.categoryId) },
                modifier = Modifier.testTag("calendar_day_spending_list")
            )
        }
        if (income.isNotEmpty()) {
            SectionTitle("Income")
            CategorySliceList(
                slices = income,
                currencySymbol = currencySymbol,
                onClick = { onOpenCategory("INCOME", it.categoryId) },
                modifier = Modifier.testTag("calendar_day_income_list")
            )
        }
    }
}

@Composable
private fun CategorySliceList(
    slices: List<DayCategorySlice>,
    currencySymbol: String,
    onClick: (DayCategorySlice) -> Unit,
    modifier: Modifier = Modifier
) {
    GlassCard(
        modifier = modifier.fillMaxWidth(),
        shape = RoundedCornerShape(22.dp),
        elevation = 6.dp,
        contentPadding = PaddingValues(4.dp)
    ) {
        slices.forEachIndexed { index, slice ->
            if (index > 0) SoftDivider()
            val color = slice.category?.let { parseHexColor(it.colorHex) } ?: uncategorizedColor()
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(18.dp))
                    .clickable { onClick(slice) }
                    .padding(horizontal = 10.dp, vertical = 10.dp)
                    .testTag("calendar_category_${slice.categoryId}"),
                verticalAlignment = Alignment.CenterVertically
            ) {
                CategoryBadge(
                    icon = getIconVector(slice.category?.iconName ?: "help"),
                    color = color,
                    size = 42
                )
                Spacer(Modifier.width(12.dp))
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = slice.category?.displayName ?: "Uncategorized",
                        fontSize = 14.sp,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onSurface,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                    Text(
                        text = if (slice.count == 1) "1 transaction" else "${slice.count} transactions",
                        fontSize = 11.sp,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
                Text(
                    text = formatInRupee(slice.amount, currencySymbol),
                    fontSize = 14.sp,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onSurface,
                    maxLines = 1
                )
                Text(
                    text = "${String.format(Locale.US, "%.0f", slice.share)}%",
                    fontSize = 12.sp,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    textAlign = TextAlign.End,
                    modifier = Modifier.width(44.dp)
                )
                Spacer(Modifier.width(4.dp))
                Icon(
                    imageVector = Icons.Default.ChevronRight,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.outline,
                    modifier = Modifier.size(18.dp)
                )
            }
        }
    }
}

// ─── One category on one day ─────────────────────────────────────────────────────────────────

@Composable
fun CalendarCategoryTransactionsScreen(
    date: LocalDate,
    type: String,
    categoryId: Int,
    transactions: List<Transaction>,
    categories: List<Category>,
    subcategories: List<Subcategory>,
    currencySymbol: String,
    onBack: () -> Unit,
    modifier: Modifier = Modifier
) {
    val category = categories.find { it.id == categoryId }
    val color = category?.let { parseHexColor(it.colorHex) } ?: uncategorizedColor()
    val isIncome = type == "INCOME"
    var sortByAmount by rememberSaveable { mutableStateOf(false) }

    val dayTyped = remember(transactions, date, type) {
        transactions.filter { it.type == type && it.localDate() == date }
    }
    val categoryTx = remember(dayTyped, categoryId, sortByAmount) {
        dayTyped
            .filter { it.categoryId == categoryId }
            .let { list -> if (sortByAmount) list.sortedByDescending { it.amount } else list.sortedBy { it.timestamp } }
    }
    val categoryTotal = categoryTx.sumOf { it.amount }
    val dayTotal = dayTyped.sumOf { it.amount }
    val share = if (dayTotal > 0.0) categoryTotal / dayTotal * 100.0 else 0.0
    val timeFormatter = remember { DateTimeFormatter.ofPattern("hh:mm a", Locale.getDefault()) }

    Column(
        modifier = modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(start = 16.dp, end = 16.dp, top = 4.dp, bottom = 24.dp)
            .testTag("calendar_category_screen"),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        CalendarTopBar(
            title = category?.displayName ?: "Uncategorized",
            subtitle = date.format(calendarDayFormatter),
            onBack = onBack
        )

        // Hero: the category's total for the day and its share of that day's flow
        GlassCard(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(24.dp),
            tint = color,
            elevation = 8.dp,
            contentPadding = PaddingValues(16.dp)
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                CategoryBadge(
                    icon = getIconVector(category?.iconName ?: "help"),
                    color = color,
                    size = 60,
                    corner = 18
                )
                Spacer(Modifier.width(16.dp))
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = category?.displayName ?: "Uncategorized",
                        fontSize = 15.sp,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onSurface,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                    AutoShrinkText(
                        text = formatInRupee(categoryTotal, currencySymbol),
                        maxFontSize = 28.sp,
                        minFontSize = 18.sp,
                        color = if (isIncome) IncomeGreen else MaterialTheme.colorScheme.onSurface,
                        style = MaterialTheme.typography.headlineMedium,
                        modifier = Modifier.fillMaxWidth()
                    )
                    Text(
                        text = "${String.format(Locale.US, "%.0f", share)}% of the day's ${if (isIncome) "income" else "spending"}",
                        fontSize = 12.sp,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
        }

        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically
        ) {
            SectionTitle("Transactions (${categoryTx.size})", modifier = Modifier.weight(1f))
            Row(
                modifier = Modifier
                    .clip(RoundedCornerShape(50))
                    .clickable { sortByAmount = !sortByAmount }
                    .padding(horizontal = 10.dp, vertical = 6.dp)
                    .testTag("calendar_sort_toggle"),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = if (sortByAmount) "By amount" else "By time",
                    fontSize = 12.sp,
                    fontWeight = FontWeight.SemiBold,
                    color = MaterialTheme.colorScheme.primary
                )
                Spacer(Modifier.width(4.dp))
                Icon(
                    imageVector = Icons.Default.SwapVert,
                    contentDescription = "Change sort order",
                    tint = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.size(18.dp)
                )
            }
        }

        GlassCard(
            modifier = Modifier
                .fillMaxWidth()
                .testTag("calendar_transaction_list"),
            shape = RoundedCornerShape(22.dp),
            elevation = 6.dp,
            contentPadding = PaddingValues(4.dp)
        ) {
            if (categoryTx.isEmpty()) {
                Box(Modifier.padding(12.dp)) {
                    EmptyNote(icon = Icons.Default.EventBusy, text = "No transactions left in this category for the day.")
                }
                return@GlassCard
            }
            categoryTx.forEachIndexed { index, tx ->
                if (index > 0) SoftDivider()
                val subcategory = tx.subcategoryId?.let { id -> subcategories.find { it.id == id } }
                val rowColor = subcategory?.colorHexOverride?.let { parseHexColor(it) } ?: color
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 10.dp, vertical = 11.dp)
                        .testTag("calendar_tx_${tx.id}"),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    CategoryBadge(
                        icon = if (subcategory != null) subcategoryIconVector(subcategory, category)
                               else getIconVector(category?.iconName ?: "help"),
                        color = rowColor,
                        size = 42
                    )
                    Spacer(Modifier.width(12.dp))
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = tx.description.ifEmpty { "Transaction Record" },
                            fontSize = 14.sp,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onSurface,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                        Text(
                            text = subcategory?.name ?: category?.displayName ?: "Uncategorized",
                            fontSize = 12.sp,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                    }
                    Spacer(Modifier.width(8.dp))
                    Column(horizontalAlignment = Alignment.End) {
                        Text(
                            text = (if (isIncome) "+ " else "- ") + formatInRupee(tx.amount, currencySymbol),
                            fontSize = 15.sp,
                            fontWeight = FontWeight.Black,
                            color = if (isIncome) IncomeGreen else ExpenseRed,
                            maxLines = 1
                        )
                        Text(
                            text = Instant.ofEpochMilli(tx.timestamp).atZone(ZoneId.systemDefault()).format(timeFormatter),
                            fontSize = 11.sp,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            }
        }
    }
}

// ─── Shared pieces ───────────────────────────────────────────────────────────────────────────

@Composable
private fun CalendarTopBar(
    title: String,
    onBack: () -> Unit,
    subtitle: String? = null,
    onTitleClick: (() -> Unit)? = null,
    trailing: @Composable () -> Unit = {}
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(top = 4.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Box(
            modifier = Modifier
                .size(44.dp)
                .clip(CircleShape)
                .clickable(onClick = onBack)
                .testTag("calendar_back_btn"),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                contentDescription = "Go back",
                tint = MaterialTheme.colorScheme.onSurface,
                modifier = Modifier.size(24.dp)
            )
        }
        Spacer(Modifier.width(6.dp))
        Column(modifier = Modifier.weight(1f)) {
            Row(
                modifier = Modifier
                    .clip(RoundedCornerShape(10.dp))
                    .then(if (onTitleClick != null) Modifier.clickable(onClick = onTitleClick) else Modifier)
                    .padding(vertical = 2.dp)
                    .testTag("calendar_title"),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = title,
                    fontSize = 21.sp,
                    lineHeight = 26.sp,
                    fontWeight = FontWeight.Black,
                    letterSpacing = (-0.3).sp,
                    color = MaterialTheme.colorScheme.onBackground,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    modifier = Modifier.weight(1f, fill = false)
                )
                if (onTitleClick != null) {
                    Spacer(Modifier.width(4.dp))
                    Icon(
                        imageVector = Icons.Default.ExpandMore,
                        contentDescription = "Choose month",
                        tint = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.size(22.dp)
                    )
                }
            }
            if (subtitle != null) {
                Text(
                    text = subtitle,
                    fontSize = 13.sp,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }
        }
        Spacer(Modifier.width(8.dp))
        trailing()
    }
}

@Composable
private fun RoundGlassIconButton(
    icon: ImageVector,
    description: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
    size: Int = 40
) {
    Box(
        modifier = modifier
            .size(size.dp)
            .liquidGlass(shape = CircleShape, strength = 0.9f, elevation = 4.dp)
            .clickable(enabled = enabled, onClick = onClick)
            .alpha(if (enabled) 1f else 0.35f),
        contentAlignment = Alignment.Center
    ) {
        Icon(
            imageVector = icon,
            contentDescription = description,
            tint = MaterialTheme.colorScheme.primary,
            modifier = Modifier.size((size * 0.55f).dp)
        )
    }
}

// Income and spending side by side as tinted glass tiles
@Composable
private fun FlowTotalsRow(
    income: Double,
    expense: Double,
    currencySymbol: String,
    caption: String? = null,
    compact: Boolean = false
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        FlowTile(
            label = "Income",
            caption = caption,
            amount = income,
            color = IncomeGreen,
            container = IncomeGreenContainer,
            icon = Icons.Default.ArrowDownward,
            currencySymbol = currencySymbol,
            compact = compact,
            modifier = Modifier
                .weight(1f)
                .testTag("calendar_income_tile")
        )
        FlowTile(
            label = "Spending",
            caption = caption,
            amount = expense,
            color = ExpenseRed,
            container = ExpenseRedContainer,
            icon = Icons.Default.ArrowUpward,
            currencySymbol = currencySymbol,
            compact = compact,
            modifier = Modifier
                .weight(1f)
                .testTag("calendar_spending_tile")
        )
    }
}

@Composable
private fun FlowTile(
    label: String,
    caption: String?,
    amount: Double,
    color: Color,
    container: Color,
    icon: ImageVector,
    currencySymbol: String,
    compact: Boolean,
    modifier: Modifier = Modifier
) {
    val shape = RoundedCornerShape(if (compact) 14.dp else 18.dp)
    Row(
        modifier = modifier
            .liquidGlass(shape = shape, tint = color, strength = if (compact) 0.7f else 0.95f, elevation = if (compact) 0.dp else 4.dp)
            .padding(horizontal = 12.dp, vertical = if (compact) 9.dp else 13.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Box(
            modifier = Modifier
                .size(if (compact) 28.dp else 34.dp)
                .clip(CircleShape)
                .background(container),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                imageVector = icon,
                contentDescription = null,
                tint = color,
                modifier = Modifier.size(if (compact) 16.dp else 19.dp)
            )
        }
        Spacer(Modifier.width(10.dp))
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = if (caption != null) "$label · $caption" else label,
                fontSize = 11.sp,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
            AutoShrinkText(
                text = formatInRupee(amount, currencySymbol),
                maxFontSize = if (compact) 15.sp else 18.sp,
                minFontSize = 11.sp,
                color = MaterialTheme.colorScheme.onSurface,
                fontWeight = FontWeight.ExtraBold,
                modifier = Modifier.fillMaxWidth()
            )
        }
    }
}

@Composable
private fun CategoryBadge(icon: ImageVector, color: Color, size: Int, corner: Int? = null) {
    val shape = if (corner != null) RoundedCornerShape(corner.dp) else CircleShape
    Box(
        modifier = Modifier
            .size(size.dp)
            .accentGlow(color, shape, 6.dp)
            .clip(shape)
            .background(color),
        contentAlignment = Alignment.Center
    ) {
        Icon(
            imageVector = icon,
            contentDescription = null,
            tint = getContrastColorFor(color),
            modifier = Modifier.size((size * 0.5f).dp)
        )
    }
}

@Composable
private fun SectionTitle(text: String, modifier: Modifier = Modifier) {
    Text(
        text = text,
        fontSize = 16.sp,
        fontWeight = FontWeight.ExtraBold,
        color = MaterialTheme.colorScheme.onBackground,
        modifier = modifier.padding(top = 4.dp, start = 2.dp)
    )
}

@Composable
private fun SoftDivider() {
    HorizontalDivider(
        thickness = 0.6.dp,
        color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.3f),
        modifier = Modifier.padding(horizontal = 10.dp)
    )
}

@Composable
private fun EmptyNote(icon: ImageVector, text: String) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.Center
    ) {
        Icon(
            imageVector = icon,
            contentDescription = null,
            tint = MaterialTheme.colorScheme.outline,
            modifier = Modifier.size(18.dp)
        )
        Spacer(Modifier.width(8.dp))
        Text(
            text = text,
            fontSize = 13.sp,
            color = MaterialTheme.colorScheme.outline
        )
    }
}

private fun relativeDayLabel(date: LocalDate, today: LocalDate): String {
    val weekday = date.dayOfWeek.getDisplayName(TextStyle.FULL, Locale.getDefault())
    return when (date) {
        today -> "Today · $weekday"
        today.minusDays(1) -> "Yesterday · $weekday"
        else -> weekday
    }
}
