package com.example.ui.screens

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawing
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowRight
import androidx.compose.material.icons.automirrored.filled.TrendingUp
import androidx.compose.material.icons.filled.AccountBalanceWallet
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.DragHandle
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.Palette
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Text
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.key
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.zIndex
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.CATEGORY_ICON_MAP
import com.example.CategoryIcon
import com.example.FEATURED_ICON_NAMES
import com.example.canonicalIconName
import com.example.data.model.Category
import com.example.data.model.Subcategory
import com.example.findCategoryIcon
import com.example.getContrastColor
import com.example.getContrastColorFor
import com.example.getIconVector
import com.example.guessSubcategoryIconName
import com.example.parseHexColor
import com.example.searchCategoryIcons
import com.example.ui.theme.AppBackground
import com.example.ui.theme.ExpenseRed
import com.example.ui.theme.GlassCard
import com.example.ui.theme.IncomeGreen
import com.example.ui.theme.accentGlow
import com.example.ui.theme.liquidGlass
import com.example.ui.theme.selectedPillBrush
import com.example.ui.theme.selectedPillRim
import com.example.ui.viewmodel.CategoryViewModel
import com.example.ui.viewmodel.PaletteTheme
import kotlin.math.roundToInt

// Working copy of a subcategory while the editor is open. `uid` keeps row identity stable across
// reorders; `id` is the database row (0 for rows added in this session).
private data class SubcategoryDraft(
    val uid: Long,
    val id: Int,
    val name: String,
    val colorHexOverride: String?,
    val iconName: String?
) {
    fun toEntity(parentId: Int) = Subcategory(
        id = id,
        parentCategoryId = parentId,
        name = name,
        colorHexOverride = colorHexOverride,
        iconName = iconName
    )
}

private val FieldShape = RoundedCornerShape(16.dp)
private val TileShape = RoundedCornerShape(14.dp)

/**
 * Full-screen create / update category form.
 *
 * Rendered as an in-tree overlay rather than a [androidx.compose.ui.window.Dialog]: a dialog
 * window is confined by WindowManager to the area between the status bar and the nav bar, yet
 * Compose measures a `usePlatformDefaultWidth = false` dialog against `screenHeightDp`, which on
 * Android 15 includes those bars. The content ends up taller than its window and the bottom gets
 * clipped. Drawing inside the activity window sidesteps that and gets the real insets.
 * The callers emit this after their Scaffold, so it composes on top of it.
 */
