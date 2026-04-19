package com.example.smartfinance.ui.screens.transaction

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.smartfinance.data.local.CategoryEntity
import com.example.smartfinance.data.local.TransactionEntity
import com.example.smartfinance.data.local.TransactionType
import com.example.smartfinance.ui.components.CustomTextField
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AddEditTransactionScreen(
    viewModel: TransactionViewModel,
    transactionId: Int? = null,
    onNavigateBack: () -> Unit
) {
    val coroutineScope = rememberCoroutineScope()
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
                amount = t.amount.toString()
                description = t.description
                selectedType = t.type
                selectedCategory = categories.find { it.id == t.categoryId }
            }
        }
    }

    // Ensure selectedCategory is valid when type changes
    LaunchedEffect(selectedType, categories) {
        if (selectedCategory?.type != selectedType && filteredCategories.isNotEmpty()) {
            selectedCategory = filteredCategories.first()
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(if (transactionId != null && transactionId != -1) "Editar Movimiento" else "Añadir Movimiento") },
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
                }
            )
        }
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .padding(24.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.Center
            ) {
                SingleChoiceSegmentedButtonRow {
                    SegmentedButton(
                        selected = selectedType == TransactionType.EXPENSE,
                        onClick = { selectedType = TransactionType.EXPENSE },
                        shape = SegmentedButtonDefaults.itemShape(index = 0, count = 2),
                        label = { Text("Gasto") }
                    )
                    SegmentedButton(
                        selected = selectedType == TransactionType.INCOME,
                        onClick = { selectedType = TransactionType.INCOME },
                        shape = SegmentedButtonDefaults.itemShape(index = 1, count = 2),
                        label = { Text("Ingreso") }
                    )
                }
            }

            Spacer(modifier = Modifier.height(24.dp))

            CustomTextField(
                value = title,
                onValueChange = { title = it },
                label = "Título",
                isPassword = false
            )
            
            Spacer(modifier = Modifier.height(16.dp))

            CustomTextField(
                value = amount,
                onValueChange = { amount = it },
                label = "Importe",
                isPassword = false
            )

            Spacer(modifier = Modifier.height(16.dp))

            ExposedDropdownMenuBox(
                expanded = expanded,
                onExpandedChange = { expanded = it }
            ) {
                OutlinedTextField(
                    value = selectedCategory?.name ?: "Selecciona Categoría",
                    onValueChange = {},
                    readOnly = true,
                    label = { Text("Categoría") },
                    trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = expanded) },
                    modifier = Modifier.menuAnchor().fillMaxWidth()
                )
                ExposedDropdownMenu(
                    expanded = expanded,
                    onDismissRequest = { expanded = false }
                ) {
                    filteredCategories.forEach { category ->
                        DropdownMenuItem(
                            text = { Text(category.name) },
                            onClick = {
                                selectedCategory = category
                                expanded = false
                            }
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            CustomTextField(
                value = description,
                onValueChange = { description = it },
                label = "Descripción",
                isPassword = false
            )

            Spacer(modifier = Modifier.weight(1f))

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
                    .height(56.dp),
                enabled = title.isNotBlank() && amount.isNotBlank() && selectedCategory != null
            ) {
                Text(text = "Guardar", fontSize = 18.sp, fontWeight = FontWeight.Bold)
            }
        }
    }
}
