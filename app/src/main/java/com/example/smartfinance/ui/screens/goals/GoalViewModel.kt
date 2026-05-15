package com.example.smartfinance.ui.screens.goals

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.smartfinance.data.local.GoalEntity
import com.example.smartfinance.data.local.TransactionEntity
import com.example.smartfinance.data.local.TransactionType
import com.example.smartfinance.data.repository.CategoryRepository
import com.example.smartfinance.data.repository.GoalRepository
import com.example.smartfinance.data.repository.TransactionRepository
import com.example.smartfinance.data.repository.UserRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import java.util.UUID
import javax.inject.Inject

@OptIn(ExperimentalCoroutinesApi::class)
@HiltViewModel
class GoalViewModel @Inject constructor(
    private val goalRepository: GoalRepository,
    private val transactionRepository: TransactionRepository,
    private val categoryRepository: CategoryRepository,
    private val userRepository: UserRepository,
    private val balanceRepository: com.example.smartfinance.data.repository.BalanceRepository,
    private val notificationHelper: com.example.smartfinance.utils.NotificationHelper
) : ViewModel() {

    private val userUid: String?
        get() = userRepository.currentUserUid

    val balance: StateFlow<Double> = balanceRepository.getBalanceFlow()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), 0.0)

    val goals: StateFlow<List<GoalEntity>> = userRepository.getCurrentUserFlow().flatMapLatest { user ->
        if (user != null) goalRepository.getGoalsByUser(user.id)
        else flowOf(emptyList())
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    private val _selectedGoal = MutableStateFlow<GoalEntity?>(null)
    val selectedGoal = _selectedGoal.asStateFlow()

    fun selectGoal(goalId: String) {
        viewModelScope.launch {
            val goal = goalRepository.getGoalById(goalId)
            _selectedGoal.value = goal
        }
    }

    fun transferToGoal(goal: GoalEntity, amount: Double) {
        if (amount <= 0 || amount > balance.value) return
        
        viewModelScope.launch {
            val updatedGoal = goal.copy(currentAmount = goal.currentAmount + amount)
            goalRepository.updateGoal(updatedGoal)
            
            userUid?.let { uid ->
                val categories = categoryRepository.getCategoriesByType(TransactionType.EXPENSE).first()
                val transferCategory = categories.find { it.name.contains("Transferencia", ignoreCase = true) }

                transactionRepository.insertTransaction(
                    TransactionEntity(
                        id = UUID.randomUUID().toString(),
                        userId = uid,
                        categoryId = transferCategory?.id ?: "",
                        title = "Ahorro: ${goal.name}",
                        amount = amount,
                        type = TransactionType.EXPENSE,
                        description = "Traspaso manual a objetivo",
                        dateMillis = System.currentTimeMillis()
                    )
                )
            }

            if (_selectedGoal.value?.id == goal.id) {
                _selectedGoal.value = updatedGoal
            }
            
            if (updatedGoal.currentAmount >= updatedGoal.targetAmount) {
                notificationHelper.showNotification(
                    title = "¡Objetivo Completado!",
                    message = "Has alcanzado tu meta de ${updatedGoal.targetAmount}€ en '${updatedGoal.name}'"
                )
            }
        }
    }

    fun withdrawFromGoal(goal: GoalEntity, amount: Double) {
        if (amount <= 0 || amount > goal.currentAmount) return
        
        viewModelScope.launch {
            val updatedGoal = goal.copy(currentAmount = goal.currentAmount - amount)
            goalRepository.updateGoal(updatedGoal)

            userUid?.let { uid ->
                val categories = categoryRepository.getCategoriesByType(TransactionType.INCOME).first()
                val transferCategory = categories.find { it.name.contains("Transferencia", ignoreCase = true) }

                transactionRepository.insertTransaction(
                    TransactionEntity(
                        id = UUID.randomUUID().toString(),
                        userId = uid,
                        categoryId = transferCategory?.id ?: "",
                        title = "Retirada: ${goal.name}",
                        amount = amount,
                        type = TransactionType.INCOME,
                        description = "Retirada de fondos de objetivo",
                        dateMillis = System.currentTimeMillis()
                    )
                )
            }

            if (_selectedGoal.value?.id == goal.id) {
                _selectedGoal.value = updatedGoal
            }
        }
    }

    fun insertGoal(goal: GoalEntity) {
        viewModelScope.launch {
            userUid?.let { uid ->
                goalRepository.insertGoal(goal.copy(userId = uid))
            }
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
