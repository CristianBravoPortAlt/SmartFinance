package com.example.smartfinance.workers

import android.content.Context
import androidx.hilt.work.HiltWorker
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import com.example.smartfinance.data.local.RecurrenceType
import com.example.smartfinance.data.local.TransactionEntity
import com.example.smartfinance.data.local.TransactionType
import com.example.smartfinance.data.repository.BalanceRepository
import com.example.smartfinance.data.repository.CategoryRepository
import com.example.smartfinance.data.repository.GoalRepository
import com.example.smartfinance.data.repository.TransactionRepository
import com.example.smartfinance.utils.NotificationHelper
import com.google.firebase.auth.FirebaseAuth
import dagger.assisted.Assisted
import dagger.assisted.AssistedInject
import kotlinx.coroutines.flow.first
import java.util.Calendar
import java.util.UUID

/**
 * Worker en segundo plano encargado de procesar los objetivos de ahorro automáticamente.
 * Utiliza WorkManager para ejecutar transferencias de saldo según la recurrencia
 * configurada (diaria, semanal, mensual o anual).
 */
@HiltWorker
class GoalWorker @AssistedInject constructor(
    @Assisted context: Context,
    @Assisted params: WorkerParameters,
    private val goalRepository: GoalRepository,
    private val balanceRepository: BalanceRepository,
    private val transactionRepository: TransactionRepository,
    private val categoryRepository: CategoryRepository,
    private val notificationHelper: NotificationHelper
) : CoroutineWorker(context, params) {
    
    /**
     * Ejecuta la lógica principal del Worker.
     * Recupera los objetivos del usuario y aplica los ahorros automáticos
     * si se cumplen las condiciones de tiempo y saldo disponible.
     * 
     * @return devuelve success siempre que el proceso finalice correctamente.
     */
    override suspend fun doWork(): Result {
        val userId = FirebaseAuth.getInstance().currentUser?.uid ?: return Result.success()

        // Obtiene los objetivos de ahorro del usuario desde el repositorio
        val goals = goalRepository.getGoalsByUser(userId).first()

        // Verifica si el objetivo tiene una recurrencia configurada
        for (goal in goals) {
            if (goal.recurrence == RecurrenceType.NONE || goal.recurrenceAmount <= 0.0) continue

            if (goal.currentAmount >= goal.targetAmount) continue

            val now = System.currentTimeMillis()
            val lastTransfer = goal.lastRecurrenceMillis ?: 0L

            // Lógica para determinar si ha pasado el tiempo suficiente desde el último ahorro
            if (shouldTransfer(goal.recurrence, lastTransfer, now)) {
                // Actualiza el progreso del objetivo en la base de datos (Room y Firestore)
                val currentBalance = balanceRepository.getBalanceFlow().first()

                if (currentBalance >= goal.recurrenceAmount) {
                    val updatedGoal = goal.copy(
                        currentAmount = (goal.currentAmount + goal.recurrenceAmount).coerceAtMost(
                            goal.targetAmount
                        ),
                        lastRecurrenceMillis = now
                    )
                    goalRepository.updateGoal(updatedGoal)

                    val categories =
                        categoryRepository.getCategoriesByType(TransactionType.EXPENSE).first()
                    val transferCategory =
                        categories.find { it.name.contains("Transferencia", ignoreCase = true) }

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

                    // Si el objetivo se completa, lanza una notificación local al sistema
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

    /**
     * Lógica para determinar si ha pasado el tiempo suficiente desde el último ahorro
     * basado en el tipo de recurrencia.
     * 
     * @param recurrence El tipo de periodo configurado (diario, semanal, etc.)
     * @param last Marca de tiempo del último proceso realizado
     * @param now Marca de tiempo actual
     * @return true si toca realizar una nueva transferencia, false en caso contrario
     */
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
