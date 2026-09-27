package com.example.ui.screens

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.gestures.scrollBy
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
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
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.TrendingUp
import androidx.compose.material.icons.filled.AccountBalanceWallet
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.BarChart
import androidx.compose.material.icons.filled.Category
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.DragIndicator
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.Lightbulb
import androidx.compose.material.icons.filled.MoreHoriz
import androidx.compose.material.icons.filled.Restore
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Tune
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.runtime.snapshots.SnapshotStateList
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.rotate
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.lerp
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.zIndex
import com.example.data.model.Category
import com.example.data.model.Subcategory
import com.example.data.model.displayName
import com.example.formatInRupee
import com.example.getContrastColorFor
import com.example.getIconVector
import com.example.parseHexColor
import com.example.ui.theme.ExpenseRed
import com.example.ui.theme.GlassCard
import com.example.ui.theme.IncomeGreen
import com.example.ui.theme.liquidGlass
import com.example.ui.viewmodel.CategoryStats
import kotlinx.coroutines.launch
import kotlin.math.roundToInt

/** Ordering options behind the search row's tune button. Drag-to-reorder only applies to [CUSTOM]. */
enum class CategorySort(val label: String) {
    CUSTOM("Custom order"),
    NAME("Name A–Z"),
    SPENT("Highest spend"),
    BUDGET("Highest budget")
}

private val CardShape = RoundedCornerShape(22.dp)
private val FieldShape = RoundedCornerShape(16.dp)
private const val SYSTEM_LENDING = "Lending"
private const val SYSTEM_LOAN = "Loan Repayment"

private fun categoryKey(id: Int) = "cat-$id"

/**
 * Manage Categories: the list of expense / income categories with budgets, subcategory chips and
 * drag-to-reorder.
 *
 * [customOrder] is the caller-owned list of category ids in the user's preferred order; it is
 * synced against [categories] here so newly created ones append and deleted ones drop out.
 */
