package com.example.ui.screens

import androidx.activity.compose.BackHandler
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.core.AnimationSpec
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.slideOutHorizontally
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.ArrowForward
import androidx.compose.material.icons.automirrored.filled.TrendingUp
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.draw.drawWithCache
import androidx.compose.ui.draw.scale
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.BlendMode
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.CompositingStrategy
import androidx.compose.ui.graphics.PathEffect
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.rotate
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.lerp
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.GlassSegmentedControl
import com.example.formatInRupee
import com.example.getContrastColor
import com.example.getIconVector
import com.example.parseHexColor
import com.example.ui.theme.GlassCard
import com.example.ui.theme.accentGlow
import com.example.ui.theme.liquidGlass
import com.example.ui.theme.selectedPillBrush
import com.example.ui.theme.selectedPillRim
import com.example.ui.viewmodel.CategoryViewModel
import com.example.ui.viewmodel.PaletteTheme
import java.time.YearMonth
import java.time.format.DateTimeFormatter
import java.util.Locale
import java.util.UUID
import kotlin.math.cos
import kotlin.math.sin

// ---------------------------------------------------------------------------------------------
// Shared data
// ---------------------------------------------------------------------------------------------

/** A currency the user can pick. Only [symbol] is persisted; the rest is presentation. */
data class CurrencyOption(
    val symbol: String,
    val code: String,
    val name: String,
    val flag: String
)

/** Currencies offered during onboarding and in Settings. Symbols are unique so the stored value stays unambiguous. */
val ShylockCurrencies = listOf(
    CurrencyOption("₹", "INR", "Indian Rupee", "🇮🇳"),
    CurrencyOption("$", "USD", "US Dollar", "🇺🇸"),
    CurrencyOption("€", "EUR", "Euro", "🇪🇺"),
    CurrencyOption("£", "GBP", "British Pound", "🇬🇧"),
    CurrencyOption("¥", "JPY", "Japanese Yen", "🇯🇵"),
    CurrencyOption("A$", "AUD", "Australian Dollar", "🇦🇺"),
    CurrencyOption("C$", "CAD", "Canadian Dollar", "🇨🇦"),
    CurrencyOption("S$", "SGD", "Singapore Dollar", "🇸🇬"),
    CurrencyOption("CHF", "CHF", "Swiss Franc", "🇨🇭"),
    CurrencyOption("AED", "AED", "UAE Dirham", "🇦🇪"),
    CurrencyOption("₩", "KRW", "South Korean Won", "🇰🇷"),
    CurrencyOption("₽", "RUB", "Russian Ruble", "🇷🇺"),
    CurrencyOption("₺", "TRY", "Turkish Lira", "🇹🇷"),
    CurrencyOption("₱", "PHP", "Philippine Peso", "🇵🇭"),
    CurrencyOption("฿", "THB", "Thai Baht", "🇹🇭"),
    CurrencyOption("₫", "VND", "Vietnamese Dong", "🇻🇳"),
    CurrencyOption("₦", "NGN", "Nigerian Naira", "🇳🇬"),
    CurrencyOption("₪", "ILS", "Israeli Shekel", "🇮🇱"),
    CurrencyOption("R$", "BRL", "Brazilian Real", "🇧🇷"),
    CurrencyOption("R", "ZAR", "South African Rand", "🇿🇦")
)

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

private val DefaultOnboardingCategories = listOf(
    // Expense
    OnboardingCategory("1", "Food", "dining", 0.0, listOf("Groceries", "Restaurants", "Coffee")),
    OnboardingCategory("2", "Transportation", "car", 0.0, listOf("Fuel", "Transit", "Taxi")),
    OnboardingCategory("3", "Health", "hospital", 0.0, listOf("Medicine", "Doctor")),
    OnboardingCategory("4", "Housing / Rent", "house", 0.0, listOf("Apartment Rent", "Maintenance")),
    OnboardingCategory("5", "Shopping", "bag", 0.0, listOf("Clothing", "Electronics")),
    OnboardingCategory("6", "Entertainment", "play", 0.0, listOf("Movies", "Games", "Hobbies")),
    OnboardingCategory("7", "Bills & Utilities", "invoice", 0.0, listOf("Electricity", "Water", "Internet")),
    OnboardingCategory("8", "Education", "book", 0.0, listOf("Books", "Tuition")),
    OnboardingCategory("9", "Fitness", "gym", 0.0, listOf("Gym Membership", "Equipment")),
    OnboardingCategory("10", "Travel", "plane", 0.0, listOf("Flights", "Hotels")),
    // Income
    OnboardingCategory("11", "Salary", "wallet", 0.0, listOf("Monthly Pay", "Bonus"), type = "INCOME"),
    OnboardingCategory("12", "Business", "work", 0.0, listOf("Client Invoice", "Consulting"), type = "INCOME"),
    OnboardingCategory("13", "Gifts", "gift", 0.0, listOf("Presents", "Donations"), type = "INCOME"),
    OnboardingCategory("14", "Interest", "trending_up", 0.0, listOf("Dividends", "Savings Interest"), type = "INCOME")
)

private val DefaultCheckedIds = setOf("1", "2", "3", "4", "7", "11", "12")

private const val STEP_WELCOME = 1
private const val STEP_CURRENCY = 2
private const val STEP_STYLE = 3
private const val STEP_CATEGORIES = 4
private const val STEP_PREVIEW = 5
private const val STEP_DONE = 6
private const val TOTAL_STEPS = 6

// Steps that show the segmented progress bar (Currency → Preview)
private val ProgressSteps = STEP_CURRENCY..STEP_PREVIEW

// ---------------------------------------------------------------------------------------------
// Wizard
// ---------------------------------------------------------------------------------------------

/**
 * Six-screen first-run flow: Welcome, Currency, Style, Categories, Preview, All Set.
 *
 * Preferences (currency, theme mode, palette) are written straight to the view model as they are
 * picked so the rest of the flow is rendered in the user's chosen look. Categories are collected
 * locally and only inserted when "Finish Setup" is tapped, and never in tour mode.
 */
