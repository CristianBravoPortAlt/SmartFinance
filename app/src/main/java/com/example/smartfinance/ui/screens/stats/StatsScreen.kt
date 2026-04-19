package com.example.smartfinance.ui.screens.stats

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.smartfinance.data.local.TransactionType
import com.example.smartfinance.ui.screens.transaction.TransactionViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun StatsScreen(
    viewModel: TransactionViewModel,
    onNavigateBack: () -> Unit
) {
    val transactions by viewModel.transactions.collectAsState()
    val categories by viewModel.categories.collectAsState()

    val expenses = transactions.filter { it.type == TransactionType.EXPENSE }
    val expensesByCategory = expenses.groupBy { it.categoryId }.mapValues { entry ->
        entry.value.sumOf { it.amount }
    }.toList().sortedByDescending { it.second }

    val totalExpense = expensesByCategory.sumOf { it.second }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Estadísticas de Gastos") },
                navigationIcon = {
                    IconButton(onClick = onNavigateBack) {
                        Icon(Icons.Default.ArrowBack, contentDescription = "Atrás")
                    }
                }
            )
        }
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .padding(24.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            if (totalExpense == 0.0) {
                Text("No hay gastos registrados.", color = MaterialTheme.colorScheme.onSurfaceVariant)
            } else {
                Box(modifier = Modifier.size(220.dp), contentAlignment = Alignment.Center) {
                    Canvas(modifier = Modifier.size(220.dp)) {
                        var startAngle = -90f
                        val shadesOfGrey = listOf(
                            Color(0xFF212121),
                            Color(0xFF616161),
                            Color(0xFF9E9E9E),
                            Color(0xFFE0E0E0),
                            Color(0xFFF5F5F5)
                        )
                        expensesByCategory.forEachIndexed { index, pair ->
                            val sweepAngle = (pair.second / totalExpense).toFloat() * 360f
                            val color = shadesOfGrey[index % shadesOfGrey.size]
                            drawArc(
                                color = color,
                                startAngle = startAngle,
                                sweepAngle = sweepAngle,
                                useCenter = false,
                                style = Stroke(width = 60f, cap = StrokeCap.Butt),
                                size = Size(size.width, size.height)
                            )
                            startAngle += sweepAngle
                        }
                    }
                    Text(
                        text = "-${totalExpense}€",
                        fontWeight = FontWeight.Bold,
                        fontSize = 24.sp,
                        color = MaterialTheme.colorScheme.onBackground
                    )
                }

                Spacer(modifier = Modifier.height(48.dp))

                expensesByCategory.forEachIndexed { index, pair ->
                    val category = categories.find { it.id == pair.first }
                    val shadesOfGrey = listOf(
                        Color(0xFF212121),
                        Color(0xFF616161),
                        Color(0xFF9E9E9E),
                        Color(0xFFE0E0E0),
                        Color(0xFFF5F5F5)
                    )
                    val color = shadesOfGrey[index % shadesOfGrey.size]
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 12.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Box(modifier = Modifier.size(20.dp)) {
                                Canvas(modifier = Modifier.fillMaxSize()) {
                                    drawCircle(color = color)
                                }
                            }
                            Spacer(modifier = Modifier.width(16.dp))
                            Text(category?.name ?: "Desconocido", fontSize = 16.sp, fontWeight = FontWeight.Medium)
                        }
                        Text("${pair.second}€", fontWeight = FontWeight.Bold, fontSize = 16.sp)
                    }
                }
            }
        }
    }
}
