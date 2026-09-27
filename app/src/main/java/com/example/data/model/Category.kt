package com.example.data.model

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "categories")
data class Category(
    @PrimaryKey(autoGenerate = true) val id: Int = 0,
    val name: String,
    val colorHex: String, // e.g. "#FF6B6B"
    val iconName: String, // e.g. "shopping_bag"
    val budgetLimit: Double = 0.0,
    @ColumnInfo(defaultValue = "'EXPENSE'")
    val type: String = "EXPENSE", // "EXPENSE" or "INCOME"
    @ColumnInfo(defaultValue = "'STANDARD'")
    val role: String = CategoryRole.STANDARD // see CategoryRole: lending / transfer categories stay out of totals
)

val Category.displayName: String
    get() = if (name == SystemCategories.LOAN_REPAYMENT) "Loan Pay Back" else name

/** Built-in categories the app relies on; they can be edited but never deleted. */
object SystemCategories {
    const val LENDING = "Lending"
    const val LOAN_REPAYMENT = "Loan Repayment"
    const val TRANSFER_OUT = "Transfer Out"
    const val TRANSFER_IN = "Transfer In"
    val NAMES = setOf(LENDING, LOAN_REPAYMENT, TRANSFER_OUT, TRANSFER_IN)
}

val Category.isSystem: Boolean get() = name in SystemCategories.NAMES

@Entity(tableName = "subcategories")
data class Subcategory(
    @PrimaryKey(autoGenerate = true) val id: Int = 0,
    val parentCategoryId: Int,
    val name: String,
    val colorHexOverride: String? = null, // Optional custom shade, e.g. "#FF8585", if null inherits parent color
    val iconName: String? = null, // Optional icon key from the icon catalog; null inherits the parent's icon
    @ColumnInfo(defaultValue = "0")
    val sortOrder: Int = 0 // Position within the parent, set from the editor's list order
)
