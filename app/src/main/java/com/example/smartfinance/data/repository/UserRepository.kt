package com.example.smartfinance.data.repository

import com.example.smartfinance.data.local.UserDao
import com.example.smartfinance.data.local.UserEntity
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class UserRepository @Inject constructor(
    private val userDao: UserDao
) {
    var currentUserId: Int = 1

    suspend fun registerUser(user: UserEntity): Long {
        val id = userDao.registerUser(user)
        currentUserId = id.toInt()
        return id
    }

    suspend fun login(email: String, pin: String): UserEntity? {
        val user = userDao.login(email, pin)
        if (user != null) {
            currentUserId = user.id
        }
        return user
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