@Composable
fun OnboardingWizard(
    viewModel: CategoryViewModel,
    isTour: Boolean = false,
    onComplete: () -> Unit
) {
    val currentTheme by viewModel.currentPaletteTheme.collectAsStateWithLifecycle()
    val currencySymbol by viewModel.currency.collectAsStateWithLifecycle()
    val themeMode by viewModel.themeMode.collectAsStateWithLifecycle()
    val haptic = LocalHapticFeedback.current

    var currentStep by rememberSaveable { mutableIntStateOf(STEP_WELCOME) }

    // Category picks live at the wizard level so they survive moving back and forth
    val checklist = remember { mutableStateListOf<OnboardingCategory>().apply { addAll(DefaultOnboardingCategories) } }
    val checkedMap = remember {
        mutableStateMapOf<String, Boolean>().apply { DefaultOnboardingCategories.forEach { put(it.id, it.id in DefaultCheckedIds) } }
    }

    val paletteColors = viewModel.palettes[currentTheme] ?: listOf("#4D96FF")
    // Stable colour per category: position in the full list, so display, preview and save agree
    fun colorHexFor(cat: OnboardingCategory): String {
        cat.customColorHex?.let { return it }
        val index = checklist.indexOfFirst { it.id == cat.id }.coerceAtLeast(0)
        return paletteColors[index % paletteColors.size]
    }

    fun saveCategories() {
        if (isTour) return
        checklist.filter { checkedMap[it.id] == true }.forEach { cat ->
            viewModel.addCategory(
                name = cat.name,
                colorHex = colorHexFor(cat),
                iconName = cat.iconName,
                budget = cat.budget,
                subcategoriesList = cat.subcategories,
                type = cat.type
            )
        }
    }

    fun goTo(step: Int) {
        haptic.performHapticFeedback(HapticFeedbackType.LongPress)
        currentStep = step
    }

    // System back walks the wizard backwards; on the final screen it behaves like "Go to Home"
    BackHandler(enabled = currentStep in ProgressSteps || currentStep == STEP_DONE) {
        if (currentStep == STEP_DONE) onComplete() else currentStep -= 1
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .testTag("onboarding_card"),
        contentAlignment = Alignment.TopCenter
    ) {
        AnimatedContent(
            targetState = currentStep,
            transitionSpec = {
                val forward = targetState > initialState
                val enter = slideInHorizontally(tween(320)) { if (forward) it / 4 else -it / 4 } + fadeIn(tween(260))
                val exit = slideOutHorizontally(tween(260)) { if (forward) -it / 4 else it / 4 } + fadeOut(tween(200))
                enter togetherWith exit
            },
            modifier = Modifier
                .fillMaxSize()
                .widthIn(max = 520.dp),
            label = "onboarding_step"
        ) { step ->
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(horizontal = 20.dp)
            ) {
                OnboardingHeader(
                    step = step,
                    isTour = isTour,
                    onBack = { goTo(step - 1) },
                    onSkip = onComplete
                )
                when (step) {
                    STEP_WELCOME -> WelcomeStep(onNext = { goTo(STEP_CURRENCY) })
                    STEP_CURRENCY -> CurrencyStep(
                        selectedSymbol = currencySymbol,
                        onSelect = { viewModel.setCurrency(it) },
                        onNext = { goTo(STEP_STYLE) }
                    )
                    STEP_STYLE -> StyleStep(
                        themeMode = themeMode,
                        onThemeMode = { viewModel.setThemeMode(it) },
                        currentPalette = currentTheme,
                        palettes = viewModel.palettes,
                        onPalette = { viewModel.setPaletteTheme(it) },
                        onNext = { goTo(STEP_CATEGORIES) }
                    )
                    STEP_CATEGORIES -> CategoriesStep(
                        checklist = checklist,
                        checkedMap = checkedMap,
                        currencySymbol = currencySymbol,
                        colorHexFor = ::colorHexFor,
                        onBack = { goTo(STEP_STYLE) },
                        onNext = { goTo(STEP_PREVIEW) }
                    )
                    STEP_PREVIEW -> PreviewStep(
                        categories = checklist.filter { it.type == "EXPENSE" && checkedMap[it.id] == true },
                        currencySymbol = currencySymbol,
                        colorHexFor = ::colorHexFor,
                        onBack = { goTo(STEP_CATEGORIES) },
                        onFinish = {
                            saveCategories()
                            goTo(STEP_DONE)
                        }
                    )
                    STEP_DONE -> AllSetStep(
                        isTour = isTour,
                        paletteColors = paletteColors,
                        onDone = onComplete
                    )
                }
            }
        }
    }
}

// ---------------------------------------------------------------------------------------------
// Header: back chevron, segmented progress, skip
// ---------------------------------------------------------------------------------------------

@Composable
private fun OnboardingHeader(
    step: Int,
    isTour: Boolean,
    onBack: () -> Unit,
    onSkip: () -> Unit
) {
    val accent = MaterialTheme.colorScheme.primary
    val showProgress = step in ProgressSteps || step == STEP_DONE
    val showBack = step in ProgressSteps
    val showSkip = step != STEP_DONE

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .height(56.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        if (showBack) {
            IconButton(onClick = onBack, modifier = Modifier.size(40.dp).testTag("onboarding_back_btn")) {
                Icon(
                    imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                    contentDescription = "Back",
                    tint = MaterialTheme.colorScheme.onSurface
                )
            }
        } else {
            Spacer(modifier = Modifier.width(40.dp))
        }

        if (showProgress) {
            val completed = if (step == STEP_DONE) ProgressSteps.count() else step - STEP_CURRENCY + 1
            Row(
                modifier = Modifier
                    .weight(1f)
                    .padding(horizontal = 16.dp),
                horizontalArrangement = Arrangement.spacedBy(6.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                ProgressSteps.forEachIndexed { index, _ ->
                    val active = index < completed
                    val fill by animateFloatAsState(if (active) 1f else 0f, tween(350), label = "progress_$index")
                    Box(
                        modifier = Modifier
                            .weight(1f)
                            .height(6.dp)
                            .clip(RoundedCornerShape(3.dp))
                            .background(MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.45f))
                    ) {
                        Box(
                            modifier = Modifier
                                .fillMaxHeight()
                                .fillMaxWidth(fill)
                                .background(Brush.horizontalGradient(listOf(lerp(accent, Color.White, 0.2f), accent)))
                        )
                    }
                }
            }
        } else {
            Spacer(modifier = Modifier.weight(1f))
        }

        if (showSkip) {
            TextButton(
                onClick = onSkip,
                modifier = Modifier.testTag("onboarding_skip_btn"),
                contentPadding = PaddingValues(horizontal = 8.dp)
            ) {
                Text(
                    text = when {
                        isTour -> "Exit Tour"
                        step == STEP_WELCOME -> "Skip Setup"
                        else -> "Skip"
                    },
                    fontWeight = FontWeight.SemiBold,
                    fontSize = 14.sp,
                    color = accent
                )
            }
        } else {
            Spacer(modifier = Modifier.width(40.dp))
        }
    }
}

// ---------------------------------------------------------------------------------------------
// Step 1 — Welcome
// ---------------------------------------------------------------------------------------------

@Composable
private fun ColumnScope.WelcomeStep(onNext: () -> Unit) {
    val accent = MaterialTheme.colorScheme.primary
    val brandBrush = brandGradient()

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .weight(1f)
            .verticalScroll(rememberScrollState()),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Spacer(modifier = Modifier.height(20.dp))
        GradientIcon(
            icon = Icons.AutoMirrored.Filled.TrendingUp,
            brush = brandBrush,
            size = 72.dp,
            contentDescription = "Shylock logo"
        )
        Spacer(modifier = Modifier.height(20.dp))
        Text(
            text = "Welcome to",
            fontSize = 26.sp,
            lineHeight = 30.sp,
            fontWeight = FontWeight.Medium,
            color = MaterialTheme.colorScheme.onBackground,
            textAlign = TextAlign.Center
        )
        Text(
            text = "Shylock",
            style = TextStyle(brush = brandBrush),
            fontSize = 48.sp,
            lineHeight = 54.sp,
            fontWeight = FontWeight.Black,
            letterSpacing = (-1).sp,
            textAlign = TextAlign.Center
        )
        Spacer(modifier = Modifier.height(14.dp))
        Text(
            text = "A smarter, simpler way to\ntrack your money and build\na better tomorrow.",
            style = MaterialTheme.typography.bodyLarge,
            lineHeight = 24.sp,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            textAlign = TextAlign.Center
        )
        Spacer(modifier = Modifier.height(28.dp))
        WelcomeHeroArt(accent = accent, brandBrush = brandBrush)
        Spacer(modifier = Modifier.height(20.dp))
    }

    OnboardingPrimaryButton(
        text = "Get Started",
        onClick = onNext,
        modifier = Modifier.testTag("onboarding_next_btn1")
    )
    Spacer(modifier = Modifier.height(16.dp))
    PageDots(total = TOTAL_STEPS, current = 0, modifier = Modifier.align(Alignment.CenterHorizontally))
    Spacer(modifier = Modifier.height(16.dp))
}

