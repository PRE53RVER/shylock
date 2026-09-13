package com.example

import android.os.Bundle
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.viewModels
import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.foundation.*
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material.icons.outlined.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.animation.Crossfade
import androidx.compose.animation.animateContentSize
import androidx.compose.animation.core.tween
import androidx.compose.animation.core.EaseInOutCubic
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.res.painterResource
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.navigation.compose.rememberNavController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import com.example.data.model.Category
import com.example.data.model.Subcategory
import com.example.data.model.Transaction
import com.example.data.model.displayName
import com.example.ui.screens.ContactDetailScreen
import com.example.ui.screens.LendingListScreen
import com.example.ui.screens.LendingSummaryCard
import com.example.ui.theme.MyApplicationTheme
import com.example.ui.theme.AppBackground
import com.example.ui.theme.AppTheme
import com.example.ui.theme.GlassCard
import com.example.ui.theme.Typography
import com.example.ui.theme.accentGlow
import com.example.ui.theme.liquidGlass
import androidx.compose.ui.graphics.luminance
import com.example.ui.theme.IncomeGreen
import com.example.ui.theme.IncomeGreenContainer
import com.example.ui.theme.ExpenseRed
import com.example.ui.theme.ExpenseRedContainer
import com.example.ui.viewmodel.CategoryStats
import com.example.ui.viewmodel.CategoryViewModel
import com.example.ui.viewmodel.PaletteTheme
import kotlinx.coroutines.launch
import java.text.NumberFormat
import java.text.SimpleDateFormat
import java.time.Instant
import java.time.LocalDate
import java.time.LocalTime
import java.time.YearMonth
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowLeft
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowRight
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import java.util.*

class MainActivity : ComponentActivity() {
    private val viewModel: CategoryViewModel by viewModels()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            val themeMode by viewModel.themeMode.collectAsStateWithLifecycle()
            val dynamicColorEnabled by viewModel.dynamicColor.collectAsStateWithLifecycle()
            val appTheme by viewModel.appTheme.collectAsStateWithLifecycle()
            val systemDark = isSystemInDarkTheme()
            val darkTheme = when (themeMode) {
                "LIGHT" -> false
                "DARK" -> true
                else -> systemDark
            }
            MyApplicationTheme(darkTheme = darkTheme, dynamicColor = dynamicColorEnabled, appTheme = appTheme) {
                AppBackground(modifier = Modifier.fillMaxSize()) {
                    CategoryColorManagerApp(viewModel = viewModel)
                }
            }
        }
    }
}

// Helper to format values with Rupee symbol and Indian locale grouping (e.g., ₹12,34,567)
fun formatInRupee(amount: Double, currency: String = "₹"): String {
    return try {
        val numberFormat = NumberFormat.getNumberInstance(Locale("en", "IN"))
        numberFormat.minimumFractionDigits = 0
        numberFormat.maximumFractionDigits = 2
        currency + " " + numberFormat.format(amount)
    } catch (e: Exception) {
        currency + " " + NumberFormat.getNumberInstance(Locale.US).format(amount)
    }
}

// Map strings to beautiful Material Icon vectors
fun getIconVector(name: String): ImageVector {
    return when (name.lowercase()) {
        "restaurant", "food", "dining" -> Icons.Default.Restaurant
        "directions_car", "transport", "car" -> Icons.Default.DirectionsCar
        "shopping_bag", "shopping", "bag" -> Icons.Default.ShoppingBag
        "medical_services", "health", "hospital" -> Icons.Default.MedicalServices
        "movie", "entertainment", "play" -> Icons.Default.Movie
        "receipt_long", "bills", "invoice" -> Icons.Default.ReceiptLong
        "flight", "travel", "plane" -> Icons.Default.Flight
        "school", "education", "book" -> Icons.Default.School
        "fitness_center", "gym" -> Icons.Default.FitnessCenter
        "home", "house" -> Icons.Default.Home
        "build", "tools" -> Icons.Default.Build
        "pets" -> Icons.Default.Pets
        else -> Icons.Default.Category
    }
}

// Map subcategory names to appropriate, compile-safe Material Icon vectors
fun getSubcategoryIcon(name: String): ImageVector {
    val lower = name.lowercase()
    return when {
        lower.contains("breakfast") || lower.contains("lunch") || lower.contains("dinner") || lower.contains("snack") || lower.contains("tea") || lower.contains("coffee") || lower.contains("food") || lower.contains("dining") -> Icons.Default.Restaurant
        lower.contains("fuel") || lower.contains("gas") || lower.contains("petrol") || lower.contains("car") || lower.contains("taxi") || lower.contains("cab") -> Icons.Default.DirectionsCar
        lower.contains("train") || lower.contains("metro") || lower.contains("bus") || lower.contains("transit") || lower.contains("flight") || lower.contains("travel") -> Icons.Default.Flight
        lower.contains("movie") || lower.contains("show") || lower.contains("theater") || lower.contains("play") || lower.contains("game") || lower.contains("entertainment") -> Icons.Default.Movie
        lower.contains("gym") || lower.contains("workout") || lower.contains("fitness") -> Icons.Default.FitnessCenter
        lower.contains("rent") || lower.contains("bill") || lower.contains("electricity") || lower.contains("water") || lower.contains("utility") -> Icons.Default.ReceiptLong
        lower.contains("hospital") || lower.contains("doctor") || lower.contains("health") || lower.contains("medicine") || lower.contains("dental") -> Icons.Default.MedicalServices
        lower.contains("pet") || lower.contains("dog") || lower.contains("cat") -> Icons.Default.Pets
        lower.contains("book") || lower.contains("school") || lower.contains("education") || lower.contains("class") -> Icons.Default.School
        lower.contains("rent") || lower.contains("home") || lower.contains("house") -> Icons.Default.Home
        lower.contains("repair") || lower.contains("tool") || lower.contains("fix") -> Icons.Default.Build
        lower.contains("shop") || lower.contains("cloth") || lower.contains("grocer") || lower.contains("bag") -> Icons.Default.ShoppingBag
        else -> Icons.Default.Category
    }
}

val ICON_OPTIONS = listOf(
    "restaurant", "directions_car", "shopping_bag", "medical_services",
    "movie", "receipt_long", "flight", "school", "fitness_center", "home",
    "build", "pets"
)

enum class AnalysisPeriod {
    WEEKLY, MONTHLY, YEARLY
}

fun calculatePeriodStats(
    categories: List<Category>,
    transactions: List<Transaction>,
    period: AnalysisPeriod,
    selectedMonth: YearMonth = YearMonth.now()
): Map<Int, CategoryStats> {
    val zone = ZoneId.systemDefault()
    val isCurrentMonth = selectedMonth == YearMonth.now()
    val anchorDate = if (isCurrentMonth) LocalDate.now() else selectedMonth.atEndOfMonth()
    val anchorMillis = if (isCurrentMonth) {
        System.currentTimeMillis()
    } else {
        selectedMonth.atEndOfMonth().atTime(LocalTime.MAX).atZone(zone).toInstant().toEpochMilli()
    }

    // Filter transactions based on period anchored to selectedMonth (and type == "EXPENSE")
    val (filteredTransactions, previousPeriodTransactions) = when (period) {
        AnalysisPeriod.WEEKLY -> {
            val weekStart = anchorDate.minusDays(6).atStartOfDay(zone).toInstant().toEpochMilli()
            val weekEnd = anchorMillis
            val prevWeekStart = anchorDate.minusDays(13).atStartOfDay(zone).toInstant().toEpochMilli()
            val prevWeekEnd = weekStart - 1

            val curr = transactions.filter { it.timestamp in weekStart..weekEnd && it.type == "EXPENSE" }
            val prev = transactions.filter { it.valueTimestamp() in prevWeekStart..prevWeekEnd && it.type == "EXPENSE" }
            curr to prev
        }
        AnalysisPeriod.MONTHLY -> {
            val monthStart = selectedMonth.atDay(1).atStartOfDay(zone).toInstant().toEpochMilli()
            val monthEnd = selectedMonth.atEndOfMonth().atTime(LocalTime.MAX).atZone(zone).toInstant().toEpochMilli()

            val prevMonth = selectedMonth.minusMonths(1)
            val prevMonthStart = prevMonth.atDay(1).atStartOfDay(zone).toInstant().toEpochMilli()
            val prevMonthEnd = prevMonth.atEndOfMonth().atTime(LocalTime.MAX).atZone(zone).toInstant().toEpochMilli()

            val curr = transactions.filter { it.timestamp in monthStart..monthEnd && it.type == "EXPENSE" }
            val prev = transactions.filter { it.valueTimestamp() in prevMonthStart..prevMonthEnd && it.type == "EXPENSE" }
            curr to prev
        }
        AnalysisPeriod.YEARLY -> {
            val yearStart = selectedMonth.minusMonths(11).atDay(1).atStartOfDay(zone).toInstant().toEpochMilli()
            val yearEnd = anchorMillis
            val prevYearStart = selectedMonth.minusMonths(23).atDay(1).atStartOfDay(zone).toInstant().toEpochMilli()
            val prevYearEnd = yearStart - 1

            val curr = transactions.filter { it.timestamp in yearStart..yearEnd && it.type == "EXPENSE" }
            val prev = transactions.filter { it.valueTimestamp() in prevYearStart..prevYearEnd && it.type == "EXPENSE" }
            curr to prev
        }
    }

    val totalSpend = filteredTransactions.sumOf { it.amount }

    return categories.associate { category ->
        val catCurrentTx = filteredTransactions.filter { it.categoryId == category.id }
        val catPreviousTx = previousPeriodTransactions.filter { it.categoryId == category.id }

        val currentSum = catCurrentTx.sumOf { it.amount }
        val previousSum = catPreviousTx.sumOf { it.amount }

        val percentage = if (totalSpend > 0.0) {
            (currentSum / totalSpend) * 100.0
        } else {
            0.0
        }

        val trend = if (previousSum > 0.0) {
            ((currentSum - previousSum) / previousSum) * 100.0
        } else if (currentSum > 0.0) {
            100.0
        } else {
            0.0
        }

        category.id to CategoryStats(
            categoryId = category.id,
            totalAmount = currentSum,
            transactionCount = catCurrentTx.size,
            percentage = percentage,
            trendPercentage = trend,
            currentMonthTotal = currentSum,
            previousMonthTotal = previousSum
        )
    }
}

// Extension to ensure we get timestamp correctly (assuming transaction has it)
fun Transaction.valueTimestamp(): Long {
    return this.timestamp
}

