package com.example.smartfinance.di

import android.content.Context
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.sqlite.db.SupportSQLiteDatabase
import com.example.smartfinance.data.local.CategoryDao
import com.example.smartfinance.data.local.CategoryEntity
import com.example.smartfinance.data.local.SmartFinanceDatabase
import com.example.smartfinance.data.local.TransactionDao
import com.example.smartfinance.data.local.TransactionType
import com.example.smartfinance.data.local.UserDao
import com.example.smartfinance.data.local.GoalDao
import com.example.smartfinance.data.repository.UserRepository
import com.example.smartfinance.data.repository.GoalRepository
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import javax.inject.Provider
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
object DatabaseModule {

    @Provides
    @Singleton
    fun provideSmartFinanceDatabase(
        @ApplicationContext context: Context,
        categoryDaoProvider: Provider<CategoryDao>
    ): SmartFinanceDatabase {
        return Room.databaseBuilder(
            context,
            SmartFinanceDatabase::class.java,
            "smart_finance_db"
        )
        .fallbackToDestructiveMigration()
        .addCallback(object : RoomDatabase.Callback() {
            override fun onCreate(db: SupportSQLiteDatabase) {
                super.onCreate(db)
                CoroutineScope(Dispatchers.IO).launch {
                    val dao = categoryDaoProvider.get()
                    val initialCategories = listOf(
                        CategoryEntity(name = "Comida", iconResName = "restaurant", colorHex = "#FF5722", type = TransactionType.EXPENSE),
                        CategoryEntity(name = "Ocio", iconResName = "movie", colorHex = "#9C27B0", type = TransactionType.EXPENSE),
                        CategoryEntity(name = "Transporte", iconResName = "directions_car", colorHex = "#2196F3", type = TransactionType.EXPENSE),
                        CategoryEntity(name = "Sueldo", iconResName = "attach_money", colorHex = "#4CAF50", type = TransactionType.INCOME),
                        CategoryEntity(name = "Regalos", iconResName = "card_giftcard", colorHex = "#FFC107", type = TransactionType.INCOME)
                    )
                    initialCategories.forEach { dao.insertCategory(it) }
                }
            }
        })
        .build()
    }

    @Provides
    @Singleton
    fun provideUserDao(database: SmartFinanceDatabase): UserDao {
        return database.userDao()
    }

    @Provides
    @Singleton
    fun provideUserRepository(userDao: UserDao): UserRepository {
        return UserRepository(userDao)
    }

    @Provides
    @Singleton
    fun provideCategoryDao(database: SmartFinanceDatabase): CategoryDao {
        return database.categoryDao()
    }

    @Provides
    @Singleton
    fun provideTransactionDao(database: SmartFinanceDatabase): TransactionDao {
        return database.transactionDao()
    }

    @Provides
    @Singleton
    fun provideGoalDao(database: SmartFinanceDatabase): GoalDao {
        return database.goalDao()
    }

    @Provides
    @Singleton
    fun provideGoalRepository(goalDao: GoalDao): GoalRepository {
        return GoalRepository(goalDao)
    }
}