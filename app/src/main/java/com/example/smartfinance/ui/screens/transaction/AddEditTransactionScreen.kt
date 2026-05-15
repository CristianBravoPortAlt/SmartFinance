package com.example.smartfinance.ui.screens.transaction

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
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.CalendarToday
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Description
import androidx.compose.material.icons.filled.EditCalendar
import androidx.compose.material.icons.filled.Euro
import androidx.compose.material.icons.filled.Repeat
import androidx.compose.material.icons.filled.Title
import androidx.compose.material3.Button
import androidx.compose.material3.CenterAlignedTopAppBar
import androidx.compose.material3.DatePicker
import androidx.compose.material3.DatePickerDialog
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExposedDropdownMenuBox
import androidx.compose.material3.ExposedDropdownMenuDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.MenuAnchorType
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.material3.rememberDatePickerState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.graphics.toColorInt
import com.example.smartfinance.data.local.CategoryEntity
import com.example.smartfinance.data.local.RecurrenceType
import com.example.smartfinance.data.local.TransactionEntity
import com.example.smartfinance.data.local.TransactionType
import com.example.smartfinance.ui.components.getCategoryIcon
import com.example.smartfinance.ui.screens.profile.AddEditCategoryDialog
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import java.util.UUID

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AddEditTransactionScreen(
    viewModel: TransactionViewModel,
    transactionId: String? = null,
    onNavigateBack: () -> Unit
) {
    var title by remember { mutableStateOf("") }
    var amount by remember { mutableStateOf("") }
    var description by remember { mutableStateOf("") }
    var selectedType by remember { mutableStateOf(TransactionType.EXPENSE) }
    var selectedCategory by remember { mutableStateOf<CategoryEntity?>(null) }
    var selectedDateMillis by remember { mutableLongStateOf(System.currentTimeMillis()) }
    var selectedRecurrence by remember { mutableStateOf(RecurrenceType.NONE) }
    
    var categoryExpanded by remember { mutableStateOf(false) }
    var recurrenceExpanded by remember { mutableStateOf(false) }
    var showDatePicker by remember { mutableStateOf(false) }
    var showAddCategoryDialog by remember { mutableStateOf(false) }

    val categories by viewModel.categories.collectAsState()
    val filteredCategories = categories.filter { it.type == selectedType }

    var existingTransaction by remember { mutableStateOf<TransactionEntity?>(null) }

    LaunchedEffect(transactionId) {
        if (!transactionId.isNullOrEmpty()) {
            val t = viewModel.getTransactionById(transactionId)
            if (t != null) {
                existingTransaction = t
                title = t.title
                amount = if (t.amount == 0.0) "" else t.amount.toString()
                description = t.description
                selectedType = t.type
                selectedCategory = categories.find { it.id == t.categoryId }
                selectedDateMillis = t.dateMillis
                selectedRecurrence = t.recurrence
            }
        }
    }

    LaunchedEffect(selectedType, categories) {
        if (selectedCategory?.type != selectedType && filteredCategories.isNotEmpty()) {
            selectedCategory = filteredCategories.first()
        }
    }

    if (showDatePicker) {
        val datePickerState = rememberDatePickerState(initialSelectedDateMillis = selectedDateMillis)
        DatePickerDialog(
            onDismissRequest = { showDatePicker = false },
            confirmButton = {
                TextButton(onClick = {
                    datePickerState.selectedDateMillis?.let { selectedDateMillis = it }
                    showDatePicker = false
                }) { Text("Confirmar") }
            },
            dismissButton = {
                TextButton(onClick = { showDatePicker = false }) { Text("Cancelar") }
            }
        ) {
            DatePicker(state = datePickerState)
        }
    }

    if (showAddCategoryDialog) {
        AddEditCategoryDialog(
            category = CategoryEntity(
                name = "",
                iconResName = "category",
                colorHex = "#4361EE",
                type = selectedType
            ),
            isNew = true,
            onDismiss = { showAddCategoryDialog = false },
            onSave = { newCategory ->
                viewModel.insertCategory(newCategory)
                showAddCategoryDialog = false
            },
            onDelete = {}
        )
    }

    Scaffold(
        topBar = {
            CenterAlignedTopAppBar(
                title = { 
                    Text(
                        if (existingTransaction != null) "Editar Movimiento" else "Nuevo Movimiento",
                        fontWeight = FontWeight.Bold
                    ) 
                },
                navigationIcon = {
                    IconButton(onClick = onNavigateBack) {
                        Icon(Icons.Default.ArrowBack, contentDescription = "Atrás")
                    }
                },
                actions = {
                    if (existingTransaction != null) {
                        IconButton(onClick = {
                            viewModel.deleteTransaction(existingTransaction!!)
                            onNavigateBack()
                        }) {
                            Icon(Icons.Default.Delete, contentDescription = "Borrar", tint = MaterialTheme.colorScheme.error)
                        }
                    }
                },
                colors = TopAppBarDefaults.centerAlignedTopAppBarColors(
                    containerColor = MaterialTheme.colorScheme.background
                )
            )
        }
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .verticalScroll(rememberScrollState())
                .padding(24.dp)
        ) {
            Surface(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(16.dp),
                color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.3f)
            ) {
                Row(
                    modifier = Modifier.padding(4.dp),
                    horizontalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    TransactionTypeButton(
                        text = "Gasto",
                        isSelected = selectedType == TransactionType.EXPENSE,
                        selectedColor = Color(0xFFFF5252),
                        modifier = Modifier.weight(1f),
                        onClick = { selectedType = TransactionType.EXPENSE }
                    )
                    TransactionTypeButton(
                        text = "Ingreso",
                        isSelected = selectedType == TransactionType.INCOME,
                        selectedColor = Color(0xFF2ECC71),
                        modifier = Modifier.weight(1f),
                        onClick = { selectedType = TransactionType.INCOME }
                    )
                }
            }

            Spacer(modifier = Modifier.height(32.dp))

            OutlinedTextField(
                value = title,
                onValueChange = { title = it },
                label = { Text("¿Qué es?") },
                placeholder = { Text("Ej. Compra semanal, Sueldo...") },
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(16.dp),
                leadingIcon = { Icon(Icons.Default.Title, contentDescription = null) },
                singleLine = true
            )
            
            Spacer(modifier = Modifier.height(20.dp))

            OutlinedTextField(
                value = amount,
                onValueChange = { if (it.isEmpty() || it.toDoubleOrNull() != null) amount = it },
                label = { Text("Importe") },
                placeholder = { Text("0.00") },
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(16.dp),
                leadingIcon = { Icon(Icons.Default.Euro, contentDescription = null) },
                suffix = { Text("€", fontWeight = FontWeight.Bold) },
                singleLine = true
            )

            Spacer(modifier = Modifier.height(20.dp))

            val dateStr = SimpleDateFormat("dd/MM/yyyy", Locale.getDefault()).format(Date(selectedDateMillis))
            OutlinedTextField(
                value = dateStr,
                onValueChange = {},
                readOnly = true,
                label = { Text("Fecha") },
                leadingIcon = { Icon(Icons.Default.CalendarToday, contentDescription = null) },
                trailingIcon = { 
                    IconButton(onClick = { showDatePicker = true }) {
                        Icon(Icons.Default.EditCalendar, contentDescription = "Seleccionar fecha")
                    }
                },
                modifier = Modifier.fillMaxWidth().clickable { showDatePicker = true },
                shape = RoundedCornerShape(16.dp)
            )

            Spacer(modifier = Modifier.height(20.dp))

            ExposedDropdownMenuBox(
                expanded = recurrenceExpanded,
                onExpandedChange = { recurrenceExpanded = it },
                modifier = Modifier.fillMaxWidth()
            ) {
                val recurrenceText = when(selectedRecurrence) {
                    RecurrenceType.NONE -> "Pago único"
                    RecurrenceType.DAILY -> "Diario"
                    RecurrenceType.WEEKLY -> "Semanal"
                    RecurrenceType.MONTHLY -> "Mensual"
                    RecurrenceType.ANNUAL -> "Anual"
                }
                OutlinedTextField(
                    value = recurrenceText,
                    onValueChange = {},
                    readOnly = true,
                    label = { Text("Frecuencia") },
                    leadingIcon = { Icon(Icons.Default.Repeat, contentDescription = null) },
                    trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = recurrenceExpanded) },
                    modifier = Modifier.menuAnchor(MenuAnchorType.PrimaryNotEditable).fillMaxWidth(),
                    shape = RoundedCornerShape(16.dp)
                )
                ExposedDropdownMenu(
                    expanded = recurrenceExpanded,
                    onDismissRequest = { recurrenceExpanded = false }
                ) {
                    RecurrenceType.entries.forEach { type ->
                        val typeText = when(type) {
                            RecurrenceType.NONE -> "Pago único"
                            RecurrenceType.DAILY -> "Diario"
                            RecurrenceType.WEEKLY -> "Semanal"
                            RecurrenceType.MONTHLY -> "Mensual"
                            RecurrenceType.ANNUAL -> "Anual"
                        }
                        DropdownMenuItem(
                            text = { Text(typeText) },
                            onClick = {
                                selectedRecurrence = type
                                recurrenceExpanded = false
                            }
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(20.dp))

            ExposedDropdownMenuBox(
                expanded = categoryExpanded,
                onExpandedChange = { categoryExpanded = it },
                modifier = Modifier.fillMaxWidth()
            ) {
                val categoryColor = selectedCategory?.colorHex?.let { Color(it.toColorInt()) } ?: MaterialTheme.colorScheme.primary
                
                OutlinedTextField(
                    value = selectedCategory?.name ?: "Selecciona Categoría",
                    onValueChange = {},
                    readOnly = true,
                    label = { Text("Categoría") },
                    leadingIcon = {
                        Box(
                            modifier = Modifier
                                .size(24.dp)
                                .clip(CircleShape)
                                .background(categoryColor.copy(alpha = 0.1f)),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = getCategoryIcon(selectedCategory?.iconResName ?: ""),
                                contentDescription = null,
                                tint = categoryColor,
                                modifier = Modifier.size(16.dp)
                            )
                        }
                    },
                    trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = categoryExpanded) },
                    modifier = Modifier
                        .menuAnchor(MenuAnchorType.PrimaryNotEditable)
                        .fillMaxWidth(),
                    shape = RoundedCornerShape(16.dp)
                )
                
                ExposedDropdownMenu(
                    expanded = categoryExpanded,
                    onDismissRequest = { categoryExpanded = false },
                    modifier = Modifier.background(MaterialTheme.colorScheme.surface)
                ) {
                    filteredCategories.forEach { category ->
                        val itemColor = Color(category.colorHex.toColorInt())
                        DropdownMenuItem(
                            text = { 
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Box(
                                        modifier = Modifier
                                            .size(32.dp)
                                            .clip(CircleShape)
                                            .background(itemColor.copy(alpha = 0.1f)),
                                        contentAlignment = Alignment.Center
                                    ) {
                                        Icon(
                                            imageVector = getCategoryIcon(category.iconResName),
                                            contentDescription = null,
                                            tint = itemColor,
                                            modifier = Modifier.size(18.dp)
                                        )
                                    }
                                    Spacer(modifier = Modifier.width(12.dp))
                                    Text(category.name)
                                }
                            },
                            onClick = {
                                selectedCategory = category
                                categoryExpanded = false
                            }
                        )
                    }
                    
                    HorizontalDivider(modifier = Modifier.padding(vertical = 4.dp))
                    DropdownMenuItem(
                        text = {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Icon(
                                    Icons.Default.Add,
                                    contentDescription = null,
                                    tint = MaterialTheme.colorScheme.primary,
                                    modifier = Modifier.size(20.dp)
                                )
                                Spacer(modifier = Modifier.width(12.dp))
                                Text(
                                    "Añadir categoría personalizada",
                                    color = MaterialTheme.colorScheme.primary,
                                    fontWeight = FontWeight.Bold
                                )
                            }
                        },
                        onClick = {
                            categoryExpanded = false
                            showAddCategoryDialog = true
                        }
                    )
                }
            }

            Spacer(modifier = Modifier.height(20.dp))

            OutlinedTextField(
                value = description,
                onValueChange = { description = it },
                label = { Text("Notas (opcional)") },
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(16.dp),
                leadingIcon = { Icon(Icons.Default.Description, contentDescription = null) },
                minLines = 3
            )

            Spacer(modifier = Modifier.height(40.dp))

            Button(
                onClick = {
                    val finalAmount = amount.toDoubleOrNull() ?: 0.0
                    val categoryId = selectedCategory?.id ?: ""
                    if (existingTransaction != null) {
                        viewModel.insertTransaction(existingTransaction!!.copy(
                            title = title,
                            amount = finalAmount,
                            description = description,
                            type = selectedType,
                            categoryId = categoryId,
                            dateMillis = selectedDateMillis,
                            recurrence = selectedRecurrence
                        ))
                    } else {
                        viewModel.insertTransaction(
                            TransactionEntity(
                                id = UUID.randomUUID().toString(),
                                userId = "",
                                categoryId = categoryId,
                                amount = finalAmount,
                                title = title,
                                description = description,
                                dateMillis = selectedDateMillis,
                                type = selectedType,
                                recurrence = selectedRecurrence
                            )
                        )
                    }
                    onNavigateBack()
                },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(60.dp),
                shape = RoundedCornerShape(16.dp),
                enabled = title.isNotBlank() && amount.isNotBlank() && selectedCategory != null
            ) {
                Text(
                    text = if (existingTransaction != null) "Actualizar" else "Guardar Movimiento",
                    fontSize = 16.sp,
                    fontWeight = FontWeight.Bold
                )
            }
            
            Spacer(modifier = Modifier.height(24.dp))
        }
    }
}

@Composable
fun TransactionTypeButton(
    text: String,
    isSelected: Boolean,
    selectedColor: Color,
    modifier: Modifier = Modifier,
    onClick: () -> Unit
) {
    Surface(
        onClick = onClick,
        modifier = modifier.height(48.dp),
        shape = RoundedCornerShape(12.dp),
        color = if (isSelected) selectedColor else Color.Transparent,
        contentColor = if (isSelected) Color.White else MaterialTheme.colorScheme.onSurfaceVariant
    ) {
        Box(contentAlignment = Alignment.Center) {
            Text(
                text = text,
                fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium,
                fontSize = 14.sp
            )
        }
    }
}
