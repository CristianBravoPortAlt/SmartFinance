package com.example.smartfinance.ui.screens.auth

sealed class AuthState {
    data object Idle : AuthState()
    data object Loading : AuthState()
    data object Success : AuthState()
    data object VerificationSent : AuthState()
    data class Error(val message: String) : AuthState()
}