package com.example.smartfinance.data.local

import androidx.room.Database
import androidx.room.RoomDatabase

@Database(entities = [UserEntity::class], version = 1, exportSchema = false)
abstract class SmartFinanceDatabase : RoomDatabase() {
    abstract fun userDao(): UserDao
}