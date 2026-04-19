package com.example.smartfinance.ui.screens.profile

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.smartfinance.data.local.SmartFinanceDatabase
import com.example.smartfinance.data.local.UserEntity
import com.example.smartfinance.data.repository.UserRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class ProfileViewModel @Inject constructor(
    private val userRepository: UserRepository,
    private val database: SmartFinanceDatabase
) : ViewModel() {

    var currentUser by mutableStateOf<UserEntity?>(null)
        private set

    init {
        loadUser()
    }

    private fun loadUser() {
        viewModelScope.launch {
            currentUser = userRepository.getUserById(userRepository.currentUserId)
        }
    }

    fun updateUser(name: String, budget: Double, phone: String, occupation: String) {
        viewModelScope.launch {
            currentUser?.let {
                val updatedUser = it.copy(
                    name = name, 
                    monthlyBudget = budget,
                    phone = phone,
                    occupation = occupation
                )
                userRepository.updateUser(updatedUser)
                currentUser = updatedUser
            }
        }
    }

    fun clearUserDataOnly(onComplete: () -> Unit) {
        viewModelScope.launch(Dispatchers.IO) {
            val userId = userRepository.currentUserId
            database.transactionDao().deleteAllTransactionsByUser(userId)
            database.goalDao().deleteAllGoalsByUser(userId)
            launch(Dispatchers.Main) {
                onComplete()
            }
        }
    }

    fun logout(onComplete: () -> Unit) {
        onComplete()
    }
}
