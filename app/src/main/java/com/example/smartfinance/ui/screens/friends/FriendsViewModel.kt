package com.example.smartfinance.ui.screens.friends

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.smartfinance.data.local.GroupEntity
import com.example.smartfinance.data.local.UserEntity
import com.example.smartfinance.data.repository.ContactInfo
import com.example.smartfinance.data.repository.FriendRepository
import com.example.smartfinance.data.repository.GroupInvitation
import com.example.smartfinance.data.repository.GroupRepository
import com.example.smartfinance.data.repository.UserRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class FriendsViewModel @Inject constructor(
    private val friendRepository: FriendRepository,
    private val groupRepository: GroupRepository,
    private val userRepository: UserRepository
) : ViewModel() {
    private val _friends = MutableStateFlow<List<UserEntity>>(emptyList())
    private val _pendingRequests = MutableStateFlow<List<UserEntity>>(emptyList())
    private val _pendingGroupInvitations = MutableStateFlow<List<Pair<GroupInvitation, GroupEntity>>>(emptyList())
    private val _contacts = MutableStateFlow<List<ContactInfo>>(emptyList())
    private val _searchQuery = MutableStateFlow("")

    val searchQuery: StateFlow<String> = _searchQuery

    val filteredFriends: StateFlow<List<UserEntity>> = combine(_friends, _searchQuery) { friends, query ->
        if (query.isBlank()) friends
        else friends.filter { it.name.contains(query, ignoreCase = true) || it.email.contains(query, ignoreCase = true) }
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val pendingRequests: StateFlow<List<UserEntity>> = _pendingRequests
    val pendingGroupInvitations: StateFlow<List<Pair<GroupInvitation, GroupEntity>>> = _pendingGroupInvitations

    val filteredContacts: StateFlow<List<ContactInfo>> = combine(_contacts, _searchQuery) { contacts, query ->
        if (query.isBlank()) contacts
        else contacts.filter { it.displayName.contains(query, ignoreCase = true) || it.phoneNumber.contains(query, ignoreCase = true) }
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    init {
        viewModelScope.launch {
            friendRepository.getFriends().collectLatest {
                _friends.value = it
            }
        }
        viewModelScope.launch {
            friendRepository.getPendingRequests().collectLatest {
                _pendingRequests.value = it
            }
        }
        viewModelScope.launch {
            userRepository.authStateFlow.collectLatest { uid ->
                if (uid != null) {
                    groupRepository.getPendingGroupInvitationsWithGroups(uid).collectLatest {
                        _pendingGroupInvitations.value = it
                    }
                }
            }
        }
    }

    fun onSearchQueryChange(query: String) {
        _searchQuery.value = query
    }

    fun loadContacts() {
        viewModelScope.launch {
            _contacts.value = friendRepository.getPhoneContacts()
        }
    }

    fun addFriend(email: String, onResult: (Boolean) -> Unit) {
        viewModelScope.launch {
            val success = friendRepository.addFriendByEmail(email)
            onResult(success)
        }
    }

    fun acceptFriend(friendId: String) {
        viewModelScope.launch { friendRepository.acceptFriendRequest(friendId) }
    }

    fun rejectFriend(friendId: String) {
        viewModelScope.launch { friendRepository.rejectFriendRequest(friendId) }
    }

    fun deleteFriend(friendId: String) {
        viewModelScope.launch { friendRepository.deleteFriend(friendId) }
    }

    fun acceptGroupInvite(invitationId: String) {
        viewModelScope.launch { groupRepository.acceptInvitation(invitationId) }
    }

    fun rejectGroupInvite(invitationId: String) {
        viewModelScope.launch { groupRepository.rejectInvitation(invitationId) }
    }
}
