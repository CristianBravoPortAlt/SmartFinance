package com.example.smartfinance.data.local

import androidx.room.Database
import androidx.room.RoomDatabase

@Database(entities = [UserEntity::class, CategoryEntity::class, TransactionEntity::class, GoalEntity::class], version = 5, exportSchema = false)
abstract class SmartFinanceDatabase : RoomDatabase() {
    abstract fun userDao(): UserDao
    abstract fun categoryDao(): CategoryDao
    abstract fun transactionDao(): TransactionDao
    abstract fun goalDao(): GoalDao
}