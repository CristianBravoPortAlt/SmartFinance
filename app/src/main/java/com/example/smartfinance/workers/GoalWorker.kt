package com.example.smartfinance.workers

import android.content.Context
import androidx.hilt.work.HiltWorker
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import com.example.smartfinance.data.local.RecurrenceType
import com.example.smartfinance.data.local.TransactionEntity
import com.example.smartfinance.data.local.TransactionType
import com.example.smartfinance.data.repository.BalanceRepository
import com.example.smartfinance.data.repository.GoalRepository
import com.example.smartfinance.utils.NotificationHelper
import dagger.assisted.Assisted
import dagger.assisted.AssistedInject
import kotlinx.coroutines.flow.first
import java.util.Calendar
import java.util.UUID

@HiltWorker
class GoalWorker @AssistedInject constructor(
    @Assisted context: Context,
    @Assisted params: WorkerParameters,
    private val goalRepository: GoalRepository,
    private val balanceRepository: BalanceRepository,
    private val transactionRepository: com.example.smartfinance.data.repository.TransactionRepository,
    private val categoryRepository: com.example.smartfinance.data.repository.CategoryRepository,
    private val notificationHelper: NotificationHelper
) : CoroutineWorker(context, params) {

    override suspend fun doWork(): Result {
        val userId = com.google.firebase.auth.FirebaseAuth.getInstance().currentUser?.uid ?: return Result.success()
        
        val goals = goalRepository.getGoalsByUser(userId).first()
        
        for (goal in goals) {
            if (goal.recurrence == RecurrenceType.NONE || goal.recurrenceAmount <= 0.0) continue
            
            if (goal.currentAmount >= goal.targetAmount) continue

            val now = System.currentTimeMillis()
            val lastTransfer = goal.lastRecurrenceMillis ?: 0L
            
            if (shouldTransfer(goal.recurrence, lastTransfer, now)) {
                val currentBalance = balanceRepository.getBalanceFlow().first()
                
                if (currentBalance >= goal.recurrenceAmount) {
                    val updatedGoal = goal.copy(
                        currentAmount = (goal.currentAmount + goal.recurrenceAmount).coerceAtMost(goal.targetAmount),
                        lastRecurrenceMillis = now
                    )
                    goalRepository.updateGoal(updatedGoal)

                    val categories = categoryRepository.getCategoriesByType(TransactionType.EXPENSE).first()
                    val transferCategory = categories.find { it.name.contains("Transferencia", ignoreCase = true) }

                    transactionRepository.insertTransaction(
                        TransactionEntity(
                            id = UUID.randomUUID().toString(),
                            userId = userId,
                            categoryId = transferCategory?.id ?: "",
                            title = "Ahorro Auto: ${goal.name}",
                            amount = goal.recurrenceAmount,
                            type = TransactionType.EXPENSE,
                            description = "Traspaso automático programado",
                            dateMillis = now
                        )
                    )
                    
                    if (updatedGoal.currentAmount >= updatedGoal.targetAmount) {
                        notificationHelper.showNotification(
                            "¡Objetivo Completado!",
                            "Se ha completado automáticamente el objetivo '${updatedGoal.name}'"
                        )
                    }
                }
            }
        }
        
        return Result.success()
    }

    private fun shouldTransfer(recurrence: RecurrenceType, last: Long, now: Long): Boolean {
        if (last == 0L) return true
        
        val lastCalendar = Calendar.getInstance().apply { timeInMillis = last }
        val nowCalendar = Calendar.getInstance().apply { timeInMillis = now }
        
        return when (recurrence) {
            RecurrenceType.DAILY -> {
                nowCalendar.get(Calendar.DAY_OF_YEAR) != lastCalendar.get(Calendar.DAY_OF_YEAR) ||
                nowCalendar.get(Calendar.YEAR) != lastCalendar.get(Calendar.YEAR)
            }
            RecurrenceType.WEEKLY -> {
                nowCalendar.get(Calendar.WEEK_OF_YEAR) != lastCalendar.get(Calendar.WEEK_OF_YEAR) ||
                nowCalendar.get(Calendar.YEAR) != lastCalendar.get(Calendar.YEAR)
            }
            RecurrenceType.MONTHLY -> {
                nowCalendar.get(Calendar.MONTH) != lastCalendar.get(Calendar.MONTH) ||
                nowCalendar.get(Calendar.YEAR) != lastCalendar.get(Calendar.YEAR)
            }
            RecurrenceType.ANNUAL -> {
                nowCalendar.get(Calendar.YEAR) != lastCalendar.get(Calendar.YEAR)
            }
            else -> false
        }
    }
}
