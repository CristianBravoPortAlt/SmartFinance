package com.example.smartfinance.data.repository

import com.example.smartfinance.data.local.GoalEntity
import com.example.smartfinance.data.local.UserEntity
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.flowOf
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class BalanceRepository @Inject constructor(
    private val userRepository: UserRepository,
    private val transactionRepository: TransactionRepository,
    private val goalRepository: GoalRepository
) {

    @OptIn(ExperimentalCoroutinesApi::class)
    fun getBalanceFlow(): Flow<Double> {
        return userRepository.getCurrentUserFlow().flatMapLatest { user ->
            if (user != null) {
                combine(
                    transactionRepository.getTotalIncome(user.id),
                    transactionRepository.getTotalExpense(user.id),
                    goalRepository.getGoalsByUser(user.id)
                ) { income, expense, goals ->
                    calculateBalance(user, income ?: 0.0, expense ?: 0.0, goals)
                }
            } else {
                flowOf(0.0)
            }
        }
    }

    private fun calculateBalance(
        user: UserEntity,
        income: Double,
        expense: Double,
        goals: List<GoalEntity>
    ): Double {
        val transactionsBalance = user.initialBalance + income - expense
        val goalsTotal = goals.sumOf { it.currentAmount }
        return transactionsBalance - goalsTotal
    }
}