// Primary Screen Component
@OptIn(ExperimentalMaterial3Api::class, ExperimentalLayoutApi::class)
@Composable
fun CategoryColorManagerApp(viewModel: CategoryViewModel) {
    val scope = rememberCoroutineScope()
    val context = LocalContext.current
    val haptic = LocalHapticFeedback.current

    val categories by viewModel.categories.collectAsStateWithLifecycle()
    val subcategories by viewModel.subcategories.collectAsStateWithLifecycle()
    val transactions by viewModel.transactions.collectAsStateWithLifecycle()
    val stats by viewModel.categoryStats.collectAsStateWithLifecycle()
    val currentTheme by viewModel.currentPaletteTheme.collectAsStateWithLifecycle()
    val selectedCatId by viewModel.selectedCategoryId.collectAsStateWithLifecycle()
    val themeMode by viewModel.themeMode.collectAsStateWithLifecycle()
    val dynamicColorEnabled by viewModel.dynamicColor.collectAsStateWithLifecycle()
    val currencySymbol by viewModel.currency.collectAsStateWithLifecycle()
    val defaultType by viewModel.defaultTransactionType.collectAsStateWithLifecycle()
    val onboardingComplete by viewModel.onboardingComplete.collectAsStateWithLifecycle()
    val currentAppTheme by viewModel.appTheme.collectAsStateWithLifecycle()

    // Month navigation & scoping states
    val selectedMonth by viewModel.selectedMonth.collectAsStateWithLifecycle()
    val currentCalendarMonth = remember { YearMonth.now() }
    val isCurrentCalendarMonth = selectedMonth == currentCalendarMonth
    val isPastCalendarMonth = selectedMonth.isBefore(currentCalendarMonth)
    var isPastMonthUnlocked by remember { mutableStateOf(false) }
    val canEdit = isCurrentCalendarMonth || isPastMonthUnlocked
    var showMonthPickerSheet by remember { mutableStateOf(false) }

    // Navigation controllers & periods for advanced Analysis
    val navController = rememberNavController()
    val navBackStackEntry by navController.currentBackStackEntryAsState()
    val currentRoute = navBackStackEntry?.destination?.route ?: "home"

    // Reset past month unlock state when navigating to another month or tab
    LaunchedEffect(selectedMonth) {
        isPastMonthUnlocked = false
    }
    LaunchedEffect(currentRoute) {
        if (currentRoute != "home") {
            isPastMonthUnlocked = false
        }
    }

    // Month-scoped transactions and aggregations
    val monthTransactions = remember(transactions, selectedMonth) {
        transactions.filter { tx ->
            val ym = YearMonth.from(Instant.ofEpochMilli(tx.timestamp).atZone(ZoneId.systemDefault()))
            ym == selectedMonth
        }
    }

    val totalMonthIncome = remember(monthTransactions) {
        monthTransactions
            .filter { it.type == "INCOME" }
            .sumOf { it.amount }
    }

    val totalMonthSpend = remember(monthTransactions) {
        monthTransactions
            .filter { it.type == "EXPENSE" }
            .sumOf { it.amount }
    }

    val lastMonthSpendVal = remember(transactions, selectedMonth) {
        val prevMonth = selectedMonth.minusMonths(1)
        transactions
            .filter { it.type == "EXPENSE" && YearMonth.from(Instant.ofEpochMilli(it.timestamp).atZone(ZoneId.systemDefault())) == prevMonth }
            .sumOf { it.amount }
    }

    // Document Creation Launcher (for JSON backup export)
    val exportLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.CreateDocument("application/json")
    ) { uri ->
        uri?.let {
            try {
                context.contentResolver.openOutputStream(it)?.use { os ->
                    val jsonBytes = viewModel.exportBackupJson().toByteArray()
                    os.write(jsonBytes)
                }
                Toast.makeText(context, "Data exported successfully!", Toast.LENGTH_SHORT).show()
            } catch (e: Exception) {
                Toast.makeText(context, "Export failed: ${e.message}", Toast.LENGTH_SHORT).show()
            }
        }
    }

    // Document Picker Launcher (for JSON backup import)
    val importLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.OpenDocument()
    ) { uri ->
        uri?.let {
            try {
                context.contentResolver.openInputStream(it)?.use { ins ->
                    val jsonStr = ins.bufferedReader().use { r -> r.readText() }
                    val success = viewModel.importBackupJson(jsonStr)
                    if (success) {
                        Toast.makeText(context, "Data imported & restored successfully!", Toast.LENGTH_SHORT).show()
                    } else {
                        Toast.makeText(context, "Failed to restore backup: Invalid JSON", Toast.LENGTH_SHORT).show()
                    }
                }
            } catch (e: Exception) {
                Toast.makeText(context, "Import failed: ${e.message}", Toast.LENGTH_SHORT).show()
            }
        }
    }


    val snackbarHostState = remember { SnackbarHostState() }

    // Modal state controllers
    var showAddCategoryDialog by remember { mutableStateOf(false) }
    var addCategoryDefaultType by remember { mutableStateOf("EXPENSE") }
    var showAddTransactionDialog by remember { mutableStateOf(false) }
    var editCategoryTarget by remember { mutableStateOf<Category?>(null) }
    var transactionToEdit by remember { mutableStateOf<Transaction?>(null) }
    var categoryToDelete by remember { mutableStateOf<Category?>(null) }
    var showClearAllDialog by remember { mutableStateOf(false) }
    var resetConfirmationText by remember { mutableStateOf("") }
    
    // Quick search & display configuration
    var searchQuery by remember { mutableStateOf("") }
    var selectedTypeFilter by remember { mutableStateOf("ALL") } // "ALL", "EXPENSE", "INCOME"
    var showTypeFilters by remember { mutableStateOf(true) }
    var isViewAllTransactions by remember { mutableStateOf(false) }
    var selectedChartTab by remember { mutableIntStateOf(0) } // 0: Donut, 1: Bar, 2: Trend

    var selectedAnalysisPeriod by remember { mutableStateOf(AnalysisPeriod.MONTHLY) }

    val periodStats = remember(categories, transactions, selectedAnalysisPeriod, selectedMonth) {
        calculatePeriodStats(categories, transactions, selectedAnalysisPeriod, selectedMonth)
    }
    val totalPeriodSpend = periodStats.values.sumOf { it.totalAmount }

    val filteredTrendTransactions = remember(transactions, selectedAnalysisPeriod, selectedMonth) {
        val zone = ZoneId.systemDefault()
        val isCurrentMonth = selectedMonth == YearMonth.now()
        val anchorDate = if (isCurrentMonth) LocalDate.now() else selectedMonth.atEndOfMonth()
        val anchorMillis = if (isCurrentMonth) {
            System.currentTimeMillis()
        } else {
            selectedMonth.atEndOfMonth().atTime(LocalTime.MAX).atZone(zone).toInstant().toEpochMilli()
        }
        when (selectedAnalysisPeriod) {
            AnalysisPeriod.WEEKLY -> {
                val start = anchorDate.minusDays(6).atStartOfDay(zone).toInstant().toEpochMilli()
                transactions.filter { it.timestamp in start..anchorMillis && it.type == "EXPENSE" }
            }
            AnalysisPeriod.MONTHLY -> {
                val start = selectedMonth.atDay(1).atStartOfDay(zone).toInstant().toEpochMilli()
                val end = selectedMonth.atEndOfMonth().atTime(LocalTime.MAX).atZone(zone).toInstant().toEpochMilli()
                transactions.filter { it.timestamp in start..end && it.type == "EXPENSE" }
            }
            AnalysisPeriod.YEARLY -> {
                val start = selectedMonth.minusMonths(11).atDay(1).atStartOfDay(zone).toInstant().toEpochMilli()
                transactions.filter { it.timestamp in start..anchorMillis && it.type == "EXPENSE" }
            }
        }
    }

    val isLendingScreen = currentRoute == "lending" || (currentRoute?.startsWith("lending_contact") == true)

    Scaffold(
        containerColor = Color.Transparent,
        contentColor = MaterialTheme.colorScheme.onBackground,
        snackbarHost = { SnackbarHost(snackbarHostState) },
        topBar = {
            // Home hosts its own branded header inside the scroll body so it can scroll away and
            // hand the whole viewport back to content; the other tabs keep a lightweight title.
            if (currentRoute != "onboarding" && currentRoute != "home" && !isLendingScreen) {
                CenterAlignedTopAppBar(
                    title = {
                        Text(
                            text = when (currentRoute) {
                                "insights" -> "INSIGHTS"
                                "settings" -> "SETTINGS"
                                "categories" -> "CATEGORIES"
                                else -> "SHYLOCK"
                            },
                            fontWeight = FontWeight.ExtraBold,
                            fontSize = 20.sp,
                            letterSpacing = 1.sp,
                            color = MaterialTheme.colorScheme.primary
                        )
                    },
                    colors = TopAppBarDefaults.centerAlignedTopAppBarColors(
                        containerColor = Color.Transparent
                    )
                )
            }
        },
        floatingActionButton = {
            if (currentRoute != "onboarding") {
                if (currentRoute == "home") {
                    if (canEdit) {
                        FloatingActionButton(
                            onClick = {
                                haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                                showAddTransactionDialog = true
                            },
                            containerColor = MaterialTheme.colorScheme.primary,
                            contentColor = MaterialTheme.colorScheme.onPrimary,
                            shape = CircleShape,
                            elevation = FloatingActionButtonDefaults.elevation(
                                defaultElevation = 0.dp,
                                pressedElevation = 0.dp
                            ),
                            modifier = Modifier
                                .accentGlow(
                                    color = MaterialTheme.colorScheme.primary,
                                    shape = CircleShape,
                                    elevation = 22.dp
                                )
                                .size(62.dp)
                                .testTag("add_transaction_btn")
                        ) {
                            Icon(
                                imageVector = Icons.Default.Add,
                                contentDescription = "Add New Record",
                                modifier = Modifier.size(28.dp)
                            )
                        }
                    }
                } else if (currentRoute == "categories") {
                    FloatingActionButton(
                    onClick = {
                        haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                        showAddCategoryDialog = true
                    },
                    containerColor = MaterialTheme.colorScheme.primary,
                    contentColor = MaterialTheme.colorScheme.onPrimary,
                    modifier = Modifier.testTag("add_category_btn"),
                    shape = RoundedCornerShape(12.dp)
                ) {
                    Icon(imageVector = Icons.Default.Add, contentDescription = "Add Category")
                }
            }
        }
    },
        bottomBar = {
            if (currentRoute != "onboarding" && !isLendingScreen) {
                val navShape = RoundedCornerShape(28.dp)
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp, vertical = 10.dp)
                        .liquidGlass(shape = navShape, elevation = 10.dp)
                ) {
                    NavigationBar(
                        containerColor = Color.Transparent,
                        tonalElevation = 0.dp
                    ) {
                        val navItems = listOf(
                            Triple("home", Icons.Default.Home, "Home"),
                            Triple("insights", Icons.Default.TrendingUp, "Insights"),
                            Triple("settings", Icons.Default.Settings, "Settings")
                        )
                        navItems.forEach { (route, icon, label) ->
                            val selected = currentRoute == route
                            NavigationBarItem(
                                selected = selected,
                                onClick = {
                                    if (currentRoute != route) {
                                        navController.navigate(route) {
                                            popUpTo("home") { saveState = true }
                                            launchSingleTop = true
                                            restoreState = true
                                        }
                                    }
                                },
                                icon = { Icon(imageVector = icon, contentDescription = label) },
                                label = {
                                    Text(
                                        label,
                                        fontWeight = if (selected) FontWeight.Bold else FontWeight.Medium,
                                        fontSize = 12.sp
                                    )
                                },
                                colors = NavigationBarItemDefaults.colors(
                                    selectedIconColor = MaterialTheme.colorScheme.primary,
                                    selectedTextColor = MaterialTheme.colorScheme.primary,
                                    indicatorColor = MaterialTheme.colorScheme.primary.copy(alpha = 0.18f),
                                    unselectedIconColor = MaterialTheme.colorScheme.onSurfaceVariant,
                                    unselectedTextColor = MaterialTheme.colorScheme.onSurfaceVariant
                                ),
                                modifier = Modifier.testTag("nav_$route")
                            )
                        }
                    }
                }
            }
        }
    ) { innerPadding ->
        val startDest = if (onboardingComplete) "home" else "onboarding"
        NavHost(
            navController = navController,
            startDestination = startDest,
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
        ) {
            // === Destination 1: Home (records/dashboard) ===
            composable("home") {
                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .verticalScroll(rememberScrollState())
                        .padding(start = 16.dp, end = 16.dp, top = 8.dp, bottom = 80.dp),
                    verticalArrangement = Arrangement.spacedBy(14.dp)
                ) {
                // Branded header — scrolls away with the content to free up the viewport
                ShylockBrandHeader()

                // Month Navigation Controls
                MonthNavigationBar(
                    selectedMonth = selectedMonth,
                    isCurrentMonth = isCurrentCalendarMonth,
                    onPrevMonth = { viewModel.selectMonth(selectedMonth.minusMonths(1)) },
                    onNextMonth = { viewModel.selectMonth(selectedMonth.plusMonths(1)) },
                    onLabelClick = { showMonthPickerSheet = true }
                )

                // Read-only past month banner
                if (isPastCalendarMonth) {
                    PastMonthBanner(
                        selectedMonth = selectedMonth,
                        isUnlocked = isPastMonthUnlocked,
                        onUnlock = { isPastMonthUnlocked = true },
                        onLock = { isPastMonthUnlocked = false }
                    )
                }

                // ================== RECORDS TAB ==================
                // Dashboard Summary Header
                val incomeCount = remember(monthTransactions) { monthTransactions.count { it.type == "INCOME" } }
                val expenseCount = remember(monthTransactions) { monthTransactions.count { it.type == "EXPENSE" } }
                val spendSparkline = remember(monthTransactions, selectedMonth) {
                    buildSpendSparkline(monthTransactions, selectedMonth)
                }

                DashboardHeader(
                    title = if (isCurrentCalendarMonth) "This Month's Summary" else "Monthly Summary",
                    totalMonthSpend = totalMonthSpend,
                    totalMonthIncome = totalMonthIncome,
                    categoriesCount = categories.size,
                    lastMonthSpend = lastMonthSpendVal,
                    currencySymbol = currencySymbol,
                    incomeCount = incomeCount,
                    expenseCount = expenseCount,
                    sparkline = spendSparkline
                )

                // Lending Ledger Compact Summary Card (Separate Ledger)
                val lendingEntries by viewModel.lendingEntries.collectAsStateWithLifecycle()
                val lendingContacts by viewModel.lendingContacts.collectAsStateWithLifecycle()
                val totalLendingOutstanding = remember(lendingEntries) {
                    lendingEntries.fold(0.0) { acc, entry ->
                        if (entry.direction == "LENT") acc + entry.amount else acc - entry.amount
                    }
                }
                LendingSummaryCard(
                    totalOutstanding = totalLendingOutstanding,
                    currencySymbol = currencySymbol,
                    contactsCount = lendingContacts.size,
                    onClick = { navController.navigate("lending") }
                )


                if (categories.isEmpty()) {
                    // Show beautiful Guided Empty State / First-Run screen
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 12.dp)
                            .liquidGlass(shape = RoundedCornerShape(28.dp), elevation = 6.dp)
                            .testTag("guided_empty_state_card")
                    ) {
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(24.dp),
                            horizontalAlignment = Alignment.CenterHorizontally,
                            verticalArrangement = Arrangement.spacedBy(16.dp)
                        ) {
                            Box(
                                modifier = Modifier
                                    .size(64.dp)
                                    .clip(CircleShape)
                                    .background(MaterialTheme.colorScheme.primary.copy(alpha = 0.15f)),
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(
                                    imageVector = Icons.Default.AccountBalanceWallet,
                                    contentDescription = "Wallet Icon",
                                    tint = MaterialTheme.colorScheme.primary,
                                    modifier = Modifier.size(32.dp)
                                )
                            }

                            Text(
                                text = "Welcome to Shylock",
                                style = MaterialTheme.typography.headlineSmall,
                                fontWeight = FontWeight.ExtraBold,
                                color = MaterialTheme.colorScheme.onSurface,
                                textAlign = TextAlign.Center
                            )

                            Text(
                                text = "Set up your custom color-budget categories to start tracking spending, or explore Shylock's color-consistent transactions and trend charts using sandbox data.",
                                style = MaterialTheme.typography.bodyMedium,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                textAlign = TextAlign.Center,
                                modifier = Modifier.padding(horizontal = 8.dp)
                            )

                            Column(
                                modifier = Modifier.fillMaxWidth(),
                                verticalArrangement = Arrangement.spacedBy(10.dp)
                            ) {
                                Button(
                                    onClick = { showAddCategoryDialog = true },
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .height(48.dp)
                                        .testTag("empty_add_category_btn"),
                                    shape = RoundedCornerShape(12.dp)
                                ) {
                                    Icon(imageVector = Icons.Default.Add, contentDescription = "Add Icon")
                                    Spacer(modifier = Modifier.width(8.dp))
                                    Text("Add Custom Category", fontWeight = FontWeight.Bold)
                                }

                                OutlinedButton(
                                    onClick = { viewModel.seedDemoData() },
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .height(48.dp)
                                        .testTag("empty_load_sample_btn"),
                                    shape = RoundedCornerShape(12.dp),
                                    border = BorderStroke(1.5.dp, MaterialTheme.colorScheme.primary)
                                ) {
                                    Icon(imageVector = Icons.Default.AutoMode, contentDescription = "Demo Icon")
                                    Spacer(modifier = Modifier.width(8.dp))
                                    Text("Load Sample Sandbox Data", fontWeight = FontWeight.Bold)
                                }
                            }
                        }
                    }
                } else {
                    if (monthTransactions.isEmpty()) {
                        // Short card on Home when no transactions in selected month
                        val monthFormatter = remember { DateTimeFormatter.ofPattern("MMMM yyyy", Locale.getDefault()) }
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .liquidGlass(shape = RoundedCornerShape(24.dp), elevation = 4.dp)
                                .testTag("transactions_empty_state_card")
                        ) {
                            Column(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(20.dp),
                                horizontalAlignment = Alignment.CenterHorizontally,
                                verticalArrangement = Arrangement.spacedBy(12.dp)
                            ) {
                                Icon(
                                    imageVector = Icons.Default.SwapHoriz,
                                    contentDescription = "No transactions",
                                    tint = MaterialTheme.colorScheme.primary.copy(alpha = 0.6f),
                                    modifier = Modifier.size(40.dp)
                                )
                                Text(
                                    text = if (isCurrentCalendarMonth)
                                        "Ready to start tracking? Add your first transaction for ${selectedMonth.format(monthFormatter)}!"
                                    else
                                        "No transactions recorded for ${selectedMonth.format(monthFormatter)}.",
                                    style = MaterialTheme.typography.bodyMedium,
                                    fontWeight = FontWeight.Medium,
                                    color = MaterialTheme.colorScheme.onSurface,
                                    textAlign = androidx.compose.ui.text.style.TextAlign.Center
                                )
                                if (canEdit) {
                                    Button(
                                        onClick = { showAddTransactionDialog = true },
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .height(44.dp)
                                            .testTag("empty_add_transaction_btn"),
                                        shape = RoundedCornerShape(10.dp)
                                    ) {
                                        Icon(imageVector = Icons.Default.Add, contentDescription = "Add transaction")
                                        Spacer(modifier = Modifier.width(8.dp))
                                        Text("Add Transaction", fontWeight = FontWeight.Bold)
                                    }
                                }
                                Text(
                                    text = "Manage Categories & Budgets",
                                    style = MaterialTheme.typography.bodySmall.copy(
                                        fontWeight = FontWeight.Bold,
                                        textDecoration = androidx.compose.ui.text.style.TextDecoration.Underline
                                    ),
                                    color = MaterialTheme.colorScheme.primary,
                                    modifier = Modifier
                                        .clickable {
                                            navController.navigate("categories")
                                        }
                                        .padding(4.dp)
                                        .testTag("empty_manage_categories_link")
                                )
                            }
                        }
                    } else {
                        // Recent Transaction list using Category Colors consistently
                        GlassCard(
                            modifier = Modifier.fillMaxWidth(),
                            shape = RoundedCornerShape(28.dp),
                            elevation = 6.dp,
                            contentPadding = PaddingValues(top = 18.dp, bottom = 8.dp)
                        ) {
                            Column {
                                // Header row with Title and "View All" toggle button
                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(horizontal = 18.dp),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Column {
                                        Text(
                                            text = "Recent Transactions",
                                            style = MaterialTheme.typography.titleLarge,
                                            fontWeight = FontWeight.ExtraBold,
                                            fontSize = 20.sp,
                                            color = MaterialTheme.colorScheme.onSurface
                                        )
                                        Text(
                                            text = "${monthTransactions.size} total this month",
                                            style = MaterialTheme.typography.labelMedium,
                                            color = MaterialTheme.colorScheme.outline
                                        )
                                    }

                                    TextButton(
                                        onClick = { isViewAllTransactions = !isViewAllTransactions },
                                        contentPadding = PaddingValues(horizontal = 8.dp, vertical = 4.dp)
                                    ) {
                                        Text(
                                            text = if (isViewAllTransactions) "Show Recent" else "View All",
                                            fontWeight = FontWeight.Bold,
                                            fontSize = 14.sp,
                                            color = MaterialTheme.colorScheme.primary
                                        )
                                        Spacer(modifier = Modifier.width(2.dp))
                                        Icon(
                                            imageVector = if (isViewAllTransactions) Icons.Default.ExpandLess else Icons.Default.ExpandMore,
                                            contentDescription = null,
                                            modifier = Modifier.size(18.dp),
                                            tint = MaterialTheme.colorScheme.primary
                                        )
                                    }
                                }

                                Spacer(modifier = Modifier.height(14.dp))

                                val filtersActive = selectedTypeFilter != "ALL" || searchQuery.isNotBlank()

                                // Search pill + filter toggle, side by side
                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(horizontal = 18.dp),
                                    horizontalArrangement = Arrangement.spacedBy(10.dp),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    GlassSearchField(
                                        value = searchQuery,
                                        onValueChange = { searchQuery = it },
                                        modifier = Modifier
                                            .weight(1f)
                                            .testTag("transaction_search_input")
                                    )

                                    val filterShape = RoundedCornerShape(18.dp)
                                    Box(
                                        modifier = Modifier
                                            .size(56.dp)
                                            .liquidGlass(shape = filterShape, strength = 0.7f)
                                            .clickable { showTypeFilters = !showTypeFilters }
                                            .testTag("transaction_filter_toggle"),
                                        contentAlignment = Alignment.Center
                                    ) {
                                        Icon(
                                            imageVector = Icons.Default.Tune,
                                            contentDescription = if (showTypeFilters) "Hide filters" else "Show filters",
                                            tint = if (filtersActive || showTypeFilters)
                                                MaterialTheme.colorScheme.primary
                                            else
                                                MaterialTheme.colorScheme.onSurfaceVariant,
                                            modifier = Modifier.size(22.dp)
                                        )
                                        if (filtersActive) {
                                            Box(
                                                modifier = Modifier
                                                    .align(Alignment.TopEnd)
                                                    .padding(top = 12.dp, end = 12.dp)
                                                    .size(7.dp)
                                                    .clip(CircleShape)
                                                    .background(MaterialTheme.colorScheme.primary)
                                            )
                                        }
                                    }
                                }

                                // Type Filters as sleek glowing pills
                                AnimatedVisibility(visible = showTypeFilters) {
                                    Row(
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .padding(horizontal = 18.dp)
                                            .padding(top = 12.dp),
                                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                                    ) {
                                        listOf("ALL" to "All", "EXPENSE" to "Expenses", "INCOME" to "Income").forEach { (filterVal, label) ->
                                            val accent = when (filterVal) {
                                                "INCOME" -> IncomeGreen
                                                "EXPENSE" -> ExpenseRed
                                                else -> MaterialTheme.colorScheme.primary
                                            }
                                            TypeFilterPill(
                                                label = label,
                                                selected = selectedTypeFilter == filterVal,
                                                accent = accent,
                                                onClick = { selectedTypeFilter = filterVal },
                                                modifier = Modifier.weight(1f)
                                            )
                                        }
                                    }
                                }

                                Spacer(modifier = Modifier.height(6.dp))

                                // Transaction Filtering based on Highlighted Category, searchQuery, and selectedTypeFilter within the selected month
                                val filteredTransactions = remember(monthTransactions, selectedCatId, searchQuery, selectedTypeFilter) {
                                    var list = monthTransactions
                                    if (selectedCatId != null) {
                                        list = list.filter { it.categoryId == selectedCatId }
                                    }
                                    if (selectedTypeFilter != "ALL") {
                                        list = list.filter { it.type == selectedTypeFilter }
                                    }
                                    if (searchQuery.isNotBlank()) {
                                        list = list.filter {
                                            it.description.contains(searchQuery, ignoreCase = true) ||
                                            it.amount.toString().contains(searchQuery)
                                        }
                                    }
                                    list
                                }

                                val displayedTransactions = remember(filteredTransactions, isViewAllTransactions) {
                                    if (isViewAllTransactions) filteredTransactions else filteredTransactions.take(6)
                                }

                                if (filteredTransactions.isEmpty()) {
                                    Column(
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .padding(vertical = 32.dp, horizontal = 18.dp),
                                        horizontalAlignment = Alignment.CenterHorizontally
                                    ) {
                                        Box(
                                            modifier = Modifier
                                                .size(52.dp)
                                                .clip(CircleShape)
                                                .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)),
                                            contentAlignment = Alignment.Center
                                        ) {
                                            Icon(
                                                imageVector = Icons.Default.ReceiptLong,
                                                contentDescription = "No transactions found",
                                                tint = MaterialTheme.colorScheme.outline,
                                                modifier = Modifier.size(26.dp)
                                            )
                                        }
                                        Spacer(modifier = Modifier.height(10.dp))
                                        Text(
                                            text = if (selectedCatId != null) "No transactions color-matched to this category." else "No records match search & filter.",
                                            style = MaterialTheme.typography.bodyMedium,
                                            color = MaterialTheme.colorScheme.outline,
                                            textAlign = TextAlign.Center
                                        )
                                    }
                                } else {
                                    // Flat, divider-separated rows so the glass panel reads as one
                                    // continuous surface instead of a stack of nested cards
                                    Column(modifier = Modifier.animateContentSize()) {
                                        displayedTransactions.forEachIndexed { index, tx ->
                                            val cat = categories.find { it.id == tx.categoryId }
                                            val sub = subcategories.find { it.id == tx.subcategoryId }
                                            val isLinked = tx.lendingEntryId != null
                                            if (index > 0) {
                                                HorizontalDivider(
                                                    modifier = Modifier.padding(horizontal = 18.dp),
                                                    thickness = 0.7.dp,
                                                    color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.25f)
                                                )
                                            }
                                            TransactionItemRow(
                                                transaction = tx,
                                                category = cat,
                                                subcategory = sub,
                                                currency = currencySymbol,
                                                readOnly = !canEdit || isLinked,
                                                onDelete = {
                                                    if (canEdit && !isLinked) {
                                                        scope.launch {
                                                            haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                                                            val deletedTx = tx
                                                            viewModel.deleteTransaction(deletedTx)
                                                            val result = snackbarHostState.showSnackbar(
                                                                message = "Transaction deleted",
                                                                actionLabel = "Undo",
                                                                duration = SnackbarDuration.Short
                                                            )
                                                            if (result == SnackbarResult.ActionPerformed) {
                                                                viewModel.reinsertTransaction(deletedTx)
                                                            }
                                                        }
                                                    }
                                                },
                                                onEdit = {
                                                    if (canEdit && !isLinked) {
                                                        transactionToEdit = tx
                                                    }
                                                }
                                            )
                                        }

                                        // Subtle footer hint when list is truncated
                                        if (!isViewAllTransactions && filteredTransactions.size > 6) {
                                            TextButton(
                                                onClick = { isViewAllTransactions = true },
                                                modifier = Modifier.fillMaxWidth()
                                            ) {
                                                Text(
                                                    text = "Show ${filteredTransactions.size - 6} more transactions",
                                                    style = MaterialTheme.typography.labelMedium,
                                                    fontWeight = FontWeight.SemiBold,
                                                    color = MaterialTheme.colorScheme.primary
                                                )
                                            }
                                        }
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }

        composable("insights") {
        val monthFormatter = remember { DateTimeFormatter.ofPattern("MMMM yyyy", Locale.getDefault()) }
        Column(
            modifier = Modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
                // Month Navigation Controls (shared across Home and Insights)
                MonthNavigationBar(
                    selectedMonth = selectedMonth,
                    isCurrentMonth = isCurrentCalendarMonth,
                    onPrevMonth = { viewModel.selectMonth(selectedMonth.minusMonths(1)) },
                    onNextMonth = { viewModel.selectMonth(selectedMonth.plusMonths(1)) },
                    onLabelClick = { showMonthPickerSheet = true },
                    modifier = Modifier.testTag("insights_month_navigation_bar")
                )

                // Past month indicator banner if viewing a past month
                if (isPastCalendarMonth) {
                    Surface(
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("insights_past_month_banner"),
                        shape = RoundedCornerShape(12.dp),
                        color = MaterialTheme.colorScheme.secondaryContainer.copy(alpha = 0.4f),
                        border = BorderStroke(1.dp, MaterialTheme.colorScheme.secondary.copy(alpha = 0.2f))
                    ) {
                        Row(
                            modifier = Modifier.padding(horizontal = 14.dp, vertical = 8.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(
                                imageVector = Icons.Default.History,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.secondary,
                                modifier = Modifier.size(16.dp)
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(
                                text = "Viewing insights for ${selectedMonth.format(monthFormatter)}",
                                style = MaterialTheme.typography.bodySmall,
                                fontWeight = FontWeight.SemiBold,
                                color = MaterialTheme.colorScheme.onSecondaryContainer
                            )
                        }
                    }
                }

                // ================== ANALYTICS TAB ==================
                // Period spending summary card
                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("insights_summary_card"),
                    shape = RoundedCornerShape(20.dp),
                    colors = CardDefaults.cardColors(
                        containerColor = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.4f)
                    )
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(18.dp),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Text(
                            text = when (selectedAnalysisPeriod) {
                                AnalysisPeriod.WEEKLY -> "Weekly breakdown • ${selectedMonth.format(monthFormatter)}"
                                AnalysisPeriod.MONTHLY -> "${selectedMonth.format(monthFormatter)} spending breakdown"
                                AnalysisPeriod.YEARLY -> "Yearly breakdown (${selectedMonth.year})"
                            }.uppercase(),
                            style = MaterialTheme.typography.labelMedium,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.primary,
                            letterSpacing = 1.sp,
                            textAlign = TextAlign.Center
                        )
                        Spacer(modifier = Modifier.height(8.dp))
                        Text(
                            text = formatInRupee(totalPeriodSpend, currency = currencySymbol),
                            style = MaterialTheme.typography.headlineLarge,
                            fontWeight = FontWeight.ExtraBold,
                            color = MaterialTheme.colorScheme.onPrimaryContainer
                        )
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(
                            text = when (selectedAnalysisPeriod) {
                                AnalysisPeriod.WEEKLY -> "Last 7 days of ${selectedMonth.format(monthFormatter)}"
                                AnalysisPeriod.MONTHLY -> "Total spend for ${selectedMonth.format(monthFormatter)}"
                                AnalysisPeriod.YEARLY -> "12-month spend up to ${selectedMonth.format(monthFormatter)}"
                            },
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.outline
                        )
                    }
                }

                // Time Period Selector
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "Analysis Limit",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold
                    )
                    
                    SingleChoiceSegmentedButtonRow {
                        AnalysisPeriod.values().forEachIndexed { index, period ->
                            SegmentedButton(
                                selected = selectedAnalysisPeriod == period,
                                onClick = { selectedAnalysisPeriod = period },
                                shape = SegmentedButtonDefaults.itemShape(index = index, count = 3)
                            ) {
                                Text(
                                    text = period.name.lowercase().replaceFirstChar { it.uppercase() },
                                    fontSize = 11.sp,
                                    fontWeight = FontWeight.Bold
                                )
                            }
                        }
                    }
                }

                // Dynamic Charts Card Container (switching base data recursively!)
                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("analytics_container_card"),
                    shape = RoundedCornerShape(20.dp),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.3f))
                ) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = "Color Graphs",
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                            
                            // Switch between different visual charts
                            SingleChoiceSegmentedButtonRow {
                                listOf("Donut", "Bar", "Trend").forEachIndexed { index, label ->
                                    SegmentedButton(
                                        selected = selectedChartTab == index,
                                        onClick = { selectedChartTab = index },
                                        shape = SegmentedButtonDefaults.itemShape(index = index, count = 3)
                                    ) {
                                        Text(label, fontSize = 11.sp, fontWeight = FontWeight.Medium)
                                    }
                                }
                            }
                        }

                        Spacer(modifier = Modifier.height(16.dp))

                        // Rendering selected visual chart with matching Category colors
                        AnimatedContent(
                            targetState = selectedChartTab,
                            transitionSpec = { fadeIn(animationSpec = tween(220)) togetherWith fadeOut(animationSpec = tween(220)) },
                            label = "analytics_chart_tabs"
                        ) { chartTab ->
                            when (chartTab) {
                                0 -> DonutChartComponent(
                                    categories = categories,
                                    stats = periodStats,
                                    totalSpend = totalPeriodSpend,
                                    selectedCatId = selectedCatId,
                                    currencySymbol = currencySymbol,
                                    onSliceClick = { viewModel.toggleCategorySelection(it) }
                                )
                                1 -> BarChartComponent(
                                    categories = categories,
                                    stats = periodStats,
                                    selectedCatId = selectedCatId,
                                    onBarClick = { viewModel.toggleCategorySelection(it) }
                                )
                                else -> SpendingTrendChart(
                                    categories = categories,
                                    transactions = filteredTrendTransactions,
                                    selectedMonth = selectedMonth
                                )
                            }
                        }

                        Spacer(modifier = Modifier.height(12.dp))

                        // Core Interactive Category Legend System
                        CategoryLegendView(
                            categories = categories,
                            stats = periodStats,
                            selectedCatId = selectedCatId,
                            onLegendClick = { viewModel.toggleCategorySelection(it) },
                            onClearClick = { viewModel.clearCategorySelection() }
                        )
                    }
                }

                // Advanced Highlighted Category Info Floating Panel (Shows detailed calculations)
                AnimatedVisibility(
                    visible = selectedCatId != null,
                    enter = expandVertically() + fadeIn(),
                    exit = shrinkVertically() + fadeOut()
                ) {
                    selectedCatId?.let { catId ->
                        val catObj = categories.find { it.id == catId }
                        val catStatsObj = periodStats[catId]
                        if (catObj != null && catStatsObj != null) {
                            DetailedStatsPanel(
                                category = catObj,
                                stats = catStatsObj,
                                currencySymbol = currencySymbol,
                                onClear = { viewModel.clearCategorySelection() }
                            )
                        }
                    }
                }
            }
        }

        // === Destination 3: Settings ===
            composable("settings") {
                var pendingPaletteTheme by remember { mutableStateOf<PaletteTheme?>(null) }
                var showConfirmationDialog by remember { mutableStateOf(false) }

                if (showConfirmationDialog && pendingPaletteTheme != null) {
                    AlertDialog(
                        onDismissRequest = { showConfirmationDialog = false },
                        title = { Text("Update Category Colors", fontWeight = FontWeight.Bold) },
                        text = { Text("Apply this palette's colors to your existing categories?") },
                        confirmButton = {
                            TextButton(
                                onClick = {
                                    pendingPaletteTheme?.let { palette ->
                                        viewModel.setPaletteTheme(palette)
                                        viewModel.applyPaletteToExistingCategories(palette)
                                        Toast.makeText(context, "${palette.name} theme applied and categories updated", Toast.LENGTH_SHORT).show()
                                    }
                                    showConfirmationDialog = false
                                }
                            ) {
                                Text("Yes")
                            }
                        },
                        dismissButton = {
                            TextButton(
                                onClick = {
                                    pendingPaletteTheme?.let { palette ->
                                        viewModel.setPaletteTheme(palette)
                                        Toast.makeText(context, "${palette.name} theme applied (existing categories preserved)", Toast.LENGTH_SHORT).show()
                                    }
                                    showConfirmationDialog = false
                                }
                            ) {
                                Text("No")
                            }
                        }
                    )
                }

                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .verticalScroll(rememberScrollState())
                        .padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(20.dp)
                ) {
                    // Section 1: Appearance
                    Text(
                        text = "Appearance",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.primary
                    )
                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(16.dp),
                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.2f))
                    ) {
                        Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(16.dp)) {
                            // Theme mode selector (moved to Settings!)
                            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Icon(imageVector = Icons.Default.Brightness6, contentDescription = "Theme", tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(20.dp))
                                    Spacer(modifier = Modifier.width(8.dp))
                                    Text("App theme mode", fontWeight = FontWeight.Bold, style = MaterialTheme.typography.bodyLarge)
                                }
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                                ) {
                                    listOf("SYSTEM" to "System", "LIGHT" to "Light", "DARK" to "Dark").forEach { (modeVal, label) ->
                                        val isSelected = themeMode == modeVal
                                        Box(
                                            modifier = Modifier
                                                .weight(1f)
                                                .clip(RoundedCornerShape(10.dp))
                                                .background(
                                                    if (isSelected) MaterialTheme.colorScheme.primary
                                                    else MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.3f)
                                                )
                                                .border(
                                                    width = 1.dp,
                                                    color = if (isSelected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.outline.copy(alpha = 0.3f),
                                                    shape = RoundedCornerShape(10.dp)
                                                )
                                                .clickable {
                                                    haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                                                    viewModel.setThemeMode(modeVal)
                                                    Toast.makeText(context, "Theme mode set to $label", Toast.LENGTH_SHORT).show()
                                                }
                                                .padding(vertical = 10.dp),
                                            contentAlignment = Alignment.Center
                                        ) {
                                            Text(
                                                text = label,
                                                fontWeight = FontWeight.Bold,
                                                style = MaterialTheme.typography.bodyMedium,
                                                color = if (isSelected) MaterialTheme.colorScheme.onPrimary else MaterialTheme.colorScheme.onSurface
                                            )
                                        }
                                    }
                                }
                            }

                            HorizontalDivider(color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.08f))

                            // App Theme selection (live, separate from category palettes)
                            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Icon(imageVector = Icons.Default.Palette, contentDescription = "App Theme", tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(20.dp))
                                    Spacer(modifier = Modifier.width(8.dp))
                                    Text("App Theme", fontWeight = FontWeight.Bold, style = MaterialTheme.typography.bodyLarge)
                                }
                                Text(
                                    text = "Changes the overall app colors. Category colors are set separately.",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                                Column(
                                    modifier = Modifier.fillMaxWidth(),
                                    verticalArrangement = Arrangement.spacedBy(8.dp)
                                ) {
                                    val isDarkTheme = when (themeMode) {
                                        "LIGHT" -> false
                                        "DARK" -> true
                                        else -> isSystemInDarkTheme()
                                    }
                                    AppTheme.values().forEach { theme ->
                                        val isSelected = currentAppTheme == theme
                                        val schemeColors = if (isDarkTheme) {
                                            com.example.ui.theme.DarkSchemes[theme] ?: MaterialTheme.colorScheme
                                        } else {
                                            com.example.ui.theme.LightSchemes[theme] ?: MaterialTheme.colorScheme
                                        }
                                        
                                        Row(
                                            modifier = Modifier
                                                .fillMaxWidth()
                                                .clip(RoundedCornerShape(10.dp))
                                                .background(
                                                    if (isSelected) MaterialTheme.colorScheme.primary.copy(alpha = 0.08f)
                                                    else MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.15f)
                                                )
                                                .border(
                                                    width = if (isSelected) 1.5.dp else 1.dp,
                                                    color = if (isSelected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.outline.copy(alpha = 0.15f),
                                                    shape = RoundedCornerShape(10.dp)
                                                )
                                                .clickable {
                                                    haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                                                    viewModel.setAppTheme(theme)
                                                    Toast.makeText(context, "${theme.displayName()} theme applied", Toast.LENGTH_SHORT).show()
                                                }
                                                .padding(horizontal = 12.dp, vertical = 10.dp),
                                            verticalAlignment = Alignment.CenterVertically,
                                            horizontalArrangement = Arrangement.SpaceBetween
                                        ) {
                                            Column(modifier = Modifier.weight(1f)) {
                                                Text(
                                                    text = theme.displayName(),
                                                    fontWeight = FontWeight.Bold,
                                                    style = MaterialTheme.typography.bodyMedium,
                                                    color = MaterialTheme.colorScheme.onSurface
                                                )
                                                Spacer(modifier = Modifier.height(6.dp))
                                                Row(
                                                    horizontalArrangement = Arrangement.spacedBy(16.dp)
                                                ) {
                                                    // Primary Swatch
                                                    Row(
                                                        verticalAlignment = Alignment.CenterVertically,
                                                        horizontalArrangement = Arrangement.spacedBy(4.dp)
                                                    ) {
                                                        Box(
                                                            modifier = Modifier
                                                                .size(14.dp)
                                                                .clip(RoundedCornerShape(4.dp))
                                                                .background(schemeColors.primary)
                                                        )
                                                        Text("Primary", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                                                    }
                                                    
                                                    // Secondary Swatch
                                                    Row(
                                                        verticalAlignment = Alignment.CenterVertically,
                                                        horizontalArrangement = Arrangement.spacedBy(4.dp)
                                                    ) {
                                                        Box(
                                                            modifier = Modifier
                                                                .size(14.dp)
                                                                .clip(RoundedCornerShape(4.dp))
                                                                .background(schemeColors.secondary)
                                                        )
                                                        Text("Secondary", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                                                    }

                                                    // Surface Swatch
                                                    Row(
                                                        verticalAlignment = Alignment.CenterVertically,
                                                        horizontalArrangement = Arrangement.spacedBy(4.dp)
                                                    ) {
                                                        Box(
                                                            modifier = Modifier
                                                                .size(14.dp)
                                                                .clip(RoundedCornerShape(4.dp))
                                                                .border(0.5.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.2f), RoundedCornerShape(4.dp))
                                                                .background(schemeColors.surface)
                                                        )
                                                        Text("Surface", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                                                    }
                                                }
                                            }
                                            
                                            if (isSelected) {
                                                Icon(
                                                    imageVector = Icons.Default.CheckCircle,
                                                    contentDescription = "Selected",
                                                    tint = MaterialTheme.colorScheme.primary,
                                                    modifier = Modifier.size(20.dp)
                                                )
                                            } else {
                                                Box(
                                                    modifier = Modifier
                                                        .size(20.dp)
                                                        .border(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.4f), CircleShape)
                                                )
                                            }
                                        }
                                    }
                                }
                            }

                            HorizontalDivider(color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.08f))

                            // Palette picker (moved to Settings!)
                            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Icon(imageVector = Icons.Default.Palette, contentDescription = "Palette", tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(20.dp))
                                    Spacer(modifier = Modifier.width(8.dp))
                                    Text("Global Color Palette", fontWeight = FontWeight.Bold, style = MaterialTheme.typography.bodyLarge)
                                }
                                Column(
                                    modifier = Modifier.fillMaxWidth(),
                                    verticalArrangement = Arrangement.spacedBy(8.dp)
                                ) {
                                    PaletteTheme.values().forEach { palette ->
                                        val isSelected = currentTheme == palette
                                        val paletteColors = viewModel.palettes[palette] ?: emptyList()
                                        Row(
                                            modifier = Modifier
                                                .fillMaxWidth()
                                                .clip(RoundedCornerShape(10.dp))
                                                .background(
                                                    if (isSelected) MaterialTheme.colorScheme.primary.copy(alpha = 0.08f)
                                                    else MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.15f)
                                                )
                                                .border(
                                                    width = if (isSelected) 1.5.dp else 1.dp,
                                                    color = if (isSelected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.outline.copy(alpha = 0.15f),
                                                    shape = RoundedCornerShape(10.dp)
                                                )
                                                .clickable {
                                                    haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                                                    if (palette != currentTheme) {
                                                        pendingPaletteTheme = palette
                                                        showConfirmationDialog = true
                                                    } else {
                                                        Toast.makeText(context, "${palette.name} theme is already active", Toast.LENGTH_SHORT).show()
                                                    }
                                                }
                                                .padding(horizontal = 12.dp, vertical = 10.dp),
                                            verticalAlignment = Alignment.CenterVertically,
                                            horizontalArrangement = Arrangement.SpaceBetween
                                        ) {
                                            Column(modifier = Modifier.weight(1f)) {
                                                Text(
                                                    text = palette.name.lowercase().replaceFirstChar { it.uppercase() },
                                                    fontWeight = FontWeight.Bold,
                                                    style = MaterialTheme.typography.bodyMedium,
                                                    color = MaterialTheme.colorScheme.onSurface
                                                )
                                                Spacer(modifier = Modifier.height(6.dp))
                                                Row(
                                                    horizontalArrangement = Arrangement.spacedBy(4.dp)
                                                ) {
                                                    paletteColors.take(6).forEach { colorString ->
                                                        Box(
                                                            modifier = Modifier
                                                                .size(16.dp)
                                                                .clip(RoundedCornerShape(4.dp))
                                                                .background(parseHexColor(colorString))
                                                        )
                                                    }
                                                }
                                            }
                                            
                                            if (isSelected) {
                                                Icon(
                                                    imageVector = Icons.Default.CheckCircle,
                                                    contentDescription = "Selected",
                                                    tint = MaterialTheme.colorScheme.primary,
                                                    modifier = Modifier.size(20.dp)
                                                )
                                            } else {
                                                Box(
                                                    modifier = Modifier
                                                        .size(20.dp)
                                                        .border(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.4f), CircleShape)
                                                )
                                            }
                                        }
                                    }
                                }
                            }

                            HorizontalDivider(color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.08f))

                            // Dynamic color toggle (switch)
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Row(modifier = Modifier.weight(1f), verticalAlignment = Alignment.CenterVertically) {
                                    Icon(imageVector = Icons.Default.ColorLens, contentDescription = "Dynamic Color", tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(20.dp))
                                    Spacer(modifier = Modifier.width(8.dp))
                                    Column {
                                        Text("Dynamic Wallpaper Color", fontWeight = FontWeight.Bold, style = MaterialTheme.typography.bodyLarge)
                                        Text("Tint UI matching system wallpaper on Android 12+", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                                    }
                                }
                                Switch(
                                    checked = dynamicColorEnabled,
                                    onCheckedChange = {
                                        haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                                        viewModel.setDynamicColor(it)
                                    },
                                    modifier = Modifier.testTag("dynamic_color_switch")
                                )
                            }

                            HorizontalDivider(color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.08f))

                            // Go to Category Management
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clickable { navController.navigate("categories") }
                                    .padding(vertical = 8.dp),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Icon(imageVector = Icons.Default.Category, contentDescription = "Categories", tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(20.dp))
                                    Spacer(modifier = Modifier.width(8.dp))
                                    Column {
                                        Text("Manage Categories & Budgets", fontWeight = FontWeight.Bold, style = MaterialTheme.typography.bodyLarge)
                                        Text("Create, customize colors, and set spending thresholds", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                                    }
                                }
                                Icon(imageVector = Icons.Default.ChevronRight, contentDescription = "Explore", tint = MaterialTheme.colorScheme.primary)
                            }
                        }
                    }

                    // Section 2: Preferences
                    Text(
                        text = "Preferences",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.primary
                    )
                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(16.dp),
                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.2f))
                    ) {
                        Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(16.dp)) {
                            // Currency selection
                            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Icon(imageVector = Icons.Default.AttachMoney, contentDescription = "Currency", tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(20.dp))
                                    Spacer(modifier = Modifier.width(8.dp))
                                    Text("Display Currency Symbol", fontWeight = FontWeight.Bold, style = MaterialTheme.typography.bodyLarge)
                                }
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                                ) {
                                    listOf("₹" to "Rupee", "$" to "Dollar", "€" to "Euro", "£" to "Pound", "¥" to "Yen").forEach { (sym, name) ->
                                        val isSelected = currencySymbol == sym
                                        Box(
                                            modifier = Modifier
                                                .weight(1f)
                                                .clip(RoundedCornerShape(10.dp))
                                                .background(
                                                    if (isSelected) MaterialTheme.colorScheme.primary
                                                    else MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.3f)
                                                )
                                                .border(
                                                    width = 1.dp,
                                                    color = if (isSelected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.outline.copy(alpha = 0.3f),
                                                    shape = RoundedCornerShape(10.dp)
                                                )
                                                .clickable {
                                                    haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                                                    viewModel.setCurrency(sym)
                                                    Toast.makeText(context, "Currency set to $name ($sym)", Toast.LENGTH_SHORT).show()
                                                }
                                                .padding(vertical = 10.dp),
                                            contentAlignment = Alignment.Center
                                        ) {
                                            Text(
                                                text = "$sym\n$name",
                                                fontWeight = FontWeight.Bold,
                                                style = MaterialTheme.typography.bodySmall,
                                                textAlign = TextAlign.Center,
                                                color = if (isSelected) MaterialTheme.colorScheme.onPrimary else MaterialTheme.colorScheme.onSurface
                                            )
                                        }
                                    }
                                }
                            }

                            HorizontalDivider(color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.08f))

                            // Default transaction type
                            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Icon(imageVector = Icons.Default.SwapHoriz, contentDescription = "Type", tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(20.dp))
                                    Spacer(modifier = Modifier.width(8.dp))
                                    Text("Default Transaction Type", fontWeight = FontWeight.Bold, style = MaterialTheme.typography.bodyLarge)
                                }
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                                ) {
                                    listOf("EXPENSE" to "Expense", "INCOME" to "Income").forEach { (tValue, label) ->
                                        val isSelected = defaultType == tValue
                                        Box(
                                            modifier = Modifier
                                                .weight(1f)
                                                .clip(RoundedCornerShape(10.dp))
                                                .background(
                                                    if (isSelected) MaterialTheme.colorScheme.primary
                                                    else MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.3f)
                                                )
                                                .border(
                                                    width = 1.dp,
                                                    color = if (isSelected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.outline.copy(alpha = 0.3f),
                                                    shape = RoundedCornerShape(10.dp)
                                                )
                                                .clickable {
                                                    haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                                                    viewModel.setDefaultTransactionType(tValue)
                                                    Toast.makeText(context, "$label default set", Toast.LENGTH_SHORT).show()
                                                }
                                                .padding(vertical = 12.dp),
                                            contentAlignment = Alignment.Center
                                        ) {
                                            Text(
                                                text = label,
                                                fontWeight = FontWeight.Bold,
                                                style = MaterialTheme.typography.bodyMedium,
                                                color = if (isSelected) MaterialTheme.colorScheme.onPrimary else MaterialTheme.colorScheme.onSurface
                                            )
                                        }
                                    }
                                }
                            }
                        }
                    }

                    // Section: Tour & Guided Help
                    Text(
                        text = "App Tour & Help",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.primary
                    )
                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(16.dp),
                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.2f))
                    ) {
                        Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(16.dp)) {
                            // Row 1: Start Help & Tour
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clickable { navController.navigate("onboarding_tour") }
                                    .testTag("onboarding_tour_row")
                                    .padding(vertical = 4.dp),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.weight(1f)) {
                                    Icon(imageVector = Icons.Default.Info, contentDescription = "Help & Tour", tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(20.dp))
                                    Spacer(modifier = Modifier.width(8.dp))
                                    Column {
                                        Text("Help & Onboarding Tour", fontWeight = FontWeight.Bold, style = MaterialTheme.typography.bodyLarge)
                                        Text("Re-run the setup guide tour anytime to review features", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                                    }
                                }
                                Icon(imageVector = Icons.Default.ChevronRight, contentDescription = "Go", tint = MaterialTheme.colorScheme.primary)
                            }

                            HorizontalDivider(color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.08f))

                            // Row 2: Reset Onboarding State
                            Row(
                                modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Row(modifier = Modifier.weight(1f), verticalAlignment = Alignment.CenterVertically) {
                                    Icon(imageVector = Icons.Default.Refresh, contentDescription = "Reset Onboarding", tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(20.dp))
                                    Spacer(modifier = Modifier.width(8.dp))
                                    Column {
                                        Text("Onboarding Completed State", fontWeight = FontWeight.Bold, style = MaterialTheme.typography.bodyLarge)
                                        Text("Flipping this off shows the welcome wizard on next open", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                                    }
                                }
                                Switch(
                                    checked = onboardingComplete,
                                    onCheckedChange = { isComplete ->
                                        haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                                        viewModel.setOnboardingComplete(isComplete)
                                        val status = if (isComplete) "Onboarding completed" else "Onboarding reset! Welcome wizard will show again."
                                        Toast.makeText(context, status, Toast.LENGTH_SHORT).show()
                                    },
                                    modifier = Modifier.testTag("onboarding_reset_switch")
                                )
                            }
                        }
                    }

                    // Section 3: Data Actions
                    Text(
                        text = "Data Operations",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.primary
                    )
                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(16.dp),
                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.2f))
                    ) {
                        Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(14.dp)) {
                            Button(
                                onClick = { exportLauncher.launch("shylock_backup.json") },
                                modifier = Modifier.fillMaxWidth().testTag("export_backup_btn"),
                                colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.secondary),
                                shape = RoundedCornerShape(12.dp)
                            ) {
                                Icon(imageVector = Icons.Default.Share, contentDescription = "Export")
                                Spacer(modifier = Modifier.width(8.dp))
                                Text("Export Backup (JSON)", fontWeight = FontWeight.Bold)
                            }

                            Button(
                                onClick = { importLauncher.launch(arrayOf("application/json")) },
                                modifier = Modifier.fillMaxWidth().testTag("import_backup_btn"),
                                colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.secondary),
                                shape = RoundedCornerShape(12.dp)
                            ) {
                                Icon(imageVector = Icons.Default.CloudDownload, contentDescription = "Import")
                                Spacer(modifier = Modifier.width(8.dp))
                                Text("Import Backup (JSON)", fontWeight = FontWeight.Bold)
                            }

                            Button(
                                onClick = {
                                    haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                                    viewModel.seedDemoData()
                                    Toast.makeText(context, "Sandbox sandbox data loaded", Toast.LENGTH_SHORT).show()
                                },
                                modifier = Modifier.fillMaxWidth().testTag("reload_sandbox_btn"),
                                colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary),
                                shape = RoundedCornerShape(12.dp)
                            ) {
                                Icon(imageVector = Icons.Default.AutoMode, contentDescription = "Sandbox")
                                Spacer(modifier = Modifier.width(8.dp))
                                Text("Reload Sample Sandbox Data", fontWeight = FontWeight.Bold)
                            }

                            Button(
                                onClick = {
                                    haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                                    showClearAllDialog = true
                                },
                                modifier = Modifier.fillMaxWidth().testTag("clear_everything_btn"),
                                colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.error),
                                shape = RoundedCornerShape(12.dp)
                            ) {
                                Icon(imageVector = Icons.Default.DeleteForever, contentDescription = "Wipe")
                                Spacer(modifier = Modifier.width(8.dp))
                                Text("Clear All Data", fontWeight = FontWeight.Bold)
                            }
                        }
                    }

                    // Section 4: About
                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(16.dp),
                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.15f))
                    ) {
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(18.dp),
                            horizontalAlignment = Alignment.CenterHorizontally,
                            verticalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Default.AccountBalanceWallet,
                                contentDescription = "Logo",
                                tint = MaterialTheme.colorScheme.primary,
                                modifier = Modifier.size(32.dp)
                            )
                            Text(
                                text = "Shylock Finance",
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.ExtraBold,
                                color = MaterialTheme.colorScheme.primary
                            )
                            Text(
                                text = "Your color-guided personal finance companion.",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                textAlign = TextAlign.Center
                            )
                            Spacer(modifier = Modifier.height(4.dp))
                            Text(
                                text = "Version 1.2 • Open Source sandbox tracker",
                                style = MaterialTheme.typography.labelMedium,
                                color = MaterialTheme.colorScheme.outline
                            )
                        }
                    }
                }
            }

            // === Destination 4: Manage Categories Screen ===
            composable("categories") {
                var selectedTypeTab by remember { mutableStateOf("EXPENSE") }
                val expenseCount = categories.count { it.type == "EXPENSE" }
                val incomeCount = categories.count { it.type == "INCOME" }
                val isExpenseTab = selectedTypeTab == "EXPENSE"
                val currentTypeBudget = categories.filter { it.type == selectedTypeTab }.sumOf { it.budgetLimit }
                val currentTypeCount = if (isExpenseTab) expenseCount else incomeCount
                
                val categoryOrder = remember(categories) {
                    mutableStateListOf<Int>().apply {
                        addAll(categories.map { it.id })
                    }
                }
                
                val sortedCategories = remember(categories, categoryOrder) {
                    val ordered = categoryOrder.mapNotNull { id -> categories.find { it.id == id } }
                    val remaining = categories.filter { it.id !in categoryOrder }
                    ordered + remaining
                }

                val typedCategories = remember(sortedCategories, selectedTypeTab) {
                    sortedCategories.filter { it.type.equals(selectedTypeTab, ignoreCase = true) }
                }

                var searchQuery by remember { mutableStateOf("") }
                val filteredCategories = remember(typedCategories, searchQuery) {
                    if (searchQuery.isBlank()) {
                        typedCategories
                    } else {
                        typedCategories.filter { it.name.contains(searchQuery, ignoreCase = true) }
                    }
                }

                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .verticalScroll(rememberScrollState())
                        .padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(16.dp)
                ) {
                    // Header / Back navigation row
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        IconButton(
                            onClick = { navController.popBackStack() },
                            modifier = Modifier.size(48.dp)
                        ) {
                            Icon(imageVector = Icons.Default.ArrowBack, contentDescription = "Go back")
                        }
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = "Manage Categories",
                            style = MaterialTheme.typography.titleLarge,
                            fontWeight = FontWeight.ExtraBold,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                    }

                    // Expense vs Income Type Toggle Tabs
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(14.dp))
                            .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.4f))
                            .padding(4.dp),
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        val tabs = listOf(
                            "EXPENSE" to "Expense Categories ($expenseCount)",
                            "INCOME" to "Income Categories ($incomeCount)"
                        )
                        tabs.forEach { (typeVal, label) ->
                            val isSelected = selectedTypeTab == typeVal
                            val activeColor = if (typeVal == "INCOME") Color(0xFF2E7D32) else MaterialTheme.colorScheme.primary
                            Box(
                                modifier = Modifier
                                    .weight(1f)
                                    .clip(RoundedCornerShape(10.dp))
                                    .background(if (isSelected) activeColor.copy(alpha = 0.15f) else Color.Transparent)
                                    .border(
                                        width = if (isSelected) 1.5.dp else 0.dp,
                                        color = if (isSelected) activeColor else Color.Transparent,
                                        shape = RoundedCornerShape(10.dp)
                                    )
                                    .clickable { selectedTypeTab = typeVal }
                                    .padding(vertical = 10.dp),
                                contentAlignment = Alignment.Center
                            ) {
                                Text(
                                    text = label,
                                    fontSize = 12.sp,
                                    fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium,
                                    color = if (isSelected) activeColor else MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                        }
                    }

                    // Step 5.1: Beautiful summary header card
                    Card(
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("categories_summary_card"),
                        shape = RoundedCornerShape(16.dp),
                        colors = CardDefaults.cardColors(
                            containerColor = if (isExpenseTab) {
                                MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.4f)
                            } else {
                                Color(0xFF2E7D32).copy(alpha = 0.12f)
                            }
                        ),
                        border = BorderStroke(
                            1.dp,
                            if (isExpenseTab) MaterialTheme.colorScheme.primary.copy(alpha = 0.3f) else Color(0xFF2E7D32).copy(alpha = 0.3f)
                        )
                    ) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(16.dp),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Column {
                                Text(
                                    text = if (isExpenseTab) "Monthly Expense Budget" else "Monthly Income Target",
                                    style = MaterialTheme.typography.labelMedium,
                                    fontWeight = FontWeight.Bold,
                                    color = if (isExpenseTab) MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.7f) else Color(0xFF2E7D32)
                                )
                                Spacer(modifier = Modifier.height(2.dp))
                                Text(
                                    text = formatInRupee(currentTypeBudget, currencySymbol),
                                    style = MaterialTheme.typography.headlineMedium,
                                    fontWeight = FontWeight.Black,
                                    color = if (isExpenseTab) MaterialTheme.colorScheme.onPrimaryContainer else Color(0xFF1B5E20)
                                )
                            }
                            
                            Card(
                                colors = CardDefaults.cardColors(
                                    containerColor = if (isExpenseTab) MaterialTheme.colorScheme.primary else Color(0xFF2E7D32)
                                ),
                                shape = RoundedCornerShape(10.dp)
                            ) {
                                Column(
                                    modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp),
                                    horizontalAlignment = Alignment.CenterHorizontally
                                ) {
                                    Text(
                                        text = currentTypeCount.toString(),
                                        style = MaterialTheme.typography.titleLarge,
                                        fontWeight = FontWeight.Black,
                                        color = Color.White
                                    )
                                    Text(
                                        text = if (isExpenseTab) "Expenses" else "Incomes",
                                        style = MaterialTheme.typography.labelSmall,
                                        fontWeight = FontWeight.Bold,
                                        color = Color.White.copy(alpha = 0.85f)
                                    )
                                }
                            }
                        }
                    }

                    Text(
                        text = "Customize colors, change budgets, or modify subcategories. Values are color-mapped instantly across transaction lists and charts.",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )

                    Button(
                        onClick = {
                            addCategoryDefaultType = selectedTypeTab
                            showAddCategoryDialog = true
                        },
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(50.dp)
                            .testTag("categories_screen_add_btn"),
                        shape = RoundedCornerShape(12.dp),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = if (isExpenseTab) MaterialTheme.colorScheme.primary else Color(0xFF2E7D32)
                        )
                    ) {
                        Icon(imageVector = Icons.Default.Add, contentDescription = "Add New Category")
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = if (isExpenseTab) "Add Expense Category" else "Add Income Category",
                            fontWeight = FontWeight.Bold
                        )
                    }

                    // Step 5.3: Dynamic search field displayed if more than 8 categories
                    if (categories.size > 8) {
                        OutlinedTextField(
                            value = searchQuery,
                            onValueChange = { searchQuery = it },
                            modifier = Modifier
                                .fillMaxWidth()
                                .testTag("categories_search_input"),
                            placeholder = { Text("Search categories...") },
                            leadingIcon = { Icon(imageVector = Icons.Default.Search, contentDescription = "Search icon") },
                            singleLine = true,
                            shape = RoundedCornerShape(12.dp),
                            trailingIcon = {
                                if (searchQuery.isNotEmpty()) {
                                    IconButton(onClick = { searchQuery = "" }) {
                                        Icon(imageVector = Icons.Default.Close, contentDescription = "Clear search")
                                    }
                                }
                            },
                            colors = OutlinedTextFieldDefaults.colors(
                                focusedBorderColor = MaterialTheme.colorScheme.primary,
                                unfocusedBorderColor = MaterialTheme.colorScheme.outline.copy(alpha = 0.3f)
                            )
                        )
                    }

                    // Grid / List header section
                    Row(
                        modifier = Modifier.fillMaxWidth().padding(top = 4.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = if (searchQuery.isNotEmpty()) "Search Results (${filteredCategories.size})" else "Active Categories (${filteredCategories.size})",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                        if (filteredCategories.size > 1 && searchQuery.isEmpty()) {
                            Text(
                                text = "Use arrows to reorder",
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.outline
                            )
                        }
                    }

                    if (filteredCategories.isEmpty()) {
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(200.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                text = if (searchQuery.isNotEmpty()) "No matching categories found." else "No categories. Click above to add some!",
                                style = MaterialTheme.typography.bodyMedium,
                                color = MaterialTheme.colorScheme.outline
                            )
                        }
                    } else {
                        Column(
                            verticalArrangement = Arrangement.spacedBy(12.dp)
                        ) {
                            filteredCategories.forEachIndexed { index, category ->
                                val statsInfo = stats[category.id]
                                val relatedSubs = subcategories.filter { it.parentCategoryId == category.id }
                                
                                UpgradedManageCategoryCard(
                                    category = category,
                                    stats = statsInfo,
                                    subcategories = relatedSubs,
                                    currencySymbol = currencySymbol,
                                    canMoveUp = index > 0 && searchQuery.isEmpty(),
                                    canMoveDown = index < filteredCategories.size - 1 && searchQuery.isEmpty(),
                                    onMoveUp = {
                                        val actualIndex = categoryOrder.indexOf(category.id)
                                        if (actualIndex > 0) {
                                            val prevId = categoryOrder[actualIndex - 1]
                                            categoryOrder[actualIndex - 1] = category.id
                                            categoryOrder[actualIndex] = prevId
                                        }
                                    },
                                    onMoveDown = {
                                        val actualIndex = categoryOrder.indexOf(category.id)
                                        if (actualIndex >= 0 && actualIndex < categoryOrder.size - 1) {
                                            val nextId = categoryOrder[actualIndex + 1]
                                            categoryOrder[actualIndex + 1] = category.id
                                            categoryOrder[actualIndex] = nextId
                                        }
                                    },
                                    onEdit = { editCategoryTarget = category },
                                    onDelete = { categoryToDelete = category }
                                )
                            }
                        }
                    }
                }
            }

            composable("onboarding") {
                OnboardingWizard(
                    viewModel = viewModel,
                    isTour = false,
                    onComplete = {
                        viewModel.setOnboardingComplete(true)
                        navController.navigate("home") {
                            popUpTo("onboarding") { inclusive = true }
                        }
                    }
                )
            }

            composable("onboarding_tour") {
                OnboardingWizard(
                    viewModel = viewModel,
                    isTour = true,
                    onComplete = {
                        navController.popBackStack()
                    }
                )
            }

            composable("lending") {
                LendingListScreen(
                    viewModel = viewModel,
                    navController = navController,
                    currencySymbol = currencySymbol,
                    onBack = { navController.navigateUp() }
                )
            }

            composable("lending_contact/{contactId}") { backStackEntry ->
                val contactId = backStackEntry.arguments?.getString("contactId")?.toIntOrNull() ?: 0
                ContactDetailScreen(
                    contactId = contactId,
                    viewModel = viewModel,
                    navController = navController,
                    currencySymbol = currencySymbol,
                    snackbarHostState = snackbarHostState,
                    onBack = { navController.navigateUp() }
                )
            }
        }
    }

    // Modal adding category dialog
    if (showAddCategoryDialog) {
        CategoryEditorDialog(
            viewModel = viewModel,
            initialType = addCategoryDefaultType,
            onDismiss = { showAddCategoryDialog = false },
            onSave = { name, colorHex, icon, budget, subsList, catType ->
                viewModel.addCategory(name, colorHex, icon, budget, subsList.map { it.name }, catType)
                showAddCategoryDialog = false
                Toast.makeText(context, "Category added successfully!", Toast.LENGTH_SHORT).show()
            }
        )
    }

    // Modal editing category dialog
    editCategoryTarget?.let { category ->
        val currentSubs = subcategories.filter { it.parentCategoryId == category.id }
        CategoryEditorDialog(
            viewModel = viewModel,
            category = category,
            initialType = category.type,
            subcategories = currentSubs,
            onDismiss = { editCategoryTarget = null },
            onSave = { name, colorHex, icon, budget, subsListWithColor, catType ->
                viewModel.editCategory(
                    id = category.id,
                    name = name,
                    colorHex = colorHex,
                    iconName = icon,
                    budget = budget,
                    subcategoriesList = subsListWithColor.map { it.name to it.colorHexOverride },
                    type = catType
                )
                editCategoryTarget = null
                Toast.makeText(context, "Category updated!", Toast.LENGTH_SHORT).show()
            }
        )
    }

    // Modal add/edit record
    if (showAddTransactionDialog || transactionToEdit != null) {
        AddTransactionDialog(
            categories = categories,
            subcategories = subcategories,
            transaction = transactionToEdit,
            defaultType = defaultType,
            transactions = transactions,
            defaultMonth = selectedMonth,
            onCreateCategoryFirstClick = {
                showAddTransactionDialog = false
                transactionToEdit = null
                navController.navigate("categories")
            },
            onDismiss = {
                showAddTransactionDialog = false
                transactionToEdit = null
            },
            onSave = { catId, subId, amount, desc, timestamp, type ->
                val editTx = transactionToEdit
                if (editTx == null) {
                    viewModel.addTransaction(catId, subId, amount, desc, timestamp, type)
                    Toast.makeText(context, "New transaction tracked!", Toast.LENGTH_SHORT).show()
                } else {
                    viewModel.editTransaction(editTx.id, catId, subId, amount, desc, timestamp, type)
                    Toast.makeText(context, "Transaction updated!", Toast.LENGTH_SHORT).show()
                }
                showAddTransactionDialog = false
                transactionToEdit = null
            }
        )
    }

    // Modal Month Picker BottomSheet
    if (showMonthPickerSheet) {
        val recordedMonths = remember(transactions, currentCalendarMonth) {
            val monthsFromTx = transactions.map {
                YearMonth.from(Instant.ofEpochMilli(it.timestamp).atZone(ZoneId.systemDefault()))
            }.toSet()
            (monthsFromTx + currentCalendarMonth).sortedDescending()
        }

        ModalBottomSheet(
            onDismissRequest = { showMonthPickerSheet = false },
            sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 20.dp)
                    .padding(bottom = 32.dp)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column {
                        Text(
                            text = "Select Month",
                            style = MaterialTheme.typography.titleLarge,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                        Text(
                            text = "Jump to any recorded month",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                    IconButton(onClick = { showMonthPickerSheet = false }) {
                        Icon(imageVector = Icons.Default.Close, contentDescription = "Close")
                    }
                }

                Spacer(modifier = Modifier.height(16.dp))

                LazyColumn(
                    modifier = Modifier.fillMaxWidth(),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    items(recordedMonths) { month ->
                        val isSelected = month == selectedMonth
                        val isCurrent = month == currentCalendarMonth
                        val monthSpend = remember(transactions, month) {
                            transactions
                                .filter { it.type == "EXPENSE" && YearMonth.from(Instant.ofEpochMilli(it.timestamp).atZone(ZoneId.systemDefault())) == month }
                                .sumOf { it.amount }
                        }
                        val monthFormatter = remember { DateTimeFormatter.ofPattern("MMMM yyyy", Locale.getDefault()) }

                        Card(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable {
                                    viewModel.selectMonth(month)
                                    showMonthPickerSheet = false
                                }
                                .testTag("month_item_${month}"),
                            shape = RoundedCornerShape(12.dp),
                            colors = CardDefaults.cardColors(
                                containerColor = if (isSelected)
                                    MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.5f)
                                else
                                    MaterialTheme.colorScheme.surfaceColorAtElevation(1.dp)
                            ),
                            border = if (isSelected) BorderStroke(1.5.dp, MaterialTheme.colorScheme.primary) else null
                        ) {
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(horizontal = 16.dp, vertical = 12.dp),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Column {
                                    Row(verticalAlignment = Alignment.CenterVertically) {
                                        Text(
                                            text = month.format(monthFormatter),
                                            style = MaterialTheme.typography.titleMedium,
                                            fontWeight = if (isSelected) FontWeight.Bold else FontWeight.SemiBold,
                                            color = if (isSelected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurface
                                        )
                                        if (isCurrent) {
                                            Spacer(modifier = Modifier.width(8.dp))
                                            Surface(
                                                shape = RoundedCornerShape(4.dp),
                                                color = MaterialTheme.colorScheme.secondaryContainer
                                            ) {
                                                Text(
                                                    text = "CURRENT",
                                                    style = MaterialTheme.typography.labelSmall,
                                                    fontWeight = FontWeight.Bold,
                                                    color = MaterialTheme.colorScheme.onSecondaryContainer,
                                                    modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                                                )
                                            }
                                        }
                                    }
                                    Spacer(modifier = Modifier.height(2.dp))
                                    Text(
                                        text = "${formatInRupee(monthSpend, currency = currencySymbol)} spent",
                                        style = MaterialTheme.typography.bodySmall,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                }

                                if (isSelected) {
                                    Icon(
                                        imageVector = Icons.Default.CheckCircle,
                                        contentDescription = "Selected",
                                        tint = MaterialTheme.colorScheme.primary,
                                        modifier = Modifier.size(24.dp)
                                    )
                                }
                            }
                        }
                    }
                }
            }
        }
    }

    // Modal category deletion confirmation dialog with cascading alerts
    categoryToDelete?.let { category ->
        val relatedSubsCount = subcategories.count { it.parentCategoryId == category.id }
        val relatedTxsCount = transactions.count { it.categoryId == category.id }
        val haptic = LocalHapticFeedback.current
        AlertDialog(
            onDismissRequest = { categoryToDelete = null },
            title = {
                Text(
                    text = "Delete Category?",
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.error
                )
            },
            text = {
                Text(
                    text = "Are you sure you want to delete Category '${category.name}'?\n\nThis will permanently delete:\n• $relatedSubsCount associated subcategories\n• $relatedTxsCount transaction records\n\nThis action cannot be undone."
                )
            },
            confirmButton = {
                Button(
                    onClick = {
                        haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                        viewModel.deleteCategory(category)
                        categoryToDelete = null
                        Toast.makeText(context, "Category deleted", Toast.LENGTH_SHORT).show()
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.error),
                    modifier = Modifier.testTag("confirm_delete_category_btn")
                ) {
                    Text("Delete Permanently", fontWeight = FontWeight.Bold)
                }
            },
            dismissButton = {
                TextButton(
                    onClick = { categoryToDelete = null },
                    modifier = Modifier.testTag("cancel_delete_category_btn")
                ) {
                    Text("Cancel")
                }
            }
        )
    }

    // Modal everything wipe type-to-confirm dialog
    if (showClearAllDialog) {
        AlertDialog(
            onDismissRequest = {
                showClearAllDialog = false
                resetConfirmationText = ""
            },
            title = {
                Text(
                    text = "Wipe All Data?",
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.error
                )
            },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    Text(
                        text = "This action is completely irreversible. It will delete all theme categories, subcategories, and transaction histories.\n\nPlease type \"RESET\" below to proceed."
                    )
                    OutlinedTextField(
                        value = resetConfirmationText,
                        onValueChange = { resetConfirmationText = it },
                        placeholder = { Text("RESET") },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth().testTag("reset_type_field")
                    )
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        viewModel.clearAllData()
                        showClearAllDialog = false
                        resetConfirmationText = ""
                        Toast.makeText(context, "All data wiped successfully", Toast.LENGTH_LONG).show()
                    },
                    enabled = resetConfirmationText == "RESET",
                    colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.error),
                    modifier = Modifier.testTag("confirm_reset_all_btn")
                ) {
                    Text("Wipe Everything", fontWeight = FontWeight.Bold)
                }
            },
            dismissButton = {
                TextButton(
                    onClick = {
                        showClearAllDialog = false
                        resetConfirmationText = ""
                    },
                    modifier = Modifier.testTag("cancel_reset_all_btn")
                ) {
                    Text("Cancel")
                }
            }
        )
    }
}