@OptIn(ExperimentalMaterial3Api::class, ExperimentalLayoutApi::class)
@Composable
fun CategoryEditorDialog(
    viewModel: CategoryViewModel,
    category: Category? = null,
    initialType: String = "EXPENSE",
    subcategories: List<Subcategory> = emptyList(),
    onDismiss: () -> Unit,
    onDelete: (() -> Unit)? = null,
    onSave: (name: String, colorHex: String, iconName: String, budget: Double, subcategories: List<Subcategory>, type: String) -> Unit
) {
    val haptic = LocalHapticFeedback.current
    val isEditing = category != null

    var name by remember { mutableStateOf(category?.name ?: "") }
    var colorHex by remember { mutableStateOf(category?.colorHex ?: "") }
    var iconName by remember { mutableStateOf(canonicalIconName(category?.iconName) ?: "restaurant") }
    var budgetStr by remember {
        mutableStateOf(category?.budgetLimit?.takeIf { it > 0.0 }?.toInt()?.toString() ?: "")
    }
    var categoryType by remember { mutableStateOf(category?.type ?: initialType) }
    var showCustomHex by remember { mutableStateOf(false) }

    var showIconPicker by remember { mutableStateOf(false) }
    var showPaletteMenu by remember { mutableStateOf(false) }
    // null = closed, -1 = adding, otherwise the index being edited
    var subEditorIndex by remember { mutableStateOf<Int?>(null) }

    var nextUid by remember { mutableStateOf(1L) }
    val subList = remember {
        mutableStateListOf<SubcategoryDraft>().apply {
            subcategories.forEachIndexed { i, s ->
                add(SubcategoryDraft(uid = i.toLong(), id = s.id, name = s.name, colorHexOverride = s.colorHexOverride, iconName = s.iconName))
            }
            nextUid = subcategories.size.toLong() + 1
        }
    }

    val allCategories by viewModel.categories.collectAsStateWithLifecycle()
    val paletteTheme by viewModel.currentPaletteTheme.collectAsStateWithLifecycle()
    val currency by viewModel.currency.collectAsStateWithLifecycle()
    val paletteColors = remember(paletteTheme) { viewModel.palettes[paletteTheme] ?: emptyList() }
    val usedColors = remember(allCategories, category) {
        allCategories.filter { it.id != category?.id }.map { it.colorHex.uppercase() }.toSet()
    }

    var isDuplicateError by remember { mutableStateOf(false) }
    var similarityWarning by remember { mutableStateOf<String?>(null) }

    // New categories start on the first free palette colour
    LaunchedEffect(paletteColors) {
        if (!isEditing && colorHex.isEmpty()) {
            colorHex = paletteColors.firstOrNull { it.uppercase() !in usedColors } ?: paletteColors.firstOrNull() ?: "#4D96FF"
        }
    }

    LaunchedEffect(colorHex) {
        if (colorHex.isNotEmpty()) {
            isDuplicateError = viewModel.isColorAlreadyAssigned(colorHex, category?.id)
            val similarity = viewModel.isColorTooSimilar(colorHex, category?.id)
            similarityWarning = if (similarity.first) similarity.second else null
        } else {
            isDuplicateError = false
            similarityWarning = null
        }
    }

    val accent = MaterialTheme.colorScheme.primary
    val swatch = parseHexColor(colorHex, accent)
    val canSave = name.isNotBlank() && colorHex.isNotBlank() && !isDuplicateError
    val canDelete = category != null && onDelete != null && category.name != "Lending" && category.name != "Loan Repayment"

    BackHandler(onBack = onDismiss)

    // The background is opaque and the no-op clickable swallows any touch that lands outside the
    // scroll column, so nothing reaches the screen underneath.
    Box(
        modifier = Modifier
            .fillMaxSize()
            .zIndex(10f)
            .clickable(interactionSource = remember { MutableInteractionSource() }, indication = null, onClick = {})
    ) {
        AppBackground(modifier = Modifier.fillMaxSize()) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .windowInsetsPadding(WindowInsets.safeDrawing)
                    .imePadding()
                    .verticalScroll(rememberScrollState())
                    .padding(horizontal = 20.dp)
                    .padding(top = 8.dp, bottom = 24.dp)
                    .testTag("category_editor_dialog"),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                // ── Header ───────────────────────────────────────────────────────────────
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Box(
                        modifier = Modifier
                            .size(40.dp)
                            .clip(CircleShape)
                            .clickable(onClick = onDismiss),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = "Back",
                            tint = MaterialTheme.colorScheme.onSurface
                        )
                    }
                    Spacer(Modifier.width(8.dp))
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = if (isEditing) "Update Category" else "Create Category",
                            style = MaterialTheme.typography.titleLarge,
                            fontWeight = FontWeight.ExtraBold,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                        Text(
                            text = "Customize your category details",
                            fontSize = 13.sp,
                            color = MaterialTheme.colorScheme.outline
                        )
                    }
                    if (canDelete) {
                        Row(
                            modifier = Modifier
                                .clip(RoundedCornerShape(12.dp))
                                .background(ExpenseRed.copy(alpha = 0.10f))
                                .border(1.dp, ExpenseRed.copy(alpha = 0.35f), RoundedCornerShape(12.dp))
                                .clickable { onDelete?.invoke() }
                                .padding(horizontal = 12.dp, vertical = 8.dp)
                                .testTag("delete_category_btn"),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(Icons.Default.Delete, contentDescription = null, tint = ExpenseRed, modifier = Modifier.size(16.dp))
                            Spacer(Modifier.width(6.dp))
                            Text("Delete", fontSize = 13.sp, fontWeight = FontWeight.SemiBold, color = ExpenseRed)
                        }
                    }
                }

                Spacer(Modifier.height(4.dp))

                // ── Category type ────────────────────────────────────────────────────────
                SectionLabel("Category Type")
                CategoryTypeToggle(selected = categoryType, onSelect = { categoryType = it })

                Spacer(Modifier.height(4.dp))

                // ── Name ─────────────────────────────────────────────────────────────────
                SectionLabel("Category Name")
                GlassInputField(
                    value = name,
                    onValueChange = { name = it },
                    placeholder = "e.g. Groceries",
                    modifier = Modifier.testTag("cat_name_input"),
                    leading = {
                        IconBadge(vector = getIconVector(iconName), color = swatch, size = 40.dp)
                    },
                    trailing = {
                        if (name.isNotEmpty()) ClearButton { name = "" }
                    }
                )
                HelperText("A short and clear name for your category")

                Spacer(Modifier.height(4.dp))

                // ── Budget ───────────────────────────────────────────────────────────────
                SectionLabel("Budget Limit ($currency)")
                val noLimit = budgetStr.isBlank() || (budgetStr.toDoubleOrNull() ?: 0.0) <= 0.0
                GlassInputField(
                    value = budgetStr,
                    onValueChange = { new -> if (new.all { it.isDigit() || it == '.' }) budgetStr = new },
                    placeholder = "0",
                    keyboardType = KeyboardType.Number,
                    modifier = Modifier.testTag("cat_budget_input"),
                    leading = {
                        Box(
                            modifier = Modifier
                                .size(40.dp)
                                .clip(RoundedCornerShape(12.dp))
                                .background(MaterialTheme.colorScheme.onSurface.copy(alpha = 0.06f)),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(currency, fontSize = 16.sp, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.outline)
                        }
                    },
                    trailing = {
                        Box(
                            modifier = Modifier
                                .clip(RoundedCornerShape(10.dp))
                                .background(
                                    if (noLimit) accent.copy(alpha = 0.16f)
                                    else MaterialTheme.colorScheme.onSurface.copy(alpha = 0.06f)
                                )
                                .clickable { budgetStr = "" }
                                .padding(horizontal = 12.dp, vertical = 8.dp)
                        ) {
                            Text(
                                "No Limit",
                                fontSize = 12.sp,
                                fontWeight = FontWeight.SemiBold,
                                color = if (noLimit) accent else MaterialTheme.colorScheme.outline
                            )
                        }
                    }
                )
                Row(verticalAlignment = Alignment.CenterVertically) {
                    HelperText("Set a monthly budget to track your spending")
                    Spacer(Modifier.width(6.dp))
                    Icon(Icons.Default.Info, contentDescription = null, tint = MaterialTheme.colorScheme.outline, modifier = Modifier.size(14.dp))
                }

                Spacer(Modifier.height(4.dp))

                // ── Icon ─────────────────────────────────────────────────────────────────
                SectionHeaderRow(
                    title = "Category Icon",
                    trailing = { ViewAllLink(onClick = { showIconPicker = true }) }
                )
                IconTileGrid(selected = iconName, accent = accent, onSelect = { iconName = it })

                Spacer(Modifier.height(4.dp))

                // ── Colour ───────────────────────────────────────────────────────────────
                SectionHeaderRow(
                    title = "Category Color",
                    trailing = {
                        Box {
                            Row(
                                modifier = Modifier
                                    .clip(RoundedCornerShape(8.dp))
                                    .clickable { showPaletteMenu = true }
                                    .padding(horizontal = 4.dp, vertical = 2.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Icon(Icons.Default.Palette, contentDescription = null, tint = MaterialTheme.colorScheme.outline, modifier = Modifier.size(14.dp))
                                Spacer(Modifier.width(5.dp))
                                Text(
                                    "Using ${paletteTheme.displayName()} palette",
                                    fontSize = 12.sp,
                                    color = MaterialTheme.colorScheme.outline
                                )
                                Icon(Icons.AutoMirrored.Filled.KeyboardArrowRight, contentDescription = null, tint = MaterialTheme.colorScheme.outline, modifier = Modifier.size(16.dp))
                            }
                            DropdownMenu(expanded = showPaletteMenu, onDismissRequest = { showPaletteMenu = false }) {
                                PaletteTheme.entries.forEach { theme ->
                                    DropdownMenuItem(
                                        text = { Text(theme.displayName() + if (theme == paletteTheme) "  ✓" else "") },
                                        onClick = { viewModel.setPaletteTheme(theme); showPaletteMenu = false }
                                    )
                                }
                            }
                        }
                    }
                )
                FlowRow(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(10.dp),
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    paletteColors.forEach { hex ->
                        val isSelected = colorHex.equals(hex, ignoreCase = true)
                        val taken = hex.uppercase() in usedColors
                        ColorSwatch(
                            color = parseHexColor(hex),
                            selected = isSelected,
                            dimmed = taken && !isSelected,
                            onClick = { colorHex = hex; showCustomHex = false }
                        )
                    }
                    // Custom colour entry
                    val customSelected = showCustomHex || (colorHex.isNotEmpty() && paletteColors.none { it.equals(colorHex, ignoreCase = true) })
                    Box(
                        modifier = Modifier
                            .size(36.dp)
                            .clip(CircleShape)
                            .border(
                                width = if (customSelected) 2.5.dp else 0.dp,
                                color = if (customSelected) MaterialTheme.colorScheme.onSurface else Color.Transparent,
                                shape = CircleShape
                            )
                            .padding(if (customSelected) 4.dp else 0.dp)
                            .clip(CircleShape)
                            .background(
                                Brush.sweepGradient(
                                    listOf(Color(0xFF4D96FF), Color(0xFFA66CFF), Color(0xFFF72585), Color(0xFFFF9F1C), Color(0xFF6BCB77), Color(0xFF4D96FF))
                                )
                            )
                            .clickable { showCustomHex = !showCustomHex }
                    )
                }
                if (showCustomHex) {
                    GlassInputField(
                        value = colorHex,
                        onValueChange = { colorHex = it.trim() },
                        placeholder = "#FF6B6B",
                        modifier = Modifier.testTag("cat_color_input"),
                        leading = {
                            Box(
                                modifier = Modifier
                                    .size(28.dp)
                                    .clip(CircleShape)
                                    .background(swatch)
                                    .border(1.dp, MaterialTheme.colorScheme.outlineVariant, CircleShape)
                            )
                        },
                        height = 48.dp
                    )
                }
                when {
                    isDuplicateError -> InfoNote(
                        text = "This color is already assigned to another category.",
                        icon = Icons.Default.Warning,
                        tint = ExpenseRed,
                        modifier = Modifier.testTag("duplicate_color_warning")
                    )
                    similarityWarning != null -> InfoNote(
                        text = "Close to '$similarityWarning'. A more distinct shade is easier to tell apart.",
                        icon = Icons.Default.Warning,
                        tint = Color(0xFFD48A00),
                        modifier = Modifier.testTag("similarity_color_warning")
                    )
                    else -> InfoNote(
                        text = "This color will be used for this category across the app",
                        icon = Icons.Default.Palette,
                        tint = accent
                    )
                }

                Spacer(Modifier.height(6.dp))

                // ── Subcategories ────────────────────────────────────────────────────────
                GlassCard(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(20.dp),
                    contentPadding = PaddingValues(14.dp)
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = "Subcategories",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.ExtraBold,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                        Spacer(Modifier.width(6.dp))
                        Text(
                            text = "(${subList.size})",
                            style = MaterialTheme.typography.titleMedium,
                            color = MaterialTheme.colorScheme.outline
                        )
                        Spacer(Modifier.weight(1f))
                        Row(
                            modifier = Modifier
                                .clip(RoundedCornerShape(12.dp))
                                .background(selectedPillBrush(accent))
                                .clickable { subEditorIndex = -1 }
                                .padding(horizontal = 14.dp, vertical = 9.dp)
                                .testTag("add_subcategory_btn"),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(Icons.Default.Add, contentDescription = null, tint = Color.White, modifier = Modifier.size(16.dp))
                            Spacer(Modifier.width(4.dp))
                            Text("Add", fontSize = 13.sp, fontWeight = FontWeight.Bold, color = Color.White)
                        }
                    }

                    Spacer(Modifier.height(12.dp))

                    if (subList.isEmpty()) {
                        Text(
                            text = "No subcategories yet. Add one to break this category down further.",
                            fontSize = 12.sp,
                            color = MaterialTheme.colorScheme.outline,
                            modifier = Modifier.padding(vertical = 8.dp)
                        )
                    } else {
                        ReorderableSubcategoryList(
                            items = subList,
                            parentColorHex = colorHex,
                            parentIconName = iconName,
                            onEdit = { subEditorIndex = it },
                            onRemove = { subList.removeAt(it) },
                            onMove = { from, to ->
                                val moved = subList.removeAt(from)
                                subList.add(to, moved)
                            }
                        )
                    }
                }

                Spacer(Modifier.height(8.dp))

                // ── Save ─────────────────────────────────────────────────────────────────
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(56.dp)
                        .alpha(if (canSave) 1f else 0.45f)
                        .then(if (canSave) Modifier.accentGlow(accent, RoundedCornerShape(16.dp), 12.dp) else Modifier)
                        .clip(RoundedCornerShape(16.dp))
                        .background(selectedPillBrush(accent))
                        .border(1.dp, selectedPillRim(accent), RoundedCornerShape(16.dp))
                        .clickable(enabled = canSave) {
                            haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                            val budgetValue = budgetStr.toDoubleOrNull() ?: 0.0
                            onSave(
                                name.trim(),
                                colorHex,
                                iconName,
                                budgetValue,
                                subList.map { it.toEntity(category?.id ?: 0) },
                                categoryType
                            )
                        }
                        .testTag("save_category_submit_btn"),
                    horizontalArrangement = Arrangement.Center,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = if (isEditing) "Save Changes" else "Create Category",
                        fontSize = 16.sp,
                        fontWeight = FontWeight.Bold,
                        color = Color.White
                    )
                    Spacer(Modifier.width(8.dp))
                    Icon(Icons.Default.Check, contentDescription = null, tint = Color.White, modifier = Modifier.size(20.dp))
                }
            }
        }
    }

    if (showIconPicker) {
        IconPickerSheet(
            selected = iconName,
            onSelect = { iconName = it; showIconPicker = false },
            onDismiss = { showIconPicker = false }
        )
    }

    subEditorIndex?.let { index ->
        val editing = subList.getOrNull(index)
        SubcategoryEditorSheet(
            viewModel = viewModel,
            existingName = editing?.name ?: "",
            existingIconName = editing?.iconName,
            existingShade = editing?.colorHexOverride,
            parentColorHex = colorHex,
            parentIconName = iconName,
            takenNames = subList.filterIndexed { i, _ -> i != index }.map { it.name },
            onDismiss = { subEditorIndex = null },
            onSave = { subName, subIcon, shade ->
                if (editing != null) {
                    subList[index] = editing.copy(name = subName, iconName = subIcon, colorHexOverride = shade)
                } else {
                    subList.add(SubcategoryDraft(uid = nextUid++, id = 0, name = subName, colorHexOverride = shade, iconName = subIcon))
                }
                subEditorIndex = null
            }
        )
    }
}

