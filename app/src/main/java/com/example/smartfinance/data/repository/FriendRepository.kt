package com.example.smartfinance.data.repository

import android.content.Context
import android.provider.ContactsContract
import com.example.smartfinance.data.local.NotificationEntity
import com.example.smartfinance.data.local.UserDao
import com.example.smartfinance.data.local.UserEntity
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.snapshots
import com.google.firebase.firestore.toObjects
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.tasks.await
import kotlinx.coroutines.withContext
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class FriendRepository @Inject constructor(
    private val userDao: UserDao,
    private val auth: FirebaseAuth,
    private val firestore: FirebaseFirestore,
    private val userRepository: UserRepository,
    @ApplicationContext private val context: Context
) {
    private val friendshipsCollection = firestore.collection("friendships")
    private val notificationsCollection = firestore.collection("notifications")

    private val currentUserUid: String?
        get() = auth.currentUser?.uid

    @OptIn(kotlinx.coroutines.ExperimentalCoroutinesApi::class)
    fun getFriends(): Flow<List<UserEntity>> {
        val uid = currentUserUid ?: return flowOf(emptyList())
        return friendshipsCollection
            .whereArrayContains("members", uid)
            .whereEqualTo("status", "accepted")
            .snapshots()
            .flatMapLatest { snapshot ->
                val friendIds = snapshot.toObjects<FirestoreFriendship>().map { 
                    it.members.first { id -> id != uid } 
                }
                if (friendIds.isEmpty()) flowOf(emptyList())
                else fetchUsersByIds(friendIds)
            }
    }

    @OptIn(kotlinx.coroutines.ExperimentalCoroutinesApi::class)
    fun getPendingRequests(): Flow<List<UserEntity>> {
        val uid = currentUserUid ?: return flowOf(emptyList())
        return friendshipsCollection
            .whereEqualTo("friendId", uid)
            .whereEqualTo("status", "pending")
            .snapshots()
            .flatMapLatest { snapshot ->
                val senderIds = snapshot.toObjects<FirestoreFriendship>().map { it.userId }
                if (senderIds.isEmpty()) flowOf(emptyList())
                else fetchUsersByIds(senderIds)
            }
    }

    private fun fetchUsersByIds(ids: List<String>): Flow<List<UserEntity>> {
        return firestore.collection("users")
            .whereIn("id", ids)
            .snapshots()
            .map { it.toObjects<UserEntity>() }
            .onEach { users ->
                users.forEach { userDao.registerUser(it) }
            }
    }

    suspend fun addFriendByEmail(email: String): Boolean {
        val uid = currentUserUid ?: return false
        val friend = userRepository.getUserByEmail(email) ?: return false
        
        if (friend.id == uid) return false

        val existing = friendshipsCollection
            .whereArrayContains("members", uid)
            .get().await()
            .toObjects<FirestoreFriendship>()
            .any { it.members.contains(friend.id) }
        
        if (existing) return true

        val friendship = FirestoreFriendship(
            id = java.util.UUID.randomUUID().toString(),
            userId = uid,
            friendId = friend.id,
            members = listOf(uid, friend.id),
            status = "pending"
        )
        friendshipsCollection.document(friendship.id).set(friendship).await()

        val notification = NotificationEntity(
            id = java.util.UUID.randomUUID().toString(),
            userId = friend.id,
            senderId = uid,
            title = "Solicitud de Amistad",
            message = "Alguien quiere ser tu amigo",
            type = "friend_request"
        )
        notificationsCollection.document(notification.id).set(notification).await()

        return true
    }

    suspend fun acceptFriendRequest(friendId: String) {
        val uid = currentUserUid ?: return
        val query = friendshipsCollection
            .whereEqualTo("userId", friendId)
            .whereEqualTo("friendId", uid)
            .get().await()
        
        if (!query.isEmpty) {
            val docId = query.documents.first().id
            friendshipsCollection.document(docId).update("status", "accepted").await()
        }
    }

    suspend fun rejectFriendRequest(friendId: String) {
        deleteFriend(friendId)
    }

    suspend fun deleteFriend(friendId: String) {
        val uid = currentUserUid ?: return
        val query = friendshipsCollection
            .whereArrayContains("members", uid)
            .get().await()
        
        val doc = query.documents.find { 
            val members = it.get("members") as? List<*>
            members?.contains(friendId) == true
        }
        
        doc?.reference?.delete()?.await()
    }

    suspend fun getPhoneContacts(): List<ContactInfo> = withContext(Dispatchers.IO) {
        val contacts = mutableListOf<ContactInfo>()
        val cursor = context.contentResolver.query(
            ContactsContract.CommonDataKinds.Phone.CONTENT_URI,
            null,
            null,
            null,
            ContactsContract.CommonDataKinds.Phone.DISPLAY_NAME + " ASC"
        )

        cursor?.use {
            val nameIndex = it.getColumnIndex(ContactsContract.CommonDataKinds.Phone.DISPLAY_NAME)
            val numberIndex = it.getColumnIndex(ContactsContract.CommonDataKinds.Phone.NUMBER)

            while (it.moveToNext()) {
                val name = it.getString(nameIndex)
                val number = it.getString(numberIndex)
                contacts.add(ContactInfo(name, number))
            }
        }
        contacts.distinctBy { it.phoneNumber }
    }
}

data class FirestoreFriendship(
    val id: String = "",
    val userId: String = "",
    val friendId: String = "",
    val members: List<String> = emptyList(),
    val status: String = "pending"
)

data class ContactInfo(
    val displayName: String,
    val phoneNumber: String
)
