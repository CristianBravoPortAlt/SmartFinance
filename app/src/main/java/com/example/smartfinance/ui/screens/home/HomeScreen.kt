package com.example.smartfinance.ui.screens.home

import android.graphics.Color.parseColor
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ReceiptLong
import androidx.compose.material.icons.filled.AccountBalanceWallet
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.ArrowDownward
import androidx.compose.material.icons.filled.ArrowUpward
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Menu
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.ReceiptLong
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ElevatedCard
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage
import com.example.smartfinance.data.local.TransactionEntity
import com.example.smartfinance.data.local.TransactionType
import com.example.smartfinance.ui.components.GoalItemCompact
import com.example.smartfinance.ui.components.getCategoryIcon
import com.example.smartfinance.ui.screens.goals.GoalViewModel
import com.example.smartfinance.ui.screens.transaction.TransactionViewModel
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import androidx.core.graphics.toColorInt

@Composable
fun HomeScreen(
    homeViewModel: HomeViewModel,
    transactionViewModel: TransactionViewModel,
    goalViewModel: GoalViewModel,
    onMenuClick: () -> Unit,
    onNavigateToProfile: () -> Unit,
    onNavigateToAddTransaction: () -> Unit,
    onNavigateToEditTransaction: (String) -> Unit,
    onNavigateToStats: () -> Unit,
    onNavigateToGoals: () -> Unit,
    onNavigateToGoalDetail: (String) -> Unit,
    onNavigateToMonthlySummary: () -> Unit
) {
    val state by homeViewModel.state.collectAsState()
    val transactions by transactionViewModel.transactions.collectAsState()
    val balance by transactionViewModel.balance.collectAsState()
    val categories by transactionViewModel.categories.collectAsState()
    val totalExpense by transactionViewModel.totalExpense.collectAsState()
    val totalIncome by transactionViewModel.totalIncome.collectAsState()
    val goals by goalViewModel.goals.collectAsState()

    val currentState = state
    var transactionToDelete by remember { mutableStateOf<TransactionEntity?>(null) }

    Scaffold(
        floatingActionButton = {
            FloatingActionButton(
                onClick = onNavigateToAddTransaction,
                containerColor = MaterialTheme.colorScheme.primary,
                contentColor = MaterialTheme.colorScheme.onPrimary,
                shape = CircleShape
            ) {
                Icon(Icons.Default.Add, contentDescription = "Añadir Movimiento", modifier = Modifier.size(28.dp))
            }
        }
    ) { paddingValues ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
                .background(MaterialTheme.colorScheme.background)
        ) {
            when (currentState) {
                is HomeState.Loading -> Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    CircularProgressIndicator(color = MaterialTheme.colorScheme.primary)
                }
                is HomeState.NoData -> Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    Text("No se pudieron cargar los datos.", color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
                is HomeState.Success -> {
                    LazyColumn(
                        modifier = Modifier.fillMaxSize(),
                        contentPadding = PaddingValues(bottom = 80.dp)
                    ) {
                        // Header
                        item {
                            HomeHeader(
                                userName = currentState.user.name,
                                profilePictureUri = currentState.user.profilePictureUri,
                                onMenuClick = onMenuClick,
                                onNavigateToProfile = onNavigateToProfile
                            )
                        }

                        // Balance Card
                        item {
                            BalanceCard(
                                balance = balance,
                                income = totalIncome ?: 0.0,
                                expense = totalExpense ?: 0.0
                            )
                        }

                        // Alerta cuando sobrepasa presupuesto
                        val budget = currentState.user.monthlyBudget
                        val expenseVal = totalExpense ?: 0.0
                        val currentMonth = SimpleDateFormat("yyyy-MM", Locale.getDefault()).format(Date())
                        
                        if (budget > 0 && 
                            expenseVal / budget >= 0.8 && 
                            currentState.user.budgetWarningDismissedMonth != currentMonth) {
                            item {
                                BudgetWarning(
                                    expense = expenseVal, 
                                    budget = budget,
                                    onDismiss = { homeViewModel.dismissBudgetWarning() }
                                )
                            }
                        }

                        if (goals.isNotEmpty()) {
                            item {
                                Column(modifier = Modifier.padding(top = 32.dp)) {
                                    Row(
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .padding(horizontal = 24.dp),
                                        horizontalArrangement = Arrangement.SpaceBetween,
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Text(
                                            text = "Mis Objetivos",
                                            fontWeight = FontWeight.Bold,
                                            fontSize = 18.sp,
                                            color = MaterialTheme.colorScheme.onBackground
                                        )
                                        TextButton(onClick = onNavigateToGoals) {
                                            Text("Ver todos", fontWeight = FontWeight.Bold)
                                        }
                                    }
                                    LazyRow(
                                        contentPadding = PaddingValues(horizontal = 24.dp),
                                        horizontalArrangement = Arrangement.spacedBy(16.dp),
                                        modifier = Modifier.padding(top = 8.dp)
                                    ) {
                                        items(goals) { goal ->
                                            GoalItemCompact(
                                                goal = goal,
                                                onClick = { onNavigateToGoalDetail(goal.id) }
                                            )
                                        }
                                    }
                                }
                            }
                        }

                        item {
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(horizontal = 24.dp, vertical = 24.dp),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text(
                                    text = "Movimientos recientes",
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 18.sp,
                                    color = MaterialTheme.colorScheme.onBackground
                                )
                                TextButton(onClick = onNavigateToMonthlySummary) {
                                    Text("Ver todo", fontWeight = FontWeight.Bold)
                                }
                            }
                        }

                        // Lista de transacciones
                        if (transactions.isEmpty()) {
                            item {
                                EmptyTransactionsState()
                            }
                        } else {
                            items(transactions.take(10)) { transaction ->
                                val category = categories.find { it.id == transaction.categoryId }
                                TransactionItem(
                                    transaction = transaction,
                                    categoryName = category?.name ?: "Sin categoría",
                                    iconName = category?.iconResName ?: "category",
                                    colorHex = category?.colorHex ?: "#9E9E9E",
                                    onClick = { onNavigateToEditTransaction(transaction.id) },
                                    onLongClick = { transactionToDelete = transaction }
                                )
                            }
                        }
                    }
                }
            }

            if (transactionToDelete != null) {
                AlertDialog(
                    onDismissRequest = { transactionToDelete = null },
                    title = { Text("Borrar Movimiento") },
                    text = { Text("¿Estás seguro de que quieres borrar '${transactionToDelete?.title}' por ${transactionToDelete?.amount}€?") },
                    confirmButton = {
                        TextButton(
                            onClick = {
                                transactionToDelete?.let { transactionViewModel.deleteTransaction(it) }
                                transactionToDelete = null
                            }
                        ) {
                            Text("Borrar", color = MaterialTheme.colorScheme.error)
                        }
                    },
                    dismissButton = {
                        TextButton(onClick = { transactionToDelete = null }) {
                            Text("Cancelar")
                        }
                    }
                )
            }
        }
    }
}