private fun PaletteTheme.displayName(): String = name.lowercase().replaceFirstChar { it.uppercase() }

// ─── Building blocks ──────────────────────────────────────────────────────────────────────────

@Composable
private fun SectionLabel(text: String) {
    Text(
        text = text,
        fontSize = 14.sp,
        fontWeight = FontWeight.SemiBold,
        color = MaterialTheme.colorScheme.onSurface
    )
}

@Composable
private fun SectionHeaderRow(title: String, trailing: @Composable () -> Unit) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically
    ) {
        SectionLabel(title)
        Spacer(Modifier.weight(1f))
        trailing()
    }
}

@Composable
private fun ViewAllLink(onClick: () -> Unit) {
    Row(
        modifier = Modifier
            .clip(RoundedCornerShape(8.dp))
            .clickable(onClick = onClick)
            .padding(horizontal = 4.dp, vertical = 2.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text("View All", fontSize = 13.sp, fontWeight = FontWeight.SemiBold, color = MaterialTheme.colorScheme.primary)
        Icon(Icons.AutoMirrored.Filled.KeyboardArrowRight, contentDescription = null, tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(18.dp))
    }
}

@Composable
private fun HelperText(text: String) {
    Text(text = text, fontSize = 12.sp, color = MaterialTheme.colorScheme.outline)
}

@Composable
private fun ClearButton(onClick: () -> Unit) {
    Box(
        modifier = Modifier
            .size(22.dp)
            .clip(CircleShape)
            .background(MaterialTheme.colorScheme.onSurface.copy(alpha = 0.10f))
            .clickable(onClick = onClick),
        contentAlignment = Alignment.Center
    ) {
        Icon(Icons.Default.Close, contentDescription = "Clear", tint = MaterialTheme.colorScheme.onSurface, modifier = Modifier.size(12.dp))
    }
}

/** Rounded square filled with [color], showing [vector] in a contrasting tint. */
@Composable
private fun IconBadge(vector: ImageVector, color: Color, size: Dp) {
    val onColor = getContrastColorFor(color)
    Box(
        modifier = Modifier
            .size(size)
            .clip(RoundedCornerShape(size * 0.3f))
            .background(color),
        contentAlignment = Alignment.Center
    ) {
        Icon(vector, contentDescription = null, tint = onColor, modifier = Modifier.size(size * 0.5f))
    }
}

/** Glass-styled single-line input with optional leading/trailing slots. */
@Composable
private fun GlassInputField(
    value: String,
    onValueChange: (String) -> Unit,
    placeholder: String,
    modifier: Modifier = Modifier,
    keyboardType: KeyboardType = KeyboardType.Text,
    height: Dp = 60.dp,
    leading: (@Composable () -> Unit)? = null,
    trailing: (@Composable () -> Unit)? = null
) {
    var focused by remember { mutableStateOf(false) }
    val borderColor = if (focused) MaterialTheme.colorScheme.primary.copy(alpha = 0.75f)
    else MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.55f)

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .height(height)
            .liquidGlass(shape = FieldShape, strength = 0.7f, borderWidth = 0.dp)
            .border(1.dp, borderColor, FieldShape)
            .padding(horizontal = 10.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        if (leading != null) {
            leading()
            Spacer(Modifier.width(12.dp))
        }
        BasicTextField(
            value = value,
            onValueChange = onValueChange,
            singleLine = true,
            keyboardOptions = KeyboardOptions(keyboardType = keyboardType),
            textStyle = MaterialTheme.typography.bodyLarge.copy(
                color = MaterialTheme.colorScheme.onSurface,
                fontSize = 16.sp,
                fontWeight = FontWeight.Medium
            ),
            cursorBrush = SolidColor(MaterialTheme.colorScheme.primary),
            modifier = modifier
                .weight(1f)
                .onFocusChanged { focused = it.isFocused },
            decorationBox = { inner ->
                Box(contentAlignment = Alignment.CenterStart) {
                    if (value.isEmpty()) {
                        Text(placeholder, fontSize = 16.sp, color = MaterialTheme.colorScheme.outline)
                    }
                    inner()
                }
            }
        )
        if (trailing != null) {
            Spacer(Modifier.width(8.dp))
            trailing()
        }
    }
}

