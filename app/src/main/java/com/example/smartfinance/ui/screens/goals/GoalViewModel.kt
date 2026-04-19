package com.example.smartfinance.ui.screens.goals

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.smartfinance.data.local.GoalEntity
import com.example.smartfinance.data.repository.GoalRepository
import com.example.smartfinance.data.repository.TransactionRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class GoalViewModel @Inject constructor(
    private val goalRepository: GoalRepository,
    private val transactionRepository: TransactionRepository,
    private val userRepository: com.example.smartfinance.data.repository.UserRepository
) : ViewModel() {

    private val userId: Int
        get() = userRepository.currentUserId

    private val totalIncome = transactionRepository.getTotalIncome(userId)
    private val totalExpense = transactionRepository.getTotalExpense(userId)

    val balance: StateFlow<Double> = combine(totalIncome, totalExpense) { income, expense ->
        (income ?: 0.0) - (expense ?: 0.0)
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), 0.0)

    val goals: StateFlow<List<GoalEntity>> = combine(
        goalRepository.getGoalsByUser(userId),
        balance
    ) { goalsList, currentBalance ->
        var remainingBalance = currentBalance
        goalsList.map { goal ->
            val allocated = minOf(remainingBalance, goal.targetAmount).coerceAtLeast(0.0)
            remainingBalance = (remainingBalance - allocated).coerceAtLeast(0.0)
            goal.copy(currentAmount = allocated)
        }
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    fun insertGoal(goal: GoalEntity) {
        viewModelScope.launch {
            goalRepository.insertGoal(goal.copy(userId = userId))
        }
    }

    fun updateGoal(goal: GoalEntity) {
        viewModelScope.launch {
            goalRepository.updateGoal(goal)
        }
    }

    fun deleteGoal(goal: GoalEntity) {
        viewModelScope.launch {
            goalRepository.deleteGoal(goal)
        }
    }
}
