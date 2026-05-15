package com.example.smartfinance.data.local

import androidx.room.Entity

@Entity(primaryKeys = ["userId", "groupId"])
data class UserGroupCrossRef(
    val userId: String = "",
    val groupId: String = ""
)
