package com.example.smartfinance.ui.screens.groups

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.ExitToApp
import androidx.compose.material.icons.automirrored.filled.ReceiptLong
import androidx.compose.material.icons.filled.AccountBalance
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Group
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material.icons.filled.Payments
import androidx.compose.material.icons.filled.PersonAdd
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CenterAlignedTopAppBar
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Surface
import androidx.compose.material3.Tab
import androidx.compose.material3.TabRow
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalClipboardManager
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.smartfinance.data.local.ExpenseSplitEntity
import com.example.smartfinance.data.local.GroupEntity
import com.example.smartfinance.data.local.GroupExpenseEntity
import com.example.smartfinance.data.local.NotificationEntity
import com.example.smartfinance.data.local.SettlementEntity
import com.example.smartfinance.data.local.UserEntity
import com.example.smartfinance.data.repository.FriendRepository
import com.example.smartfinance.data.repository.GroupRepository
import com.example.smartfinance.data.repository.NotificationRepository
import com.example.smartfinance.data.repository.UserRepository
import com.example.smartfinance.ui.screens.friends.FriendItem
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import java.util.Locale
import javax.inject.Inject

data class UserBalance(
    val user: UserEntity,
    val balance: Double
)

@HiltViewModel
class GroupDetailViewModel @Inject constructor(
    private val userRepository: UserRepository,
    private val friendRepository: FriendRepository,
    private val groupRepository: GroupRepository,
    private val notificationRepository: NotificationRepository
) : ViewModel() {
    private val _group = MutableStateFlow<GroupEntity?>(null)
    val group: StateFlow<GroupEntity?> = _group

    private val _members = MutableStateFlow<List<UserEntity>>(emptyList())
    val members: StateFlow<List<UserEntity>> = _members

    private val _expenses = MutableStateFlow<List<GroupExpenseEntity>>(emptyList())
    val expenses: StateFlow<List<GroupExpenseEntity>> = _expenses

    private val _balances = MutableStateFlow<List<UserBalance>>(emptyList())
    val balances: StateFlow<List<UserBalance>> = _balances

    private val _settlements = MutableStateFlow<List<SettlementEntity>>(emptyList())
    val settlements: StateFlow<List<SettlementEntity>> = _settlements

    val currentUserUid = userRepository.currentUserUid

    val friends = friendRepository.getFriends().stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    fun loadGroup(groupId: String) {
        viewModelScope.launch {
            groupRepository.getGroupById(groupId).collectLatest { _group.value = it }
        }
        viewModelScope.launch {
            groupRepository.getGroupMembers(groupId).collectLatest { 
                _members.value = it
                calculateBalances(groupId, _expenses.value)
            }
        }
        viewModelScope.launch {
            groupRepository.getExpensesForGroup(groupId).collectLatest { 
                _expenses.value = it
                calculateBalances(groupId, it)
            }
        }
        viewModelScope.launch {
            groupRepository.getSettlementsForGroup(groupId).collectLatest {
                _settlements.value = it
                calculateBalances(groupId, _expenses.value)
            }
        }
    }

    fun leaveGroup(groupId: String, onComplete: () -> Unit) {
        viewModelScope.launch {
            val userId = userRepository.currentUserUid ?: ""
            groupRepository.leaveGroup(groupId, userId)
            onComplete()
        }
    }

    fun deleteExpense(expenseId: String) {
        viewModelScope.launch {
            groupRepository.deleteExpense(expenseId)
            group.value?.let { loadGroup(it.id) }
        }
    }

    fun inviteFriendToGroup(friend: UserEntity, group: GroupEntity) {
        viewModelScope.launch {
            val senderId = userRepository.currentUserUid ?: ""
            groupRepository.inviteUserToGroup(group.id, friend.id, senderId)
            
            val notification = NotificationEntity(
                userId = friend.id,
                senderId = senderId,
                title = "Invitación a grupo",
                message = "Has sido invitado al grupo ${group.name}",
                type = "group_invite"
            )
            notificationRepository.insertNotification(notification)
        }
    }

    fun settleUp(groupId: String, fromUserId: String, toUserId: String, amount: Double) {
        viewModelScope.launch {
            val settlement = SettlementEntity(
                groupId = groupId,
                fromUserId = fromUserId,
                toUserId = toUserId,
                amount = amount
            )
            groupRepository.addSettlement(settlement)
            groupRepository.getExpensesForGroup(groupId).first().let { calculateBalances(groupId, it) }
        }
    }

    fun deleteSettlement(settlementId: String) {
        viewModelScope.launch {
            groupRepository.deleteSettlement(settlementId)
            group.value?.let { loadGroup(it.id) }
        }
    }

    private suspend fun calculateBalances(groupId: String, expenses: List<GroupExpenseEntity>) {
        val membersList = members.value
        if (membersList.isEmpty()) return

        val balanceMap = membersList.associate { it.id to 0.0 }.toMutableMap()

        for (expense in expenses) {
            balanceMap[expense.paidByUserId] = (balanceMap[expense.paidByUserId] ?: 0.0) + expense.amount
            val splits: List<ExpenseSplitEntity> = groupRepository.getSplitsForExpense(expense.id)
            for (split in splits) {
                balanceMap[split.userId] = (balanceMap[split.userId] ?: 0.0) - split.amount
            }
        }

        val settlements = groupRepository.getSettlementsForGroup(groupId).first()
        settlements.forEach { settlement: SettlementEntity ->
            balanceMap[settlement.fromUserId] = (balanceMap[settlement.fromUserId] ?: 0.0) + settlement.amount
            balanceMap[settlement.toUserId] = (balanceMap[settlement.toUserId] ?: 0.0) - settlement.amount
        }

        _balances.value = membersList.map { UserBalance(it, balanceMap[it.id] ?: 0.0) }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun GroupDetailScreen(
    groupId: String,
    viewModel: GroupDetailViewModel = hiltViewModel(),
    onNavigateBack: () -> Unit,
    onNavigateToAddExpense: (String) -> Unit
) {
    val group by viewModel.group.collectAsState()
    val expenses by viewModel.expenses.collectAsState()
    val balances by viewModel.balances.collectAsState()
    val settlements by viewModel.settlements.collectAsState()
    val members by viewModel.members.collectAsState()
    val friends by viewModel.friends.collectAsState()
    
    var selectedTab by remember { mutableIntStateOf(0) }
    var showInviteDialog by remember { mutableStateOf(false) }
    var showLeaveDialog by remember { mutableStateOf(false) }
    var settlementToConfirm by remember { mutableStateOf<UserBalance?>(null) }

    val clipboardManager = LocalClipboardManager.current
    val snackbarHostState = remember { SnackbarHostState() }
    val scope = rememberCoroutineScope()

    LaunchedEffect(groupId) {
        viewModel.loadGroup(groupId)
    }

    Scaffold(
        snackbarHost = { SnackbarHost(snackbarHostState) },
        topBar = {
            CenterAlignedTopAppBar(
                title = { 
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Text(group?.name ?: "Detalle del Grupo", fontWeight = FontWeight.Bold)
                        group?.let {
                            Text(
                                text = "Código: ${it.inviteCode}",
                                fontSize = 10.sp,
                                color = MaterialTheme.colorScheme.primary,
                                modifier = Modifier.clickable {
                                    clipboardManager.setText(AnnotatedString(it.inviteCode))
                                    scope.launch {
                                        snackbarHostState.showSnackbar("Código '${it.inviteCode}' copiado")
                                    }
                                }
                            )
                        }
                    }
                },
                navigationIcon = {
                    IconButton(onClick = onNavigateBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Atrás")
                    }
                },
                actions = {
                    if (selectedTab == 3) {
                        IconButton(onClick = { showInviteDialog = true }) {
                            Icon(Icons.Default.PersonAdd, "Invitar")
                        }
                    }
                    var showMenu by remember { mutableStateOf(false) }
                    IconButton(onClick = { showMenu = true }) {
                        Icon(Icons.Default.MoreVert, "Más opciones")
                    }
                    DropdownMenu(
                        expanded = showMenu,
                        onDismissRequest = { showMenu = false }
                    ) {
                        DropdownMenuItem(
                            text = { Text("Salir del Grupo", color = MaterialTheme.colorScheme.error) },
                            onClick = {
                                showMenu = false
                                showLeaveDialog = true
                            },
                            leadingIcon = { Icon(Icons.AutoMirrored.Filled.ExitToApp, null, tint = MaterialTheme.colorScheme.error) }
                        )
                    }
                }
            )
        },
        floatingActionButton = {
            if (selectedTab == 0) {
                FloatingActionButton(
                    onClick = { onNavigateToAddExpense(groupId) },
                    containerColor = MaterialTheme.colorScheme.primary,
                    contentColor = Color.White
                ) {
                    Icon(Icons.Default.Add, contentDescription = "Añadir Gasto")
                }
            }
        }
    ) { padding ->
        Column(modifier = Modifier.padding(padding)) {
            TabRow(selectedTabIndex = selectedTab) {
                Tab(selected = selectedTab == 0, onClick = { selectedTab = 0 }, text = { Text("Gastos") }, icon = { Icon(Icons.AutoMirrored.Filled.ReceiptLong, null) })
                Tab(selected = selectedTab == 1, onClick = { selectedTab = 1 }, text = { Text("Saldos") }, icon = { Icon(Icons.Default.AccountBalance, null) })
                Tab(selected = selectedTab == 2, onClick = { selectedTab = 2 }, text = { Text("Pagos") }, icon = { Icon(Icons.Default.Payments, null) })
                Tab(selected = selectedTab == 3, onClick = { selectedTab = 3 }, text = { Text("Miembros") }, icon = { Icon(Icons.Default.Group, null) })
            }

            when (selectedTab) {
                0 -> ExpenseList(expenses, viewModel.currentUserUid, onDelete = { viewModel.deleteExpense(it.id) })
                1 -> BalanceList(balances, viewModel.currentUserUid, onSettleUp = { balance ->
                    settlementToConfirm = balance
                })
                2 -> SettlementList(settlements, members, onDelete = { viewModel.deleteSettlement(it.id) })
                3 -> MemberList(members)
            }
        }
    }

    if (settlementToConfirm != null) {
        val balance = settlementToConfirm!!
        val creditor = balances.find { it.balance > 0 }
        if (creditor != null) {
            var amountToPay by remember { mutableStateOf(String.format(Locale.getDefault(), "%.2f", Math.abs(balance.balance))) }
            AlertDialog(
                onDismissRequest = { settlementToConfirm = null },
                title = { Text("Liquidar Deuda") },
                text = {
                    Column {
                        Text("Vas a pagar a ${creditor.user.name}")
                        Spacer(modifier = Modifier.height(16.dp))
                        OutlinedTextField(
                            value = amountToPay,
                            onValueChange = { amountToPay = it },
                            label = { Text("Importe a pagar (€)") },
                            modifier = Modifier.fillMaxWidth(),
                            keyboardOptions = androidx.compose.foundation.text.KeyboardOptions(keyboardType = androidx.compose.ui.text.input.KeyboardType.Number)
                        )
                    }
                },
                confirmButton = {
                    Button(onClick = {
                        val amtStr = amountToPay.replace(",", ".")
                        val amt = amtStr.toDoubleOrNull() ?: 0.0
                        if (amt > 0) {
                            viewModel.settleUp(groupId, balance.user.id, creditor.user.id, amt)
                            settlementToConfirm = null
                        }
                    }) { Text("Confirmar Pago") }
                },
                dismissButton = { TextButton(onClick = { settlementToConfirm = null }) { Text("Cancelar") } }
            )
        }
    }

    if (showInviteDialog) {
        InviteFriendDialog(
            friends = friends.filter { f -> members.none { it.id == f.id } },
            onDismiss = { showInviteDialog = false },
            onInvite = { friend ->
                group?.let { viewModel.inviteFriendToGroup(friend, it) }
                showInviteDialog = false
            }
        )
    }

    if (showLeaveDialog) {
        AlertDialog(
            onDismissRequest = { showLeaveDialog = false },
            title = { Text("Salir del Grupo") },
            text = { Text("¿Estás seguro de que deseas salir de este grupo? Ya no podrás ver los gastos ni los saldos.") },
            confirmButton = {
                Button(
                    onClick = {
                        viewModel.leaveGroup(groupId) {
                            showLeaveDialog = false
                            onNavigateBack()
                        }
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.error)
                ) {
                    Text("Salir")
                }
            },
            dismissButton = {
                TextButton(onClick = { showLeaveDialog = false }) { Text("Cancelar") }
            }
        )
    }
}

@Composable
fun InviteFriendDialog(friends: List<UserEntity>, onDismiss: () -> Unit, onInvite: (UserEntity) -> Unit) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Invitar Amigo") },
        text = {
            if (friends.isEmpty()) {
                Text("No tienes más amigos para invitar.")
            } else {
                LazyColumn(modifier = Modifier.heightIn(max = 300.dp)) {
                    items(friends) { friend ->
                        FriendItem(friend, onClick = { onInvite(friend) })
                    }
                }
            }
        },
        confirmButton = {},
        dismissButton = { TextButton(onClick = onDismiss) { Text("Cerrar") } }
    )
}

