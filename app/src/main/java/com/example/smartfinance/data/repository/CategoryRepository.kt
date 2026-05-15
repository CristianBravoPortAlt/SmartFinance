package com.example.smartfinance.data.repository

import com.example.smartfinance.data.local.CategoryDao
import com.example.smartfinance.data.local.CategoryEntity
import com.example.smartfinance.data.local.TransactionType
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.snapshots
import com.google.firebase.firestore.toObjects
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.tasks.await
import java.util.UUID
import javax.inject.Inject

class CategoryRepository @Inject constructor(
    private val categoryDao: CategoryDao,
    private val auth: FirebaseAuth,
    private val firestore: FirebaseFirestore
) {
    private val categoriesCollection = firestore.collection("categories")
    
    private val currentUserUid: String?
        get() = auth.currentUser?.uid

    fun getAllCategories(): Flow<List<CategoryEntity>> {
        val uid = currentUserUid ?: return kotlinx.coroutines.flow.flowOf(emptyList())
        return categoriesCollection
            .whereEqualTo("userId", uid)
            .snapshots()
            .map { it.toObjects<CategoryEntity>() }
            .onEach { categories ->
                categories.forEach { categoryDao.insertCategory(it) }
            }
    }

    fun getCategoriesByType(type: TransactionType): Flow<List<CategoryEntity>> {
        val uid = currentUserUid ?: return kotlinx.coroutines.flow.flowOf(emptyList())
        return categoriesCollection
            .whereEqualTo("userId", uid)
            .whereEqualTo("type", type.name)
            .snapshots()
            .map { it.toObjects<CategoryEntity>() }
    }

    suspend fun getCategoryById(id: String): CategoryEntity? {
        return categoryDao.getCategoryById(id)
    }

    suspend fun insertCategory(category: CategoryEntity) {
        val uid = currentUserUid ?: return
        val finalCategory = if (category.id.isEmpty()) {
            category.copy(id = UUID.randomUUID().toString(), userId = uid)
        } else {
            category.copy(userId = uid)
        }
        
        categoriesCollection.document(finalCategory.id).set(finalCategory).await()
        categoryDao.insertCategory(finalCategory)
    }

    suspend fun updateCategory(category: CategoryEntity) {
        val uid = currentUserUid ?: return
        val finalCategory = category.copy(userId = uid)
        categoriesCollection.document(finalCategory.id).set(finalCategory).await()
        categoryDao.updateCategory(finalCategory)
    }

    suspend fun deleteCategory(category: CategoryEntity) {
        categoriesCollection.document(category.id).delete().await()
        categoryDao.deleteCategory(category)
    }

    suspend fun initializeCategoriesIfNeeded(): List<CategoryEntity> {
        val uid = currentUserUid ?: return emptyList()
        val current = categoryDao.getAllCategories(uid).first()
        if (current.isNotEmpty()) return current

        val initialCategories = listOf(
            CategoryEntity(name = "Comida", iconResName = "restaurant", colorHex = "#FF5722", type = TransactionType.EXPENSE),
            CategoryEntity(name = "Ocio", iconResName = "movie", colorHex = "#9C27B0", type = TransactionType.EXPENSE),
            CategoryEntity(name = "Transporte", iconResName = "directions_car", colorHex = "#2196F3", type = TransactionType.EXPENSE),
            CategoryEntity(name = "Transferencia", iconResName = "swap_horiz", colorHex = "#607D8B", type = TransactionType.EXPENSE),
            CategoryEntity(name = "Sueldo", iconResName = "attach_money", colorHex = "#4CAF50", type = TransactionType.INCOME),
            CategoryEntity(name = "Regalos", iconResName = "card_giftcard", colorHex = "#FFC107", type = TransactionType.INCOME),
            CategoryEntity(name = "Transferencia", iconResName = "swap_horiz", colorHex = "#607D8B", type = TransactionType.INCOME)
        )
        
        val insertedCategories = mutableListOf<CategoryEntity>()
        initialCategories.forEach { category ->
            val finalCategory = category.copy(id = UUID.randomUUID().toString(), userId = uid)
            categoriesCollection.document(finalCategory.id).set(finalCategory).await()
            categoryDao.insertCategory(finalCategory)
            insertedCategories.add(finalCategory)
        }
        return insertedCategories
    }

    suspend fun deleteAllCategoriesByUser(userId: String) {
        val snapshots = categoriesCollection.whereEqualTo("userId", userId).get().await()
        val batch = firestore.batch()
        for (doc in snapshots.documents) {
            batch.delete(doc.reference)
        }
        batch.commit().await()
        categoryDao.deleteAllCategoriesByUser(userId)
    }
}
