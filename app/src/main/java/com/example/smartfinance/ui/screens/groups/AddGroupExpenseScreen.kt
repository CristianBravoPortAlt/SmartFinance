package com.example.smartfinance.ui.screens.groups

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.Button
import androidx.compose.material3.CenterAlignedTopAppBar
import androidx.compose.material3.Checkbox
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateMapOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.smartfinance.data.local.ExpenseSplitEntity
import com.example.smartfinance.data.local.GroupExpenseEntity
import com.example.smartfinance.data.local.UserEntity
import com.example.smartfinance.data.repository.GroupRepository
import com.example.smartfinance.data.repository.UserRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class AddGroupExpenseViewModel @Inject constructor(
    private val userRepository: UserRepository,
    private val groupRepository: GroupRepository
) : ViewModel() {
    private val _members = MutableStateFlow<List<UserEntity>>(emptyList())
    val members: StateFlow<List<UserEntity>> = _members

    fun loadMembers(groupId: String) {
        viewModelScope.launch {
            groupRepository.getGroupMembers(groupId).collectLatest { _members.value = it }
        }
    }

    fun addExpense(
        groupId: String,
        amount: Double,
        description: String,
        splitType: String,
        splitsMap: Map<String, Double>,
        onSuccess: () -> Unit
    ) {
        viewModelScope.launch {
            val currentUserId = userRepository.currentUserUid ?: return@launch
            
            val expense = GroupExpenseEntity(
                groupId = groupId,
                paidByUserId = currentUserId,
                amount = amount,
                description = description,
                splitType = splitType
            )
            
            val splits = mutableListOf<ExpenseSplitEntity>()
            when (splitType) {
                "EQUAL" -> {
                    val participantIds = splitsMap.keys
                    val share = amount / participantIds.size
                    participantIds.forEach { uid ->
                        splits.add(ExpenseSplitEntity("", uid, share))
                    }
                }
                "PERCENTAGE" -> {
                    splitsMap.forEach { (uid, percent) ->
                        splits.add(ExpenseSplitEntity("", uid, (percent / 100.0) * amount))
                    }
                }
                "EXACT" -> {
                    splitsMap.forEach { (uid, exactAmount) ->
                        splits.add(ExpenseSplitEntity("", uid, exactAmount))
                    }
                }
            }
            
            groupRepository.addExpense(expense, splits)
            onSuccess()
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AddGroupExpenseScreen(
    groupId: String,
    viewModel: AddGroupExpenseViewModel = hiltViewModel(),
    onNavigateBack: () -> Unit
) {
    val members by viewModel.members.collectAsState()
    
    var amount by remember { mutableStateOf("") }
    var description by remember { mutableStateOf("") }
    var splitType by remember { mutableStateOf("EQUAL") }
    
    val splitsMap = remember { mutableStateMapOf<String, Double>() }

    LaunchedEffect(groupId) {
        viewModel.loadMembers(groupId)
    }
    
    LaunchedEffect(members) {
        if (members.isNotEmpty() && splitsMap.isEmpty()) {
            members.forEach { splitsMap[it.id] = 1.0 }
        }
    }

    Scaffold(
        topBar = {
            CenterAlignedTopAppBar(
                title = { Text("Añadir Gasto de Grupo", fontWeight = FontWeight.Bold) },
                navigationIcon = {
                    IconButton(onClick = onNavigateBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Atrás")
                    }
                }
            )
        }
    ) { padding ->
        Column(
            modifier = Modifier
                .padding(padding)
                .padding(16.dp)
                .fillMaxSize()
        ) {
            OutlinedTextField(
                value = amount,
                onValueChange = { amount = it },
                label = { Text("Importe (€)") },
                modifier = Modifier.fillMaxWidth(),
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number)
            )
            Spacer(modifier = Modifier.height(8.dp))
            OutlinedTextField(
                value = description,
                onValueChange = { description = it },
                label = { Text("Descripción") },
                modifier = Modifier.fillMaxWidth()
            )
            Spacer(modifier = Modifier.height(16.dp))
            
            Text("Dividir por:", fontWeight = FontWeight.Bold)
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceEvenly) {
                FilterChip(selected = splitType == "EQUAL", onClick = { splitType = "EQUAL" }, label = { Text("Igual") })
                FilterChip(selected = splitType == "PERCENTAGE", onClick = { splitType = "PERCENTAGE" }, label = { Text("%") })
                FilterChip(selected = splitType == "EXACT", onClick = { splitType = "EXACT" }, label = { Text("Exacto") })
            }
            
            Spacer(modifier = Modifier.height(16.dp))
            Text("Participantes:", fontWeight = FontWeight.Bold)
            
            LazyColumn(modifier = Modifier.weight(1f)) {
                items(members) { member ->
                    ParticipantSplitItem(
                        member = member,
                        splitType = splitType,
                        value = splitsMap[member.id] ?: 0.0,
                        onValueChange = { newValue ->
                            if (splitType == "EQUAL") {
                                if (newValue > 0) splitsMap[member.id] = 1.0 else splitsMap.remove(member.id)
                            } else {
                                splitsMap[member.id] = newValue
                            }
                        }
                    )
                }
            }
            
            Button(
                onClick = {
                    val amtDouble = amount.toDoubleOrNull() ?: 0.0
                    if (amtDouble > 0 && description.isNotBlank() && splitsMap.isNotEmpty()) {
                        viewModel.addExpense(groupId, amtDouble, description, splitType, splitsMap) {
                            onNavigateBack()
                        }
                    }
                },
                modifier = Modifier.fillMaxWidth(),
                enabled = amount.isNotBlank() && description.isNotBlank()
            ) {
                Text("Guardar Gasto")
            }
        }
    }
}

@Composable
fun ParticipantSplitItem(
    member: UserEntity,
    splitType: String,
    value: Double,
    onValueChange: (Double) -> Unit
) {
    Row(
        modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        if (splitType == "EQUAL") {
            Checkbox(checked = value > 0, onCheckedChange = { onValueChange(if (it) 1.0 else 0.0) })
        }
        Text(member.name, modifier = Modifier.weight(1f))
        
        if (splitType != "EQUAL") {
            OutlinedTextField(
                value = if (value == 0.0) "" else value.toString(),
                onValueChange = { onValueChange(it.toDoubleOrNull() ?: 0.0) },
                modifier = Modifier.width(100.dp),
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                suffix = { if (splitType == "PERCENTAGE") Text("%") else Text("€") }
            )
        }
    }
}
