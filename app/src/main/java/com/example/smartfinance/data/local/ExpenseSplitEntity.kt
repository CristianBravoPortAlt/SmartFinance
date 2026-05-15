package com.example.smartfinance.data.local

import androidx.room.Entity

@Entity(tableName = "expense_splits", primaryKeys = ["expenseId", "userId"])
data class ExpenseSplitEntity(
    val expenseId: String = "",
    val userId: String = "",
    val amount: Double = 0.0
)
