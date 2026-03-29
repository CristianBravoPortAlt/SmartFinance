package com.example.smartfinance.ui.screens.auth

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.smartfinance.data.local.UserEntity
import com.example.smartfinance.data.repository.UserRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class AuthViewModel @Inject constructor(
    private val userRepository: UserRepository
) : ViewModel() {

    var state by mutableStateOf<AuthState>(AuthState.Idle)
        private set

    fun register(name: String, email: String, pin: String) {
        state = AuthState.Loading
        viewModelScope.launch {
            try {
                val newUser = UserEntity(name = name, email = email, pin = pin)
                userRepository.registerUser(newUser)
                state = AuthState.Success
            } catch (e: Exception) {
                state = AuthState.Error("Error al registrar: ${e.message}")
            }
        }
    }

    fun login(email: String, pin: String) {
        state = AuthState.Loading
        viewModelScope.launch {
            try {
                val user = userRepository.login(email, pin)
                state = if (user != null) {
                    AuthState.Success
                } else {
                    AuthState.Error("Credenciales incorrectas")
                }
            } catch (e: Exception) {
                state = AuthState.Error("Error al iniciar sesión")
            }
        }
    }

    fun resetState() {
        state = AuthState.Idle
    }
}