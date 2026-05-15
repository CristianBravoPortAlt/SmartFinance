package com.example.smartfinance.ui.screens.stats

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowForward
import androidx.compose.material.icons.automirrored.filled.TrendingDown
import androidx.compose.material.icons.automirrored.filled.TrendingUp
import androidx.compose.material.icons.filled.ChevronLeft
import androidx.compose.material.icons.filled.ChevronRight
import androidx.compose.material.icons.filled.Menu
import androidx.compose.material.icons.filled.TrendingDown
import androidx.compose.material.icons.filled.TrendingUp
import androidx.compose.material3.CenterAlignedTopAppBar
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.smartfinance.data.local.TransactionEntity
import com.example.smartfinance.data.local.TransactionType
import com.example.smartfinance.ui.screens.home.TransactionItem
import com.example.smartfinance.ui.screens.transaction.TransactionViewModel
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Locale

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MonthlySummaryScreen(
    viewModel: TransactionViewModel,
    onMenuClick: () -> Unit,
    onNavigateBack: () -> Unit,
    onNavigateToEdit: (String) -> Unit
) {
    val transactions by viewModel.transactions.collectAsState()
    val categories by viewModel.categories.collectAsState()
    val coroutineScope = rememberCoroutineScope()

    val totalPages = 24
    val initialPage = 12
    val pagerState = rememberPagerState(initialPage = initialPage, pageCount = { totalPages })
    
    val currentViewMonth = remember(pagerState.currentPage) {
        Calendar.getInstance().apply {
            add(Calendar.MONTH, pagerState.currentPage - initialPage)
        }
    }

    val monthName = remember(currentViewMonth) {
        SimpleDateFormat("MMMM yyyy", Locale.getDefault()).format(currentViewMonth.time)
            .replaceFirstChar { if (it.isLowerCase()) it.titlecase(Locale.getDefault()) else it.toString() }
    }

    var selectedDay by remember { mutableStateOf<Int?>(null) }
    
    LaunchedEffect(pagerState.currentPage) {
        selectedDay = null
    }

    val selectedDate = remember(selectedDay, currentViewMonth) {
        if (selectedDay != null) {
            (currentViewMonth.clone() as Calendar).apply { set(Calendar.DAY_OF_MONTH, selectedDay!!) }
        } else null
    }

    val monthTransactions = remember(transactions, currentViewMonth) {
        transactions.filter {
            val tCal = Calendar.getInstance().apply { timeInMillis = it.dateMillis }
            tCal.get(Calendar.YEAR) == currentViewMonth.get(Calendar.YEAR) &&
                    tCal.get(Calendar.MONTH) == currentViewMonth.get(Calendar.MONTH)
        }
    }

    val displayTransactions = remember(monthTransactions, selectedDate) {
        if (selectedDate != null) {
            monthTransactions.filter {
                val tCal = Calendar.getInstance().apply { timeInMillis = it.dateMillis }
                tCal.get(Calendar.DAY_OF_YEAR) == selectedDate.get(Calendar.DAY_OF_YEAR)
            }
        } else {
            monthTransactions
        }
    }

    val currentIncome = displayTransactions.filter { it.type == TransactionType.INCOME }.sumOf { it.amount }
    val currentExpense = displayTransactions.filter { it.type == TransactionType.EXPENSE }.sumOf { it.amount }

    val monthIncome = monthTransactions.filter { it.type == TransactionType.INCOME }.sumOf { it.amount }
    val monthExpense = monthTransactions.filter { it.type == TransactionType.EXPENSE }.sumOf { it.amount }
    val monthlySavings = monthIncome - monthExpense

    Scaffold(
        topBar = {
            CenterAlignedTopAppBar(
                title = { 
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        IconButton(onClick = {
                            coroutineScope.launch {
                                pagerState.animateScrollToPage(pagerState.currentPage - 1)
                            }
                        }) {
                            Icon(Icons.Default.ChevronLeft, contentDescription = "Mes anterior")
                        }
                        Text(
                            text = monthName, 
                            fontWeight = FontWeight.Bold,
                            modifier = Modifier.padding(horizontal = 8.dp)
                        )
                        IconButton(onClick = {
                            coroutineScope.launch {
                                pagerState.animateScrollToPage(pagerState.currentPage + 1)
                            }
                        }) {
                            Icon(Icons.Default.ChevronRight, contentDescription = "Mes siguiente")
                        }
                    }
                },
                navigationIcon = {
                    IconButton(onClick = onMenuClick) {
                        Icon(Icons.Default.Menu, contentDescription = "Menú")
                    }
                },
                actions = {
                    TextButton(onClick = {
                        coroutineScope.launch {
                            pagerState.animateScrollToPage(initialPage)
                        }
                    }) {
                        Text("Hoy", fontWeight = FontWeight.Bold)
                    }
                },
                colors = TopAppBarDefaults.centerAlignedTopAppBarColors(
                    containerColor = MaterialTheme.colorScheme.background
                )
            )
        }
    ) { padding ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .background(MaterialTheme.colorScheme.background)
        ) {
            item {
                HorizontalPager(
                    state = pagerState,
                    modifier = Modifier.fillMaxWidth()
                ) { page ->
                    val pageMonth = Calendar.getInstance().apply {
                        add(Calendar.MONTH, page - initialPage)
                    }
                    CalendarGrid(
                        viewMonth = pageMonth,
                        selectedDay = if (page == pagerState.currentPage) selectedDay else null,
                        transactions = transactions,
                        onDaySelected = { day ->
                            selectedDay = if (selectedDay == day) null else day
                        }
                    )
                }
            }

            item {
                if (monthTransactions.isNotEmpty()) {
                    SavingsSummaryMessage(savings = monthlySavings)
                }
            }

            item {
                ComparisonSection(
                    income = currentIncome,
                    expense = currentExpense,
                    isDayView = selectedDay != null
                )
            }

            item {
                val title = if (selectedDate != null) {
                    SimpleDateFormat("EEEE d 'de' MMMM", Locale.getDefault()).format(selectedDate.time)
                        .replaceFirstChar { it.uppercase() }
                } else {
                    "Movimientos del mes"
                }
                
                Text(
                    text = title,
                    fontSize = 14.sp,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.padding(horizontal = 24.dp, vertical = 16.dp)
                )
                HorizontalDivider(
                    modifier = Modifier.padding(horizontal = 24.dp), 
                    thickness = 0.5.dp,
                    color = MaterialTheme.colorScheme.outline.copy(alpha = 0.1f)
                )
            }

            if (displayTransactions.isEmpty()) {
                item {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(48.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            "Sin movimientos registrados",
                            color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.6f)
                        )
                    }
                }
            } else {
                items(displayTransactions) { transaction ->
                    val category = categories.find { it.id == transaction.categoryId }
                    TransactionItem(
                        transaction = transaction,
                        categoryName = category?.name ?: "Sin categoría",
                        iconName = category?.iconResName ?: "category",
                        colorHex = category?.colorHex ?: "#9E9E9E",
                        onClick = { onNavigateToEdit(transaction.id) },
                        onLongClick = {}
                    )
                }
            }
            
            item { Spacer(modifier = Modifier.height(32.dp)) }
        }
    }
}