@Composable
fun HomeHeader(
    userName: String,
    profilePictureUri: String?,
    onMenuClick: () -> Unit,
    onNavigateToProfile: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 24.dp, vertical = 24.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            IconButton(
                onClick = onMenuClick,
                modifier = Modifier
                    .size(44.dp)
                    .clip(CircleShape)
                    .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f))
            ) {
                Icon(Icons.Default.Menu, contentDescription = "Menú", tint = MaterialTheme.colorScheme.primary)
            }
            Spacer(modifier = Modifier.width(16.dp))
            Column {
                Text(
                    text = "Hola,",
                    fontSize = 16.sp,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    letterSpacing = 0.5.sp
                )
                Text(
                    text = userName,
                    fontSize = 24.sp,
                    fontWeight = FontWeight.ExtraBold,
                    color = MaterialTheme.colorScheme.onSurface,
                    lineHeight = 28.sp
                )
            }
        }
        Surface(
            onClick = onNavigateToProfile,
            shape = CircleShape,
            color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f),
            modifier = Modifier.size(44.dp)
        ) {
            Box(contentAlignment = Alignment.Center) {
                if (profilePictureUri != null) {
                    AsyncImage(
                        model = profilePictureUri,
                        contentDescription = "Perfil",
                        modifier = Modifier.fillMaxSize(),
                        contentScale = ContentScale.Crop
                    )
                } else {
                    Icon(
                        Icons.Default.Person,
                        contentDescription = "Perfil",
                        tint = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.size(24.dp)
                    )
                }
            }
        }
    }
}

@Composable
fun BalanceCard(balance: Double, income: Double, expense: Double) {
    ElevatedCard(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 24.dp),
        shape = RoundedCornerShape(28.dp),
        elevation = CardDefaults.elevatedCardElevation(defaultElevation = 12.dp)
    ) {
        Box(
            modifier = Modifier
                .background(
                    Brush.linearGradient(
                        colors = listOf(
                            MaterialTheme.colorScheme.primary,
                            MaterialTheme.colorScheme.tertiary
                        )
                    )
                )
                .padding(24.dp)
        ) {
            Column {
                Text(
                    text = "Balance Total",
                    fontSize = 14.sp,
                    color = MaterialTheme.colorScheme.onPrimary.copy(alpha = 0.7f),
                    fontWeight = FontWeight.Medium
                )
                Text(
                    text = String.format(Locale.getDefault(), "%.2f€", balance),
                    fontSize = 42.sp,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onPrimary,
                    letterSpacing = (-1).sp
                )
                
                Spacer(modifier = Modifier.height(24.dp))
                
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    BalanceSummaryItem(
                        label = "Ingresos",
                        amount = income,
                        icon = Icons.Default.ArrowUpward,
                        iconColor = Color(0xFF2ECC71)
                    )
                    BalanceSummaryItem(
                        label = "Gastos",
                        amount = expense,
                        icon = Icons.Default.ArrowDownward,
                        iconColor = Color(0xFFFF5252)
                    )
                }
            }
        }
    }
}