// Utility: parse hex accurately or fallback
fun parseHexColor(hex: String, fallback: Color = Color.Gray): Color {
    return try {
        Color(android.graphics.Color.parseColor(hex))
    } catch (e: Exception) {
        fallback
    }
}

// Utility: luminance contrast validation check for text overlay on custom backgrounds
fun getContrastColor(hexColor: String): Color {
    return try {
        val colorInt = android.graphics.Color.parseColor(hexColor)
        val r = android.graphics.Color.red(colorInt)
        val g = android.graphics.Color.green(colorInt)
        val b = android.graphics.Color.blue(colorInt)
        // Standard formula
        val luminance = 0.299 * r + 0.587 * g + 0.114 * b
        if (luminance > 165.0) Color.DarkGray else Color.White
    } catch (e: Exception) {
        Color.White
    }
}

// Same contrast rule as getContrastColor, for colors that are already resolved
fun getContrastColorFor(color: Color): Color =
    if (color.luminance() > 0.55f) Color(0xFF06222B) else Color.White

// Visual Component: Month Navigation Controls
// Visual Component: Branded home header (wordmark, tagline, time-aware greeting, avatar)
@Composable
fun ShylockBrandHeader(modifier: Modifier = Modifier) {
    val greeting = remember {
        when (LocalTime.now().hour) {
            in 5..11 -> "Good morning" to "🌞"
            in 12..16 -> "Good afternoon" to "☀️"
            in 17..21 -> "Good evening" to "👋"
            else -> "Good night" to "🌙"
        }
    }

    Row(
        modifier = modifier
            .fillMaxWidth()
            .padding(vertical = 6.dp)
            .testTag("brand_header"),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Column(modifier = Modifier.weight(1f)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(
                    text = "SHY",
                    fontSize = 26.sp,
                    fontWeight = FontWeight.Black,
                    letterSpacing = 3.sp,
                    color = MaterialTheme.colorScheme.onBackground
                )
                Text(
                    text = "LOCK",
                    fontSize = 26.sp,
                    fontWeight = FontWeight.Black,
                    letterSpacing = 3.sp,
                    color = MaterialTheme.colorScheme.primary
                )
            }
            Text(
                text = "Take control of your money",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }

        Row(verticalAlignment = Alignment.CenterVertically) {
            Column(horizontalAlignment = Alignment.End) {
                Text(
                    text = "${greeting.first} ${greeting.second}",
                    style = MaterialTheme.typography.labelMedium,
                    fontWeight = FontWeight.SemiBold,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Text(
                    text = "Let's make it count",
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.outline
                )
            }
            Spacer(modifier = Modifier.width(10.dp))
            Box(
                modifier = Modifier
                    .size(46.dp)
                    .liquidGlass(shape = CircleShape, strength = 0.9f),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = Icons.Default.Person,
                    contentDescription = "Profile",
                    tint = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.size(24.dp)
                )
            }
        }
    }
}