@Composable
fun CalendarGrid(
    viewMonth: Calendar,
    selectedDay: Int?,
    transactions: List<TransactionEntity>,
    onDaySelected: (Int) -> Unit
) {
    val daysOfWeek = listOf("L", "M", "M", "J", "V", "S", "D")
    val calendar = (viewMonth.clone() as Calendar).apply { set(Calendar.DAY_OF_MONTH, 1) }
    
    val firstDayOfWeek = (calendar.get(Calendar.DAY_OF_WEEK) + 5) % 7
    val daysInMonth = calendar.getActualMaximum(Calendar.DAY_OF_MONTH)

    Surface(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 24.dp, vertical = 16.dp),
        shape = RoundedCornerShape(24.dp),
        color = MaterialTheme.colorScheme.surface,
        tonalElevation = 2.dp
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(modifier = Modifier.fillMaxWidth()) {
                daysOfWeek.forEach { day ->
                    Text(
                        text = day,
                        modifier = Modifier.weight(1f),
                        textAlign = TextAlign.Center,
                        fontSize = 12.sp,
                        color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.5f),
                        fontWeight = FontWeight.Medium
                    )
                }
            }
            
            Spacer(modifier = Modifier.height(16.dp))

            var dayCounter = 1
            for (i in 0..5) {
                if (dayCounter > daysInMonth) break
                Row(modifier = Modifier.fillMaxWidth()) {
                    for (j in 0..6) {
                        val isActive = (i > 0 || j >= firstDayOfWeek) && dayCounter <= daysInMonth
                        if (isActive) {
                            val day = dayCounter
                            val isSelected = selectedDay == day
                            
                            val hasIncome = transactions.any {
                                val tCal = Calendar.getInstance().apply { timeInMillis = it.dateMillis }
                                tCal.get(Calendar.YEAR) == viewMonth.get(Calendar.YEAR) &&
                                tCal.get(Calendar.MONTH) == viewMonth.get(Calendar.MONTH) &&
                                tCal.get(Calendar.DAY_OF_MONTH) == day && it.type == TransactionType.INCOME
                            }
                            val hasExpense = transactions.any {
                                val tCal = Calendar.getInstance().apply { timeInMillis = it.dateMillis }
                                tCal.get(Calendar.YEAR) == viewMonth.get(Calendar.YEAR) &&
                                tCal.get(Calendar.MONTH) == viewMonth.get(Calendar.MONTH) &&
                                tCal.get(Calendar.DAY_OF_MONTH) == day && it.type == TransactionType.EXPENSE
                            }

                            CalendarDay(
                                day = day,
                                isSelected = isSelected,
                                hasIncome = hasIncome,
                                hasExpense = hasExpense,
                                onClick = { onDaySelected(day) },
                                modifier = Modifier.weight(1f)
                            )
                            dayCounter++
                        } else {
                            Box(modifier = Modifier.weight(1f))
                        }
                    }
                }
                Spacer(modifier = Modifier.height(8.dp))
            }
        }
    }
}

