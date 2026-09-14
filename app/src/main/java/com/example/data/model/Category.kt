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
    val type: String = "EXPENSE" // "EXPENSE" or "INCOME"
)

val Category.displayName: String
    get() = if (name == "Loan Repayment") "Loan Pay Back" else name

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
