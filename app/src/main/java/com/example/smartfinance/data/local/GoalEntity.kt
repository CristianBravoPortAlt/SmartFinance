package com.example.smartfinance.data.local

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "goals")
data class GoalEntity(
    @PrimaryKey(autoGenerate = true) val id: Int = 0,
    val userId: Int,
    val name: String,
    val targetAmount: Double,
    val currentAmount: Double = 0.0,
    val deadlineMillis: Long? = null,
    val colorHex: String = "#2196F3"
)