@Composable
fun BalanceSummaryItem(label: String, amount: Double, icon: androidx.compose.ui.graphics.vector.ImageVector, iconColor: Color) {
    Row(verticalAlignment = Alignment.CenterVertically) {
        Box(
            modifier = Modifier
                .size(32.dp)
                .clip(CircleShape)
                .background(Color.White.copy(alpha = 0.2f)),
            contentAlignment = Alignment.Center
        ) {
            Icon(icon, contentDescription = null, tint = iconColor, modifier = Modifier.size(18.dp))
        }
        Spacer(modifier = Modifier.width(12.dp))
        Column {
            Text(label, fontSize = 12.sp, color = Color.White.copy(alpha = 0.7f))
            Text(String.format(Locale.getDefault(), "%.2f€", amount), fontSize = 16.sp, fontWeight = FontWeight.Bold, color = Color.White)
        }
    }
}

@Composable
fun BudgetWarning(expense: Double, budget: Double, onDismiss: () -> Unit) {
    val percentage = expense / budget
    val isOver = percentage >= 1.0
    
    Surface(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 24.dp, vertical = 8.dp),
        color = MaterialTheme.colorScheme.surface,
        shape = RoundedCornerShape(20.dp),
        tonalElevation = 2.dp
    ) {
        Row(
            modifier = Modifier.padding(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                modifier = Modifier
                    .size(40.dp)
                    .clip(CircleShape)
                    .background(
                        if (isOver) MaterialTheme.colorScheme.errorContainer 
                        else MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.5f)
                    ),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    if (isOver) Icons.Default.Warning else Icons.Default.AccountBalanceWallet,
                    contentDescription = null,
                    tint = if (isOver) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.primary,
                    modifier = Modifier.size(20.dp)
                )
            }
            Spacer(modifier = Modifier.width(16.dp))
            Column(modifier = Modifier.weight(1f)) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = if (isOver) "¡Presupuesto excedido!" else "Progreso del presupuesto",
                        fontWeight = FontWeight.Bold,
                        fontSize = 14.sp,
                        color = if (isOver) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.onSurface
                    )
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text(
                            text = "${(percentage * 100).toInt()}%",
                            fontSize = 12.sp,
                            fontWeight = FontWeight.SemiBold,
                            color = if (isOver) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.primary
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        IconButton(onClick = onDismiss, modifier = Modifier.size(20.dp)) {
                            Icon(
                                Icons.Default.Close, 
                                contentDescription = "Cerrar", 
                                tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.5f),
                                modifier = Modifier.size(16.dp)
                            )
                        }
                    }
                }
                Spacer(modifier = Modifier.height(8.dp))
                LinearProgressIndicator(
                    progress = { percentage.coerceAtMost(1.0).toFloat() },
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(6.dp)
                        .clip(CircleShape),
                    color = if (isOver) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.primary,
                    trackColor = MaterialTheme.colorScheme.surfaceVariant
                )
            }
        }
    }
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
fun TransactionItem(
    transaction: TransactionEntity,
    categoryName: String,
    iconName: String,
    colorHex: String,
    onClick: () -> Unit,
    onLongClick: () -> Unit
) {
    val sdf = SimpleDateFormat("dd MMM", Locale.getDefault())
    val dateStr = sdf.format(Date(transaction.dateMillis))
    val categoryColor = Color(colorHex.toColorInt())

    Surface(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 24.dp, vertical = 6.dp)
            .clip(RoundedCornerShape(20.dp))
            .combinedClickable(
                onClick = onClick,
                onLongClick = onLongClick
            ),
        shape = RoundedCornerShape(20.dp),
        color = MaterialTheme.colorScheme.surface,
        tonalElevation = 1.dp
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                modifier = Modifier
                    .size(48.dp)
                    .clip(CircleShape)
                    .background(categoryColor.copy(alpha = 0.15f)),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = getCategoryIcon(iconName),
                    contentDescription = null,
                    tint = categoryColor,
                    modifier = Modifier.size(24.dp)
                )
            }
            
            Spacer(modifier = Modifier.width(16.dp))
            
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = transaction.title,
                    fontWeight = FontWeight.Bold,
                    fontSize = 16.sp,
                    color = MaterialTheme.colorScheme.onSurface,
                    maxLines = 1
                )
                Text(
                    text = "$categoryName • $dateStr",
                    fontSize = 13.sp,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
            
            val isIncome = transaction.type == TransactionType.INCOME
            Text(
                text = "${if (isIncome) "+" else "-"} ${String.format(Locale.getDefault(), "%.2f€", transaction.amount)}",
                fontWeight = FontWeight.ExtraBold,
                fontSize = 16.sp,
                color = if (isIncome) Color(0xFF2ECC71) else Color(0xFFFF5252)
            )
        }
    }
}

@Composable
fun EmptyTransactionsState() {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(48.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Icon(
            Icons.AutoMirrored.Filled.ReceiptLong,
            contentDescription = null,
            modifier = Modifier.size(64.dp),
            tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.3f)
        )
        Spacer(modifier = Modifier.height(16.dp))
        Text(
            "Sin movimientos aún",
            color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.6f),
            fontWeight = FontWeight.Medium
        )
    }
}
