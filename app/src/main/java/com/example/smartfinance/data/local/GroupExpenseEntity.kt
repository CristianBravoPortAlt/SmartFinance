package com.example.smartfinance.data.local

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "group_expenses")
data class GroupExpenseEntity(
    @PrimaryKey val id: String = "",
    val groupId: String = "",
    val paidByUserId: String = "",
    val amount: Double = 0.0,
    val description: String = "",
    val date: Long = System.currentTimeMillis(),
    val category: String = "General",
    val splitType: String = "EQUAL", // EQUAL, PERCENTAGE o EXACT
    val linkedTransactionId: String? = null
)