// Pill-shaped glass search input used in the transactions panel
@Composable
fun GlassSearchField(
    value: String,
    onValueChange: (String) -> Unit,
    modifier: Modifier = Modifier
) {
    OutlinedTextField(
        value = value,
        onValueChange = onValueChange,
        placeholder = {
            Text(
                "Search description or amount...",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.outline,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
        },
        leadingIcon = {
            Icon(
                imageVector = Icons.Default.Search,
                contentDescription = "Search icon",
                tint = MaterialTheme.colorScheme.primary
            )
        },
        modifier = modifier,
        singleLine = true,
        textStyle = MaterialTheme.typography.bodyMedium,
        trailingIcon = if (value.isNotEmpty()) {
            {
                IconButton(onClick = { onValueChange("") }) {
                    Icon(
                        imageVector = Icons.Default.Clear,
                        contentDescription = "Clear search",
                        tint = MaterialTheme.colorScheme.outline
                    )
                }
            }
        } else null,
        shape = CircleShape,
        colors = OutlinedTextFieldDefaults.colors(
            focusedContainerColor = MaterialTheme.colorScheme.surface.copy(alpha = 0.55f),
            unfocusedContainerColor = MaterialTheme.colorScheme.surface.copy(alpha = 0.35f),
            focusedBorderColor = MaterialTheme.colorScheme.primary.copy(alpha = 0.7f),
            unfocusedBorderColor = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.45f)
        )
    )
}

// Glowing filled pill when selected, quiet glass when not
@Composable
fun TypeFilterPill(
    label: String,
    selected: Boolean,
    accent: Color,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val shape = CircleShape
    val base = modifier
        .height(44.dp)
        .then(if (selected) Modifier.accentGlow(accent.copy(alpha = 0.9f), shape, 14.dp) else Modifier)

    Box(
        modifier = if (selected) {
            base
                .clip(shape)
                .background(
                    Brush.horizontalGradient(
                        listOf(accent.copy(alpha = 0.95f), accent)
                    )
                )
                .clickable { onClick() }
        } else {
            base
                .liquidGlass(shape = shape, strength = 0.6f)
                .clickable { onClick() }
        },
        contentAlignment = Alignment.Center
    ) {
        Text(
            text = label,
            fontSize = 13.sp,
            fontWeight = if (selected) FontWeight.Bold else FontWeight.Medium,
            color = if (selected) getContrastColorFor(accent) else MaterialTheme.colorScheme.onSurfaceVariant,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis
        )
    }
}

/**
 * Buckets the month's expenses into 9 equal slices of days for the header sparkline.
 * Returns an empty list when there is nothing to plot so the caller can skip the chart.
 */
fun buildSpendSparkline(monthTransactions: List<Transaction>, month: YearMonth, buckets: Int = 9): List<Double> {
    val expenses = monthTransactions.filter { it.type == "EXPENSE" }
    if (expenses.isEmpty()) return emptyList()

    val zone = ZoneId.systemDefault()
    val daysInMonth = month.lengthOfMonth()
    val totals = DoubleArray(buckets)
    expenses.forEach { tx ->
        val day = Instant.ofEpochMilli(tx.timestamp).atZone(zone).dayOfMonth
        val index = (((day - 1) * buckets) / daysInMonth).coerceIn(0, buckets - 1)
        totals[index] += tx.amount
    }
    return totals.toList()
}

