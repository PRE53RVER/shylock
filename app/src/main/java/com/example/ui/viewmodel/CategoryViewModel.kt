package com.example.ui.viewmodel

import android.app.Application
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.data.db.AppDatabase
import com.example.data.model.Category
import com.example.data.model.DetectedPayment
import com.example.data.model.LendingContact
import com.example.data.model.LendingEntry
import com.example.data.model.Subcategory
import com.example.data.model.Transaction
import com.example.data.repository.CategoryRepository
import com.example.data.repository.LendingRepository
import com.example.data.repository.MoneyInboxRepository
import com.example.inbox.MoneyInboxNotifications
import com.example.inbox.MoneyInboxSettings
import com.example.ui.theme.AppTheme
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.time.Instant
import java.time.YearMonth
import java.time.ZoneId
import java.util.Date
import java.util.Locale

enum class PaletteTheme {
    MODERN, PASTEL, NEON, PROFESSIONAL
}

data class CategoryStats(
    val categoryId: Int,
    val totalAmount: Double,
    val transactionCount: Int,
    val percentage: Double,
    val trendPercentage: Double, // trend vs last month
    val currentMonthTotal: Double,
    val previousMonthTotal: Double
)

class CategoryViewModel(application: Application) : AndroidViewModel(application) {
    private val database = AppDatabase.getDatabase(application)
    private val repository = CategoryRepository(
        database.categoryDao(),
        database.subcategoryDao(),
        database.transactionDao()
    )
    private val lendingRepository = LendingRepository(database.lendingDao())
    private val inboxRepository = MoneyInboxRepository(database.detectedPaymentDao())