/** Stacked glass cards with a tiny bar chart — the "Track / Plan / Grow" hero. */
@Composable
private fun WelcomeHeroArt(accent: Color, brandBrush: Brush) {
    val secondary = MaterialTheme.colorScheme.secondary
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .height(230.dp)
            .drawBehind {
                // Ambient glow so the cards have something to float on
                drawCircle(
                    brush = Brush.radialGradient(
                        listOf(accent.copy(alpha = 0.28f), Color.Transparent),
                        center = Offset(size.width * 0.5f, size.height * 0.55f),
                        radius = size.width * 0.45f
                    ),
                    radius = size.width * 0.45f,
                    center = Offset(size.width * 0.5f, size.height * 0.55f)
                )
            },
        contentAlignment = Alignment.Center
    ) {
        // Back card, tilted
        Box(
            modifier = Modifier
                .offset(x = (-26).dp, y = (-18).dp)
                .size(width = 200.dp, height = 140.dp)
                .graphicsLayer { rotationZ = -10f }
                .liquidGlass(shape = RoundedCornerShape(22.dp), strength = 0.55f, elevation = 6.dp)
        )
        // Front card with labels + chart
        Row(
            modifier = Modifier
                .offset(x = 10.dp, y = 12.dp)
                .size(width = 232.dp, height = 150.dp)
                .graphicsLayer { rotationZ = 5f }
                .liquidGlass(shape = RoundedCornerShape(22.dp), strength = 1f, elevation = 12.dp)
                .padding(18.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(
                modifier = Modifier.weight(1f),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                listOf("Track", "Plan", "Grow").forEach {
                    Text(
                        text = it,
                        fontSize = 15.sp,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                }
            }
            Row(
                modifier = Modifier
                    .width(78.dp)
                    .height(90.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalAlignment = Alignment.Bottom
            ) {
                listOf(0.45f, 0.7f, 1f).forEach { fraction ->
                    Box(
                        modifier = Modifier
                            .weight(1f)
                            .fillMaxHeight(fraction)
                            .clip(RoundedCornerShape(topStart = 6.dp, topEnd = 6.dp))
                            .background(Brush.verticalGradient(listOf(secondary, accent)))
                    )
                }
            }
        }
        // Floating badge
        Box(
            modifier = Modifier
                .align(Alignment.BottomEnd)
                .padding(end = 34.dp, bottom = 10.dp)
                .size(52.dp)
                .accentGlow(accent, CircleShape, 10.dp)
                .liquidGlass(shape = CircleShape, strength = 1f),
            contentAlignment = Alignment.Center
        ) {
            GradientIcon(
                icon = Icons.AutoMirrored.Filled.TrendingUp,
                brush = brandBrush,
                size = 26.dp,
                contentDescription = null
            )
        }
    }
}

// ---------------------------------------------------------------------------------------------
// Step 2 — Currency
// ---------------------------------------------------------------------------------------------

@Composable
private fun ColumnScope.CurrencyStep(
    selectedSymbol: String,
    onSelect: (String) -> Unit,
    onNext: () -> Unit
) {
    var query by rememberSaveable { mutableStateOf("") }
    val filtered = remember(query) {
        val q = query.trim()
        if (q.isEmpty()) ShylockCurrencies
        else ShylockCurrencies.filter {
            it.name.contains(q, ignoreCase = true) || it.code.contains(q, ignoreCase = true) || it.symbol.contains(q)
        }
    }

    StepTitle(
        title = "Choose Your\nDefault Currency",
        subtitle = "This symbol will be used across Shylock for all your transactions, stats, and budget metrics."
    )
    Spacer(modifier = Modifier.height(18.dp))
    OnboardingSearchField(
        value = query,
        onValueChange = { query = it },
        placeholder = "Search currency...",
        modifier = Modifier.fillMaxWidth()
    )
    Spacer(modifier = Modifier.height(14.dp))

    LazyColumn(
        modifier = Modifier
            .fillMaxWidth()
            .weight(1f),
        verticalArrangement = Arrangement.spacedBy(10.dp),
        contentPadding = PaddingValues(bottom = 12.dp)
    ) {
        items(filtered, key = { it.code }) { option ->
            val selected = option.symbol == selectedSymbol
            OnboardingOptionRow(
                selected = selected,
                onClick = { onSelect(option.symbol) },
                modifier = Modifier.testTag("onboarding_currency_${option.code}"),
                leading = {
                    Box(
                        modifier = Modifier
                            .size(40.dp)
                            .clip(CircleShape)
                            .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.6f)),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(text = option.flag, fontSize = 20.sp)
                    }
                    Spacer(modifier = Modifier.width(12.dp))
                    Text(
                        text = option.symbol,
                        fontSize = 17.sp,
                        fontWeight = FontWeight.ExtraBold,
                        color = if (selected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurface,
                        modifier = Modifier.widthIn(min = 34.dp)
                    )
                }
            ) {
                Text(
                    text = "${option.name} (${option.code})",
                    style = MaterialTheme.typography.bodyLarge,
                    fontWeight = FontWeight.SemiBold,
                    color = MaterialTheme.colorScheme.onSurface,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }
        }
        if (filtered.isEmpty()) {
            item {
                Text(
                    text = "No currency matches \"$query\"",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    textAlign = TextAlign.Center,
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(top = 24.dp)
                )
            }
        }
    }

    Spacer(modifier = Modifier.height(8.dp))
    OnboardingPrimaryButton(
        text = "Next",
        onClick = onNext,
        modifier = Modifier.testTag("onboarding_next_btn2")
    )
    Spacer(modifier = Modifier.height(20.dp))
}

// ---------------------------------------------------------------------------------------------
// Step 3 — Style & Appearance
// ---------------------------------------------------------------------------------------------

@Composable
private fun ColumnScope.StyleStep(
    themeMode: String,
    onThemeMode: (String) -> Unit,
    currentPalette: PaletteTheme,
    palettes: Map<PaletteTheme, List<String>>,
    onPalette: (PaletteTheme) -> Unit,
    onNext: () -> Unit
) {
    StepTitle(
        title = "Style & Appearance",
        subtitle = "Make it yours! Choose a theme mode and color palette that matches your style."
    )
    Spacer(modifier = Modifier.height(18.dp))

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .weight(1f)
            .verticalScroll(rememberScrollState()),
        verticalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            listOf(
                Triple("SYSTEM", "System", Icons.Default.DesktopWindows),
                Triple("LIGHT", "Light", Icons.Default.LightMode),
                Triple("DARK", "Dark", Icons.Default.DarkMode)
            ).forEach { (value, label, icon) ->
                ThemeModeTile(
                    label = label,
                    icon = icon,
                    selected = themeMode == value,
                    onClick = { onThemeMode(value) },
                    modifier = Modifier
                        .weight(1f)
                        .testTag("onboarding_mode_$value")
                )
            }
        }

        Spacer(modifier = Modifier.height(6.dp))
        Text(
            text = "Color Palette",
            style = MaterialTheme.typography.titleSmall,
            fontWeight = FontWeight.Bold,
            color = MaterialTheme.colorScheme.onBackground
        )
        Text(
            text = "This will be used for category colors, charts and highlights.",
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )

        PaletteTheme.values().forEach { palette ->
            val colors = palettes[palette] ?: emptyList()
            OnboardingOptionRow(
                selected = palette == currentPalette,
                onClick = { onPalette(palette) },
                modifier = Modifier.testTag("onboarding_palette_${palette.name}")
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(
                        text = palette.name.lowercase().replaceFirstChar { it.uppercase() },
                        style = MaterialTheme.typography.bodyLarge,
                        fontWeight = FontWeight.SemiBold,
                        color = MaterialTheme.colorScheme.onSurface,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                        modifier = Modifier.weight(1f)
                    )
                    Spacer(modifier = Modifier.width(10.dp))
                    Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                        colors.take(6).forEach { hex ->
                            Box(
                                modifier = Modifier
                                    .size(18.dp)
                                    .clip(RoundedCornerShape(5.dp))
                                    .background(parseHexColor(hex))
                                    .border(1.dp, Color.White.copy(alpha = 0.18f), RoundedCornerShape(5.dp))
                            )
                        }
                    }
                }
            }
        }
        Spacer(modifier = Modifier.height(4.dp))
    }

    Spacer(modifier = Modifier.height(8.dp))
    OnboardingPrimaryButton(
        text = "Next",
        onClick = onNext,
        modifier = Modifier.testTag("onboarding_next_btn3")
    )
    Spacer(modifier = Modifier.height(20.dp))
}

