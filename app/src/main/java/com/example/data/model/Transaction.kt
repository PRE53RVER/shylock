package com.example.data.model

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "transactions")
data class Transaction(
    @PrimaryKey(autoGenerate = true) val id: Int = 0,
    val categoryId: Int,
    val subcategoryId: Int? = null,
    val amount: Double,
    val description: String,
    val timestamp: Long = System.currentTimeMillis(),
    val type: String = "EXPENSE", // "EXPENSE" or "INCOME"
    val lendingEntryId: Int? = null
)
