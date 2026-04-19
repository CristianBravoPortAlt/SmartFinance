package com.example.smartfinance.data.repository

import com.example.smartfinance.data.local.CategoryDao
import com.example.smartfinance.data.local.CategoryEntity
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
import javax.inject.Inject

class CategoryRepository @Inject constructor(
    private val categoryDao: CategoryDao
) {
    fun getAllCategories(): Flow<List<CategoryEntity>> {
        return categoryDao.getAllCategories()
    }

    fun getCategoriesByType(type: com.example.smartfinance.data.local.TransactionType): Flow<List<CategoryEntity>> {
        return categoryDao.getCategoriesByType(type)
    }

    suspend fun getCategoryById(id: Int): CategoryEntity? {
        return categoryDao.getCategoryById(id)
    }

    suspend fun insertCategory(category: CategoryEntity) {
        categoryDao.insertCategory(category)
    }

    suspend fun initializeCategoriesIfNeeded() {
        val current = categoryDao.getAllCategories().first()
        if (current.isEmpty()) {
            val initialCategories = listOf(
                CategoryEntity(name = "Comida", iconResName = "restaurant", colorHex = "#FF5722", type = com.example.smartfinance.data.local.TransactionType.EXPENSE),
                CategoryEntity(name = "Ocio", iconResName = "movie", colorHex = "#9C27B0", type = com.example.smartfinance.data.local.TransactionType.EXPENSE),
                CategoryEntity(name = "Transporte", iconResName = "directions_car", colorHex = "#2196F3", type = com.example.smartfinance.data.local.TransactionType.EXPENSE),
                CategoryEntity(name = "Sueldo", iconResName = "attach_money", colorHex = "#4CAF50", type = com.example.smartfinance.data.local.TransactionType.INCOME),
                CategoryEntity(name = "Regalos", iconResName = "card_giftcard", colorHex = "#FFC107", type = com.example.smartfinance.data.local.TransactionType.INCOME)
            )
            initialCategories.forEach { categoryDao.insertCategory(it) }
        }
    }
}