@OptIn(ExperimentalLayoutApi::class)
@Composable
fun ManageCategoriesScreen(
    categories: List<Category>,
    subcategories: List<Subcategory>,
    stats: Map<Int, CategoryStats>,
    currencySymbol: String,
    selectedType: String,
    onSelectType: (String) -> Unit,
    customOrder: SnapshotStateList<Int>,
    onBack: () -> Unit,
    onAddCategory: () -> Unit,
    onEditCategory: (Category) -> Unit,
    onDeleteCategory: (Category) -> Unit,
    modifier: Modifier = Modifier
) {
    val haptic = LocalHapticFeedback.current
    val scope = rememberCoroutineScope()
    val listState = rememberLazyListState()

    val isIncome = selectedType == "INCOME"
    val accent = if (isIncome) IncomeGreen else MaterialTheme.colorScheme.primary

    var searchQuery by rememberSaveable { mutableStateOf("") }
    var sort by rememberSaveable { mutableStateOf(CategorySort.CUSTOM) }
    var tipDismissed by rememberSaveable { mutableStateOf(false) }
    var showMoreMenu by remember { mutableStateOf(false) }
    var showSortMenu by remember { mutableStateOf(false) }

    // Keep the custom order in step with the live category list
    LaunchedEffect(categories) {
        val ids = categories.map { it.id }
        customOrder.removeAll { it !in ids }
        ids.forEach { if (it !in customOrder) customOrder.add(it) }
    }
    val orderIndex = customOrder.withIndex().associate { it.value to it.index }
    val orderIsDefault = customOrder.toList() == categories.map { it.id }

    val expenseCount = categories.count { it.type == "EXPENSE" }
    val incomeCount = categories.count { it.type == "INCOME" }
    val typed = categories.filter { it.type.equals(selectedType, ignoreCase = true) }
    val totalAmount = typed.sumOf { stats[it.id]?.totalAmount ?: 0.0 }
    val totalBudget = typed.sumOf { it.budgetLimit }

    val sorted = when (sort) {
        CategorySort.CUSTOM -> typed.sortedBy { orderIndex[it.id] ?: Int.MAX_VALUE }
        CategorySort.NAME -> typed.sortedBy { it.displayName.lowercase() }
        CategorySort.SPENT -> typed.sortedByDescending { stats[it.id]?.totalAmount ?: 0.0 }
        CategorySort.BUDGET -> typed.sortedByDescending { it.budgetLimit }
    }
    val visible = if (searchQuery.isBlank()) sorted
    else sorted.filter { cat ->
        cat.displayName.contains(searchQuery, ignoreCase = true) ||
            subcategories.any { it.parentCategoryId == cat.id && it.name.contains(searchQuery, ignoreCase = true) }
    }
    val canReorder = sort == CategorySort.CUSTOM && searchQuery.isBlank() && visible.size > 1

    // Moves a category from [from] to [to] in the visible list by re-slotting it in the master order
    val onMove: (Int, Int) -> Unit = { from, to ->
        val fromId = visible.getOrNull(from)?.id
        val toId = visible.getOrNull(to)?.id
        if (fromId != null && toId != null && fromId != toId && customOrder.remove(fromId)) {
            val anchor = customOrder.indexOf(toId)
            customOrder.add(if (from < to) anchor + 1 else anchor, fromId)
        }
    }

    // ── Drag-to-reorder state. The handle drives the offset; the dragged card floats with it and
    // swaps with a neighbour once its centre crosses that neighbour's centre. ──────────────────
    var draggingId by remember { mutableStateOf<Int?>(null) }
    var dragOffset by remember { mutableFloatStateOf(0f) }
    val visibleNow by rememberUpdatedState(visible)
    val onMoveNow by rememberUpdatedState(onMove)

    fun dragHandleModifier(category: Category): Modifier = Modifier.pointerInput(category.id, canReorder) {
        if (!canReorder) return@pointerInput
        val edgePx = 72.dp.toPx()
        detectDragGestures(
            onDragStart = {
                draggingId = category.id
                dragOffset = 0f
                haptic.performHapticFeedback(HapticFeedbackType.LongPress)
            },
            onDragEnd = { draggingId = null; dragOffset = 0f },
            onDragCancel = { draggingId = null; dragOffset = 0f },
            onDrag = { change, amount ->
                change.consume()
                dragOffset += amount.y
                val layout = listState.layoutInfo
                val infos = layout.visibleItemsInfo
                val dragged = infos.firstOrNull { it.key == categoryKey(category.id) } ?: return@detectDragGestures
                val items = visibleNow
                val index = items.indexOfFirst { it.id == category.id }
                if (index < 0) return@detectDragGestures
                val center = dragged.offset + dragged.size / 2f + dragOffset

                val next = items.getOrNull(index + 1)?.let { n -> infos.firstOrNull { it.key == categoryKey(n.id) } }
                val prev = items.getOrNull(index - 1)?.let { p -> infos.firstOrNull { it.key == categoryKey(p.id) } }
                if (next != null && center > next.offset + next.size / 2f) {
                    onMoveNow(index, index + 1)
                    // The card's slot moves to where the neighbour's bottom edge was
                    dragOffset -= (next.offset + next.size - dragged.size) - dragged.offset
                    haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                } else if (prev != null && center < prev.offset + prev.size / 2f) {
                    onMoveNow(index, index - 1)
                    dragOffset -= prev.offset - dragged.offset
                    haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                }

                // Nudge the list when the card is pushed against either edge of the viewport
                val top = dragged.offset + dragOffset
                val bottom = top + dragged.size
                val scrollBy = when {
                    bottom > layout.viewportEndOffset - edgePx -> (bottom - (layout.viewportEndOffset - edgePx)).coerceAtMost(28f)
                    top < layout.viewportStartOffset + edgePx -> (top - (layout.viewportStartOffset + edgePx)).coerceAtLeast(-28f)
                    else -> 0f
                }
                if (scrollBy != 0f) {
                    scope.launch {
                        val consumed = listState.scrollBy(scrollBy)
                        if (draggingId == category.id) dragOffset += consumed
                    }
                }
            }
        )
    }

    LazyColumn(
        state = listState,
        modifier = modifier
            .fillMaxSize()
            .testTag("manage_categories_screen"),
        contentPadding = PaddingValues(start = 16.dp, end = 16.dp, top = 4.dp, bottom = 96.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        item(key = "header") {
            ManageHeader(
                subtitle = if (isIncome) "Track all your income sources" else "Customize your expenses, your way",
                onBack = onBack,
                menu = {
                    Box {
                        RoundGlassButton(icon = Icons.Default.MoreHoriz, description = "More options", tint = accent) {
                            showMoreMenu = true
                        }
                        DropdownMenu(expanded = showMoreMenu, onDismissRequest = { showMoreMenu = false }) {
                            DropdownMenuItem(
                                text = { Text(if (isIncome) "New income category" else "New expense category") },
                                leadingIcon = { Icon(Icons.Default.Add, contentDescription = null) },
                                onClick = { showMoreMenu = false; onAddCategory() }
                            )
                            DropdownMenuItem(
                                text = { Text("Reset custom order") },
                                leadingIcon = { Icon(Icons.Default.Restore, contentDescription = null) },
                                enabled = !orderIsDefault,
                                onClick = {
                                    showMoreMenu = false
                                    customOrder.clear()
                                    customOrder.addAll(categories.map { it.id })
                                    sort = CategorySort.CUSTOM
                                }
                            )
                            if (tipDismissed) {
                                DropdownMenuItem(
                                    text = { Text("Show tips") },
                                    leadingIcon = { Icon(Icons.Default.Lightbulb, contentDescription = null) },
                                    onClick = { showMoreMenu = false; tipDismissed = false }
                                )
                            }
                        }
                    }
                }
            )
        }

        item(key = "tabs") {
            CategoryTypeTabs(
                selected = selectedType,
                expenseCount = expenseCount,
                incomeCount = incomeCount,
                onSelect = onSelectType
            )
        }

        item(key = "summary") {
            CategorySummaryCard(
                isIncome = isIncome,
                accent = accent,
                amount = totalAmount,
                target = totalBudget,
                currencySymbol = currencySymbol
            )
        }

        if (!tipDismissed) {
            item(key = "tip") {
                TipBanner(
                    icon = if (isIncome) Icons.Default.BarChart else Icons.Default.Lightbulb,
                    accent = accent,
                    text = if (isIncome)
                        "Add or edit your income sources. Keeping these updated helps you track your financial growth better."
                    else
                        "Customize colors, change budgets, or modify subcategories. Values are color-mapped instantly across transaction lists and charts.",
                    onDismiss = { tipDismissed = true }
                )
            }
        }

        item(key = "search") {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(10.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                CategorySearchField(
                    value = searchQuery,
                    onValueChange = { searchQuery = it },
                    accent = accent,
                    modifier = Modifier.weight(1f)
                )
                Box {
                    Box(
                        modifier = Modifier
                            .size(52.dp)
                            .liquidGlass(shape = FieldShape, strength = 0.8f)
                            .clickable { showSortMenu = true }
                            .testTag("categories_sort_btn"),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = Icons.Default.Tune,
                            contentDescription = "Sort categories",
                            tint = if (sort == CategorySort.CUSTOM) MaterialTheme.colorScheme.onSurface else accent,
                            modifier = Modifier.size(22.dp)
                        )
                    }
                    DropdownMenu(expanded = showSortMenu, onDismissRequest = { showSortMenu = false }) {
                        CategorySort.entries.forEach { option ->
                            DropdownMenuItem(
                                text = { Text(option.label) },
                                trailingIcon = {
                                    if (option == sort) Icon(Icons.Default.Check, contentDescription = null, tint = accent)
                                },
                                onClick = { sort = option; showSortMenu = false }
                            )
                        }
                    }
                }
            }
        }

        item(key = "list_header") {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = 4.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = if (searchQuery.isNotBlank()) "Search Results (${visible.size})" else "Active Categories (${visible.size})",
                    fontSize = 18.sp,
                    fontWeight = FontWeight.ExtraBold,
                    color = MaterialTheme.colorScheme.onSurface,
                    modifier = Modifier.weight(1f)
                )
                val hint = when {
                    searchQuery.isNotBlank() -> null
                    sort != CategorySort.CUSTOM -> "Sorted by ${sort.label.lowercase()}"
                    visible.size > 1 -> "Drag to reorder"
                    else -> null
                }
                if (hint != null) {
                    Icon(
                        imageVector = Icons.Default.Tune,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.outline,
                        modifier = Modifier.size(15.dp)
                    )
                    Spacer(Modifier.width(5.dp))
                    Text(hint, fontSize = 12.sp, color = MaterialTheme.colorScheme.outline)
                }
            }
        }

        if (visible.isEmpty()) {
            item(key = "empty") {
                EmptyCategories(
                    accent = accent,
                    searching = searchQuery.isNotBlank(),
                    isIncome = isIncome
                )
            }
        } else {
            itemsIndexed(visible, key = { _, cat -> categoryKey(cat.id) }) { _, category ->
                val isDragging = draggingId == category.id
                ManageCategoryCard(
                    category = category,
                    stats = stats[category.id],
                    subcategories = subcategories.filter { it.parentCategoryId == category.id },
                    currencySymbol = currencySymbol,
                    shareOfTotal = if (totalAmount > 0.0) ((stats[category.id]?.totalAmount ?: 0.0) / totalAmount).toFloat() else 0f,
                    isDragging = isDragging,
                    canReorder = canReorder,
                    dragHandle = dragHandleModifier(category),
                    onEdit = { onEditCategory(category) },
                    onDelete = { onDeleteCategory(category) },
                    modifier = Modifier.then(
                        if (isDragging) Modifier
                            .zIndex(1f)
                            .graphicsLayer { translationY = dragOffset }
                        else Modifier.animateItem()
                    )
                )
            }
        }
    }
}

