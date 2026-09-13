package com.example.data.model

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "lending_contacts")
data class LendingContact(
    @PrimaryKey(autoGenerate = true) val id: Int = 0,
    val name: String,
    val note: String? = null,
    val colorHex: String? = null,
    val iconName: String? = null
)

@Entity(tableName = "lending_entries")
data class LendingEntry(
    @PrimaryKey(autoGenerate = true) val id: Int = 0,
    val contactId: Int,
    val amount: Double,
    val direction: String, // "LENT" or "REPAID"
    val description: String,
    val timestamp: Long = System.currentTimeMillis()
)
