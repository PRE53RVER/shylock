package com.example.data.repository

import com.example.data.db.CategoryDao
import com.example.data.db.SubcategoryDao
import com.example.data.db.TransactionDao
import com.example.data.model.Category
import com.example.data.model.Subcategory
import com.example.data.model.Transaction
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first

class CategoryRepository(
    private val categoryDao: CategoryDao,
    private val subcategoryDao: SubcategoryDao,
    private val transactionDao: TransactionDao
) {
    val allCategories: Flow<List<Category>> = categoryDao.getAllCategories()
    val allSubcategories: Flow<List<Subcategory>> = subcategoryDao.getAllSubcategories()
    val allTransactions: Flow<List<Transaction>> = transactionDao.getAllTransactions()

    fun getSubcategoriesForCategory(parentId: Int): Flow<List<Subcategory>> {
        return subcategoryDao.getSubcategoriesForCategory(parentId)
    }

    suspend fun getCategoryById(id: Int): Category? {
        return categoryDao.getCategoryById(id)
    }

    suspend fun getCategoryByColor(colorHex: String): Category? {
        return categoryDao.getCategoryByColor(colorHex)
    }

    suspend fun getCategoryByName(name: String): Category? {
        return categoryDao.getCategoryByName(name)
    }

    suspend fun getTransactionByLendingEntryId(lendingEntryId: Int): Transaction? {
        return transactionDao.getTransactionByLendingEntryId(lendingEntryId)
    }

    suspend fun deleteTransactionByLendingEntryId(lendingEntryId: Int) {
        transactionDao.deleteByLendingEntryId(lendingEntryId)
    }

    suspend fun deleteTransactionsByLendingEntryIds(lendingEntryIds: List<Int>) {
        if (lendingEntryIds.isNotEmpty()) {
            transactionDao.deleteByLendingEntryIds(lendingEntryIds)
        }
    }

    suspend fun ensureSystemCategories(): Pair<Category, Category> {
        val categories = categoryDao.getAllCategories().first()
        var lending = categories.find { it.name == "Lending" }
        if (lending == null) {
            val id = categoryDao.insertCategory(
                Category(
                    name = "Lending",
                    colorHex = "#00B4D8",
                    iconName = "payments",
                    budgetLimit = 0.0,
                    type = "EXPENSE"
                )
            ).toInt()
            lending = categoryDao.getCategoryById(id)!!
        } else if (lending.type != "EXPENSE") {
            lending = lending.copy(type = "EXPENSE")
            categoryDao.updateCategory(lending)
        }

        var repayment = categories.find { it.name == "Loan Repayment" }
        if (repayment == null) {
            val id = categoryDao.insertCategory(
                Category(
                    name = "Loan Repayment",
                    colorHex = "#00C9A7",
                    iconName = "trending_up",
                    budgetLimit = 0.0,
                    type = "INCOME"
                )
            ).toInt()
            repayment = categoryDao.getCategoryById(id)!!
        } else if (repayment.type != "INCOME") {
            repayment = repayment.copy(type = "INCOME")
            categoryDao.updateCategory(repayment)
        }

        return Pair(lending, repayment)
    }

    suspend fun insertCategory(category: Category): Long {
        return categoryDao.insertCategory(category)
    }

    suspend fun updateCategory(category: Category) {
        categoryDao.updateCategory(category)
    }

    suspend fun deleteCategory(category: Category) {
        // Delete all child transactions and subcategories first
        subcategoryDao.deleteByParentCategory(category.id)
        transactionDao.deleteByCategoryId(category.id)
        categoryDao.deleteCategory(category)
    }

    suspend fun insertSubcategory(subcategory: Subcategory): Long {
        return subcategoryDao.insertSubcategory(subcategory)
    }

    suspend fun updateSubcategory(subcategory: Subcategory) {
        subcategoryDao.updateSubcategory(subcategory)
    }

    suspend fun deleteSubcategory(subcategory: Subcategory) {
        subcategoryDao.deleteSubcategory(subcategory)
    }

    suspend fun insertTransaction(transaction: Transaction): Long {
        return transactionDao.insertTransaction(transaction)
    }

    suspend fun updateTransaction(transaction: Transaction) {
        transactionDao.updateTransaction(transaction)
    }

    suspend fun deleteTransaction(transaction: Transaction) {
        transactionDao.deleteTransaction(transaction)
    }

    // Seed initial demo data if database is empty or missing income categories
    suspend fun seedDemoDataIfEmpty() {
        val categories = categoryDao.getAllCategories().first()
        val now = System.currentTimeMillis()
        val dayMs = 24 * 60 * 60 * 1000L
        val monthMs = 30 * dayMs

        if (categories.isEmpty()) {
            // Seed Expense Categories
            val catFoodId = categoryDao.insertCategory(Category(name = "Food", colorHex = "#FF6B6B", iconName = "restaurant", budgetLimit = 15000.0, type = "EXPENSE")).toInt()
            val catTransportId = categoryDao.insertCategory(Category(name = "Transport", colorHex = "#4D96FF", iconName = "directions_car", budgetLimit = 5000.0, type = "EXPENSE")).toInt()
            val catShoppingId = categoryDao.insertCategory(Category(name = "Shopping", colorHex = "#FFD93D", iconName = "shopping_bag", budgetLimit = 8000.0, type = "EXPENSE")).toInt()
            val catHealthId = categoryDao.insertCategory(Category(name = "Health", colorHex = "#6BCB77", iconName = "medical_services", budgetLimit = 4000.0, type = "EXPENSE")).toInt()
            val catEntId = categoryDao.insertCategory(Category(name = "Entertainment", colorHex = "#A66CFF", iconName = "movie", budgetLimit = 6000.0, type = "EXPENSE")).toInt()
            val catBillsId = categoryDao.insertCategory(Category(name = "Bills", colorHex = "#FF9F1C", iconName = "receipt_long", budgetLimit = 10000.0, type = "EXPENSE")).toInt()
            val catTravelId = categoryDao.insertCategory(Category(name = "Travel", colorHex = "#00C2A8", iconName = "flight", budgetLimit = 20000.0, type = "EXPENSE")).toInt()
            val catEduId = categoryDao.insertCategory(Category(name = "Education", colorHex = "#3A86FF", iconName = "school", budgetLimit = 5000.0, type = "EXPENSE")).toInt()

            // Seed Subcategories for Food
            val subBreakfast = subcategoryDao.insertSubcategory(Subcategory(parentCategoryId = catFoodId, name = "Breakfast", colorHexOverride = "#FF8585", iconName = "bakery_dining", sortOrder = 0)).toInt()
            val subLunch = subcategoryDao.insertSubcategory(Subcategory(parentCategoryId = catFoodId, name = "Lunch", colorHexOverride = "#FF9999", iconName = "lunch_dining", sortOrder = 1)).toInt()
            val subDinner = subcategoryDao.insertSubcategory(Subcategory(parentCategoryId = catFoodId, name = "Dinner", colorHexOverride = "#FFB3B3", iconName = "dinner_dining", sortOrder = 2)).toInt()
            val subSnacks = subcategoryDao.insertSubcategory(Subcategory(parentCategoryId = catFoodId, name = "Tea & Snacks", colorHexOverride = "#FFC9C9", iconName = "local_cafe", sortOrder = 3)).toInt()

            // Seed Subcategories for Transport
            val subFuel = subcategoryDao.insertSubcategory(Subcategory(parentCategoryId = catTransportId, name = "Fuel", colorHexOverride = "#70A9FF", iconName = "local_gas_station", sortOrder = 0)).toInt()
            val subMetro = subcategoryDao.insertSubcategory(Subcategory(parentCategoryId = catTransportId, name = "Metro & Train", colorHexOverride = "#94BDFF", iconName = "subway", sortOrder = 1)).toInt()

            // Seed Income Categories
            val catSalaryId = categoryDao.insertCategory(Category(name = "Salary", colorHex = "#2E7D32", iconName = "payments", budgetLimit = 0.0, type = "INCOME")).toInt()
            val catBusinessId = categoryDao.insertCategory(Category(name = "Business", colorHex = "#00897B", iconName = "business", budgetLimit = 0.0, type = "INCOME")).toInt()
            val catGiftsId = categoryDao.insertCategory(Category(name = "Gifts", colorHex = "#D81B60", iconName = "card_giftcard", budgetLimit = 0.0, type = "INCOME")).toInt()
            val catInterestId = categoryDao.insertCategory(Category(name = "Interest", colorHex = "#1E88E5", iconName = "trending_up", budgetLimit = 0.0, type = "INCOME")).toInt()

            // Subcategories for Income
            val subSalaryMonthly = subcategoryDao.insertSubcategory(Subcategory(parentCategoryId = catSalaryId, name = "Monthly Pay", colorHexOverride = "#4CAF50", iconName = "payments", sortOrder = 0)).toInt()
            subcategoryDao.insertSubcategory(Subcategory(parentCategoryId = catSalaryId, name = "Bonus", colorHexOverride = "#81C784", iconName = "loyalty", sortOrder = 1))
            val subBizInvoice = subcategoryDao.insertSubcategory(Subcategory(parentCategoryId = catBusinessId, name = "Client Invoice", colorHexOverride = "#26A69A", iconName = "request_quote", sortOrder = 0)).toInt()
            subcategoryDao.insertSubcategory(Subcategory(parentCategoryId = catBusinessId, name = "Consulting", colorHexOverride = "#80CBC4", iconName = "handshake", sortOrder = 1))
            subcategoryDao.insertSubcategory(Subcategory(parentCategoryId = catGiftsId, name = "Presents", colorHexOverride = "#F06292", iconName = "card_giftcard"))
            subcategoryDao.insertSubcategory(Subcategory(parentCategoryId = catInterestId, name = "Dividends", colorHexOverride = "#42A5F5", iconName = "show_chart"))

            // Curr month spending (Expense)
            transactionDao.insertTransaction(Transaction(categoryId = catFoodId, subcategoryId = subBreakfast, amount = 350.0, description = "Breakfast pancakes", timestamp = now - 1 * dayMs, type = "EXPENSE"))
            transactionDao.insertTransaction(Transaction(categoryId = catFoodId, subcategoryId = subLunch, amount = 650.0, description = "Italian lunch buffet", timestamp = now - 2 * dayMs, type = "EXPENSE"))
            transactionDao.insertTransaction(Transaction(categoryId = catFoodId, subcategoryId = subDinner, amount = 1200.0, description = "Family Dinner", timestamp = now - 3 * dayMs, type = "EXPENSE"))
            transactionDao.insertTransaction(Transaction(categoryId = catFoodId, subcategoryId = subSnacks, amount = 180.0, description = "Cappuccino & Croissant", timestamp = now, type = "EXPENSE"))
            
            transactionDao.insertTransaction(Transaction(categoryId = catTransportId, subcategoryId = subMetro, amount = 850.0, description = "Monthly train card", timestamp = now - 5 * dayMs, type = "EXPENSE"))
            transactionDao.insertTransaction(Transaction(categoryId = catTransportId, subcategoryId = subFuel, amount = 2200.0, description = "Gas refill", timestamp = now - 1 * dayMs, type = "EXPENSE"))
            
            transactionDao.insertTransaction(Transaction(categoryId = catShoppingId, amount = 4800.0, description = "Summer Jacket & Jeans", timestamp = now - 4 * dayMs, type = "EXPENSE"))
            transactionDao.insertTransaction(Transaction(categoryId = catHealthId, amount = 1500.0, description = "Multivitamins & Prescription", timestamp = now - 6 * dayMs, type = "EXPENSE"))
            transactionDao.insertTransaction(Transaction(categoryId = catEntId, amount = 950.0, description = "Movie night & Popcorn", timestamp = now - 2 * dayMs, type = "EXPENSE"))
            transactionDao.insertTransaction(Transaction(categoryId = catBillsId, amount = 4500.0, description = "Electricity & Water Bill", timestamp = now - 8 * dayMs, type = "EXPENSE"))
            transactionDao.insertTransaction(Transaction(categoryId = catTravelId, amount = 12000.0, description = "Weekend Gateway Resort", timestamp = now - 10 * dayMs, type = "EXPENSE"))
            transactionDao.insertTransaction(Transaction(categoryId = catEduId, amount = 2500.0, description = "Compose Coding course", timestamp = now - 7 * dayMs, type = "EXPENSE"))

            // Curr month income
            transactionDao.insertTransaction(Transaction(categoryId = catSalaryId, subcategoryId = subSalaryMonthly, amount = 75000.0, description = "Monthly Salary Credit", timestamp = now - 5 * dayMs, type = "INCOME"))
            transactionDao.insertTransaction(Transaction(categoryId = catBusinessId, subcategoryId = subBizInvoice, amount = 18000.0, description = "Client Invoice Payment", timestamp = now - 12 * dayMs, type = "INCOME"))

            // Last month spending (Expense)
            transactionDao.insertTransaction(Transaction(categoryId = catFoodId, subcategoryId = subBreakfast, amount = 280.0, description = "Breakfast", timestamp = now - monthMs - 2 * dayMs, type = "EXPENSE"))
            transactionDao.insertTransaction(Transaction(categoryId = catFoodId, subcategoryId = subLunch, amount = 580.0, description = "Lunch out", timestamp = now - monthMs - 3 * dayMs, type = "EXPENSE"))
            transactionDao.insertTransaction(Transaction(categoryId = catFoodId, subcategoryId = subDinner, amount = 980.0, description = "Dinner out", timestamp = now - monthMs - 4 * dayMs, type = "EXPENSE"))
            transactionDao.insertTransaction(Transaction(categoryId = catFoodId, subcategoryId = subSnacks, amount = 150.0, description = "Mocha & Muffin", timestamp = now - monthMs - 1 * dayMs, type = "EXPENSE"))

            transactionDao.insertTransaction(Transaction(categoryId = catTransportId, subcategoryId = subMetro, amount = 850.0, description = "Monthly train card", timestamp = now - monthMs - 5 * dayMs, type = "EXPENSE"))
            transactionDao.insertTransaction(Transaction(categoryId = catTransportId, subcategoryId = subFuel, amount = 2000.0, description = "Gas refill", timestamp = now - monthMs - 7 * dayMs, type = "EXPENSE"))

            transactionDao.insertTransaction(Transaction(categoryId = catShoppingId, amount = 5500.0, description = "Shopping spree", timestamp = now - monthMs - 12 * dayMs, type = "EXPENSE"))
            transactionDao.insertTransaction(Transaction(categoryId = catHealthId, amount = 800.0, description = "Pharmacy", timestamp = now - monthMs - 14 * dayMs, type = "EXPENSE"))
            transactionDao.insertTransaction(Transaction(categoryId = catEntId, amount = 1100.0, description = "Concert Tix", timestamp = now - monthMs - 10 * dayMs, type = "EXPENSE"))
            transactionDao.insertTransaction(Transaction(categoryId = catBillsId, amount = 4200.0, description = "Rent and Utilities", timestamp = now - monthMs - 15 * dayMs, type = "EXPENSE"))
            transactionDao.insertTransaction(Transaction(categoryId = catTravelId, amount = 10000.0, description = "Flight booking", timestamp = now - monthMs - 20 * dayMs, type = "EXPENSE"))
            transactionDao.insertTransaction(Transaction(categoryId = catEduId, amount = 3000.0, description = "Android Books", timestamp = now - monthMs - 18 * dayMs, type = "EXPENSE"))

            // Last month income
            transactionDao.insertTransaction(Transaction(categoryId = catSalaryId, subcategoryId = subSalaryMonthly, amount = 75000.0, description = "Monthly Salary Credit", timestamp = now - monthMs - 5 * dayMs, type = "INCOME"))
        } else if (categories.none { it.type == "INCOME" }) {
            // Seed Income Categories for existing upgraded database
            val catSalaryId = categoryDao.insertCategory(Category(name = "Salary", colorHex = "#2E7D32", iconName = "payments", budgetLimit = 0.0, type = "INCOME")).toInt()
            val catBusinessId = categoryDao.insertCategory(Category(name = "Business", colorHex = "#00897B", iconName = "business", budgetLimit = 0.0, type = "INCOME")).toInt()
            val catGiftsId = categoryDao.insertCategory(Category(name = "Gifts", colorHex = "#D81B60", iconName = "card_giftcard", budgetLimit = 0.0, type = "INCOME")).toInt()
            val catInterestId = categoryDao.insertCategory(Category(name = "Interest", colorHex = "#1E88E5", iconName = "trending_up", budgetLimit = 0.0, type = "INCOME")).toInt()

            val subSalaryMonthly = subcategoryDao.insertSubcategory(Subcategory(parentCategoryId = catSalaryId, name = "Monthly Pay", colorHexOverride = "#4CAF50", iconName = "payments", sortOrder = 0)).toInt()
            subcategoryDao.insertSubcategory(Subcategory(parentCategoryId = catSalaryId, name = "Bonus", colorHexOverride = "#81C784", iconName = "loyalty", sortOrder = 1))
            val subBizInvoice = subcategoryDao.insertSubcategory(Subcategory(parentCategoryId = catBusinessId, name = "Client Invoice", colorHexOverride = "#26A69A", iconName = "request_quote", sortOrder = 0)).toInt()
            subcategoryDao.insertSubcategory(Subcategory(parentCategoryId = catBusinessId, name = "Consulting", colorHexOverride = "#80CBC4", iconName = "handshake", sortOrder = 1))
            subcategoryDao.insertSubcategory(Subcategory(parentCategoryId = catGiftsId, name = "Presents", colorHexOverride = "#F06292", iconName = "card_giftcard"))
            subcategoryDao.insertSubcategory(Subcategory(parentCategoryId = catInterestId, name = "Dividends", colorHexOverride = "#42A5F5", iconName = "show_chart"))

            // Seed a demo income transaction if none exist
            transactionDao.insertTransaction(Transaction(categoryId = catSalaryId, subcategoryId = subSalaryMonthly, amount = 75000.0, description = "Monthly Salary Credit", timestamp = now - 5 * dayMs, type = "INCOME"))
            transactionDao.insertTransaction(Transaction(categoryId = catBusinessId, subcategoryId = subBizInvoice, amount = 18000.0, description = "Client Invoice Payment", timestamp = now - 12 * dayMs, type = "INCOME"))
        }

        // Always ensure system categories exist
        ensureSystemCategories()
    }
}