// ─── Header ──────────────────────────────────────────────────────────────────────────────────

@Composable
private fun ManageHeader(
    subtitle: String,
    onBack: () -> Unit,
    menu: @Composable () -> Unit
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
                .testTag("categories_back_btn"),
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
            Text(
                text = "Manage Categories",
                fontSize = 22.sp,
                lineHeight = 26.sp,
                fontWeight = FontWeight.Black,
                letterSpacing = (-0.3).sp,
                color = MaterialTheme.colorScheme.onBackground
            )
            Text(
                text = subtitle,
                fontSize = 13.sp,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
        }
        Spacer(Modifier.width(8.dp))
        menu()
    }
}

@Composable
private fun RoundGlassButton(
    icon: ImageVector,
    description: String,
    tint: Color,
    onClick: () -> Unit
) {
    Box(
        modifier = Modifier
            .size(44.dp)
            .liquidGlass(shape = CircleShape, strength = 0.9f, elevation = 4.dp)
            .clickable(onClick = onClick),
        contentAlignment = Alignment.Center
    ) {
        Icon(icon, contentDescription = description, tint = tint, modifier = Modifier.size(22.dp))
    }
}

// ─── Expense / Income tabs ───────────────────────────────────────────────────────────────────

@Composable
private fun CategoryTypeTabs(
    selected: String,
    expenseCount: Int,
    incomeCount: Int,
    onSelect: (String) -> Unit
) {
    val trackShape = RoundedCornerShape(18.dp)
    val segmentShape = RoundedCornerShape(14.dp)
    val tabs = listOf(
        Triple("EXPENSE", "Expense Categories ($expenseCount)", Icons.Default.AccountBalanceWallet),
        Triple("INCOME", "Income Categories ($incomeCount)", Icons.AutoMirrored.Filled.TrendingUp)
    )
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .height(58.dp)
            .liquidGlass(shape = trackShape, strength = 0.75f)
            .padding(5.dp),
        horizontalArrangement = Arrangement.spacedBy(5.dp)
    ) {
        tabs.forEach { (type, label, icon) ->
            val isSelected = selected == type
            val accent = if (type == "INCOME") IncomeGreen else MaterialTheme.colorScheme.primary
            Row(
                modifier = Modifier
                    .weight(1f)
                    .fillMaxHeight()
                    .clip(segmentShape)
                    .background(if (isSelected) accent.copy(alpha = 0.16f) else Color.Transparent)
                    .border(
                        width = if (isSelected) 1.5.dp else 0.dp,
                        color = if (isSelected) accent.copy(alpha = 0.9f) else Color.Transparent,
                        shape = segmentShape
                    )
                    .clickable { onSelect(type) }
                    .padding(horizontal = 6.dp)
                    .testTag("categories_tab_${type.lowercase()}"),
                horizontalArrangement = Arrangement.Center,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Icon(
                    imageVector = icon,
                    contentDescription = null,
                    tint = if (isSelected) accent else MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.size(18.dp)
                )
                Spacer(Modifier.width(7.dp))
                Text(
                    text = label,
                    fontSize = 12.sp,
                    fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium,
                    color = if (isSelected) accent else MaterialTheme.colorScheme.onSurfaceVariant,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }
        }
    }
}