@Composable
private fun ThemeModeTile(
    label: String,
    icon: ImageVector,
    selected: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val accent = MaterialTheme.colorScheme.primary
    val shape = RoundedCornerShape(18.dp)
    Box(
        modifier = modifier
            .height(104.dp)
            .then(if (selected) Modifier.accentGlow(accent, shape, 8.dp) else Modifier)
            .liquidGlass(shape = shape, strength = if (selected) 1f else 0.7f)
            .then(if (selected) Modifier.border(1.5.dp, accent, shape) else Modifier)
            .clickable(onClick = onClick)
    ) {
        Column(
            modifier = Modifier.fillMaxSize(),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            Icon(
                imageVector = icon,
                contentDescription = null,
                tint = if (selected) accent else MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.size(28.dp)
            )
            Spacer(modifier = Modifier.height(10.dp))
            Text(
                text = label,
                fontSize = 13.sp,
                fontWeight = if (selected) FontWeight.Bold else FontWeight.Medium,
                color = if (selected) MaterialTheme.colorScheme.onSurface else MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
        if (selected) {
            Icon(
                imageVector = Icons.Default.CheckCircle,
                contentDescription = "Selected",
                tint = accent,
                modifier = Modifier
                    .align(Alignment.BottomEnd)
                    .padding(8.dp)
                    .size(18.dp)
            )
        }
    }
}

// ---------------------------------------------------------------------------------------------
// Step 4 — Categories
// ---------------------------------------------------------------------------------------------

@Composable
private fun ColumnScope.CategoriesStep(
    checklist: MutableList<OnboardingCategory>,
    checkedMap: MutableMap<String, Boolean>,
    currencySymbol: String,
    colorHexFor: (OnboardingCategory) -> String,
    onBack: () -> Unit,
    onNext: () -> Unit
) {
    var tab by rememberSaveable { mutableStateOf("EXPENSE") }
    var detailTarget by remember { mutableStateOf<OnboardingCategory?>(null) }
    var showAddCustom by remember { mutableStateOf(false) }

    val expenseCount = checklist.count { it.type == "EXPENSE" && checkedMap[it.id] == true }
    val incomeCount = checklist.count { it.type == "INCOME" && checkedMap[it.id] == true }
    val displayed = checklist.filter { it.type == tab }

    StepTitle(
        title = "Set Up Your Categories",
        subtitle = "Choose your starting budget categories. Uncheck any to exclude, or add custom ones."
    )
    Spacer(modifier = Modifier.height(16.dp))

    GlassSegmentedControl(
        options = listOf("Expenses ($expenseCount)", "Income ($incomeCount)"),
        selectedIndex = if (tab == "INCOME") 1 else 0,
        onSelect = { tab = if (it == 1) "INCOME" else "EXPENSE" },
        expand = true,
        height = 44.dp,
        fontSize = 13.sp,
        modifier = Modifier
            .fillMaxWidth()
            .testTag("onboarding_type_tabs")
    )
    Spacer(modifier = Modifier.height(12.dp))

    LazyColumn(
        modifier = Modifier
            .fillMaxWidth()
            .weight(1f),
        verticalArrangement = Arrangement.spacedBy(10.dp),
        contentPadding = PaddingValues(bottom = 12.dp)
    ) {
        items(displayed, key = { it.id }) { cat ->
            CategoryChecklistRow(
                category = cat,
                checked = checkedMap[cat.id] == true,
                colorHex = colorHexFor(cat),
                currencySymbol = currencySymbol,
                onToggle = { checkedMap[cat.id] = !(checkedMap[cat.id] ?: false) },
                onOpen = { detailTarget = cat }
            )
        }
        item {
            AddCustomCategoryButton(onClick = { showAddCustom = true })
        }
    }

    Spacer(modifier = Modifier.height(8.dp))
    Row(
        modifier = Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically
    ) {
        OnboardingBackButton(onClick = onBack, modifier = Modifier.weight(0.7f))
        Spacer(modifier = Modifier.width(12.dp))
        OnboardingPrimaryButton(
            text = "Next",
            onClick = onNext,
            modifier = Modifier
                .weight(1.3f)
                .testTag("onboarding_next_btn4")
        )
    }
    Spacer(modifier = Modifier.height(20.dp))

    detailTarget?.let { target ->
        // Read the live entry so edits made in the sheet are reflected immediately
        val live = checklist.firstOrNull { it.id == target.id } ?: target
        CategoryDetailSheet(
            category = live,
            included = checkedMap[live.id] == true,
            colorHex = colorHexFor(live),
            currencySymbol = currencySymbol,
            onIncludedChange = { checkedMap[live.id] = it },
            onUpdate = { updated ->
                val index = checklist.indexOfFirst { it.id == updated.id }
                if (index >= 0) checklist[index] = updated
            },
            onRemove = if (!live.isCustom) null else fun() {
                checklist.removeAll { it.id == live.id }
                checkedMap.remove(live.id)
                detailTarget = null
            },
            onDismiss = { detailTarget = null }
        )
    }

    if (showAddCustom) {
        AddCustomCategoryDialog(
            type = tab,
            onDismiss = { showAddCustom = false },
            onAdd = { name ->
                val trimmed = name.trim()
                if (trimmed.isNotEmpty() && checklist.none { it.name.equals(trimmed, ignoreCase = true) }) {
                    val newCat = OnboardingCategory(
                        id = UUID.randomUUID().toString(),
                        name = trimmed,
                        iconName = if (tab == "INCOME") "wallet" else "category",
                        budget = 0.0,
                        subcategories = emptyList(),
                        isCustom = true,
                        type = tab
                    )
                    checklist.add(newCat)
                    checkedMap[newCat.id] = true
                }
                showAddCustom = false
            }
        )
    }
}

@Composable
private fun CategoryChecklistRow(
    category: OnboardingCategory,
    checked: Boolean,
    colorHex: String,
    currencySymbol: String,
    onToggle: () -> Unit,
    onOpen: () -> Unit
) {
    val accent = MaterialTheme.colorScheme.primary
    val shape = RoundedCornerShape(16.dp)
    val catColor = parseHexColor(colorHex)
    val subtitle = buildString {
        append(category.subcategories.joinToString(", ").ifEmpty { "No subcategories" })
        if (category.budget > 0) append("  •  ${formatInRupee(category.budget, currencySymbol)}/mo")
    }

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .liquidGlass(shape = shape, strength = if (checked) 0.95f else 0.6f)
            .then(if (checked) Modifier.border(1.dp, accent.copy(alpha = 0.35f), shape) else Modifier)
            .clickable(onClick = onToggle)
            .padding(start = 12.dp, end = 4.dp, top = 10.dp, bottom = 10.dp)
            .alpha(if (checked) 1f else 0.72f)
            .testTag("onboarding_cat_${category.id}"),
        verticalAlignment = Alignment.CenterVertically
    ) {
        GlassCheckbox(checked = checked, accent = accent)
        Spacer(modifier = Modifier.width(12.dp))
        Box(
            modifier = Modifier
                .size(40.dp)
                .clip(RoundedCornerShape(12.dp))
                .background(catColor),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                imageVector = getIconVector(category.iconName),
                contentDescription = null,
                tint = getContrastColor(colorHex),
                modifier = Modifier.size(20.dp)
            )
        }
        Spacer(modifier = Modifier.width(12.dp))
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = category.name,
                style = MaterialTheme.typography.bodyLarge,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onSurface,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
            Text(
                text = subtitle,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
        }
        IconButton(onClick = onOpen, modifier = Modifier.size(40.dp)) {
            Icon(
                imageVector = Icons.Default.ChevronRight,
                contentDescription = "Edit ${category.name}",
                tint = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}

/** Rounded-square check box that reads as a lit accent tile when on, an outline when off. */
@Composable
private fun GlassCheckbox(checked: Boolean, accent: Color) {
    val shape = RoundedCornerShape(7.dp)
    Box(
        modifier = Modifier
            .size(24.dp)
            .clip(shape)
            .background(if (checked) accent else Color.Transparent)
            .border(
                width = if (checked) 0.dp else 1.5.dp,
                color = if (checked) Color.Transparent else MaterialTheme.colorScheme.outline.copy(alpha = 0.7f),
                shape = shape
            ),
        contentAlignment = Alignment.Center
    ) {
        if (checked) {
            Icon(
                imageVector = Icons.Default.Check,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.onPrimary,
                modifier = Modifier.size(16.dp)
            )
        }
    }
}

@Composable
private fun AddCustomCategoryButton(onClick: () -> Unit) {
    val accent = MaterialTheme.colorScheme.primary
    val shape = RoundedCornerShape(16.dp)
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(shape)
            .drawBehind {
                val stroke = Stroke(
                    width = 1.5.dp.toPx(),
                    pathEffect = PathEffect.dashPathEffect(floatArrayOf(10f, 8f))
                )
                drawRoundRect(
                    color = accent.copy(alpha = 0.7f),
                    style = stroke,
                    cornerRadius = CornerRadius(16.dp.toPx())
                )
            }
            .background(accent.copy(alpha = 0.06f))
            .clickable(onClick = onClick)
            .padding(horizontal = 14.dp, vertical = 12.dp)
            .testTag("onboarding_add_custom_btn"),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Box(
            modifier = Modifier
                .size(32.dp)
                .clip(RoundedCornerShape(9.dp))
                .border(1.dp, accent.copy(alpha = 0.6f), RoundedCornerShape(9.dp)),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                imageVector = Icons.Default.Add,
                contentDescription = null,
                tint = accent,
                modifier = Modifier.size(18.dp)
            )
        }
        Spacer(modifier = Modifier.width(14.dp))
        Text(
            text = "Add Custom Category",
            style = MaterialTheme.typography.bodyLarge,
            fontWeight = FontWeight.SemiBold,
            color = MaterialTheme.colorScheme.onSurface
        )
    }
}

@Composable
private fun AddCustomCategoryDialog(
    type: String,
    onDismiss: () -> Unit,
    onAdd: (String) -> Unit
) {
    var name by remember { mutableStateOf("") }
    val isIncome = type == "INCOME"
    Dialog(onDismissRequest = onDismiss) {
        GlassCard(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(24.dp),
            elevation = 12.dp,
            contentPadding = PaddingValues(22.dp)
        ) {
            Text(
                text = if (isIncome) "New Income Category" else "New Expense Category",
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onSurface
            )
            Text(
                text = "You can add subcategories and a budget after creating it.",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            Spacer(modifier = Modifier.height(16.dp))
            OutlinedTextField(
                value = name,
                onValueChange = { name = it },
                label = { Text("Category name") },
                placeholder = { Text(if (isIncome) "E.g., Freelance, Dividends" else "E.g., Subscriptions, Pets") },
                singleLine = true,
                shape = RoundedCornerShape(14.dp),
                modifier = Modifier
                    .fillMaxWidth()
                    .testTag("onboarding_custom_name")
            )
            Spacer(modifier = Modifier.height(18.dp))
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.End,
                verticalAlignment = Alignment.CenterVertically
            ) {
                TextButton(onClick = onDismiss) {
                    Text("Cancel", color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
                Spacer(modifier = Modifier.width(8.dp))
                OnboardingPrimaryButton(
                    text = "Add",
                    icon = Icons.Default.Add,
                    enabled = name.isNotBlank(),
                    onClick = { onAdd(name) },
                    height = 44.dp,
                    modifier = Modifier
                        .width(120.dp)
                        .testTag("onboarding_custom_add")
                )
            }
        }
    }
}

/** Bottom sheet to fine-tune one starter category: inclusion, monthly budget, subcategories. */
@OptIn(ExperimentalMaterial3Api::class, ExperimentalLayoutApi::class)
@Composable
private fun CategoryDetailSheet(
    category: OnboardingCategory,
    included: Boolean,
    colorHex: String,
    currencySymbol: String,
    onIncludedChange: (Boolean) -> Unit,
    onUpdate: (OnboardingCategory) -> Unit,
    onRemove: (() -> Unit)?,
    onDismiss: () -> Unit
) {
    val accent = MaterialTheme.colorScheme.primary
    var budgetText by remember(category.id) {
        mutableStateOf(if (category.budget > 0) category.budget.toBigDecimal().stripTrailingZeros().toPlainString() else "")
    }
    var newSub by remember(category.id) { mutableStateOf("") }

    fun commitBudget() {
        val parsed = budgetText.toDoubleOrNull()?.coerceAtLeast(0.0) ?: 0.0
        if (parsed != category.budget) onUpdate(category.copy(budget = parsed))
    }

    ModalBottomSheet(
        onDismissRequest = { commitBudget(); onDismiss() },
        sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true),
        containerColor = MaterialTheme.colorScheme.surface
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 20.dp)
                .padding(bottom = 32.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Box(
                    modifier = Modifier
                        .size(46.dp)
                        .clip(RoundedCornerShape(14.dp))
                        .background(parseHexColor(colorHex)),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = getIconVector(category.iconName),
                        contentDescription = null,
                        tint = getContrastColor(colorHex),
                        modifier = Modifier.size(22.dp)
                    )
                }
                Spacer(modifier = Modifier.width(14.dp))
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = category.name,
                        style = MaterialTheme.typography.titleLarge,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                    Text(
                        text = if (category.type == "INCOME") "Income category" else "Expense category",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
                Switch(
                    checked = included,
                    onCheckedChange = onIncludedChange,
                    colors = SwitchDefaults.colors(checkedTrackColor = accent)
                )
            }

            if (category.type == "EXPENSE") {
                OutlinedTextField(
                    value = budgetText,
                    onValueChange = { input ->
                        if (input.isEmpty() || input.matches(Regex("^\\d{0,9}(\\.\\d{0,2})?$"))) budgetText = input
                    },
                    label = { Text("Monthly budget (optional)") },
                    prefix = { Text(currencySymbol, fontWeight = FontWeight.Bold, color = accent) },
                    placeholder = { Text("0") },
                    singleLine = true,
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                    shape = RoundedCornerShape(14.dp),
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("onboarding_budget_field")
                )
            }

            Text(
                text = "Subcategories",
                style = MaterialTheme.typography.titleSmall,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onSurface
            )
            if (category.subcategories.isEmpty()) {
                Text(
                    text = "None yet — add a few to organise this category.",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            } else {
                FlowRow(
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    category.subcategories.forEach { sub ->
                        InputChip(
                            selected = false,
                            onClick = { onUpdate(category.copy(subcategories = category.subcategories - sub)) },
                            label = { Text(sub) },
                            trailingIcon = {
                                Icon(
                                    imageVector = Icons.Default.Close,
                                    contentDescription = "Remove $sub",
                                    modifier = Modifier.size(16.dp)
                                )
                            },
                            shape = RoundedCornerShape(50)
                        )
                    }
                }
            }
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                OutlinedTextField(
                    value = newSub,
                    onValueChange = { newSub = it },
                    placeholder = { Text("Add subcategory") },
                    singleLine = true,
                    shape = RoundedCornerShape(14.dp),
                    modifier = Modifier.weight(1f)
                )
                val canAdd = newSub.isNotBlank() && category.subcategories.none { it.equals(newSub.trim(), ignoreCase = true) }
                FilledIconButton(
                    onClick = {
                        onUpdate(category.copy(subcategories = category.subcategories + newSub.trim()))
                        newSub = ""
                    },
                    enabled = canAdd,
                    shape = RoundedCornerShape(12.dp),
                    modifier = Modifier.size(48.dp)
                ) {
                    Icon(imageVector = Icons.Default.Add, contentDescription = "Add subcategory")
                }
            }

            Spacer(modifier = Modifier.height(4.dp))
            Row(modifier = Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                if (onRemove != null) {
                    TextButton(onClick = onRemove) {
                        Text("Remove", color = MaterialTheme.colorScheme.error, fontWeight = FontWeight.SemiBold)
                    }
                    Spacer(modifier = Modifier.width(8.dp))
                }
                OnboardingPrimaryButton(
                    text = "Done",
                    icon = Icons.Default.Check,
                    onClick = { commitBudget(); onDismiss() },
                    modifier = Modifier
                        .weight(1f)
                        .testTag("onboarding_detail_done")
                )
            }
        }
    }
}

