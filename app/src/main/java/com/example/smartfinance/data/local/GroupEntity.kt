package com.example.smartfinance.data.local

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "groups")
data class GroupEntity(
    @PrimaryKey val id: String = "",
    val name: String = "",
    val description: String = "",
    val inviteCode: String = "",
    val adminId: String = ""
)