// ─── Summary card: ring + bar ────────────────────────────────────────────────────────────────

@Composable
private fun CategorySummaryCard(
    isIncome: Boolean,
    accent: Color,
    amount: Double,
    target: Double,
    currencySymbol: String
) {
    val hasTarget = target > 0.0
    val ratio = if (hasTarget) (amount / target).toFloat() else 0f
    val over = hasTarget && amount > target
    val percent = (ratio * 100).roundToInt()

    GlassCard(
        modifier = Modifier
            .fillMaxWidth()
            .testTag("categories_summary_card"),
        shape = CardShape,
        tint = accent,
        elevation = 8.dp,
        contentPadding = PaddingValues(horizontal = 18.dp, vertical = 18.dp)
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            ProgressRing(
                progress = if (hasTarget) ratio else (if (amount > 0.0) 1f else 0f),
                accent = if (over && !isIncome) ExpenseRed else accent,
                size = 112.dp
            ) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Text(
                        text = formatInRupee(amount, currencySymbol),
                        fontSize = if (amount >= 1_000_000) 12.sp else 14.sp,
                        fontWeight = FontWeight.ExtraBold,
                        color = MaterialTheme.colorScheme.onSurface,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                        modifier = Modifier.padding(horizontal = 12.dp)
                    )
                    Text(
                        text = if (isIncome) "Total Income" else "Spent",
                        fontSize = 11.sp,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
            Spacer(Modifier.width(18.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = if (isIncome) "Monthly Income Target" else "Monthly Expense Budget",
                    fontSize = 13.sp,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Text(
                    text = if (hasTarget) formatInRupee(target, currencySymbol) else "Not set",
                    fontSize = 26.sp,
                    lineHeight = 30.sp,
                    fontWeight = FontWeight.Black,
                    letterSpacing = (-0.5).sp,
                    color = MaterialTheme.colorScheme.onSurface,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                Spacer(Modifier.height(10.dp))
                Row(verticalAlignment = Alignment.CenterVertically) {
                    GradientBar(
                        progress = ratio,
                        color = if (over && !isIncome) ExpenseRed else accent,
                        modifier = Modifier
                            .weight(1f)
                            .height(8.dp)
                    )
                    Spacer(Modifier.width(10.dp))
                    Text(
                        text = when {
                            !hasTarget -> "—"
                            isIncome -> "$percent% of target"
                            else -> "$percent% used"
                        },
                        fontSize = 11.sp,
                        fontWeight = FontWeight.SemiBold,
                        color = if (over && !isIncome) ExpenseRed else MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
                Spacer(Modifier.height(6.dp))
                Text(
                    text = when {
                        !hasTarget -> if (isIncome) "Set targets on your income sources" else "Set limits on categories to track this"
                        isIncome && amount >= target -> "Target reached"
                        isIncome -> "${formatInRupee(target - amount, currencySymbol)} to go"
                        over -> "${formatInRupee(amount - target, currencySymbol)} over"
                        else -> "${formatInRupee(target - amount, currencySymbol)} left"
                    },
                    fontSize = 12.sp,
                    fontWeight = if (over && !isIncome) FontWeight.Bold else FontWeight.Medium,
                    color = if (over && !isIncome) ExpenseRed else MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.fillMaxWidth(),
                    textAlign = TextAlign.End
                )
            }
        }
    }
}

/** Circular progress track with a rounded sweep in [accent]; [content] is centred inside it. */
@Composable
private fun ProgressRing(
    progress: Float,
    accent: Color,
    size: Dp,
    stroke: Dp = 11.dp,
    content: @Composable () -> Unit
) {
    val animated by animateFloatAsState(progress.coerceIn(0f, 1f), tween(700), label = "ring")
    val track = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.08f)
    val head = lerp(accent, Color.White, 0.30f)
    Box(modifier = Modifier.size(size), contentAlignment = Alignment.Center) {
        Canvas(modifier = Modifier.fillMaxSize()) {
            val sw = stroke.toPx()
            val arcSize = Size(this.size.width - sw, this.size.height - sw)
            val topLeft = Offset(sw / 2f, sw / 2f)
            drawArc(
                color = track,
                startAngle = 0f,
                sweepAngle = 360f,
                useCenter = false,
                topLeft = topLeft,
                size = arcSize,
                style = Stroke(width = sw)
            )
            if (animated > 0f) {
                // Rotate so the sweep (and its gradient) starts at 12 o'clock
                rotate(-90f) {
                    drawArc(
                        brush = Brush.sweepGradient(
                            0f to head,
                            animated.coerceAtLeast(0.01f) to accent,
                            1f to accent
                        ),
                        startAngle = 0f,
                        sweepAngle = 360f * animated,
                        useCenter = false,
                        topLeft = topLeft,
                        size = arcSize,
                        style = Stroke(width = sw, cap = StrokeCap.Round)
                    )
                }
            }
        }
        content()
    }
}

/** Rounded track with a horizontal gradient fill; [progress] beyond 1 is clamped. */
@Composable
private fun GradientBar(progress: Float, color: Color, modifier: Modifier = Modifier) {
    val animated by animateFloatAsState(progress.coerceIn(0f, 1f), tween(600), label = "bar")
    Box(
        modifier = modifier
            .clip(RoundedCornerShape(50))
            .background(MaterialTheme.colorScheme.onSurface.copy(alpha = 0.08f))
    ) {
        if (animated > 0f) {
            Box(
                modifier = Modifier
                    .fillMaxHeight()
                    .fillMaxWidth(animated)
                    .clip(RoundedCornerShape(50))
                    .background(Brush.horizontalGradient(listOf(color, lerp(color, Color.White, 0.28f))))
            )
        }
    }
}

// ─── Tip banner ──────────────────────────────────────────────────────────────────────────────

@Composable
private fun TipBanner(
    icon: ImageVector,
    accent: Color,
    text: String,
    onDismiss: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .liquidGlass(shape = RoundedCornerShape(18.dp), tint = accent, strength = 0.8f)
            .padding(start = 14.dp, end = 10.dp, top = 12.dp, bottom = 12.dp)
            .testTag("categories_tip_banner"),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Box(
            modifier = Modifier
                .size(38.dp)
                .clip(RoundedCornerShape(12.dp))
                .background(accent.copy(alpha = 0.16f)),
            contentAlignment = Alignment.Center
        ) {
            Icon(icon, contentDescription = null, tint = accent, modifier = Modifier.size(20.dp))
        }
        Spacer(Modifier.width(12.dp))
        Text(
            text = text,
            fontSize = 12.5.sp,
            lineHeight = 17.sp,
            color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.85f),
            modifier = Modifier.weight(1f)
        )
        Spacer(Modifier.width(6.dp))
        Box(
            modifier = Modifier
                .size(32.dp)
                .clip(CircleShape)
                .clickable(onClick = onDismiss)
                .testTag("categories_tip_dismiss"),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                imageVector = Icons.Default.Close,
                contentDescription = "Dismiss tip",
                tint = MaterialTheme.colorScheme.outline,
                modifier = Modifier.size(18.dp)
            )
        }
    }
}