@Composable
fun MonthNavigationBar(
    selectedMonth: YearMonth,
    isCurrentMonth: Boolean,
    onPrevMonth: () -> Unit,
    onNextMonth: () -> Unit,
    onLabelClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val monthFormatter = remember { DateTimeFormatter.ofPattern("MMMM yyyy", Locale.getDefault()) }

    Box(
        modifier = modifier
            .fillMaxWidth()
            .liquidGlass(shape = CircleShape, elevation = 4.dp)
            .testTag("month_navigation_bar")
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 6.dp, vertical = 6.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            IconButton(
                onClick = onPrevMonth,
                modifier = Modifier
                    .size(40.dp)
                    .testTag("prev_month_btn")
            ) {
                Icon(
                    imageVector = Icons.AutoMirrored.Filled.KeyboardArrowLeft,
                    contentDescription = "Previous Month",
                    tint = MaterialTheme.colorScheme.onSurface
                )
            }

            Row(
                modifier = Modifier
                    .clip(CircleShape)
                    .clickable { onLabelClick() }
                    .padding(horizontal = 14.dp, vertical = 8.dp)
                    .testTag("month_label_button"),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Icon(
                    imageVector = Icons.Default.CalendarMonth,
                    contentDescription = null,
                    modifier = Modifier.size(18.dp),
                    tint = MaterialTheme.colorScheme.primary
                )
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    text = selectedMonth.format(monthFormatter),
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onSurface
                )
                Spacer(modifier = Modifier.width(4.dp))
                Icon(
                    imageVector = Icons.Default.ArrowDropDown,
                    contentDescription = "Open month selector",
                    modifier = Modifier.size(20.dp),
                    tint = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }

            IconButton(
                onClick = onNextMonth,
                enabled = !isCurrentMonth,
                modifier = Modifier
                    .size(40.dp)
                    .testTag("next_month_btn")
            ) {
                Icon(
                    imageVector = Icons.AutoMirrored.Filled.KeyboardArrowRight,
                    contentDescription = "Next Month",
                    tint = if (!isCurrentMonth) MaterialTheme.colorScheme.onSurface else MaterialTheme.colorScheme.onSurface.copy(alpha = 0.25f)
                )
            }
        }
    }
}

// Visual Component: Read-Only Past Month Banner
@Composable
fun PastMonthBanner(
    selectedMonth: YearMonth,
    isUnlocked: Boolean,
    onUnlock: () -> Unit,
    onLock: () -> Unit,
    modifier: Modifier = Modifier
) {
    val monthFormatter = remember { DateTimeFormatter.ofPattern("MMMM yyyy", Locale.getDefault()) }
    Box(
        modifier = modifier
            .fillMaxWidth()
            .liquidGlass(
                shape = RoundedCornerShape(18.dp),
                tint = if (isUnlocked) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.outline,
                strength = if (isUnlocked) 1f else 0.8f
            )
            .testTag("past_month_banner")
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 14.dp, vertical = 10.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.weight(1f)
            ) {
                Icon(
                    imageVector = if (isUnlocked) Icons.Default.Edit else Icons.Default.Lock,
                    contentDescription = null,
                    modifier = Modifier.size(18.dp),
                    tint = if (isUnlocked) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant
                )
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    text = if (isUnlocked)
                        "Editing ${selectedMonth.format(monthFormatter)} (unlocked)"
                    else
                        "Viewing ${selectedMonth.format(monthFormatter)} (read-only)",
                    style = MaterialTheme.typography.bodyMedium,
                    fontWeight = FontWeight.Medium,
                    color = MaterialTheme.colorScheme.onSurface
                )
            }
            if (!isUnlocked) {
                Button(
                    onClick = onUnlock,
                    contentPadding = PaddingValues(horizontal = 12.dp, vertical = 4.dp),
                    shape = RoundedCornerShape(8.dp),
                    modifier = Modifier
                        .height(34.dp)
                        .testTag("unlock_past_month_edit_btn")
                ) {
                    Icon(
                        imageVector = Icons.Default.Edit,
                        contentDescription = null,
                        modifier = Modifier.size(14.dp)
                    )
                    Spacer(modifier = Modifier.width(4.dp))
                    Text("Edit", style = MaterialTheme.typography.labelMedium, fontWeight = FontWeight.Bold)
                }
            } else {
                OutlinedButton(
                    onClick = onLock,
                    contentPadding = PaddingValues(horizontal = 12.dp, vertical = 4.dp),
                    shape = RoundedCornerShape(8.dp),
                    modifier = Modifier
                        .height(34.dp)
                        .testTag("lock_past_month_btn")
                ) {
                    Icon(
                        imageVector = Icons.Default.Lock,
                        contentDescription = null,
                        modifier = Modifier.size(14.dp)
                    )
                    Spacer(modifier = Modifier.width(4.dp))
                    Text("Lock", style = MaterialTheme.typography.labelMedium, fontWeight = FontWeight.Bold)
                }
            }
        }
    }
}

// Visual Component: Header
@Composable
fun DashboardHeader(
    title: String = "This Month's Summary",
    totalMonthSpend: Double,
    totalMonthIncome: Double,
    categoriesCount: Int,
    lastMonthSpend: Double,
    currencySymbol: String = "₹",
    incomeCount: Int = 0,
    expenseCount: Int = 0,
    sparkline: List<Double> = emptyList()
) {
    val netBalance = totalMonthIncome - totalMonthSpend
    val percentageChange = if (lastMonthSpend > 0.0) {
        ((totalMonthSpend - lastMonthSpend) / lastMonthSpend) * 100
    } else null

    val isSpendingUp = (percentageChange ?: 0.0) > 0.0
    val formattedPercent = if (percentageChange != null) {
        String.format(Locale.US, "%.1f", Math.abs(percentageChange))
    } else null

    GlassCard(
        modifier = Modifier
            .fillMaxWidth()
            .testTag("dashboard_header_card"),
        shape = RoundedCornerShape(28.dp),
        elevation = 8.dp,
        contentPadding = PaddingValues(20.dp)
    ) {
        // Top row: Title and Categories pill
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = title,
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.ExtraBold,
                fontSize = 20.sp,
                color = MaterialTheme.colorScheme.onSurface
            )
            Box(
                modifier = Modifier.liquidGlass(shape = CircleShape, strength = 0.75f)
            ) {
                Text(
                    text = "$categoriesCount Categories",
                    fontWeight = FontWeight.Bold,
                    style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.onSurface,
                    modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp)
                )
            }
        }

        Spacer(modifier = Modifier.height(16.dp))

        // Balance block on the left, spend rhythm sparkline on the right
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = "Net Balance",
                    style = MaterialTheme.typography.labelLarge,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    fontWeight = FontWeight.Medium
                )
                Spacer(modifier = Modifier.height(2.dp))
                Text(
                    text = formatInRupee(netBalance, currency = currencySymbol),
                    style = MaterialTheme.typography.headlineLarge,
                    fontSize = 34.sp,
                    fontWeight = FontWeight.Black,
                    color = if (netBalance >= 0.0) MaterialTheme.colorScheme.primary else ExpenseRed,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )

                if (formattedPercent != null) {
                    Spacer(modifier = Modifier.height(10.dp))
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Surface(
                            shape = CircleShape,
                            color = if (isSpendingUp) ExpenseRedContainer else IncomeGreenContainer
                        ) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                modifier = Modifier.padding(horizontal = 9.dp, vertical = 5.dp)
                            ) {
                                Icon(
                                    imageVector = if (isSpendingUp) Icons.Default.ArrowUpward else Icons.Default.ArrowDownward,
                                    contentDescription = null,
                                    tint = if (isSpendingUp) ExpenseRed else IncomeGreen,
                                    modifier = Modifier.size(13.dp)
                                )
                                Spacer(modifier = Modifier.width(3.dp))
                                Text(
                                    text = "$formattedPercent%",
                                    style = MaterialTheme.typography.labelMedium,
                                    fontWeight = FontWeight.Bold,
                                    color = if (isSpendingUp) ExpenseRed else IncomeGreen
                                )
                            }
                        }
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = "vs last month",
                            style = MaterialTheme.typography.labelMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            }

            if (sparkline.isNotEmpty()) {
                Column(horizontalAlignment = Alignment.End) {
                    SpendRhythmBars(
                        values = sparkline,
                        modifier = Modifier.size(width = 108.dp, height = 52.dp)
                    )
                    Spacer(modifier = Modifier.height(6.dp))
                    Text(
                        text = "Smaller steps,\nbigger freedom",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        textAlign = TextAlign.End,
                        lineHeight = 13.sp
                    )
                }
            }
        }

        Spacer(modifier = Modifier.height(18.dp))

        // Two nested glass tiles: Income & Expenses
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            SummaryTile(
                modifier = Modifier.weight(1f),
                label = "Income",
                amount = formatInRupee(totalMonthIncome, currency = currencySymbol),
                count = incomeCount,
                accent = IncomeGreen,
                accentContainer = IncomeGreenContainer,
                icon = Icons.Default.TrendingUp
            )
            SummaryTile(
                modifier = Modifier.weight(1f),
                label = "Expenses",
                amount = formatInRupee(totalMonthSpend, currency = currencySymbol),
                count = expenseCount,
                accent = ExpenseRed,
                accentContainer = ExpenseRedContainer,
                icon = Icons.Default.ArrowDownward
            )
        }

        // The MoM pill above already carries the comparison when prior data exists, so this
        // line only appears when there is nothing to compare against.
        if (formattedPercent == null) {
            Spacer(modifier = Modifier.height(14.dp))
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.Center
            ) {
                Icon(
                    imageVector = Icons.Default.History,
                    contentDescription = "Comparison",
                    tint = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.size(14.dp)
                )
                Spacer(modifier = Modifier.width(6.dp))
                Text(
                    text = "No spending in prior month to compare",
                    style = MaterialTheme.typography.labelSmall,
                    fontWeight = FontWeight.Medium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
    }
}

// Income / Expense tile inside the dashboard header
@Composable
private fun SummaryTile(
    label: String,
    amount: String,
    count: Int,
    accent: Color,
    accentContainer: Color,
    icon: ImageVector,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier
            .liquidGlass(shape = RoundedCornerShape(20.dp), tint = accent, strength = 0.75f)
            .padding(14.dp)
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Box(
                modifier = Modifier
                    .size(34.dp)
                    .clip(CircleShape)
                    .background(accentContainer),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = icon,
                    contentDescription = label,
                    tint = accent,
                    modifier = Modifier.size(18.dp)
                )
            }
            Spacer(modifier = Modifier.width(10.dp))
            Column {
                Text(
                    text = label,
                    style = MaterialTheme.typography.labelMedium,
                    fontWeight = FontWeight.SemiBold,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Spacer(modifier = Modifier.height(2.dp))
                Text(
                    text = amount,
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.ExtraBold,
                    color = accent,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }
        }
        Spacer(modifier = Modifier.height(6.dp))
        Text(
            text = "$count ${if (count == 1) "transaction" else "transactions"}",
            style = MaterialTheme.typography.labelSmall,
            color = MaterialTheme.colorScheme.outline
        )
    }
}

// Compact bar chart showing how spending was paced across the month
@Composable
private fun SpendRhythmBars(
    values: List<Double>,
    modifier: Modifier = Modifier
) {
    val accent = MaterialTheme.colorScheme.primary
    val peak = values.maxOrNull() ?: 0.0
    val peakIndex = values.indexOfFirst { it == peak }

    Canvas(modifier = modifier) {
        if (values.isEmpty()) return@Canvas
        val gap = size.width * 0.028f
        val barWidth = (size.width - gap * (values.size - 1)) / values.size
        val radius = androidx.compose.ui.geometry.CornerRadius(barWidth / 2f, barWidth / 2f)
        val minHeight = barWidth.coerceAtMost(size.height)

        values.forEachIndexed { index, value ->
            val ratio = if (peak > 0.0) (value / peak).toFloat() else 0f
            val barHeight = (size.height * ratio).coerceAtLeast(minHeight)
            val left = index * (barWidth + gap)
            drawRoundRect(
                brush = Brush.verticalGradient(
                    colors = if (index == peakIndex) {
                        listOf(accent.copy(alpha = 0.95f), accent.copy(alpha = 0.45f))
                    } else {
                        listOf(accent.copy(alpha = 0.42f), accent.copy(alpha = 0.14f))
                    },
                    startY = size.height - barHeight,
                    endY = size.height
                ),
                topLeft = Offset(left, size.height - barHeight),
                size = Size(barWidth, barHeight),
                cornerRadius = radius
            )
        }
    }
}

// Visual Component: Donut Chart with Custom Canvas Drawing
@Composable
fun DonutChartComponent(
    categories: List<Category>,
    stats: Map<Int, CategoryStats>,
    totalSpend: Double,
    selectedCatId: Int?,
    currencySymbol: String = "₹",
    onSliceClick: (Int) -> Unit
) {
    if (totalSpend == 0.0 || categories.isEmpty()) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .height(180.dp),
            verticalArrangement = Arrangement.Center,
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Icon(
                imageVector = Icons.Default.PieChart,
                contentDescription = "Empty chart",
                tint = MaterialTheme.colorScheme.outline.copy(alpha = 0.3f),
                modifier = Modifier.size(60.dp)
            )
            Spacer(modifier = Modifier.height(10.dp))
            Text(
                text = "Tracking category spending creates beautiful visualizations here!",
                fontSize = 12.sp,
                color = MaterialTheme.colorScheme.outline,
                textAlign = TextAlign.Center,
                modifier = Modifier.padding(horizontal = 24.dp)
            )
        }
    } else {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(190.dp),
            contentAlignment = Alignment.Center
        ) {
            Canvas(
                modifier = Modifier
                    .size(150.dp)
                    .clickable(
                        interactionSource = remember { MutableInteractionSource() },
                        indication = null
                    ) {
                        // simple visual clicking toggler
                    }
            ) {
                var currentAngle = -90f
                val strokeWidth = 32.dp.toPx()

                categories.forEach { cat ->
                    val catSpend = stats[cat.id]?.totalAmount ?: 0.0
                    if (catSpend > 0.0) {
                        val sweepAngle = ((catSpend / totalSpend) * 360f).toFloat()
                        val isHighlighted = selectedCatId == cat.id
                        val activeStroke = if (isHighlighted) strokeWidth + 12f else strokeWidth
                        val activeAlpha = if (selectedCatId == null || isHighlighted) 1.0f else 0.35f

                        drawArc(
                            color = parseHexColor(cat.colorHex).copy(alpha = activeAlpha),
                            startAngle = currentAngle,
                            sweepAngle = sweepAngle,
                            useCenter = false,
                            style = Stroke(width = activeStroke, cap = StrokeCap.Round),
                            size = Size(size.width, size.height)
                        )
                        currentAngle += sweepAngle
                    }
                }
            }

            // Central info display inside donut hole
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                if (selectedCatId != null) {
                    val highlightedCat = categories.find { it.id == selectedCatId }
                    val highlightedStats = stats[selectedCatId]
                    if (highlightedCat != null && highlightedStats != null) {
                        Icon(
                            imageVector = getIconVector(highlightedCat.iconName),
                            contentDescription = highlightedCat.name,
                            tint = parseHexColor(highlightedCat.colorHex),
                            modifier = Modifier.size(22.dp)
                        )
                        Spacer(modifier = Modifier.height(2.dp))
                        Text(
                            text = highlightedCat.name,
                            fontWeight = FontWeight.Bold,
                            fontSize = 13.sp,
                            color = parseHexColor(highlightedCat.colorHex)
                        )
                        Text(
                            text = "$currencySymbol${NumberFormat.getIntegerInstance().format(highlightedStats.totalAmount)}",
                            fontWeight = FontWeight.ExtraBold,
                            fontSize = 17.sp,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                        Text(
                            text = String.format("%.1f%%", highlightedStats.percentage),
                            fontWeight = FontWeight.Medium,
                            fontSize = 11.sp,
                            color = MaterialTheme.colorScheme.outline
                        )
                    }
                } else {
                    Text(
                        text = "Total Spend",
                        fontSize = 11.sp,
                        color = MaterialTheme.colorScheme.outline,
                        fontWeight = FontWeight.Bold
                    )
                    Text(
                        text = "$currencySymbol${NumberFormat.getIntegerInstance().format(totalSpend)}",
                        fontWeight = FontWeight.ExtraBold,
                        fontSize = 18.sp,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                }
            }
        }
    }
}

// Visual Component: Custom Bar Chart Drawing
@Composable
fun BarChartComponent(
    categories: List<Category>,
    stats: Map<Int, CategoryStats>,
    selectedCatId: Int?,
    onBarClick: (Int) -> Unit
) {
    val activeStats = categories.map { it to (stats[it.id]?.totalAmount ?: 0.0) }
        .filter { it.second > 0.0 }
    val maxSpend = activeStats.maxOfOrNull { it.second } ?: 1.0

    if (activeStats.isEmpty()) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(180.dp),
            contentAlignment = Alignment.Center
        ) {
            Text("No spending data available to graph.", fontSize = 12.sp, color = Color.Gray)
        }
    } else {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .height(180.dp)
                .horizontalScroll(rememberScrollState())
                .padding(vertical = 8.dp),
            horizontalArrangement = Arrangement.spacedBy(16.dp),
            verticalAlignment = Alignment.Bottom
        ) {
            activeStats.forEach { (cat, spend) ->
                val ratio = (spend / maxSpend).toFloat()
                val isHighlighted = selectedCatId == cat.id
                val alpha = if (selectedCatId == null || isHighlighted) 1.0f else 0.35f

                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    modifier = Modifier
                        .width(48.dp)
                        .clickable { onBarClick(cat.id) }
                ) {
                    Box(
                        modifier = Modifier
                            .weight(1f)
                            .fillMaxWidth(),
                        contentAlignment = Alignment.BottomCenter
                    ) {
                        // Animated bar growth height
                        val animatedRatio by animateFloatAsState(targetValue = ratio, animationSpec = spring())
                        Box(
                            modifier = Modifier
                                .fillMaxWidth(0.6f)
                                .fillMaxHeight(animatedRatio.coerceIn(0.1f, 1.0f))
                                .clip(RoundedCornerShape(topStart = 8.dp, topEnd = 8.dp))
                                .background(parseHexColor(cat.colorHex).copy(alpha = alpha))
                                .border(
                                    width = if (isHighlighted) 2.dp else 0.dp,
                                    color = MaterialTheme.colorScheme.onSurface,
                                    shape = RoundedCornerShape(topStart = 8.dp, topEnd = 8.dp)
                                )
                        )
                    }
                    Spacer(modifier = Modifier.height(6.dp))
                    Icon(
                        imageVector = getIconVector(cat.iconName),
                        contentDescription = cat.displayName,
                        tint = parseHexColor(cat.colorHex).copy(alpha = alpha),
                        modifier = Modifier.size(16.dp)
                    )
                    Text(
                        text = cat.displayName,
                        fontSize = 9.sp,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onSurface.copy(alpha = alpha),
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }
            }
        }
    }
}

// Visual Component: Line Trend Chart spanning historical days
@Composable
fun SpendingTrendChart(
    categories: List<Category>,
    transactions: List<Transaction>,
    selectedMonth: YearMonth = YearMonth.now()
) {
    // Generate dates for 7-day window anchored to selectedMonth
    val dates = remember(selectedMonth) {
        val isCurrentMonth = selectedMonth == YearMonth.now()
        val anchor = if (isCurrentMonth) LocalDate.now() else selectedMonth.atEndOfMonth()
        val zone = ZoneId.systemDefault()
        (6 downTo 0).map { offset ->
            anchor.minusDays(offset.toLong()).atStartOfDay(zone).toInstant().toEpochMilli()
        }
    }

    val dailySpeeds = remember(transactions, dates) {
        dates.map { date ->
            val cal = Calendar.getInstance().apply { timeInMillis = date }
            val y = cal.get(Calendar.YEAR)
            val d = cal.get(Calendar.DAY_OF_YEAR)

            // Find spending for this exact day
            transactions.filter {
                val tCal = Calendar.getInstance().apply { timeInMillis = it.timestamp }
                tCal.get(Calendar.YEAR) == y && tCal.get(Calendar.DAY_OF_YEAR) == d
            }.sumOf { it.amount }
        }
    }

    val maxVal = dailySpeeds.maxOrNull()?.toFloat() ?: 100f
    val sdf = SimpleDateFormat("EEE", Locale.US)
    val monthFormatter = remember { DateTimeFormatter.ofPattern("MMM yyyy", Locale.getDefault()) }

    Column(modifier = Modifier.fillMaxWidth()) {
        Text(
            text = "Daily Spending Activity (${selectedMonth.format(monthFormatter)})",
            fontSize = 11.sp,
            color = MaterialTheme.colorScheme.primary,
            fontWeight = FontWeight.Bold,
            modifier = Modifier.padding(bottom = 8.dp)
        )

        Canvas(
            modifier = Modifier
                .fillMaxWidth()
                .height(130.dp)
        ) {
            val width = size.width
            val height = size.height
            val spacing = width / 6f

            val points = dailySpeeds.mapIndexed { idx, value ->
                val x = idx * spacing
                // invert coords since (0,0) is top-left
                val y = height - ((value.toFloat() / if (maxVal > 0f) maxVal else 100f) * (height - 40f)) - 20f
                Offset(x, y)
            }

            // Draw guidelines
            for (i in 1..3) {
                val lY = height * (i / 4f)
                drawLine(
                    color = Color.LightGray.copy(alpha = 0.25f),
                    start = Offset(0f, lY),
                    end = Offset(width, lY),
                    strokeWidth = 1f
                )
            }

            // Connect lines
            for (i in 0 until points.size - 1) {
                drawLine(
                    color = Color(0xFF4D96FF),
                    start = points[i],
                    end = points[i + 1],
                    strokeWidth = 4.dp.toPx(),
                    cap = StrokeCap.Round
                )
            }

            // Draw circles over point markers for color diversity
            points.forEach { pt ->
                drawCircle(
                    color = Color(0xFFFF6B6B),
                    radius = 5.dp.toPx(),
                    center = pt
                )
                drawCircle(
                    color = Color.White,
                    radius = 2.5.dp.toPx(),
                    center = pt
                )
            }
        }

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            dates.forEach { date ->
                val dCal = Calendar.getInstance().apply { timeInMillis = date }
                val dayNum = dCal.get(Calendar.DAY_OF_MONTH)
                val dayName = sdf.format(Date(date))
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    modifier = Modifier.width(36.dp)
                ) {
                    Text(
                        text = dayName,
                        fontSize = 9.sp,
                        color = MaterialTheme.colorScheme.outline,
                        fontWeight = FontWeight.Bold
                    )
                    Text(
                        text = "$dayNum",
                        fontSize = 10.sp,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        fontWeight = FontWeight.SemiBold
                    )
                }
            }
        }
    }
}