@Composable
private fun CategoryTypeToggle(selected: String, onSelect: (String) -> Unit) {
    val options = listOf(
        Triple("EXPENSE", "Expense", Icons.Default.AccountBalanceWallet),
        Triple("INCOME", "Income", Icons.AutoMirrored.Filled.TrendingUp)
    )
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .height(60.dp)
            .liquidGlass(shape = FieldShape, strength = 0.7f)
            .padding(4.dp),
        horizontalArrangement = Arrangement.spacedBy(4.dp)
    ) {
        options.forEach { (value, label, icon) ->
            val isSelected = selected == value
            val accent = if (value == "INCOME") IncomeGreen else MaterialTheme.colorScheme.primary
            Row(
                modifier = Modifier
                    .weight(1f)
                    .fillMaxSize()
                    .clip(RoundedCornerShape(12.dp))
                    .then(
                        if (isSelected) Modifier
                            .background(selectedPillBrush(accent))
                            .border(1.dp, selectedPillRim(accent), RoundedCornerShape(12.dp))
                        else Modifier
                    )
                    .clickable { onSelect(value) },
                horizontalArrangement = Arrangement.Center,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Icon(
                    imageVector = icon,
                    contentDescription = null,
                    tint = if (isSelected) Color.White else MaterialTheme.colorScheme.onSurface.copy(alpha = 0.75f),
                    modifier = Modifier.size(20.dp)
                )
                Spacer(Modifier.width(8.dp))
                Text(
                    text = label,
                    fontSize = 15.sp,
                    fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium,
                    color = if (isSelected) Color.White else MaterialTheme.colorScheme.onSurface
                )
            }
        }
    }
}

