package com.example.smartfinance.data.repository

import com.example.smartfinance.data.local.NotificationDao
import com.example.smartfinance.data.local.NotificationEntity
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.snapshots
import com.google.firebase.firestore.toObjects
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.tasks.await
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class NotificationRepository @Inject constructor(
    private val notificationDao: NotificationDao,
    private val firestore: FirebaseFirestore
) {
    private val notificationsCollection = firestore.collection("notifications")

    fun getNotificationsByUser(userId: String): Flow<List<NotificationEntity>> {
        return notificationsCollection
            .whereEqualTo("userId", userId)
            .orderBy("timestamp", com.google.firebase.firestore.Query.Direction.DESCENDING)
            .snapshots()
            .map { it.toObjects<NotificationEntity>() }
    }

    suspend fun insertNotification(notification: NotificationEntity) {
        val docRef = if (notification.id.isEmpty()) {
            notificationsCollection.document()
        } else {
            notificationsCollection.document(notification.id)
        }
        val finalNotification = notification.copy(id = docRef.id)
        docRef.set(finalNotification).await()
    }

    suspend fun markAsRead(id: String) {
        notificationsCollection.document(id).update("isRead", true).await()
    }

    suspend fun deleteAllNotificationsByUser(userId: String) {
        val snapshots = notificationsCollection.whereEqualTo("userId", userId).get().await()
        val batch = firestore.batch()
        for (doc in snapshots.documents) {
            batch.delete(doc.reference)
        }
        batch.commit().await()
        notificationDao.deleteAllNotificationsByUser(userId)
    }
}
