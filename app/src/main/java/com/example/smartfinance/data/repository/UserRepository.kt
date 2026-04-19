package com.example.smartfinance.data.repository

import com.example.smartfinance.data.local.UserDao
import com.example.smartfinance.data.local.UserEntity
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class UserRepository @Inject constructor(
    private val userDao: UserDao
) {
    suspend fun registerUser(user: UserEntity) {
        userDao.registerUser(user)
    }

    suspend fun login(email: String, pin: String): UserEntity? {
        return userDao.login(email, pin)
    }

    suspend fun getUserById(userId: Int): UserEntity? {
        return userDao.getUserById(userId)
    }

    suspend fun updateUser(user: UserEntity) {
        userDao.updateUser(user)
    }

    suspend fun deleteUser(user: UserEntity) {
        userDao.deleteUser(user)
    }
}