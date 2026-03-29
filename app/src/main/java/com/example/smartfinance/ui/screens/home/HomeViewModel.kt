package com.example.smartfinance.ui.screens.home

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.smartfinance.data.repository.UserRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.Job
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class HomeViewModel @Inject constructor(
    private val userRepository: UserRepository
) : ViewModel() {

    var state by mutableStateOf<HomeState>(HomeState.Loading)
        private set

    private var getDataJob: Job? = null

    fun loadUserData(userId: Int) {
        getDataJob?.cancel()
        getDataJob = viewModelScope.launch {
            state = HomeState.Loading
            try {
                val user = userRepository.getUserById(userId)
                if (user != null) {
                    state = HomeState.Success(user, 1250.50)
                } else {
                    state = HomeState.NoData
                }
            } catch (e: Exception) {
                state = HomeState.NoData
            }
        }
    }
}