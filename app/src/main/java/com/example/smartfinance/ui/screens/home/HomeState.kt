package com.example.smartfinance.ui.screens.home

import com.example.smartfinance.data.local.UserEntity

sealed class HomeState {
    data object Loading : HomeState()
    data object NoData : HomeState()
    data class Success(val user: UserEntity, val balance: Double) : HomeState()
}