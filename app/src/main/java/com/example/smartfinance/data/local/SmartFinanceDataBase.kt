package com.example.smartfinance.data.local

import androidx.room.Database
import androidx.room.RoomDatabase
import com.example.smartfinance.data.repository.GroupInvitation

@Database(
    entities = [
        UserEntity::class, 
        CategoryEntity::class, 
        TransactionEntity::class, 
        GoalEntity::class,
        GroupEntity::class,
        UserGroupCrossRef::class,
        FriendshipEntity::class,
        GroupExpenseEntity::class,
        ExpenseSplitEntity::class,
        SettlementEntity::class,
        NotificationEntity::class,
        GroupInvitation::class
    ], 
    version = 17,
    exportSchema = false
)
abstract class SmartFinanceDatabase : RoomDatabase() {
    abstract fun userDao(): UserDao
    abstract fun categoryDao(): CategoryDao
    abstract fun transactionDao(): TransactionDao
    abstract fun goalDao(): GoalDao
    abstract fun groupDao(): GroupDao
    abstract fun notificationDao(): NotificationDao
}