/**
 * Six-across grid of the featured icons. If the current pick isn't one of them it takes the first
 * slot so the selection is always visible without opening the full picker.
 */
@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun IconTileGrid(
    selected: String,
    accent: Color,
    onSelect: (String) -> Unit,
    modifier: Modifier = Modifier,
    columns: Int = 6
) {
    val names = remember(selected) {
        if (selected in FEATURED_ICON_NAMES) FEATURED_ICON_NAMES
        else listOf(selected) + FEATURED_ICON_NAMES.dropLast(1)
    }
    FlowRow(
        modifier = modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(10.dp),
        verticalArrangement = Arrangement.spacedBy(10.dp),
        maxItemsInEachRow = columns
    ) {
        names.forEach { iconKey ->
            IconTile(
                vector = getIconVector(iconKey),
                selected = iconKey == selected,
                accent = accent,
                onClick = { onSelect(iconKey) },
                modifier = Modifier.weight(1f)
            )
        }
    }
}

@Composable
private fun IconTile(
    vector: ImageVector,
    selected: Boolean,
    accent: Color,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Box(
        modifier = modifier
            .aspectRatio(1f)
            .clip(TileShape)
            .background(
                if (selected) accent.copy(alpha = 0.22f)
                else MaterialTheme.colorScheme.onSurface.copy(alpha = 0.06f)
            )
            .border(
                width = if (selected) 1.5.dp else 1.dp,
                color = if (selected) accent else MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.35f),
                shape = TileShape
            )
            .clickable(onClick = onClick),
        contentAlignment = Alignment.Center
    ) {
        Icon(
            imageVector = vector,
            contentDescription = null,
            tint = if (selected) accent else MaterialTheme.colorScheme.onSurface,
            modifier = Modifier.size(24.dp)
        )
    }
}

