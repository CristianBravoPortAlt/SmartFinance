package com.example.smartfinance.data.local

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "categories")
data class CategoryEntity(
    @PrimaryKey val id: String = "",
    val userId: String = "",
    val name: String = "",
    val iconResName: String = "",
    val colorHex: String = "#9E9E9E",
    val type: TransactionType = TransactionType.EXPENSE
)