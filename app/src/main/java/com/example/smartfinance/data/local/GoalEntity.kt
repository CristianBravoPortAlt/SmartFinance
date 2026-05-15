package com.example.smartfinance.data.local

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "goals")
data class GoalEntity(
    @PrimaryKey val id: String = "",
    val userId: String = "",
    val name: String = "",
    val targetAmount: Double = 0.0,
    val currentAmount: Double = 0.0,
    val deadlineMillis: Long? = null,
    val colorHex: String = "#2196F3",
    val recurrence: RecurrenceType = RecurrenceType.NONE,
    val recurrenceAmount: Double = 0.0,
    val lastRecurrenceMillis: Long? = null
)
