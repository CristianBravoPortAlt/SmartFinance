package com.example.smartfinance.ui.screens.home

import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.List
import androidx.compose.material.icons.filled.Star
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.smartfinance.data.local.TransactionType
import com.example.smartfinance.ui.screens.transaction.TransactionViewModel
import androidx.compose.ui.graphics.Color

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

    LaunchedEffect(Unit) {
        homeViewModel.loadUserData(1)
    }

    Scaffold(
        floatingActionButton = {
            FloatingActionButton(
                onClick = onNavigateToAddTransaction,
                containerColor = MaterialTheme.colorScheme.primary
            ) {
                Icon(Icons.Default.Add, contentDescription = "Añadir Movimiento")
            }
        }
    ) { paddingValues ->
        Box(modifier = Modifier.fillMaxSize().padding(paddingValues), contentAlignment = Alignment.Center) {
            when (state) {
                is HomeState.Loading -> CircularProgressIndicator(color = MaterialTheme.colorScheme.primary)
                is HomeState.NoData -> Text("Sin datos.")
                is HomeState.Success -> {
                    Column(
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(horizontal = 24.dp, vertical = 32.dp)
                    ) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Column {
                                Text(
                                    text = "Hola,",
                                    fontSize = 16.sp,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                                Text(
                                    text = state.user.name,
                                    fontSize = 24.sp,
                                    fontWeight = FontWeight.Black
                                )
                            }
                            Row {
                                IconButton(onClick = onNavigateToGoals) {
                                    Icon(Icons.Default.Star, contentDescription = "Objetivos", tint = MaterialTheme.colorScheme.primary)
                                }
                                IconButton(onClick = onNavigateToStats) {
                                    Icon(Icons.Default.List, contentDescription = "Estadísticas", tint = MaterialTheme.colorScheme.onBackground)
                                }
                                IconButton(onClick = onNavigateToProfile) {
                                    Icon(Icons.Default.Person, contentDescription = "Perfil", tint = MaterialTheme.colorScheme.onBackground)
                                }
                            }
                        }

                        Spacer(modifier = Modifier.height(32.dp))

                        val expenseVal = totalExpense ?: 0.0
                        val budget = state.user.monthlyBudget
                        val budgetPercentage = if (budget > 0) expenseVal / budget else 0.0

                        if (budgetPercentage >= 0.8) {
                            Card(
                                modifier = Modifier.fillMaxWidth().padding(bottom = 16.dp),
                                colors = CardDefaults.cardColors(
                                    containerColor = if (budgetPercentage >= 1.0) MaterialTheme.colorScheme.errorContainer else MaterialTheme.colorScheme.errorContainer.copy(alpha=0.5f)
                                ),
                                shape = RoundedCornerShape(12.dp)
                            ) {
                                Row(
                                    modifier = Modifier.padding(16.dp),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Icon(Icons.Default.Warning, contentDescription = null, tint = MaterialTheme.colorScheme.error)
                                    Spacer(modifier = Modifier.width(16.dp))
                                    Column {
                                        Text(
                                            if (budgetPercentage >= 1.0) "¡Has superado tu presupuesto!" else "Atención: Te acercas al límite de gastos",
                                            fontWeight = FontWeight.Bold,
                                            color = MaterialTheme.colorScheme.onErrorContainer
                                        )
                                        Text(
                                            "Gastado: ${expenseVal}€ de ${budget}€",
                                            fontSize = 14.sp,
                                            color = MaterialTheme.colorScheme.onErrorContainer
                                        )
                                    }
                                }
                            }
                        }

                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .border(1.dp, MaterialTheme.colorScheme.outline, RoundedCornerShape(12.dp))
                                .padding(24.dp)
                        ) {
                            Column {
                                Text(
                                    text = "Balance disponible",
                                    fontSize = 14.sp,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                                Spacer(modifier = Modifier.height(8.dp))
                                Text(
                                    text = "${balance}€",
                                    fontSize = 40.sp,
                                    fontWeight = FontWeight.Light,
                                    color = MaterialTheme.colorScheme.onBackground
                                )
                            }
                        }

                        Spacer(modifier = Modifier.height(40.dp))

                        Text(
                            text = "Movimientos",
                            fontWeight = FontWeight.Bold,
                            fontSize = 18.sp,
                            color = MaterialTheme.colorScheme.onBackground
                        )
                        Spacer(modifier = Modifier.height(16.dp))

                        if (transactions.isEmpty()) {
                            Text(
                                "No hay movimientos registrados.",
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                modifier = Modifier.padding(top = 16.dp)
                            )
                        } else {
                            LazyColumn(
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                items(transactions) { transaction ->
                                    val category = categories.find { it.id == transaction.categoryId }
                                    Row(
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .clickable { onNavigateToEditTransaction(transaction.id) }
                                            .padding(vertical = 12.dp, horizontal = 4.dp),
                                        horizontalArrangement = Arrangement.SpaceBetween,
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Column {
                                            Text(transaction.title, fontWeight = FontWeight.SemiBold, fontSize = 16.sp)
                                            Text(category?.name ?: "Sin categoría", color = MaterialTheme.colorScheme.onSurfaceVariant, fontSize = 12.sp)
                                        }
                                        val amountText = if (transaction.type == TransactionType.INCOME) "+ ${transaction.amount}€" else "- ${transaction.amount}€"
                                        val color = if (transaction.type == TransactionType.INCOME) Color(0xFF4CAF50) else Color(0xFFF44336)
                                        Text(amountText, fontWeight = FontWeight.Bold, color = color)
                                    }
                                    HorizontalDivider(color = MaterialTheme.colorScheme.outline)
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}