@Composable
private fun ColorSwatch(color: Color, selected: Boolean, dimmed: Boolean, onClick: () -> Unit) {
    Box(
        modifier = Modifier
            .size(36.dp)
            .clip(CircleShape)
            .border(
                width = if (selected) 2.5.dp else 0.dp,
                color = if (selected) MaterialTheme.colorScheme.onSurface else Color.Transparent,
                shape = CircleShape
            )
            .padding(if (selected) 4.dp else 0.dp)
            .clip(CircleShape)
            .background(color)
            .alpha(if (dimmed) 0.4f else 1f)
            .clickable(onClick = onClick)
    )
}

@Composable
private fun InfoNote(
    text: String,
    icon: ImageVector,
    tint: Color,
    modifier: Modifier = Modifier
) {
    Row(
        modifier = modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(12.dp))
            .background(tint.copy(alpha = 0.08f))
            .border(1.dp, tint.copy(alpha = 0.25f), RoundedCornerShape(12.dp))
            .padding(horizontal = 12.dp, vertical = 10.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Icon(icon, contentDescription = null, tint = tint, modifier = Modifier.size(16.dp))
        Spacer(Modifier.width(10.dp))
        Text(text, fontSize = 12.sp, color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.85f))
    }
}

// ─── Subcategory list with drag-to-reorder ────────────────────────────────────────────────────

private val SubRowHeight = 56.dp
private val SubRowGap = 8.dp