@Composable
fun CalendarDay(
    day: Int,
    isSelected: Boolean,
    hasIncome: Boolean,
    hasExpense: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val isToday = remember {
        val today = Calendar.getInstance()
        today.get(Calendar.DAY_OF_MONTH) == day
    }

    Column(
        modifier = modifier
            .clip(RoundedCornerShape(12.dp))
            .clickable { onClick() }
            .padding(vertical = 8.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text(
            text = day.toString(),
            fontSize = 16.sp,
            fontWeight = if (isSelected || isToday) FontWeight.Bold else FontWeight.Medium,
            color = if (isSelected) MaterialTheme.colorScheme.primary else if (isToday) MaterialTheme.colorScheme.secondary else MaterialTheme.colorScheme.onSurface
        )
        Row(
            modifier = Modifier.padding(top = 4.dp),
            horizontalArrangement = Arrangement.Center
        ) {
            if (hasIncome) {
                Box(
                    modifier = Modifier
                        .size(4.dp)
                        .clip(CircleShape)
                        .background(Color(0xFF2ECC71))
                )
            }
            if (hasExpense) {
                if (hasIncome) Spacer(modifier = Modifier.width(3.dp))
                Box(
                    modifier = Modifier
                        .size(4.dp)
                        .clip(CircleShape)
                        .background(MaterialTheme.colorScheme.onSurface.copy(alpha = 0.3f))
                )
            }
        }
    }
}

@Composable
fun SavingsSummaryMessage(savings: Double) {
    val isSaving = savings >= 0
    val text = if (isSaving) {
        "¡Enhorabuena! Has ahorrado ${String.format(Locale.getDefault(), "%.2f", savings)}€ este mes"
    } else {
        "Has gastado ${String.format(Locale.getDefault(), "%.2f", -savings)}€ de más este mes"
    }
    val icon = if (isSaving) Icons.AutoMirrored.Filled.TrendingUp else Icons.AutoMirrored.Filled.TrendingDown
    val color = if (isSaving) Color(0xFF2ECC71) else MaterialTheme.colorScheme.error

    Surface(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 24.dp),
        color = color.copy(alpha = 0.1f),
        shape = RoundedCornerShape(16.dp),
        border = androidx.compose.foundation.BorderStroke(1.dp, color.copy(alpha = 0.2f))
    ) {
        Row(
            modifier = Modifier.padding(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(icon, contentDescription = null, tint = color, modifier = Modifier.size(24.dp))
            Spacer(modifier = Modifier.width(16.dp))
            Text(
                text = text,
                fontSize = 14.sp,
                fontWeight = FontWeight.Bold,
                color = color
            )
        }
    }
}

@Composable
fun ComparisonSection(income: Double, expense: Double, isDayView: Boolean) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 24.dp, vertical = 24.dp),
        verticalAlignment = Alignment.Bottom
    ) {
        Column(
            modifier = Modifier.width(60.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            val maxVal = maxOf(income, expense, 1.0)
            val expenseHeight = ((expense / maxVal) * 120).dp
            val incomeHeight = ((income / maxVal) * 120).dp

            Row(verticalAlignment = Alignment.Bottom, horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                Box(
                    modifier = Modifier
                        .width(22.dp)
                        .height(expenseHeight.coerceAtLeast(6.dp))
                        .background(MaterialTheme.colorScheme.tertiary, RoundedCornerShape(topStart = 6.dp, topEnd = 6.dp))
                )
                Box(
                    modifier = Modifier
                        .width(22.dp)
                        .height(incomeHeight.coerceAtLeast(6.dp))
                        .background(Color(0xFF2ECC71), RoundedCornerShape(topStart = 6.dp, topEnd = 6.dp))
                )
            }
        }

        Spacer(modifier = Modifier.width(20.dp))

        Column(modifier = Modifier.weight(1f)) {
            SummarySmallCard(
                label = if (isDayView) "Gastos del día" else "Gastos del mes",
                amount = expense,
                color = MaterialTheme.colorScheme.tertiary
            )
            Spacer(modifier = Modifier.height(16.dp))
            SummarySmallCard(
                label = if (isDayView) "Ingresos del día" else "Ingresos del mes",
                amount = income,
                color = Color(0xFF2ECC71)
            )
        }
    }
}

@Composable
fun SummarySmallCard(label: String, amount: Double, color: Color) {
    Surface(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(20.dp),
        color = MaterialTheme.colorScheme.surface,
        tonalElevation = 2.dp
    ) {
        Row(
            modifier = Modifier.padding(16.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Box(
                    modifier = Modifier
                        .size(8.dp)
                        .clip(CircleShape)
                        .background(color)
                )
                Spacer(modifier = Modifier.width(12.dp))
                Column {
                    Text(
                        text = label, 
                        fontSize = 12.sp, 
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        fontWeight = FontWeight.Medium
                    )
                    Text(
                        text = String.format(Locale.getDefault(), "%.2f€", amount),
                        fontSize = 18.sp,
                        fontWeight = FontWeight.ExtraBold,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                }
            }
            Icon(
                Icons.AutoMirrored.Filled.ArrowForward,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.primary,
                modifier = Modifier.size(20.dp)
            )
        }
    }
}