// ---------------------------------------------------------------------------------------------
// Step 5 — Preview
// ---------------------------------------------------------------------------------------------

@Composable
private fun ColumnScope.PreviewStep(
    categories: List<OnboardingCategory>,
    currencySymbol: String,
    colorHexFor: (OnboardingCategory) -> String,
    onBack: () -> Unit,
    onFinish: () -> Unit
) {
    val accent = MaterialTheme.colorScheme.primary
    val totalBudget = categories.sumOf { it.budget }
    val hasBudgets = totalBudget > 0
    // With no budgets entered yet, weight every category equally so the ring still previews the palette
    val weights = categories.map { if (hasBudgets) it.budget else 1.0 }
    val weightSum = weights.sum().takeIf { it > 0 } ?: 1.0
    val monthLabel = remember { YearMonth.now().format(DateTimeFormatter.ofPattern("MMMM yyyy", Locale.getDefault())) }

    StepTitle(
        title = "Almost There!",
        subtitle = "Here's a preview of how your categories will look in Shylock."
    )
    Spacer(modifier = Modifier.height(18.dp))

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .weight(1f)
            .verticalScroll(rememberScrollState())
    ) {
        GlassCard(
            modifier = Modifier
                .fillMaxWidth()
                .testTag("onboarding_preview_card"),
            shape = RoundedCornerShape(24.dp),
            elevation = 10.dp,
            contentPadding = PaddingValues(18.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Box(
                    modifier = Modifier
                        .size(34.dp)
                        .clip(RoundedCornerShape(10.dp))
                        .background(accent.copy(alpha = 0.16f))
                        .border(1.dp, accent.copy(alpha = 0.3f), RoundedCornerShape(10.dp)),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = Icons.Default.CalendarMonth,
                        contentDescription = null,
                        tint = accent,
                        modifier = Modifier.size(18.dp)
                    )
                }
                Spacer(modifier = Modifier.width(10.dp))
                Text(
                    text = "Monthly Budget",
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onSurface,
                    modifier = Modifier.weight(1f)
                )
                Row(
                    modifier = Modifier
                        .liquidGlass(shape = RoundedCornerShape(12.dp), strength = 0.7f)
                        .padding(horizontal = 10.dp, vertical = 6.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = monthLabel,
                        fontSize = 12.sp,
                        fontWeight = FontWeight.SemiBold,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                    Spacer(modifier = Modifier.width(4.dp))
                    Icon(
                        imageVector = Icons.Default.ExpandMore,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.size(16.dp)
                    )
                }
            }

            Spacer(modifier = Modifier.height(18.dp))

            if (categories.isEmpty()) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 28.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Icon(
                        imageVector = Icons.Default.PieChart,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.outline,
                        modifier = Modifier.size(44.dp)
                    )
                    Spacer(modifier = Modifier.height(10.dp))
                    Text(
                        text = "No expense categories selected.\nGo back and pick at least one to see a preview.",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        textAlign = TextAlign.Center
                    )
                }
            } else {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(190.dp),
                    contentAlignment = Alignment.Center
                ) {
                    BudgetRing(
                        colors = categories.map { parseHexColor(colorHexFor(it)) },
                        weights = weights,
                        modifier = Modifier.size(180.dp)
                    )
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Text(
                            text = if (hasBudgets) "Total Budget" else "Budgets",
                            style = MaterialTheme.typography.labelMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        Text(
                            text = if (hasBudgets) formatInRupee(totalBudget, currencySymbol) else "Not set",
                            fontSize = if (hasBudgets) 22.sp else 18.sp,
                            fontWeight = FontWeight.ExtraBold,
                            color = MaterialTheme.colorScheme.onSurface,
                            maxLines = 1
                        )
                    }
                }

                Spacer(modifier = Modifier.height(14.dp))

                categories.forEachIndexed { index, cat ->
                    val share = weights[index] / weightSum
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 7.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Box(
                            modifier = Modifier
                                .size(12.dp)
                                .clip(CircleShape)
                                .background(parseHexColor(colorHexFor(cat)))
                        )
                        Spacer(modifier = Modifier.width(12.dp))
                        Text(
                            text = cat.name,
                            style = MaterialTheme.typography.bodyMedium,
                            fontWeight = FontWeight.Medium,
                            color = MaterialTheme.colorScheme.onSurface,
                            modifier = Modifier.weight(1f),
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                        Text(
                            text = if (cat.budget > 0) formatInRupee(cat.budget, currencySymbol) else "—",
                            style = MaterialTheme.typography.bodyMedium,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                        Spacer(modifier = Modifier.width(14.dp))
                        Text(
                            text = "${(share * 100).toInt()}%",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier.widthIn(min = 34.dp),
                            textAlign = TextAlign.End
                        )
                    }
                }

                if (!hasBudgets) {
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(
                        text = "Tip: tap a category's arrow in the previous step to set a monthly budget.",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.outline,
                        textAlign = TextAlign.Center,
                        modifier = Modifier.fillMaxWidth()
                    )
                }
            }
        }
        Spacer(modifier = Modifier.height(12.dp))
    }

    Spacer(modifier = Modifier.height(8.dp))
    Row(
        modifier = Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically
    ) {
        OnboardingBackButton(onClick = onBack, modifier = Modifier.weight(0.7f))
        Spacer(modifier = Modifier.width(12.dp))
        OnboardingPrimaryButton(
            text = "Finish Setup",
            onClick = onFinish,
            modifier = Modifier
                .weight(1.3f)
                .testTag("onboarding_finish_btn")
        )
    }
    Spacer(modifier = Modifier.height(20.dp))
}