@Composable
private fun ReorderableSubcategoryList(
    items: List<SubcategoryDraft>,
    parentColorHex: String,
    parentIconName: String,
    onEdit: (Int) -> Unit,
    onRemove: (Int) -> Unit,
    onMove: (from: Int, to: Int) -> Unit
) {
    val haptic = LocalHapticFeedback.current
    val density = LocalDensity.current
    val pitchPx = with(density) { (SubRowHeight + SubRowGap).toPx() }
    var draggingUid by remember { mutableStateOf<Long?>(null) }
    var dragOffset by remember { mutableFloatStateOf(0f) }

    Column(verticalArrangement = Arrangement.spacedBy(SubRowGap)) {
        items.forEachIndexed { index, item ->
            key(item.uid) {
                val isDragging = draggingUid == item.uid
                val shade = item.colorHexOverride ?: parentColorHex
                val vector = findCategoryIcon(item.iconName)
                    ?: findCategoryIcon(guessSubcategoryIconName(item.name))
                    ?: getIconVector(parentIconName)

                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(SubRowHeight)
                        .zIndex(if (isDragging) 1f else 0f)
                        .graphicsLayer { translationY = if (isDragging) dragOffset else 0f }
                        .liquidGlass(shape = TileShape, strength = if (isDragging) 1f else 0.55f, elevation = if (isDragging) 6.dp else 0.dp)
                        .padding(horizontal = 12.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Box(
                        modifier = Modifier
                            .size(28.dp)
                            .clip(CircleShape)
                            .background(parseHexColor(shade)),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(vector, contentDescription = null, tint = getContrastColor(shade), modifier = Modifier.size(15.dp))
                    }
                    Spacer(Modifier.width(12.dp))
                    Text(
                        text = item.name,
                        fontSize = 15.sp,
                        fontWeight = FontWeight.Medium,
                        color = MaterialTheme.colorScheme.onSurface,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                        modifier = Modifier.weight(1f)
                    )
                    RowActionIcon(Icons.Default.Edit, "Edit", MaterialTheme.colorScheme.onSurface.copy(alpha = 0.75f)) { onEdit(index) }
                    RowActionIcon(Icons.Default.Delete, "Remove", ExpenseRed) { onRemove(index) }
                    Icon(
                        imageVector = Icons.Default.DragHandle,
                        contentDescription = "Reorder",
                        tint = MaterialTheme.colorScheme.outline,
                        modifier = Modifier
                            .size(36.dp)
                            .pointerInput(item.uid) {
                                detectDragGestures(
                                    onDragStart = {
                                        draggingUid = item.uid
                                        dragOffset = 0f
                                        haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                                    },
                                    onDragEnd = { draggingUid = null; dragOffset = 0f },
                                    onDragCancel = { draggingUid = null; dragOffset = 0f },
                                    onDrag = { change, amount ->
                                        change.consume()
                                        dragOffset += amount.y
                                        val current = items.indexOfFirst { it.uid == item.uid }
                                        if (current < 0) return@detectDragGestures
                                        val target = (current + (dragOffset / pitchPx).roundToInt())
                                            .coerceIn(0, items.lastIndex)
                                        if (target != current) {
                                            onMove(current, target)
                                            dragOffset -= (target - current) * pitchPx
                                            haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                                        }
                                    }
                                )
                            }
                            .padding(8.dp)
                    )
                }
            }
        }
    }
}

@Composable
private fun RowActionIcon(
    vector: ImageVector,
    description: String,
    tint: Color,
    onClick: () -> Unit
) {
    Box(
        modifier = Modifier
            .size(36.dp)
            .clip(CircleShape)
            .clickable(onClick = onClick),
        contentAlignment = Alignment.Center
    ) {
        Icon(vector, contentDescription = description, tint = tint, modifier = Modifier.size(18.dp))
    }
}

// ─── Icon picker sheet ────────────────────────────────────────────────────────────────────────

@OptIn(ExperimentalMaterial3Api::class, ExperimentalLayoutApi::class)
@Composable
private fun IconPickerSheet(
    selected: String,
    onSelect: (String) -> Unit,
    onDismiss: () -> Unit
) {
    var query by remember { mutableStateOf("") }
    val groups = remember(query) { searchCategoryIcons(query) }
    val accent = MaterialTheme.colorScheme.primary

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true),
        containerColor = MaterialTheme.colorScheme.surface
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 20.dp)
                .padding(bottom = 8.dp)
        ) {
            Text(
                text = "Choose an Icon",
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.ExtraBold,
                color = MaterialTheme.colorScheme.onSurface
            )
            Text(
                text = "${CATEGORY_ICON_MAP.size} icons across ${groups.size} groups",
                fontSize = 12.sp,
                color = MaterialTheme.colorScheme.outline
            )
            Spacer(Modifier.height(12.dp))
            GlassInputField(
                value = query,
                onValueChange = { query = it },
                placeholder = "Search icons…",
                height = 46.dp,
                leading = {
                    Icon(Icons.Default.Search, contentDescription = null, tint = accent, modifier = Modifier.size(18.dp))
                },
                trailing = { if (query.isNotEmpty()) ClearButton { query = "" } }
            )
            Spacer(Modifier.height(12.dp))

            if (groups.isEmpty()) {
                Text(
                    text = "No icons match \"$query\"",
                    fontSize = 13.sp,
                    color = MaterialTheme.colorScheme.outline,
                    modifier = Modifier.padding(vertical = 24.dp)
                )
            }

            LazyColumn(
                modifier = Modifier.fillMaxWidth(),
                verticalArrangement = Arrangement.spacedBy(16.dp),
                contentPadding = PaddingValues(bottom = 32.dp)
            ) {
                items(groups.size) { gi ->
                    val group = groups[gi]
                    Column {
                        Text(
                            text = group.title,
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.outline,
                            modifier = Modifier.padding(bottom = 8.dp)
                        )
                        FlowRow(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(10.dp),
                            verticalArrangement = Arrangement.spacedBy(10.dp),
                            maxItemsInEachRow = 6
                        ) {
                            group.icons.forEach { ic: CategoryIcon ->
                                IconTile(
                                    vector = ic.vector,
                                    selected = ic.name == selected,
                                    accent = accent,
                                    onClick = { onSelect(ic.name) },
                                    modifier = Modifier.weight(1f)
                                )
                            }
                            // Pad the last row so tiles keep their size
                            val remainder = group.icons.size % 6
                            if (remainder != 0) repeat(6 - remainder) { Spacer(Modifier.weight(1f)) }
                        }
                    }
                }
            }
        }
    }
}

