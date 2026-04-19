package com.example.smartfinance.data.repository

import com.example.smartfinance.data.local.GoalDao
import com.example.smartfinance.data.local.GoalEntity
import kotlinx.coroutines.flow.Flow
import javax.inject.Inject

class GoalRepository @Inject constructor(
    private val goalDao: GoalDao
) {
    fun getGoalsByUser(userId: Int): Flow<List<GoalEntity>> = goalDao.getGoalsByUser(userId)

    suspend fun insertGoal(goal: GoalEntity) = goalDao.insertGoal(goal)

    suspend fun updateGoal(goal: GoalEntity) = goalDao.updateGoal(goal)

    suspend fun deleteGoal(goal: GoalEntity) = goalDao.deleteGoal(goal)

    suspend fun getGoalById(id: Int): GoalEntity? = goalDao.getGoalById(id)
}