    // Raw database state flows
    val categories: StateFlow<List<Category>> = repository.allCategories
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val subcategories: StateFlow<List<Subcategory>> = repository.allSubcategories
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val transactions: StateFlow<List<Transaction>> = repository.allTransactions
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val lendingContacts: StateFlow<List<LendingContact>> = lendingRepository.allContacts
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val lendingEntries: StateFlow<List<LendingEntry>> = lendingRepository.allEntries
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    // Money Inbox: drafts detected from notifications, waiting for the user's decision
    val detectedPayments: StateFlow<List<DetectedPayment>> = inboxRepository.pendingPayments
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val pendingInboxCount: StateFlow<Int> = inboxRepository.pendingCount
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), 0)

    // Selected calendar month for scoped viewing (null means dynamically follows YearMonth.now() live)
    private val _selectedMonth = MutableStateFlow<YearMonth?>(null)
    val selectedMonth: StateFlow<YearMonth> = _selectedMonth
        .map { it ?: YearMonth.now() }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), YearMonth.now())

    fun selectMonth(yearMonth: YearMonth) {
        if (yearMonth == YearMonth.now()) {
            _selectedMonth.value = null // live follow current month
        } else {
            _selectedMonth.value = yearMonth
        }
    }

    fun resetToCurrentMonth() {
        _selectedMonth.value = null
    }

    // Active palette theme with SharedPreferences persistence
    private val prefs = application.getSharedPreferences("shylock_settings", android.content.Context.MODE_PRIVATE)

    private val _currentPaletteTheme = MutableStateFlow(
        PaletteTheme.valueOf(prefs.getString("palette_theme", PaletteTheme.MODERN.name) ?: PaletteTheme.MODERN.name)
    )
    val currentPaletteTheme = _currentPaletteTheme.asStateFlow()

    // Persist whether user explicitly chose dark or light mode, or "SYSTEM"
    private val _themeMode = MutableStateFlow(
        prefs.getString("theme_mode", "SYSTEM") ?: "SYSTEM"
    )
    val themeMode = _themeMode.asStateFlow()

    // Dynamic color support toggle
    private val _dynamicColor = MutableStateFlow(
        prefs.getBoolean("dynamic_color", false)
    )
    val dynamicColor = _dynamicColor.asStateFlow()

    // Persistent App Theme (separate from Dark/Light mode and Category palettes)
    private val _appTheme = MutableStateFlow(
        try {
            val savedTheme = prefs.getString("app_theme", AppTheme.OCEAN.name) ?: AppTheme.OCEAN.name
            AppTheme.valueOf(savedTheme)
        } catch (e: Exception) {
            AppTheme.OCEAN
        }
    )
    val appTheme = _appTheme.asStateFlow()

    // Currency preferences
    private val _currency = MutableStateFlow(
        prefs.getString("currency", "₹") ?: "₹"
    )
    val currency = _currency.asStateFlow()

    // Default Transaction type preferences
    private val _defaultTransactionType = MutableStateFlow(
        prefs.getString("default_transaction_type", "EXPENSE") ?: "EXPENSE"
    )
    val defaultTransactionType = _defaultTransactionType.asStateFlow()

    // Highlighted category ID for analytics filtering/hover
    private val _selectedCategoryId = MutableStateFlow<Int?>(null)
    val selectedCategoryId = _selectedCategoryId.asStateFlow()

    // Onboarding preferences
    private val _onboardingComplete = MutableStateFlow(
        prefs.getBoolean("onboarding_complete", false)
    )
    val onboardingComplete = _onboardingComplete.asStateFlow()

    // Money Inbox preferences (also read directly by the listener service / reminder receiver)
    private val _paymentDetectionEnabled = MutableStateFlow(
        prefs.getBoolean(MoneyInboxSettings.KEY_DETECTION_ENABLED, false)
    )
    val paymentDetectionEnabled = _paymentDetectionEnabled.asStateFlow()

    private val _paymentAlertsEnabled = MutableStateFlow(
        prefs.getBoolean(MoneyInboxSettings.KEY_ALERTS_ENABLED, true)
    )
    val paymentAlertsEnabled = _paymentAlertsEnabled.asStateFlow()

    private val _dailyReviewReminderEnabled = MutableStateFlow(
        prefs.getBoolean(MoneyInboxSettings.KEY_DAILY_REMINDER_ENABLED, false)
    )
    val dailyReviewReminderEnabled = _dailyReviewReminderEnabled.asStateFlow()

    // Set when a notification tap asks the app to open on the Money Inbox tab
    private val _openInboxRequest = MutableStateFlow(false)
    val openInboxRequest = _openInboxRequest.asStateFlow()

    // Predefined color catalogs under themed palettes
    val palettes = mapOf(
        PaletteTheme.MODERN to listOf(
            "#FF6B6B", "#4D96FF", "#FFD93D", "#6BCB77", "#A66CFF",
            "#FF9F1C", "#00C2A8", "#F72585", "#4361EE", "#7209B7",
            "#2EC4B6", "#EF476F"
        ),
        PaletteTheme.PASTEL to listOf(
            "#FFADAD", "#FFD6A5", "#FDFFB6", "#CAFFBF", "#9BF6FF",
            "#A0C4FF", "#BDB2FF", "#FFC6FF", "#E8AEB7", "#B8E0D2",
            "#D6E2E9", "#F0E6EF"
        ),
        PaletteTheme.NEON to listOf(
            "#FF007F", "#39FF14", "#04D9FF", "#FE019A", "#BC13FE",
            "#FF073A", "#CCFF00", "#FF4900", "#00E5FF", "#B000FF",
            "#00FF66", "#FF00F0"
        ),
        PaletteTheme.PROFESSIONAL to listOf(
            "#1B365D", "#4B6B94", "#1F4E5B", "#3B7A57", "#8C6239",
            "#4A3B32", "#5C2C35", "#56385D", "#2F4F4F", "#3F51B5",
            "#607D8B", "#795548"
        )
    )

    init {
        viewModelScope.launch {
            repository.ensureSystemCategories()
            syncAllExistingLendingEntries()
        }
    }

    fun seedDemoData() {
        viewModelScope.launch {
            repository.seedDemoDataIfEmpty()
        }
    }

    // Reactive computed analytics combining categories, transactions, and selected month
    val categoryStats: StateFlow<Map<Int, CategoryStats>> = combine(
        categories,
        transactions,
        selectedMonth
    ) { categoryList, transactionList, targetMonth ->
        computeStats(categoryList, transactionList, targetMonth)
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyMap())

    private fun computeStats(
        categoryList: List<Category>,
        transactionList: List<Transaction>,
        targetMonth: YearMonth
    ): Map<Int, CategoryStats> {
        val targetPrevMonth = targetMonth.minusMonths(1)

        val currentMonthExpenses = transactionList.filter { 
            isTimestampInMonth(it.timestamp, targetMonth) && it.type == "EXPENSE" 
        }
        val currentMonthIncomes = transactionList.filter { 
            isTimestampInMonth(it.timestamp, targetMonth) && it.type == "INCOME" 
        }
        val totalCurrentSpend = currentMonthExpenses.sumOf { it.amount }
        val totalCurrentIncome = currentMonthIncomes.sumOf { it.amount }

        return categoryList.associate { category ->
            val isIncome = category.type == "INCOME"
            val typeTxList = if (isIncome) currentMonthIncomes else currentMonthExpenses
            val prevTypeTxList = transactionList.filter { 
                isTimestampInMonth(it.timestamp, targetPrevMonth) && it.type == (if (isIncome) "INCOME" else "EXPENSE")
            }
            val totalForType = if (isIncome) totalCurrentIncome else totalCurrentSpend

            val catCurrentTx = typeTxList.filter { it.categoryId == category.id }
            val catPreviousTx = prevTypeTxList.filter { it.categoryId == category.id }

            val currentSum = catCurrentTx.sumOf { it.amount }
            val previousSum = catPreviousTx.sumOf { it.amount }

            val percentage = if (totalForType > 0.0) {
                (currentSum / totalForType) * 100.0
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

    // Helper functions for calendar dates using modern java.time
    fun isTimestampInMonth(timestamp: Long, ym: YearMonth): Boolean {
        val date = Instant.ofEpochMilli(timestamp).atZone(ZoneId.systemDefault()).toLocalDate()
        return YearMonth.from(date) == ym
    }

    fun getMonthString(timestamp: Long): String {
        val ym = YearMonth.from(Instant.ofEpochMilli(timestamp).atZone(ZoneId.systemDefault()))
        return ym.toString()
    }

    fun getPreviousMonthString(timestamp: Long): String {
        val ym = YearMonth.from(Instant.ofEpochMilli(timestamp).atZone(ZoneId.systemDefault()))
        return ym.minusMonths(1).toString()
    }

    // Theme setter representing persistence
    fun setPaletteTheme(theme: PaletteTheme) {
        _currentPaletteTheme.value = theme
        prefs.edit().putString("palette_theme", theme.name).apply()
    }

    fun applyPaletteToExistingCategories(theme: PaletteTheme) {
        viewModelScope.launch {
            val themeColors = palettes[theme] ?: return@launch
            if (themeColors.isEmpty()) return@launch
            val currentCats = categories.value
            currentCats.forEachIndexed { index, category ->
                val newColor = themeColors[index % themeColors.size]
                repository.updateCategory(
                    category.copy(colorHex = newColor)
                )
            }
        }
    }

    fun setThemeMode(mode: String) {
        _themeMode.value = mode
        prefs.edit().putString("theme_mode", mode).apply()
    }

    fun setAppTheme(theme: AppTheme) {
        _appTheme.value = theme
        prefs.edit().putString("app_theme", theme.name).apply()
    }

    fun setDynamicColor(enabled: Boolean) {
        _dynamicColor.value = enabled
        prefs.edit().putBoolean("dynamic_color", enabled).apply()
    }

    fun setCurrency(symbol: String) {
        _currency.value = symbol
        prefs.edit().putString("currency", symbol).apply()
    }

    fun setDefaultTransactionType(type: String) {
        _defaultTransactionType.value = type
        prefs.edit().putString("default_transaction_type", type).apply()
    }

    fun setOnboardingComplete(complete: Boolean) {
        _onboardingComplete.value = complete
        prefs.edit().putBoolean("onboarding_complete", complete).apply()
    }

    // ---- Money Inbox ----

    fun setPaymentDetectionEnabled(enabled: Boolean) {
        _paymentDetectionEnabled.value = enabled
        prefs.edit().putBoolean(MoneyInboxSettings.KEY_DETECTION_ENABLED, enabled).apply()
    }

    fun setPaymentAlertsEnabled(enabled: Boolean) {
        _paymentAlertsEnabled.value = enabled
        prefs.edit().putBoolean(MoneyInboxSettings.KEY_ALERTS_ENABLED, enabled).apply()
    }

    fun setDailyReviewReminderEnabled(enabled: Boolean) {
        _dailyReviewReminderEnabled.value = enabled
        prefs.edit().putBoolean(MoneyInboxSettings.KEY_DAILY_REMINDER_ENABLED, enabled).apply()
        MoneyInboxNotifications.syncDailyReminder(getApplication())
    }

    fun requestOpenInbox() {
        _openInboxRequest.value = true
    }

    fun consumeOpenInboxRequest() {
        _openInboxRequest.value = false
    }

    /** Builds the prefilled draft the add-record dialog opens with for a detected payment. */
    fun draftTransactionFor(payment: DetectedPayment): Transaction {
        val type = payment.transactionType
        val description = payment.counterparty ?: payment.source
        val category = categories.value.firstOrNull { it.type.equals(type, ignoreCase = true) }
        return Transaction(
            id = 0,
            categoryId = category?.id ?: 0,
            subcategoryId = null,
            amount = payment.amount,
            description = description,
            timestamp = payment.timestamp,
            type = type
        )
    }

    /** Records a draft through the normal add-transaction path and clears it from the inbox. */
    fun recordDetectedPayment(paymentId: Int, categoryId: Int, subcategoryId: Int?, amount: Double, description: String, timestamp: Long, type: String) {
        viewModelScope.launch {
            repository.insertTransaction(
                Transaction(
                    categoryId = categoryId,
                    subcategoryId = subcategoryId,
                    amount = amount,
                    description = description,
                    timestamp = timestamp,
                    type = type
                )
            )
            inboxRepository.markRecorded(paymentId)
        }
    }

    fun dismissDetectedPayment(payment: DetectedPayment) {
        viewModelScope.launch { inboxRepository.markDismissed(payment.id) }
    }

    fun restoreDetectedPayment(payment: DetectedPayment) {
        viewModelScope.launch { inboxRepository.restorePending(payment.id) }
    }

    // Export Data to JSON string
    fun exportBackupJson(): String {
        return try {
            val cats = categories.value
            val subs = subcategories.value
            val txs = transactions.value
            val lContacts = lendingContacts.value
            val lEntries = lendingEntries.value
            
            val json = org.json.JSONObject()
            
            val catsArray = org.json.JSONArray()
            for (c in cats) {
                val jo = org.json.JSONObject()
                jo.put("id", c.id)
                jo.put("name", c.name)
                jo.put("colorHex", c.colorHex)
                jo.put("iconName", c.iconName)
                jo.put("budgetLimit", c.budgetLimit)
                jo.put("type", c.type)
                catsArray.put(jo)
            }
            json.put("categories", catsArray)

            val subsArray = org.json.JSONArray()
            for (s in subs) {
                val jo = org.json.JSONObject()
                jo.put("id", s.id)
                jo.put("parentCategoryId", s.parentCategoryId)
                jo.put("name", s.name)
                jo.put("colorHexOverride", s.colorHexOverride ?: org.json.JSONObject.NULL)
                jo.put("iconName", s.iconName ?: org.json.JSONObject.NULL)
                jo.put("sortOrder", s.sortOrder)
                subsArray.put(jo)
            }
            json.put("subcategories", subsArray)

            val txsArray = org.json.JSONArray()
            for (t in txs) {
                val jo = org.json.JSONObject()
                jo.put("id", t.id)
                jo.put("categoryId", t.categoryId)
                jo.put("subcategoryId", t.subcategoryId ?: org.json.JSONObject.NULL)
                jo.put("amount", t.amount)
                jo.put("description", t.description)
                jo.put("timestamp", t.timestamp)
                jo.put("type", t.type)
                jo.put("lendingEntryId", t.lendingEntryId ?: org.json.JSONObject.NULL)
                txsArray.put(jo)
            }
            json.put("transactions", txsArray)

            val contactsArray = org.json.JSONArray()
            for (c in lContacts) {
                val jo = org.json.JSONObject()
                jo.put("id", c.id)
                jo.put("name", c.name)
                jo.put("note", c.note ?: org.json.JSONObject.NULL)
                jo.put("colorHex", c.colorHex ?: org.json.JSONObject.NULL)
                jo.put("iconName", c.iconName ?: org.json.JSONObject.NULL)
                contactsArray.put(jo)
            }
            json.put("lending_contacts", contactsArray)

            val entriesArray = org.json.JSONArray()
            for (e in lEntries) {
                val jo = org.json.JSONObject()
                jo.put("id", e.id)
                jo.put("contactId", e.contactId)
                jo.put("amount", e.amount)
                jo.put("direction", e.direction)
                jo.put("description", e.description)
                jo.put("timestamp", e.timestamp)
                entriesArray.put(jo)
            }
            json.put("lending_entries", entriesArray)

            json.toString(2)
        } catch (e: Exception) {
            ""
        }
    }

    // Import Data from JSON string
    fun importBackupJson(jsonStr: String): Boolean {
        return try {
            val json = org.json.JSONObject(jsonStr)
            val catsArray = json.optJSONArray("categories") ?: org.json.JSONArray()
            val subsArray = json.optJSONArray("subcategories") ?: org.json.JSONArray()
            val txsArray = json.optJSONArray("transactions") ?: org.json.JSONArray()
            val lContactsArray = json.optJSONArray("lending_contacts") ?: org.json.JSONArray()
            val lEntriesArray = json.optJSONArray("lending_entries") ?: org.json.JSONArray()
            
            viewModelScope.launch {
                val cats = categories.value
                val subs = subcategories.value
                val txs = transactions.value
                val lContacts = lendingContacts.value
                
                for (t in txs) repository.deleteTransaction(t)
                for (s in subs) repository.deleteSubcategory(s)
                for (c in cats) repository.deleteCategory(c)
                for (lc in lContacts) lendingRepository.deleteContact(lc)
                
                val catIdMapping = mutableMapOf<Int, Int>()
                for (i in 0 until catsArray.length()) {
                    val jo = catsArray.getJSONObject(i)
                    val oldId = jo.getInt("id")
                    val name = jo.getString("name")
                    val colorHex = jo.getString("colorHex")
                    val iconName = jo.getString("iconName")
                    val budgetLimit = jo.getDouble("budgetLimit")
                    val type = jo.optString("type", "EXPENSE")
                    
                    val newId = repository.insertCategory(
                        Category(name = name, colorHex = colorHex, iconName = iconName, budgetLimit = budgetLimit, type = type)
                    ).toInt()
                    catIdMapping[oldId] = newId
                }

                val subIdMapping = mutableMapOf<Int, Int>()
                for (i in 0 until subsArray.length()) {
                    val jo = subsArray.getJSONObject(i)
                    val oldSubId = jo.getInt("id")
                    val oldParentId = jo.getInt("parentCategoryId")
                    val name = jo.getString("name")
                    val colorHexOverride = if (jo.isNull("colorHexOverride")) null else jo.getString("colorHexOverride")
                    val subIconName = if (jo.isNull("iconName")) null else jo.optString("iconName", null)
                    val sortOrder = jo.optInt("sortOrder", i)
                    
                    val newParentId = catIdMapping[oldParentId]
                    if (newParentId != null) {
                        val newSubId = repository.insertSubcategory(
                            Subcategory(parentCategoryId = newParentId, name = name, colorHexOverride = colorHexOverride, iconName = subIconName, sortOrder = sortOrder)
                        ).toInt()
                        subIdMapping[oldSubId] = newSubId
                    }
                }

                // Restore Lending Contacts & Entries first
                val contactIdMapping = mutableMapOf<Int, Int>()
                for (i in 0 until lContactsArray.length()) {
                    val jo = lContactsArray.getJSONObject(i)
                    val oldId = jo.getInt("id")
                    val name = jo.getString("name")
                    val note = if (jo.isNull("note")) null else jo.getString("note")
                    val colorHex = if (jo.isNull("colorHex")) null else jo.getString("colorHex")
                    val iconName = if (jo.isNull("iconName")) null else jo.getString("iconName")
                    
                    val newId = lendingRepository.insertContact(
                        LendingContact(name = name, note = note, colorHex = colorHex, iconName = iconName)
                    ).toInt()
                    contactIdMapping[oldId] = newId
                }

                val entryIdMapping = mutableMapOf<Int, Int>()
                for (i in 0 until lEntriesArray.length()) {
                    val jo = lEntriesArray.getJSONObject(i)
                    val oldEntryId = jo.optInt("id", 0)
                    val oldContactId = jo.getInt("contactId")
                    val amount = jo.getDouble("amount")
                    val direction = jo.getString("direction")
                    val description = jo.getString("description")
                    val timestamp = jo.getLong("timestamp")

                    val newContactId = contactIdMapping[oldContactId]
                    if (newContactId != null) {
                        val newEntryId = lendingRepository.insertEntry(
                            LendingEntry(
                                contactId = newContactId,
                                amount = amount,
                                direction = direction,
                                description = description,
                                timestamp = timestamp
                            )
                        ).toInt()
                        if (oldEntryId != 0) {
                            entryIdMapping[oldEntryId] = newEntryId
                        }
                    }
                }

                for (i in 0 until txsArray.length()) {
                    val jo = txsArray.getJSONObject(i)
                    val oldCatId = jo.getInt("categoryId")
                    val oldSubId = if (jo.isNull("subcategoryId")) null else jo.getInt("subcategoryId")
                    val amount = jo.getDouble("amount")
                    val description = jo.getString("description")
                    val timestamp = jo.getLong("timestamp")
                    val type = jo.optString("type", "EXPENSE")
                    val oldLendingEntryId = if (jo.has("lendingEntryId") && !jo.isNull("lendingEntryId")) jo.getInt("lendingEntryId") else null
                    val newLendingEntryId = if (oldLendingEntryId != null) entryIdMapping[oldLendingEntryId] else null
                    
                    val newCatId = catIdMapping[oldCatId]
                    val newSubId = if (oldSubId != null) subIdMapping[oldSubId] else null
                    
                    if (newCatId != null) {
                        repository.insertTransaction(
                            Transaction(
                                categoryId = newCatId,
                                subcategoryId = newSubId,
                                amount = amount,
                                description = description,
                                timestamp = timestamp,
                                type = type,
                                lendingEntryId = newLendingEntryId
                            )
                        )
                    }
                }

                repository.ensureSystemCategories()
                syncAllExistingLendingEntries()
            }
            true
        } catch (e: Exception) {
            false
        }
    }

    // Select/Highlight visual categories
    fun toggleCategorySelection(categoryId: Int) {
        if (_selectedCategoryId.value == categoryId) {
            _selectedCategoryId.value = null
        } else {
            _selectedCategoryId.value = categoryId
        }
    }

    fun clearCategorySelection() {
        _selectedCategoryId.value = null
    }

    // Color operations & Checks
    // Hex duplicate check
    suspend fun isColorAlreadyAssigned(hexColor: String, excludeId: Int? = null): Boolean {
        val existing = repository.getCategoryByColor(normalizeHex(hexColor))
        return if (excludeId != null) {
            existing != null && existing.id != excludeId
        } else {
            existing != null
        }
    }

    // Similarity check (warning only)
    fun isColorTooSimilar(hexColor: String, excludeId: Int? = null, threshold: Double = 60.0): Pair<Boolean, String?> {
        val normalized = normalizeHex(hexColor)
        val activeCats = categories.value
        for (cat in activeCats) {
            if (excludeId != null && cat.id == excludeId) continue
            if (computeRgbDistance(normalized, cat.colorHex) < threshold) {
                return true to cat.name
            }
        }
        return false to null
    }

    private fun computeRgbDistance(hex1: String, hex2: String): Double {
        return try {
            val c1 = android.graphics.Color.parseColor(hex1)
            val c2 = android.graphics.Color.parseColor(hex2)
            val r1 = android.graphics.Color.red(c1)
            val g1 = android.graphics.Color.green(c1)
            val b1 = android.graphics.Color.blue(c1)
            val r2 = android.graphics.Color.red(c2)
            val g2 = android.graphics.Color.green(c2)
            val b2 = android.graphics.Color.blue(c2)
            Math.sqrt(
                Math.pow((r1 - r2).toDouble(), 2.0) +
                Math.pow((g1 - g2).toDouble(), 2.0) +
                Math.pow((b1 - b2).toDouble(), 2.0)
            )
        } catch (e: Exception) {
            999.0
        }
    }

    private fun normalizeHex(hex: String): String {
        val upper = hex.uppercase()
        return if (!upper.startsWith("#")) "#$upper" else upper
    }

    // Suggest unused colors in current theme
    fun getSmartColorSuggestions(): List<String> {
        val currentThemeColors = palettes[_currentPaletteTheme.value] ?: emptyList()
        val activeCategoryColors = categories.value.map { it.colorHex.uppercase() }
        
        // Filter out identical colors first
        val unusedInTheme = currentThemeColors.filter { it.uppercase() !in activeCategoryColors }

        // If theme colors are exhausted, suggest from catalog with maximum RGB distance
        if (unusedInTheme.isNotEmpty()) {
            return unusedInTheme
        }

        // Return current theme colors that are most distant or all theme colors as suggestion
        return currentThemeColors
    }

    // Lighten color helper for subcategories
    fun getLighterShades(parentHex: String): List<String> {
        val shades = mutableListOf<String>()
        // Let's generate factors: 0 (original), 15%, 30%, 45%, 60% lighter
        val factors = listOf(0.0f, 0.15f, 0.30f, 0.45f, 0.60f)
        try {
            val colorInt = android.graphics.Color.parseColor(parentHex)
            val r = android.graphics.Color.red(colorInt)
            val g = android.graphics.Color.green(colorInt)
            val b = android.graphics.Color.blue(colorInt)

            for (factor in factors) {
                val newR = (r + (255 - r) * factor).toInt().coerceIn(0, 255)
                val newG = (g + (255 - g) * factor).toInt().coerceIn(0, 255)
                val newB = (b + (255 - b) * factor).toInt().coerceIn(0, 255)
                shades.add(String.format("#%02X%02X%02X", newR, newG, newB))
            }
        } catch (e: Exception) {
            shades.add(parentHex)
        }
        return shades
    }

    // CRUD Category APIs
    // Subcategories arrive with parentCategoryId unset; a null colorHexOverride gets the next lighter
    // shade of the parent so seeded lists (onboarding) still fan out into a gradient.
    fun addCategory(name: String, colorHex: String, iconName: String, budget: Double, subcategoriesList: List<Subcategory>, type: String = "EXPENSE") {
        viewModelScope.launch {
            val id = repository.insertCategory(
                Category(
                    name = name,
                    colorHex = normalizeHex(colorHex),
                    iconName = iconName,
                    budgetLimit = budget,
                    type = type
                )
            ).toInt()

            val shades = getLighterShades(colorHex)
            subcategoriesList.forEachIndexed { index, sub ->
                repository.insertSubcategory(
                    sub.copy(
                        id = 0,
                        parentCategoryId = id,
                        colorHexOverride = sub.colorHexOverride ?: shades[index.coerceAtMost(shades.lastIndex)],
                        sortOrder = index
                    )
                )
            }
        }
    }

    // Subcategories are reconciled by id rather than rebuilt, so transactions keep pointing at the
    // same rows after a rename, icon change or reorder.
    fun editCategory(id: Int, name: String, colorHex: String, iconName: String, budget: Double, subcategoriesList: List<Subcategory>, type: String = "EXPENSE") {
        viewModelScope.launch {
            repository.updateCategory(
                Category(
                    id = id,
                    name = name,
                    colorHex = normalizeHex(colorHex),
                    iconName = iconName,
                    budgetLimit = budget,
                    type = type
                )
            )

            val existing = repository.getSubcategoriesForCategory(id).first()
            val keptIds = subcategoriesList.map { it.id }.filter { it != 0 }.toSet()
            existing.filter { it.id !in keptIds }.forEach { repository.deleteSubcategory(it) }

            subcategoriesList.forEachIndexed { index, sub ->
                val row = sub.copy(parentCategoryId = id, sortOrder = index)
                if (row.id != 0 && existing.any { it.id == row.id }) {
                    repository.updateSubcategory(row)
                } else {
                    repository.insertSubcategory(row.copy(id = 0))
                }
            }
        }
    }

    fun deleteCategory(category: Category) {
        if (category.name == "Lending" || category.name == "Loan Repayment") {
            return
        }
        viewModelScope.launch {
            repository.deleteCategory(category)
        }
    }

    // Add and Edit transactions with Timestamp and Type support
    fun addTransaction(categoryId: Int, subcategoryId: Int?, amount: Double, description: String, timestamp: Long, type: String = "EXPENSE") {
        viewModelScope.launch {
            repository.insertTransaction(
                Transaction(
                    categoryId = categoryId,
                    subcategoryId = subcategoryId,
                    amount = amount,
                    description = description,
                    timestamp = timestamp,
                    type = type
                )
            )
        }
    }

    fun editTransaction(id: Int, categoryId: Int, subcategoryId: Int?, amount: Double, description: String, timestamp: Long, type: String = "EXPENSE") {
        viewModelScope.launch {
            val currentTx = transactions.value.find { it.id == id }
            if (currentTx?.lendingEntryId != null) {
                return@launch
            }
            repository.updateTransaction(
                Transaction(
                    id = id,
                    categoryId = categoryId,
                    subcategoryId = subcategoryId,
                    amount = amount,
                    description = description,
                    timestamp = timestamp,
                    type = type
                )
            )
        }
    }

    fun deleteTransaction(transaction: Transaction) {
        if (transaction.lendingEntryId != null) {
            return
        }
        viewModelScope.launch {
            repository.deleteTransaction(transaction)
        }
    }

    fun reinsertTransaction(transaction: Transaction) {
        viewModelScope.launch {
            repository.insertTransaction(transaction.copy(id = 0))
        }
    }

    fun clearAllData() {
        viewModelScope.launch {
            val cats = categories.value
            val subs = subcategories.value
            val txs = transactions.value
            for (t in txs) {
                repository.deleteTransaction(t)
            }
            for (s in subs) {
                repository.deleteSubcategory(s)
            }
            for (c in cats) {
                repository.deleteCategory(c)
            }
            val lContacts = lendingContacts.value
            for (lc in lContacts) {
                lendingRepository.deleteContact(lc)
            }
            inboxRepository.clearAll()
            repository.ensureSystemCategories()
            clearCategorySelection()
        }
    }

    // Lending Ledger Operations
    fun getEntriesForContact(contactId: Int): Flow<List<LendingEntry>> {
        return lendingRepository.getEntriesForContact(contactId)
    }

    fun addLendingContact(
        name: String,
        note: String? = null,
        colorHex: String? = null,
        iconName: String? = null,
        initialLentAmount: Double? = null,
        initialDescription: String? = null
    ) {
        viewModelScope.launch {
            val newContactId = lendingRepository.insertContact(
                LendingContact(
                    name = name.trim(),
                    note = note?.trim()?.ifBlank { null },
                    colorHex = colorHex,
                    iconName = iconName
                )
            ).toInt()
            if (initialLentAmount != null && initialLentAmount > 0) {
                val desc = initialDescription?.trim()?.ifBlank { "Lent to ${name.trim()}" } ?: "Lent to ${name.trim()}"
                val now = System.currentTimeMillis()
                val entryId = lendingRepository.insertEntry(
                    LendingEntry(
                        contactId = newContactId,
                        amount = initialLentAmount,
                        direction = "LENT",
                        description = desc,
                        timestamp = now
                    )
                ).toInt()
                syncTransactionForLendingEntry(entryId, newContactId, initialLentAmount, "LENT", desc, now)
            }
        }
    }

    fun updateLendingContact(contact: LendingContact) {
        viewModelScope.launch {
            lendingRepository.updateContact(contact)
        }
    }

    fun deleteLendingContact(contact: LendingContact) {
        viewModelScope.launch {
            val entries = lendingRepository.getEntriesListForContact(contact.id)
            if (entries.isNotEmpty()) {
                repository.deleteTransactionsByLendingEntryIds(entries.map { it.id })
            }
            lendingRepository.deleteContact(contact)
        }
    }

    fun addLendingEntry(
        contactId: Int,
        amount: Double,
        direction: String,
        description: String,
        timestamp: Long = System.currentTimeMillis()
    ) {
        viewModelScope.launch {
            val trimmedDesc = description.trim()
            val entryId = lendingRepository.insertEntry(
                LendingEntry(
                    contactId = contactId,
                    amount = amount,
                    direction = direction,
                    description = trimmedDesc,
                    timestamp = timestamp
                )
            ).toInt()
            syncTransactionForLendingEntry(entryId, contactId, amount, direction, trimmedDesc, timestamp)
        }
    }

    fun updateLendingEntry(entry: LendingEntry) {
        viewModelScope.launch {
            lendingRepository.updateEntry(entry)
            syncTransactionForLendingEntry(
                entryId = entry.id,
                contactId = entry.contactId,
                amount = entry.amount,
                direction = entry.direction,
                description = entry.description,
                timestamp = entry.timestamp
            )
        }
    }

    fun deleteLendingEntry(entry: LendingEntry) {
        viewModelScope.launch {
            repository.deleteTransactionByLendingEntryId(entry.id)
            lendingRepository.deleteEntry(entry)
        }
    }

    fun reinsertLendingEntry(entry: LendingEntry) {
        viewModelScope.launch {
            addLendingEntry(
                contactId = entry.contactId,
                amount = entry.amount,
                direction = entry.direction,
                description = entry.description,
                timestamp = entry.timestamp
            )
        }
    }

    private suspend fun syncTransactionForLendingEntry(
        entryId: Int,
        contactId: Int,
        amount: Double,
        direction: String,
        description: String,
        timestamp: Long
    ) {
        val contact = lendingRepository.getContactById(contactId)
        val contactName = contact?.name?.trim() ?: "Contact"
        val isLent = direction == "LENT"
        val txType = if (isLent) "EXPENSE" else "INCOME"

        val categoriesPair = repository.ensureSystemCategories()
        val category = if (isLent) categoriesPair.first else categoriesPair.second

        val defaultDesc = if (isLent) "Lent to $contactName" else "Repayment from $contactName"
        val finalDesc = if (description.isNotBlank()) description.trim() else defaultDesc

        val existingTx = repository.getTransactionByLendingEntryId(entryId)
        if (existingTx != null) {
            repository.updateTransaction(
                existingTx.copy(
                    categoryId = category.id,
                    subcategoryId = null,
                    amount = amount,
                    description = finalDesc,
                    timestamp = timestamp,
                    type = txType
                )
            )
        } else {
            repository.insertTransaction(
                Transaction(
                    categoryId = category.id,
                    subcategoryId = null,
                    amount = amount,
                    description = finalDesc,
                    timestamp = timestamp,
                    type = txType,
                    lendingEntryId = entryId
                )
            )
        }
    }

    private suspend fun syncAllExistingLendingEntries() {
        val entries = lendingRepository.getAllEntriesList()
        for (entry in entries) {
            val existingTx = repository.getTransactionByLendingEntryId(entry.id)
            if (existingTx == null) {
                syncTransactionForLendingEntry(
                    entryId = entry.id,
                    contactId = entry.contactId,
                    amount = entry.amount,
                    direction = entry.direction,
                    description = entry.description,
                    timestamp = entry.timestamp
                )
            }
        }
    }
}