/** Segmented ring; slices are separated by small gaps and sweep in on first composition. */
@Composable
private fun BudgetRing(
    colors: List<Color>,
    weights: List<Double>,
    modifier: Modifier = Modifier
) {
    val progress by rememberEnterProgress(tween(900))
    val total = weights.sum().takeIf { it > 0 } ?: 1.0
    Canvas(modifier = modifier) {
        val strokeWidth = 24.dp.toPx()
        val inset = strokeWidth / 2
        val arcSize = Size(size.width - strokeWidth, size.height - strokeWidth)
        val gap = if (colors.size > 1) 3f else 0f
        var start = -90f
        colors.forEachIndexed { index, color ->
            val sweep = (360f * (weights[index] / total)).toFloat() * progress
            val visible = (sweep - gap).coerceAtLeast(0f)
            if (visible > 0f) {
                drawArc(
                    color = color,
                    startAngle = start + gap / 2,
                    sweepAngle = visible,
                    useCenter = false,
                    topLeft = Offset(inset, inset),
                    size = arcSize,
                    style = Stroke(width = strokeWidth, cap = StrokeCap.Butt)
                )
            }
            start += sweep
        }
    }
}

// ---------------------------------------------------------------------------------------------
// Step 6 — All set
// ---------------------------------------------------------------------------------------------