// Visual Component: Legend System with highlighting
@OptIn(ExperimentalLayoutApi::class)
@Composable
fun CategoryLegendView(
    categories: List<Category>,
    stats: Map<Int, CategoryStats>,
    selectedCatId: Int?,
    onLegendClick: (Int) -> Unit,
    onClearClick: () -> Unit
) {
    Column {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = "Color Codes Legend",
                fontSize = 11.sp,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.outline
            )
            if (selectedCatId != null) {
                TextButton(
                    onClick = onClearClick,
                    contentPadding = PaddingValues(0.dp)
                ) {
                    Text("Clear Highlight", fontSize = 11.sp, fontWeight = FontWeight.Bold)
                }
            }
        }

        Spacer(modifier = Modifier.height(4.dp))

        FlowRow(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            categories.forEach { cat ->
                val isSelected = selectedCatId == cat.id
                val spend = stats[cat.id]?.totalAmount ?: 0.0

                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier
                        .clip(RoundedCornerShape(8.dp))
                        .background(
                            if (isSelected) parseHexColor(cat.colorHex).copy(alpha = 0.15f)
                            else Color.Transparent
                        )
                        .border(
                            width = 1.dp,
                            color = if (isSelected) parseHexColor(cat.colorHex) else Color.LightGray.copy(alpha = 0.3f),
                            shape = RoundedCornerShape(8.dp)
                        )
                        .clickable { onLegendClick(cat.id) }
                        .padding(horizontal = 8.dp, vertical = 6.dp)
                ) {
                    Box(
                        modifier = Modifier
                            .size(20.dp)
                            .clip(CircleShape)
                            .background(parseHexColor(cat.colorHex)),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = getIconVector(cat.iconName),
                            contentDescription = null,
                            tint = getContrastColor(cat.colorHex),
                            modifier = Modifier.size(12.dp)
                        )
                    }
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(
                        text = cat.displayName,
                        fontSize = 11.sp,
                        fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium,
                        color = if (isSelected) parseHexColor(cat.colorHex) else MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    if (spend > 0.0) {
                        Spacer(modifier = Modifier.width(4.dp))
                        Text(
                            text = "(₹${NumberFormat.getIntegerInstance().format(spend)})",
                            fontSize = 9.sp,
                            color = MaterialTheme.colorScheme.outline,
                            fontWeight = FontWeight.SemiBold
                        )
                    }
                }
            }
        }
    }
}

// Visual Component: Highlighting Display Panel Card (Hover stats analogue)
@Composable
fun DetailedStatsPanel(
    category: Category,
    stats: CategoryStats,
    currencySymbol: String = "₹",
    onClear: () -> Unit
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .shadow(4.dp, RoundedCornerShape(16.dp))
            .testTag("detailed_stats_panel"),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = parseHexColor(category.colorHex).copy(alpha = 0.08f)),
        border = BorderStroke(1.5.dp, parseHexColor(category.colorHex))
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Box(
                        modifier = Modifier
                            .size(28.dp)
                            .clip(CircleShape)
                            .background(parseHexColor(category.colorHex)),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = getIconVector(category.iconName),
                            contentDescription = category.displayName,
                            tint = getContrastColor(category.colorHex),
                            modifier = Modifier.size(16.dp)
                        )
                    }
                    Spacer(modifier = Modifier.width(10.dp))
                    Text(
                        text = "${category.displayName} Analysis",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        color = parseHexColor(category.colorHex)
                    )
                }

                IconButton(onClick = onClear, modifier = Modifier.size(48.dp)) {
                    Icon(imageVector = Icons.Default.Close, contentDescription = "Close detailed panel", modifier = Modifier.size(20.dp))
                }
            }

            Spacer(modifier = Modifier.height(14.dp))

            // Stats grid
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                // Total amount spent
                Column(modifier = Modifier.weight(1f)) {
                    Text(text = "Total Spent", fontSize = 10.sp, color = MaterialTheme.colorScheme.outline, fontWeight = FontWeight.Bold)
                    Text(
                        text = "$currencySymbol${NumberFormat.getNumberInstance(Locale.US).format(stats.totalAmount)}",
                        fontSize = 17.sp,
                        fontWeight = FontWeight.ExtraBold,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                }

                // Transaction count
                Column(modifier = Modifier.weight(1f)) {
                    Text(text = "Transactions", fontSize = 10.sp, color = MaterialTheme.colorScheme.outline, fontWeight = FontWeight.Bold)
                    Text(
                        text = "${stats.transactionCount} records",
                        fontSize = 16.sp,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                }

                // Distribution percentage
                Column(modifier = Modifier.weight(1f)) {
                    Text(text = "Share of Spend", fontSize = 10.sp, color = MaterialTheme.colorScheme.outline, fontWeight = FontWeight.Bold)
                    Text(
                        text = String.format("%.1f%%", stats.percentage),
                        fontSize = 16.sp,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                }
            }

            Spacer(modifier = Modifier.height(14.dp))

            // Monthly Trend analytics
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(8.dp))
                    .background(Color.LightGray.copy(alpha = 0.15f))
                    .padding(8.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    val isUp = stats.trendPercentage >= 0.0
                    Icon(
                        imageVector = if (isUp) Icons.Default.TrendingUp else Icons.Default.TrendingDown,
                        contentDescription = "Trend direction",
                        tint = if (isUp) Color(0xFF6BCB77) else Color(0xFFFF6B6B),
                        modifier = Modifier.size(18.dp)
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(
                        text = "Trend vs Last Month:",
                        fontSize = 11.sp,
                        fontWeight = FontWeight.SemiBold,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                }

                val trendFormatted = String.format("%.1f%%", stats.trendPercentage)
                val trendText = if (stats.trendPercentage > 0.0) "+$trendFormatted" else trendFormatted
                Text(
                    text = trendText,
                    fontSize = 12.sp,
                    fontWeight = FontWeight.Black,
                    color = if (stats.trendPercentage >= 0.0) Color(0xFF6BCB77) else Color(0xFFFF6B6B)
                )
            }
        }
    }
}

// Visual Component: Upgraded Manage Category Card
@OptIn(ExperimentalLayoutApi::class)
@Composable
fun UpgradedManageCategoryCard(
    category: Category,
    stats: CategoryStats?,
    subcategories: List<Subcategory>,
    currencySymbol: String,
    canMoveUp: Boolean,
    canMoveDown: Boolean,
    onMoveUp: () -> Unit,
    onMoveDown: () -> Unit,
    onEdit: () -> Unit,
    onDelete: () -> Unit
) {
    val budget = category.budgetLimit
    val spent = stats?.totalAmount ?: 0.0
    val ratio = if (budget > 0.0) (spent / budget).toFloat() else 0f
    val isOverBudget = spent > budget && budget > 0.0
    val catColor = parseHexColor(category.colorHex)

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .testTag("category_card_${category.id}"),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        border = BorderStroke(1.dp, catColor.copy(alpha = 0.25f))
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            // Row 1: Header (Icon, Name, Reorder/Edit/Delete actions)
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.weight(1f)
                ) {
                    Box(
                        modifier = Modifier
                            .size(38.dp)
                            .clip(CircleShape)
                            .background(catColor),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = getIconVector(category.iconName),
                            contentDescription = null,
                            tint = getContrastColor(category.colorHex),
                            modifier = Modifier.size(20.dp)
                        )
                    }
                    Spacer(modifier = Modifier.width(12.dp))
                    Column {
                        Text(
                            text = category.displayName,
                            fontWeight = FontWeight.ExtraBold,
                            style = MaterialTheme.typography.titleMedium,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                        Text(
                            text = "${subcategories.size} Subcategories",
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.outline
                        )
                    }
                }

                // Small quick reorder and edits panel
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    // Reorder controls
                    IconButton(
                        onClick = onMoveUp,
                        enabled = canMoveUp,
                        modifier = Modifier.size(48.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.KeyboardArrowUp,
                            contentDescription = "Move Up",
                            tint = if (canMoveUp) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.outline.copy(alpha = 0.3f),
                            modifier = Modifier.size(24.dp)
                        )
                    }
                    IconButton(
                        onClick = onMoveDown,
                        enabled = canMoveDown,
                        modifier = Modifier.size(48.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.KeyboardArrowDown,
                            contentDescription = "Move Down",
                            tint = if (canMoveDown) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.outline.copy(alpha = 0.3f),
                            modifier = Modifier.size(24.dp)
                        )
                    }

                    // Divider
                    Box(
                        modifier = Modifier
                            .width(1.dp)
                            .height(24.dp)
                            .background(MaterialTheme.colorScheme.outline.copy(alpha = 0.2f))
                    )

                    // Edit / Delete controls
                    IconButton(
                        onClick = onEdit,
                        modifier = Modifier.size(48.dp).testTag("edit_category_btn_${category.id}")
                    ) {
                        Icon(
                            imageVector = Icons.Default.Edit,
                            contentDescription = "Edit Category",
                            tint = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.size(20.dp)
                        )
                    }
                    val isSystemCategory = category.name == "Lending" || category.name == "Loan Repayment"
                    if (!isSystemCategory) {
                        IconButton(
                            onClick = onDelete,
                            modifier = Modifier.size(48.dp).testTag("delete_category_btn_${category.id}")
                        ) {
                            Icon(
                                imageVector = Icons.Default.Delete,
                                contentDescription = "Delete Category",
                                tint = MaterialTheme.colorScheme.error.copy(alpha = 0.8f),
                                modifier = Modifier.size(20.dp)
                            )
                        }
                    }
                }
            }

            // Row 2: Subcategories chips
            if (subcategories.isNotEmpty()) {
                FlowRow(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(6.dp),
                    verticalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    subcategories.forEach { sub ->
                        val subColor = parseHexColor(sub.colorHexOverride ?: category.colorHex)
                        Box(
                            modifier = Modifier
                                .clip(RoundedCornerShape(6.dp))
                                .background(subColor.copy(alpha = 0.1f))
                                .border(1.dp, subColor.copy(alpha = 0.25f), RoundedCornerShape(6.dp))
                                .padding(horizontal = 8.dp, vertical = 4.dp)
                        ) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Box(
                                    modifier = Modifier
                                        .size(6.dp)
                                        .clip(CircleShape)
                                        .background(subColor)
                                )
                                Spacer(modifier = Modifier.width(6.dp))
                                Text(
                                    text = sub.name,
                                    style = MaterialTheme.typography.labelSmall,
                                    color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.8f)
                                )
                            }
                        }
                    }
                }
            }

            // Divider
            HorizontalDivider(color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.08f))

            // Row 3: Budget progress & stats meters
            Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = if (budget > 0.0) "Monthly Limit: ${formatInRupee(budget, currencySymbol)}" else "No Limit Set",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.outline,
                        fontWeight = FontWeight.Bold
                    )
                    Text(
                        text = "Spent: ${formatInRupee(spent, currencySymbol)}",
                        style = MaterialTheme.typography.bodySmall,
                        fontWeight = FontWeight.Black,
                        color = if (isOverBudget) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.onSurface
                    )
                }

                LinearProgressIndicator(
                    progress = { ratio.coerceIn(0f, 1f) },
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(8.dp)
                        .clip(RoundedCornerShape(4.dp)),
                    color = if (isOverBudget) MaterialTheme.colorScheme.error else catColor,
                    trackColor = catColor.copy(alpha = 0.15f)
                )

                if (budget > 0.0) {
                    val percentLeft = ((1.0 - ratio) * 100).coerceAtLeast(0.0)
                    Text(
                        text = if (isOverBudget) "Over budget limits!" else String.format("%.1f%% of budget pool remaining", percentLeft),
                        style = MaterialTheme.typography.labelSmall,
                        fontWeight = FontWeight.Bold,
                        color = if (isOverBudget) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.outline
                    )
                }
            }
        }
    }
}

// Visual Component: Category Card Item
@Composable
fun CategoryCardItem(
    category: Category,
    stats: CategoryStats?,
    subcategories: List<Subcategory>,
    onEdit: () -> Unit,
    onDelete: () -> Unit
) {
    Card(
        modifier = Modifier
            .width(190.dp)
            .height(225.dp)
            .testTag("category_card_${category.id}"),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        border = BorderStroke(1.5.dp, parseHexColor(category.colorHex).copy(alpha = 0.5f))
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(12.dp),
            verticalArrangement = Arrangement.SpaceBetween
        ) {
            // Title & Actions row
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Box(
                        modifier = Modifier
                            .size(26.dp)
                            .clip(CircleShape)
                            .background(parseHexColor(category.colorHex)),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = getIconVector(category.iconName),
                            contentDescription = null,
                            tint = getContrastColor(category.colorHex),
                            modifier = Modifier.size(14.dp)
                        )
                    }
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(
                        text = category.displayName,
                        fontWeight = FontWeight.Bold,
                        style = MaterialTheme.typography.titleSmall,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                }

                // Tiny edit dropdown trigger
                Row {
                    IconButton(
                        onClick = onEdit,
                        modifier = Modifier.size(48.dp).testTag("edit_category_btn")
                    ) {
                        Icon(
                            imageVector = Icons.Default.Edit,
                            contentDescription = "Edit Category",
                            modifier = Modifier.size(16.dp)
                        )
                    }
                    IconButton(
                        onClick = onDelete,
                        modifier = Modifier.size(48.dp).testTag("delete_category_btn")
                    ) {
                        Icon(
                            imageVector = Icons.Default.Delete,
                            contentDescription = "Delete Category",
                            modifier = Modifier.size(16.dp),
                            tint = Color.Red.copy(alpha = 0.6f)
                        )
                    }
                }
            }

            // Subcategories listing
            Column(
                modifier = Modifier
                    .weight(1f)
                    .padding(vertical = 4.dp)
            ) {
                Text(
                    text = "Subcategories",
                    style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.outline,
                    fontWeight = FontWeight.Bold
                )

                if (subcategories.isEmpty()) {
                    Text(
                        text = "None created",
                        style = MaterialTheme.typography.labelSmall,
                        color = Color.LightGray,
                        modifier = Modifier.padding(top = 2.dp)
                    )
                } else {
                    Column(
                        modifier = Modifier.verticalScroll(rememberScrollState()),
                        verticalArrangement = Arrangement.spacedBy(2.dp)
                    ) {
                        subcategories.take(4).forEach { sub ->
                            val subColor = sub.colorHexOverride ?: category.colorHex
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(vertical = 1.dp)
                            ) {
                                Box(
                                    modifier = Modifier
                                        .size(6.dp)
                                        .clip(CircleShape)
                                        .background(parseHexColor(subColor))
                                )
                                Spacer(modifier = Modifier.width(4.dp))
                                Text(
                                    text = sub.name,
                                    style = MaterialTheme.typography.labelSmall,
                                    color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.8f),
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis
                                )
                            }
                        }
                        if (subcategories.size > 4) {
                            Text(
                                text = "+${subcategories.size - 4} more",
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.primary,
                                fontWeight = FontWeight.Bold
                            )
                        }
                    }
                }
            }

            // Budget vs spent meter
            Column {
                val budget = category.budgetLimit
                val spent = stats?.totalAmount ?: 0.0
                val ratio = if (budget > 0.0) (spent / budget).toFloat() else 0f

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Text(
                        text = "Limit: ${formatInRupee(budget)}",
                        style = MaterialTheme.typography.labelMedium,
                        color = MaterialTheme.colorScheme.outline
                    )
                    Text(
                        text = "Spent: ${formatInRupee(spent)}",
                        style = MaterialTheme.typography.labelMedium,
                        fontWeight = FontWeight.Bold,
                        color = if (spent > budget && budget > 0.0) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.onSurface
                    )
                }

                Spacer(modifier = Modifier.height(3.dp))

                LinearProgressIndicator(
                    progress = { ratio.coerceIn(0f, 1f) },
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(4.dp)
                        .clip(CircleShape),
                    color = if (spent > budget && budget > 0.0) Color.Red else parseHexColor(category.colorHex),
                    trackColor = Color.LightGray.copy(alpha = 0.25f)
                )
            }
        }
    }
}

// Visual Component: Transaction Item row with category color badges
/** "Today, 2:15 PM" for today, "Yesterday, 9:00 AM", otherwise "12 Sep, 2026". */
fun formatTransactionDate(timestamp: Long): String {
    val zone = ZoneId.systemDefault()
    val date = Instant.ofEpochMilli(timestamp).atZone(zone).toLocalDate()
    val today = LocalDate.now()
    val timeFormatter = DateTimeFormatter.ofPattern("h:mm a", Locale.getDefault())
    val time = Instant.ofEpochMilli(timestamp).atZone(zone).format(timeFormatter)
    return when (date) {
        today -> "Today, $time"
        today.minusDays(1) -> "Yesterday, $time"
        else -> date.format(DateTimeFormatter.ofPattern("d MMM, yyyy", Locale.getDefault()))
    }
}

@Composable
fun TransactionItemRow(
    transaction: Transaction,
    category: Category?,
    subcategory: Subcategory?,
    currency: String = "₹",
    readOnly: Boolean = false,
    onDelete: () -> Unit,
    onEdit: () -> Unit
) {
    val fallbackColor = "#777777"
    val catColor = category?.colorHex ?: fallbackColor
    val visualColor = subcategory?.colorHexOverride ?: catColor
    val isLendingLinked = transaction.lendingEntryId != null
    val effectiveReadOnly = readOnly || isLendingLinked

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .then(if (!effectiveReadOnly) Modifier.clickable { onEdit() } else Modifier)
            .padding(horizontal = 18.dp, vertical = 12.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier.weight(1f)
        ) {
            // Circular category badge — the app's colour system, carried through to every row
            Box(
                modifier = Modifier
                    .size(44.dp)
                    .clip(CircleShape)
                    .background(parseHexColor(visualColor)),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = getIconVector(category?.iconName ?: "help"),
                    contentDescription = category?.displayName ?: "Unknown category",
                    tint = getContrastColor(visualColor),
                    modifier = Modifier.size(22.dp)
                )
            }

            Spacer(modifier = Modifier.width(14.dp))

            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = transaction.description.ifEmpty { "Transaction Record" },
                    fontWeight = FontWeight.Bold,
                    fontSize = 15.sp,
                    color = MaterialTheme.colorScheme.onSurface,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                Spacer(modifier = Modifier.height(3.dp))

                // Category · subcategory · when, as one quiet metadata line
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(
                        text = category?.displayName ?: "Unknown",
                        fontSize = 12.sp,
                        fontWeight = FontWeight.SemiBold,
                        color = parseHexColor(catColor),
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                        modifier = Modifier.weight(1f, fill = false)
                    )
                    if (subcategory != null) {
                        Text(
                            text = " · ${subcategory.name}",
                            fontSize = 12.sp,
                            color = parseHexColor(subcategory.colorHexOverride ?: catColor),
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis,
                            modifier = Modifier.weight(1f, fill = false)
                        )
                    }
                    Text(
                        text = " • ${formatTransactionDate(transaction.timestamp)}",
                        fontSize = 12.sp,
                        color = MaterialTheme.colorScheme.outline,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }

                // Lending badge stays a chip: it marks a row that cannot be edited here
                if (isLendingLinked) {
                    Spacer(modifier = Modifier.height(4.dp))
                    Surface(
                        shape = RoundedCornerShape(6.dp),
                        color = MaterialTheme.colorScheme.primary.copy(alpha = 0.14f),
                        border = BorderStroke(0.5.dp, MaterialTheme.colorScheme.primary.copy(alpha = 0.4f)),
                        modifier = Modifier.testTag("lending_badge_${transaction.id}")
                    ) {
                        Text(
                            text = "Lending",
                            fontSize = 11.sp,
                            fontWeight = FontWeight.SemiBold,
                            color = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                        )
                    }
                }
            }
        }

        Spacer(modifier = Modifier.width(8.dp))

        Row(verticalAlignment = Alignment.CenterVertically) {
            val isIncome = transaction.type == "INCOME"
            val prefix = if (isIncome) "+" else "-"
            val amountColor = if (isIncome) IncomeGreen else ExpenseRed
            Text(
                text = "$prefix${formatInRupee(transaction.amount, currency = currency)}",
                fontWeight = FontWeight.Black,
                fontSize = 16.sp,
                color = amountColor,
                maxLines = 1
            )
            if (!effectiveReadOnly) {
                IconButton(
                    onClick = onDelete,
                    modifier = Modifier.size(36.dp).testTag("delete_transaction_btn")
                ) {
                    Icon(
                        imageVector = Icons.Default.Delete,
                        contentDescription = "Delete",
                        tint = MaterialTheme.colorScheme.outline.copy(alpha = 0.55f),
                        modifier = Modifier.size(18.dp)
                    )
                }
            }
        }
    }
}

// Subcategory Edit row data class helper
data class SubcategoryEditorItem(
    val id: Int = 0,
    val name: String,
    val colorHexOverride: String? = null
)

