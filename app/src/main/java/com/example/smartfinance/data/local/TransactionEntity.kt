package com.example.smartfinance.data.local

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "transactions")
data class TransactionEntity(
    @PrimaryKey val id: String = "",
    val userId: String = "",
    val categoryId: String = "",
    val title: String = "",
    val amount: Double = 0.0,
    val dateMillis: Long = System.currentTimeMillis(),
    val type: TransactionType = TransactionType.EXPENSE,
    val description: String = "",
    val recurrence: RecurrenceType = RecurrenceType.NONE
)
