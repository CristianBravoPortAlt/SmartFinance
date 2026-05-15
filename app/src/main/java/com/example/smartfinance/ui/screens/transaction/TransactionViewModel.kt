package com.example.smartfinance.ui.screens.transaction

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.smartfinance.data.local.CategoryEntity
import com.example.smartfinance.data.local.TransactionEntity
import com.example.smartfinance.data.local.UserEntity
import com.example.smartfinance.data.repository.CategoryRepository
import com.example.smartfinance.data.repository.GoalRepository
import com.example.smartfinance.data.repository.TransactionRepository
import com.example.smartfinance.data.repository.UserRepository
import com.example.smartfinance.utils.NotificationHelper
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import javax.inject.Inject

@OptIn(ExperimentalCoroutinesApi::class)
@HiltViewModel
class TransactionViewModel @Inject constructor(
    private val transactionRepository: TransactionRepository,
    private val categoryRepository: CategoryRepository,
    private val userRepository: UserRepository,
    private val goalRepository: GoalRepository,
    private val balanceRepository: com.example.smartfinance.data.repository.BalanceRepository,
    private val notificationHelper: NotificationHelper
) : ViewModel() {

    private val userUid: String?
        get() = userRepository.currentUserUid

    init {
        viewModelScope.launch {
            categoryRepository.initializeCategoriesIfNeeded()
        }
    }

    val transactions: StateFlow<List<TransactionEntity>> = userRepository.getCurrentUserFlow()
        .flatMapLatest { user ->
            if (user != null) transactionRepository.getTransactionsByUser(user.id)
            else flowOf(emptyList())
        }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val categories: StateFlow<List<CategoryEntity>> = categoryRepository.getAllCategories()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val totalIncome: StateFlow<Double> = userRepository.getCurrentUserFlow()
        .flatMapLatest { user ->
            if (user != null) transactionRepository.getTotalIncome(user.id)
            else flowOf(0.0)
        }.map { it ?: 0.0 }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), 0.0)

    val totalExpense: StateFlow<Double> = userRepository.getCurrentUserFlow()
        .flatMapLatest { user ->
            if (user != null) transactionRepository.getTotalExpense(user.id)
            else flowOf(0.0)
        }.map { it ?: 0.0 }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), 0.0)

    val balance: StateFlow<Double> = balanceRepository.getBalanceFlow()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), 0.0)

    init {
        viewModelScope.launch {
            combine(
                totalExpense,
                userRepository.getCurrentUserFlow()
            ) { expense, user ->
                Pair(expense, user)
            }.collectLatest { (expense, user) ->
                checkBudgetLimit(expense, user)
            }
        }
    }

    private fun checkBudgetLimit(expense: Double, user: UserEntity?) {
        val budget = user?.monthlyBudget ?: 0.0
        val currentMonth = SimpleDateFormat("yyyy-MM", Locale.getDefault()).format(Date())
        
        if (budget > 0 && expense >= budget) {
            if (user != null && user.budgetNotificationSentMonth != currentMonth) {
                notificationHelper.showNotification(
                    title = "¡Presupuesto Excedido!",
                    message = "Has superado tu límite mensual de ${String.format(Locale.getDefault(), "%.2f", budget)}€"
                )
                
                viewModelScope.launch {
                    userRepository.updateUser(user.copy(budgetNotificationSentMonth = currentMonth))
                }
            }
        }
    }

    fun insertTransaction(transaction: TransactionEntity) {
        viewModelScope.launch {
            userUid?.let { uid ->
                transactionRepository.insertTransaction(transaction.copy(userId = uid))
            }
        }
    }

    fun deleteTransaction(transaction: TransactionEntity) {
        viewModelScope.launch {
            transactionRepository.deleteTransaction(transaction)
        }
    }

    suspend fun getTransactionById(id: String): TransactionEntity? {
        return transactionRepository.getTransactionById(id)
    }

    fun insertCategory(category: CategoryEntity) {
        viewModelScope.launch {
            categoryRepository.insertCategory(category)
        }
    }

    fun updateCategory(category: CategoryEntity) {
        viewModelScope.launch {
            categoryRepository.updateCategory(category)
        }
    }

    fun deleteCategory(category: CategoryEntity) {
        viewModelScope.launch {
            categoryRepository.deleteCategory(category)
        }
    }
}
