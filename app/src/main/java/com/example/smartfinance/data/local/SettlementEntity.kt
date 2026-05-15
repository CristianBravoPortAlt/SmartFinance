package com.example.smartfinance.data.local

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "settlements")
data class SettlementEntity(
    @PrimaryKey val id: String = "",
    val groupId: String = "",
    val fromUserId: String = "",
    val toUserId: String = "",
    val amount: Double = 0.0,
    val date: Long = System.currentTimeMillis(),
    val debtorTransactionId: String? = null,
    val creditorTransactionId: String? = null
)
