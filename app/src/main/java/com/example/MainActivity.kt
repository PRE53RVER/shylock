package com.example

import android.Manifest
import android.content.Intent
import android.os.Build
import android.os.Bundle
import android.provider.Settings
import androidx.core.content.ContextCompat
import android.content.pm.PackageManager
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
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material.icons.outlined.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.lerp
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
import androidx.compose.ui.unit.TextUnit
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.res.painterResource
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.compose.LifecycleResumeEffect
import androidx.navigation.compose.rememberNavController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import com.example.data.model.Category
import com.example.data.model.DetectedPayment
import com.example.data.model.Subcategory
import com.example.data.model.Transaction
import com.example.data.model.displayName
import com.example.ui.screens.CategoryEditorDialog
import com.example.ui.screens.ContactDetailScreen
import com.example.ui.screens.LendingListScreen
import com.example.ui.screens.LendingSummaryCard
import com.example.ui.screens.MoneyInboxScreen
import com.example.ui.screens.PendingBadge
import com.example.inbox.MoneyInboxNotifications
import com.example.inbox.MoneyInboxSettings
import com.example.ui.screens.OnboardingWizard
import com.example.ui.screens.ShylockCurrencies
import com.example.ui.theme.MyApplicationTheme
import com.example.ui.theme.AppBackground
import com.example.ui.theme.AppTheme
import com.example.ui.theme.GlassCard
import com.example.ui.theme.Typography
import com.example.ui.theme.accentGlow
import com.example.ui.theme.glassShadowColor
import com.example.ui.theme.liquidGlass
import com.example.ui.theme.selectedPillBrush
import com.example.ui.theme.selectedPillRim
import com.example.ui.theme.softShadow
import androidx.compose.ui.graphics.luminance
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.withStyle
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
import androidx.compose.material.icons.automirrored.filled.TrendingUp
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import java.util.*

class MainActivity : ComponentActivity() {
    private val viewModel: CategoryViewModel by viewModels()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        MoneyInboxNotifications.ensureChannels(this)
        MoneyInboxNotifications.syncDailyReminder(this)
        handleInboxIntent(intent)
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

    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        setIntent(intent)
        handleInboxIntent(intent)
    }

    // A tap on a "Payment detected" / daily reminder notification lands on the Money Inbox tab
    private fun handleInboxIntent(intent: Intent?) {
        if (intent?.getBooleanExtra(MoneyInboxNotifications.EXTRA_OPEN_INBOX, false) == true) {
            intent.removeExtra(MoneyInboxNotifications.EXTRA_OPEN_INBOX)
            viewModel.requestOpenInbox()
        }
    }
}

// Plain number for the amount input: "500" rather than "500.0", two decimals when needed
fun formatAmountInput(amount: Double): String =
    if (amount % 1.0 == 0.0) amount.toLong().toString() else String.format(Locale.US, "%.2f", amount)

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

// Resolves a persisted icon key (see CategoryIcons.kt) to a glyph, falling back to the generic tag
fun getIconVector(name: String): ImageVector = findCategoryIcon(name) ?: Icons.Default.Category