@Composable
private fun ColumnScope.AllSetStep(
    isTour: Boolean,
    paletteColors: List<String>,
    onDone: () -> Unit
) {
    val accent = MaterialTheme.colorScheme.primary
    val brandBrush = brandGradient()

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .weight(1f)
            .verticalScroll(rememberScrollState()),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Spacer(modifier = Modifier.height(12.dp))
        CelebrationArt(accent = accent, brandBrush = brandBrush, confettiColors = paletteColors.map { parseHexColor(it) })
        Spacer(modifier = Modifier.height(18.dp))
        Text(
            text = "You're All Set!",
            fontSize = 32.sp,
            lineHeight = 38.sp,
            fontWeight = FontWeight.Black,
            color = MaterialTheme.colorScheme.onBackground,
            textAlign = TextAlign.Center
        )
        Spacer(modifier = Modifier.height(8.dp))
        Text(
            text = "Time to take control of your money\nand make it count.",
            style = MaterialTheme.typography.bodyLarge,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            textAlign = TextAlign.Center
        )
        Spacer(modifier = Modifier.height(26.dp))
        Column(
            modifier = Modifier.fillMaxWidth(),
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            FeatureRow(icon = Icons.Default.BarChart, text = "Track your expenses")
            FeatureRow(icon = Icons.Default.TrackChanges, text = "Stay within budget")
            FeatureRow(icon = Icons.Default.EmojiEvents, text = "Achieve your financial goals")
        }
        Spacer(modifier = Modifier.height(20.dp))
    }

    OnboardingPrimaryButton(
        text = if (isTour) "Finish Tour" else "Go to Home",
        onClick = onDone,
        modifier = Modifier.testTag("onboarding_done_btn")
    )
    Spacer(modifier = Modifier.height(20.dp))
}

@Composable
private fun FeatureRow(icon: ImageVector, text: String) {
    val accent = MaterialTheme.colorScheme.primary
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .liquidGlass(shape = RoundedCornerShape(16.dp), strength = 0.85f)
            .padding(horizontal = 14.dp, vertical = 12.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Box(
            modifier = Modifier
                .size(38.dp)
                .clip(RoundedCornerShape(11.dp))
                .background(accent.copy(alpha = 0.16f))
                .border(1.dp, accent.copy(alpha = 0.3f), RoundedCornerShape(11.dp)),
            contentAlignment = Alignment.Center
        ) {
            Icon(imageVector = icon, contentDescription = null, tint = accent, modifier = Modifier.size(20.dp))
        }
        Spacer(modifier = Modifier.width(14.dp))
        Text(
            text = text,
            style = MaterialTheme.typography.bodyLarge,
            fontWeight = FontWeight.SemiBold,
            color = MaterialTheme.colorScheme.onSurface
        )
    }
}

/** Glowing ring with the brand mark, surrounded by a burst of palette-coloured confetti. */
@Composable
private fun CelebrationArt(accent: Color, brandBrush: Brush, confettiColors: List<Color>) {
    val pop by rememberEnterProgress(spring(dampingRatio = Spring.DampingRatioMediumBouncy, stiffness = Spring.StiffnessLow))
    val colors = confettiColors.ifEmpty { listOf(accent) }
    // Fixed scatter so the burst looks designed rather than random on every recomposition
    val particles = remember(colors) {
        List(22) { i ->
            val angle = (i * 360f / 22f) + (if (i % 2 == 0) 9f else -6f)
            val distance = 0.62f + (i % 4) * 0.09f
            val length = 10f + (i % 3) * 5f
            Confetti(angle, distance, length, colors[i % colors.size], round = i % 5 == 0)
        }
    }

    Box(
        modifier = Modifier
            .fillMaxWidth()
            .height(230.dp),
        contentAlignment = Alignment.Center
    ) {
        Canvas(modifier = Modifier.fillMaxSize()) {
            val center = Offset(size.width / 2, size.height / 2)
            val reach = size.height / 2
            particles.forEach { p ->
                val rad = Math.toRadians(p.angle.toDouble())
                val dist = reach * p.distance * pop
                val pos = Offset(center.x + (cos(rad) * dist).toFloat(), center.y + (sin(rad) * dist).toFloat())
                val len = p.length.dp.toPx()
                if (p.round) {
                    drawCircle(color = p.color.copy(alpha = pop), radius = len / 3, center = pos)
                } else {
                    rotate(degrees = p.angle + 35f, pivot = pos) {
                        drawRoundRect(
                            color = p.color.copy(alpha = pop),
                            topLeft = Offset(pos.x - len / 2, pos.y - len / 6),
                            size = Size(len, len / 3),
                            cornerRadius = CornerRadius(len / 6)
                        )
                    }
                }
            }
        }
        Box(
            modifier = Modifier
                .size(128.dp)
                .scale(0.6f + 0.4f * pop)
                .accentGlow(accent, CircleShape, 18.dp)
                .liquidGlass(shape = CircleShape, strength = 1f)
                .border(3.dp, accent, CircleShape),
            contentAlignment = Alignment.Center
        ) {
            GradientIcon(
                icon = Icons.AutoMirrored.Filled.TrendingUp,
                brush = brandBrush,
                size = 60.dp,
                contentDescription = "Setup complete"
            )
        }
    }
}

