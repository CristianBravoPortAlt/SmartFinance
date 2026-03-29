package com.example.smartfinance.di

import android.content.Context
import androidx.room.Room
import com.example.smartfinance.data.local.SmartFinanceDatabase
import com.example.smartfinance.data.local.UserDao
import com.example.smartfinance.data.repository.UserRepository

import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
object DatabaseModule {

    @Provides
    @Singleton
    fun provideSmartFinanceDatabase(@ApplicationContext context: Context): SmartFinanceDatabase {
        return Room.databaseBuilder(
            context,
            SmartFinanceDatabase::class.java,
            "smart_finance_db"
        ).build()
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
}