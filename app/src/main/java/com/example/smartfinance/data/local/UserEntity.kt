package com.example.smartfinance.data.local

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "users")
data class UserEntity(
    @PrimaryKey val id: String = "",
    val name: String = "",
    val email: String = "",
    val monthlyBudget: Double = 500.0,
    val phone: String = "",
    val occupation: String = "",
    val profilePictureUri: String? = null,
    val themePreference: String = "system",
    val initialBalance: Double = 0.0,
    val hasSalary: Boolean = false,
    val salaryAmount: Double? = null,
    val salaryPayDay: Int? = null,
    val budgetNotificationSentMonth: String = "",
    val budgetWarningDismissedMonth: String = ""
)
