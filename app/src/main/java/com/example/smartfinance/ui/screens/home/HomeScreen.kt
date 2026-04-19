package com.example.smartfinance.ui.screens.home

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.smartfinance.data.local.TransactionType
import com.example.smartfinance.ui.screens.transaction.TransactionViewModel
import com.example.smartfinance.ui.components.getCategoryIcon
import java.text.SimpleDateFormat
import java.util.*

@Composable
fun HomeScreen(
    homeViewModel: HomeViewModel,
    transactionViewModel: TransactionViewModel,
    onNavigateToProfile: () -> Unit,
    onNavigateToAddTransaction: () -> Unit,
    onNavigateToEditTransaction: (Int) -> Unit,
    onNavigateToStats: () -> Unit,
    onNavigateToGoals: () -> Unit
) {
    val state = homeViewModel.state
    val transactions by transactionViewModel.transactions.collectAsState()
    val balance by transactionViewModel.balance.collectAsState()
    val categories by transactionViewModel.categories.collectAsState()
    val totalExpense by transactionViewModel.totalExpense.collectAsState()
    val totalIncome by transactionViewModel.totalIncome.collectAsState()

    LaunchedEffect(Unit) {
        homeViewModel.loadUserData()
    }

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
            when (state) {
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
                                userName = state.user.name,
                                onNavigateToGoals = onNavigateToGoals,
                                onNavigateToStats = onNavigateToStats,
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

                        // Budget Warning
                        val budget = state.user.monthlyBudget
                        val expenseVal = totalExpense ?: 0.0
                        if (budget > 0 && expenseVal / budget >= 0.8) {
                            item {
                                BudgetWarning(expense = expenseVal, budget = budget)
                            }
                        }

                        // Transactions Title
                        item {
                            Text(
                                text = "Movimientos recientes",
                                fontWeight = FontWeight.Bold,
                                fontSize = 18.sp,
                                color = MaterialTheme.colorScheme.onBackground,
                                modifier = Modifier.padding(horizontal = 24.dp, vertical = 16.dp)
                            )
                        }

                        // Transactions List
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
                                    onClick = { onNavigateToEditTransaction(transaction.id) }
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun HomeHeader(
    userName: String,
    onNavigateToGoals: () -> Unit,
    onNavigateToStats: () -> Unit,
    onNavigateToProfile: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 24.dp, vertical = 24.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Column {
            Text(
                text = "Hola,",
                fontSize = 16.sp,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                letterSpacing = 0.5.sp
            )
            Text(
                text = userName,
                fontSize = 28.sp,
                fontWeight = FontWeight.ExtraBold,
                color = MaterialTheme.colorScheme.onSurface,
                lineHeight = 32.sp
            )
        }
        Row(verticalAlignment = Alignment.CenterVertically) {
            Surface(
                onClick = onNavigateToGoals,
                shape = CircleShape,
                color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f),
                modifier = Modifier.size(44.dp)
            ) {
                Box(contentAlignment = Alignment.Center) {
                    Icon(Icons.Default.Star, contentDescription = "Objetivos", tint = Color(0xFFFFB703), modifier = Modifier.size(24.dp))
                }
            }
            Spacer(modifier = Modifier.width(12.dp))
            Surface(
                onClick = onNavigateToProfile,
                shape = CircleShape,
                color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f),
                modifier = Modifier.size(44.dp)
            ) {
                Box(contentAlignment = Alignment.Center) {
                    Icon(Icons.Default.Person, contentDescription = "Perfil", tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(24.dp))
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
                    text = String.format("%.2f€", balance),
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
            Text(String.format("%.2f€", amount), fontSize = 16.sp, fontWeight = FontWeight.Bold, color = Color.White)
        }
    }
}

@Composable
fun BudgetWarning(expense: Double, budget: Double) {
    val percentage = expense / budget
    val isOver = percentage >= 1.0
    
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 24.dp, vertical = 16.dp),
        colors = CardDefaults.cardColors(
            containerColor = if (isOver) Color(0xFFFFEBEE) else Color(0xFFFFF8E1)
        ),
        shape = RoundedCornerShape(16.dp)
    ) {
        Row(
            modifier = Modifier.padding(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(
                if (isOver) Icons.Default.Error else Icons.Default.Warning,
                contentDescription = null,
                tint = if (isOver) Color(0xFFD32F2F) else Color(0xFFFFA000)
            )
            Spacer(modifier = Modifier.width(16.dp))
            Column {
                Text(
                    if (isOver) "¡Presupuesto excedido!" else "Límite de presupuesto cerca",
                    fontWeight = FontWeight.Bold,
                    color = if (isOver) Color(0xFFD32F2F) else Color(0xFF795548),
                    fontSize = 14.sp
                )
                LinearProgressIndicator(
                    progress = { percentage.coerceAtMost(1.0).toFloat() },
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 8.dp)
                        .height(6.dp)
                        .clip(CircleShape),
                    color = if (isOver) Color(0xFFD32F2F) else Color(0xFFFFA000),
                    trackColor = Color.White.copy(alpha = 0.5f)
                )
                Text(
                    "Has gastado ${String.format("%.2f", expense)}€ de tu límite de ${String.format("%.2f", budget)}€",
                    fontSize = 12.sp,
                    color = if (isOver) Color(0xFFD32F2F).copy(alpha = 0.8f) else Color(0xFF795548).copy(alpha = 0.8f)
                )
            }
        }
    }
}

@Composable
fun TransactionItem(
    transaction: com.example.smartfinance.data.local.TransactionEntity,
    categoryName: String,
    iconName: String,
    colorHex: String,
    onClick: () -> Unit
) {
    val sdf = SimpleDateFormat("dd MMM", Locale.getDefault())
    val dateStr = sdf.format(Date(transaction.dateMillis))
    val categoryColor = Color(android.graphics.Color.parseColor(colorHex))

    Surface(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 24.dp, vertical = 6.dp)
            .clickable { onClick() },
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
                text = "${if (isIncome) "+" else "-"} ${String.format("%.2f€", transaction.amount)}",
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
            Icons.Default.ReceiptLong,
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
