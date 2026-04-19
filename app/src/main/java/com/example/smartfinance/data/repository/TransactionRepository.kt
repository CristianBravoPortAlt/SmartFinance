package com.example.smartfinance.data.repository

import com.example.smartfinance.data.local.TransactionDao
import com.example.smartfinance.data.local.TransactionEntity
import kotlinx.coroutines.flow.Flow
import javax.inject.Inject

class TransactionRepository @Inject constructor(
    private val transactionDao: TransactionDao
) {
    fun getTransactionsByUser(userId: Int): Flow<List<TransactionEntity>> {
        return transactionDao.getTransactionsByUser(userId)
    }

    suspend fun insertTransaction(transaction: TransactionEntity) {
        transactionDao.insertTransaction(transaction)
    }

    suspend fun deleteTransaction(transaction: TransactionEntity) {
        transactionDao.deleteTransaction(transaction)
    }

    fun getTotalIncome(userId: Int): Flow<Double?> {
        return transactionDao.getTotalIncome(userId)
    }

    fun getTotalExpense(userId: Int): Flow<Double?> {
        return transactionDao.getTotalExpense(userId)
    }

    suspend fun getTransactionById(id: Int): TransactionEntity? {
        return transactionDao.getTransactionById(id)
    }
}
