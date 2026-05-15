package com.example.smartfinance.ui.screens.home

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.smartfinance.data.repository.FriendRepository
import com.example.smartfinance.data.repository.GroupInvitation
import com.example.smartfinance.data.repository.GroupRepository
import com.example.smartfinance.data.repository.TransactionRepository
import com.example.smartfinance.data.repository.UserRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.emitAll
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import javax.inject.Inject

@OptIn(ExperimentalCoroutinesApi::class)
@HiltViewModel
class HomeViewModel @Inject constructor(
    private val userRepository: UserRepository,
    private val transactionRepository: TransactionRepository,
    private val groupRepository: GroupRepository,
    private val friendRepository: FriendRepository
) : ViewModel() {

    init {
        viewModelScope.launch {
            userRepository.syncUserWithFirebase()
        }
    }

    val state: StateFlow<HomeState> = userRepository.getCurrentUserFlow()
        .flatMapLatest { user ->
            if (user != null) {
                combine(
                    transactionRepository.getTotalIncome(user.id),
                    transactionRepository.getTotalExpense(user.id)
                ) { income, expense ->
                    val balance = (income ?: 0.0) - (expense ?: 0.0)
                    HomeState.Success(user, balance)
                }
            } else {
                if (userRepository.currentUserUid != null) {
                    flowOf(HomeState.Loading)
                } else {
                    flowOf(HomeState.NoData)
                }
            }
        }
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = HomeState.Loading
        )

    val groupInvitations: Flow<List<GroupInvitation>> = flow {
        userRepository.currentUserUid?.let { uid ->
            emitAll(groupRepository.getInvitationsForUser(uid))
        } ?: emit(emptyList())
    }

    val friendRequests: Flow<List<com.example.smartfinance.data.local.UserEntity>> = flow {
        userRepository.currentUserUid?.let {
            emitAll(friendRepository.getPendingRequests())
        } ?: emit(emptyList())
    }

    fun acceptGroupInvitation(invitationId: String) {
        viewModelScope.launch { groupRepository.acceptInvitation(invitationId) }
    }

    fun rejectGroupInvitation(invitationId: String) {
        viewModelScope.launch { groupRepository.rejectInvitation(invitationId) }
    }

    fun acceptFriendRequest(friendId: String) {
        viewModelScope.launch { friendRepository.acceptFriendRequest(friendId) }
    }

    fun rejectFriendRequest(friendId: String) {
        viewModelScope.launch { friendRepository.rejectFriendRequest(friendId) }
    }

    fun dismissBudgetWarning() {
        viewModelScope.launch {
            val user = userRepository.getCurrentUserFlow().first()
            if (user != null) {
                val currentMonth = SimpleDateFormat("yyyy-MM", Locale.getDefault()).format(Date())
                userRepository.updateUser(user.copy(budgetWarningDismissedMonth = currentMonth))
            }
        }
    }
}
