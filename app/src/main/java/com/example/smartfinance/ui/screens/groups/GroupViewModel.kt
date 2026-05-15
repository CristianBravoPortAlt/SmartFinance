package com.example.smartfinance.ui.screens.groups

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.smartfinance.data.local.GroupEntity
import com.example.smartfinance.data.repository.GroupRepository
import com.example.smartfinance.data.repository.UserRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class GroupViewModel @Inject constructor(
    private val userRepository: UserRepository,
    private val groupRepository: GroupRepository
) : ViewModel() {

    private val _groups = MutableStateFlow<List<GroupEntity>>(emptyList())
    val groups: StateFlow<List<GroupEntity>> = _groups

    init {
        viewModelScope.launch {
            userRepository.currentUserUid?.let { uid ->
                groupRepository.getGroupsForUser(uid).collectLatest {
                    _groups.value = it
                }
            }
        }
    }

    fun createGroup(name: String, description: String) {
        viewModelScope.launch {
            userRepository.currentUserUid?.let { uid ->
                groupRepository.createGroup(name, description, uid)
            }
        }
    }

    fun joinGroup(inviteCode: String) {
        viewModelScope.launch {
            userRepository.currentUserUid?.let { uid ->
                groupRepository.joinGroup(inviteCode, uid)
            }
        }
    }
}
