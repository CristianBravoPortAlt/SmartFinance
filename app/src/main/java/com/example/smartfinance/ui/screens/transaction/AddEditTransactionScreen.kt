package com.example.smartfinance.ui.screens.transaction

import android.graphics.Color.parseColor
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Description
import androidx.compose.material.icons.filled.Title
import androidx.compose.material.icons.filled.Euro
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.smartfinance.data.local.CategoryEntity
import com.example.smartfinance.data.local.TransactionEntity
import com.example.smartfinance.data.local.TransactionType
import com.example.smartfinance.ui.components.getCategoryIcon
import androidx.core.graphics.toColorInt

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AddEditTransactionScreen(
    viewModel: TransactionViewModel,
    transactionId: Int? = null,
    onNavigateBack: () -> Unit
) {
    var title by remember { mutableStateOf("") }
    var amount by remember { mutableStateOf("") }
    var description by remember { mutableStateOf("") }
    var selectedType by remember { mutableStateOf(TransactionType.EXPENSE) }
    var selectedCategory by remember { mutableStateOf<CategoryEntity?>(null) }
    
    var expanded by remember { mutableStateOf(false) }

    val categories by viewModel.categories.collectAsState()
    val filteredCategories = categories.filter { it.type == selectedType }

    var existingTransaction by remember { mutableStateOf<TransactionEntity?>(null) }

    LaunchedEffect(transactionId) {
        if (transactionId != null && transactionId != -1) {
            val t = viewModel.getTransactionById(transactionId)
            if (t != null) {
                existingTransaction = t
                title = t.title
                amount = if (t.amount == 0.0) "" else t.amount.toString()
                description = t.description
                selectedType = t.type
                selectedCategory = categories.find { it.id == t.categoryId }
            }
        }
    }

    LaunchedEffect(selectedType, categories) {
        if (selectedCategory?.type != selectedType && filteredCategories.isNotEmpty()) {
            selectedCategory = filteredCategories.first()
        }
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
                label = { Text("¿Qué has pagado?") },
                placeholder = { Text("Ej. Compra semanal") },
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

            ExposedDropdownMenuBox(
                expanded = expanded,
                onExpandedChange = { expanded = it }
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
                    trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = expanded) },
                    modifier = Modifier
                        .menuAnchor()
                        .fillMaxWidth(),
                    shape = RoundedCornerShape(16.dp)
                )
                
                ExposedDropdownMenu(
                    expanded = expanded,
                    onDismissRequest = { expanded = false },
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
                                expanded = false
                            }
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(20.dp))

            // Description Field
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

            // Save Button
            Button(
                onClick = {
                    val finalAmount = amount.toDoubleOrNull() ?: 0.0
                    val categoryId = selectedCategory?.id ?: 1
                    if (existingTransaction != null) {
                        viewModel.insertTransaction(existingTransaction!!.copy(
                            title = title,
                            amount = finalAmount,
                            description = description,
                            type = selectedType,
                            categoryId = categoryId
                        ))
                    } else {
                        viewModel.insertTransaction(
                            TransactionEntity(
                                userId = 1,
                                categoryId = categoryId,
                                amount = finalAmount,
                                title = title,
                                description = description,
                                dateMillis = System.currentTimeMillis(),
                                type = selectedType
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
                    text = if (existingTransaction != null) "Actualizar" else "Añadir Movimiento",
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
