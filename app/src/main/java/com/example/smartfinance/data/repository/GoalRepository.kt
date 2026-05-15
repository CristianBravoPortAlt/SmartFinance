package com.example.smartfinance.data.repository

import com.example.smartfinance.data.local.GoalDao
import com.example.smartfinance.data.local.GoalEntity
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.snapshots
import com.google.firebase.firestore.toObject
import com.google.firebase.firestore.toObjects
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.tasks.await
import java.util.UUID
import javax.inject.Inject

class GoalRepository @Inject constructor(
    private val goalDao: GoalDao,
    private val auth: FirebaseAuth,
    private val firestore: FirebaseFirestore
) {
    private val goalsCollection = firestore.collection("goals")

    private val currentUserUid: String?
        get() = auth.currentUser?.uid

    fun getGoalsByUser(userId: String): Flow<List<GoalEntity>> {
        return goalsCollection
            .whereEqualTo("userId", userId)
            .snapshots()
            .map { it.toObjects<GoalEntity>() }
            .onEach { goals ->
                goals.forEach { goalDao.insertGoal(it) }
            }
    }

    suspend fun insertGoal(goal: GoalEntity) {
        val uid = currentUserUid ?: return
        val finalGoal = if (goal.id.isEmpty()) {
            goal.copy(id = UUID.randomUUID().toString(), userId = uid)
        } else {
            goal.copy(userId = uid)
        }
        
        goalsCollection.document(finalGoal.id).set(finalGoal).await()
        goalDao.insertGoal(finalGoal)
    }

    suspend fun updateGoal(goal: GoalEntity) {
        val uid = currentUserUid ?: return
        val finalGoal = goal.copy(userId = uid)
        goalsCollection.document(finalGoal.id).set(finalGoal).await()
        goalDao.updateGoal(finalGoal)
    }

    suspend fun deleteGoal(goal: GoalEntity) {
        goalsCollection.document(goal.id).delete().await()
        goalDao.deleteGoal(goal)
    }

    suspend fun getGoalById(id: String): GoalEntity? {
        val snapshot = goalsCollection.document(id).get().await()
        return if (snapshot.exists()) {
            val goal = snapshot.toObject<GoalEntity>()
            if (goal != null) goalDao.insertGoal(goal)
            goal
        } else {
            goalDao.getGoalById(id)
        }
    }

    suspend fun deleteAllGoalsByUser(userId: String) {
        val snapshots = goalsCollection.whereEqualTo("userId", userId).get().await()
        val batch = firestore.batch()
        for (doc in snapshots.documents) {
            batch.delete(doc.reference)
        }
        batch.commit().await()
        goalDao.deleteAllGoalsByUser(userId)
    }
}