@Composable
fun ExpenseList(expenses: List<GroupExpenseEntity>, currentUserId: String?, onDelete: (GroupExpenseEntity) -> Unit) {
    if (expenses.isEmpty()) {
        Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
            Text("No hay gastos registrados", color = MaterialTheme.colorScheme.outline)
        }
    } else {
        LazyColumn(contentPadding = PaddingValues(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
            items(expenses) { expense ->
                ExpenseItem(expense, isOwner = expense.paidByUserId == currentUserId, onDelete = { onDelete(expense) })
            }
        }
    }
}

@Composable
fun ExpenseItem(expense: GroupExpenseEntity, isOwner: Boolean, onDelete: () -> Unit) {
    Surface(
        shape = RoundedCornerShape(16.dp),
        color = MaterialTheme.colorScheme.surface,
        tonalElevation = 2.dp,
        modifier = Modifier.fillMaxWidth()
    ) {
        Row(modifier = Modifier.padding(16.dp), verticalAlignment = Alignment.CenterVertically) {
            Box(modifier = Modifier.size(40.dp).clip(CircleShape).background(MaterialTheme.colorScheme.primaryContainer), contentAlignment = Alignment.Center) {
                Text(expense.description.take(1).uppercase(), color = MaterialTheme.colorScheme.onPrimaryContainer)
            }
            Spacer(modifier = Modifier.width(16.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text(expense.description, fontWeight = FontWeight.Bold)
                Text("Pagado por ${expense.paidByUserId.take(5)}...", fontSize = 12.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
            Text("${expense.amount}€", fontWeight = FontWeight.ExtraBold, color = MaterialTheme.colorScheme.primary)
            if (isOwner) {
                IconButton(onClick = onDelete) {
                    Icon(Icons.Default.Delete, contentDescription = "Eliminar", tint = MaterialTheme.colorScheme.error, modifier = Modifier.size(20.dp))
                }
            }
        }
    }
}

@Composable
fun BalanceList(balances: List<UserBalance>, currentUserId: String?, onSettleUp: (UserBalance) -> Unit) {
    LazyColumn(contentPadding = PaddingValues(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
        items(balances) { balance ->
            BalanceItem(balance, isMe = balance.user.id == currentUserId, onSettleUp = { onSettleUp(balance) })
        }
    }
}

@Composable
fun BalanceItem(userBalance: UserBalance, isMe: Boolean, onSettleUp: () -> Unit) {
    Surface(
        shape = RoundedCornerShape(16.dp),
        color = MaterialTheme.colorScheme.surface,
        tonalElevation = 2.dp,
        modifier = Modifier.fillMaxWidth()
    ) {
        Row(modifier = Modifier.padding(16.dp), verticalAlignment = Alignment.CenterVertically) {
            Box(modifier = Modifier.size(40.dp).clip(CircleShape).background(MaterialTheme.colorScheme.secondaryContainer), contentAlignment = Alignment.Center) {
                Text(userBalance.user.name.take(1).uppercase(), color = MaterialTheme.colorScheme.onSecondaryContainer)
            }
            Spacer(modifier = Modifier.width(16.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text(userBalance.user.name, fontWeight = FontWeight.Bold)
                val statusText = if (userBalance.balance > 0) "Se le deben" 
                                else if (userBalance.balance < 0) "Debe al grupo"
                                else "Al día"
                Text(statusText, fontSize = 12.sp, color = if (userBalance.balance == 0.0) MaterialTheme.colorScheme.outline else MaterialTheme.colorScheme.onSurfaceVariant)
            }
            Column(horizontalAlignment = Alignment.End) {
                val amountColor = if (userBalance.balance > 0) Color(0xFF4CAF50) 
                                 else if (userBalance.balance < 0) Color(0xFFF44336)
                                 else MaterialTheme.colorScheme.outline
                val balanceText = String.format(Locale.getDefault(), "%.2f", Math.abs(userBalance.balance))
                Text("${balanceText}€", fontWeight = FontWeight.ExtraBold, color = amountColor)
                if (userBalance.balance < 0 && isMe) {
                    TextButton(onClick = onSettleUp, contentPadding = PaddingValues(0.dp)) {
                        Text("Registrar Pago", fontSize = 10.sp)
                    }
                }
            }
        }
    }
}

@Composable
fun SettlementList(settlements: List<SettlementEntity>, members: List<UserEntity>, onDelete: (SettlementEntity) -> Unit) {
    if (settlements.isEmpty()) {
        Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
            Text("No hay pagos registrados", color = MaterialTheme.colorScheme.outline)
        }
    } else {
        LazyColumn(contentPadding = PaddingValues(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
            items(settlements) { settlement ->
                val fromUser = members.find { it.id == settlement.fromUserId }?.name ?: "Usuario"
                val toUser = members.find { it.id == settlement.toUserId }?.name ?: "Usuario"
                SettlementItem(settlement, fromUser, toUser, onDelete = { onDelete(settlement) })
            }
        }
    }
}

@Composable
fun SettlementItem(settlement: SettlementEntity, fromUser: String, toUser: String, onDelete: () -> Unit) {
    Surface(
        shape = RoundedCornerShape(16.dp),
        color = MaterialTheme.colorScheme.surface,
        tonalElevation = 2.dp,
        modifier = Modifier.fillMaxWidth()
    ) {
        Row(modifier = Modifier.padding(16.dp), verticalAlignment = Alignment.CenterVertically) {
            Box(modifier = Modifier.size(40.dp).clip(CircleShape).background(Color(0xFF4CAF50).copy(alpha = 0.1f)), contentAlignment = Alignment.Center) {
                Icon(Icons.Default.Payments, contentDescription = null, tint = Color(0xFF4CAF50))
            }
            Spacer(modifier = Modifier.width(16.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text("$fromUser pagó a $toUser", fontWeight = FontWeight.Bold, fontSize = 14.sp)
                Text(java.text.SimpleDateFormat("dd/MM/yyyy", Locale.getDefault()).format(settlement.date), fontSize = 11.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
            Text("${settlement.amount}€", fontWeight = FontWeight.ExtraBold, color = Color(0xFF4CAF50))
            IconButton(onClick = onDelete) {
                Icon(Icons.Default.Delete, contentDescription = "Eliminar", tint = MaterialTheme.colorScheme.error, modifier = Modifier.size(20.dp))
            }
        }
    }
}

@Composable
fun MemberList(members: List<UserEntity>) {
    LazyColumn(contentPadding = PaddingValues(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
        items(members) { member ->
            FriendItem(member)
        }
    }
}
