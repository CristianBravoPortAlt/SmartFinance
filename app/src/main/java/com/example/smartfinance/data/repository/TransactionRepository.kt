package com.example.smartfinance.data.repository

import com.example.smartfinance.data.local.TransactionDao
import com.example.smartfinance.data.local.TransactionEntity
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.snapshots
import com.google.firebase.firestore.toObject
import com.google.firebase.firestore.toObjects
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.tasks.await
import javax.inject.Inject

/**
 * Repositorio encargado de gestionar las transacciones (ingresos y gastos).
 * Sincroniza Firebase Firestore (Nube) y Room (Local).
 *
 * Nota: hago la sincronnización ya que firebase con la versión gratuita no almacena imagenes y tiene un límite
 */
class TransactionRepository @Inject constructor(
    private val transactionDao: TransactionDao,
    private val firestore: FirebaseFirestore
) {
    private val transactionsCollection = firestore.collection("transactions")

    /**
     * Obtiene un flujo de transacciones filtradas por el ID de usuario.
     * Los datos se recuperan de Firestore y se guardan automáticamente en la BD local.
     * 
     * @param userId El identificador único del usuario.
     * @return Flow con la lista de transacciones ordenada por fecha descendente.
     */
    fun getTransactionsByUser(userId: String): Flow<List<TransactionEntity>> {
        return transactionsCollection
            .whereEqualTo("userId", userId)
            .snapshots()
            .map { it.toObjects<TransactionEntity>().sortedByDescending { t -> t.dateMillis } }
            .onEach { transactions ->
                transactions.forEach { transactionDao.insertTransaction(it) }
            }
    }

    /**
     * Inserta una nueva transacción tanto en Firestore como en la base de datos local.
     * 
     * @param transaction La entidad de la transacción a insertar.
     */
    suspend fun insertTransaction(transaction: TransactionEntity) {
        val docRef = if (transaction.id.isEmpty()) {
            transactionsCollection.document()
        } else {
            transactionsCollection.document(transaction.id)
        }
        val finalTransaction = transaction.copy(id = docRef.id)
        docRef.set(finalTransaction).await()
        transactionDao.insertTransaction(finalTransaction)
    }

    suspend fun deleteTransaction(transaction: TransactionEntity) {
        transactionsCollection.document(transaction.id).delete().await()
        transactionDao.deleteTransaction(transaction)
    }

    fun getTotalIncome(userId: String): Flow<Double?> {
        return getTransactionsByUser(userId).map { transactions ->
            transactions.filter { it.type.name == "INCOME" }.sumOf { it.amount }
        }
    }

    fun getTotalExpense(userId: String): Flow<Double?> {
        return getTransactionsByUser(userId).map { transactions ->
            transactions.filter { it.type.name == "EXPENSE" }.sumOf { it.amount }
        }
    }

    suspend fun getTransactionById(id: String): TransactionEntity? {
        val snapshot = transactionsCollection.document(id).get().await()
        return if (snapshot.exists()) {
            val transaction = snapshot.toObject<TransactionEntity>()
            if (transaction != null) transactionDao.insertTransaction(transaction)
            transaction
        } else {
            transactionDao.getTransactionById(id)
        }
    }

    suspend fun deleteAllTransactionsByUser(userId: String) {
        val snapshots = transactionsCollection.whereEqualTo("userId", userId).get().await()
        val batch = firestore.batch()
        for (doc in snapshots.documents) {
            batch.delete(doc.reference)
        }
        batch.commit().await()
        transactionDao.deleteAllTransactionsByUser(userId)
    }
}