// ─── Search ──────────────────────────────────────────────────────────────────────────────────

@Composable
private fun CategorySearchField(
    value: String,
    onValueChange: (String) -> Unit,
    accent: Color,
    modifier: Modifier = Modifier
) {
    var focused by remember { mutableStateOf(false) }
    val rim = if (focused) accent.copy(alpha = 0.7f) else Color.Transparent
    Row(
        modifier = modifier
            .height(52.dp)
            .liquidGlass(shape = FieldShape, strength = 0.8f)
            .border(1.dp, rim, FieldShape)
            .padding(horizontal = 14.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Icon(
            imageVector = Icons.Default.Search,
            contentDescription = null,
            tint = MaterialTheme.colorScheme.onSurface,
            modifier = Modifier.size(22.dp)
        )
        Spacer(Modifier.width(12.dp))
        BasicTextField(
            value = value,
            onValueChange = onValueChange,
            singleLine = true,
            textStyle = MaterialTheme.typography.bodyMedium.copy(
                color = MaterialTheme.colorScheme.onSurface,
                fontSize = 14.sp
            ),
            cursorBrush = SolidColor(accent),
            modifier = Modifier
                .weight(1f)
                .onFocusChanged { focused = it.isFocused }
                .testTag("categories_search_input"),
            decorationBox = { inner ->
                Box(contentAlignment = Alignment.CenterStart) {
                    if (value.isEmpty()) {
                        Text(
                            text = "Search categories...",
                            fontSize = 14.sp,
                            color = MaterialTheme.colorScheme.outline,
                            maxLines = 1
                        )
                    }
                    inner()
                }
            }
        )
        if (value.isNotEmpty()) {
            Spacer(Modifier.width(6.dp))
            Box(
                modifier = Modifier
                    .size(22.dp)
                    .clip(CircleShape)
                    .background(MaterialTheme.colorScheme.onSurface.copy(alpha = 0.10f))
                    .clickable { onValueChange("") },
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = Icons.Default.Close,
                    contentDescription = "Clear search",
                    tint = MaterialTheme.colorScheme.onSurface,
                    modifier = Modifier.size(12.dp)
                )
            }
        }
    }
}