// ─── Subcategory editor sheet ─────────────────────────────────────────────────────────────────

@OptIn(ExperimentalMaterial3Api::class, ExperimentalLayoutApi::class)
@Composable
private fun SubcategoryEditorSheet(
    viewModel: CategoryViewModel,
    existingName: String,
    existingIconName: String?,
    existingShade: String?,
    parentColorHex: String,
    parentIconName: String,
    takenNames: List<String>,
    onDismiss: () -> Unit,
    onSave: (name: String, iconName: String?, shade: String?) -> Unit
) {
    val isEditing = existingName.isNotEmpty()
    var name by remember { mutableStateOf(existingName) }
    // null until the user picks: the badge then follows a guess from the name, or the parent
    var pickedIcon by remember { mutableStateOf(existingIconName) }
    var shade by remember { mutableStateOf(existingShade) }
    var showIconPicker by remember { mutableStateOf(false) }

    val shades = remember(parentColorHex) { viewModel.getLighterShades(parentColorHex) }
    LaunchedEffect(shades) {
        if (shade == null && !isEditing && shades.isNotEmpty()) shade = shades.getOrElse(1) { shades.first() }
    }

    val effectiveIcon = pickedIcon ?: guessSubcategoryIconName(name) ?: parentIconName
    val resolvedShade = shade ?: parentColorHex
    val accent = MaterialTheme.colorScheme.primary
    val duplicate = takenNames.any { it.equals(name.trim(), ignoreCase = true) }
    val canSave = name.isNotBlank() && !duplicate

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true),
        containerColor = MaterialTheme.colorScheme.surface
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .verticalScroll(rememberScrollState())
                .imePadding()
                .padding(horizontal = 20.dp)
                .padding(bottom = 28.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            Text(
                text = if (isEditing) "Edit Subcategory" else "Add Subcategory",
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.ExtraBold,
                color = MaterialTheme.colorScheme.onSurface
            )
            Text(
                text = "Give it a name, an icon and a shade of the parent color",
                fontSize = 13.sp,
                color = MaterialTheme.colorScheme.outline
            )

            Spacer(Modifier.height(4.dp))
            SectionLabel("Name")
            GlassInputField(
                value = name,
                onValueChange = { name = it },
                placeholder = "e.g. Fuel",
                modifier = Modifier.testTag("subcat_name_input"),
                leading = { IconBadge(vector = getIconVector(effectiveIcon), color = parseHexColor(resolvedShade, accent), size = 40.dp) },
                trailing = { if (name.isNotEmpty()) ClearButton { name = "" } }
            )
            if (duplicate) {
                Text("A subcategory with this name already exists.", fontSize = 12.sp, color = ExpenseRed)
            }

            Spacer(Modifier.height(4.dp))
            SectionHeaderRow(
                title = "Icon",
                trailing = { ViewAllLink(onClick = { showIconPicker = true }) }
            )
            IconTileGrid(selected = effectiveIcon, accent = accent, onSelect = { pickedIcon = it })
            HelperText(
                if (pickedIcon == null) "Auto-picked from the name — tap any icon to override"
                else "Icon set manually"
            )

            Spacer(Modifier.height(4.dp))
            SectionLabel("Shade")
            FlowRow(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(10.dp),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                shades.forEach { hex ->
                    ColorSwatch(
                        color = parseHexColor(hex),
                        selected = resolvedShade.equals(hex, ignoreCase = true),
                        dimmed = false,
                        onClick = { shade = hex }
                    )
                }
            }
            HelperText("Lighter tints of the category color keep the palette consistent")

            Spacer(Modifier.height(8.dp))
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(52.dp)
                    .alpha(if (canSave) 1f else 0.45f)
                    .clip(RoundedCornerShape(16.dp))
                    .background(selectedPillBrush(accent))
                    .border(1.dp, selectedPillRim(accent), RoundedCornerShape(16.dp))
                    .clickable(enabled = canSave) { onSave(name.trim(), pickedIcon, shade) }
                    .testTag("save_subcategory_btn"),
                horizontalArrangement = Arrangement.Center,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = if (isEditing) "Save Subcategory" else "Add Subcategory",
                    fontSize = 15.sp,
                    fontWeight = FontWeight.Bold,
                    color = Color.White
                )
                Spacer(Modifier.width(8.dp))
                Icon(Icons.Default.Check, contentDescription = null, tint = Color.White, modifier = Modifier.size(18.dp))
            }
        }
    }

    if (showIconPicker) {
        IconPickerSheet(
            selected = effectiveIcon,
            onSelect = { pickedIcon = it; showIconPicker = false },
            onDismiss = { showIconPicker = false }
        )
    }
}