// Name-based guess for subcategories that never had an icon assigned
fun getSubcategoryIcon(name: String): ImageVector =
    findCategoryIcon(guessSubcategoryIconName(name)) ?: Icons.Default.Category

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

    // Money Inbox state
    val detectedPayments by viewModel.detectedPayments.collectAsStateWithLifecycle()
    val pendingInboxCount by viewModel.pendingInboxCount.collectAsStateWithLifecycle()
    val paymentDetectionEnabled by viewModel.paymentDetectionEnabled.collectAsStateWithLifecycle()
    val paymentAlertsEnabled by viewModel.paymentAlertsEnabled.collectAsStateWithLifecycle()
    val dailyReviewReminderEnabled by viewModel.dailyReviewReminderEnabled.collectAsStateWithLifecycle()
    val openInboxRequest by viewModel.openInboxRequest.collectAsStateWithLifecycle()
    // Notification-listener access is granted in system settings, so re-check whenever we resume
    var hasNotificationAccess by remember { mutableStateOf(MoneyInboxSettings.hasNotificationAccess(context)) }
    LifecycleResumeEffect(Unit) {
        hasNotificationAccess = MoneyInboxSettings.hasNotificationAccess(context)
        onPauseOrDispose { }
    }
    val notificationPermissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestPermission()
    ) { /* Alerts silently stay off at the OS level if declined; the toggle itself is kept */ }
    val requestPostNotifications: () -> Unit = {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU &&
            ContextCompat.checkSelfPermission(context, Manifest.permission.POST_NOTIFICATIONS) != PackageManager.PERMISSION_GRANTED
        ) {
            notificationPermissionLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
        }
    }
    val openNotificationAccessSettings: () -> Unit = {
        try {
            context.startActivity(Intent(Settings.ACTION_NOTIFICATION_LISTENER_SETTINGS).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK))
        } catch (e: Exception) {
            Toast.makeText(context, "Open Settings > Notifications > Notification access to allow SHYLOCK", Toast.LENGTH_LONG).show()
        }
    }
    // Turning detection on also walks the user to the system screen where access is granted
    val enablePaymentDetection: () -> Unit = {
        viewModel.setPaymentDetectionEnabled(true)
        requestPostNotifications()
        if (!MoneyInboxSettings.hasNotificationAccess(context)) openNotificationAccessSettings()
    }

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

    // Notification taps ask for the inbox; honour it once the nav graph is ready
    LaunchedEffect(openInboxRequest, onboardingComplete) {
        if (openInboxRequest && onboardingComplete) {
            navController.navigate("inbox") {
                popUpTo("home") { saveState = true }
                launchSingleTop = true
                restoreState = true
            }
            viewModel.consumeOpenInboxRequest()
        }
    }

    // Reset past month unlock state when navigating to another month or tab
    LaunchedEffect(selectedMonth) {
        isPastMonthUnlocked = false
    }
    LaunchedEffect(currentRoute) {
        if (currentRoute != "home") {
            isPastMonthUnlocked = false
        }
        // The category highlight belongs to Insights only; drop it when leaving that tab
        if (currentRoute != "insights") {
            viewModel.clearCategorySelection()
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
    var inboxDraftTarget by remember { mutableStateOf<DetectedPayment?>(null) }
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
    val isOnboardingScreen = currentRoute == "onboarding" || currentRoute == "onboarding_tour"

    Scaffold(
        containerColor = Color.Transparent,
        contentColor = MaterialTheme.colorScheme.onBackground,
        snackbarHost = { SnackbarHost(snackbarHostState) },
        topBar = {
            // Home, Insights and Settings host their own headers inside the scroll body so they can
            // scroll away and hand the whole viewport back to content; the other tabs keep a
            // lightweight title.
            if (!isOnboardingScreen && currentRoute != "home" && currentRoute != "inbox" && currentRoute != "insights" && currentRoute != "settings" && !isLendingScreen) {
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
            if (!isOnboardingScreen) {
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
                                    elevation = 10.dp
                                )
                                .size(52.dp)
                                .testTag("add_transaction_btn")
                        ) {
                            Icon(
                                imageVector = Icons.Default.Add,
                                contentDescription = "Add New Record",
                                modifier = Modifier.size(26.dp)
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
            if (!isOnboardingScreen && !isLendingScreen) {
                val navShape = RoundedCornerShape(26.dp)
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .navigationBarsPadding()
                        .padding(start = 16.dp, end = 16.dp, top = 8.dp, bottom = 6.dp)
                        .liquidGlass(shape = navShape, elevation = 10.dp)
                ) {
                    // NavigationBar normally absorbs the gesture-bar inset inside its own height,
                    // which squashed the items against the top rim. Insets are handled by the
                    // outer Box above, so the whole 68dp here is content and items centre properly.
                    NavigationBar(
                        containerColor = Color.Transparent,
                        tonalElevation = 0.dp,
                        windowInsets = WindowInsets(0.dp),
                        modifier = Modifier.height(68.dp)
                    ) {
                        val navItems = listOf(
                            Triple("home", Icons.Default.Home, "Home"),
                            Triple("inbox", Icons.Default.Email, "Money Inbox"),
                            Triple("insights", Icons.Default.BarChart, "Insights"),
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
                                icon = {
                                    if (route == "inbox" && pendingInboxCount > 0) {
                                        BadgedBox(
                                            badge = {
                                                PendingBadge(
                                                    count = pendingInboxCount,
                                                    compact = true,
                                                    modifier = Modifier.testTag("nav_inbox_badge")
                                                )
                                            }
                                        ) {
                                            Icon(imageVector = icon, contentDescription = label)
                                        }
                                    } else {
                                        Icon(imageVector = icon, contentDescription = label)
                                    }
                                },
                                label = {
                                    Text(
                                        label,
                                        fontWeight = if (selected) FontWeight.Bold else FontWeight.Medium,
                                        fontSize = 11.sp
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
                        .padding(start = 16.dp, end = 16.dp, top = 2.dp, bottom = 84.dp),
                    verticalArrangement = Arrangement.spacedBy(10.dp)
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
                            shape = RoundedCornerShape(24.dp),
                            elevation = 6.dp,
                            contentPadding = PaddingValues(top = 14.dp, bottom = 10.dp)
                        ) {
                            Column {
                                // Header row with Title and "View All" toggle button
                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(horizontal = 16.dp),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Column {
                                        Text(
                                            text = "Recent Transactions",
                                            style = MaterialTheme.typography.titleLarge,
                                            fontWeight = FontWeight.ExtraBold,
                                            fontSize = 18.sp,
                                            color = MaterialTheme.colorScheme.onSurface
                                        )
                                        Text(
                                            text = "${monthTransactions.size} total this month",
                                            style = MaterialTheme.typography.labelSmall,
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
                                            fontSize = 13.sp,
                                            color = MaterialTheme.colorScheme.primary
                                        )
                                        Spacer(modifier = Modifier.width(2.dp))
                                        Icon(
                                            imageVector = if (isViewAllTransactions) Icons.Default.ExpandLess else Icons.Default.ExpandMore,
                                            contentDescription = null,
                                            modifier = Modifier.size(16.dp),
                                            tint = MaterialTheme.colorScheme.primary
                                        )
                                    }
                                }

                                Spacer(modifier = Modifier.height(10.dp))

                                val filtersActive = selectedTypeFilter != "ALL" || searchQuery.isNotBlank()

                                // Search pill + filter toggle, side by side
                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(horizontal = 16.dp),
                                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    GlassSearchField(
                                        value = searchQuery,
                                        onValueChange = { searchQuery = it },
                                        modifier = Modifier
                                            .weight(1f)
                                            .testTag("transaction_search_input")
                                    )

                                    val filterShape = RoundedCornerShape(13.dp)
                                    Box(
                                        modifier = Modifier
                                            .size(38.dp)
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
                                            modifier = Modifier.size(19.dp)
                                        )
                                        if (filtersActive) {
                                            Box(
                                                modifier = Modifier
                                                    .align(Alignment.TopEnd)
                                                    .padding(top = 7.dp, end = 7.dp)
                                                    .size(6.dp)
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
                                            .padding(horizontal = 16.dp)
                                            .padding(top = 8.dp),
                                        horizontalArrangement = Arrangement.spacedBy(8.dp)
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

                                Spacer(modifier = Modifier.height(10.dp))

                                // Transaction Filtering based on searchQuery and selectedTypeFilter within the selected month
                                val filteredTransactions = remember(monthTransactions, searchQuery, selectedTypeFilter) {
                                    var list = monthTransactions
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
                                            .padding(vertical = 28.dp, horizontal = 16.dp),
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
                                            text = "No records match search & filter.",
                                            style = MaterialTheme.typography.bodyMedium,
                                            color = MaterialTheme.colorScheme.outline,
                                            textAlign = TextAlign.Center
                                        )
                                    }
                                } else {
                                    // Divider-separated rows inside one faint tray, so the list reads
                                    // as a single grouped surface rather than a stack of nested cards
                                    val trayShape = RoundedCornerShape(18.dp)
                                    Column(
                                        modifier = Modifier
                                            .padding(horizontal = 10.dp)
                                            .clip(trayShape)
                                            .background(MaterialTheme.colorScheme.surface.copy(alpha = 0.22f))
                                            .border(0.6.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.22f), trayShape)
                                            .animateContentSize()
                                    ) {
                                        displayedTransactions.forEachIndexed { index, tx ->
                                            val cat = categories.find { it.id == tx.categoryId }
                                            val sub = subcategories.find { it.id == tx.subcategoryId }
                                            val isLinked = tx.lendingEntryId != null
                                            if (index > 0) {
                                                HorizontalDivider(
                                                    modifier = Modifier.padding(horizontal = 12.dp),
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

        // === Destination: Money Inbox (payments detected from notifications) ===
        composable("inbox") {
            MoneyInboxScreen(
                payments = detectedPayments,
                currencySymbol = currencySymbol,
                detectionEnabled = paymentDetectionEnabled,
                hasNotificationAccess = hasNotificationAccess,
                onRecord = { payment -> inboxDraftTarget = payment },
                onDismiss = { payment ->
                    viewModel.dismissDetectedPayment(payment)
                    scope.launch {
                        val result = snackbarHostState.showSnackbar(
                            message = "Payment dismissed",
                            actionLabel = "Undo",
                            duration = SnackbarDuration.Short
                        )
                        if (result == SnackbarResult.ActionPerformed) {
                            viewModel.restoreDetectedPayment(payment)
                        }
                    }
                },
                onEnableDetection = enablePaymentDetection,
                onGrantAccess = openNotificationAccessSettings,
                onOpenSettings = {
                    navController.navigate("settings") {
                        popUpTo("home") { saveState = true }
                        launchSingleTop = true
                        restoreState = true
                    }
                }
            )
        }

        composable("insights") {
            val monthFormatter = remember { DateTimeFormatter.ofPattern("MMMM yyyy", Locale.getDefault()) }
            val lastPeriodSpend = remember(periodStats) { periodStats.values.sumOf { it.previousMonthTotal } }
            val periodSparkline = remember(filteredTrendTransactions, selectedAnalysisPeriod, selectedMonth) {
                buildPeriodSparkline(filteredTrendTransactions, selectedAnalysisPeriod, selectedMonth)
            }
            val topCategory = remember(periodStats, categories) {
                periodStats.values
                    .filter { it.totalAmount > 0.0 }
                    .maxByOrNull { it.totalAmount }
                    ?.let { top -> categories.find { it.id == top.categoryId }?.let { cat -> cat to top } }
            }

            // A highlighted category that has no spend in the newly chosen period is no longer
            // drawn in the charts or list, so drop the highlight rather than show an empty panel
            LaunchedEffect(periodStats, selectedCatId) {
                val catId = selectedCatId ?: return@LaunchedEffect
                if ((periodStats[catId]?.totalAmount ?: 0.0) <= 0.0) {
                    viewModel.clearCategorySelection()
                }
            }

            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .verticalScroll(rememberScrollState())
                    .padding(start = 16.dp, end = 16.dp, top = 2.dp, bottom = 24.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                // Wordmark + chart-type shortcut, scrolls away with the content like Home
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("insights_brand_header"),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    ShylockWordmark()
                    Box(
                        modifier = Modifier
                            .size(40.dp)
                            .liquidGlass(shape = CircleShape, strength = 0.9f)
                            .clickable { selectedChartTab = (selectedChartTab + 1) % 3 }
                            .testTag("insights_chart_cycle_btn"),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = Icons.Default.BarChart,
                            contentDescription = "Switch chart type",
                            tint = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.size(22.dp)
                        )
                    }
                }

                // Page title with the compact month picker on the trailing edge
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = "Insights",
                            fontSize = 28.sp,
                            lineHeight = 32.sp,
                            fontWeight = FontWeight.Black,
                            letterSpacing = (-0.5).sp,
                            color = MaterialTheme.colorScheme.onBackground
                        )
                        Text(
                            text = "Understand your spending better",
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                    Spacer(modifier = Modifier.width(12.dp))
                    MonthPickerPill(
                        selectedMonth = selectedMonth,
                        onClick = { showMonthPickerSheet = true },
                        modifier = Modifier.testTag("insights_month_navigation_bar")
                    )
                }

                // Past month indicator banner if viewing a past month
                if (isPastCalendarMonth) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .liquidGlass(shape = RoundedCornerShape(14.dp), tint = MaterialTheme.colorScheme.secondary, strength = 0.8f)
                            .padding(horizontal = 14.dp, vertical = 9.dp)
                            .testTag("insights_past_month_banner"),
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
                            color = MaterialTheme.colorScheme.onSurface
                        )
                    }
                }

                // Hero spending summary for the selected period
                InsightsSummaryCard(
                    period = selectedAnalysisPeriod,
                    selectedMonth = selectedMonth,
                    isCurrentMonth = isCurrentCalendarMonth,
                    totalSpend = totalPeriodSpend,
                    lastPeriodSpend = lastPeriodSpend,
                    currencySymbol = currencySymbol,
                    sparkline = periodSparkline
                )

                // Time Period Selector
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "Analysis Period",
                        style = MaterialTheme.typography.titleMedium,
                        fontSize = 15.sp,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onBackground
                    )
                    Spacer(modifier = Modifier.width(12.dp))
                    GlassSegmentedControl(
                        options = AnalysisPeriod.values().map { it.name.lowercase().replaceFirstChar { c -> c.uppercase() } },
                        selectedIndex = AnalysisPeriod.values().indexOf(selectedAnalysisPeriod),
                        onSelect = { selectedAnalysisPeriod = AnalysisPeriod.values()[it] },
                        expand = true,
                        height = 36.dp,
                        modifier = Modifier
                            .weight(1f)
                            .testTag("analysis_period_selector")
                    )
                }

                // Spending breakdown: chart switcher, chart, per-category list, tip
                GlassCard(
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("analytics_container_card"),
                    shape = RoundedCornerShape(24.dp),
                    elevation = 8.dp,
                    contentPadding = PaddingValues(horizontal = 14.dp, vertical = 14.dp)
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = "Spending Breakdown",
                                style = MaterialTheme.typography.titleMedium,
                                fontSize = 16.sp,
                                lineHeight = 20.sp,
                                fontWeight = FontWeight.ExtraBold,
                                color = MaterialTheme.colorScheme.onSurface
                            )
                            Text(
                                text = "Your expenses by category",
                                style = MaterialTheme.typography.labelMedium,
                                fontWeight = FontWeight.Normal,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                        Spacer(modifier = Modifier.width(10.dp))
                        GlassSegmentedControl(
                            options = listOf("Donut", "Bar", "Trend"),
                            selectedIndex = selectedChartTab,
                            onSelect = { selectedChartTab = it },
                            height = 32.dp,
                            fontSize = 11.sp,
                            modifier = Modifier.testTag("chart_type_selector")
                        )
                    }

                    Spacer(modifier = Modifier.height(6.dp))

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
                                caption = when (selectedAnalysisPeriod) {
                                    AnalysisPeriod.WEEKLY -> "Last 7 days"
                                    AnalysisPeriod.MONTHLY -> if (isCurrentCalendarMonth) "This Month" else selectedMonth.format(monthFormatter)
                                    AnalysisPeriod.YEARLY -> "Last 12 months"
                                },
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

                    Spacer(modifier = Modifier.height(10.dp))

                    // Per-category rows double as the chart legend and highlight toggle
                    CategoryBreakdownList(
                        categories = categories,
                        stats = periodStats,
                        selectedCatId = selectedCatId,
                        currencySymbol = currencySymbol,
                        onRowClick = { viewModel.toggleCategorySelection(it) }
                    )

                    Spacer(modifier = Modifier.height(10.dp))

                    InsightTipCard(
                        topCategory = topCategory?.first,
                        topShare = topCategory?.second?.percentage ?: 0.0,
                        period = selectedAnalysisPeriod,
                        onClick = { navController.navigate("categories") }
                    )
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
            // Pickers live in bottom sheets so the main list stays a compact set of rows
            var showAppThemeSheet by remember { mutableStateOf(false) }
            var showPaletteSheet by remember { mutableStateOf(false) }
            var showCurrencySheet by remember { mutableStateOf(false) }
            var showTypeSheet by remember { mutableStateOf(false) }
            var showAboutSheet by remember { mutableStateOf(false) }

            val currencyOptions = remember { ShylockCurrencies.map { it.symbol to it.name } }
            val currencyLabel = currencyOptions.firstOrNull { it.first == currencySymbol }
                ?.let { "${it.second} (${it.first})" } ?: currencySymbol
            val paletteLabel = currentTheme.name.lowercase().replaceFirstChar { it.uppercase() }
            val typeLabel = if (defaultType == "INCOME") "Income" else "Expense"
            val themeModeIndex = when (themeMode) {
                "LIGHT" -> 1
                "DARK" -> 2
                else -> 0
            }
            val isDarkTheme = when (themeMode) {
                "LIGHT" -> false
                "DARK" -> true
                else -> isSystemInDarkTheme()
            }

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
                    .padding(start = 16.dp, end = 16.dp, top = 2.dp, bottom = 24.dp),
                verticalArrangement = Arrangement.spacedBy(14.dp)
            ) {
                // Page header — scrolls away with the content like Home and Insights
                SettingsHeader(modifier = Modifier.testTag("settings_header"))

                // Help & Onboarding — standalone highlighted card
                SettingsGroupCard(elevation = 6.dp) {
                    SettingsRow(
                        title = "Help & Onboarding",
                        subtitle = "Re-run the setup guide anytime to explore features",
                        onClick = { navController.navigate("onboarding_tour") },
                        modifier = Modifier.testTag("onboarding_tour_row"),
                        leading = { SettingsTileIcon(Icons.Default.AutoAwesome, "Help & Tour") },
                        trailing = { SettingsChevron() }
                    )
                }

                // ---------- Appearance ----------
                SettingsSectionHeader(
                    title = "Appearance",
                    subtitle = "Customize how Shylock looks and feels",
                    icon = { Icon(imageVector = Icons.Default.Palette, contentDescription = null, tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(30.dp)) }
                )
                SettingsGroupCard {
                    val themeModeControl: @Composable (Boolean) -> Unit = { expand ->
                        GlassSegmentedControl(
                            options = listOf("System", "Light", "Dark"),
                            selectedIndex = themeModeIndex,
                            onSelect = { index ->
                                val (modeVal, label) = listOf("SYSTEM" to "System", "LIGHT" to "Light", "DARK" to "Dark")[index]
                                if (modeVal != themeMode) {
                                    haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                                    viewModel.setThemeMode(modeVal)
                                    Toast.makeText(context, "Theme mode set to $label", Toast.LENGTH_SHORT).show()
                                }
                            },
                            expand = expand,
                            height = 36.dp,
                            fontSize = 11.sp,
                            modifier = Modifier
                                .then(if (expand) Modifier.fillMaxWidth() else Modifier)
                                .testTag("theme_mode_segmented")
                        )
                    }
                    // The three-way control sits beside the title where the card is wide enough;
                    // on narrow phones it drops below the text so the title is never crushed
                    BoxWithConstraints(modifier = Modifier.fillMaxWidth()) {
                        if (maxWidth >= 350.dp) {
                            SettingsRow(
                                title = "App theme mode",
                                subtitle = "Follow system or choose manually",
                                leading = { SettingsTileIcon(Icons.Default.LightMode, "Theme mode") },
                                trailing = { themeModeControl(false) }
                            )
                        } else {
                            Column {
                                SettingsRow(
                                    title = "App theme mode",
                                    subtitle = "Follow system or choose manually",
                                    leading = { SettingsTileIcon(Icons.Default.LightMode, "Theme mode") }
                                )
                                Box(modifier = Modifier.padding(start = 10.dp, end = 10.dp, bottom = 12.dp)) {
                                    themeModeControl(true)
                                }
                            }
                        }
                    }
                    SettingsDivider()
                    SettingsRow(
                        title = "App Theme",
                        subtitle = "Changes the overall app colors",
                        onClick = { showAppThemeSheet = true },
                        modifier = Modifier.testTag("app_theme_row"),
                        leading = { SettingsTileIcon(Icons.Default.Palette, "App Theme") },
                        trailing = {
                            SettingsValue(currentAppTheme.displayName())
                            SettingsChevron()
                        }
                    )
                    SettingsDivider()
                    SettingsRow(
                        title = "Global Color Palette",
                        subtitle = "Choose a palette for category colors",
                        onClick = { showPaletteSheet = true },
                        modifier = Modifier.testTag("palette_row"),
                        leading = { SettingsTileIcon(Icons.Default.Dashboard, "Palette") },
                        trailing = {
                            SettingsValue(paletteLabel)
                            SettingsChevron()
                        }
                    )
                    SettingsDivider()
                    SettingsRow(
                        title = "Dynamic Wallpaper Color",
                        subtitle = "Tint UI using your Android wallpaper (12+)",
                        leading = { SettingsTileIcon(Icons.Default.Brush, "Dynamic Color") },
                        trailing = {
                            Switch(
                                checked = dynamicColorEnabled,
                                onCheckedChange = {
                                    haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                                    viewModel.setDynamicColor(it)
                                },
                                modifier = Modifier.testTag("dynamic_color_switch")
                            )
                        }
                    )
                }

                // ---------- Currency & Transactions ----------
                SettingsSectionHeader(
                    title = "Currency & Transactions",
                    subtitle = "Set your preferred currency and defaults",
                    icon = { Icon(imageVector = Icons.Default.Paid, contentDescription = null, tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(30.dp)) }
                )
                SettingsGroupCard {
                    SettingsRow(
                        title = "Display Currency Symbol",
                        subtitle = "Choose how amounts are displayed",
                        onClick = { showCurrencySheet = true },
                        modifier = Modifier.testTag("currency_row"),
                        leading = {
                            // The tile shows the live symbol so the row reads at a glance
                            Text(
                                text = currencySymbol,
                                fontSize = 22.sp,
                                fontWeight = FontWeight.Black,
                                color = MaterialTheme.colorScheme.primary
                            )
                        },
                        trailing = {
                            SettingsValue(currencyLabel)
                            SettingsChevron()
                        }
                    )
                    SettingsDivider()
                    SettingsRow(
                        title = "Default Transaction Type",
                        subtitle = "Pre-select type when adding transactions",
                        onClick = { showTypeSheet = true },
                        modifier = Modifier.testTag("default_type_row"),
                        leading = { SettingsTileIcon(Icons.Default.SwapHoriz, "Type") },
                        trailing = {
                            SettingsValue(typeLabel)
                            SettingsChevron()
                        }
                    )
                }

                // ---------- Money Inbox ----------
                SettingsSectionHeader(
                    title = "Money Inbox",
                    subtitle = "Detect payments from bank and UPI notifications",
                    icon = { Icon(imageVector = Icons.Default.Email, contentDescription = null, tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(30.dp)) }
                )
                SettingsGroupCard {
                    SettingsRow(
                        title = "Payment detection",
                        subtitle = if (paymentDetectionEnabled && !hasNotificationAccess) {
                            "On, but notification access is not granted — tap to allow"
                        } else {
                            "Draft detected payments into Money Inbox for review"
                        },
                        onClick = if (paymentDetectionEnabled && !hasNotificationAccess) openNotificationAccessSettings else null,
                        leading = { SettingsTileIcon(Icons.Default.Radar, "Detection") },
                        trailing = {
                            Switch(
                                checked = paymentDetectionEnabled,
                                onCheckedChange = { enabled ->
                                    haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                                    if (enabled) enablePaymentDetection() else viewModel.setPaymentDetectionEnabled(false)
                                },
                                modifier = Modifier.testTag("payment_detection_switch")
                            )
                        }
                    )
                    SettingsDivider()
                    SettingsRow(
                        title = "New payment alerts",
                        subtitle = "Notify me when a payment is detected",
                        leading = { SettingsTileIcon(Icons.Default.NotificationsActive, "Alerts") },
                        trailing = {
                            Switch(
                                checked = paymentAlertsEnabled,
                                enabled = paymentDetectionEnabled,
                                onCheckedChange = { enabled ->
                                    haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                                    viewModel.setPaymentAlertsEnabled(enabled)
                                    if (enabled) requestPostNotifications()
                                },
                                modifier = Modifier.testTag("payment_alerts_switch")
                            )
                        }
                    )
                    SettingsDivider()
                    SettingsRow(
                        title = "Daily review reminder",
                        subtitle = "At 9 PM, remind me if today’s payments are still unrecorded",
                        leading = { SettingsTileIcon(Icons.Default.Schedule, "Reminder") },
                        trailing = {
                            Switch(
                                checked = dailyReviewReminderEnabled,
                                enabled = paymentDetectionEnabled,
                                onCheckedChange = { enabled ->
                                    haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                                    viewModel.setDailyReviewReminderEnabled(enabled)
                                    if (enabled) requestPostNotifications()
                                },
                                modifier = Modifier.testTag("daily_reminder_switch")
                            )
                        }
                    )
                }

                // ---------- Data Management ----------
                SettingsSectionHeader(
                    title = "Data Management",
                    subtitle = "Backup, restore or clear your data",
                    icon = { DatabaseGlyph(modifier = Modifier.size(28.dp)) }
                )
                SettingsGroupCard {
                    SettingsRow(
                        title = "Export Backup (JSON)",
                        subtitle = "Save your data to a local file",
                        onClick = { exportLauncher.launch("shylock_backup.json") },
                        modifier = Modifier.testTag("export_backup_btn"),
                        leading = { SettingsTileIcon(Icons.Default.FileUpload, "Export") },
                        trailing = { SettingsChevron() }
                    )
                    SettingsDivider()
                    SettingsRow(
                        title = "Import Backup (JSON)",
                        subtitle = "Restore your data from a file",
                        onClick = { importLauncher.launch(arrayOf("application/json")) },
                        modifier = Modifier.testTag("import_backup_btn"),
                        leading = { SettingsTileIcon(Icons.Default.FileDownload, "Import") },
                        trailing = { SettingsChevron() }
                    )
                    SettingsDivider()
                    SettingsRow(
                        title = "Reload Sample Sandbox Data",
                        subtitle = "Reset with sample data for testing",
                        onClick = {
                            haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                            viewModel.seedDemoData()
                            Toast.makeText(context, "Sample sandbox data loaded", Toast.LENGTH_SHORT).show()
                        },
                        modifier = Modifier.testTag("reload_sandbox_btn"),
                        leading = { SettingsTileIcon(Icons.Default.Sync, "Sandbox") },
                        trailing = { SettingsChevron() }
                    )
                    Spacer(modifier = Modifier.height(6.dp))
                    // Destructive action: tinted danger row inset inside the card
                    val danger = MaterialTheme.colorScheme.error
                    val dangerShape = RoundedCornerShape(16.dp)
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 4.dp, vertical = 2.dp)
                            .clip(dangerShape)
                            .background(danger.copy(alpha = 0.10f))
                            .border(1.dp, danger.copy(alpha = 0.28f), dangerShape)
                    ) {
                        SettingsRow(
                            title = "Clear All Data",
                            subtitle = "Permanently delete all app data",
                            accent = danger,
                            titleColor = danger,
                            onClick = {
                                haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                                showClearAllDialog = true
                            },
                            modifier = Modifier.testTag("clear_everything_btn"),
                            leading = { SettingsTileIcon(Icons.Default.Delete, "Wipe", tint = danger) },
                            trailing = { SettingsChevron(tint = danger) }
                        )
                    }
                }

                // ---------- Standalone destinations ----------
                SettingsGroupCard {
                    SettingsRow(
                        title = "Categories & Budgets",
                        subtitle = "Manage your categories, colors and budget limits",
                        onClick = { navController.navigate("categories") },
                        modifier = Modifier.testTag("categories_row"),
                        leading = { SettingsTileIcon(Icons.Default.PieChart, "Categories") },
                        trailing = { SettingsChevron() }
                    )
                }
                SettingsGroupCard {
                    SettingsRow(
                        title = "About",
                        subtitle = "App info, version and open source details",
                        onClick = { showAboutSheet = true },
                        modifier = Modifier.testTag("about_row"),
                        leading = { SettingsTileIcon(Icons.Default.Info, "About") },
                        trailing = { SettingsChevron() }
                    )
                }
            }

            // ---------- Picker sheets ----------
            if (showAppThemeSheet) {
                SettingsPickerSheet(
                    title = "App Theme",
                    subtitle = "Category colors are set separately",
                    onDismiss = { showAppThemeSheet = false }
                ) {
                    AppTheme.values().forEach { theme ->
                        val isSelected = currentAppTheme == theme
                        val schemeColors = if (isDarkTheme) {
                            com.example.ui.theme.DarkSchemes[theme] ?: MaterialTheme.colorScheme
                        } else {
                            com.example.ui.theme.LightSchemes[theme] ?: MaterialTheme.colorScheme
                        }
                        SettingsOptionRow(
                            selected = isSelected,
                            onClick = {
                                haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                                if (!isSelected) {
                                    viewModel.setAppTheme(theme)
                                    Toast.makeText(context, "${theme.displayName()} theme applied", Toast.LENGTH_SHORT).show()
                                }
                                showAppThemeSheet = false
                            },
                            modifier = Modifier.testTag("app_theme_option_${theme.name}")
                        ) {
                            Text(
                                text = theme.displayName(),
                                fontWeight = FontWeight.Bold,
                                style = MaterialTheme.typography.bodyMedium,
                                color = MaterialTheme.colorScheme.onSurface
                            )
                            Spacer(modifier = Modifier.height(6.dp))
                            Row(horizontalArrangement = Arrangement.spacedBy(14.dp)) {
                                listOf(
                                    "Primary" to schemeColors.primary,
                                    "Secondary" to schemeColors.secondary,
                                    "Surface" to schemeColors.surface
                                ).forEach { (label, swatch) ->
                                    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                                        Box(
                                            modifier = Modifier
                                                .size(14.dp)
                                                .clip(RoundedCornerShape(4.dp))
                                                .border(0.5.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.2f), RoundedCornerShape(4.dp))
                                                .background(swatch)
                                        )
                                        Text(label, style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                                    }
                                }
                            }
                        }
                    }
                }
            }

            if (showPaletteSheet) {
                SettingsPickerSheet(
                    title = "Global Color Palette",
                    subtitle = "Colors offered to new and existing categories",
                    onDismiss = { showPaletteSheet = false }
                ) {
                    PaletteTheme.values().forEach { palette ->
                        val isSelected = currentTheme == palette
                        val paletteColors = viewModel.palettes[palette] ?: emptyList()
                        SettingsOptionRow(
                            selected = isSelected,
                            onClick = {
                                haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                                if (palette != currentTheme) {
                                    pendingPaletteTheme = palette
                                    showConfirmationDialog = true
                                } else {
                                    Toast.makeText(context, "${palette.name} theme is already active", Toast.LENGTH_SHORT).show()
                                }
                                showPaletteSheet = false
                            },
                            modifier = Modifier.testTag("palette_option_${palette.name}")
                        ) {
                            Text(
                                text = palette.name.lowercase().replaceFirstChar { it.uppercase() },
                                fontWeight = FontWeight.Bold,
                                style = MaterialTheme.typography.bodyMedium,
                                color = MaterialTheme.colorScheme.onSurface
                            )
                            Spacer(modifier = Modifier.height(6.dp))
                            Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
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
                    }
                }
            }

            if (showCurrencySheet) {
                SettingsPickerSheet(
                    title = "Currency Symbol",
                    subtitle = "Only changes how amounts are displayed",
                    onDismiss = { showCurrencySheet = false }
                ) {
                    currencyOptions.forEach { (sym, name) ->
                        val isSelected = currencySymbol == sym
                        SettingsOptionRow(
                            selected = isSelected,
                            onClick = {
                                haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                                if (!isSelected) {
                                    viewModel.setCurrency(sym)
                                    Toast.makeText(context, "Currency set to $name ($sym)", Toast.LENGTH_SHORT).show()
                                }
                                showCurrencySheet = false
                            },
                            modifier = Modifier.testTag("currency_option_$name"),
                            leading = {
                                Text(
                                    text = sym,
                                    fontSize = 20.sp,
                                    fontWeight = FontWeight.Black,
                                    color = MaterialTheme.colorScheme.primary
                                )
                            }
                        ) {
                            Text(
                                text = name,
                                fontWeight = FontWeight.Bold,
                                style = MaterialTheme.typography.bodyMedium,
                                color = MaterialTheme.colorScheme.onSurface
                            )
                            Text(
                                text = formatInRupee(12345.0, sym),
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                }
            }

            if (showTypeSheet) {
                SettingsPickerSheet(
                    title = "Default Transaction Type",
                    subtitle = "Pre-selected when you add a new record",
                    onDismiss = { showTypeSheet = false }
                ) {
                    listOf(
                        Triple("EXPENSE", "Expense", Icons.Default.ArrowUpward),
                        Triple("INCOME", "Income", Icons.Default.ArrowDownward)
                    ).forEach { (tValue, label, icon) ->
                        val isSelected = defaultType == tValue
                        val tint = if (tValue == "INCOME") IncomeGreen else ExpenseRed
                        SettingsOptionRow(
                            selected = isSelected,
                            onClick = {
                                haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                                if (!isSelected) {
                                    viewModel.setDefaultTransactionType(tValue)
                                    Toast.makeText(context, "$label default set", Toast.LENGTH_SHORT).show()
                                }
                                showTypeSheet = false
                            },
                            modifier = Modifier.testTag("default_type_option_$tValue"),
                            leading = { Icon(imageVector = icon, contentDescription = null, tint = tint, modifier = Modifier.size(22.dp)) }
                        ) {
                            Text(
                                text = label,
                                fontWeight = FontWeight.Bold,
                                style = MaterialTheme.typography.bodyMedium,
                                color = MaterialTheme.colorScheme.onSurface
                            )
                            Text(
                                text = if (tValue == "INCOME") "Money coming in" else "Money going out",
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                }
            }

            if (showAboutSheet) {
                SettingsPickerSheet(
                    title = "About Shylock",
                    subtitle = "App info, version and open source details",
                    onDismiss = { showAboutSheet = false }
                ) {
                    Column(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Box(
                            modifier = Modifier
                                .size(64.dp)
                                .liquidGlass(shape = CircleShape, strength = 0.9f, elevation = 6.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = Icons.Default.AccountBalanceWallet,
                                contentDescription = "Logo",
                                tint = MaterialTheme.colorScheme.primary,
                                modifier = Modifier.size(32.dp)
                            )
                        }
                        Spacer(modifier = Modifier.height(4.dp))
                        ShylockWordmark(fontSize = 24.sp)
                        Text(
                            text = "Your color-guided personal finance companion.",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            textAlign = TextAlign.Center
                        )
                    }
                    Spacer(modifier = Modifier.height(18.dp))
                    listOf(
                        Triple(Icons.Default.Info, "Version", "1.2"),
                        Triple(Icons.Default.Code, "License", "Open Source"),
                        Triple(Icons.Default.PhoneAndroid, "Built with", "Kotlin • Jetpack Compose"),
                        Triple(Icons.Default.Lock, "Privacy", "All data stays on this device")
                    ).forEach { (icon, label, value) ->
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(vertical = 8.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(imageVector = icon, contentDescription = null, tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(20.dp))
                            Spacer(modifier = Modifier.width(12.dp))
                            Text(
                                text = label,
                                style = MaterialTheme.typography.bodyMedium,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                modifier = Modifier.weight(1f)
                            )
                            Text(
                                text = value,
                                style = MaterialTheme.typography.bodyMedium,
                                fontWeight = FontWeight.SemiBold,
                                color = MaterialTheme.colorScheme.onSurface
                            )
                        }
                        HorizontalDivider(color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.08f))
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
                viewModel.addCategory(name, colorHex, icon, budget, subsList, catType)
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
            onDelete = {
                editCategoryTarget = null
                categoryToDelete = category
            },
            onSave = { name, colorHex, icon, budget, subsList, catType ->
                viewModel.editCategory(
                    id = category.id,
                    name = name,
                    colorHex = colorHex,
                    iconName = icon,
                    budget = budget,
                    subcategoriesList = subsList,
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

    // Record a Money Inbox draft: the same add-record dialog, prefilled from the notification
    inboxDraftTarget?.let { payment ->
        AddTransactionDialog(
            categories = categories,
            subcategories = subcategories,
            prefill = remember(payment, categories) { viewModel.draftTransactionFor(payment) },
            defaultType = payment.transactionType,
            transactions = transactions,
            onCreateCategoryFirstClick = {
                inboxDraftTarget = null
                navController.navigate("categories")
            },
            onDismiss = { inboxDraftTarget = null },
            onSave = { catId, subId, amount, desc, timestamp, type ->
                viewModel.recordDetectedPayment(payment.id, catId, subId, amount, desc, timestamp, type)
                inboxDraftTarget = null
                Toast.makeText(context, if (type == "INCOME") "Added to income!" else "Payment recorded!", Toast.LENGTH_SHORT).show()
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

// ===================== Settings screen building blocks =====================

// Page header for Settings: big title + tagline on the left, a glass version chip on the right,
// and a soft glowing crescent painted behind them so the header ties into the ambient background.
@Composable
fun SettingsHeader(modifier: Modifier = Modifier) {
    val accent = MaterialTheme.colorScheme.primary
    Row(
        modifier = modifier
            .fillMaxWidth()
            .drawBehind {
                val r = size.width * 0.115f
                val c = Offset(size.width * 0.66f, size.height * 0.52f)
                // Ambient halo
                drawCircle(
                    brush = Brush.radialGradient(
                        colors = listOf(accent.copy(alpha = 0.16f), Color.Transparent),
                        center = c,
                        radius = r * 1.9f
                    ),
                    radius = r * 1.9f,
                    center = c
                )
                // Crescent: a thick ring whose brightness fades around the circumference
                drawCircle(
                    brush = Brush.sweepGradient(
                        0.00f to accent.copy(alpha = 0.18f),
                        0.14f to Color.Transparent,
                        0.40f to Color.Transparent,
                        0.56f to accent.copy(alpha = 0.32f),
                        0.76f to accent.copy(alpha = 0.55f),
                        0.94f to accent.copy(alpha = 0.28f),
                        1.00f to accent.copy(alpha = 0.18f),
                        center = c
                    ),
                    radius = r,
                    center = c,
                    style = Stroke(width = r * 0.55f)
                )
            }
            .padding(top = 6.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = "Settings",
                fontSize = 30.sp,
                lineHeight = 34.sp,
                fontWeight = FontWeight.Black,
                letterSpacing = (-0.5).sp,
                color = MaterialTheme.colorScheme.onBackground
            )
            Text(
                text = "Customize your Shylock experience",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
        Spacer(modifier = Modifier.width(12.dp))
        // Version chip
        Row(
            modifier = Modifier
                .liquidGlass(shape = RoundedCornerShape(18.dp), strength = 0.95f, elevation = 6.dp)
                .padding(horizontal = 12.dp, vertical = 10.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                modifier = Modifier
                    .size(34.dp)
                    .liquidGlass(shape = CircleShape, strength = 0.8f),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = Icons.Default.Settings,
                    contentDescription = null,
                    tint = accent,
                    modifier = Modifier.size(20.dp)
                )
            }
            Spacer(modifier = Modifier.width(10.dp))
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                ShylockWordmark(fontSize = 12.sp, letterSpacing = 3.sp)
                Text(
                    text = "v1.2 • Open Source",
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
    }
}

// Section label with a leading accent icon and a one-line description
@Composable
fun SettingsSectionHeader(
    title: String,
    subtitle: String,
    modifier: Modifier = Modifier,
    icon: @Composable () -> Unit
) {
    Row(
        modifier = modifier
            .fillMaxWidth()
            .padding(start = 4.dp, end = 4.dp, top = 6.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Box(modifier = Modifier.size(34.dp), contentAlignment = Alignment.Center) { icon() }
        Spacer(modifier = Modifier.width(14.dp))
        Column {
            Text(
                text = title,
                fontSize = 19.sp,
                lineHeight = 24.sp,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onBackground
            )
            Text(
                text = subtitle,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}

// Glass container that groups settings rows; rows supply their own inner padding
@Composable
fun SettingsGroupCard(
    modifier: Modifier = Modifier,
    elevation: Dp = 0.dp,
    content: @Composable ColumnScope.() -> Unit
) {
    GlassCard(
        modifier = modifier.fillMaxWidth(),
        shape = RoundedCornerShape(22.dp),
        elevation = elevation,
        contentPadding = PaddingValues(6.dp),
        content = content
    )
}

// One settings entry: tinted icon tile, title + subtitle, and a trailing control or disclosure
@Composable
fun SettingsRow(
    title: String,
    subtitle: String,
    modifier: Modifier = Modifier,
    accent: Color = MaterialTheme.colorScheme.primary,
    titleColor: Color = MaterialTheme.colorScheme.onSurface,
    onClick: (() -> Unit)? = null,
    leading: @Composable BoxScope.() -> Unit,
    trailing: @Composable RowScope.() -> Unit = {}
) {
    val rowShape = RoundedCornerShape(16.dp)
    Row(
        modifier = modifier
            .fillMaxWidth()
            .clip(rowShape)
            .then(if (onClick != null) Modifier.clickable(onClick = onClick) else Modifier)
            .padding(horizontal = 10.dp, vertical = 12.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        SettingsTile(accent = accent, content = leading)
        Spacer(modifier = Modifier.width(14.dp))
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = title,
                style = MaterialTheme.typography.bodyLarge,
                fontWeight = FontWeight.Bold,
                color = titleColor,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
            Text(
                text = subtitle,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                maxLines = 2,
                overflow = TextOverflow.Ellipsis
            )
        }
        Spacer(modifier = Modifier.width(10.dp))
        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
            trailing()
        }
    }
}

// Rounded square icon well tinted with the row's accent — reads as a second layer of glass
@Composable
fun SettingsTile(
    accent: Color = MaterialTheme.colorScheme.primary,
    size: Dp = 46.dp,
    content: @Composable BoxScope.() -> Unit
) {
    val tileShape = RoundedCornerShape(14.dp)
    Box(
        modifier = Modifier
            .size(size)
            .clip(tileShape)
            .background(
                Brush.linearGradient(
                    listOf(accent.copy(alpha = 0.26f), accent.copy(alpha = 0.10f))
                )
            )
            .border(1.dp, accent.copy(alpha = 0.30f), tileShape),
        contentAlignment = Alignment.Center,
        content = content
    )
}

@Composable
fun SettingsTileIcon(
    icon: ImageVector,
    contentDescription: String?,
    tint: Color = MaterialTheme.colorScheme.primary
) {
    Icon(
        imageVector = icon,
        contentDescription = contentDescription,
        tint = tint,
        modifier = Modifier.size(23.dp)
    )
}

@Composable
fun SettingsChevron(tint: Color = MaterialTheme.colorScheme.primary) {
    Icon(
        imageVector = Icons.Default.ChevronRight,
        contentDescription = null,
        tint = tint,
        modifier = Modifier.size(24.dp)
    )
}

// Current value shown before the chevron on disclosure rows
@Composable
fun SettingsValue(text: String) {
    Text(
        text = text,
        style = MaterialTheme.typography.bodyMedium,
        fontWeight = FontWeight.SemiBold,
        color = MaterialTheme.colorScheme.primary,
        maxLines = 1,
        overflow = TextOverflow.Ellipsis,
        modifier = Modifier.widthIn(max = 120.dp)
    )
}

@Composable
fun SettingsDivider() {
    HorizontalDivider(
        color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.08f),
        modifier = Modifier.padding(horizontal = 10.dp)
    )
}

// Bottom sheet shell shared by the Settings pickers: title row with a close button, then content
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingsPickerSheet(
    title: String,
    subtitle: String,
    onDismiss: () -> Unit,
    content: @Composable ColumnScope.() -> Unit
) {
    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true),
        containerColor = MaterialTheme.colorScheme.surface
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 20.dp)
                .padding(bottom = 32.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = title,
                        style = MaterialTheme.typography.titleLarge,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                    Text(
                        text = subtitle,
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
                IconButton(onClick = onDismiss) {
                    Icon(imageVector = Icons.Default.Close, contentDescription = "Close")
                }
            }
            Spacer(modifier = Modifier.height(8.dp))
            content()
        }
    }
}

// Selectable option inside a picker sheet: optional leading glyph, caller-supplied body, check mark
@Composable
fun SettingsOptionRow(
    selected: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    leading: (@Composable () -> Unit)? = null,
    content: @Composable ColumnScope.() -> Unit
) {
    val shape = RoundedCornerShape(14.dp)
    Row(
        modifier = modifier
            .fillMaxWidth()
            .clip(shape)
            .background(
                if (selected) MaterialTheme.colorScheme.primary.copy(alpha = 0.10f)
                else MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.18f)
            )
            .border(
                width = if (selected) 1.5.dp else 1.dp,
                color = if (selected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.outline.copy(alpha = 0.18f),
                shape = shape
            )
            .clickable(onClick = onClick)
            .padding(horizontal = 14.dp, vertical = 12.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        if (leading != null) {
            Box(modifier = Modifier.size(36.dp), contentAlignment = Alignment.Center) { leading() }
            Spacer(modifier = Modifier.width(12.dp))
        }
        Column(modifier = Modifier.weight(1f), content = content)
        Spacer(modifier = Modifier.width(12.dp))
        if (selected) {
            Icon(
                imageVector = Icons.Default.CheckCircle,
                contentDescription = "Selected",
                tint = MaterialTheme.colorScheme.primary,
                modifier = Modifier.size(22.dp)
            )
        } else {
            Box(
                modifier = Modifier
                    .size(22.dp)
                    .border(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.4f), CircleShape)
            )
        }
    }
}

// Stacked-cylinder "database" glyph; Material's icon set has no direct equivalent
@Composable
fun DatabaseGlyph(
    modifier: Modifier = Modifier,
    color: Color = MaterialTheme.colorScheme.primary
) {
    val gap = MaterialTheme.colorScheme.background
    Canvas(modifier = modifier) {
        val w = size.width
        val h = size.height
        val capH = h * 0.30f
        // Body between the two end caps
        drawRect(color = color, topLeft = Offset(0f, capH / 2f), size = Size(w, h - capH))
        drawOval(color = color, topLeft = Offset(0f, h - capH), size = Size(w, capH))
        // Separator arcs carve the body into three discs
        listOf(0.42f, 0.70f).forEach { f ->
            drawArc(
                color = gap.copy(alpha = 0.85f),
                startAngle = 0f,
                sweepAngle = 180f,
                useCenter = false,
                topLeft = Offset(0f, h * f - capH / 2f),
                size = Size(w, capH),
                style = Stroke(width = h * 0.09f)
            )
        }
        // Lit top cap
        drawOval(color = lerp(color, Color.White, 0.30f), topLeft = Offset(0f, 0f), size = Size(w, capH))
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

/**
 * Single-line text that steps its font size down until it fits its width, so hero amounts stay
 * whole instead of being ellipsized on narrow phones. (Compose 1.7 has no built-in auto-size.)
 */
@Composable
fun AutoShrinkText(
    text: String,
    maxFontSize: androidx.compose.ui.unit.TextUnit,
    minFontSize: androidx.compose.ui.unit.TextUnit,
    color: Color,
    modifier: Modifier = Modifier,
    fontWeight: FontWeight = FontWeight.Black,
    letterSpacing: androidx.compose.ui.unit.TextUnit = 0.sp,
    style: androidx.compose.ui.text.TextStyle = LocalTextStyle.current
) {
    var fontSize by remember(text, maxFontSize) { mutableStateOf(maxFontSize) }
    Text(
        text = text,
        modifier = modifier,
        fontSize = fontSize,
        lineHeight = (fontSize.value * 1.15f).sp,
        fontWeight = fontWeight,
        letterSpacing = letterSpacing,
        color = color,
        style = style,
        maxLines = 1,
        softWrap = false,
        overflow = TextOverflow.Clip,
        onTextLayout = { result ->
            if (result.didOverflowWidth && fontSize.value - 2f >= minFontSize.value) {
                fontSize = (fontSize.value - 2f).sp
            }
        }
    )
}

// Two-tone "SHYLOCK" wordmark shared by the Home and Insights headers
@Composable
fun ShylockWordmark(
    modifier: Modifier = Modifier,
    fontSize: androidx.compose.ui.unit.TextUnit = 22.sp,
    letterSpacing: androidx.compose.ui.unit.TextUnit = 6.sp
) {
    Row(modifier = modifier, verticalAlignment = Alignment.CenterVertically) {
        Text(
            text = "SHY",
            fontSize = fontSize,
            fontWeight = FontWeight.Black,
            letterSpacing = letterSpacing,
            color = MaterialTheme.colorScheme.onBackground
        )
        Text(
            text = "LOCK",
            fontSize = fontSize,
            fontWeight = FontWeight.Black,
            letterSpacing = letterSpacing,
            color = MaterialTheme.colorScheme.primary
        )
    }
}

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
            .padding(vertical = 0.dp)
            .testTag("brand_header"),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Column(modifier = Modifier.weight(1f)) {
            ShylockWordmark()
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
                    .size(38.dp)
                    .liquidGlass(shape = CircleShape, strength = 0.9f),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = Icons.Default.Person,
                    contentDescription = "Profile",
                    tint = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.size(22.dp)
                )
            }
        }
    }
}

// Pill-shaped glass search input used in the transactions panel.
// Built on BasicTextField because OutlinedTextField enforces a 56dp minimum height,
// which is taller than the rest of the panel's controls.
@Composable
fun GlassSearchField(
    value: String,
    onValueChange: (String) -> Unit,
    modifier: Modifier = Modifier
) {
    var focused by remember { mutableStateOf(false) }
    val borderColor = if (focused)
        MaterialTheme.colorScheme.primary.copy(alpha = 0.7f)
    else
        MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.45f)
    val containerColor = MaterialTheme.colorScheme.surface.copy(alpha = if (focused) 0.55f else 0.35f)

    Row(
        modifier = modifier
            .height(38.dp)
            .clip(CircleShape)
            .background(containerColor)
            .border(0.8.dp, borderColor, CircleShape)
            .padding(horizontal = 12.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Icon(
            imageVector = Icons.Default.Search,
            contentDescription = "Search icon",
            tint = MaterialTheme.colorScheme.primary,
            modifier = Modifier.size(18.dp)
        )
        Spacer(modifier = Modifier.width(8.dp))
        BasicTextField(
            value = value,
            onValueChange = onValueChange,
            singleLine = true,
            textStyle = MaterialTheme.typography.bodyMedium.copy(color = MaterialTheme.colorScheme.onSurface, fontSize = 13.sp),
            cursorBrush = SolidColor(MaterialTheme.colorScheme.primary),
            modifier = Modifier
                .weight(1f)
                .onFocusChanged { focused = it.isFocused },
            decorationBox = { innerTextField ->
                Box(contentAlignment = Alignment.CenterStart) {
                    if (value.isEmpty()) {
                        Text(
                            "Search description or amount...",
                            style = MaterialTheme.typography.bodyMedium,
                            fontSize = 13.sp,
                            color = MaterialTheme.colorScheme.outline,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                    }
                    innerTextField()
                }
            }
        )
        if (value.isNotEmpty()) {
            Spacer(modifier = Modifier.width(6.dp))
            Icon(
                imageVector = Icons.Default.Clear,
                contentDescription = "Clear search",
                tint = MaterialTheme.colorScheme.outline,
                modifier = Modifier
                    .size(18.dp)
                    .clip(CircleShape)
                    .clickable { onValueChange("") }
            )
        }
    }
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
        .height(32.dp)
        .then(if (selected) Modifier.accentGlow(accent, shape, 6.dp) else Modifier)

    Box(
        modifier = if (selected) {
            base
                .clip(shape)
                .background(selectedPillBrush(accent))
                .border(0.8.dp, selectedPillRim(accent), shape)
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
            fontSize = 12.sp,
            fontWeight = if (selected) FontWeight.Bold else FontWeight.Medium,
            color = if (selected) Color.White else MaterialTheme.colorScheme.onSurfaceVariant,
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

/**
 * Buckets the already period-filtered expenses into equal time slices for the Insights header
 * sparkline. Always returns [buckets] values (zeros when empty) so the chart keeps its silhouette.
 */
fun buildPeriodSparkline(
    periodExpenses: List<Transaction>,
    period: AnalysisPeriod,
    month: YearMonth,
    buckets: Int = 6
): List<Double> {
    val zone = ZoneId.systemDefault()
    val isCurrentMonth = month == YearMonth.now()
    val anchor = if (isCurrentMonth) LocalDate.now() else month.atEndOfMonth()
    val (start, end) = when (period) {
        AnalysisPeriod.WEEKLY -> anchor.minusDays(6) to anchor.plusDays(1)
        AnalysisPeriod.MONTHLY -> month.atDay(1) to month.plusMonths(1).atDay(1)
        AnalysisPeriod.YEARLY -> month.minusMonths(11).atDay(1) to month.plusMonths(1).atDay(1)
    }
    val startMillis = start.atStartOfDay(zone).toInstant().toEpochMilli()
    val span = (end.atStartOfDay(zone).toInstant().toEpochMilli() - startMillis).coerceAtLeast(1L)

    val totals = DoubleArray(buckets)
    periodExpenses.forEach { tx ->
        val index = (((tx.timestamp - startMillis) * buckets) / span).toInt().coerceIn(0, buckets - 1)
        totals[index] += tx.amount
    }
    return totals.toList()
}

// Compact glass pill that opens the month picker sheet (no prev/next arrows)
@Composable
fun MonthPickerPill(
    selectedMonth: YearMonth,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val monthFormatter = remember { DateTimeFormatter.ofPattern("MMMM yyyy", Locale.getDefault()) }
    Row(
        modifier = modifier
            .height(44.dp)
            .liquidGlass(shape = RoundedCornerShape(14.dp), strength = 0.9f, elevation = 4.dp)
            .clickable { onClick() }
            .padding(horizontal = 14.dp)
            .testTag("month_label_button"),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Icon(
            imageVector = Icons.Default.CalendarMonth,
            contentDescription = null,
            modifier = Modifier.size(17.dp),
            tint = MaterialTheme.colorScheme.primary
        )
        Spacer(modifier = Modifier.width(8.dp))
        Text(
            text = selectedMonth.format(monthFormatter),
            style = MaterialTheme.typography.titleSmall,
            fontWeight = FontWeight.Bold,
            color = MaterialTheme.colorScheme.onSurface,
            maxLines = 1
        )
        Spacer(modifier = Modifier.width(6.dp))
        Icon(
            imageVector = Icons.Default.ExpandMore,
            contentDescription = "Open month selector",
            modifier = Modifier.size(18.dp),
            tint = MaterialTheme.colorScheme.onSurfaceVariant
        )
    }
}

/**
 * Glass pill segmented control: the selected segment is a glowing accent pill, the rest sit
 * quietly on the glass track separated by hairlines. [expand] stretches segments to share the
 * available width; otherwise each hugs its label.
 */
@Composable
fun GlassSegmentedControl(
    options: List<String>,
    selectedIndex: Int,
    onSelect: (Int) -> Unit,
    modifier: Modifier = Modifier,
    expand: Boolean = false,
    height: Dp = 40.dp,
    fontSize: TextUnit = 12.sp
) {
    val accent = MaterialTheme.colorScheme.primary
    val trackShape = RoundedCornerShape(50)
    Row(
        modifier = modifier
            .height(height)
            .liquidGlass(shape = trackShape, strength = 0.7f)
            .padding(3.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        options.forEachIndexed { index, label ->
            val selected = index == selectedIndex
            // Hairline between two unselected neighbours only; the accent pill provides its own edge
            if (index > 0 && !selected && index - 1 != selectedIndex) {
                Box(
                    modifier = Modifier
                        .width(1.dp)
                        .fillMaxHeight(0.55f)
                        .background(MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.45f))
                )
            }
            val segmentBase = Modifier
                .then(if (expand) Modifier.weight(1f) else Modifier)
                .fillMaxHeight()
                .then(if (selected) Modifier.accentGlow(accent, trackShape, 8.dp) else Modifier)
                .clip(trackShape)
            Box(
                modifier = if (selected) {
                    segmentBase
                        .background(selectedPillBrush(accent))
                        .border(0.8.dp, selectedPillRim(accent), trackShape)
                        .clickable { onSelect(index) }
                } else {
                    segmentBase.clickable { onSelect(index) }
                }
                    .padding(horizontal = if (expand) 4.dp else 11.dp),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = label,
                    fontSize = fontSize,
                    fontWeight = if (selected) FontWeight.Bold else FontWeight.Medium,
                    color = if (selected) Color.White else MaterialTheme.colorScheme.onSurfaceVariant,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }
        }
    }
}

// Hero summary on the Insights tab: glowing period total, change vs the previous period,
// pacing bars and a plain-language delta tile
@Composable
fun InsightsSummaryCard(
    period: AnalysisPeriod,
    selectedMonth: YearMonth,
    isCurrentMonth: Boolean,
    totalSpend: Double,
    lastPeriodSpend: Double,
    currencySymbol: String,
    sparkline: List<Double>,
    modifier: Modifier = Modifier
) {
    val accent = MaterialTheme.colorScheme.primary
    val monthFormatter = remember { DateTimeFormatter.ofPattern("MMMM yyyy", Locale.getDefault()) }

    val headline = when (period) {
        AnalysisPeriod.WEEKLY -> "WEEKLY SPENDING"
        AnalysisPeriod.MONTHLY -> "MONTHLY SPENDING"
        AnalysisPeriod.YEARLY -> "YEARLY SPENDING"
    }
    val subtitle = when (period) {
        AnalysisPeriod.WEEKLY -> "Total spent in the last 7 days"
        AnalysisPeriod.MONTHLY -> if (isCurrentMonth) "Total spent this month" else "Total spent in ${selectedMonth.format(monthFormatter)}"
        AnalysisPeriod.YEARLY -> "Total spent over the last 12 months"
    }
    val previousNoun = when (period) {
        AnalysisPeriod.WEEKLY -> "last week"
        AnalysisPeriod.MONTHLY -> "last month"
        AnalysisPeriod.YEARLY -> "last year"
    }

    val percentageChange = if (lastPeriodSpend > 0.0) ((totalSpend - lastPeriodSpend) / lastPeriodSpend) * 100 else null
    val isSpendingUp = (percentageChange ?: 0.0) > 0.0
    val difference = Math.abs(totalSpend - lastPeriodSpend)
    val deltaMessage = when {
        lastPeriodSpend > 0.0 && totalSpend > lastPeriodSpend ->
            "You spent ${formatInRupee(difference, currencySymbol)} more than $previousNoun"
        lastPeriodSpend > 0.0 && totalSpend < lastPeriodSpend ->
            "You spent ${formatInRupee(difference, currencySymbol)} less than $previousNoun"
        lastPeriodSpend > 0.0 -> "Same as $previousNoun — steady as she goes"
        totalSpend > 0.0 -> "Nothing recorded $previousNoun to compare"
        else -> "No spending recorded yet"
    }

    GlassCard(
        modifier = modifier
            .fillMaxWidth()
            .testTag("insights_summary_card"),
        shape = RoundedCornerShape(24.dp),
        elevation = 8.dp,
        contentPadding = PaddingValues(start = 16.dp, end = 12.dp, top = 14.dp, bottom = 14.dp)
    ) {
        Row(modifier = Modifier.fillMaxWidth()) {
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = headline,
                    style = MaterialTheme.typography.labelSmall,
                    fontWeight = FontWeight.Bold,
                    letterSpacing = 1.2.sp,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Spacer(modifier = Modifier.height(2.dp))
                AutoShrinkText(
                    text = formatInRupee(totalSpend, currency = currencySymbol),
                    maxFontSize = 34.sp,
                    minFontSize = 20.sp,
                    letterSpacing = (-1).sp,
                    color = accent,
                    style = MaterialTheme.typography.headlineLarge,
                    modifier = Modifier.fillMaxWidth()
                )
                Text(
                    text = subtitle,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )

                Spacer(modifier = Modifier.height(8.dp))
                if (percentageChange != null) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier
                                .clip(CircleShape)
                                .background(if (isSpendingUp) ExpenseRedContainer else IncomeGreenContainer)
                                .border(
                                    0.8.dp,
                                    (if (isSpendingUp) ExpenseRed else IncomeGreen).copy(alpha = 0.45f),
                                    CircleShape
                                )
                                .padding(horizontal = 9.dp, vertical = 4.dp)
                        ) {
                            Icon(
                                imageVector = if (isSpendingUp) Icons.Default.ArrowUpward else Icons.Default.ArrowDownward,
                                contentDescription = null,
                                tint = if (isSpendingUp) ExpenseRed else IncomeGreen,
                                modifier = Modifier.size(12.dp)
                            )
                            Spacer(modifier = Modifier.width(4.dp))
                            Text(
                                text = "${String.format(Locale.US, "%.0f", Math.abs(percentageChange))}%",
                                style = MaterialTheme.typography.labelSmall,
                                fontWeight = FontWeight.Bold,
                                color = if (isSpendingUp) ExpenseRed else IncomeGreen
                            )
                        }
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = "vs $previousNoun",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                } else {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(
                            imageVector = Icons.Default.History,
                            contentDescription = "Comparison",
                            tint = MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier.size(13.dp)
                        )
                        Spacer(modifier = Modifier.width(5.dp))
                        Text(
                            text = "No prior period to compare",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.width(10.dp))

            Column(
                modifier = Modifier.width(132.dp),
                horizontalAlignment = Alignment.End
            ) {
                SpendRhythmBars(
                    values = sparkline,
                    modifier = Modifier
                        .padding(end = 4.dp)
                        .size(width = 78.dp, height = 52.dp)
                )
                Spacer(modifier = Modifier.height(8.dp))
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .liquidGlass(shape = RoundedCornerShape(12.dp), strength = 0.75f)
                        .padding(start = 10.dp, end = 6.dp, top = 8.dp, bottom = 8.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = deltaMessage,
                        style = MaterialTheme.typography.labelSmall,
                        lineHeight = 14.sp,
                        color = MaterialTheme.colorScheme.onSurface,
                        modifier = Modifier.weight(1f)
                    )
                    Icon(
                        imageVector = Icons.Default.ChevronRight,
                        contentDescription = null,
                        tint = accent,
                        modifier = Modifier.size(18.dp)
                    )
                }
            }
        }
    }
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
            .liquidGlass(shape = RoundedCornerShape(18.dp), elevation = 4.dp)
            .testTag("month_navigation_bar")
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 6.dp, vertical = 2.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            IconButton(
                onClick = onPrevMonth,
                modifier = Modifier
                    .size(36.dp)
                    .testTag("prev_month_btn")
            ) {
                Icon(
                    imageVector = Icons.AutoMirrored.Filled.KeyboardArrowLeft,
                    contentDescription = "Previous Month",
                    tint = MaterialTheme.colorScheme.onSurface,
                    modifier = Modifier.size(22.dp)
                )
            }

            Row(
                modifier = Modifier
                    .clip(CircleShape)
                    .clickable { onLabelClick() }
                    .padding(horizontal = 12.dp, vertical = 4.dp)
                    .testTag("month_label_button"),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Icon(
                    imageVector = Icons.Default.CalendarMonth,
                    contentDescription = null,
                    modifier = Modifier.size(17.dp),
                    tint = MaterialTheme.colorScheme.primary
                )
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    text = selectedMonth.format(monthFormatter),
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onSurface
                )
                Spacer(modifier = Modifier.width(6.dp))
                Icon(
                    imageVector = Icons.Default.ExpandMore,
                    contentDescription = "Open month selector",
                    modifier = Modifier.size(18.dp),
                    tint = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }

            IconButton(
                onClick = onNextMonth,
                enabled = !isCurrentMonth,
                modifier = Modifier
                    .size(36.dp)
                    .testTag("next_month_btn")
            ) {
                Icon(
                    imageVector = Icons.AutoMirrored.Filled.KeyboardArrowRight,
                    contentDescription = "Next Month",
                    tint = if (!isCurrentMonth) MaterialTheme.colorScheme.onSurface else MaterialTheme.colorScheme.onSurface.copy(alpha = 0.25f),
                    modifier = Modifier.size(22.dp)
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
        shape = RoundedCornerShape(24.dp),
        elevation = 8.dp,
        contentPadding = PaddingValues(16.dp)
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
                fontSize = 18.sp,
                color = MaterialTheme.colorScheme.onSurface
            )
            Box(
                modifier = Modifier.liquidGlass(shape = CircleShape, strength = 0.75f)
            ) {
                Text(
                    text = "$categoriesCount Categories",
                    fontWeight = FontWeight.SemiBold,
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurface,
                    modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp)
                )
            }
        }

        Spacer(modifier = Modifier.height(8.dp))

        // Balance block on the left, spend rhythm sparkline on the right
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = "Net Balance",
                    style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    fontWeight = FontWeight.Medium
                )
                Spacer(modifier = Modifier.height(1.dp))
                val balanceColor = if (netBalance >= 0.0) MaterialTheme.colorScheme.primary else ExpenseRed
                Text(
                    text = formatInRupee(netBalance, currency = currencySymbol),
                    style = MaterialTheme.typography.headlineLarge,
                    fontSize = 32.sp,
                    lineHeight = 38.sp,
                    fontWeight = FontWeight.Black,
                    letterSpacing = (-0.5).sp,
                    color = balanceColor,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )

                Spacer(modifier = Modifier.height(6.dp))
                if (formattedPercent != null) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Surface(
                            shape = CircleShape,
                            color = if (isSpendingUp) ExpenseRedContainer else IncomeGreenContainer
                        ) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                            ) {
                                Icon(
                                    imageVector = if (isSpendingUp) Icons.Default.ArrowUpward else Icons.Default.ArrowDownward,
                                    contentDescription = null,
                                    tint = if (isSpendingUp) ExpenseRed else IncomeGreen,
                                    modifier = Modifier.size(12.dp)
                                )
                                Spacer(modifier = Modifier.width(3.dp))
                                Text(
                                    text = "$formattedPercent%",
                                    style = MaterialTheme.typography.labelSmall,
                                    fontWeight = FontWeight.Bold,
                                    color = if (isSpendingUp) ExpenseRed else IncomeGreen
                                )
                            }
                        }
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = "vs last month",
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                } else {
                    // Same slot as the MoM pill so the card keeps one height either way
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(
                            imageVector = Icons.Default.History,
                            contentDescription = "Comparison",
                            tint = MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier.size(13.dp)
                        )
                        Spacer(modifier = Modifier.width(5.dp))
                        Text(
                            text = "No prior month to compare",
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            }

            if (sparkline.isNotEmpty()) {
                Column(horizontalAlignment = Alignment.End) {
                    SpendRhythmBars(
                        values = sparkline,
                        modifier = Modifier.size(width = 112.dp, height = 44.dp)
                    )
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        text = "Smaller steps,\nbigger freedom",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        textAlign = TextAlign.End,
                        lineHeight = 12.sp
                    )
                }
            }
        }

        Spacer(modifier = Modifier.height(12.dp))

        // Two nested glass tiles: Income & Expenses
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(10.dp)
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
    Row(
        modifier = modifier
            .liquidGlass(shape = RoundedCornerShape(16.dp), tint = accent, strength = 0.75f)
            .padding(horizontal = 10.dp, vertical = 9.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Box(
            modifier = Modifier
                .size(36.dp)
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
        Spacer(modifier = Modifier.width(9.dp))
        Column {
            Text(
                text = label,
                style = MaterialTheme.typography.labelSmall,
                lineHeight = 13.sp,
                fontWeight = FontWeight.SemiBold,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            Text(
                text = amount,
                fontSize = 16.sp,
                lineHeight = 20.sp,
                fontWeight = FontWeight.ExtraBold,
                color = accent,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
            Text(
                text = "$count ${if (count == 1) "transaction" else "transactions"}",
                style = MaterialTheme.typography.labelSmall,
                lineHeight = 13.sp,
                color = MaterialTheme.colorScheme.outline
            )
        }
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
    val peakIndex = if (peak > 0.0) values.indexOfFirst { it == peak } else -1

    Canvas(modifier = modifier) {
        if (values.isEmpty()) return@Canvas
        val gap = size.width * 0.04f
        val barWidth = (size.width - gap * (values.size - 1)) / values.size
        val radius = androidx.compose.ui.geometry.CornerRadius(barWidth / 2f, barWidth / 2f)
        // Empty buckets still get a short stub so the chart keeps its silhouette
        val minHeight = (size.height * 0.18f).coerceAtLeast(barWidth)

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

/// Visual Component: Donut Chart with Custom Canvas Drawing
@Composable
fun DonutChartComponent(
    categories: List<Category>,
    stats: Map<Int, CategoryStats>,
    totalSpend: Double,
    selectedCatId: Int?,
    currencySymbol: String = "₹",
    caption: String = "This Month",
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
            Box(
                modifier = Modifier
                    .size(72.dp)
                    .liquidGlass(shape = CircleShape, strength = 0.6f),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = Icons.Default.PieChart,
                    contentDescription = "Empty chart",
                    tint = MaterialTheme.colorScheme.primary.copy(alpha = 0.6f),
                    modifier = Modifier.size(34.dp)
                )
            }
            Spacer(modifier = Modifier.height(12.dp))
            Text(
                text = "No expenses in this period yet",
                style = MaterialTheme.typography.titleSmall,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onSurface
            )
            Text(
                text = "Add a few transactions and your breakdown appears here.",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                textAlign = TextAlign.Center,
                modifier = Modifier.padding(horizontal = 24.dp)
            )
        }
    } else {
        val trackColor = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.06f)
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(196.dp),
            contentAlignment = Alignment.Center
        ) {
            Canvas(
                modifier = Modifier
                    .size(176.dp)
                    .clickable(
                        interactionSource = remember { MutableInteractionSource() },
                        indication = null
                    ) {
                        // simple visual clicking toggler
                    }
            ) {
                var currentAngle = -90f
                val strokeWidth = 22.dp.toPx()
                // Inset so a highlighted slice's thicker stroke stays inside the canvas
                val inset = 5.dp.toPx()
                val arcSize = Size(size.width - inset * 2, size.height - inset * 2)
                val arcOffset = Offset(inset, inset)

                // Faint track ring gives the donut a base to sit on
                drawCircle(
                    color = trackColor,
                    radius = (arcSize.width - strokeWidth) / 2f,
                    style = Stroke(width = strokeWidth)
                )

                categories.forEach { cat ->
                    val catSpend = stats[cat.id]?.totalAmount ?: 0.0
                    if (catSpend > 0.0) {
                        val sweepAngle = ((catSpend / totalSpend) * 360f).toFloat()
                        val isHighlighted = selectedCatId == cat.id
                        val activeStroke = if (isHighlighted) strokeWidth + inset * 1.5f else strokeWidth
                        val activeAlpha = if (selectedCatId == null || isHighlighted) 1.0f else 0.3f

                        drawArc(
                            color = parseHexColor(cat.colorHex).copy(alpha = activeAlpha),
                            startAngle = currentAngle,
                            sweepAngle = sweepAngle,
                            useCenter = false,
                            topLeft = arcOffset,
                            size = arcSize,
                            style = Stroke(width = activeStroke, cap = StrokeCap.Butt)
                        )
                        currentAngle += sweepAngle
                    }
                }
            }

            // Central info display inside donut hole
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                modifier = Modifier.width(108.dp)
            ) {
                if (selectedCatId != null) {
                    val highlightedCat = categories.find { it.id == selectedCatId }
                    val highlightedStats = stats[selectedCatId]
                    if (highlightedCat != null && highlightedStats != null) {
                        Icon(
                            imageVector = getIconVector(highlightedCat.iconName),
                            contentDescription = highlightedCat.name,
                            tint = parseHexColor(highlightedCat.colorHex),
                            modifier = Modifier.size(18.dp)
                        )
                        Spacer(modifier = Modifier.height(2.dp))
                        Text(
                            text = highlightedCat.displayName,
                            fontWeight = FontWeight.Bold,
                            fontSize = 12.sp,
                            color = parseHexColor(highlightedCat.colorHex),
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                        AutoShrinkText(
                            text = formatInRupee(highlightedStats.totalAmount, currency = currencySymbol),
                            maxFontSize = 20.sp,
                            minFontSize = 12.sp,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                        Text(
                            text = String.format("%.1f%% of spend", highlightedStats.percentage),
                            fontWeight = FontWeight.Medium,
                            fontSize = 10.sp,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                } else {
                    Text(
                        text = "Total Spend",
                        fontSize = 11.sp,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        fontWeight = FontWeight.Medium
                    )
                    AutoShrinkText(
                        text = formatInRupee(totalSpend, currency = currencySymbol),
                        maxFontSize = 24.sp,
                        minFontSize = 13.sp,
                        letterSpacing = (-0.5).sp,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                    Text(
                        text = caption,
                        fontSize = 10.sp,
                        color = MaterialTheme.colorScheme.outline,
                        fontWeight = FontWeight.Medium
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
                .height(160.dp),
            contentAlignment = Alignment.Center
        ) {
            Text("No spending data available to graph.", fontSize = 12.sp, color = Color.Gray)
        }
    } else {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .height(160.dp)
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
                .height(112.dp)
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

/// Visual Component: table-style category breakdown. Doubles as the chart legend and the
// highlight toggle — tapping a row selects that category in the chart above.
@Composable
fun CategoryBreakdownList(
    categories: List<Category>,
    stats: Map<Int, CategoryStats>,
    selectedCatId: Int?,
    currencySymbol: String,
    onRowClick: (Int) -> Unit,
    modifier: Modifier = Modifier
) {
    // Only categories with spend in the selected period, matching the donut and bar charts
    val ordered = remember(categories, stats) {
        categories
            .filter { (stats[it.id]?.totalAmount ?: 0.0) > 0.0 }
            .sortedWith(
                compareByDescending<Category> { stats[it.id]?.totalAmount ?: 0.0 }.thenBy { it.displayName }
            )
    }
    val panelShape = RoundedCornerShape(14.dp)

    if (ordered.isEmpty()) {
        Text(
            text = "No spending recorded in this period.",
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.outline,
            textAlign = TextAlign.Center,
            modifier = modifier
                .fillMaxWidth()
                .padding(vertical = 12.dp)
                .testTag("category_breakdown_list")
        )
        return
    }

    Column(
        modifier = modifier
            .fillMaxWidth()
            .clip(panelShape)
            .background(MaterialTheme.colorScheme.surface.copy(alpha = 0.38f))
            .border(1.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.35f), panelShape)
            .testTag("category_breakdown_list")
    ) {
        ordered.forEachIndexed { index, cat ->
            val catColor = parseHexColor(cat.colorHex)
            val isSelected = selectedCatId == cat.id
            val spend = stats[cat.id]?.totalAmount ?: 0.0
            val share = stats[cat.id]?.percentage ?: 0.0
            val dimmed = selectedCatId != null && !isSelected

            if (index > 0) {
                HorizontalDivider(
                    thickness = 0.6.dp,
                    color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.3f)
                )
            }

            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(if (isSelected) catColor.copy(alpha = 0.12f) else Color.Transparent)
                    .clickable { onRowClick(cat.id) }
                    .padding(horizontal = 12.dp, vertical = 8.dp)
                    .alpha(if (dimmed) 0.55f else 1f),
                verticalAlignment = Alignment.CenterVertically
            ) {
                // Colour dot with a soft halo so it reads on the dark glass
                Box(
                    modifier = Modifier
                        .size(15.dp)
                        .clip(CircleShape)
                        .background(catColor.copy(alpha = 0.22f)),
                    contentAlignment = Alignment.Center
                ) {
                    Box(
                        modifier = Modifier
                            .size(9.dp)
                            .clip(CircleShape)
                            .background(catColor)
                    )
                }
                Spacer(modifier = Modifier.width(10.dp))
                Text(
                    text = cat.displayName,
                    fontSize = 13.sp,
                    fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium,
                    color = if (isSelected) catColor else MaterialTheme.colorScheme.onSurface,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    modifier = Modifier.weight(1f)
                )
                Text(
                    text = formatInRupee(spend, currency = currencySymbol),
                    fontSize = 13.sp,
                    fontWeight = FontWeight.SemiBold,
                    color = MaterialTheme.colorScheme.onSurface,
                    maxLines = 1,
                    textAlign = TextAlign.End,
                    modifier = Modifier.widthIn(min = 64.dp)
                )
                Spacer(modifier = Modifier.width(10.dp))
                Text(
                    text = "${String.format(Locale.US, "%.0f", share)}%",
                    fontSize = 12.sp,
                    fontWeight = FontWeight.Medium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    textAlign = TextAlign.End,
                    modifier = Modifier.width(38.dp)
                )
                Spacer(modifier = Modifier.width(4.dp))
                Icon(
                    imageVector = Icons.Default.ChevronRight,
                    contentDescription = null,
                    tint = if (isSelected) catColor else MaterialTheme.colorScheme.outline,
                    modifier = Modifier.size(16.dp)
                )
            }
        }
    }
}

// Visual Component: contextual tip strip under the breakdown, links to category budgets
@Composable
fun InsightTipCard(
    topCategory: Category?,
    topShare: Double,
    period: AnalysisPeriod,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val accent = MaterialTheme.colorScheme.primary
    val periodNoun = when (period) {
        AnalysisPeriod.WEEKLY -> "week"
        AnalysisPeriod.MONTHLY -> "month"
        AnalysisPeriod.YEARLY -> "year"
    }
    val message = if (topCategory != null && topShare > 0.0) {
        "${topCategory.displayName} is ${String.format(Locale.US, "%.0f", topShare)}% of your spend this $periodNoun. Set a budget to keep it in check."
    } else {
        "Keep track of your top spending categories to stay within your budget."
    }

    Row(
        modifier = modifier
            .fillMaxWidth()
            .liquidGlass(shape = RoundedCornerShape(14.dp), strength = 0.7f)
            .clickable { onClick() }
            .padding(start = 12.dp, end = 10.dp, top = 10.dp, bottom = 10.dp)
            .testTag("insight_tip_card"),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Icon(
            imageVector = Icons.Outlined.Lightbulb,
            contentDescription = null,
            tint = accent,
            modifier = Modifier.size(20.dp)
        )
        Spacer(modifier = Modifier.width(10.dp))
        Text(
            text = buildAnnotatedString {
                withStyle(SpanStyle(fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.onSurface)) {
                    append("Tip: ")
                }
                append(message)
            },
            style = MaterialTheme.typography.bodySmall,
            lineHeight = 17.sp,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.weight(1f)
        )
        Spacer(modifier = Modifier.width(6.dp))
        Icon(
            imageVector = Icons.Default.ChevronRight,
            contentDescription = "Manage category budgets",
            tint = accent,
            modifier = Modifier.size(18.dp)
        )
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
    val panelShape = RoundedCornerShape(20.dp)
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .liquidGlass(shape = panelShape, tint = parseHexColor(category.colorHex), elevation = 6.dp)
            .border(1.2.dp, parseHexColor(category.colorHex).copy(alpha = 0.7f), panelShape)
            .testTag("detailed_stats_panel")
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
                    .clip(RoundedCornerShape(12.dp))
                    .background(MaterialTheme.colorScheme.surface.copy(alpha = 0.4f))
                    .padding(horizontal = 12.dp, vertical = 10.dp),
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
                                Icon(
                                    imageVector = subcategoryIconVector(sub, category),
                                    contentDescription = null,
                                    tint = subColor,
                                    modifier = Modifier.size(12.dp)
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
            .padding(horizontal = 12.dp, vertical = 8.dp),
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
                    .size(36.dp)
                    .clip(CircleShape)
                    .background(parseHexColor(visualColor)),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = if (subcategory != null) subcategoryIconVector(subcategory, category)
                                  else getIconVector(category?.iconName ?: "help"),
                    contentDescription = category?.displayName ?: "Unknown category",
                    tint = getContrastColor(visualColor),
                    modifier = Modifier.size(19.dp)
                )
            }

            Spacer(modifier = Modifier.width(11.dp))

            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = transaction.description.ifEmpty { "Transaction Record" },
                    fontWeight = FontWeight.Bold,
                    fontSize = 14.sp,
                    lineHeight = 18.sp,
                    color = MaterialTheme.colorScheme.onSurface,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                Spacer(modifier = Modifier.height(2.dp))

                // Category · subcategory · when, as one quiet metadata line
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(
                        text = category?.displayName ?: "Unknown",
                        fontSize = 11.sp,
                        lineHeight = 14.sp,
                        fontWeight = FontWeight.SemiBold,
                        color = parseHexColor(catColor),
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                        modifier = Modifier.weight(1f, fill = false)
                    )
                    if (subcategory != null) {
                        Text(
                            text = " · ${subcategory.name}",
                            fontSize = 11.sp,
                            lineHeight = 14.sp,
                            color = parseHexColor(subcategory.colorHexOverride ?: catColor),
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis,
                            modifier = Modifier.weight(1f, fill = false)
                        )
                    }
                    Text(
                        text = " • ${formatTransactionDate(transaction.timestamp)}",
                        fontSize = 11.sp,
                        lineHeight = 14.sp,
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
                fontSize = 15.sp,
                color = amountColor,
                maxLines = 1
            )
            if (!effectiveReadOnly) {
                Spacer(modifier = Modifier.width(2.dp))
                IconButton(
                    onClick = onDelete,
                    modifier = Modifier.size(30.dp).testTag("delete_transaction_btn")
                ) {
                    Icon(
                        imageVector = Icons.Default.Delete,
                        contentDescription = "Delete",
                        tint = MaterialTheme.colorScheme.outline.copy(alpha = 0.55f),
                        modifier = Modifier.size(16.dp)
                    )
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
    prefill: Transaction? = null, // seeds a *new* record (e.g. a Money Inbox draft) without entering edit mode
    defaultType: String = "EXPENSE",
    transactions: List<Transaction> = emptyList(),
    defaultMonth: YearMonth? = null,
    onCreateCategoryFirstClick: () -> Unit,
    onDismiss: () -> Unit,
    onSave: (catId: Int, subId: Int?, amount: Double, desc: String, timestamp: Long, type: String) -> Unit
) {
    val haptic = LocalHapticFeedback.current
    val seed = transaction ?: prefill
    var amountStr by remember { mutableStateOf(seed?.amount?.let { formatAmountInput(it) } ?: "") }
    var desc by remember { mutableStateOf(seed?.description ?: "") }
    val initialType = seed?.type ?: defaultType
    var selectedType by remember { mutableStateOf(initialType) }
    val filteredCategories = remember(categories, selectedType) {
        categories.filter { it.type.equals(selectedType, ignoreCase = true) }
    }
    var selectedCatId by remember {
        val initialCats = categories.filter { it.type.equals(initialType, ignoreCase = true) }
        val seededCat = seed?.categoryId?.takeIf { id -> initialCats.any { it.id == id } }
        mutableStateOf<Int?>(seededCat ?: initialCats.firstOrNull()?.id)
    }
    var selectedSubId by remember { mutableStateOf<Int?>(seed?.subcategoryId) }
    var timestamp by remember {
        val initialTimestamp = seed?.timestamp ?: run {
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
                                    imageVector = subcategoryIconVector(sub, categories.find { it.id == selectedCatId }),
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