// ─── Category card ───────────────────────────────────────────────────────────────────────────

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun ManageCategoryCard(
    category: Category,
    stats: CategoryStats?,
    subcategories: List<Subcategory>,
    currencySymbol: String,
    shareOfTotal: Float,
    isDragging: Boolean,
    canReorder: Boolean,
    dragHandle: Modifier,
    onEdit: () -> Unit,
    onDelete: () -> Unit,
    modifier: Modifier = Modifier
) {
    val budget = category.budgetLimit
    val spent = stats?.totalAmount ?: 0.0
    val hasBudget = budget > 0.0
    val isOver = hasBudget && spent > budget
    val ratio = if (hasBudget) (spent / budget).toFloat() else shareOfTotal
    val catColor = parseHexColor(category.colorHex)
    val isSystem = category.name == SYSTEM_LENDING || category.name == SYSTEM_LOAN
    val subLabel = when (subcategories.size) {
        0 -> "0 subcategories"
        1 -> "1 subcategory"
        else -> "${subcategories.size} subcategories"
    }

    Column(
        modifier = modifier
            .fillMaxWidth()
            .liquidGlass(
                shape = CardShape,
                tint = catColor,
                strength = if (isDragging) 1f else 0.95f,
                elevation = if (isDragging) 12.dp else 4.dp
            )
            .padding(14.dp)
            .testTag("category_card_${category.id}")
    ) {
        Row(verticalAlignment = Alignment.Top) {
            // Colour badge
            Box(
                modifier = Modifier
                    .size(58.dp)
                    .clip(RoundedCornerShape(16.dp))
                    .background(Brush.linearGradient(listOf(lerp(catColor, Color.White, 0.10f), catColor, lerp(catColor, Color.Black, 0.12f)))),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = getIconVector(category.iconName),
                    contentDescription = null,
                    tint = getContrastColorFor(catColor),
                    modifier = Modifier.size(28.dp)
                )
            }
            Spacer(Modifier.width(14.dp))
            Column(modifier = Modifier.weight(1f)) {
                Row(verticalAlignment = Alignment.Top) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = category.displayName,
                            fontSize = 17.sp,
                            lineHeight = 21.sp,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onSurface,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                        Text(
                            text = subLabel,
                            fontSize = 13.sp,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                    // Actions
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        CardAction(
                            icon = Icons.Default.Edit,
                            description = "Edit ${category.displayName}",
                            tint = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.testTag("edit_category_btn_${category.id}"),
                            onClick = onEdit
                        )
                        if (!isSystem) {
                            CardAction(
                                icon = Icons.Default.Delete,
                                description = "Delete ${category.displayName}",
                                tint = ExpenseRed,
                                modifier = Modifier.testTag("delete_category_btn_${category.id}"),
                                onClick = onDelete
                            )
                        }
                        Box(
                            modifier = Modifier
                                .size(34.dp)
                                .alpha(if (canReorder) 1f else 0.3f)
                                .then(dragHandle)
                                .testTag("drag_category_handle_${category.id}"),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = Icons.Default.DragIndicator,
                                contentDescription = "Reorder",
                                tint = if (isDragging) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.outline,
                                modifier = Modifier.size(22.dp)
                            )
                        }
                    }
                }
                Spacer(Modifier.height(4.dp))
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(
                        text = formatInRupee(spent, currencySymbol),
                        fontSize = 14.sp,
                        fontWeight = FontWeight.Bold,
                        color = if (isOver) ExpenseRed else MaterialTheme.colorScheme.onSurface
                    )
                    Text(
                        text = "  /  " + if (hasBudget) formatInRupee(budget, currencySymbol) else "No Limit",
                        fontSize = 14.sp,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
                Spacer(Modifier.height(8.dp))
                GradientBar(
                    progress = ratio,
                    color = if (isOver) ExpenseRed else catColor,
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(6.dp)
                )
                if (isOver) {
                    Spacer(Modifier.height(4.dp))
                    Text(
                        text = "${formatInRupee(spent - budget, currencySymbol)} over limit",
                        fontSize = 11.sp,
                        fontWeight = FontWeight.SemiBold,
                        color = ExpenseRed
                    )
                }
            }
        }

        if (subcategories.isNotEmpty()) {
            Spacer(Modifier.height(12.dp))
            FlowRow(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                subcategories.forEach { sub ->
                    SubcategoryChip(name = sub.name)
                }
            }
        }
    }
}