// Modal Window: Category Form Editor with unique constraints & smart suggestions
@OptIn(ExperimentalLayoutApi::class)
@Composable
fun CategoryEditorDialog(
    viewModel: CategoryViewModel,
    category: Category? = null,
    initialType: String = "EXPENSE",
    subcategories: List<Subcategory> = emptyList(),
    onDismiss: () -> Unit,
    onSave: (name: String, colorHex: String, iconName: String, budget: Double, subcategories: List<SubcategoryEditorItem>, type: String) -> Unit
) {
    val haptic = LocalHapticFeedback.current
    val coroutineScope = rememberCoroutineScope()
    var name by remember { mutableStateOf(category?.name ?: "") }
    var colorHex by remember { mutableStateOf(category?.colorHex ?: "") }
    var iconName by remember { mutableStateOf(category?.iconName ?: "restaurant") }
    var budgetStr by remember { mutableStateOf(category?.budgetLimit?.toInt()?.toString() ?: "") }
    var categoryType by remember { mutableStateOf(category?.type ?: initialType) }

    // Subcategories listing inside editor
    val subList = remember {
        mutableStateListOf<SubcategoryEditorItem>().apply {
            addAll(subcategories.map { SubcategoryEditorItem(it.id, it.name, it.colorHexOverride) })
        }
    }
    var newSubcategoryName by remember { mutableStateOf("") }

    // Validation & Warning status holding
    var isDuplicateError by remember { mutableStateOf(false) }
    var similarityWarning by remember { mutableStateOf<String?>(null) }
    
    // Auto color recommendation palette suggestions
    val smartSuggestions = remember(viewModel.categories.collectAsStateWithLifecycle().value, viewModel.currentPaletteTheme.collectAsStateWithLifecycle().value) {
        viewModel.getSmartColorSuggestions()
    }

    // Set initial auto suggested color if creating new category and none picked
    LaunchedEffect(smartSuggestions, colorHex) {
        if (category == null && colorHex.isEmpty() && smartSuggestions.isNotEmpty()) {
            colorHex = smartSuggestions.first()
        }
    }

    // Checking validation constraints on color choice
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

    Dialog(onDismissRequest = onDismiss) {
        Card(
            modifier = Modifier
                .fillMaxWidth()
                .shadow(8.dp, RoundedCornerShape(20.dp))
                .testTag("category_editor_dialog"),
            shape = RoundedCornerShape(20.dp),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
        ) {
            Column(
                modifier = Modifier
                    .padding(18.dp)
                    .verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Text(
                    text = if (category == null) "Create Category" else "Update Category",
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.ExtraBold,
                    color = MaterialTheme.colorScheme.onSurface
                )

                // Category Type Selection (Expense vs Income)
                Text(
                    text = "Category Type",
                    fontSize = 11.sp,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.outline
                )
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    val types = listOf("EXPENSE" to "Expense", "INCOME" to "Income")
                    types.forEach { (typeVal, label) ->
                        val isSelected = categoryType == typeVal
                        val activeColor = if (typeVal == "INCOME") Color(0xFF2E7D32) else MaterialTheme.colorScheme.primary
                        Box(
                            modifier = Modifier
                                .weight(1f)
                                .clip(RoundedCornerShape(12.dp))
                                .background(
                                    if (isSelected) activeColor.copy(alpha = 0.15f)
                                    else MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.2f)
                                )
                                .border(
                                    width = if (isSelected) 2.dp else 1.dp,
                                    color = if (isSelected) activeColor else Color.LightGray.copy(alpha = 0.3f),
                                    shape = RoundedCornerShape(12.dp)
                                )
                                .clickable { categoryType = typeVal }
                                .padding(vertical = 10.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                text = label,
                                fontSize = 13.sp,
                                fontWeight = FontWeight.Bold,
                                color = if (isSelected) activeColor else MaterialTheme.colorScheme.onSurface
                            )
                        }
                    }
                }

                // Name field
                OutlinedTextField(
                    value = name,
                    onValueChange = { name = it },
                    label = { Text("Category Name") },
                    modifier = Modifier.fillMaxWidth().testTag("cat_name_input"),
                    singleLine = true
                )

                // Budget Limit field
                OutlinedTextField(
                    value = budgetStr,
                    onValueChange = { budgetStr = it },
                    label = { Text("Budget Limit (₹)") },
                    modifier = Modifier.fillMaxWidth().testTag("cat_budget_input"),
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                    singleLine = true
                )

                // Theme Icons Selector grid
                Text(
                    text = "Category Icon",
                    fontSize = 11.sp,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.outline
                )

                FlowRow(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    ICON_OPTIONS.forEach { icon ->
                        val isSelected = iconName == icon
                        IconButton(
                            onClick = { iconName = icon },
                            colors = IconButtonDefaults.iconButtonColors(
                                containerColor = if (isSelected) MaterialTheme.colorScheme.primaryContainer else MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.3f)
                            ),
                            modifier = Modifier.size(48.dp)
                        ) {
                            Icon(
                                imageVector = getIconVector(icon),
                                contentDescription = icon,
                                modifier = Modifier.size(24.dp),
                                tint = if (isSelected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurface
                            )
                        }
                    }
                }

                // Pick Custom Color hex representation
                OutlinedTextField(
                    value = colorHex,
                    onValueChange = { colorHex = it },
                    label = { Text("Category Theme Color Hex") },
                    isError = isDuplicateError,
                    modifier = Modifier.fillMaxWidth().testTag("cat_color_input"),
                    placeholder = { Text("#FF6B6B") },
                    trailingIcon = {
                        Box(
                            modifier = Modifier
                                .size(24.dp)
                                .clip(CircleShape)
                                .background(parseHexColor(colorHex))
                        )
                    },
                    singleLine = true
                )

                // Error Warning Prompts
                if (isDuplicateError) {
                    Text(
                        text = "This color is already assigned to another category.",
                        color = Color.Red,
                        fontSize = 10.sp,
                        fontWeight = FontWeight.Bold,
                        modifier = Modifier.testTag("duplicate_color_warning")
                    )
                } else if (similarityWarning != null) {
                    Text(
                        text = "Warning: Color distance is close to existing Category: '$similarityWarning'. Choosing a more distinct shade is recommended for readability.",
                        color = Color(0xFFD48A00),
                        fontSize = 9.sp,
                        fontWeight = FontWeight.Medium,
                        modifier = Modifier.testTag("similarity_color_warning")
                    )
                }

                // Smart Color Suggestions grid
                Text(
                    text = "Smart Recommended Colors (Theme Palette)",
                    fontSize = 11.sp,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.outline
                )

                FlowRow(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(10.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    smartSuggestions.forEach { suggestion ->
                        val isSelected = colorHex.uppercase() == suggestion.uppercase()
                        Box(
                            modifier = Modifier
                                .size(28.dp)
                                .clip(CircleShape)
                                .background(parseHexColor(suggestion))
                                .border(
                                    width = if (isSelected) 3.dp else 1.dp,
                                    color = if (isSelected) MaterialTheme.colorScheme.onSurface else Color.LightGray.copy(alpha = 0.5f),
                                    shape = CircleShape
                                )
                                .clickable { colorHex = suggestion }
                        )
                    }
                }

                // Subcategories Creation Row
                HorizontalDivider(modifier = Modifier.padding(vertical = 4.dp))
                Text(
                    text = "Manage Subcategories",
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.Bold
                )

                // Simple mini subcategory adder
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    OutlinedTextField(
                        value = newSubcategoryName,
                        onValueChange = { newSubcategoryName = it },
                        label = { Text("Subcategory Name") },
                        modifier = Modifier.weight(1f),
                        singleLine = true
                    )
                    Button(
                        onClick = {
                            if (newSubcategoryName.isNotBlank()) {
                                // Automatically allocate lighter shade factor based on index
                                val shades = viewModel.getLighterShades(colorHex)
                                val shadeOverride = if (subList.size < shades.size) shades[subList.size] else shades.last()
                                subList.add(SubcategoryEditorItem(name = newSubcategoryName, colorHexOverride = shadeOverride))
                                newSubcategoryName = ""
                            }
                        },
                        shape = RoundedCornerShape(8.dp),
                        contentPadding = PaddingValues(horizontal = 8.dp)
                    ) {
                        Text("Add", fontSize = 11.sp)
                    }
                }

                // Temporary list displays
                Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                    subList.forEachIndexed { index, subitem ->
                        val resolvedSubColor = subitem.colorHexOverride ?: colorHex
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clip(RoundedCornerShape(8.dp))
                                .background(Color.LightGray.copy(alpha = 0.15f))
                                .padding(horizontal = 8.dp, vertical = 6.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Box(
                                    modifier = Modifier
                                        .size(10.dp)
                                        .clip(CircleShape)
                                        .background(parseHexColor(resolvedSubColor))
                                )
                                Spacer(modifier = Modifier.width(8.dp))
                                Text(subitem.name, fontSize = 12.sp, color = MaterialTheme.colorScheme.onSurface)
                            }

                            // Subcategory lighter shades customize pallet picker! Shows visual gradient
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                val shadesOfParent = viewModel.getLighterShades(colorHex)
                                shadesOfParent.forEach { shade ->
                                    val isSelectedValue = shade.uppercase() == resolvedSubColor.uppercase()
                                    Box(
                                        modifier = Modifier
                                            .padding(horizontal = 2.dp)
                                            .size(14.dp)
                                            .clip(CircleShape)
                                            .background(parseHexColor(shade))
                                            .border(
                                                width = if (isSelectedValue) 1.5.dp else 0.dp,
                                                color = Color.Black,
                                                shape = CircleShape
                                            )
                                            .clickable {
                                                subList[index] = subitem.copy(colorHexOverride = shade)
                                            }
                                    )
                                }

                                Spacer(modifier = Modifier.width(6.dp))
                                IconButton(
                                    onClick = { subList.remove(subitem) },
                                    modifier = Modifier.size(36.dp)
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.Close,
                                        contentDescription = "Remove",
                                        modifier = Modifier.size(16.dp),
                                        tint = Color.Red
                                    )
                                }
                            }
                        }
                    }
                }

                // Interactive save control actions
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.End
                ) {
                    TextButton(onClick = onDismiss) {
                        Text("Cancel")
                    }
                    Button(
                        onClick = {
                            if (name.isNotBlank() && colorHex.isNotBlank() && !isDuplicateError) {
                                haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                                val budgetValue = budgetStr.toDoubleOrNull() ?: 0.0
                                onSave(name, colorHex, iconName, budgetValue, subList.toList(), categoryType)
                            }
                        },
                        enabled = name.isNotBlank() && colorHex.isNotBlank() && !isDuplicateError,
                        modifier = Modifier.testTag("save_category_submit_btn")
                    ) {
                        Text("Save")
                    }
                }
            }
        }
    }
}

// Modal Window: Tract transactions form dialog
@OptIn(ExperimentalLayoutApi::class, ExperimentalMaterial3Api::class)
@Composable
fun AddTransactionDialog(
    categories: List<Category>,
    subcategories: List<Subcategory>,
    transaction: Transaction? = null,
    defaultType: String = "EXPENSE",
    transactions: List<Transaction> = emptyList(),
    defaultMonth: YearMonth? = null,
    onCreateCategoryFirstClick: () -> Unit,
    onDismiss: () -> Unit,
    onSave: (catId: Int, subId: Int?, amount: Double, desc: String, timestamp: Long, type: String) -> Unit
) {
    val haptic = LocalHapticFeedback.current
    var amountStr by remember { mutableStateOf(transaction?.amount?.toString() ?: "") }
    var desc by remember { mutableStateOf(transaction?.description ?: "") }
    val initialType = transaction?.type ?: defaultType
    var selectedType by remember { mutableStateOf(initialType) }
    val filteredCategories = remember(categories, selectedType) {
        categories.filter { it.type.equals(selectedType, ignoreCase = true) }
    }
    var selectedCatId by remember {
        val initialCats = categories.filter { it.type.equals(initialType, ignoreCase = true) }
        mutableStateOf<Int?>(transaction?.categoryId ?: initialCats.firstOrNull()?.id)
    }
    var selectedSubId by remember { mutableStateOf<Int?>(transaction?.subcategoryId) }
    var timestamp by remember {
        val initialTimestamp = transaction?.timestamp ?: run {
            if (defaultMonth != null && defaultMonth != YearMonth.now()) {
                val day = java.time.LocalDate.now().dayOfMonth.coerceIn(1, defaultMonth.lengthOfMonth())
                defaultMonth.atDay(day).atTime(12, 0).atZone(java.time.ZoneId.systemDefault()).toInstant().toEpochMilli()
            } else {
                System.currentTimeMillis()
            }
        }
        mutableStateOf(initialTimestamp)
    }
    var showDatePicker by remember { mutableStateOf(false) }

    // Auto-populate when transaction updates
    LaunchedEffect(transaction) {
        transaction?.let {
            amountStr = it.amount.toString()
            desc = it.description
            selectedCatId = it.categoryId
            selectedSubId = it.subcategoryId
            selectedType = it.type
            timestamp = it.timestamp
        }
    }

    // Clear selected category if it doesn't match the new transaction type
    LaunchedEffect(selectedType) {
        if (selectedCatId != null) {
            val currentCat = categories.find { it.id == selectedCatId }
            if (currentCat == null || !currentCat.type.equals(selectedType, ignoreCase = true)) {
                selectedCatId = null
                selectedSubId = null
            }
        }
    }
    
    // Auto-update subcategory when category changes
    val subchoices = remember(selectedCatId, subcategories) {
        if (selectedCatId == null) emptyList()
        else subcategories.filter { it.parentCategoryId == selectedCatId }
    }
    
    val amountDouble = amountStr.trim().toDoubleOrNull()
    val isAmountValid = amountDouble != null && amountDouble > 0.0
    val isAmountError = amountStr.isNotBlank() && !isAmountValid
    
    // Automatically reset sub selection if it is no longer valid inside new category
    LaunchedEffect(selectedCatId) {
        if (transaction == null || selectedCatId != transaction.categoryId) {
            selectedSubId = null
        }
    }

    if (showDatePicker) {
        val datePickerState = rememberDatePickerState(initialSelectedDateMillis = timestamp)
        DatePickerDialog(
            onDismissRequest = { showDatePicker = false },
            confirmButton = {
                TextButton(onClick = {
                    datePickerState.selectedDateMillis?.let {
                        timestamp = it
                    }
                    showDatePicker = false
                }) {
                    Text("OK")
                }
            },
            dismissButton = {
                TextButton(onClick = { showDatePicker = false }) {
                    Text("Cancel")
                }
            }
        ) {
            DatePicker(state = datePickerState)
        }
    }

    Dialog(onDismissRequest = onDismiss) {
        Card(
            modifier = Modifier
                .fillMaxWidth()
                .shadow(8.dp, RoundedCornerShape(20.dp)),
            shape = RoundedCornerShape(20.dp),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
        ) {
            Column(
                modifier = Modifier
                    .padding(18.dp)
                    .verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Text(
                    text = if (transaction == null) "Add New Record" else "Modify Record",
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.ExtraBold,
                    color = MaterialTheme.colorScheme.onSurface
                )

                // Type Segment selection
                Text(
                    text = "Record Type",
                    fontSize = 11.sp,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.outline
                )
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    val types = listOf("EXPENSE" to "Expense", "INCOME" to "Income")
                    types.forEach { (typeVal, label) ->
                        val isSelected = selectedType == typeVal
                        Box(
                            modifier = Modifier
                                .weight(1f)
                                .clip(RoundedCornerShape(12.dp))
                                .background(
                                    if (isSelected) {
                                        if (typeVal == "INCOME") Color(0xFF2E7D32).copy(alpha = 0.15f)
                                        else MaterialTheme.colorScheme.primary.copy(alpha = 0.15f)
                                    } else MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.2f)
                                )
                                .border(
                                    width = if (isSelected) 2.dp else 1.dp,
                                    color = if (isSelected) {
                                        if (typeVal == "INCOME") Color(0xFF2E7D32)
                                        else MaterialTheme.colorScheme.primary
                                    } else Color.LightGray.copy(alpha = 0.3f),
                                    shape = RoundedCornerShape(12.dp)
                                )
                                .clickable { selectedType = typeVal }
                                .padding(vertical = 12.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                text = label,
                                fontSize = 13.sp,
                                fontWeight = FontWeight.Bold,
                                color = if (isSelected) {
                                    if (typeVal == "INCOME") Color(0xFF2E7D32)
                                    else MaterialTheme.colorScheme.primary
                                } else MaterialTheme.colorScheme.onSurface
                            )
                        }
                    }
                }

                // Amount
                OutlinedTextField(
                    value = amountStr,
                    onValueChange = { amountStr = it },
                    label = { Text("Amount (₹)") },
                    modifier = Modifier.fillMaxWidth().testTag("tx_amount_input"),
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                    singleLine = true,
                    isError = isAmountError,
                    supportingText = {
                        if (isAmountError) {
                            Text(
                                text = if (amountDouble != null && amountDouble <= 0.0)
                                    "Amount must be greater than zero."
                                else
                                    "Please enter a valid positive number.",
                                color = MaterialTheme.colorScheme.error,
                                modifier = Modifier.testTag("tx_amount_error_text")
                            )
                        }
                    }
                )

                // Description
                var isDescFocused by remember { mutableStateOf(false) }
                var userSelectedSuggestion by remember { mutableStateOf(false) }

                val priorDescriptions = remember(transactions) {
                    transactions
                        .sortedByDescending { it.timestamp }
                        .map { it.description.trim() }
                        .filter { it.isNotBlank() }
                        .fold(mutableListOf<String>()) { acc, d ->
                            if (acc.none { it.equals(d, ignoreCase = true) }) {
                                acc.add(d)
                            }
                            acc
                        }
                        .toList()
                }

                val filteredSuggestions = remember(desc, priorDescriptions) {
                    if (desc.isBlank()) {
                        emptyList()
                    } else {
                        priorDescriptions
                            .filter { it.startsWith(desc, ignoreCase = true) }
                            .take(5)
                    }
                }

                val showDropdown = remember(isDescFocused, desc, filteredSuggestions, userSelectedSuggestion) {
                    isDescFocused && 
                    desc.isNotEmpty() && 
                    filteredSuggestions.isNotEmpty() && 
                    !userSelectedSuggestion &&
                    !(filteredSuggestions.size == 1 && filteredSuggestions.first() == desc)
                }

                ExposedDropdownMenuBox(
                    expanded = showDropdown,
                    onExpandedChange = {}
                ) {
                    OutlinedTextField(
                        value = desc,
                        onValueChange = { 
                            desc = it 
                            userSelectedSuggestion = false
                        },
                        label = { Text("Description") },
                        modifier = Modifier
                            .fillMaxWidth()
                            .menuAnchor()
                            .onFocusChanged { isDescFocused = it.isFocused }
                            .testTag("tx_desc_input"),
                        singleLine = true
                    )

                    ExposedDropdownMenu(
                        expanded = showDropdown,
                        onDismissRequest = {},
                        modifier = Modifier
                            .background(MaterialTheme.colorScheme.surface)
                            .heightIn(max = 240.dp)
                    ) {
                        filteredSuggestions.forEach { suggestion ->
                            DropdownMenuItem(
                                leadingIcon = {
                                    Icon(
                                        imageVector = Icons.Default.History,
                                        contentDescription = "Recent description",
                                        tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.6f),
                                        modifier = Modifier.size(16.dp)
                                    )
                                },
                                text = {
                                    Text(
                                        text = suggestion,
                                        maxLines = 1,
                                        overflow = TextOverflow.Ellipsis,
                                        style = MaterialTheme.typography.bodyMedium,
                                        color = MaterialTheme.colorScheme.onSurface
                                    )
                                },
                                onClick = {
                                    desc = suggestion
                                    userSelectedSuggestion = true
                                    isDescFocused = false
                                }
                            )
                        }
                    }
                }

                // Date Selection
                Text(
                    text = "Transaction Date",
                    fontSize = 11.sp,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.outline
                )
                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable { showDatePicker = true },
                    shape = RoundedCornerShape(12.dp),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.25f)),
                    border = BorderStroke(1.dp, Color.LightGray.copy(alpha = 0.3f))
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 14.dp, vertical = 14.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(
                                imageVector = Icons.Default.DateRange,
                                contentDescription = "Calendar Icon",
                                tint = MaterialTheme.colorScheme.primary,
                                modifier = Modifier.size(18.dp)
                            )
                            Spacer(modifier = Modifier.width(10.dp))
                            val formattedDate = SimpleDateFormat("dd MMMM, yyyy", Locale.US).format(Date(timestamp))
                            Text(
                                text = formattedDate,
                                style = MaterialTheme.typography.bodyMedium,
                                fontWeight = FontWeight.SemiBold,
                                color = MaterialTheme.colorScheme.onSurface
                            )
                        }
                        Icon(
                            imageVector = Icons.Default.ArrowDropDown,
                            contentDescription = "Dropdown indicator",
                            tint = MaterialTheme.colorScheme.primary
                        )
                    }
                }

                 // Parent Category Selection
                Text(
                    text = "Select Category",
                    fontSize = 11.sp,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.outline
                )

                if (filteredCategories.isEmpty()) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 12.dp)
                            .clip(RoundedCornerShape(12.dp))
                            .background(MaterialTheme.colorScheme.errorContainer.copy(alpha = 0.15f))
                            .border(BorderStroke(1.dp, MaterialTheme.colorScheme.error.copy(alpha = 0.3f)), RoundedCornerShape(12.dp))
                            .padding(16.dp),
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        Text(
                            text = "No ${if (selectedType == "INCOME") "income" else "expense"} categories available.",
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        Text(
                            text = "Create a category first",
                            style = MaterialTheme.typography.bodyMedium.copy(
                                fontWeight = FontWeight.Bold,
                                textDecoration = androidx.compose.ui.text.style.TextDecoration.Underline
                            ),
                            color = MaterialTheme.colorScheme.error,
                            modifier = Modifier
                                .clickable {
                                    onCreateCategoryFirstClick()
                                }
                                .padding(8.dp)
                                .testTag("create_category_first_link")
                        )
                    }
                } else {
                    FlowRow(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(10.dp),
                        verticalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        filteredCategories.forEach { cat ->
                            val isSelected = selectedCatId == cat.id
                            val catColor = parseHexColor(cat.colorHex)
                            Column(
                                horizontalAlignment = Alignment.CenterHorizontally,
                                modifier = Modifier
                                    .width(74.dp)
                                    .clip(RoundedCornerShape(12.dp))
                                    .background(
                                        if (isSelected) catColor.copy(alpha = 0.15f)
                                        else MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.2f)
                                    )
                                    .border(
                                        width = if (isSelected) 2.5.dp else 1.dp,
                                        color = if (isSelected) catColor else Color.LightGray.copy(alpha = 0.3f),
                                        shape = RoundedCornerShape(12.dp)
                                    )
                                    .clickable { selectedCatId = cat.id }
                                    .padding(vertical = 8.dp, horizontal = 4.dp)
                            ) {
                                Box(
                                    modifier = Modifier
                                        .size(38.dp)
                                        .clip(CircleShape)
                                        .background(if (isSelected) catColor else catColor.copy(alpha = 0.2f)),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Icon(
                                        imageVector = getIconVector(cat.iconName),
                                        contentDescription = cat.displayName,
                                        tint = if (isSelected) getContrastColor(cat.colorHex) else catColor,
                                        modifier = Modifier.size(20.dp)
                                    )
                                }
                                Spacer(modifier = Modifier.height(4.dp))
                                Text(
                                    text = cat.displayName,
                                    fontSize = 11.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = MaterialTheme.colorScheme.onSurface,
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis
                                )
                            }
                        }
                    }
                }

                // Subcategory Optional Selection with Icons
                if (subchoices.isNotEmpty()) {
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        text = "Select Subcategory (Transforms Color)",
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.outline
                    )

                    FlowRow(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        subchoices.forEach { sub ->
                            val isSelected = selectedSubId == sub.id
                            val subColorHex = sub.colorHexOverride ?: categories.find { it.id == selectedCatId }?.colorHex ?: "#777777"
                            val subColor = parseHexColor(subColorHex)
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                modifier = Modifier
                                    .clip(RoundedCornerShape(12.dp))
                                    .background(
                                        if (isSelected) subColor else subColor.copy(alpha = 0.12f)
                                    )
                                    .border(
                                        width = if (isSelected) 2.dp else 1.dp,
                                        color = subColor,
                                        shape = RoundedCornerShape(12.dp)
                                    )
                                    .clickable {
                                        selectedSubId = if (isSelected) null else sub.id
                                    }
                                    .padding(horizontal = 10.dp, vertical = 6.dp)
                            ) {
                                Icon(
                                    imageVector = getSubcategoryIcon(sub.name),
                                    contentDescription = sub.name,
                                    tint = if (isSelected) getContrastColor(subColorHex) else subColor,
                                    modifier = Modifier.size(14.dp)
                                )
                                Spacer(modifier = Modifier.width(6.dp))
                                Text(
                                    text = sub.name,
                                    fontSize = 11.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = if (isSelected) getContrastColor(subColorHex) else MaterialTheme.colorScheme.onSurface
                                )
                            }
                        }
                    }
                }

                // Modal submission control actions
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.End,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    TextButton(onClick = onDismiss) {
                        Text("Cancel")
                    }
                    Button(
                        onClick = {
                            val amount = amountStr.trim().toDoubleOrNull() ?: 0.0
                            if (amount > 0.0 && selectedCatId != null) {
                                haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                                onSave(selectedCatId!!, selectedSubId, amount, desc, timestamp, selectedType)
                            }
                        },
                        enabled = isAmountValid && selectedCatId != null,
                        modifier = Modifier.testTag("save_transaction_submit_btn")
                    ) {
                        Text("Save")
                    }
                }
            }
        }
    }
}


data class OnboardingCategory(
    val id: String,
    val name: String,
    val iconName: String,
    val budget: Double,
    val subcategories: List<String>,
    val isCustom: Boolean = false,
    val customColorHex: String? = null,
    val type: String = "EXPENSE"
)