private data class Confetti(
    val angle: Float,
    val distance: Float,
    val length: Float,
    val color: Color,
    val round: Boolean
)

// ---------------------------------------------------------------------------------------------
// Shared building blocks
// ---------------------------------------------------------------------------------------------

/** 0 → 1 once, starting the frame after first composition, so enter animations actually play. */
@Composable
private fun rememberEnterProgress(spec: AnimationSpec<Float>): State<Float> {
    var started by remember { mutableStateOf(false) }
    LaunchedEffect(Unit) { started = true }
    return animateFloatAsState(targetValue = if (started) 1f else 0f, animationSpec = spec, label = "enter_progress")
}

@Composable
private fun brandGradient(): Brush {
    val scheme = MaterialTheme.colorScheme
    return Brush.linearGradient(listOf(scheme.secondary, scheme.primary, lerp(scheme.primary, Color.White, 0.25f)))
}

/** Tints a vector icon with a brush instead of a flat colour. */
@Composable
private fun GradientIcon(
    icon: ImageVector,
    brush: Brush,
    size: Dp,
    contentDescription: String?
) {
    Icon(
        imageVector = icon,
        contentDescription = contentDescription,
        tint = Color.White,
        modifier = Modifier
            .size(size)
            .graphicsLayer { compositingStrategy = CompositingStrategy.Offscreen }
            .drawWithCache {
                onDrawWithContent {
                    drawContent()
                    drawRect(brush = brush, blendMode = BlendMode.SrcAtop)
                }
            }
    )
}

@Composable
private fun StepTitle(title: String, subtitle: String) {
    Column(
        modifier = Modifier.fillMaxWidth(),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Spacer(modifier = Modifier.height(6.dp))
        Text(
            text = title,
            fontSize = 24.sp,
            lineHeight = 30.sp,
            fontWeight = FontWeight.ExtraBold,
            color = MaterialTheme.colorScheme.onBackground,
            textAlign = TextAlign.Center
        )
        Spacer(modifier = Modifier.height(8.dp))
        Text(
            text = subtitle,
            style = MaterialTheme.typography.bodyMedium,
            lineHeight = 20.sp,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            textAlign = TextAlign.Center,
            modifier = Modifier.padding(horizontal = 8.dp)
        )
    }
}

@Composable
private fun PageDots(total: Int, current: Int, modifier: Modifier = Modifier) {
    val accent = MaterialTheme.colorScheme.primary
    Row(modifier = modifier, horizontalArrangement = Arrangement.spacedBy(6.dp), verticalAlignment = Alignment.CenterVertically) {
        repeat(total) { index ->
            val active = index == current
            Box(
                modifier = Modifier
                    .height(6.dp)
                    .width(if (active) 18.dp else 6.dp)
                    .clip(CircleShape)
                    .background(if (active) accent else MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.7f))
            )
        }
    }
}

/** Glowing accent pill — the wizard's one call-to-action per screen. */
@Composable
private fun OnboardingPrimaryButton(
    text: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    icon: ImageVector = Icons.AutoMirrored.Filled.ArrowForward,
    enabled: Boolean = true,
    height: Dp = 54.dp
) {
    val accent = MaterialTheme.colorScheme.primary
    val shape = RoundedCornerShape(50)
    Row(
        modifier = modifier
            .fillMaxWidth()
            .height(height)
            .then(if (enabled) Modifier.accentGlow(accent, shape, 12.dp) else Modifier)
            .clip(shape)
            .background(selectedPillBrush(accent))
            .border(0.8.dp, selectedPillRim(accent), shape)
            .alpha(if (enabled) 1f else 0.5f)
            .clickable(enabled = enabled, onClick = onClick)
            .padding(horizontal = 20.dp),
        horizontalArrangement = Arrangement.Center,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(
            text = text,
            fontSize = 16.sp,
            fontWeight = FontWeight.Bold,
            color = MaterialTheme.colorScheme.onPrimary,
            maxLines = 1
        )
        Spacer(modifier = Modifier.width(8.dp))
        Icon(
            imageVector = icon,
            contentDescription = null,
            tint = MaterialTheme.colorScheme.onPrimary,
            modifier = Modifier.size(20.dp)
        )
    }
}

@Composable
private fun OnboardingBackButton(onClick: () -> Unit, modifier: Modifier = Modifier) {
    TextButton(
        onClick = onClick,
        modifier = modifier
            .height(54.dp)
            .testTag("onboarding_prev_btn"),
        shape = RoundedCornerShape(50)
    ) {
        Text(
            text = "Back",
            fontSize = 16.sp,
            fontWeight = FontWeight.SemiBold,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
    }
}

/** Selectable glass row: optional leading slot, caller body, check mark or hollow radio. */
@Composable
private fun OnboardingOptionRow(
    selected: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    leading: (@Composable RowScope.() -> Unit)? = null,
    content: @Composable ColumnScope.() -> Unit
) {
    val accent = MaterialTheme.colorScheme.primary
    val shape = RoundedCornerShape(16.dp)
    Row(
        modifier = modifier
            .fillMaxWidth()
            .then(if (selected) Modifier.accentGlow(accent, shape, 6.dp) else Modifier)
            .liquidGlass(shape = shape, strength = if (selected) 1f else 0.7f)
            .then(if (selected) Modifier.border(1.5.dp, accent, shape) else Modifier)
            .clickable(onClick = onClick)
            .padding(horizontal = 14.dp, vertical = 12.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        leading?.invoke(this)
        Column(modifier = Modifier.weight(1f), content = content)
        Spacer(modifier = Modifier.width(12.dp))
        if (selected) {
            Icon(
                imageVector = Icons.Default.CheckCircle,
                contentDescription = "Selected",
                tint = accent,
                modifier = Modifier.size(24.dp)
            )
        } else {
            Box(
                modifier = Modifier
                    .size(22.dp)
                    .border(1.5.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.5f), CircleShape)
            )
        }
    }
}

/** Pill glass search input; mirrors the Home transactions search but with a caller-set hint. */
@Composable
private fun OnboardingSearchField(
    value: String,
    onValueChange: (String) -> Unit,
    placeholder: String,
    modifier: Modifier = Modifier
) {
    val accent = MaterialTheme.colorScheme.primary
    Row(
        modifier = modifier
            .height(48.dp)
            .liquidGlass(shape = RoundedCornerShape(50), strength = 0.8f)
            .padding(horizontal = 16.dp)
            .testTag("onboarding_currency_search"),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Icon(
            imageVector = Icons.Default.Search,
            contentDescription = null,
            tint = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.size(20.dp)
        )
        Spacer(modifier = Modifier.width(10.dp))
        BasicTextField(
            value = value,
            onValueChange = onValueChange,
            singleLine = true,
            textStyle = MaterialTheme.typography.bodyMedium.copy(color = MaterialTheme.colorScheme.onSurface),
            cursorBrush = SolidColor(accent),
            modifier = Modifier.weight(1f),
            decorationBox = { inner ->
                Box(contentAlignment = Alignment.CenterStart) {
                    if (value.isEmpty()) {
                        Text(
                            text = placeholder,
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.outline
                        )
                    }
                    inner()
                }
            }
        )
        if (value.isNotEmpty()) {
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