@Composable
private fun CardAction(
    icon: ImageVector,
    description: String,
    tint: Color,
    modifier: Modifier = Modifier,
    onClick: () -> Unit
) {
    Box(
        modifier = modifier
            .size(34.dp)
            .clip(CircleShape)
            .clickable(onClick = onClick),
        contentAlignment = Alignment.Center
    ) {
        Icon(icon, contentDescription = description, tint = tint, modifier = Modifier.size(19.dp))
    }
}

@Composable
private fun SubcategoryChip(name: String) {
    val shape = RoundedCornerShape(11.dp)
    Text(
        text = name,
        fontSize = 12.sp,
        fontWeight = FontWeight.Medium,
        color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.9f),
        maxLines = 1,
        overflow = TextOverflow.Ellipsis,
        modifier = Modifier
            .clip(shape)
            .background(MaterialTheme.colorScheme.onSurface.copy(alpha = 0.05f))
            .border(1.dp, MaterialTheme.colorScheme.onSurface.copy(alpha = 0.16f), shape)
            .padding(horizontal = 12.dp, vertical = 7.dp)
    )
}

// ─── Empty state ─────────────────────────────────────────────────────────────────────────────

@Composable
private fun EmptyCategories(accent: Color, searching: Boolean, isIncome: Boolean) {
    GlassCard(
        modifier = Modifier.fillMaxWidth(),
        shape = CardShape,
        strength = 0.8f,
        contentPadding = PaddingValues(horizontal = 20.dp, vertical = 28.dp)
    ) {
        Column(
            modifier = Modifier.fillMaxWidth(),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Box(
                modifier = Modifier
                    .size(52.dp)
                    .clip(RoundedCornerShape(16.dp))
                    .background(accent.copy(alpha = 0.14f)),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = if (searching) Icons.Default.Search else Icons.Default.Category,
                    contentDescription = null,
                    tint = accent,
                    modifier = Modifier.size(26.dp)
                )
            }
            Spacer(Modifier.height(12.dp))
            Text(
                text = if (searching) "No matching categories" else "No ${if (isIncome) "income" else "expense"} categories yet",
                fontSize = 15.sp,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onSurface
            )
            Spacer(Modifier.height(4.dp))
            Text(
                text = if (searching) "Try a different name or subcategory." else "Tap + to create your first one.",
                fontSize = 13.sp,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}