@Composable
fun OnboardingWizard(
    viewModel: CategoryViewModel,
    isTour: Boolean = false,
    onComplete: () -> Unit
) {
    val currentTheme by viewModel.currentPaletteTheme.collectAsStateWithLifecycle()
    val currencySymbol by viewModel.currency.collectAsStateWithLifecycle()
    val defaultType by viewModel.defaultTransactionType.collectAsStateWithLifecycle()
    val themeMode by viewModel.themeMode.collectAsStateWithLifecycle()
    
    var currentStep by remember { mutableStateOf(1) }
    
    // Choose active theme color accents dynamically for styling the onboarding flow
    val themeColorHex = remember(currentTheme) {
        val colorsList = viewModel.palettes[currentTheme] ?: listOf("#4D96FF")
        colorsList.getOrNull(1) ?: colorsList.firstOrNull() ?: "#4D96FF"
    }
    val themeColor = remember(themeColorHex) { parseHexColor(themeColorHex) }
    
    // Step 4 states remembered at the top level so they are not lost on back/forward
    val checklist = remember {
        mutableStateListOf(
            // Expense Categories
            OnboardingCategory(id = "1", name = "Food", iconName = "dining", budget = 0.0, subcategories = listOf("Groceries", "Restaurants", "Coffee"), type = "EXPENSE"),
            OnboardingCategory(id = "2", name = "Transportation", iconName = "car", budget = 0.0, subcategories = listOf("Fuel", "Transit", "Taxi"), type = "EXPENSE"),
            OnboardingCategory(id = "3", name = "Health", iconName = "hospital", budget = 0.0, subcategories = listOf("Medicine", "Doctor"), type = "EXPENSE"),
            OnboardingCategory(id = "4", name = "Housing / Rent", iconName = "house", budget = 0.0, subcategories = listOf("Apartment Rent", "Maintenance"), type = "EXPENSE"),
            OnboardingCategory(id = "5", name = "Shopping", iconName = "bag", budget = 0.0, subcategories = listOf("Clothing", "Electronics"), type = "EXPENSE"),
            OnboardingCategory(id = "6", name = "Entertainment", iconName = "play", budget = 0.0, subcategories = listOf("Movies", "Subscriptions"), type = "EXPENSE"),
            OnboardingCategory(id = "7", name = "Bills & Utilities", iconName = "invoice", budget = 0.0, subcategories = listOf("Electricity", "Water", "Internet"), type = "EXPENSE"),
            OnboardingCategory(id = "8", name = "Education", iconName = "book", budget = 0.0, subcategories = listOf("Books", "Tuition"), type = "EXPENSE"),
            OnboardingCategory(id = "9", name = "Fitness", iconName = "gym", budget = 0.0, subcategories = listOf("Gym Membership", "Equipment"), type = "EXPENSE"),
            OnboardingCategory(id = "10", name = "Travel", iconName = "plane", budget = 0.0, subcategories = listOf("Flights", "Hotels"), type = "EXPENSE"),
            // Income Categories
            OnboardingCategory(id = "11", name = "Salary", iconName = "wallet", budget = 0.0, subcategories = listOf("Monthly Pay", "Bonus"), type = "INCOME"),
            OnboardingCategory(id = "12", name = "Business", iconName = "work", budget = 0.0, subcategories = listOf("Client Invoice", "Consulting"), type = "INCOME"),
            OnboardingCategory(id = "13", name = "Gifts", iconName = "gift", budget = 0.0, subcategories = listOf("Presents", "Donations"), type = "INCOME"),
            OnboardingCategory(id = "14", name = "Interest", iconName = "trending_up", budget = 0.0, subcategories = listOf("Dividends", "Savings Interest"), type = "INCOME")
        )
    }
    
    val checkedMap = remember {
        mutableStateMapOf<String, Boolean>().apply {
            checklist.forEach { put(it.id, it.id in listOf("1", "2", "3", "4", "7", "11", "12")) }
        }
    }
    
    var customCatName by remember { mutableStateOf("") }
    
    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
            .padding(16.dp),
        contentAlignment = Alignment.Center
    ) {
        Card(
            modifier = Modifier
                .fillMaxWidth()
                .widthIn(max = 480.dp)
                .testTag("onboarding_card"),
            shape = RoundedCornerShape(24.dp),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
            elevation = CardDefaults.cardElevation(defaultElevation = 6.dp)
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(24.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                // Header standard top indicators + Skip option
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "Step $currentStep of 4",
                        style = MaterialTheme.typography.labelMedium,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.outline
                    )
                    TextButton(
                        onClick = onComplete,
                        modifier = Modifier.testTag("onboarding_skip_btn")
                    ) {
                        Text(
                            text = if (isTour) {
                                "Exit Tour"
                            } else {
                                if (currentStep == 1) "Skip Setup" else "Skip"
                            },
                            fontWeight = FontWeight.ExtraBold,
                            color = themeColor
                        )
                    }
                }
                
                // Unified progress indicator line
                Row(
                    modifier = Modifier.fillMaxWidth().padding(bottom = 8.dp),
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    for (i in 1..4) {
                        val isActive = i <= currentStep
                        Box(
                            modifier = Modifier
                                .weight(1f)
                                .height(6.dp)
                                .clip(RoundedCornerShape(3.dp))
                                .background(if (isActive) themeColor else MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.4f))
                        )
                    }
                }
                
                // Screen content according to stage
                when (currentStep) {
                    1 -> {
                        Spacer(modifier = Modifier.height(12.dp))
                        Icon(
                            imageVector = Icons.Default.TrendingUp,
                            contentDescription = "Welcome Logo",
                            tint = themeColor,
                            modifier = Modifier.size(80.dp)
                        )
                        
                        Text(
                            text = "Shylock",
                            style = MaterialTheme.typography.headlineLarge,
                            fontWeight = FontWeight.Black,
                            color = MaterialTheme.colorScheme.onSurface,
                            textAlign = androidx.compose.ui.text.style.TextAlign.Center
                        )
                        
                        Text(
                            text = "Your visually beautiful personal finance assistant. Track expenses with custom, contrast-validated color categories, budgets, and automated insights.",
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            textAlign = androidx.compose.ui.text.style.TextAlign.Center,
                            lineHeight = 22.sp
                        )
                        
                        Text(
                            text = "Let's set up your spending categories and customize your visual budget layout to get started.",
                            style = MaterialTheme.typography.bodyMedium,
                            fontWeight = FontWeight.Bold,
                            color = themeColor,
                            textAlign = androidx.compose.ui.text.style.TextAlign.Center,
                            lineHeight = 20.sp
                        )
                        
                        Spacer(modifier = Modifier.height(16.dp))
                        
                        Button(
                            onClick = { currentStep = 2 },
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(52.dp)
                                .testTag("onboarding_next_btn1"),
                            colors = ButtonDefaults.buttonColors(containerColor = themeColor),
                            shape = RoundedCornerShape(14.dp)
                        ) {
                            Text("Get started", fontSize = 16.sp, fontWeight = FontWeight.Bold)
                            Spacer(modifier = Modifier.width(8.dp))
                            Icon(imageVector = Icons.Default.ArrowForward, contentDescription = "Next")
                        }
                    }
                    
                    2 -> {
                        Text(
                            text = "Select Default Currency",
                            style = MaterialTheme.typography.titleLarge,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                        
                        Text(
                            text = "Choose your preferred currency format. This symbol will be used everywhere throughout Shylock for transaction records, stats, and budget metrics.",
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            textAlign = androidx.compose.ui.text.style.TextAlign.Center,
                            lineHeight = 20.sp
                        )
                        
                        Spacer(modifier = Modifier.height(8.dp))
                        
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            listOf("₹", "$", "€", "£", "¥").forEach { symbol ->
                                val isSelected = symbol == currencySymbol
                                Box(
                                    modifier = Modifier
                                        .weight(1f)
                                        .clip(RoundedCornerShape(10.dp))
                                        .background(if (isSelected) themeColor.copy(alpha = 0.15f) else MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.3f))
                                        .border(
                                            width = if (isSelected) 2.dp else 1.dp,
                                            color = if (isSelected) themeColor else Color.Transparent,
                                            shape = RoundedCornerShape(10.dp)
                                        )
                                        .clickable { viewModel.setCurrency(symbol) }
                                        .padding(vertical = 14.dp),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Text(
                                        text = symbol,
                                        fontWeight = FontWeight.ExtraBold,
                                        fontSize = 18.sp,
                                        color = if (isSelected) themeColor else MaterialTheme.colorScheme.onSurface
                                    )
                                }
                            }
                        }
                        
                        Spacer(modifier = Modifier.height(16.dp))
                        
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(12.dp)
                        ) {
                            OutlinedButton(
                                onClick = { currentStep = 1 },
                                modifier = Modifier.weight(1f).height(48.dp),
                                shape = RoundedCornerShape(12.dp)
                            ) {
                                Text("Back")
                            }
                            
                            Button(
                                onClick = { currentStep = 3 },
                                modifier = Modifier
                                    .weight(1.5f)
                                    .height(48.dp)
                                    .testTag("onboarding_next_btn2"),
                                colors = ButtonDefaults.buttonColors(containerColor = themeColor),
                                shape = RoundedCornerShape(12.dp)
                            ) {
                                Text("Next", fontWeight = FontWeight.Bold)
                                Spacer(modifier = Modifier.width(6.dp))
                                Icon(imageVector = Icons.Default.ArrowForward, contentDescription = "Next Step")
                            }
                        }
                    }
                    
                    3 -> {
                        Text(
                            text = "Style & Appearance",
                            style = MaterialTheme.typography.titleLarge,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                        
                        Text(
                            text = "Make it yours! Select your favorite theme mode and color palette to customize Shylock's flavor.",
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            textAlign = androidx.compose.ui.text.style.TextAlign.Center,
                            lineHeight = 18.sp
                        )
                        
                        // Small aesthetic live preview card
                        Card(
                            modifier = Modifier
                                .fillMaxWidth()
                                .border(1.5.dp, themeColor.copy(alpha = 0.5f), RoundedCornerShape(16.dp)),
                            shape = RoundedCornerShape(16.dp),
                            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.15f))
                        ) {
                            Column(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(14.dp),
                                verticalArrangement = Arrangement.spacedBy(6.dp)
                            ) {
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Row(verticalAlignment = Alignment.CenterVertically) {
                                        Box(
                                            modifier = Modifier
                                                .size(24.dp)
                                                .clip(CircleShape)
                                                .background(themeColor),
                                            contentAlignment = Alignment.Center
                                        ) {
                                            Icon(
                                                imageVector = Icons.Default.Category,
                                                contentDescription = null,
                                                tint = getContrastColor(themeColorHex),
                                                modifier = Modifier.size(12.dp)
                                            )
                                        }
                                        Spacer(modifier = Modifier.width(8.dp))
                                        Text(
                                            text = "Sample Category Preview",
                                            fontWeight = FontWeight.Bold,
                                            style = MaterialTheme.typography.bodySmall,
                                            color = MaterialTheme.colorScheme.onSurface
                                        )
                                    }
                                }
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween
                                ) {
                                    Text(
                                        text = "Spent Today: $currencySymbol 45.90",
                                        style = MaterialTheme.typography.labelSmall,
                                        color = MaterialTheme.colorScheme.onSurface
                                    )
                                    Text(
                                        text = "Target Limit: $currencySymbol 100",
                                        style = MaterialTheme.typography.labelSmall,
                                        color = MaterialTheme.colorScheme.outline
                                    )
                                }
                                LinearProgressIndicator(
                                    progress = { 0.46f },
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .height(6.dp)
                                        .clip(RoundedCornerShape(3.dp)),
                                    color = themeColor,
                                    trackColor = themeColor.copy(alpha = 0.12f)
                                )
                            }
                        }
                        
                        Column(
                            modifier = Modifier.fillMaxWidth(),
                            verticalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            // Light / Dark mode mode selector
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.spacedBy(6.dp)
                            ) {
                                listOf("SYSTEM" to "System", "LIGHT" to "Light", "DARK" to "Dark").forEach { (modeVal, label) ->
                                    val isSelected = themeMode == modeVal
                                    Box(
                                        modifier = Modifier
                                            .weight(1f)
                                            .clip(RoundedCornerShape(8.dp))
                                            .background(
                                                if (isSelected) themeColor
                                                else MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.4f)
                                            )
                                            .border(
                                                width = 1.dp,
                                                color = if (isSelected) themeColor else MaterialTheme.colorScheme.outline.copy(alpha = 0.15f),
                                                shape = RoundedCornerShape(8.dp)
                                            )
                                            .clickable { viewModel.setThemeMode(modeVal) }
                                            .padding(vertical = 8.dp),
                                        contentAlignment = Alignment.Center
                                    ) {
                                        Text(
                                            text = label,
                                            fontWeight = FontWeight.Bold,
                                            fontSize = 12.sp,
                                            color = if (isSelected) MaterialTheme.colorScheme.onPrimary else MaterialTheme.colorScheme.onSurface
                                        )
                                    }
                                }
                            }
                            
                            // Scrollable or listing palette schemes
                            Column(
                                modifier = Modifier.fillMaxWidth(),
                                verticalArrangement = Arrangement.spacedBy(6.dp)
                            ) {
                                PaletteTheme.values().forEach { palette ->
                                    val isSelected = palette == currentTheme
                                    val paletteColors = viewModel.palettes[palette] ?: emptyList()
                                    
                                    Row(
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .clip(RoundedCornerShape(10.dp))
                                            .background(if (isSelected) themeColor.copy(alpha = 0.08f) else MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.15f))
                                            .border(
                                                width = if (isSelected) 1.5.dp else 1.dp,
                                                color = if (isSelected) themeColor else MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.4f),
                                                shape = RoundedCornerShape(10.dp)
                                            )
                                            .clickable { viewModel.setPaletteTheme(palette) }
                                            .padding(horizontal = 12.dp, vertical = 10.dp),
                                        verticalAlignment = Alignment.CenterVertically,
                                        horizontalArrangement = Arrangement.SpaceBetween
                                    ) {
                                        Column(modifier = Modifier.weight(1f)) {
                                            Text(
                                                text = palette.name.lowercase().replaceFirstChar { it.uppercase() },
                                                fontWeight = FontWeight.Bold,
                                                fontSize = 13.sp,
                                                color = MaterialTheme.colorScheme.onSurface
                                            )
                                            Spacer(modifier = Modifier.height(6.dp))
                                            Row(
                                                horizontalArrangement = Arrangement.spacedBy(4.dp)
                                            ) {
                                                paletteColors.take(6).forEach { colorString ->
                                                    Box(
                                                        modifier = Modifier
                                                            .size(16.dp)
                                                            .clip(RoundedCornerShape(4.dp))
                                                            .background(parseHexColor(colorString))
                                                    )
                                                }
                                            }
                                        }
                                        
                                        RadioButton(
                                            selected = isSelected,
                                            onClick = { viewModel.setPaletteTheme(palette) },
                                            colors = RadioButtonDefaults.colors(selectedColor = themeColor)
                                        )
                                    }
                                }
                            }
                        }
                        
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(12.dp)
                        ) {
                            OutlinedButton(
                                onClick = { currentStep = 2 },
                                modifier = Modifier.weight(1f).height(48.dp),
                                shape = RoundedCornerShape(12.dp)
                            ) {
                                Text("Back")
                            }
                            
                            Button(
                                onClick = { currentStep = 4 },
                                modifier = Modifier
                                    .weight(1.5f)
                                    .height(48.dp)
                                    .testTag("onboarding_next_btn3"),
                                colors = ButtonDefaults.buttonColors(containerColor = themeColor),
                                shape = RoundedCornerShape(12.dp)
                            ) {
                                Text("Next", fontWeight = FontWeight.Bold)
                                Spacer(modifier = Modifier.width(6.dp))
                                Icon(imageVector = Icons.Default.ArrowForward, contentDescription = "Next Step")
                            }
                        }
                    }
                    
                    4 -> {
                        var onboardingTypeTab by remember { mutableStateOf("EXPENSE") }
                        val expenseSelectedCount = checklist.count { it.type == "EXPENSE" && checkedMap[it.id] == true }
                        val incomeSelectedCount = checklist.count { it.type == "INCOME" && checkedMap[it.id] == true }
                        val isIncomeTab = onboardingTypeTab == "INCOME"
                        val activeTabColor = if (isIncomeTab) Color(0xFF2E7D32) else themeColor

                        Text(
                            text = "Set Up Your Categories",
                            style = MaterialTheme.typography.titleLarge,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                        
                        Text(
                            text = "Choose your starting budget categories. Uncheck any category to exclude it, or add custom ones.",
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            textAlign = androidx.compose.ui.text.style.TextAlign.Center,
                            lineHeight = 16.sp
                        )

                        // Expense vs Income Tabs
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clip(RoundedCornerShape(12.dp))
                                .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.4f))
                                .padding(3.dp),
                            horizontalArrangement = Arrangement.spacedBy(4.dp)
                        ) {
                            listOf(
                                "EXPENSE" to "Expenses ($expenseSelectedCount)",
                                "INCOME" to "Income ($incomeSelectedCount)"
                            ).forEach { (typeVal, label) ->
                                val isSelected = onboardingTypeTab == typeVal
                                val tabColor = if (typeVal == "INCOME") Color(0xFF2E7D32) else themeColor
                                Box(
                                    modifier = Modifier
                                        .weight(1f)
                                        .clip(RoundedCornerShape(10.dp))
                                        .background(if (isSelected) tabColor.copy(alpha = 0.15f) else Color.Transparent)
                                        .border(
                                            width = if (isSelected) 1.5.dp else 0.dp,
                                            color = if (isSelected) tabColor else Color.Transparent,
                                            shape = RoundedCornerShape(10.dp)
                                        )
                                        .clickable { onboardingTypeTab = typeVal }
                                        .padding(vertical = 8.dp),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Text(
                                        text = label,
                                        fontSize = 12.sp,
                                        fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium,
                                        color = if (isSelected) tabColor else MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                }
                            }
                        }
                        
                        // Checklist column filtered by active tab
                        val displayedChecklist = checklist.filter { it.type == onboardingTypeTab }
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .heightIn(max = 210.dp)
                                .verticalScroll(rememberScrollState()),
                            verticalArrangement = Arrangement.spacedBy(6.dp)
                        ) {
                            displayedChecklist.forEachIndexed { index, cat ->
                                val catChecked = checkedMap[cat.id] ?: true
                                val colorsList = viewModel.palettes[currentTheme] ?: listOf("#4D96FF")
                                val catColorStr = if (cat.isCustom) {
                                    cat.customColorHex ?: (colorsList.getOrNull((index + checklist.size) % colorsList.size) ?: colorsList.first())
                                } else {
                                    colorsList.getOrNull(index % colorsList.size) ?: colorsList.first()
                                }
                                val catColor = parseHexColor(catColorStr)
                                
                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .clip(RoundedCornerShape(8.dp))
                                        .clickable { checkedMap[cat.id] = !catChecked }
                                        .padding(horizontal = 4.dp, vertical = 2.dp),
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.SpaceBetween
                                ) {
                                    Row(
                                        verticalAlignment = Alignment.CenterVertically,
                                        modifier = Modifier.weight(1f)
                                    ) {
                                        Checkbox(
                                            checked = catChecked,
                                            onCheckedChange = { checkedMap[cat.id] = it },
                                            colors = CheckboxDefaults.colors(checkedColor = activeTabColor)
                                        )
                                        Spacer(modifier = Modifier.width(4.dp))
                                        Box(
                                            modifier = Modifier
                                                .size(28.dp)
                                                .clip(CircleShape)
                                                .background(catColor),
                                            contentAlignment = Alignment.Center
                                        ) {
                                            Icon(
                                                imageVector = getIconVector(cat.iconName),
                                                contentDescription = null,
                                                tint = getContrastColor(catColorStr),
                                                modifier = Modifier.size(14.dp)
                                            )
                                        }
                                        Spacer(modifier = Modifier.width(8.dp))
                                        Column {
                                            Text(
                                                text = cat.name,
                                                fontWeight = FontWeight.Bold,
                                                style = MaterialTheme.typography.bodyMedium,
                                                color = MaterialTheme.colorScheme.onSurface
                                            )
                                            if (cat.subcategories.isNotEmpty()) {
                                                Text(
                                                    text = cat.subcategories.joinToString(", "),
                                                    style = MaterialTheme.typography.labelSmall,
                                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                                )
                                            }
                                        }
                                    }
                                }
                            }
                        }
                        
                        // Quick textfield to add a custom category
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            OutlinedTextField(
                                value = customCatName,
                                onValueChange = { customCatName = it },
                                placeholder = {
                                    Text(
                                        if (isIncomeTab) "E.g., Freelance, Dividends" else "E.g., Subscription, Gym",
                                        fontSize = 13.sp
                                    )
                                },
                                modifier = Modifier.weight(1f),
                                singleLine = true,
                                textStyle = MaterialTheme.typography.bodyMedium.copy(fontSize = 14.sp),
                                colors = OutlinedTextFieldDefaults.colors(
                                    focusedBorderColor = activeTabColor,
                                    cursorColor = activeTabColor
                                )
                            )
                            IconButton(
                                onClick = {
                                    if (customCatName.isNotBlank()) {
                                        val trimmedName = customCatName.trim()
                                        if (!checklist.any { it.name.equals(trimmedName, ignoreCase = true) }) {
                                            val newCat = OnboardingCategory(
                                                id = java.util.UUID.randomUUID().toString(),
                                                name = trimmedName,
                                                iconName = if (isIncomeTab) "wallet" else "category",
                                                budget = 0.0,
                                                subcategories = emptyList(),
                                                isCustom = true,
                                                type = onboardingTypeTab
                                            )
                                            checklist.add(newCat)
                                            checkedMap[newCat.id] = true
                                        }
                                        customCatName = ""
                                    }
                                },
                                modifier = Modifier
                                    .size(48.dp)
                                    .background(activeTabColor, RoundedCornerShape(10.dp)),
                                colors = IconButtonDefaults.iconButtonColors(contentColor = Color.White)
                            ) {
                                Icon(imageVector = Icons.Default.Add, contentDescription = "Add custom category")
                            }
                        }
                        
                        Spacer(modifier = Modifier.height(4.dp))
                        
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(12.dp)
                        ) {
                            OutlinedButton(
                                onClick = { currentStep = 3 },
                                modifier = Modifier.weight(1f).height(48.dp),
                                shape = RoundedCornerShape(12.dp)
                            ) {
                                Text("Back")
                            }
                            
                            Button(
                                onClick = {
                                    if (!isTour) {
                                        val colorsList = viewModel.palettes[currentTheme] ?: listOf("#4D96FF")
                                        val checkedItems = checklist.filter { checkedMap[it.id] == true }
                                        checkedItems.forEachIndexed { index, cat ->
                                            val colorStr = if (cat.isCustom) {
                                                cat.customColorHex ?: (colorsList.getOrNull((index + checklist.size) % colorsList.size) ?: colorsList.first())
                                            } else {
                                                colorsList.getOrNull(index % colorsList.size) ?: colorsList.first()
                                            }
                                            viewModel.addCategory(
                                                name = cat.name,
                                                colorHex = colorStr,
                                                iconName = cat.iconName,
                                                budget = cat.budget,
                                                subcategoriesList = cat.subcategories,
                                                type = cat.type
                                            )
                                        }
                                    }
                                    onComplete()
                                },
                                modifier = Modifier
                                    .weight(1.5f)
                                    .height(48.dp)
                                    .testTag("onboarding_finish_btn"),
                                colors = ButtonDefaults.buttonColors(containerColor = themeColor),
                                shape = RoundedCornerShape(12.dp)
                            ) {
                                Text("Let's Track!", fontWeight = FontWeight.Bold)
                                Spacer(modifier = Modifier.width(6.dp))
                                Icon(imageVector = Icons.Default.Check, contentDescription = "Finished")
                            }
                        }
                    }
                }
            }
        }
    }
}

