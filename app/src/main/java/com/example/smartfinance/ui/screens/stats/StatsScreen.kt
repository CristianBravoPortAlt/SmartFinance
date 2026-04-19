package com.example.smartfinance.ui.screens.stats

import androidx.compose.animation.core.*
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.PieChart
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.smartfinance.data.local.TransactionType
import com.example.smartfinance.ui.components.getCategoryIcon
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

    val chartColors = listOf(
        Color(0xFF4361EE),
        Color(0xFFF72585),
        Color(0xFF4CC9F0),
        Color(0xFFF8961E),
        Color(0xFF2ECC71),
        Color(0xFF9D4EDD),
        Color(0xFF7209B7),
        Color(0xFF4895EF),
        Color(0xFFF94144),
        Color(0xFF90BE6D)
    )

    Scaffold(
        topBar = {
            CenterAlignedTopAppBar(
                title = { Text("Análisis de Gastos", fontWeight = FontWeight.Bold) },
                navigationIcon = {
                    IconButton(onClick = onNavigateBack) {
                        Icon(Icons.Default.ArrowBack, contentDescription = "Atrás")
                    }
                },
                colors = TopAppBarDefaults.centerAlignedTopAppBarColors(
                    containerColor = MaterialTheme.colorScheme.background
                )
            )
        }
    ) { padding ->
        if (totalExpense == 0.0) {
            Box(modifier = Modifier.fillMaxSize().padding(padding), contentAlignment = Alignment.Center) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Icon(
                        Icons.Default.PieChart,
                        contentDescription = null,
                        modifier = Modifier.size(64.dp),
                        tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.2f)
                    )
                    Spacer(modifier = Modifier.height(16.dp))
                    Text("No hay gastos registrados este mes", color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
            }
        } else {
            LazyColumn(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(padding),
                contentPadding = PaddingValues(24.dp)
            ) {
                item {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(260.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        DonutChart(
                            expensesByCategory = expensesByCategory,
                            totalExpense = totalExpense,
                            colors = chartColors
                        )
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            Text(
                                text = "Total Gastado",
                                fontSize = 14.sp,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                            Text(
                                text = String.format("%.2f€", totalExpense),
                                fontWeight = FontWeight.ExtraBold,
                                fontSize = 32.sp,
                                color = MaterialTheme.colorScheme.onBackground,
                                letterSpacing = (-1).sp
                            )
                        }
                    }
                }

                item { Spacer(modifier = Modifier.height(40.dp)) }

                item {
                    Text(
                        text = "Por Categorías",
                        fontWeight = FontWeight.Bold,
                        fontSize = 18.sp,
                        modifier = Modifier.padding(bottom = 16.dp)
                    )
                }

                itemsIndexed(expensesByCategory) { index, pair ->
                    val category = categories.find { it.id == pair.first }
                    val color = chartColors[index % chartColors.size]
                    val percentage = (pair.second / totalExpense).toFloat()
                    
                    CategoryStatItem(
                        name = category?.name ?: "Otros",
                        amount = pair.second,
                        percentage = percentage,
                        color = color,
                        iconName = category?.iconResName ?: "category"
                    )
                    Spacer(modifier = Modifier.height(16.dp))
                }
            }
        }
    }
}

@Composable
fun DonutChart(
    expensesByCategory: List<Pair<Int, Double>>,
    totalExpense: Double,
    colors: List<Color>
) {
    val transition = rememberInfiniteTransition(label = "donut")
    val alpha by transition.animateFloat(
        initialValue = 0.8f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(
            animation = tween(2000),
            repeatMode = RepeatMode.Reverse
        ),
        label = "alpha"
    )

    Canvas(modifier = Modifier.size(220.dp)) {
        var startAngle = -90f
        expensesByCategory.forEachIndexed { index, pair ->
            val sweepAngle = (pair.second / totalExpense).toFloat() * 360f
            drawArc(
                color = colors[index % colors.size].copy(alpha = alpha),
                startAngle = startAngle,
                sweepAngle = sweepAngle,
                useCenter = false,
                style = Stroke(width = 45f, cap = StrokeCap.Round),
                size = Size(size.width, size.height)
            )
            startAngle += sweepAngle
        }
    }
}

@Composable
fun CategoryStatItem(
    name: String,
    amount: Double,
    percentage: Float,
    color: Color,
    iconName: String
) {
    Surface(
        shape = RoundedCornerShape(20.dp),
        color = MaterialTheme.colorScheme.surface,
        tonalElevation = 1.dp
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Box(
                    modifier = Modifier
                        .size(40.dp)
                        .clip(CircleShape)
                        .background(color.copy(alpha = 0.15f)),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = getCategoryIcon(iconName),
                        contentDescription = null,
                        tint = color,
                        modifier = Modifier.size(20.dp)
                    )
                }
                
                Spacer(modifier = Modifier.width(16.dp))
                
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = name,
                        fontWeight = FontWeight.Bold,
                        fontSize = 16.sp
                    )
                    Text(
                        text = String.format("%.1f%% del total", percentage * 100),
                        fontSize = 12.sp,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
                
                Text(
                    text = String.format("%.2f€", amount),
                    fontWeight = FontWeight.Bold,
                    fontSize = 16.sp,
                    color = MaterialTheme.colorScheme.onSurface
                )
            }
            
            Spacer(modifier = Modifier.height(12.dp))
            
            LinearProgressIndicator(
                progress = { percentage },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(6.dp)
                    .clip(CircleShape),
                color = color,
                trackColor = color.copy(alpha = 0.1f)
            )
        }
    }
}
