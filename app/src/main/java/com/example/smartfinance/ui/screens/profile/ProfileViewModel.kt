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
            currentUser = userRepository.getUserById(1)
        }
    }

    fun updateUser(name: String, pin: String, budget: Double) {
        viewModelScope.launch {
            currentUser?.let {
                val updatedUser = it.copy(name = name, pin = pin, monthlyBudget = budget)
                userRepository.updateUser(updatedUser)
                currentUser = updatedUser
            }
        }
    }

    fun deleteAllData(onComplete: () -> Unit) {
        viewModelScope.launch(Dispatchers.IO) {
            database.clearAllTables()
            launch(Dispatchers.Main) {
                onComplete()
            }
        }
    }
}
